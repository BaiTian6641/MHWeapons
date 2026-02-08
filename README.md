# MHWeaponsMod (Forge 1.20.1 skeleton)

This project is a minimal Forge mod skeleton for Minecraft 1.20.1.

Quick commands:

- Refresh dependencies and run Forge setup (downloads Minecraft, mappings and assets):

```bash
./gradlew --refresh-dependencies assemble
```

- Build the mod jar (may require reobf or build depending on ForgeGradle version):

```bash
./gradlew reobfJar assemble
# or
./gradlew build
```

- Run a development client (dev environment must be prepared by `genIntellijRuns` or equivalent):

```bash
./gradlew runClient
```

Notes:
- Java 17 is required (the Gradle toolchain is configured to use Java 17 in `build.gradle`).
- If you need IntelliJ run configurations, run:

```bash
./gradlew genIntellijRuns
```

Docs are in the `docs/` folder (plan, changes, description).
