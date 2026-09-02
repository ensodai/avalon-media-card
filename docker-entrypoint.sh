#!/usr/bin/env bash
set -e

mkdir -p /app/data /app/plugins

if [ -d /app/default-plugins ] && [ -z "$(ls -A /app/plugins 2>/dev/null)" ]; then
    echo "[Avalon Entrypoint] Initializing default plugins in /app/plugins..."
    cp -r /app/default-plugins/* /app/plugins/ 2>/dev/null || true
fi

rm -f /app/plugins/collaps-plugin.jar /app/plugins/*collaps*.jar 2>/dev/null || true

exec "$@"
