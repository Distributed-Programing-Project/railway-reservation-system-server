# UC003 — Trả vé (Refund Ticket) Migration Analysis (Legacy → Socket Client/Server)

Ngày lập: 2026-05-02

## Executive Summary

UC003 (Trả vé) trong codebase **đã có bản “mới”** theo kiến trúc socket client/server:

- Client: `vn.edu.iuh.fit.client.controller.ReturnTicketController` + `src/main/resources/client/ui/views/tra-ve.fxml`
- Common DTO + ActionType đã có: `ReturnTicketSearchDTO`, `ReturnTicketPreviewRequestDTO`, `ReturnTicketPreviewDTO`, `ReturnTicketConfirmDTO`, `ReturnTicketTicketDTO` + `ActionType.*RETURN*`
- Server: `TicketServiceImpl` đã có 3 API: `searchTicketsForReturn` / `previewReturnTickets` / `confirmReturnTickets` với **transaction boundary ở server**, tạo `Invoice(type=REFUND)` và update `Ticket(status=RETURNED)` + release seat availability.

Tuy nhiên, vẫn còn **gap quan trọng** so với bản cũ và so với chính `docs/usecases/uc003-tra-ve.md`:

1. **Search** hiện chỉ theo `idCard` (người mua) — **chưa hỗ trợ** tìm theo **mã vé** và **giấy tờ hành khách** như bản cũ/must-keep.
2. **Vé đã đổi**: code server đang **từ chối** vé exchanged, trong khi rule/must-keep là **cho phép trả với phí 30%**.
3. **In/preview biên lai trả vé**: UI hiện in “tóm tắt” bằng `ticket_template.xml` (TicketRenderer) thay vì ưu tiên template `bien-lai-tra-ve.xml` (JRXML) như yêu cầu.
4. **Rounding fee**: bản cũ làm tròn phí lên bội `1000` — bản mới chưa có.

Phần còn lại (server source-of-truth, chống double refund, tạo refund invoice + invoice details, release seat) là **đúng hướng** và có thể reuse trực tiếp.

---

# PHẦN A — TÓM TẮT BẢN CŨ

## A1) `migration/TraVeController.java`

### 1) UI fields/table/detail pane

- Search: `txtSearchCCCD` + `btnTimKiem`
- Table: `tblDanhSachVe` với các cột:
  - `colMaVe`, `colTau`, `colHanhTrinh`, `colNgayDi`, `colGhe`, `colGiaVe`, `colTrangThai`
- Detail pane: `paneChiTietTra` + labels:
  - `lblMaVeChon`, `lblTau`, `lblHanhTrinh`, `lblNgayDi`, `lblGhe`, `lblThoiGianConLai`, `lblThongBaoLoi`
- Refund box:
  - `lblDieuKienVe`, `lblGiaVeGoc`, `lblPhiTraVe`, `lblTienHoanLai`, `btnXacNhanTra`
- Có mode đổi vé dùng chung màn:
  - `btnDoiVe`, `boxTinhTien` hide/show theo mode `transactionType`

### 2) Flow tìm kiếm

- Input `identifier`:
  - Nếu là **12 digits** hoặc `QR_...` → gọi `VeTauDAO.getVeTauDetail(identifier)` (tìm theo **mã vé/QR**)
  - Nếu không → `KhachHangDAO.timKhachHangTheoGiayTo(identifier)` (tìm khách theo **CCCD/Hộ chiếu**)
    - rồi `VeTauDAO.getLichSuVeCuaKhachHang(maKH)` (lịch sử mua)

### 3) Rule filter vé trả được

- Chỉ đưa vào list vé có `trangThai == "DaBan"` (và các check khác lúc chọn vé)

### 4) Rule chọn vé và hiển thị chi tiết

- Khi chọn `selectedVe`:
  - Render thông tin tàu / hành trình / ngày đi / ghế
  - Tính thời gian còn lại (hoursDiff)
  - Với mode **TRẢ VÉ**: hiển thị block tính tiền, enable/disable nút theo eligibility

### 5) Rule tính thời gian còn lại

