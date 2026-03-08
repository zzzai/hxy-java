# MiniApp Motion, Accessibility & Performance Spec v1 (2026-03-08)

## 1. Scope
Covers homepage enhanced interactions (sticky, parallax, ripple) and profile/marketing animation modules.

## 2. Motion Degrade
- Respect reduced-motion setting.
- Low-end device profile disables heavy parallax/ripple.
- Keep one primary CTA visually dominant.

## 3. Accessibility
- Minimum contrast and readable font sizes.
- Focus order stable for interactive elements.
- Error and warning states must be text-visible, not color-only.

## 4. Performance Budget
- First meaningful content under target budget set by client team.
- Animation frame drops must trigger degrade mode.
- Non-critical widgets load after core transaction modules.

## 5. Test Matrix
- Standard device
- Low-end device
- Weak network
- reduced-motion enabled
