# Android SBI

MOSIP-compliant Secure Biometric Interface (SBI) client for Android.

Package: `io.tech.sbi` · `minSdk 26` · `targetSdk 36`

---

## Building a release APK

### Prerequisites

| Requirement | Notes |
|---|---|
| **JDK 17 or newer** | Android Gradle Plugin 8.6 refuses to run on anything older. JDK 11 fails with `Android Gradle plugin requires Java 17 to run`. |
| **Android SDK** | Path set via `sdk.dir` in `local.properties`, or the `ANDROID_HOME` environment variable. |

### 1. Generate a signing keystore (once, ever)

Only needed if your team does not already have a release keystore. **If one
exists, use it** — see [Signing key custody](#signing-key-custody).

```bash
keytool -genkeypair -v -keystore sbi-release.jks -keyalg RSA -keysize 4096 -validity 10000 -alias sbi
```

You will be prompted for a keystore password and certificate details (name,
organisation, locality, country). The certificate details are cosmetic —
Android only checks that the signing key is unchanged between updates. When
asked for a key password, press RETURN to reuse the keystore password.

`-validity 10000` is roughly 27 years, deliberately: a signing key that expires
strands the app, and Play Store requires validity beyond 2033.

Store the `.jks` **outside the repository** — for example `~/keys/` or
`C:\keys\` — so it can never be committed by accident.

### 2. Configure credentials

Update `keystore.properties` in the repository root.

```properties
storeFile=C:/keys/sbi-release.jks
storePassword=<your keystore password>
keyAlias=sbi
keyPassword=<your key password>
```

Use **forward slashes** in `storeFile`. This is a Java `.properties` file, where
a backslash is an escape character.

If this file is absent the release build still succeeds — it just produces an
unsigned APK, so contributors and CI without the keystore are not blocked.

### 3. Build

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

If the file is named `app-release-unsigned.apk`, `keystore.properties` was not
found or not readable.

### 4. Verify the signature

```bash
$ANDROID_HOME/build-tools/<version>/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

Expect `Verified using v2 scheme (APK Signature Scheme v2): true`. v1 (JAR
signing) is correctly `false` — `minSdk` is 26, so every supported device
handles v2.

Record the certificate SHA-256 fingerprint. It is how you confirm a future
build came from the same key.

---

## Signing key custody

**Whoever holds the keystore controls all future updates of this app.** Android
rejects an update signed with a different key.

- Back up the `.jks` and its passwords somewhere durable. If lost, existing
  installations can never be updated — only uninstalled and replaced.
- Never commit the keystore or `keystore.properties`.
- Before generating a new key, confirm no production key already exists.

### The signing key changes the device serial

`CommonDeviceAPI.getSerialNumber()` returns `Settings.Secure.ANDROID_ID`, which
Android scopes to *(signing key, user, device)*. **Changing the signing key
changes the device's serial number.**

A handset moving from a differently-signed build to this one therefore reports
a new serial, and silently loses its DMS registration and certificates. Plan for
re-registration when the signing key changes.
