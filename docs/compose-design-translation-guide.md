# Compose Design Translation Guide for Agents

Use this guide as a concise decision reference when translating designs into production Jetpack Compose UI.

## 1) Translation workflow

Work top-down and outside-in.

1. identify the root screen structure
2. split the design into visual sections
3. choose the layout primitive for each section
4. map visual styling to theme tokens
5. map interactive elements to Material components first
6. flatten unnecessary nesting before writing code

Rule: translate the **intent** of the design, not every design tool frame literally.

---

## 2) Pick the root layout fast

| Visual pattern | Compose root |
|---|---|
| Top bar + content + bottom bar | `Scaffold` |
| Full-bleed background with overlays | `Box` |
| Static vertical content | `Column` |
| Long or dynamic vertical list | `LazyColumn` |
| Horizontal list of repeated items | `LazyRow` |
| Grid of repeated items | `LazyVerticalGrid` |
| Wrapping chips/tags | `FlowRow` |
| Tabs with page content | `Scaffold` + `TabRow` + pager |
| Drawer + content | `ModalNavigationDrawer` + `Scaffold` |
| Bottom sheet over content | `ModalBottomSheet` or sheet scaffold |

If content can overflow vertically, add scrolling on the first pass.

---

## 3) Pick the section layout fast

| Visual relationship | Use |
|---|---|
| Stacked vertically | `Column` / `LazyColumn` |
| Arranged horizontally | `Row` / `LazyRow` |
| Overlapping/layered | `Box` |
| Fixed-column grid | `LazyVerticalGrid` |
| Wrapping row/column of tags | `FlowRow` / `FlowColumn` |
| Absolute decoration inside a container | `Box` + `align()` / `offset()` |

Questions to ask for every section:
- is it static or a list?
- is it single-axis or layered?
- does it need scrolling?
- does it repeat enough to become its own reusable component?

---

## 4) Prefer semantic Compose over literal drawing

Always check whether a Material component already matches the pattern.

| Visual pattern | Prefer |
|---|---|
| Filled action | `Button` |
| Low-emphasis action | `TextButton` / `OutlinedButton` |
| Elevated surface | `ElevatedCard` |
| Outlined surface | `OutlinedCard` |
| Text input | `TextField` / `OutlinedTextField` |
| App bar | `TopAppBar` variants |
| FAB | `FloatingActionButton` / `ExtendedFloatingActionButton` |
| Bottom navigation | `NavigationBar` |
| Dialog | `AlertDialog` |
| Divider | `HorizontalDivider` |
| Toggle | `Switch`, `Checkbox`, `Slider` |
| Chips | `FilterChip`, `AssistChip`, `SuggestionChip`, `InputChip` |

Rule: if a Material 3 component exists, use it before rebuilding the same appearance with `Box` + modifiers.

---

## 5) Map design properties to Compose correctly

### Layout and sizing

| Design concept | Compose |
|---|---|
| Fixed size | `width`, `height`, `size` |
| Hug contents | default wrap-content behavior |
| Fill width | `fillMaxWidth()` |
| Fill height | `fillMaxHeight()` |
| Fill both | `fillMaxSize()` |
| Min/max constraints | `widthIn`, `heightIn`, `sizeIn` |
| Aspect ratio | `aspectRatio()` |

### Spacing

Parent owns spacing.

| Design spacing | Compose |
|---|---|
| Container padding | `Modifier.padding(...)` on the container |
| Gap in vertical group | `Arrangement.spacedBy(...)` |
| Gap in horizontal group | `Arrangement.spacedBy(...)` |
| Edge-aware horizontal padding | `start` / `end`, not `left` / `right` |

Prefer `Arrangement.spacedBy()` over many manual `Spacer`s for uniform gaps.

### Styling

| Design property | Compose source |
|---|---|
| Colors | `MaterialTheme.colorScheme` or app tokens |
| Typography | `MaterialTheme.typography` |
| Shapes/corner radius | `MaterialTheme.shapes` or `RoundedCornerShape(...)` |
| Elevation | Material elevation APIs or shadow modifiers |
| Gradients | `Brush.linearGradient`, `radialGradient`, `sweepGradient` |
| Borders | `Modifier.border(...)` |
| Opacity | `alpha(...)` or `Color.copy(alpha = ...)` |
| Image fitting | `ContentScale.*` |

Rule: repeated design values should come from tokens, not hardcoded values in component bodies.

---

## 6) Modifier ordering defaults

Default order: **layout → external spacing → decoration → clipping → interaction → internal padding**

```kotlin
Modifier
    .fillMaxWidth()
    .padding(horizontal = 16.dp, vertical = 8.dp)
    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
    .clip(RoundedCornerShape(12.dp))
    .clickable { }
    .padding(16.dp)
```

Common rules:
- `fillMaxWidth()` usually comes before inward padding
- background usually comes before inner padding
- clip before click ripple if bounds should match the shape
- apply scaffold `innerPadding` to content
- add scroll behavior deliberately instead of hoping content fits

---

## 7) Production translation rules

- Flatten design-tool nesting aggressively.
- Name extracted sections by purpose, not raw layout.
- Extract repeated rows/cards/chips into reusable components.
- Keep reusable components stateless; screens own state and effects.
- Use theme tokens for colors, type, spacing, and shape.
- Design for multiple widths with `fillMaxWidth()`, `weight()`, constraints, and adaptive layout patterns.
- Preserve accessibility on first pass: touch targets, semantics, content descriptions, contrast.

If state or ambient tokens are needed, pair this guide with:
- `docs/compose-state-guide.md`
- `docs/compose-side-effects-guide.md`
- `docs/compose-components-guide.md`
- `docs/compose-compositionlocals-guide.md`

---

## 8) Do / avoid

### Do

- Start from the screen skeleton before building small pieces
- Choose `Lazy*` containers for long or dynamic lists
- Prefer semantic Material components over hand-drawn replicas
- Use token-backed spacing, typography, and colors
- Apply `innerPadding` from `Scaffold`
- Add vertical scrolling when content may overflow
- Keep layouts as flat as possible while preserving clarity

### Avoid

- Mirroring every Figma frame as a composable or layout node
- Hardcoded colors, font sizes, and repeated spacing in production UI
- Rebuilding cards, buttons, app bars, and fields from scratch without reason
- Ignoring touch target size or `contentDescription`
- Fixed-width layouts that only work on one device size
- Forgetting scroll behavior for tall content
- Ignoring RTL-safe `start` / `end` spacing

---

## 9) Agent checklist before submitting design-translation code

- [ ] Did I choose the correct root layout?
- [ ] Did I split the design into meaningful sections instead of raw frames?
- [ ] Are repeated items using `LazyColumn`, `LazyRow`, or grid primitives where appropriate?
- [ ] Did I use Material components where they fit semantically?
- [ ] Are colors, type, spacing, and shape driven by theme/tokens?
- [ ] Is modifier order correct for layout, background, clip, click, and padding?
- [ ] Did I apply `innerPadding` and scrolling where needed?
- [ ] Is the result responsive and accessibility-aware?

