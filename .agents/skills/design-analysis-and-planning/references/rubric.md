# Multi-Dimensional Design Flaw Diagnostic Rubric

This rubric provides a rigorous, systematic framework for auditing any UI object, page, view, component, or interaction flow. When analyzing an intended design or existing code, evaluate every dimension below and record concrete flaws, severity levels, and root causes.

---

## 1. UX & Cognitive Ergonomics

Assess how intuitive, frictionless, and clear the user's mental model and task completion are.

| Diagnostic Area | What to Inspect | Common Flaws |
| :--- | :--- | :--- |
| **Mental Model Alignment** | Does the interface mirror the user's real-world expectations and vocabulary? | System-centric jargon, confusing metaphors, disconnected terminology. |
| **Information Chunking** | Are complex data structures grouped logically (Miller's Law / 7±2 rule)? | Wall of unstructured fields, dense data tables without visual groupings. |
| **Decision Friction** | How many choices is the user presented with at once (Hick's Law)? | Decision paralysis, competing primary actions, lack of default selections. |
| **Action Affordances** | Can the user tell what is clickable, draggable, editable, or read-only? | Flat text acting as buttons, buttons looking like static labels, hidden hover-only controls. |
| **Fitts's Law & Reachability** | Are primary actions placed within natural thumb/mouse reach zones? | Destructive buttons immediately adjacent to primary actions; tiny click zones. |
| **Feedback & State Clarity** | Does the system acknowledge user inputs within 100ms? | Lack of optimistic updates, silent button clicks, missing progress feedback. |

---

## 2. Visual Design & Aesthetic Hierarchy

Assess visual structure, typographic cadence, balance, and polish.

| Diagnostic Area | What to Inspect | Common Flaws |
| :--- | :--- | :--- |
| **Typographic Hierarchy** | Is there a clear, distinct scale for titles, headers, body, and captions? | Similar font sizes for h2 and body; inconsistent line heights causing collisions. |
| **Spacing & Spatial Rhythm** | Does the layout follow a consistent spacing grid (4px / 8px)? | Arbitrary margins (e.g. `margin-top: 13px`), inconsistent gutters, crowded cards. |
| **Visual Anchoring** | Does the eye naturally track from the most important element to secondary details? | Competing focal points; eye jumps randomly across disparate cards without clear priority. |
| **Color System & Contrast** | Are colors semantically consistent (e.g., red only for danger/loss)? | Using 6 different shades of gray; warning colors used for decorative highlights. |
| **Surface Depth & Elevation** | Do modals, popovers, and cards use elevation/shadows appropriately? | Flat overlapping layers with no separation; harsh borders or excessive nested card borders. |
| **Micro-Interactions** | Are state transitions smooth, intentional, and under 200–300ms? | Jarring instant pops; overly slow animations (e.g. >500ms) delaying task execution. |

---

## 3. Accessibility (a11y) & Inclusivity (WCAG 2.2 AA)

Ensure universal usability across diverse abilities, devices, and assistive technologies.

| Diagnostic Area | Standard / Metric | Common Flaws |
| :--- | :--- | :--- |
| **Text Color Contrast** | Normal text: $\ge 4.5:1$<br>Large text ($\ge 18\text{pt}$ or $14\text{pt}$ bold): $\ge 3.0:1$ | Low-contrast gray text on off-white backgrounds; subtle placeholder text. |
| **Non-Text Contrast** | UI components, icons, borders, active states: $\ge 3.0:1$ | Borderless input fields invisible against card surfaces; unselected tab indicators. |
| **Touch & Click Targets** | Mobile: $\ge 44 \times 44\text{ pt}$ / $48 \times 48\text{ dp}$<br>Desktop: $\ge 24 \times 24\text{ px}$ with spacing | Icon buttons with tiny hitboxes (e.g. $16 \times 16\text{ px}$); tap target overlaps. |
| **Keyboard Navigability** | Full interactive control via `Tab`, `Shift+Tab`, `Enter`, `Space`, `Esc`, arrows. | Unfocusable custom divs; focus traps where the user cannot escape a modal. |
| **Focus Rings** | Clearly distinguishable visual indicator on focused interactive items. | `outline: none` without replacement `:focus-visible` ring. |
| **Screen Reader Semantics** | Semantic HTML5 tags (`<button>`, `<main>`, `<nav>`), ARIA roles, labels, live regions. | `<div>` used as button without `role="button"` or `aria-label`; icon buttons missing labels. |
| **Motion Sensitivity** | Respect user system setting for reduced motion. | Parallax or persistent looping animations lacking `@media (prefers-reduced-motion)`. |

---

## 4. Architecture, State & Component Lifecycle

Assess code maintainability, reusability, performance, and separation of concerns.

| Diagnostic Area | What to Inspect | Common Flaws |
| :--- | :--- | :--- |
| **Component Cohesion** | Does the component have a single, well-defined responsibility (SRP)? | "God components" containing UI layout, network requests, state machines, and business rules. |
| **Prop Interface Ergonomics** | Is the prop API clean, typed, predictable, and composable? | Passing 25+ loose props; prop drilling down 4 component layers instead of slots/children. |
| **Coupling & Reusability** | Can this component be rendered in isolation (e.g. Storybook / preview)? | Hardcoded global store references or parent-specific CSS selectors inside a reusable widget. |
| **Re-render Performance** | Are re-renders isolated to subtrees that actually consume updated data? | Parent re-renders cause entire list of 500 items to recalculate; un-memoized callbacks. |
| **State Organization** | Is state located as close to consumers as possible? | Global state pollution for transient local inputs; conflicting duplicate sources of truth. |

---

## 5. Edge Cases & Resilience

Assess how the component or page behaves under extreme conditions, errors, and device constraints.

| Diagnostic Area | What to Inspect | Common Flaws |
| :--- | :--- | :--- |
| **Empty States** | What is displayed when data is zero, missing, or first-time setup? | Blank white box; unhandled `undefined.map()` crash; missing onboarding call-to-action. |
| **Extreme Data Density** | What happens with 10,000 items vs. 1 item? | Browser freeze from un-virtualized lists; pagination controls hidden below fold. |
| **Text Overflow & I18n** | How does the layout handle long strings, German/Germanic translations, or RTL? | Truncated buttons with illegible "...", text overflowing card bounds, broken flex wraps. |
| **Async Lifecycle** | Are skeleton loaders, loading spinners, and error retries explicitly handled? | Content layout shift (CLS) when images/data pop in; unhandled network timeout leaving infinite spinner. |
| **Responsive Breakpoints** | How does the view adapt between mobile (320px), tablet (768px), and ultra-wide (2560px)? | Horizontal scroll on mobile; gigantic stretched forms on wide desktop displays. |
| **Permissions & Roles** | How does the view adapt when the user lacks edit or view permissions? | Displaying non-functional buttons that return 403 on click, rather than disabled or hidden states. |

---

## Flaw Severity Grading Scale

When reporting flaws, assign one of four severity levels:

- **🔴 CRITICAL (P0)**: Blocks user journey, causes data loss, breaks basic accessibility (e.g., keyboard trap, missing form submit button, unhandled exception crash).
- **🟠 HIGH (P1)**: Major usability friction, severe accessibility violation (contrast failure on primary text), significant layout breakage on common screen sizes.
- **🟡 MEDIUM (P2)**: Suboptimal visual hierarchy, inconsistent spacing, minor prop-drilling or performance inefficiency, unhandled non-critical edge cases (e.g. ultra-long text truncation).
- **🟢 LOW (P3)**: Cosmetic polish, micro-interaction refinements, minor naming or code structure suggestions.
