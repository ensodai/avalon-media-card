#!/usr/bin/env bash
set -e

# ==============================================================================
# 🌿 Avalon Media Server - 1-Click Rootless Installer (RU / EN)
# ==============================================================================

# Очистка экрана и приветствие
clear
echo "=================================================================="
echo "          🌿 Avalon Media Server Installer / Установщик"
echo "=================================================================="
echo ""

# Интерактивное чтение языка строго из /dev/tty
read -rp "Продолжить на русском? / Continue in Russian? [Y/n] (Да/нет): " LANG_INPUT </dev/tty || LANG_INPUT="ru"
LANG_INPUT=$(echo "$LANG_INPUT" | tr '[:upper:]' '[:lower:]')

if [[ "$LANG_INPUT" == "n" || "$LANG_INPUT" == "no" || "$LANG_INPUT" == "нет" || "$LANG_INPUT" == "н" ]]; then
  LANG_CODE="en"
else
  LANG_CODE="ru"
fi

DEFAULT_DIR="$HOME/.avalon"

# Тексты локализации
if [ "$LANG_CODE" == "ru" ]; then
  MSG_TITLE="Настройка параметров сервера Avalon"
  MSG_DIR_PROMPT="Папка для установки [$DEFAULT_DIR]: "
  MSG_PORT_PROMPT="Порт сервера [8080]: "
  MSG_ADMIN_USER_PROMPT="Логин администратора веб-панели [admin]: "
  MSG_PASS_PROMPT="Пароль администратора (Enter для автогенерации): "
  MSG_GEN_PASS="🔑 Сгенерирован надежный пароль администратора: "
  MSG_WEB_PROMPT="Установить веб-клиент? [Y/n] (Да/нет): "
  MSG_DEP_CHECK="🔍 Проверка окружения..."
  MSG_JAVA_MISSING="❌ Java 21 не найдена! Пожалуйста, установите Java 21 (например: sudo apt install openjdk-21-jre-headless)"
  MSG_JAVA_OK="✅ Java 21 обнаружена."
  MSG_DOWNLOADING="📦 Скачивание последней версии Avalon Server с GitHub..."
  MSG_DOWNLOADING_PLUGINS="🧩 Скачивание и распаковка базовых плагинов..."
  MSG_DOWNLOADING_WEB="🌐 Скачивание и распаковка веб-клиента..."
  MSG_SERVICE_SETUP="⚙️ Настройка пользовательской службы (systemd)..."
  MSG_SERVICE_START="🚀 Запуск службы Avalon Media Server..."
  MSG_SUCCESS_HEADER="🎉 Avalon Media Server успешно установлен и запущен!"
  MSG_URL="🌐 Сервер API / RPC: "
  MSG_WEB_URL="🌐 Веб-клиент:        "
  MSG_LOGIN="👤 Логин:          "
  MSG_PASS="🔑 Пароль:         "
  MSG_DIR="📂 Рабочая папка:   "
  MSG_CMDS_TITLE="Команды управления службой:"
  MSG_CMD_STATUS="  Статус:      systemctl --user status avalon"
  MSG_CMD_RESTART="  Перезапуск:  systemctl --user restart avalon"
  MSG_CMD_LOGS="  Логи:        journalctl --user -u avalon -f"
