---
name: android-build-and-launch
description: >-
  Builds, packages, installs, and launches the Thornbury Dental Android app on a connected physical device or emulator.
  Use when the user asks to build, run, deploy, launch, or test the Android application.
---

# Android App Build and Launch Skill

This skill automates building the Thornbury Dental Android app with Gradle, deploying it to a connected physical device or emulator, and launching the main activity.

## Project Structure & Config

- **Android Root**: `android/`
- **Application ID**: `com.example.thornburydental`
- **Main Launcher Activity**: `com.example.thornburydental.MainActivity`
- **Output APK**: `android/app/build/outputs/apk/debug/app-debug.apk`
- **Local SDK**: `C:\Users\Admin\AppData\Local\Android\Sdk` (configured in `android/local.properties`)
- **Default JDK / JBR**: `C:\Program Files\Android\Android Studio\jbr`

---

## Fast Automated Execution

Run the bundled PowerShell helper script from the workspace root:

```powershell
powershell -ExecutionPolicy Bypass -File .agents/skills/android-build-and-launch/scripts/build_and_launch.ps1
```

### Script Flags
- `-BuildOnly`: Compiles the debug APK without attempting to install or launch.
- `-LaunchOnly`: Skips compilation and installs/launches the existing APK.
- `-DeviceId <ID>`: Target a specific device serial (e.g. `ZD222MKB8C`).

Helper script location: [build_and_launch.ps1](./scripts/build_and_launch.ps1)

---

## Manual Step-by-Step Procedure

If running commands individually:

### 1. Set Environment Variables
Ensure `JAVA_HOME` points to the Android Studio JBR or JDK 17+:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$adb = "C:\Users\Admin\AppData\Local\Android\Sdk\platform-tools\adb.exe"
```

### 2. Verify Connected Devices
```powershell
& $adb devices
```
Confirm the target device is in the `device` state.

### 3. Build the Debug APK
From the `android/` folder:
```powershell
cd android
.\gradlew.bat assembleDebug
```

### 4. Install onto Device
```powershell
& $adb -s <DEVICE_ID> install -r app\build\outputs\apk\debug\app-debug.apk
```

### 5. Launch the App
```powershell
& $adb -s <DEVICE_ID> shell am start -n com.example.thornburydental/.MainActivity
```

---

## Verification & Troubleshooting

### Verify the App is Running
1. **Check Process ID**:
   ```powershell
   & $adb shell pidof com.example.thornburydental
   ```
2. **Check Active Foreground Window**:
   ```powershell
   & $adb shell dumpsys window | Select-String "mCurrentFocus"
   ```
   Should display `com.example.thornburydental/.MainActivity`.

### Common Issues
- **`java : The term 'java' is not recognized`**: Set `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`.
- **`device unauthorized`**: Unlock the Android device screen and accept the USB debugging prompt.
- **`no devices/emulators found`**: Reconnect USB cable or run `& $adb kill-server; & $adb start-server`.
