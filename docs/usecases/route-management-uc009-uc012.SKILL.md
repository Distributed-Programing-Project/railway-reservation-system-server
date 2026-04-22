---
name: route-management-uc009-uc012
description: create or refactor backend, service, validation, controller, dto, and query flow for route management in the train station system based on uc009, uc010, uc011, and uc012. use when chatgpt or codex needs to implement filter route, create route, update route, disable route, promote draft route, or delete draft route while staying consistent with the current route, route_stop, and schedule entities and avoiding fields that no longer exist.
---

# Mục tiêu
Bám sát nghiệp vụ quản lý tuyến đường trong tài liệu phân tích, nhưng ưu tiên schema hiện tại của codebase nếu có mâu thuẫn.

# Nguồn sự thật
Ưu tiên theo thứ tự sau:

1. entity hiện tại trong codebase
2. ràng buộc nghiệp vụ từ use case
3. tên màn hình hoặc mô tả UI
4. không tự sinh thêm field nếu entity hiện tại không có

Với schema hiện tại, `Route` gồm:
- `id`
- `departureStation`
- `destinationStation`
- `status`
- `priceBasic`
- `routeStops`
- `schedules`

`RouteStop` gồm:
- `id`
- `orderStop`
- `stationStop`
- `route`
- `scheduleDetails`

# Quy tắc bắt buộc trước khi code
1. Đọc entity `Route`, `RouteStop`, `Schedule`, `Station`.
2. Kiểm tra enum trạng thái thực tế đang dùng trong codebase. Không hard-code tên trạng thái từ tài liệu nếu enum đã đổi tên.
3. Nếu tài liệu cũ nhắc tới field không còn tồn tại, dừng và bám theo entity mới.
4. Nếu cần tạo DTO hoặc API, dùng tên field đúng theo entity hiện tại, không dùng lại tên cũ như `diemDi`, `diemDen`, `maTuyen`.

# Ánh xạ nghiệp vụ
## UC009 - Lọc tuyến đường
Cho phép lọc theo:
- ga đi
- ga đến
- trạng thái

Kỳ vọng:
- hỗ trợ lọc độc lập hoặc kết hợp
- nếu không có kết quả, trả danh sách rỗng hoặc thông báo phù hợp
- không lỗi khi người dùng bỏ trống một phần filter

## UC010 - Tạo tuyến đường
Cho phép tạo tuyến đường mới.

Ràng buộc:
- ga đi và ga đến không được trùng nhau
- không tạo trùng tuyến nếu đã tồn tại cùng cặp ga đi/ga đến theo quy tắc nghiệp vụ của hệ thống
- `priceBasic` phải hợp lệ nếu hệ thống yêu cầu nhập hoặc tính sẵn
- nếu hệ thống quản lý tuyến qua các ga dừng, phải nhận danh sách `routeStops` có thứ tự tăng dần, không trùng ga, và phải nhất quán với ga đầu/cuối

## UC011 - Sửa tuyến đường
Cho phép sửa tuyến đường theo điều kiện an toàn dữ liệu.

Ràng buộc:
- không cho sửa nếu tuyến đã bị khóa theo rule nghiệp vụ
- theo tài liệu cũ, tuyến đã gắn lịch trình thì không được sửa
- với schema hiện tại, phải kiểm tra quan hệ `route.schedules`
- nếu cho phép sửa khi chưa có lịch trình, phải kiểm tra lại:
  - ga đi khác ga đến
  - không tạo trùng tuyến với tuyến khác
  - danh sách `routeStops` hợp lệ

## UC012 - Vô hiệu hóa và phát triển tuyến đường
Diễn giải theo schema hiện tại:
- vô hiệu hóa: đổi trạng thái route sang trạng thái tạm ngưng hoặc tương đương, không xóa cứng
- phát triển tuyến nháp: đổi trạng thái từ nháp sang sẵn sàng hoặc tương đương
- xóa chỉ áp dụng cho tuyến nháp nếu rule nghiệp vụ cho phép

