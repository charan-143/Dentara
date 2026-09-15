# Tab Report: Patients

By far the largest tab: roster + registration + a 6-tab patient chart (Demographics, Examination, Reports & Imaging, Diagnosis & Treatment, Prescriptions, Visit History). Source files: `PatientRosterScreen.kt`, `RegisterPatientScreen.kt`, `PatientDetailChartScreen.kt` (1621 lines — contains the Odontogram, Treatment Plans, Prescriptions-view, Reports-view and Visit-History composables inline), `PatientDemographicsM3View.kt`, `MaxillaryQuadrantViews.kt`, `MandibularQuadrantViews.kt`, `ExaminationQuestionsSection.kt`, `CreateTreatmentPlanDialog.kt`, `IssuePrescriptionDialog.kt`, `PrescriptionsManagementScreen.kt`, `AddReportDialog.kt`, `AddReportScreen.kt`, `ReportViewerLightboxDialog.kt`, plus `ui/components/AnatomicalToothVisual.kt`.

**Scope correction:** the 3D molar viewer (`ui/components/molar3d/*`) is **not** part of this tab — it's only called from `WelcomeBrandScreen.kt` (the pre-login onboarding root), purely decorative. `ui/components/DentalModelView.kt` also appears to have no callers anywhere in the app — likely dead, not deep-audited given time.

## ⚠️ Two findings dominate this tab and should be triaged first
1. A patient's chart fabricates a specific **blood group and gender** when the real value isn't known, instead of saying "Unknown" — see the Demographics component below. Blood type is safety-critical clinical data; showing a wrong one as fact is the single most serious finding in this entire audit.
2. The diagnostic report viewer **never displays the clinician's actual uploaded X-ray/image** — it always draws a generic fake radiograph instead, regardless of what was really uploaded. See the Reports & Imaging component below.

## Components

