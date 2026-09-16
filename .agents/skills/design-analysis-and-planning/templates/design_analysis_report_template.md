# Design Analysis & 3-Plan Proposal Template

Use this template when outputting a formal design critique, flaw analysis, and 3-plan proposal.

---

# Design Analysis: [Target Name]

**Target Type**: `[Object | Page | View | Component | Flow]`  
**Analyzed File(s) / Context**: `[File path or description]`  
**Primary User Goal**: `[What the user is trying to accomplish]`  
**Target Environment / Constraints**: `[Platform, framework, design tokens, viewport constraints]`

---

## 1. Executive Diagnostic Scorecard

| Evaluation Dimension | Health Rating (`🟢 Healthy` \| `🟡 Needs Work` \| `🔴 Critical Flaws`) | Summary of Findings |
| :--- | :--- | :--- |
| **UX & Usability** | `[Rating]` | `[1-sentence summary]` |
| **Visual Hierarchy & Polish** | `[Rating]` | `[1-sentence summary]` |
| **Accessibility (WCAG 2.2 AA)**| `[Rating]` | `[1-sentence summary]` |
| **Architecture & State** | `[Rating]` | `[1-sentence summary]` |
| **Edge Cases & Resilience** | `[Rating]` | `[1-sentence summary]` |

---

## 2. Comprehensive Flaw Audit

List the flaws discovered using the diagnostic rubric, ordered by severity.

### 🔴 Critical & High-Priority Flaws (P0 / P1)
- **[FLAW-01] [Short Flaw Title]**
  - **Category**: `[UX | Visual | Accessibility | Architecture | Edge Cases]`
  - **Location / Element**: `[Specific code line, element, or sub-component]`
  - **Description**: `[Detailed description of what is broken or suboptimal]`
  - **Root Cause**: `[Why this happens in code/design]`
  - **Impact**: `[Direct consequence on user or engineering]`

### 🟡 Medium & Low-Priority Flaws (P2 / P3)
- **[FLAW-02] [Short Flaw Title]**
  - **Category**: `[UX | Visual | Accessibility | Architecture | Edge Cases]`
  - **Location / Element**: `[Specific element or style rule]`
  - **Description**: `[Description of flaw]`
  - **Impact**: `[Suboptimal outcome or cosmetic debt]`

---

## 3. The Three Proposed Design Plans

---

### Plan A: Surgical Refinement (Low Risk / Immediate Velocity)

> **Philosophy**: Minimal-touch fixes that resolve critical P0/P1 flaws and improve polish without breaking APIs or altering architecture.

- **Proposed Changes**:
  - `[Change 1: e.g. Add aria-labels and fix focus ring outline]`
  - `[Change 2: e.g. Fix button alignment and typography contrast]`
  - `[Change 3: e.g. Add empty state and loading guard]`
- **Pros**:
  - Zero breaking changes; 100% backward compatible.
  - Can be implemented in a matter of hours.
- **Cons**:
  - Leaves underlying architectural debt intact.
  - Does not modernize user interaction flow.
- **Implementation Effort**: `[e.g. 2–4 hours | 1 day]`

---

### Plan B: Balanced Best-Practice Overhaul (Production Gold Standard)

> **Philosophy**: Elevate the component/view to modern production standards, modularizing architecture and achieving full WCAG 2.2 AA compliance.

- **Proposed Changes**:
  - `[Change 1: e.g. Refactor into compound components (<Card.Header>, etc.)]`
  - `[Change 2: e.g. Extract logic into custom hook useAppointmentBooking()]`
  - `[Change 3: e.g. Implement full skeleton loader and optimistic toast updates]`
  - `[Change 4: e.g. Add responsive container queries and mobile drawer transition]`
- **Pros**:
  - Resolves all P0–P3 flaws.
  - Highly reusable, testable, and maintainable.
  - Follows design system token standards.
- **Cons**:
  - Requires moderate refactoring of consumer call sites.
- **Implementation Effort**: `[e.g. 2–4 days]`

---

### Plan C: Radical Reimagination (First-Principles Innovation)

> **Philosophy**: Re-envision the problem space to provide a breakthrough, high-craft user experience with modern interaction paradigms.

- **Proposed Changes**:
  - `[Change 1: e.g. Replace static modal with keyboard-first command bar and contextual floating sheet]`
  - `[Change 2: e.g. Introduce smart predictive defaults and instant fuzzy filtering]`
  - `[Change 3: e.g. Implement headless state machine supporting multi-platform rendering]`
- **Pros**:
  - Step-function improvement in user delight and efficiency.
  - Differentiates product from competitors.
- **Cons**:
  - Highest engineering effort and design coordination required.
  - May require user onboarding for new interaction paradigm.
- **Implementation Effort**: `[e.g. 1–2 sprints]`

---

## 4. Comparative Decision Matrix

| Evaluation Criteria | Weight | Plan A (Surgical) | Plan B (Balanced) | Plan C (Radical) |
| :--- | :---: | :---: | :---: | :---: |
| **Flaw Resolution** | 25% | Partial (P0/P1 only) | Complete (P0–P3) | Complete + Future-proof |
| **UX & Visual Delight** | 25% | Baseline | High | Exceptional |
| **Architecture & Code Quality** | 20% | Unchanged | Clean & Modular | Scalable Platform |
| **Implementation Effort** | 15% | Very Low (⚡) | Moderate (⏱️) | High (🛠️) |
| **Deployment / Regression Risk**| 15% | Minimal | Low | Medium |
| **Overall Recommendation Score**| 100%| `[Score / 10]` | `[Score / 10]` | `[Score / 10]` |

---

## 5. Agent Recommendation & Phased Roadmap

### Recommended Choice
> **[Select Plan A, B, or C]**: State clear justification based on project context, timeline, and ROI.

### Phased Execution Roadmap
1. **Phase 1 (Immediate)**: `[Action items from Plan A or first milestone of Plan B]`
2. **Phase 2 (Core Build)**: `[Sub-components, state machine, tests]`
3. **Phase 3 (Verification & Polish)**: `[A11y audit, responsiveness test, user testing]`
