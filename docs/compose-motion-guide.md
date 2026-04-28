# Compose Motion Guide for Agents

Use this guide as a concise decision reference when writing or reviewing motion in Jetpack Compose.

## 1) Pick the motion API fast

| Need | Use | Notes |
|---|---|---|
| Theme-aware motion inside reusable components | `MaterialTheme.motionScheme` | Preferred for new components |
| Explicit `tween()` / `keyframes()` control | `MotionTokens` + animation API | Use when the caller needs concrete timing/easing |
| Spatial/layout change | `defaultSpatialSpec()` / `fastSpatialSpec()` / `slowSpatialSpec()` | Position, size, bounds, movement |
| Visual/effect change | `defaultEffectsSpec()` / `fastEffectsSpec()` / `slowEffectsSpec()` | Color, alpha, non-spatial changes |
| Enter/exit transition | `AnimatedVisibility` with separate enter/exit specs | Enter and exit should not share the same easing |
| Multi-property coordinated state change | `updateTransition()` | Use one transition for related animated values |

Rule: prefer `MotionScheme` for reusable components so motion can adapt to the theme.

---

## 2) Default patterns agents should generate

### Theme-aware state change

```kotlin
val color by animateColorAsState(
    targetValue = targetColor,
    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
    label = "color"
)

val size by animateDpAsState(
    targetValue = targetSize,
    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
    label = "size"
)
```

### Explicit tween with tokens

```kotlin
val alpha by animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    animationSpec = tween(
        durationMillis = MotionTokens.DurationShort4.toInt(),
        easing = MotionTokens.EasingStandardCubicBezier,
    ),
    label = "alpha"
)
```

### Enter / exit pairing

```kotlin
AnimatedVisibility(
    visible = visible,
    enter = fadeIn(
        animationSpec = tween(
            durationMillis = MotionTokens.DurationMedium2.toInt(),
            easing = MotionTokens.EasingEmphasizedDecelerateCubicBezier,
        )
    ),
    exit = fadeOut(
        animationSpec = tween(
            durationMillis = MotionTokens.DurationShort4.toInt(),
            easing = MotionTokens.EasingEmphasizedAccelerateCubicBezier,
        )
    ),
) {
    content()
}
```

### Coordinated transition

```kotlin
val transition = updateTransition(targetState = expanded, label = "card")

val elevation by transition.animateDp(
    transitionSpec = {
        tween(
            durationMillis = MotionTokens.DurationMedium1.toInt(),
            easing = MotionTokens.EasingEmphasizedCubicBezier,
        )
    },
    label = "elevation"
) { if (it) 8.dp else 0.dp }
```

---

## 3) Duration and easing defaults

### Duration ranges

| Transition type | Preferred range |
|---|---|
| Micro interaction | `DurationShort1`–`DurationShort2` |
| Small state change | `DurationShort3`–`DurationShort4` |
| Container/component change | `DurationMedium1`–`DurationMedium2` |
| Screen-level element | `DurationMedium3`–`DurationMedium4` |
| Shared element / hero | `DurationLong1`–`DurationLong2` |
| Full-screen complex morph | `DurationLong3`+ |

### Easing rules

| Situation | Easing |
|---|---|
| Entering / arriving | `EasingEmphasizedDecelerateCubicBezier` |
| Exiting / leaving | `EasingEmphasizedAccelerateCubicBezier` |
| State change while staying on screen | `EasingEmphasizedCubicBezier` or `EasingStandardCubicBezier` |
| Looping / repeating | `EasingLinearCubicBezier` |

Rules:
- enter = decelerate
- exit = accelerate
- exit is usually faster than enter
- avoid legacy easing tokens in new code
- use `MotionScheme` specs instead of manual tween values when theme-awareness matters

---

## 4) How to choose quickly

1. **Is the component reusable and theme-aware?**
   - yes → use `MaterialTheme.motionScheme`
   - no / caller needs explicit control → use `MotionTokens` with `tween()`

2. **Is the change spatial or non-spatial?**
   - position / size / bounds → spatial spec
   - alpha / color / emphasis → effects spec

3. **Is it enter, exit, or state change?**
   - enter → decelerate easing
   - exit → accelerate easing
   - state change → emphasized/standard easing

4. **How large is the transition?**
   - micro → short
   - component/container → medium
   - shared element or large layout → long

---

## 5) Review flags

| Pattern in code | Flag | Fix |
|---|---|---|
| `tween(200)` or other integer literals | Hardcoded duration | Replace with nearest `MotionTokens.Duration*` |
| `FastOutSlowInEasing`, `LinearOutSlowInEasing`, `FastOutLinearInEasing` | Legacy easing | Replace with `MotionTokens.Easing*` |
| Same easing for enter and exit | Wrong pairing | Use decelerate for enter, accelerate for exit |
| Missing `animationSpec` in animated state | Unspecified motion | Use `MaterialTheme.motionScheme...` or explicit token-based tween |
| New reusable component uses only manual `tween()` | Not theme-aware | Prefer `MaterialTheme.motionScheme` |
| Very long duration on small component | Too slow | Move to short/medium range |

---

## 6) Do / avoid

### Do

- Use `MaterialTheme.motionScheme` in reusable Material 3 components
- Use spatial specs for size/position changes
- Use effects specs for color/alpha changes
- Pair enter and exit with different easing/duration
- Use token durations instead of magic numbers
- Keep large/slow motion for large transitions only

### Avoid

- Hardcoded animation durations in production UI
- Legacy easing constants in new code
- Using the same easing for enter and exit
- Treating color and bounds changes as the same kind of motion
- Very slow motion on small routine interactions
- Ignoring theme-provided motion when building reusable components

---

## 7) Agent checklist before submitting motion code

- [ ] Should this component use `MaterialTheme.motionScheme`?
- [ ] Is the animation spatial or effects-based?
- [ ] Did I choose a token duration instead of a magic number?
- [ ] Are enter and exit using the correct easing pair?
- [ ] Is exit faster than or equal to enter when appropriate?
- [ ] Did I avoid legacy easing constants?
- [ ] Is the motion size/duration appropriate for the scale of the UI change?

