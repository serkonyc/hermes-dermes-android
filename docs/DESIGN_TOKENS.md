# Dermes design tokens — Quiet Neural Flagship

Single source of truth. Java `UI.java` and `Motion.java` must mirror these exactly.
All values are dp/sp unless noted. Dark-first system.

## Color — canvas and surfaces

| Token | Hex | Use |
|---|---|---|
| `canvas` | `#05070C` | App background; never pure black |
| `surface_1` | `#0B0F17` | Cards, sheets, nav bar base |
| `surface_2` | `#121826` | Elevated cards, composer |
| `surface_3` | `#1A2234` | Active/pressed, selected pill |
| `hairline` | `#FFFFFF @ 7%` | Card and sheet edges, top nav edge |
| `scrim` | `#05070C @ 72%` | Behind sheets/dialogs |

Neural glow (state layer, never a flat fill):

| Token | Value | Use |
|---|---|---|
| `glow_cyan` | `#2DE2E6 @ 22%` | Idle-ready ambient glow |
| `glow_violet` | `#7A5CFF @ 18%` | Thinking / model busy |
| `glow_radial` | radial at top-center, r=68% width | Header atmosphere |

## Color — text

| Token | Hex | Use |
|---|---|---|
| `text_primary` | `#EAE7F2` | Warm-white body and titles |
| `text_secondary` | `#9AA0B4` | Supporting lines, meta |
| `text_disabled` | `#5A6072` | Disabled only |
| `text_on_accent` | `#04121A` | On bright accent fills |

## Color — accent and semantic

| Token | Hex | Use |
|---|---|---|
| `accent` | `#2DE2E6` | Primary action, selected nav, links |
| `accent_deep` | `#12A9C4` | Gradient end, pressed accent |
| `assistant` | `#7A5CFF` | Assistant identity, streaming label |
| `assistant_deep` | `#4A2FD6` | Gradient end for assistant |
| `user_bubble` | `#123043` | User message surface |
| `amber` | `#F5B94D` | Downloading, paused, pending |
| `green` | `#3ED89A` | Ready, installed, success |
| `red` | `#F2607A` | Destructive, error only |

Rules: accent + assistant appear as gradient only (`accent → assistant`, 135°)
on: active nav pill, thinking glow, send button, hero state chip. Never on body text.
Semantic colors only for state chips and icon tints — never as card fills.
Contrast: body text ≥ AA on all surfaces.

## Type

| Role | Size / sp | Line / sp | Weight | Tracking | Use |
|---|---|---|---|---|---|
| `display` | 34 | 42 | 400 | -0.4 | Hub greeting, once per app |
| `headline` | 26 | 34 | 600 | -0.3 | Screen title in top bar |
| `title` | 17 | 24 | 600 | -0.1 | Card/section titles |
| `body` | 16 | 26 | 400 | 0 | Chat text, descriptions |
| `body_strong` | 16 | 26 | 600 | 0 | Emphasis in body |
| `label` | 13 | 18 | 600 | +0.2 | Chips, nav labels, meta |
| `caption` | 12 | 16 | 500 | +0.3 | Timestamps, sizes |

Minimum size anywhere: 12. Rules: hierarchy comes from type before color.
Chat body line-height 26 for reading. Never bold body at 16 for headings.

## Shape

| Token | dp | Use |
|---|---|---|
| `r_sm` | 14 | Chips, inputs inside cards |
| `r_md` | 20 | List rows, inner cards |
| `r_lg` | 28 | Cards, sheets, model card |
| `r_xl` | 32 | Bottom sheet, hero card |
| `r_pill` | 999 | Composer, nav pill, send button, chips |

## Space and layout

- Scale: 4 / 8 / 12 / 16 / 24 / 32 / 48.
- Screen margins: 24 default, 20 on dense screens. Never edge-touching content.
- Section gap: 32. Card padding: 20. List row min height: 64. Touch min: 48.
- Bottom nav height: 72 + safe area. Top bar: 64 + status bar.
- Content max width when unfolded: 720, side rail instead of bottom nav.

## Elevation

| Level | Shadow | Edge |
|---|---|---|
| `flat` | none | hairline |
| `raised` | `0 8 24 #000 @ 45%` | hairline |
| `float` | `0 18 44 #000 @ 55%` | hairline + top inner highlight `#FFF @ 6%` |

Composer and bottom sheets use `float`. Cards use `raised`.

## Icons

- Single original vector family: 24dp grid, stroke 1.7, round caps/joins.
- Outline = inactive, filled = active (nav). Optical centering, not geometric.
- One meaning per glyph across app + launcher + notifications.
- Set: hub, chat, tools, settings, history, digest, capsule, device, runner,
  update, storage, privacy, download, model, send, stop, mic, attach, back,
  chevron, search, plus, close, check, alert, glow/spark.

## Motion

| Token | Value | Use |
|---|---|---|
| `spring_ui` | damping 0.78, stiffness 380 | Press, chips, toggles |
| `spring_nav` | damping 0.86, stiffness 260 | Nav pill morph, sheet |
| `axis_enter` | 240ms, `cubic(0.2,0,0,1)` + 24dp slide | Destination change |
| `axis_exit` | 180ms, `cubic(0.4,0,1,1)` | Destination leave |
| `container` | 320ms spring_nav | Hub/tools → detail bounds |
| `stagger` | 28ms per item, max 5 items | List/card entrance |
| `glow_cycle` | 2400ms, ease-in-out loop | Thinking glow |
| `reveal` | 120ms fade + 6dp rise | Streaming text blocks |
| `press` | scale 0.965, spring_ui | All pressable |

Haptics: light = nav select, medium = send/toggle, warning = destructive.
Reduced-motion: replace springs/transitions with 90ms alpha crossfade; keep state meaning.

## Status vocabulary

| State | Chip color | Glow | Motion |
|---|---|---|---|
| Model ready | green | cyan idle | none |
| Loading model | cyan | cyan→violet sweep | glow_cycle |
| Thinking | assistant | violet | glow_cycle |
| Downloading | amber | none | progress line |
| Unloaded | secondary text | none | none |
| Error | red | none | none |
</content>