### Component: Patient Roster List
Shared: no
Complexity: medium
**Design**
- layout: `Column` → bordered header `Surface` (title/count + "Register Patient" button + search field + 2 `FilterChip`s), then `LazyColumn` of `Card` rows or an empty state
- spacing: 20dp horizontal/14dp vertical header padding; rows 10dp apart; row internal padding 14dp
- colors: header `ThornburyCanvas`/`ThornburyHairline`; row card `ThornburySurfaceCard`/`ThornburyHairline`; selected chip `ThornburyPrimary`/white
- typography: title `titleLarge` bold; row name `titleMedium` bold; row meta `ClinicalCodeStyle` 11sp muted
- icons: `PersonAdd` (register button), `Search`/`Close` (search field), `Check` (selected filter chip), `PersonSearch` (empty state)
- surface: header flat bordered; rows 10dp radius
- states: empty ("No patients match the search criteria."), populated
**Backend**
- data_source: `DentalRepository.patients`, `DentalRepository.appointments` (for the "Upcoming" filter's patient-id set)
- data_shape: filters on `Patient.name/opNo/email/phone`; "Upcoming" = patients with any `confirmed` appointment
- state_management: 3 screen-local `remember` states (`searchQuery`, `filterMode`, `showRegisterDialog` — see Gap)
- interactions: tap a row → `onSelectPatient` (opens Patient Chart); "Register Patient" button → `onNavigateToRegisterPatient` (opens the full-page `RegisterPatientScreen`, wired in `Navigation.kt`)
- business_logic: case-insensitive substring search across 4 fields; upcoming-count derived live from appointments
- edge_states: empty-search state only; no loading/error state (data is local/synchronous)
- dependencies: `RegisterPatientScreen` (Navigation.kt-level), Patient Chart (this tab)
- permissions: none
**Gaps**
- [HIGH] This file defines a complete, ~200-line second registration form — `RegisterPatientDialog` (lines 366-573) — gated behind `showRegisterDialog`, but nothing in the codebase ever sets `showRegisterDialog = true` (the visible "Register Patient" button calls the unrelated `onNavigateToRegisterPatient` callback instead, which opens `RegisterPatientScreen.kt`). `RegisterPatientDialog` is 100% unreachable dead code. — evidence: `PatientRosterScreen.kt:42` (declared), `275-283` (only ever set to `false`), `366-573` (the dead composable itself) — fix: delete `RegisterPatientDialog`, or wire it in if a modal (vs. full-page) registration flow is actually wanted — don't maintain both.

### Component: Register Patient Flow
Shared: no (the dead dialog above is a separate, unreachable duplicate of this)
Complexity: high
**Design**
- layout: `Scaffold` w/ back-arrow `TopAppBar`, 4 stacked `OutlinedCard`s (Personal Info, Clinical Indexing/OP Number, Contact, Emergency Contact), Cancel/Register button row
- spacing: 20dp horizontal screen padding; cards 16dp apart, 18dp internal padding
- colors: cards `ThornburySurfaceSoft`/`ThornburyHairline`; error banner `ThornburyErrorWash`/`ThornburyError`; selected gender chip `ThornburyPrimary`/white
- typography: card titles `titleMedium` bold; field labels `labelMedium` bold
- icons: `Person`, `Badge`, `ContactPhone`, `Shield` (one per card), `ErrorOutline` (error banner), `ArrowBack`, `Check`
- surface: cards 16dp radius; fields 10dp radius
- states: inline error banner on validation failure; no loading/success state (registration is synchronous/local)
**Backend**
- data_source: writes via `DentalRepository.registerPatient(name, opNo, dob, phone, email, address, allergies, medicalAlerts, medicalHistory)`
- data_shape: `registerPatient`'s actual parameter list has **no `gender` and no emergency-contact fields at all**
- state_management: 9 screen-local `remember` states, incl. `selectedGender` (default "Female"), `emergencyName`, `emergencyPhone`
- interactions: gender `FilterChip`s (Female/Male/Other), OP number auto-filled to a random `OP-4xxxx` (editable), all fields editable, Cancel/Register buttons
- business_logic: only `name.isNotBlank()` is validated; OP number falls back to a fresh random value if cleared
- edge_states: single inline error message for the one validation rule
- dependencies: `Navigation.kt` (entry/exit), `ThornburyDatePickerField` (shared component)
- permissions: none
**Gaps**
- [HIGH] The "Gender" selector and the entire "Emergency Contact" card (name + phone) are fully interactive and look saved, but `DentalRepository.registerPatient()` has no parameters for gender or emergency contact — the call site (`RegisterPatientScreen.kt:425-435`) simply never passes `selectedGender`, `emergencyName`, or `emergencyPhone`. Every value a clinician enters into these fields is silently discarded on submit. — evidence: `RegisterPatientScreen.kt:36,42-43` (state declared) vs `425-435` (not passed) vs `DentalRepository.kt:981-991` (`registerPatient` signature has no such parameters) — fix: add `gender`/emergency-contact fields to the `Patient` model + DB schema and thread them through, or remove the UI for fields that can't be saved.
- [MEDIUM] Because there is nowhere to persist gender, the patient chart's Demographics view later *guesses* a gender from the patient's first name (see the Demographics component's blood-group/gender finding below) — this card's gender selector and that guess are completely disconnected from each other.

### Component: Patient Chart Shell (top bar + 6-tab bar)
Shared: no
Complexity: low
**Design**
- layout: bordered `Surface` app bar (back arrow, patient name/OP/DOB) + `PrimaryScrollableTabRow` with 6 tabs, 4 of which carry a live count badge
- spacing: app bar 12dp/8dp padding; tab edge padding 12dp
- colors: app bar `ThornburyCanvas`/`ThornburyHairline`; selected tab text `ThornburyPrimaryText`, badge `ThornburyPrimary`/white
- typography: patient name `titleMedium` bold; tab label `labelMedium`, bold when selected
- icons: `ArrowBack`
- surface: flat bordered app bar
- states: `patient == null` shows a plain "Patient not found" full-screen message instead of crashing
**Backend**
- data_source: `DentalRepository.patients/treatmentPlans/prescriptions/reports/appointments`, all filtered to this `patientId`
- data_shape: n/a (routing shell)
- state_management: `selectedTabIndex` plus several dialog-visibility flags (see gaps in child components)
- interactions: tab taps switch content; back arrow calls `onBack`
- business_logic: `patients.find { it.id == patientId } ?: patients.firstOrNull()` — if the requested patient ID isn't found, it silently falls back to showing a *different, arbitrary* patient rather than the "not found" state
- edge_states: `patient == null` (only possible if the roster is completely empty) shows "Patient not found"
- dependencies: every tab component below
- permissions: none
**Gaps**
- [MEDIUM] `patients.find { it.id == patientId } ?: patients.firstOrNull()` (`PatientDetailChartScreen.kt:45`) means an invalid/stale `patientId` doesn't show "Patient not found" — it silently opens a **different patient's** chart instead. This is a real risk in a clinical record app (wrong-patient-record display). — evidence: `PatientDetailChartScreen.kt:45,59-64` — fix: only fall back to the "not found" state when the specific `patientId` isn't in the list; don't substitute an arbitrary other patient.

### Component: Demographics / Overview
Shared: no
Complexity: high
**Design**
- layout: `Column` → Hero ID `ElevatedCard` (avatar monogram, name, OP chip, 4 vital-metric columns, Call/Email/Map buttons), then 3 `OutlinedCard`s (Contact & Address as `ListItem`s, Medical History bullets, Family History bullets, Past Dental History bullets)
- spacing: 16dp outer padding, 16dp between cards, 14dp internal card padding
- colors: avatar `MaterialTheme.colorScheme.primaryContainer`; cards `ThornburyCanvas`/`ThornburyHairline`; bullet dot `ThornburyPrimary`
- typography: name `headlineSmall` bold; vital label `labelSmall` 10sp bold; vital value `bodyMedium` bold
- icons: `Cake`, `Person`, `LocalHospital`, `History` (vitals), `Phone`/`Email`/`Place` (quick actions), `Edit`, `ContentCopy`, `Groups`, `MedicalServices`
- surface: hero card 16dp radius w/ 2dp elevation; other cards 14dp radius
- states: each history card has its own empty-state italic message; contact rows individually show "No telephone/email/address recorded" when blank
**Backend**
- data_source: local `Patient` fields only — no network/DB read beyond what's already loaded
- data_shape: reads `dob`, `medicalHistory`, `familyHistory`, `pastDentalHistory`, `medicalAlerts`, `phone`, `email`, `address`; writes back via `DentalRepository.updatePatientDemographics(...)`
- state_management: 2 dialog-visibility states (`showEditContactDialog`, `activeHistoryEditType`); edit dialogs hold their own local draft text
- interactions: Call/Email/Map buttons launch real Android intents (safely wrapped, with a Toast fallback if nothing's recorded or no app can handle the intent); OP number chip copies to clipboard with a Toast; each card's "Edit" opens its own dialog; Medical History bullets that mention allergy-related keywords are explicitly filtered *out* of the "Systemic Medical History" list
- business_logic: `getPatientMonogram` (initials from name), `calculatePatientAge` (DOB → age or `null`), **`deriveGender`**, **`deriveBloodGroup`** — see Gaps; `parseClinicalBullets` splits free-text history into bullet lines (handles `-`/`*`/`•`/numbered prefixes, falls back to sentence-splitting)
- edge_states: per-card empty messages; age shows "Unknown" when DOB is blank/unparseable (this one, unlike Today/Schedule's age calculator, does **not** fabricate a fallback number)
- dependencies: `ThornburyDatePickerField`, `DentalRepository.updatePatientDemographics`
- permissions: none
**Gaps**
- [HIGH — patient safety] The "BLOOD GROUP" vital-sign field is not real data: `deriveBloodGroup()` regex-searches `medicalHistory`/`medicalAlerts` free text for an `A/B/AB/O[+-]` pattern, and if none is found, **hardcodes "O+"** as the displayed value — not "Unknown," an actual, specific, wrong blood type presented as fact on the patient's own chart. Any patient whose blood type was never typed verbatim into a free-text history field will show as O+ regardless of their real blood type. — evidence: `PatientDemographicsM3View.kt:1182-1186` (`?: "O+"` fallback) — fix: show "Unknown" (or nothing) when no blood type is on record; never fabricate a specific value for safety-critical data.
- [HIGH] The "GENDER" vital-sign field is likewise fabricated: `deriveGender()` guesses from the patient's first name against five hardcoded first names (`rosalind`, `kavitha`, `marisol` → Female; `dmitri`, `owen` → Male) or a literal "mr"/"mrs"/"ms"/"miss" substring, defaulting to the meaningless value **"Adult"** for everyone else. This is disconnected from the real gender selector already collected (and silently discarded) during registration — see the Register Patient Flow gap above; the root cause is that `Patient` has no `gender` field at all. — evidence: `PatientDemographicsM3View.kt:1172-1180`, `Models.kt:100-116` (no `gender` field) — fix: add `gender` to the `Patient` model, populate it from registration, and display "Unknown" here when absent — delete `deriveGender`.
- [LOW] `calculatePatientAge()` here is a third independent reimplementation of the same DOB→age logic already duplicated between `TodayQueueScreen.kt` and `ScheduleScreen.kt` (see the Schedule Tab Report). This copy is actually the best-behaved of the three (returns `null`/"Unknown" instead of a fake fallback age) — worth using as the canonical version when deduplicating. — evidence: `PatientDemographicsM3View.kt:1147-1170`.

### Component: Odontogram (Examination tab → "Odontogram Chart" sub-tab)
Shared: no (relies on the shared `AnatomicalToothVisual.kt` rendering component)
Complexity: high
**Design**
- layout: `QuadrantFilterRow` (7-way horizontally-scrolling filter chips) → `MaxillaryArchContainer`/`MandibularArchContainer`, each showing 1-2 quadrant `Card`s with a horizontally-scrolling strip of `AnatomicalToothView` teeth, separated by an `OcclusalPlaneDivider`
- spacing: 16dp screen padding, 12-14dp between arch containers, 8dp between teeth
- colors: quadrant badge `ThornburyPrimaryWash`; pathology summary badge 3-way color-coded (all-sound=success, has-decay=error, other=warning); tooth cell border colored by condition (`ToothSound/Decay/Filled/Crown/Missing/Implant/RootCanal` tokens)
- typography: quadrant title `titleMedium` bold; tooth universal number `ClinicalCodeStyle` bold; FDI number `labelSmall` 9sp muted
- icons: none beyond text glyphs (✕ for missing teeth, condition-code letters)
- surface: quadrant cards 12-14dp radius
- states: per-tooth condition drives color/label; `QuadrantFilter.ALL` shows both arches with a divider, any other filter isolates one arch/quadrant
- responsiveness: teeth strips scroll horizontally per quadrant rather than wrapping — reasonable for phone width
**Backend**
- data_source: `patient.teeth: Map<Int, ToothRecord>` (in-memory on the `Patient` object); missing tooth numbers are synthesized on the fly via `defaultUpperToothRecord`/`getMandibularToothRecord` so every tooth 1-32 always renders even before any exam data exists
- data_shape: `ToothRecord { number, fdiNumber, name, arch, condition, notes }`
- state_management: `quadrantFilter` is screen-local (resets whenever the Examination tab is re-entered); tooth edits go through `ToothEditDialog` → `DentalRepository.updateToothCondition`
- interactions: tap a tooth → opens `ToothEditDialog` (condition picker + notes field, with a live anatomical preview via `AnatomicalToothCanvas`); tap a filter chip → narrows to that arch/quadrant
- business_logic: `getToothColor(condition)` maps condition → theme color; quadrant pathology badge counts non-sound teeth
- edge_states: none needed — synthesized defaults mean there's no "no data" state to handle
- dependencies: `AnatomicalToothVisual.kt` (shared tooth-rendering component)
- permissions: none
**Gaps**
- [LOW] `UpperRightQuadrantCard`, `UpperLeftQuadrantCard`, and `MaxillaryArchContainer` each have a second, `Patient`-accepting convenience overload (`fun MaxillaryArchContainer(patient: Patient, ...)` etc.) that is never called anywhere in the app — every real call site passes `teeth: Map<Int, ToothRecord>` directly instead. Minor unused-code — evidence: `MaxillaryQuadrantViews.kt:347-360,490-503,619-637` (only called from that file's own `@Preview`, not from production code).

### Component: Clinical Assessment Questionnaire (Examination tab → "Clinical Assessment" sub-tab)
Shared: no
Complexity: medium
**Design**
- layout: single collapsible `Card` with 6 numbered sub-sections (Chief Complaints, Pain & Sensitivity, Periodontal & Soft Tissue, Oral Hygiene & Habits, Caries Risk, Clinician Notes), each its own sub-card with `FlowRow` chip groups, radio-style selection surfaces, or a notes field
- spacing: 16dp/12dp screen padding, 20dp between sub-sections, 14dp per sub-card
- colors: a "LIVE SYNC" pill (`ThornburySuccessWash`/`ThornburySuccess`) in the header; selected pain-severity/caries-risk options use per-option semantic colors (success/warning/amber/error); periodontal "bleeding" and soft-tissue "abnormal" selections turn warning/error colored when selected
- typography: header `titleMedium` bold; sub-section titles `titleSmall` bold; option labels `bodySmall`
- icons: one per sub-section (`Assignment`, `QuestionAnswer`, `Bolt`, `FavoriteBorder`, `CleanHands`, `Security`, `EditNote`) plus per-chip icons for chief complaints/sensitivity triggers
- surface: outer card 12dp radius; sub-cards 10dp radius, semi-transparent tinted background
- states: expand/collapse toggle for the whole section; each chip/option has selected/unselected styling; no loading/error state (fully local/synchronous)
**Backend**
- data_source/writes: `patient.examAnswers` in, `DentalRepository.updateExaminationAnswers(patientId, ExaminationAnswers)` out
- data_shape: `ExaminationAnswers` matches the UI's fields exactly (chief complaints, pain severity, sensitivity triggers, periodontal/soft-tissue findings, brushing/flossing frequency, functional habits, caries risk, clinician notes)
- state_management: 11 independent `remember(patientId, initialAnswers)` locals; every single toggle/selection immediately calls `onSaveAnswers` with a freshly-assembled `ExaminationAnswers` (true live-sync, matching the "LIVE SYNC" badge's claim — verified accurate)
- interactions: every chip/radio/quick-suggestion is tappable and persists instantly; "Quick Clinical Suggestions" append preset sentences to the notes field
- business_logic: multi-select toggles are plain list add/remove; single-select fields are plain equality checks
- edge_states: none needed (empty defaults are valid states)
- dependencies: none beyond the repository call
- permissions: none
**Gaps**
none found — this component is solid: real-time persistence matches its own UI claim, and the data model lines up field-for-field with the UI.

### Component: Reports & Imaging
Shared: `AddReportDialog` is dead weight duplicating live `AddReportScreen`; `ReportViewerLightboxDialog` and `ReportsAndImagingView` are tab-local
Complexity: high
**Design**
- layout (list, `ReportsAndImagingView` in `PatientDetailChartScreen.kt`): header + "Add Report" button, 6-way `FilterChip` kind filter, `LazyColumn` of report `Card`s (icon by kind, title, kind badge, summary, clinician/date footer) or an empty state with its own "Add New Test" CTA
- layout (add, `AddReportScreen.kt`, full-page — the live flow): Kind selector chips, Title + quick-title chips, Summary + quick-phrase chips, a real file-upload zone (SAF document picker) with per-file uploading/failed/attached states
- layout (viewer, `ReportViewerLightboxDialog.kt`): near-fullscreen dialog with a release-status badge, Release/Hold toggle, Periapical/Panoramic mode switch, a pinch-zoom/pan `Canvas` radiograph viewport with invert/zoom controls and a simulated calibration ruler + orientation marker, and a findings summary card
- colors/typography/icons: consistent Thornbury tokens throughout; kind icons (`Image`/`ViewInAr`/`FormatListNumbered`/`Science`); release badge success-wash when released, warning-wash when held
- states: list has an empty state with two nested CTAs; upload zone shows per-file spinner/error/attached rows; viewer distinguishes released vs. held
**Backend**
- data_source: `DentalRepository.reports` filtered by patient; `AddReportScreen` also calls `DentalRepository.uploadAttachment(context, uri, ...)` for real file uploads (SAF `OpenDocument`, persistable URI permission, content-resolver metadata lookup) — this part is genuinely well-built, with pending/failed uploads tracked separately from confirmed `attachments` so a failed upload can never silently appear as attached
- data_shape: `DiagnosticReport { id, patientId, clinicianName, kind, title, summary, takenAt, releasedAt?, image?, attachments: List<ReportAttachment> }`
- state_management: list-level `selectedFilter`; `AddReportScreen` tracks in-flight uploads locally, only promoting to `attachments` on confirmed success
- interactions: tap a report card → opens the Lightbox; "Add Report" → `AddReportScreen` (via `Navigation.kt`); Lightbox's Release/Hold toggle → `DentalRepository.toggleReportRelease`; Lightbox pinch/zoom/pan/invert on the (simulated) radiograph
- business_logic: `isInitiallyPanoramic` guesses viewer mode from title/summary keywords; `hasLesion`/`hasPathology` similarly keyword-sniff the summary to decide which fake pathology markings to draw
- edge_states: list empty state; upload failure rows (tap to dismiss)
- dependencies: `Navigation.kt` (full-page add flow), shared `DentalRepository`
- permissions: none — `releasedImmediately`/portal-release is a clinician-only toggle in the UI but nothing in code actually gates it by role
**Gaps**
- [HIGH] `ReportViewerLightboxDialog` **never reads `report.image` or `report.attachments`** anywhere in its ~600 lines — it always procedurally draws a generic simulated periapical or panoramic radiograph (`drawPeriapicalRadiograph`/`drawPanoramicRadiograph`), picking which fake pathology markings to add by keyword-matching the report's `title`/`summary` text, and hardcoding `toothNumber = 19` regardless of the report's actual subject. A clinician who uploads a real X-ray via `AddReportScreen`'s (correctly-built) file picker will never see that real image again — only a generic fake drawing, for every single report regardless of what was actually uploaded. — evidence: `ReportViewerLightboxDialog.kt` (whole file — no reference to `report.image` or `report.attachments`), `ReportViewerLightboxDialog.kt:350` (hardcoded `toothNumber = 19`) — fix: render `report.attachments`/`report.image` (an actual image viewer) when present, and reserve the simulated-radiograph drawing (if kept at all) for reports that genuinely have no attachment.
- [HIGH] `AddReportDialog.kt` is an entire duplicate, unreachable "add report" implementation — same dead-dialog-vs-live-screen pattern as `RegisterPatientDialog`. Its trigger, `showAddReportDialog` in `PatientDetailChartScreen.kt`, is declared and reset to `false` but never set to `true` anywhere; the real "Add Report" button instead calls `onOpenAddReportScreen`, which routes to the separate, live `AddReportScreen.kt` via `Navigation.kt`. `AddReportDialog` also hardcodes `selectedClinician = "Dr. Ingrid Halvorsen"` and a permanently-`false` `releasedImmediately` with no UI control for it, despite its own doc comment claiming "explicit control over immediate release" — moot since it's unreachable, but confirms it's an abandoned earlier draft. — evidence: `PatientDetailChartScreen.kt:55,225-241` (dead trigger), `AddReportDialog.kt:33-50` — fix: delete `AddReportDialog.kt`.
- [HIGH] The live, correctly-built `AddReportScreen.kt` still hardcodes `val clinicianName = "Dr. Ingrid Halvorsen"` (line 70) instead of reading `AuthRepository.currentUser` — every diagnostic report, regardless of which clinician actually created it, is permanently attributed to Dr. Halvorsen. This is the same app-wide pattern flagged in the Today and Profile Tab Reports, and recurs again in the Treatment Plan component below — see the cross-tab note at the end of this report. — evidence: `AddReportScreen.kt:70`.

### Component: Diagnosis & Treatment (Treatment Plans)
Shared: no
Complexity: high
**Design**
- layout (list, `TreatmentPlansView`): header + "New Plan" button, `LazyColumn` of plan `Card`s (diagnosis, tamper-lock badge, hash display, step checklist with tap-to-toggle completion, addenda list, lock/unlock action) or an empty state with its own "Create Initial Treatment Plan" CTA
- layout (create, `CreateTreatmentPlanDialog.kt`): near-fullscreen dialog, two fields only (Diagnosis, Treatment Plan Details free-text) each with quick-pick preset chips, by design (comment explicitly states "Price and fee fields are completely omitted")
- colors: locked badge `ThornburySurfaceDark`/teal icon; draft badge `ThornburyWarningWash`/`ThornburyWarning`; completed step row `ThornburySuccessWash` tint
- typography: plan title `titleMedium` bold; hash text `ClinicalCodeStyle` 10sp; step text strikethrough-free but semibold+success-colored when completed
- icons: `Lock`/`LockOpen`, `CheckCircle`/`RadioButtonUnchecked`, `Add`, `MedicalInformation`, `Assignment`
- surface: plan cards 12dp radius; hash box 6dp radius
- states: locked vs. draft; per-step completed vs. not; empty state
**Backend**
- data_source: `DentalRepository.treatmentPlans` filtered by patient; created via `DentalRepository.createTreatmentPlan(patientId, clinicianName, diagnosis, steps)`
- data_shape: `TreatmentPlan { id, patientId, clinicianName, diagnosis, dateCreated, isLocked, tamperHash, steps: List<PlanStep>, addenda }`; `PlanStep { id, toothNumber?, procedure, code, fee, completed }`
- state_management: plan list is shared/reactive; step-completion and lock-toggle both write straight through to the repository
- interactions: tap a step row/checkbox → `togglePlanStepCompletion`; "Publish & Lock Plan"/"Unlock for Addenda" → `togglePlanLock`; "New Plan" → `CreateTreatmentPlanDialog`
- business_logic: **verified real** — `tamperHash` is computed via `MessageDigest.getInstance("SHA-256")` over a canonical `planId|patientId|diagnosis|steps...` string (`DentalRepository.kt:516-523`), so the "tamper-evident cryptographically signed" claim in the UI is accurate, not fake (checked specifically given how many other claims in this app turned out to be fabricated)
- edge_states: empty state with its own CTA
- dependencies: none beyond the repository
- permissions: none — any signed-in user can lock/unlock/create plans; no clinician-vs-other-role gating despite the "clinician sign-off" framing elsewhere in the app
**Gaps**
- [HIGH] `clinicianName` in `CreateTreatmentPlanDialog` is a hardcoded literal, `"Dr. Ingrid Halvorsen"` (line 41), with **no UI control to change it at all** (unlike `IssuePrescriptionDialog`, which correctly offers a clinician radio-list right next to it in the same tab). Every treatment plan ever created — a signed clinical/legal record — is permanently attributed to the same name regardless of who is actually using the app. — evidence: `CreateTreatmentPlanDialog.kt:41` — fix: reuse the same clinician-selection pattern already implemented correctly in `IssuePrescriptionDialog.kt`, or bind to `AuthRepository.currentUser`.
- [MEDIUM] The free-text "Treatment Plan Details" box is parsed line-by-line into `PlanStep`s with `toothNumber = null` always, and a fabricated procedure code `"D${1000 + idx}"` (sequential placeholder, not a real ADA/CDT code) for every step. Downstream, `TreatmentPlansView` explicitly renders `"(Tooth #${step.toothNumber})"` only when non-null and shows `${step.code}: ...` as if it were a real procedure code — so every plan created through this dialog silently omits tooth references the display was built to show, and shows placeholder codes that look authoritative but aren't. — evidence: `CreateTreatmentPlanDialog.kt:280-289` vs `PatientDetailChartScreen.kt:936-937` — fix: let the clinician tag a tooth number per line, and either use real procedure codes or visually mark generated ones as placeholders.

### Component: Prescriptions
Shared: `PrescriptionsManagementScreen.kt` is dead code; `IssuePrescriptionDialog` is genuinely shared (also reachable via `Navigation.kt` at the app-root level)
Complexity: high
**Design**
- layout (list, `PatientPrescriptionsView` in `PatientDetailChartScreen.kt`): header + "Issue New Script" button, `LazyColumn` of prescription `Card`s (drug + dosage pill, Sig/instructions, issuedBy/date footer, "Share Rx" button) or an empty message
- layout (issue, `IssuePrescriptionDialog.kt`): modal dialog — clinician radio-list, drug formulary list (each row live-flagged "Allergy Conflict" in red if it matches the patient's recorded allergies), dosage/frequency/duration/instructions fields (pre-filled from the selected drug, editable), an allergy warning banner with a required override checkbox + justification field when a conflict exists, Share/Sign & Issue actions
- colors: allergy-conflict row border/text `ThornburyError`; override banner `ThornburyErrorWash`
- typography/icons: consistent with the rest of the app; `Medication`, `Share`, `Warning`, `Check`
- states: Sign & Issue is disabled unless there's no conflict, or the conflict is explicitly overridden with a non-blank justification (verified correct in code)
**Backend**
- data_source: `DentalRepository.prescriptions` filtered by patient; `DentalRepository.checkAllergyConflict(patient, drugName)` (real logic: checks the patient's recorded `Allergy` list against the selected drug, including penicillin/amoxicillin cross-reactivity and NSAID-class matching) backs both the per-row conflict flag and the warning banner
- data_shape: `Prescription { id, patientId, patientName, clinicianName, drugName, dosage, frequency, duration, instructions, issueDate, isDispensed }`
- state_management: dialog-local drug/clinician/dosage selection, `remember(drug)`-keyed so changing drugs resets dosage fields to that drug's defaults
- interactions: select clinician/drug/dosage/etc.; "Share" builds a plain-text prescription slip and opens the Android share sheet; "Sign & Issue" calls `DentalRepository.issuePrescription(...)`
- business_logic: allergy cross-check is real and reasonably thorough (see above); Sign & Issue gating on override+justification is correctly implemented
- edge_states: none needed
- dependencies: `Navigation.kt` (this dialog is also opened directly from the root nav, not only from within this tab)
- permissions: none
**Gaps**
- [HIGH] `PrescriptionsManagementScreen.kt` — a complete, separate "prescriptions across all patients" screen (drug-category filtering, its own patient-picker, its own call into `IssuePrescriptionDialog`) — has **zero callers anywhere in the codebase**; the actual Prescriptions tab content is the inline `PatientPrescriptionsView` in `PatientDetailChartScreen.kt` instead. This is the same dead-screen pattern as `RegisterPatientDialog` and `AddReportDialog`, but for a whole file rather than one composable. — evidence: `PrescriptionsManagementScreen.kt:43` is the only occurrence of that name in the entire repo — fix: delete the file, or wire it in (e.g. as a practice-wide "All Prescriptions" view) if that capability is actually wanted.
- [MEDIUM] The "Share" button builds and shares a prescription slip with no gating at all — it doesn't check `allergyWarning`/override state the way "Sign & Issue" does, so a clinician could share a prescription slip for a drug with a known, unoverridden allergy conflict, and the shared text carries no warning that it was never actually signed/issued. — evidence: `IssuePrescriptionDialog.kt:319-352` (no `enabled=` condition, unlike `Sign & Issue` at line 368) — fix: gate Share behind the same allergy-override condition, or clearly label shared-but-unsigned slips as drafts.

### Component: Visit History
Shared: no
Complexity: medium
**Design**
- layout: header + "Book Follow-up" button, `LazyColumn` of appointment `Card`s (procedure title, status badge with dropdown, time/room/clinician lines, conditional allergy banner) or an empty state with its own "Schedule Initial Visit" CTA
- colors: status badge 3-way (`completed`=success wash, `confirmed`=primary wash, else=neutral); allergy banner error-wash
- typography/icons: consistent; `EventAvailable`, `ArrowDropDown`
- states: empty state; per-row status dropdown (`DropdownMenu`) letting any status be picked directly, not just a linear progression
**Backend**
- data_source: `DentalRepository.appointments` filtered by patient
- data_shape: full `Appointment` record — same model/gaps already documented in the Schedule Tab Report (no date field, `time` sorted lexicographically)
- state_management: `statusPickerApptId` (which row's dropdown is open)
- interactions: tap status badge → dropdown of `confirmed`/`completed`/`cancelled` → `onUpdateStatus`; "Book Follow-up" → `BookAppointmentDialog` (shared with the Schedule tab, see that report)
- business_logic: none beyond direct status assignment
- edge_states: empty state
- dependencies: `BookAppointmentDialog` (Schedule tab's component)
- permissions: none — any status can be set from any status via the dropdown, including reopening a `cancelled` visit back to `confirmed` with no confirmation step
**Gaps**
- [LOW] Unlike the Today/Schedule tabs' cancel action (which requires confirming in a dialog), this view's status dropdown lets a `confirmed` appointment be set straight to `cancelled` (or back again) with a single tap and no confirmation — a mis-tap silently changes a visit's status. — evidence: `PatientDetailChartScreen.kt:1560-1583` — fix: reuse the same confirmation-dialog pattern already used elsewhere in the app for cancellation.

## Tab-Level Notes
- Navigation flow: Roster → (select) → Patient Chart (6 tabs) or → (Register Patient) → `RegisterPatientScreen`. Today and Schedule tabs also enter the Patient Chart directly via `onOpenPatientChart`.
- Shared state: `DentalRepository.patients/appointments/treatmentPlans/prescriptions/reports`, all app-wide `StateFlow`s.
- Shared/external components referenced but not spec'd here: bottom `NavigationBar` (all tabs); `BookAppointmentDialog` (fully specified in the Schedule Tab Report; also used here); `AnatomicalToothVisual.kt` (shared tooth-rendering primitive, used correctly by both quadrant files).
- **Systemic dead-code pattern**: three separate "modal dialog" implementations in this tab (`RegisterPatientDialog`, `AddReportDialog`, `PrescriptionsManagementScreen`) are fully unreachable, each superseded by a working full-page/inline equivalent. All three look intentionally built and complete, not stubs — this strongly suggests a past refactor (dialogs → full-page screens, or a merge that inlined the tab-view logic into `PatientDetailChartScreen.kt`) that never cleaned up the components it replaced. Recommend deleting all three rather than fixing them individually.
- **Systemic hardcoded-clinician pattern**: `"Dr. Ingrid Halvorsen"` is hardcoded as the acting clinician in at least 4 places across this tab alone (`CreateTreatmentPlanDialog.kt:41`, `AddReportScreen.kt:70`, `AddReportDialog.kt:48` [dead], plus the same pattern already found in `TodayQueueScreen.kt` and `ProfileScreen.kt` in their own reports) — while `IssuePrescriptionDialog.kt` shows the correct pattern (a real clinician picker). One shared fix (a small util reading `AuthRepository.currentUser`, or reusing `IssuePrescriptionDialog`'s picker) would resolve all of these at once.
- **Systemic DOB→age duplication**: a third copy of the age-from-DOB calculation lives in `PatientDemographicsM3View.kt` (`calculatePatientAge`), in addition to the two already flagged in the Today/Schedule reports — this copy is the best-behaved of the three (no fake fallback age).

## Gap Summary (sorted High→Low)
- [HIGH — patient safety] Demographics "BLOOD GROUP" fabricates a specific wrong value ("O+") when unknown, instead of showing "Unknown" — `PatientDemographicsM3View.kt:1182-1186`
- [HIGH] Demographics "GENDER" is guessed from a 5-name hardcoded list, defaulting to "Adult" — `PatientDemographicsM3View.kt:1172-1180`
- [HIGH] Report Viewer Lightbox never shows the real uploaded image/attachment — always draws a fake radiograph — `ReportViewerLightboxDialog.kt` (whole file)
- [HIGH] `RegisterPatientDialog` is ~200 lines of unreachable dead code (superseded by `RegisterPatientScreen`) — `PatientRosterScreen.kt:42,275-283,366-573`
- [HIGH] `AddReportDialog.kt` is unreachable dead code (superseded by `AddReportScreen`) — `PatientDetailChartScreen.kt:55,225-241`, `AddReportDialog.kt`
- [HIGH] `PrescriptionsManagementScreen.kt` is an entire unreachable dead file (superseded by inline `PatientPrescriptionsView`) — `PrescriptionsManagementScreen.kt:43`
- [HIGH] Registration's Gender/Emergency Contact fields are collected in the UI but never passed to `registerPatient()` — silently discarded — `RegisterPatientScreen.kt:36,42-43,425-435`
- [HIGH] `CreateTreatmentPlanDialog`'s clinician is hardcoded with no picker, unlike its sibling `IssuePrescriptionDialog` — `CreateTreatmentPlanDialog.kt:41`
- [HIGH] `AddReportScreen`'s clinician is hardcoded (live, correctly-built screen otherwise) — `AddReportScreen.kt:70`
- [MEDIUM] Patient Chart silently substitutes a different patient if the requested `patientId` isn't found, instead of showing "not found" — `PatientDetailChartScreen.kt:45`
- [MEDIUM] Treatment plan steps never carry a tooth number and use fabricated procedure codes — `CreateTreatmentPlanDialog.kt:280-289`
- [MEDIUM] "Share" in the prescription dialog bypasses the allergy-override gate that "Sign & Issue" enforces — `IssuePrescriptionDialog.kt:319-352`
- [LOW] Unused `Patient`-accepting overloads in the odontogram quadrant files — `MaxillaryQuadrantViews.kt:347-360,490-503,619-637`
- [LOW] Visit History lets any appointment status be changed to any other with one tap, no confirmation — `PatientDetailChartScreen.kt:1560-1583`
