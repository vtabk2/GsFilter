# AGENTS-ARCHITECTURE.md

## Principle

Follow the architecture that already exists.

Do not introduce MVVM, MVI, Clean Architecture, use cases, repositories, new modules, dependency injection frameworks, or abstraction layers unless:

- The pattern already exists and the change belongs within it.
- The requested feature clearly requires it.
- The user explicitly requests an architectural change.

## Architecture Discovery

Before changing structure, inspect:

- Package organization.
- Module boundaries.
- Existing ViewModels.
- Existing repositories.
- Existing domain or use-case layers.
- Data source patterns.
- Dependency injection patterns.
- Navigation approach.
- State management approach.
- Error and result types.
- Test structure.

Do not infer architecture from class names alone.

## Dependency Direction

Dependencies should point toward stable logic.

Prefer:

- UI depends on presentation/state logic.
- Presentation depends on domain or repository contracts when those layers exist.
- Data implementations depend on platform and persistence APIs.
- Pure logic does not depend on Android framework classes.

Avoid:

- Repository depending on Fragment or Activity.
- ViewModel directly manipulating Views.
- Pure logic accepting Context.
- Data layer triggering navigation.
- UI owning persistence rules.
- Utility classes becoming hidden global dependencies.

## Responsibilities

### Activity and Fragment

May handle:

- View binding.
- User interaction.
- Navigation.
- Permission launchers.
- Rendering state.
- Connecting lifecycle to collectors.

Should not own:

- Business rules.
- File operation strategy.
- Sorting or filtering rules.
- Data consistency logic.
- Complex validation.
- Long-running work orchestration that belongs elsewhere.

### ViewModel

May handle:

- UI state.
- User intents.
- Coordination of operations.
- Mapping domain results to presentation state.
- Lifecycle-independent state decisions.

Should not:

- Hold Activity, Fragment, View, Dialog, or PopupWindow.
- Depend directly on UI widgets.
- Perform direct filesystem or resolver work when a repository layer exists.
- Become a generic dumping ground.
- Expose mutable state publicly.

### Repository

May handle:

- Data access coordination.
- MediaStore and database access.
- File operations.
- Source-of-truth decisions.
- Mapping platform errors into project-level results.

Should not:

- Render UI.
- Own navigation.
- Depend on Activity.
- Contain view formatting.
- Become a monolithic service containing all features.

### Data Source

Use data sources only when they already improve separation.

They may encapsulate:

- MediaStore.
- Room.
- File APIs.
- Network or document providers.
- Encryption storage.

Do not add a data-source layer merely to wrap one method without benefit.

### Domain or Use Case

Add or use a domain/use-case layer only when:

- The project already uses it.
- Logic is shared across multiple presentation flows.
- A business operation combines multiple repositories.
- Isolation clearly improves testing and reasoning.

Do not create one class per trivial action.

## State Management

- Keep one clear source of truth.
- Avoid duplicating the same mutable state across Fragment, ViewModel, repository, and adapter.
- Model mutually exclusive states explicitly.
- Separate persistent state from one-time effects.
- Avoid event wrappers unless consistent with the project.
- Do not mix loading, error, and data flags in contradictory combinations.
- Keep state updates predictable.
- Avoid hidden mutation.
- Prefer immutable exposed state.
- Preserve state during configuration changes according to existing conventions.

## Result and Error Modeling

- Reuse existing result types.
- Do not introduce a new generic result hierarchy for one feature.
- Preserve meaningful failure categories.
- Avoid converting all failures into strings.
- Keep technical errors out of UI state unless required.
- Model partial success when batch operations can partially fail.
- Treat cancellation separately from failure.
- Do not swallow root causes.

## Module Boundaries

- Respect current module ownership.
- Do not move classes between modules without a clear reason.
- Avoid dependencies from lower-level modules to app/UI modules.
- Do not introduce circular module dependencies.
- Keep Android-specific code in Android modules.
- Keep pure logic in modules or packages that can remain framework-independent when consistent with the project.
- Avoid exposing internal implementation details across modules.

## Feature Boundaries

- Keep feature-specific logic close to the feature.
- Promote logic to shared code only after real reuse exists.
- Avoid global managers for feature-local concerns.
- Avoid a shared `Utils` package as a default destination.
- Do not make all models global.
- Keep naming aligned with user-facing domain concepts.

## Abstraction Rules

Create an abstraction only when at least one is true:

