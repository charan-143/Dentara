# Dentara / Practix engineering evidence

Dentara is the Next.js clinical workspace; Practix is the Android application
under `android/` (application ID `com.dentara.dental`). Its existing architecture
includes Compose UI, a repository/database layer, SQLCipher persistence and a
native whisper.cpp bridge. See `android/FEATURES.md` for the feature inventory;
implementation listings are not a substitute for end-to-end device validation.

## Repeatable checks

```sh
python -m unittest discover -s tools/tests -v
cd android
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

The last command needs a device/emulator. Native builds require the pinned SDK,
NDK and CMake versions in the Gradle configuration and an initialized whisper.cpp
submodule. JVM tests with fake speech bridges validate lifecycle/error handling,
not real transcription accuracy.

## Dictation evaluation

Capture consented, de-identified scripted recordings on the target handset and
export one JSONL row per clip locally:

```json
{"id":"clip-001","reference":"tooth fourteen depth three","hypothesis":"tooth fourteen depth two","inference_ms":250,"audio_seconds":2.5}
```

The values above are format examples only. Obtain hypothesis/timing from actual
WhisperEngine runs; manually transcribe references before looking at hypotheses.
Record device model, OS, model file SHA-256, decoder settings, noise conditions and
speaker count alongside the report. Keep a fixed test set separate from calibration.

```sh
python tools/evaluate_speech.py /private/recordings.jsonl --output /private/report.json
```

Reports include corpus-weighted WER, exact transcript rate, p50/p95 inference
latency and real-time factor. They omit transcript text and recording IDs. WER
can exceed 1 due to insertions. Number-word differences remain errors. Evaluate
clinical field correctness separately: a low average WER can hide a wrong tooth
number or measurement. No clinical quality threshold is assumed by this tool.

## Still requires measurement

For repeatable process-cold startup timing, connect an unlocked test handset:

```sh
python tools/benchmark_android.py --serial DEVICE_SERIAL --package com.dentara.dental --output startup.json
```

Use `--adb /path/to/adb` when platform-tools is not on PATH. This force-stops
and relaunches the app ten times, preserving its data. It records Android
`TotalTime`, device/build metadata and p50/p95, not time-to-interactive or
disk-cache-cold startup. Do not run during an active clinical workflow.

- Real-device voice-to-reviewed-chart workflow and error recovery.
- Real speech WER/latency with representative accents and background noise.
- Database encryption overhead versus a controlled same-device baseline.
- Cold startup distributions, memory use and representative database sizes.
- Demo screenshots/video using fictitious records and a reproducible installation guide.

Do not publish real patient records, speech recordings, keys or credentials as
portfolio evidence. Existing uncommitted application changes were preserved.
