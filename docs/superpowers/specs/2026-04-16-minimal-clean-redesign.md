# Kira Tracker: Minimal Clean Redesign

**Date:** 2026-04-16
**Status:** Approved
**Scope:** Frontend UI redesign with theme toggle

## Overview

Add a "Minimal Clean" design theme alongside the existing "Classic" gold-themed design. Users can switch between themes via a Settings modal. All existing functionality remains unchanged.

## Goals

1. Implement Minimal Clean design (based on mockup-final.html)
2. Add Settings modal with theme toggle
3. Preserve all 16 existing features
4. Support Dark Mode for both themes

## Non-Goals

- Backend changes
- New features beyond theme switching
- Removing the Classic design

## Architecture

### CSS Class System

```
body                    → Base styles
body.classic            → Gold/warm theme (default)
body.minimal            → Clean/minimal theme
body.dark               → Dark mode modifier (works with both)
```

### Theme Matrix

| Theme    | Light              | Dark                |
|----------|-------------------|---------------------|
| Classic  | Gold gradients    | Gold on dark bg     |
| Minimal  | Black on white    | White on dark bg    |

### CSS Variables

**Minimal Light:**
```css
body.minimal {
    --bg: #FAFAF8;
    --card: #FFFFFF;
    --border: #E8E6E1;
    --text: #1A1A1A;
    --text-secondary: #6B6B6B;
    --accent: #1A1A1A;
    --accent-light: #F5F5F3;
    --success: #2D8A4E;
    --success-light: #E8F5ED;
    --error: #C43A31;
    --error-light: #FEF2F1;
}
```

**Minimal Dark:**
```css
body.minimal.dark {
    --bg: #0F0F0E;
    --card: #1A1A18;
    --border: #2A2A28;
    --text: #F5F5F3;
    --accent: #FFFFFF;
    --accent-light: #2A2A28;
}
```

## Components

### 1. Settings Modal

**Trigger:** New settings icon button in topbar (replaces direct dark mode toggle)

**Contents:**
- Theme selector: Classic / Minimal (pill toggle)
- Dark Mode: On / Off (toggle switch)
- Language: DE / RU (pill toggle)

**Behavior:**
- Opens as bottom sheet modal (like existing modals)
- Changes apply immediately
- Settings persisted to localStorage

### 2. Topbar Changes

**Classic:**
- Logo: "🐕 Kira" with gold gradient

**Minimal:**
- Logo: "Kira" in black (light) or white (dark)
- Cleaner icon buttons without gold styling

### 3. Hero Section

**Classic:**
- Large avatar circle with gold gradient
- "Kira Tracker" title with gradient
- Subtitle below

**Minimal:**
- No avatar
- Time-based greeting: "Guten Morgen/Tag/Abend"
- Subtitle: contextual message about Kira

### 4. Status Cards

**Classic:**
- Emoji icons (🌅 🌇)
- Gradient backgrounds when done/open
- Gold-tinted success/error states

**Minimal:**
- Clean labels with colored left border
- Solid background colors for states
- Typography-focused, less decorative

### 5. Input Cards

**Classic:**
- Gold gradient submit button
- Pill-shaped quick-select buttons with gold tint

**Minimal:**
- Black submit button
- Subtle gray quick-select buttons
- Focus state with black border

### 6. Action Cards (Notify, Gamble, Stats)

**Classic:**
- Gold glassmorphism background
- Gradient text

**Minimal:**
- White card with subtle border
- Black text, gray secondary text

### 7. Modals

**Both themes:**
- Same structure and animations
- Colors follow theme variables

## Persistence

```javascript
// Theme: 'classic' | 'minimal'
localStorage.getItem('theme')
localStorage.setItem('theme', value)

// Dark: '0' | '1'
localStorage.getItem('dark')

// Language: 'de' | 'ru'
localStorage.getItem('lang')
```

## Implementation Steps

1. **Add Minimal CSS variables** - New `:root` and `.minimal` variable sets
2. **Create Settings modal** - HTML structure and JS functions
3. **Update topbar** - Replace dark toggle with settings button
4. **Add `.minimal` style overrides** - Component-by-component styling
5. **Add theme toggle logic** - JS functions for switching
6. **Test all combinations** - Classic/Minimal × Light/Dark × DE/RU
7. **Remove mockup files** - Clean up temporary files

## Files Modified

- `src/main/resources/static/index.html` - All changes in single file

## Testing Checklist

- [ ] Theme persists across page reload
- [ ] Dark mode works with both themes
- [ ] Language switch works with both themes
- [ ] All 16 features function correctly in Minimal theme
- [ ] Pause/Sleep banners display correctly
- [ ] Modals styled correctly in both themes
- [ ] Mobile responsive in both themes
- [ ] Confetti animation works
- [ ] Admin mode edit/delete buttons visible

## Features to Preserve

1. Dark/Light mode toggle
2. DE/RU language toggle
3. Pause banner system (4 vacation modes)
4. Sleep banner (22:00-06:00)
5. Morning/Evening walk status
6. Walk entry with quick-select
7. Food tracking
8. Leaderboard with time range
9. Walk history with admin edit/delete
10. Push notification system
11. Gamble (random person)
12. Stats page link
13. Confetti on first walk
14. Weather display
15. Streak counter
16. Admin mode with auth
