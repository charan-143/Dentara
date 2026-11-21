# Dentara web application

This guide covers only the Next.js web application in the repository root. It
does not describe the Practix Android application; see [Android documentation](../android/README.md).

## Stack and scope

Next.js 15, React 19, TypeScript and Neon Postgres, with a PGlite development
database adapter. The site has public practice information and a staff-only
clinical workspace. It has no patient accounts or patient-facing portal.

## Local setup

```sh
cp .env.example .env.local
# Set DATABASE_URL in .env.local
npm install
node --env-file=.env.local scripts/migrate.ts
npm run dev
```

For invented demo data, run `node --env-file=.env.local scripts/seed.ts` before
starting the development server. The seed script refuses production. It also
supports a one-time `--admin-only` bootstrap; follow the script's prompts and
use a unique strong password. Never place production credentials in source
control.

## Main areas

- `app/`: public site, authentication, clinic screens, and API routes.
- `actions/`: server actions for authentication and clinical workflows.
- `lib/`: database access, authorization, sessions, audit, scheduling, and
  password policy.
- `migrations/`: database schema changes.
- `scripts/`: migration, seed, and bundle analysis commands.

See [web implementation status and evidence](WEB_STATUS.md) for current route
coverage, automated checks, and unverified areas.

## Production deployment outline

The app expects `DATABASE_URL` to be configured in the deployment environment.
Create the database, apply migrations with `node scripts/migrate.ts`, build the
Next.js app, then deploy using the hosting provider's documented workflow.
The current `vercel.json` does not automate migrations. Confirm the production
database URL, hosting agreement and backup/restore process before any live
deployment.

For a first administrator, `scripts/seed.ts --admin-only` is designed for a
one-time bootstrap and refuses if accounts already exist. Run it only against
the intended production database with a strong password supplied through the
supported environment-variable path; see that script before use.

## Data and safety boundary

Do not use this project with real patient data based on source inspection alone.
Hosting agreements, encryption at rest, tested backups and restoration,
retention and destruction processes, access reviews, penetration testing, and
clinical safety validation require separate work and evidence. The prescribing
checks are limited rules and are not a medication reference. Password reset
delivery also needs a configured mail transport before operational use.
