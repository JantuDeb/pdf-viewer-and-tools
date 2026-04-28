# Compose State Management Guide for Agents

Use this guide as a concise decision reference when writing or reviewing state management in Jetpack Compose.

## 1) State ownership rules

- **Composable-only UI state** → `remember` / `rememberSaveable`
- **Screen/business state** → `StateFlow` in `ViewModel` or manager/repository layer
- **One-shot events** → `SharedFlow`
- **Derived values** → `derivedStateOf`
- **Suspend/async result to UI** → `produceState`
- **Compose state to reactive side effects** → `snapshotFlow`

Typical usage:
- collect reactive data in UI
- keep transient UI controls local to the composable
- use derived state for filtered or transformed output
- bridge Compose state to reactive streams only when side effects need it

---

## 2) Pick the right API fast

| Need | Use | Notes |
|---|---|---|
| Local toggle, dialog, menu, selection | `remember { mutableStateOf(...) }` | Lost on config change |
| Primitive local state | `mutableIntStateOf`, `mutableFloatStateOf` | Prefer over boxed `mutableStateOf(0)` |
| Search query, tab, view mode | `rememberSaveable` | Only for saveable screen state |
| Filtered/sorted list | `remember { derivedStateOf { ... } }` | Only for non-trivial work |
| Load async value in UI | `produceState` | Coroutine cancels with composition |
| React to state with Flow ops | `snapshotFlow` | Use in `LaunchedEffect` |
| Latest callback inside long effect | `rememberUpdatedState` | Avoid stale lambdas |
| Complex screen state bundle | `@Stable` state holder | Keep composable params small |
| ViewModel state | `StateFlow` | Collect in UI |
| One-off snackbar/navigation event | `MutableSharedFlow(extraBufferCapacity = 1)` | Do not use `Channel` by default |

---

## 3) Default patterns agents should generate

### UI-local state

```kotlin
var showMenu by remember { mutableStateOf(false) }
var searchQuery by rememberSaveable { mutableStateOf("") }
var page by remember { mutableIntStateOf(0) }
```

### Derived state

```kotlin
val filteredFiles by remember(allFiles, searchQuery) {
    derivedStateOf {
        if (searchQuery.isBlank()) allFiles
        else allFiles.filter { it.displayName.contains(searchQuery.trim(), ignoreCase = true) }
    }
}
```

### ViewModel state boundary

```kotlin
class MyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()
}

@Composable
fun MyScreen(viewModel: MyViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
}
```

If lifecycle-aware collection is not available in the module, fall back to `collectAsState()`.

### Suspend to state

```kotlin
@Composable
fun rememberThumbnail(path: String): Bitmap? =
    produceState<Bitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) { loadBitmap(path) }
    }.value
```

### State to Flow side effect

```kotlin
LaunchedEffect(listState) {
    snapshotFlow { listState.firstVisibleItemIndex }
        .distinctUntilChanged()
        .collect { index -> onScroll(index) }
}
```

---

## 4) Do / avoid

### Do

- Use primitive state APIs for primitives
- Hoist state only as high as needed
- Prefer stateless child composables plus a small stateful wrapper
- Replace list items with `copy(...)` instead of mutating fields in place
- Annotate stable UI models with `@Immutable` or `@Stable` when appropriate
- Use `when (val state = uiState)` for sealed UI state rendering

### Avoid

- `mutableStateOf` inside `ViewModel`
- Plain local vars for UI state
- `derivedStateOf` for cheap expressions
- Assuming `rememberSaveable` works for arbitrary custom classes without a `Saver` / parcelable support
- Mutating objects inside `SnapshotStateList` without replacing the item
- Reading changing state once inside `LaunchedEffect(Unit)` and expecting updates

---

## 5) Safe defaults

- For repository or database flows, collect once in UI and derive filtered output with `derivedStateOf` when the transform is non-trivial.
- Keep transient UI controls local unless another layer truly needs them.
- For navigation, snackbars, and completion events, emit `SharedFlow` events from the owner and collect them in a `LaunchedEffect`.
- For expensive resource or image loading, prefer `produceState` or a ViewModel-backed async pipeline instead of blocking composition.

---

## 6) Agent checklist before submitting state-management code

- [ ] Is UI-only state kept in the composable?
- [ ] Is app/screen state stored outside the composable?
- [ ] Did I use primitive state APIs where possible?
- [ ] Did I avoid mutable Compose state in the `ViewModel`?
- [ ] Did I avoid in-place mutation for observable collections?
- [ ] Did I use `derivedStateOf` only when it saves real work?
- [ ] Did I choose `SharedFlow` for one-shot events?
- [ ] If using `rememberSaveable`, is the value actually saveable?

