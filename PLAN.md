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

## Task: Add the full fixed Adjust control set

Status: DONE

### Requirements

- Add these fixed adjust controls: Brightness, Exposure, Contrast, Highlights, Shadows, Saturation, Vibrance, Temperature, Tint, Sharpness, Clarity, Fade, Vignette, Grain.
- Keep UI text in English.
- Keep GPU preview rendering.
- Use per-control ranges, for example Sharpness is `0..100` while tone/color controls can be `-100..100`.
- Avoid hard-coding every slider block in XML.

### Approach

- Expand `Adjustments` with the full fixed set.
- Add metadata for each adjust control so Activity can build sliders dynamically.
- Map SeekBar progress through per-control min/max metadata.
- Pass the new adjust values to the OpenGL shader through `ShaderFilterParams`.
- Keep sharpness/clarity as shader placeholders if true convolution is too much for this step.

### Checklist

- [x] Add fixed adjust metadata and expanded state.
- [x] Replace hard-coded adjust sliders with dynamic controls.
- [x] Update shader uniforms and fragment shader logic.
- [x] Update tests for shader parameter mapping.
- [x] Run unit tests and assemble debug.

### Notes

- Added per-control ranges so signed controls use `-100..100` and intensity-only controls such as Sharpness use `0..100`.
- `:app:testDebugUnitTest :app:assembleDebug` passed after the full adjust update.

## Task: Bound Adjust extremes

Status: DONE

### Requirements

- Recheck min/max behavior for all 14 Adjust sliders.
- Keep displayed slider ranges unchanged.
- Prevent extreme values from making the preview look broken when dragged to min/max.

### Approach

- Keep UI clamping in `AdjustControl`.
- Soften shader parameter scaling in `ShaderFilterParams`.
- Add boundary tests for min/max shader values.

### Checklist

- [x] Bound shader parameter scaling for all 14 adjust values.
- [x] Add min/max regression coverage for all shader adjust values.
- [x] Run unit tests and assemble debug.

### Notes

- Contrast was the risky case: `-100` previously mapped to shader contrast `0.0`, flattening the preview. It now maps to `0.5`; `100` maps to `1.5`.
- `:app:testDebugUnitTest :app:assembleDebug` passed after bounding contrast extremes.
- Reopened to cover all 14 adjust values, not only Contrast.
- Tone/color/sharpness values now use safer shader strength caps while keeping the visible slider ranges unchanged.
- `:app:testDebugUnitTest :app:assembleDebug` passed after bounding all 14 adjust values.

## Task: Make filters expandable with recipe presets

Status: DONE

### Requirements

- Implement the lightweight expansion model for filters.
- Keep filters as data presets, not one class per filter.
- Let each filter define preset adjust values.
- Keep user Adjust sliders separate and additive.
- Keep current UI and GPU preview path.

### Approach

- Add preset `Adjustments` to `FilterRecipe`.
- Combine recipe adjustments with user adjustments in `ShaderFilterParams`.
- Add starter preset values to existing filters.
- Cover additive preset mapping with unit tests.

### Checklist

- [x] Add preset adjustments to filter recipes.
- [x] Combine preset and user adjustments before shader mapping.
- [x] Add tests for preset plus user adjust behavior.
- [x] Run unit tests and assemble debug.

### Notes

- Existing filters now carry small preset `Adjustments`; user Adjust values are added on top before shader scaling.
- `:app:testDebugUnitTest :app:assembleDebug` passed after recipe preset support.

## Task: Expand starter filter preset pack

Status: DONE

### Requirements

- Continue expanding filters using recipe preset data.
- Keep the existing 10 starter categories.
- Keep the GPU preview path and current UI.
- Avoid adding LUT or a new filter engine in this step.

### Approach

- Add small sets of new `FilterOption` presets to sparse categories.
- Add English string resources for new filter names.
- Add a catalog test for unique filter IDs.

### Checklist

- [x] Add starter preset filters.
- [x] Add filter name resources.
- [x] Add catalog uniqueness test.
- [x] Run unit tests and assemble debug.

### Notes

- Added 12 data-only filter presets across the existing 10 categories.
- `:app:testDebugUnitTest :app:assembleDebug` passed after expanding the preset pack.
- Reopened to add another small batch of filter samples using the same preset-data model.
- Added 8 more data-only presets: Cream, Glow, Kodak, Sunset, Mist, Cafe, Drama, Silver.
- `:app:testDebugUnitTest :app:assembleDebug` passed after the second preset batch.

## Task: Update Filter UI to category chips and thumbnail rail

Status: DONE

### Requirements

- Apply the provided UI direction to the Filter section only.
- Keep Adjust controls as the existing sliders.
- Keep the existing MVVM and GPU preview path.
- Keep filter expansion data-driven.
- Add another small preset batch only through catalog data.

### Approach

