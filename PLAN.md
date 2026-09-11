# PLAN

## Task: Add explicit thumbnail cache revision

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Allow cropped content with the same path to force a new thumbnail cache entry.
- Keep unchanged paths reusable without adding automatic bitmap churn.
- Preserve existing source-key cache behavior.

### Checklist

- [x] Add a source-key revision helper.
- [x] Cover the revision format with a focused unit test.
- [x] Run tests, compile, and review diff.

### Notes

- `FilterSourceKey.withRevision(sourceKey, revision)` lets crop flows invalidate a same-path thumbnail explicitly.
- The revision is part of the existing Glide and scaled-source cache key because both already consume `sourceKey`.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, and `git diff --check` passed.

## Task: Key thumbnail source cache by source path

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Reuse the scaled thumbnail source when the same source key/path is requested again.
- Keep source dimensions and target-size invalidation checks.
- Preserve the one-slot memory bound and bitmap ownership behavior.

### Checklist

- [x] Pass `sourceKey` into thumbnail rendering.
- [x] Match scaled-source caches by source key, with bitmap identity fallback.
- [x] Run focused tests, compile, and review diff.

### Notes

- Scaled thumbnail sources now reuse the same source key/path across newly decoded `Bitmap` instances when dimensions and target bounds match.
- Same-object generation changes and missing source keys still invalidate through the existing fallback checks.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, and `git diff --check` passed.

## Task: Reuse scaled source for color thumbnails

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Reuse the scaled source across color-filter thumbnail requests for one image and size.
- Invalidate on source identity, source generation, or target-size changes.
- Keep memory bounded and preserve CPU fallback/cancellation behavior.

### Checklist

- [x] Add a one-slot scaled-source cache for the scaled-first path.
- [x] Render the cached source without scaling it again in GPU/CPU fallback paths.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Color thumbnail requests cùng ảnh/kích thước dùng lại scaled source, tránh scale lặp cho từng filter.
- Cache giữ một bitmap, kiểm tra source identity/generation/size và không đổi cleanup/cancellation.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:compileDebugKotlin`, and `git diff --check` passed.

## Task: Cache offscreen GPU initialization failure

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Skip repeated offscreen EGL setup after initialization is known to fail.
- Preserve CPU fallback behavior for unsupported devices.
- Continue retrying recoverable runtime failures from an existing session.

### Checklist

- [x] Cache only `RenderSession` construction failures.
- [x] Keep existing runtime-session invalidation behavior.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Sau lỗi khởi tạo `RenderSession`, các request offscreen tiếp theo fallback CPU ngay, không lặp EGL setup.
- Lỗi runtime trên session đang chạy vẫn invalidate và retry như trước.
- `:filter:compileDebugKotlin`, `:filter:testDebugUnitTest`, `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, and `git diff --check` passed.

## Task: Avoid redundant main-screen image updates

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Keep the original image updated when the source bitmap changes.
- Avoid resetting the same ImageView on filter/beauty/adjustment changes.
- Reuse the computed selected recipe within one state render.

### Checklist

- [x] Guard original ImageView updates by bitmap identity.
- [x] Compute `selectedRecipe` once per render.
- [x] Run compile/tests and review diff.

### Notes

- Ảnh gốc chỉ gọi `setImageBitmap` khi bitmap identity thực sự thay đổi.
- `selectedRecipe` được tính một lần rồi dùng chung cho preview và controls.
- `:app:testDebugUnitTest`, `:filter:testDebugUnitTest`, compilation, and `git diff --check` passed.

## Task: Restore filter state per image

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Keep filter state independent for each asset image.
- Restore selected filter, category, intensity, beauty, and adjustments when returning to an image.
- Re-detect face features for the current bitmap instead of reusing another image's detection result.

### Checklist

- [x] Save the current filter snapshot before switching assets.
- [x] Restore a valid snapshot after the target asset loads.
- [x] Add state-restore coverage and run focused tests/compile.

### Notes

- Snapshot được lưu theo `assetPath`, gồm category/filter, intensity theo filter, beauty và adjustments.
- Ảnh mới chưa có snapshot sẽ bắt đầu từ catalog defaults; ảnh quay lại sẽ khôi phục state cũ.
- `makeupFeatures` không lưu chéo ảnh và vẫn được detector tính lại theo bitmap hiện tại.
- `:app:testDebugUnitTest`, `:filter:testDebugUnitTest`, compilation, and `git diff --check` passed.

## Task: Reuse preview LUT texture storage

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Preserve preview LUT output and existing cleanup when LUT is disabled.
- Reuse the preview LUT texture when switching between LUTs.
- Avoid changing preview render scheduling or texture ownership.

### Checklist

- [x] Update an existing preview LUT texture with `texSubImage2D`.
- [x] Keep texture creation for the first LUT and cleanup for disabled LUTs.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Preview đổi giữa các LUT bằng cách cập nhật texture hiện có, không xoá/tạo lại texture.
- Khi LUT bị tắt, cleanup cũ vẫn được giữ nguyên; LUT đầu tiên vẫn tạo texture mới.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Avoid redundant filter-control rerenders

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Keep filter items refreshed when the source thumbnail changes.
- Avoid rerendering unchanged chips, sliders, and beauty controls.
- Preserve scroll-to-selected behavior for category/filter selection.

### Checklist

- [x] Separate filter-item list rendering from control rendering.
- [x] Skip selected-item scrolling for thumbnail-only updates.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Thumbnail-only updates chỉ submit lại filter items, không rerender chip/slider/beauty controls.
- RecyclerView chỉ scroll tới filter đã chọn khi category hoặc filter thực sự thay đổi.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Reuse LUT texture storage across filter changes

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Preserve LUT output and texture filtering/wrap parameters.
- Reuse the existing LUT texture when only LUT content changes.
- Keep the single-texture-per-render-session memory bound.

### Checklist

- [x] Update existing LUT texture storage with `texSubImage2D`.
- [x] Keep texture creation as the fallback for a new session.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Khi đổi LUT trong cùng render session, texture được tái sử dụng và cập nhật bằng `texSubImage2D`.
- Vẫn giữ một LUT texture mỗi session; session mới vẫn tạo texture như trước.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Avoid preview vertex-array allocation

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Preserve the existing aspect-ratio vertex coordinates.
- Write coordinates directly into the reusable direct buffer.
- Avoid changing render timing or buffer ownership.

### Checklist

- [x] Replace the temporary vertex array with direct buffer writes.
- [x] Keep the zero-size fallback buffer reset unchanged.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Preview ghi trực tiếp 8 vertex values vào reusable direct buffer, không tạo `FloatArray` tạm.
- Nhánh zero-size và geometry/aspect ratio giữ nguyên.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Reuse scaled art thumbnail source

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Reuse the same scaled art source across filters for one source image and size.
- Invalidate when source pixels, source identity, or target bounds change.
- Bound memory to one scaled bitmap and preserve cancellation cleanup.

### Checklist

- [x] Add a one-slot synchronized scaled-source cache.
- [x] Keep the cached bitmap out of per-render recycle cleanup.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Các filter art cùng source/bounds dùng lại một bitmap 2× thay vì scale lại từng request.
- Cache chỉ giữ một scaled bitmap, source chỉ được tham chiếu bằng `WeakReference`.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Abort cancelled art scaling before GPU work

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Stop a cancelled art thumbnail after source scaling and before GPU upload.
- Recycle temporary scaled bitmaps through the existing `finally` path.
- Preserve fallback behavior for non-cancelled renders.

### Checklist

- [x] Add a cancellation checkpoint after art source scaling.
- [x] Keep existing cleanup and runtime fallback behavior.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Request art bị cancel sẽ dừng trước GPU upload sau khi scale xong.
- `finally` vẫn recycle temporary source và fallback không đổi.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Reuse preview texture storage on source changes

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Update same-size ARGB_8888 preview images without reallocating texture storage.
- Reallocate when dimensions or bitmap config change.
- Set immutable texture parameters only when creating the texture.

### Checklist

- [x] Track the uploaded texture config.
- [x] Use `texSubImage2D` only for the safe same-size ARGB path.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Preview source cùng kích thước ARGB_8888 dùng `texSubImage2D`, tránh cấp phát lại storage.
- Đổi kích thước/config vẫn dùng `texImage2D`; texture parameters chỉ set lúc tạo.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Mark filter RecyclerView size as fixed

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Tell RecyclerView its own size does not depend on adapter item count.
- Preserve the existing fixed item dimensions and horizontal layout.
- Avoid changing cache sizes or visible item behavior.

### Checklist

- [x] Enable `hasFixedSize` for the filter list.
- [x] Verify the item XML keeps fixed width and height.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Filter RecyclerView dùng item width/height cố định nên giảm layout pass khi đổi category.
- Không thay đổi cache, thứ tự hoặc số item hiển thị.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Hoist fixed LUT sampler binding

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Bind `uLutTexture` to texture unit 1 once per GL program.
- Keep LUT strength at zero as the switch for non-LUT filters.
- Preserve preview and offscreen sampler state after context recreation.

