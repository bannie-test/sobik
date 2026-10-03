# Brick Art Puzzle — Kế hoạch sản phẩm & kỹ thuật

> Ứng dụng web (responsive, ưu tiên mobile) biến ảnh thành tranh ghép gạch kiểu "LEGO Art"
> (mosaic từ các nút tròn), cắt thành N mảnh (mặc định 36) để người chơi ghép lại, có bấm giờ,
> thử thách đua tốc độ, câu đố sau khi ghép xong, 3 độ khó, thư viện 20 tranh Phục Hưng,
> đăng nhập để tải ảnh riêng và chia sẻ thành tích lên mạng xã hội.

Trạng thái: **bản kế hoạch** — chưa có code. Các mục ⚠️ là quyết định/rủi ro cần chốt (xem §13).

---

## 1. Diễn giải yêu cầu & các giả định

| Yêu cầu gốc | Diễn giải trong kế hoạch |
|---|---|
| "Chuyển ảnh thành tranh dạng lego art" | Thu nhỏ ảnh về lưới **nút gạch** (vd 48×48 nút như bộ LEGO Art), ánh xạ mỗi nút về **bảng màu gạch giới hạn** (~24–40 màu), vẽ mỗi nút thành viên gạch tròn có bóng đổ. |
| "Tách thành số mảnh theo yêu cầu, default 36" | Tranh mosaic được cắt thành lưới **mảnh vuông** r×c (36 = 6×6). Mảnh vuông hợp với phong cách gạch hơn mảnh jigsaw có ngàm, và thao tác trên điện thoại dễ hơn. |
| "Thử thách bấm giờ xem ai ghép nhanh hơn" | (1) Kỷ lục cá nhân + bảng xếp hạng; (2) **link thách đấu** — mọi người ghép cùng một bộ xáo trộn (cùng seed) rồi so thời gian; (3) giai đoạn sau: **phòng đua realtime**. |
| "Quiz sau khi ghép xong" | Ghép xong → 3 câu hỏi trắc nghiệm về bức tranh; trả lời đúng được cộng điểm thưởng. |
| "Mode dễ / khó / siêu khó" | Khác nhau ở: xoay mảnh, ảnh mờ gợi ý, nam châm hút đúng chỗ, quyền xem ảnh gốc, phản hồi đúng/sai (§5). |
| "Option hiện ảnh gốc song song" | Tuỳ chọn hiển thị ảnh gốc cạnh bàn ghép: dọc → dải thu gọn phía trên; ngang/tablet → chia đôi màn hình. |
| "Thư viện cố định 20 ảnh Phục Hưng" | Danh sách tuyển chọn tác phẩm thuộc phạm vi công cộng (public domain), kèm metadata + bộ câu hỏi (§7). |
| "Đăng nhập, tải ảnh riêng, chia sẻ" | Đăng nhập Google/Facebook/email; ảnh riêng tư mặc định; chia sẻ ảnh thành tích qua Web Share API + trang chia sẻ công khai có ảnh xem trước (§9). |

⚠️ **Tên thương hiệu**: "LEGO" là nhãn hiệu đã đăng ký. Không dùng "LEGO" trong tên app, logo, tên miền
hay quảng cáo; dùng các từ như *brick mosaic*, *tranh gạch*, *Brick Art*. Bảng màu chỉ "lấy cảm hứng",
không gọi là "màu LEGO chính hãng".

---

## 2. Phạm vi theo giai đoạn

### MVP (Giai đoạn 1) — chơi không cần đăng nhập
- Thư viện 20 tranh Phục Hưng (đã tiền xử lý sẵn).
- Tải ảnh từ máy (xử lý hoàn toàn trên trình duyệt, không upload) — chơi ở chế độ khách.
- Bộ tạo mosaic: chọn độ chi tiết, bật/tắt dithering, xem trước.
- Chọn số mảnh: 9 / 16 / **36** / 64 / 100 / 144 (mobile gợi ý tối đa 100).
- 3 chế độ Dễ / Khó / Siêu khó + tuỳ chọn hiện ảnh gốc.
- Đồng hồ bấm giờ, đếm số lần di chuyển, số lần xem ảnh gốc.
- Quiz sau khi ghép (tranh thư viện).
- Màn hình kết quả + tạo ảnh thành tích + chia sẻ (Web Share API / tải về).
- Kỷ lục cá nhân lưu cục bộ (localStorage / IndexedDB).
- PWA cơ bản: cài lên màn hình chính, chơi offline tranh thư viện.