- Replace text-only Filter buttons with category chips and compact filter thumbnail cards.
- Reuse the bundled source bitmap for lightweight thumbnails.
- Keep the existing horizontal containers; avoid adding RecyclerView or a new UI layer for this small demo.
- Add filter presets as `FilterOption` data and English string resources.

### Checklist

- [x] Update Filter category controls to chip styling.
- [x] Update Filter option controls to thumbnail cards.
- [x] Add a small preset batch using the existing recipe model.
- [x] Run unit tests and assemble debug.

### Notes

- The attached UI reference is for Filter only; Adjust remains unchanged.
- Filter thumbnails reuse the already-loaded asset bitmap so the control rail stays cheap.
- Added Cinematic presets: Epic, Blockbuster, Arthouse; Noir also appears in Cinematic.
- `:app:testDebugUnitTest :app:assembleDebug` passed after the UI and preset update.

## Task: Apply category default filter immediately

Status: DONE

### Requirements

- Switching Filter category should immediately apply a new filter.
- Keep `Original` available without making category switches fall back to the unfiltered image.
- Preserve the existing MVVM and GPU preview path.

### Approach

- Add a catalog helper for the default filter of a category.
- Prefer the first non-`Original` filter when a category has alternatives.
- Use that helper from `FilterViewModel.selectCategory`.

### Checklist

- [x] Update category default filter selection.
- [x] Add catalog coverage for the default selection rule.
- [x] Run unit tests and assemble debug.

### Notes

- Root issue: `Original` belongs to categories such as Popular/Natural, so switching category could select the unfiltered preset and look like no filter was applied.
- Category switches now use the first non-`Original` filter when available, while app startup still defaults to `Original`.
- `:app:testDebugUnitTest :app:assembleDebug` passed after the fix.
- Superseded: category switching must not auto-apply a filter; it only changes the visible filter group.

## Task: Keep category switching filter-neutral

Status: DONE

### Requirements

- Changing a Filter category should only change the visible filter list.
- Do not auto-select or auto-apply a filter inside the selected category.
- Applying a filter still requires tapping that filter item.
- Show `Original` as a fixed leading none icon outside the category list.
- Keep `Original` out of category filter lists.

### Approach

- Update `FilterViewModel.selectCategory` to preserve the current selected filter.
- Remove the category-default helper and the test that encoded the wrong behavior.
- Add a fixed none icon button before the scrollable category chips.

### Checklist

- [x] Preserve selected filter when switching categories.
- [x] Remove wrong category default selection test/helper.
- [x] Move `Original` out of category filter lists.
- [x] Add fixed leading none icon action.
- [x] Run unit tests and assemble debug.

### Notes

- This corrects the previous interpretation of the category-switch behavior.
- `Original` is now a fixed leading none icon outside the scrollable category list.
- `Original` no longer belongs to any category filter list.
- `:app:testDebugUnitTest :app:assembleDebug` passed after the fix.

## Task: Refocus category on the active filter

Status: DONE

### Requirements

- Do not auto-apply a filter when switching categories.
- Re-selecting the visible category should show the currently selected filter again when it belongs to another category.
- Scroll the filter rail to the selected filter when it is present.
- Keep the fixed leading none icon for `Original`.

### Approach

- Add a catalog helper that finds the first category containing a filter.
- Use it only when the current category chip is tapped again.
- Give the filter scroll view an id and scroll to the selected card after render.

### Checklist

- [x] Add active-filter category lookup.
- [x] Re-select current category to refocus the active filter.
- [x] Scroll selected filter card into view when present.
- [x] Run unit tests and assemble debug.

### Notes

- This keeps category browsing filter-neutral while giving the user a quick way back to the active preset.
- Re-tapping the currently visible category now refocuses the first category containing the active filter when the active filter is outside the visible category.
- The filter rail scrolls to the selected card when that card is present.
- `:app:testDebugUnitTest :app:assembleDebug` passed after the refocus update.

## Task: Update Adjust UI to icon rail

Status: DONE

### Requirements

- Replace the vertical Adjust sliders with one active slider and a horizontal icon rail.
- Keep labels in English.
- Show a dot above each adjust icon when that value differs from default.
- Keep existing adjust ranges and shader mapping.
- Keep the fixed reset action.

### Approach

- Use the existing `AdjustControl` metadata as the source of truth.
- Store the selected adjust control in `MainActivity` UI state.
- Build adjust icon items dynamically from `AdjustControl.entries`.
- Reuse one `SeekBar` and value label for the active adjust control.

### Checklist

- [x] Replace Adjust layout with reset/slider/value plus icon rail.
- [x] Wire selected adjust control and value updates.
- [x] Show changed dots per adjust control.
- [x] Run unit tests and assemble debug.
- [x] Retune Adjust text/icon colors for the no-background layout.
- [x] Make the top reset icon reset only the selected adjust control.
- [x] Add a bottom Reset All button for all adjust controls.

### Notes

