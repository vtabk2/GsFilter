# AGENTS-REVIEW.md

## Purpose

Review code for correctness, safety, compatibility, and maintainability.

A review is not an invitation to redesign the entire codebase.

## Review Scope

First identify:

- Exact diff or files under review.
- Intended behavior.
- Affected API levels.
- Data or user impact.
- Whether the change is feature, fix, refactor, test, or build change.

Review changed code deeply and surrounding code only as needed to understand impact.

Do not report unrelated pre-existing style preferences as findings.

## Severity

### Critical

Use for:

- Data loss.
- Data corruption.
- Security vulnerability.
- Exposure of private media or credentials.
- Irrecoverable destructive behavior.
- Guaranteed or near-guaranteed crash in common flow.
- Broken encryption or authentication.
- Broad production outage.

### High

Use for:

- Incorrect user-visible behavior.
- Common crash.
- Race condition.
- Serious lifecycle bug.
- Memory leak with meaningful impact.
- Broken storage or permission flow.
- Major compatibility regression.
- Transaction inconsistency.
- Silent partial failure in destructive operations.

### Medium

Use for:

- Performance regression.
- Less-common crash.
- Maintainability risk likely to cause defects.
- Incomplete error handling.
- Edge-case compatibility issue.
- Incorrect logging of sensitive context.
- Weak test coverage for risky logic.
- State inconsistency under uncommon conditions.

### Low

Use sparingly for:

- Minor readability concern.
- Naming that creates real ambiguity.
- Small consistency issue.
- Non-blocking test quality issue.

Do not label formatting preferences as defects.

## Finding Format

Each finding should include:

- Severity.
- Location.
- Problem.
- Why it matters.
- Reproduction or triggering condition.
- Suggested fix.
- Confidence when uncertain.

Keep findings specific and actionable.

## Review Priorities

Review in this order:

1. Correctness.
2. Data safety.
3. Security and privacy.
4. Crash risk.
5. Concurrency.
6. Lifecycle.
7. API compatibility.
8. Storage and permission behavior.
9. Performance and memory.
10. Architecture.
11. Test quality.
12. Maintainability.
13. Style.

## Correctness

Check:

- Does the implementation match the requested behavior?
- Are all branches handled?
- Are conditions reversed or incomplete?
- Are null and empty cases correct?
- Are duplicate cases correct?
- Are IDs and positions confused?
- Are stale values used?
- Are state transitions valid?
- Is success reported before work completes?
- Are failures converted into false success?
- Are partial results handled correctly?
- Are user actions idempotent where needed?

## File and Media Safety

Check:

- Is source deleted only after destination success?
- Can overwrite happen silently?
- Are duplicate names handled?
- Are extensions preserved?
- Can unrelated files be moved or renamed?
- Can temporary files be orphaned?
- Is rollback possible after partial failure?
- Are URI permissions valid?
- Can the item disappear between query and operation?
- Are cursor and streams closed?
- Is trash recoverable?
- Is locker encryption preserved?
- Can private content become visible?

Treat destructive logic as high-risk.

## Android Compatibility

Check:

- API 24 compatibility.
- API 29 storage behavior.
- API 30+ scoped storage behavior.
- Runtime guards for newer APIs.
- Permission differences by API.
- Notification permission where relevant.
- Photo picker or selected media behavior where relevant.
- Deprecated API usage.
- OEM-sensitive assumptions.
- Target SDK behavior changes.

## Threading and Coroutines

Check:

- Main-thread blocking.
- Incorrect dispatcher.
- Lost cancellation.
- Fire-and-forget work.
- Jobs outliving owners.
- Shared mutable state.
- Race conditions.
- Concurrent rename/delete conflicts.
- Multiple collectors.
- Double execution.
- Unhandled child failures.
- Retry loops.
- Deadlock or lock ordering.
- Thread sleeps.

## Lifecycle

Check:

- Fragment binding after view destruction.
- Callbacks after owner destruction.
- Dialog or PopupWindow leaks.
- Activity references in long-lived objects.
- Duplicate observers.
- Navigation after state saved.
- Repeated events after recreation.
- State loss on rotation.
- Stale work updating new UI.

## Memory

Check:

