# SUITE 5: IMPLEMENTED FEATURES & CODEBASE SPECIFICATION
**Application:** Practix  
**Sole Proprietor & Principal Developer:** Devanapally Charan Tej  
**Document Version:** 1.0.0-FEATURES  
**Status:** Implemented & Verified in Codebase  
**Target Environments:** Android (API 24–36, Jetpack Compose, Native C++/NDK) & Next.js Serverless Practice Portal  

---

## 1. Feature Architecture Overview

```
========================================================================================================
                              PRACTIX IMPLEMENTED CAPABILITY MATRIX
========================================================================================================
┌───────────────────────────────────────┬──────────────────────────────────────────────────────────────┐
│ CORE SUBSYSTEM                        │ KEY IMPLEMENTED MODULES & CAPABILITIES                       │
├───────────────────────────────────────┼──────────────────────────────────────────────────────────────┤
│ 🎙️ Voice Dictation & Speech Engine     │ • whisper.cpp native C++ SIMD inference                      │
│                                       │ • Continuous audio circular buffer streaming                 │
│                                       │ • Left-to-right deterministic parsing (FDI & Universal)      │
│                                       │ • 6-site periodontal depth auto-mapping & single-word undo   │
├───────────────────────────────────────┼──────────────────────────────────────────────────────────────┤
│ 🦷 3D Odontogram & Anatomical Visual  │ • Google Filament PBR rendering engine & OpenGL ES renderer  │
│                                       │ • Full anatomical 3D molar mesh (.obj loader) & surface tags │
│                                       │ • Color-coded clinical status overlays (Caries, Crowns, etc.)│
├───────────────────────────────────────┼──────────────────────────────────────────────────────────────┤
│ 🧠 Multi-Agent Clinical AI & CDS      │ • On-device Llama 3.2 / Gemma 2 GGUF engine (llama.cpp)      │
│                                       │ • Multi-Agent Orchestrator (Intake, Radiology, Safety)       │
│                                       │ • Pharmacological Red-Flag Sentinel (Allergies, NSAID, MRONJ)│
│                                       │ • Cloud Gemini 2.0 Multimodal fallback with de-identification│
├───────────────────────────────────────┼──────────────────────────────────────────────────────────────┤
│ 🔒 Cryptographic Security & Audit     │ • SQLCipher 256-bit AES database encryption                  │
│                                       │ • Android Hardware Keystore & BiometricPrompt authentication │
│                                       │ • Tamper-evident append-only HMAC-SHA256 audit hash chain    │
│                                       │ • 15-min idle timeout & FLAG_SECURE window protection        │
├───────────────────────────────────────┼──────────────────────────────────────────────────────────────┤
│ 🏥 Practice Management & Clinical Ops │ • Today Queue & Day-Grid appointment scheduler               │
│                                       │ • Patient demographics & immutable treatment plans           │
│                                       │ • Preset prescription manager & vector PDF chart exporter    │
│                                       │ • AlarmManager automated morning briefing & arrival alerts   │
└───────────────────────────────────────┴──────────────────────────────────────────────────────────────┘
```

---

## 2. Hands-Free Voice Charting & Speech Recognition

