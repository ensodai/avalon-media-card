#!/usr/bin/env bash
set -e

# ==============================================================================
# 🌿 Avalon Media Server - 1-Click Interactive Installer (RU / EN)
# ==============================================================================

# Проверка прав root
if [ "$EUID" -ne 0 ]; then
  echo "❌ Пожалуйста, запустите установщик с правами root (sudo)."
  echo "❌ Please run this installer as root (sudo)."
  exit 1
fi

# Очистка экрана и приветствие
clear
echo "=================================================================="
echo "          🌿 Avalon Media Server Installer / Установщик"
echo "=================================================================="
echo ""

# Интерактивное чтение строго из /dev/tty (работает при curl | bash)
read -rp "Продолжить на русском? / Continue in Russian? [Y/n] (Да/нет): " LANG_INPUT </dev/tty || LANG_INPUT="ru"
LANG_INPUT=$(echo "$LANG_INPUT" | tr '[:upper:]' '[:lower:]')

if [[ "$LANG_INPUT" == "n" || "$LANG_INPUT" == "no" || "$LANG_INPUT" == "нет" || "$LANG_INPUT" == "н" ]]; then
  LANG_CODE="en"
else
  LANG_CODE="ru"
fi

# Тексты локализации
if [ "$LANG_CODE" == "ru" ]; then
  MSG_TITLE="Настройка параметров сервера Avalon"
  MSG_DIR_PROMPT="Папка для установки [/opt/avalon]: "
  MSG_USER_MODE_PROMPT="Создать отдельного пользователя 'avalon'? (Рекомендуется для безопасности) [Y/n]: "
  MSG_CUSTOM_USER_PROMPT="Имя пользователя для запуска службы [%s]: "
  MSG_PORT_PROMPT="Порт сервера [8080]: "
  MSG_ADMIN_USER_PROMPT="Логин администратора веб-панели [admin]: "
  MSG_PASS_PROMPT="Пароль администратора (Enter для автогенерации): "
  MSG_GEN_PASS="🔑 Сгенерирован надежный пароль администратора: "
  MSG_DEP_CHECK="🔍 Проверка и установка необходимых компонентов..."
  MSG_JAVA_INSTALL="☕ Установка OpenJDK 21..."
  MSG_JAVA_OK="✅ Java 21 обнаружена."
  MSG_DOWNLOADING="📦 Скачивание последней версии Avalon Server с GitHub..."
  MSG_DOWNLOADING_PLUGINS="🧩 Скачивание и установка плагинов..."
  MSG_SERVICE_SETUP="⚙️ Создание службы systemd (avalon.service)..."
  MSG_SERVICE_START="🚀 Запуск службы Avalon Media Server..."
  MSG_SUCCESS_HEADER="🎉 Avalon Media Server успешно установлен и запущен!"
  MSG_URL="🌐 Адрес веб-панели: "
  MSG_LOGIN="👤 Логин:          "
  MSG_PASS="🔑 Пароль:         "
  MSG_DIR="📂 Рабочая папка:   "
  MSG_SYS_USER="🛡️ Пользователь ОС: "
  MSG_CMDS_TITLE="Команды управления службой:"
  MSG_CMD_STATUS="  Статус:      systemctl status avalon"
  MSG_CMD_RESTART="  Перезапуск:  systemctl restart avalon"
  MSG_CMD_LOGS="  Логи:        journalctl -u avalon -f"
