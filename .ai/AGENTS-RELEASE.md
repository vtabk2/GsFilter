# AGENTS-RELEASE.md

## Scope

These rules apply when preparing a release or writing release notes.

## Release Note Location

- Store release notes in `docs/`.
- Use one Markdown file per release.
- Name each file `release-notes-X.Y.Z.md`, where `X.Y.Z` is the released version.
- Do not put release notes in `PLAN.md`; `PLAN.md` is only for implementation tracking.

## Release Note Format

Follow this structure:

````md
# Release notes X.Y.Z

Short summary of the release.

## Thay doi

- User-facing or important technical change.

## Nang cap

```kotlin
dependencies {
    implementation("group:name:X.Y.Z")
}
```

## Migration

- Required migration step, or state that no code change is required.

**Commit hom nay:** [`short_sha`](commit_url)
````

Omit sections that do not apply to the release.

## Content Rules

- Prefer Vietnamese release notes when the surrounding release documentation is Vietnamese.
- Summarize behavior that matters to the app or integrator.
- Include upgrade instructions when a dependency version changes.
- Include migration notes when public APIs, resources, manifest entries, permissions, setup, or behavior changed.
- Include known risks or manual checks when validation is incomplete.
- Do not list every internal refactor unless it affects usage, migration, or release risk.
