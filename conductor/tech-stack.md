# Tech Stack: Hayai

## Core Infrastructure
- **Programming Language:** Kotlin (1.9.x+) - The primary language for all application logic, leveraging modern features like Coroutines, Flow, and Serialization.
- **Build System:** Gradle with Kotlin DSL - Utilizing Version Catalogs (`libs.versions.toml`) for centralized and type-safe dependency management across multiple modules.
- **Architecture:** Multi-module Clean Architecture - A decoupled and testable structure organized into `:domain`, `:data`, `:presentation-core`, `:presentation-widget`, and `:core` modules.

## User Interface & Presentation
- **UI Framework:** Jetpack Compose - A modern, declarative toolkit for building high-performance Android user interfaces, prioritized for all new screen development.
- **Image Loading:** Coil (highly likely, standard in this ecosystem) - Fast and efficient image loading for manga covers and pages.
- **Localization:** Moko Resources - Cross-platform resource management for multi-language support.

## Data & Persistence
- **Database:** SQLDelight - Type-safe SQL generation and database management, ensuring data integrity and compile-time safety.
- **Serialization:** Kotlin Serialization - Efficient JSON parsing and data serialization for network and local storage.
- **Networking:** OkHttp & Retrofit (standard in Mihon forks) - Robust HTTP client and API integration for manga sources and trackers.

## Concurrency & Reactive Programming
- **Reactive Streams:** Kotlin Flow - For handling asynchronous data streams across the application layers.
- **Concurrency:** Kotlin Coroutines - Structured concurrency for non-blocking and efficient background processing.

## Quality & Monitoring
- **Crash Reporting:** Firebase Crashlytics - Real-time crash monitoring and analysis.
- **Benchmarking:** Macrobenchmark - For tracking and optimizing application performance on real devices.
