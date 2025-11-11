# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/berlin/yuna/nativeexample` hosts the Nano bootstrap (`Main`) and HTTP handlers; keep new features in subpackages to preserve a single domain.
- `target/` contains shaded jars and native binaries; clean it before commits and never check artifacts in.
- Dockerfiles (`Dockerfile`, `Dockerfile_alpine`) wrap the native build; place any additional build assets under `docker/`.
- Add documentation, sample payloads, and Postman collections under `docs/` (create if needed) rather than mixing them with source files.

## Build, Test, and Development Commands
- `./mvnw clean package` — compiles Java 21 sources and produces a runnable jar with all Nano dependencies.
- `./mvnw clean package -Pnative` — builds `target/nano-graalvm-example-no-dependencies.native` via the GraalVM plugin; requires `$JAVA_HOME` pointing at GraalVM ≥21.
- `docker build -t app-native -f Dockerfile .` followed by `docker run --rm -p 8080:8080 app-native` — containerized native build and smoke test.
- `native-image -H:Name=app.native -cp target/nano-graalvm-example-no-dependencies.jar berlin.yuna.nativeexample.Main` — manual experiments when iterating outside Maven.

## Coding Style & Naming Conventions
- Java 21+, 4-space indentation, no tabs; prefer records, final fields, and pure functions. Public methods must return Optionals or objects, never `null`.
- Package prefix stays `berlin.yuna.nativeexample`; HTTP routes live under `/v1/...` and follow GET/POST/DELETE only.
- Keep DTOs immutable and serialize timestamps as UTC epoch milliseconds.
- Avoid reflection, parallel streams, and shared mutable state to stay GraalVM-friendly.

## Testing Guidelines
- Use JUnit 5 component tests that start the Nano server and hit real endpoints (`/info`, `/metrics/prometheus`, `/api/data`).
- Name integration tests `*IT` and store them in `src/test/java`; execute with `./mvnw clean test`.
- Ensure deterministic runs by injecting `Clock` instances, fixed seeds, and explicit locale/timezone settings.
- Every PR should prove both JVM and native builds pass: `./mvnw clean test` then `./mvnw clean package -Pnative`.

## Commit & Pull Request Guidelines
- Follow `<type>: <imperative>` commit headers (`feat: add load generator`), mirroring the existing `chore:` history.
- Force-push only to spike branches named `feature/<topic>-spike`; never merge spike code straight to `main`.
- PR descriptions must include: scope summary, linked issue, curl output or screenshots for new endpoints, and native build artifacts size or checksum.
- Capture any GraalVM flags or Docker tweaks in the PR body so reviewers can reproduce the build.
