# SUITE 2: LEGAL & APP STORE COMPLIANCE (PRE-RELEASE)
**Application:** Practix  
**Sole Proprietor & Principal Developer:** Devanapally Charan Tej  
**Document Version:** 1.0.0-LEGAL  
**Jurisdictions Covered:** United States (HIPAA / HITECH), India (DPDP Act 2023 / DISHA / ABDM), European Union & United Kingdom (GDPR / UK DPA 2018), Global App Distribution  

---

## 1. Privacy Policy

**Effective Date:** October 1, 2026  
**Last Updated:** October 1, 2026  

### 1.1 Overview & Architecture Statement
Practix ("we," "our," or "the Platform"), developed and operated by sole proprietor **Devanapally Charan Tej**, is a clinical practice workspace engineered specifically for dental professionals. Practix operates on an **Offline-First and Local-Sovereignty Architecture**. Unlike traditional cloud-dependent Electronic Health Record (EHR) platforms, all clinical charting, voice dictations, patient demographics, and odontogram records are stored locally on your device in a 256-bit AES encrypted SQLite database (SQLCipher). We do not monetize, harvest, sell, or profile patient Protected Health Information (PHI) or personal data.

---

### 1.2 Information We Collect & Data Flow Mechanics

```
+--------------------------+----------------------------------------+-------------------------------------------------------------+
| Category of Data         | Method of Processing                   | Storage Location & Retention                                |
+--------------------------+----------------------------------------+-------------------------------------------------------------+
| Patient Clinical Records | Input via UI or Hands-Free Dictation   | Local Encrypted SQLCipher Database on Device.               |
| (PHI / SPDI)             | processed 100% on-device (whisper.cpp) | Never transmitted to Practix servers unless practice-      |
|                          |                                        | configured cloud sync is explicitly enabled.                |
+--------------------------+----------------------------------------+-------------------------------------------------------------+
| Audio Dictation Streams  | Captured via Android Microphone        | Ephemeral memory buffer only. Streamed to native C++        |
|                          | for real-time speech-to-text           | Whisper engine. Purged immediately upon token generation.   |
|                          |                                        | Never saved as unencrypted raw audio files to disk.         |
+--------------------------+----------------------------------------+-------------------------------------------------------------+
| Clinician Authentication | Local Biometrics (BiometricPrompt)     | Biometric templates remain isolated in device Hardware      |
| & Credentials            | and scrypt-hashed passwords            | Secure Element (TEE/StrongBox); non-exportable.             |
+--------------------------+----------------------------------------+-------------------------------------------------------------+
| Diagnostics / Crash Logs | Opt-in anonymized system error logs    | Ephemeral; stripped of all clinical patient identifiers     |
|                          |                                        | and chart content before transmission.                      |
+--------------------------+----------------------------------------+-------------------------------------------------------------+
```

---

### 1.3 Regional Healthcare & Data Privacy Compliance

#### A. United States: HIPAA & HITECH Act Compliance
- **Business Associate Agreement (BAA):** For practices utilizing optional Practix Cloud synchronization or multi-operatory backup services, Devanapally Charan Tej (Practix) executes a standard Business Associate Agreement governing the handling of Electronic Protected Health Information (ePHI) pursuant to 45 CFR § 164.502(e) and § 164.504(e).
- **Technical Safeguards (§ 164.312):**
  - *Encryption at Rest (§ 164.312(a)(2)(iv)):* Implemented via SQLCipher AES-256 with key derivation managed by the Android Hardware-Backed Keystore.
  - *Automatic Logoff (§ 164.312(a)(2)(iii)):* App enforces automatic session lockout after 15 minutes of inactivity.
  - *Audit Controls (§ 164.312(b)):* Cryptographically hash-chained, append-only SQLite audit log tracking every chart read, edit, and export.

