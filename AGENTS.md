# AGENTS.md

## Purpose

This file is the entry point for AI-assisted work in this Android project.

The goal is not to produce the most code. The goal is to make the smallest correct change, preserve existing behavior, reduce regression risk, and keep the codebase understandable.

## Project Context

- Android application written in Kotlin.
- Minimum supported Android version: API 24.
- Build system: Gradle Kotlin DSL.
- UI system: XML and ViewBinding unless the project already uses another system.
- The application may work with media, files, albums, storage, cleanup, gallery, locker, trash, editing, and large image handling.
- Existing project conventions always take priority over generic recommendations.

## Instruction Files

Always read and follow:

- `.ai/AGENTS-ANDROID.md`
- `.ai/AGENTS-ARCHITECTURE.md`
- `.ai/AGENTS-CODE_STYLE.md`

When creating, updating, or reviewing tests, also read:

- `.ai/AGENTS-TESTING.md`

When reviewing code, a commit, a pull request, a patch, or a proposed implementation, also read:

- `.ai/AGENTS-REVIEW.md`

When preparing a release, also read:

- `.ai/AGENTS-RELEASE.md`

## Core Principles

1. Understand before changing.
2. Read before writing.
3. Preserve existing architecture.
4. Prefer small, local, reversible changes.
5. Fix root causes instead of hiding symptoms.
6. Do not guess missing product requirements.
7. Do not modify unrelated code.
8. Reuse existing abstractions and utilities.
9. Prefer correctness and data safety over brevity.
10. Explain meaningful risks and trade-offs.

## Required Workflow

### 1. Understand the Request

Before writing code:

- Restate the task internally in concrete terms.
- Identify the expected behavior.
- Identify the current behavior when known.
- Identify affected screens, classes, modules, files, data flows, and API levels.
- Separate explicit requirements from assumptions.
- Note whether the task is a bug fix, feature, refactor, test task, investigation, or review.

Do not start implementation from the task title alone.

### 2. Inspect the Existing Code

Read the relevant code before proposing a solution.

Inspect:

- The direct call site.
- Related interfaces and implementations.
- Existing helpers and extensions.
- Similar features elsewhere in the project.
- Existing tests.
- Gradle dependencies and versions when relevant.
- Manifest declarations and resources when relevant.
- API-level branches when relevant.

Do not create a new helper, repository, use case, wrapper, extension, or abstraction before checking whether one already exists.

### 3. Analyze the Problem

Determine:

- The root cause.
- Whether the issue is local or systemic.
- Whether there are API-level differences.
- Whether threading, lifecycle, state, permission, storage, or memory is involved.
- Whether the requested behavior conflicts with existing behavior.
- Whether the change can cause data loss, corruption, or compatibility regressions.

For bug fixes, distinguish the root cause from the visible symptom.

### 4. Consider Solutions

If multiple valid approaches exist:

- Present the practical options.
- Explain the trade-offs.
- Recommend one.
- Prefer the option with the smallest safe diff.
- Prefer consistency with the current codebase.
- Avoid speculative abstractions for hypothetical future needs.

Do not list many theoretical alternatives when one clear approach already matches the project.

### 5. Handle Ambiguity

Do not guess when ambiguity changes behavior.

Ask for clarification when:

- Two interpretations produce different user-visible behavior.
- A destructive operation is not clearly specified.
- Backward compatibility requirements are unclear.
- The requested scope is unclear.
- The expected fallback is unclear.
- A rename, move, delete, overwrite, restore, encrypt, or permission flow can have multiple meanings.

A good clarification question is short and concrete.

Examples:

- Should existing files with the same name be overwritten or renamed?
- Should this apply to one item or the entire album?
- Should failure stop the operation or skip the failed item?
- Is API 29 behavior required to match API 30+ exactly?

If minor details do not materially change the result, use the safest reasonable assumption and state it.

### 6. Implement the Smallest Safe Change

During implementation:

- Modify only files required for the task.
- Preserve public APIs unless change is necessary.
- Avoid broad renames.
- Avoid formatting unrelated code.
- Avoid moving files without a clear need.
- Avoid adding dependencies.
- Avoid changing architecture.
- Avoid replacing working code merely because another style is preferred.
- Keep the diff easy to review.

