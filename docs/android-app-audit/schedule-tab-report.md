# Tab Report: Schedule

Source: `ScheduleScreen.kt` (800 lines) + `BookAppointmentDialog.kt` (392 lines), both under `android/app/src/main/java/com/example/thornburydental/ui/clinic/`. No ViewModel — screen-local `remember` state plus direct reads of `DentalRepository`'s `StateFlow`s.

## Components

### Component: Schedule Header (title + Book Appointment CTA)
Shared: no
Complexity: low
**Design**
- layout: full-width `Surface` → `Row` (title/subtitle `Column` weighted 1f, + trailing filled `Button`)
- spacing: 20dp horizontal / 18dp vertical container padding; 12dp gap before button
- colors: surface `ThornburyCanvas` with `ThornburyHairline` 1dp border; title `ThornburyInk`, subtitle `ThornburyMuted`; button container `ThornburyPrimary`/white content
- typography: title `titleLarge` bold; subtitle `bodyMedium`; button label `labelMedium` bold
- icons: `Icons.Default.Add` on the button
- surface: no radius (flat bordered surface), button 10dp radius
- states: static header; button has only the default Material3 press state
**Backend**
- data_source: none itself — button opens the Select Patient modal
- data_shape: n/a
- state_management: sets `showSelectPatientDialog = true`
- interactions: "Book Appointment" → opens Select Patient Dialog
- business_logic: none
- edge_states: none
- dependencies: Select Patient Dialog, Book Appointment Dialog (downstream of this button)
- permissions: none
**Gaps**
none found

### Component: Search & Filter Section
Shared: no
Complexity: medium
**Design**
- layout: `Column` → pill search `OutlinedTextField`, then two labeled horizontally-scrolling `FilterChip` rows (Surgery Room, Appointment Status)
- spacing: 20dp horizontal/14dp vertical container padding, 12dp between the three blocks, 4dp label-to-chips gap, 8dp between chips
- colors: search field focused border `ThornburyPrimary` / unfocused `ThornburyHairline`; selected chip `ThornburyPrimary` container + white label; unselected chip `ThornburySurfaceSoft`/`ThornburyInk`
- typography: section labels `labelSmall` bold muted; chip labels default `FilterChip` text style; search placeholder `bodyMedium`
- icons: `Icons.Default.Search` (leading), `Icons.Default.Close` (trailing, only when non-empty)
- surface: search field 24dp pill radius; chips 12dp radius
- states: search field empty/non-empty (toggles clear icon); chips selected/unselected
**Backend**
- data_source: `DentalRepository.appointments` (via parent) — the room filter's "All Surgeries (${appointments.size})" label reads the unfiltered count live
- data_shape: filters against `Appointment.room`, `Appointment.status`, and a free-text match across `patientName`/`patientOpNo`/`procedure`/`clinicianName`/`room`
- state_management: 3 independent screen-local `remember` states (`searchQuery`, `roomFilter`, `statusFilter`); combined via a `remember(appointments, roomFilter, statusFilter, searchQuery)` derived `filteredAppointments`
- interactions: type to search; tap a room chip; tap a status chip — all three combine with AND logic
- business_logic: case-insensitive substring match on 5 fields for search; exact-match filtering for room/status; "all" sentinel bypasses each filter
- edge_states: none of these three controls have their own empty/loading state — the combined empty result is handled one level up by the list section
- dependencies: feeds the Appointment List Item Card below
- permissions: none
**Gaps**
none found — filtering logic is straightforward and internally consistent.

