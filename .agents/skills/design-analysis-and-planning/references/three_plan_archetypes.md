# The Three-Plan Formulation Archetypes

When proposing solutions to flaws identified in an object, page, view, or component, never propose three minor variations of the same idea. Instead, formulate **three genuinely distinct architectural and design paths** that represent distinct trade-offs between implementation effort, risk, velocity, and design ambition.

---

## Plan Archetype Summary

| Dimension | Plan A: Surgical Refinement | Plan B: Balanced Best-Practice | Plan C: Radical Reimagination |
| :--- | :--- | :--- | :--- |
| **Guiding Philosophy** | "Do no harm. Maximize ROI per line changed." | "Do it right. Build the production gold standard." | "Rethink from first principles. Unlock step-function gains." |
| **Core Value** | Zero regression risk, instant delivery. | High craft, maintainability, system alignment. | Innovation, differentiated UX, 10x scalability. |
| **API & Data Impact** | 100% backward-compatible; zero schema changes. | Clean typed props; localized state refactor. | Re-architected data contracts / headless composability. |
| **Accessibility** | Resolves P0/P1 contrast, labels, and focus. | Full WCAG 2.2 AA compliant with keyboard traps audited. | Fully accessible multimodal interaction model. |
| **Typical Effort** | Hours (1–2 days max). | Days (3–5 days). | Weeks (1–3 sprints). |
| **Risk Profile** | Near-zero. | Low to moderate (requires testing call sites). | High (requires design approval & phased rollout). |

---

## Detailed Plan Profiles

### Plan A: The Surgical / Conservative Refinement
**"The Low-Risk Quick Win"**

- **Intent**: Immediately resolve identified flaws without destabilizing existing code, breaking consumers, or requiring broad stakeholder approvals.
- **Architectural Scope**:
  - Keep the component interface, prop signatures, and DOM hierarchy mostly intact.
  - Fix visual hierarchy via CSS variables, typography adjustments, and spacing alignment.
  - Patch critical accessibility bugs (add missing `aria-label`, correct heading levels `h1`–`h3`, restore `:focus-visible` styling, adjust contrast).
  - Add missing fallback guards (e.g. `items?.length ? ... : <EmptyState />`, text truncation with title tooltips).
- **When to Choose**:
  - Code freeze or nearing release cutoff.
  - Stable legacy codebases where broad refactoring is cost-prohibitive.
  - Tight budget or immediate need to unblock users.

---

### Plan B: The Balanced / Modern Best-Practice
**"The Production Gold Standard"**

- **Intent**: Refactor the target to adhere to industry best practices, modern UI patterns, and design system tokens.
- **Architectural Scope**:
  - Deconstruct monolithic components into clean sub-components using the Compound Component pattern (e.g., `<Card>`, `<Card.Header>`, `<Card.Body>`, `<Card.Actions>`).
  - Introduce custom hooks or state machines (e.g., `useComponentState`) to separate business logic from rendering.
  - Implement fluid responsive design (container queries, CSS grid, mobile bottom sheets transitioning to desktop modals).
  - Build comprehensive lifecycle states: animated skeleton placeholders, optimistic updates, inline error boundaries with retry mechanisms.
  - Achieve full WCAG 2.2 AA compliance including keyboard navigation shortcuts, ARIA live regions for async changes, and touch-target padding.
- **When to Choose**:
  - Standard product feature iterations and sprint work.
  - Component is central to day-to-day user journeys.
  - The team wants to eliminate technical debt while elevating user delight.

---

### Plan C: The Radical / Next-Gen Reimagination
**"The Step-Function Leap"**

- **Intent**: Re-envision the user's ultimate job-to-be-done by discarding legacy assumptions and leveraging modern paradigm shifts.
- **Architectural Scope**:
  - **Interaction Paradigm Shift**: Replace static multi-step forms with an inline command-palette (`Cmd+K`), contextual floating drawers, conversational/AI-assisted inputs, or direct canvas manipulation.
  - **Headless & Composable Core**: Decouple logic into a headless primitive (or state machine) with pluggable visual renderers for mobile, web, and embedded widgets.
  - **Optimistic & Zero-Latency**: Real-time collaborative synchronization, instant optimistic execution with undo toasts, background persistence.
  - **Adaptive Personalization**: Context-aware UI that hides unnecessary fields based on user role, past behavior, or screen context.
- **When to Choose**:
  - Flagship product redesign or greenfield feature launch.
  - Core competitive differentiator where existing UX feels dated or clunky.
  - High strategic priority with dedicated design and engineering bandwidth.

---

## How to Formulate the Plans for Different Target Types

### 1. For a Component (e.g., Dropdown, Card, Modal, Datepicker)
- **Plan A**: Patch CSS styles, add ARIA attributes, fix click-away handler.
- **Plan B**: Refactor into accessible headless primitive (e.g. Radix/Ariakit style) with compound sub-components and keyboard arrow navigation.
- **Plan C**: Transform from a passive selector into an adaptive search-as-you-type fuzzy palette with recent history and smart suggestions.

### 2. For a View / Section (e.g., Patient Timeline, Checkout Summary, Filter Bar)
- **Plan A**: Reorganize existing controls into logical vertical groups; fix responsive wrap.
- **Plan B**: Convert to a sticky faceted filter panel with active tag badges and instant URL query-param synchronization.
- **Plan C**: Natural language search & filter bar with saved views, smart presets, and automatic anomaly highlights.

### 3. For a Full Page / Screen (e.g., Dashboard, Profile, Settings)
- **Plan A**: Standardize grid gutters, align typography, clean up card borders and empty states.
- **Plan B**: Modular widget dashboard with drag-and-drop or toggleable panels, tabbed navigation, and synchronized date ranges.
- **Plan C**: Workspace-oriented command center with customizable views, keyboard shortcuts, split-pane inspection, and live activity streams.

### 4. For an Object / Domain Entity (e.g., Invoice, Medical Record, User Account)
- **Plan A**: Clean presentation card displaying all fields with consistent labeling and copy-to-clipboard icons.
- **Plan B**: Master-detail slide-over sheet with audit log tabs, status badges, and quick-action menu.
- **Plan C**: Interactive living document with inline collaborative editing, version history comparison, and automated status transition workflows.