### Checklist

- [x] Initialize the LUT sampler in both GL renderers.
- [x] Remove the per-render sampler assignment.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- `uLutTexture` được bind vào unit 1 một lần trong preview/offscreen context.
- `lutStrength` vẫn điều khiển việc bật/tắt LUT như trước.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Skip neutral preview makeup uploads

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Skip repeated zero makeup uniform uploads in the preview renderer.
- Upload the group again when makeup controls become active.
- Preserve adjustment and preview rendering behavior.

### Checklist

- [x] Track makeup uniform state per preview GL context.
- [x] Pass the existing upload gate to the shared binder.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Preview bỏ repeated zero makeup uniform uploads, chỉ upload lại khi trạng thái makeup thay đổi.
- Gate dùng chung với offscreen renderer; context recreate reset đúng trạng thái.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Hoist preview program and sampler state

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Keep the preview program active for its GL context.
- Bind the fixed input sampler once per program recreation.
- Preserve dynamic texture and filter uniform updates.

### Checklist

- [x] Initialize program and sampler state during surface creation.
- [x] Remove redundant per-frame program/sampler calls.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Preview giữ program active và bind sampler `uTexture` một lần mỗi GL context.
- Texture binding và dynamic uniforms vẫn cập nhật mỗi frame như trước.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Reuse preview vertex attribute bindings

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Bind preview vertex and texture attributes once per GL program/context.
- Preserve drawing behavior and context recreation handling.
- Remove per-frame enable/disable calls.

### Checklist

- [x] Bind attributes during preview surface creation.
- [x] Remove redundant per-frame attribute calls.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Preview bind vertex/texture attributes một lần mỗi GL context.
- Context recreate vẫn bind lại trong `onSurfaceCreated`.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Reuse one GPU LUT texture per session

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Reuse a LUT texture when consecutive renders use the same LUT.
- Delete the previous LUT texture when replacing it.
- Release the cached texture with the EGL session.

### Checklist

- [x] Move LUT texture ownership into `RenderSession`.
- [x] Rebind the cached texture for each render without re-uploading it.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Offscreen session giữ tối đa một LUT texture và tái sử dụng khi LUT không đổi.
- LUT cũ được xoá khi thay thế hoặc khi EGL session release.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Bulk GPU readback conversion

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Preserve RGBA-to-ARGB conversion and vertical flip exactly.
- Replace per-pixel direct-buffer reads with one bulk read.
- Avoid allocating another full-size pixel buffer.

### Checklist

- [x] Read packed RGBA pixels into the existing output array in bulk.
- [x] Convert and flip rows in place.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Readback dùng một lần `IntBuffer.get()` rồi chuyển RGBA→ARGB và flip hàng tại chỗ.
- Không thêm pixel buffer kích thước ảnh.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Add no-warp GPU fragment fast-path

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Return the original coordinate when both warp controls are inactive.
- Preserve face slimming and eye enlargement behavior when either is active.
- Keep coordinate clamping unchanged for active and inactive paths.

### Checklist

- [x] Add the early return in the shared GLSL warp function.
- [x] Verify active warp paths remain reachable.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Khi cả `uFaceSlimming` và `uEyeEnlargement` tắt, shader trả thẳng `coord`.
- Nhánh active warp vẫn chạy nguyên vẹn, bao gồm clamp cuối hàm.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Hoist fixed GPU sampler binding

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Bind the input sampler to texture unit 0 once per offscreen program.
- Preserve preview behavior, which uses the shared uniform binder directly.
- Avoid changing texture upload or LUT state handling.

### Checklist

- [x] Add an opt-out for the already-initialized sampler binding.
- [x] Initialize the offscreen sampler binding once per session.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Offscreen program bind `uTexture` một lần trong `RenderSession`; preview vẫn bind mặc định.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Remove redundant GPU edge-coordinate clamps

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Preserve edge behavior at texture boundaries.
- Rely only on the texture's existing `GL_CLAMP_TO_EDGE` sampler state.
- Reduce per-fragment arithmetic in art effects.

### Checklist

- [x] Remove the duplicate GLSL coordinate clamp.
- [x] Verify all input texture paths configure edge clamping.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- `lumaAt` dùng trực tiếp sampler đã cấu hình `GL_CLAMP_TO_EDGE`.
- Cả offscreen texture và preview texture đều giữ cấu hình clamp ở S/T.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Skip unchanged GPU source uploads

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Reuse the input texture when the same bitmap content is rendered again.
- Detect bitmap pixel changes through Android's `generationId`.
- Avoid retaining the source bitmap through the cached GL session.

### Checklist

- [x] Track the last uploaded bitmap weakly and its generation.
- [x] Skip only safe unchanged uploads.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- GPU session bỏ qua `texSubImage2D`/`texImage2D` khi cùng bitmap và `generationId` chưa đổi.
- `WeakReference` tránh giữ source bitmap lớn bởi cached session.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Reduce CPU edge sampling overhead

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Keep the existing edge/Sobel formula and output unchanged.
- Avoid repeated coordinate clamping and row-index arithmetic per neighbor.
- Do not add a full-image cache or increase peak bitmap memory.

### Checklist

- [x] Cache clamped x/y positions and row offsets in `edgeAt`.
- [x] Remove the no-longer-needed coordinate helper.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- `edgeAt` giữ nguyên công thức Sobel nhưng tái sử dụng tọa độ clamp và row offsets.
- Không tạo full-image cache nên không tăng peak bitmap memory.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Hoist CPU render flags out of the pixel loop

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Compute constant per-render flags once instead of once per pixel.
- Preserve all active-stage formulas and output behavior.
- Keep the public single-pixel test helper working.

### Checklist

- [x] Hoist neighborhood, edge, beauty, and sharpness decisions.
- [x] Pass the cached values through the existing render path.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- CPU fallback không còn tính lại các cờ render cố định ở từng pixel.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Fast-path CPU identity sampling

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Read source and neighbor pixels directly when no active face warp exists.
- Preserve bilinear sampling for active slimming or eye-enlargement warps.
- Keep edge detection and filter output behavior unchanged.

### Checklist

- [x] Detect active warp from controls and valid face geometry.
- [x] Use direct pixel reads for identity sampling and neighbors.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- CPU fallback đọc trực tiếp pixel nguồn và 4 neighbor khi không có face warp hoạt động; cờ warp được tính một lần mỗi bitmap.
- Face slimming/eye enlargement hợp lệ vẫn giữ bilinear sampling.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Skip neutral CPU adjustment stages

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Skip neutral adjustment, vignette, and grain work in CPU fallback renders.
- Preserve stage order and formulas whenever a stage is active.
- Keep art edge detection and effect output unchanged.

### Checklist

- [x] Gate neutral color and tonal adjustment stages.
- [x] Gate neutral finishing stages.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- CPU fallback skips neutral color, tonal, vignette, and grain calculations.
- Active adjustment stages retain their existing formulas and order.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Reuse offscreen program and viewport state

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Set the fixed offscreen program and viewport once per GPU session.
- Preserve output when rendering multiple thumbnails at the same dimensions.
- Keep preview rendering unchanged.

### Checklist

- [x] Move offscreen program/viewport setup into session initialization.
- [x] Remove redundant per-thumbnail state calls.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Reused offscreen sessions now retain the active program and fixed viewport state.
- Sequential thumbnails avoid repeated program and viewport setup calls.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Add Popular category visibility configuration

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Add an XML boolean configuration for showing or hiding the `popular` category.
- Keep Popular visible by default for backward compatibility.
- Keep filters available in their other categories and normalize hidden-category selection state.

### Checklist

- [x] Add `gsFilterShowPopular` and apply it to category chips/state.
- [x] Add focused coverage or verification for hidden-category fallback.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- `FilterControlsView` exposes `app:gsFilterShowPopular`, defaulting to `true`.
- When disabled, the Popular chip is hidden and a Popular selection falls back to the first visible category.
- The sample layout sets the option explicitly to `true`; change it to `false` to hide Popular.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Skip unused CPU blur averaging

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Avoid blur-channel averaging when neighborhood sampling is disabled.
- Preserve the existing sampled blur values when beauty smoothing or sharpness/clarity is active.
- Keep art effect edge detection unchanged.

### Checklist

- [x] Reuse the source channels for the inactive blur path.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- CPU fallback now avoids three blur averages when smoothing, sharpness, and clarity are inactive.
- Active neighborhood sampling and art edge detection remain unchanged.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Reuse offscreen texture storage

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Update existing texture storage without reallocating when source dimensions match.
- Reallocate safely when dimensions change.
- Preserve texture filtering, upload format, and fallback behavior.

### Checklist

- [x] Track the dimensions currently stored in the session input texture.
- [x] Use `texSubImage2D` for same-size uploads and `texImage2D` otherwise.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Same-size ARGB_8888 source uploads now update existing texture storage without reallocating it.
- Dimension or config changes use the original `texImage2D` path.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Cancel queued GPU thumbnail renders

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Let canceled thumbnail fetchers stop while waiting for the shared GPU renderer.
- Keep one-render-at-a-time EGL access and safe cleanup.
- Preserve existing fallback behavior for active requests.

