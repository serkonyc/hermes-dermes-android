# Dermes flagship redesign plan

## Diagnosis

v4.15 only added a shell around existing screens. It did not solve product structure or visual quality:

1. Home, Chat, and Models repeat overlapping model and download information.
2. Chat history is a popup rather than a first-class screen.
3. Settings, Device, Update, History, Digest, and tools have no coherent hierarchy.
4. Visual style is assembled screen-by-screen, not from one design system.
5. Existing icons mix raster tiles and vector icons; none define the app as flagship.
6. Current colors are generic dark navy with little atmosphere, depth, or brand identity.
7. Motion is limited to press scale and entrance fade; it does not explain navigation or model state.
8. Elements are distributed mechanically rather than around visual rhythm, focal hierarchy, or reachability.

## Approved product structure

Bottom navigation, three destinations:

- **Chat**: conversation surface.
- **Tools**: Digest, Capsules, History, Device, Neon Runner, model management.
- **Settings**: version and update, storage, privacy, diagnostics.

Hub remains the launcher and navigation root. It is a focused assistant entry point, not a dashboard or a fourth navigation destination.

Navigation rules:

- Hub opens Chat or a selected tool.
- Bottom destinations replace one another instead of stacking.
- Tools open detail screens with a back arrow.
- Chat history opens as a full screen from the Chat header.
- Back returns through the current detail stack to its destination.

Information ownership:

- Chat owns conversation history, runtime state, and composer.
- Tools owns Digest, Capsules, History, Device, Runner, model selection/download, and model parameters.
- Settings owns app-wide preferences only.

## Visual direction: “Quiet Neural Flagship”

The app should feel premium, private, alive, and unmistakably Android-native—not a generic dark chat template.

### Color story

Replace the current blue-gray scheme with a deeper obsidian palette and controlled bioluminescent accents:

- **Canvas**: near-black graphite, not flat `#000`, to preserve depth.
- **Surfaces**: four tonal levels, each only slightly warmer/lighter than the one below.
- **Primary text**: warm-white, never pure white.
- **Accent**: iridescent cyan-teal as the default “intelligence” color.
- **Secondary accent**: violet for assistant identity and model state, used in gradients, not large flat fills.
- **Semantic colors**: amber for download/paused, green for ready, red only for destructive/error.
- Gradient use: restricted to the neural glow, active navigation indicator, assistant identity, and selected states.
- No rainbow palettes, no toy-blue fills, no uniform gray cards.
- Color contrast remains AA minimum for body text and controls.

### Surface and material language

- Obsidian background with a subtle radial glow anchored to the model’s state.
- Elevated surfaces use tonal color plus a very faint edge highlight; no glassmorphism everywhere.
- The composer is the highest physical surface and receives a stronger shadow and subtle glow.
- Bottom navigation is a translucent obsidian sheet with a top edge hairline, not a flat gray rectangle.
- Chat assistant content remains on the canvas for reading; user messages receive the restrained accent surface.

### Icon system

Create one original vector icon family for the app:

- Consistent optical grid and stroke weight.
- Rounded terminals, 24dp canvas, simplified geometry.
- Unselected icons are outline; selected/active icons are filled.
- Navigation, Chat, Tools, Settings, Digest, Capsules, History, Device, Runner, update, storage, privacy, and error icons all come from the same set.
- Replace the current raster tile icons everywhere.
- Launcher icon and notification icons use the same geometry and optical balance.
- Add active/inactive variants for every bottom-navigation icon.

### Typography and distribution logic

Typography carries hierarchy before color or cards:

- **Display / hero greeting**: large and calm, used only once per major screen.
- **Headline**: screen identity, large enough to scan from arm’s length.
- **Title**: group and card titles.
- **Body**: reading-first, with increased line height in Chat.
- **Label / meta**: compact metadata only, never important instructions.

Layout logic:

- 24dp screen margins as the default; dense screens use 20dp.
- One vertical rhythm: 8dp, 12dp, 16dp, 24dp, 32dp, 48dp.
- One focal object per screen: Hub’s assistant state, Chat’s conversation, Tools’ selected capability, Settings’ update state.
- Hub: greeting, current runtime state, and two or three contextual actions—not a grid of equally important tiles.
- Chat: messages are the content; runtime metadata is compact and non-competing; composer is anchored and visually separated.
- Tools: grouped by user intent, using list rows and clear section headers, not a mosaic.
- Settings: grouped rows with trailing state, switches, and chevrons; no feature-card grid.
- Touch targets minimum 48dp, with thumb-reachable primary controls.
- Fold behavior: outer screen uses the compact layout; unfolded state gains a content max-width and side navigation rail.

## Animation system

Motion is stateful and responsive, not decorative:

- Navigation destination changes use a horizontal shared-axis transition plus fade.
- Bottom nav selection morphs an icon pill from old to new position.
- Hub to detail screens uses container-transform-style bounds and fade.
- Chat sends trigger composer compression, message insertion, and assistant-glow start.
- While the model loads or thinks, a slow cyan-violet glow travels through the header, assistant label, and runtime badge.
- Streaming text uses progressive reveal without shimmer noise.
- Lists and tool cards stagger at 20–35ms; the effect stays subtle.
- Press states use spring-like scale and surface feedback, not a crude 0.95 jump.
- Haptics: light for navigation selection, medium for send, warning for destructive actions; all follow system settings.
- Respect reduced-motion settings and omit movement while retaining state clarity.
- Motion durations are short by default; model thinking animation is slow and calm, never frantic.

## Screen concepts

### Hub

- Large greeting and current assistant state.
- One prominent “Continue chat” or “Start chat” control.
- Compact runtime badge: ready, loading, downloading, memory state.
- Three contextual tools maximum, ordered by recency and usefulness.

### Chat

- Full history access from header.
- Clear assistant/user separation.
- Assistant thinking glow.
- Floating pill composer with send/stop morph and attachment affordance in the same system.
- Conversation metadata collapsed unless useful.

### Tools

- Grouped capabilities, each with one-line value statement and state.
- Model management shown as a real operational card: installed, storage, download progress, unload state.
- Digest, Capsules, History, Device, Runner presented as destinations, not competing marketing tiles.

### Settings

- Update status as a premium status card.
- Storage and privacy controls grouped semantically.
- Diagnostics available but visually quiet.

## Implementation plan

1. Freeze the approved IA and produce a hi-fi HTML design board with every screen and state.
2. Define a new color, type, shape, elevation, icon, and motion token set.
3. Build one shared design system in Java Views:
   - Scaffold, header, section, list row, settings row, tool tile, status badge, composer, state view.
4. Generate the complete vector icon set and replace all raster UI icons.
5. Convert the app to Hub + fragment-based bottom navigation:
   - `ChatFragment`
   - `ToolsFragment`
   - `SettingsFragment`
6. Rewrite Chat with history screen, assistant states, and new composer.
7. Rewrite Tools as a capability browser with model management.
8. Rewrite Settings with the same scaffold and components.
9. Implement motion and haptic rules centrally so screens do not invent their own animation.
10. Implement Fold and size-class behavior.
11. Run automated visual checks and screenshot review across all screens/states.
12. Release a visual beta APK before touching functional behavior.

## Acceptance criteria

- No screen uses an ad-hoc title, spacing, icon, card, or color treatment.
- One visual system is recognizable across every screen.
- Navigation, model state, and primary action are understandable within one second.
- No text below 11sp and no contrast failures.
- All icons share geometry and stroke language.
- Every navigation action and model state has a defined animation and fallback.
- The app feels like a coherent flagship product, not a collection of independently styled Activities.
</content>
