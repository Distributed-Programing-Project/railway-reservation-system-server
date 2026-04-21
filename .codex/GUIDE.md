# Hướng dẫn sử dụng Claude Code Agent

## Tổng quan các lệnh

| Lệnh | Mô tả ngắn |
|---|---|
| `/feature <usecase>` | Full pipeline: BA review → diagram → scaffold → code review |
| `/bizreview <usecase>` | Chỉ review nghiệp vụ |
| `/mermaid <usecase>` | Chỉ vẽ diagram |
| `/scaffold <usecase>` | Plan + sinh code từ usecase doc |
| `/reviewcode [path]` | Tech lead review code |
| `/reviewarch` | Solution architect review sơ đồ lớp |
| `/commit` | Commit code theo conventional commits |
| `/merge-main` | Merge branch hiện tại vào main |

---

## Khi nào dùng lệnh nào

### Làm usecase mới từ đầu → `/feature`

Dùng khi bắt đầu 1 usecase chưa có gì, muốn đi đầy đủ từ nghiệp vụ đến code.

```
Điều kiện: đã có docs/usecases/<usecase>.md

/feature buy-ticket
```

Chạy tuần tự 4 pha, dừng xin confirm mỗi bước:
1. BA review nghiệp vụ → sửa usecase doc nếu sai
2. Vẽ 3 diagram → lưu vào `docs/diagrams/`
3. Plan code → confirm → sinh code
4. Tech lead review → fix blocker ngay

> ⚠️ Tốn token nhất — chỉ dùng cho usecase phức tạp hoặc lần đầu làm.

---

### Chỉ muốn check nghiệp vụ → `/bizreview`

Dùng khi mới viết xong usecase doc, chưa chắc nghiệp vụ có đúng không.

```
/bizreview buy-ticket       ← review 1 usecase
/bizreview                  ← review tất cả usecases trong docs/usecases/
```

Agent đóng vai **Senior BA 10+ năm kinh nghiệm vé tàu VR**, sẽ check:
- Flow có đầy đủ không (happy path + alternative + error)
- Rules có đúng thực tế không (loại vé, hoàn vé, hóa đơn...)
- Có conflict với usecase khác không

Nếu sai → tự sửa lại `docs/usecases/<usecase>.md`.

---

### Chỉ muốn vẽ diagram → `/mermaid`

Dùng khi usecase doc đã đúng, chỉ cần tài liệu hóa bằng diagram.

```
/mermaid buy-ticket
```

Sinh 3 diagram vào `docs/diagrams/buy-ticket.md`:
- **System Architecture** — toàn bộ luồng phân tán (Client → Socket → Server → DB)
- **Sequence** — flow chi tiết với tên class/method thật
- **Class Diagram** — entity + DTO liên quan

---

### Chỉ muốn sinh code → `/scaffold`

Dùng khi nghiệp vụ đã được duyệt (`/bizreview` xong), muốn sinh code nhanh.

```
/scaffold buy-ticket
```

Chạy 2 pha:
1. **Plan** — hiện danh sách file sẽ tạo/sửa, layer breakdown, risks → hỏi confirm
2. **Generate** — sinh code sau khi xác nhận

Sinh theo thứ tự: ActionType → Enum → Entity → DTO → Repository → Service → Handler.

> Dùng cái này thay `/feature` khi: nghiệp vụ đơn giản, đã biết rõ cần gì, muốn tiết kiệm token.

---

### Review code đã viết → `/reviewcode`

Dùng sau khi viết code (tay hoặc qua `/scaffold`), trước khi commit.

```
/reviewcode                              ← review toàn bộ uncommitted changes
/reviewcode src/main/.../service/        ← review 1 package
/reviewcode src/main/.../TicketService.java  ← review 1 file
```

Agent đóng vai **Tech Lead**, check 9 hạng mục:
Architecture violations, Naming, Entity style, DTO style, Distributed concerns, JPA/Hibernate, Code quality, Code simplification (show before/after), Security.

Verdict cuối: **APPROVE / APPROVE WITH SUGGESTIONS / REQUEST CHANGES**

---

### Review sơ đồ lớp → `/reviewarch`