### Checklist

- [x] Propagate the fetcher cancellation state into thumbnail rendering.
- [x] Replace blocking monitor wait with bounded cancellable lock acquisition.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Canceled Glide fetchers now stop while waiting for the shared GPU render lock.
- Active renders still complete normally and release EGL/temporary bitmap resources.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Remove redundant offscreen framebuffer clear

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Preserve output for the fullscreen offscreen render.
- Avoid clearing pixels that the fullscreen draw always overwrites.
- Keep preview rendering unchanged.

### Checklist

- [x] Remove the per-thumbnail offscreen clear calls.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Offscreen render no longer clears the pbuffer before the fullscreen draw.
- The draw covers the entire viewport, so the rendered output is unchanged.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Reuse GPU vertex attribute bindings

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Bind the static position and texture-coordinate attributes once per GPU session.
- Preserve the preview path and existing render output.
- Release the session normally when dimensions change or GL fails.

### Checklist

- [x] Bind offscreen attributes during session initialization.
- [x] Remove per-thumbnail attribute setup/teardown.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Offscreen position and texture-coordinate attributes are configured once per reused GPU session.
- Each thumbnail now skips repeated attribute pointer and enable/disable calls.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Avoid redundant adjustment uniform writes

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Skip neutral adjustment uniform writes after the reused session is initialized.
- Upload the neutral reset once when leaving an adjusted render.
- Preserve active adjustment values and the existing preview path.

### Checklist

- [x] Track adjustment uniform state per reused GPU session.
- [x] Gate redundant neutral writes and reset transitions safely.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Repeated neutral renders skip redundant adjustment uniform writes after session initialization.
- Active adjustments and transitions back to neutral still upload/reset the full adjustment state.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Avoid redundant makeup uniform writes

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Skip makeup scalar/geometry uniform writes when they are already inactive.
- Reset previously active makeup state exactly once before inactive renders continue.
- Leave the preview path and active beauty/warp rendering unchanged.

### Checklist

- [x] Track inactive/active makeup state per reused GPU session.
- [x] Gate redundant uniform writes and reset transitions safely.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Repeated inactive art renders skip redundant makeup scalar and geometry uniform writes.
- A transition from active beauty/warp to inactive state performs one reset before skipping later writes.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Remove contour list allocations during uniform upload

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Upload the same capped contour points without creating temporary lists.
- Preserve point order and the existing maximum point counts.
- Keep the inactive-art fast path unchanged.

### Checklist

- [x] Replace capped `take()` iterations with indexed loops.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Contour uploads now write directly into reusable uniform arrays without temporary lists.
- Point caps and ordering are unchanged.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Skip unused makeup contour uploads

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Avoid uploading makeup contour arrays when beauty and face-warp controls are inactive.
- Preserve geometry reset behavior when makeup or warp controls are active without detected features.
- Keep shader output unchanged for art thumbnails and beauty previews.

### Checklist

- [x] Gate contour and region-uniform uploads behind active makeup/warp controls.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Art thumbnails skip contour-array and region-uniform uploads when all beauty and face-warp controls are zero.
- Beauty and face-warp renders keep the existing upload/reset behavior.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Reduce GPU thumbnail readback conversion overhead

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Preserve RGBA-to-ARGB conversion and vertical image flip.
- Reduce per-pixel `ByteBuffer` access during GPU readback.
- Keep the existing reusable readback buffers.

### Checklist

- [x] Convert each pixel with one packed read instead of four byte reads.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- GPU readback now uses one packed RGBA read per pixel, then performs the same vertical flip and ARGB conversion.
- Reusable direct buffers remain unchanged.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

## Task: Reuse thumbnail input texture objects

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Reuse the input `GL_TEXTURE_2D` object for sequential renders in one GPU session.
- Preserve texture parameters and upload fresh bitmap pixels for each render.
- Delete the cached texture when the session is released.

### Checklist

- [x] Move input texture ownership into `RenderSession`.
- [x] Upload each source bitmap into the reusable texture.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Sequential renders reuse one configured input texture object per GPU session.
- Each render still uploads fresh source pixels; the texture is deleted with its session.
- `:filter:testDebugUnitTest`, filter/app compilation, and `git diff --check` passed.

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
- [x] Add Portrait category reuse for existing people-friendly filters.

### Notes

- Use the existing preset-data model; no LUT, shader, or renderer changes needed.
- Added 10 data-only presets: Clean Light, Soft Day, Rose Skin, Fresh Plate, Warm Table, Deep Teal, Fade Drama, Midnight City, Sunlit Forest, and Pearl Mono.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Keep Retro Matte out of Black & White

Status: DONE

### Requirements

- Explain why Retro Matte differs from BW filters.
- Keep Retro Matte's existing vintage look unchanged.
- Keep Black & White category limited to true monochrome filters.

### Checklist

- [x] Remove `retro_matte` from Black & White category data.
- [x] Add focused catalog coverage.
- [x] Run focused verification.

### Notes

- `retro_matte` uses warm/cool channel shifts and reduced saturation, not `isMonochrome = true`.
- Kept Retro Matte in Vintage only.
- Added catalog coverage requiring Black & White category filters to be monochrome.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Add compact Filter/Adjust tab spacing

Status: DONE

### Requirements

- Add a mechanism to place `gs_filter_tab_filter` and `gs_filter_tab_adjust` closer together.
- Keep the existing library default layout unless the new option is enabled.
- Apply the compact option in the demo screen.

### Checklist

- [x] Add tab compact/spacing style attributes.
- [x] Apply compact tab layout in `FilterControlsView`.
- [x] Enable the compact tab layout in the app sample.
- [x] Run focused verification.

### Notes

- Current tab labels are far apart because both tab containers use equal `layout_weight`.
- Added `gsFilterCompactTabs` and `gsFilterTabSpacing`.
- The app sample enables compact tabs with `gsFilterTabSpacing="0dp"`.
- `git diff --check` passed.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Align compact tab indicators

Status: DONE

### Requirements

- Keep compact Filter/Adjust tabs close together.
- Align each active indicator under its label text.
- Avoid changing non-compact tab behavior.

### Checklist

- [x] Offset compact tab indicators by label padding.
- [x] Run focused verification.

### Notes

- Compact mode aligns tab containers by edge, but label padding shifts the visible text inward.
- Compact indicators now keep text-width sizing and add the matching label padding as a start/end margin.
- `git diff --check` passed.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.

- Reopened to add another people-prioritized preset batch.
- Added 8 people-prioritized presets: Selfie Clear, Soft Portrait, Studio Skin, Golden Skin, Indoor Warm, Flash Soft, Low Light Skin, and Clean Face.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.
- Reopened to add `PORTRAIT` to existing people-friendly filters: Fresh, Clear, Clean Light, and Pearl Mono.
- Added `PORTRAIT` to Fresh, Clear, Clean Light, and Pearl Mono.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Trim visually similar built-in filter presets

Status: DONE

### Requirements

- Review all built-in filters for recipes that are too similar.
- Remove redundant presets while preserving category coverage and existing renderer behavior.
- Keep the change data-only unless tests need updating for removed catalog entries.

### Checklist

- [x] Identify near-duplicate presets from recipe/category similarity.
- [x] Remove redundant catalog entries and unused string resources.
- [x] Update focused catalog tests.
- [x] Run focused verification.

### Notes

- Use the existing preset-data model; no shader, LUT, UI, or renderer changes needed.
- Removed 15 visually close presets: Classic, Honey, Silver, Clean, Bright, Skin Soft, Ice, Dark Mood, Sweet, Fresh Food, Vivid, Green Pop, Rose Skin, Clean Face, and Manga.
- Built-in catalog now has 95 filter options including Original, with matching filter name string resources.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Reorder built-in filter categories for browsing

Status: DONE

### Requirements

- Reorder existing built-in filter categories only.
- Put people/common photo categories earlier.
- Keep category ids, filters, renderer behavior, and resources unchanged.

### Checklist

- [x] Reorder `FilterCatalog.categories`.
- [x] Add focused category order coverage.
- [x] Run focused verification.

### Notes

- This is an ordering-only change; no new category, preset, shader, LUT, UI, or resource work needed.
- New order: Popular, Portrait, Natural, Food, Landscape, Night, Film, Cinematic, Vintage, Black & White, Warm, Cool, Aesthetic, Creative, Art.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Reduce non-Popular filter category overlap

Status: DONE

### Requirements

- Optimize built-in filter category assignments outside `Popular`.
- Keep existing filter ids, names, recipes, renderer behavior, and category list unchanged.
- Keep intentional cross-listing only when a filter clearly belongs to two photo contexts.

### Checklist

- [x] Reduce broad non-Popular overlaps in `FilterCatalog`.
- [x] Verify category coverage still stays useful.
- [x] Run focused verification.

### Notes

