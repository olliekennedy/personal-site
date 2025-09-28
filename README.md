# PersonalSite

## Run Locally

Run the build:
```
./gradlew clean build
```

Run the app (hit localhost:9000):
```
./gradlew run
```

Development (hot reload):
```
./gradlew dev
```

## Package
```
./gradlew distZip
```
Resulting archive appears under build/distributions/.

## Endpoints
- /ping -> health check

## Notes
Requires JDK 21 (Gradle toolchain will provision if not installed).
