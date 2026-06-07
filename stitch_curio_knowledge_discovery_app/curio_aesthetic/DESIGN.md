---
name: Curio Aesthetic
colors:
  surface: '#0b1514'
  surface-dim: '#0b1514'
  surface-bright: '#313b3a'
  surface-container-lowest: '#06100f'
  surface-container-low: '#131d1c'
  surface-container: '#172120'
  surface-container-high: '#222c2b'
  surface-container-highest: '#2c3736'
  on-surface: '#dae5e3'
  on-surface-variant: '#c0c8c6'
  inverse-surface: '#dae5e3'
  inverse-on-surface: '#283231'
  outline: '#8b9290'
  outline-variant: '#414847'
  surface-tint: '#a8cec8'
  primary: '#a8cec8'
  on-primary: '#103632'
  primary-container: '#062e2a'
  on-primary-container: '#729791'
  inverse-primary: '#416560'
  secondary: '#e6feff'
  on-secondary: '#003739'
  secondary-container: '#00f4fe'
  on-secondary-container: '#006c71'
  tertiary: '#e9c400'
  on-tertiary: '#3a3000'
  tertiary-container: '#c9a900'
  on-tertiary-container: '#4c3f00'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#c3eae4'
  primary-fixed-dim: '#a8cec8'
  on-primary-fixed: '#00201d'
  on-primary-fixed-variant: '#294d48'
  secondary-fixed: '#63f7ff'
  secondary-fixed-dim: '#00dce5'
  on-secondary-fixed: '#002021'
  on-secondary-fixed-variant: '#004f53'
  tertiary-fixed: '#ffe16d'
  tertiary-fixed-dim: '#e9c400'
  on-tertiary-fixed: '#221b00'
  on-tertiary-fixed-variant: '#544600'
  background: '#0b1514'
  on-background: '#dae5e3'
  surface-variant: '#2c3736'
typography:
  display-lg:
    fontFamily: Hanken Grotesk
    fontSize: 56px
    fontWeight: '800'
    lineHeight: '1.1'
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Hanken Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: '1.2'
  headline-lg-mobile:
    fontFamily: Hanken Grotesk
    fontSize: 28px
    fontWeight: '700'
    lineHeight: '1.2'
  headline-md:
    fontFamily: Hanken Grotesk
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.3'
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.6'
  label-md:
    fontFamily: Hanken Grotesk
    fontSize: 14px
    fontWeight: '600'
    lineHeight: '1'
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Hanken Grotesk
    fontSize: 12px
    fontWeight: '500'
    lineHeight: '1'
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  xs: 0.25rem
  sm: 0.5rem
  md: 1rem
  lg: 1.5rem
  xl: 2.5rem
  xxl: 4rem
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 48px
---

## Brand & Style

This design system is built for a premium, high-energy digital experience that balances discovery with curation. The brand personality is **sophisticated yet electric**, designed to evoke a sense of modern prestige and intellectual curiosity. It caters to a tech-forward audience that values both depth and dynamism.

The visual style is a fusion of **Modern Dark Mode** and **Glassmorphism**, utilizing deep jewel-toned backgrounds contrasted against vibrant, luminous accents. It avoids the clinical feel of standard dark interfaces by using rich saturation and translucent layering, creating a tactile "physicality" to digital objects. The emotional response should be one of "exclusive energy"—professional enough for high-stakes environments but vibrant enough for creative exploration.

## Colors

The palette is anchored by **Deep Emerald**, providing a foundation that is more organic and sophisticated than standard blacks or grays. 

- **Primary (Deep Emerald):** Used for large surfaces and background foundations. It provides depth without sacrificing color.
- **Secondary (Bright Cyan):** The primary action color. It should be used for high-visibility triggers, progress indicators, and primary buttons.
- **Tertiary (Gold):** Reserved for "premium" moments—achievements, curation highlights, or decorative flourishes that signal value.
- **Neutral:** A range of desaturated teals and slates to maintain the cool, dark atmosphere without appearing muddy.