### Giai đoạn 2 — tài khoản & cạnh tranh
- Đăng nhập (Google, Facebook, email magic link); gộp tiến trình chơi khách vào tài khoản.
- Lưu ảnh riêng lên cloud, "Bộ sưu tập của tôi".
- Bảng xếp hạng theo (tranh × số mảnh × chế độ), toàn cầu / bạn bè / tuần.
- **Link thách đấu**: tạo thử thách cố định cấu hình + seed, gửi bạn bè, xem bảng thời gian.
- Câu hỏi tự viết cho ảnh riêng (người tạo thử thách tự đặt câu đố).
- Trang chia sẻ công khai `/s/{id}` với ảnh OG.

### Giai đoạn 3 — mở rộng
- Phòng đua realtime 2–8 người (đếm ngược chung, thanh tiến độ trực tiếp).
- Quiz tự sinh cho ảnh riêng bằng AI thị giác (tuỳ chọn, cần người dùng đồng ý).
- Thành tựu/huy hiệu, chuỗi ngày chơi, "Tranh của ngày".
- Song ngữ Việt/Anh, xuất "danh sách gạch" để lắp tranh thật.

---

## 3. Luồng người dùng

```
Trang chủ
 ├─ Thư viện Phục Hưng ─┐
 ├─ Tải ảnh của tôi ────┤→ (ảnh riêng) Cắt khung 1:1 / 4:5 / 3:4
 └─ Nhập mã thách đấu ──┤
                        ▼
              Thiết lập ván chơi
              • Số mảnh (mặc định 36)  • Chế độ Dễ/Khó/Siêu khó
              • Độ chi tiết mosaic     • Hiện ảnh gốc: Luôn hiện / Nhấn giữ / Tắt
              • Xem trước tranh gạch
                        ▼
              Đếm ngược 3-2-1 → Bàn ghép (đồng hồ chạy)
                        ▼
              Hoàn thành → hiệu ứng "lắp gạch" → Quiz (3 câu, mỗi câu 15 giây)
                        ▼
              Kết quả: thời gian, số bước, điểm quiz, tổng điểm, hạng
              [Chia sẻ]  [Thách đấu bạn bè]  [Chơi lại]  [Tranh khác]
```

---

## 4. Bộ tạo tranh gạch (mosaic engine)

Chạy trong **Web Worker** + `OffscreenCanvas` để không đơ giao diện; thuần TypeScript, có unit test.

1. **Đọc & chuẩn hoá ảnh**: `createImageBitmap` (tự xử lý xoay EXIF), giới hạn cạnh dài 2048 px.
2. **Cắt khung** theo tỉ lệ lưới (mặc định 1:1 như LEGO Art; cho phép 4:5, 3:4, 16:9).
3. **Thu nhỏ về lưới nút** W×H (độ chi tiết: 32 / **48** / 64 / 96 nút cạnh dài) bằng lấy trung bình
   vùng trong **không gian RGB tuyến tính** (tránh tối màu do gamma).
4. **Tinh chỉnh tuỳ chọn**: độ tương phản, độ bão hoà, độ sáng (thanh trượt, xem trước ngay).
5. **Lượng tử hoá màu**: chuyển sang CIELAB, chọn màu gần nhất trong bảng màu theo ΔE (CIE76 cho
   nhanh; CIEDE2000 nếu cần chính xác hơn).
6. **Dithering tuỳ chọn** (Floyd–Steinberg trong Lab) — đẹp hơn với ảnh chân dung, chuyển màu mượt.
7. **Kết quả lưu dạng mảng chỉ số màu** (`Uint8Array` W×H + id bảng màu). Tranh 48×48 chỉ ~2,3 KB
   → lưu/chia sẻ/đồng bộ cực nhẹ, vẽ lại ở mọi độ phân giải.
