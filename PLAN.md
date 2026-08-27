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

Status: DONE

### Requirements

- Use a small bitmap for filter rail thumbnails instead of the full source bitmap.
- Coalesce filter preview renders while adjust values are changing quickly.
- Keep visible filter and adjust behavior unchanged.

### Approach

- Create a thumbnail bitmap alongside the loaded sample bitmap.
- Pass the thumbnail bitmap to filter cards only.
- Keep only the latest pending shader params per animation frame.

### Checklist

- [x] Add thumbnail bitmap to filter UI state.
- [x] Generate a small thumbnail after sample decode.
- [x] Use the thumbnail for filter cards.
- [x] Coalesce pending filter preview params.
- [x] Run unit tests and assemble debug.

### Notes

- Superseded by the module extraction request before source changes were made.
- Reopened after module extraction; continuing only the thumbnail and render coalescing work.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after this optimization.

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

## Task: Prefix filter library string resources

Status: DONE

### Requirements

- Prefix string resources owned by the `:filter` module to avoid host app collisions.
- Keep displayed English text unchanged.
- Preserve current app behavior.

### Approach

- Rename `:filter` string resource names to `gs_*`.
- Update `FilterCatalog`, `AdjustControl`, and sample layout references.

### Checklist

- [x] Prefix filter module string names.
- [x] Update Kotlin and XML resource references.
- [x] Run unit tests and assemble debug.

### Notes

- Renamed filter module strings from generic names to `gs_*`.
- Updated `FilterCatalog`, `AdjustControl`, and the sample layout content description to use the prefixed strings.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after the string prefix update.

## Task: Cache filtered rail thumbnails

Status: DONE

### Requirements

- Show filtered sample thumbnails in the filter rail.
- Cache generated thumbnails so returning to a category does not recompute them.
- Avoid Glide/GPUImage and new dependencies.

### Approach

- Add a small in-memory thumbnail cache in the `:filter` module.
- Render filter recipes against the existing small thumbnail bitmap.
- Load filtered thumbnails off the main thread in the sample app.

### Checklist

- [x] Add filtered thumbnail renderer/cache.
- [x] Use cached filtered thumbnails in filter cards.
- [x] Add focused unit coverage for thumbnail color logic.
- [x] Run unit tests and assemble debug.

### Notes

- Based on the `love-frame` Glide model idea, but kept as a lightweight library cache for this shader-based module.
- Cache keys use a stable source key so a previously selected image can reuse its filter rail thumbnails when selected again.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after this cache work.

## Task: Load filtered rail thumbnails with Glide

Status: DONE

### Requirements

- Use Glide to load and cache filtered sample thumbnails in the filter rail.
- Keep cache keys stable across switching images and switching back.
- Remove manual Activity coroutine/cache plumbing for thumbnail loading.

### Approach

- Add Glide to the sample app module.
- Register a small custom Glide `ModelLoader` through `AppGlideModule`.
- Keep the filter thumbnail render logic in the `:filter` library and let Glide own loading/caching.

### Checklist

- [x] Add Glide dependency.
- [x] Register filtered thumbnail loading through `AppGlideModule`.
- [x] Replace manual thumbnail cache usage with Glide model loading.
- [x] Keep stable source keys for reusable thumbnail cache entries.
- [x] Run unit tests and assemble debug.

### Notes

- This follows the `love-frame` approach more closely while avoiding GPUImage in this module.
- Added `GsFilterGlideModule` so Glide registers filtered thumbnail loading during app initialization.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after the Glide integration.

## Task: Move Glide thumbnail loader into filter module

Status: DONE

### Requirements

- Keep reusable filtered thumbnail Glide model/loader in `:filter`.
- Keep `AppGlideModule` in the app module as the host registration point.
- Preserve current filter rail behavior.

### Approach

- Move `FilterThumbnailModel` and `FilterThumbnailModelLoader` into `:filter`.
- Add Glide as a compile-time dependency for `:filter`.
- Update app imports and Glide registration.

### Checklist

- [x] Move thumbnail Glide model/loader into `:filter`.
- [x] Update app imports.
- [x] Run unit tests and assemble debug.

### Notes

- `AppGlideModule` stays in `:app`; reusable loader code moves to `:filter`.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after moving the loader.

## Task: Create reusable filter controls view

Status: DONE

### Requirements

- Move filter rail rendering to a reusable view in `:filter`.
- Use RecyclerView for the filter rail.
- Expose callbacks for close, filter tab, adjust tab, category selection, and filter selection.
- Keep stable source keys for Glide thumbnail cache reuse.

### Approach

