# Circuit Break

Dopamine replacement habit breaker. Spin instead of snack.

## How It Works

When you feel a craving (food, nicotine, compulsion), open the app and spin. It deals you a 2-5 minute physical or cognitive activity that replaces the dopamine-seeking behavior with skill-building.

The app alternates physical and cognitive tasks to keep you engaged across the day without burning out any single muscle group or mental domain.

## Features

- **Spin engine** — shuffle through 29 physical and 22 cognitive activities
- **Push/pull pairing** — every push exercise is paired with a pull for shoulder health
- **Three modes** — Anything (alternating), Physical only, Cognitive only
- **Sound + haptic** — click and vibration feedback on every spin
- **Streak tracking** — daily tally and day streak persisted locally
- **Custom items** — add or remove activities from the native settings screen
- **No accounts, no cloud, no permissions** — everything is local

## Download

Latest APK: [Releases](https://github.com/joeyjoey1234/Circuit_Break/releases)

Or grab the latest build from [Actions](https://github.com/joeyjoey1234/Circuit_Break/actions) → latest run → Artifacts → `circuit-break-debug`.

## Build from Source

```bash
git clone https://github.com/joeyjoey1234/Circuit_Break.git
cd Circuit_Break
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

Requires Android SDK 34 and JDK 17.

## Project Structure

```
app/src/main/
├── assets/
│   ├── spin.html          ← Spin engine (WebView)
│   └── defaults.json      ← Default activity lists
└── java/com/circuitbreak/app/
    ├── MainActivity.kt    ← WebView host + JS bridge
    ├── SettingsActivity.kt ← Compose UI for managing items
    └── data/
        ├── ActivityItem.kt
        └── ItemStore.kt   ← JSON persistence
```