else
  MSG_TITLE="Avalon Server Configuration"
  MSG_DIR_PROMPT="Installation directory [/opt/avalon]: "
  MSG_USER_MODE_PROMPT="Create dedicated 'avalon' user? (Recommended for security) [Y/n]: "
  MSG_CUSTOM_USER_PROMPT="Linux user to run the service [%s]: "
  MSG_PORT_PROMPT="Server port [8080]: "
  MSG_ADMIN_USER_PROMPT="Web admin username [admin]: "
  MSG_PASS_PROMPT="Admin password (press Enter to auto-generate): "
  MSG_GEN_PASS="🔑 Generated secure admin password: "
  MSG_DEP_CHECK="🔍 Checking and installing required dependencies..."
  MSG_JAVA_INSTALL="☕ Installing OpenJDK 21..."
  MSG_JAVA_OK="✅ Java 21 detected."
  MSG_DOWNLOADING="📦 Downloading the latest Avalon Server release from GitHub..."
  MSG_DOWNLOADING_PLUGINS="🧩 Downloading and installing core plugins..."
  MSG_SERVICE_SETUP="⚙️ Configuring systemd service (avalon.service)..."
  MSG_SERVICE_START="🚀 Starting Avalon Media Server service..."
  MSG_SUCCESS_HEADER="🎉 Avalon Media Server successfully installed and running!"
  MSG_URL="🌐 Web URL:          "
  MSG_LOGIN="👤 Admin username:   "
  MSG_PASS="🔑 Admin password:   "
  MSG_DIR="📂 Directory:        "
  MSG_SYS_USER="🛡️ System User:      "
  MSG_CMDS_TITLE="Service management commands:"
  MSG_CMD_STATUS="  Status:      systemctl status avalon"
  MSG_CMD_RESTART="  Restart:     systemctl restart avalon"
  MSG_CMD_LOGS="  Live logs:   journalctl -u avalon -f"
fi

echo ""
echo "=================================================================="
echo "  $MSG_TITLE"
echo "=================================================================="

# 1. Папка установки
read -rp "$MSG_DIR_PROMPT" INSTALL_DIR </dev/tty || INSTALL_DIR="/opt/avalon"
INSTALL_DIR=${INSTALL_DIR:-/opt/avalon}

# 2. Выбор пользователя ОС
read -rp "$MSG_USER_MODE_PROMPT" CREATE_DEDICATED_USER </dev/tty || CREATE_DEDICATED_USER="y"
CREATE_DEDICATED_USER=$(echo "$CREATE_DEDICATED_USER" | tr '[:upper:]' '[:lower:]')

if [[ "$CREATE_DEDICATED_USER" == "n" || "$CREATE_DEDICATED_USER" == "no" || "$CREATE_DEDICATED_USER" == "нет" || "$CREATE_DEDICATED_USER" == "н" ]]; then
  DEFAULT_USER=${SUDO_USER:-root}
  PROMPT_TEXT=$(printf "$MSG_CUSTOM_USER_PROMPT" "$DEFAULT_USER")
  read -rp "$PROMPT_TEXT" RUN_USER </dev/tty || RUN_USER="$DEFAULT_USER"
  RUN_USER=${RUN_USER:-$DEFAULT_USER}
else
  RUN_USER="avalon"
  # Создаем системного пользователя без шелла, если его еще нет
  if ! id -u avalon >/dev/null 2>&1; then
    useradd -r -s /usr/sbin/nologin -d "$INSTALL_DIR" avalon || true
  fi
fi

# 3. Порт
read -rp "$MSG_PORT_PROMPT" SERVER_PORT </dev/tty || SERVER_PORT="8080"
SERVER_PORT=${SERVER_PORT:-8080}

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

# Определение пакетного менеджера и установка Java 21 / curl / unzip
install_dependencies() {
  if command -v apt-get >/dev/null 2>&1; then
    apt-get update -qq
    apt-get install -y -qq curl unzip openjdk-21-jre-headless
  elif command -v dnf >/dev/null 2>&1; then
    dnf install -y -q curl unzip java-21-openjdk-headless
  elif command -v yum >/dev/null 2>&1; then
    yum install -y -q curl unzip java-21-openjdk-headless
  elif command -v pacman >/dev/null 2>&1; then
    pacman -Sy --noconfirm curl unzip jre21-openjdk-headless
  elif command -v apk >/dev/null 2>&1; then
    apk add --no-cache curl unzip openjdk21-jre-headless
  fi
}