- Add `FilterControlsView` in `:filter`.
- Keep app `AppGlideModule` as the Glide registration point.
- Replace app-side dynamic filter card creation with the reusable view callbacks.

### Checklist

- [x] Add `FilterControlsView` with RecyclerView-backed filter rail.
- [x] Add required filter module resources.
- [x] Wire app layout and callbacks to the new view.
- [x] Run unit tests and assemble debug.

### Notes

- The view owns filter/category UI rendering; the host app owns close behavior and ViewModel updates through callbacks.
- Added `FilterControlsView` in `:filter` with callbacks for close, filter/adjust tab selection, category selection, and filter selection.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after wiring the reusable view.

## Task: Make filter controls styling reusable

Status: DONE

### Requirements

- Allow host apps to customize `FilterControlsView` colors and drawables from XML.
- Support tab indicator styling without changing library Kotlin code.
- Let `FilterControlsView` render from a reusable filter pack instead of only the built-in catalog.
- Allow loading a filter pack JSON file from host app assets.
- Cancel pending view-owned JSON loads when the view detaches.
- Keep existing sample appearance by default.

### Approach

- Add `FilterControlsView` custom attributes in `:filter`.
- Read style values once during view construction.
- Replace hardcoded colors/drawables with resolved style values.
- Add a small `FilterPack` model plus JSON parser in `:filter`.
- Keep built-in filters as the default pack and let hosts override it.

### Checklist

- [x] Add custom style attributes.
- [x] Apply attrs in `FilterControlsView`.
- [x] Expose tab indicator configuration.
- [x] Add reusable filter pack and JSON parsing.
- [x] Make `FilterControlsView` render from the active pack.
- [x] Cancel pending JSON loads on detach.
- [x] Run unit tests and assemble debug.

### Notes

- Keep this minimal: style attrs only for the existing UI surface, no custom layout provider yet.
- Added a test-only `org.json` dependency because Android's local unit test jar does not implement `JSONObject`.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after the reusable catalog update.

## Task: Document reusable filter library usage

Status: DONE

### Requirements

- Add practical README details for the reusable `:filter` module.
- Cover setup, preview, controls, JSON filter packs, styling, and thumbnail caching.
- Keep the documentation concise enough to maintain.

### Checklist

- [x] Document module setup and Glide registration.
- [x] Document `FilterPreviewView` and `FilterControlsView` usage.
- [x] Document JSON catalog schema and style attributes.
- [x] Run a lightweight verification.

### Notes

- README now documents reusable module setup, JSON filter packs, style attrs, callbacks, lifecycle ownership, and thumbnail cache keys.

## Task: Restore visible Adjust tab

Status: DONE

### Requirements

- Make the `Adjust` tab clearly visible in the sample UI.
- Keep `FilterControlsView` reusable and callback-based.
- Preserve existing filter and adjust behavior.

### Checklist

- [x] Make the tab row reserve visible space for both tabs.
- [x] Enable tab indicator styling in the sample app.
- [x] Run unit tests and assemble debug.

### Notes

- The tab row now gives `Filter` and `Adjust` equal width before the close button.
- The sample app enables the library tab indicator and uses the app primary text color for unselected tab text.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after the fix.

## Task: Keep none action visually unselected

Status: DONE

### Requirements

- The leading none/original icon is only an action button.
- Do not show selected styling on the none/original icon.
- Keep tapping it resetting the active filter to original.

### Checklist

- [x] Remove selected visual state from the none/original icon.
- [x] Run unit tests and assemble debug.

### Notes

- The none/original icon now always uses normal styling while still resetting the active filter to original when tapped.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after the update.

## Task: Use RippleImageView for icon buttons

Status: DONE

### Requirements

- Convert icon-only buttons to `RippleImageView`.
- Cover filter close, filter none/original, and adjust reset.
- Keep existing click behavior and styling.

### Checklist

- [x] Replace programmatic filter icon buttons.
- [x] Replace sample adjust reset XML button.
- [x] Run unit tests and assemble debug.

### Notes

- `GsCore` `RippleImageView` extends `FrameLayout`, so icon buttons now use `iconRippleRes` and `paddingRipple` instead of `ImageView` APIs.
- `GsCore` is exposed as an `api` dependency from `:filter` because the sample app XML references `com.core.gscore.view.RippleImageView`.
- The adjust reset `RippleImageView` keeps its existing accessibility label.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after the update.
- Reopened to use the default `RippleImageView` icon padding instead of overriding `paddingRipple`.
- Removed custom `paddingRipple` assignments and the unused icon padding dimens.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after removing the custom padding.
- Reopened to keep icon padding configurable only when `FilterControlsView` declares an explicit padding attr.
- Added optional `gsFilterIconPadding`; omitted attr keeps `RippleImageView` default padding.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after the optional padding attr update.

