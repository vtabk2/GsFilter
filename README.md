# GsFilter

GsFilter là demo filter ảnh Android nhỏ, đồng thời là module thư viện filter có thể tái sử dụng.

Dự án gồm:

- `:filter`: module tái sử dụng cho model filter, preview OpenGL, control Filter/Beauty/Adjust, CPU/GPU bitmap render, batch render progress, và thumbnail qua Glide.
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
- Beauty controls nằm ở tab riêng, gồm Smoothing, Whitening, Blush, Lipstick, Under-eye, Teeth Whitening, Eye Shadow, Eyeliner, Eyebrow, Face Slimming và Eye Enlargement; tất cả dùng range `0..100`.
- Thumbnail rail của filter được load bằng Glide với cache key ổn định.
- LUT nội bộ dùng texture 33x33x33 sinh từ `FilterLut`, không cần ship file LUT ngoài.
- Render nhiều bitmap nên đi qua `FilterRenderer.renderBatch()` để xử lý lần lượt và nhận progress %.

Ảnh demo được tải từ `app/src/main/assets`. Nút `Next image` chuyển lần lượt qua các ảnh được hỗ trợ trong thư mục assets.

## Cài đặt thư viện

### Project khác qua JitPack

Thêm JitPack vào `settings.gradle.kts` của app:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Thêm GsFilter vào module app:

```kotlin
dependencies {
    implementation("com.github.vtabk2:GsFilter:1.0.1")
}
```

Nếu project đang dùng Groovy:

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

```groovy
dependencies {
    implementation 'com.github.vtabk2:GsFilter:1.0.1'
}
```

Nếu dùng `FilterControlsView` và thumbnail rail, host app cần thêm Glide KSP processor để `AppGlideModule` được generate:

```kotlin
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("com.github.bumptech.glide:glide:5.0.7")
    ksp("com.github.bumptech.glide:ksp:5.0.7")
}
```

Khai báo KSP plugin version khớp Kotlin, ví dụ Kotlin `2.2.21` dùng KSP `2.2.21-2.0.4`.

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

### Project local trong repo này

App mẫu dùng module local:

```kotlin
dependencies {
    implementation(project(":filter"))
    implementation("com.github.bumptech.glide:glide:5.0.7")
    ksp("com.github.bumptech.glide:ksp:5.0.7")
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

Nếu host không dùng `FilterPreviewView`, có thể render bitmap kết quả trực tiếp bằng API public trong module `:filter`:

```kotlin
val resultBitmap = FilterRenderer.getBitmap(
    source = sourceBitmap,
    recipe = selectedFilter.recipe,
    adjustments = adjustments,
    maxWidth = 2048,
    maxHeight = 2048,
)
```

`maxWidth`/`maxHeight` là tùy chọn, renderer chỉ downscale để giữ aspect ratio và không upscale ảnh nhỏ.

API này thử GPU offscreen trước và tự fallback CPU khi EGL không khả dụng. Với ảnh lớn, gọi nó ngoài main thread:

```kotlin
val resultBitmap = withContext(Dispatchers.Default) {
    FilterRenderer.getBitmap(
        source = sourceBitmap,
        recipe = selectedFilter.recipe,
        adjustments = adjustments,
        maxWidth = 2048,
        maxHeight = 2048,
    )
}
```

Khi cần render nhiều ảnh, dùng `renderBatch()` để module xử lý từng bitmap một lượt. Cách này tránh giữ nhiều bitmap kết quả cùng lúc và trả progress sau mỗi ảnh:

```kotlin
withContext(Dispatchers.Default) {
    FilterRenderer.renderBatch(
        sources = bitmaps,
        recipe = selectedFilter.recipe,
        adjustments = adjustments,
        maxWidth = 2048,
        maxHeight = 2048,
        onProgress = { progress ->
            updateProgress(progress.percent)
        },
    ) { _, bitmap ->
        saveBitmap(bitmap)
        bitmap.recycle()
    }
}
```

`FilterRenderProgress.percent` trả `0..100`; batch rỗng được xem là `100`. Callback chạy trên thread gọi `renderBatch()`, nên chỉ cập nhật UI trực tiếp sau khi chuyển về main thread. Nếu `saveBitmap()` ghi file blocking, host nên tự chuyển phần lưu sang `Dispatchers.IO`.

Host app vẫn chịu trách nhiệm decode URI, lưu file, lỗi từng ảnh, cancel job, và permission/MediaStore.

Trong app mẫu, `FilterViewModel.renderFilteredBitmap(maxWidth, maxHeight, useGpu)` đã bọc sẵn việc chạy background.

## Cách dùng controls

`FilterControlsView` tự render UI category/filter và các tab Beauty/Adjust. Host vẫn giữ app state, preview rendering, save/export, và navigation.

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
    app:gsFilterTabSpacing="20dp" />
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
- Trong tab Adjust, hàng icon control nằm trên seekbar/value hiện tại; nút reset control và Reset All dùng trạng thái disabled khi không có gì để reset.
- `gsFilterCompactTabs` kéo label/indicator của tab Filter và Adjust gần nhau hơn; `gsFilterTabSpacing` chỉnh khoảng cách giữa hai tab.
- `gsFilterCloseIcon` đổi icon của `gs_filter_close_button`; app mẫu đang dùng `ic_gs_tick`.
- `gsAdjustResetIcon` mặc định dùng selector có disabled/pressed state.

### Beauty

Beauty là tab chỉnh khuôn mặt và makeup riêng, tách khỏi `Adjust`. Người dùng chọn một control trên thanh icon ngang rồi kéo SeekBar chung ở dưới; tất cả control dùng range `0..100`.

Các control hiện có:

- `Smoothing`: làm mịn da, có bảo vệ cạnh để giữ chi tiết khuôn mặt.
- `Whitening`: nâng sáng và giảm nhẹ độ bão hòa vùng da, không phủ trắng toàn ảnh.
- `Blush`: thêm má hồng theo hai vùng má được phát hiện, có hỗ trợ mặt nghiêng.
- `Lipstick`: tô môi theo contour môi, giới hạn màu trong vùng môi.
- `Under-eye`: làm sáng vùng dưới mắt.
- `Teeth Whitening`: làm sáng vùng răng trong khu vực miệng.
- `Eye Shadow`: thêm màu mắt theo vùng mí và góc mặt.
- `Eyeliner`: thêm đường viền mắt theo vùng mắt.
- `Eyebrow`: tăng màu theo contour lông mày.
- `Face Slimming`: thu gọn khuôn mặt bằng warp giới hạn trong vùng mặt.
- `Eye Enlargement`: phóng to mắt bằng warp theo vùng mắt và góc nghiêng.

Beauty state được giữ trong `FilterRecipe`, render bằng GPU và có CPU fallback. Nút `Reset Beauty` đưa control đang chọn về giá trị của filter hiện tại; `Reset All Beauty` đưa toàn bộ Beauty về giá trị mặc định của filter. Khi đang ở `Original`, các giá trị mặc định đều là `0`.

Host app nhận thay đổi Beauty qua callback:

```kotlin
binding.filterControls.onBeautyChanged = { control, value ->
    when (control) {
        FilterControlsView.BeautyControl.Smoothing -> viewModel.setSkinSmoothing(value)
        FilterControlsView.BeautyControl.Whitening -> viewModel.setSkinWhitening(value)
        FilterControlsView.BeautyControl.Blush -> viewModel.setBlush(value)
        FilterControlsView.BeautyControl.Lipstick -> viewModel.setLipstick(value)
        FilterControlsView.BeautyControl.UnderEye -> viewModel.setUnderEye(value)
        FilterControlsView.BeautyControl.TeethWhitening -> viewModel.setTeethWhitening(value)
        FilterControlsView.BeautyControl.EyeShadow -> viewModel.setEyeShadow(value)
        FilterControlsView.BeautyControl.Eyeliner -> viewModel.setEyeliner(value)
        FilterControlsView.BeautyControl.Eyebrow -> viewModel.setEyebrow(value)
        FilterControlsView.BeautyControl.FaceSlimming -> viewModel.setFaceSlimming(value)
        FilterControlsView.BeautyControl.EyeEnlargement -> viewModel.setEyeEnlargement(value)
    }
}
binding.filterControls.onResetBeautyClick = {
    viewModel.resetBeauty()
}
```

Host có thể map từng `BeautyControl` vào state riêng như app mẫu.

## Mẫu filter tích hợp

Built-in `FilterCatalog` có 104 preset, không tính action `Original`. Một preset có thể xuất hiện ở nhiều category; `Popular` là nhóm shortcut cho các mẫu hay dùng.

| Category  | Preset hỗ trợ                                                                                                                                                                                                                       |
|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Popular   | Fresh, Warm, Tasty, B&W, Clear, Gold, Teal Orange, Cream, Glow, Sunset, Blockbuster, Soft, Beauty, Golden Hour, Travel, Neon, Clean Light, Selfie Clear, Fresh Plate, Clean Portrait, Daylight Fresh, Food Pop, Teal Cinema         |
| Portrait  | Portra, Skin, Peach, Cream, Glow, Soft, Blush, Rosy, Tan, Beauty, Soft Mono, Selfie Clear, Soft Portrait, Studio Skin, Golden Skin, Indoor Warm, Flash Soft, Low Light Skin, Pearl Mono, Clean Portrait, Soft Skin, Golden Portrait |
| Natural   | Fresh, Clear, Airy, Pure, Clean Light, Soft Day, Daylight Fresh                                                                                                                                                                     |
| Food      | Tasty, Crispy, Cafe, Warm Plate, Fresh Plate, Warm Table, Food Pop                                                                                                                                                                  |
| Landscape | Clear Day, Golden Hour, Winter, Forest, Ocean, Sky, Travel, Blue Hour, Sunlit Forest, Green Film                                                                                                                                    |
| Night     | Neon, City, Night, Blue Hour, Cyberpunk, Low Light Skin, Midnight City, Night Mood                                                                                                                                                  |
| Film      | Portra, Fuji, Gold, Kodak, Grain Film                                                                                                                                                                                               |
| Cinematic | Cinema, Teal Orange, Noir, Drama, Epic, Blockbuster, Arthouse, Bleach, Deep Teal, Fade Drama, Teal Cinema                                                                                                                           |
| Vintage   | Retro, Fade, Dust, Oldie, 90s, Retro Matte, Vintage Fade                                                                                                                                                                            |
| B&W       | B&W, Noir, Matte, High Contrast, Soft Mono, Pearl Mono                                                                                                                                                                              |
| Warm      | Warm, Sunset, Amber, Caramel, Cozy                                                                                                                                                                                                  |
| Cool      | Cool, Arctic, Mist, Blue Mist, Steel, Cyan Clean                                                                                                                                                                                    |
| Aesthetic | Beige, Latte, Pink, Dreamy, Minimal, Editorial Matte                                                                                                                                                                                |
| Creative  | Neon, Cyberpunk, Purple, Dream, Fantasy                                                                                                                                                                                             |
| Art       | Pencil, Soft Sketch, Color Pencil, Fine Line, Ink, Charcoal                                                                                                                                                                         |

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

Trong app mẫu, khi không có thay đổi filter, Beauty hoặc Adjust, đường export dùng bản sao pixel-identical của bitmap gốc thay vì chạy bitmap qua GPU shader. Để kiểm tra lúc bấm lưu, xem log tag `BitmapCompare` trong Logcat.

## Styling `FilterControlsView`

Các XML attributes hiện có:

| Attribute                        | Mục đích                                                                                                                             |
|----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| `gsFilterTextColor`              | Màu category/icon thường và fallback cho text tab                                                                                    |
| `gsFilterSelectedTextColor`      | Màu category/icon đang chọn và fallback cho selected tab khi tab background bật                                                      |
| `gsFilterTabTextColor`           | Màu text tab Filter/Adjust bình thường; mặc định theo `gsFilterTextColor`                                                            |
| `gsFilterSelectedTabTextColor`   | Màu text tab Filter/Adjust đang chọn; mặc định theo `gsFilterSelectedTextColor`, hoặc `gsFilterSelectedColor` khi tab background tắt |
| `gsFilterSelectedColor`          | Accent fallback cho trạng thái selected                                                                                              |
| `gsFilterChipBackground`         | Background category chip bình thường                                                                                                 |
| `gsFilterSelectedChipBackground` | Background category chip đang chọn                                                                                                   |
| `gsFilterTabBackground`          | Background tab Filter/Adjust bình thường; mặc định theo `gsFilterChipBackground`                                                     |
| `gsFilterSelectedTabBackground`  | Background tab Filter/Adjust đang chọn; mặc định theo `gsFilterSelectedChipBackground`                                               |
| `gsFilterUseTabBackground`       | Set `false` để render tab Filter/Adjust không có background                                                                          |
| `gsFilterCardBackground`         | Background filter thumbnail card bình thường                                                                                         |
| `gsFilterSelectedCardBackground` | Background filter thumbnail card đang chọn                                                                                           |
| `gsFilterCardForeground`         | Foreground viền thumbnail card bình thường, nằm trên ảnh                                                                             |
| `gsFilterSelectedCardForeground` | Foreground viền thumbnail card đang chọn, nằm trên ảnh                                                                               |
| `gsFilterLabelBackground`        | Background nhãn thumbnail                                                                                                            |
| `gsFilterLabelTextColor`         | Màu text nhãn thumbnail                                                                                                              |
| `gsFilterCloseIcon`              | Drawable icon đóng/xác nhận; app mẫu có `ic_gs_tick`                                                                                 |
| `gsFilterNoneIcon`               | Drawable icon Original/none                                                                                                          |
| `gsFilterIconPadding`            | Override padding icon close/original nếu cần; bỏ trống thì dùng default của `RippleImageView`                                        |
| `gsFilterShowTabIndicator`       | Hiển thị indicator dưới tab Filter/Adjust đang chọn                                                                                  |
| `gsFilterCompactTabs`            | Kéo label/indicator của tab Filter và Adjust gần nhau hơn trong khi vùng bấm vẫn rộng                                                |
| `gsFilterTabSpacing`             | Khoảng cách giữa tab Filter và Adjust khi cần chỉnh gần/xa nhau                                                                      |
| `gsFilterTabIndicatorColor`      | Màu tab indicator                                                                                                                    |
| `gsFilterTabIndicatorHeight`     | Chiều cao tab indicator                                                                                                              |
| `gsFilterTabIndicatorWidthMode`  | Kích thước ngang indicator: `full`, `min`, hoặc `text`                                                                               |
| `gsFilterTabIndicatorMinWidth`   | Chiều rộng indicator khi mode là `min`; mặc định theo `gs_filter_chip_min_width`                                                     |
| `gsFilterShowIntensity`          | Bật/tắt slider Intensity cho filter có recipe                                                                                        |
| `gsFilterIntensityTextColor`     | Màu label và value của slider Intensity                                                                                              |
| `gsFilterIntensityProgressColor` | Màu progress và thumb của slider Intensity                                                                                           |
| `gsFilterIntensityTrackColor`    | Màu track của slider Intensity                                                                                                       |
| `gsAdjustTextColor`              | Màu text giá trị adjust                                                                                                              |
| `gsAdjustSecondaryTextColor`     | Màu icon/label adjust item chưa chọn                                                                                                 |
| `gsAdjustSelectedColor`          | Màu adjust item đang chọn, seekbar progress/thumb, và changed-dot                                                                    |
| `gsAdjustTrackColor`             | Màu track của adjust seekbar                                                                                                         |
| `gsAdjustResetIcon`              | Drawable icon reset control adjust hiện tại                                                                                          |
| `gsAdjustResetIconPadding`       | Override padding icon reset nếu cần; bỏ trống thì dùng default của `RippleImageView`                                                 |
| `gsAdjustResetAllText`           | Text nút reset tất cả                                                                                                                |

Các dimension có thể override:

| Dimension                           | Default | Mục đích                                                           |
|-------------------------------------|---------|--------------------------------------------------------------------|
| `gs_filter_item_spacing`            | `8dp`   | Khoảng cách mặc định giữa các filter control                       |
| `gs_filter_chip_min_width`          | `80dp`  | Chiều rộng tối thiểu của category chip và fallback indicator `min` |
| `gs_filter_chip_min_height`         | `32dp`  | Chiều cao tối thiểu của tab và category chip                       |
| `gs_filter_chip_horizontal_padding` | `18dp`  | Padding ngang của tab và category chip                             |
| `gs_filter_chip_vertical_padding`   | `4dp`   | Padding dọc của tab và category chip                               |
| `gs_filter_category_top_spacing`    | `20dp`  | Khoảng cách trên giữa tab row Filter/Adjust và category row        |
| `gs_filter_tab_indicator_height`    | `2dp`   | Chiều cao indicator của tab Filter/Adjust                          |
| `gs_filter_thumbnail_width`         | `78dp`  | Chiều rộng item thumbnail filter                                   |
| `gs_filter_thumbnail_height`        | `102dp` | Chiều cao item thumbnail filter                                    |
| `gs_filter_thumbnail_label_height`  | `24dp`  | Chiều cao dải label thumbnail filter                               |
| `gs_filter_thumbnail_label_padding` | `4dp`   | Padding ngang label thumbnail filter                               |
| `gs_adjust_icon_size`               | `24dp`  | Kích thước icon adjust control                                     |
| `gs_adjust_dot_size`                | `5dp`   | Kích thước dot báo value đã đổi                                    |
| `gs_adjust_item_gap`                | `4dp`   | Khoảng cách giữa dot, icon, và label của adjust item               |
| `gs_adjust_item_width`              | `78dp`  | Chiều rộng adjust item trong horizontal rail                       |
| `gs_adjust_value_width`             | `36dp`  | Chiều rộng text giá trị adjust hiện tại                            |

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