Dùng khi muốn đánh giá toàn bộ thiết kế entity/DTO, không phải review code cụ thể.

```
/reviewarch
```

Agent đóng vai **Solution Architect**, check:
- Relationship & cardinality có đúng không
- Normalization (dữ liệu duplicate, field sai chỗ)
- PKs (phải là UUID)
- Cascade rules
- DTO design
- Missing abstractions

Nếu có vấn đề → vẽ lại sơ đồ improved, giải thích từng thay đổi.
Lưu vào `docs/diagrams/architecture-review.md`.

---

### Commit code → `/commit`

Dùng thay cho `git commit` thủ công.

```
/commit
```

Tự động:
- Phân tích diff, nhóm file liên quan vào cùng commit
- Chọn đúng type: `feat / fix / update / refactor / chore`
- Tách commit độc lập (common vs server, feature vs refactor)
- Thêm co-author Claude + Phạm Trà

---

### Merge vào main → `/merge-main`

Dùng khi muốn merge branch hiện tại vào `main`.

```
/merge-main
```

Tự động: kiểm tra working tree sạch → push branch → checkout main → pull → merge `--no-ff` → push.

> ⚠️ Nếu main được protected trên GitHub (cần PR) — lệnh sẽ báo và dừng lại.

---

## Flow chuẩn cho 1 usecase mới

### Bước 1 — Viết usecase docs (lặp cho từng usecase)
```
/reviewusecase buy-ticket Khách hàng chọn tàu, chọn ghế, thanh toán và nhận vé
/reviewusecase cancel-ticket Khách hàng hủy vé đã đặt và nhận hoàn tiền
/reviewusecase login Nhân viên đăng nhập vào hệ thống
```
Mỗi lệnh nhận mô tả thô → tự expand thành file chuẩn → lưu vào `docs/usecases/`.

### Bước 2 — Review nghiệp vụ toàn bộ (sau khi đủ usecase)
```
/bizreview           ← review tất cả cùng lúc, check consistency
/bizreview buy-ticket ← hoặc review từng cái
```

### Bước 3 — Implement từng usecase
```
/feature buy-ticket   ← full pipeline (diagram → scaffold → review)
/feature cancel-ticket
```

### Bước 4 — Commit
```
/commit
```

---

## Cấu trúc folder cần duy trì

```
docs/
├── usecases/          ← bạn viết tay (input cho /bizreview, /scaffold, /feature)
│   ├── buy-ticket.md
│   └── login.md
└── diagrams/          ← sinh tự động bởi /mermaid và /reviewarch
    ├── buy-ticket.md
    └── architecture-review.md
```

### Format file usecase (docs/usecases/<name>.md)

Không bắt buộc format cứng, nhưng nên có đủ:
- **Actor**: ai thực hiện usecase này
- **Luồng chính**: các bước từ đầu đến cuối
- **Luồng thay thế**: khi có điều kiện khác (ghế đã đặt, khách hủy...)
- **Luồng lỗi**: input sai, hết hạn, lỗi hệ thống
- **Dữ liệu vào**: client gửi gì lên server
- **Dữ liệu ra**: server trả về gì
- **Business rules**: các ràng buộc nghiệp vụ

---

## Bảng so sánh nhanh

| Tình huống | Dùng lệnh |
|---|---|
| Có ý tưởng usecase, muốn viết thành tài liệu chuẩn | `/reviewusecase <name> <mô tả>` |
| Đã có đủ usecase, muốn check nghiệp vụ có đúng không | `/bizreview` |
| Usecase mới, phức tạp, muốn đi đầy đủ từ A→Z | `/feature <usecase>` |
| Usecase đơn giản, đã chắc nghiệp vụ, chỉ cần sinh code | `/scaffold <usecase>` |
| Cần tài liệu diagram cho báo cáo | `/mermaid <usecase>` |
| Vừa viết xong code, trước khi commit | `/reviewcode` |
| Muốn đánh giá lại thiết kế entity/DTO tổng thể | `/reviewarch` |
| Xong 1 batch công việc, muốn commit | `/commit` |
| Branch xong, muốn merge vào main | `/merge-main` |