## Task: Reduce filter category switch flicker

Status: DONE

### Requirements

- Switching filter category should feel stable and avoid visible flashing.
- Keep filter thumbnails Glide-backed.
- Preserve current category and filter selection behavior.

### Checklist

- [x] Disable unnecessary RecyclerView item animations for the filter rail.
- [x] Skip duplicate `FilterControlsView` state renders.
- [x] Run unit tests and assemble debug.

### Notes

- Category switching no longer runs RecyclerView default item animations.
- Duplicate host state feedback no longer submits the same filter rail list again.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after the flicker reduction.
- Removed redundant `removeBlink()` call because the category rail needs `itemAnimator = null` to stop add/remove animations, not only change animations.

## Task: Let filter none and close buttons wrap content

Status: DONE

### Requirements

- Filter none/original and close buttons should use wrap content sizing.
- Keep `RippleImageView` default measurement and padding behavior.

### Checklist

- [x] Change filter close button layout params to wrap content.
- [x] Change filter none/original button layout params to wrap content.
- [x] Run unit tests and assemble debug.

### Notes

- Close and none/original `RippleImageView` buttons now use `WRAP_CONTENT` for width and height.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after the wrap-content update.

## Task: Center wrap-content filter action layouts

Status: DONE

### Requirements

- Center dependent rows/layout params after changing filter close and none/original buttons to wrap content.
- Keep existing button behavior and default `RippleImageView` sizing.

### Checklist

- [x] Center the filter header close button layout params.
- [x] Center the none/original row and related layout params.
- [x] Run unit tests and assemble debug.

### Notes

- Close and none/original buttons now keep wrap-content sizing while their parent/dependent row layout params are centered vertically.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after the centering update.

## Task: Split tab background styling from category chips

Status: DONE

### Requirements

- Filter/Adjust tabs should have their own background attrs.
- Host should be able to disable Filter/Adjust tab backgrounds through an attr.
- Category chips should keep using chip background attrs.
- Preserve existing appearance when the new tab attrs are not set.

### Checklist

- [x] Add separate normal/selected tab background attrs.
- [x] Add an attr to disable tab backgrounds.
- [x] Apply the new attrs in tab rendering.
- [x] Document the attrs and run verification.

### Notes

- Added `gsFilterTabBackground`, `gsFilterSelectedTabBackground`, and `gsFilterUseTabBackground`.
- Tab backgrounds default to the existing chip backgrounds unless overridden.
- Setting `gsFilterUseTabBackground="false"` removes Filter/Adjust tab backgrounds while leaving category chip backgrounds intact.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after splitting tab background attrs.

## Task: Split tab text color styling from category chips

Status: DONE

### Requirements

- Filter/Adjust tab text colors should be configurable separately from category chips.
- Disabling tab backgrounds should not leave selected tab text white on a light background by default.
- Existing hosts should keep the same look when tab backgrounds are enabled.

### Checklist

- [x] Add separate normal/selected tab text color attrs.
- [x] Apply the new tab text colors in tab rendering.
- [x] Use a non-white selected tab fallback when tab backgrounds are disabled.
- [x] Document the attrs and run verification.

### Notes

- Added `gsFilterTabTextColor` and `gsFilterSelectedTabTextColor`.
- Tab text defaults to the existing global text attrs, but selected tab text falls back to `gsFilterSelectedColor` when tab backgrounds are disabled.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after splitting tab text color attrs.

## Task: Let adjust reset icon wrap content

Status: DONE

### Requirements

- The adjust reset `RippleImageView` should not be forced to a large fixed size.
- Keep existing reset behavior and default `RippleImageView` padding.

### Checklist

- [x] Change adjust reset button width/height to wrap content.
- [x] Remove the now-unused fixed reset size dimen.
- [x] Run unit tests and assemble debug.

### Notes

- The reset adjust `RippleImageView` was large because it was forced to `36dp x 36dp`.
- It now uses `wrap_content` so `RippleImageView` default sizing/padding controls its measured size.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after the reset icon size update.

## Task: Move adjust controls logic into filter module

Status: DONE

### Requirements

- Move adjust UI/control logic out of the sample app and into reusable `:filter` module code.
- Keep the app module as a host that only forwards adjust callbacks to the ViewModel and renders state back.
- Preserve the current adjust behavior: selected control, seekbar ranges, current reset, reset all, changed dots, and icon/text selection colors.
- Keep Filter and Adjust under the existing shared `FilterControlsView` tab header.

### Checklist