8. **Vẽ**: mỗi nút = hình vuông màu nền + hình tròn nổi (gradient sáng phía trên-trái, bóng phía dưới-phải).
   Vẽ sẵn 1 "sprite nút" cho mỗi màu rồi `drawImage` hàng loạt → nhanh.
9. **Thống kê gạch**: đếm số nút mỗi màu → hiển thị "danh sách gạch" (vui, và hữu ích nếu muốn lắp thật).

**Bảng màu**: 1 bảng mặc định ~30 màu + vài bảng chủ đề (Đơn sắc, Sepia, Pop art). Định nghĩa trong
JSON `{ id, name, hex }`.

**Quan hệ nút ↔ mảnh**: lưới mảnh r×c phải chia hết lưới nút. Với tranh vuông: 36 mảnh (6×6) × 8×8 nút
= 48×48 nút. Khi người chơi đổi số mảnh, độ chi tiết được làm tròn về bội số gần nhất
(vd 64 mảnh = 8×8 → 48 nút giữ nguyên, mỗi mảnh 6×6 nút; 100 mảnh = 10×10 → 50 nút).

Với ảnh không vuông: `c = round(sqrt(N × tỉ_lệ))`, `r = round(N / c)`; hiển thị số mảnh thực tế (vd 35 hoặc 36).

**Tranh thư viện được tiền xử lý lúc build** (script Node + `sharp`): sinh mảng chỉ số màu, ảnh gốc
WebP nhiều kích cỡ, thumbnail → không tốn CPU trên máy người chơi.

---

## 5. Gameplay

### 5.1 Mô hình bàn ghép (tối ưu cho cảm ứng)
- **Bàn ghép** = lưới ô r×c (ô trống có viền mờ) + **khay mảnh** cuộn ngang ở đáy màn hình.
- Kéo mảnh từ khay vào ô; thả vào ô đã có mảnh → **đổi chỗ**; kéo từ ô ra khay → gỡ mảnh.
- Chạm 1 lần vào mảnh (chế độ có xoay) → xoay 90°.
- Pinch-zoom & kéo bàn khi số mảnh lớn; nút "vừa màn hình".
- Rung nhẹ (`navigator.vibrate`) khi mảnh khớp; âm thanh "tách" có thể tắt.
- Xáo trộn **xác định theo seed** (PRNG mulberry32) → cùng seed = cùng thứ tự & góc xoay cho mọi người
  (nền tảng cho thử thách công bằng).
- Lưu tạm ván đang chơi vào IndexedDB → đóng app mở lại vẫn chơi tiếp (đồng hồ tạm dừng; với ván xếp hạng thì
  khi app ẩn quá 30 giây ván bị đánh dấu "không xếp hạng").

Kích thước mảnh trên điện thoại 360 px: 36 mảnh ≈ 58 px/mảnh (thoải mái), 100 mảnh ≈ 34 px (cần zoom),
144 mảnh ≈ 28 px → chỉ khuyến nghị cho tablet/desktop.

### 5.2 Các chế độ chơi

| | **Dễ** | **Khó** | **Siêu khó** |
|---|---|---|---|
| Xoay mảnh | Không | Có (0/90/180/270°) | Có |
| Ảnh mờ gợi ý trên bàn | Có (độ mờ 25%) | Không | Không |
| Đường lưới trên bàn | Có | Có | Không (chỉ khung ngoài) |
| Nam châm/khoá mảnh đúng | Mảnh đúng tự khoá, viền xanh | Mảnh đúng tự khoá, không báo hiệu | Không khoá, không báo — chỉ kiểm tra khi đầy bàn |
| Thứ tự khay | Mảnh viền lên trước | Ngẫu nhiên | Ngẫu nhiên |
| Ảnh gốc | Luôn hiện / Nhấn giữ / Tắt | Nhấn giữ (+5 giây phạt mỗi lần) / Tắt | Tắt (tuỳ chọn: tối đa 1 lần xem 3 giây, +15 giây) |
| Gợi ý "đặt hộ 1 mảnh" | Không giới hạn (+3 giây) | 3 lần (+10 giây) | Không |
| Hệ số điểm | ×1 | ×2 | ×3 |

