# Usecase - 002: Đổi vé tàu

## Actor

- **Primary:** Nhân viên tại quầy
- **System:** Server, Database, Máy in

## Tiền điều kiện

- Nhân viên đã đăng nhập vào hệ thống và chọn chức năng **Đổi vé**.
- Khách hàng (người đi tàu hoặc người mua đại diện) đã mua vé trước đó và vé đang ở trạng thái hợp lệ (`status = "SOLD"`).

## Hậu điều kiện

- Bản ghi `Ticket` cũ được cập nhật `status = "EXCHANGED"`.
- `ScheduleDetail` của vé cũ được giải phóng (trạng thái ghế trống).
- Bản ghi `Ticket` mới được tạo với `status = "SOLD"` và lưu giữ liên kết với vé cũ thông qua `originalTicketId`.
- Bản ghi `Invoice` (type = `EXCHANGE`) và danh sách `InvoiceDetail` được lưu để đối soát phí.
- Vé giấy mới và biên lai đổi vé được in ra cho khách.

## Mô tả

Nhân viên tại quầy tiếp nhận yêu cầu đổi vé từ khách hàng. Hệ thống thực hiện kiểm tra các ràng buộc về thời gian khởi hành và số lần đổi. Nếu thỏa mãn, nhân viên tiến hành chọn hành trình hoặc chỗ ngồi mới. Hệ thống tự động tính toán phí đổi vé cố định và phần chênh lệch giá giữa vé mới và vé cũ để thực hiện thu thêm hoặc hoàn tiền cho khách.

---

## Luồng chính

1. Nhân viên nhập thông tin tra cứu: **Số CMND / Hộ chiếu** của khách hàng.
2. Hệ thống truy vấn và hiển thị danh sách các vé có thể đổi (trạng thái `SOLD`).
3. Nhân viên chọn các vé khách muốn đổi và ấn **"Yêu cầu Đổi vé"**.
4. Hệ thống kiểm tra điều kiện cho từng vé:
   - Cách giờ tàu chạy ít nhất 24 giờ.
   - Vé chưa từng thực hiện giao dịch đổi trước đó.
5. Hệ thống xác nhận **"Đủ điều kiện đổi vé"** và mở giao diện tìm kiếm chuyến tàu mới.
6. Nhân viên nhập thông tin hành trình mới (Ga đi, Ga đến, Ngày đi) và ấn **"Tìm kiếm"**.
7. Hệ thống hiển thị danh sách các chuyến tàu (`Schedule`) phù hợp.
8. Nhân viên chọn chuyến tàu và sơ đồ ghế mới tương ứng.
9. Hệ thống cập nhật trạng thái ghế mới sang màu **Xanh lá** (đang chọn) và đưa vào giỏ tạm.
10. Nhân viên ấn **"Tính phí & Xác nhận"**.
11. Hệ thống tính toán chi tiết:
    - **Phí đổi vé**: (Số lượng vé \* Mức phí quy định).
    - **Chênh lệch**: (Tổng giá vé mới - Tổng giá vé cũ).
    - **Tổng thanh toán**: Phí đổi vé + Chênh lệch.
12. Hệ thống hiển thị bảng kê chi tiết các khoản phí cho nhân viên và khách hàng đối chiếu.
13. Nhân viên nhận tiền từ khách (nếu tổng > 0) hoặc chuẩn bị tiền hoàn (nếu tổng < 0) và ấn **"Xác nhận thanh toán"**.
14. Hệ thống thực hiện các thao tác sau trong một giao dịch (Transaction):
    - Cập nhật trạng thái vé cũ thành `EXCHANGED` và vô hiệu hóa QR code cũ.
    - Giải phóng ghế cũ trong `ScheduleDetail`.
    - Tạo bản ghi `Ticket` mới gắn với ghế mới và lưu `originalTicketId`.
    - Lưu `Invoice` loại `EXCHANGE` cùng các `InvoiceDetail` tương ứng.
15. Hệ thống báo **"Đổi vé thành công"** và hiển thị bản xem trước vé mới.
16. Nhân viên ấn **"In vé"** để hoàn tất quy trình.

---

## Luồng thay thế

### [AF-1] Vé mới có giá trị thấp hơn vé cũ

- **11.1** Nếu giá vé mới rẻ hơn vé cũ và phần chênh lệch lớn hơn phí đổi vé, Tổng thanh toán sẽ là số âm.
- **12.1** Hệ thống hiển thị số tiền **"Cần hoàn trả khách"**.
- **13.1** Nhân viên xác nhận đã chi trả tiền mặt/chuyển khoản cho khách trước khi ấn hoàn tất.

### [AF-2] Đổi nhiều vé cùng lúc

- **3.1** Nhân viên có thể chọn hàng loạt vé trong danh sách lịch sử để đổi chung một chuyến tàu mới.
- **11.2** Hệ thống gom tất cả vào một hóa đơn `EXCHANGE` duy nhất để dễ quản lý.

---

## Luồng lỗi

