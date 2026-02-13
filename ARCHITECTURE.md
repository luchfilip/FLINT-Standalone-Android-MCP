# Architecture Rules

Strict rules for Android development in this project. No exceptions.

**Stack:** Kotlin, Jetpack Compose, Hilt DI, Navigation Compose, MVI pattern, Clean Architecture.
**minSdk = 28, targetSdk = 35.**

---

## ViewModel Rules

- NEVER inject `Context`, `@ApplicationContext`, or `@ActivityContext` into ViewModels. ViewModels must have zero Android framework dependencies. Need Context? Wrap the operation behind an interface. Inject the interface. The implementation lives outside the ViewModel.
- NEVER use `android.util.Log`, `Context`, `Intent`, `Build.VERSION`, or any `android.*` import in ViewModels. If you need something from Android, create a wrapper interface. Inject that interface.
- NEVER do work in ViewModel `init` blocks. No network calls, no database reads, no flow collection in `init`. All state transitions must be triggered by events from the UI layer. Start with an initial state, then transition via events.
- NEVER instantiate objects inside ViewModels or inside any class. Everything goes through DI. No `val x = SomeClass()`. No `SomeClass.create()`.
- ViewModels receive `UiEvent`, update `UiState`, and emit `UiEffect`. That is the entire surface area. Nothing else.
- `UiState` is a `data class` or `sealed interface` with `@Immutable` annotation. Use `ImmutableList` for all collections.
- `UiEvent` is a `sealed interface`. The ViewModel exposes one `onEvent(event: UiEvent)` function. No other public methods.
- `UiEffect` is a `sealed interface` for one-shot actions. Examples: show snackbar, trigger navigation, show dialog.
- Use `combine()` on reactive flows from dependencies. Never poll with `while(true) + delay`.

---

## Dependency Injection Rules

- Every dependency must be injected. No object creation inside classes. No `val x = SomeClass()`.
- Classes that touch Android framework (`Context`, `Intent`, `Notification`, `PackageManager`, etc.) must hide behind interfaces. The interface goes in the domain layer. The implementation goes in the framework layer.
- Use `@Binds` for interface-to-implementation mappings in Hilt modules. Use `@Provides` only when you need construction logic.
- Singletons that expose observable state must use `StateFlow`. Never expose polling methods like `fun getCount(): Int`.

---

## Reactivity Rules

- NEVER use `while(true) + delay` for polling. Be reactive. Observe `StateFlow` with `combine()`.
- If a dependency exposes state (`isRunning`, `toolCount`, `logs`), it must expose it as `StateFlow`. Consumers collect. They never poll.
- Batch rapid updates naturally through `StateFlow` (it already deduplicates). If more control is needed, use `conflate()` or `debounce()`.

---

## Composable Rules

- Every screen has two composables: `XScreen` (root) and `XContent` (stateless).
- `XScreen` accesses the ViewModel, collects state, and passes it to `XContent`.
- `XContent` receives state and callback lambdas only. No ViewModel reference. No side effects.
- Content composables must stay high-level. Extract sections into separate composables when nesting exceeds 3 levels inside the content.
- `LaunchedEffect(Unit)` in the root `XScreen` composable triggers the initial event (e.g., `ObserveState`, `LoadData`).
- Navigation lambdas (`onNavigateBack`, `onNavigateToX`) are passed as parameters. Never hardcode navigation inside composables.

---

## Testability Rules

- ViewModels must be testable without Android framework. No Robolectric needed.
- Dependencies that use `android.*` APIs must implement an interface so they can be faked in tests.
- Concrete classes like loggers that use `android.util.Log` must have an interface. The ViewModel injects the interface. Tests provide a fake.

---

## API Level Rules

- minSdk is 28. Do not add `Build.VERSION.SDK_INT` checks for APIs available at API 28 or below.
- This includes: `NotificationChannel` (API 26), `startForegroundService` (API 26), all O+ and P+ APIs.
- Only add version checks for APIs introduced AFTER API 28.

---

## Navigation Rules

- Use type-safe navigation with `@Serializable` route objects.
- Use `composable<T>` registration in the `NavGraph`.
- String routes are forbidden.

---

## File Organization

- One feature = one package under `ui/` (e.g., `ui/hub/`, `ui/settings/`, `ui/flintapps/`).
- Each feature package contains: `ViewModel.kt` and `Screen.kt`.
- Shared components go in `ui/components/`.
- Theme files go in `ui/theme/`.
- Navigation graph and routes go in `nav/`.

---

## State Management

- Use `ImmutableList` from `kotlinx.collections.immutable` for all list state.
- Use `@Immutable` annotation on state, event, and effect classes.
- State classes use `data class`. Use `sealed interface` for distinct states (`Loading`, `Loaded`, `Error`).
- Default state is always the initial state. No init work. No loading triggered in the constructor.
