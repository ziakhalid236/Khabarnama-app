# Khabarnama — Global, Pakistani & Islamic News Dispatch

[![Build Android APK & Play Store AAB](https://img.shields.io/badge/Build-Android%20APK%20%26%20AAB-126B59?logo=github-actions&logoColor=white)](.github/workflows/android-release.yml)
[![Direct APK Download](https://img.shields.io/badge/Download-Release%20APK-B8913B?logo=android&logoColor=white)](../../releases)
[![Platform](https://img.shields.io/badge/Platform-Android%2014+-0F5C4D?logo=android&logoColor=white)](app/build.gradle.kts)
[![License](https://img.shields.io/badge/License-Proprietary-gray)](#)

---

## ✨ Key Features

- **23+ Trusted Global, Pakistani & Islamic Publishers (38 Live RSS Feeds)**:
  - **Pakistan (`🇵🇰`)**: Dawn, The Express Tribune, Geo News, ARY News, Pakistan Today, Business Recorder
  - **World & Middle East (`🇶🇦`, `🇸🇦`, `🇬🇧`, `🇹🇷`, `🇺🇳`)**: Al Jazeera, Arab News, BBC News, Middle East Eye, Anadolu Agency, Saudi Gazette, Daily Sabah, UN News
  - **Islamic & Scholarly (`🕌`)**: SeekersGuidance, AboutIslam, Islam21c, MuslimMatters, IlmFeed, 5Pillars, ProductiveMuslim
  - **Business, Sports & Tech**: BBC Business, CNBC World, ESPNcricinfo, BBC Sport, TechCrunch, The Verge, Ars Technica
- **Instant Multi-Language Translation (`🇬🇧 EN`, `🇵🇰 UR`, `🇸🇦 AR`, `🇹🇷 TR`, `🇮🇳 HI`)**:
  - English-locked LTR app shell while headlines and summaries translate on-demand into Urdu (Nastaliq), Arabic (Naskh), Turkish, and Hindi using a dual-engine architecture (Firebase AI Logic + Instant Cloud Translation Fallback) with local Room caching.
- **Astronomical Prayer Times & Hijri Calendar**:
  - Powered by the `Adhan` library with city-specific time zones (`Karachi`, `Lahore`, `Islamabad`, `Makkah`, `Madinah`, `London`, `New York`) and Hijri moon-sighting adjustment (`-2` to `+2` days).
- **Offline-First Architecture**:
  - Automatic local Room database caching and 6-hour periodic background sync via `WorkManager`. No external backend server or database required.

---

## 🚀 GitHub Actions — Automatic APK & Play Store AAB Build

This repository includes a pre-configured GitHub Actions workflow at **`.github/workflows/android-release.yml`**.

### How to Build on GitHub (اردو رہنمائی):
1. **Push to GitHub**: Export or push this project to your GitHub repository (`main` or `master` branch).
2. **Open GitHub Actions**: Go to your repository on GitHub and click the **Actions** tab. The build runs automatically.
3. **Direct 1-Tap Download (GitHub Releases)**:
   - Go to the **Releases** tab on your GitHub repository.
   - Click on the latest release (`v1.0`).
   - Tap directly on **`Khabarnama-Release-v1.0.apk`** to download and install immediately on your Android phone without needing to unzip any file!
4. **Alternative Download (Workflow Artifacts)**:
   - On the completed Actions run page, scroll to **Artifacts** to download:
     - **`Khabarnama-Signed-Release-APK`**
     - **`Khabarnama-PlayStore-Release-AAB`** (ready for Google Play Console upload)
     - **`Khabarnama-Debug-APK`**

### Optional: Custom Production Keystore in GitHub Secrets
If you want GitHub Actions to sign the release with your own permanent `.jks` keystore (with alias `upload`), add these 3 repository secrets under **GitHub Repo → Settings → Secrets and variables → Actions**:
- `UPLOAD_KEYSTORE_BASE64`: Base64-encoded contents of your `.jks` file (`base64 -w 0 my-upload-key.jks`)
- `STORE_PASSWORD`: Your keystore password
- `KEY_PASSWORD`: Your key password

*(If these secrets are not set, the workflow automatically generates a valid upload keystore so your build always succeeds immediately.)*

---

## 📦 Changing the Final Application ID before Play Store Upload
Open `app/build.gradle.kts` (line 14) and update the single constant:
```kotlin
val APP_APPLICATION_ID = "com.aistudio.urdunews.kmqv"
```
