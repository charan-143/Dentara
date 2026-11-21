# Practix Android implementation status

Updated 2026-11-21 from source inspection. The source files listed below show
that code paths exist; they do not prove that they work correctly on a device.
No current build or test run was completed for this documentation update.

## Feature and evidence matrix

| Feature area | Implementation status | Source evidence | Automated evidence | Device / production evidence |
| --- | --- | --- | --- | --- |
| Patient directory and chart | Screens and data layer exist | `ui/clinic/PatientRosterScreen.kt`, `PatientDetailChartScreen.kt`, `data/DentalRepository.kt` | `LocalDatabaseTest`, `DentalRepositoryTest` exist; latest result not verified | End-to-end clinical charting not verified |
| Appointments and reminders | Schedule screens and alarm code exist | `ui/clinic/ScheduleScreen.kt`, `TodayQueueScreen.kt`, `reminder/` | `ReminderAndPreferencesUnitTest` exists; latest result not verified | Reschedule, delete, reboot and exact-alarm behavior not verified |
| Voice charting | Whisper/native bridge, voice controller and parser exist | `speech/WhisperEngine.kt`, `WhisperNative.kt`, `VoiceChartingController.kt`, `ClinicalVoiceExtractor.kt` | Whisper, voice parser and integration test files exist; latest result not verified | Speech accuracy, latency, noise and complete review/commit workflow not verified |
| Audio multimodal extraction | Remote request and preprocessing code exist; no local Gemma inference backend is initialized | `speech/Gemma4AudioEngine.kt`, `data/cds/GeminiMultimodalClient.kt` | `Gemma4AudioEngineTest` exists; latest result not verified | Remote model response, clinical field accuracy and consent/privacy behavior not verified |
| Clinical decision support | Rules and orchestration code exist | `data/cds/`, `ui/cds/CdsCopilotScreen.kt` | `RedFlagSafetyCheckerTest` and embedding/corpus tests exist; latest result not verified | Expert-reviewed clinical safety/accuracy not verified |
| Local persistence and key management | SQLCipher and Keystore code exist; startup now fails closed on detected key loss | `data/db/`, `data/security/KeyStoreManager.kt` | `LocalDatabaseTest` exists; key lifecycle and migration test results not verified | Reinstall, restore, invalidated Keystore and recovery/export scenarios not verified |
| Audit integrity | Hash-chain code and verifier exist | `data/security/AuditChainIntegrityVerifier.kt`, database audit DAO | `AuditChainIntegrityTest` exists; latest result not verified | Tampering and recovery on supported devices not verified |
| Prescription and medication presets | Prescription UI and preset/data code exist | `ui/clinic/IssuePrescriptionDialog.kt`, `ManagePresetsDialog.kt`, data/database layer | No current end-to-end prescribing test result recorded | Clinical formulary completeness and safe prescribing not established |
| Reports, PDF and FHIR export | Export code and screens exist | `export/PatientPdfGenerator.kt`, `FhirExportManager.kt`, report UI | PDF/FHIR unit test files exist; latest result not verified | Interoperability and exported-document validation not verified |
| Backup and notifications | Backup disabled in manifest; notifications use generic/private content | `app/src/main/AndroidManifest.xml`, `reminder/DentaraAlarmReceiver.kt` | No current backup/lock-screen test result recorded | OEM-specific lock-screen and restore behavior not verified |

## Available checks

The repository contains Android unit and instrumentation test dependencies and
unit test source files. These commands can be used to produce fresh evidence;
their presence here does not imply a passing result:

```sh
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

The existing root [engineering evidence guide](../docs/PROJECT_EVIDENCE.md)
describes speech evaluation and device benchmarking. Record command output,
device/OS, model checksum and date before changing a status to verified.

## Known readiness limits

- A source audit or successful debug build is not clinical validation.
- Audio preprocessing is not an on-device Gemma inference runtime.
- Key invalidation now stops database initialization, but no encrypted recovery
  or export flow is implemented.
- API-key storage, release credential rotation, secure migration recovery,
  real-device workflow validation and a clinical safety review still need
  evidence or implementation.
