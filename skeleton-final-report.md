# **LỜI MỞ ĐẦU**

Trong bối cảnh chuyển đổi số diễn ra sâu rộng, hoạt động bán vé tàu hỏa cần được tin học hóa nhằm nâng cao chất lượng phục vụ hành khách, giảm tải áp lực tại quầy và tối ưu năng lực vận hành của nhà ga. Hệ thống bán vé truyền thống/legacy theo mô hình nguyên khối thường gặp hạn chế khi số lượng giao dịch tăng cao: khó mở rộng, khó bảo trì, và đặc biệt dễ phát sinh xung đột dữ liệu khi nhiều nhân viên thao tác đồng thời trên cùng một lịch trình và tài nguyên ghế.

Đề tài “**Hệ thống Bán vé Tàu hỏa Phân tán**” được thực hiện nhằm nâng cấp hệ thống lên kiến trúc **Client–Server** hiện đại, giao tiếp **TCP Socket** theo mô hình **Request/Response** (truyền nhận đối tượng qua ObjectStream), kết hợp thiết kế **N‑Tier** và lưu trữ tập trung bằng **MariaDB** thông qua **JPA/Hibernate**. Cách tiếp cận này giúp hệ thống xử lý đồng thời tốt hơn, chuẩn hóa quy trình nghiệp vụ (bán/đổi/trả vé, ưu đãi, tích điểm), đồng thời đảm bảo tính nhất quán dữ liệu trong môi trường nhiều người dùng.

---

# **CHƯƠNG 1: GIỚI THIỆU**

## **1.1 Tổng quan**

Theo các tài liệu legacy, bài toán bán vé tàu hỏa tại nhà ga gồm nhiều nghiệp vụ liên quan chặt chẽ: quản lý lịch trình – tàu – tuyến đường, quản lý ghế theo từng toa, bán vé theo đối tượng ưu đãi, đổi vé/trả vé theo mốc thời gian và quy định phí. Trong giờ cao điểm, một hệ thống kém tối ưu có thể dẫn đến:
- Trùng ghế hoặc giữ ghế không kiểm soát khi nhiều nhân viên thao tác.
- Sai lệch khi áp dụng ưu đãi/hoàn phí do quy tắc nghiệp vụ không được ràng buộc thống nhất.
- Khó truy vết giao dịch, khó đối soát doanh thu khi dữ liệu phân tán/không nhất quán.

Vì vậy, việc nâng cấp hệ thống theo hướng phân tán Client–Server là cần thiết để:
- Tăng khả năng phục vụ đồng thời (nhiều máy trạm/nhân viên).
- Chuẩn hóa và tự động hóa các quy trình nghiệp vụ cốt lõi.
- Đảm bảo dữ liệu tập trung, toàn vẹn và dễ mở rộng.

## **1.2 Mục tiêu đề tài**

Mục tiêu của đề tài bao gồm:
- Số hóa quy trình bán vé tàu hỏa tại quầy với dữ liệu tập trung.
- Nâng cấp kiến trúc từ nguyên khối lên **Client–Server**.
- Sử dụng **TCP Socket** để trao đổi dữ liệu, chuẩn hóa giao thức nghiệp vụ theo **ActionType**.
- Tách biệt trách nhiệm:
  - Client tập trung vào UI/UX, kiểm tra nhập liệu cơ bản và hiển thị kết quả.
  - Server tập trung vào xử lý nghiệp vụ, kiểm soát quy tắc và giao dịch dữ liệu.
- Thiết kế theo **N‑Tier**: Presentation – Common – Service – Data Access.
- Ứng dụng **MariaDB/JPA/Hibernate** để quản lý thực thể, ràng buộc toàn vẹn và transaction.
- Giải quyết các bài toán nghiệp vụ nổi bật:
  - Tự động tạo hồ sơ khách hàng khi bán vé (**Auto‑Registration**).
  - Hỗ trợ tra cứu/phân trang danh sách khách hàng (**Pagination**).
  - Kiểm soát đồng thời khi chọn/giữ ghế và phát hành vé.

## **1.3 Phạm vi đề tài**

Phạm vi sử dụng:
- **Nhân viên bán vé** và **Nhân viên quản lý** tại nhà ga.

