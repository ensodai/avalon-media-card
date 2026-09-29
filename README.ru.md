<p align="center">
  <img src="16x9logo.png" alt="Avalon MediaCard Logo" width="650" />
</p>

<h1 align="center">Avalon MediaCard</h1>

<p align="center">
  <strong>Персональный онлайн-кинотеатр для вас и ваших друзей.</strong>
</p>

<p align="center">
  <a href="README.md">English</a> • <strong>Русский</strong>
</p>

<p align="center">
  <a href="https://github.com/ensodai/avalon-media-card/pkgs/container/avalon-media-card"><img src="https://img.shields.io/badge/Docker-ghcr.io-009688?style=flat-square&logo=docker&logoColor=white" alt="Docker Container ghcr.io" /></a>
  <a href="https://github.com/ensodai/avalon-media-card/releases"><img src="https://img.shields.io/github/v/release/ensodai/avalon-media-card?style=flat-square&color=4285F4" alt="Последний релиз" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-PolyForm%20Shield-blue?style=flat-square" alt="Лицензия ядра: PolyForm Shield 1.0.0" /></a>
  <a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/Plugin%20SDK-MIT-green?style=flat-square" alt="Лицензия SDK плагинов: MIT" /></a>
</p>

---

<p align="center">
  <img src="assets/screenshots/avalon_media_card_hero.png" alt="Интерфейс Avalon MediaCard на различных устройствах" width="900" />
</p>

## 🌟 О проекте

**Avalon MediaCard** — это современная self-hosted платформа, которая объединяет фильмы, сериалы и аниме в едином минималистичном интерфейсе.

Приложение работает прямо в браузере без обязательной установки дополнительных программ, а также доступно в виде нативных приложений для Smart TV (Android TV), компьютеров (Windows, macOS, Linux) и смартфонов. В отличие от традиционных ресурсоемких медиасерверов, Avalon спроектирован с упором на предельную легкость: серверная часть комфортно работает на недорогих VPS, домашних серверах или микрокомпьютерах класса Raspberry Pi.

---

## ✨ Главные преимущества

* 🚀 **Мгновенный просмотр без скачивания:** Воспроизведение начинается сразу при подключении гибридных источников (CDN или P2P). Больше не нужно ждать, пока десятки гигабайт загрузятся на диск сервера.
* 🍿 **Совместный просмотр (Watch Party):** Смотрите кино синхронно с друзьями, где бы они ни находились. Встроенное лобби позволяет синхронизировать готовность и выбрать статус просмотра (например, *«Смотрю молча, без пауз»* или *«Готов обсуждать»*).
* 🧠 **Умные и приватные рекомендации:** Локальный математический движок анализирует ваши предпочтения и подбирает релевантный контент. Вся аналитика и история просмотров хранятся **исключительно на вашем сервере** и никогда не передаются во внешние облачные сервисы.
* 👥 **Изолированные профили:** У каждого пользователя — своя домашняя лента, персональные оценки, прогресс просмотра эпизодов и независимая история.
* 📺 **Адаптация под любые экраны:** Единый плавный интерфейс, оптимизированный как для управления мышью и клавиатурой на ПК, так и для пультов Smart TV (полноценная D-Pad навигация без задержек).

---

## ⚡ Быстрый старт (Развертывание через Docker)

Рекомендуемый и самый надежный способ запустить сервер Avalon MediaCard — использовать **Docker Compose**. Этот метод сохраняет все профили, настройки и данные пользователей между обновлениями и перезапусками.

### Вариант 1: Запуск в терминале за одну команду

```bash
mkdir -p avalon && cd avalon
curl -fsSL https://raw.githubusercontent.com/ensodai/avalon-media-card/main/docker-compose.yml -o docker-compose.yml
docker compose up -d
```

### Вариант 2: Для Portainer, Dockge или ручной настройки

Если вы используете веб-панели управления контейнерами или настраиваете стек вручную, используйте конфигурацию `compose.yaml`:

```yaml
services:
  avalon-server:
    image: ghcr.io/ensodai/avalon-media-card:latest
    container_name: avalon-media-card
    restart: unless-stopped
    ports:
      - "8080:8080"
    volumes:
      - ./data:/app/data
      - ./plugins:/app/plugins
```

### Вход в систему

Откройте веб-браузер и перейдите по адресу:
`http://localhost:8080` *(или укажите IP-адрес вашего сервера в локальной сети)*.

Данные системного администратора по умолчанию:
* **Логин:** `admin`
* **Пароль:** `admin`

> ⚠️ **Важно:** Сразу после первого входа смените пароль администратора в панели управления и создайте персональные аккаунты для членов семьи или друзей.

---

## 📱 Клиентские приложения

Смотреть контент можно как напрямую через веб-браузер, так и через нативные приложения:

