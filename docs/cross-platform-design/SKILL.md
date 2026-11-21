---
name: cross-platform-design
description: >-
  Design and build coherent, accessible interfaces for web, mobile, tablet, and
  desktop. Use when creating or redesigning responsive pages, app screens,
  component systems, prototypes, or interaction patterns in React/Next.js or
  other UI stacks. Combines design-system, responsive-layout, motion, accessibility,
  and browser-validation practices inspired by Penpot, shadcn/ui, Tailwind,
  Radix, Lucide, Motion, Magic UI, Storybook, axe, and Playwright.
---

# Cross-platform design

Create one product experience that adapts to the user's available space and input method. Preserve the same content, identity, and interaction meaning across form factors; change layout and controls only when the context calls for it.

## 1. Read the project and the brief

- Inspect the current routes, components, styles, tokens, and installed dependencies before proposing a stack. Keep existing conventions unless they block the brief.
- Identify audience, primary task, content hierarchy, brand references, target platforms, supported browsers, and constraints. Infer reasonable details from context; ask only when a missing choice materially changes the design.
- For an existing product, preserve its recognizable visual language and working behavior. For a new product, write a concise design direction before implementation: mood, type, color roles, density, imagery, and motion.
- Treat Penpot or other design files as a source of shared components, styles, and design tokens when supplied. Keep the design and implementation aligned; do not invent a parallel system.

## 2. Define the shared system

- Establish semantic tokens for color, typography, spacing, radii, elevation, borders, focus, and motion. Prefer roles such as `surface`, `text-muted`, `danger`, and `focus-ring` over scattered literal values.
- Set a small, consistent spacing and type scale. Use fluid sizing only where it improves continuity; cap line lengths and content widths for readability.
- Choose components for consistent behavior and semantics, not novelty. Reuse the project's component library. If starting fresh, consider shadcn/ui for editable local components, Radix primitives for accessible unstyled behavior, Tailwind for utility styling, and Lucide for consistent interface icons.
- Use real, licensed product imagery and meaningful icons. Do not use icons as decoration where text is clearer. Keep component APIs composable and avoid duplicating variants.
- Build a restrained, reusable set of patterns: buttons, fields, navigation, dialogs/sheets, menus, feedback, loading, empty, and error states.

## 3. Design from narrow to wide

Start with the smallest supported viewport and progressively add room. Breakpoints should follow content pressure, not named devices. Check at least a narrow phone (~360–390 CSS px), a large phone, a tablet in portrait and landscape, a compact laptop, and a wide desktop. Also check zoom and long content.

| Context | Layout guidance |
|---|---|
| Phone | One clear column, concise navigation, touch-sized actions, fields that fit, and sticky actions only when they remain unobstructive. Use safe-area insets for edge-to-edge layouts. |
| Tablet | Reflow into two panes only when both remain usable; use available width without stretching text or controls. Support portrait and landscape. |
| Desktop | Add navigation rails, side-by-side content, and denser tables when useful. Set a readable maximum content width and keep primary actions easy to find. |
| Large desktop | Expand information or workspace utility rather than magnifying every element. Preserve hierarchy and line length. |

- Prefer CSS Grid/Flexbox, intrinsic sizing, `minmax()`, wrapping, and content-aware breakpoints over fixed device dimensions.
- Avoid horizontal page overflow. Wide data may use a labeled, keyboard-accessible scroll region or a deliberate alternate presentation on narrow screens.
- Use `dvh`/`svh` thoughtfully for mobile viewport behavior. Do not disable browser zoom. Ensure focused controls remain visible above the on-screen keyboard.
- Support touch, mouse, keyboard, pen, and assistive technology. Use capability queries for hover and fine-pointer affordances; provide active feedback for touch.
- Keep core information and actions available across form factors. Do not hide essential actions solely because the viewport is narrow.

## 4. Build interactions with sound primitives

- Use semantic HTML first. Add ARIA only where native semantics are insufficient.
- For dialogs, popovers, menus, tabs, and disclosure patterns, use accessible primitives such as Radix when they fit the existing stack. Preserve focus entry, containment where appropriate, dismissal, and return behavior.
- Use Lucide icons consistently, with accessible names for icon-only controls. Decorative icons should be hidden from assistive technology.
- Use Motion for purposeful transitions that clarify state or spatial relationships. Keep durations short, avoid animating layout unnecessarily, and respect `prefers-reduced-motion`. Motion must not delay input or hide status changes.
- Treat Magic UI effects as optional visual patterns. Use them sparingly and keep content readable, contrast intact, and interaction available without animation.
- Handle loading, success, validation, empty, disabled, and failure states. Preserve user-entered data on recoverable errors.

## 5. Accessibility and resilience

- Target WCAG 2.2 AA. Check text and non-text contrast, visible focus, logical keyboard order, labels, headings, landmarks, error association, and status announcements.
- Provide adequately sized and spaced touch targets. Do not rely on color, hover, motion, or an icon alone to communicate meaning.
- Verify reflow at 320 CSS px and text enlargement; avoid clipping, fixed-height text regions, and overlapping sticky elements.
- Make focus visible and unobscured, including inside dialogs and beneath sticky headers. Ensure validation messages are specific and programmatically associated.
- Consider long names, translated strings, empty datasets, slow networks, failed images, and reduced motion as normal states.

## 6. Review in Storybook and real browsers

- If Storybook exists, document meaningful variants and states, and use its viewport controls to review responsive behavior. Do not add Storybook solely to satisfy this skill.
- Use axe or an equivalent automated accessibility scan as a useful detector, not a substitute for keyboard and screen-reader review. Resolve meaningful violations rather than suppressing them without explanation.
- Use Playwright or the project's browser automation to inspect representative flows and viewports when available. Check layout, navigation, forms, dialogs, focus, and responsive overflow.
- Review actual browsers and, for mobile-specific behavior, a real touch device when available. Emulation alone cannot confirm safe areas, keyboard interactions, browser chrome, or touch feel.
- Before calling the work complete, inspect the result at narrow, medium, and wide widths; test keyboard and touch paths; check reduced motion; and review loading, empty, and error states.

## Tool and repository roles

These projects inform this workflow; they are not all runtime dependencies and should not all be installed into the application:

- **Penpot**: open-source visual design and prototyping; source of design specs and assets.
- **shadcn/ui**: copyable React UI components and patterns.
- **Tailwind CSS**: utility-first styling framework.
- **Radix UI Primitives**: accessible, unstyled interaction primitives.
- **Lucide**: consistent interface icon set.
- **Motion** (`motiondivision/motion`): animation for React and the web.
- **Magic UI**: optional animated UI examples and components.
- **Storybook**: isolated component development and documentation.
- **axe-core**: automated accessibility rules engine.
- **Playwright**: browser automation and cross-browser checks.
- **Nextlevelbuild**: use the exact repository or package URL supplied by the user before treating this truncated link as an identifiable source; do not guess its project identity.

Install a library only when a concrete implementation needs it and the project owner wants it. Prefer official installation instructions and supported framework versions. Some sources are full applications or developer tools rather than packages. Never clone or vendor all repositories into the app as a substitute for selecting dependencies.

## Deliverable

For design work, provide the working interface or implementation plus a short summary of the responsive behavior, component and token choices, accessibility considerations, and the viewports or interaction paths reviewed. Call out any checks that could not be performed in the available environment.