#### B. India: Digital Personal Data Protection (DPDP) Act, 2023
- **Data Fiduciary vs. Data Processor:** The licensed Dental Practice acts as the Data Fiduciary; Practix (Devanapally Charan Tej) operates strictly as the Data Processor/Technology Provider.
- **Grounds for Processing:** Processing of digital personal health data is conducted solely for healthcare diagnosis, clinical treatment planning, and medical safety verification pursuant to Section 7 of the DPDP Act.
- **Patient Rights:** Support for data principals to request summary reports, corrections, or erasure via the Clinician Management console.

#### C. European Union & United Kingdom: GDPR / UK GDPR
- **Lawful Basis:** Processing is necessary for the purposes of preventive/occupational medicine and the provision of health treatment (GDPR Article 9(2)(h)).
- **Data Sovereignty:** Data remains local to the clinician’s physical terminal. Where cloud backup is contracted, servers reside within ISO 27001/SOC 2 compliant data centers located within the EU/UK.

---

### 1.4 Third-Party Services & Sub-Processors
When optional advanced cloud features are enabled:
1. **Google Cloud / Gemini API (Multimodal Diagnostic Fallback):** Transmits strictly de-identified radiographic images for diagnostic assistance. No direct patient identifiers (Name, DOB, Social Security Number/Aadhaar) are sent.
2. **Neon Database & Vercel (Web Companion Portal):** Operates under enterprise data protection addenda with automated database-level encryption.

---

### 1.5 Data Retention & Deletion
Clinicians retain 100% control over local data. Uninstalling the application or selecting **"Purge Practice Database"** from the authenticated settings menu permanently overwrites the SQLCipher encryption key, rendering all historical local records cryptographically irrecoverable.

---

## 2. Terms of Service (ToS) / End User License Agreement (EULA)

**PLEASE READ THIS AGREEMENT CAREFULLY. BY DOWNLOADING, INSTALLING, OR USING PRACTIX, YOU AGREE TO BE BOUND BY THESE TERMS.**

### 2.1 License Grant & Professional Use Restriction
Devanapally Charan Tej ("Licensor") grants the purchasing clinical professional or dental institution a non-exclusive, non-transferable, revocable, limited commercial license to install and execute Practix on authorized devices solely for lawful dental practice management, clinical recordkeeping, and diagnostic assistance.

**Professional Certification Requirement:** You expressly warrant that you are a licensed dentist, dental specialist, registered dental hygienist, or an authorized clinical agent acting under the direct supervision of a licensed dental practitioner. Practix is not intended for consumer, layperson, or uncertified self-diagnosis.

---

### 2.2 Clinician Responsibility & Independent Medical Judgment
1. **Tool of Convenience Only:** Practix, its voice dictation parser, 3D anatomical models, and On-Device Clinical Decision Support (CDS) algorithms are auxiliary clinical tools designed to assist licensed practitioners.
2. **Sole Responsibility:** The attending clinician bears the **sole and exclusive legal and professional responsibility** for verifying all chart entries, tooth notations, periodontal measurements, prescriptions, dosages, and treatment plans before administering care or releasing records.
3. **No Practice of Medicine:** Practix does not practice dentistry, dispense medical advice, or replace clinical examination, radiological confirmation, and pathology testing.

---

### 2.3 Prohibited Conduct
Users shall not:
- Reverse engineer, decompile, disassemble, or attempt to extract the underlying source code of native binaries (`libpractix_native.so`, `whisper.cpp`, or proprietary GGUF model weights).
- Bypass, modify, or disable the SQLCipher encryption mechanisms, cryptographic audit hash triggers, or biometric authentication overlays.
- Utilize the platform to record audio of patients without obtaining proper statutory consent as mandated by applicable wiretapping or healthcare consent laws in your jurisdiction.

---

