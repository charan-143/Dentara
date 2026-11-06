# Dentara Web: Designing a Clinical Workspace Where Auditability Matters More Than a Beautiful Dashboard

*Project: [Dentara](https://github.com/charan-143/Dentara) · Status: public repository · Technologies: Next.js 15, React 19, TypeScript, Neon PostgreSQL, Zod, Vercel*

A dental-clinic application looks deceptively familiar at first: sign-in, patient records, appointments, staff management, and a dashboard. But once clinical information is involved, the architecture has different priorities. An attractive interface cannot compensate for an inconsistent record, an untraceable edit, or an unauthorized session.

## The architecture in the repository

The Dentara repository combines a public practice website with a staff-only workspace. Its web stack uses Next.js and React, with Neon-hosted PostgreSQL for persistent records and Vercel as the intended deployment environment. The account model is deliberately different from a typical consumer SaaS: patients are clinical records, not self-service users; staff access is by invitation. Zod and TypeScript support input and contract validation, but the database and server must remain the source of truth.

The serverless database detail matters. Short-lived server functions may open many connections in parallel. The project's README explicitly recommends Neon's pooled connection endpoint, demonstrating how deployment architecture affects database configuration.

## Identity is a server-side responsibility

The repository documents scrypt password hashing, random session tokens with only digests stored in the database, session expiry, revocation, and per-account/IP sign-in throttling. The engineering lesson is that a client-side route guard is a user-experience feature, **not** authorization. Each clinical server route has to check the current session and permission. Similarly, an invitation token should be single-use and its raw value should not sit in a database indefinitely.

## An audit trail is more than a history table

The most interesting design decision is an append-only, hash-chained audit trail. A normal `updated_at` field tells you when a row changed. A tamper-evident chain lets an auditor detect certain changes or deletions in a sequence of events, provided the verification procedure and trusted checkpoints are sound. The database schema includes constraints intended to prevent updates, deletes, and concurrent forks. That is an instructive combination of application logic and database-enforced invariants.

Clinical editing also deserves a separate state model. The README describes published treatment plans as locked and corrections as dated addenda. That avoids silently rewriting the past: a correction becomes a new event with its own provenance.

## What the project makes clear about shipping safely

The repository is candid about its limits. Its README states that several clinical screens still need implementation and that the project is **not suitable for real patient information** without procurement, operational, backup, security, and clinical-safety work. A production build passing is not a medical compliance certification. Even well-written authentication code does not replace independent security review or clinical validation.

## The engineering lesson

For a regulated workflow, design from the invariants backwards. Decide which actions must be authorized, which records are immutable, what an auditor needs to reconstruct, and what happens if a database write fails. Then design the UI and API around those rules. This is a more transferable lesson than simply listing Next.js and PostgreSQL on a résumé.

### Sources and scope

- [Repository README](https://github.com/charan-143/Dentara/blob/master/README.md)
- [Web package manifest](https://github.com/charan-143/Dentara/blob/master/package.json)
- [Documented architecture and SQL migrations](https://github.com/charan-143/Dentara/tree/master/migrations)

*This is an architectural case study based on repository code and documentation, not a claim of a production clinical deployment or verified regulatory compliance.*