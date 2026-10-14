# IrishSearch shamrock icon

The owner requested a Notre Dame-specific shamrock direction on October 14, 2026. This original three-leaf mark combines a shamrock with a search lens. It does not use the university monogram.

- `shamrock-concept.png`: initial concept generated with the built-in image generation tool.
- `shamrock-icon.svg`: flat vector translation for brand use.
- `shamrock-preview.svg` / `.png`: circle, squircle, monochrome and small-size review sheet.
- Production Android resources: `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, and the legacy `ic_launcher.xml` fallback under `app/src/main/res/drawable`; colors in `launcher_icon_colors.xml`.

The mark is intentionally bold, with generous adaptive-icon safe-zone clearance. The monochrome version uses a cutout lens and preserves the shamrock silhouette. The app UI retains its existing Material 3 palette; this green accent belongs to the launcher mark.

Generation prompt: “One refined Android launcher icon for IrishSearch, an independent Notre Dame course search and semester planner. A distinctive original three-leaf shamrock integrated elegantly with a small magnifying-glass search motif. Prioritize a bold instantly readable shamrock silhouette; lens subtly integrated into lower-right leaf/stem. Flat vector-like geometric design, optical balance, substantial shapes readable at 24dp. Deep ink navy #193449 full square background, pale cool green shamrock with restrained darker green accent, pale near-white lens. No gradients, shadows, texture, lettering, book, border, official university monogram, or copied institutional logo. Centered mark in the central 62% with empty outer area for adaptive clipping. One square icon, not a mockup.”

The generated concept was manually translated into native vector paths for clean rendering and monochrome support. The vector paths, rather than the raster concept, are the shipped art.
