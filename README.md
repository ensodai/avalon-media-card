<p align="center">
  <img src="16x9logo.png" alt="Avalon MediaCard Logo" width="600" />
</p>

<h1 align="center">Avalon MediaCard</h1>

<p align="center">
  <strong>Современный кроссплатформенный медиацентр и видеоплеер на Kotlin Multiplatform</strong>
</p>

---

## 🌟 О проекте

**Avalon MediaCard** — это мощная и гибкая медиа-система с поддержкой онлайн-стриминга, торрентов, агрегации метаданных (TMDB, Trakt, Shikimori) и адаптивного воспроизведения видео на любых платформах.

Проект построен на принципах **Чистой Архитектуры** (Clean Architecture) и концепции **Server-Driven UI (SDUI)** со встроенной системой динамических плагинов.

---

## 📱 Поддерживаемые платформы

| Платформа | Технологический стек | Плеер / Движок |
|---|---|---|
| **Android** (Mobile & Android TV) | Compose Multiplatform, Material 3, AndroidX Media3 | Media3 ExoPlayer, FFmpeg Extension, LibVLC, MPV |
| **Desktop** (Linux / Windows / macOS) | Compose Multiplatform (Desktop JVM), Skiko | LibMPV (Native C-Interop via JNA) |
| **Web** (Modern Browsers) | Compose Multiplatform for Web (**WasmGC** & JS) | PlaysVideo, HLS.js, DASH.js, HTML5 Video |

---

## 🛠 Технологический стек

* **Язык**: [Kotlin 2.1+](https://kotlinlang.org/) (K2 Compiler enabled)
* **UI Фреймворк**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) (Android, Desktop, Web Wasm)
* **Бэкенд**: [Ktor 3.x](https://ktor.io/) (Netty Engine) + [Kotlin-RPC](https://github.com/Kotlin/kotlinx-rpc)
* **База данных**: SQLite + [Exposed ORM](https://github.com/JetBrains/Exposed) (Suspended Transactions DSL/DAO)
* **DI**: [Koin](https://insert-koin.io/) (Koin Annotations)
* **Сериализация**: `kotlinx.serialization` (JSON / Proto)
* **Асинхронность**: Kotlin Coroutines & Flow

---

## 🔌 Архитектура плагинов (`basePlugins`)

Система плагинов построена на общем контракте `avalon-media-card-core-contract`:
* **TorrServer Plugin** — Стриминг торрентов на лету, предпросмотр и парсинг эпизодов.
* **Trakt Plugin** — Синхронизация истории просмотров, списков и рейтингов с Trakt.tv.
* **Recommendation Plugin** — Интеллектуальный движок персонализации на основе семантических векторов и блюпринтов.
* **VK Video Plugin** — Поиск и воспроизведение видеоконтента VK.
* **RuTube Plugin** — Интеграция с каталогом RuTube.
* **AniLibria Plugin** — Онлайн-просмотр аниме с выбором озвучек и качества.
* **Collaps Plugin / Lampac Adapter** — Агрегаторы онлайн-балансеров.
* **Media Details / Home Feed / Person Details** — SDUI-модули интерфейса.

---

## 🚀 Сборка и запуск

### 1. Предварительные требования
* **JDK 21+** (Temurin / Corretto / Oracle)
* **Android SDK** (API Level 35+) для сборки Android-клиента

### 2. Настройка окружения
Скопируйте пример файла переменных окружения:
```bash
cp .env.example .env
```

### 3. Запуск модулей

* **Сборка всех платформ:**
  ```bash
  ./gradlew assemble
  ```

* **Запуск Ktor Backend сервера:**
  ```bash
  ./gradlew :server:run
  ```

* **Запуск Desktop-приложения:**
  ```bash
  ./gradlew :desktopApp:run
  ```

* **Сборка Android Release APK:**
  ```bash
  ./gradlew :androidApp:assembleRelease
  ```

* **Сборка Web Wasm:**
  ```bash
  ./gradlew :web:wasmJsBrowserDistribution
  ```

---

## 📄 Лицензия

Распространяется под лицензией MIT. Подробности см. в файле LICENSE.
