# Compose CompositionLocals Guide for Agents

Use this guide as a concise decision reference when writing or reviewing `CompositionLocal` usage in Jetpack Compose.

## 1) When to use CompositionLocals

Use a `CompositionLocal` when a value is:

- needed by many descendants
- environment-like or configuration-like
- awkward to thread through many intermediate parameters
- naturally scoped to a subtree

Good fits:
- theme tokens
- spacing scales
- brand colors
- locale-like settings
- density, layout direction, Android context, lifecycle owners

Prefer normal parameters when:
- only 1–2 levels need the value
- the dependency should stay explicit and obvious
- the value changes often and fine-grained control matters
- the dependency should be easy to mock without ambient state

---

## 2) Pick the right local API fast

| Need | Use | Notes |
|---|---|---|
| Descendants should react to value updates | `compositionLocalOf` | Use when readers depend on changes |
| Value is effectively static or rarely changes | `staticCompositionLocalOf` | Avoid if descendants expect live updates |
| Default should be computed lazily from context | `compositionLocalWithComputedDefaultOf` | Use only when a computed fallback is actually needed |
| Missing provider should fail loudly | `error("...")` default | Good for required environment values |
| Missing provider is acceptable | nullable default | Good for optional dependencies |

Rule: default to explicit parameters first, then introduce a `CompositionLocal` only when the value is genuinely ambient.

---

## 3) Default patterns agents should generate

### Required local

```kotlin
val LocalAppColors = compositionLocalOf<AppColors> {
    error("AppColors not provided")
}
```

### Static token local

```kotlin
object AppSpacing {
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
}

val LocalAppSpacing = staticCompositionLocalOf { AppSpacing }
```

### Provider scope

```kotlin
CompositionLocalProvider(
    LocalAppColors provides appColors,
    LocalAppSpacing provides AppSpacing,
) {
    Content()
}
```

### Reading a local

```kotlin
@Composable
fun Header() {
    val colors = LocalAppColors.current
    Text(
        text = "Title",
        color = colors.primary,
    )
}
```

### Scoped override

```kotlin
CompositionLocalProvider(LocalAppColors provides darkColors) {
    ScreenA()
    CompositionLocalProvider(LocalAppColors provides lightColors) {
        DialogContent()
    }
}
```

---

## 4) What belongs in a CompositionLocal

Good candidates:
- design tokens
- environment/configuration values
- subtree-scoped policies
- optional platform bridges

Bad candidates:
- screen business state
- `ViewModel` replacement
- generic dependency injection container
- frequently changing mutable state used like a store
- random app services grouped into one ambient object

If the value represents app logic rather than UI environment, it probably should not be a `CompositionLocal`.

---

## 5) Key rules agents should follow

- Declare locals at top level, outside composable functions.
- Keep locals focused and specific; avoid one “god object” local.
- Prefer `staticCompositionLocalOf` for stable design tokens like spacing scales.
- Prefer `compositionLocalOf` when readers must update as the value changes.
- Scope providers as narrowly as practical.
- Read the local once and pass derived values down when that improves clarity.
- Use test providers to inject fake values in previews and tests.

---

## 6) Testing and previews

```kotlin
@Preview
@Composable
fun HeaderPreview() {
    CompositionLocalProvider(
        LocalAppColors provides previewColors,
        LocalAppSpacing provides AppSpacing,
    ) {
        Header()
    }
}
```

```kotlin
composeRule.setContent {
    CompositionLocalProvider(LocalAppColors provides testColors) {
        Header()
    }
}
```

Provide fake or preview values explicitly instead of relying on production globals.

---

## 7) Do / avoid

### Do

- Use locals for theme-like or environment-like values
- Prefer explicit parameters for ordinary data flow
- Fail fast for required locals with no meaningful default
- Keep providers near the subtree that owns the value
- Override locals in tests and previews when needed

### Avoid

- Using `CompositionLocal` as generic dependency injection
- Storing app-wide mutable business state in a local
- Hiding important dependencies that should stay explicit
- Using `staticCompositionLocalOf` for values that must trigger updates
- Packing many unrelated services into one local container
- Replacing screen state hoisting with ambient values

---

## 8) Agent checklist before submitting CompositionLocal code

- [ ] Is this value truly ambient, or would a parameter be clearer?
- [ ] Did I choose `compositionLocalOf` vs `staticCompositionLocalOf` correctly?
- [ ] Is the local declared at top level?
- [ ] Is the default safe, nullable, or intentionally failing?
- [ ] Is the provider scoped to the right subtree?
- [ ] Am I avoiding business-state or service-locator misuse?
- [ ] Can previews and tests override the local easily?

