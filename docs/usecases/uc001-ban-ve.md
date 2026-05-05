# Usecase - 001: Bán vé tàu

## Actor
- **Primary:** Nhân viên bán vé (quầy)
- **System:** JavaFX Client, TCP Socket Server, MariaDB, (tùy chọn) máy in/PDF viewer

## Tiền điều kiện
- Nhân viên đã đăng nhập và mở chức năng **Bán vé** trên client.
- Lịch trình (`Schedule`) đã được tạo và đang ở trạng thái bán được (`StatusSchedule.NOT_STARTED`).

## Hậu điều kiện
- Tạo `Invoice` type `SALE` và các `InvoiceDetail` tương ứng.
- Tạo các `Ticket` (status `TicketStatus.PAID`) gắn với `ScheduleDetail` đã chọn.
- Nếu khách có tài khoản tích điểm: cập nhật `Customer.rewardPoints` (quy tắc tính điểm nằm trong `SaleServiceImpl`).
- Ghế đã bán được tính theo `Ticket.status NOT IN (CANCELLED, EXCHANGED, RETURNED)`; các ghế đã “giữ chỗ” (hold) được giải phóng sau khi bán thành công.

## Mô tả
Nhân viên tra cứu chuyến theo ga đi/ga đến/ngày đi (và ngày về nếu khứ hồi), xem sơ đồ ghế, giữ ghế tạm thời theo phiên (`clientSessionId`), nhập thông tin hành khách/người mua, (tùy chọn) đổi điểm hoặc thanh toán online, sau đó xác nhận thanh toán để phát hành vé và hóa đơn.

---

## Luồng chính

### 1) Tra cứu chuyến tàu
1. Nhân viên chọn ga đi/ga đến và ngày đi (DTO: `SaleScheduleSearchDTO`).
2. Client gửi `Request(ActionType.SEARCH_SCHEDULES_FOR_SALE, SaleScheduleSearchDTO)`.
3. Server validate:
   - Nếu thiếu dữ liệu → **"Dữ liệu yêu cầu không hợp lệ"** (`SaleMessages.INVALID_REQUEST`).
   - Nếu ga đi = ga đến hoặc trống → **"Ga đi và ga đến không hợp lệ"** (`SaleMessages.INVALID_STATIONS`).
   - Nếu ngày đi/ngày về không hợp lệ → **"Ngày đi/ngày về không hợp lệ"** (`SaleMessages.INVALID_DATES`).
4. Server truy vấn danh sách `Schedule` theo ngày, lọc theo tuyến có thứ tự ga hợp lệ (repo: `ScheduleRepositoryImpl.filterSchedules(...)`) và trả về `SaleScheduleSearchResultDTO`.

### 2) Xem sơ đồ ghế
1. Nhân viên chọn 1 chuyến (outbound/return) → client gọi `SaleClientService.getSeatMap(...)`.
2. Client gửi `Request(ActionType.GET_SEATMAP_FOR_SCHEDULE, SeatMapRequestDTO)`.
3. Server lấy `ScheduleDetail` (JPQL join `ScheduleDetail` ↔ `Seat` ↔ `Carriage`) và tính trạng thái ghế:
   - `SOLD`: ghế đã có vé (status không thuộc `CANCELLED/EXCHANGED/RETURNED`).
   - `HELD`: ghế đang được `SeatHoldStore` giữ chỗ (TTL 10 phút).
   - `AVAILABLE`: còn trống.
4. Trả về `SeatMapResponseDTO` gồm danh sách toa (`CarriageSeatMapDTO`) và ghế (`SeatMapSeatDTO`).

### 3) Giữ ghế (hold) theo phiên làm việc
1. Nhân viên chọn các ghế theo `scheduleDetailIds`.
2. Client gửi `Request(ActionType.HOLD_SEATS_FOR_SALE, SeatHoldRequestDTO)` với `clientSessionId`.
3. Server:
   - Validate `scheduleId`, `clientSessionId`, danh sách `scheduleDetailIds` thuộc đúng schedule.
   - Loại các ghế đã bán (Ticket.status không thuộc `CANCELLED/EXCHANGED/RETURNED`).
   - Gọi `SeatHoldStore.tryHold(...)` cho từng `scheduleDetailId`.
4. Trả về `SeatHoldResponseDTO` gồm `successIds`, `failedIds`, `expiresAtEpochMillis`.

### 4) Nhập thông tin hành khách + người mua
1. Nhân viên nhập danh sách hành khách theo số ghế đã giữ:
   - DTO: `SalePassengerDTO` cho từng hành khách (tên/giấy tờ/loại vé/ngày sinh…).