### Component: Appointment List Item Card (+ empty state)
Shared: no
Complexity: high
**Design**
- layout: `LazyColumn` of `OutlinedCard`s; each card: top `Row` (time/duration badge left, room badge + status pill right), patient/procedure/clinician block, conditional allergy banner, divider, bottom action-button `Row`. Empty state: centered icon + title + body text in its own `OutlinedCard`.
- spacing: cards 20dp horizontal/6dp vertical margin, 16dp internal padding; 12dp/14dp/10dp internal spacers; action buttons 6dp apart
- colors: card `ThornburySurfaceCard`/`ThornburyHairline`; time badge `ThornburySurfaceSoft`; room badge `ThornburyPrimaryWash`; status pill color-coded (`completed`→success wash/text, `cancelled`→error wash/text, else→primary wash/text); allergy banner `ThornburyErrorWash`/`ThornburyError`; empty-state icon `ThornburyMutedSoft`
- typography: patient name `titleMedium` bold; age/OP/clinician `bodySmall`/`bodyMedium` muted; procedure `bodyMedium` semibold; badges/pill `labelSmall` bold
- icons: `Schedule`, `MedicalServices`, `Person`, `Warning` (conditional), `FolderOpen`/`Check`/`Close` on action buttons, `EventBusy` for empty state
- surface: cards 16dp radius, 1dp hairline border; badges 8dp radius, status pill fully rounded
- states: empty (no matches), populated list, and per-row conditional allergy banner + status-dependent action buttons (Mark Seen/Cancel only shown when `status == "confirmed"`)
**Backend**
- data_source: `DentalRepository.appointments` (`collectAsState()`), filtered by the section above
- data_shape: `Appointment { id, patientId, patientName, patientOpNo, patientDob, clinicianId, clinicianName, time, durationMin, room, procedure, allergyList?, status }` — **no date field, only a free-text `time` string**
- state_management: reads the shared `appointments` StateFlow; row actions call `DentalRepository.updateAppointmentStatus` directly (no local state)
- interactions: tap patient name/row or "Chart" button → `onOpenPatientChart(appt.patientId)` (navigates into the Patients-tab-owned chart); "Mark Seen" → `updateAppointmentStatus(id, "completed")`; "Cancel" → opens the Cancellation Confirmation Dialog
- business_logic: `calculateAge(patientDob)` computed inline per row; `formatTimeWithAmPm(time)` computed inline per row; status→label/color mapping inline
- edge_states: empty state shown when `filteredAppointments.isEmpty()`; allergy banner conditionally shown when `allergyList` is non-blank; action row conditionally shown only for `"confirmed"` status
- dependencies: `Navigation.kt`'s `activePatientId` (via `onOpenPatientChart`), owned by the Patients Tab Agent; the Cancellation Confirmation Dialog (below)
- permissions: none — any signed-in user sees full read/write access to every appointment (no clinician-scoping)
**Gaps**
- [HIGH] `Appointment` has no date field at all (`data/Models.kt:192-206`) — only a bare `time: String` such as `"09:30 AM"`. Every appointment ever booked, for any day, is stored and displayed with no notion of which calendar day it falls on; the "Schedule" list can only ever show one undifferentiated pool of times-of-day, not a real per-day schedule. `BookAppointmentDialog` has no date picker either, despite a reusable `ThornburyDatePickerField` already existing in the codebase. — evidence: `Models.kt:192-206`, `BookAppointmentDialog.kt` (no date field present), `ThornburyDatePickerField.kt` (unused here) — fix: add a `date` field to `Appointment` (+ DB column/migration), a date picker to the booking dialog, and date-aware filtering/grouping to the schedule list.
- [HIGH] `AppointmentDao.getAllAppointments()` sorts with `ORDER BY time ASC` (`AppointmentDao.kt:27`) where `time` is a `TEXT` column (`ThornburyDbHelper.kt:202`) storing strings like `"02:00 PM"`/`"11:30 AM"`. SQLite's default text ordering is lexicographic, not chronological, so e.g. `"02:00 PM"` sorts *before* `"11:30 AM"` (`'0' < '1'`) even though 11:30 AM is earlier in the day — any afternoon appointment with a single-digit hour (1–9 PM) will sort out of order ahead of all 10 AM/11 AM/10 PM/11 PM entries. Verifiable directly from the seed times used elsewhere in this file (`commonTimes` in `BookAppointmentDialog.kt:63`: `"09:00 AM", "09:30 AM", "10:15 AM", "11:30 AM", "02:00 PM", "03:45 PM", "04:30 PM"` — lexicographically these sort as `02:00 PM, 03:45 PM, 04:30 PM, 09:00 AM, ...`, not chronological order). — evidence: `AppointmentDao.kt:27`, `ThornburyDbHelper.kt:202` — fix: store time in 24-hour zero-padded form (or a real timestamp once the date gap above is fixed) so lexicographic and chronological order coincide.
- [MEDIUM] The "Time of Visit" field in `BookAppointmentDialog` is free-text (`OutlinedTextField`) with only an `isNotBlank()` guard on save (`BookAppointmentDialog.kt:356,367`) — a clinician can type anything ("asdf", "3", etc.) and it will be saved verbatim as the appointment's displayed/sorted time, silently producing malformed schedule entries (`formatTimeWithAmPm`'s catch-all would just pass the garbage through unchanged). — evidence: `BookAppointmentDialog.kt:232-240,356,367` — fix: validate against an actual time format (or replace with a real time-picker) before enabling Confirm.
- [LOW] `calculateAge()` and `formatTimeWithAmPm()` are byte-for-byte duplicated, private, top-level functions in both `ScheduleScreen.kt:32-52,54-69` and `TodayQueueScreen.kt:32-52,54-69` — same logic (including the same silent `38`-year-old fallback on unparseable DOB) maintained in two places that will silently drift if only one copy is ever fixed. — evidence: both files, lines 32-69 — fix: move both to a shared util file (e.g. `util/DateFormatting.kt`) and have both screens import it.

