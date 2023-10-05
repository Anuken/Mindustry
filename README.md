![Mindustry Logo](core/assets-raw/sprites/ui/logo.png)

[![Build Status](https://github.com/Anuken/Mindustry/workflows/Tests/badge.svg?event=push)](https://github.com/Anuken/Mindustry/actions)
[![Discord](https://img.shields.io/discord/391020510269669376.svg?logo=discord&logoColor=white&logoWidth=20&labelColor=7289DA&label=Discord&color=17cf48)](https://discord.gg/mindustry)

Mindustry is an automation tower defense real-time strategy game written in Java.

## Resources

- [Trello Board](https://trello.com/b/aE2tcUwF/mindustry-40-plans)
- [Wiki](https://mindustrygame.github.io/wiki)
- [Javadoc](https://mindustrygame.github.io/docs)

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md) for guidelines on how to contribute to the project.

## Building

Bleeding-edge builds are automatically generated for every commit. You can find them [here](https://github.com/Anuken/MindustryBuilds/releases).

If you prefer to compile the project on your own, please follow the instructions below based on your operating system.

### Windows

To run Mindustry, open a terminal in the Mindustry directory and execute the following command:
```
gradlew desktop:run
```

To build Mindustry, use the following command:
```
gradlew desktop:dist
```

For sprite packing, run the following command:
```
gradlew tools:pack
```

### Linux/Mac OS

To run Mindustry, open a terminal in the Mindustry directory and execute the following command:
```
./gradlew desktop:run
```

To build Mindustry, use the following command:
```
./gradlew desktop:dist
```

For sprite packing, run the following command:
```
./gradlew tools:pack
```

### Server

Server builds are bundled with each released build (in Releases). If you prefer to compile the server on your own, replace 'desktop' with 'server' in the above commands. For example:
```
gradlew server:dist
```

### Android

To build Mindustry for Android, please follow these steps:

1. Install the Android SDK from [here](https://developer.android.com/studio#command-tools). Make sure to download the "Command line tools only" package, as Android Studio is not required.
2. Extract the downloaded Android SDK and locate the `cmdline-tools` directory. Inside this directory, create a folder named `latest` and move all its contents into the newly created folder.
3. In the same directory where the `cmdline-tools` directory is located, run the following command to accept the SDK licenses:
   ```
   sdkmanager --licenses
   ```
4. Set the `ANDROID_HOME` environment variable to point to your extracted Android SDK directory.
5. Enable developer mode on your device/emulator. If you are testing on a physical device, follow [these instructions](https://developer.android.com/studio/command-line/adb#Enabling). For an emulator, refer to your emulator's specific instructions.
6. To build an unsigned APK, run the following command:
   ```
   gradlew android:assembleDebug
   ```
   This will generate the APK file in the `android/build/outputs/apk` directory.

To debug the application on a connected device/emulator, run the following command:
```
gradlew android:installDebug android:run
```

### Troubleshooting

#### Permission Denied

If you encounter a "Permission denied"

 or "Command not found" error on Mac/Linux, run the following command before executing `./gradlew`:
```
chmod +x ./gradlew
```
This is a one-time procedure.

---

Please note that Gradle may take several minutes to download files. Please be patient.

After building, the output .JAR file should be located at `/desktop/build/libs/Mindustry.jar` for desktop builds, and at `/server/build/libs/server-release.jar` for server builds.

## Feature Requests

We welcome your feature requests and feedback! Please submit them [here](https://github.com/Anuken/Mindustry-Suggestions/issues/new/choose).

## Downloads

| [![itch.io](https://static.itch.io/images/badge.svg)](https://anuke.itch.io/mindustry) | [![Google Play](https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png)](https://play.google.com/store/apps/details?id=io.anuke.mindustry) | [![F-Droid](https://fdroid.gitlab.io/artwork/badge/get-it-on.png)](https://f-droid.org/packages/io.anuke.mindustry) | [![Flathub](https://flathub.org/assets/badges/flathub-badge-en.svg)](https://flathub.org/apps/details/com.github.Anuken.Mindustry) |
| --- | --- | --- | --- |