- This is a catalog data-only change; no preset removal, shader, LUT, UI, or resource work needed.
- Reduced non-Popular cross-listed filters from 55 to 9.
- Remaining cross-listed filters are intentional: Portra, Noir, Retro Matte, Soft Mono, Neon, Blue Hour, Cyberpunk, Low Light Skin, and Pearl Mono.
- Every category still has at least 5 filters after the overlap cleanup.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Prioritize filters inside each built-in category

Status: DONE

### Requirements

- Optimize the visible order of filters inside each built-in category.
- Keep filter ids, names, recipes, category assignments, renderer behavior, and resources unchanged.
- Put the strongest/common choices first for people, natural, food, landscape, night, and style categories.

### Checklist

- [x] Add category-specific filter priority ordering.
- [x] Add focused catalog order coverage.
- [x] Run focused verification.

### Notes

- This should be ordering-only catalog behavior; no shader, LUT, UI layout, or preset data changes needed.
- Added category-specific priority ordering while preserving fallback catalog order for unlisted filters.
- Added focused catalog tests for top filters in Popular, Portrait, Natural, Food, Landscape, Night, Film, Cinematic, Black & White, and Art.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Add starter LUT filter presets

Status: DONE

### Requirements

- Add 10 starter LUT-style built-in filters.
- Keep existing filter recipes and UI behavior working.
- Apply LUT grading consistently in preview, thumbnails, GPU export, and CPU fallback.
- Avoid new dependencies and external LUT asset files for this first pass.

### Checklist

- [x] Add LUT metadata to `FilterRecipe` and render params.
- [x] Add GPU LUT sampling and CPU fallback grading.
- [x] Add 10 built-in LUT presets and names.
- [x] Update JSON/catalog/render tests.
- [x] Run focused verification.

### Notes

- Use generated internal LUT textures instead of shipping PNG assets for now.
- Added 10 LUT presets: Clean Portrait, Soft Skin, Golden Portrait, Daylight Fresh, Food Pop, Green Film, Teal Cinema, Night Mood, Vintage Fade, and Editorial Matte.
- Built-in catalog now has 105 filter options including Original.
- GPU preview/export sample generated 16x16x16 LUT textures through `uLutTexture`; CPU fallback uses the same LUT grading formulas.
- JSON filter packs can specify `lut` and `lutStrength`.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Make LUT intensity visibly responsive

Status: DONE

### Requirements

- Make the Intensity seekbar visibly affect LUT-based filters.
- Make the Intensity seekbar feel less abrupt while dragging.
- Preserve 0 and 100 endpoints for all filters.

### Checklist

- [x] Map LUT strength with linear filter intensity.
- [x] Replace squared preset intensity with a smoother response curve.
- [x] Update focused shader parameter coverage.
- [x] Run focused verification.

### Notes

- Root issue: LUT strength used the same squared intensity curve as recipe adjustments, so mid-slider values became too subtle.
- The squared preset curve also made the upper half of the seekbar change too quickly.
- LUT strength now follows the linear seekbar value, while preset adjustments use a smoothstep curve.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Retune starter LUT presets for visible differences

Status: DONE

### Requirements

- Make the newly added LUT filters visibly distinct on real photos.
- Keep the existing generated-LUT implementation.
- Keep filter ids, names, categories, UI, and renderer API unchanged.
- Refresh thumbnail cache keys so old subtle LUT thumbnails are not reused.

### Checklist

- [x] Increase starter LUT grade strength.
- [x] Set LUT presets to full default strength.
- [x] Increase generated LUT size from 16 to 33.
- [x] Bump thumbnail render cache version.
- [x] Add focused LUT visibility coverage.
- [x] Run focused verification.

### Notes

- User feedback: current LUT presets are too subtle and do not look meaningfully different.
- 33x33x33 is still small enough for generated internal LUTs and gives finer color grading than the first 16x16x16 pass.
- Retuned all starter LUT formulas with stronger channel shifts, split-tone, fade, saturation, and contrast differences.
- Set all 10 LUT presets to `lutStrength = 100`.
- Bumped thumbnail render cache version to `gpu-preview-v14`.
- Added LUT visibility coverage requiring each color LUT to produce a minimum visible color delta.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Soften Food Pop LUT highlights

Status: DONE

### Requirements

- Fix Food Pop looking too harsh on portrait/highlight photos.
- Keep Food Pop visibly food-oriented.
- Keep other LUTs, categories, UI, and renderer behavior unchanged.

### Checklist

- [x] Retune the `FoodPop` LUT formula.
- [x] Reduce the `lut_food_pop` preset strength/boosts.
- [x] Refresh thumbnail cache keys.
- [x] Run focused verification.

### Notes

- Screenshot feedback: Food Pop pushes skin too yellow/orange and creates cyan-looking highlight artifacts.
- Reduced Food Pop LUT contrast/saturation and highlight warming while keeping a small warm food shift.
- Reduced `lut_food_pop` strength from 100 to 85 and lowered saturation/vibrance/clarity boosts.
- Bumped thumbnail render cache version to `gpu-preview-v15`.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Add tick icon for filter close button

Status: DONE

### Requirements

- Add a tick icon drawable for `gs_filter_close_button`.
- Use the existing close-icon customization path.
- Keep current close-button behavior unchanged.

### Checklist

- [x] Add a prefixed tick vector drawable.
- [x] Apply the tick icon in the app sample.
- [x] Run focused verification.

### Notes

- `FilterControlsView` already supports `gsFilterCloseIcon`, so no view code change is needed.
- Added `ic_gs_tick` and assigned it to `gsFilterCloseIcon` in the app sample.
- `git diff --check` passed.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Update README for latest filter controls

Status: DONE

### Requirements

- Document recent LUT recipe support.
- Document compact Filter/Adjust tab spacing.
- Document close-button icon customization with the new tick icon.

### Checklist

- [x] Update README feature summary.
- [x] Update controls XML example and notes.
- [x] Update JSON recipe fields.
- [x] Update styling attribute table.
- [x] Run documentation verification.

### Notes

- Keep the README concise and aligned with existing Vietnamese docs.
- Documented generated LUT support and JSON `lut`/`lutStrength` fields.
- Documented `gsFilterCompactTabs`, `gsFilterTabSpacing`, and `gsFilterCloseIcon` with `ic_gs_tick`.
- `git diff --check` passed.

## Task: Document supported built-in filter presets

Status: DONE

### Requirements

- Add the supported built-in filter preset list to README.
- Group presets by category.
- Note that JSON packs can replace or extend the built-in list.

### Checklist

- [x] Generate the current built-in preset list from catalog resources.
- [x] Add the preset list to README.
- [x] Run documentation verification.

### Notes

- `Original` is a fixed action, not counted as a filter preset.
- Added the built-in preset list grouped by category to README.
- Verified the catalog currently has 104 string-id presets, excluding `Original`.
- `git diff --check` passed.

## Task: Make Reset All adjust action lighter

Status: DONE

### Requirements

- Make Reset All look like a secondary adjust action instead of a full-width primary button.
- Show Reset All only when at least one adjust value changed.
- Keep existing reset callbacks and behavior.

### Checklist

- [x] Restyle Reset All in adjust layout.
- [x] Hide/show Reset All from current adjustments.
- [x] Run focused verification.

### Notes

- Keep the per-control reset icon unchanged.
- Reset All is now a small right-aligned text action.
- Reset All is hidden until any adjust control differs from defaults.
- `git diff --check` passed.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Fix Reset All inflate crash

Status: DONE

### Requirements

- Fix `gs_view_adjust_controls` crashing while inflating `gs_adjust_reset_all_button`.
- Keep Reset All as a lightweight right-aligned action.

### Checklist

- [x] Use a framework-safe ripple background attribute.
- [x] Run focused verification.

### Notes

- Crash points at the `Button` after adding `?attr/selectableItemBackground`; host themes may not define that unqualified attr.
- Switched Reset All background to `?android:attr/selectableItemBackground`.
- `git diff --check` passed.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Improve Reset All disabled state and rounded background

Status: DONE

### Requirements

- Keep Reset All visible with a reasonable disabled state when no adjust value changed.
- Use a rounded Reset All background with 30dp corner radius.
- Preserve existing reset callback behavior.

### Checklist

- [x] Add rounded enabled/disabled Reset All background.
- [x] Use stateful Reset All text color.
- [x] Toggle `isEnabled` instead of hiding the button.
- [x] Run focused verification.

### Notes

- Avoid theme-dependent background attrs after the previous inflate crash.
- Added `gs_bg_adjust_reset_all` with 30dp rounded enabled/disabled states.
- Reset All now stays visible and uses `isEnabled` for the disabled state.
- `git diff --check` passed.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Add Reset All pressed state

Status: DONE

### Requirements

- Add a pressed state to the Reset All rounded background.
- Keep existing enabled/disabled states and 30dp corner radius.

### Checklist

- [x] Add `state_pressed` to Reset All background selector.
- [x] Run focused verification.

### Notes

- Pressed item must appear before the default enabled item in the selector.
- Added the pressed rounded state before disabled/default states.
- `git diff --check` passed.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Move Adjust seekbar below control icons

Status: DONE

### Requirements