### 2.4 Limitation of Liability & Warranty Disclaimer
TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW:
- PRACTIX IS PROVIDED **"AS IS"** AND **"AS AVAILABLE"** WITHOUT WARRANTIES OF ANY KIND, EXPRESS OR IMPLIED.
- IN NO EVENT SHALL DEVANAPALLY CHARAN TEJ OR AFFILIATES BE LIABLE FOR ANY INDIRECT, INCIDENTAL, CONSEQUENTIAL, SPECIAL, OR PUNITIVE DAMAGES, INCLUDING BUT NOT LIMITED TO CLINICAL MALPRACTICE CLAIMS, MISDIAGNOSES, MEDICATION DISPENSING ERRORS, PATIENT INJURY, LOSS OF PROFITS, OR DATA LOSS ARISING OUT OF THE USE OF OR INABILITY TO USE THE APPLICATION.
- LICENSOR’S TOTAL AGGREGATE LIABILITY SHALL NOT EXCEED THE TOTAL AMOUNT ACTUALLY PAID BY YOU FOR THE SOFTWARE LICENSE DURING THE TWELVE (12) MONTHS IMMEDIATELY PRECEDING THE CLAIM.

---

## 3. Medical, Professional & Third-Party Disclaimers

### 3.1 Statutory Clinical Decision Support (CDS) Disclaimer
> **IMPORTANT CLINICAL NOTICE:**  
> The Clinical Decision Support (CDS) features, automated red-flag warnings, and on-device language model suggestions (including Llama 3.2 and Gemma 2 runtimes) provided within Practix do not constitute medical diagnoses, definitive treatment directives, or clinical prognoses.  
> 
> The algorithms evaluate entered clinical parameters against standardized statistical rules and medical corpora. They may not account for rare clinical presentations, atypical comorbidities, emergent contraindications, or erroneous data input. The clinician must independently cross-examine all suggested drug interactions, antibiotic regimens, and surgical interventions against current professional guidelines (e.g., ADA, CDC, BNF, or CDSCO).

---

### 3.2 Prescribing Safety & Drug Interaction Limitations
The built-in Prescribing Safety Sentinel verifies entries against a curated database of common dental medications, anticoagulant interactions, and bisphosphonate-related osteonecrosis risks. **Practix is not an exhaustive pharmacopeia.** It does not replace reference to official drug package inserts, national formulary databases, or comprehensive pharmacology consultation.

---

### 3.3 Patient Consent for Voice Recording Notice
Practix processes operatory voice streams locally on the device hardware. Clinicians are strictly responsible for complying with federal, state, and local patient consent laws (e.g., US Two-Party Consent States, GDPR Article 6/9 requirements) prior to activating active microphone capture in the presence of a patient.

---

## 4. App Store & Google Play Metadata (ASO Optimized)

### 4.1 Store Identity & Fast Reference

```
+--------------------------+-----------------------------------------------------------------------------------------+
| Field                    | Specification / Value                                                                   |
+--------------------------+-----------------------------------------------------------------------------------------+
| App Title (Google Play)  | Practix: Dental Charting & CDS (30 char limit)                                          |
| App Title (App Store)    | Practix - Dental Chart & AI CDS (30 char limit)                                         |
| Subtitle (App Store)     | Voice Odontogram & Clinic EHR (30 char limit)                                           |
| Short Description (Play) | Offline voice dental charting, 3D odontogram, and on-device clinical decision support.  |
| Primary Category         | Medical                                                                                 |
| Secondary Category       | Productivity / Business                                                                 |
| Content Rating           | PEGI 3 / Everyone / Medical Information Reference                                       |
+--------------------------+-----------------------------------------------------------------------------------------+
```

---

### 4.2 Full Store Description (Markdown Format)

