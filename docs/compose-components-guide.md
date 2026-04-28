# Compose Component Design Guide for Agents

Use this guide as a concise decision reference when writing or reviewing reusable components in Jetpack Compose.

## 1) Component hierarchy

| Level | Purpose | Accepts | Composes |
|---|---|---|---|
| Tokens | Visual decisions | theme values, `CompositionLocal`s | `MaterialTheme`, app tokens |
| Atoms | Small single-purpose UI | primitives, callbacks, slots, `modifier` | raw Compose or Material components |
| Molecules | Small functional groups | data + callbacks + `modifier` | atoms |
| Organisms | Screen sections | data + callbacks + `modifier` | molecules + atoms |
| Templates | Layout structure only | slots + `modifier` | scaffolds + layout containers |
| Screens | App entry points | `ViewModel`, navigation, state owners | templates + organisms + molecules |

Dependency rule: each level should depend only on lower levels. Styling should come from tokens, not hardcoded values.

---

## 2) Pick the right level fast

| If you are building... | It is probably a... | Example |
|---|---|---|
| Brand spacing, colors, elevation, shapes | Token | spacing scale, brand accent |
| A single reusable control | Atom | button, text field, avatar, icon |
| 2–4 atoms working together | Molecule | search bar, list row, card |
| A full section of a screen | Organism | catalog row, settings section |
| A screen layout with slots | Template | top bar + content + bottom bar |
| The route that talks to a `ViewModel` | Screen | screen composable / destination |

Rule: only the **screen** level should talk directly to a `ViewModel`.

---

## 3) Token rules

- Prefer `MaterialTheme.colorScheme`, `typography`, and `shapes` first.
- Create custom tokens only for real app concepts that Material does not cover well: spacing, brand colors, elevation scale, motion scale.
- Expose custom tokens through `CompositionLocal` and a wrapper theme.
- Do not hardcode repeated values like `16.dp`, `14.sp`, or `Color(0xFF...)` across public components.

```kotlin
object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
}

val LocalAppSpacing = staticCompositionLocalOf { AppSpacing }
```

---

## 4) Atom contract

Every public UI atom should usually provide:

- `modifier: Modifier = Modifier`
- token-based styling
- sensible defaults
- slot APIs when content is variable
- a preview or sample usage

```kotlin
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        content = content,
    )
}
```

Naming rule: name by what the component **is**, not where it is used.

- Good: `AppButton`, `MovieCard`, `UserListItem`
- Avoid: `ButtonForSettings`, `HomeMovieCard`, `RedBorderCard`

---

## 5) Default API shapes

### Atom

```kotlin
@Composable
fun AppAvatar(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) { /* ... */ }
```

### Molecule

```kotlin
@Composable
fun UserListItem(
    user: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) { /* compose atoms */ }
```

### Organism

```kotlin
@Composable
fun UserSection(
    title: String,
    users: List<User>,
    onUserClick: (User) -> Unit,
    modifier: Modifier = Modifier,
) { /* compose molecules */ }
```

### Template

```kotlin
@Composable
fun MainScreenTemplate(
    topBar: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = topBar,
        modifier = modifier,
    ) { padding ->
        Box(Modifier.padding(padding)) {
            content()
        }
    }
}
```

---

## 6) Composition rules

- **Atoms** may wrap Material or raw Compose primitives.
- **Molecules** should compose atoms, not duplicate atom styling.
- **Organisms** should compose molecules and atoms, not fetch their own state.
- **Templates** define layout only; they should not know about business data.
- **Screens** own `ViewModel`, navigation, state collection, and side effects.

If state/effects are needed, keep reusable components stateless and push state ownership upward. See `docs/compose-state-guide.md` and `docs/compose-side-effects-guide.md`.

---

## 7) Ask before scaffolding lower levels

When asked to build a component, first identify its level.

If the request is clearly a molecule or organism, ask:

> “This looks like a reusable **[molecule/organism]**. Should I also scaffold the lower-level atoms/tokens it needs, or should I reuse existing ones?”

Default behavior if no answer is available:
- reuse existing atoms/tokens if they already exist
- otherwise build only the requested level, but keep the API atomic-friendly

---

## 8) Do / avoid

### Do

- Use theme or token values for colors, spacing, type, shape
- Add `modifier: Modifier = Modifier` to public composables
- Prefer slot APIs when content may vary
- Keep reusable components stateless where practical
- Name components by purpose, not screen or styling accident
- Add previews for atoms and important molecules
- Keep accessibility in mind: content descriptions, semantics, touch target size

### Avoid

- Hardcoded colors, spacing, typography, or shapes in reusable components
- Public composables without a `modifier`
- Organisms or molecules that read from a `ViewModel`
- Templates that know about business data
- One giant component with many unrelated responsibilities
- Naming by screen, color, or one specific use case
- Mixing layout responsibility into atoms that should stay small and composable

---

## 9) Agent checklist before submitting component-design code

- [ ] Did I choose the correct level: token, atom, molecule, organism, template, or screen?
- [ ] Does the component use theme/token values instead of hardcoded design values?
- [ ] Does every public component expose `modifier: Modifier = Modifier`?
- [ ] Should any content be a slot instead of a hardcoded child?
- [ ] Is reusable UI free of direct `ViewModel` access?
- [ ] Am I composing lower-level pieces instead of duplicating UI patterns?
- [ ] Is the name based on purpose, not screen-specific context?
- [ ] Did I consider previewability and accessibility?