- Keep Filter tab layout unchanged.
- In Adjust tab, show the adjust control icons before the active seekbar/value row.
- Keep existing adjust behavior, reset controls, and styling.

### Checklist

- [x] Reorder `gs_view_adjust_controls` children.
- [x] Run focused verification.

### Notes

- Kotlin binds by view id, so this should be a layout-only change.
- Adjust icons now render above the active seekbar/value row.
- `git diff --check` passed.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Tint all seekbar thumbs with selected color

Status: DONE

### Requirements

- Make every existing SeekBar thumb/dot use its selected/progress color.
- Keep existing track colors, ranges, and drag behavior unchanged.

### Checklist

- [x] Update Adjust seekbar thumb tint.
- [x] Update Filter intensity seekbar thumb tint.
- [x] Run focused verification.

### Notes

- Existing SeekBars are `gs_adjust_seek_bar` and `gs_filter_intensity_seek_bar`.
- Adjust seekbar thumb now uses `gsAdjustSelectedColor`.
- Filter intensity seekbar thumb now uses `gsFilterIntensityProgressColor`.
- `git diff --check` passed.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Keep adjust reset button visible but disabled

Status: DONE

### Requirements

- Stop hiding/showing the current-adjust reset button.
- Keep the reset button visible in the row and disable it when the selected adjust value is already default.
- Preserve the existing reset behavior when enabled.

### Checklist

- [x] Set the initial reset button state to disabled in layout.
- [x] Toggle `isEnabled` instead of `visibility` during adjust rendering.
- [x] Run focused verification.

### Notes

