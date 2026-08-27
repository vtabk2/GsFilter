# AGENTS-RELEASE.md

## Scope

These rules apply when preparing a release or writing release notes.

## Release Note Location

- Store release notes in `docs/`.
- Use one Markdown file per release.
- Name each file `release-notes-X.Y.Z.md`, where `X.Y.Z` is the released version.
- Do not put release notes in `PLAN.md`; `PLAN.md` is only for implementation tracking.

## PLAN Metadata

When a completed PLAN item should be considered for release notes, include lightweight metadata near the top of that item:

```md
Status: DONE
Release: X.Y.Z
Completed: YYYY-MM-DD
Docs: docs/release-notes-X.Y.Z.md
```

- Use `Release: X.Y.Z` for work assigned to a known release.
- Use `Release: Unreleased` when the work is complete but the release version is not assigned yet.
- Use date-only `Completed: YYYY-MM-DD`; do not include time unless it matters for an audit.
- Add `Docs:` after release notes are written; omit it when no release note exists yet.
- During release note preparation, scan the whole `PLAN.md` and include completed items matching `Status: DONE` and the target `Release:`.
- Keep full release notes out of `PLAN.md`; store them in `docs/` using the release note rules below.

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