Ván dùng tuỳ chọn "Luôn hiện ảnh gốc" được xếp hạng ở bảng riêng (hoặc gắn nhãn 👁) để công bằng.

### 5.3 Hiển thị ảnh gốc song song
- **Điện thoại dọc**: dải ảnh gốc thu gọn phía trên bàn ghép (chạm để phóng to/thu nhỏ); chế độ "Nhấn giữ"
  = nút con mắt, giữ để xem, thả là ẩn.
- **Ngang / tablet / desktop**: chia đôi màn hình — trái ảnh gốc (chọn hiện ảnh thật hoặc bản gạch), phải bàn ghép.
- Có lựa chọn hiển thị **ảnh chụp gốc** hay **bản mosaic** làm tham chiếu (mosaic dễ hơn).

### 5.4 Tính điểm
```
điểm_ghép = max(0, 10 000 − thời_gian_giây × 10) × hệ_số_chế_độ × (số_mảnh / 36)
điểm_quiz = số_câu_đúng × 500 (+ thưởng tốc độ trả lời tối đa 200/câu)
tổng = điểm_ghép + điểm_quiz
```
Bảng xếp hạng sắp theo **thời gian** (tiêu chí chính của thử thách), điểm tổng dùng cho huy hiệu/thống kê.
Các con số trên là khởi điểm, sẽ cân chỉnh sau khi chơi thử.

---

## 6. Thử thách "ai ghép nhanh hơn"

### 6.1 Thách đấu qua link (Giai đoạn 2)
- Người chơi bấm "Thách đấu" sau khi ghép → server tạo `challenge` gồm: tranh, số mảnh, chế độ, seed, câu đố
  (với ảnh riêng: người tạo có thể viết câu hỏi), hạn chót (mặc định 7 ngày).
- Link dạng `https://<domain>/c/AB12CD` + mã 6 ký tự để nhập tay. Gửi qua Zalo/Messenger/...
- Người nhận mở link → (đăng nhập hoặc nhập biệt danh) → chơi đúng cấu hình đó → bảng thời gian của thử thách.

### 6.2 Phòng đua realtime (Giai đoạn 3)
- Chủ phòng tạo phòng, 2–8 người vào bằng mã; server phát thời điểm bắt đầu chung → đếm ngược đồng bộ.
- Mỗi người phát tiến độ (% mảnh đúng) qua kênh realtime; ai xong trước thắng, sau đó cùng làm quiz.

### 6.3 Chống gian lận (mức hợp lý, không tuyệt đối)
- Seed và thời điểm bắt đầu do **server cấp** khi tải ván xếp hạng; kết thúc gửi kèm **nhật ký nước đi**
  (mảnh, ô, góc, mốc thời gian).
- Server phát lại nhật ký để xác nhận bàn ghép hoàn chỉnh và thời gian khớp với đồng hồ server (sai lệch < vài giây).
- Loại kết quả bất khả thi (vd < 0,4 giây/mảnh), giới hạn tần suất gửi, đánh dấu kết quả nghi vấn để duyệt.

---

## 7. Thư viện 20 tranh Phục Hưng (đề xuất)

Tiêu chí: nổi tiếng, đa dạng hoạ sĩ & vùng (Ý + Bắc Âu), màu sắc/bố cục khác nhau để độ khó đa dạng,
tác phẩm thuộc phạm vi công cộng. Nguồn ảnh ưu tiên: kho mở của bảo tàng (NGA Washington, Rijksmuseum,
Met — CC0) và Wikimedia Commons.