- Large bitmap retention.
- Full-resolution decode.
- Cursor or stream leaks.
- Adapter retaining Activity.
- Unbounded cache.
- Large collection copies.
- Duplicate media metadata.
- Callback reference cycles.
- Static View or Context references.
- Work queue growth.

## Performance

Check:

- Resolver or database work on main thread.
- Queries inside RecyclerView binding.
- Repeated full scans.
- O(n²) loops.
- Repeated sorting or grouping.
- Excess allocations.
- Large bitmap decoding.
- Missing paging or batching.
- Excessive state emissions.
- Redundant recomputation.
- Synchronous file operations.
- Work repeated on each lifecycle event.

Require evidence before recommending a large optimization rewrite.

## Security and Privacy

Check:

- Exported components.
- Intent validation.
- URI grant flags.
- FileProvider paths.
- Path traversal.
- Sensitive logging.
- Passcode handling.
- Encryption key handling.
- Locker content exposure.
- Insecure temporary files.
- Backup behavior.
- Clipboard exposure.
- Weak random generation.
- Custom cryptography.
- Broad storage access without necessity.

## Architecture

Check:

- UI contains business rules.
- ViewModel depends on Views or Context.
- Repository handles navigation.
- Data layer depends on UI.
- Multiple sources of truth.
- New abstraction lacks value.
- Module dependency direction breaks.
- Generic helper classes grow.
- Feature boundary is violated.
- Refactor scope exceeds the task.

Do not demand a different architecture merely because it is preferred.

## Error Handling

Check:

- Exceptions swallowed.
- Cancellation treated as error.
- Broad catch blocks.
- SecurityException ignored.
- Partial failure hidden.
- User receives misleading success.
- Technical details exposed to user.
- Errors lose root cause.
- Retry applied to permanent failure.
- Cleanup failure ignored.

## State and UI

Check:

- Loading gets stuck.
- Empty and error states conflict.
- Duplicate click triggers.
- Selection breaks after DiffUtil updates.
- RecyclerView recycled state is not reset.
- State emits after operation cancellation.
- One-time effects repeat.
- UI text is hardcoded.
- Accessibility semantics are lost.
- Existing navigation is changed unintentionally.

## Tests

Check:

- Risky logic has coverage.
- Bug fix includes regression coverage when practical.
- Tests verify behavior, not implementation details.
- Tests are deterministic.
- Coroutine tests control time.
- Fakes are reset.
- Test names explain behavior.
- Edge cases are covered.
- Android boundary is not incorrectly mocked.
- Assertions are meaningful.
- Existing test conventions are followed.

Do not demand tests for trivial wiring.

## Build and Dependencies

Check:

- New dependency is necessary.
- Version is compatible.
- API is available in the current version.
- Dependency size and transitive impact.
- Duplicate libraries.
- Gradle configuration scope.
- Release build implications.
- ProGuard or R8 impact.
- minSdk and targetSdk changes.
- Manifest merge impact.

## Review Restraint

Do not report:

- Personal style preference.
- Unrelated pre-existing problems.
- Hypothetical issues with no plausible trigger.
- Large architectural redesign suggestions for a small fix.
- Naming-only findings unless meaning is genuinely unclear.
- “Could be cleaner” without concrete risk.
- Test coverage requests for trivial code.

## Confidence

When uncertain:

- State the uncertainty.
- Explain the condition under which the issue occurs.
- Avoid presenting speculation as fact.
- Suggest a focused verification step.

## Review Output

Start with findings ordered by severity.

For each finding:

```text
[High] Destination can overwrite an existing file

Location:
Reason:
Trigger:
Impact:
Suggested fix:
```

After findings, include:

- Open questions.
- Test gaps.
- Positive notes, only when useful.
- Summary.

If no issues are found, say so clearly and mention remaining verification limits.

## Review Checklist

- Requested behavior understood.
- Critical data paths reviewed.
- API 24, 29, and 30+ considered where relevant.
- Main-thread work checked.
- Lifecycle ownership checked.
- Storage and permission failure checked.
- Partial failure checked.
- Security and privacy checked.
- Large input behavior checked.
- Tests reviewed.
- Findings are specific.
- No unrelated redesign was requested.
