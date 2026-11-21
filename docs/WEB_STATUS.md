# Dentara web implementation status

Updated 2026-11-21 from repository source inspection. “Implemented in code” is
not a claim that a workflow has passed automated, security, or user validation.
No current passing build/test result was available during this documentation
pass.

## Feature and evidence matrix

| Feature area | Implementation status | Source evidence | Automated evidence | Runtime / production evidence |
| --- | --- | --- | --- | --- |
| Public practice site | Implemented in code | `app/page.tsx`, `app/layout.tsx` | `npm run build` available; result not verified in this pass | Not verified |
| Staff sign-in and reset UI | Implemented in code | `app/(auth)/signin/page.tsx`, `app/(auth)/reset/page.tsx`, `actions/auth.ts`, `lib/auth.ts` | No web auth test suite found; result not verified | Email delivery and deployed auth flow not verified |
| Invitation acceptance and signup route | Route files exist; access behavior needs review | `app/(auth)/invite/[token]/page.tsx`, `app/(auth)/signup/page.tsx`, `lib/auth.ts` | No workflow tests found | Not verified |
| Today dashboard | Implemented in code | `app/clinic/page.tsx` | No dedicated web test suite found | Not verified |
| Schedule and appointment creation | Implemented in code | `app/clinic/schedule/page.tsx`, `app/clinic/schedule/new/page.tsx`, `actions/clinical.ts`, `lib/scheduling.ts` | No scheduling workflow tests found | Conflict handling in real deployment not verified |
| Patient roster, registration and chart | Implemented in code | `app/clinic/patients/page.tsx`, `new/page.tsx`, `[id]/page.tsx` | No patient workflow tests found | Not verified |
| Staff management | Implemented in code | `app/clinic/staff/page.tsx` | No staff administration tests found | Invitation mail delivery not verified |
| Audit screen and API | Implemented in code | `app/clinic/audit/page.tsx`, `app/api/v1/audit/route.ts`, `lib/audit.ts` | No web audit integration tests found | Production chain verification not verified |
| Clinical API routes | Implemented in code | `app/api/v1/*` | No API integration suite found | Authentication, authorization and operational monitoring not verified end to end |
| Database migration and demo seed | Implemented in code | `scripts/migrate.ts`, `scripts/seed.ts`, `migrations/` | No migration/restore test result recorded | Production migration and restore not verified |

## Available checks

These commands are defined or documented as available checks; this page does
not claim they passed:

```sh
npm run typecheck
npm run build
python -m unittest discover -s tools/tests -v
```

The Python suite covers the standalone speech-evaluation utility, not the web
application. There is no dedicated web unit, browser, or integration test suite
in the repository at this time.

## Readiness limits

Source code contains safeguards, but no current evidence is recorded here for
deployed authentication, database migrations under production load, backup
restore, penetration testing, accessibility review, clinical validation, or
privacy/regulatory readiness. Treat these as **not verified** until evidence is
captured and linked here.
