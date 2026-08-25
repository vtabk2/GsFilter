# AGENTS-TESTING.md

## Philosophy

Prioritize tests for logic.

Tests should protect behavior that is important, easy to regress, and practical to verify automatically.

Do not add tests only to increase line coverage.

## Priority Order

1. Pure Kotlin unit tests.
2. Regression tests for fixed bugs.
3. State and mapping tests.
4. Repository tests using fakes.
5. Coroutine and Flow tests.
6. Database tests.
7. Instrumentation tests when Android behavior is essential.
8. Manual verification for framework, OEM, UI, or storage behavior that cannot be isolated safely.

## What to Test

Prioritize:

- Validation.
- Filtering.
- Sorting.
- Grouping.
- Duplicate detection.
- Rename strategy.
- Path and filename generation.
- Conflict resolution.
- Selection rules.
- Date grouping.
- Size calculations.
- Mapping logic.
- State transitions.
- Batch result aggregation.
- Partial failure behavior.
- Repository coordination.
- Error mapping.
- Regression scenarios.
- Boundary cases.
- Business rules.

## What Not to Force Into Unit Tests

Do not force local JVM tests for:

- Activity rendering.
- Fragment rendering.
- RecyclerView visuals.
- PopupWindow behavior.
- Android permission dialogs.
- OEM settings screens.
- Real MediaStore mutations.
- Real ContentResolver provider behavior.
- Real filesystem permission behavior.
- System UI flags.
- Hardware bitmap behavior.
- Framework lifecycle timing that requires instrumentation.

For these, isolate decision logic and test that logic. Use instrumentation or manual verification only for the Android boundary.

## Extraction for Testability

When logic is mixed with Android APIs:

- Extract only the decision-making portion.
- Keep the extracted API small.
- Do not introduce a large architecture solely for testing.
- Avoid wrapping every Android class behind an interface.
- Prefer a pure function or focused class.
- Keep framework calls in a thin boundary.

Example targets for extraction:

- Whether an album can be renamed directly.
- Whether unrelated files require a temporary move.
- Which duplicate-name policy applies.
- Which permission flow should be selected by API level.
- How operation results are summarized.

## Regression Tests

For a bug fix:

1. Identify the smallest input that reproduces the bug.
2. Write a test that fails with the old behavior when practical.
3. Apply the fix.
4. Confirm the test passes.
5. Add nearby boundary cases when the bug suggests broader risk.

Name the test by behavior, not ticket number alone.

Do not write a test that only mirrors the implementation.

## Test Naming

Use readable behavior-oriented names.

Examples:

```kotlin
@Test
fun `rename strategy uses direct rename when album contains only media`() {
}
```

```kotlin
@Test
fun `duplicate target name returns conflict instead of overwriting`() {
}
```

Avoid names such as:

- `testRename1`
- `worksCorrectly`
- `successCase`
- `methodNameTest`

## Arrange Act Assert

Structure tests clearly:

- Arrange input and dependencies.
- Act once.
- Assert behavior and relevant side effects.

Do not enforce comments when the test is already obvious.

Avoid multiple unrelated acts in one test.

## Test Independence

- Tests must not depend on execution order.
- Do not share mutable state between tests.
- Reset fakes between tests.
- Avoid real time and randomness.
- Inject clock or random source only when behavior depends on them.
- Do not depend on machine-specific paths.
- Do not depend on network access.
- Avoid global singleton mutation.

## Fakes vs Mocks

Prefer fakes when:

- The dependency has meaningful state.
- Multiple calls interact.
- Readability improves.
- The fake can be reused within the feature tests.

Use mocks when:

- Verifying a narrow interaction is important.
- Building a fake would be excessive.
- The project already standardizes on a mocking library.

Avoid:

- Mocking simple data classes.
- Deep mock chains.
- Mocking Android framework internals in local unit tests.
- Verifying every call when only final behavior matters.
- Adding a mocking dependency for one test.

## Coroutine Tests

- Use the project's coroutine test library.
- Use a test dispatcher.
- Avoid real delays.
- Control virtual time explicitly.
- Advance time only when behavior depends on delay.
- Verify cancellation when relevant.
- Verify loading and terminal states.
- Do not use `runBlocking` for coroutine unit tests when `runTest` is available.
- Ensure uncaught child failures fail the test.
- Avoid dispatcher assumptions hidden in production code.