if ! command -v java >/dev/null 2>&1 || ! java -version 2>&1 | grep -q "21"; then
  echo "$MSG_JAVA_INSTALL"
  install_dependencies
else
  echo "$MSG_JAVA_OK"
fi

if ! command -v curl >/dev/null 2>&1 || ! command -v unzip >/dev/null 2>&1; then
  install_dependencies
fi

# Создаем структуру каталогов
mkdir -p "$INSTALL_DIR/plugins" "$INSTALL_DIR/data"

# Получение последних ссылок на релиз из GitHub API
echo "$MSG_DOWNLOADING"
REPO="ensodai/avalon-media-card"
RELEASE_JSON=$(curl -s "https://api.github.com/repos/$REPO/releases/latest")

JAR_URL=$(echo "$RELEASE_JSON" | grep -o 'https://[^"]*avalon-server[^"]*\.jar' | head -n 1)
PLUGINS_URL=$(echo "$RELEASE_JSON" | grep -o 'https://[^"]*avalon-plugins[^"]*\.zip' | head -n 1)

if [ -z "$JAR_URL" ]; then
  JAR_URL="https://github.com/$REPO/releases/latest/download/avalon-server.jar"
fi

curl -fsSL -o "$INSTALL_DIR/avalon-server.jar" "$JAR_URL"

# Скачивание плагинов
if [ -n "$PLUGINS_URL" ]; then
  echo "$MSG_DOWNLOADING_PLUGINS"
  curl -fsSL -o "$INSTALL_DIR/plugins.zip" "$PLUGINS_URL"
  unzip -o -q "$INSTALL_DIR/plugins.zip" -d "$INSTALL_DIR/plugins"
  rm -f "$INSTALL_DIR/plugins.zip"
fi

# Запись .env
cat <<EOF > "$INSTALL_DIR/.env"
PORT=$SERVER_PORT
ADMIN_USERNAME=$ADMIN_USER
ADMIN_PASSWORD=$ADMIN_PASSWORD
EOF
chmod 600 "$INSTALL_DIR/.env"

# Установка прав на директорию
chown -R "$RUN_USER:$RUN_USER" "$INSTALL_DIR" || chown -R "$RUN_USER" "$INSTALL_DIR"

# Определение пути к Java
JAVA_PATH=$(command -v java || echo "/usr/bin/java")

# Создание systemd службы
echo "$MSG_SERVICE_SETUP"
cat <<EOF > /etc/systemd/system/avalon.service
[Unit]
Description=Avalon Media Server
After=network.target

[Service]
Type=simple
User=$RUN_USER
WorkingDirectory=$INSTALL_DIR
ExecStart=$JAVA_PATH -Xmx2g -jar $INSTALL_DIR/avalon-server.jar
Restart=always
RestartSec=5
AmbientCapabilities=CAP_NET_BIND_SERVICE
EnvironmentFile=-$INSTALL_DIR/.env

[Install]
WantedBy=multi-user.target
EOF

# Запуск службы
echo "$MSG_SERVICE_START"
systemctl daemon-reload
systemctl enable --now avalon

# Определение локального/внешнего IP
SERVER_IP=$(hostname -I 2>/dev/null | awk '{print $1}')
SERVER_IP=${SERVER_IP:-"127.0.0.1"}

# Финальный красивый вывод
echo ""
echo "=================================================================="
echo " $MSG_SUCCESS_HEADER"
echo "=================================================================="
echo "$MSG_URL http://${SERVER_IP}:${SERVER_PORT}"
echo "$MSG_LOGIN$ADMIN_USER"
echo "$MSG_PASS$ADMIN_PASSWORD"
echo "$MSG_DIR$INSTALL_DIR"
echo "$MSG_SYS_USER$RUN_USER"
echo ""
echo "$MSG_CMDS_TITLE"
echo "$MSG_CMD_STATUS"
echo "$MSG_CMD_RESTART"
echo "$MSG_CMD_LOGS"
echo "=================================================================="
echo ""
