# Tab Report: Today

Source: `android/app/src/main/java/com/example/thornburydental/ui/clinic/TodayQueueScreen.kt` (792 lines, single file, no ViewModel).

**Note:** this file contains its own private copies of `calculateAge()` and `formatTimeWithAmPm()`, byte-for-byte-duplicated (mostly) from `ScheduleScreen.kt` — already logged as a Low-severity duplication gap in the Schedule Tab Report. Analyzing this file in full revealed that the two `formatTimeWithAmPm()` copies have actually **drifted** — see Gap A below, now a High-severity, tab-defining bug caused directly by that duplication.

## Components

### Component: Header (greeting, date, status pill)
Shared: no
Complexity: low
**Design**
- layout: bordered full-width `Surface` → `Row` (greeting/date `Column` weight 1f + trailing pill `Surface`)
- spacing: 20dp horizontal / 18dp vertical container padding, 4dp between greeting and date lines
- colors: surface `ThornburyCanvas`/`ThornburyHairline` border; greeting `ThornburyInk`; date line `ThornburyMuted`; pill `ThornburySurfaceSoft`/`ThornburyHairline`, pill text `ThornburyPrimaryText`
- typography: greeting `titleLarge` bold; date line `bodyMedium`; pill `labelSmall` bold
- icons: none
- surface: flat bordered surface, pill fully rounded
- states: static — greeting text itself varies by time-of-day (see Backend)
**Backend**
- data_source: `Calendar.getInstance()` for the hour-of-day only; `DentalRepository.appointments` (via parent) for the pill counts
- data_shape: pill reads `confirmedList.size` / `completedList.size`, both live-derived
- state_management: `greeting` is a plain computed `val` (not state) re-evaluated each recomposition from `Calendar.getInstance().get(HOUR_OF_DAY)`
- interactions: none (not tappable)
- business_logic: `greeting` = "Good morning"/"Good afternoon"/"Good evening" by hour buckets (<12, <17, else)
- edge_states: none
- dependencies: should depend on `AuthRepository` for the clinician name; does not currently
- permissions: none
**Gaps**
- [HIGH] The date subtitle is a hardcoded literal, `"Thursday, 10 September • Surgery 1"` — on the tab whose entire purpose is "what's happening today," it never reflects the real current date, on any day. `Calendar.getInstance()` is already imported and used two lines away (for the greeting) and inside `calculateAge()`, so there's no technical barrier to formatting the real date here. — evidence: `TodayQueueScreen.kt:138` — fix: format the current date (e.g. via `Calendar`/`java.time.LocalDate`) into the same `"EEEE, d MMMM"` style string.
- [MEDIUM] The greeting hardcodes `"Dr. Ingrid Halvorsen"` instead of reading the signed-in user — same root cause as the Profile tab's Gap 1 (`AuthRepository.currentUser` is available app-wide but unused here too). — evidence: `TodayQueueScreen.kt:130` — fix: bind to `AuthRepository.currentUser?.name`.

### Component: Next Patient in Chair (spotlight card)
Shared: no
Complexity: medium
**Design**
- layout: `ElevatedCard` (only rendered when a confirmed appointment exists) → header `Row` ("NEXT IN CHAIR" pill + time/duration badge), patient name/age/OP `Row`, procedure+room line, action button `Row`
- spacing: 18dp card padding, 20dp horizontal card margin, 14/16dp internal spacers
- colors: card `ThornburySurfaceCard`, 2dp elevation; "NEXT IN CHAIR" pill `ThornburyPrimary`/`ThornburyOnPrimary`; time badge `ThornburyCanvas`/`ThornburyHairline`; OP chip `ThornburySurfaceSoft`
- typography: pill label `labelSmall` bold, 1.2sp letter-spacing; time badge `labelSmall` bold, 0.5sp letter-spacing; patient name `titleMedium` bold; procedure line `bodyMedium`
- icons: `FolderOpen` (Open Chart button), `Check` (Mark Seen button, tinted success)
- surface: card 16dp radius; badges 6-9999dp radius depending on shape
- states: entire card is conditionally rendered (only shown if a confirmed appointment exists) — no explicit "none scheduled" variant of the card itself (the section below it independently shows its own empty state)
**Backend**
- data_source: `nextAppt = confirmedList.firstOrNull()`, derived from `DentalRepository.appointments`
- data_shape: full `Appointment` record
- state_management: `nextAppt` is a `remember(confirmedList)`-derived value, not independent state
- interactions: tapping the name row or "Open Chart" → `onOpenPatientChart(nextAppt.patientId)`; "Mark Seen" → `DentalRepository.updateAppointmentStatus(nextAppt.id, "completed")`
- business_logic: "next" = the first item in `confirmedList`, i.e. whatever order the appointments arrive in — see Gap below
- edge_states: card simply doesn't render if there is no confirmed appointment (no dedicated "nothing next" message, which is reasonable since the list below still communicates that)
- dependencies: `Navigation.kt`'s patient-chart navigation (Patients tab); `AppointmentDao`'s ordering (shared backend)
- permissions: none
**Gaps**
- [MEDIUM] "Next in chair" is simply `confirmedList.firstOrNull()` — the first confirmed appointment in whatever order `DentalRepository.appointments` returns them, not "the confirmed appointment soonest from now." Combined with the appointment-sort bug already identified in the Schedule Tab Report (`AppointmentDao.kt:27` sorts the `time` TEXT column lexicographically, so e.g. `"02:00 PM"` sorts ahead of `"11:30 AM"`), the patient spotlighted here as clinically "next" can be wrong. — evidence: `TodayQueueScreen.kt:78,82` + `AppointmentDao.kt:27` — fix: once appointment time/date storage is fixed (see Schedule Tab Report), derive "next" as the earliest confirmed appointment at or after the current time.

