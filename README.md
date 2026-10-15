# Image to PDF by Piyush

Native Android app built with Kotlin and Android SDK. It selects multiple images, lets you reorder/remove them, supports A4 or Letter pages, portrait/landscape orientation, configurable margins, and saves a PDF through Android's document picker. Image decoding and PDF generation happen on-device.

## Build on GitHub (Android phone friendly)

1. Create a new GitHub repository, for example `ImageToPDFByPiyush`.
2. Upload the contents of this folder to the repository root (not the enclosing ZIP folder).
3. Open **Actions** and enable workflows if prompted.
4. Run **Build Android APK** from the Actions tab, or push to `main` to trigger it automatically.
5. Open the completed workflow run, scroll to **Artifacts**, and download `Image-to-PDF-by-Piyush-debug-apk`.
6. Extract the ZIP artifact and install `app-debug.apk` on your Android device. You may need to allow installs from your browser/file manager.

## Local build

Install JDK 17, Android SDK platform 35 and Gradle 8.9 or compatible. From this folder run:

```bash
gradle assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Package details

- App name: Image to PDF by Piyush
- Application ID: `com.piyush.imagetopdf`
- Minimum Android: Android 6.0 (API 23)
- Target SDK: 35

## Important notes

- This project currently builds a debug APK. Before publishing on Google Play, create a signed release build, protect your signing key, add a privacy policy, test on physical devices, and complete Play Console requirements.
- AdMob is not included in this initial source package. To add live ads, create an AdMob app and ad unit, add the official Google Mobile Ads SDK, use test ad IDs while developing, and update the manifest with your own app ID. Never publish with someone else's IDs.
- The page option `Fit image (A4 page)` currently uses A4 page dimensions and scales each image proportionally to fit; it does not crop the image.