- [x] Add internal adjust controls implementation in `:filter`.
- [x] Move adjust panel resources needed by the reusable view into `:filter`.
- [x] Replace app-side adjust panel layout and Activity logic with `FilterControlsView` callbacks.
- [x] Document the reusable adjust callbacks and styling attrs.
- [x] Run unit tests and assemble debug.

### Notes

- Adjust rendering now lives inside `FilterControlsView`, not as a separate app layout, so it shares the existing Filter/Adjust tab row.
- The app now only wires `onAdjustmentChanged`, `onResetAllAdjustClick`, and `setAdjustments()`.
- Removed app-owned adjust reset icon, changed-dot drawable, strings, colors, and dimens that moved into `:filter`.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after moving adjust controls into the filter module.

## Task: Add category row top spacing

Status: DONE

### Requirements

- Move the filter category row down by 10dp.
- Keep other filter/adjust spacings unchanged.

### Checklist

- [x] Add a dedicated 10dp category top spacing dimen.
- [x] Apply it to the category row.
- [x] Run unit tests and assemble debug.

### Notes

- Added `gs_filter_category_top_spacing` and applied it only to the category row.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after the spacing update.
- Documented `gs_filter_category_top_spacing` as an overridable library dimen in README.
- Expanded README overridable dimensions table to include all filter module dimen resources.
- Updated README defaults to match the current `filter/src/main/res/values/dimens.xml` values.

## Task: Translate README to Vietnamese

Status: DONE

### Requirements

- Convert README prose to Vietnamese.
- Keep API names, resource names, and code snippets unchanged.
- Preserve the existing documented library usage.

### Checklist

- [x] Translate README content.
- [x] Run lightweight documentation verification.

### Notes

- This is documentation-only; no source behavior should change.
- Paused because the latest request switched to wiring a test JSON filter pack in `MainActivity`.
- Resumed after the JSON pack toggle implementation was verified.
- README prose is now Vietnamese while API names, resource names, and code snippets stay copy-pasteable.
- README now documents the sample app `JSON pack` switch and `filter_pack.json` test flow.
- `git diff --check` passed after the README update.

## Task: Wire MainActivity to test filter_pack.json

Status: DONE

### Requirements

- Add a test `filter_pack.json` asset.
- Configure `MainActivity` to load that pack through `FilterControlsView`.
- Keep ViewModel category/filter state aligned with the loaded pack.
- Add a top-right toggle to switch between the JSON test pack and the built-in catalog.

### Checklist

- [x] Add the sample JSON pack asset.
- [x] Wire `MainActivity` catalog load callbacks.
- [x] Update `FilterViewModel` to use the active pack for category refocus.
- [x] Add a top-right JSON pack toggle.
- [x] Run relevant verification.

### Notes

