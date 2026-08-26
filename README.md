# GsFilter

GsFilter is a small Android image filter demo that is being shaped into a reusable filter library.

The project is split into:

- `:filter`: reusable filter models, OpenGL preview, filter controls, JSON filter pack parsing, and Glide thumbnail loading.
- `:app`: sample host app using MVVM, an image from `assets`, and the reusable `:filter` module.

Current scope:

- Min SDK 24.
- Kotlin + XML views.
- No camera flow.
- GPU preview through `FilterPreviewView`.
- Filter presets are data recipes.
- Adjust controls are fixed.
- Filter rail thumbnails are loaded through Glide with stable cache keys.

## Add the library module

For this local project, the sample app uses:

```kotlin
dependencies {
    implementation(project(":filter"))
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
}
```

The filter rail needs the host app to register the library thumbnail loader in an `AppGlideModule`:

```java
@GlideModule
public final class GsFilterGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(
            @NonNull Context context,
            @NonNull Glide glide,
            @NonNull Registry registry
    ) {
        registry.append(
                FilterThumbnailModel.class,
                Bitmap.class,
                new FilterThumbnailModelLoader.Factory()
        );
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
```

## Preview usage

Add the preview view in XML:

```xml
<com.gsfilter.filter.FilterPreviewView
    android:id="@+id/filterPreview"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Then bind the bitmap and active filter state:

```kotlin
binding.filterPreview.setSourceBitmap(sourceBitmap)
binding.filterPreview.setFilterState(selectedFilter.recipe, adjustments)
```

`setFilterState()` skips duplicate params and coalesces fast adjust changes to the next animation frame.

## Controls usage

`FilterControlsView` owns the filter/category UI rendering. The host still owns app state, preview rendering, save/export, and navigation.

```xml
<com.gsfilter.filter.FilterControlsView
    android:id="@+id/filterControls"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:gsFilterShowTabIndicator="true"
    app:gsFilterTabIndicatorColor="@color/your_selected_color"
    app:gsFilterCatalogAsset="filters/filter_pack.json" />