- `hoursDiff = ChronoUnit.HOURS.between(now, gioKhoiHanh)`
- Nếu `hoursDiff < 4` → không đủ điều kiện trả

### 6) Rule tính phí trả vé

Trong mode trả vé:

- Nếu `hoursDiff < 4` → reject
- Nếu vé “đã đổi” (dựa theo `ghiChu` chứa “vé đã đổi”) → fee rate = `30%`
- Else:
  - `hoursDiff < 24` → fee rate = `20%`
  - `hoursDiff >= 24` → fee rate = `10%`
- **Min fee**: `max(price * rate, 10_000)`
- **Round up**: `ceil(fee / 1000) * 1000`
- Refund amount = `price - fee`

### 7) Flow xác nhận trả

- UI confirm dialog
- Gọi `TraVeService.processTraVe(selectedVe, calculatedRefundAmount, maNhanVien)`
  - (transaction làm trực tiếp trên DB từ client)
- Thành công:
  - Tạo `JasperPrint` từ template `bien-lai-tra-ve.xml`
  - Preview dialog và có nút print

### 8) Flow tạo/preview/in biên lai trả vé

- JasperReports:
  - `JRXmlLoader.load("/views/bien-lai-tra-ve.xml")`
  - `JasperCompileManager.compileReport`
  - `JasperFillManager.fillReport(..., new JREmptyDataSource())`
  - Preview: `JasperPrintManager.printPageToImage` → show `ImageView`
  - Print: `JasperPrintManager.printReport(jasperPrint, true)`

---

## A2) `migration/TraVeService.java`

### 1) DB transaction đang làm gì

- Open JDBC connection, `setAutoCommit(false)`
- Update ticket:
  - `VeTau.trangThai = "DaHuy"`
  - `VeTau.maQR = "INVALID_..."` (invalidate QR)
- Resolve schedule detail id:
  - query `chiTietLichTrinhId` by `maVe`
- Release seat/schedule detail:
  - `ChiTietLichTrinh.trangThai = "ConTrong"`
- Tạo refund invoice:
  - `HoaDon(loaiHoaDon="HoanTien", tongTienHoaDon=0)`
  - `ChiTietHoaDon(isTraVe=true, soTienHoanLai=..., BAO_HIEM=2000, ...)`

### 2) Các điểm cần chuyển sang server service mới

Toàn bộ các bước **phải** thuộc server-side transaction:

- Validate ticket returnability (status/time)
- Fee calculation + final refund amount
- Update ticket status + invalidate QR
- Release seat / scheduleDetail
- Create refund invoice + invoice details
- Concurrency/double refund prevention

---

## A3) `migration/bien-lai-tra-ve.xml`

### 1) Template format

- Là **JasperReports JRXML** (`<jasperReport ...>`)

### 2) Parameters/fields cần truyền

Parameters (tổng hợp):

- Transaction: `p_MaGiaoDich`, `p_NgayTra`, `p_NhanVien`
- Ticket: `p_MaVe`
- Customer: `p_KhachHang`, `p_SoGiayTo`
- Trip: `p_Tau`, `p_GaDi`, `p_GaDen`, `p_NgayDi`, `p_Toa`, `p_Ghe`
- Money: `p_GiaVeGoc`, `p_LePhi`, `p_ThucNhan`

### 3) So sánh với dữ liệu mới (server)

Hiện server confirm trả vé trả về `refundInvoiceId` (Response.data) và có đủ entity để suy ra nhiều field:

- Có thể derive từ `Invoice(REFUND)`, `InvoiceDetail`, `Ticket`, `ScheduleDetail`, `Schedule`, `Seat/Carriage`, `Customer`, `Employee`.

Nhưng **DTO/response hiện tại chưa trả** về đầy đủ dữ liệu “biên lai” cho client, nên nếu muốn in đúng template JRXML thì cần:

- API **query receipt by refundInvoiceId** hoặc
- confirm API trả kèm `RefundReceiptDTO` (khuyến nghị: tách “get receipt” để in lại)

### 4) Field nào có/thiếu (tại boundary hiện tại)

