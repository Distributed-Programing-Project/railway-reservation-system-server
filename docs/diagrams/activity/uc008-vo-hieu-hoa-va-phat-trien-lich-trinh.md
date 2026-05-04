# Activity Diagram — Vô hiệu hóa và Phát triển lịch trình

> Dựa trên: `docs/usecases/uc008-vo-hieu-hoa-va-phat-trien-lich-trinh.md`

## Mô tả use case

| Actor | Hệ thống |
|---|---|
| 1. Nhân viên quản lý chọn chức năng Quản lý lịch trình. | |
| | 2. Hệ thống hiển thị danh sách lịch trình và các nút hành động. |
| 3a. Nhân viên chọn lịch trình `DRAFT` và bấm **"Phát triển"**. | |
| | 4a. Hệ thống hiển thị hộp thoại xác nhận. |
| 5a. Nhân viên bấm **"Xác nhận"**. | |
| | 6a. Hệ thống kiểm tra giá vé tất cả ghế và thời gian khởi hành. |
| | 7a. Nếu hợp lệ → Cập nhật trạng thái `DRAFT` → `NOT_STARTED`. |
| | 8a. Hệ thống thông báo thành công và tải lại danh sách. |
| 3b. Nhân viên chọn lịch trình `DRAFT` hoặc `NOT_STARTED` và bấm **"Vô hiệu hóa"**. | |
| | 4b. Hệ thống hiển thị hộp thoại xác nhận (nội dung khác nhau tuỳ trạng thái). |
| 5b. Nhân viên bấm **"Xác nhận"**. | |
| | 6b. Hệ thống kiểm tra trạng thái lịch trình. |
| | 7b. Nếu `DRAFT` → Hệ thống xóa vĩnh viễn lịch trình (Cascade delete `ScheduleDetail`). |
| | 7b'. Nếu `NOT_STARTED` → Hệ thống kiểm tra số vé đã bán. Nếu chưa có vé → Cập nhật → `PAUSED`. |
| | 8b. Hệ thống thông báo thành công và tải lại danh sách. |
| **Luồng ngoại lệ** | |
| 5.x Nhân viên bấm **"Hủy"** trong hộp thoại xác nhận. | |
| | 6.x Hệ thống đóng hộp thoại, không thực hiện thay đổi. |
| | 6.y *(3a)* Có ghế chưa cấu hình giá hoặc thời gian khởi hành đã qua → Hệ thống thông báo lỗi. |
| | 7.x *(3b — NOT_STARTED)* Đã có ít nhất 1 vé bán → Hệ thống từ chối và thông báo lỗi. |

---

## Sơ đồ Activity

```mermaid
flowchart LR
    subgraph actor["👤 Nhân viên quản lý (isManager=true)"]
        direction TB
        START(( )) --> A1[Chọn chức năng\nQuản lý lịch trình]
        A1 --> DA{Chọn hành động}
        DA -->|Phát triển| A2[Chọn lịch trình DRAFT\nBấm Phát triển]
        A2 --> DA2{Xác nhận\nPhát triển?}
        DA2 -->|Hủy| R1([Hủy thao tác])
        DA -->|Vô hiệu hóa| A3[Chọn lịch trình\nBấm Vô hiệu hóa]
        A3 --> DA3{Xác nhận\nVô hiệu hóa?}
        DA3 -->|Hủy| R2([Hủy thao tác])
    end

    subgraph system["⚙️ Hệ thống"]
        direction TB
        S1[Hiển thị danh sách lịch trình\nvà các nút hành động]
        S2[Hiển thị hộp thoại\nxác nhận Phát triển]
        S3{Giá vé và\nthời gian hợp lệ?}
        SE1[Lỗi: chưa cấu hình giá\nhoặc thời gian đã qua]
        S4[Cập nhật DRAFT\n→ NOT_STARTED]
        S5[Thông báo Phát triển\nthành công]
        S6[Hiển thị hộp thoại\nxác nhận Vô hiệu hóa]
        S7{Trạng thái\nlịch trình?}
        S8[Xóa lịch trình DRAFT\nCascade delete ScheduleDetail]
        S9{Đã có\nvé bán?}
        SE2[Lỗi: đã có\nkhách mua vé]
        S10[Cập nhật NOT_STARTED\n→ PAUSED]
        S11[Thông báo Vô hiệu hóa\nthành công]
        END1((( )))
        END2((( )))
        END3((( )))
        END4((( )))
    end

    A1 --> S1
    A2 --> S2
    S2 --> DA2
    DA2 -->|Xác nhận| S3
    S3 -->|Không hợp lệ| SE1
    SE1 --> END1
    S3 -->|Hợp lệ| S4
    S4 --> S5
    S5 --> END2
    A3 --> S6
    S6 --> DA3
    DA3 -->|Xác nhận| S7
    S7 -->|DRAFT| S8
    S8 --> S11
    S7 -->|NOT_STARTED| S9
    S9 -->|Có| SE2
    SE2 --> END3
    S9 -->|Không| S10
    S10 --> S11
    S11 --> END4
```