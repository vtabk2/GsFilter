# AGENTS Pack

Place the extracted files at the root of the Android project:

```text
YourProject/
├── AGENTS.md
├── .ai/
│   ├── AGENTS-ANDROID.md
│   ├── AGENTS-ARCHITECTURE.md
│   ├── AGENTS-CODE_STYLE.md
│   ├── AGENTS-TESTING.md
│   └── AGENTS-REVIEW.md
├── README.md
├── app/
├── build.gradle.kts
└── settings.gradle.kts
```

## Pre-merge verification

Run the following command to verify full compile and OTP flow before merging:

```bash
./gradlew :app:compileDevDebugKotlin :app:compileProdDebugKotlin :app:testDevDebugUnitTest :app:testProdDebugUnitTest
```