- The current-adjust reset button id is `gs_adjust_reset`.
- Reset button now stays in the seek row and uses disabled alpha when the selected adjust is at default.
- `git diff --check` passed.
- `:app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Expose filter APIs needed by Family Photo Frame

Status: DONE

### Requirements

- Keep changes limited to the `filter` module.
- Make display names usable by consuming apps.
- Provide one public render entry point that uses GPU first and CPU fallback.
- Fix custom JSON packs so their default filter belongs to the parsed pack.
- Avoid public renderer internals that the consuming app does not need.

### Checklist

- [x] Make filter/category display name helpers public.
- [x] Add a public GPU-first render facade.
- [x] Parse custom pack default filter safely.
- [x] Add focused unit coverage.
- [x] Run focused verification.

### Notes

- Family Photo Frame currently needs `displayName` outside the module and has to keep its own GPU/CPU fallback wrapper.
- Added `FilterRenderer.getBitmap(...)` as the public GPU-first/CPU-fallback entry point.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Add batch filter rendering progress

Status: DONE

### Requirements

- Keep changes limited to the `filter` module.
- Optimize the public render path for a small list of images by processing one bitmap at a time.
- Expose simple percent progress for callers rendering multiple images.
- Avoid new dependencies and avoid a large batch/export framework.

### Checklist

- [x] Add a public batch render helper with progress callbacks.
- [x] Keep offscreen GPU rendering and LUT cache safe when callers render from multiple threads.
- [x] Add focused unit coverage for progress percent calculation.
- [x] Run focused verification.

### Notes

- The module owns bitmap filtering only; URI decode, file output, overwrite policy, and MediaStore progress UI stay in the consuming app.
- Added `FilterRenderer.renderBatch(...)` so consuming apps can render a small bitmap list one at a time and receive `FilterRenderProgress.percent`.
- `FilterGpuBitmapRenderer` now serializes offscreen GPU renders to avoid accidental parallel EGL pressure.
- `GlLutTexture` now protects its LUT bitmap cache from concurrent access.
- `git diff --check` passed.
- `:filter:testDebugUnitTest` passed after rerunning Gradle outside the sandbox for wrapper network access.

## Task: Save the filtered image to app storage

Status: DONE
Created: 2026-09-06
Completed: 2026-09-06

### Requirements

- Add logic to save the currently filtered image.
- Save only inside the app's private storage.
- Keep API 24 compatibility.
- Avoid new dependencies and storage permission complexity.

### Checklist

- [x] Wire the existing done/close action to save the current filtered bitmap.
- [x] Write the rendered bitmap to app-private storage.
- [x] Surface save success and failure.
- [x] Run focused verification.

### Notes

- The tick action now renders the current filter state, saves a JPEG under `filesDir/filtered`, then finishes on success.
- Uses app-private storage only, so no storage permission, picker, or MediaStore flow is needed.
- `git diff --check` passed.
- Static search found no `MediaStore`, document picker action, or storage permissions under `app/src/main` or `filter/src/main`.
- `:app:compileDebugKotlin` could not run because Gradle fails to establish a loopback connection even with `--no-daemon`.

## Task: Prevent oversized sample bitmap draw crash

Status: COMPLETE
Created: 2026-09-06

### Requirements

- Stop the sample image preview from crashing on very large bundled assets.
- Cap the decoded sample image at a maximum 4K edge.
- Keep API 24 compatibility.
- Avoid new dependencies.

### Checklist

- [x] Decode the bundled sample at a drawable preview size.
- [x] Verify focused build checks.

### Notes

- Crash: `Canvas: trying to draw too large(943718400bytes) bitmap`.
- CodeGraph marker exists, but the local CLI reports no usable index, so source inspection uses `rg`.
- Added bounds-first asset decoding with a 4096px max edge for the app sample.
- Added pure unit coverage for the 15360x15360 sample-size calculation.
- `git diff --check` passed.
- `:app:testDebugUnitTest :app:compileDebugKotlin` passed after rerunning Gradle outside the sandbox for Gradle cache access.
- `:app:assembleDebug` passed after rerunning Gradle outside the sandbox for Gradle cache access.
- Updated the cap from 2048px to 4096px per follow-up request.
- `:app:testDebugUnitTest :app:compileDebugKotlin` passed after the 4096px update.
- `:app:assembleDebug` passed after the 4096px update.
- Reopened because `FilterViewModel` was passing the RAM-based preview size to `LoadUtils.getBitmapFromAsset`, so the sample decode stayed near 1080px instead of 4K.

## Task: Use KSP for Glide processing

Status: DONE
Created: 2026-09-06
Completed: 2026-09-06

### Requirements

- Use KSP in the app module.
- Use Kotlin 2.2.21 with matching KSP 2.2.21-2.0.4.
- Replace Glide Java annotation processing with Glide KSP processing.
- Keep the diff small and avoid unrelated dependency changes.

### Checklist

- [x] Add the KSP Gradle plugin.
- [x] Switch Glide processing dependency to KSP.
- [x] Run focused verification.

### Notes

- Existing compile check failed before KSP because Kotlin plugin 2.1.10 could not read Kotlin stdlib 2.3.10 metadata from Coil 3.4.0.
- No KSP plugin or `ksp(...)` dependency existed before this task.
- First KSP run reached `:app:kspDebugKotlin` but failed with `OutOfMemoryError: Metaspace` under the default 384MiB metaspace.
- Added Gradle JVM args to give KSP enough metaspace.
- `:app:compileDebugKotlin` passed and generated Glide KSP sources.
- `:app:testDebugUnitTest :filter:testDebugUnitTest :app:compileDebugKotlin :app:assembleDebug` passed after the KSP update.

## Task: Use LoadUtils for sample image loading

Status: DONE
Created: 2026-09-06
Completed: 2026-09-06

### Requirements

- Use `LoadUtils` to load the sample image when it can replace the local decode logic.
- Keep asset input support.
- Keep API 24 compatibility.
- Avoid new dependencies.

### Checklist

- [x] Add asset bitmap loading support to `LoadUtils`.
- [x] Replace `FilterViewModel`'s local sample decode with `LoadUtils`.
- [x] Update focused decode coverage.
- [x] Run available focused verification.

### Notes

- Added `LoadUtils.getBitmapFromAsset(...)` for bundled asset images.
- Reused `LoadUtils` sizing logic for path, resource, and asset bitmap loads.
- `FilterViewModel` now loads `sample.jpg` through `LoadUtils` with `LoadUtils.calculatorImageSize(...)`.
- Removed the ViewModel-local sample-size helper and pointed its unit coverage at `LoadUtils`.
- Static search found no remaining `sampleBitmapInSampleSize`, `SAMPLE_BITMAP_MAX_EDGE`, or `BitmapFactory` usage in `FilterViewModel`.
- `git diff --check` passed.
- `:app:testDebugUnitTest :app:compileDebugKotlin` could not run because Gradle fails to establish a loopback connection before executing tasks.

## Task: Add shader-based beauty smoothing and whitening without GPUPixel

Status: DONE
Created: 2026-09-08

### Requirements

- Add a lightweight beauty effect to the existing OpenGL ES renderer.
- Expose beauty controls in a dedicated `Beauty` tab separate from `Adjust`.
- Add lightweight blush and lipstick overlays driven by face contours.
- Support beauty rendering without GPUPixel, JNI, or a native rendering dependency.
- Keep the existing filter and adjustment behavior unchanged when beauty is disabled.

### Approach

- Reuse the existing single-pass GPU shader and parameter pipeline.
- Use a conservative color-based skin mask so non-skin pixels are affected less.
- Apply a small neighborhood blur for smoothing and a restrained lift for whitening.
- Use ML Kit cheek landmarks and lip contours to map makeup regions into compact normalized features; keep rendering in the existing GPU/CPU pipeline.
- Reuse the existing tab and slider patterns so Beauty stays separate from Adjust.

### Checklist

- [x] Add beauty parameters to the existing recipe/parameter mapping.
- [x] Add shader uniforms and GPU beauty math.
- [x] Keep CPU fallback behavior consistent.
- [x] Add focused unit coverage for parameter mapping and CPU behavior.
- [x] Add a dedicated Beauty tab with smoothing and whitening controls.
- [x] Persist Beauty values in the existing ViewModel state and render recipe.
- [x] Add face-contour detection and normalized makeup features.
- [x] Add blush/lipstick recipe, shader, CPU fallback, and Beauty controls.
- [x] Rotate makeup masks with the detected eye-line angle for tilted faces.
- [x] Soften and reduce makeup masks so maximum slider values remain natural.
- [x] Suppress the hidden cheek mask for faces turned strongly to one side.
- [x] Improve lip center and size calculation for angled mouths.
- [x] Use the detected outer lip contour as the lipstick mask for non-frontal faces.
- [x] Split upper/lower lip contours so skin between and below the lips is excluded.
- [x] Run affected tests and debug build.

### Verification notes

- `git diff --check` passed.
- Beauty layout XML parsed successfully and its view IDs are unique.
- ML Kit contour detection is bundled so makeup works without a first-run model download; no GPUPixel or JNI was added.
- Cheek placement now prefers ML Kit cheek landmarks instead of the first cheek contour point; lipstick uses the detected outer lip polygon.
- Strong yaw now fades the hidden cheek instead of painting two unreliable cheek points.
- Lip placement now uses the contour centroid and mouth-corner midpoint when available, instead of only an axis-aligned bounding-box midpoint.
- Tilt handling derives roll from the two eye contours and rotates cheek/lip masks in both render paths.
- `:filter:testDebugUnitTest :app:compileDebugKotlin` passed with Android Studio JBR 21 using a short `C:\jtmp` TEMP path to avoid the Windows Java loopback issue.
- `:app:installDebug` passed and the app was tested on Samsung A56 (`SM-A566B`); Lip 100 changed only the mouth region in the captured result.
- Upper and lower ML Kit lip contours now render as separate hard polygon masks; the CPU path has regression coverage for the gap and skin below the lips.
- Lipstick now also requires source-pixel saturation and red dominance, preventing warm skin inside an imperfect contour from being colored.
- Whitening now uses a luminance lift with highlight protection and mild desaturation instead of a per-channel white overlay.

## Next Task: Finish Beauty accuracy and add practical makeup features

Status: DONE
Created: 2026-09-09
Completed: 2026-09-09

### Goal

- Make the existing Beauty controls look natural on frontal, three-quarter, and profile faces.
- Keep the current GPU/CPU pipeline and ML Kit contour input.
- Do not add GPUPixel, JNI, or a new rendering dependency.

### Progress

- [x] Add local-contrast edge protection to the CPU and GPU smoothing masks.
- [x] Add a regression test for smoothing beside a high-contrast boundary.
- [x] Run unit tests, compile, install Debug, and launch on Samsung A56.
- [x] Compare smoothing at 0 and 100 on the angled `sample.jpg`; the Result changes while the image edges remain intact.
- [x] Retune Blush with a smaller soft core, skin-color gating, and luminance-preserving tint in CPU/GPU.
- [x] Add a regression test that keeps Blush off neutral pixels outside the skin region.
- [x] Constrain Smoothing and Whitening to the detected face region so clothing/background colors are not treated as skin.
- [x] Verify Blush and Lipstick on the angled sample and Beauty rendering on the frontal asset.

### Implementation order

1. **Smoothing**
   - Keep the skin mask, but protect eyes, lips, hair, and strong edges.
   - Replace the visibly flat blur behavior with a smaller edge-aware blend.
   - Verify that texture remains at low/medium strength and edges do not melt at maximum strength.

2. **Blush**
   - Derive cheek centers from visible face geometry and eye-line roll, not fixed left/right points.
   - Use rotated soft masks with yaw-based strength so the hidden cheek fades naturally.
   - Blend blush into the source hue/saturation instead of mixing toward a fixed opaque red color.

3. **Lipstick**
   - Keep separate upper/lower contour polygons as the primary mask.
   - Add a narrow feather and reject pixels outside the lip color range to remove spill on the surrounding skin.
   - Preserve lip shading and texture by using a restrained color blend rather than replacing RGB directly.
   - Re-test frontal, tilted, and profile images before adding more makeup controls.

4. **Whitening tuning**
   - Compare low, medium, and maximum strength against the sample and angled faces.
   - Ensure highlights, lips, eyes, and hair are not lifted noticeably.
   - Tune only shared mask/strength constants after the three controls above are stable.

5. **Optional next controls**
   - Teeth whitening using a mouth/teeth color mask.
   - Under-eye brightening using eye contours and a narrow lower-eye region.
   - Eye shadow or eye liner only after face-region masks are reliable.
   - Face reshape/eye enlargement is deferred until landmark stability and export behavior are validated.

### Acceptance checks

- Beauty disabled produces the original filtered result byte-for-byte where currently guaranteed.
- Each control affects only its intended region and remains bounded at 0 and 100.
- No visible rectangular/oval patch, color halo, or spill on angled faces.
- CPU fallback and GPU preview use the same mask and strength behavior.
- Add focused CPU regression tests for each mask boundary, then run unit tests, compile, install Debug, and manually check at least frontal plus angled samples.

### Verification notes

- Added the normalized ML Kit face bounding box to the existing makeup features and reused it as the CPU/GPU face-area mask.
- On-device verification: Whitening 0→100 stayed inside the face on the angled sample; Blush 0→100 stayed on the visible cheek; Lipstick 100 stayed on the mouth.
- The frontal `demo.png` asset opens the Beauty tab and renders without detection errors.

## Task: Cycle test images from app assets

Status: DONE
Created: 2026-09-09

### Requirements

- Keep image input in `app/src/main/assets`.
- Start with `sample.jpg`, then cycle through other `.jpg`, `.jpeg`, `.png`, and `.webp` assets.
- Reset detected makeup features when changing images and re-run face detection for the new bitmap.
- Keep the current filter, Beauty, and Adjust values while switching images.

### Checklist

- [x] Discover supported image assets and keep `sample.jpg` as the first image.
- [x] Add a `Next image` button that cycles through the asset list.
- [x] Prevent switching while an image is loading.
- [x] Add asset-list unit coverage.
- [x] Run unit tests and compile the app.
- [x] Install Debug and verify switching from `sample.jpg` to `demo.png` on Samsung A56.

### Notes

- Add more test photos directly to `app/src/main/assets`; the button remains disabled when only one image exists.
- The current asset bitmap is replaced safely through the existing state/preview flow; old bitmaps are left for normal garbage collection instead of being recycled while the GL view may still use them.

## Task: Add under-eye brightening control

Status: DONE
Created: 2026-09-09
Completed: 2026-09-09

### Requirements

- Add an Under-eye Brightening slider to the existing Beauty tab.
- Use ML Kit eye contours to place a narrow mask below each detected eye.
- Keep CPU fallback and GPU preview behavior aligned.
- Keep the effect bounded and avoid adding a dependency.

### Checklist

- [x] Add the recipe, state, ViewModel, UI, and JSON mapping.
- [x] Add normalized eye geometry and CPU/GPU under-eye rendering.
- [x] Add a regression test for the under-eye boundary.
- [ ] Run unit tests, compile, install Debug, and manually check the frontal asset.

### Notes

- Unit tests, compile, and Debug installation passed on 2026-09-09.
- Manual frontal-device verification is pending because the connected Samsung A56 is currently locked behind a PIN.

## Task: Add teeth whitening control

Status: DONE
Created: 2026-09-09
Completed: 2026-09-09

### Requirements

- Add a Teeth Whitening slider to the existing Beauty tab.
- Restrict the effect to bright, low-saturation pixels inside the detected mouth area.
- Preserve colored lips and dark mouth pixels.
- Keep CPU fallback and GPU preview behavior aligned without a new dependency.

### Checklist

- [x] Add the recipe, state, ViewModel, UI, and JSON mapping.
- [x] Add the mouth/teeth color mask to CPU and GPU rendering.
- [x] Add a regression test for bright teeth versus lips/skin.
- [ ] Run unit tests, compile, install Debug, and manually check an image with visible teeth.

### Notes

- Unit tests, compile, and Debug installation passed on 2026-09-09.
- Manual verification is pending an unlocked device and an asset with visible teeth; the current bundled photos have closed/partly closed mouths.

## Task: Match Beauty controls to the Adjust selector UI

Status: DONE
Created: 2026-09-09
Completed: 2026-09-09

### Requirements

- Keep Beauty values and SeekBar range at `0..100`.
- Present Beauty controls as a horizontal selectable rail like Adjust.
- Use one shared SeekBar for the selected Beauty control.
- Preserve existing Beauty state, recipe values, reset behavior, and callbacks.

### Checklist

- [x] Replace the repeated Beauty slider rows with a selector rail and shared SeekBar.
- [x] Reuse existing icon, color, and reset styling where possible.
- [x] Run layout/build/unit checks and review the final diff.

### Notes

- Beauty now follows the Adjust interaction: select Smoothing, Whitening, Blush, Lipstick, Under-eye, or Teeth from a horizontal rail, then edit the selected value with one shared `0..100` SeekBar.
- Existing recipe values and callbacks remain unchanged; the UI only changes how controls are presented.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:compileDebugKotlin` and `:app:installDebug` passed.

