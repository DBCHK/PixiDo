<p align="center">
  <img src="https://img.shields.io/badge/PixiDo-Plan.%20Track.%20Achieve.-7C3AED?style=for-the-badge&logo=android&logoColor=white" alt="PixiDo" />
</p>

<h1 align="center">✨ PixiDo</h1>

<p align="center">
  <strong>A modern life organizer for tasks, money, calendar, and goals.</strong><br/>
  GitHub-style activity heatmap · multi-account budgets · Material You themes · unique sound design
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Min%20SDK-24-orange?style=flat-square" alt="Min SDK" />
  <img src="https://img.shields.io/badge/Target%20SDK-36-blue?style=flat-square" alt="Target SDK" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=flat-square" alt="License" />
</p>

---

## 🌈 What is PixiDo?

**PixiDo** turns everyday planning into something that feels good to use.  
Track todos, accounts & spending, calendar blocks, and life goals — all in one violet-powered workspace with a **contribution heatmap** that lights up as you complete work.

> **Plan. Track. Achieve.**

---

## ✨ Features

<table>
  <tr>
    <td width="50%" valign="top">

### ✅ Tasks & focus
- Clean empty slate — no dummy clutter  
- Priorities, categories, subtasks, streaks  
- Search + smart filters  
- Focus timer with XP rewards  
- **GitHub-style activity heatmap**

### 💳 Budget & accounts
- Choose your **currency** (USD, EUR, INR, …)  
- Optional monthly spending limit  
- **Bank · Cash · Credit card · Savings · Wallet**  
- Per-account balance, limits & usage bars  
- Income / expense logging

    </td>
    <td width="50%" valign="top">

### 📅 Calendar & goals
- Time-block style day planner  
- Vision goals with progress bumps  
- Milestone tracking that stays empty until you create

### 👤 You, personalized
- Simple profile · display name · avatar  
- **Google Sign-In (SSO)** · cloud restore after reinstall  
- **Cloud backup** every 24 hours **or** never  
- **8 themes** including **Material You**  
- Sound & haptics toggles  
- Quick notes with pin + colors

    </td>
  </tr>
</table>

### 🔊 Sound design
Every interaction has its **own** soft procedural tone — subtle, sweet, slow, and low. Pure sine recipes for taps, completes, deletes, FABs, focus sessions, theme changes, and more. No retro beeps.

### 📊 Momentum
- Stats strip: **Pending · Done · Today · Streak**  
- XP & levels that persist  
- Undo delete via snackbar  
- Smooth tab transitions (optional reduce motion)

---

## 🎨 Themes

| Theme | Vibe |
|:------|:-----|
| **Material You** | Wallpaper-driven colors (Android 12+) |
| **System** | Follows device light / dark |
| **PixiDo Dark** | Signature violet night |
| **PixiDo Light** | Clean bright workspace |
| **Ocean** | Deep teal focus |
| **Sunset** | Warm orange glow |
| **Forest** | Calm green productivity |
| **Midnight** | Neon fuchsia edge |

---

## 🛠 Tech stack

```text
┌─────────────────────────────────────────────────────────┐
│  Jetpack Compose  ·  Material 3  ·  Navigation          │
│  Room  ·  DataStore  ·  Coil  ·  Coroutines / Flow      │
│  Custom SoundEngine (procedural AudioTrack SFX)         │
│  KSP  ·  Gradle Kotlin DSL                              │
└─────────────────────────────────────────────────────────┘
```

| Layer | Choices |
|:------|:--------|
| UI | Jetpack Compose + Material You dynamic color |
| Architecture | Single-activity · ViewModel · Repository |
| Persistence | Room (tasks, budget, accounts, goals, notes, activity) |
| Preferences | DataStore (profile, theme, currency, sound, XP) |
| Media | Coil for avatars · procedural SFX + haptics |

---

## 🚀 Getting started

### Prerequisites
- **Android Studio** Ladybug / latest stable (or newer)  
- **JDK 11+**  
- Android SDK with **API 36** platform tools  

### Clone & run

```bash
git clone https://github.com/<your-username>/PixiDo.git
cd PixiDo
```

Open the project in Android Studio, sync Gradle, then:

```bash
# Debug build
./gradlew :app:assembleDebug

# Or from Windows PowerShell
.\gradlew.bat :app:assembleDebug
```

Install the APK from:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Or hit **Run ▶** on an emulator / device (API 24+).

### Optional env
Copy secrets template if you wire Firebase / Gemini later:

```bash
cp .env.example .env
```

---

## 📁 Project structure

```text
PixiDo/
├── app/
│   └── src/main/
│       ├── java/com/example/
│       │   ├── audio/          # Sfx enum + SoundEngine + Compose locals
│       │   ├── data/           # Room entities, DAO, repository, prefs
│       │   ├── ui/
│       │   │   ├── components/ # Heatmap, notes, dialogs, nav, focus
│       │   │   ├── screens/    # Tasks, Budget, Calendar, Goals, Profile
│       │   │   └── theme/      # Color, Type, multi-theme Material 3
│       │   ├── MainActivity.kt
│       │   └── ui/AuraViewModel.kt
│       └── res/                # Icons, themes, strings
├── gradle/
├── metadata.json
└── README.md
```

---

## 🗺 App map

```text
┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│    Tasks     │   │    Budget    │   │   Calendar   │   │    Goals     │
│  heatmap ·   │   │  accounts ·  │   │  day strip · │   │  milestones  │
│  notes · XP  │   │  currency ·  │   │  time blocks │   │  progress    │
│  focus timer │   │  cashflow    │   │              │   │              │
└──────┬───────┘   └──────────────┘   └──────────────┘   └──────────────┘
       │
       └── Profile  →  Google SSO · backup · themes · sound / haptics
```

### Google account & backup setup
1. Add `app/google-services.json` from your Firebase project (package `com.aistudio.aura.lifeorganizer.v1`).
2. Enable **Authentication → Google** and **Cloud Firestore** in Firebase Console.
3. Optionally set `GOOGLE_WEB_CLIENT_ID` in `.env` (Web client OAuth ID).
4. On first Google sign-in, existing cloud data is restored; otherwise a first backup is uploaded.
5. Choose **Every 24 hours** or **Don't backup** in Profile.


---

## 🧪 Build & quality

```bash
# Compile
./gradlew :app:compileDebugKotlin

# Unit / screenshot tests (Roborazzi present)
./gradlew :app:testDebugUnitTest
```

---

## 🗺 Roadmap ideas

- [ ] Cloud sync / multi-device  
- [ ] Widgets (heatmap + next task)  
- [ ] Recurring tasks & bills  
- [ ] Export budget CSV  
- [ ] Optional AI assist for planning  

Contributions and ideas are welcome.

---

## 🤝 Contributing

1. Fork the repo  
2. Create a branch: `git checkout -b feature/cool-thing`  
3. Commit with a clear message  
4. Open a PR and describe *what* + *why*

Please keep the empty-slate philosophy: **no prebuilt sample tasks** forced on users.

---

## 📄 License

This project is available under the **MIT License**.  
Feel free to learn from it, fork it, and build your own vibe.

---

<p align="center">
  <sub>Built with Kotlin · Compose · a little pixel magic</sub><br/>
  <b>PixiDo</b> — plan it, track it, ship your day ✨
</p>
