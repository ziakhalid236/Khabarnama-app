# Khabarnama News - Editorial Redesign & Selective Content Translation

A comprehensive architectural and design plan to refine Khabarnama News into an elite, editorial-grade news reading application with locked English UI scaffolding, on-the-fly article translation, a unified day-and-night theme, and a streamlined interaction model.

## User Review & Critical Decisions

> [!IMPORTANT]
> Based on your direct answers, the application is being upgraded with the following confirmed specifications:

- **Confirmed Decision 1: English-Locked App Shell**: The entire application UI (top app bar, category chips, navigation, prayer times card, dialog buttons, and system controls) remains permanently in English with a consistent Left-to-Right (LTR) layout. Changing the language will **only** translate the news headlines and article body summaries.
- **Confirmed Decision 2: News-Only Multilingual Translation**: When the user selects a language (English, Urdu, Arabic, Turkish, Hindi), only the news cards and detail articles render in that language. Urdu and Arabic news texts render with proper script fonts (Noto Naskh Arabic) inside the card while the card's framing and metadata remain in clean English.
- **Confirmed Decision 3: Editorial Slate Unified Theme**: The dual light/dark toggle is replaced with a single, meticulously tuned "Editorial Slate" theme with pearl-slate backgrounds (`#F4F6F8`), pure white cards (`#FFFFFF`), crisp micro-borders (`#E2E8F0`), deep obsidian typography (`#0F172A`), and vivid emerald teal accents (`#00897B`). This palette is balanced for comfortable reading in both bright daylight and low-light environments.
- **Confirmed Decision 4: Complete Removal of Save/Bookmark Buttons**: All bookmark and save buttons have been eliminated from the card header, card footer, featured carousel, and article detail views to remove interface clutter.
- **Confirmed Decision 5: Text Size Dialog Removal**: The font size selector modal and app bar button are removed to keep the top bar clean and focused.
- **Confirmed Decision 6: Top Bar Language Switcher**: A sleek pill button in the top app bar displaying the current language code (e.g. `🌐 EN`, `🌐 UR`, `🌐 AR`) opens a quick-selection modal dialog.

---

## 1. Overview & Core Concept

- **What It Does**: Khabarnama News delivers a curated, Facebook-style vertical news stream featuring verified global news, Pakistan updates, and highlighted Islamic affairs. The application maintains an English navigation framework while offering one-tap translation of news stories into 5 languages.
- **Target Audience**: Modern bilingual and multilingual readers who want international news and regional/Islamic highlights with journalistic crediting, prayer times, and zero distractions.
- **Key Value**: Clean editorial aesthetics without visual fatigue, verified publisher attribution, instant content translation without breaking the app's layout, and zero save clutter.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Browsing the Editorial Feed**:
   - The user opens the app to an Editorial Slate header with an emerald news crest, current Hijri calendar date, and next prayer time countdown.
   - Sticky category chips (`All`, `Islamic`, `Pakistan`, `World`, `Business`, `Sports`, `Technology`) filter stories instantly with pull-to-refresh and infinite scrolling.
   - When the user selects `Islamic`, an emerald sub-chip bar unfolds with Muslim World, Hajj & Umrah, Islamic Events, Fatwa & Scholars, and Ramadan & Eid.

2. **Switching News Content Language**:
   - The user taps the `🌐 Language` pill in the top app bar.
   - A clean modal presents: **English**, **اردو (Urdu)**, **العربية (Arabic)**, **Türkçe (Turkish)**, and **हिन्दी (Hindi)**.
   - Selecting a language immediately updates all article headlines and summaries. The app bar, category chips, and UI buttons remain in crisp English.

3. **Reading News & Verifying Sources**:
   - Each news card features a 16:9 high-resolution cover image, verified publisher badge (`Arab News ✓`, `Reuters ✓`, `BBC News ✓`), publication time, headline, and concise 3–4 line summary.
   - Tapping the `📷 Source: [Publisher]` badge opens the *Image Source* bottom sheet containing the original article link, browser launcher, and copyright attribution.
   - Readers can hit `Like` (with animated counter) or `Share` (system share sheet).

### Visual Identity & Theme Tokens

