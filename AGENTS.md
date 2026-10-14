# IrishSearch agent instructions

- Never use `rm -rf`. Use `trash` to move files or directories to Trash.
- For Android implementation, read and apply the `android-development` skill at `/Users/iheanyi/.agents/skills/android-development/SKILL.md` whenever it is relevant.
- For UI design work, read and apply the `design` skill at `/Users/iheanyi/.agents/skills/design/SKILL.md` and use the Mobbin MCP for concrete design inspiration before changing the interface.
- For native UI design, also apply `mobile-android-design` at `/Users/iheanyi/.agents/skills/mobile-android-design/SKILL.md` and the `emil-design-eng` skill when available. Translate its motion principles to Compose rather than copying web CSS patterns.
- Follow current Google Material 3 and Jetpack Compose guidance for every Android view. Use `MaterialTheme.colorScheme`, `typography`, and `shapes` plus Material 3 component defaults; keep layout spacing on one documented app scale. Check alignment, 48 dp touch targets, dynamic text, light and dark themes, sheet insets, and state feedback on a device.
- For visual changes, inspect the affected screen and its neighboring flows on a device after a successful build. Record specific before/after findings instead of treating use of Material components alone as design validation.
