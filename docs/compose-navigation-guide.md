# Compose Navigation Guide for Agents

Use this guide as a concise decision reference when writing or reviewing navigation in Jetpack Compose.

## 1) Pick the navigation approach fast

| Need | Use | Notes |
|---|---|---|
| App navigation controller in UI | `rememberNavController()` | Create it in composition, not in a `ViewModel` |
| Recommended route modeling | `@Serializable` route classes/objects | Prefer type-safe routes over strings |
| Screen destination | `composable<Route>` | Default destination type |
| Dialog destination | `dialog<Route>` | Use for dialog-style destinations |
| Feature subgraph | `navigation<GraphRoute>(startDestination = ...)` | Group related destinations |
| Observe current destination | `currentBackStackEntryAsState()` | Useful for bars, tabs, selection state |

Rule: prefer type-safe route models for all new navigation code.

---

## 2) Default patterns agents should generate

### Basic typed NavHost

```kotlin
@Serializable
object Home

@Serializable
data class Details(val itemId: Int)

val navController = rememberNavController()

NavHost(
    navController = navController,
    startDestination = Home,
) {
    composable<Home> {
        HomeScreen(onOpenDetails = { id -> navController.navigate(Details(id)) })
    }
    composable<Details> { backStackEntry ->
        val route = backStackEntry.toRoute<Details>()
        DetailsScreen(itemId = route.itemId)
    }
}
```

### Navigate with back stack options

```kotlin
navController.navigate(
    route = Details(itemId = 42),
    navOptions = navOptions {
        launchSingleTop = true
    }
)
```

### Result passing back

```kotlin
navController.previousBackStackEntry
    ?.savedStateHandle
    ?.set("result", "success")

navController.popBackStack()
```

### Nested graph

```kotlin
@Serializable
object FeatureRoot

@Serializable
object FeatureHome

@Serializable
data class FeatureDetails(val id: String)

navigation<FeatureRoot>(startDestination = FeatureHome) {
    composable<FeatureHome> { FeatureHomeScreen() }
    composable<FeatureDetails> { entry ->
        val route = entry.toRoute<FeatureDetails>()
        FeatureDetailsScreen(id = route.id)
    }
}
```

---

## 3) Back stack rules

| Goal | Pattern |
|---|---|
| Go back one screen | `popBackStack()` |
| Avoid duplicate copies of same destination | `launchSingleTop = true` |
| Clear part of history | `popUpTo(...)` |
| Preserve/restores destination state | `saveState = true`, `restoreState = true` |
| Drive selected nav item from back stack | `currentBackStackEntryAsState()` |

Rules:
- `NavController` belongs to the composable host layer.
- `ViewModel` should expose navigation events, not own the controller.
- Use `popUpTo` deliberately for auth flows, tab reselection, and top-level navigation.
- Bottom navigation usually needs `launchSingleTop`, `saveState`, and `restoreState`.

---

## 4) Deep links and arguments

### Deep link destination

```kotlin
@Serializable
data class Details(val itemId: Int)

composable<Details>(
    deepLinks = listOf(
        navDeepLink<Details>(uriPattern = "https://example.com/details/{itemId}")
    )
) { backStackEntry ->
    val route = backStackEntry.toRoute<Details>()
    DetailsScreen(itemId = route.itemId)
}
```

### Complex typed arguments

```kotlin
@Serializable
data class User(val id: Int, val name: String)

@Serializable
data class UserProfile(val user: User)

navController.navigate(UserProfile(User(1, "A")))
```

Rules:
- prefer typed route arguments over manual string construction
- keep route payloads serializable and reasonably small
- use `SavedStateHandle` for return results, not ad-hoc global state

---

## 5) Navigation side-effect rules

- Do not call `navigate()` directly from the composable body based on plain conditions.
- Trigger navigation from event handlers, collected events, or `LaunchedEffect`.
- If navigation depends on view state, key the effect correctly.

```kotlin
LaunchedEffect(shouldNavigate) {
    if (shouldNavigate) {
        navController.navigate(Details(42))
    }
}
```

For event-driven flows, pair with the side-effects guide and collect navigation events in a `LaunchedEffect`.

---

## 6) Testing defaults

```kotlin
val navController = TestNavHostController(context).apply {
    navigatorProvider.addNavigator(ComposeNavigator())
}
```

Test goals:
- start destination is correct
- clicking UI navigates to the expected destination
- arguments deserialize correctly
- back navigation and `popUpTo` behave as expected
- deep links resolve to the right route

Prefer asserting destination state through the test nav controller instead of only checking visible text.

---

## 7) Review flags

| Pattern in code | Flag | Fix |
|---|---|---|
| `navigate("details/42")` | String-based route | Use typed serializable route |
| `NavController(...)` in `ViewModel` | Wrong owner | Keep controller in UI host |
| `navigate(...)` in composable body | Re-navigation on recomposition | Move to event handler or `LaunchedEffect` |
| Mixed typed and string routes | Inconsistent navigation model | Standardize on typed routes |
| Top-level nav without `launchSingleTop` | Duplicate destinations | Add `launchSingleTop = true` |
| Tab nav without `saveState` / `restoreState` | Lost tab state | Add state restore options |

---

## 8) Do / avoid

### Do

- Use `rememberNavController()` in the host composable
- Model destinations as `@Serializable` route types
- Keep screen composables parameter-driven and testable
- Use nested graphs for feature grouping
- Use `SavedStateHandle` for back results
- Observe the back stack for selected UI state

### Avoid

- String route building for new code
- Owning `NavController` in a `ViewModel`
- Navigating during composition without an effect or event
- Passing oversized payloads when an ID is enough
- Mixing route systems in the same graph
- Using navigation as a replacement for ordinary state hoisting

---

## 9) Agent checklist before submitting navigation code

- [ ] Is `rememberNavController()` created in the host composable?
- [ ] Are destinations modeled as typed serializable routes?
- [ ] Are arguments read with `toRoute<T>()`?
- [ ] Does navigation happen from events or effects, not raw composition?
- [ ] Do back stack options (`popUpTo`, `launchSingleTop`, `restoreState`) match the use case?
- [ ] If returning data, did I use `SavedStateHandle`?
- [ ] Is the graph structure clear for features and top-level destinations?
- [ ] Is the navigation testable with `TestNavHostController`?