Các phân hệ chính:
- Bán vé (một chiều/khứ hồi), chọn ghế, tính tiền, in/hiển thị vé.
- Quản lý tàu, toa, ghế; quản lý tuyến đường, điểm dừng và lịch trình.
- Quản lý khách hàng/nhân viên; xem lịch sử, tra cứu nhanh.
- Đổi vé, trả vé theo ràng buộc thời gian và phí.
- Thống kê doanh thu/số lượng vé theo thời gian và tiêu chí lọc.

## **1.4 Mô tả yêu cầu chức năng**

Các yêu cầu chức năng mức cao:
- **Đăng nhập** và phân quyền cơ bản theo vai trò.
- **Quản lý tuyến/ga/lịch trình**:
  - Tạo/cập nhật lịch trình; phát hành/vô hiệu hóa lịch trình.
  - Tra cứu tuyến đường, các điểm dừng và danh sách ga.
- **Bán vé**:
  - Tra cứu lịch trình phù hợp và xem sơ đồ ghế.
  - Giữ ghế tạm thời theo phiên làm việc; xác nhận thanh toán để phát hành vé.
  - Tự động tạo/cập nhật hồ sơ khách hàng khi mua vé.
  - Áp dụng ưu đãi theo đối tượng, tính tổng tiền và xuất hóa đơn.
- **Đổi vé**:
  - Tra cứu vé đủ điều kiện đổi; xem trước phí/tiền chênh lệch; xác nhận đổi.
- **Trả vé**:
  - Tra cứu vé đủ điều kiện trả; xem trước phí khấu trừ/tiền hoàn; xác nhận trả.
- **Quản lý khách hàng**:
  - Tra cứu theo từ khóa; **phân trang**; xem lịch sử mua vé.
  - Thêm/sửa/xóa theo điều kiện nghiệp vụ.
- **Quản lý tàu/toa/ghế**:
  - Tạo tàu, cập nhật trạng thái, gán toa; quản lý cấu hình vận hành.
- **Thống kê**:
  - Thống kê theo thời gian (ngày/tháng/quý), theo loại nghiệp vụ (bán/đổi/trả), theo tuyến/lịch trình.

## **1.5 Mô tả yêu cầu phi chức năng**

Các yêu cầu phi chức năng trọng yếu:
- **Kiến trúc N‑Tier** giúp tách biệt trách nhiệm, tăng khả năng bảo trì và tái sử dụng.
- **Hiệu năng giao tiếp TCP Socket**:
  - Thời gian phản hồi ổn định, có timeout đọc/ghi và xử lý lỗi kết nối.
  - Dữ liệu truyền theo đối tượng giúp giảm mã hóa/giải mã thủ công, tăng tốc độ phát triển.
- **Xử lý đồng thời khi đặt/giữ ghế**:
  - Cơ chế giữ ghế theo phiên để giảm tranh chấp.
  - Cơ chế phát hiện xung đột khi nhiều giao dịch đồng thời.
- **Tính nhất quán dữ liệu**:
  - Transaction cho các thao tác tạo hóa đơn/vé/đổi/trả.
  - Ràng buộc trạng thái vé và khóa lạc quan để ngăn bán trùng ghế.

---

# **CHƯƠNG 2: PHÂN TÍCH YÊU CẦU VÀ NGHIỆP VỤ**

## **2.1 Mô hình Use Case**

***[Insert Sơ đồ Use Case tổng quát tại đây]***

Sơ đồ Use Case tổng quát thể hiện các nhóm nghiệp vụ chính: bán vé, đổi vé, trả vé, quản lý khách hàng/nhân viên, quản lý tàu–tuyến–lịch trình và thống kê. Các Use Case trọng yếu (Bán/Đổi/Trả) được yêu cầu kiểm soát đồng thời và toàn vẹn dữ liệu.

## **2.2 Danh sách tác nhân (Actors)**

*   **Nhân viên bán vé**
    *   Thực hiện bán vé tại quầy (chọn lịch trình, chọn ghế, nhập thông tin hành khách, thanh toán).
    *   Thực hiện đổi vé/trả vé theo quy định.
    *   Tra cứu khách hàng và lịch sử mua vé.
*   **Nhân viên quản lý**
    *   Quản trị dữ liệu nền (tàu, toa, tuyến, ga, lịch trình).
    *   Quản lý nhân viên và theo dõi thống kê.

