# AGENTS-ANDROID.md

## Scope

These rules apply to Android-specific implementation work.

## Platform Compatibility

- Minimum supported API is 24.
- Do not use newer APIs without runtime guards or compatible alternatives.
- Prefer AndroidX compatibility APIs.
- Verify behavior on API 24-28, API 29, and API 30+ when storage or permissions are involved.
- Do not assume emulator behavior matches OEM devices.
- Avoid relying on undocumented OEM behavior.
- Preserve compatibility with current target SDK requirements.

## Context Usage

- Prefer the narrowest valid context.
- Do not retain Activity or Fragment context in long-lived objects.
- Use application context only when UI context is not required.
- Do not pass Context into pure logic.
- Avoid static references to Context, View, Activity, Fragment, Dialog, or PopupWindow.
- Release references owned by views or adapters when lifecycle ends.

## Lifecycle

- Use lifecycle-aware scopes.
- Do not launch UI-related work in `GlobalScope`.
- Cancel work when the owning lifecycle ends.
- Check whether callbacks can arrive after Fragment view destruction.
- Do not access binding outside the valid view lifecycle.
- Avoid collecting flows multiple times unintentionally.
- Use repeat-on-lifecycle patterns where appropriate.
- Ensure dialogs and popups are dismissed safely.

## Coroutines

- Use structured concurrency.
- Use `Dispatchers.IO` for blocking file, database, stream, and resolver operations.
- Do not switch dispatchers unnecessarily.
- Preserve cancellation.
- Do not convert cancellation into a generic failure.
- Avoid launching nested coroutines when sequential suspend calls are sufficient.
- Avoid fire-and-forget jobs unless ownership and cancellation are explicit.
- Use supervisor behavior only when independent child failure is intended.
- Handle partial failures explicitly in batch work.
- Avoid blocking calls inside suspend functions.

## Flow and State

- Keep UI state immutable where practical.
- Avoid exposing mutable flows directly.
- Do not use events as persistent state.
- Avoid duplicate collectors.
- Avoid emitting identical expensive state repeatedly.
- Use state holders consistent with the project.
- Do not migrate LiveData to Flow, or Flow to LiveData, without scope justification.
- Handle initial, loading, success, empty, and error states when relevant.
- Preserve state across configuration changes through existing project mechanisms.

## Activity and Fragment

- Keep business decisions outside Activity and Fragment.
- Use Activities and Fragments for orchestration, navigation, binding, and user interaction.
- Avoid large methods that mix permission, storage, UI, and business rules.
- Preserve existing navigation behavior.
- Do not redesign screens unless requested.
- Avoid Fragment transactions after state has been saved unless behavior is explicitly safe.
- Handle repeated taps and duplicate navigation.

## ViewBinding

- Use ViewBinding where configured.
- Do not introduce synthetic view access.
- In Fragments, clear binding references at the correct lifecycle point.
- Do not store child view references longer than necessary.
- Avoid binding work that causes repeated expensive computation.
- Keep listener registration and removal lifecycle-safe.

## RecyclerView

- Prefer `ListAdapter` and `DiffUtil` when consistent with existing code.
- Avoid `notifyDataSetChanged()` unless item-level diffing cannot express the update.
- Use stable IDs only when IDs are truly stable.
- Do not perform resolver, file, bitmap, or database work in `onBindViewHolder`.
- Avoid allocating formatters or heavy objects repeatedly during binding.
- Reset recycled view state completely.
- Avoid adapter references to destroyed Activities or Fragments.
- Use payloads for focused updates when beneficial.
- Verify selection state with list updates.
- Avoid position-based logic after asynchronous changes; use item identity.

## Bitmap and Image Loading

- Never decode a full-resolution bitmap unless required.
- Use sampled decoding for large images.
- Respect EXIF orientation when necessary.
- Close streams reliably.
- Avoid keeping many large bitmaps strongly referenced.
- Use the existing image-loading library when available.
- Avoid decoding on the main thread.
- Handle malformed, unsupported, and partially downloaded images.
- Use region or subsampling viewers for extremely large images.
- Consider hardware bitmap limitations before editing or pixel access.
- Avoid repeated thumbnail generation during scrolling.
- Recycle only when ownership is clear; do not manually recycle bitmaps owned by libraries.