- Use the existing `FilterControlsView.loadCatalogFromAssets()` path instead of adding a new loader.
- `MainActivity` now loads `app/src/main/assets/filter_pack.json` when the top-right `JSON pack` switch is on.
- Turning the switch off restores `FilterCatalog.pack`.
- `FilterControlsView.setCatalog()` now invalidates pending asset loads so toggling off cannot be overwritten by a late JSON callback.
- `Get-Content app/src/main/assets/filter_pack.json | ConvertFrom-Json | Out-Null` passed.
- `git diff --check` passed.
- `gradle :filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed using the cached Gradle wrapper and Android Studio JBR after the sandboxed run was blocked by network permissions.

## Task: Add view-free filtered bitmap rendering

Status: DONE

### Requirements

- Provide logic to get a filtered `Bitmap` without using `FilterPreviewView` or any UI view.
- Keep filter recipe and user adjust values applied together.
- Reuse existing filter math where possible.

### Checklist

- [x] Add a public bitmap renderer API in `:filter`.
- [x] Keep thumbnail rendering on the same math path.
- [x] Add focused unit coverage.
- [x] Run relevant verification.

### Notes

- `FilterPreviewView` only renders to `GLSurfaceView`; it does not expose a bitmap output API.
- `FilterThumbnailRenderer` already has CPU pixel math, but currently only takes `FilterRecipe`.
- Added `FilterBitmapRenderer.getBitmap(source, recipe, adjustments)` for view-free bitmap output.
- `FilterThumbnailRenderer` now delegates to `FilterBitmapRenderer` so thumbnail and exported bitmap rendering share one math path.
- Added `FilterBitmapRendererTest` for default output, user adjustments, and invalid dimensions.
- `git diff --check` passed.
- `:filter:testDebugUnitTest :app:assembleDebug` passed after rerunning Gradle outside the sandbox due network/cache restrictions.

## Task: Expand non-LUT built-in filters

Status: DONE

### Requirements

- Add the remaining filter presets that can be represented without LUT.
- Keep filters as `FilterRecipe` + `Adjustments` data.
- Add missing categories needed for those presets.
- Preserve existing engine, preview, thumbnail, and adjust behavior.

### Checklist

- [x] Add Landscape, Night, Aesthetic, and Creative categories.
- [x] Add non-LUT filter recipes across existing categories.
- [x] Add filter/category string resources.
- [x] Add focused catalog coverage for category references.
- [x] Run relevant verification.

### Notes

- This should be data-only catalog expansion; no shader/LUT changes.
- Added Landscape, Night, Aesthetic, and Creative categories with recipe-only presets.
- `git diff --check` passed.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after rerunning Gradle outside the sandbox due network/cache restrictions.

## Task: Add sketch-style non-LUT filters

Status: DONE

### Requirements

- Add drawing-style filters that do not require LUT.
- Start with lightweight Sketch and Ink effects.
- Keep built-in catalog, JSON filter packs, preview, thumbnails, and bitmap export aligned.

### Checklist

- [x] Add a recipe effect type.
- [x] Render Sketch and Ink in the shared GPU shader path.
- [x] Render Sketch and Ink in the CPU bitmap path.
- [x] Add built-in Sketch and Ink presets/resources.
- [x] Move sketch-style filters to a single Art category.
- [x] Add the remaining Art presets.
- [x] Add lightweight effect tuning params.
- [x] Add focused tests and run verification.

### Notes

- Added `FilterEffect.Color`, `FilterEffect.Sketch`, and `FilterEffect.Ink`.
- JSON recipes can now use `effect: "sketch"` or `effect: "ink"`.
- Built-in catalog includes `Sketch` and `Ink` under Art.
- `Get-Content app/src/main/assets/filter_pack.json | ConvertFrom-Json | Out-Null` passed.
- `git diff --check` passed.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after rerunning Gradle outside the sandbox due network/cache restrictions.
- Reopened to put sketch-style filters in a single Art category.
- Added `Art` category and moved built-in sketch-style filters there only.
- Sample JSON pack now has `JSON Art` for `json_sketch`.
- `Get-Content app/src/main/assets/filter_pack.json | ConvertFrom-Json | Out-Null` passed after regrouping.
- `git diff --check` passed after regrouping.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after regrouping.
- Reopened to add the remaining Art presets: Pencil, Soft Sketch, Color Pencil, Fine Line, Manga, Charcoal, and Cross Hatch.
- Built-in Art now has the 8 requested samples: Pencil, Soft Sketch, Color Pencil, Fine Line, Ink, Manga, Charcoal, and Cross Hatch.
- Added reusable effect tuning fields: `effectStrength`, `effectThreshold`, and `effectTone`.
- `Get-Content app/src/main/assets/filter_pack.json | ConvertFrom-Json | Out-Null` passed.
- `git diff --check` passed.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after rerunning Gradle outside the sandbox due network/cache restrictions.

## Task: Optimize bitmap export with scaling and offscreen GPU rendering

Status: DONE

### Requirements

- Add output scaling for view-free bitmap rendering.
- Add an offscreen GPU render path that does not require `FilterPreviewView`.
- Keep API 24 compatibility.
- Keep existing preview behavior unchanged.

### Checklist

- [x] Add CPU renderer max-size option.
- [x] Add background export helper in the sample app.
- [x] Add offscreen GPU bitmap renderer in `:filter`.
- [x] Reuse shader constants between preview and offscreen renderer.
- [x] Add/update focused tests where local JVM can cover logic.
- [x] Run relevant verification.

### Notes

- Offscreen GPU rendering will use EGL pbuffer + `glReadPixels`; hosts can choose it only when they need faster full-image export.
- `FilterBitmapRenderer.getBitmap()` now accepts optional `maxWidth` and `maxHeight`, downscales only when needed, and keeps aspect ratio.
- Added `FilterGpuBitmapRenderer.getBitmap()` for EGL pbuffer rendering without `FilterPreviewView`.
- `FilterPreviewView` and `FilterGpuBitmapRenderer` now share `GlFilterProgram` shader/program binding logic.
- Added `FilterViewModel.renderFilteredBitmap(maxWidth, maxHeight, useGpu)` to show off-main export in the sample app.
- Updated README usage for CPU scaled output and GPU offscreen output.
- `git diff --check` passed.
- `:filter:testDebugUnitTest :app:assembleDebug` passed after rerunning Gradle outside the sandbox due network/cache restrictions.

## Task: Optimize filtered thumbnails cache and size

Status: DONE

### Requirements

- Make Glide thumbnail cache keys include the full filter recipe, including Art effect tuning params.
- Render thumbnail cache bitmaps at a small fixed max size.
- Preserve existing filter rail behavior.

### Checklist

- [x] Add a stable thumbnail cache key.
- [x] Cap thumbnail rendering size.
- [x] Add focused tests.
- [x] Run relevant verification.

### Notes

- Glide thumbnail cache now uses a stable recipe-based string key instead of relying on model object hashing.
- Cache keys include full `FilterRecipe`, including Art effect fields.
- Thumbnail rendering is capped to `128px` through `FilterThumbnailRenderer`.
- `git diff --check` passed.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after rerunning Gradle outside the sandbox due network/cache restrictions.

## Task: Preload category thumbnails and prefer GPU export

Status: DONE

### Requirements

- Preload filtered thumbnails for the selected category.
- Prefer GPU rendering for full bitmap export.
- Fall back to CPU rendering if GPU export fails.
- Preserve existing UI and callback behavior.

### Checklist

- [x] Add category thumbnail preloading.
- [x] Change bitmap export default to GPU-first.
- [x] Add focused tests where practical.
- [x] Run relevant verification.

### Notes

- `FilterControlsView` now preloads the selected category's filtered thumbnails through Glide using the existing stable `FilterThumbnailModel.cacheKey`.
- Preload requests are de-duped per visible category/source/filter recipe set.
- `FilterViewModel.renderFilteredBitmap()` now defaults to GPU rendering and falls back to CPU on GPU runtime failure while preserving coroutine cancellation.
- No new unit test was added because the new behavior is at the Android View/Glide/EGL boundary; existing JVM tests still cover the reusable renderer math and cache-key logic.
- `git diff --check` passed.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after rerunning Gradle outside the sandbox due network/cache restrictions.

## Task: Match filter thumbnails with main preview

Status: DONE

### Requirements

- Make filter rail thumbnails match the main filtered preview.
- Match the rendered filter state, not the thumbnail card layout/framing.
- Preserve Glide thumbnail loading and cache behavior.
- Keep the fix small and reusable inside the filter module.

### Checklist

- [x] Trace thumbnail render path versus preview render path.
- [x] Fix the shared rendering mismatch.
- [x] Add/update focused tests where practical.
- [x] Run relevant verification.

### Notes

- Thumbnail rendering now tries `FilterGpuBitmapRenderer` first so filter cards use the same shader program as `FilterPreviewView`.
- Thumbnail rendering still falls back to `FilterBitmapRenderer` if offscreen GPU rendering is unavailable.
- `FilterThumbnailModel` cache keys now include a render version so old CPU-rendered Glide thumbnails are not reused.
- Added a cache-key version unit test.
- `git diff --check` passed.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after rerunning Gradle outside the sandbox due network/cache restrictions.
- Reopened because thumbnail render still did not include active `Adjustments`, while the main preview does.
- Thumbnail render models now include active `Adjustments`, and Glide cache keys change when adjustments change.
- Thumbnail rendering now applies the filter before scaling down to thumbnail size so Sketch/Ink/Art effects stay closer to the main preview.
- The thumbnail card layout/framing was left unchanged.
- `git diff --check` passed.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after rerunning Gradle outside the sandbox due network/cache restrictions.
- Reopened because Art thumbnails still used the app's pre-scaled thumbnail source, while the main preview uses the full source bitmap.
- The sample app now passes the full source bitmap to `FilterControlsView`; the separate 256px thumbnail source was removed.
- `FilterThumbnailRenderer` keeps the scaled-first fast path for normal color filters, but renders Art/effect filters full-size first and scales afterward.
- Thumbnail cache version was bumped to `gpu-preview-v2`.
- Added coverage for the Art full-size-first renderer decision.
- `git diff --check` passed.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after rerunning Gradle outside the sandbox due network/cache restrictions.
- Reopened because rendering Art at full output size is slow and still differs from preview, which renders to the view size from the full source texture.
- Art/effect thumbnails now render directly at thumbnail output size while sampling from the full source texture.
- Normal color filters keep the faster scaled-source thumbnail path.
- Thumbnail cache version was bumped to `gpu-preview-v3`.
- `git diff --check` passed.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after rerunning Gradle outside the sandbox due network/cache restrictions.
- Reopened because Art thumbnails still needed render-size-based texel scaling instead of source-size sampling.
- `uTexelSize` now uses the actual render target size: preview uses the drawn image area on the GL surface, and offscreen thumbnails use the output framebuffer size.
- Glide thumbnail loading now passes the requested view size into the renderer, capped at `128px`, so thumbnails are not rendered larger and scaled again by the view.
- Thumbnail cache version was bumped to `gpu-preview-v5`.
- `git diff --check` passed.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after rerunning Gradle outside the sandbox due network/cache restrictions.
- Reopened because thumbnail display still cropped portrait images, making Art filters feel different from the main preview.
- The `FIT_CENTER` thumbnail display experiment was later reverted; thumbnail cards use center crop again.
- Thumbnail cache version was bumped to `gpu-preview-v6`.
- `git diff --check` passed.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after rerunning Gradle outside the sandbox due network/cache restrictions.
- Reopened because the thumbnail label still covered part of the rendered bitmap, so Glide requested/rendered a larger area than the user could see.
- The label-area layout experiment was later reverted; Glide load keys still include the bounded render size.
- Thumbnail cache version was bumped to `gpu-preview-v7`.
- `git diff --check` passed.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after rerunning Gradle outside the sandbox due network/cache restrictions.
- Reopened because preserving portrait ratio in the old `88dp` card made thumbnails too small to judge.
- The `132dp` height experiment was reverted; `gs_filter_thumbnail_height` remains `88dp` in resources and README.
- Reopened because the thumbnail mismatch is not a UI size issue; Art thumbnails need their own softer render tuning.
- `FilterThumbnailRenderer` now derives a thumbnail-only Art recipe that reduces weak/noisy edge detail while leaving preview/export recipes unchanged.
- Color filters are left unchanged by thumbnail recipe tuning.
- Thumbnail cache version was bumped to `gpu-preview-v8`.
- Added coverage for color-preserving and Ink-softening thumbnail recipe behavior.
- Art thumbnail render caps now use `256px` while color thumbnails stay at `128px`, so high-density screens do not upscale small Art bitmaps and inflate stroke width.
- Thumbnail cache version was bumped to `gpu-preview-v9`.
- Added coverage for separate color/Art thumbnail render caps.
- Art thumbnails now pass a smaller `texelScale` into the shared GL shader so Sobel sampling produces thinner thumbnail strokes without changing preview/export rendering.
- Thumbnail cache version was bumped to `gpu-preview-v10`.
- Added coverage for color/Art texel scale selection.
- Art thumbnail `texelScale` was tightened from `0.7` to `0.6`, and thumbnail cache version was bumped to `gpu-preview-v11`.
- `git diff --check` passed.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug` passed after rerunning Gradle outside the sandbox due network/cache restrictions.

