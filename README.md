# GsFilter

GsFilter là demo filter ảnh Android nhỏ, đồng thời là module thư viện filter có thể tái sử dụng.

Dự án gồm:

- `:filter`: module tái sử dụng cho model filter, preview OpenGL, control Filter/Adjust, parse JSON filter pack, CPU/GPU bitmap render, và thumbnail qua Glide.
- `:app`: app mẫu dùng MVVM, ảnh từ `assets`, và module `:filter`.

Phạm vi hiện tại:

- Min SDK 24.
- Compile SDK 36; app mẫu target SDK 36.
- Gradle wrapper 9.0.0, Android Gradle Plugin 8.13.2, Kotlin 2.1.10, Java 17.
- Kotlin + XML views.
- Không có camera flow.
- Preview GPU qua `FilterPreviewView`.
- Filter preset là data recipe, gồm color/effect/LUT.
- Adjust controls là bộ cố định.
- Thumbnail rail của filter được load bằng Glide với cache key ổn định.
- LUT nội bộ dùng texture 33x33x33 sinh từ `FilterLut`, không cần ship file LUT ngoài.

## Thêm module thư viện

Với project local này, app mẫu dùng:

```kotlin
dependencies {
    implementation(project(":filter"))
    implementation("com.github.bumptech.glide:glide:5.0.7")
    annotationProcessor("com.github.bumptech.glide:compiler:5.0.7")
}
```

Filter rail cần host app đăng ký thumbnail loader của thư viện trong `AppGlideModule`:

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

## Cách dùng preview

Thêm preview view trong XML:

```xml
<com.gsfilter.filter.view.FilterPreviewView
    android:id="@+id/filterPreview"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Sau đó bind bitmap và trạng thái filter hiện tại:

```kotlin
binding.filterPreview.setSourceBitmap(sourceBitmap)
binding.filterPreview.setFilterState(selectedFilter.recipe, adjustments)
```

`setFilterState()` bỏ qua params trùng nhau và gom các thay đổi adjust nhanh vào frame kế tiếp.

## Render bitmap không cần view

Nếu host không dùng `FilterPreviewView`, có thể render bitmap kết quả trực tiếp bằng API trong module `:filter`:

```kotlin
val resultBitmap = FilterBitmapRenderer.getBitmap(
    source = sourceBitmap,
    recipe = selectedFilter.recipe,
    adjustments = adjustments,
    maxWidth = 2048,
    maxHeight = 2048,
)
```

`maxWidth`/`maxHeight` là tùy chọn, renderer chỉ downscale để giữ aspect ratio và không upscale ảnh nhỏ.

API CPU này không phụ thuộc UI view hay `GLSurfaceView`. Với ảnh lớn, gọi nó ngoài main thread:

```kotlin
val resultBitmap = withContext(Dispatchers.Default) {
    FilterBitmapRenderer.getBitmap(
        source = sourceBitmap,
        recipe = selectedFilter.recipe,
        adjustments = adjustments,
        maxWidth = 2048,
        maxHeight = 2048,
    )
}
```

Nếu cần export nhanh hơn cho ảnh lớn, có thể dùng GPU offscreen renderer. API này tạo EGL pbuffer nội bộ, không cần gắn `FilterPreviewView` lên UI:

```kotlin
val resultBitmap = withContext(Dispatchers.Default) {
    FilterGpuBitmapRenderer.getBitmap(
        source = sourceBitmap,
        recipe = selectedFilter.recipe,
        adjustments = adjustments,
        maxWidth = 2048,
        maxHeight = 2048,
    )
}
```

Trong app mẫu, `FilterViewModel.renderFilteredBitmap(maxWidth, maxHeight, useGpu)` đã bọc sẵn việc chạy background.

## Cách dùng controls

`FilterControlsView` tự render UI category/filter và tab Adjust. Host vẫn giữ app state, preview rendering, save/export, và navigation.

```xml
<com.gsfilter.filter.view.FilterControlsView
    android:id="@+id/filterControls"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:gsFilterCloseIcon="@drawable/ic_gs_tick"
    app:gsFilterCompactTabs="true"
    app:gsFilterShowTabIndicator="true"
    app:gsFilterTabIndicatorColor="@color/your_selected_color"
    app:gsFilterTabIndicatorWidthMode="text"
    app:gsFilterTabSpacing="20dp"
    app:gsFilterCatalogAsset="filters/filter_pack.json" />
