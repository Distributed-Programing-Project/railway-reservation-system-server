# Usecase - 003: Trả vé

## Actor
- **Primary:** Nhân viên bán vé (quầy)
- **System:** JavaFX Client, TCP Socket Server, MariaDB

## Tiền điều kiện
- Nhân viên đã đăng nhập và có `employeeId` (hoặc `employeeCode`) để ghi nhận giao dịch trả.
- Vé cần trả tồn tại và đang ở trạng thái `TicketStatus.PAID`.

## Hậu điều kiện
- Tạo `Invoice` type `REFUND` và `InvoiceDetail` tương ứng (mỗi vé 1 dòng hoàn).
- Update vé đã trả: `Ticket.status = TicketStatus.RETURNED`, `Ticket.qrCode = "INVALID"`.
- Update các `InvoiceDetail` của hóa đơn SALE gốc: `isReturned = true` và `refundAmount` theo tính toán.
- Có thể tra cứu biên lai hoàn tiền qua `refundInvoiceId` (`GET_REFUND_RECEIPT`).

## Mô tả
Nhân viên tra cứu vé cần trả theo mã vé/QR hoặc giấy tờ (người mua/hành khách), chọn danh sách vé, hệ thống tính trước số tiền hoàn và phí hoàn. Nhân viên xác nhận trả vé, hệ thống tạo biên lai hoàn tiền và cập nhật trạng thái vé.

---

## Luồng chính

### 1) Tra cứu vé trả
1. Nhân viên nhập `query` (mã vé/QR/CCCD…) → client gọi `ReturnTicketClientService.searchTicketsForReturn(query)`.
2. Client gửi `Request(ActionType.SEARCH_TICKETS_FOR_RETURN, ReturnTicketSearchDTO)`.
3. Server (`TicketServiceImpl.searchTicketsForReturn`):
   - Nếu `query` rỗng: load mặc định danh sách vé `PAID`.
   - Nếu có `query`: auto-detect theo thứ tự:
     - Tìm theo `ticketId` hoặc `qrCode` (`findTicketByIdOrQrWithSchedule`),
     - Nếu không thấy thì tìm theo giấy tờ người mua (`findTicketsByCustomerIdCardWithStatus`),
     - và/hoặc giấy tờ hành khách (`findTicketsByPassengerDocumentWithStatus`).
4. Trả `List<ReturnTicketTicketDTO>` (lọc chỉ lấy vé `PAID`).

### 2) Xem trước số tiền hoàn
1. Nhân viên chọn `ticketIds` → client gọi `previewReturnTickets(ticketIds)`.
2. Client gửi `Request(ActionType.PREVIEW_RETURN_TICKETS, ReturnTicketPreviewRequestDTO)`.
3. Server (`TicketServiceImpl.previewReturnTickets`) validate:
   - `@NotEmpty(ticketIds)` → nếu thiếu → **"Dữ liệu không hợp lệ: Danh sách mã vé không được để trống."**
4. Server tính toán (`doComputeReturn`):
   - Không cho trùng ticketIds → **"Danh sách mã vé chứa các giá trị trùng lặp."**
   - Vé phải tồn tại và đủ số lượng → **"Một số vé không tồn tại hoặc không hợp lệ."**
   - Vé phải `PAID` → **"Vé %s không ở trạng thái hợp lệ để trả."**
   - Vé phải có lịch trình và giờ khởi hành → **"Không tìm thấy thông tin lịch trình cho vé này."**
   - Điều kiện thời gian: còn **>= 4 tiếng** trước giờ khởi hành → nếu < 4h → **"Vé %s không đủ điều kiện trả vì thời gian khởi hành còn dưới 4 tiếng."**
   - Phí trả vé:
     - Nếu vé đã từng đổi (`originalTicketId != null` hoặc `isExchanged=true`) → phí 30%.
     - Nếu chưa đổi: < 24h → 20%, ngược lại 10%.
     - Phí tối thiểu mỗi vé: 10.000 và làm tròn lên bội số 1.000.
5. Server kiểm tra tất cả vé phải thuộc **cùng 1 khách hàng** → nếu không → **"Tất cả các vé phải thuộc cùng một khách hàng để thực hiện trả theo lô."**
6. Trả `ReturnTicketPreviewDTO(totalTicketPrice, refundFee, refundAmount)`.

### 3) Xác nhận trả vé
1. Nhân viên nhập `refundAmount` theo preview và `employeeId`.
2. Client gửi `Request(ActionType.CONFIRM_RETURN_TICKETS, ReturnTicketConfirmDTO)`.
3. Server (`TicketServiceImpl.confirmReturnTickets`) validate:
   - Thiếu danh sách vé → **"Dữ liệu không hợp lệ: Danh sách mã vé không được để trống."**
   - `refundAmount < 0` → **"Dữ liệu không hợp lệ: Số tiền hoàn trả phải lớn hơn hoặc bằng 0"**.
   - Thiếu `employeeId` → **"Dữ liệu không hợp lệ: Employee ID không được để trống"**.
