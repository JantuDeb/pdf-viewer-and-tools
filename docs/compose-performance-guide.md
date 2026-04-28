# Compose Performance Guide for Agents

Use this guide as a concise decision reference when writing or reviewing performance-sensitive UI in Jetpack Compose.

## 1) Core performance model

Every frame has three relevant phases:

| Phase | What happens | State reads here cause |
|---|---|---|
| Composition | Composables run, objects/lambdas may be allocated | recomposition |
| Layout | measure and placement happen | layout updates |
| Draw | pixels are emitted | redraws |

Rule: if a value only affects position, size, or drawing, avoid reading it in composition.

---

## 2) Pick the optimization fast

| Problem | Prefer | Notes |
|---|---|---|
| Expensive derived value | `remember(...) { derivedStateOf { ... } }` | Use only when it saves real work |
| Expensive pure computation | `remember(keys...) { ... }` | Skip for cheap values |
| Position depends on state | layout-phase read such as `Modifier.offset { ... }` | Better than reading in composition |
| Large/dynamic list | `LazyColumn` / `LazyRow` with `key` | Add `contentType` for mixed item types |
| Repeated recomposition due to unstable params | stable/immutable parameter types | Check compiler reports if needed |
| Unclear hot path | profile first | Measure before optimizing |

---

## 3) Default patterns agents should generate

### Defer state reads to layout

```kotlin
Box(
    modifier = Modifier.offset {
        IntOffset(offsetX.value.toInt(), 0)
    }
)
```

Prefer this over:

```kotlin
val x = offsetX.value
Box(modifier = Modifier.offset(x.dp, 0.dp))
```

### Cache expensive work

```kotlin
val metadata = remember(id) { computeMetadata(id) }
```

### Derived list/filter state

```kotlin
val filtered = remember(items, query) {
    derivedStateOf { items.filter { query in it.title } }
}
```

### Lazy list with reuse hints

```kotlin
LazyColumn {
    items(
        items = rows,
        key = { it.id },
        contentType = { it.type },
    ) { row ->
        RowItem(row)
    }
}
```

### Explicitly sized drawing

```kotlin
Canvas(
    modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
) {
    if (size.minDimension <= 0f) return@Canvas
    drawCircle(radius = size.minDimension / 2)
}
```

---

## 4) Stability and recomposition rules

- Stable parameters make skipping easier.
- Prefer immutable UI models for composable parameters.
- Use `@Immutable` for immutable value types.
- Use `@Stable` only when the type really satisfies Compose stability rules.
- Avoid slapping annotations on mutable data just to silence warnings.
- If recomposition behavior is unclear, enable compiler metrics/reports and inspect skippability/stability.

```kotlin
@Immutable
data class Person(val name: String, val age: Int)
```

Compiler reports can help identify:
- composables that are restartable but not skippable
- classes inferred as unstable
- parameters preventing skipping

---

## 5) Lazy list rules

- Always provide stable `key` values for dynamic lists.
- Add `contentType` when the list mixes different item layouts.
- Do not allocate per-item state or expensive objects inside item lambdas unless truly required.
- Pass already-prepared UI models into rows when practical.
- Avoid filtering, sorting, or mapping inline on every recomposition of the list container.

Bad:

```kotlin
LazyColumn {
    items(users) { user ->
        val rowState = remember { mutableStateOf(user) }
        UserRow(rowState.value)
    }
}
```

Better:

```kotlin
LazyColumn {
    items(users, key = { it.id }) { user ->
        UserRow(user)
    }
}
```

---

## 6) Measurement and tooling

Profile before making non-trivial optimizations.

Useful tools:
- Compose compiler reports/metrics for stability and skippability
- Layout Inspector recomposition counts
- Macrobenchmark for startup and frame timing
- Baseline Profiles for Android startup/scroll performance
- platform profilers for Desktop, iOS, and Web in Compose Multiplatform

Typical flow:
1. observe recomposition/layout hot spots
2. confirm unstable params or excessive recomputation
3. optimize the smallest real bottleneck
4. measure again

---

## 7) Review flags

| Pattern in code | Flag | Fix |
|---|---|---|
| expensive calculation in composable body | repeated work | wrap in `remember(keys)` if it is truly expensive |
| filtering/sorting directly in composition | repeated recomputation | use `derivedStateOf` or precompute upstream |
| state read in composition only to drive offset/placement | unnecessary recomposition | defer to layout-phase lambda |
| lazy list without `key` | poor reuse | provide stable item key |
| mixed list without `contentType` | weaker reuse | provide content type |
| `Canvas(Modifier.fillMaxSize())` with no height guard | risky zero-size draw path | add explicit size/constraints and guard size |
| `@Stable` on mutable data class | misleading stability | use immutable types or redesign |
| optimization added without evidence | premature optimization | measure first |

---

## 8) Do / avoid

### Do

- Read state in the latest phase that still works
- Use `remember` for expensive pure computations
- Use `derivedStateOf` for meaningful derived state
- Prefer immutable/stable parameter types
- Add list keys and content types where appropriate
- Explicitly size drawing surfaces and guard zero-size draw cases
- Measure with real tools before and after optimizing

### Avoid

- Reading frequently changing layout state in composition when layout/draw can read it
- Wrapping every cheap value in `remember`
- Filtering large lists inline on every recomposition
- Allocating unnecessary objects inside lazy item lambdas
- Hardcoding performance “fixes” without profiling
- Using stability annotations to paper over mutable models

---

## 9) Agent checklist before submitting performance-sensitive code

- [ ] Did I avoid expensive work in plain composition where possible?
- [ ] Should a state read move to layout or draw phase?
- [ ] Is `remember` used only for state or expensive calculations?
- [ ] Would `derivedStateOf` reduce real recomposition work here?
- [ ] Do lazy lists have stable `key` values and `contentType` when needed?
- [ ] Are composable parameters stable/immutable where practical?
- [ ] Are draw operations safely constrained and guarded against zero size?
- [ ] Did I choose this optimization based on measurement rather than guesswork?

