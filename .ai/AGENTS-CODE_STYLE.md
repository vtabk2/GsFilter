# AGENTS-CODE_STYLE.md

## General Style

- Follow existing project formatting.
- Prefer clarity over cleverness.
- Keep diffs focused.
- Avoid unrelated formatting.
- Use descriptive names.
- Keep control flow easy to follow.
- Prefer early returns when they reduce nesting.
- Avoid deeply nested conditionals.
- Avoid unnecessary comments that repeat the code.
- Comment constraints, reasons, and non-obvious behavior.

## Kotlin

- Use Kotlin only unless an existing Java file must be modified.
- Prefer immutable `val` over `var`.
- Avoid `lateinit` when nullable or delegated ownership is safer.
- Avoid `!!` unless an invariant is guaranteed and explained.
- Prefer safe calls and explicit validation.
- Use sealed types or enums for finite states when appropriate.
- Use data classes for data, not behavior-heavy services.
- Avoid excessive scope-function chaining.
- Choose scope functions based on readability.
- Avoid returning `Unit` explicitly.
- Avoid unnecessary explicit types when inference is clear.
- Add explicit types for public APIs or ambiguous expressions.
- Prefer expression bodies only when they remain readable.
- Avoid clever operator overloading.
- Avoid custom infix functions unless the project already uses them.
- Keep extension functions narrow and discoverable.

## Functions

- Keep functions focused on one responsibility.
- Prefer functions under roughly 50 lines when practical.
- Do not split readable logic into many one-line wrappers.
- Extract logic when it improves reuse, testability, or comprehension.
- Avoid long parameter lists.
- Use parameter objects only when they represent a real concept.
- Use default parameters carefully; do not hide behavior changes.
- Avoid Boolean parameters when call sites become unclear.
- Use named arguments for ambiguous Boolean or same-type parameters.
- Make side effects clear from the function name.
- Avoid functions that both query and mutate unless the behavior is explicit.

## Classes

- Keep class responsibility clear.
- Avoid generic `Manager`, `Helper`, `Utils`, or `Processor` classes.
- Prefer composition over inheritance.
- Do not create base classes for one implementation.
- Avoid mutable public properties.
- Keep constructors focused on required dependencies.
- Avoid optional service-locator-style dependencies.
- Keep Android lifecycle owners out of long-lived classes.

## Naming

- Use names that describe domain intent.
- Use verbs for actions.
- Use nouns for values and types.
- Avoid abbreviations unless widely understood in the project.
- Avoid generic names such as `data`, `item`, `value`, or `result` when a domain-specific name is clearer.
- Boolean names should read as conditions: `isLoading`, `hasPermission`, `canRename`.
- Collection names should be plural.
- IDs should make their domain clear when multiple ID types exist.
- Keep resource names consistent with existing conventions.
- Do not rename established public symbols only for preference.

## Nullability

- Model absence explicitly.
- Do not use empty strings as null substitutes unless required by an external API.
- Do not use sentinel values such as `-1` without a documented domain meaning.
- Validate nullable input at boundaries.
- Avoid repeated null checks by narrowing once.
- Preserve null meaning during mapping.
- Do not convert errors to null silently.

## Collections

- Prefer collection operations when readable.
- Avoid long chains that allocate many intermediate collections in hot paths.
- Use sequences only when they provide a measurable or clear benefit.
- Use sets for repeated membership checks.
- Use maps for stable key lookup.
- Avoid mutating a collection while iterating.
- Return immutable views unless mutation is required.
- Avoid repeated sorting or grouping.
- Be careful with list positions after asynchronous updates.

## Coroutines and Suspend Style

- Suspend functions should perform non-blocking or appropriately dispatched work.
- Do not hide launched jobs inside ordinary functions without ownership.
- Preserve cancellation.
- Use explicit dispatcher injection only when the project already supports it or testing requires it.
- Avoid `runBlocking` in production code.
- Do not use `Thread.sleep`.
- Avoid callback-to-coroutine wrappers that can resume more than once.
- Close resources during cancellation.

## Exceptions

- Catch the narrowest useful exception.
- Do not use exceptions for normal control flow.
- Do not swallow failures.
- Preserve causes when wrapping.
- Separate cancellation from failure.
- Add contextual information without exposing sensitive data.
- Avoid broad `catch (Exception)` unless at a boundary with clear fallback.
- Do not catch `Throwable`.

## Logging

