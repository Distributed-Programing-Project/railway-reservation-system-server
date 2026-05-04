# Activity Diagram — Vô hiệu hóa và Phát triển lịch trình

> Dựa trên: `docs/usecases/uc008-vo-hieu-hoa-va-phat-trien-lich-trinh.md`

## Mô tả use case

**Actor:** Nhân viên quản lý (`isManager = true`)

### Luồng chính: Phát triển lịch trình (Publish)

| Actor | Hệ thống |
|---|---|
| 1. Nhân viên quản lý truy cập màn hình Quản lý lịch trình. | |
| | 2. Hệ thống hiển thị danh sách lịch trình và các nút hành động (Phát triển, Vô hiệu hóa). |
| 3a. Nhân viên chọn một lịch trình đang ở trạng thái `DRAFT` và bấm **"Phát triển"**. | |
| | 4a. Hệ thống hiển thị hộp thoại xác nhận. |
| 5a. Nhân viên bấm **"Xác nhận"**. | |
| | 6a. Hệ thống kiểm tra: tất cả ghế đã cấu hình giá vé và thời gian khởi hành còn trong tương lai. |
| | 7a. Nếu hợp lệ → Hệ thống cập nhật trạng thái từ `DRAFT` thành `NOT_STARTED`. |
| | 8a. Hệ thống thông báo thành công và tải lại danh sách. |

### Luồng thay thế: Vô hiệu hóa lịch trình (Disable)

| Actor | Hệ thống |
|---|---|
| 3b. Nhân viên chọn lịch trình ở trạng thái `DRAFT` hoặc `NOT_STARTED` và bấm **"Vô hiệu hóa"**. | |
| | 4b. Hệ thống hiển thị hộp thoại xác nhận (nội dung khác nhau tuỳ trạng thái). |
| 5b. Nhân viên bấm **"Xác nhận"**. | |
| | 6b. Hệ thống kiểm tra trạng thái lịch trình. |
| | 7b. Nếu `DRAFT` → Hệ thống xóa vĩnh viễn lịch trình và tất cả `ScheduleDetail` liên quan (Cascade delete). |
| | 7b'. Nếu `NOT_STARTED` → Hệ thống kiểm tra số vé đã bán (JOIN qua `ScheduleDetail`). |
| | 8b. Nếu chưa có vé bán → Hệ thống cập nhật trạng thái sang `PAUSED`. |
| | 9b. Hệ thống thông báo thành công và tải lại danh sách. |

**Luồng ngoại lệ:**

| Actor | Hệ thống |
|---|---|
| 5.x Nhân viên bấm **"Hủy"** trong hộp thoại xác nhận. | |
| | 6.x Hệ thống đóng hộp thoại, không thực hiện thay đổi nào. |
| | 6.y *(Phát triển)* Nếu có ghế chưa cấu hình giá vé → Hệ thống thông báo lỗi *"Cần cấu hình giá vé trước khi phát triển"*. |
| | 6.z *(Phát triển)* Nếu thời gian khởi hành đã qua → Hệ thống thông báo lỗi *"Thời gian khởi hành không hợp lệ"*. |
| | 8.x *(Vô hiệu hóa NOT_STARTED)* Nếu đã có ít nhất 1 vé bán → Hệ thống từ chối và thông báo lỗi *"Không thể vô hiệu hóa lịch trình đã có khách mua vé"*. |

---

## Sơ đồ Activity

```mermaid
flowchart LR
    subgraph actor["👤 Nhân viên quản lý (isManager=true)"]
        direction TB
        START(( )) --> A1[Truy cập Quản lý lịch trình]
        A1 --> DA{Chọn hành động}
        DA -->|Phát triển| A2[Chọn lịch trình DRAFT\nBấm Phát triển]
        DA -->|Vô hiệu hóa| A3[Chọn lịch trình DRAFT\nhoặc NOT_STARTED\nBấm Vô hiệu hóa]
        A2 --> D_pub{Xác nhận\nPhát triển?}
        A3 --> D_dis{Xác nhận\nVô hiệu hóa?}
        D_pub -->|Hủy| R_cancel1([Hủy thao tác])
        D_dis -->|Hủy| R_cancel2([Hủy thao tác])
    end

    subgraph system["⚙️ Hệ thống"]
        direction TB
        S1[Hiển thị danh sách lịch trình\nvà nút hành động]
        S2[Hiển thị xác nhận\nPhát triển]
        S3{Giá vé và thời gian\nhợp lệ?}
        S4[Cập nhật\nDRAFT → NOT_STARTED]
        S5[Thông báo Phát triển thành công\nTải lại danh sách]
        S6[Thông báo lỗi\nGiá chưa cấu hình\nhoặc thời gian đã qua]
        S7[Hiển thị xác nhận\nVô hiệu hóa]
        S8{Trạng thái\nlịch trình?}
        S9[Xóa lịch trình DRAFT\nCascade delete ScheduleDetail]
        S10{Đã có\nvé bán?}
        S11[Cập nhật\nNOT_STARTED → PAUSED]
        S12[Thông báo lỗi\nĐã có khách mua vé]
        S13[Thông báo Vô hiệu hóa thành công\nTải lại danh sách]
        END1((( )))
        END2((( )))
        END3((( )))
        END4((( )))
    end

    A1 --> S1
    S1 --> DA
    A2 --> S2
    S2 --> D_pub
    D_pub -->|Xác nhận| S3
    S3 -->|Không hợp lệ| S6
    S6 --> END1
    S3 -->|Hợp lệ| S4
    S4 --> S5
    S5 --> END2
    A3 --> S7
    S7 --> D_dis
    D_dis -->|Xác nhận| S8
    S8 -->|DRAFT| S9
    S9 --> S13
    S8 -->|NOT_STARTED| S10
    S10 -->|Có| S12
    S12 --> END3
    S10 -->|Không| S11
    S11 --> S13
    S13 --> END4
```