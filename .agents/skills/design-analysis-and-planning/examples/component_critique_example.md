# Example Walkthrough: Design Analysis & 3-Plan Proposal

This example demonstrates how to apply the `design-analysis-and-planning` skill to an intended UI component: a **Dental Patient Appointment Card**.

---

# Design Analysis: Patient Appointment Card (`<AppointmentCard />`)

**Target Type**: `Component`  
**Analyzed File**: `components/appointments/AppointmentCard.tsx`  
**Primary User Goal**: View upcoming dental appointment details, check treatment status, and reschedule or cancel with zero confusion.  
**Target Environment / Constraints**: React + TypeScript, Tailwind CSS, desktop web and mobile viewports.

---

## 1. Executive Diagnostic Scorecard

| Evaluation Dimension | Health Rating | Summary of Findings |
| :--- | :--- | :--- |
| **UX & Usability** | 🟡 Needs Work | Primary and destructive actions lack clear hierarchy; status changes are not visually emphasized. |
| **Visual Hierarchy & Polish** | 🟡 Needs Work | Weak typography distinction between patient name, procedure type, and date; inconsistent padding. |
| **Accessibility (WCAG 2.2 AA)**| 🔴 Critical Flaws | Contrast failure on gray status badge (2.8:1); icon-only "trash" button has no `aria-label`; missing keyboard focus ring. |
| **Architecture & State** | 🟡 Needs Work | Monolithic 280-line component mixing date formatting, modal dialog toggling, and direct API fetch calls. |
| **Edge Cases & Resilience** | 🔴 Critical Flaws | Crashes when `patient.notes` is undefined; long dentist names cause horizontal layout overflow on mobile (375px). |

---

## 2. Comprehensive Flaw Audit

### 🔴 Critical & High-Priority Flaws (P0 / P1)
- **[FLAW-01] Accessibility Contrast Violation on Status Badge**
  - **Category**: Accessibility
  - **Location**: `<span className="bg-gray-100 text-gray-400">Confirmed</span>`
  - **Description**: Text contrast ratio is 2.8:1, failing WCAG 2.2 AA requirement (minimum 4.5:1).
  - **Impact**: Low-vision users and users in bright environments cannot read the appointment status.
- **[FLAW-02] Icon-Only Action Button Missing Accessible Name**
  - **Category**: Accessibility
  - **Location**: `<button onClick={onDelete}><TrashIcon /></button>`
  - **Description**: Rendered button has no text label or `aria-label`.
  - **Impact**: Screen reader announces "button" with no context; blind users may accidentally trigger cancellation.
- **[FLAW-03] Mobile Viewport Overflow on Long Names**
  - **Category**: Edge Cases / Responsive
  - **Location**: Card header flex container (`flex-row justify-between`)
  - **Description**: When a dentist or procedure name exceeds 22 characters, the action button is pushed off-screen on 375px mobile screens.
  - **Impact**: Users cannot access the action menu on mobile devices.

### 🟡 Medium & Low-Priority Flaws (P2 / P3)
- **[FLAW-04] Equal Visual Weight for Reschedule and Cancel**
  - **Category**: UX & Usability
  - **Location**: Card footer actions
  - **Description**: Both "Reschedule" and "Cancel" buttons use identical gray outline styling, increasing cognitive load and accidental cancellation risk.
- **[FLAW-05] Architectural Coupling of Networking & UI**
  - **Category**: Architecture
  - **Location**: `useEffect` inside `AppointmentCard` calling `fetch('/api/appointments/' + id)`
  - **Description**: The presentation card fetches its own nested data instead of accepting normalized props or using a data hook.
  - **Impact**: Card cannot be rendered in Storybook or unit tested without mocking global network calls.

---

## 3. The Three Proposed Design Plans

---

### Plan A: Surgical Refinement (Low Risk / Immediate 2-Hour Patch)

> **Philosophy**: Fix all critical accessibility and crash bugs immediately without altering existing component props or DOM architecture.

- **Proposed Changes**:
  1. **Fix Contrast**: Update status badge color to `bg-emerald-50 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300` (contrast ratio 6.2:1).
  2. **Add Accessible Labels**: Add `aria-label="Cancel appointment for {patientName}"` and `focus-visible:ring-2 focus-visible:ring-offset-2`.
  3. **Prevent Overflow**: Add `truncate max-w-[200px]` to patient and doctor name headers with `title={patientName}` tooltip.
  4. **Safeguard Notes**: Add optional chaining `patient?.notes ?? 'No notes recorded'`.
  5. **Visual Action Weight**: Change "Reschedule" to solid secondary button (`bg-slate-900 text-white`) and "Cancel" to subtle ghost red (`text-rose-600 hover:bg-rose-50`).