## MediaStore

- Prefer `content://` URIs over direct paths.
- Query only required columns.
- Close cursors.
- Handle missing columns and null values safely.
- Avoid resolver queries inside tight UI loops.
- Batch work when practical.
- Respect scoped storage.
- For API 29+, use MediaStore semantics appropriate to the operation.
- Use pending state for writes when appropriate.
- Use relative paths where supported.
- Handle recoverable security exceptions and user approval flows.
- Do not assume `_data` is available or valid.
- Do not convert URIs to file paths unless strictly necessary and supported.
- Handle deleted or moved media between query and access.
- Refresh data after mutations through the existing source-of-truth mechanism.

## File and Album Operations

- Treat rename, move, copy, delete, restore, and album operations as transactional where practical.
- Confirm the destination before deleting or replacing the source.
- Define duplicate-name behavior.
- Sanitize names.
- Reject invalid or reserved names.
- Preserve file extensions unless the user explicitly changes format.
- Consider case-only rename behavior on case-insensitive filesystems.
- Handle partial success in multi-file operations.
- Return or record per-item results when batch behavior matters.
- Avoid renaming a directory when unrelated file types inside it could be affected without approval.
- If temporary movement is used, ensure rollback behavior is possible.
- Prevent operations from escaping the intended directory.
- Do not trust user-visible names as unique identifiers.

## Scoped Storage

### API 24-28

- Direct file APIs may be available with runtime permissions.
- Verify read and write permission behavior.
- Do not assume a path is writable merely because it exists.
- Handle removable storage and unavailable volumes.

### API 29

- Treat API 29 as a distinct compatibility case.
- Verify legacy and scoped storage configuration.
- Avoid assuming API 30 all-files access behavior exists.
- Prefer MediaStore-compatible solutions.
- Test rename and move behavior carefully.

### API 30+

- Do not request broad storage access unless the app's core use case qualifies.
- Use MediaStore, SAF, or user-granted access where appropriate.
- Handle all-files access denial safely.
- Do not direct users to settings unless necessary.
- Ensure permission UX explains why access is required.

## Storage Access Framework

- Use SAF when the user must grant access to arbitrary files or directories outside normal MediaStore access.
- Persist URI permissions when long-term access is required and allowed.
- Handle revoked permissions.
- Do not assume document providers support every operation.
- Check provider capabilities.
- Use streams instead of file paths.
- Handle providers with unknown size or MIME type.
- Do not perform network-backed document operations on the main thread.

## Permissions

- Request only permissions required for the current action.
- Explain the reason before requesting sensitive access.
- Handle denial, permanent denial, and partial grant.
- Avoid request loops.
- Do not infer permission from settings UI alone; verify actual capability.
- Account for permission model differences across Android versions.
- Handle photo picker and selected-media access where applicable.
- Do not crash when permissions change while the app is running.
- Preserve usable limited functionality when full access is unavailable.

## Room and Database

- Keep database work off the main thread.
- Use transactions for multi-step consistency.
- Do not use destructive migration unless data loss is explicitly acceptable.
- Add migrations for schema changes.
- Ensure indexes support frequent queries.
- Avoid loading unbounded result sets.
- Use stable entities and mappings.
- Keep Android framework dependencies out of pure database logic.
- Handle database corruption and migration failure according to product requirements.
- Do not silently change column meaning.

## WorkManager and Background Work

- Use WorkManager for deferrable persistent work.
- Do not use WorkManager for immediate UI-bound actions.
- Define constraints intentionally.
- Make workers idempotent.
- Handle retries with backoff.
- Do not retry permanent failures.
- Avoid passing large payloads through WorkManager Data.
- Persist progress or operation state when required.
- Respect cancellation.

## Services and Notifications

- Use foreground services only when platform policy and user-visible ongoing work require them.
- Create notification channels correctly.
- Keep notification text in resources.
- Handle notification permission requirements.
- Stop services when work completes.
- Avoid background execution violations.
- Do not hide ongoing user-impacting operations.