2. Nhân viên nhập thông tin người mua:
   - DTO: `SaleBuyerDTO` (buyerName, documentType, documentNumber, buyerEmail, buyerPhone, hasAccount, customerId).
3. (Tùy chọn) nhập VAT: `SaleVatDTO` và yêu cầu đổi điểm: `SaleRedeemPointsDTO`.

### 5) Thanh toán và phát hành vé
1. Client gửi `Request(ActionType.CREATE_SALE_TRANSACTION, SaleCreateRequestDTO)`.
2. Server kiểm tra:
   - Ràng buộc số lượng: tối đa 10 vé mỗi chiều → **"Mỗi chiều chỉ được mua tối đa 10 vé"**.
   - Khứ hồi: số ghế chiều đi và chiều về phải bằng nhau → **"Số lượng ghế chiều đi và chiều về phải bằng nhau"**.
   - Hold hợp lệ theo `clientSessionId` → nếu ghế do phiên khác giữ → **"Ghế đang được giao dịch bởi quầy khác"**.
   - Quy tắc trẻ em: có `childrenUnder6` thì bắt buộc có người lớn đi kèm → **"Vé trẻ em bắt buộc phải có người lớn đi kèm"**.
   - Không cho đổi điểm khi có vé ưu đãi theo đối tượng (CHILD/SENIOR/STUDENT…) → **"Không được đổi điểm khi có vé ưu đãi đối tượng"**.
   - Thanh toán:
     - Tiền mặt: nếu `amountPaid < totalAmount` → **"Chưa đủ điều kiện thanh toán"**.
     - Online: cần `paymentOrderId`, trạng thái đơn SUCCESS, chưa dùng, đúng session, đúng số tiền; nếu sai trả các lỗi `SaleMessages.ONLINE_*` tương ứng.
3. Nếu hợp lệ, server tạo `Invoice(SALE)` + `InvoiceDetail`, persist `Ticket(PAID)`, trả `SaleCreateResponseDTO` (invoiceId, totalAmount, changeAmount, earnedPoints, redeemedPoints, tickets…).
4. Server/Client giải phóng hold ghế sau khi tạo giao dịch thành công (`releaseHeldSeatsForSale(...)`).

---

## Luồng thay thế / Luồng lỗi (bắt buộc đúng message từ code)
- **[Dữ liệu yêu cầu không hợp lệ]**: **"Dữ liệu yêu cầu không hợp lệ"** (`SaleMessages.INVALID_REQUEST`).
- **[Ga đi/ga đến không hợp lệ]**: **"Ga đi và ga đến không hợp lệ"** (`SaleMessages.INVALID_STATIONS`).
- **[Ngày đi/ngày về không hợp lệ]**: **"Ngày đi/ngày về không hợp lệ"** (`SaleMessages.INVALID_DATES`).
- **[Không tìm thấy chuyến]**: **"Không tìm thấy chuyến tàu phù hợp"** (`SaleMessages.NOT_FOUND_SCHEDULE`).
- **[Giữ ghế thất bại do ghế đã bán]** (khi chốt giao dịch): **"Ghế đã được bán bởi giao dịch khác, vui lòng chọn lại"** (`SaleMessages.SEAT_ALREADY_SOLD`).
- **[Giữ ghế thất bại do quầy khác giữ]**: **"Ghế đang được giao dịch bởi quầy khác"** (`SaleMessages.SEAT_HELD_BY_OTHER`).
- **[Vượt số lượng vé]**: **"Mỗi chiều chỉ được mua tối đa 10 vé"** (`SaleMessages.TOO_MANY_TICKETS_PER_LEG`).
- **[Ràng buộc khứ hồi]**: **"Số lượng ghế chiều đi và chiều về phải bằng nhau"** (`SaleMessages.SEAT_CONSTRAINT_MISMATCH`).
- **[Thiếu giấy tờ người mua]**: **"Cần CCCD/CMND hoặc hộ chiếu"** (`SaleMessages.CUSTOMER_DOCUMENT_REQUIRED`).
- **[Chưa đủ điều kiện thanh toán]**: **"Chưa đủ điều kiện thanh toán"** (`SaleMessages.PAYMENT_NOT_READY`).
- **[Online payment lỗi]**:
  - **"Cần tạo đơn thanh toán online"**, **"Không tìm thấy đơn thanh toán online"**, **"Thanh toán online chưa được xác nhận"**, **"Đơn thanh toán online đã được sử dụng"**, **"Phiên giao dịch không khớp với đơn thanh toán"**, **"Số tiền đơn thanh toán không khớp tổng tiền"**.
