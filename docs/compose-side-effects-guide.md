# Compose Side Effects Guide for Agents

Use this guide as a concise decision reference when writing or reviewing side effects in Jetpack Compose.

## 1) Effect ownership rules

- **Run suspend work when inputs change** → `LaunchedEffect(keys...)`
- **Launch async work from click/gesture handlers** → `rememberCoroutineScope()`
- **Register listeners or resources with cleanup** → `DisposableEffect(keys...)`
- **Push Compose state to external systems after composition** → `SideEffect`
- **Keep latest callback/state inside a long-running effect** → `rememberUpdatedState`
- **Convert async/callback sources into Compose `State`** → `produceState`
- **Observe Compose state with Flow operators** → `snapshotFlow`

Typical usage:
- use `rememberCoroutineScope()` for user-triggered async actions
- use `produceState` for loading async values into UI state
- use `snapshotFlow` when Compose state must feed reactive side effects
- keep transient UI flags in Compose state and observe them through effect APIs

---

## 2) Pick the right effect fast

| Need | Use | Notes |
|---|---|---|
| Run once when entering composition | `LaunchedEffect(Unit)` | Use only when truly one-time |
| Re-run coroutine when data changes | `LaunchedEffect(key)` | Key must be stable and meaningful |
| Start coroutine from button/menu click | `rememberCoroutineScope().launch { ... }` | Do not block UI thread |
| Register/unregister listener | `DisposableEffect(key)` | Always include `onDispose` |
| Sync value to analytics / view system | `SideEffect` | Runs after every successful composition |
| Long-lived effect needs latest callback | `rememberUpdatedState` | Avoid restarting effect only for callback changes |
| Suspend/callback result to `State<T>` | `produceState` | Great for loading resources |
| Debounce / distinct state changes | `snapshotFlow` | Use inside `LaunchedEffect` |

---

## 3) Default patterns agents should generate

### Keyed coroutine work

```kotlin
LaunchedEffect(userId) {
    user = repository.loadUser(userId)
}
```

Rule: choose keys that represent the actual dependency. Never use a value that changes every recomposition.

### Event-driven async work

```kotlin
val scope = rememberCoroutineScope()

Button(onClick = {
    scope.launch {
        onSave(repository.save())
    }
}) {
    Text("Save")
}
```

### Cleanup with listener registration

```kotlin
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) onResume()
    }

    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
    }
}
```

### Latest callback without restarting effect

```kotlin
val currentOnTimeout by rememberUpdatedState(onTimeout)

LaunchedEffect(Unit) {
    delay(5_000)
    currentOnTimeout()
}
```

### Async source to state

```kotlin
@Composable
fun rememberThumbnail(path: String): Bitmap? =
    produceState<Bitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) { loadBitmap(path) }
    }.value
```

### Compose state to Flow

```kotlin
LaunchedEffect(listState) {
    snapshotFlow { listState.firstVisibleItemIndex }
        .distinctUntilChanged()
        .collect { index -> onScrollChanged(index) }
}
```

### External synchronization after recomposition

```kotlin
SideEffect {
    analytics.setCurrentScreen(screenName)
}
```

---

## 4) Key rules agents should follow

- `LaunchedEffect(Unit)` means “run once for this composition lifetime”, not “react to state changes”.
- If the effect depends on `query`, `userId`, or similar data, put that data in the key.
- `DisposableEffect` is the default choice for listeners, callbacks, broadcast receivers, lifecycle observers, and other registrations.
- `SideEffect` has **no cleanup** and runs after every successful composition; keep it small and idempotent.
- Use `rememberCoroutineScope` only for event handlers, not as a replacement for `LaunchedEffect`.
- Use `rememberUpdatedState` when a long-running effect should keep the latest lambda or value without restarting.
- Prefer `snapshotFlow { state }.distinctUntilChanged()` when you need Flow operators like debounce, throttle, or distinct.

---

## 5) Do / avoid

### Do

- Use stable keys for `LaunchedEffect`
- Clean up every registration in `onDispose`
- Put long-running or suspend work in effect APIs, not directly in composition
- Wrap external async values with `produceState` when the UI needs `State<T>`
- Use `try/finally` inside coroutines when cancellation must still release resources

### Avoid

- Launching suspend work directly in the composable body
- `runBlocking` in click handlers
- Random / changing keys in `LaunchedEffect`
- `LaunchedEffect(Unit)` when the work should actually react to changing inputs
- Forgetting `onDispose` for listeners or observers
- Using `SideEffect` for resource allocation or subscriptions

---

## 6) Safe defaults

- For resource loading, prefer `produceState` keyed by the input that actually changes the result.
- For user actions such as save, delete, refresh, or submit, use `rememberCoroutineScope` from the composable event handler.
- For bridging Compose state into external behavior, prefer `snapshotFlow` over ad-hoc polling.
- If a screen adds lifecycle observers, receivers, or listener registration, use `DisposableEffect` with explicit cleanup.
- If a screen needs analytics or non-Compose property syncing after recomposition, use a tiny `SideEffect`.

---

## 7) Agent checklist before submitting side-effect code

- [ ] Does each `LaunchedEffect` use the right key?
- [ ] Should this be event-driven work with `rememberCoroutineScope` instead?
- [ ] Does every listener/observer registration have `onDispose` cleanup?
- [ ] Am I avoiding `runBlocking` and other UI-thread blocking calls?
- [ ] If a callback may change, should I use `rememberUpdatedState`?
- [ ] If I need debounce/distinct/Flow operators, did I use `snapshotFlow`?
- [ ] Is `SideEffect` small, idempotent, and free of resource ownership?