## Flow Tests

Test:

- Initial state.
- Emission order.
- Distinct behavior when relevant.
- Loading-to-success transition.
- Loading-to-error transition.
- Empty state.
- Cancellation.
- Re-subscription behavior when relevant.

Avoid sleeping to wait for emissions.

Use the project's existing Flow testing pattern.

## ViewModel Tests

Test ViewModel logic when it contains meaningful state decisions.

Focus on:

- User intent to state transition.
- Loading behavior.
- Success mapping.
- Error mapping.
- Retry.
- Duplicate action prevention.
- Cancellation or stale-result handling.
- One-time effect behavior when used.

Do not test Android widgets through ViewModel tests.

## Repository Tests

Use fakes for data sources when practical.

Test:

- Correct source selection.
- Data merging.
- Error propagation.
- Partial success.
- Retry policy.
- Cache invalidation.
- Mapping.
- Transaction boundaries.
- Duplicate handling.
- Cancellation.

Do not mock every internal method.

## Room Tests

Use Room tests when:

- A query is complex.
- Migration behavior matters.
- Transactions matter.
- Conflict strategy matters.
- Sorting or grouping depends on SQL.
- Schema changes occur.

Test migrations with realistic old schema data.

Do not use destructive migration as a substitute for migration tests when data must be preserved.

## File and Media Logic Tests

Prefer pure tests for:

- Name sanitization.
- Extension handling.
- Duplicate suffix generation.
- Album rename strategy.
- Temporary move planning.
- Operation ordering.
- Rollback planning.
- Batch summaries.
- MIME grouping.
- Relative path decisions.
- API-level strategy selection.

Use instrumentation or manual tests for actual MediaStore and provider behavior.

## Parameterized Tests

Use parameterized tests when:

- The same behavior must be checked across many inputs.
- Boundary tables improve readability.
- API-level strategy differs predictably.
- Filename validation has many invalid cases.

Do not use parameterization when it makes failures harder to understand.

## Edge Cases

Consider:

- Empty input.
- Single item.
- Duplicate items.
- Very large collections.
- Null metadata.
- Missing extension.
- Dot files.
- Multiple dots.
- Case-only differences.
- Unsupported MIME type.
- Invalid URI.
- Missing source.
- Existing destination.
- Permission denied.
- Partial failure.
- Cancellation.
- Overflow or negative values.
- Timezone and locale effects when applicable.

## Coverage

Coverage is a signal, not the goal.

Prioritize coverage of:

- Core decisions.
- Destructive operations.
- Previously broken logic.
- Error paths.
- Boundary behavior.
- State transitions.

Do not add low-value tests for trivial getters, generated code, or framework wiring merely to raise coverage.

## Existing Tests

- Read existing tests before adding new ones.
- Follow current libraries and naming.
- Reuse fixtures and fakes.
- Do not add a second assertion library without approval.
- Do not rewrite unrelated tests.
- Preserve established test package structure.
- Update tests only when intended behavior changes.

## Test Dependencies

- Use existing test dependencies.
- Do not add a library unless required.
- Explain why a new library is necessary.
- Prefer Kotlin/JUnit/coroutine tools already present.
- Avoid dependency upgrades during a focused test task.

## Flaky Tests

Do not accept flaky tests.

Avoid:

- Real time.
- Random data without fixed seed.
- Network.
- Shared mutable global state.
- Thread sleeps.
- Uncontrolled dispatchers.
- Device-specific assumptions.
- Order dependence.

If a flaky existing test is encountered, report the cause. Do not hide it with retries unless explicitly approved.

## Running Tests

Run the smallest relevant set first.

Then, when practical:

- Run the affected test class.
- Run the affected module tests.
- Run broader tests if risk warrants it.

Report exactly what was run.

Do not say “all tests pass” unless all tests were run.

## Manual Verification

Provide manual steps when framework behavior matters.

Include:

- Device/API level.
- Permission state.
- Starting data state.
- Action.
- Expected result.
- Failure-path check.
- Rotation or process recreation when relevant.
- Large dataset or large image scenario when relevant.

## Test Completion Report

After test work, report:

- Tests added or updated.
- Behaviors covered.
- Commands executed.
- Results.
- Untested Android boundaries.
- Manual verification still required.
- Known limitations.