| # | Tác phẩm | Hoạ sĩ | Năm | Nơi lưu giữ | Độ khó gợi ý |
|---|---|---|---|---|---|
| 1 | Mona Lisa | Leonardo da Vinci | ~1503–1519 | Louvre, Paris | Trung bình |
| 2 | Bữa tiệc ly (The Last Supper) | Leonardo da Vinci | 1495–1498 | Santa Maria delle Grazie, Milan ⚠️ | Khó |
| 3 | Người đàn bà bế chồn (Lady with an Ermine) | Leonardo da Vinci | ~1489–1491 | Bảo tàng Czartoryski, Kraków | Dễ |
| 4 | Ginevra de' Benci | Leonardo da Vinci | ~1474–1478 | National Gallery of Art, Washington | Dễ |
| 5 | Sự ra đời của thần Vệ Nữ | Sandro Botticelli | ~1484–1486 | Uffizi, Florence ⚠️ | Trung bình |
| 6 | Mùa xuân (Primavera) | Sandro Botticelli | ~1480 | Uffizi, Florence ⚠️ | Khó |
| 7 | Sự sáng tạo ra Adam | Michelangelo | ~1508–1512 | Nhà nguyện Sistine, Vatican ⚠️ | Dễ |
| 8 | Trường học Athens | Raphael | 1509–1511 | Bảo tàng Vatican ⚠️ | Khó |
| 9 | Đức Mẹ Sistine | Raphael | 1512 | Gemäldegalerie Alte Meister, Dresden | Trung bình |
| 10 | Thần Vệ Nữ xứ Urbino | Titian | 1538 | Uffizi, Florence ⚠️ | Trung bình |
| 11 | Bacchus và Ariadne | Titian | 1520–1523 | National Gallery, London | Khó |
| 12 | Cơn giông (The Tempest) | Giorgione | ~1506–1508 | Gallerie dell'Accademia, Venice ⚠️ | Trung bình |
| 13 | Lễ rửa tội của Chúa Kitô | Piero della Francesca | ~1450 | National Gallery, London | Trung bình |
| 14 | Chân dung vợ chồng Arnolfini | Jan van Eyck | 1434 | National Gallery, London | Trung bình |
| 15 | Khu vườn khoái lạc trần gian | Hieronymus Bosch | ~1490–1510 | Prado, Madrid | Siêu khó |
| 16 | Những người thợ săn trong tuyết | Pieter Bruegel Cha | 1565 | Kunsthistorisches Museum, Vienna | Khó |
| 17 | Tháp Babel | Pieter Bruegel Cha | 1563 | Kunsthistorisches Museum, Vienna | Khó |
| 18 | Hai sứ thần (The Ambassadors) | Hans Holbein Con | 1533 | National Gallery, London | Khó |
| 19 | Chân dung tự hoạ năm 1500 | Albrecht Dürer | 1500 | Alte Pinakothek, Munich | Dễ |
| 20 | Chân dung Baldassare Castiglione | Raphael | ~1514–1515 | Louvre, Paris | Dễ |

⚠️ **Bản quyền ảnh chụp**: bản thân các tác phẩm đã hết bảo hộ. Tuy nhiên **Ý** (Bộ luật Di sản văn hoá
— "Codice Urbani", điều 107–108) yêu cầu xin phép/trả phí khi dùng ảnh tác phẩm thuộc bảo tàng nhà nước Ý
cho **mục đích thương mại**; Vatican cũng có chính sách riêng. Nếu app có doanh thu (quảng cáo, gói trả phí),
cần tham vấn pháp lý hoặc thay các tranh có ⚠️ bằng tranh ở bảo tàng có kho mở, ví dụ: *Đức Mẹ Cowper nhỏ*
(Raphael, NGA), *Bữa tiệc của các vị thần* (Bellini & Titian, NGA), *Chân dung một thiếu nữ*
(Petrus Christus, Gemäldegalerie Berlin), *Thánh Giêrônimô trong thư phòng* (Antonello da Messina, National Gallery London).

### Dữ liệu mỗi tranh
```json
{
  "id": "mona-lisa",
  "title": { "vi": "Mona Lisa", "en": "Mona Lisa" },
  "artist": "Leonardo da Vinci",
  "year": "c. 1503–1519",
  "museum": "Musée du Louvre, Paris",
  "source": { "url": "...", "license": "Public domain", "credit": "..." },
  "crop": { "aspect": "4:5", "x": 0.0, "y": 0.05, "w": 1.0, "h": 0.8 },
  "suggestedDifficulty": "medium",
  "funFact": { "vi": "...", "en": "..." },
  "quiz": [ /* 6–8 câu, mỗi ván rút ngẫu nhiên 3 */ ]
}
```

---

## 8. Quiz