Ràng buộc:
- không vô hiệu hóa nếu tuyến đã có lịch trình ràng buộc mà rule nghiệp vụ cấm
- không xóa nếu không ở trạng thái nháp
- mọi chuyển trạng thái phải kiểm tra enum thật trong codebase

# Workflow triển khai
## 1. Lọc tuyến đường
1. Tạo request DTO lọc với các field tùy chọn.
2. Xây repository query động theo:
   - departureStation.id
   - destinationStation.id
   - status
3. Trả danh sách route tối ưu cho UI, tránh vòng lặp vô hạn ở JSON.
4. Nếu có phân trang trong project, dùng phân trang.

## 2. Tạo tuyến đường
1. Validate request.
2. Nạp station đi và đến.
3. Từ chối nếu đi = đến.
4. Kiểm tra duplicate route.
5. Khởi tạo route với trạng thái mặc định theo nghiệp vụ.
6. Nếu có `routeStops`, validate danh sách rồi lưu theo thứ tự.
7. Trả object đã tạo.

## 3. Sửa tuyến đường
1. Tìm route theo id.
2. Kiểm tra route có đang được phép sửa không.
3. Kiểm tra route đã có schedule hay chưa.
4. Validate dữ liệu mới.
5. Cập nhật route.
6. Nếu project hỗ trợ cập nhật `routeStops`, ưu tiên chiến lược rõ ràng:
   - replace toàn bộ danh sách
   - hoặc patch có kiểm soát
7. Lưu và trả kết quả.

## 4. Vô hiệu hóa / phát triển / xóa nháp
1. Tìm route.
2. Kiểm tra trạng thái hiện tại.
3. Kiểm tra route có schedule liên quan không.
4. Áp dụng chuyển trạng thái hợp lệ:
   - draft -> ready
   - ready -> suspended
   - draft -> deleted nếu project cho xóa mềm hoặc xóa cứng
5. Lưu thay đổi.

# Validation tối thiểu
- `departureStationId` bắt buộc khi tạo
- `destinationStationId` bắt buộc khi tạo
- `departureStationId != destinationStationId`
- `priceBasic >= 0` nếu field này là input
- `routeStops.orderStop` phải bắt đầu hợp lệ, không trùng, tăng dần
- không trùng `stationStop` trong cùng một route nếu business rule yêu cầu
- route phải có ít nhất ga đầu và ga cuối theo cách biểu diễn đang dùng trong project

# Repository / service rules
- Không viết business logic chính trong controller.
- Repository chỉ lo truy vấn.
- Service xử lý toàn bộ rule:
  - duplicate route
  - route in use
  - transition status
  - route stop integrity
- Dùng transaction nếu cập nhật `Route` và `RouteStop` cùng lúc.

# API gợi ý
Tùy kiến trúc project, có thể dùng:
- `GET /routes`
- `POST /routes`
- `PUT /routes/{id}`
- `PATCH /routes/{id}/status`
- `DELETE /routes/{id}` chỉ cho draft nếu nghiệp vụ cho phép

# Response gợi ý
Với danh sách:
- id
- departure station summary
- destination station summary
- status
- priceBasic
- totalStops

Với chi tiết:
- route info
- ordered routeStops
- number of schedules using this route

# Điều không được làm
- Không tự thêm field `thoiGianDuKien` nếu entity hiện tại không có.
- Không tự suy diễn trạng thái bằng string nếu enum chưa được kiểm tra.
- Không cho phép sửa hoặc vô hiệu hóa chỉ vì UI yêu cầu; luôn kiểm tra quan hệ thật trong database.
- Không serialize nguyên object hai chiều gây vòng lặp vô hạn.

# Checklist trước khi kết thúc
- Chạy lại toàn bộ validation của create/update.
- Test case tối thiểu:
  - lọc rỗng
  - lọc có kết quả
  - tạo route đi = đến
  - tạo route trùng
  - sửa route đã có schedule
  - vô hiệu hóa route đang được dùng
  - phát triển route draft
  - xóa route không phải draft