```markdown
Transform your dental operatory with Practix — the next-generation, voice-native, offline-first clinical workspace built exclusively for dental surgeons, general dentists, periodontists, and hygienists.

Developed by Devanapally Charan Tej, Practix enables you to maintain absolute chairside sterility while dictating periodontal exams, tooth restorations, and treatment plans in real time with our on-device speech engine.

KEY FEATURES FOR MODERN DENTAL OPERATORIES:

🎙️ HANDS-FREE VOICE DENTAL CHARTING
• Dictate tooth status, caries, existing restorations, and pocket depths hands-free.
• Powered by whisper.cpp — 100% on-device speech recognition with zero cloud audio transmission.
• Instant voice undo and dual dental numbering support (FDI and Universal systems).

🦷 INTERACTIVE 3D ANATOMICAL ODONTOGRAM
• High-performance 3D tooth surface visualizer powered by Google Filament.
• Color-coded tracking for caries, composites, crowns, endodontic treatments, and missing teeth.
• Rotate, zoom, and isolate quadrants chairside for enhanced patient education.

🧠 ON-DEVICE CLINICAL DECISION SUPPORT (CDS)
• Local AI copilot runs on-device without internet access (Llama 3.2 & Gemma 2 GGUF models).
• Red-Flag Safety Sentinel alerts against drug-allergy conflicts, anticoagulant-NSAID bleeding risks, and bisphosphonate (MRONJ) surgical contraindications.
• Synthesize comprehensive, multi-phase treatment plans in seconds.

🔒 ZERO-COMPROMISE SECURITY & COMPLIANCE
• 256-bit AES database encryption at rest (SQLCipher) backed by Android Keystore.
• Biometric Lock (Fingerprint/Face Unlock) with configurable auto-lock timeouts.
• Tamper-evident, append-only cryptographic audit logs meeting HIPAA, GDPR, and DPDP Act standards.

📋 PRACTICE OPERATIONS & EXPORT
• Today Queue & day-grid appointment management.
• Generate locked, immutable clinical treatment plans with signed addenda support.
• Single-tap professional PDF chart export for referrals and insurance claims.

Designed for speed, built for sterility, and engineered for privacy. Experience the future of dental charting today with Practix.
```

---

### 4.3 Keywords & ASO Tags
`practix, dental charting, odontogram 3D, dental software, dental EHR, periodontal charting, voice dictation dentist, clinical decision support, dental records, HIPAA dental app, offline dental clinic, dental practice management`

---

### 4.4 Release Notes (Version 1.0.0)

```
Version 1.0.0 — Initial Production Release

Welcome to Practix! We are proud to introduce the premier offline-first, voice-assisted clinical workspace for modern dental professionals:
• On-Device Voice Charting: Real-time dictation of tooth conditions and pocket depths using native Whisper.
• 3D Odontogram Visualizer: Interactive anatomical tooth rendering powered by the Google Filament engine.
• Clinical Decision Support (CDS): Local AI assistance for multi-phase treatment planning and prescribing safety checks.
• Enterprise Security: Full database encryption via SQLCipher AES-256 and Biometric authentication overlay.
• PDF Export: One-tap generation of comprehensive, cryptographically verified clinical charts.
```

---

### 4.5 Store Privacy & Content Questionnaire Declaration

```
+-----------------------------------+--------------------+------------------------------------------------------------+
| Data Type                         | Collected?         | Declaration Details                                        |
+-----------------------------------+--------------------+------------------------------------------------------------+
| Health & Medical Data             | YES (Local Only)   | Stored solely on local device. Not linked to user identity |
|                                   |                    | across external apps. Never tracked.                       |
+-----------------------------------+--------------------+------------------------------------------------------------+
| Audio Recordings                  | NO (Not Stored)    | Ephemeral processing in volatile RAM for speech inference; |
|                                   |                    | immediately discarded. No audio files retained.            |
+-----------------------------------+--------------------+------------------------------------------------------------+
| User Identifiers & Biometrics     | NO                 | Biometric authentication handled exclusively by device TEE |
|                                   |                    | via AndroidX BiometricPrompt. No biometric data received.  |
+-----------------------------------+--------------------+------------------------------------------------------------+
| Analytics & Crash Diagnostics     | OPTIONAL           | Anonymized diagnostic logs stripped of all clinical PHI.   |
+-----------------------------------+--------------------+------------------------------------------------------------+
```
