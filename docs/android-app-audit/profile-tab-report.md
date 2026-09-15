# Tab Report: Profile

Source: `android/app/src/main/java/com/example/thornburydental/ui/clinic/ProfileScreen.kt` (435 lines, single file, no ViewModel — all `remember { mutableStateOf(...) }` local state).

## Components

### Component: Top App Bar
Shared: no
Complexity: low
**Design**
- layout: `TopAppBar` with a two-line `Column` title (bold `titleLarge` headline + `bodySmall` muted subtitle)
- spacing: default `TopAppBar` insets, no custom padding
- colors: container `ThornburyCanvas`; title `ThornburyInk`; subtitle `ThornburyMuted`
- typography: title `titleLarge` bold; subtitle `bodySmall` regular
- icons: none
- surface: flat, no elevation override
- states: static only — no loading/error/empty variants
**Backend**
- data_source: none — static copy ("Clinician Profile & Settings")
- data_shape: n/a
- state_management: none
- interactions: none (not tappable)
- business_logic: none
- edge_states: none
- dependencies: none
- permissions: none
**Gaps**
none found

### Component: Profile Header Card
Shared: no
Complexity: low
**Design**
- layout: `OutlinedCard` → centered `Column`: 72dp circular avatar, name, subtitle, pill badge
- spacing: card padding 20dp; 12dp/6dp spacers between avatar/name/subtitle/badge
- colors: avatar background `ThornburyPrimary` with white "IH" initials text; name `ThornburyInk`; subtitle `ThornburyPrimaryText`; badge surface `ThornburySurfaceSoft` with `ThornburyHairline` border, badge text `ThornburyMuted`
- typography: avatar initials `headlineMedium` bold white; name `headlineSmall` bold; subtitle `bodyMedium` medium-weight; badge `labelSmall`
- icons: none (initials used instead of an image avatar)
- surface: card radius 18dp, 1dp `ThornburyHairline` border; badge is a fully-rounded (9999dp) pill
- states: static only
**Backend**
- data_source: *should* be `AuthRepository.currentUser` (collected at line 35) but is not actually used here — see Gap 1
- data_shape: `User { id, email, name, role, phone, createdAt }` (from `data/Models.kt`) is available but unread by this component
- state_management: `currentUser by AuthRepository.currentUser.collectAsState()` is collected but its derived `userName` (line 36) is never rendered
- interactions: none (not tappable)
- business_logic: none applied (name/title/registration number/room are literal string constants, not derived from `currentUser`)
- edge_states: no "not signed in" / loading appearance — impossible to distinguish since nothing here is actually dynamic
- dependencies: `AuthRepository` (imported and partially read, but effectively unused)
- permissions: none
**Gaps**
- [HIGH] The screen collects `AuthRepository.currentUser` and computes `userName` (line 36), but that value is never rendered anywhere — the avatar initials ("IH"), name ("Dr. Ingrid Halvorsen"), title ("Principal Endodontist & Dental Surgeon"), and registration badge ("GDC Registration #88204 • Surgery 1") are all hardcoded string literals. Any authenticated user — including the seeded `PATIENT` role user "Rosalind Achebe" (`UserDao.kt:191-196`) — would see this same clinician identity. — evidence: `ProfileScreen.kt:36,99,110,118,131` — fix: derive avatar initials, name, and a role-appropriate subtitle from `currentUser`, with a defined fallback (e.g. "Not signed in") when null.

### Component: Professional & Contact Details Card
Shared: no
Complexity: low
**Design**
- layout: `OutlinedCard` → `Column` of label/value rows (`ProfileDetailRow`) separated by `HorizontalDivider`
- spacing: card padding 18dp; 8dp vertical padding around each divider; 14dp before first row
- colors: card container `ThornburySurfaceSoft`, border `ThornburyHairline`; section header icon tint `ThornburyPrimary`; row label `ThornburyMuted`, row value `ThornburyInk` bold
- typography: section header `titleMedium` bold; row label `bodySmall`; row value `bodyMedium` bold
- icons: `Icons.Default.Badge` (section header only)
- surface: 16dp corner radius, 1dp hairline border
- states: static only
**Backend**
- data_source: *should* be `currentUser` (department/room/email/phone) — currently none; all four rows are literal strings
- data_shape: `User.email`, `User.phone` exist and are unused here
- state_management: none
- interactions: none (not tappable)
- business_logic: none
- edge_states: none
- dependencies: none (should depend on `AuthRepository`)
- permissions: none
**Gaps**
- [HIGH] Email and phone rows are hardcoded (`dr.halvorsen@thornburydental.co.uk`, `+44 20 7946 0912`) instead of reading `currentUser.email` / `currentUser.phone` — same root cause as the Profile Header gap above, called out separately here because this card's whole purpose is to display contact details. — evidence: `ProfileScreen.kt:173,176` — fix: bind to `currentUser`.
- [LOW] The hardcoded email domain here (`@thornburydental.co.uk`) doesn't even match the seeded clinician record's real email (`dr.halvorsen@thornburydental.com`, `.com` not `.co.uk`) in `UserDao.kt:173`. Moot once the row is bound to real data, but worth noting as a symptom of the hardcoding. — evidence: `ProfileScreen.kt:173` vs `UserDao.kt:173`.

