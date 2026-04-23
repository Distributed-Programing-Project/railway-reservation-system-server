---
name: schedule-management-uc008
description: create or refactor backend, service, validation, controller, dto, and status transition flow for schedule management in the train station system based on uc008. use when chatgpt or codex needs to implement disable schedule, promote draft schedule, or enforce schedule status transition rules while staying consistent with the current schedule, schedule_detail, route, train, and route_stop entities and refusing to invent outdated departure date or time fields that are not present in the current schema.
---

# Mục tiêu
Bám sát UC008 - Vô hiệu hoá và phát triển lịch trình, nhưng tuyệt đối ưu tiên schema hiện tại của codebase.

# Cảnh báo bắt buộc
Tài liệu nghiệp vụ cũ mô tả `Lịch trình` có:
- ngày khởi hành
- giờ khởi hành

Nhưng entity `Schedule` hiện tại chỉ có:
- `id`
- `status`
- `train`
- `route`

Entity `ScheduleDetail` hiện tại có:
- `id`
- `priceSeat`
- `seat`
- `schedule`
- `routeStop`

Vì vậy:
- không tự thêm `departureDate`, `departureTime`, `arrivalTime` vào code
- nếu project thực tế vẫn cần ngày giờ chạy tàu, phải tìm xem chúng nằm ở entity khác, migration khác, hoặc chưa commit
- nếu không có trong codebase, chỉ implement đúng phạm vi hiện tại của schema

# Nghiệp vụ UC008 cần bám
Tên use case: Vô hiệu hoá và phát triển lịch trình.

Diễn giải từ tài liệu:
- có thể vô hiệu hoá lịch trình nháp
- có thể vô hiệu hoá lịch trình chưa có khách mua vé
- có thể phát triển lịch trình nháp thành trạng thái chưa khởi hành
- không cho phép vô hiệu hoá nếu lịch trình đã có khách mua vé hoặc trạng thái không hợp lệ

# Ánh xạ lại theo schema mới
## Schedule
Là bản ghi liên kết `Train` với `Route` và mang `status`.

## ScheduleDetail
Là dữ liệu chi tiết theo:
- một `Seat`
- trong một `Schedule`
- tại một `RouteStop`
- có `priceSeat`

## Status transition
Không được dùng string từ tài liệu nếu enum thật trong codebase khác tên.
Phải đọc enum thật trước khi code.

Map theo ý nghĩa:
- `draft` hoặc tương đương -> có thể promote sang `ready`, `unstarted`, hoặc trạng thái tương đương
- `draft` hoặc `ready` chưa phát sinh bán vé -> có thể disable sang `suspended`, `inactive`, hoặc trạng thái tương đương
- nếu đã có dữ liệu bán vé liên quan -> từ chối disable

# Workflow triển khai
## 1. Promote schedule draft
1. Tìm `Schedule` theo id.
2. Kiểm tra trạng thái hiện tại có phải draft hoặc tương đương không.
3. Nếu không phải draft, từ chối.
4. Chuyển sang trạng thái hoạt động ban đầu theo enum thật của hệ thống.
5. Lưu thay đổi.
6. Trả kết quả mới.

## 2. Disable schedule
1. Tìm `Schedule` theo id.
2. Kiểm tra trạng thái hiện tại có thuộc nhóm được phép disable không.
3. Kiểm tra lịch trình đã phát sinh bán vé chưa.
4. Nếu đã phát sinh giao dịch, từ chối disable.
5. Nếu chưa phát sinh, cập nhật trạng thái sang tạm ngưng hoặc tương đương.
6. Lưu thay đổi.

## 3. Delete schedule draft
Chỉ làm nếu project thực sự cho phép xóa lịch trình nháp.

1. Tìm `Schedule`.
2. Kiểm tra trạng thái là draft.
3. Kiểm tra chưa phát sinh dữ liệu ràng buộc không cho xóa.
4. Xóa mềm hoặc xóa cứng theo kiến trúc hiện tại.
5. Không xóa nếu schedule đã được phát triển hoặc đã có liên kết nghiệp vụ cần giữ lịch sử.

# Rule kiểm tra bán vé
Tài liệu nói "không có khách mua vé" nhưng schema hiện tại không cho biết trực tiếp.

Vì vậy trước khi code phải tìm:
- bảng vé
- bảng đặt chỗ
- bảng hóa đơn chi tiết
- quan hệ từ `Schedule` hoặc `ScheduleDetail` sang vé

Nếu đã có module bán vé:
- kiểm tra có vé nào gắn với `ScheduleDetail` của `Schedule` đó không

Nếu chưa có module này trong phạm vi codebase hiện tại:
- không bịa rule kiểm tra
- chỉ viết hook method rõ ràng như `hasSoldTickets(scheduleId)`

# Service rules
- Không viết business logic ở controller.
- Service phải chịu trách nhiệm:
  - kiểm tra status transition hợp lệ
  - kiểm tra schedule tồn tại
  - kiểm tra sold ticket dependency
  - quyết định xóa mềm hay đổi trạng thái
- Dùng transaction nếu thao tác đụng tới `ScheduleDetail` hoặc dữ liệu liên quan.

# API gợi ý
- `PATCH /schedules/{id}/promote`
- `PATCH /schedules/{id}/disable`
- `DELETE /schedules/{id}` chỉ cho draft nếu nghiệp vụ cho phép

# Validation tối thiểu
- `scheduleId` phải tồn tại
- trạng thái hiện tại phải phù hợp với action
- action phải thuộc tập hợp cho phép
- nếu disable, phải qua bước kiểm tra đã bán vé chưa
- nếu delete, chỉ cho draft nếu rule của hệ thống cho phép

# Điều không được làm
- Không thêm ngày/giờ khởi hành chỉ vì tài liệu cũ có.
- Không dùng string status cứng khi chưa đọc enum thật.
- Không disable hoặc delete lịch trình đang có giao dịch bán vé.
- Không xóa cứng dữ liệu đã phát sinh lịch sử nghiệp vụ nếu hệ thống cần audit.

# Checklist trước khi kết thúc
- Test promote thành công với schedule draft.
- Test promote thất bại với schedule không phải draft.
- Test disable thành công với schedule chưa phát sinh vé.
- Test disable thất bại với schedule đã có vé.
- Test delete draft thành công nếu project cho phép.
- Test delete thất bại với schedule không phải draft.
