# Dentara / Practix

This repository contains two separate products. Their capabilities and evidence
are documented independently; a feature in one product does not imply that it
exists in the other.

| Product | Platform | Documentation | Current evidence |
| --- | --- | --- | --- |
| Dentara clinical workspace | Next.js, React, Neon/Postgres | [Web application guide](docs/WEB.md) · [Web implementation status](docs/WEB_STATUS.md) | [Web feature and evidence matrix](docs/WEB_STATUS.md) |
| Practix clinical app | Android, Kotlin, Jetpack Compose | [Android guide](android/README.md) · [Android feature matrix](android/STATUS.md) · [Detailed inventory](android/FEATURES.md) | [Android feature and evidence matrix](android/STATUS.md) |

## Status vocabulary

- **Implemented in code** means a route, screen, or code path exists. It does
  not establish that the workflow is correct or safe in production.
- **Automated check** names a test or build command and reports its last
  recorded result only when that result is available. A command listed as
  repeatable is not evidence that it passed.
- **Device/deployment evidence** requires a result from the corresponding
  Android device/emulator or deployed environment.
- **Not verified** means the repository does not contain current evidence for
  that claim.

The status matrices are source inspections, not clinical validation, a security
certification, or a production-readiness approval. Check each product's status
page for known limits and verification steps.
