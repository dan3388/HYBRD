# Hibreed — Workout Tracker (Android)

A simple Android workout tracker. Log a workout per exercise, with multiple sets per workout (each set has its own weight and reps), and view all your sessions grouped by day.

## Status

This project is a work-in-progress dev build. The app currently has a local-only data layer (Room/SQLite) — there is no cloud sync yet.

## What it does so far

- **Sessions screen** —
  - Lists all logged workouts grouped by day.
  - Each workout card shows the exercise and its sets, e.g. `185 × 6  150 × 8`.
  - Tapping a card opens the editor for that workout.
  - A FAB (`+`) starts a new workout.
- **Workout / set editor** —
  - Pick an exercise (from a searchable, grouped library of ~90 seeded exercises, or add your own custom one).
  - Pick a date.
  - Add/remove **sets** — each set has its own weight and reps.
  - Save the workout.
  - In edit mode you can also delete the whole workout.
- **Exercise library** — ~90 exercises seeded once into the database, grouped by muscle group (Chest, Back, Shoulders, Biceps, Triceps, Legs, Core, Full Body).

### Conventions
- Weight `0` = **body weight** (blank weight field). Displayed as "bw" on cards.
- Editable weight/reps boxes use `lbs` / `reps` as labels.

## Tech stack

- **Android** — native app, XML layouts + ViewBinding (no Jetpack Compose).
- **Kotlin** + built-in Kotlin support (AGP 9.3.1, Kotlin 2.2.10).
- **Room** — local SQLite storage (`room 2.7.1`) with KSP.
- **Navigation component** — 3 screens: Sessions → Editor → Exercise Picker.
- **Material 3** components.

## Project structure

```
app/src/main/java/com/example/hibreed/
├── MainActivity.kt              # Entry point; seeds the exercise library
├── HibreedApp.kt                # Application; exposes DAOs
├── SessionsFragment.kt          # Sessions list, grouped by day
├── LogFragment.kt               # Workout / set editor (create + edit)
├── ExercisePickerFragment.kt    # Searchable exercise picker
└── data/
    ├── Exercise.kt              # Exercise entity
    ├── Workout.kt               # Workout entity (exercise + date)
    ├── Set.kt                   # Set entity (weight, reps, sort order)
    ├── ExerciseDao.kt           # Exercise queries
    ├── WorkoutDao.kt            # Workout/Set + relation queries (transactional save)
    ├── AppDatabase.kt           # Room database (currently v2)
    ├── ExerciseRepository.kt    # Seeds the exercise library once
    └── ...
└── ui/
    ├── SessionAdapter.kt        # Groups + renders workout cards
    ├── SessionModels.kt         # List item types (day header / workout)
    └── ...
```

## Recent refactor — multiple sets per workout

The app was originally logging a single set per workout (a single `SessionLog` row carrying weight/reps/sets). It was refactored so each workout can hold **multiple sets**, each with its own weight and reps.

- **New schema:** `Workout` (exercise + date) and `Set` (weight, reps, `sortOrder`) tables replace the old `SessionLog` table.
- **Database version bumped to 2.** Because this is a dev build, the refactor uses **drop & rebuild** (`fallbackToDestructiveMigration`) — existing test data is wiped, then the exercise library re-seeds automatically. No data migration was written.

## Building

Requirements: Android Studio with a JDK, and the Android SDK.

```
gradlew :app:assembleDebug
```

Debug APK output:

```
app/build/outputs/apk/debug/app-debug.apk
```

> Note: `android.disallowKotlinSourceSets=false` is set in `gradle.properties` (required for KSP with AGP 9's built-in Kotlin).

## Running

Install the debug APK on a device/emulator and open the app. The exercise library seeds on first run. On the first launch after the schema refactor, the database drops and rebuilds (expected for this dev build).

## Roadmap (possible next steps)

- Presets/history of past weights for quick-fill.
- Edit an individual set without rewriting the whole workout.
- Charts / progress tracking over time.
- Cloud sync (Firebase was rejected for now; storage is intentionally local-only).