### 8.1 Định dạng
- Trắc nghiệm 4 đáp án, 15 giây/câu, 3 câu/ván rút ngẫu nhiên từ ngân hàng 6–8 câu/tranh (tổng ~140 câu).
- Sau mỗi câu hiện đáp án đúng + 1 câu giải thích ngắn (học được kiến thức nghệ thuật).
- Thể loại câu hỏi: hoạ sĩ, niên đại, nơi lưu giữ, nhân vật/biểu tượng trong tranh, chất liệu, chuyện thú vị,
  và **câu hỏi quan sát** ("Trong tranh có bao nhiêu nhân vật?", "Vật gì nằm ở dưới chân hai sứ thần?").

```json
{
  "id": "ambassadors-skull",
  "type": "single_choice",
  "question": { "vi": "Hình méo kỳ lạ ở dưới tranh 'Hai sứ thần' là gì?" },
  "options": [ { "vi": "Một chiếc đầu lâu" }, { "vi": "Một cây đàn luýt" },
               { "vi": "Một tấm thảm" }, { "vi": "Một quả địa cầu" } ],
  "answer": 0,
  "explain": { "vi": "Đầu lâu vẽ theo phép phối cảnh méo (anamorphosis), chỉ nhìn đúng khi đứng lệch sang phải bức tranh." },
  "difficulty": 2
}
```

Ví dụ thêm (sẽ soạn đầy đủ cho cả 20 tranh):
- *Mona Lisa* — Hiện tranh được lưu giữ ở đâu? → Louvre. / Tranh vẽ trên chất liệu gì? → Gỗ dương.
- *Sự sáng tạo ra Adam* — Tranh nằm ở phần nào của nhà nguyện Sistine? → Trần nhà.
- *Những người thợ săn trong tuyết* — Tranh thuộc loạt tranh về chủ đề gì? → Các mùa trong năm.

### 8.2 Quiz cho ảnh riêng của người dùng
1. **Quiz trí nhớ tự sinh** (MVP, không cần AI): "Màu gạch nào nhiều nhất?", "Mảnh này thuộc góc nào?",
   "Tranh có khoảng bao nhiêu màu?" — sinh từ dữ liệu mosaic.
2. **Câu hỏi do người tạo thử thách tự viết** (Giai đoạn 2) — hợp để thách đố bạn bè về ảnh của mình.
3. **AI sinh câu hỏi từ ảnh** (Giai đoạn 3, tuỳ chọn, người dùng phải đồng ý gửi ảnh).

---

## 9. Tài khoản, ảnh riêng & chia sẻ

### 9.1 Đăng nhập
- Google, Facebook, email magic link (Apple chỉ cần nếu sau này làm app iOS).
- Chế độ khách đầy đủ tính năng chơi; đăng nhập để: lưu ảnh lên cloud, xếp hạng, tạo thử thách.
- Khi đăng nhập lần đầu: gộp kỷ lục/ván chơi cục bộ vào tài khoản.

### 9.2 Ảnh riêng
- Xử lý mosaic trên máy; khi lưu lên cloud: ảnh gốc thu về ≤ 2048 px, chuyển WebP, **xoá EXIF (vị trí GPS)**.
- Mặc định **riêng tư**; chỉ trở thành công khai khi người dùng chủ động chia sẻ/tạo thử thách.
- Giới hạn: tối đa 10 MB/ảnh đầu vào, 50 ảnh/tài khoản miễn phí (điều chỉnh sau).
- Kiểm duyệt nội dung (bộ lọc ảnh nhạy cảm) trước khi ảnh xuất hiện trên trang chia sẻ công khai; nút báo cáo.
- Người dùng xoá được ảnh và xoá tài khoản (xoá toàn bộ dữ liệu).

### 9.3 Chia sẻ
- **Ảnh thành tích** dựng trên canvas, khổ 1080×1350 (hợp Instagram/Facebook) và 1080×1920 (Story/TikTok):
  tranh gạch, tên tranh, thời gian, chế độ, số mảnh, điểm quiz, tên người chơi, link/QR.
- Điện thoại: `navigator.share({ files: [ảnh], text, url })` → mở bảng chia sẻ hệ thống
  (Facebook, Messenger, Zalo, Instagram, TikTok…).