## PopupWindow, Dialog, and System UI

- Preserve immersive mode behavior when showing and dismissing popups.
- Avoid stealing focus unexpectedly.
- Verify behavior on API 29 when immersive flags are involved.
- Ensure PopupWindow and Dialog do not leak Activity.
- Reapply system UI state only when necessary.
- Avoid repeated application of flags that causes flicker.
- Dismiss safely during lifecycle changes.

## Performance

- Never block the main thread.
- Avoid repeated full media scans.
- Support paging, batching, or incremental updates for large datasets.
- Avoid O(n²) behavior in file or media collections.
- Use sets or maps for repeated membership lookup.
- Avoid unnecessary object allocation in loops.
- Cache only when invalidation is clear.
- Avoid premature caching that risks stale data.
- Profile before large optimization rewrites.
- Preserve responsiveness during long operations.
- Expose progress when work is visibly long.
- Support cancellation where practical.

## Memory

- Avoid retaining large lists when streaming or paging is possible.
- Avoid duplicate in-memory copies of media metadata.
- Avoid retaining bitmaps, cursors, streams, views, and contexts.
- Close all closeable resources.
- Watch for callbacks that outlive owners.
- Avoid unbounded caches.
- Consider low-memory devices.
- Handle large albums and 16K images safely.
- Do not load entire files merely to inspect metadata.

## Error Handling

- Surface actionable errors.
- Distinguish permission, not-found, duplicate, unsupported, canceled, and I/O failures.
- Do not swallow `SecurityException`.
- Do not hide partial failure.
- Preserve original exceptions when wrapping.
- Log technical context without exposing sensitive paths or content.
- Keep user messages understandable and localized.

## Security and Privacy

- Do not expose private media through exported components.
- Validate FileProvider paths.
- Use temporary URI grants correctly.
- Revoke grants when appropriate.
- Keep locker content encrypted if the feature promises encryption.
- Do not log secrets, passcodes, encryption keys, or private filenames unnecessarily.
- Use secure random generation for cryptographic material.
- Do not implement custom cryptography when approved platform primitives exist.
- Define backup behavior for sensitive content.
- Validate incoming intents and URIs.

## Android Completion Checklist

- API 24 behavior considered.
- API 29 storage behavior considered.
- API 30+ storage behavior considered.
- Main-thread safety checked.
- Lifecycle and cancellation checked.
- Permission denial checked.
- URI validity checked.
- Cursor and stream closure checked.
- Large dataset behavior checked.
- Large image behavior checked.
- OEM-sensitive behavior identified.
- User-visible strings use resources.

## Implementation Planning

These rules apply only when the request requires modifying, adding, removing, or refactoring source code.

Before modifying any source code:

1. Check whether `PLAN.md` exists at the project root.

2. If `PLAN.md` exists:
   - Read `PLAN.md` before analyzing or modifying source code.
   - Check whether the current request continues, changes, or expands an existing task.
   - If the request continues an existing task, continue that task instead of creating a duplicate.
   - Review the existing task status, requirements, approach, checklist, and notes before continuing.

3. If `PLAN.md` does not exist:
   - Create `PLAN.md` at the project root before modifying source code.

4. For every source-code change:
   - Create a new plan item if the request is unrelated to existing tasks.
   - Update the existing plan item if the request continues or expands an existing task.
   - Set the relevant task to `IN PROGRESS` before modifying source code.

5. Do not modify source code before `PLAN.md` has been created or updated for the current request.

6. During implementation:
   - Keep the plan synchronized with important discoveries, decisions, scope changes, and implementation progress.
   - Update the checklist as steps are completed.
   - Record blockers or important technical findings when they affect the implementation.

7. After implementation:
   - Verify the changes according to the applicable Android rules and project requirements.
   - Update the plan with the actual implementation result.
   - Mark the task as `DONE` only after verification is complete.

8. Keep completed tasks in `PLAN.md` as project history unless explicitly asked to remove them.

9. Do not create duplicate plan items for the same ongoing task.

Requests that only require explanation, analysis, debugging guidance, code review, documentation, or answering questions do not require `PLAN.md` unless source code will actually be modified.