- Có sẵn sau confirm (server DB): **đủ** (invoiceId, issueDate, employee, ticket details…)
- Thiếu ở response/DTO cho client: gần như **tất cả** field JRXML trừ `refundInvoiceId`

### 5) Đề xuất reuse template trong client mới

- Giữ `bien-lai-tra-ve.xml` trong `classpath` (`/client/print/bien-lai-tra-ve.xml`)
- Client có “print action”:
  - Gọi server lấy `RefundReceiptDTO` (theo `refundInvoiceId`)
  - Jasper fill report bằng param map đúng như template
- Font Việt: template đang dùng `fontName="SansSerif"` → cần mapping font extension (xem GAP).

---

# PHẦN B — TÓM TẮT BẢN MỚI

## B1) Project mới đã có UC trả vé chưa?

Có.

### Client

- Controller: `src/main/java/vn/edu/iuh/fit/client/controller/ReturnTicketController.java`
- FXML: `src/main/resources/client/ui/views/tra-ve.fxml`
- Client service: `src/main/java/vn/edu/iuh/fit/client/service/ReturnTicketClientService.java`

### Common / Network

- ActionType:
  - `SEARCH_TICKETS_FOR_RETURN`
  - `PREVIEW_RETURN_TICKETS`
  - `CONFIRM_RETURN_TICKETS`
- DTO:
  - `ReturnTicketSearchDTO`
  - `ReturnTicketPreviewRequestDTO`
  - `ReturnTicketPreviewDTO`
  - `ReturnTicketConfirmDTO`
  - `ReturnTicketTicketDTO`
- Router mapping: `src/main/java/vn/edu/iuh/fit/server/network/RequestRouter.java`

### Server

- Service interface + impl:
  - `TicketService.searchTicketsForReturn`
  - `TicketService.previewReturnTickets`
  - `TicketService.confirmReturnTickets`
  - `TicketServiceImpl` implements:
    - read-only search/preview
    - transactional confirm:
      - create `Invoice(type=REFUND)`
      - create `InvoiceDetail(refundAmount, isReturned=true)`
      - mark SALE invoice details as returned
      - set `Ticket.status = RETURNED`, invalidate QR
      - release seat availability
      - return `refundInvoiceId`

## B2) Điểm đã tốt

- **Không DAO/JPA từ client**: client chỉ gọi socket request service.
- **Server transaction boundary**: confirm chạy trong transactional wrapper.
- **Server source-of-truth**:
  - validate status/time
  - compute fee/refund amount
  - concurrency handling (OptimisticLock)
  - chống double refund (status check)
- Data model có `InvoiceType.REFUND`, `InvoiceDetail.isReturned/refundAmount`.

## B3) Điểm còn thiếu / lệch so với bản cũ

- Search criteria chưa đầy đủ (mã vé, giấy tờ hành khách).
- Vé đã đổi đang bị từ chối (code) dù docs + bản cũ cho phép trả 30%.
- Print receipt chưa dùng `bien-lai-tra-ve.xml`.
- Fee rounding up to 1000 chưa có.

---

# PHẦN C — GAP ANALYSIS

## C1) Bảng rule/UX

