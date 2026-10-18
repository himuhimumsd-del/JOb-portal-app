---
name: Executive Mobile Talent Ecosystem
colors:
  surface: '#faf8ff'
  surface-dim: '#d2d9f4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#eaedff'
  surface-container-high: '#e2e7ff'
  surface-container-highest: '#dae2fd'
  on-surface: '#131b2e'
  on-surface-variant: '#444653'
  inverse-surface: '#283044'
  inverse-on-surface: '#eef0ff'
  outline: '#757684'
  outline-variant: '#c4c5d5'
  surface-tint: '#3755c3'
  primary: '#00288e'
  on-primary: '#ffffff'
  primary-container: '#1e40af'
  on-primary-container: '#a8b8ff'
  inverse-primary: '#b8c4ff'
  secondary: '#0058be'
  on-secondary: '#ffffff'
  secondary-container: '#2170e4'
  on-secondary-container: '#fefcff'
  tertiary: '#003564'
  on-tertiary: '#ffffff'
  tertiary-container: '#004c8b'
  on-tertiary-container: '#8fbeff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#dde1ff'
  primary-fixed-dim: '#b8c4ff'
  on-primary-fixed: '#001453'
  on-primary-fixed-variant: '#173bab'
  secondary-fixed: '#d8e2ff'
  secondary-fixed-dim: '#adc6ff'
  on-secondary-fixed: '#001a42'
  on-secondary-fixed-variant: '#004395'
  tertiary-fixed: '#d4e3ff'
  tertiary-fixed-dim: '#a4c9ff'
  on-tertiary-fixed: '#001c39'
  on-tertiary-fixed-variant: '#004883'
  background: '#faf8ff'
  on-background: '#131b2e'
  surface-variant: '#dae2fd'
typography:
  display-hero:
    fontFamily: Plus Jakarta Sans
    fontSize: 36px
    fontWeight: '800'
    lineHeight: 44px
    letterSpacing: -0.03em
  display-hero-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '800'
    lineHeight: 36px
    letterSpacing: -0.025em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 28px
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 26px
    letterSpacing: -0.005em
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 22px
    letterSpacing: 0em
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 18px
    letterSpacing: 0em
  label-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-sm: 0.75rem
  margin: 1.25rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
---

## Brand & Style

This design system targets ambitious modern professionals and high-growth employers seeking an elevated, intuitive, and frictionless hiring interface. It balances institutional credibility with modern consumer-grade fluidity, rejecting the dense, cluttered aesthetic of legacy job boards in favor of calm, decisive discovery.

The design movement combines **Minimalism** with tactile **Layered Depth**:
- Clean canvas foundations with precise optical breathing room.
- Elevated pristine cards that feel touchable, organized, and structurally intentional.
- Tactile floating dock elements that ground mobile interactions within easy reach of the thumb zone.
- Electric, purposeful accents applied selectively through gradients and chromatic status signifiers to guide candidate progression without visual fatigue.

## Colors

The palette establishes an immediate sense of authority, optimism, and systematic clarity:

- **Canvas Foundation**: Soft neutral surfaces (`#F8FAFC` base background, `#F1F5F9` structural dividers and inset panels) that soften eye strain compared to harsh `#FFFFFF`.
- **Primary Surfaces**: Crisp `#FFFFFF` card surfaces to create natural elevation above the backdrop.
- **Brand Hero Gradient**: Anchored by `linear-gradient(135deg, #1E40AF 0%, #3B82F6 50%, #60A5FA 100%)` for hero headers, promotional talent banners, and key value propositions.
- **Text & Hierarchy**: Deep Slate `#0F172A` for primary headlines and job titles; Slate `#64748B` for meta tags, timestamps, and secondary captions; Light Muted Slate `#94A3B8` for disabled states and placeholders.
- **Status & Attribute Accents**:
  - Emerald (`#10B981` bg: `#ECFDF5`, text: `#065F46`): "Actively Hiring" and verified signals.
  - Indigo (`#6366F1` bg: `#EEF2FF`, text: `#3730A3`): "Full-time", "Remote", contract classifications.
  - Amber (`#F59E0B` bg: `#FFFBEB`, text: `#92400E`): "Applied", in-review states.
  - Violet (`#8B5CF6` bg: `#F5F3FF`, text: `#5B21B6`): "Shortlisted", high-priority notifications.
- **Floating Controls**: High-contrast `#090D16` carbon black for the bottom navigation container, paired with an illuminated soft-white active state.

## Typography

The typography leverages **Plus Jakarta Sans** across all structural tiers to deliver an inviting, geometric, yet rigorous reading experience tailored for mobile legibility.

- **Headlines & Metric Callouts**: Set with tight negative tracking (`-0.01em` to `-0.03em`) and heavy weights (`700` and `800`) to provide punchy, readable headers even at small viewport widths.
- **Body & Requirements**: Tuned with open line-height ratios (1.5x–1.6x) and balanced weights to support effortless skimming of dense job descriptions and salary disclosures.
- **Labels & Microcopy**: Compact sizes paired with semibold or bold weights and slight letter-spacing expansions (`0.01em` to `0.04em`) to maintain sharp clarity inside colored status badges, filter chips, and pill elements.