```

Wire callback từ host:

```kotlin
binding.filterControls.onCloseClick = { finish() }
binding.filterControls.onControlTabSelected = { tab ->
    selectedControlTab = tab
    renderControlTabs()
}
binding.filterControls.onCategorySelected = { category -> viewModel.selectCategory(category) }
binding.filterControls.onFilterSelected = { filter -> viewModel.selectFilter(filter) }
binding.filterControls.onFilterIntensityChanged = { value -> viewModel.setFilterIntensity(value) }
binding.filterControls.onAdjustmentChanged = { control, value -> viewModel.setAdjustment(control, value) }
binding.filterControls.onResetAllAdjustClick = { viewModel.resetAdjustments() }
binding.filterControls.onCatalogLoaded = { pack ->
    // Nếu host giữ state theo MVVM, lưu cùng FilterPack này vào ViewModel.
    // Thường nên reset category/filter hiện tại về pack.defaultCategory/defaultFilter.
}
binding.filterControls.onCatalogLoadFailed = { error ->
    // Hiển thị lỗi do host quản lý hoặc giữ built-in filter pack.
}
```

Render state hiện tại ngược lại vào view:

```kotlin
binding.filterControls.setState(
    selectedCategory = state.selectedCategory,
    selectedFilter = state.selectedFilter,
    thumbnailBitmap = state.sourceBitmap,
    thumbnailKey = state.filterThumbnailKey,
    selectedRecipe = state.selectedRecipe,
)
binding.filterControls.setAdjustments(state.adjustments)
```

Nếu host không có override intensity theo filter, có thể dùng overload `setState()` không truyền `selectedRecipe`.

Ghi chú:

- `Original` là action cố định ở đầu, dùng none icon.
- Đổi category chỉ đổi danh sách filter đang hiển thị.
- Filter chỉ được áp dụng sau khi người dùng bấm vào từng filter item.
- Bấm lại category đang hiển thị có thể đưa UI về category chứa filter đang chọn.
- Tab Adjust được render trong cùng `FilterControlsView`; host nhận callback adjust và gửi `Adjustments` hiện tại lại bằng `setAdjustments()`.
- `gsFilterCompactTabs` kéo label/indicator của tab Filter và Adjust gần nhau hơn; `gsFilterTabSpacing` chỉnh khoảng cách giữa hai tab.
- `gsFilterCloseIcon` đổi icon của `gs_filter_close_button`; app mẫu đang dùng `ic_gs_tick`.

## Test JSON pack trong app mẫu

App mẫu có `app/src/main/assets/filter_pack.json` để test flow mở rộng filter bằng JSON.

Switch `JSON pack` ở góc phải trên cùng đang bật mặc định:

- Bật: `MainActivity` gọi `binding.filterControls.loadCatalogFromAssets("filter_pack.json")`.
- Tắt: app quay lại built-in `FilterCatalog.pack`.

`MainActivity` cũng gửi `FilterPack` đã load vào `FilterViewModel`, vì ViewModel cần dùng đúng catalog hiện tại khi xử lý category/filter state.

## Mẫu filter tích hợp

Built-in `FilterCatalog` có 104 preset, không tính action `Original`. Một preset có thể xuất hiện ở nhiều category; `Popular` là nhóm shortcut cho các mẫu hay dùng. Host app có thể thay thế hoặc mở rộng danh sách này bằng JSON pack.

| Category | Preset hỗ trợ |
| --- | --- |
| Popular | Fresh, Warm, Tasty, B&W, Clear, Gold, Teal Orange, Cream, Glow, Sunset, Blockbuster, Soft, Beauty, Golden Hour, Travel, Neon, Clean Light, Selfie Clear, Fresh Plate, Clean Portrait, Daylight Fresh, Food Pop, Teal Cinema |
| Portrait | Portra, Skin, Peach, Cream, Glow, Soft, Blush, Rosy, Tan, Beauty, Soft Mono, Selfie Clear, Soft Portrait, Studio Skin, Golden Skin, Indoor Warm, Flash Soft, Low Light Skin, Pearl Mono, Clean Portrait, Soft Skin, Golden Portrait |
| Natural | Fresh, Clear, Airy, Pure, Clean Light, Soft Day, Daylight Fresh |
| Food | Tasty, Crispy, Cafe, Warm Plate, Fresh Plate, Warm Table, Food Pop |
| Landscape | Clear Day, Golden Hour, Winter, Forest, Ocean, Sky, Travel, Blue Hour, Sunlit Forest, Green Film |
| Night | Neon, City, Night, Blue Hour, Cyberpunk, Low Light Skin, Midnight City, Night Mood |
| Film | Portra, Fuji, Gold, Kodak, Grain Film |
| Cinematic | Cinema, Teal Orange, Noir, Drama, Epic, Blockbuster, Arthouse, Bleach, Deep Teal, Fade Drama, Teal Cinema |
| Vintage | Retro, Fade, Dust, Oldie, 90s, Retro Matte, Vintage Fade |
| B&W | B&W, Noir, Matte, High Contrast, Soft Mono, Pearl Mono |
| Warm | Warm, Sunset, Amber, Caramel, Cozy |
| Cool | Cool, Arctic, Mist, Blue Mist, Steel, Cyan Clean |
| Aesthetic | Beige, Latte, Pink, Dreamy, Minimal, Editorial Matte |
| Creative | Neon, Cyberpunk, Purple, Dream, Fantasy |
| Art | Pencil, Soft Sketch, Color Pencil, Fine Line, Ink, Charcoal, Cross Hatch |

## Khóa cache thumbnail ổn định

Filter rail dùng Glide để render và cache thumbnail đã apply filter từ `FilterThumbnailModel`.

Truyền source bitmap và source key ổn định vào `setState()`:

```kotlin
FilterSourceKey.asset("sample.jpg")
FilterSourceKey.file(path, length, lastModifiedMillis)
FilterSourceKey.uri(uri, width, height, lastModifiedMillis)
```

Dùng lại đúng key khi cùng một ảnh source được chọn lại. Glide có thể lấy lại thumbnail filter từ cache thay vì render lại toàn bộ rail.

Thumbnail đưa vào Glide cache được bound theo request, tối đa `256px` cho cả filter màu và filter Art/effect. Cache key gồm source key, kích thước source, filter recipe, adjustments, render version, và kích thước thumbnail bound.

## JSON filter packs

`FilterControlsView` có thể load JSON pack từ assets của host app:

```xml
app:gsFilterCatalogAsset="filters/filter_pack.json"
```

Hoặc load/set bằng code:

```kotlin
val pack = FilterPackJson.parse(json)
binding.filterControls.setCatalog(pack)
```

Nếu host giữ category/filter state trong `ViewModel`, hãy lưu cùng `FilterPack` đó ở ViewModel. View có thể tự render JSON pack, nhưng preview và selected filter state vẫn thuộc host.

Ví dụ JSON:

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
        "effect": "color",
        "lut": "teal_cinema",
        "lutStrength": 80,
        "intensity": 100,
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

Các field recipe đang hỗ trợ:

- `effect`: `color`, `sketch`, `ink`, `pencil`, `color_pencil`, `charcoal`, hoặc `cross_hatch`; mặc định là `color`.
- `effectStrength`, `effectThreshold`, `effectTone`: số `0..100` để tinh chỉnh effect nét vẽ.
- `lut`: `none`, `clean_portrait`, `soft_skin`, `golden_portrait`, `daylight_fresh`, `food_pop`, `green_film`, `teal_cinema`, `night_mood`, `vintage_fade`, hoặc `editorial_matte`; mặc định là `none`.
- `lutStrength`: số `0..100`, mặc định `100`.
- `intensity`: số `0..100`, mặc định `100`.
- `isMonochrome`: boolean.
- `redShift`, `greenShift`, `blueShift`: được clamp trong `-100..100`.
- `adjustments`: `brightness`, `exposure`, `contrast`, `highlights`, `shadows`, `saturation`, `vibrance`, `temperature`, `tint`, `sharpness`, `clarity`, `fade`, `vignette`, `grain`.

Range của adjust:

- Control dạng signed: `-100..100`.
- Control dạng intensity: `sharpness`, `fade`, `vignette`, `grain` dùng `0..100`.

JSON pack không nên khai báo `Original`; view đã tự render nó thành none action cố định.

## Styling `FilterControlsView`

Các XML attributes hiện có:

| Attribute | Mục đích |
| --- | --- |
| `gsFilterTextColor` | Màu category/icon thường và fallback cho text tab |
| `gsFilterSelectedTextColor` | Màu category/icon đang chọn và fallback cho selected tab khi tab background bật |
| `gsFilterTabTextColor` | Màu text tab Filter/Adjust bình thường; mặc định theo `gsFilterTextColor` |
| `gsFilterSelectedTabTextColor` | Màu text tab Filter/Adjust đang chọn; mặc định theo `gsFilterSelectedTextColor`, hoặc `gsFilterSelectedColor` khi tab background tắt |
| `gsFilterSelectedColor` | Accent fallback cho trạng thái selected |
| `gsFilterChipBackground` | Background category chip bình thường |
| `gsFilterSelectedChipBackground` | Background category chip đang chọn |
| `gsFilterTabBackground` | Background tab Filter/Adjust bình thường; mặc định theo `gsFilterChipBackground` |
| `gsFilterSelectedTabBackground` | Background tab Filter/Adjust đang chọn; mặc định theo `gsFilterSelectedChipBackground` |
| `gsFilterUseTabBackground` | Set `false` để render tab Filter/Adjust không có background |
| `gsFilterCardBackground` | Background filter thumbnail card bình thường |
| `gsFilterSelectedCardBackground` | Background filter thumbnail card đang chọn |
| `gsFilterCardForeground` | Foreground viền thumbnail card bình thường, nằm trên ảnh |
| `gsFilterSelectedCardForeground` | Foreground viền thumbnail card đang chọn, nằm trên ảnh |
| `gsFilterLabelBackground` | Background nhãn thumbnail |
| `gsFilterLabelTextColor` | Màu text nhãn thumbnail |
| `gsFilterCloseIcon` | Drawable icon đóng/xác nhận; app mẫu có `ic_gs_tick` |
| `gsFilterNoneIcon` | Drawable icon Original/none |
| `gsFilterIconPadding` | Override padding icon close/original nếu cần; bỏ trống thì dùng default của `RippleImageView` |
| `gsFilterShowTabIndicator` | Hiển thị indicator dưới tab Filter/Adjust đang chọn |
| `gsFilterCompactTabs` | Kéo label/indicator của tab Filter và Adjust gần nhau hơn trong khi vùng bấm vẫn rộng |
| `gsFilterTabSpacing` | Khoảng cách giữa tab Filter và Adjust khi cần chỉnh gần/xa nhau |
| `gsFilterTabIndicatorColor` | Màu tab indicator |
| `gsFilterTabIndicatorHeight` | Chiều cao tab indicator |
| `gsFilterTabIndicatorWidthMode` | Kích thước ngang indicator: `full`, `min`, hoặc `text` |
| `gsFilterTabIndicatorMinWidth` | Chiều rộng indicator khi mode là `min`; mặc định theo `gs_filter_chip_min_width` |
| `gsFilterShowIntensity` | Bật/tắt slider Intensity cho filter có recipe |
| `gsFilterIntensityTextColor` | Màu label và value của slider Intensity |
| `gsFilterIntensityProgressColor` | Màu progress của slider Intensity |
| `gsFilterIntensityTrackColor` | Màu track của slider Intensity |
| `gsFilterCatalogAsset` | Asset path tùy chọn cho JSON filter pack |
| `gsAdjustTextColor` | Màu text giá trị adjust và fallback thumb icon reset |
| `gsAdjustSecondaryTextColor` | Màu icon/label adjust item chưa chọn |
| `gsAdjustSelectedColor` | Màu adjust item đang chọn, seekbar progress, và changed-dot |
| `gsAdjustTrackColor` | Màu track của adjust seekbar |
| `gsAdjustResetIcon` | Drawable icon reset control adjust hiện tại |
| `gsAdjustResetIconPadding` | Override padding icon reset nếu cần; bỏ trống thì dùng default của `RippleImageView` |
| `gsAdjustResetAllText` | Text nút reset tất cả |

Các dimension có thể override:

| Dimension | Default | Mục đích |
| --- | --- | --- |
| `gs_filter_item_spacing` | `8dp` | Khoảng cách mặc định giữa các filter control |
| `gs_filter_chip_min_width` | `80dp` | Chiều rộng tối thiểu của category chip và fallback indicator `min` |
| `gs_filter_chip_min_height` | `32dp` | Chiều cao tối thiểu của tab và category chip |
| `gs_filter_chip_horizontal_padding` | `18dp` | Padding ngang của tab và category chip |
| `gs_filter_chip_vertical_padding` | `4dp` | Padding dọc của tab và category chip |
| `gs_filter_category_top_spacing` | `20dp` | Khoảng cách trên giữa tab row Filter/Adjust và category row |
| `gs_filter_tab_indicator_height` | `2dp` | Chiều cao indicator của tab Filter/Adjust |
| `gs_filter_thumbnail_width` | `78dp` | Chiều rộng item thumbnail filter |
| `gs_filter_thumbnail_height` | `102dp` | Chiều cao item thumbnail filter |
| `gs_filter_thumbnail_label_height` | `24dp` | Chiều cao dải label thumbnail filter |
| `gs_filter_thumbnail_label_padding` | `4dp` | Padding ngang label thumbnail filter |
| `gs_adjust_button_min_height` | `48dp` | Chiều cao tối thiểu nút reset tất cả |
| `gs_adjust_icon_size` | `24dp` | Kích thước icon adjust control |
| `gs_adjust_dot_size` | `5dp` | Kích thước dot báo value đã đổi |
| `gs_adjust_item_gap` | `3dp` | Khoảng cách giữa dot, icon, và label của adjust item |
| `gs_adjust_item_width` | `78dp` | Chiều rộng adjust item trong horizontal rail |
| `gs_adjust_value_width` | `36dp` | Chiều rộng text giá trị adjust hiện tại |

Host app có thể override dimension của thư viện bằng cách khai báo cùng resource name trong `values/dimens.xml` của app:

```xml
<dimen name="gs_filter_category_top_spacing">20dp</dimen>
```

Với thay đổi visual lớn hơn, ưu tiên thay drawable qua các attr ở trên trước khi thêm custom layout API mới.

## Mở rộng filter

Hướng mở rộng nên đi theo data trước:

1. Thêm hoặc load thêm `FilterOption` recipes.
2. Group chúng bằng `FilterCategory`.
3. Giữ user `Adjustments` tách riêng; recipe adjustments và user adjustments được cộng lại khi render.
4. Dùng `lut`/`lutStrength` khi recipe thường chưa đủ; chỉ thêm shader mới khi cả recipe và LUT hiện có không diễn tả được look mong muốn.

Cách này giúp thư viện filter dễ mở rộng hơn so với mỗi filter một class riêng hoặc phụ thuộc cứng vào GPUImage internals.