4. Server transaction (`doConfirmReturnTickets`):
   - So khớp `refundAmount` với tính toán hệ thống (sai lệch > 1.0) → **"Số tiền hoàn trả không khớp với tính toán của hệ thống."**
   - Resolve nhân viên → nếu không thấy → **"Không tìm thấy nhân viên: id=%s"**.
   - Tạo `Invoice(REFUND)` và persist.
   - Tạo `InvoiceDetail` hoàn tiền cho từng vé (`isReturned=true`, `refundAmount=...`; `insurance=0` theo rule “insurance non-refundable”).
   - Update các `InvoiceDetail` SALE gốc: set returned + refundAmount.
   - Update vé: `TicketStatus.RETURNED`, `qrCode="INVALID"`.
5. Trả `Response.success("Trả vé thành công", refundInvoiceId)`.

### 4) In/tra cứu biên lai hoàn tiền
1. Client gửi `Request(ActionType.GET_REFUND_RECEIPT, RefundReceiptRequestDTO)` với `refundInvoiceId`.
2. Server load `Invoice` + join các quan hệ (customer/employee/details/ticket/schedule/route/stations…) và trả `RefundReceiptDTO`.

---

## Luồng lỗi (bắt buộc đúng message từ code)
- **[Danh sách vé trống]**: **"Danh sách mã vé không được để trống."**
- **[TicketIds trùng]**: **"Danh sách mã vé chứa các giá trị trùng lặp."**
- **[Vé không hợp lệ/không tồn tại]**: **"Một số vé không tồn tại hoặc không hợp lệ."**
- **[Vé không ở trạng thái trả được]**: **"Vé %s không ở trạng thái hợp lệ để trả."**
- **[Không đủ điều kiện thời gian]**: **"Vé %s không đủ điều kiện trả vì thời gian khởi hành còn dưới 4 tiếng."**
- **[Không cùng khách hàng]**: **"Tất cả các vé phải thuộc cùng một khách hàng để thực hiện trả theo lô."**
- **[RefundAmount không khớp]**: **"Số tiền hoàn trả không khớp với tính toán của hệ thống."**
- **[Xung đột dữ liệu]**: **"Ghế bạn chọn vừa có người khác đặt nhanh hơn. Vui lòng thử lại!"** (`TicketMessages.DATA_CONFLICT`)

---

## Dữ liệu vào/ra (I/O Data)

### Client → Server
| ActionType | DTO | Field |
|---|---|---|
| `SEARCH_TICKETS_FOR_RETURN` | `ReturnTicketSearchDTO` | `query`, `queryType` |
| `PREVIEW_RETURN_TICKETS` | `ReturnTicketPreviewRequestDTO` | `ticketIds` |
| `CONFIRM_RETURN_TICKETS` | `ReturnTicketConfirmDTO` | `ticketIds`, `refundAmount`, `employeeId` |
| `GET_REFUND_RECEIPT` | `RefundReceiptRequestDTO` | `refundInvoiceId` |

### Server → Client
| API | Response.message | Response.data |
|---|---|---|
| Tra cứu vé trả | `TicketMessages.FIND_SUCCESS` | `List<ReturnTicketTicketDTO>` |
| Xem trước hoàn tiền | `TicketMessages.PREVIEW_SUCCESS` | `ReturnTicketPreviewDTO` |
| Xác nhận trả vé | `TicketMessages.RETURN_SUCCESS` | `refundInvoiceId` (String) |
| Biên lai hoàn tiền | `"Lấy dữ liệu biên lai hoàn tiền thành công."` | `RefundReceiptDTO` |

---

## Use Case Diagram (Mermaid)
```mermaid
graph LR
  Actor[Nhân viên bán vé] --> UC003((UC003 - Trả vé))
  UC003 --> U1[Tra cứu vé (SEARCH_TICKETS_FOR_RETURN)]
  UC003 --> U2[Xem trước hoàn (PREVIEW_RETURN_TICKETS)]
  UC003 --> U3[Xác nhận trả (CONFIRM_RETURN_TICKETS)]
  UC003 --> U4[In biên lai (GET_REFUND_RECEIPT)]
```

---

## BA Review — Yêu cầu cập nhật Database / Entity / DTO (Từ BA Review)
- **Rule thời gian trả vé (>= 4h)**: cần thể hiện rõ trong tài liệu nghiệp vụ vì code đang enforce cứng theo phút (`MINUTES_4H`).
- **Phí trả vé theo mốc 24h và trạng thái “đã đổi”**: code tính theo `originalTicketId/isExchanged` và `minutesToDeparture` → cần BA xác nhận đây là chính sách chính thức (đặc biệt fee 30% cho vé đã đổi).
- **Không hoàn phí bảo hiểm**: `InvoiceDetail.insurance = 0` khi hoàn tiền và comment “Insurance is non-refundable” → cần BA xác nhận rule và cách hiển thị trên biên lai.
- **Trả vé theo lô phải cùng khách hàng**: code enforce `CUSTOMER_MISMATCH` → cần BA ghi thành requirement để tránh tranh chấp nghiệp vụ ở quầy.
