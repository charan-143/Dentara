# Practix Android: Where Jetpack Compose Meets Voice Recognition, 3D Dental Charts and Secure Local Data

*Project: [Dentara / Practix Android](https://github.com/charan-143/Dentara/tree/master/android) · Status: public repository · Technologies: Kotlin, Jetpack Compose, C++/NDK, whisper.cpp, SQLCipher, Filament, Coroutines/Flow*

A mobile clinical application has a distinctive constraint: a clinician needs an accurate patient record without constantly stopping to type. That makes input design, latency, and recovery from mistakes part of the system architecture—not merely visual polish.

## Separating mobile layers

The Practix codebase documents a layered Android approach built around Kotlin, Jetpack Compose, Material 3, and reactive state with coroutines and Flow. UI screens should observe application state rather than directly coordinate database writes, microphone capture, and rendering. This separation makes it easier to reason about transient speech recognition, durable chart updates, and navigation without tying those behaviors to one composable.

## Voice-first charting is a pipeline

The Android module includes NDK/CMake build configuration for native components and documents a whisper.cpp-based speech system. A useful conceptual pipeline is:

`Microphone capture → segment speech → speech-to-text → deterministic dental command parsing → confirmation/read-back → chart mutation`

The middle steps matter. Transcription and clinical interpretation are not equivalent: a recognizer can hear a number correctly but map it to the wrong tooth or periodontal site. A deterministic parser with explicit numbering rules and an opportunity to confirm findings is easier to audit than silently converting unrestricted model output into clinical state. The project documentation discusses voice command parsing, multiple numbering systems, and a read-back mechanism.

## 3D visualization is a separate subsystem

The repository's Gradle configuration includes Google Filament libraries, and the feature document describes anatomical tooth rendering with surface-level condition indicators. A 3D scene introduces its own state and performance questions: how the anatomical model is loaded, how dental conditions map onto meshes, and how clinicians navigate the chart without losing clinical context. The rendering engine and the patient record should not become one inseparable data model.

## Private data must remain private on-device

The Gradle dependencies include SQLCipher, and the feature documentation describes encrypted persistence and device authentication. These are strong architectural directions, but a dependency alone does not prove correct key management, lifecycle handling, backup behavior, or clinical suitability. A release-grade mobile app still needs device testing, independent review, and a clear policy for data retention and synchronization.

## What this project teaches

Combining speech, native inference, a 3D graphics engine, and clinical persistence is primarily an **integration problem**. Each subsystem has different error conditions: noisy audio, incomplete model files, frame-rate drops, navigation interruption, and unsafe chart mutations. A good design makes failure visible and reversible wherever possible.

### Sources and scope

- [Android feature specification](https://github.com/charan-143/Dentara/blob/master/android/FEATURES.md)
- [Android Gradle configuration](https://github.com/charan-143/Dentara/blob/master/android/app/build.gradle.kts)

*Repository evidence confirms dependencies and documented design; it does not independently establish on-device latency, clinical accuracy, or production readiness. No patient data or internal credentials are reproduced here.*