### Component: Select Patient Dialog (modal)
Shared: no
Complexity: medium
**Design**
- layout: `AlertDialog` → title, search field + scrollable `LazyColumn` of patient rows (or empty-state text), dismiss-only footer
- spacing: 380dp max height text area; 12dp before list; rows spaced 6dp apart, 12dp internal row padding
- colors: dialog container `ThornburyCanvas`; row surface `ThornburySurfaceSoft`/`ThornburyHairlineSoft`; row name `ThornburyInk` bold, meta `ThornburyMuted`; chevron `ThornburyPrimary`
- typography: dialog title `titleLarge` bold; row name `bodyMedium` bold; row meta `bodySmall`
- icons: `Icons.Default.Search` (field), `Icons.Default.ChevronRight` (per row)
- surface: dialog 18dp radius; rows 10dp radius
- states: empty ("No registered patients match your search.") vs populated list
**Backend**
- data_source: `DentalRepository.patients` (`collectAsState()`, read from the parent screen)
- data_shape: filters on `Patient.name` / `Patient.opNo`
- state_management: dialog-local `patientSearchQuery`; derived `filteredPatients` via `remember(patients, patientSearchQuery)`
- interactions: tap a patient row → sets `bookingTargetPatient` and closes this dialog (opens Book Appointment Dialog); "Close" → dismiss with no selection
- business_logic: case-insensitive substring match on name or OP number
- edge_states: empty state is text-only, no call to action
- dependencies: Book Appointment Dialog (opens next), `PatientRosterScreen`'s registration flow (not reachable from here)
- permissions: none
**Gaps**
- [MEDIUM] When a search matches no registered patients, the empty state is a dead end — there's no "Register new patient" action from here, forcing the clinician to close the dialog, switch to the Patients tab, register the patient, then return to Schedule and start booking over. — evidence: `ScheduleScreen.kt:669-681` (text-only empty state, no button) — fix: add a CTA that routes into `RegisterPatientScreen` (Patients tab), ideally returning here with the new patient pre-selected.

### Component: Book Appointment Dialog
Shared: yes (also used by `PatientDetailChartScreen.kt`, Patients tab)
Complexity: high
**Design**
- layout: `Dialog` → `Card` → scrollable `Column`: header (title/patient + close), procedure field + quick-pick chips, clinician radio-list, time field + duration quick-picks + time quick-pick chips, room `FilterChip` row, Cancel/Confirm action row
- spacing: 20dp card padding; 14dp between major sections; 6dp label-to-field gaps
- colors: card `ThornburyCanvas`/`ThornburyHairline`; selected clinician row `ThornburySurfaceSoft`/`ThornburyPrimary` border; selected duration/room chip `ThornburyPrimary` container white text; Confirm button `ThornburyPrimary`, disabled state `ThornburyPrimaryDisabled`/`ThornburyMuted`
- typography: dialog title `titleLarge` bold; section labels `labelMedium` bold; clinician name `bodySmall` (bold if selected); chip text `labelSmall`
- icons: `Icons.Default.Close` (dismiss), `Icons.Default.CalendarMonth` (Confirm button)
- surface: card 18dp radius; inner controls 8–12dp radius
- states: Confirm button disabled unless `procedure` and `time` are both non-blank; clinician/duration/room selection states
**Backend**
- data_source: `DentalRepository.clinicians` (static `List<Clinician>`, read once, not a reactive flow — confirmed safe since it never changes at runtime)
- data_shape: `Clinician { id, name, credentials, specialty, room, bio, phone }`; emits `(procedure, clinicianId, clinicianName, time, durationMin, room)` via `onSave`
- state_management: 4 dialog-local `remember` states (`selectedClinician`, `procedure`, `time`, `durationMin`, `room`) — `room` auto-updates to the selected clinician's default room, but can be independently overridden after
- interactions: edit/quick-pick procedure text; select clinician (radio or row tap, also resets room); edit/quick-pick time; pick duration (only first 3 of 4 `durationOptions` are ever shown — see gap); pick room chip; Cancel (dismiss); Confirm (validates, calls `onSave`)
- business_logic: Confirm is enabled only when `procedure.isNotBlank() && time.isNotBlank()`; selecting a clinician resets `room` to that clinician's default
- edge_states: none (no loading/error state; this is a synchronous local form)
- dependencies: caller supplies `patient` and `onSave`; used by both `ScheduleScreen` and `PatientDetailChartScreen`
- permissions: none
**Gaps**
- [MEDIUM] `durationOptions = listOf(30, 45, 60, 90)` but the UI only ever renders `durationOptions.take(3)` (`BookAppointmentDialog.kt:64,254`) — the 90-minute option is defined but unreachable through the UI (e.g. for longer implant-surgery bookings). — evidence: `BookAppointmentDialog.kt:64,254` — fix: render all 4 options (adjust layout to wrap or scroll) or remove the unused `90` from the list.
- [MEDIUM] No date field anywhere in this dialog (see the tab-level date gap above) — every booking made here is time-of-day only. — evidence: whole file, no date control present.
- [LOW] Time input is the same unvalidated free-text field described in the Appointment List Item Card gaps above (shared root cause, called out there).