- Dự phòng (desktop/trình duyệt không hỗ trợ): tải ảnh PNG + nút Facebook sharer, X intent,
  Zalo share, sao chép link.
- **Trang chia sẻ công khai** `/s/{id}`: hiển thị tranh + thành tích + nút "Thử ghép tranh này"
  (dẫn vào cùng thử thách). Thẻ Open Graph với ảnh OG sinh phía server → link hiện ảnh đẹp trên Facebook/Zalo.
- Instagram không có URL chia sẻ cho web → chỉ dùng Web Share API hoặc tải ảnh.

---

## 10. Kiến trúc kỹ thuật (đề xuất)

| Lớp | Lựa chọn | Lý do |
|---|---|---|
| Frontend | **Next.js (React) + TypeScript**, Tailwind CSS | SSR cho trang chia sẻ/OG, PWA, hệ sinh thái lớn |
| Bàn ghép | Canvas 2D (hoặc PixiJS nếu > 100 mảnh giật) + Pointer Events | Mượt trên mobile, kéo thả/zoom tự kiểm soát |
| Xử lý ảnh | Web Worker + OffscreenCanvas, thuần TS | Không đơ UI, chạy offline, không cần upload |
| State | Zustand; IndexedDB (idb-keyval) cho ván đang chơi | Nhẹ, đơn giản |
| Backend | **Supabase**: Auth, Postgres, Storage, Realtime, Edge Functions | Một dịch vụ cho đủ nhu cầu, có gói miễn phí, RLS bảo vệ dữ liệu |
| Ảnh OG | `@vercel/og` (Satori) | Sinh ảnh xem trước cho link chia sẻ |
| Hosting | Vercel (frontend) + Supabase | Triển khai nhanh, CDN |
| Tiền xử lý thư viện | Script Node + `sharp` chạy lúc build | Tranh thư viện tải tức thì |
| Kiểm thử | Vitest (engine, chấm điểm, xác thực nhật ký), Playwright (viewport iPhone/Android) | |
| Theo dõi | Sentry (lỗi), Plausible/PostHog (phân tích ẩn danh) | |

### 10.1 Mô hình dữ liệu (Postgres)
```
profiles        (id, display_name, avatar_url, created_at)
artworks        (id, owner_id NULL=thư viện, kind[library|user], title, artist, year, museum,
                 source_url, license, image_path, aspect, visibility[private|unlisted|public], created_at)
mosaics         (id, artwork_id, palette_id, width, height, dithering, indices BYTEA, created_at)
quiz_questions  (id, artwork_id, author_id, locale, payload JSONB, difficulty)
challenges      (id, code, creator_id, mosaic_id, pieces, mode, seed, preview_policy,
                 quiz_ids[], expires_at, created_at)
attempts        (id, user_id, mosaic_id, challenge_id NULL, pieces, mode, seed, preview_policy,
                 server_started_at, server_finished_at, duration_ms, moves, peeks, hints,
                 quiz_correct, score, move_log JSONB, status[valid|flagged|unranked])
shares          (id, attempt_id, image_path, created_at)
rooms / room_players  (Giai đoạn 3)
```
Bảng xếp hạng = view/materialized view trên `attempts` theo (mosaic, pieces, mode, preview_policy).
Row Level Security: người dùng chỉ đọc/ghi ảnh & ván của mình; dữ liệu công khai chỉ qua view.

### 10.2 Cấu trúc thư mục
```
apps/web/                 Next.js app (routes: /, /library, /play/[id], /c/[code], /s/[id], /me)
packages/mosaic-engine/   resize, Lab, lượng tử hoá, dithering, render nút (có test)
packages/puzzle-core/     seed PRNG, xáo trộn, luật chế độ, kiểm tra thắng, chấm điểm, phát lại nhật ký
packages/content/         20 tranh: metadata, ngân hàng câu hỏi vi/en, bảng màu
scripts/build-library.ts  tiền xử lý ảnh thư viện
supabase/                 migrations SQL, RLS policies, edge functions (verify-attempt, og-image)
```
`puzzle-core` dùng chung cho client và edge function xác thực → luật chơi chỉ viết một lần.

---

