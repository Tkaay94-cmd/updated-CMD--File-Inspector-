markdown
ThirdPartyInspector
===================

Purpose
-------
ThirdPartyInspector is an on-device defensive inspection app to help users discover potentially unwanted, hidden, sideloaded or unusually-privileged third-party applications and files. It only uses legitimate Android APIs and requires explicit user consent for all scanning actions.

This repository is an initial skeleton with Gradle configuration and foundational architecture (Kotlin, Compose, Hilt, Room, WorkManager).

Build instructions
------------------
1. Install Java 17+ and Android SDK matching compileSdk (see gradle.properties compileSdk).
2. Open project in Android Studio (recommended).
3. Let Gradle sync and download dependencies.
4. Build and run on a device or emulator (minSdk 29).

Command-line:
./gradlew assembleDebug

Notes about versions
--------------------
Versions are centralized in gradle.properties. Update them if needed to match your environment.

Privacy & Safety
----------------
- This app does NOT attempt to root the device, exploit vulnerabilities, bypass sandboxing, access private data of other apps, or exfiltrate scan data.
- By default, it does NOT request QUERY_ALL_PACKAGES.
- All scan data is stored locally in Room. Export must be explicit by the user.

Next steps (planned)
--------------------
- Implement PermissionInspector, AccessibilityInspector, DeviceAdminInspector, FileSystemInspector, NetworkInspector, RootEnvironmentInspector, InstallerInspector.
- Implement RiskEngine rules for combinations (e.g., accessibility + overlay + unknown installer).
- Implement UI screens for findings, package details, file findings, scan history.
- Add unit tests and mocked Android tests.

Contact
-------
This skeleton was generated as the first iteration. Continue to iterate in small steps: implement one inspector, compile, run tests, fix issues.
# updated-CMD--File-Inspector-