| Rule/UX bản cũ | Bản mới hiện có | Giữ/đổi | Client hay Server | DTO/API cần có |
|---|---|---|---|---|
| Tìm theo **mã vé** (12 digits/QR_) | Chưa có (client chỉ gửi `idCard`) | MUST KEEP | Server-side lookup | `ReturnTicketSearchDTO` mở rộng: `query`, `queryType` hoặc `ticketId` |
| Tìm theo **CCCD/Hộ chiếu người mua** | Có (search by `idCard`, likely customer) | MUST KEEP | Server lookup | OK |
| Tìm theo **CCCD/Hộ chiếu hành khách** (fallback) | Chưa rõ / chưa có | MUST KEEP | Server lookup | Search API hỗ trợ passengerIdCard |
| Filter vé trả được (status) | Có: server requires `TicketStatus.PAID` | MUST KEEP | Server | OK |
| Điều kiện thời gian ≥ 4h | Có: server enforces `minutesToDeparture >= 4h` | MUST KEEP | Server | OK |
| Fee: <24h = 20%, ≥24h = 10% | Có: server computes rate theo minutes | MUST KEEP | Server | OK |
| Vé đã đổi fee 30% | Docs có, bản cũ có; **server code đang reject exchanged** | MUST KEEP | Server-only | Fix server compute rule |
| Min fee 10.000 | Có: `MIN_RETURN_FEE_PER_TICKET = 10_000` | MUST KEEP | Server | OK |
| Round up to 1000 | Chưa có | MUST KEEP | Server | Add rounding in compute |
| Confirm dialog + trả nhiều vé | Có (multi-select + confirm) | MUST KEEP | Client UI + server confirm | OK |
| Update ticket status + invalidate QR | Có | SERVER-SIDE ONLY | Server | OK |
| Release seat availability | Có (set seat.available=true) | SERVER-SIDE ONLY | Server | OK |
| Create refund invoice + invoice detail | Có | SERVER-SIDE ONLY | Server | OK |
| Chống double refund | Có (status check + optimistic lock) | SERVER-SIDE ONLY | Server | OK |
| Preview/in biên lai bằng `bien-lai-tra-ve.xml` | Chưa có (UI in summary via `ticket_template.xml`) | MUST KEEP | Client print (data from server) | `RefundReceiptDTO` + new action `GET_REFUND_RECEIPT` |

## C2) Phân loại bắt buộc

### MUST KEEP

- Tìm vé theo **mã vé / CCCD / hộ chiếu**
- Chỉ trả vé hợp lệ (status/time)
- Phí trả vé theo thời gian + vé đã đổi
- Phí tối thiểu + làm tròn theo rule cũ
- Chứng từ hoàn tiền trong DB (Invoice/InvoiceDetail)
- Preview/in biên lai trả vé bằng `bien-lai-tra-ve.xml`

### SERVER-SIDE ONLY

- Xác nhận vé trả được
- Tính số tiền hoàn cuối cùng
- Update trạng thái ticket + invalidate QR
- Release schedule detail/seat availability theo model mới
- Tạo invoice/refund invoice + invoice detail
- Chống double refund / optimistic lock

### CLIENT PREVIEW ONLY

- Hiển thị phí dự kiến + điều kiện (wording)
- Cảnh báo điều kiện
- Preview biên lai

### CAN CHANGE

- Wording UI, button names, layout preview renderer

---

# PHẦN D — ĐỀ XUẤT KIẾN TRÚC MỚI (Spec để code phase sau)

## D1) Client

- Controller: giữ `ReturnTicketController` (đã có) nhưng refactor theo phases:
  - Search: hỗ trợ query type (ticketId / idCard)
  - Print receipt: dùng `bien-lai-tra-ve.xml` thay vì “ticket summary”
- FXML: giữ `tra-ve.fxml` (đã có), update prompt/UX tùy spec.
- Client service:
  - Extend `ReturnTicketClientService` thêm `getRefundReceipt(refundInvoiceId)` (hoặc tương đương)
- DTO mới đề xuất (để in đúng template):
  - `RefundReceiptRequestDTO { String refundInvoiceId }`
  - `RefundReceiptDTO { ...fields mapping JRXML params... }`
    - `maGiaoDich`, `ngayTra`, `nhanVien`
    - `maVe`, `khachHang`, `soGiayTo`
    - `tau`, `gaDi`, `gaDen`, `ngayDi`, `toa`, `ghe`
    - `giaVeGoc`, `lePhi`, `thucNhan`

## D2) Server

- TicketService hiện đã có đúng 3 action, cần bổ sung:
  - `GET_REFUND_RECEIPT` (read-only)
    - validate invoiceId exists + type=REFUND
    - build `RefundReceiptDTO` from DB
- Adjust compute/eligibility rules:
  - allow exchanged tickets with 30% fee instead of reject
  - implement rounding up fee to 1000 (server-only)
- Ensure search supports:
  - by ticketId/QR (single ticket lookup)
  - by idCard of buyer **or** passenger (return list)