## 11. Yêu cầu phi chức năng

- **Hiệu năng**: tạo mosaic 48×48 < 300 ms trên điện thoại tầm trung; bàn ghép 60 fps với 100 mảnh;
  trang đầu (LCP) < 2,5 giây trên 4G.
- **Responsive**: thiết kế từ 360 px; vùng chạm ≥ 44 px; hỗ trợ dọc/ngang; tôn trọng safe-area (tai thỏ).
- **Khả năng tiếp cận**: tương phản đủ, không chỉ dùng màu để báo đúng/sai (thêm biểu tượng ✓), hỗ trợ
  `prefers-reduced-motion`, bàn phím trên desktop.
- **Riêng tư**: ảnh khách không rời thiết bị; xoá EXIF; chính sách quyền riêng tư & điều khoản; tuân thủ
  Nghị định 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân (nếu phục vụ người dùng Việt Nam).
- **Offline**: Service Worker lưu cache thư viện → chơi được khi mất mạng; kết quả xếp hạng gửi khi có mạng
  (đánh dấu "không xếp hạng" nếu không có seed từ server).

---

## 12. Lộ trình & mốc kiểm tra

| Mốc | Nội dung | Tiêu chí hoàn thành |
|---|---|---|
| M0 – Nguyên mẫu engine | `mosaic-engine` + trang thử: tải ảnh → tranh gạch | 20 tranh mẫu trông "ra gạch" đẹp, đạt hiệu năng §11 |
| M1 – Bàn ghép | `puzzle-core`, kéo/thả/đổi chỗ/xoay, 3 chế độ, đồng hồ | Chơi thử 36 mảnh trên iPhone & Android thật thấy mượt |
| M2 – Nội dung | 20 tranh tiền xử lý, metadata, ~140 câu quiz vi | Quiz đã được rà soát sự kiện lịch sử |
| M3 – Hoàn thiện MVP | Màn hình kết quả, ảnh thành tích, Web Share, PWA, kỷ lục cục bộ | Chia sẻ được lên Facebook/Zalo từ điện thoại; chơi thử với 5–10 người |
| M4 – Tài khoản | Supabase Auth, lưu ảnh riêng, gộp dữ liệu khách | RLS được kiểm thử |
| M5 – Cạnh tranh | Ván xếp hạng, xác thực nhật ký, bảng xếp hạng, link thách đấu, trang `/s/{id}` | Không gửi được thời gian giả qua API |
| M6 – Realtime & mở rộng | Phòng đua, AI quiz, huy hiệu, song ngữ | |

Rủi ro chính: (1) bản quyền ảnh tranh ở bảo tàng Ý/Vatican nếu thương mại; (2) nhãn hiệu LEGO;
(3) mảnh quá nhỏ trên điện thoại khi > 100 mảnh; (4) kiểm duyệt ảnh người dùng trên trang công khai;
(5) gian lận bảng xếp hạng — giảm thiểu bằng xác thực phía server, chấp nhận không tuyệt đối.

---

## 13. Câu hỏi cần chốt

1. **Vị trí code**: repo `sobik` hiện là app Android giải Rubik. Làm web app này trong repo mới, hay thư mục
   `web/` riêng trong repo này? (Đề xuất: repo mới.)
2. **Mục đích thương mại?** Quyết định có giữ các tranh ⚠️ ở bảo tàng Ý/Vatican và mức đầu tư kiểm duyệt.
3. **Ngôn ngữ**: chỉ tiếng Việt hay Việt + Anh ngay từ MVP?
4. **Hình dạng mảnh**: mảnh vuông (đề xuất) hay mảnh jigsaw có ngàm?
5. **Phòng đua realtime** có cần ngay trong MVP không, hay link thách đấu là đủ cho bản đầu?
6. **Quiz ảnh riêng**: chấp nhận quiz trí nhớ tự sinh + câu hỏi tự viết, hay muốn AI sinh câu hỏi sớm?
7. **Tên sản phẩm & tên miền** (tránh dùng chữ "LEGO").
8. **Hạ tầng**: đồng ý dùng Supabase + Vercel? Có ràng buộc lưu dữ liệu tại Việt Nam không?
