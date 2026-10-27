# JitPack Publishing Guide for CrashGuard 🚀

This guide provides the exact workflow to publish `CrashGuard` to **JitPack.io** using GitHub.

---

## 📌 Coordinates Information

| Attribute | Value |
|---|---|
| **GitHub Owner** | `infoadmstudio2020-cyber` |
| **GitHub Repository** | `crash-guard` |
| **Group ID** | `com.github.infoadmstudio2020-cyber` (or `com.roni.library` if custom domain verified) |
| **Artifact ID** | `crash-guard` |
| **Release Version** | `1.0.1` |
| **Complete Coordinate** | `com.github.infoadmstudio2020-cyber:crash-guard:1.0.1` |

---

## 🛠️ Step-by-Step Publishing Workflow

### Step 1: Create a Public GitHub Repository
1. Log in to your GitHub account (`infoadmstudio2020-cyber`).
2. Create a new **public** repository named `crash-guard`.
   *(JitPack requires public repositories for free tier builds)*

### Step 2: Push the Complete Project to GitHub
In your local terminal inside the `CrashGuard` directory:

```bash
# Initialize git if not already initialized
git init -b main

# Add all files
git add .

# Create initial commit
git commit -m "feat: initial release of CrashGuard Android Library v1.0.1"

# Link your remote repository
git remote add origin https://github.com/infoadmstudio2020-cyber/crash-guard.git

# Push main branch
git push -u origin main
```

### Step 3: Create and Push the `1.0.1` Git Tag
JitPack builds artifacts corresponding to Git tags or releases.

```bash
# Create annotated tag for version 1.0.1
git tag -a 1.0.1 -m "Release version 1.0.1"

# Push tag to GitHub
git push origin 1.0.1
```

*Alternatively, create a Release on GitHub web UI with tag `1.0.1`.*

---

## ⚙️ How JitPack Builds `crash-guard`

The repository includes `jitpack.yml` at the project root:

```yaml
jdk:
  - openjdk17

before_install:
  - chmod +x gradlew

install:
  - ./gradlew :crash-guard:assembleRelease :crash-guard:publishToMavenLocal
```

And in `crash-guard/build.gradle`, the Maven publication is defined:

```groovy
afterEvaluate {
    publishing {
        publications {
            release(MavenPublication) {
                from components.release
                groupId = 'com.roni.library'
                artifactId = 'crash-guard'
                version = '1.0.1'
            }
        }
    }
}
```

---

## 🔍 Step 4: Verify on JitPack
1. Open [https://jitpack.io](https://jitpack.io)
2. Enter `infoadmstudio2020-cyber/crash-guard` into the search box and click **Look up**.
3. Locate tag `1.0.1` in the versions table.
4. Click **Get it**.
5. JitPack will trigger its builder and display a status indicator:
   - 🟢 **Green (Log)**: Build succeeded and artifact is indexed!
   - 🔴 **Red (Log)**: Click to inspect build log if any compiler error occurred.

---

## 📲 How Developers Consume Your Library

In consumer `settings.gradle`:
```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

In consumer `app/build.gradle`:
```groovy
dependencies {
    implementation 'com.github.infoadmstudio2020-cyber:crash-guard:1.0.1'
}
```

---

## 🔒 Custom Domain (`com.roni.library`) Verification (Optional)
If you wish to publish under `com.roni.library:crash-guard:1.0.1` instead of `com.github.infoadmstudio2020-cyber`:
1. Add a DNS TXT record for `roni.com` pointing to your GitHub user profile.
2. Follow the official [JitPack Custom Domain documentation](https://jitpack.io/docs/CUSTOM-DOMAIN/).
