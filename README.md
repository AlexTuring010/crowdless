# crowdless

Android app for predicting **crowd levels at destinations** and
suggesting **less crowded alternatives** based on user preferences.
Built by a team of five across two hackathons.

> **🥇 1st place — AI Hackathon 2026 — Open Track**
> Organized by ACE @ AUEB ("ΟΠΑ"), Athens (March 2026).
>
> **🥇 1st place in the Greek round — Cassini Hackathon Greece 2025**
> Organized by Impact Hub Athens (December 2025). The team advanced to
> the European final and did not podium there.

## What it is

Crowdless tells users how busy a destination is likely to be and
offers alternatives that match their interests. The original Cassini
brief was **satellite data + GNSS for tourism sustainability**; the
team's pitch extended that into a consumer-facing app.

For the AI-Hackathon Open Track in 2026 the team rebuilt the app with
stronger AI features around the same idea — *"an AI-powered app that
informs users about crowd levels and suggests alternative places based
on their preferences"* (organizer's description on the winners post).

The rebuilt AI-Hackathon version lives on a teammate's repo:
**https://github.com/EXARTeo/Crowdless_App** (private — ask the owner
for access).

## Status of this repo

This repo holds the **Cassini-era Android client** (Kotlin, single-
activity) — the engineering footprint of the first hackathon, not the
polished AI-ΟΠΑ winner.

The wins were **team wins**: pitch, business case, and presentation
carried more weight than the code. The team built much that isn't
here — the deck, market research, the rebuilt 2026 version.

## Team

Same five people across both hackathons:

- **Alexandros Gkiafis** ([@AlexTuring010](https://github.com/AlexTuring010))
  — Android client (this repo)
- **Theodoros Exarchos** ([@EXARTeo](https://github.com/EXARTeo))
  — AI-Hackathon rebuild
- **Mariglena Reci**
- **Stelios Rotas**
- **Panagiotis Thivaios**

Mentor at Cassini: George Mendrinos.

## Stack

- **Platform:** Android, Kotlin
- **Build:** Gradle (Kotlin DSL)
- **Architecture:** Single-activity
  (`app/src/main/java/com/example/crowdless/MainActivity.kt`,
   layout in `app/src/main/res/layout/activity_main.xml`)

## Build and run

```bash
./gradlew assembleDebug
```

Install the APK from `app/build/outputs/apk/debug/` to a connected
Android device, or open in Android Studio and run.

## Links

- Crowdless project page: https://www.linkedin.com/company/crowdlessapp/
- AI Hackathon 2026 winners (organizer's post): https://www.linkedin.com/posts/aihackathon2026-opentrack-innovationecosystem-ugcPost-7436487613802381312
- Cassini Hackathon Greece 2025 European-final recap (mentor George Mendrinos): https://www.linkedin.com/posts/georgemendrinos_yesterday-was-the-european-final-the-team-ugcPost-7394692583706468352
- Prior year of the AI ΟΠΑ Hackathon series (different team makeup, different challenge, didn't win): https://github.com/AlexTuring010/novibet-clone

## License

MIT — see [LICENSE](./LICENSE).