All interactive elements must maintain high contrast ratios against the deep emerald base to ensure the energy of the UI is felt through legibility and "glow" effects.

## Typography

This design system utilizes **Hanken Grotesk** exclusively to maintain a sharp, contemporary edge. The type scale is aggressive, favoring large display sizes for impact and smaller, highly legible labels for utility.

- **Headlines:** Use Bold and ExtraBold weights to create a strong visual hierarchy. Letter spacing is slightly tightened for display sizes to give a compact, editorial feel.
- **Body:** Regular weight is used for maximum readability. The line height is generous (1.6) to prevent fatigue on dark backgrounds.
- **Labels:** Always use Medium or SemiBold weights. Small labels and metadata should utilize uppercase styling with increased letter spacing to differentiate from body text.

## Layout & Spacing

The layout philosophy follows a **Fluid-Fixed Hybrid** model. While the overall container can stretch to accommodate ultra-wide screens, the internal content is governed by a 12-column grid to maintain focus.

- **Vertical Rhythm:** Built on an 8px base unit. All margins and paddings must be multiples of 8px.
- **Desktop:** 12 columns, 24px gutters, and 48px side margins. 
- **Mobile:** 4 columns, 16px gutters, and 16px side margins.
- **Reflow:** On smaller viewports, cards and grid items should stack vertically, while navigation collapses into a bottom-sheet or a side-drawer to prioritize thumb-reach.

## Elevation & Depth

In this dark-themed design system, depth is conveyed through **chromatic layering and luminescence** rather than traditional gray-scale shadows.

- **Tonal Layers:** Surfaces "rise" toward the user by becoming lighter and more saturated. Background is the darkest (#041412), while primary containers are slightly lighter (#0B221F).
- **Glassmorphism:** Use backdrop-blur (20px to 40px) on top-level navigation bars and modal overlays. This creates a "frosted emerald" effect that keeps the UI feeling airy.
- **Luminous Borders:** Instead of shadows, high-elevation elements (like active cards) use a 1px inner stroke of Cyan or a low-opacity white to define edges. 
- **Glow:** Primary action buttons use a soft, 15% opacity drop-shadow of the Secondary color (#00F5FF) to simulate a light source emitting from the button itself.

## Shapes

The shape language is **distinctly rounded**, reflecting a modern and approachable luxury. 

- **Containers:** Standard cards and containers use `rounded-lg` (1rem). 
- **Interactive Elements:** Buttons and input fields use `rounded-md` (0.5rem).
- **Specialty Elements:** User avatars and featured tags utilize the "Pill" shape for maximum contrast against the rectangular grid.
- **Exceptions:** Very large sections (like hero images) can use `rounded-xl` (1.5rem) to emphasize the soft, premium nature of the layout.

## Components

### Buttons
- **Primary:** Solid Cyan (#00F5FF) with black text. High-energy, impossible to miss.
- **Secondary:** Transparent background with a 1px Gold (#FFD700) or Cyan border. Text color matches the border.
- **Ghost:** No background or border. Used for tertiary actions to keep the UI clean.

### Inputs
Fields should have a dark, semi-transparent background (5% white over Deep Emerald) with a subtle bottom-border. On focus, the border should animate to a full Cyan stroke with a soft outer glow.

### Cards
Cards are the primary container. They should use a subtle gradient from top-left (#0B221F) to bottom-right (#062E2A) to add a sense of curvature. Hover states should include a 1px Cyan border-glow.

### Chips & Tags
Used for categorization. These should be pill-shaped with low-opacity background fills of the accent colors (e.g., 10% Cyan fill with 100% Cyan text) to keep them legible but secondary to main actions.

### Progress & Loading
Loading states should utilize "Shimmer" effects that move from Deep Emerald to a slightly lighter teal, mimicking the flow of light through a gemstone.