- **Aesthetic**: Modern Editorial Slate with crisp borders and deep emerald accents.
- **Color Palette**:
  - `Background`: `#F4F6F8` (Refined neutral slate)
  - `Surface / Card`: `#FFFFFF` (Pure crisp white)
  - `Card Border`: `#E2E8F0` (1dp crisp border for tactile elevation)
  - `Primary / Accent`: `#00796B` & `#00897B` (Rich Emerald Teal)
  - `Islamic Accent`: `#0F8A5F` (Vibrant Islamic Green) with `#E8F5E9` container
  - `Text Primary`: `#0F172A` (Obsidian slate for high readability)
  - `Text Secondary`: `#475569` (Muted slate for time and subtext)
- **Typography**:
  - English UI & Headlines: **Plus Jakarta Sans** (clean, contemporary, geometric).
  - Urdu/Arabic Story Content: **Noto Naskh Arabic** (bundled locally for authentic diacritics and ligatures).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Selective News Translation vs Full-App RTL Switch**:
  - *Chosen Approach*: Only translate news article titles and summaries into the user's selected language, keeping all UI chrome, category chips, and prayer tools locked in English LTR.
  - *Why*: Prevents jarring whole-app layout flips, avoids awkward RTL inversion of system bars, and delivers exactly what bilingual readers want—reading the news content in their chosen tongue while enjoying a consistent English app shell.
- **Decision 2: Unified Editorial Slate Theme vs Dark/Light Toggle**:
  - *Chosen Approach*: Single, high-contrast, low-glare editorial theme with neutral slate backgrounds, white cards, and crisp slate borders.
  - *Why*: Removes toggle clutter, ensures consistent brand polish across all screen types, and provides optimal contrast in all lighting conditions.
- **Decision 3: Complete Removal of Bookmarks & Font Sizer**:
  - *Chosen Approach*: Stripping out local storage bookmarks, badges, and font resizing.
  - *Why*: Drastically simplifies the interface, eliminates secondary drawers and buttons, and aligns strictly with the user's explicit instructions.

---

## 4. Technical Architecture & Data Strategy

```
┌──────────────────────────────────────────────────────────────┐
│                    Top Navigation Bar                        │
│   [Logo: Khabarnama]  [Hijri Date]  [Prayer Time]  [🌐 EN]   │
└──────────────────────────────┬───────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────┐
│               Sticky Category Chips Bar                      │
│   [All] [Islamic (Emerald)] [Pakistan] [World] [Business]... │
└──────────────────────────────┬───────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────┐
│               Featured Islamic Carousel (Top)                │
│       16:9 Wide Cards • Emerald Overlay • Highlight Tag      │
└──────────────────────────────┬───────────────────────────────┘
                               │
┌──────────────────────────────▼───────────────────────────────┐
│              Vertical News Feed (LazyColumn)                 │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │  FacebookNewsCard                                        │ │
│ │  • Publisher Header (Avatar + Verified + Time)           │ │
│ │  • Translated Headline (Selected Language)               │ │
│ │  • Translated Summary (3-4 lines with See more)          │ │
│ │  • 16:9 Cover Image with Floating Source Pill            │ │
│ │  • Engagement Bar: Like (Counter) | Share | Source Link  │ │
│ └──────────────────────────────────────────────────────────┘ │
└──────────────────────────────┬───────────────────────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
 ┌──────────────────────┐              ┌──────────────────────┐
 │ LanguageSelectDialog │              │ ImageSourceSheet     │
 │  [EN] [UR] [AR]...   │              │  Original URL / Web  │
 └──────────────────────┘              └──────────────────────┘
```

### Component & State Mapping

- `NewsViewModel`:
  - `currentLanguage: StateFlow<AppLanguage>` (EN, UR, AR, TR, HI)
  - `selectedCategory: StateFlow<NewsCategory>`
  - `selectedIslamicSection: StateFlow<IslamicSection>`
  - `articles: StateFlow<List<NewsArticle>>` (queried with current language)
  - `activeImageSourceArticle: StateFlow<NewsArticle?>`
  - `isShowingLanguageDialog: StateFlow<Boolean>`
  - `isShowingPrayerDialog: StateFlow<Boolean>`
  - `isRefreshing: StateFlow<Boolean>`

- `Theme.kt`:
  - Locked to `EditorialSlateColorScheme` (no dynamic/dark toggles).