### Component: Cancellation Confirmation Dialog (modal)
Shared: no
Complexity: low
**Design**
- layout: `AlertDialog` — title, descriptive body sentence, Confirm/Keep button row
- spacing: default `AlertDialog` insets
- colors: container `ThornburyCanvas`; Confirm button `ThornburyError`/white; body text `ThornburyBody`
- typography: title `titleMedium` bold; body `bodyMedium`
- icons: none
- surface: 16dp radius
- states: none beyond presence/absence
**Backend**
- data_source: none — operates on the `appointmentToCancel` snapshot passed in
- data_shape: reads `patientName`, `procedure`, `time`, `room` from the target `Appointment`
- state_management: screen-local `appointmentToCancel` nullable state, set by the row's Cancel button
- interactions: "Confirm Cancel" → `DentalRepository.updateAppointmentStatus(id, "cancelled")`; "Keep Appointment" → dismiss with no change
- business_logic: none beyond the status transition
- edge_states: `formatTimeWithAmPm(appointmentToCancel?.time ?: "")` guards a null target defensively
- dependencies: `DentalRepository.updateAppointmentStatus`
- permissions: none
**Gaps**
none found — small, self-contained, correctly guards its nullable input.

## Tab-Level Notes
- Navigation flow: Header → Select Patient Dialog → Book Appointment Dialog → back to list (booking persisted via `DentalRepository.bookAppointment`). Each list row can also navigate out to the Patients-tab-owned Patient Chart via `onOpenPatientChart`.
- Shared state: reads `DentalRepository.appointments` and `DentalRepository.patients`, both shared app-wide StateFlows.
- Shared/external components referenced but not spec'd here: `PatientDetailChartScreen` (Patients tab, entered via "Chart"/row tap); `BookAppointmentDialog` is itself shared *with* the Patients tab (also launched from the patient chart) — specified once here in full since Schedule is its primary/first-registered caller, Patients tab should just reference it.
- Cross-cutting observation: the complete absence of a `date` concept on `Appointment` is the dominant finding for this entire tab — it undermines the premise of a "Schedule," not just one component, and every other gap involving `time` traces back to the same missing field.

## Gap Summary (sorted High→Low)
- [HIGH] `Appointment` model has no date field — the app cannot represent which day an appointment is on — `Models.kt:192-206`, `BookAppointmentDialog.kt`
- [HIGH] `AppointmentDao.getAllAppointments()` sorts appointment time lexicographically (TEXT column), producing chronologically incorrect ordering for common AM/PM inputs — `AppointmentDao.kt:27`, `ThornburyDbHelper.kt:202`
- [MEDIUM] "Time of Visit" is unvalidated free text beyond a blank check — malformed times can be saved — `BookAppointmentDialog.kt:232-240,356,367`
- [MEDIUM] Select Patient Dialog's empty state has no "register new patient" path — dead-ends a common workflow — `ScheduleScreen.kt:669-681`
- [MEDIUM] Duration option `90` minutes is defined but never rendered/selectable — `BookAppointmentDialog.kt:64,254`
- [MEDIUM] No date field in the booking dialog itself (instance of the tab-level date gap)
- [LOW] `calculateAge()`/`formatTimeWithAmPm()` duplicated verbatim between `ScheduleScreen.kt` and `TodayQueueScreen.kt` — `ScheduleScreen.kt:32-69`, `TodayQueueScreen.kt:32-69`