## **2.3 Các quy định nghiệp vụ cốt lõi (Business Rules)**

Phần này trình bày các ràng buộc nghiệp vụ từ hệ thống legacy và cách hệ thống mới kiểm soát/hiện thực chúng trong kiến trúc Client–Server.

*   **Nghiệp vụ Bán vé:**
    *   **Giới hạn số lượng vé**
        *   Tối đa **10 vé/ giao dịch** (trong hệ thống hiện tại được kiểm soát theo từng chiều đối với vé khứ hồi).
        *   Với vé **khứ hồi**, số lượng vé chiều đi và chiều về phải **tương ứng** (đảm bảo số hành khách đồng nhất giữa hai chiều).
    *   **Quy định tính giá**
        *   Công thức nghiệp vụ (legacy):  
            *   Giá vé = **Giá cơ bản × Hệ số km × Hệ số chỗ + Bảo hiểm**
        *   Nguyên tắc hiện thực (hệ thống mới):
            *   Giá ghế được cấu hình theo **lịch trình chi tiết**.
            *   Hóa đơn lưu **subTotal** (giá trước giảm) và **discount** (mức giảm) cho từng dòng.
    *   **Chính sách ưu đãi**
        *   Trẻ em **< 6 tuổi**
            *   Không chiếm ghế; phải đi kèm người lớn.
            *   Hệ thống cấp “vé/phiếu trẻ em” với giá trị 0 (đi kèm vé người lớn).
        *   Trẻ em **6–10 tuổi**
            *   Áp dụng mức giảm theo quy định.
        *   **Người cao tuổi**
            *   Áp dụng mức giảm theo quy định.
        *   **Sinh viên**
            *   Áp dụng mức giảm theo quy định.
    *   **Ràng buộc kèm người lớn**
        *   Nếu giao dịch chỉ có vé trẻ em mà không có vé người lớn → từ chối giao dịch.
    *   **Đảm bảo đồng thời và chống trùng ghế**
        *   Ghế được **giữ tạm thời theo phiên** để giảm tranh chấp khi nhiều nhân viên thao tác.
        *   Khi xác nhận bán, hệ thống kiểm tra ghế đã bán/chưa bán theo trạng thái vé và cơ chế phát hiện xung đột.

*   **Nghiệp vụ Đổi vé:**
    *   **Điều kiện đổi**
        *   Thực hiện **trước 24h** so với giờ khởi hành.
        *   **Cùng ga đi/ga đến** (theo quy định legacy; khi hiện thực cần ràng buộc theo tuyến/lịch trình tương ứng).
        *   **Đổi 1 lần** (mỗi vé chỉ được đổi một lần theo chính sách).
    *   **Phí đổi**
        *   Phí đổi cố định: **20.000đ/vé**.
    *   **Quy định chênh lệch giá**
        *   Nếu vé mới cao hơn: thu **chênh lệch + phí đổi**.
        *   Nếu vé mới thấp hơn: xử lý theo quy định (tối thiểu vẫn thu phí đổi).
    *   **Cập nhật trạng thái và ghế**
        *   Vé cũ chuyển trạng thái **đã đổi** để giải phóng ghế.
        *   Vé mới được phát hành gắn với ghế mới.

*   **Nghiệp vụ Trả vé:**
    *   **Điều kiện thời gian**
        *   Không cho trả khi còn **dưới 4h** tới giờ khởi hành.
    *   **% phí khấu trừ**
        *   Trước **24h**: trừ **10%**.
        *   Từ **4h–24h**: trừ **20%**.
        *   Vé **đã đổi**: trừ **30%**.
    *   **Lệ phí tối thiểu**
        *   Tối thiểu **10.000đ/vé** (đảm bảo phí không nhỏ hơn mức sàn).
    *   **Nguyên tắc kiểm soát**
        *   Tính toán tiền hoàn/khấu trừ nhất quán và ghi nhận chứng từ hoàn.
        *   Kiểm soát trả theo lô và xác minh người đại diện mua (nếu có quy định vận hành).

