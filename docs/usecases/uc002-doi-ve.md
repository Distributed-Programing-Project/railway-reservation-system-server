# Usecase - 002: Đổi vé

## Actor
- **Primary:** Nhân viên bán vé (quầy)
- **System:** JavaFX Client, TCP Socket Server, MariaDB

## Tiền điều kiện
- Nhân viên đã đăng nhập và có `employeeId` (hoặc `employeeCode` dạng QL001) để ghi nhận giao dịch đổi.
- Vé cần đổi tồn tại và đang ở trạng thái `TicketStatus.PAID`.
- Nhân viên đã “giữ chỗ” (hold) các ghế mới theo `clientSessionId`.

## Hậu điều kiện
- Các vé cũ được cập nhật: `Ticket.exchanged = true`, `Ticket.status = TicketStatus.EXCHANGED`, `Ticket.qrCode = "INVALID"`.
- Tạo các vé mới: `Ticket.status = TicketStatus.PAID`, `Ticket.originalTicketId = oldTicketId` (link vé gốc), `Ticket.exchanged = false`.
- Tạo `Invoice` type `EXCHANGE` và các `InvoiceDetail` cho các vé mới.
- Giải phóng hold ghế mới sau khi đổi xong (`SeatHoldStore.releaseAll(...)`).

## Mô tả
Nhân viên tra cứu các vé đủ điều kiện đổi theo CCCD/hộ chiếu, chọn các vé cần đổi, chọn ghế mới (đã hold), xem trước phí đổi, sau đó xác nhận đổi để hệ thống cập nhật trạng thái vé cũ và phát hành vé mới + hóa đơn đổi.

---

## Luồng chính

### 1) Tra cứu vé đủ điều kiện đổi
1. Nhân viên nhập CCCD/hộ chiếu → client gọi `ExchangeTicketClientService.searchTicketsForExchange(idCard)`.
2. Client gửi `Request(ActionType.SEARCH_TICKETS_FOR_EXCHANGE, ExchangeEligibleTicketSearchDTO)`.
3. Server (`TicketServiceImpl.searchTicketsForExchange`) validate:
   - `@NotBlank(idCard)` → nếu thiếu → **"Dữ liệu không hợp lệ: Số CCCD không được để trống"** (prefix `TicketMessages.DATA_INVALID_PREFIX`).
4. Server truy vấn `TicketRepositoryImpl.findTicketsByCustomerIdCardWithStatusForExchange(..., TicketStatus.PAID)` và trả về danh sách `ExchangeEligibleTicketDTO` kèm cờ `eligible`/`ineligibleReason`.

### 2) Xem trước phí đổi
1. Nhân viên chọn danh sách `oldTicketIds` và danh sách `newScheduleDetailIds` tương ứng (1–1).
2. Client gửi `Request(ActionType.PREVIEW_EXCHANGE_TICKETS, ExchangeTicketPreviewRequestDTO)`.
3. Server (`TicketServiceImpl.previewExchangeTickets`) kiểm tra:
   - Số lượng vé cũ và ghế mới phải khớp → **"Số lượng vé cũ và ghế mới phải khớp nhau."** (`TicketMessages.COUNT_MISMATCH`).
   - `clientSessionId` hợp lệ → nếu thiếu → **"Phiên làm việc không hợp lệ."** (`TicketMessages.INVALID_SESSION`).
   - Vé cũ hợp lệ & đủ điều kiện đổi (method `validateBusinessRulesOrThrow(...)`):
     - Đã đổi hoặc là vé phát sinh từ đổi → **"Vé %s đã từng được đổi trước đó."**
     - Vé đã trả → **"Vé %s đã được trả, không thể đổi."**
     - Không phải `PAID` → **"Vé %s không ở trạng thái hợp lệ để đổi."**
     - Thời gian khởi hành còn < 24h → **"Vé %s không được đổi vì chỉ còn %d h tới giờ khởi hành (yêu cầu ít nhất 24h)."**
   - Ghế mới phải đang được hold bởi đúng phiên → nếu không → **"Ghế bạn chọn đang được giữ chỗ bởi phiên khác. Vui lòng chọn ghế khác."** (`TicketMessages.SEAT_HELD_BY_OTHER`)
4. Server tính phí:
   - `exchangeFeeTotal = oldTickets.size * 20_000` (hằng `EXCHANGE_FEE`).
   - `priceDifference = totalNewPrice - totalOldPrice` (giá vé cũ lấy theo invoice detail SALE gần nhất).
   - `totalAmount = exchangeFeeTotal + priceDifference`, nếu `totalAmount < 0` thì **KHÔNG hoàn chênh lệch**, chỉ thu `exchangeFeeTotal`.
5. Trả `ExchangeTicketPreviewDTO`.

### 3) Xác nhận đổi vé
1. Client gửi `Request(ActionType.EXCHANGE_TICKET, ExchangeTicketRequestDTO)` với `employeeId`, `clientSessionId`, (tùy chọn) `taxCode`, `companyName`.
2. Server (`TicketServiceImpl.exchangeTickets`) validate DTO:
   - Thiếu danh sách → **"Dữ liệu không hợp lệ: Danh sách vé cũ không được để trống"** / **"Dữ liệu không hợp lệ: Danh sách ghế mới không được để trống"**.
   - Thiếu `employeeId` → **"Dữ liệu không hợp lệ: Employee ID không được để trống"**.
   - `taxCode` > 20 ký tự → **"Dữ liệu không hợp lệ: Mã số thuế không được vượt quá 20 ký tự"**.
   - `companyName` > 200 ký tự → **"Dữ liệu không hợp lệ: Tên công ty không được vượt quá 200 ký tự"**.