## Task: Add filter intensity seekbar

Status: DONE

### Requirements

- Keep the main preview image updating when Adjust values change.
- Keep filter rail thumbnails as preset previews only.
- Do not reload or preload filter thumbnails during Adjust seekbar changes.
- Show an Intensity seekbar for every non-original filter.
- Apply intensity through the shared recipe/render mapping.

### Checklist

- [x] Stop `setAdjustments()` from rendering the filter rail.
- [x] Stop passing user adjustments into filter thumbnail models.
- [x] Add Intensity UI for non-original filters.
- [x] Apply filter intensity changes to preview/export.
- [x] Recalculate Intensity with a softer lower-half curve.
- [x] Skip test execution by request.

### Notes

- Current flow applies user `Adjustments` to every thumbnail model, which makes the rail reload while dragging Adjust.
- The first version exposes one Intensity seekbar instead of separate recipe internals.
- Added an Intensity seekbar for non-original filters.
- Filter thumbnails now keep preset recipes and ignore user Adjust changes.
- `FilterRecipe.intensity` now scales color preset values, B&W amount, and Art effect mix in the shared render mapping.
- Preview and export now use the selected recipe with any per-filter intensity override.
- Reopened because linear intensity changes too quickly below 50%.
- Intensity now uses `linear * linear`; slider 50 maps to effective 25.
- Added focused mapping coverage for Intensity 50 and JSON intensity clamp.
- `:filter:compileDebugKotlin :app:compileDebugKotlin :filter:compileDebugUnitTestKotlin` passed.
- `git diff --check` passed.
- `:filter:compileDebugKotlin :app:compileDebugKotlin` passed.
- Unit tests were not run by request.