If an unrelated issue is discovered, report it separately. Do not fix it without approval unless it blocks the requested task.

### 7. Validate

Before finishing:

- Check compilation implications.
- Check nullability.
- Check threading.
- Check lifecycle behavior.
- Check API 24 compatibility.
- Check storage and permission failure paths.
- Check empty, null, duplicate, and large-input cases.
- Check whether existing behavior is preserved.
- Run relevant tests when available.
- Add or update tests when appropriate.
- Check imports and unused code.
- Review the final diff for accidental changes.

### 8. Report

After implementation, provide:

- What changed.
- Why it changed.
- Files changed.
- Tests added or updated.
- Tests or checks performed.
- Known risks or limitations.
- Manual verification steps when necessary.

Do not claim that tests passed unless they were actually run.

## Scope Rules

### Allowed

- Changes directly required by the task.
- Small supporting changes needed for correctness.
- Focused refactors required to make the fix safe or testable.
- Regression tests for affected logic.
- Comments that explain non-obvious constraints.

### Not Allowed Without Explicit Request

- Unrelated refactoring.
- Architecture migration.
- Dependency upgrades.
- Build tool upgrades.
- Package renaming.
- Large file reformatting.
- UI redesign.
- Navigation redesign.
- Changing minSdk or targetSdk.
- Replacing XML with Compose.
- Replacing LiveData with Flow or vice versa across unrelated code.
- Introducing new modules.
- Adding generic helper layers.
- Fixing unrelated warnings.

## Decision Priority

When choosing between implementations, prioritize:

1. Correctness.
2. Data safety.
3. Compatibility.
4. Existing project consistency.
5. Small diff.
6. Testability.
7. Performance.
8. Maintainability.
9. Elegance.

Do not choose an implementation only because it is shorter.

## Dependency Rules

- Do not add a dependency when the platform or existing dependencies can solve the problem cleanly.
- Do not upgrade dependencies unless explicitly requested or strictly required.
- Check existing versions before using APIs from a library.
- Avoid adding a large library for a small utility.
- Explain the reason and impact before adding any dependency.
- Prefer dependencies already approved by the project.

## File Safety

For file, media, storage, locker, trash, rename, move, copy, delete, restore, or cleanup operations:

- Treat data loss as a critical risk.
- Define overwrite behavior explicitly.
- Handle partial failure.
- Preserve recoverability when the feature expects trash or restore.
- Avoid deleting the source before confirming the destination write.
- Avoid assuming direct file paths are available.
- Do not silently skip failed items without reporting or recording them.
- Keep operations cancellable where practical.
- Do not expose unencrypted locker content accidentally.

## Communication Style

Before coding, when analysis is requested or ambiguity exists, use:

- Understanding
- Current behavior
- Root cause
- Options
- Recommendation
- Question, if needed

After coding, use:

- Changes
- Reason
- Validation
- Risks
- Manual checks

Be concise but include important technical reasoning.

## Forbidden Behavior

- Do not invent project APIs or classes.
- Do not claim to have read files that were not inspected.
- Do not claim to have run commands that were not run.
- Do not hide uncertainty.
- Do not use deprecated APIs when a supported alternative exists.
- Do not block the main thread.
- Do not use `GlobalScope`.
- Do not hardcode user-visible text.
- Do not use root-only solutions.
- Do not weaken security to simplify implementation.
- Do not swallow exceptions without a reason.
- Do not catch `Exception` broadly unless boundary handling requires it and the failure is logged or surfaced appropriately.
- Do not introduce silent behavior changes.

## Final Checklist

Before completing any task, verify:

- The request is fully addressed.
- The implementation matches existing project patterns.
- No unrelated files changed.
- API 24 compatibility is preserved.
- Main-thread blocking was not introduced.
- Lifecycle and cancellation are handled.
- Storage and permission failures are safe.
- User-visible text uses resources.
- Imports are clean.
- Unused code is removed.
- Relevant tests exist or the absence of tests is explained.
- Risks and manual verification are documented.
