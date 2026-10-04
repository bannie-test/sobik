# Kệ Rubik — dữ liệu mẫu

Ứng dụng tự quét thư mục này khi mở "Kệ Rubik". Không cần sửa code để thêm mẫu.

```
rubik/brands/<brand-id>/<loại>/<model-id>/info.json      (tuỳ chọn)
rubik/brands/<brand-id>/<loại>/<model-id>/image.webp     (hoặc image.png / image.jpg, tuỳ chọn)
```

- `<brand-id>`: id hãng trong `content/src/main/resources/content/puzzles.json` (ví dụ `moyu`, `gan`, `x-man-design`).
  Lọc theo hãng sẽ lấy mọi mẫu trong thư mục hãng đó và các thương hiệu con của nó (ví dụ `moyu` gồm cả `pbcube`).
- `<loại>`: `2x2`, `3x3`, `4x4`, `5x5`, `6x6`, `7x7`, `pyraminx`, `megaminx`, `skewb`, `square-1`, `clock`.
- `<model-id>`: chữ thường, nối bằng gạch ngang, ví dụ `moyu-wrm-v11`. Nếu không có `info.json`, tên hiển thị lấy từ tên thư mục.
- Ảnh: tỉ lệ 4:3, khuyến nghị **800×600**, nền trắng hoặc trong suốt, định dạng WebP (nhẹ nhất). Thiếu ảnh thì app hiện ảnh chờ.

`info.json` (mọi trường đều tuỳ chọn):

```json
{
  "name": "MoYu WeiLong WRM V11",
  "tags": ["Mới", "Flagship", "Maglev", "UV"],
  "features": ["..."],
  "reviews": ["..."],
  "audience": "Người chơi thi đấu",
  "image": "image.webp"
}
```

Nhớ ghi nguồn ảnh/thông tin trong `sources` của `puzzles.json` — danh sách nguồn hiển thị cuối trang Kệ Rubik.