### Component: Practice & App Settings Card (surgery room selector, dark mode, notifications)
Shared: no
Complexity: high
**Design**
- layout: `OutlinedCard` → `Column`: labeled `FilterChip` row (3 rooms), divider, two label+description/Switch rows
- spacing: card padding 18dp; 6/14/10dp spacers; chips spaced 8dp apart
- colors: card `ThornburySurfaceSoft`/`ThornburyHairline`; selected chip container `ThornburyPrimary` with white label; row titles `ThornburyInk` bold, descriptions `ThornburyMuted`
- typography: section header `titleMedium` bold; room label `labelMedium` bold; chip label `labelSmall`; row titles `bodyMedium` bold; descriptions `bodySmall`
- icons: `Icons.Default.Settings` (section header only)
- surface: card 16dp radius; chips 12dp radius
- states: chip selected/unselected only; switches checked/unchecked only — no disabled/loading/error state anywhere
**Backend**
- data_source: none currently — should be `UserPreferencesDao`/`DentalRepository` (a preferences table already exists and is used elsewhere for onboarding, e.g. `UserPreferencesDao.kt`)
- data_shape: none persisted
- state_management: three independent `remember { mutableStateOf(...) }` locals (`selectedRoom`, `isDarkModeEnabled`, `areNotificationsEnabled`) — all reset to their defaults on every screen recomposition/navigation, nothing survives process death or even leaving the tab
- interactions: 3 room `FilterChip`s (select default room), dark-mode `Switch`, notifications `Switch` — all update local state only
- business_logic: none — no validation, no derived values
- edge_states: none
- dependencies: none (should affect app-wide theming and the header/contact cards above)
- permissions: none
**Gaps**
- [MEDIUM] Selecting a different "Default Surgery Room" chip only updates local, unpersisted state — it is never saved via `UserPreferencesDao`/`DentalRepository`, and it never updates the "Surgery 1" references shown in the Profile Header badge (line 131) or the "Primary Clinic Room" row (line 170) above, so the screen visibly contradicts itself when a different room is picked. — evidence: `ProfileScreen.kt:38,219-232` vs `131,170` — fix: persist the selection and derive both display strings from the same source of truth.
- [MEDIUM] "Operatory Dark Mode" switch changes local state that nothing reads — no theming code in this file or wired from it actually changes appearance when toggled. — evidence: `ProfileScreen.kt:39,256-259` — fix: wire to the app's theme (see `theme/Theme.kt`) or remove the control until dark mode exists.
- [MEDIUM] "Appointment Reminders" switch is local-only — not persisted, not connected to any reminder/notification scheduling in the codebase. — evidence: `ProfileScreen.kt:40,284-287` — fix: persist via `UserPreferencesDao` and wire to real reminder logic, or mark as not-yet-implemented in the UI.

### Component: Security & Privacy Card
Shared: no
Complexity: low
**Design**
- layout: `OutlinedCard` → header row (icon+title) + one status `Surface` banner (icon + two-line text)
- spacing: card padding 18dp, 12dp before banner
- colors: header icon tint `ThornburySuccess`; banner surface `ThornburySuccessWash` with 30%-alpha `ThornburySuccess` border; banner title `ThornburySuccess`, body `ThornburyBodyStrong`
- typography: header `titleMedium` bold; banner title `labelMedium` bold; banner body `bodySmall`
- icons: `Icons.Default.Security` (header), `Icons.Default.Shield` (banner)
- surface: card 16dp radius; banner 10dp radius
- states: static only (single always-on state)
**Backend**
- data_source: none — static copy, but the underlying claim is real
- data_shape: n/a
- state_management: none
- interactions: none (not tappable)
- business_logic: none
- edge_states: none
- dependencies: `MainActivity` actually sets `WindowManager.LayoutParams.FLAG_SECURE` (`MainActivity.kt:20-21`) — **verified accurate**, this is the one claim on the screen that is true
- permissions: none
**Gaps**
none found — verified against `MainActivity.kt:20-21`, the FLAG_SECURE claim is accurate, unlike the rest of the screen's static claims.

