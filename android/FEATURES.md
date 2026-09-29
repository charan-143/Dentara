# Practix Android Application — Feature Matrix & Codebase Specification

**Application Name:** Practix (formerly Thornbury Dental / Dentara)  
**Package:** `com.dentara.dental` (`com.example.thornburydental`)  
**Target SDK:** Android API 36 (Min SDK 24, Target SDK 36)  
**Architecture:** Modern Android Architecture (Jetpack Compose M3, Clean Layered Architecture, Kotlin Coroutines/Flow, Native C++/NDK, SQLCipher Encrypted Persistence)  
**Status:** Implemented & Verified in Codebase  

---

## 1. System Architecture & Capabilities Overview

```
========================================================================================================
                               PRACTIX ANDROID APPLICATION CAPABILITIES
========================================================================================================
┌───────────────────────────────────────┬──────────────────────────────────────────────────────────────┐
│ CORE SUBSYSTEM                        │ KEY IMPLEMENTED MODULES & CAPABILITIES                       │
├───────────────────────────────────────┼──────────────────────────────────────────────────────────────┤
│ 🎙️ Voice Dictation & Speech Engine     │ • whisper.cpp native C++ SIMD inference (NEON/AVX2)          │
│                                       │ • Continuous audio circular buffer streaming ($16\text{ kHz}$)│
│                                       │ • Left-to-right deterministic parsing (FDI & Universal)      │
│                                       │ • 6-site periodontal depth auto-mapping & single-word undo   │
│                                       │ • Real-time Text-to-Speech (TTS) auditory read-back          │
├───────────────────────────────────────┼──────────────────────────────────────────────────────────────┤
│ 🦷 3D Odontogram & Anatomical Visual  │ • Google Filament PBR rendering engine & OpenGL ES fallback  │
│                                       │ • Full anatomical 3D molar mesh (.obj loader) & surface tags │
│                                       │ • Maxillary & Mandibular arch quadrant visualizers           │
│                                       │ • Real-time condition shaders (Caries, Restorations, Crowns) │
├───────────────────────────────────────┼──────────────────────────────────────────────────────────────┤
│ 🧠 Multi-Agent Clinical AI & CDS      │ • On-device Llama 3.2 / Gemma 2 GGUF engine (llama.cpp)      │
│                                       │ • 5-Agent Orchestration (Intake, Radiology, Safety, etc.)    │
│                                       │ • Dynamic RAM Tiering (1B / 2B / 3B model auto-scaling)      │
│                                       │ • Pharmacological Red-Flag Sentinel (Allergies, NSAID, MRONJ)│
│                                       │ • Cloud Gemini 2.0 Multimodal fallback with de-identification│
├───────────────────────────────────────┼──────────────────────────────────────────────────────────────┤
│ 🔒 Cryptographic Security & Audit     │ • SQLCipher 256-bit AES database encryption                  │
│                                       │ • Android Hardware Keystore & BiometricPrompt authentication │
│                                       │ • Tamper-evident append-only HMAC-SHA256 audit hash chain    │
│                                       │ • 15-min idle timeout & FLAG_SECURE window protection        │
├───────────────────────────────────────┼──────────────────────────────────────────────────────────────┤
│ 🏥 Practice Management & Operatory    │ • Today Queue & Day-Grid appointment scheduler               │
│                                       │ • Patient demographics & immutable treatment plans           │
│                                       │ • Preset prescription manager & vector PDF chart exporter    │
│                                       │ • AlarmManager automated morning briefing & arrival alerts   │
└───────────────────────────────────────┴──────────────────────────────────────────────────────────────┘
```

---

## 2. Navigation & User Journey Flows