else
  MSG_TITLE="Avalon Server Configuration"
  MSG_DIR_PROMPT="Installation directory [$DEFAULT_DIR]: "
  MSG_PORT_PROMPT="Server port [8080]: "
  MSG_ADMIN_USER_PROMPT="Web admin username [admin]: "
  MSG_PASS_PROMPT="Admin password (press Enter to auto-generate): "
  MSG_GEN_PASS="🔑 Generated secure admin password: "
  MSG_WEB_PROMPT="Install web client? [Y/n]: "
  MSG_DEP_CHECK="🔍 Checking environment..."
  MSG_JAVA_MISSING="❌ Java 21 not found! Please install Java 21 (e.g.: sudo apt install openjdk-21-jre-headless)"
  MSG_JAVA_OK="✅ Java 21 detected."
  MSG_DOWNLOADING="📦 Downloading the latest Avalon Server release from GitHub..."
  MSG_DOWNLOADING_PLUGINS="🧩 Downloading and unpacking core plugins..."
  MSG_DOWNLOADING_WEB="🌐 Downloading and unpacking web client..."
  MSG_SERVICE_SETUP="⚙️ Configuring user service (systemd)..."
  MSG_SERVICE_START="🚀 Starting Avalon Media Server service..."
  MSG_SUCCESS_HEADER="🎉 Avalon Media Server successfully installed and running!"
  MSG_URL="🌐 Server API / RPC: "
  MSG_WEB_URL="🌐 Web Client:        "
  MSG_LOGIN="👤 Admin username:   "
  MSG_PASS="🔑 Admin password:   "
  MSG_DIR="📂 Directory:        "
  MSG_CMDS_TITLE="Service management commands:"
  MSG_CMD_STATUS="  Status:      systemctl --user status avalon"
  MSG_CMD_RESTART="  Restart:     systemctl --user restart avalon"
  MSG_CMD_LOGS="  Live logs:   journalctl --user -u avalon -f"
fi

echo ""
echo "=================================================================="
echo "  $MSG_TITLE"
echo "=================================================================="

# 1. Папка установки
read -rp "$MSG_DIR_PROMPT" INSTALL_DIR </dev/tty || INSTALL_DIR="$DEFAULT_DIR"
INSTALL_DIR=${INSTALL_DIR:-$DEFAULT_DIR}
INSTALL_DIR="${INSTALL_DIR/#\~/$HOME}"

# 2. Порт
read -rp "$MSG_PORT_PROMPT" SERVER_PORT </dev/tty || SERVER_PORT="8080"
SERVER_PORT=${SERVER_PORT:-8080}

# 3. Веб-клиент
read -rp "$MSG_WEB_PROMPT" INSTALL_WEB_INPUT </dev/tty || INSTALL_WEB_INPUT="y"
INSTALL_WEB_INPUT=$(echo "$INSTALL_WEB_INPUT" | tr '[:upper:]' '[:lower:]')
if [[ "$INSTALL_WEB_INPUT" == "n" || "$INSTALL_WEB_INPUT" == "no" || "$INSTALL_WEB_INPUT" == "нет" || "$INSTALL_WEB_INPUT" == "н" ]]; then
  INSTALL_WEB=false
else
  INSTALL_WEB=true
fi

# 4. Логин веб-админа
read -rp "$MSG_ADMIN_USER_PROMPT" ADMIN_USER </dev/tty || ADMIN_USER="admin"
ADMIN_USER=${ADMIN_USER:-admin}

# 5. Пароль веб-админа
read -rp "$MSG_PASS_PROMPT" ADMIN_PASSWORD </dev/tty || ADMIN_PASSWORD=""
if [ -z "$ADMIN_PASSWORD" ]; then
  if command -v openssl >/dev/null 2>&1; then
    ADMIN_PASSWORD=$(openssl rand -base64 12 | tr -dc 'a-zA-Z0-9!@#%')
    ADMIN_PASSWORD=${ADMIN_PASSWORD:0:10}
  else
    ADMIN_PASSWORD=$(tr -dc 'a-zA-Z0-9' < /dev/urandom | head -c 10)
  fi
  echo "$MSG_GEN_PASS $ADMIN_PASSWORD"
fi

echo ""
echo "$MSG_DEP_CHECK"

# Проверка наличия Java 21
if ! command -v java >/dev/null 2>&1; then
  echo "$MSG_JAVA_MISSING"
  exit 1
fi
echo "$MSG_JAVA_OK"

# Создаем структуру каталогов
mkdir -p "$INSTALL_DIR/plugins" "$INSTALL_DIR/data"