- **Pros**: Zero breaking changes, drops into existing codebase with 100% backward compatibility in under 2 hours.
- **Cons**: Card remains coupled to network fetch; does not solve component reusability.
- **Implementation Effort**: ~2 hours.

---

### Plan B: Balanced Best-Practice Overhaul (Production Gold Standard)

> **Philosophy**: Re-architect into clean, reusable compound components with dedicated hook logic, fluid responsive layouts, and full WCAG 2.2 AA compliance.

- **Proposed Changes**:
  1. **Compound Component Decomposition**: Split into `<AppointmentCard>`, `<AppointmentCard.Header>`, `<AppointmentCard.Details>`, `<AppointmentCard.Badge>`, and `<AppointmentCard.Actions>`.
  2. **Logic Extraction**: Move async actions into a custom hook `useAppointmentActions(appointmentId)`.
  3. **Responsive Container Queries**: Use `@container` queries so the card smoothly switches from stacked on mobile to horizontal two-column on desktop.
  4. **Loading & Empty State**: Provide `<AppointmentCard.Skeleton />` with pulsing shimmer for smooth loading transitions without layout shift.
  5. **Confirmation Dialog Primitive**: Integrate accessible modal dialog for cancellation with focus trap and `Esc` key handling.
- **Pros**: Resolves all flaws, enables composable variants across Doctor and Patient portals, 100% unit-testable in isolation.
- **Cons**: Requires updating 3 parent view files that render `<AppointmentCard />`.
- **Implementation Effort**: 1–2 days.

---

### Plan C: Radical Reimagination (Next-Gen Contextual Timeline Card)

> **Philosophy**: Transform the static card into an interactive, real-time dental procedure journey node with contextual micro-actions and optimistic sync.

- **Proposed Changes**:
  1. **Interactive Treatment Timeline**: The card expands inline to reveal pre-op instructions, digital consent form checklist, and post-op care guides.
  2. **One-Tap Smart Reschedule Drawer**: Clicking "Reschedule" opens a lightweight slide-over with AI-suggested open slots based on the dentist's real-time chair availability.
  3. **Optimistic Local Updates**: Cancelling or rescheduling reflects immediately in the UI with a 5-second "Undo" snackbar banner before network commit.
  4. **Direct Calendar Sync**: Native `.ics` and Google Calendar / Apple Wallet integration buttons embedded right into the card.
- **Pros**: Unmatched patient experience, reduces appointment no-shows, elevates product to industry-leading craft.
- **Cons**: Requires new backend API endpoints for real-time chair availability and undo queues.
- **Implementation Effort**: 1 sprint (~1.5 weeks).

---

## 4. Comparative Decision Matrix

| Evaluation Criteria | Plan A (Surgical) | Plan B (Balanced) | Plan C (Radical) |
| :--- | :---: | :---: | :---: |
| **Flaw Resolution** | Fixes P0/P1 only (60%) | Complete P0–P3 (100%) | Complete + Transformative |
| **UX & Visual Polish** | Modest improvement | Modern, high craft | Best-in-class experience |
| **Code Architecture** | Legacy preserved | Clean compound pattern | Event-driven micro-surface |
| **Implementation Effort** | 2 Hours (⚡) | 1.5 Days (⏱️) | 1.5 Weeks (🛠️) |
| **Regression Risk** | Negligible | Very Low | Moderate |
| **Recommendation Score** | 7.0 / 10 | **9.2 / 10 (Recommended)** | 8.5 / 10 |

---

## 5. Recommendation & Roadmap

### Recommended: Plan B (Balanced Best-Practice)
Plan B delivers the highest ROI: it eliminates critical accessibility flaws, solves mobile layout breakage, and deconstructs technical debt into reusable compound components without needing major backend API overhauls.

### Phased Execution Roadmap
1. **Milestone 1**: Implement Plan A fixes immediately to patch accessibility and prevent mobile overflow bugs.
2. **Milestone 2**: Refactor component structure to Compound Components and extract `useAppointmentActions` hook.
3. **Milestone 3**: Add `<AppointmentCard.Skeleton />` and integrate accessible cancellation dialog.
4. **Milestone 4**: Backlog Plan C features (real-time calendar sync and one-tap reschedule drawer) for the next quarterly roadmap.