## Task: Move Filter and Adjust view structure to XML

Status: DONE

### Requirements

- Move the static UI structure of Filter and Adjust controls into XML layout resources.
- Keep dynamic catalog/filter/adjust item binding in Kotlin.
- Keep existing XML styling attributes working from the host layout.
- Add XML attributes for the new filter Intensity row where useful.
- Keep behavior unchanged.

### Checklist

- [x] Add XML layout resources for Filter controls.
- [x] Add XML layout resources for Adjust controls and repeated items.
- [x] Update custom views to inflate and bind XML views.
- [x] Keep/apply Filter and Adjust styling attributes.
- [x] Make XML-bound child view lookups fail-soft.
- [x] Move filter item selected border to foreground above the thumbnail image.
- [x] Hide selected Adjust reset button until the active value changes.
- [x] Run lightweight verification.

### Notes

- `AdjustControlsView` already supports XML attributes; the sample layout just did not set them.
- User clarified that separate XML layout files are preferred for easier UI tuning.
- Added `gs_view_filter_controls.xml`, `gs_item_filter_category.xml`, `gs_item_filter_option.xml`, `gs_view_adjust_controls.xml`, and `gs_item_adjust_control.xml`.
- `FilterControlsView` and `AdjustControlsView` now inflate XML and bind state/listeners by id.
- Added XML attrs for filter Intensity tint/show behavior and configured Filter/Adjust colors in the sample `activity_main.xml`.
- The Adjust placeholder container is hidden with the Adjust tab so it does not keep empty spacing in Filter mode.
- README styling attrs now include the new Intensity XML attributes.
- Reopened to make XML-bound child view references nullable so missing customized ids do not crash the host app.
- `AdjustControlsView` and `FilterControlsView` now use nullable `findViewById` references and safe binding for XML child views.
- Reopened to move the filter item selected border to foreground so it draws above the thumbnail image.
- Added transparent foreground stroke drawables for normal and selected filter cards, configurable through XML attrs.
- Reopened to keep the selected Adjust reset button invisible until the active adjust value differs from default.
- The selected Adjust reset button now defaults to `invisible` in XML and toggles visible only when the active control value differs from default.
- `git diff --check` passed.
- `:filter:compileDebugKotlin :app:compileDebugKotlin` passed.
- Unit tests were not run.