- **[Vé không đủ điều kiện]:** Thời gian đổi vé nằm trong khoảng < 24h trước giờ tàu chạy → Hệ thống cảnh báo lỗi và ngăn chặn giao dịch.
- **[Vé đã đổi một lần]:** Hệ thống kiểm tra thấy vé đã có `originalTicketId` hoặc `isExchanged = true` → Thông báo _"Vé này đã được đổi trước đó, không thể đổi lần thứ hai"_.
- **[Lỗi chiếm chỗ]:** Trong lúc chọn ghế mới, ghế đó đã bị quầy khác bán mất (`OptimisticLockException`) → Hệ thống báo lỗi xung đột và yêu cầu nhân viên chọn lại ghế khác.

---

## Dữ liệu vào (Client → Server)

### Tra cứu

| Field    | Kiểu     | Mô tả                          |
| -------- | -------- | ------------------------------ |
| `idCard` | `String` | Số giấy tờ định danh của khách |

### Giao dịch đổi

| Field                  | Kiểu            | Bắt buộc | Mô tả                        |
| ---------------------- | --------------- | -------- | ---------------------------- |
| `oldTicketIds`         | `List<String>`  | ✓        | Danh sách mã vé cũ           |
| `newScheduleDetailIds` | `List<String>`  | ✓        | Danh sách scheduleDetailId ghế mới |
| `employeeId`           | `String`        | ✓        | ID nhân viên thực hiện giao dịch |
| `taxCode`              | `String`        | ✗        | Mã số thuế (VAT)             |
| `companyName`          | `String`        | ✗        | Tên công ty (VAT)            |

---

### Dữ liệu ra (Server → Client)

### Thông tin đổi vé thành công

| Field              | Kiểu     | Mô tả                                      |
| ------------------ | -------- | ------------------------------------------ |
| `invoiceId`        | `String` | ID hóa đơn đổi vé (EXCHANGE)              |
| `totalAmount`      | `double` | Tổng số tiền (phí đổi + chênh lệch giá) |
| `oldTicketCount`   | `int`    | Số lượng vé cũ đã đổi                     |
| `newTicketCount`   | `int`    | Số lượng vé mới đã tạo                    |

---

## Business Rules

1. **Quy tắc 24h:** Chỉ được đổi vé nếu thời gian hiện tại cách giờ khởi hành trên vé cũ từ 24 tiếng trở lên.
2. **Quy tắc 1 lần:** Mỗi vé chỉ có duy nhất một cơ hội đổi.
3. **Tính phí:** Phí đổi vé là khoản phí dịch vụ cố định, không được hoàn trả ngay cả khi giá vé mới thấp hơn vé cũ.
4. **Tính nhất quán:** Việc hủy vé cũ và tạo vé mới phải xảy ra đồng thời. Nếu một bước lỗi, toàn bộ ghế (cả cũ và mới) phải giữ nguyên trạng thái ban đầu.

---

## Sơ đồ Use Case

```mermaid
graph LR
    NV["👤 Nhân viên"]

    subgraph "Hệ thống Bán Vé"
        UC_Search(["Tra cứu lịch sử vé"])
        UC_Check(["Kiểm tra điều kiện đổi"])
        UC_Exchange(["Thực hiện đổi vé & Tính phí"])
        UC_Print(["In vé mới"])
    end

    NV --> UC_Search
    NV --> UC_Check
    NV --> UC_Exchange
    NV --> UC_Print

    UC_Exchange -.->|include| UC_Check
```

---

## 🛠 Yêu cầu cập nhật Database / Entity (Từ BA Review)

**Trạng thái:** ✅ TẤT CẢ ĐÃ IMPLEMENT

1. **Ticket Entity:** ✅ Đã có — `originalTicketId` (String) và `isExchanged` (boolean) đã được implement.
   - **Lý do (Vấn đề thực tế):** Trường `originalTicketId` lưu ID của vé cũ, dùng để kiểm tra điều kiện "chỉ đổi 1 lần". Nếu trường này có giá trị, hệ thống sẽ từ chối đổi tiếp. Trường `isExchanged` đánh dấu vé đã bị thay thế để không cho phép sử dụng đi tàu hoặc đổi/trả lần nữa. QR code của vé cũ được invalidate (`qrCode = "INVALID"`) khi đổi vé — đã implement.

2. **Invoice Entity:** ✅ Đã có — `InvoiceType.EXCHANGE` đã có trong enum. `taxCode`, `companyName` đã có trong entity.
   - **Lý do (Vấn đề thực tế):** Cần phân biệt hóa đơn đổi vé với hóa đơn bán mới (`SALE`) và hoàn tiền (`REFUND`). Thông tin VAT (`taxCode`, `companyName`) cần thiết nếu khách đổi vé cho pháp nhân.

3. **InvoiceDetail Entity:** ✅ Đã có — `subTotal` đã đổi sang `Double` để hỗ trợ giá trị âm.
   - **Lý do (Vấn đề thực tế):** Nếu usecase đổi vé cần ghi nhận dòng "thu hồi vé cũ" với số âm trong báo cáo tài chính, cần kiểu dữ liệu hỗ trợ giá trị âm.

4. **ScheduleDetail Entity:** ✅ Đã có — `@Version int version` đã được implement.
   - **Lý do (Vấn đề thực tế):** Cần Optimistic Locking để ngăn chặn việc hai nhân viên ở hai quầy khác nhau cùng đổi vé vào chung một chỗ ngồi trống tại cùng một thời điểm. Khi quầy A đổi thành công, version tăng lên; quầy B đến sau sẽ bị ném `OptimisticLockException` và bị từ chối.