## D3) ActionType đề xuất (giữ naming hiện có + bổ sung)

Existing (đã có):

- `SEARCH_TICKETS_FOR_RETURN`
- `PREVIEW_RETURN_TICKETS`
- `CONFIRM_RETURN_TICKETS`

Add:

- `GET_REFUND_RECEIPT`

---

# PHẦN E — BIÊN LAI / HÓA ĐƠN TRẢ VÉ

## 1) Có nên tạo invoice/refund document khi trả vé không?

**Có** (bắt buộc) — và bản mới đã làm đúng:

- `Invoice(type=REFUND)` + `InvoiceDetail(refundAmount, isReturned=true)` được tạo trong transaction.
- In/không in chỉ là UI action, không ảnh hưởng việc persist chứng từ.

## 2) Tên nghiệp vụ

- `InvoiceType.REFUND` hiện đang dùng name “Hoàn vé” → phù hợp.

## 3) UI wording

- Nên gọi: “In biên lai hoàn tiền” / “In phiếu hoàn tiền”
- Không gọi là hóa đơn bán vé.

## 4) Template ưu tiên

- `src/main/resources/client/print/bien-lai-tra-ve.xml` (JRXML)

### Lưu ý font Việt cho JRXML

Template hiện dùng `fontName="SansSerif"`:

- Nếu Jasper font extension chỉ định “Arial” → template **sẽ không tự dùng**.
- Có 2 cách (để phase code sau quyết định):
  1) Update template đổi `SansSerif` → `Arial`
  2) Font extension thêm `<fontFamily name="SansSerif"> ... arial.ttf ... </fontFamily>`

---

# PHẦN F — ROADMAP (phases để triển khai sau)

> Lưu ý: repo đã có phần lớn UC003; roadmap dưới đây tập trung “migrate hoàn chỉnh” theo must-keep + template in.

## Phase 0: Analysis + DTO/API spec (RECOMMENDED FIRST)

- Files touched (docs only):
  - `docs/uc003-refund-ticket-migration-analysis.md`
- Goal:
  - Chốt rule, DTO, action types, receipt mapping.
- Commands:
  - None
- Manual checklist:
  - Agree: search modes + exchanged-ticket refund policy + rounding rule + receipt fields.

## Phase 1: UI/controller skeleton hardening

- Files touched:
  - `ReturnTicketController` + `tra-ve.fxml` (nếu cần)
- Goal:
  - Split UI state machine: search → select → preview → confirm → print.
- Commands:
  - `mvn clean compile`, `mvn test -Dtest=FxmlLoadSmokeTest,PreviewRendererTest`
- Checklist:
  - Không block UI thread, cancel pending tasks properly.

## Phase 2: Lookup ticket/customer (search upgrade)

- Goal:
  - Support search by ticketId + buyer/passenger idCard.
- Server-side:
  - Extend repository queries.
- DTO/API:
  - Expand `ReturnTicketSearchDTO` or add new DTO.

## Phase 3: Refund preview calculation (rule parity)

- Goal:
  - Align with must-keep:
    - exchanged ticket allowed with 30%
    - rounding up fee to 1000
- Note:
  - Client chỉ display, server computes.

## Phase 4: Confirm refund server transaction (already mostly done)

- Goal:
  - Ensure idempotency/double refund prevention robust (optional: add unique constraints / checks).

## Phase 5: Print refund receipt using `bien-lai-tra-ve.xml`

- Goal:
  - Implement `GET_REFUND_RECEIPT` + Jasper fill from params.
- Client:
  - Replace current “summary ticket preview” with receipt preview.
- Font:
  - Ensure Identity-H + embedded (reuse font extension).

## Phase 6: Manual regression

- Checklist:
  - Return 1 ticket / multiple tickets
  - Borderline time rules (3h59 reject, 4h accept)
  - Exchanged ticket refund fee 30%
  - Seat released visible in seatmap
  - Receipt prints Vietnamese correctly

---

# PHẦN G — REPORT DELIVERABLES

This document is the migration spec for the next coding turns.