- Follow the project's logging framework.
- Do not add a new logging dependency without approval.
- Avoid logging private media names, paths, tokens, passwords, keys, or passcodes.
- Use appropriate log levels.
- Do not leave noisy debug logs in production code.
- Log failures with enough context to diagnose.
- Avoid duplicate logging at every layer.
- Do not use logs as the only user-facing error handling.

## Android Resources

- Never hardcode user-visible text in Kotlin or Java.
- Store user-visible text in `strings.xml`.
- Use plurals for quantity-sensitive grammar when applicable.
- Keep non-translatable strings at the beginning of `strings.xml` when that is the project convention.
- Mark non-translatable strings with `translatable="false"`.
- Do not mix non-translatable strings randomly among translated strings.
- Reuse existing resources when meaning is identical.
- Do not concatenate translated fragments when grammar can vary by language.
- Follow the project's existing localization constraints.

## String Formatting

Project-specific rule:

- Do not use `%s`, `%d`, `%1$s`, `%1$d`, or similar placeholders in `strings.xml`.
- Do not call `getString(resource, formatArgs)` for project strings.
- Build simple dynamic text in Kotlin using clear concatenation or string templates.
- Use `String.format()` only for locale-aware numbers, decimals, dates, times, or fixed Kotlin format strings.
- Do not use `String.format()` with Android string resources.
- Use `Locale` explicitly when formatting depends on locale.
- Avoid manual formatting when a platform formatter is more correct.

Examples:

```kotlin
binding.tvResult.text =
    cleanedCount.toString() + " " + getString(R.string.files_cleaned)
```

```kotlin
val sizeText = String.format(Locale.US, "%.1f MB", sizeMb)
```

## XML

- Preserve existing layout structure unless a change is required.
- Avoid unnecessary nested layouts.
- Use resource dimensions and styles consistently.
- Do not hardcode colors or dimensions when reusable resources exist.
- Keep IDs descriptive.
- Preserve accessibility labels and content descriptions.
- Avoid tools-only attributes affecting runtime assumptions.
- Do not redesign spacing, typography, or navigation without request.
- Verify RTL behavior when layout direction matters.

## View Accessibility

- Provide content descriptions for meaningful icons.
- Do not add content descriptions to decorative views.
- Preserve touch target size.
- Keep text contrast and scaling in mind.
- Do not disable accessibility globally to hide a problem.
- Use `importantForAccessibility` intentionally.
- Ensure controls expose meaningful roles and state.
- Avoid relying only on color to convey state.

## Imports

- Remove unused imports.
- Avoid wildcard imports unless project style requires them.
- Keep import ordering consistent with IDE/project formatting.
- Do not reorder imports in unrelated files.
- Avoid fully qualified names in code when a normal import is clearer.

## Constants

- Avoid magic numbers and magic strings.
- Place constants at the narrowest appropriate scope.
- Do not create global constants for one local use.
- Use meaningful names.
- Keep units in names when ambiguity exists, such as `timeoutMillis`.
- Prefer typed constants or enums for finite concepts.

## Comments and Documentation

- Explain why, not what.
- Document public APIs when behavior is non-obvious.
- Document destructive behavior and invariants.
- Document API-level workarounds.
- Reference issue IDs only when useful and durable.
- Remove stale comments.
- Do not leave commented-out code.
- Use TODO only with actionable context.
- Do not generate verbose KDoc for obvious private functions.

## File Organization

- Keep one clear primary responsibility per file.
- Avoid placing unrelated classes in one file.
- Small private types may remain near their owner.
- Do not create generic extension files that accumulate unrelated functions.
- Place extensions near their domain.
- Preserve package structure.
- Do not move files for aesthetic reasons during unrelated tasks.

## Formatting

- Use project formatter settings.
- Avoid large whitespace-only diffs.
- Preserve line endings.
- Keep code easy to diff.
- Do not manually align code with fragile spaces.
- Use trailing commas only if project style uses them.
- Do not add or remove final newlines as an unrelated task.

## Deprecated Code

- Do not introduce deprecated APIs.
- If existing deprecated code must be touched, use a supported alternative when scope permits.
- Do not suppress deprecation warnings without a documented reason.
- Do not perform a broad deprecation migration during a small task.

## Code Style Checklist

- Names reflect domain intent.
- Nullability is explicit.
- No unsafe `!!` was introduced.
- Functions remain focused.
- No unnecessary helper or abstraction was added.
- User-visible text is in resources.
- String formatting follows project rules.
- Logging avoids sensitive information.
- Imports are clean.
- No unrelated formatting occurred.