### 2.1 Native Whisper Engine (`whisper.cpp` Integration)
- **Source Files:**
  - Native C++: [`CMakeLists.txt`](file:///d:/code/project1/Dentara/android/app/src/main/cpp/CMakeLists.txt), [`whisper.cpp`](file:///d:/code/project1/Dentara/android/app/src/main/cpp/whisper.cpp)
  - Kotlin Native Bridge: [`WhisperNative.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/WhisperNative.kt), [`WhisperEngine.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/WhisperEngine.kt)
- **Implemented Capabilities:**
  - Compiled with Clang via Android NDK 28 for `arm64-v8a` (NEON SIMD) and `x86_64` targets.
  - Zero-copy audio streaming with memory-mapped (`mmap`) GGML model weights.
  - Native runtime handles automated sampling conversion ($16\text{ kHz}$ mono Float32).

### 2.2 Continuous Audio Capture & Dictation Service
- **Source Files:**
  - [`AudioRecordManager.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/AudioRecordManager.kt)
  - [`VoiceChartingService.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/VoiceChartingService.kt)
  - [`VoiceChartingController.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/VoiceChartingController.kt)
  - [`VoiceActivitySegmenter.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/VoiceActivitySegmenter.kt)
- **Implemented Capabilities:**
  - Android Foreground Service with microphone notification status for uninterruptible operatory dictation.
  - Adaptive energy-based Voice Activity Detection (VAD) to trim ambient pauses and background drill noise.
  - Zero-fill buffer sanitization on completion to prevent audio data lingering in heap memory.

### 2.3 Deterministic Clinical Command Parser
- **Source Files:**
  - [`VoiceCommandParser.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/VoiceCommandParser.kt)
  - [`ToothNumberingSystem.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/ToothNumberingSystem.kt)
- **Implemented Capabilities:**
  - **Left-to-Right Token Scanner:** Distinguishes tooth numbers from millimeter pocket depths without regex collision.
  - **Dual Tooth Numbering Systems:** Explicit support for Universal (1–32) and FDI Two-Digit (11–48 permanent, 51–85 primary) with scheme-level boundary validation.
  - **Automated 6-Site Sequence Mapping:** When 6 consecutive depths are dictated without site tags, automatically assigns them to Distobuccal $\rightarrow$ Buccal $\rightarrow$ Mesiobuccal $\rightarrow$ Distolingual $\rightarrow$ Lingual $\rightarrow$ Mesiolingual.
  - **Bleeding on Probing (BOP) Detection:** Detects *"bleeding"*, *"BOP"*, or *"plus"* qualifiers per site.
  - **Voice Undo & Reversion:** Detects *"undo that"*, *"cancel last"*, or *"revert"* and fires rollback transactions via [`VoiceUndoDao.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/db/VoiceUndoDao.kt).
  - **Spoken Read-Back:** [`ChartReadBack.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/ChartReadBack.kt) generates synthesized auditory confirmations for chairside verification.

---

## 3. Interactive 3D Odontogram & Anatomical Visualizer

### 3.1 3D Rendering Pipeline (Google Filament & OpenGL ES)
- **Source Files:**
  - [`FilamentToothView.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/components/FilamentToothView.kt)
  - [`DentalModelView.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/components/DentalModelView.kt)
  - [`Molar3DView.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/components/molar3d/Molar3DView.kt)
  - [`MolarGLRenderer.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/components/molar3d/MolarGLRenderer.kt)
  - [`MolarObjParser.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/components/molar3d/MolarObjParser.kt)
  - Asset: `mandibular-first-molar.obj` (high-fidelity anatomical polygon model)
- **Implemented Capabilities:**
  - Physically Based Rendering (PBR) lighting simulating tooth enamel translucency and reflection.
  - Multi-touch gesture controls: smooth $360^\circ$ rotation, pinch-to-zoom, and axial panning.
  - Mesh sub-surface hit-testing for anatomical facets (Mesial, Distal, Occlusal, Buccal, Lingual).

### 3.2 2D/3D Quadrant & Tooth Condition Overlay
- **Source Files:**
  - [`MaxillaryQuadrantViews.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/MaxillaryQuadrantViews.kt)
  - [`MandibularQuadrantViews.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/MandibularQuadrantViews.kt)
  - [`AnatomicalToothVisual.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/components/AnatomicalToothVisual.kt)
- **Implemented Capabilities:**
  - Complete maxillary (upper) and mandibular (lower) arch quadrant visualizers.
  - Real-time clinical condition shader and color tags:
    - **Active Caries / Decay:** Coral / Crimson red indicator.
    - **Existing Restorations:** Deep blue / metallic overlay (Amalgam, Composite, Inlay/Onlay).
    - **Crown & Bridge:** Gold / porcelain ceramic tint.
    - **Endodontic Treatment:** Root canal obturation indicator lines.
    - **Watch / Incipient Lesion:** Warning amber outline.
    - **Extracted / Missing:** Translucent grey hatch pattern.

---

## 4. Multi-Agent Clinical AI & Decision Support (CDS)

### 4.1 Specialized Multi-Agent Clinical Architecture
- **Source Files:**
  - [`MultiAgentClinicalOrchestrator.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/MultiAgentClinicalOrchestrator.kt)
  - [`ClinicalAgentModels.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/ClinicalAgentModels.kt)
- **Implemented Sub-Agents:**
  1. **Patient Intake & Context Synthesizer (`INTAKE_SYNTHESIZER`):** Aggregates demographics, vital statistics, systemic medical history, and existing tooth condition records.
  2. **Multimodal Dental Radiology Agent (`MULTIMODAL_RADIOLOGY`):** Ingests and inspects periapical radiographs, bitewings, panoramic scans, and PDF diagnostic reports.
  3. **Evidence & Differential Synthesis Agent (`EVIDENCE_DIFFERENTIAL`):** Cross-references ADA/AAE/AAP guidelines to formulate differential diagnoses.
  4. **Pharmacological & Red-Flag Safety Agent (`SAFETY_GUARDRAIL`):** Evaluates acute clinical contraindications and drug interactions.
  5. **Master Orchestrator (`MASTER_ORCHESTRATOR`):** Unifies agent outputs into an executive clinical summary with real-time UI token streaming.

### 4.2 On-Device LLM Runtime & Dynamic RAM Tiering
- **Source Files:**
  - [`OnDeviceLlmEngine.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/OnDeviceLlmEngine.kt)
  - [`LlamaInferenceBridge.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/LlamaInferenceBridge.kt)
  - [`LlmConfig.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/LlmConfig.kt)
- **Implemented Capabilities:**
  - Dynamic hardware profiling via `ActivityManager.MemoryInfo`:
    - **Tier A ($\ge 6\text{ GB}$ RAM):** Runs `Llama-3.2-3B-Instruct (Q4_0, ~2.0 GB)`.
    - **Tier B ($\ge 4\text{ GB}$ RAM):** Runs `Gemma-2-2B-IT (Q4_0, ~1.4 GB)`.
    - **Tier C ($< 4\text{ GB}$ RAM):** Runs `Llama-3.2-1B-Instruct (Q4_0, ~0.8 GB)`.
  - Barge-in & generation cancellation controller (`LlmGenerationController`).

### 4.3 Red-Flag Clinical Safety Sentinel
- **Source Files:**
  - [`RedFlagSafetyChecker.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/RedFlagSafetyChecker.kt)
  - [`ClinicianSuggestionVerifier.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/ClinicianSuggestionVerifier.kt)
- **Implemented Contraindication Rules:**
  - **Allergy Conflicts:** Direct beta-lactam / penicillin cross-matching.
  - **Anticoagulant $\leftrightarrow$ NSAID Bleeding Risk:** Flags Warfarin, Eliquis (Apixaban), Xarelto (Rivaroxaban), or Plavix (Clopidogrel) combined with Ibuprofen, Ketorolac, or Naproxen.
  - **Bisphosphonate $\leftrightarrow$ MRONJ Surgical Risk:** Blocks invasive dentoalveolar extractions without explicit clinical rationale when patient is on Alendronate, Zoledronic acid, or Denosumab.

### 4.4 Cloud Multimodal Radiography Fallback
- **Source Files:**
  - [`GeminiMultimodalClient.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/GeminiMultimodalClient.kt)
  - [`MultimodalReportProcessor.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/MultimodalReportProcessor.kt)
  - [`OnDeviceMultimodalVlmEngine.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/OnDeviceMultimodalVlmEngine.kt)
- **Implemented Capabilities:**
  - High-resolution visual inspection of dental X-rays, bitewings, and intraoral photography using Gemini 1.5/2.0 API.
  - Automatic de-identification layer removing patient names and timestamps prior to payload transmission.

---

## 5. Cryptographic Security, Audit & Authentication

### 5.1 Encrypted Database Architecture (SQLCipher AES-256)
- **Source Files:**
  - [`LocalDatabaseManager.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/db/LocalDatabaseManager.kt)
  - [`ThornburyDbHelper.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/db/ThornburyDbHelper.kt)
  - [`KeyStoreManager.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/security/KeyStoreManager.kt)
- **Implemented Capabilities:**
  - 256-bit AES page encryption for all SQLite tables via SQLCipher binaries.
  - Hardware-backed key generation via Android KeyStore (`AndroidKeyStore` provider, AES-256 GCM).
  - Secure memory array zeroing after database passphrase binding.

### 5.2 Biometric Lock & Session Security
- **Source Files:**
  - [`BiometricAuthManager.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/security/BiometricAuthManager.kt)
  - [`BiometricLockOverlay.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/components/BiometricLockOverlay.kt)
  - [`AppSessionLifecycleObserver.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/security/AppSessionLifecycleObserver.kt)
- **Implemented Capabilities:**
  - Class-3 Strong Biometric Prompt (Fingerprint / 3D Facial Recognition) integration.
  - Automatic 15-minute idle detection triggering full memory overlay lock.
  - `WindowManager.LayoutParams.FLAG_SECURE` enabled to block OS task switcher previews and screenshot leaks.

### 5.3 Tamper-Evident Hash-Chained Audit Logs
- **Source Files:**
  - Schema: [`migrations/0001_init.sql`](file:///d:/code/project1/Dentara/migrations/0001_init.sql)
  - Next.js / Serverless Audit: [`lib/audit.ts`](file:///d:/code/project1/Dentara/lib/audit.ts)
- **Implemented Capabilities:**
  - Cryptographic append-only chaining:  
    $$\text{current\_hash} = \text{SHA256}(\text{prev\_hash} \,\|\, \text{timestamp} \,\|\, \text{actor\_id} \,\|\, \text{action\_type} \,\|\, \text{resource\_id})$$
  - SQL Triggers rejecting `UPDATE` and `DELETE` queries on the audit table.
  - Startup verification scanning every log row for chain continuity.

---

## 6. Clinical Workflows, UI Screens & Operations

### 6.1 Clinical UI Screens (Jetpack Compose M3)
- **Source Files:**
  - [`TodayQueueScreen.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/TodayQueueScreen.kt): Real-time queue, patient check-in statuses, and active operatory timers.
  - [`ScheduleScreen.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/ScheduleScreen.kt): Day-grid calendar scheduler with conflict prevention.
  - [`PatientRosterScreen.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/PatientRosterScreen.kt): Patient search, quick filtering by risk category, and demographic summary.
  - [`RegisterPatientScreen.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/RegisterPatientScreen.kt): New patient intake with allergy tags, medical condition chips, and emergency contact entries.
  - [`PatientDetailChartScreen.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/PatientDetailChartScreen.kt): Multi-tab clinical chart housing Diagnosis, Treatment Plans, Imaging, and Examination notes.
  - [`CdsCopilotScreen.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/cds/CdsCopilotScreen.kt): Interactive AI assistant panel showing multi-agent timeline, evidence citations, and differential diagnoses.

### 6.2 Treatment Planning & Prescribing Modals
- **Source Files:**
  - [`CreateTreatmentPlanDialog.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/CreateTreatmentPlanDialog.kt)
  - [`IssuePrescriptionDialog.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/IssuePrescriptionDialog.kt)
  - [`ManagePresetsDialog.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/ManagePresetsDialog.kt)
  - [`BookAppointmentDialog.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/BookAppointmentDialog.kt)
- **Implemented Capabilities:**
  - Multi-phase treatment plan synthesis (Phase 1 Urgent, Phase 2 Restorative, Phase 3 Maintenance).
  - Immutable Plan Locking: Published plans lock with check constraints; changes require signed addenda.
  - Prescription presets management (e.g., Amoxicillin 500mg TID, Ibuprofen 600mg TID, Chlorhexidine 0.12% rinse).

### 6.3 Vector PDF Chart & Report Exporter
- **Source Files:**
  - [`PatientPdfGenerator.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/export/PatientPdfGenerator.kt)
  - [`PatientShareOptions.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/export/PatientShareOptions.kt)
  - [`SharePatientPdfDialog.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/SharePatientPdfDialog.kt)
- **Implemented Capabilities:**
  - Native Android `PdfDocument` vector rendering generating crisp, scalable A4 clinical summaries.
  - Includes patient header, medical alerts banner, complete tooth chart status table, treatment plans, prescriptions, and cryptographic document hash.
  - Android `FileProvider` integration for secure sharing via email, print, or practice management transfer.

### 6.4 Automated Clinical Reminders & Notifications
- **Source Files:**
  - [`DentaraAlarmReceiver.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/reminder/DentaraAlarmReceiver.kt)
- **Implemented Capabilities:**
  - `SCHEDULE_EXACT_ALARM` integration for automated morning daily clinical briefings.
  - Real-time patient arrival and operatory transition alerts.
  - Boot completed receiver restoring active clinic alarms upon device restart.

---

## 7. Data Access Objects (DAO) & Persistence Layer

```
+--------------------------+-----------------------------------------------------------------------------------------+
| DAO Name                 | Entity & Operation Scope                                                                |
+--------------------------+-----------------------------------------------------------------------------------------+
| PatientDao               | CRUD operations for patient records, medical history, and allergy arrays.               |
| ToothDao                 | Per-tooth and per-surface condition records, numbering system tags, and restorations.   |
| TreatmentPlanDao         | Multi-item treatment plans, phase status, and immutability lock triggers.               |
| PrescriptionDao          | Medication records, dosages, duration, and clinical override justification logs.       |
| AppointmentDao           | Operatory scheduling, chair assignments, appointment status, and conflict checks.       |
| ReportDao                | Diagnostic report metadata, imaging file links (DICOM, PNG, JPEG), and lab findings.    |
| UserDao & AuthRepository | Clinician credentials, scrypt password validation, session tokens, and role gates.       |
| MedicationPresetDao      | Practice-wide favorite prescription templates and standard dosage instructions.        |
| VoiceUndoDao             | Reversible state snapshots for chairside speech dictation rollbacks.                    |
| UserPreferencesDao       | Active numbering system (FDI vs Universal), theme mode, and auto-lock timeout preferences|
+--------------------------+-----------------------------------------------------------------------------------------+
```

---

## 8. Companion Web Portal & Backend Architecture

- **Source Files:**
  - Routing: [`app/page.tsx`](file:///d:/code/project1/Dentara/app/page.tsx), [`app/clinic/`](file:///d:/code/project1/Dentara/app/clinic/), [`app/(auth)/signin/`](file:///d:/code/project1/Dentara/app/(auth)/signin/)
  - Actions: [`actions/auth.ts`](file:///d:/code/project1/Dentara/actions/auth.ts), [`actions/clinical.ts`](file:///d:/code/project1/Dentara/actions/clinical.ts)
  - Security & DB: [`lib/db.ts`](file:///d:/code/project1/Dentara/lib/db.ts), [`lib/password.ts`](file:///d:/code/project1/Dentara/lib/password.ts), [`lib/auth.ts`](file:///d:/code/project1/Dentara/lib/auth.ts), [`lib/authz.ts`](file:///d:/code/project1/Dentara/lib/authz.ts), [`lib/audit.ts`](file:///d:/code/project1/Dentara/lib/audit.ts)
- **Implemented Capabilities:**
  - Next.js 14+ Server Actions with zero client-side exposed database credentials.
  - Server-side password derivation via `scrypt` ($N=65536, r=8, p=2, 32\text{-byte salt}$).
  - Connection-pooled Neon Serverless PostgreSQL client with automated migration pipeline ([`scripts/migrate.ts`](file:///d:/code/project1/Dentara/scripts/migrate.ts)).
  - Strict security headers (CSP, HSTS, `X-Frame-Options: DENY`, `no-store` cache controls).