- Multiple implementations exist.
- A stable boundary improves testing.
- Platform details must be isolated.
- The code is reused meaningfully.
- The existing architecture requires the abstraction.

Avoid abstractions that:

- Merely rename a single call.
- Hide important behavior.
- Require many pass-through methods.
- Add generic type complexity without benefit.
- Exist only for hypothetical future use.
- Make tracing behavior harder.

## Refactoring Rules

Refactor only when:

- Required to implement the requested behavior safely.
- Needed to remove the root cause.
- Needed to make critical logic testable.
- Explicitly requested.

For refactors:

- Keep behavior unchanged unless specified.
- Separate mechanical changes from behavioral changes where practical.
- Avoid combining package moves, renames, formatting, and logic changes.
- Preserve public APIs where possible.
- Add regression coverage around risky behavior.
- State migration risk clearly.

## Dependency Injection

This project uses Hilt as its dependency injection framework.

- Continue using Hilt for existing and new dependencies.
- Follow the project's existing Hilt component, module, scope, qualifier, and binding patterns.
- Use constructor injection with `@Inject` whenever the dependency can be created that way.
- Use `@HiltViewModel` for ViewModels that receive injected dependencies.
- When modifying an existing ViewModel that does not yet use Hilt, migrate it to `@HiltViewModel` and constructor injection when this is safe, practical, and within the task scope.
- Do not preserve manual ViewModel construction merely because the existing file forgot to use Hilt.
- Update the corresponding Activity or Fragment to obtain the ViewModel through the existing Hilt-compatible delegate, such as `by viewModels()` or `by activityViewModels()`, when appropriate.
- Remove obsolete manual factories or constructor wiring only when they are no longer used after the migration.
- Do not perform a broad migration of unrelated ViewModels unless explicitly requested.
- Use Hilt modules only for dependencies that cannot use constructor injection, external types, interfaces, qualified implementations, or lifecycle-specific bindings.
- Reuse existing Hilt modules before creating new modules.
- Place bindings in the narrowest appropriate Hilt component.
- Preserve existing scopes such as `@Singleton`, `@ActivityRetainedScoped`, `@ViewModelScoped`, or feature-specific scopes.
- Do not replace Hilt with Koin, manual dependency injection, service locators, or global singletons.
- Do not manually construct classes whose dependencies are already provided by Hilt.
- Do not pass dependencies manually through Activities, Fragments, ViewModels, repositories, or use cases when Hilt should provide them.
- Do not create duplicate bindings for an existing dependency.
- Use qualifiers when multiple bindings share the same type.
- Avoid adding an interface only for dependency injection; use one only when it provides a real architectural or testing boundary.
- Keep Android lifecycle ownership correct.
- Do not change Hilt, Dagger, Kotlin, KSP, or kapt versions unless explicitly requested or required by the task.
- When adding a new dependency, inspect the existing Hilt graph and follow the nearest equivalent implementation.

## Naming and Ownership

- Name layers by responsibility, not by pattern fashion.
- Avoid vague names such as Manager, Helper, Processor, Handler, or Utils unless the project has a precise established meaning.
- Make ownership of mutable state clear.
- Make ownership of resources and coroutine scopes clear.
- Avoid classes that coordinate unrelated features.

## Architecture Smells

Report, but do not automatically rewrite, when you find:

- God ViewModel.
- God repository.
- Context in pure logic.
- UI logic in repository.
- File I/O in adapter.
- Navigation in data layer.
- Multiple sources of truth.
- Hidden global mutable state.
- Circular dependencies.
- Pass-through abstraction chains.
- Generic utility dumping grounds.
- Feature packages depending directly on unrelated feature internals.
- Business rules duplicated across screens.

## Migration Rules

Architectural migrations require explicit scope.

Before migration, define:

- Goal.
- Affected modules.
- Compatibility plan.
- Incremental steps.
- Rollback strategy.
- Test strategy.
- Expected behavior changes.
- Dependency impact.

Do not perform opportunistic migration inside a small bug fix.

## Architecture Review Checklist

- Existing architecture was identified.
- New dependencies follow current direction.
- UI remains separated from data access.
- Pure logic remains Android-independent where practical.
- State has one source of truth.
- New abstractions have a concrete reason.
- No unnecessary module or package movement occurred.
- Public APIs changed only when necessary.
- Refactoring scope is proportional to the task.
- Error and partial-success behavior are explicit.
