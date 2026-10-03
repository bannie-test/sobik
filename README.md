# Sobik — Rubik Solver cho Android

Ứng dụng Android native (Kotlin + Jetpack Compose + CameraX) quét và giải Rubik 2x2 / 3x3 hoàn toàn
offline, kèm hướng dẫn người mới, thư viện công thức và kiến thức cho 4x4–7x7.

## Luồng sử dụng

```
Chọn 2x2/3x3 → chọn chế độ quét (Thủ công / Hẹn giờ / Tự động)
  → camera hướng dẫn từng mặt (F, R, B, L, U, D) → xem trước màu
  → đủ 6 mặt: phân loại màu toàn cục + độ tin cậy từng ô
  → Validation → (lỗi) màn hình sửa màu: chạm ô, gợi ý sửa, quét lại 1 mặt
  → hợp lệ → lưu CubeState → Solve All / Cross / Next F2L / OLL / PLL
  → lời giải + mô hình 3D: Next / Previous / Play / Pause
```

## Cài đặt ứng dụng vào điện thoại

Yêu cầu: điện thoại Android 7.0 (API 24) trở lên, có camera sau. Ứng dụng chạy hoàn toàn offline.

### Cách 1 — Tải APK từ GitHub Actions (không cần cài công cụ)

1. Mở repository trên GitHub → tab **Actions** → chọn workflow **build** → chọn lần chạy mới nhất có dấu ✓ xanh.
2. Kéo xuống mục **Artifacts**, tải **sobik-debug-apk** (file `.zip`, cần đăng nhập GitHub).
3. Giải nén để lấy file `app-debug.apk`, rồi chép file vào điện thoại (cáp USB, Google Drive, Zalo, email...).
4. Trên điện thoại, mở file `app-debug.apk` bằng ứng dụng Files / Trình quản lý tệp.
5. Nếu Android báo chặn cài ứng dụng không rõ nguồn gốc: bấm **Cài đặt (Settings)** → bật
   **Cho phép từ nguồn này (Allow from this source)** cho ứng dụng bạn dùng để mở file → quay lại và bấm **Cài đặt**.
   - Nếu Google Play Protect cảnh báo, chọn **Vẫn cài đặt (Install anyway)** — đây là bản debug tự build nên chưa được Google xác minh.
6. Mở ứng dụng **Sobik**, cấp quyền **Camera** khi được hỏi (chỉ cần cho chức năng quét).

### Cách 2 — Cài bằng máy tính qua USB (adb)

1. Trên điện thoại: **Cài đặt → Thông tin điện thoại → bấm 7 lần vào "Số hiệu bản tạo (Build number)"** để bật
   Tùy chọn nhà phát triển, sau đó vào **Tùy chọn nhà phát triển → bật Gỡ lỗi USB (USB debugging)**.
2. Cắm điện thoại vào máy tính, chọn **Cho phép (Allow)** khi điện thoại hỏi về gỡ lỗi USB.
3. Trên máy tính đã cài Android SDK Platform Tools:

   ```bash
   adb devices                          # kiểm tra điện thoại đã được nhận
   adb install -r app-debug.apk         # cài (hoặc cập nhật) ứng dụng
   ```

### Cách 3 — Build từ mã nguồn

Cần JDK 17 và Android SDK (compileSdk 35), cách đơn giản nhất là cài **Android Studio**.

- **Android Studio**: *File → Open* thư mục dự án, chờ Gradle sync xong, cắm điện thoại (đã bật Gỡ lỗi USB),
  chọn cấu hình **app** và bấm **Run ▶**.
- **Dòng lệnh**:

  ```bash
  ./gradlew :app:installDebug                   # build và cài thẳng lên điện thoại đang cắm
  # hoặc chỉ build APK:
  ./gradlew :app:assembleDebug                  # -> app/build/outputs/apk/debug/app-debug.apk
  ```

### Gỡ cài đặt / cập nhật

- Cập nhật: cài đè file APK mới (cùng chữ ký debug của cùng một máy build). Nếu báo xung đột chữ ký, gỡ bản cũ trước.
- Gỡ: giữ biểu tượng Sobik → **Gỡ cài đặt**, hoặc `adb uninstall com.sobik.app`.

> Ghi chú: APK debug dùng để thử nghiệm. Để phát hành (Google Play), cần build bản `release` ký bằng khóa riêng
> (`./gradlew :app:bundleRelease` kèm cấu hình `signingConfigs`).

## Kiến trúc module