*   **Nghiệp vụ Quản lý Khách hàng:**
    *   **Tự động tạo hồ sơ khách hàng khi mua vé (Auto‑Registration)**
        *   Khi bán vé, nếu không có hồ sơ khách hàng tương ứng theo giấy tờ (CCCD/Hộ chiếu) → hệ thống tự động tạo.
        *   Nếu đã tồn tại → cập nhật thông tin liên hệ (SĐT/Email) nếu có.
        *   Áp dụng cho cả **người mua** và **hành khách** (khi đủ thông tin giấy tờ).
    *   **Tích điểm**
        *   Quy đổi: **10.000đ = 1 điểm**.
        *   Điểm cộng sau khi giao dịch thanh toán thành công và ghi nhận hóa đơn.
    *   **Đổi điểm**
        *   Mức giảm tối đa: **10% giá vé** cho một giao dịch.
        *   Không cho phép đồng thời đổi điểm và áp dụng ưu đãi theo đối tượng (tránh chồng khuyến mãi).

*   **Nghiệp vụ Quản lý Tàu & Lịch trình:**
    *   **Trạng thái tàu**
        *   Quy định các trạng thái vận hành (đang hoạt động/ngưng hoạt động/bảo trì...) để quyết định khả năng đưa vào lịch trình.
    *   **Trạng thái lịch trình**
        *   Lịch trình có vòng đời (khởi tạo → phát hành → vô hiệu hóa) phục vụ kiểm soát bán vé theo thời gian.
    *   **Quy định cấu hình toa**
        *   Một tàu có nhiều toa; mỗi toa có ghế và loại ghế.
        *   Giá ghế được cấu hình theo lịch trình chi tiết, có thể theo phân đoạn tuyến/điểm dừng.

---

# **CHƯƠNG 3: THIẾT KẾ VÀ HIỆN THỰC**

## **3.1 Thiết kế Kiến trúc Phân tán (Distributed Architecture)**

Hệ thống vận hành theo mô hình **Client–Server**:
- **Client**: ứng dụng JavaFX tại quầy, cung cấp giao diện, thu thập dữ liệu, gửi yêu cầu nghiệp vụ.
- **Server**: ứng dụng Socket Server, tiếp nhận yêu cầu, định tuyến xử lý, tương tác CSDL và phản hồi kết quả.

Giao tiếp TCP Socket theo ObjectStream:
- Client gửi `Request(ActionType, DTO)` qua `ObjectOutputStream`.
- Server đọc Request qua `ObjectInputStream`, dùng Router ánh xạ ActionType → Service tương ứng.
- Service xử lý nghiệp vụ và trả `Response(success, message, data)` về Client.

Phân tách N‑Tier:
- **Presentation Layer**: JavaFX UI (FXML/Controller/CSS), phân trang danh sách (Pagination) và luồng thao tác bán vé theo bước.
- **Common Layer**: các DTO, Request/Response, ActionType, enums trạng thái và message chuẩn hóa.
- **Service Layer**: nơi hiện thực business rules (bán/đổi/trả, auto‑registration, tính điểm/đổi điểm...).
- **Data Access Layer**: repository JPA/Hibernate, truy vấn phân trang, truy vấn nghiệp vụ theo trạng thái vé/ghế.

***[Insert Sơ đồ Kiến trúc Hệ thống N-Tier tại đây]***

## **3.2 Mô hình lớp (Class Diagram)**

Các thực thể cốt lõi (phù hợp với JPA/Hibernate) và vai trò nghiệp vụ:
- **Customer**: lưu thông tin khách hàng, giấy tờ và điểm thưởng; liên kết với danh sách vé.
- **Ticket**: vé phát hành theo giao dịch bán/đổi; liên kết khách hàng và (nếu có) lịch trình chi tiết.
- **Schedule / ScheduleDetail**: lịch trình chạy tàu và chi tiết ghế theo lịch trình; hỗ trợ khóa lạc quan để kiểm soát xung đột.
- **Train / Carriage / Seat**: mô hình tàu–toa–ghế phục vụ cấu hình và hiển thị sơ đồ ghế.
- **Route / Station / RouteStop**: mô hình tuyến đường và các ga; hỗ trợ cấu hình điểm đi/đến và dừng.
- **Invoice / InvoiceDetail**: ghi nhận giao dịch và dòng chi tiết của vé (giá, giảm giá, hoàn trả...).