3. Server chạy transaction (`AbstractGenericRepositoryImpl.transactional(...)`) trong `doExchangeTicketsOrThrow(...)`:
   - Resolve nhân viên theo `employeeId` hoặc `employeeCode` (native query trong `findEmployeeByIdOrCode`) → nếu không thấy → **"Không tìm thấy nhân viên: id=%s"**.
   - Cập nhật vé cũ → `EXCHANGED` và `qrCode="INVALID"`.
   - Load ghế mới: `ScheduleDetailRepositoryImpl.findByIdsWithSeatAndSchedule(...)`.
   - Kiểm tra ghế đã bán hay chưa bằng `getSoldSeatIdsWithLock(...)` (PESSIMISTIC lock theo `Seat`).
   - Tạo vé mới + `Invoice(EXCHANGE)` + `InvoiceDetail`.
4. Trả `ExchangeTicketResponseDTO` với message dạng: **"Đổi vé thành công. Số tiền thanh toán: %.2f"**.

---

## Luồng lỗi (bắt buộc đúng message từ code)
- **[Mismatch số lượng]**: **"Số lượng vé cũ và ghế mới phải khớp nhau."**
- **[Phiên không hợp lệ]**: **"Phiên làm việc không hợp lệ."**
- **[Một số vé không hợp lệ]**: **"Một số vé không tồn tại hoặc không hợp lệ."**
- **[Vé đã đổi]**: **"Vé %s đã từng được đổi trước đó."**
- **[Vé đã trả]**: **"Vé %s đã được trả, không thể đổi."**
- **[Vé không ở trạng thái PAID]**: **"Vé %s không ở trạng thái hợp lệ để đổi."**
- **[Hết hạn đổi (24h)]**: **"Vé %s không được đổi vì chỉ còn %d h tới giờ khởi hành (yêu cầu ít nhất 24h)."**
- **[Ghế đang hold bởi phiên khác]**: **"Ghế bạn chọn đang được giữ chỗ bởi phiên khác. Vui lòng chọn ghế khác."**
- **[Ghế đã có người đặt]**: **"Ghế số %s của chuyến %s đã có người đặt."**
- **[Xung đột dữ liệu]**: **"Ghế bạn chọn vừa có người khác đặt nhanh hơn. Vui lòng thử lại!"** (`TicketMessages.DATA_CONFLICT`)

---

## Dữ liệu vào/ra (I/O Data)

### Client → Server
| ActionType | DTO | Field |
|---|---|---|
| `SEARCH_TICKETS_FOR_EXCHANGE` | `ExchangeEligibleTicketSearchDTO` | `idCard` |
| `PREVIEW_EXCHANGE_TICKETS` | `ExchangeTicketPreviewRequestDTO` | `oldTicketIds`, `newScheduleDetailIds`, `clientSessionId` |
| `EXCHANGE_TICKET` | `ExchangeTicketRequestDTO` | `oldTicketIds`, `newScheduleDetailIds`, `employeeId`, `clientSessionId`, `taxCode`, `companyName` |

### Server → Client
| API | Response.message | Response.data |
|---|---|---|
| Tra cứu vé đổi | `TicketMessages.EXCHANGE_SEARCH_SUCCESS` | `List<ExchangeEligibleTicketDTO>` |
| Xem trước phí đổi | `TicketMessages.EXCHANGE_PREVIEW_SUCCESS` | `ExchangeTicketPreviewDTO` |
| Đổi vé | `TicketMessages.EXCHANGE_SUCCESS` | `ExchangeTicketResponseDTO` |

---

## Use Case Diagram (Mermaid)
```mermaid
graph LR
  Actor[Nhân viên bán vé] --> UC002((UC002 - Đổi vé))
  UC002 --> U1[Tra cứu vé (SEARCH_TICKETS_FOR_EXCHANGE)]
  UC002 --> U2[Xem phí đổi (PREVIEW_EXCHANGE_TICKETS)]
  UC002 --> U3[Xác nhận đổi (EXCHANGE_TICKET)]
```

---

## BA Review — Yêu cầu cập nhật Database / Entity / DTO (Từ BA Review)
- **Không hoàn tiền khi vé mới rẻ hơn**: code enforce rule “`totalAmount` âm ⇒ chỉ thu phí đổi” → cần BA xác nhận đây là chính sách chính thức.
- **Vô hiệu hóa QR**: vé cũ bị set `qrCode = "INVALID"` và status `EXCHANGED` → cần BA xác nhận yêu cầu nghiệp vụ “không quét/không lên tàu bằng vé cũ”.
- **Tính giá vé cũ theo invoice detail SALE**: dùng `TicketRepositoryImpl.findActualPaidAmountByTicketId(...)` (ưu tiên giá thực trả, không dùng `ScheduleDetail.priceSeat`) → phù hợp nghiệp vụ giảm giá/ưu đãi; cần BA nêu rõ trong spec.
- **Khóa đồng thời**: `ScheduleDetail.version` (optimistic) + `Seat` lock (pessimistic) trong `getSoldSeatIdsWithLock` → cần mô tả trong tài liệu kỹ thuật để giải thích lỗi xung đột **"Ghế bạn chọn vừa có người khác đặt nhanh hơn. Vui lòng thử lại!"**.
