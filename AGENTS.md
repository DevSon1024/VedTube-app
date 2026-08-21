# AI Agent Instructions for VedTube Development

This document serves as the absolute source of truth for any AI agent or LLM assisting with the development of the **VedTube** project. You must strictly adhere to these rules, architectural guidelines, and development philosophies before generating or modifying any code.

## 1. Core Development Philosophy

- **Goal:** VedTube is a native Android application built with Kotlin and Jetpack Compose.
- **Focus:** Prioritize a clean, lag-free, and smooth native user experience. The app must work smoothly without any bottleneck bugs or UI stuttering.
- **Stability First:** Continually improve and refactor code to ensure the app does not crash under any circumstances. Implement robust error handling, edge-case management, and safe state recovery throughout the application.
- **No Hallucinations:** Only use existing APIs, classes, and resources within the project. If you are unsure about an existing implementation, ask the user to fetch the file contents.

## 2. UI / Jetpack Compose Guidelines

- **Framework:** Jetpack Compose is the primary UI framework. Avoid legacy XML layouts entirely for UI screens (XML is only permitted for drawables, vector assets, and basic values).
- **Material Design:** Strictly utilize Material Design 3 (M3) components and styling (`androidx.compose.material3.*`) to ensure a clean, native, and smooth experience.
- **Composables:** Keep composable functions highly focused and modular. Extract reusable UI elements into the appropriate `ui/` or `components/` packages.
- **State Hoisting:** Prefer state hoisting for UI components to keep them stateless and reusable where appropriate. Do not perform heavy O(N) calculations, I/O, or file operations directly within composable functions to prevent UI bottlenecks.

## 3. Code Quality & Performance Optimization

- **Language:** Kotlin is the exclusive programming language.
- **Asynchronous Operations:** Use Kotlin Coroutines and Flows (`StateFlow`/`SharedFlow`) for all asynchronous programming and state observation. Avoid RxJava or traditional callback interfaces.
- **Null Safety & Crash Prevention:** Handle nullable types safely. **Never** use the not-null assertion operator (`!!`) unless absolutely and undeniably necessary (and accompanied by an explanatory inline comment). Rely on safe calls (`?.`) and Elvis operators (`?:`) to prevent `NullPointerExceptions`.
- **Performance:** Prioritize lag-free, 60/120fps performance. Avoid memory thrashing by efficiently caching instances (like formatters) and using optimized data loading strategies (like Paging 3) for large media collections.
- **File & I/O Operations:** Always dispatch database (Room), network, or file writing operations to `Dispatchers.IO` to ensure the main thread is never blocked.

## 4. Documentation & Update Tracking

You must actively maintain the project's changelog. After every completed task, error resolution, or feature addition, you must append an entry to the `update_details.md` file.

**Format for `update_details.md` entries:**

- **Date**: in format DD/MM/YY (H:M)
- **issue**: what issue that we are going solve. lenght should be 1-2 lines
- **Type of Details:** (e.g., Error Solving, New Update, Refactor, Performance Improvement, More)
- Do not read the whole file every time; just insert the data at the end with a date and time.
- **Description:** A brief, clear summary of what was changed, fixed, or added in the recent chat/interaction.
- After the details of the latest update, you must append only `---` to close out that specific chat area/session.
- Do not include any filler text.
- **Git:** Do not commit or push anything until explicitly being asked to do so.