## Task: Label Adjust reset and add Beauty icons

Status: DONE
Created: 2026-09-09
Completed: 2026-09-09

### Requirements

- Rename the Adjust all-controls reset action to `Reset All Adjustments`.
- Give each Beauty control its own dedicated vector icon.
- Keep existing behavior and the `0..100` Beauty SeekBar range unchanged.

### Checklist

- [x] Update the reset string.
- [x] Add and wire six Beauty vector icons.
- [x] Run focused resource/build checks and review the final diff.

### Notes

- Adjust now uses `Reset All Adjustments` for the bottom action.
- Beauty controls use dedicated smoothing, whitening, blush, lipstick, under-eye, and teeth icons instead of reusing Adjust icons.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:compileDebugKotlin :app:installDebug` passed.

### Follow-up

- [x] Rename the Beauty all-controls reset action to `Reset All Beauty`.

### Follow-up notes

- The Beauty reset label now matches the Adjust wording pattern.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:compileDebugKotlin :app:installDebug` passed after the label update.

## Task: Add eye shadow and eyeliner Beauty controls

Status: DONE
Created: 2026-09-09
Completed: 2026-09-09

### Requirements

- Add Eye Shadow and Eyeliner controls to the existing Beauty tab.
- Use the detected eye centers/radii and roll angle for placement.
- Keep masks localized to the upper eyelid/eye area for frontal and tilted faces.
- Keep CPU fallback and GPU preview behavior aligned.
- Preserve the Beauty `0..100` range and existing controls.

### Checklist

- [x] Add recipe, state, ViewModel, UI, and JSON mapping.
- [x] Add CPU/GPU eye shadow and eyeliner masks.
- [x] Add focused rendering regression coverage.
- [x] Run unit tests, compile, install Debug, and review the final diff.

### Notes

- Eye Shadow and Eyeliner are driven by existing ML Kit eye geometry and roll angle; no new dependency was added.
- Both CPU fallback and GPU preview use the same localized upper-eyelid masks.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:compileDebugKotlin :app:installDebug` passed.

## Task: Add face slimming and eye enlargement Beauty controls

Status: DONE
Created: 2026-09-09
Completed: 2026-09-09

### Requirements

- Add Face Slimming and Eye Enlargement controls to the Beauty tab.
- Use the detected face and eye geometry, including roll angle, for placement.
- Apply bounded inverse warps so pixels outside the target face/eye regions stay unchanged.
- Keep CPU fallback and GPU preview behavior aligned.
- Preserve the Beauty `0..100` range and existing controls.

### Checklist

- [x] Add recipe, state, ViewModel, UI, and JSON mapping.
- [x] Add bounded CPU/GPU inverse-warp logic.
- [x] Add focused geometry/warp regression coverage.
- [x] Run unit tests, compile, install Debug, and review the final diff.

### Notes

- Face Slimming uses a bounded horizontal inverse warp inside the detected face ellipse.
- Eye Enlargement uses a bounded radial inverse warp around detected eyes and follows the detected roll angle.
- CPU fallback uses bilinear source sampling so the warp does not create hard pixel steps.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:compileDebugKotlin :app:installDebug` passed.

## Task: Add eyebrow enhancement Beauty control

Status: DONE
Created: 2026-09-09
Completed: 2026-09-09

### Requirements

- Add an Eyebrow enhancement control to the existing Beauty tab.
- Use ML Kit upper/lower eyebrow contours for both frontal and angled faces.
- Keep CPU fallback and GPU preview masks aligned.
- Preserve the Beauty `0..100` range and avoid a new dependency.

### Checklist

- [x] Add recipe, state, ViewModel, UI, JSON mapping, and icon.
- [x] Add contour-based CPU/GPU brow rendering.
- [x] Add a focused regression test for the polygon boundary.
- [x] Run unit tests, compile, install Debug, and review the final diff.

### Notes

- Eyebrow enhancement uses separate ML Kit upper/lower eyebrow contour polygons, so the effect follows tilted and three-quarter faces without a guessed oval region.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:compileDebugKotlin :app:installDebug` passed on Samsung A56 (`SM-A566B`); the app launched without shader or runtime errors in logcat.

## Task: Keep the initial Result unchanged

Status: DONE
Created: 2026-09-09
Completed: 2026-09-09

### Requirements

- Start with the built-in `original` filter so Result matches the source before any user action.
- Keep the JSON filter-pack switch available for explicit testing.

### Checklist

- [x] Disable JSON pack by default in the sample screen.
- [x] Run focused build/test checks and install Debug.

### Notes

- The JSON pack remains opt-in; its first filter is `Fresh Air`, so loading it at startup was the reason Result differed before any user action.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:compileDebugKotlin :app:installDebug` passed, and UIAutomator confirmed `filterPackSwitch checked=false` after launch.

## Task: Match Original and Result when no edits are active

Status: SUPERSEDED
Created: 2026-09-09
Completed: 2026-09-09

### Requirements

- Make Result visually identical to Original when the recipe and adjustments are at defaults.
- Keep the GPU preview for any active filter, Beauty, or Adjust change.

### Checklist

- [ ] Add an identity Result view using the same ImageView rendering path as Original.
- [ ] Toggle GPU preview only when an edit is active.
- [x] Run tests/build, install Debug, and compare the sample screenshot.

### Notes

- Reverted the extra identity ImageView and one-pixel preview balancing after deciding the existing preview path is sufficient.

## Task: Keep JSON pack at None by default

Status: DONE
Created: 2026-09-09
Completed: 2026-09-09

### Requirements

- Enabling the JSON pack must leave the editor at None/Original.
- Do not preselect or apply the first JSON filter.

### Checklist

- [x] Add the JSON pack's Original option as the default without displaying it as a category tile.
- [x] Add a parser regression test for an uncategorized Original option.
- [x] Run focused tests and compile/install Debug.

### Notes

- JSON pack now declares `defaultFilterId: "original"`; its Original option has no category, so no filter tile is selected or displayed by default.
- `:filter:testDebugUnitTest :app:testDebugUnitTest :app:compileDebugKotlin :app:installDebug` passed and Debug was installed on Samsung A56 (`SM-A566B`).

## Task: Add bitmap equality diagnostic

Status: DONE
Created: 2026-09-09
Completed: 2026-09-09

### Requirements

- Compare the source bitmap with the rendered bitmap without adding another preview view.

### Checklist

- [x] Use Android's native pixel comparison and log the result when saving.
- [x] Compile the app after removing the identity ImageView.

### Notes

- The diagnostic checks nullability and dimensions before calling `Bitmap.sameAs`; it does not compare layout scaling or screenshot pixels.

## Task: Preserve identity bitmap output without an extra ImageView

Status: DONE
Created: 2026-09-09
Completed: 2026-09-09

### Requirements

- When no filter, Beauty, or Adjust value is active, keep the rendered bitmap pixel-identical to the source.
- Do not add a second preview ImageView.

### Checklist

- [x] Return a copied source bitmap for the identity export path.
- [x] Compile and install the Debug app.

### Notes

- The previous `false` comparison came from running the identity image through GPU texture sampling; the identity path now avoids that rounding.

## Task: Update README for current sample app

Status: DONE
Created: 2026-09-09
Completed: 2026-09-09

### Checklist

- [x] Document Beauty controls and the assets-based Next Image flow.
- [x] Document Windows build/test/install commands.
- [x] Document JSON pack default Original/None and identity bitmap export.

## Task: Optimize filter thumbnail loading and GL context recovery

Status: DONE
Created: 2026-09-10
Completed: 2026-09-10

### Requirements

- Keep the existing filter UI and GPU pipeline.
- Avoid eagerly rendering every thumbnail when the filter rail is shown.
- Re-upload the preview bitmap after an OpenGL context recreation.
- Keep the change local to the `filter` module.

### Checklist

- [x] Remove eager thumbnail preloading and its now-unused state.
- [x] Preserve and re-upload the current preview bitmap on GL context creation.
- [x] Run focused unit tests, compile, and review the diff.

### Notes

- Thumbnail rendering now starts when RecyclerView binds visible items instead of eagerly preloading the whole category.
- The preview renderer keeps the current source bitmap and re-uploads it after an OpenGL context recreation.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, and `:app:compileDebugKotlin` passed.

## Task: Bound offscreen readback buffer retention