- **[Trẻ em dưới 6]**: **"Vé trẻ em bắt buộc phải có người lớn đi kèm"** (`SaleMessages.CHILD_REQUIRES_ADULT`).
- **[Đổi điểm không hợp lệ]**: **"Không được đổi điểm khi có vé ưu đãi đối tượng"** (`SaleMessages.POINTS_NOT_ALLOWED_WITH_DISCOUNT`).

---

## Dữ liệu vào/ra (I/O Data)

### Client → Server (Request.data DTO)
| ActionType | DTO | Field |
|---|---|---|
| `SEARCH_SCHEDULES_FOR_SALE` | `SaleScheduleSearchDTO` | `departureStationId`, `destinationStationId`, `departureDate`, `ticketCategory`, `returnDate`, `page`, `size` |
| `GET_SEATMAP_FOR_SCHEDULE` | `SeatMapRequestDTO` | `scheduleId`, `clientSessionId` |
| `HOLD_SEATS_FOR_SALE` / `RELEASE_HELD_SEATS_FOR_SALE` | `SeatHoldRequestDTO` | `scheduleId`, `scheduleDetailIds`, `clientSessionId` |
| `CREATE_SALE_TRANSACTION` | `SaleCreateRequestDTO` | `clientSessionId`, `ticketCategory`, `outboundScheduleId`, `returnScheduleId`, `outboundScheduleDetailIds`, `returnScheduleDetailIds`, `outboundPassengers`, `returnPassengers`, `childrenUnder6`, `buyer`, `vat`, `redeemPoints`, `paymentMethod`, `amountPaid`, `paymentOrderId` |

### Server → Client (Response.data DTO)
| API | Response.message | Response.data |
|---|---|---|
| Danh sách ga | `SaleMessages.STATION_LIST_SUCCESS` | `List<StationDTO>` |
| Tra cứu chuyến | `SaleMessages.SEARCH_SCHEDULE_SUCCESS` | `SaleScheduleSearchResultDTO` |
| Sơ đồ ghế | `SaleMessages.SEATMAP_SUCCESS` | `SeatMapResponseDTO` |
| Giữ/nhả ghế | `SaleMessages.HOLD_SUCCESS` / `SaleMessages.RELEASE_HOLD_SUCCESS` | `SeatHoldResponseDTO` |
| Bán vé | `SaleMessages.SALE_SUCCESS` | `SaleCreateResponseDTO` |

---

## Use Case Diagram (Mermaid)
```mermaid
graph LR
  Actor[Nhân viên bán vé] --> UC001((UC001 - Bán vé))
  UC001 --> U1[Tra cứu chuyến (SEARCH_SCHEDULES_FOR_SALE)]
  UC001 --> U2[Xem sơ đồ ghế (GET_SEATMAP_FOR_SCHEDULE)]
  UC001 --> U3[Giữ/Nhả ghế (HOLD/RELEASE)]
  UC001 --> U4[Tạo đơn online (CREATE_PAYMENT_ORDER)]
  UC001 --> U5[Xác nhận online (CONFIRM_INTERNAL_PAYMENT)]
  UC001 --> U6[Chốt giao dịch (CREATE_SALE_TRANSACTION)]
```

---

## BA Review — Yêu cầu cập nhật Database / Entity / DTO (Từ BA Review)
- **Vì sao cần `Schedule.arrivalTime`**: phục vụ hiển thị thời gian đến dự kiến trên UI/biên nhận và làm cơ sở kiểm tra logic liên quan (đổi/trả theo mốc thời gian).
- **Vì sao `ScheduleDetail` có `segmentDepartureStation/segmentDestinationStation` và `routeStop = null` khi tạo batch**: hệ thống tạo “tất cả cặp chặng” (mỗi ghế × mọi cặp ga theo route path) để định giá theo chặng và bán vé theo đoạn; `routeStop` không đại diện cho chặng bán mà chỉ là điểm dừng trên tuyến nên khi tạo chi tiết bán vé theo đoạn, `routeStop` không bắt buộc và có thể `null`.
- **Seat hold TTL (10 phút)**: cần chuẩn hóa trong tài liệu nghiệp vụ (quy định giữ ghế tối đa 10 phút) để tránh tranh chấp giữa quầy và giảm ghế “kẹt” không bán được.
- **Trạng thái ghế “SOLD/HELD/AVAILABLE”**: cần thống nhất nguồn dữ liệu; hiện tại SOLD dựa vào `Ticket.status` (không dựa `Seat.available`). Đề xuất BA xác nhận có tiếp tục dùng `Seat.available` hay coi là field dư thừa.
- **Đổi điểm**: quy tắc “không đổi điểm khi có vé ưu đãi theo đối tượng” cần ghi thành requirement rõ ràng (đang enforce bằng code `POINTS_NOT_ALLOWED_WITH_DISCOUNT`).
