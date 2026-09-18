# Release Guide

This document provides a comprehensive guide to releasing the **OSGi Messaging Adapter** artifacts to Maven Central via the Sonatype Central Publisher Portal.

---

## 1. Overview

The project publishes three OSGi bundles under the `in.bytehue` group ID:
* `in.bytehue:in.bytehue.messaging.mqtt5.api` — The MQTT 5.0 messaging API specification.
* `in.bytehue:in.bytehue.messaging.mqtt5.provider` — The self-sufficient OSGi MQTT 5.0 provider implementation backed by HiveMQ.
* `in.bytehue:in.bytehue.messaging.mqtt5.remote.adapter` — REST-like remote resource management adapter.

### Versioning Mechanism
The build relies on Bnd Workspace and two tracking files under `cnf/version/`:
* `cnf/version/current.version` — The active version being built (e.g. `1.3.0.SNAPSHOT` or `1.3.0`).
* `cnf/version/baseline.version` — The baseline version (the previous release, e.g. `1.2.0`) used by Bnd to enforce OSGi Semantic Versioning rules.

---

## 2. Prerequisites

### Environment Variables
Before triggering a local release, configure the following secrets in your shell environment:

```bash
export SONATYPE_USERNAME="<your-sonatype-portal-token-username>"
export SONATYPE_PASSWORD="<your-sonatype-portal-token-password>"
export GPG_PASSPHRASE="<your-gpg-passphrase>"
```

### Tooling
* **Java**: JDK 17 (recommended) or JDK 25 (Java 8 bytecode compliant via `sourceCompatibility = 1.8`).
* **GPG**: Installed and accessible in your `PATH` (configured with pinentry loopback mode).
* **Git**: Clean working directory on the `main` branch with push permissions to `origin`.

---

## 3. Local Release Workflow (`./scripts/release.sh`)

The primary and recommended release mechanism is the interactive script [./scripts/release.sh](scripts/release.sh).

### Step 1: Pre-flight Verification
Ensure the repository is clean and on the latest `main` branch:
```bash
git checkout main
git pull origin main
git status
```

### Step 2: Run the Release Script
Execute the script from the root directory:
```bash
./scripts/release.sh
```

### Step 3: Interactive Prompts
1. **Next Version Prompt**:
   The script displays the current version to release (e.g. `1.3.0`) and asks for the subsequent development version:
   ```text
   Current Version (to be released): 1.3.0
   Enter the next version (e.g. 1.2.0): 1.3.1
   ```
2. **Confirmation**:
   Type `y` to proceed.

### What the Script Performs Automatically:
1. **Version Update**: Removes `.SNAPSHOT` from `cnf/version/current.version` (setting `1.3.0`).
2. **Git Commit & Tag**:
   * Commits the release preparation: `🏁 REL v1.3.0 Preparation`.
   * Creates the git tag: `v1.3.0`.
   * Pushes the tag to `origin`: `git push origin v1.3.0`.
3. **Build & Test**:
   * Calls [./scripts/build.sh](scripts/build.sh), which spins up a local background HiveMQ broker on port 1883, runs `./gradlew clean build` and all test suites, and gracefully shuts down the broker.
4. **Bundle Release to Sonatype**:
   * Cleans staging folders (`cnf/cache/sonatype-release/` and `cnf/target/sonatype-staging/`).
   * Publishes bundles sequentially:
     - `./gradlew :in.bytehue.messaging.mqtt5.api:release`
     - `./gradlew :in.bytehue.messaging.mqtt5.provider:release`
     - `./gradlew :in.bytehue.messaging.mqtt5.remote.adapter:release`
   * Bnd stages the artifacts, signs them with GPG, and uploads them to the Sonatype Portal API (`https://central.sonatype.com/api/v1/publisher/upload`) in `autopublish` mode.
5. **Next Development Cycle Setup**:
   * Sets `cnf/version/baseline.version` to `1.3.0`.
   * Sets `cnf/version/current.version` to `1.3.1.SNAPSHOT`.
   * Commits locally: `🏁 Next Development Cycle Preparation`.

### Step 4: Push Main Branch
Once the script completes, push the local release commits to `origin`:
```bash
git push origin main
```

---

## 4. CI/CD Automated Release (GitHub Actions)

A GitHub Actions workflow is also configured at [.github/workflows/release.yml](.github/workflows/release.yml).

### Triggering CI Release
1. Create a `release` branch or push release-ready commits to `origin/release`:
   ```bash
   git checkout -b release
   git push origin release
   ```
2. The workflow will:
   * Validate the Gradle wrapper.
   * Set up Java 17 (Zulu).
   * Launch the local HiveMQ broker and execute the full test suite.
   * Publish the provider bundle to Sonatype using GitHub repository secrets `SONATYPE_USERNAME` and `SONATYPE_PASSWORD`.

---

## 5. Troubleshooting & FAQs

### Port 1883 is Occupied
If `./scripts/start-broker.sh` fails because port 1883 is in use:
```bash
pgrep -f hivemq | xargs kill -9 2>/dev/null || true
```

### Sonatype 401 / Authentication Errors
Ensure that `SONATYPE_USERNAME` and `SONATYPE_PASSWORD` correspond to a generated **Sonatype Central User Token**, not your web account login password.

### GPG Signing Failure
If GPG fails with `gpg: signing failed: Inappropriate ioctl for device`, ensure loopback pinentry is enabled:
```bash
export GPG_TTY=$(tty)
```
In `~/.gnupg/gpg.conf`:
```text
use-agent
pinentry-mode loopback
```
In `~/.gnupg/gpg-agent.conf`:
```text
allow-loopback-pinentry
```
Restart the GPG agent:
```bash
gpgconf --kill gpg-agent
```