## Task: Add configurable FilterControls tab indicator width

Status: DONE

### Requirements

- Let hosts choose the Filter/Adjust tab indicator width behavior.
- Support full tab width, minimum/wrap width, or label text width.
- Preserve the current full-width default.

### Checklist

- [x] Add a `FilterControlsView` XML attr for tab indicator width mode.
- [x] Apply the selected mode to both Filter and Adjust indicators.
- [x] Document the new attr.
- [x] Run lightweight verification.

### Notes

- Current XML indicator width is `match_parent`, so `full` must remain the default.
- Added `gsFilterTabIndicatorWidthMode` with `full`, `min`, and `text`.
- Added `gsFilterTabIndicatorMinWidth` for the `min` mode, defaulting to `gs_filter_chip_min_width`.
- `git diff --check` passed.
- `:filter:compileDebugKotlin :app:compileDebugKotlin` passed after rerunning Gradle with Android Studio JBR and approved network access for the wrapper distribution.

## Task: Sharpen filter thumbnails on high-density screens

Status: DONE

### Requirements

- Make filter rail thumbnails look less blurry at the currently visible size.
- Keep the existing thumbnail card layout.
- Avoid increasing source decode or adding dependencies.

### Checklist

- [x] Raise the filtered thumbnail render cap used by color filters.
- [x] Bump Glide thumbnail cache version so old low-res entries are not reused.
- [x] Update focused thumbnail tests.
- [x] Run lightweight verification.

### Notes

- Screenshot shows the current 78dp cards are upscaling color thumbnails capped at 128px on a high-density device.
- Color thumbnail render cap is now 256px, matching Art/effect thumbnails.
- Thumbnail cache version was bumped to `gpu-preview-v12`.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle with Android Studio JBR and approved network access for the wrapper distribution.

## Task: Add another data-only filter preset batch

Status: DONE

### Requirements

- Add new built-in filters using existing `FilterRecipe` and `Adjustments` data.
- Prioritize new samples for people and portrait photos.
- Keep the current filter engine, preview, thumbnail, and Adjust behavior unchanged.
- Avoid new dependencies or UI changes.

### Checklist

- [x] Add preset filter entries to the built-in catalog.
- [x] Add filter name string resources.
- [x] Run focused catalog verification.
- [x] Add people-prioritized preset entries to the built-in catalog.
- [x] Add people-prioritized filter name string resources.
- [x] Run focused catalog verification for the expanded batch.

### Notes

- Use the existing preset-data model; no LUT, shader, or renderer changes needed.
- Added 10 data-only presets: Clean Light, Soft Day, Rose Skin, Fresh Plate, Warm Table, Deep Teal, Fade Drama, Midnight City, Sunlit Forest, and Pearl Mono.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.
- Reopened to add another people-prioritized preset batch.
- Added 8 people-prioritized presets: Selfie Clear, Soft Portrait, Studio Skin, Golden Skin, Indoor Warm, Flash Soft, Low Light Skin, and Clean Face.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.