***[Insert Biểu đồ Class Diagram Thực thể tại đây]***

## **3.3 Cơ sở dữ liệu**

Hệ thống sử dụng **MariaDB** kết hợp **JPA/Hibernate** để ánh xạ dữ liệu:
- MariaDB đảm bảo lưu trữ tập trung, dễ quản trị và tối ưu truy vấn.
- JPA/Hibernate giúp chuẩn hóa mô hình thực thể, giảm mã truy vấn thủ công và hỗ trợ transaction/locking.

Nguyên tắc thiết kế dữ liệu:
- Khóa chính dùng UUID giúp thuận tiện khi mở rộng phân tán.
- Dữ liệu nghiệp vụ quan trọng (vé, hóa đơn, chi tiết lịch trình/ghế) được thiết kế để đảm bảo toàn vẹn và truy vết.

***[Insert Sơ đồ EER CSDL tại đây]***

### **3.3.1 Các ràng buộc toàn vẹn**

Các ràng buộc đề xuất/áp dụng:
- **Ràng buộc khóa và duy nhất**
  - Mã tàu (`train_code`) là duy nhất.
  - Một số trường định danh theo nghiệp vụ (giấy tờ khách hàng) cần đảm bảo không trùng theo chính sách.
- **Ràng buộc tham chiếu**
  - Ticket tham chiếu Customer; Ticket (vé ghế) tham chiếu ScheduleDetail.
  - Schedule tham chiếu Train và Route.
  - Route tham chiếu Station (ga đi/ga đến).
- **Ràng buộc trạng thái**
  - Ghế không được bán trùng: tại cùng một ScheduleDetail chỉ được tồn tại tối đa một Ticket ở trạng thái hợp lệ (không thuộc nhóm hủy/đổi/trả).
- **Ràng buộc transaction**
  - Tạo hóa đơn, tạo vé và tạo chi tiết hóa đơn phải đồng bộ trong một transaction.

## **3.4 Thiết kế Luồng tuần tự (Sequence Diagrams)**

Luồng dữ liệu từ UI đến CSDL qua TCP Socket:
- **Client UI**
  - Thu thập dữ liệu và kiểm tra nhập liệu cơ bản.
  - Đóng gói DTO và gọi dịch vụ gửi socket.
- **ObjectStream Transport**
  - Gửi/nhận đối tượng Request/Response.
- **Server Router**
  - Nhận Request, định tuyến theo ActionType, gọi Service tương ứng.
- **Service + Repository**
  - Kiểm tra ràng buộc nghiệp vụ, thao tác dữ liệu trong transaction, trả kết quả về Client.

***[Insert Sơ đồ Sequence Bán vé qua TCP Socket tại đây]***
***[Insert Sơ đồ Sequence Đổi vé qua TCP Socket tại đây]***
***[Insert Sơ đồ Sequence Trả vé qua TCP Socket tại đây]***

## **3.5 Thiết kế Giao diện (UI/UX)**

***[Insert Sơ đồ Screen Flow tại đây]***

Hệ thống sử dụng **JavaFX** để xây dựng giao diện theo mô hình:
- FXML định nghĩa bố cục.
- Controller xử lý sự kiện và gọi SocketRequestService.
- CSS thống nhất phong cách hiển thị, tối ưu trải nghiệm thao tác tại quầy.

Các điểm UX đáng chú ý:
- Luồng bán vé theo bước (chọn lịch trình → chọn ghế → nhập thông tin → thanh toán).
- Danh sách lớn có hỗ trợ tìm kiếm và **phân trang** để giảm tải và tăng tốc độ phản hồi.
- Thông báo lỗi theo nghiệp vụ rõ ràng (ghế bị giữ, không đủ điều kiện đổi/trả theo thời gian, dữ liệu không hợp lệ...).

***[Insert Màn hình Bán vé tại đây]***
***[Insert Màn hình Quản lý Khách hàng tại đây]***

---

# **CHƯƠNG 4: KIỂM THỬ HỆ THỐNG**

## **4.1 Môi trường kiểm thử**

Môi trường đề xuất:
- **Java**: JDK phù hợp với dự án, công cụ build Maven.
- **MariaDB**: cài đặt local hoặc server nội bộ; dữ liệu seed cho các kịch bản bán/đổi/trả.
- **JavaFX**: runtime và thư viện UI tương ứng.
- **Hệ thống**: máy tính tối thiểu 8GB RAM; mạng LAN/localhost để mô phỏng nhiều Client.

