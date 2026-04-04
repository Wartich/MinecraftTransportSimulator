# Build Instructions

This project uses different Gradle versions for different Minecraft versions.

## Building Older Versions (1.12.2 - 1.20.1)

Use the root Gradle wrapper (Gradle 7.5):

```bash
# Build all older versions
./gradlew build

# Build specific version
./gradlew :mcinterfaceforge1122:build   # 1.12.2
./gradlew :mcinterfaceforge1165:build   # 1.16.5
./gradlew :mcinterfaceforge1182:build   # 1.18.2
./gradlew :mcinterfaceforge1192:build   # 1.19.2
./gradlew :mcinterfaceforge1201:build   # 1.20.1
```

## Building 1.21.1

The 1.21.1 version has its own Gradle wrapper (Gradle 8.8) due to NeoForge requirements:

```bash
cd mcinterfaceneoforge1211
./gradlew build
```

## Why Two Gradle Versions?

- **Gradle 7.5**: Compatible with ForgeGradle 5.x used by older Minecraft versions
- **Gradle 8.8**: Required by NeoGradle for Minecraft 1.21.1+