### Component: Account Actions Card
Shared: no
Complexity: high
**Design**
- layout: `OutlinedCard` → `Column`: title + 3 stacked full-width buttons (2 `OutlinedButton`, 1 filled `Button`)
- spacing: card padding 18dp; 12dp/8dp spacers between buttons
- colors: filled button container `ThornburyPrimary`, content white; outlined buttons use default Material3 outlined-button colors
- typography: buttons default `labelLarge`-ish Material3 button text; sign-out label explicitly `labelMedium` bold
- icons: `Icons.Default.Edit`, `Icons.Default.Lock`, `Icons.Default.Home` (one per button)
- surface: card 16dp radius; buttons 10dp radius
- states: no disabled/loading/pressed-custom states — relies entirely on default Material3 button states
**Backend**
- data_source: none
- data_shape: n/a
- state_management: `showSignOutDialog` is declared (line 41) and never read/set again anywhere in the file
- interactions: "Edit Profile Details" → no-op; "Change Account Password" → no-op; "Return to Brand Page" → calls `onSignOut` directly (correctly wired to `Navigation.kt`'s `rootDestination = RootDestination.WELCOME`)
- business_logic: none
- edge_states: none
- dependencies: `onSignOut` callback from `Navigation.kt`
- permissions: none
**Gaps**
- [MEDIUM] "Edit Profile Details" button has an empty `onClick = { }` — completely non-functional. — evidence: `ProfileScreen.kt:371-372` — fix: implement an edit-profile flow or hide the button until one exists.
- [MEDIUM] "Change Account Password" button has an empty `onClick = { }` — completely non-functional (note `UserDao.hashPassword` already exists and could back a real implementation). — evidence: `ProfileScreen.kt:383-384` — fix: implement a change-password flow using the existing hashing logic, or hide until implemented.
- [MEDIUM] `showSignOutDialog` state is declared but dead — the sign-out button calls `onSignOut` immediately with no confirmation step, so a mis-tap signs the user out (and per `Navigation.kt`, drops them back to the Welcome/onboarding root) with no way to cancel. — evidence: `ProfileScreen.kt:41` (declared, unused) vs `395-407` (direct call) — fix: gate the call behind an `AlertDialog` confirmation using the existing (currently dead) state variable.

## Tab-Level Notes
- Navigation flow: Profile is a leaf tab — it has no drill-down navigation of its own. Its only outward transition is `onSignOut`, which `Navigation.kt` wires to `rootDestination = RootDestination.WELCOME` (returns to `WelcomeBrandScreen`, out of scope for this tab).
- Shared state: none — every piece of state on this screen is local to the composable and none of it is shared with other tabs.
- Shared/external components referenced but not spec'd here: bottom `NavigationBar` shell (`Navigation.kt`, shared across all 4 tabs); `AuthRepository` (shared backend, partially read but under-used — see gaps); `theme/Color.kt` + `theme/Theme.kt` (shared design tokens, used correctly throughout).
- Cross-cutting observation: this is the only tab with **zero** live backend read — every other tab's screens actually query `DentalRepository`/DAOs for real data; Profile is 100% hardcoded despite `AuthRepository` and `UserPreferencesDao` already existing and being used elsewhere in the app (onboarding). This looks like the screen was built against a mock/placeholder clinician before auth was wired up, and never revisited.

## Gap Summary (sorted High→Low)
- [HIGH] Profile Header Card: entire identity block (avatar initials, name, title, registration badge) is hardcoded, ignoring the already-collected `currentUser` — `ProfileScreen.kt:36,99,110,118,131`
- [HIGH] Professional & Contact Details Card: email/phone rows hardcoded instead of bound to `currentUser` — `ProfileScreen.kt:173,176`
- [MEDIUM] Practice & App Settings: default-room selection not persisted and contradicts the header/contact-card room text — `ProfileScreen.kt:38,219-232` vs `131,170`
- [MEDIUM] Practice & App Settings: "Operatory Dark Mode" switch has no effect on the app — `ProfileScreen.kt:39,256-259`
- [MEDIUM] Practice & App Settings: "Appointment Reminders" switch is not persisted or wired to real reminders — `ProfileScreen.kt:40,284-287`
- [MEDIUM] Account Actions: "Edit Profile Details" button is a no-op — `ProfileScreen.kt:371-372`
- [MEDIUM] Account Actions: "Change Account Password" button is a no-op — `ProfileScreen.kt:383-384`
- [MEDIUM] Account Actions: dead `showSignOutDialog` state — sign-out has no confirmation step — `ProfileScreen.kt:41,395-407`
- [LOW] Hardcoded email domain (`.co.uk`) doesn't match the seeded clinician's real email (`.com`) — `ProfileScreen.kt:173` vs `UserDao.kt:173`
