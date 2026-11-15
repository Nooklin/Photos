# Third-party components

Nooklin Photos is GPL-3.0 (see `LICENSE`). It bundles or links the components below.
The full Apache License 2.0 text is in `apache-2.0/LICENSE.txt`.

## Apache License 2.0 (direct dependencies, versions in `gradle/libs.versions.toml`)
- AndroidX: Core, Activity, Lifecycle, Navigation, DataStore, Room, Biometric, ExifInterface, Print
- Jetpack Compose (UI, Material3, Material Icons Extended)
- Coil 3 (coil-compose)
- Media3 (ExoPlayer, UI, Transformer, Effect)
- LiteRT (TensorFlow Lite runtime), without its Google Play, Google Play services and WorkManager parts

Each of these projects may ship its own NOTICE file inside its published artifact. Transitive dependencies are not listed here yet; they need to be listed from a resolved dependency report before a release.

## SIL Open Font License 1.1
- Work Sans: `work-sans/OFL.txt`
- Inter (latin subset): `inter/OFL.txt`

## Machine learning models (in `app/src/main/assets/`)
See `MODELS.md`.
