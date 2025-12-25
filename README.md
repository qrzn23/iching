# I Ching (Android)

## Run from terminal

Build:
```
./gradlew assembleDebug
```

Install:
```
./gradlew installDebug
```

Launch:
```
adb shell am start -n com.example.iching/.MainActivity
```

Logs:
```
adb logcat | grep -i iching
```

## Common device/install blockers

- Device unauthorized:
  - `adb kill-server`
  - `adb start-server`
  - Unplug/replug device
  - Accept RSA prompt on phone
  - Verify: `adb devices`

- INSTALL_FAILED_UPDATE_INCOMPATIBLE (signature mismatch):
  - `adb uninstall com.example.iching`
  - Alternative: change `applicationId` if you must keep the old install (not preferred).

## Manual layout check

- Portrait small phone: Consult, Cast, Interpretation, Viewer all usable without horizontal swipe.
- Landscape phone: section buttons visible, text area readable, trigram selectors usable.
- Emulator medium: verify spacing, buttons hit targets, text scroll only in content area.