# Скачивание сервера и плагинов с GitHub Releases
echo "$MSG_DOWNLOADING"
REPO="ensodai/avalon-media-card"
JAR_URL="https://github.com/$REPO/releases/latest/download/avalon-server.jar"
PLUGINS_URL="https://github.com/$REPO/releases/latest/download/avalon-plugins.zip"
WEB_URL="https://github.com/$REPO/releases/latest/download/avalon-web.zip"

curl -fsSL -o "$INSTALL_DIR/avalon-server.jar" "$JAR_URL"

echo "$MSG_DOWNLOADING_PLUGINS"
curl -fsSL -o "$INSTALL_DIR/plugins.zip" "$PLUGINS_URL"
unzip -o -q "$INSTALL_DIR/plugins.zip" -d "$INSTALL_DIR/plugins"
rm -f "$INSTALL_DIR/plugins.zip"

# Скачивание и распаковка веб-клиента при выборе
if [ "$INSTALL_WEB" = true ]; then
  mkdir -p "$INSTALL_DIR/web"
  echo "$MSG_DOWNLOADING_WEB"
  if curl -fsSL -o "$INSTALL_DIR/web.zip" "$WEB_URL"; then
    unzip -o -q "$INSTALL_DIR/web.zip" -d "$INSTALL_DIR/web"
    rm -f "$INSTALL_DIR/web.zip"
  fi
fi

# Запись .env
cat <<EOF > "$INSTALL_DIR/.env"
PORT=$SERVER_PORT
ADMIN_USERNAME=$ADMIN_USER
ADMIN_PASSWORD=$ADMIN_PASSWORD
EOF
chmod 600 "$INSTALL_DIR/.env"

# Создание скриптов запуска и остановки
JAVA_PATH=$(command -v java || echo "java")

cat <<EOF > "$INSTALL_DIR/start.sh"
#!/usr/bin/env bash
cd "$INSTALL_DIR"
$JAVA_PATH -Xmx2g -jar avalon-server.jar
EOF
chmod +x "$INSTALL_DIR/start.sh"

# Настройка службы systemd пользователя (~/.config/systemd/user/)
USER_SYSTEMD_DIR="$HOME/.config/systemd/user"
mkdir -p "$USER_SYSTEMD_DIR"

echo "$MSG_SERVICE_SETUP"
cat <<EOF > "$USER_SYSTEMD_DIR/avalon.service"
[Unit]
Description=Avalon Media Server
After=network.target

[Service]
Type=simple
WorkingDirectory=$INSTALL_DIR
ExecStart=$JAVA_PATH -Xmx2g -jar $INSTALL_DIR/avalon-server.jar
Restart=always
RestartSec=5
EnvironmentFile=-$INSTALL_DIR/.env

[Install]
WantedBy=default.target
EOF

# Запуск службы через systemctl --user (если systemd доступен)
if command -v systemctl >/dev/null 2>&1; then
  echo "$MSG_SERVICE_START"
  systemctl --user daemon-reload || true
  systemctl --user enable --now avalon 2>/dev/null || true
fi

# Определение локального IP
SERVER_IP=$(hostname -I 2>/dev/null | awk '{print $1}')
SERVER_IP=${SERVER_IP:-"127.0.0.1"}

# Финальный красивый вывод
echo ""
echo "=================================================================="
echo " $MSG_SUCCESS_HEADER"
echo "=================================================================="
if [ "$INSTALL_WEB" = true ]; then
  echo "$MSG_WEB_URL http://${SERVER_IP}:${SERVER_PORT}"
fi
echo "$MSG_URL http://${SERVER_IP}:${SERVER_PORT}/api/rpc"
echo "$MSG_LOGIN$ADMIN_USER"
echo "$MSG_PASS$ADMIN_PASSWORD"
echo "$MSG_DIR$INSTALL_DIR"
echo ""
echo "$MSG_CMDS_TITLE"
echo "$MSG_CMD_STATUS"
echo "$MSG_CMD_RESTART"
echo "$MSG_CMD_LOGS"
echo "  Ручной запуск: $INSTALL_DIR/start.sh"
echo "=================================================================="
echo ""
