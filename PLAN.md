# PLAN

## Task: MVP Android image filter demo using MVVM

Status: DONE

### Requirements

- Create a small Android Kotlin app.
- Use MVVM.
- Load a bundled image from assets.
- Apply simple image filters without CameraX.
- Split editing controls into two sections: Filter and Adjust.
- Keep the first implementation small and API 24-compatible.

### Approach

- Scaffold one `app` module only.
- Use XML + ViewBinding, matching project instructions.
- Keep Activity as a thin view layer.
- Put UI state and filter selection in a ViewModel.
- Model controls as two categories:
  - Filter: one-tap preset filters.
  - Adjust: numeric image adjustments.
- Render Filter controls from a catalog so more filters can be added without changing Activity layout.
- Use pure Kotlin filter logic where practical.
- Decode and filter bitmaps off the main thread.
- Avoid camera, OpenGL, AGSL, Hilt, and extra architecture until needed.

### Checklist

- [x] Add minimal Gradle Android project files.
- [x] Add app manifest, resources, and XML layout.
- [x] Add MVVM Activity/ViewModel code.
- [x] Add Filter catalog and fixed Adjust CPU processing for asset bitmap.
- [x] Add a bundled asset image.
- [x] Add a small unit test for filter math.
- [x] Run available verification.

### Notes

- Local cache contains Android Gradle Plugin 8.13.2 and Kotlin Android plugin 2.1.10.
- No existing source code is present yet, so this is a new MVP scaffold.
- Filter expansion uses `FilterCatalog`; Activity renders Filter controls dynamically instead of hard-coding buttons.
- `gradle :app:testDebugUnitTest` initially could not run because `gradle` and `JAVA_HOME` were not in PATH.
- Verification used cached Gradle at `C:\Users\TuanAnh\.gradle\wrapper\dists\gradle-8.14.3-bin\cv11ve7ro1n3o1j4so8xd9n66\gradle-8.14.3\bin\gradle.bat` with `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`.
- `:app:testDebugUnitTest` passed.
- `:app:assembleDebug` passed.
- Static searches found no `GlobalScope`, `!!`, broad `catch (Exception)`, deleted `FilterPreset`, or old hard-coded filter button IDs under `app/src/main`.

## Task: Optimize image preview filters with an extensible GPU pipeline

Status: DONE

### Requirements

- Improve filter speed.
- Use a minimal filter set inspired by `wasabeef/android-gpuimage`, without locking the app to GPUImage internals.
- Keep the existing MVVM split.
- Keep Filter expandable and Adjust fixed.
- Continue using the bundled asset image; no CameraX.

### Approach

- Avoid direct GPUImage integration as the core because it is harder to extend cleanly.
- Build a small internal GPU shader pipeline after the CPU MVP, keeping GPUImage as a reference for filter names and shader behavior.
- Keep the ViewModel as UI state only.
- Keep Filter as extensible shader presets and Adjust as fixed parameters.
- Add category metadata before shader work so the UI can scale past a flat filter list.
- Keep unit-testable mapping logic separate from Android/GPUImage classes where practical.

### Checklist

- [x] Add the 10 starter filter categories from the agreed proposal.
- [x] Define minimal internal GPU filter API.
- [x] Add a GLSurfaceView/Renderer preview path.
- [x] Add a minimal shader-backed filter catalog.
- [x] Keep CPU path available only as fallback or remove after GPU path is stable.
- [x] Update or replace tests for the new catalog/mapping logic.
- [x] Run unit tests and assemble debug after the category update.

### Notes

- GPUImage README documents `jp.co.cyberagent.android:gpuimage:2.x.x` and OpenGL ES 2.0 support.
- Maven Central lists `2.1.0` as the latest current version.
- Direct GPUImage integration is paused because the library is convenient but not ideal as a long-term extensible core.
- Use GPUImage as reference only; save/capture are out of scope.
- Added category metadata and dynamic category/filter controls while keeping the current CPU preview path.
- Added catalog tests for category coverage and Popular reuse.
- `:app:testDebugUnitTest :app:assembleDebug` passed after the category update.
- Added `FilterPreviewView`, a small OpenGL ES 2.0 renderer that applies filter recipe and fixed adjust uniforms on GPU.
- Removed the CPU per-pixel preview renderer.
- Added shader parameter mapping tests.
- `:app:testDebugUnitTest :app:assembleDebug` passed after the GPU preview update.

## Task: Show original and filtered previews side by side

Status: DONE

### Requirements

- Put the original image and filtered result on the same row.
- Split the available width evenly.
- Keep the existing GPU preview path.

### Approach

- Change only `activity_main.xml`.
- Use a horizontal `LinearLayout` with two weighted preview columns.

### Checklist

- [x] Move original and result previews into one weighted row.
- [x] Run assemble debug.

### Notes

- Original and filtered previews now share one horizontal row with equal width.
- `:app:assembleDebug` passed after the layout update.

## Task: Switch UI text resources to English

Status: DONE

### Requirements

- Keep app UI text in English for now.
- Preserve existing resource IDs and app behavior.

### Approach

- Update only `app/src/main/res/values/strings.xml`.
- Run assemble debug to verify resource compilation.

### Checklist

- [x] Replace Vietnamese UI strings with English strings.
- [x] Run assemble debug.

### Notes

- Updated `app/src/main/res/values/strings.xml` to English text while preserving resource IDs.
- Confirmed no Vietnamese diacritics remain in `strings.xml`.
- `:app:assembleDebug` passed after the resource update.