- This changes only how Adjust controls are presented; it does not change adjust math.
- Adjust now uses one active slider with reset, value, and horizontal icon controls.
- Changed adjust controls show a blue dot above their icon.
- `:app:testDebugUnitTest :app:assembleDebug` passed after the UI update.
- Reopened to retune Adjust text/icon colors after removing the dark panel background.
- Adjust colors now target the light screen background: primary text/icon dark, secondary labels gray, slider track light gray.
- `:app:testDebugUnitTest :app:assembleDebug` passed after the color retune.
- Reopened to split selected-control reset from reset-all behavior.
- The top reset icon now resets only the selected adjust control.
- Added a bottom `Reset All` button that resets all adjust controls.
- `:app:testDebugUnitTest :app:assembleDebug` passed after the reset split.

## Task: Split controls into Filter and Adjust tabs

Status: DONE

### Requirements

- Show the editor controls as two tabs: `Filter` and `Adjust`.
- Keep existing filter and adjust behavior unchanged.
- Hide the inactive control panel.

### Approach

- Reuse the existing filter and adjust views.
- Add a small Activity-local tab state that toggles panel visibility.

### Checklist

- [x] Add tab controls to the main layout.
- [x] Wrap existing filter and adjust controls in separate panels.
- [x] Wire tab selection in `MainActivity`.
- [x] Run unit tests and assemble debug.

### Notes

- The editor now shows `Filter` and `Adjust` tabs under the preview.
- Switching tabs only toggles panel visibility; selected filter and adjust values are preserved.
- `:app:testDebugUnitTest :app:assembleDebug` passed after the tab update.

## Task: Optimize filter preview rendering

Status: DONE

### Requirements

- Improve filter preview speed without changing visible filter behavior.
- Keep the current GPU shader and preset model.
- Avoid new dependencies.

### Approach

- Cache OpenGL attribute and uniform locations after shader program creation.
- Skip filter-state render requests when shader params have not changed.

### Checklist

- [x] Cache shader handles in `FilterPreviewView`.
- [x] Skip duplicate filter param renders.
- [x] Run unit tests and assemble debug.

### Notes

- `FilterPreviewView` now resolves shader attribute/uniform locations once per GL program instead of every frame.
- Duplicate filter params no longer enqueue another GL render request.
- `:app:testDebugUnitTest :app:assembleDebug` passed after the rendering optimization.

## Task: Optimize filter thumbnails and adjust dragging

Status: SUPERSEDED

### Requirements

- Use a small bitmap for filter rail thumbnails instead of the full source bitmap.
- Coalesce filter preview renders while adjust values are changing quickly.
- Keep visible filter and adjust behavior unchanged.

### Approach

- Create a thumbnail bitmap alongside the loaded sample bitmap.
- Pass the thumbnail bitmap to filter cards only.
- Keep only the latest pending shader params per animation frame.

### Checklist

- [ ] Add thumbnail bitmap to filter UI state.
- [ ] Generate a small thumbnail after sample decode.
- [ ] Use the thumbnail for filter cards.
- [ ] Coalesce pending filter preview params.
- [ ] Run unit tests and assemble debug.

### Notes

- Superseded by the module extraction request before source changes were made.

## Task: Extract filter code into library module

Status: DONE

### Requirements

- Create a `:filter` Android library module.
- Move reusable filter engine/model/view code into the library module.
- Keep the app module as the sample host UI.
- Preserve current app behavior.

### Approach

- Move `com.gsfilter.filter` sources into `:filter`.
- Move filter catalog and adjust metadata into the library when their resources are moved with them.
- Make `:app` depend on `:filter`.
- Keep app-only state, assets, and Activity/ViewModel in `:app`.

### Checklist

- [x] Add `:filter` module Gradle config.
- [x] Move filter sources into `:filter`.
- [x] Move filter-facing strings/icons/resources into `:filter`.
- [x] Update app imports/layout references for the library package.
- [x] Move relevant unit tests to `:filter`.
- [x] Run unit tests and assemble debug.

### Notes

- Added `:filter` as an Android library module.
- Moved filter models, catalog, adjust metadata, shader preview view, adjust icons, filter strings, and relevant unit tests into `:filter`.
- `:app` now depends on `:filter` and keeps only the sample Activity/ViewModel/state/assets/screen UI resources.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after the extraction.

## Task: Prefix filter library icon resources

Status: DONE

### Requirements

- Rename drawable icons owned by the `:filter` module to use the `ic_gs_` prefix.
- Update filter library code references.
- Preserve current app behavior.

### Approach

- Rename `ic_adjust_*` drawables in `:filter` to `ic_gs_adjust_*`.
- Update `AdjustControl` icon resource references.

### Checklist

- [x] Rename filter module drawable files.
- [x] Update `AdjustControl` drawable references.
- [x] Run unit tests and assemble debug.

### Notes

- Renamed `:filter` adjust drawables from `ic_adjust_*` to `ic_gs_adjust_*`.
- Updated `AdjustControl` to reference the prefixed drawable names.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after the rename.