### Component: Search & Filter Chips
Shared: no
Complexity: medium
**Design**
- layout: `Column` → pill `OutlinedTextField` + horizontally-scrolling `FilterChip` row (4 status options)
- spacing: 20dp horizontal padding, 16dp above, 12dp between field and chips, 8dp between chips
- colors: search focused border `ThornburyPrimary`/unfocused `ThornburyHairline`; selected chip `ThornburyPrimary`/`ThornburyOnPrimary`; unselected `ThornburySurfaceSoft`/`ThornburyBody`
- typography: chip label `labelSmall`, bold when selected; search placeholder `bodyMedium`
- icons: `Search` (leading), `Close` (trailing, conditional on non-empty query)
- surface: search field 24dp pill radius; chips 20dp radius
- states: search empty/non-empty; chip selected/unselected
**Backend**
- data_source: `DentalRepository.appointments` (via parent)
- data_shape: filters on `status`, and free-text match across `patientName`/`patientOpNo`/`procedure`/`room`
- state_management: 2 screen-local `remember` states (`filterMode`, `searchQuery`); combined into `filteredAppointments` via `remember(appointments, filterMode, searchQuery)`
- interactions: type to search; tap a status filter chip ("All"/"Upcoming"/"Seen"/"Cancelled", each with a live count)
- business_logic: case-insensitive substring match on 4 fields; "all" sentinel bypasses the status filter
- edge_states: none of its own (feeds the list/empty-state section)
- dependencies: feeds the Appointment List Item Card
- permissions: none
**Gaps**
none found — same solid pattern as the equivalent Schedule tab component, correctly live-counted.

### Component: "Today's Schedule" Section Header
Shared: no
Complexity: low
**Design**
- layout: `Row` — title left, count pill right
- spacing: 20dp horizontal / 14dp vertical padding
- colors: title `ThornburyInk`; pill `ThornburySurfaceSoft`/`ThornburyHairline`, text `ThornburyPrimaryText`
- typography: title `titleMedium` bold; pill `labelSmall` bold
- icons: none
- surface: pill fully rounded
- states: static label, live count
**Backend**
- data_source: `filteredAppointments.size` (from the Search & Filter component above)
- data_shape: n/a
- state_management: none (pure derived display)
- interactions: none
- business_logic: none
- edge_states: none
- dependencies: Search & Filter Chips (upstream)
- permissions: none
**Gaps**
none found