| Module | Loại | Vai trò |
|---|---|---|
| `core-model` | Kotlin/JVM | `CubeType`, `CubeState`, `Face`, `StickerColor`, `Move`/`Notation`, `Algorithm`, `AlgorithmCase`, `Solution`, `ScanResult`, `ValidationResult`, `BeginnerLesson`, interface `CubeSolver` |
| `cube-engine` | Kotlin/JVM | Hình học NxN (hoán vị sticker cho mọi loại move suy ra từ toạ độ 3D), mô hình cubie (CP/CO/EP/EO theo quy ước Kociemba), `CubeValidator` + gợi ý sửa |
| `solver-2x2` | Kotlin/JVM | IDA* tối ưu (cố định góc DBL, chỉ U/R/F), bảng < 6 KB, ~1 ms/lời giải, ≤ 11 bước |
| `solver-3x3` | Kotlin/JVM | `kociemba/`: Two-Phase tự cài đặt (Computational Solver). `method/`: CFOP (Human Method Solver: Cross tối ưu, F2L, OLL, PLL dựa trên thư viện công thức) |
| `scanner` | Kotlin/JVM | Lấy mẫu lưới trong khung hướng dẫn từ YUV, LAB, hiệu chuẩn theo tâm, gán màu cân bằng (Hungarian) + confidence, `ScanSession`, chiến lược chụp Manual/Timed/Auto |
| `scanner-camerax` | Android | Analyzer CameraX (chỉ đọc điểm mẫu, không tạo Bitmap), preview, xin quyền camera |
| `visualization` | Android/Compose | Renderer 3D phần mềm trên Canvas (2x2–7x7, animation từng layer), sơ đồ 2D chạm để sửa, trình phát lời giải |
| `content` | Kotlin/JVM | JSON: `algorithms_3x3.json` (9 beginner, 41 F2L, 57 OLL, 21 PLL), `beginner_3x3.json` (8 bài), `big_cubes.json` (4x4–7x7) |
| `data` | Kotlin/JVM | Lưu CubeState đã hợp lệ (file văn bản, ~70 byte/cube) |
| `app` | Android | UI Compose, ViewModel, điều hướng, DI thủ công |

Quy tắc phụ thuộc: UI không chứa logic solver; scanner không biết UI; solver chỉ nhận `CubeState` và trả
`Solution`; validation độc lập với scanner; thư viện công thức là dữ liệu, được *tiêm* vào CFOP solver;
visualization không phụ thuộc solver.

## Quyết định kỹ thuật

- **Không dùng OpenCV ở bản đầu.** Người dùng đặt mặt cube vào khung lưới cố định, nên không cần phát
  hiện contour; mỗi frame chỉ đọc ~450 điểm ảnh YUV ở độ phân giải ~640x480. Không tăng kích thước APK,
  không giữ Bitmap. Có thể thêm bộ phát hiện mặt bằng OpenCV sau này qua `PixelSource`/`FaceSampler`.
- **Phân loại màu**: CIE LAB (giảm trọng số độ sáng), tâm 3x3 làm màu tham chiếu, ràng buộc mỗi màu đúng
  n² ô (gán tối ưu). 2x2 không có tâm → k-means cân bằng. Ô có biên độ phân biệt thấp bị đánh dấu
  confidence thấp để người dùng xác nhận thay vì quét lại.
- **Validation trả lỗi cụ thể**: `INVALID_COLOR_COUNT`, `INVALID_CENTER_MAPPING`, `INVALID_PIECE`,
  `DUPLICATE_PIECE`, `INVALID_CORNER_ORIENTATION`, `INVALID_EDGE_ORIENTATION`,
  `INVALID_PERMUTATION_PARITY`, `INVALID_FACE_ORIENTATION`, `INVALID_CUBE_STATE`… kèm gợi ý sửa tối
  thiểu (một ô bị đọc sai, hai ô đọc nhầm cho nhau, một mặt bị quét xoay).
- **Solver tự viết** (không chép mã bên ngoài → không có vấn đề license). Two-Phase dùng bảng pruning nén
  4 bit (~4 MB tổng), xây lười trong lúc người dùng đang quét. Trung bình ~20 bước.
- **Short vs Human Friendly**: Solve All trả cả lời giải Kociemba và lời giải CFOP (có xoay y/z2 khi cần).
  Có thể cắm solver tính toán khác (IDA*/pattern database) qua tham số `computational` của
  `ThreeByThreeSolver`.
- **Nhận diện case được suy ra từ chính công thức** (áp dụng nghịch đảo công thức lên cube đã giải), nên
  hình case và nhận diện không bao giờ lệch với dữ liệu. Unit test đảm bảo thư viện phủ đủ 41 case F2L,
  mọi mẫu OLL và mọi hoán vị PLL, và mỗi công thức thay thế giải cùng case.

## Build & test

```bash
./gradlew -PjvmOnly=true test     # toàn bộ test engine/solver/scanner/content (không cần Android SDK)
./gradlew :app:assembleDebug      # cần Android SDK (compileSdk 35)
```

CI (`.github/workflows/build.yml`) chạy cả hai và đính kèm APK debug.

## Mở rộng sau này

- Scanner/solver 4x4+: thêm `CubeType.supportsScan/Solver`, đăng ký solver trong `AppContainer.solverFor`;
  engine hình học và renderer đã hỗ trợ NxN.
- Chế độ quét mới: cài `CaptureStrategy` và thêm vào `ScanMode`.
- Nội dung bài học/công thức: sửa JSON trong `content/src/main/resources/content` (đã có test kiểm chứng).