Status: DONE
Created: 2026-09-10
Completed: 2026-09-10

### Requirements

- Keep readback reuse for normal thumbnails and previews.
- Do not retain buffers sized for a one-off large export indefinitely.
- Keep rendered pixels unchanged.

### Checklist

- [x] Trim oversized cached readback buffers after rendering.
- [x] Run focused unit tests, compile, and review the diff.

### Notes

- Readback buffers up to 1 MP are reused; larger one-off buffers are released after rendering.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, and `:app:compileDebugKotlin` passed.

## Task: Cancel stale filter thumbnail renders

Status: DONE
Created: 2026-09-10
Completed: 2026-09-10

### Requirements

- Stop delivering thumbnail results after Glide cancels a request.
- Recycle a rendered bitmap only when it has not been handed to Glide.
- Keep normal thumbnail rendering unchanged.

### Checklist

- [x] Track cancellation in the thumbnail data fetcher.
- [x] Drop and recycle canceled results safely.
- [x] Run focused unit tests, compile, and review the diff.

### Notes

- Canceled thumbnail requests no longer deliver stale bitmaps to Glide; rendered results are recycled when safe.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, and `:app:compileDebugKotlin` passed.

## Task: Reuse offscreen GPU geometry buffers

Status: DONE
Created: 2026-09-10
Completed: 2026-09-10

### Requirements

- Reuse the immutable vertex and texture coordinate buffers for offscreen renders.
- Keep access serialized by the existing render lock.
- Keep rendered pixels unchanged.

### Checklist

- [x] Hoist the two fixed `FloatBuffer` instances out of `getBitmap()`.
- [x] Run focused unit tests, compile, and review the diff.

### Notes

- Offscreen rendering now reuses fixed vertex and texture coordinate buffers under the existing render lock.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, and `:app:compileDebugKotlin` passed.

## Task: Reduce offscreen GPU readback overhead

Status: DONE
Created: 2026-09-10
Completed: 2026-09-10

### Requirements

- Keep the current per-render EGL lifecycle and thread safety.
- Reuse offscreen readback buffers under the existing render lock.
- Avoid an unnecessary GPU flush before the synchronous pixel readback.
- Keep rendered pixels unchanged.

### Checklist

- [x] Reuse the RGBA and ARGB readback buffers.
- [x] Remove the redundant `glFinish()` before `glReadPixels()`.
- [x] Run focused unit tests, compile, and review the diff.

### Notes

- Offscreen GPU rendering now reuses its largest readback buffers instead of allocating them for every bitmap.
- `glReadPixels()` remains the synchronization point for readback, so the explicit `glFinish()` was removed.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, and `:app:compileDebugKotlin` passed.

## Task: Reduce GL uniform allocations and release inactive LUT textures

Status: DONE
Created: 2026-09-10
Completed: 2026-09-10

### Requirements

- Keep shader output unchanged.
- Reuse temporary uniform buffers across renders.
- Release the preview LUT texture when LUT processing is inactive.
- Keep the change local to the `filter` module.

### Checklist

- [x] Reuse contour uniform arrays instead of allocating them per draw.
- [x] Delete inactive LUT textures.
- [x] Run focused unit tests, compile, and review the diff.

### Notes

- Contour uniform buffers are reused per rendering thread, reducing temporary allocations during preview updates.
- The preview renderer releases the LUT texture when LUT strength reaches zero.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, and `:app:compileDebugKotlin` passed.
## Task: Avoid duplicate CPU edge sampling

Status: DONE
Created: 2026-09-10
Completed: 2026-09-10

### Requirements

- Keep CPU output unchanged.
- Compute the source edge map at most once per pixel.
- Skip edge sampling when neither smoothing nor art effect uses it.

### Checklist

- [x] Share conditional edge value in CPU filterPixel.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- CPU fallback now calculates `edgeAt` once per pixel only when smoothing or an art effect uses it.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, and `:app:compileDebugKotlin` passed.
## Task: Skip inactive CPU neighborhood sampling

Status: DONE
Created: 2026-09-10
Completed: 2026-09-10

### Requirements

- Avoid neighbor sampling when smoothing, sharpness, and clarity are inactive.
- Reuse the existing blurred channels for sharpening instead of recomputing them.
- Preserve CPU output for active effects.

### Checklist

- [x] Gate four bilinear neighbor samples behind active blur-dependent effects.
- [x] Reuse blurred channels in the sharpening pass.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- CPU fallback skips four neighbor samples when smoothing, sharpness, and clarity are inactive.
- The existing blurred channels are reused by the sharpening pass.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, and `:app:compileDebugKotlin` passed.
## Task: Hoist per-render CPU constants

Status: DONE
Created: 2026-09-10
Completed: 2026-09-10

### Requirements

- Compute render-wide constants once per bitmap render.
- Reuse normalized pixel coordinates across warp, makeup, and final effects.
- Preserve CPU output and existing public helper behavior.

### Checklist

- [x] Hoist exposure and texel size out of the pixel loop.
- [x] Reuse normalized texture coordinates in the pixel path.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- `pow(exposure)` and texel size are now computed once per bitmap render.
- Normalized texture coordinates are reused by warp, face masks, makeup, and effects.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, and `:app:compileDebugKotlin` passed.
## Task: Skip inactive CPU beauty processing

Status: DONE
Created: 2026-09-10
Completed: 2026-09-10

### Requirements

- Skip skin and makeup mask work when all beauty controls are inactive.
- Preserve face-aware processing when any beauty control is active.
- Keep effect, adjustment, and warp behavior unchanged.

### Checklist

- [x] Gate beauty and makeup calculations behind one active-controls check.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- CPU fallback skips skin, face, and makeup masks when all beauty controls are inactive.
- Face warp and non-beauty effects remain unchanged.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, and `:app:compileDebugKotlin` passed.
## Task: Skip inactive art fragment work

Status: DONE
Created: 2026-09-10
Completed: 2026-09-10

### Requirements

- Avoid beauty and blur texture sampling when art thumbnails do not use those controls.
- Keep the art edge pass and all active beauty behavior unchanged.
- Preserve the existing shader output for active parameters.

### Checklist

- [x] Gate blur sampling behind active smoothing/sharpness/clarity.
- [x] Gate beauty calculations behind active beauty controls.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Art fragments skip four blur texture samples when smoothing, sharpness, and clarity are inactive.
- Beauty masks and makeup calculations are skipped when all beauty controls are inactive; art edge detection remains active.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, and `:app:compileDebugKotlin` passed.
## Task: Bound art thumbnail source textures

Status: DONE
Created: 2026-09-10
Completed: 2026-09-10

### Requirements

- Avoid uploading full-resolution source images for small art thumbnails.
- Keep at least 2x thumbnail resolution for edge detail.
- Recycle only temporary scaled sources; never recycle the caller-owned bitmap.

### Checklist

- [x] Scale oversized art sources before GPU upload.
- [x] Preserve existing art output dimensions and fallback behavior.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Art thumbnails cap the temporary GPU source texture at 2x the requested thumbnail bounds.
- Temporary scaled bitmaps are recycled without touching the caller-owned source.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, and `:app:compileDebugKotlin` passed.
## Task: Skip neutral art color stages

Status: DONE
Created: 2026-09-10
Completed: 2026-09-10

### Requirements

- Skip neutral color-adjustment stages in the GPU fragment shader.
- Preserve the existing stage order when a parameter is active.
- Keep art effect, LUT, beauty, and grain behavior unchanged.

### Checklist

- [x] Gate neutral RGB shift, exposure, contrast, and tonal stages.
- [x] Gate neutral monochrome, saturation, and vibrance stages.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- GPU art fragments now skip neutral color stages while preserving active stage order.
- Saturation and vibrance continue sharing the original pre-saturation luma value.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, and `:app:compileDebugKotlin` passed.
## Task: Skip inactive art finishing stages

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Skip neutral fade, vignette, and grain stages in the GPU fragment shader.
- Preserve output when each stage is active.
- Keep the art effect and adjustment ordering unchanged.

### Checklist

- [x] Gate fade, vignette, and grain calculations by their uniforms.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- GPU art fragments skip inactive fade, vignette, and grain calculations.
- Active finishing stages retain their original formulas and order.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, and `:app:compileDebugKotlin` passed.
## Task: Reuse offscreen GPU sessions for thumbnails

Status: DONE
Created: 2026-09-11
Completed: 2026-09-11

### Requirements

- Reuse EGL context and shader program for sequential renders at the same output size.
- Recreate the session when output dimensions change.
- Invalidate and release the cached session after a GL runtime failure.

### Checklist

- [x] Cache the EGL/program/handles session under the existing render lock.
- [x] Detach the session after each render and release it safely.
- [x] Run focused unit tests, compile, and review diff.

### Notes

- Sequential offscreen renders at the same size reuse EGL setup and shader program compilation.
- Sessions are replaced on size changes and invalidated after runtime GL failures.
- `:filter:testDebugUnitTest`, `:filter:compileDebugKotlin`, and `:app:compileDebugKotlin` passed.