## **4.2 Kịch bản kiểm thử (Test Cases)**

| TC ID | Mục tiêu kiểm thử | Dữ liệu vào | Bước thực hiện | Kết quả mong đợi |
|---|---|---|---|---|
| TC01 | Giới hạn 10 vé/giao dịch | Chọn 11 ghế trong một giao dịch | Gửi yêu cầu tạo giao dịch bán vé | Server từ chối, trả thông báo vi phạm giới hạn; không tạo hóa đơn/vé |
| TC02 | Đồng thời giữ ghế | 2 Client giữ cùng 1 ghế | Client A giữ ghế; Client B giữ/mua ghế đó | Client B nhận lỗi “ghế đang được giữ/đã bán”; dữ liệu ghế không trùng |
| TC03 | Auto‑Registration khi bán vé | CCCD/hộ chiếu chưa tồn tại | Bán vé với thông tin người mua/hành khách mới | Hệ thống tự tạo hồ sơ Customer; vé/hóa đơn liên kết đúng |
| TC04 | Đổi vé trước 24h + phí 20.000đ | Vé còn >24h; ghế mới đã được giữ | Preview đổi → Confirm đổi | Tổng tiền = phí đổi + chênh lệch; vé cũ “đã đổi”; vé mới phát hành |
| TC05 | Trả vé theo mốc 4h–24h | Vé còn 6h đến khởi hành | Preview trả → Confirm trả | Phí khấu trừ 20% (>=10.000đ); vé “đã trả”; tạo chứng từ hoàn |

---

# **CHƯƠNG 5: KẾT LUẬN**

## **5.1 Kết quả đạt được**

Các kết quả chính:
- Hoàn thiện hệ thống bán vé theo kiến trúc **Client–Server** qua **TCP Socket** (Request/Response).
- Thiết kế **N‑Tier** rõ ràng, giúp tách biệt UI – DTO – nghiệp vụ – truy cập dữ liệu.
- Hiện thực nghiệp vụ bán/đổi/trả vé theo các ràng buộc legacy, đồng thời kiểm soát đồng thời khi chọn/giữ ghế.
- Tích hợp **MariaDB/JPA/Hibernate**, quản lý thực thể và transaction nhất quán.
- Giải quyết các vấn đề nghiệp vụ then chốt như **Auto‑Registration** và **Pagination** trong quản lý khách hàng.

## **5.2 Hạn chế của đồ án**

Các hạn chế hiện tại:
- Chưa triển khai đầy đủ cơ chế bảo mật truyền thông (mã hóa kênh socket, xác thực nâng cao).
- Chưa đánh giá hiệu năng ở quy mô lớn và chưa có bộ đo giám sát (monitoring) hoàn chỉnh.
- Một số công thức tính giá theo hệ số (km/chỗ/bảo hiểm) cần được chuẩn hóa thành module cấu hình linh hoạt.

## **5.3 Hướng phát triển**

Định hướng mở rộng:
- Tích hợp TLS cho socket, bổ sung cơ chế phân quyền chi tiết và nhật ký/audit giao dịch.
- Chuẩn hóa module tính giá theo công thức legacy, hỗ trợ cấu hình ưu đãi theo thời gian/đối tượng.
- Mở rộng kênh bán vé online (web/mobile), tích hợp cổng thanh toán thực.
- Tối ưu truy vấn và cache dữ liệu nền (ga, tuyến, tàu) để cải thiện tốc độ tải UI.
- Nghiên cứu mở rộng phân tán đa server hoặc chuyển hướng microservices theo nhu cầu.

---

# **TÀI LIỆU THAM KHẢO**

- Java Networking: TCP Socket (`ServerSocket`, `Socket`), ObjectStream (`ObjectInputStream`, `ObjectOutputStream`).
- JPA/Hibernate: Entity Mapping, Transaction, Optimistic Locking (`@Version`).
- MariaDB: thiết kế CSDL quan hệ, chỉ mục và tối ưu truy vấn.
- JavaFX: FXML, Controller, TableView, Pagination, CSS.