```

Wire callbacks from the host:

```kotlin
binding.filterControls.onCloseClick = { finish() }
binding.filterControls.onControlTabSelected = { tab ->
    selectedControlTab = tab
    renderControlTabs()
}
binding.filterControls.onCategorySelected = { category -> viewModel.selectCategory(category) }
binding.filterControls.onFilterSelected = { filter -> viewModel.selectFilter(filter) }
binding.filterControls.onAdjustmentChanged = { control, value -> viewModel.setAdjustment(control, value) }
binding.filterControls.onResetAllAdjustClick = { viewModel.resetAdjustments() }
binding.filterControls.onCatalogLoaded = { pack ->
    // If the host keeps MVVM state, store this same pack there too.
    // The current selected category/filter should usually be reset to pack.defaultCategory/defaultFilter.
}
binding.filterControls.onCatalogLoadFailed = { error ->
    // Show a host-owned error state or keep the built-in filter pack.
}
```

Render current state back into the view:

```kotlin
binding.filterControls.setState(
    selectedCategory = state.selectedCategory,
    selectedFilter = state.selectedFilter,
    thumbnailBitmap = state.filterThumbnailBitmap,
    thumbnailKey = FilterSourceKey.asset("sample.jpg"),
)
binding.filterControls.setAdjustments(state.adjustments)
```

Notes:

- `Original` is a fixed leading none-icon action.
- Changing category only changes the visible filter list.
- A filter is applied only after tapping a filter item.
- Re-tapping the visible category can refocus the category containing the active filter.
- The Adjust tab is rendered by the same `FilterControlsView`; the host receives adjust callbacks and sends current `Adjustments` back with `setAdjustments()`.

## Stable thumbnail cache keys

The filter rail uses Glide to render and cache filtered thumbnails from `FilterThumbnailModel`.

Pass a small thumbnail bitmap and a stable source key to `setState()`:

```kotlin
FilterSourceKey.asset("sample.jpg")
FilterSourceKey.file(path, length, lastModifiedMillis)
FilterSourceKey.uri(uri, width, height, lastModifiedMillis)
```

Use the same key when the same source image is selected again. That lets Glide reuse cached filter thumbnails instead of recomputing the rail.

## JSON filter packs

`FilterControlsView` can load a JSON pack from host app assets:

```xml
app:gsFilterCatalogAsset="filters/filter_pack.json"
```

Or load/set it from code:

```kotlin
val pack = FilterPackJson.parse(json)
binding.filterControls.setCatalog(pack)
```

If the host keeps category/filter state in a `ViewModel`, keep the same `FilterPack` there too. The view can render a JSON pack by itself, but the preview and selected filter state still belong to the host.

Example JSON:

```json
{
  "defaultCategoryId": "cinematic",
  "categories": [
    { "id": "cinematic", "name": "Cinematic" },
    { "id": "portrait", "name": "Portrait" }
  ],
  "filters": [
    {
      "id": "teal_orange",
      "name": "Teal Orange",
      "categoryIds": ["cinematic"],
      "recipe": {
        "redShift": 12,
        "greenShift": 4,
        "blueShift": 8,
        "adjustments": {
          "exposure": -4,
          "contrast": 22,
          "highlights": -16,
          "shadows": -10,
          "saturation": -6,
          "temperature": 8,
          "tint": -8,
          "clarity": 12,
          "vignette": 20
        }
      }
    }
  ]
}
```

Supported recipe fields:

- `isMonochrome`: boolean.
- `redShift`, `greenShift`, `blueShift`: clamped to `-100..100`.
- `adjustments`: `brightness`, `exposure`, `contrast`, `highlights`, `shadows`, `saturation`, `vibrance`, `temperature`, `tint`, `sharpness`, `clarity`, `fade`, `vignette`, `grain`.

Adjust ranges:

- Signed controls: `-100..100`.
- Intensity controls: `sharpness`, `fade`, `vignette`, `grain` use `0..100`.

JSON packs should not include `Original`; the view already renders it as the fixed none action.

## Styling `FilterControlsView`

Available XML attributes:

| Attribute | Purpose |
| --- | --- |
| `gsFilterTextColor` | Normal category/icon color and tab text fallback |
| `gsFilterSelectedTextColor` | Selected category/icon color and selected tab fallback when tab backgrounds are enabled |
| `gsFilterTabTextColor` | Normal Filter/Adjust tab text color; defaults to `gsFilterTextColor` |
| `gsFilterSelectedTabTextColor` | Selected Filter/Adjust tab text color; defaults to `gsFilterSelectedTextColor`, or `gsFilterSelectedColor` when tab backgrounds are disabled |
| `gsFilterSelectedColor` | Default selected accent fallback |
| `gsFilterChipBackground` | Normal category chip background |
| `gsFilterSelectedChipBackground` | Selected category chip background |
| `gsFilterTabBackground` | Normal Filter/Adjust tab background; defaults to `gsFilterChipBackground` |
| `gsFilterSelectedTabBackground` | Selected Filter/Adjust tab background; defaults to `gsFilterSelectedChipBackground` |
| `gsFilterUseTabBackground` | Set `false` to render Filter/Adjust tabs without a background |
| `gsFilterCardBackground` | Normal filter thumbnail card background |
| `gsFilterSelectedCardBackground` | Selected filter thumbnail card background |
| `gsFilterLabelBackground` | Thumbnail label background |
| `gsFilterLabelTextColor` | Thumbnail label text color |
| `gsFilterCloseIcon` | Close icon drawable |
| `gsFilterNoneIcon` | Original/none icon drawable |
| `gsFilterIconPadding` | Optional close/original icon padding override; omitted uses `RippleImageView` default |
| `gsFilterShowTabIndicator` | Show indicator below selected Filter/Adjust tab |
| `gsFilterTabIndicatorColor` | Tab indicator color |
| `gsFilterTabIndicatorHeight` | Tab indicator height |
| `gsFilterCatalogAsset` | Optional asset path for a JSON filter pack |
| `gsAdjustTextColor` | Adjust value text, reset icon thumb fallback |
| `gsAdjustSecondaryTextColor` | Unselected adjust item icon/label color |
| `gsAdjustSelectedColor` | Selected adjust item, seekbar progress, and changed-dot accent |
| `gsAdjustTrackColor` | Adjust seekbar track color |
| `gsAdjustResetIcon` | Adjust current-control reset icon drawable |
| `gsAdjustResetIconPadding` | Optional reset icon padding override; omitted uses `RippleImageView` default |
| `gsAdjustResetAllText` | Reset-all button text |

For bigger visual changes, prefer replacing drawable resources through these attrs before adding a new custom layout API.

## Extending filters

The intended extension path is data first:

1. Add or load more `FilterOption` recipes.
2. Group them with `FilterCategory`.
3. Keep user `Adjustments` separate; recipe adjustments and user adjustments are combined for rendering.
4. Add shader/LUT support later only when recipe fields cannot express the desired look.

This keeps the filter library easier to expand than one class per filter or a hard dependency on GPUImage internals.

## Verification

Current project check:

```powershell
gradle :filter:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug
```