* 📺 **Android TV / Android:** загрузите `avalon-android.apk` из [Последних релизов](https://github.com/ensodai/avalon-media-card/releases/latest).
* 💻 **ПК (Desktop):** сборки для Windows (`.exe` / `.msi`) и Linux (`.deb` / AppImage).
* 🌐 **Веб-версия:** доступна прямо на запущенном сервере по порту `8080`.

Для подключения в приложении укажите адрес вашего сервера `http://<IP_СЕРВЕРА>:8080` и авторизуйтесь.

---

<details>
<summary>🔌 <b>Опционально: подключение онлайн-балансеров (Lampac / Accsdb)</b></summary>

Плагин `lampac-adapter-plugin` позволяет интегрировать сервер Avalon со шлюзом [Lampac](https://github.com/lampac-nextgen/lampac) для получения онлайн-потоков.

Если на стороне Lampac включен механизм авторизации **Accsdb**, Avalon передает идентификатор в каждом запросе.

#### 1. Настройка на стороне Lampac (`init.conf`)

Создайте выделенный аккаунт для Avalon:
```json
"accsdb": {
  "enable": true,
  "accounts": {
    "avalon": "2040-10-17T00:00:00"
  }
}
```
*(Или в панели Lampac Admin → **Users** → Добавить пользователя с ID `avalon`).*

Либо используйте общий пароль (`shared_passwd`):
```json
"accsdb": {
  "enable": true,
  "shared_passwd": "your_shared_secret"
}
```

#### 2. Переменные окружения в Avalon

При необходимости укажите переменные в секции `environment` вашего `docker-compose.yml`:

| Переменная | Обязательна | Описание |
|---|---|---|
| `LAMPAC_HOST` | Рекомендуется | URL Lampac, например `http://192.168.1.10:9118` (по умолчанию `http://localhost:9118`) |
| `LAMPAC_UID` | Если включен Accsdb | ID аккаунта в Accsdb / персональный пароль (передается как `?uid=`) |
| `LAMPAC_TOKEN` | Опционально | Передается как `?token=` |
| `LAMPAC_ACCOUNT_EMAIL` | Опционально | Передается как `?account_email=` (также используется для `shared_passwd`) |

Пример в `docker-compose.yml`:
```yaml
    environment:
      - LAMPAC_HOST=http://lampac:9118
      - LAMPAC_UID=avalon
```

Если авторизация Accsdb в Lampac выключена, переменные `LAMPAC_UID` / `LAMPAC_TOKEN` / `LAMPAC_ACCOUNT_EMAIL` указывать не требуется.

</details>

---

<details>
<summary>🛠️ <b>Архитектура под капотом (Для разработчиков и энтузиастов)</b></summary>

### Технологический стек

* **Сверхлегкий сигнальный сервер:** Бэкенд реализован на **Kotlin 2.1+ / Ktor 3.x (Netty)**. Он выполняет роль координатора API, менеджера базы данных (SQLite / PostgreSQL через JetBrains Exposed ORM) и сигнального узла WebSockets на базе **kotlinx-rpc**.
* **Клиентский рендеринг видеопотока:** В отличие от Plex или Jellyfin, сервер Avalon не перегружает процессор тяжелым транскодированием видео на лету. Вся нагрузка по декодированию ложится на конечное устройство пользователя с задействованием **LibMPV** (Desktop JNA), **AndroidX Media3** (Android TV/Mobile) или **WasmGC + FFmpeg** (браузер).
* **Server-Driven UI (SDUI):** Интерфейс построен на базе **Compose Multiplatform**. Динамические полки рекомендаций, карточки контента и навигационные манифесты передаются сервером в реальном времени.
* **Изолированная модульная система плагинов:** Динамическая подгрузка JAR-плагинов в изолированных `ClassLoader` на базе контракта [`avalon-media-card-core-contract`](https://github.com/ensodai/avalon-media-card-core-contract). Плагины поддерживают горячую перезагрузку без перезапуска сервера.

### Сборка из исходного кода

```bash
# Клонирование репозитория
git clone https://github.com/ensodai/avalon-media-card.git
cd avalon-media-card

# Настройка окружения
cp .env.example .env

# Запуск бэкенда (Ktor)
./gradlew :server:run

# Запуск десктопного клиента
./gradlew :desktopApp:run

# Сборка Android APK
./gradlew :androidApp:assembleRelease

# Сборка WebAssembly клиента
./gradlew :web:wasmJsBrowserDistribution
```

> 📖 Подробную топологию системы, матрицу видеоплееров и инженерный разбор см. в **[ARCHITECTURE.ru.md](ARCHITECTURE.ru.md)**.

</details>

---

## 📜 Лицензирование

* **Ядро платформы и приложения (Core):** Распространяются под лицензией [PolyForm Shield License 1.0.0](LICENSE) (полностью бесплатно для личного использования, самостоятельного хостинга и модификаций; действуют условия защиты от прямой коммерческой конкуренции).
* **Plugin SDK и базовые плагины:** Контракт [`avalon-media-card-core-contract`](https://github.com/ensodai/avalon-media-card-core-contract) и плагины в каталоге `basePlugins/` лицензированы под свободной лицензией [MIT License](https://opensource.org/licenses/MIT).

---

## ⚖️ Юридическое уведомление и отказ от ответственности (Legal Disclaimer)

**Avalon MediaCard** спроектирован исключительно в качестве расширяемого SDK для персональных медиа и локальной экосистемы воспроизведения. Репозиторий проекта предоставляет модульный программный каркас, предназначенный для каталогизации и воспроизведения медиафайлов, находящихся в общественном достоянии (Public Domain), а также законно приобретенных персональных цифровых материалов, размещенных на собственной инфраструктуре пользователя.

Программное обеспечение Avalon MediaCard не размещает, не содержит в базовой поставке, не индексирует, не скрейпит и не распространяет защищенный авторским правом контент, инструменты обхода технических средств защиты (DRM) или сторонние конфигурации доступа к нелицензионным материалам. Все внешние плагины, коннекторы P2P-кеширования и резолверы метаданных разрабатываются, устанавливаются и настраиваются конечным пользователем самостоятельно на свой риск. Авторы и контрибьюторы проекта Avalon MediaCard не несут юридической ответственности за сценарии использования API-интерфейсов и характер модулей, загружаемых пользователями в их частные серверные экземпляры.