## Layout & Spacing

The layout is built around a mobile-first fluid grid structured with strict 4px and 8px baseline increments:

- **Grid Architecture**: 4-column fluid layout on mobile viewports with a default outer canvas margin of `16px` (`margin-mobile`), scaling up to `20px` (`margin`) on larger phablet/tablet viewports.
- **Vertical Rhythm**: Generous vertical spacing prevents visual crowding. Hero containers utilize `space-xl` internal padding; standard job feed cards use `space-lg` (`16px`) padding to maximize touch targets while maintaining compact density.
- **Safe Area Insets**: Bottom margins must allocate a fixed `96px` clearance buffer above the hardware home indicator to accommodate the floating navigation dock without content occlusion.

## Elevation & Depth

Visual hierarchy relies on soft, diffused ambient drop shadows rather than harsh borders or deep skeuomorphic bevels:

- **Level 0 (Canvas Base)**: Flat `#F8FAFC` background.
- **Level 1 (Resting Cards)**: `#FFFFFF` surface accompanied by `box-shadow: 0 4px 20px -2px rgba(15, 23, 42, 0.05)`. This creates a crisp, float-above separation without dirtying the light canvas.
- **Level 2 (Active / Hover / Tap Cards)**: Scaled slightly (`scale(1.01)`) with `box-shadow: 0 10px 25px -4px rgba(15, 23, 42, 0.08)`.
- **Level 3 (Modal Sheets & Floating Search)**: `box-shadow: 0 20px 32px -8px rgba(15, 23, 42, 0.12)`.
- **Level 4 (Floating Bottom Dock)**: Carbon dark `#090D16` container with `box-shadow: 0 12px 36px 0 rgba(15, 23, 42, 0.25)`. The active item inside features a glowing white pill with an ambient back-glow: `box-shadow: 0 0 16px rgba(255, 255, 255, 0.4)`.

## Shapes

The interface embraces a friendly yet modern curvature language:

- **Base Cards & Modals (`rounded-lg`, 1rem / 16px)**: Applied to job cards, company highlight panels, input wrappers, and filter bottom sheets.
- **Hero Banners & Inset Cards (`rounded-xl`, 1.5rem / 24px)**: Applied to blue gradient hero panels and profile spotlight modules.
- **Pills & Circular Triggers (`rounded-full`, 9999px)**: Reserved for status chips, category pills, avatar housings, icon button badges, and the floating navigation dock.

## Components

### Buttons
- **Primary Action**: Pill-shaped or `rounded-lg` button filled with `#1E40AF` or the dynamic blue gradient (`linear-gradient(135deg, #1E40AF, #3B82F6)`). Text is bold white (`#FFFFFF`). Touch target height is strictly 48px to 52px. Active press state triggers a scale reduction to `0.98`.
- **Secondary Action**: Off-white background (`#F1F5F9`) with primary text (`#0F172A`), no border, subtle touch depression.
- **Icon Buttons (Bookmark, Share)**: Circular (40px x 40px) `#FFFFFF` surface with `Level 1` shadow and `#64748B` slate iconography, transitioning to `#1E40AF` on active toggle.

### Status Chips & Badges
- Compact pill-shaped modules (`height: 26px`, padding `0 10px`).
- Uses high-legibility contrasting tones:
  - **Actively Hiring**: Emerald background `#ECFDF5`, text `#065F46`, paired with an optional 6px pulsing emerald status dot.
  - **Role Type (Full-time, Contract)**: Indigo background `#EEF2FF`, text `#3730A3`.
  - **Candidate Pipeline (Applied)**: Amber background `#FFFBEB`, text `#92400E`.
  - **Match Status (Shortlisted)**: Violet background `#F5F3FF`, text `#5B21B6`.

### Job Cards
- Built on `#FFFFFF` with `rounded-lg` (16px) corners and `Level 1` ambient shadow.
- Header row includes company logo avatar (44px `rounded-md`), company name, and location/remote badge.
- Body displays job title in `headline-sm` (`#0F172A`) followed by compensation range in semibold slate.
- Footer organizes multi-status chips (e.g., "Full-time", "Senior", "Actively Hiring") alongside timestamp microcopy.

### Floating Bottom Navigation Dock
- Centered floating pill suspended 20px above the screen bottom.
- Dimensions: fixed height of 64px, containing a sleek `#090D16` dark glass/carbon frame.
- **Active State**: Glowing pure white pill (`#FFFFFF`) with deep slate icon (`#0F172A`) and soft light diffusion.
- **Inactive States**: Crisp monochrome icons (`#94A3B8`) with zero border, activating instantly on tap.

### Input & Search Fields
- Inset container with `#F1F5F9` background or `#FFFFFF` surface with subtle `1px solid #E2E8F0` border.
- Height: 52px for primary search bars; leading slot dedicated to a 20px slate magnifying glass icon; trailing slot reserved for a filter badge button.

### Lists & Groupings
- Clear horizontal separation utilizing 12px vertical gaps between card entities rather than heavy horizontal line rules, maintaining visual lightness across long feeds.