### Component: Appointment List Item Card (+ empty state)
Shared: no
Complexity: high
**Design**
- layout: `LazyColumn` of `OutlinedCard`s; each: top `Row` (time badge + status pill), patient/OP/procedure block, divider-free bottom action `Row`. Empty state: centered icon + title + body in its own `OutlinedCard`, with distinct copy for "search yielded nothing" vs "filter yielded nothing."
- spacing: cards 20dp horizontal/4dp vertical margin, 14dp internal padding, 10-12dp internal spacers, actions 6dp apart
- colors: card container `ThornburySurfaceCard` when confirmed else `ThornburyCanvas`; status pill 3-way color coded (confirmed→success wash, completed→info/teal wash, cancelled→error wash); empty-state icon `ThornburyMuted`
- typography: patient name `titleMedium` bold; OP/room `labelSmall` monospace; procedure/clinician `bodySmall`; status pill `labelSmall` bold 11sp
- icons: `FolderOpen`/`Check`/`Close` (actions), `Check` (Seen pill), `EventBusy` (empty state)
- surface: cards 12dp radius, hairline vs soft-hairline border depending on confirmed state
- states: empty (two variants: search vs filter), populated; per-row action buttons conditional on `status == "confirmed"`
**Backend**
- data_source: `filteredAppointments` (derived above)
- data_shape: full `Appointment` record per row
- state_management: reads shared state; row actions call `DentalRepository.updateAppointmentStatus` directly, or set `appointmentToCancel` to open the confirmation dialog
- interactions: tap name/row or "Chart" → `onOpenPatientChart`; "Mark Seen" → status→`completed`; "Cancel" → opens Cancellation Confirmation Dialog
- business_logic: `calculateAge(row.patientDob)` and `formatTimeWithAmPm(row.time)` computed inline per row (see Gap A — the latter is broken in this file)
- edge_states: two distinct empty-state messages depending on whether a search query or just a filter tab produced zero results — a nicer touch than the Schedule tab's single generic empty message
- dependencies: `Navigation.kt` (chart), Cancellation Confirmation Dialog (below)
- permissions: none
**Gaps**
- [HIGH] This file's private `formatTimeWithAmPm()` (`TodayQueueScreen.kt:54-65`) is missing the "already-formatted" guard present in the near-identical copy in `ScheduleScreen.kt:54-69`. Since `Appointment.time` is always already stored as a 12-hour string with an AM/PM suffix (e.g. `"09:30 AM"`, `"02:00 PM"` — see `BookAppointmentDialog.kt`'s defaults/quick-picks), this function re-parses that string as if it were bare 24-hour `"HH:MM"`: it splits on `:` giving `minute = "30 AM"` (the AM/PM suffix stays glued to the minute part) and then computes and appends its *own* `amPm` suffix from `hour < 12`. For a morning time like `"09:30 AM"` this renders `"09:30 AM AM"` (doubled). For an afternoon time like `"02:00 PM"` it renders `"02:00 PM AM"` — both suffixes present, the appended one wrong. This is visible on **every appointment time shown on this tab** (the Next-in-Chair spotlight, `line 219`, and every list row, `line 548`). — evidence: `TodayQueueScreen.kt:54-65` (no guard) vs `ScheduleScreen.kt:54-69` (has `if (cleanTime.uppercase().contains("AM") || contains("PM")) return cleanTime`) — fix: copy the guard from `ScheduleScreen.kt`, or (better, addressing the root cause) delete both private copies and share one corrected implementation, per the dedup fix already suggested in the Schedule Tab Report.

### Component: Cancellation Confirmation Dialog (modal)
Shared: no
Complexity: low
**Design**
- layout: `AlertDialog` — title, body sentence, Confirm/Keep row
- spacing: default `AlertDialog` insets
- colors: container `ThornburyCanvas`; confirm button `ThornburyError`/white
- typography: title `titleMedium` bold; body `bodyMedium`
- icons: none
- surface: 12dp radius
- states: presence/absence only
**Backend**
- data_source: none — operates on the `appointmentToCancel` snapshot
- data_shape: reads `patientName`, `time` (raw, not through the buggy formatter), `room`
- state_management: screen-local nullable `appointmentToCancel`
- interactions: "Cancel Appointment" → `updateAppointmentStatus(id, "cancelled")`; "Keep Appointment" → dismiss
- business_logic: none beyond the status transition
- edge_states: uses `!!` on `appointmentToCancel` inside the dialog body (line 754) — safe in practice since the dialog is only composed `if (appointmentToCancel != null)`, but worth noting as a style inconsistency versus the Schedule tab's equivalent dialog, which uses safe-call (`?.`) throughout instead of an upfront `!!`
- dependencies: `DentalRepository.updateAppointmentStatus`
- permissions: none
**Gaps**
none found — the `!!` at line 754 is guarded by the enclosing `if` and cannot actually throw; not counted as a real defect, noted only for style parity with Schedule's version.

## Tab-Level Notes
- Navigation flow: Header/spotlight/list all funnel into `onOpenPatientChart`, owned by the Patients Tab Agent. No booking flow exists on this tab (that's Schedule's job) — Today is purely a same-day triage/status view.
- Shared state: reads the same `DentalRepository.appointments` StateFlow as the Schedule tab.
- Shared/external components referenced but not spec'd here: bottom `NavigationBar` (all tabs); `PatientDetailChartScreen` (Patients tab); `AppointmentDao`/`DentalRepository` (shared backend, and the source of the cross-tab sort-order gap referenced above).
- Cross-cutting observation: this file and `ScheduleScreen.kt` are near-clones of each other (same two private helper functions, same card/badge/dialog structure) built independently rather than sharing a common list-item component — the `formatTimeWithAmPm` drift documented in Gap A is a direct, now-materialized consequence of that duplication.

## Gap Summary (sorted High→Low)
- [HIGH] `formatTimeWithAmPm()` in this file lacks the guard its `ScheduleScreen.kt` sibling has, so every appointment time on this tab renders with a doubled/wrong AM-PM suffix (e.g. "09:30 AM AM") — `TodayQueueScreen.kt:54-65` vs `ScheduleScreen.kt:54-69`
- [HIGH] Header date is a hardcoded literal ("Thursday, 10 September") that never reflects the real date — `TodayQueueScreen.kt:138`
- [MEDIUM] "Next in Chair" picks the first confirmed appointment rather than the chronologically-soonest one, compounded by the cross-tab appointment-sort bug — `TodayQueueScreen.kt:78,82`, `AppointmentDao.kt:27`
- [MEDIUM] Greeting hardcodes "Dr. Ingrid Halvorsen" instead of `AuthRepository.currentUser` — `TodayQueueScreen.kt:130`
