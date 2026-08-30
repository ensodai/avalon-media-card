#!/bin/bash
set -e

CLIENT_DIR="client/src/jvmMain/resources/win32-x86-64"
DESKTOP_DIR="desktopApp/src/jvmMain/resources/win32-x86-64"

# Проверяем, есть ли файлы (кэш)
if [ -f "$CLIENT_DIR/libmpv-2.dll" ]; then
    echo "[MPV Download] DLL файлы уже существуют. Скачивание пропущено."
    exit 0
fi

echo "[MPV Download] Файлы не найдены. Начинаем скачивание..."

# Убеждаемся, что установлен 7z (или 7za)
if ! command -v 7z &> /dev/null && ! command -v 7za &> /dev/null; then
    echo "[MPV Download] ОШИБКА: Не установлена утилита 7z (p7zip-full). Пожалуйста, установите её (sudo apt install p7zip-full) и повторите попытку."
    exit 1
fi
ZIP_CMD=$([ -x "$(command -v 7z)" ] && echo "7z" || echo "7za")

# Ищем последнюю версию через API
LATEST_URL=$(curl -s https://api.github.com/repos/zhongfly/mpv-winbuild/releases/latest | grep "browser_download_url" | grep "mpv-dev-x86_64-20" | grep ".7z" | head -n 1 | cut -d '"' -f 4)

if [ -z "$LATEST_URL" ]; then
    echo "[MPV Download] ОШИБКА: Не удалось получить ссылку на релиз."
    exit 1
fi

echo "[MPV Download] Скачивание: $LATEST_URL"
curl -L -o mpv-dev.7z "$LATEST_URL"

echo "[MPV Download] Распаковка libmpv-2.dll..."
$ZIP_CMD e mpv-dev.7z libmpv-2.dll -r -y > /dev/null

echo "[MPV Download] Копирование файлов в ресурсы..."
mkdir -p "$CLIENT_DIR" "$DESKTOP_DIR"

cp libmpv-2.dll "$CLIENT_DIR/libmpv-2.dll"
cp libmpv-2.dll "$CLIENT_DIR/mpv-2.dll"
cp libmpv-2.dll "$DESKTOP_DIR/libmpv-2.dll"
cp libmpv-2.dll "$DESKTOP_DIR/mpv-2.dll"

echo "[MPV Download] Очистка временных файлов..."
rm mpv-dev.7z libmpv-2.dll

echo "[MPV Download] Успешно завершено!"
