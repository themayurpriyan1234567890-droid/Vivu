# Vivu Visualizer (clean-room Android rebuild)

A starter Android app for playing selected audio/video and displaying a live audio-reactive spectrum overlay.

## Build an APK automatically with GitHub Actions

You do **not** need Android Studio if you use GitHub's cloud build:

1. Sign in to GitHub and create a new **public or private repository**.
2. Upload the contents of this project folder to the repository (not just the ZIP file).
3. Open the repository's **Actions** tab and enable Actions if GitHub asks.
4. Open the **Build Android APK** workflow and press **Run workflow**, or push a commit to `main`.
5. Wait for the workflow to finish with a green check.
6. Open the completed workflow run, scroll to **Artifacts**, and download `Vivu-Visualizer-debug-APK`.
7. Extract the downloaded artifact ZIP. It contains `app-debug.apk`. Transfer it to your Android phone and install it. Android may ask you to allow installation from that source.

Every push to `main` also builds an APK artifact. To publish a GitHub Release, create and push a version tag such as `v1.0.0`; the workflow attaches the debug APK to that release.

## Build locally

- Android Studio
- JDK 17
- Android SDK Platform 35
- Gradle 8.9

Open this folder in Android Studio and run the `app` configuration, or run:

```bash
gradle :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

## Current scope

- Audio file selection and playback
- Video file selection and playback
- Live spectrum overlay driven by Android's audio Visualizer effect

## Not yet implemented

- Exporting the visualizer permanently into an MP4
- Timeline editing, trimming, and visualizer preset controls
- 4K/8K export

This is a clean-room implementation and does not include or modify the proprietary source code of the supplied APK. The GitHub workflow builds a **debug APK**, not a Play Store-signed release APK.