### 2.1 Navigation Architecture
- **Source File:** [`Navigation.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/Navigation.kt)
- **Root Routing States:**
  - `WELCOME` $\rightarrow$ Brand presentation and clinic greeting ([`WelcomeBrandScreen.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/onboarding/WelcomeBrandScreen.kt)).
  - `ONBOARDING_WIZARD` $\rightarrow$ Setup guide for clinic profile, biometrics, preferred numbering system (FDI vs Universal), and voice model initialization ([`OnboardingWizardScreen.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/onboarding/OnboardingWizardScreen.kt)).
  - `CLINIC` $\rightarrow$ Main practice operatory workspace with bottom navigation tabs.

### 2.2 Main Operatory Tabs
1. **Today Queue (`TODAY`):** Real-time daily patient queue, check-in status toggles, operatory chair timers, and quick patient activation.
2. **Patient Directory (`PATIENTS`):** Comprehensive patient roster, searchable by name/MRN/tags, risk-level indicators, and deep links into full anatomical dental charts.
3. **Clinical AI Copilot (`CDS`):** Multi-agent clinical intelligence console displaying real-time intake synthesis, differential diagnoses, evidence citations, and drug contraindication sentinel alerts.
4. **Appointment Scheduler (`SCHEDULE`):** Day-grid operatory calendar, provider scheduling, chair conflict prevention, and appointment booking modals.
5. **Clinician Profile & Settings (`PROFILE`):** Clinician credentials, biometric security configurations, tooth numbering standard selection, medication preset manager, and encrypted database diagnostics.

---

## 3. Hands-Free Voice Charting & Speech Recognition

### 3.1 Native Whisper Inference Engine
- **Source Files:**
  - Native C++: [`CMakeLists.txt`](file:///d:/code/project1/Dentara/android/app/src/main/cpp/CMakeLists.txt), [`whisper.cpp`](file:///d:/code/project1/Dentara/android/app/src/main/cpp/whisper.cpp)
  - Native Bridge: [`WhisperNative.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/WhisperNative.kt), [`WhisperEngine.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/WhisperEngine.kt)
  - Model Management: [`SpeechModel.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/model/SpeechModel.kt), [`SpeechModelStore.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/model/SpeechModelStore.kt), [`SpeechModelDownloader.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/model/SpeechModelDownloader.kt)
- **Features:**
  - High-performance, zero-latency on-device speech-to-text inference compiled with Clang via NDK 28 with NEON SIMD optimizations for `arm64-v8a` and AVX2 for `x86_64`.
  - Direct memory mapping (`mmap`) of GGML quantized model weights (`whisper-tiny.bin` / `whisper-base.bin`) for minimal heap overhead.
  - Zero-copy native buffer passing with automatic $16\text{ kHz}$ mono Float32 normalization.

### 3.2 Continuous Hands-Free Audio Capture Service
- **Source Files:**
  - [`VoiceChartingService.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/VoiceChartingService.kt)
  - [`AudioRecordManager.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/AudioRecordManager.kt)
  - [`VoiceActivitySegmenter.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/VoiceActivitySegmenter.kt)
  - [`VoiceChartingController.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/VoiceChartingController.kt)
- **Features:**
  - Dedicated Android Foreground Service with continuous operatory microphone status notification.
  - Adaptive energy-based Voice Activity Detection (VAD) that filters out dental handpiece/drill background noise while capturing discrete dental findings.
  - Ring buffer acoustic streaming with automatic memory sanitization on session completion.

### 3.3 Deterministic Clinical Command Parser & Verification
- **Source Files:**
  - [`VoiceCommandParser.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/VoiceCommandParser.kt)
  - [`ToothNumberingSystem.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/ToothNumberingSystem.kt)
  - [`ChartReadBack.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/speech/ChartReadBack.kt)
  - [`HandsFreeVoiceDictationBar.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/components/speech/HandsFreeVoiceDictationBar.kt)
  - [`ParsedCommandPreviewSheet.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/components/speech/ParsedCommandPreviewSheet.kt)
- **Features:**
  - **Left-to-Right Token Scanner:** Differentiates tooth numbers, tooth surfaces (Mesial, Occlusal, Distal, Buccal, Lingual), condition classifications, and millimeter pocket depths.
  - **Dual Tooth Numbering Systems:** Supports Universal System (1–32) and FDI Two-Digit Notation (11–48 permanent, 51–85 primary) with scheme-level boundary validation.
  - **Automated 6-Site Perio Sequence Mapping:** Sequential dictation of 6 depth measurements automatically maps to Distobuccal $\rightarrow$ Buccal $\rightarrow$ Mesiobuccal $\rightarrow$ Distolingual $\rightarrow$ Lingual $\rightarrow$ Mesiolingual.
  - **Bleeding on Probing (BOP):** Detects *"bleeding"*, *"BOP"*, or *"plus"* qualifiers per pocket site.
  - **Voice Undo & Reversible Transactions:** Command parsing recognizes *"undo that"*, *"cancel last"*, or *"revert"* and executes instant state rollback via [`VoiceUndoDao.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/db/VoiceUndoDao.kt).
  - **Auditory Read-Back (TTS):** Synthesizes immediate voice feedback for chairside verification without requiring the dentist to glance at the display.

---

## 4. Interactive 3D Odontogram & Anatomical Visualizer

### 4.1 3D Rendering Engine (Google Filament & OpenGL ES)
- **Source Files:**
  - [`FilamentToothView.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/components/FilamentToothView.kt)
  - [`DentalModelView.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/components/DentalModelView.kt)
  - [`Molar3DView.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/components/molar3d/Molar3DView.kt)
  - [`MolarGLRenderer.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/components/molar3d/MolarGLRenderer.kt)
  - [`MolarObjParser.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/components/molar3d/MolarObjParser.kt)
  - 3D Asset: `mandibular-first-molar.obj`
- **Features:**
  - High-fidelity Physically Based Rendering (PBR) simulating natural tooth enamel subsurface light scattering and specular reflections.
  - Intuitive gesture controls: $360^\circ$ continuous orbit rotation, multi-touch pinch-to-zoom, and dual-axis pan.
  - Surface-level geometric hit-testing for anatomical zones (Occlusal, Mesial, Distal, Buccal, Lingual).

### 4.2 Dental Quadrants & Anatomical Chart Views
- **Source Files:**
  - [`MaxillaryQuadrantViews.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/MaxillaryQuadrantViews.kt)
  - [`MandibularQuadrantViews.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/MandibularQuadrantViews.kt)
  - [`AnatomicalToothVisual.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/components/AnatomicalToothVisual.kt)
- **Features:**
  - Dual arch views displaying all 4 quadrants (Maxillary Right/Left, Mandibular Left/Right).
  - High-contrast clinical status color coding:
    - **Active Caries / Decay:** Crimson / Coral indicator.
    - **Existing Restorations (Composite / Amalgam / Inlay):** Deep Cobalt Blue / Metallic Silver overlay.
    - **Crown & Prosthetics:** Porcelain Ceramic / Gold badge.
    - **Endodontic Treatment (Root Canal):** Canal obturation line rendering.
    - **Incipient Lesion / Watch:** Warning Amber contour.
    - **Extracted / Missing:** Translucent grey hatch pattern.

---

## 5. Multi-Agent Clinical AI & Decision Support (CDS)

### 5.1 Multi-Agent Clinical Orchestration Architecture
- **Source Files:**
  - [`MultiAgentClinicalOrchestrator.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/MultiAgentClinicalOrchestrator.kt)
  - [`ClinicalAgentModels.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/ClinicalAgentModels.kt)
  - [`CdsCopilotScreen.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/cds/CdsCopilotScreen.kt)
- **Integrated Clinical Agents:**
  1. **Intake & Context Synthesizer (`INTAKE_SYNTHESIZER`):** Aggregates patient age, vitals, medical history, active medications, and current dental chart conditions into a structured clinical snapshot.
  2. **Multimodal Dental Radiology Agent (`MULTIMODAL_RADIOLOGY`):** Evaluates periapical radiographs, bitewings, panoramic radiographs, and imaging reports to identify bone loss, periapical radiolucencies, and secondary caries.
  3. **Evidence & Differential Synthesis Agent (`EVIDENCE_DIFFERENTIAL`):** Cross-references ADA, AAP, and AAE guidelines to formulate ranked differential diagnoses and evidence-based treatment suggestions.
  4. **Pharmacological & Red-Flag Safety Sentinel (`SAFETY_GUARDRAIL`):** Scans for contraindications, severe drug-drug interactions, and systemic allergy conflicts.
  5. **Master Clinical Orchestrator (`MASTER_ORCHESTRATOR`):** Synthesizes agent inputs into a cohesive clinical report with streaming UI updates.

### 5.2 On-Device LLM Runtime & Dynamic RAM Tiering
- **Source Files:**
  - [`OnDeviceLlmEngine.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/OnDeviceLlmEngine.kt)
  - [`LlamaInferenceBridge.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/LlamaInferenceBridge.kt)
  - [`LlmConfig.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/LlmConfig.kt)
  - [`ClinicalEmbeddingEngine.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/ClinicalEmbeddingEngine.kt)
  - [`ClinicalCorpusRepository.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/ClinicalCorpusRepository.kt)
- **Features:**
  - **Dynamic Hardware Tiering:** Queries system RAM via `ActivityManager.MemoryInfo`:
    - **Tier A ($\ge 6\text{ GB}$ Available):** Loads `Llama-3.2-3B-Instruct (Q4_0, ~2.0 GB)`.
    - **Tier B ($\ge 4\text{ GB}$ Available):** Loads `Gemma-2-2B-IT (Q4_0, ~1.4 GB)`.
    - **Tier C ($< 4\text{ GB}$ Available):** Loads `Llama-3.2-1B-Instruct (Q4_0, ~0.8 GB)`.
  - **Barge-In Controller:** Immediate generation cancellation upon user interruption or new priority task arrival.
  - **Vector Retrieval-Augmented Generation (RAG):** Cosine similarity matching against on-device clinical guidelines corpus.

### 5.3 Pharmacological Red-Flag Safety Checker
- **Source Files:**
  - [`RedFlagSafetyChecker.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/RedFlagSafetyChecker.kt)
  - [`ClinicianSuggestionVerifier.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/ClinicianSuggestionVerifier.kt)
- **Features:**
  - **Penicillin & Beta-Lactam Cross-Allergy:** Blocks Amoxicillin, Augmentin, and Cephalosporins when penicillin allergy is documented.
  - **Anticoagulant $\leftrightarrow$ NSAID Hemorrhage Risk:** Generates critical alerts when NSAIDs (Ibuprofen, Ketorolac, Naproxen) are prescribed to patients taking Warfarin, Apixaban (Eliquis), Rivaroxaban (Xarelto), or Clopidogrel (Plavix).
  - **Bisphosphonate $\leftrightarrow$ MRONJ Surgical Risk:** Blocks invasive dentoalveolar surgery and extractions for patients on Alendronate, Zoledronic acid, or Denosumab without explicit clinical override documentation.

### 5.4 Cloud Multimodal Radiography Fallback
- **Source Files:**
  - [`GeminiMultimodalClient.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/GeminiMultimodalClient.kt)
  - [`MultimodalReportProcessor.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/MultimodalReportProcessor.kt)
  - [`OnDeviceMultimodalVlmEngine.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/cds/OnDeviceMultimodalVlmEngine.kt)
- **Features:**
  - High-resolution visual analysis of dental radiographs and intraoral photography using Gemini 1.5 / 2.0 API.
  - Automatic de-identification layer stripping patient names, MRNs, and metadata prior to cloud transmission.

---

## 6. Cryptographic Security, Audit & Authentication

### 6.1 Encrypted Persistence (SQLCipher AES-256)
- **Source Files:**
  - [`LocalDatabaseManager.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/db/LocalDatabaseManager.kt)
  - [`ThornburyDbHelper.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/db/ThornburyDbHelper.kt)
  - [`KeyStoreManager.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/security/KeyStoreManager.kt)
- **Features:**
  - Complete 256-bit AES page-level encryption for all SQLite tables via SQLCipher binaries.
  - Master key derivation secured inside the hardware-backed Android KeyStore (`AndroidKeyStore` provider with AES-256 GCM).
  - In-memory key zeroing immediately following database unlock.

### 6.2 Biometric Lock & Privacy Protection
- **Source Files:**
  - [`BiometricAuthManager.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/security/BiometricAuthManager.kt)
  - [`BiometricLockOverlay.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/components/BiometricLockOverlay.kt)
  - [`AppSessionLifecycleObserver.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/security/AppSessionLifecycleObserver.kt)
- **Features:**
  - Class-3 Strong BiometricPrompt (Fingerprint / 3D Iris / Face Unlock) integration with PIN fallback.
  - Automatic 15-minute inactivity auto-lock overlay preserving operatory privacy when stepping away from the chair.
  - `FLAG_SECURE` window protection enabled across the application lifecycle to prevent OS task switcher thumbnail caching and screenshot data leakage.

---

## 7. Clinical Data Management & DAOs

### 7.1 Data Access Object (DAO) Matrix
- **Package:** `com.example.thornburydental.data.db`
- **Implemented DAOs:**

| DAO Class | Scope & Responsibilities |
| :--- | :--- |
| [`PatientDao.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/db/PatientDao.kt) | Full CRUD for patient demographic profiles, medical histories, alerts, and allergy arrays. |
| [`ToothDao.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/db/ToothDao.kt) | Per-tooth & per-surface condition records, pocket depths, restorations, and numbering system mapping. |
| [`TreatmentPlanDao.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/db/TreatmentPlanDao.kt) | Multi-phase treatment planning, cost estimation, item status, and published plan immutability triggers. |
| [`PrescriptionDao.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/db/PrescriptionDao.kt) | Prescription orders, dosage frequencies, durations, prescriber IDs, and clinical override logs. |
| [`AppointmentDao.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/db/AppointmentDao.kt) | Operatory chair scheduling, appointment statuses (Scheduled, Arrived, In-Chair, Completed), and conflict checks. |
| [`ReportDao.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/db/ReportDao.kt) | Diagnostic reports, radiology scans, laboratory findings, and file attachment URI management. |
| [`MedicationPresetDao.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/db/MedicationPresetDao.kt) | Quick-select prescription presets (e.g., Amoxicillin 500mg, Ibuprofen 600mg, Chlorhexidine 0.12%). |
| [`VoiceUndoDao.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/db/VoiceUndoDao.kt) | Persistent state snapshots for seamless chairside voice command undos and rollbacks. |
| [`UserPreferencesDao.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/db/UserPreferencesDao.kt) | Clinician preferences for tooth numbering system, auto-lock timeout, theme, and default operatory chair. |
| [`UserDao.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/data/db/UserDao.kt) | Provider profiles, licensure records, and local authentication credentials. |

---

## 8. Clinical Workflows, Screens & Dialogs

### 8.1 Primary UI Screens
- [`TodayQueueScreen.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/TodayQueueScreen.kt): Daily clinic operatory flow, patient check-in timeline, active appointment cards, and quick-action buttons.
- [`PatientRosterScreen.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/PatientRosterScreen.kt): Searchable patient list with instant query filtering, risk category chips (High Medical Risk, Pre-Med Required), and navigation to charts.
- [`RegisterPatientScreen.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/RegisterPatientScreen.kt): Comprehensive patient registration form covering demographics, medical history checklists, allergy chips, and emergency contacts.
- [`PatientDetailChartScreen.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/PatientDetailChartScreen.kt): Central dental chart featuring:
  - Header with patient summary & medical alerts.
  - Interactive Odontogram with 3D/2D quadrant toggle.
  - 6-point periodontal pocket depth charting matrix.
  - Diagnosis Tab ([`PatientDiagnosisTabView.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/PatientDiagnosisTabView.kt)).
  - Treatment Plans Tab ([`PatientTreatmentPlansTabView.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/PatientTreatmentPlansTabView.kt)).
  - Imaging & Diagnostic Reports Tab ([`PatientReportsImagingTabView.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/PatientReportsImagingTabView.kt)).
  - Clinical Examination Notes Section ([`ExaminationQuestionsSection.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/ExaminationQuestionsSection.kt)).
- [`ScheduleScreen.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/ScheduleScreen.kt): Day-grid operatory scheduler with visual appointment blocks and provider time allocations.
- [`ProfileScreen.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/ProfileScreen.kt): Provider settings, numbering scheme selection, security toggles, and database management.

### 8.2 Clinical Dialogs & Operatory Modals
- [`BookAppointmentDialog.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/BookAppointmentDialog.kt): Operatory booking with date/time pickers and duration presets.
- [`CreateTreatmentPlanDialog.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/CreateTreatmentPlanDialog.kt): Phased treatment plan creator (Phase 1 Urgent, Phase 2 Restorative, Phase 3 Maintenance).
- [`EditDiagnosisDialog.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/EditDiagnosisDialog.kt): Per-tooth clinical diagnosis editing with condition selectors and notes.
- [`IssuePrescriptionDialog.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/IssuePrescriptionDialog.kt): Prescription generator with safety alerts and dosage templates.
- [`ManagePresetsDialog.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/ManagePresetsDialog.kt): Customization dialog for standard clinic prescription templates.
- [`AddReportScreen.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/AddReportScreen.kt): Attachment dialog for radiographs, photos, and lab PDFs.
- [`ReportViewerLightboxDialog.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/ReportViewerLightboxDialog.kt): Full-screen zoomable radiograph lightbox viewer.
- [`SharePatientPdfDialog.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/ui/clinic/SharePatientPdfDialog.kt): Export dialog for generating and sharing encrypted patient charts.

---

## 9. Vector PDF Export & System Sharing

### 9.1 Native PDF Generation
- **Source Files:**
  - [`PatientPdfGenerator.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/export/PatientPdfGenerator.kt)
  - [`PatientShareOptions.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/export/PatientShareOptions.kt)
- **Features:**
  - Vector PDF rendering using Android `android.graphics.pdf.PdfDocument` generating A4 clinical charts.
  - Complete document layout:
    - Practice header and clinician licensure metadata.
    - Patient demographics, medical alert banner, and allergy summary.
    - Full 32-tooth odontogram condition matrix table with pocket depths.
    - Phased treatment plans with itemized procedure codes and cost estimates.
    - Active prescriptions with dosage and refill instructions.
    - Document verification footer with cryptographic SHA-256 hash.
  - Integration with Android `FileProvider` for secure sharing via Android Intent (Email, Print, Cloud Drive).

---

## 10. Automated Reminders & Alarm Framework

### 10.1 Operatory Scheduling & Morning Briefing Alarms
- **Source Files:**
  - [`DentaraAlarmReceiver.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/reminder/DentaraAlarmReceiver.kt)
  - [`ReminderManager.kt`](file:///d:/code/project1/Dentara/android/app/src/main/java/com/example/thornburydental/reminder/ReminderManager.kt)
- **Features:**
  - Uses Android `AlarmManager.setExactAndAllowWhileIdle` (`SCHEDULE_EXACT_ALARM`) for battery-efficient operatory alerts.
  - Daily 07:30 AM automated clinic briefing summarizing today's booked cases, high-risk patients, and operatory chair allocations.
  - Real-time patient arrival notifications alerting chairside clinicians.
  - `RECEIVE_BOOT_COMPLETED` receiver restoring pending clinic alarms following device reboots.

---

## 11. Codebase Directory & Source File Mapping

```
android/
├── build.gradle.kts                      # Root Gradle build configuration
├── settings.gradle.kts                   # Project module definitions
├── gradle.properties                     # JVM and Gradle memory tuning
├── mandibular-first-molar.obj            # 3D anatomical molar polygon mesh asset
└── app/
    ├── build.gradle.kts                  # App module configuration (NDK 28, Compose, SQLCipher, Filament)
    └── src/
        └── main/
            ├── AndroidManifest.xml       # Permissions, Services, Receivers, and FileProvider
            ├── cpp/                      # Native C++ Whisper Inference Engine
            │   ├── CMakeLists.txt        # NDK CMake build configuration
            │   └── whisper.cpp/          # Native whisper.cpp SIMD inference runtime
            ├── assets/                   # Bundled assets (3D meshes, clinical corpus)
            └── java/com/example/thornburydental/
                ├── MainActivity.kt       # Single-activity Jetpack Compose host
                ├── Navigation.kt         # Navigation graph & root destination controller
                ├── ThornburyApplication.kt # Application lifecycle & DB initialization
                ├── data/                 # Data Layer (Repositories, Models, Systems)
                │   ├── DentalRepository.kt
                │   ├── AuthRepository.kt
                │   ├── Models.kt
                │   ├── NetworkClient.kt
                │   ├── ToothNumberingSystem.kt
                │   ├── VoiceChartEntry.kt
                │   ├── cds/              # Multi-Agent Clinical AI & Decision Support
                │   │   ├── MultiAgentClinicalOrchestrator.kt
                │   │   ├── ClinicalAgentModels.kt
                │   │   ├── OnDeviceLlmEngine.kt
                │   │   ├── LlamaInferenceBridge.kt
                │   │   ├── LlmConfig.kt
                │   │   ├── RedFlagSafetyChecker.kt
                │   │   ├── ClinicianSuggestionVerifier.kt
                │   │   ├── ClinicalEmbeddingEngine.kt
                │   │   ├── ClinicalCorpusRepository.kt
                │   │   ├── GeminiMultimodalClient.kt
                │   │   └── MultimodalReportProcessor.kt
                │   ├── db/               # Encrypted SQLite / SQLCipher Persistence & DAOs
                │   │   ├── LocalDatabaseManager.kt
                │   │   ├── ThornburyDbHelper.kt
                │   │   ├── DbConverters.kt
                │   │   ├── PatientDao.kt
                │   │   ├── ToothDao.kt
                │   │   ├── TreatmentPlanDao.kt
                │   │   ├── PrescriptionDao.kt
                │   │   ├── AppointmentDao.kt
                │   │   ├── ReportDao.kt
                │   │   ├── UserDao.kt
                │   │   ├── MedicationPresetDao.kt
                │   │   ├── VoiceUndoDao.kt
                │   │   └── UserPreferencesDao.kt
                │   └── security/         # Cryptography, Keystore & Biometrics
                │       ├── KeyStoreManager.kt
                │       ├── BiometricAuthManager.kt
                │       └── AppSessionLifecycleObserver.kt
                ├── speech/               # Speech-to-Text & Voice Charting Engine
                │   ├── WhisperEngine.kt
                │   ├── WhisperNative.kt
                │   ├── AudioRecordManager.kt
                │   ├── VoiceChartingService.kt
                │   ├── VoiceChartingController.kt
                │   ├── VoiceChartingViewModel.kt
                │   ├── VoiceCommandParser.kt
                │   ├── VoiceActivitySegmenter.kt
                │   ├── ChartReadBack.kt
                │   ├── MicrophonePermission.kt
                │   └── model/            # GGML Speech Model Distribution
                ├── ui/                   # Jetpack Compose Presentation Layer
                │   ├── clinic/           # Operatory & Practice Management Screens
                │   │   ├── TodayQueueScreen.kt
                │   │   ├── PatientRosterScreen.kt
                │   │   ├── RegisterPatientScreen.kt
                │   │   ├── PatientDetailChartScreen.kt
                │   │   ├── ScheduleScreen.kt
                │   │   ├── ProfileScreen.kt
                │   │   ├── MaxillaryQuadrantViews.kt
                │   │   ├── MandibularQuadrantViews.kt
                │   │   ├── PatientDiagnosisTabView.kt
                │   │   ├── PatientTreatmentPlansTabView.kt
                │   │   ├── PatientReportsImagingTabView.kt
                │   │   ├── ExaminationQuestionsSection.kt
                │   │   ├── CreateTreatmentPlanDialog.kt
                │   │   ├── IssuePrescriptionDialog.kt
                │   │   ├── BookAppointmentDialog.kt
                │   │   ├── ManagePresetsDialog.kt
                │   │   ├── EditDiagnosisDialog.kt
                │   │   ├── AddReportScreen.kt
                │   │   ├── ReportViewerLightboxDialog.kt
                │   │   └── SharePatientPdfDialog.kt
                │   ├── cds/              # AI Copilot UI
                │   │   ├── CdsCopilotScreen.kt
                │   │   └── TreatmentPlanCard.kt
                │   ├── components/       # Custom Reusable Composables & 3D Views
                │   │   ├── AnatomicalToothVisual.kt
                │   │   ├── DentalModelView.kt
                │   │   ├── FilamentToothView.kt
                │   │   ├── BiometricLockOverlay.kt
                │   │   ├── molar3d/      # OpenGL ES Molar Polygon Renderer
                │   │   └── speech/       # Voice Dictation Floating Controls
                │   └── onboarding/       # Setup & Brand Screens
                ├── export/               # Vector PDF Generation & Document Sharing
                │   ├── PatientPdfGenerator.kt
                │   └── PatientShareOptions.kt
                └── reminder/             # Alarms & Background Notifications
                    ├── DentaraAlarmReceiver.kt
                    └── ReminderManager.kt
```
