# Diagrams — UC007: Sửa lịch trình tàu

---

## 1. System Architecture

```mermaid
graph TB
    subgraph CLIENT ["Client (JavaFX)"]
        SCC["ScheduleManagementController"]
        DLG["AddScheduleDialogController"]
        SC["SocketRequestService"]
    end

    subgraph TRANSPORT ["TCP Socket Transport"]
        OOS["ObjectOutputStream.writeObject(Request)"]
        OIS["ObjectInputStream.readObject() → Response"]
    end

    subgraph SERVER ["Server (Java Socket Server)"]
        SRV["Server.java\nhandleClient(Socket)"]
        RR["RequestRouter.route(Request)"]
        SVC["ScheduleServiceImpl\n.updateSchedule(ScheduleUpdateDTO)"]
        EMP_REPO["EmployeeRepositoryImpl\n.findEmployeeById(em, id)"]
        SCH_REPO["ScheduleRepositoryImpl\n.findScheduleById(em, id)\n.updateSchedule(em, schedule)"]
        JPA["JPAUtils — transactional()"]
    end

    subgraph DB ["MariaDB"]
        T_EMP["employees"]
        T_SCH["schedules"]
        T_DET["schedule_details"]
        T_SEAT["seats / carriages"]
    end

    SCC -- "initForEdit(ScheduleDTO) → mở dialog" --> DLG
    DLG -- "new Request(UPDATE_SCHEDULE, ScheduleUpdateDTO)" --> SC
    SC --> OOS
    OOS -- "TCP Socket :9090" --> SRV
    SRV -- "castData(request, ScheduleUpdateDTO.class)" --> RR
    RR --> SVC
    SVC --> JPA
    SVC --> EMP_REPO
    EMP_REPO -- "SELECT e FROM Employee WHERE id" --> T_EMP
    SVC -- "ValidationUtils + requireActiveManager\n+ check DRAFT + departureTime/arrivalTime\n+ validateActiveTrain/Route" --> SVC
    SVC --> SCH_REPO
    SCH_REPO -- "SELECT s FROM Schedule JOIN FETCH..." --> T_SCH
    SCH_REPO -- "UPDATE schedules SET ..." --> T_SCH
    SCH_REPO -- "DELETE FROM schedule_details\n(nếu train/route thay đổi)" --> T_DET
    SCH_REPO -- "SELECT seats + em.persist(ScheduleDetail) x N\n(nếu train/route thay đổi)" --> T_SEAT
    SVC -- "Response.success(scheduleId)" --> RR
    RR -- "Response" --> SRV
    SRV -- "out.writeObject(response) + out.reset()" --> OIS
    OIS -- "TCP Socket" --> SC
    SC -- "response.isSuccess()" --> DLG
```

---

## 2. Sequence Diagram

```mermaid
sequenceDiagram
    actor Manager as Nhân viên quản lý
    participant SCC as ScheduleManagementController
    participant DLG as AddScheduleDialogController
    participant SC as SocketRequestService
    participant SRV as Server.handleClient()
    participant RR as RequestRouter
    participant SVC as ScheduleServiceImpl
    participant EMP_REPO as EmployeeRepositoryImpl
    participant SCH_REPO as ScheduleRepositoryImpl
    participant DB as MariaDB

    Manager->>SCC: Đăng nhập thành công
    Manager->>SCC: Chọn màn hình "Quản lý lịch trình"
    Manager->>SCC: Chọn lịch trình trong bảng, nhấn "Sửa"
    SCC->>SCC: Kiểm tra selected.status == DRAFT
    alt status != DRAFT
        SCC-->>Manager: Alert "Chỉ được phép sửa lịch trình ở trạng thái Bản nháp (DRAFT)"
    end

    SCC->>DLG: FXMLLoader.load() → ctrl.initForEdit(selectedDTO)
    DLG->>DLG: Điền form: routeCombo, trainCombo, departureDatePicker, arrivalDatePicker
    Note over DLG: loadRoutesAsync() + loadTrainsAsync() chạy nền (Task)<br/>populate ComboBox — lọc ACTIVE, bao gồm preSelectedTrainId/RouteId

    Manager->>DLG: Chỉnh sửa thông tin, nhấn "Xác nhận"
    DLG->>DLG: validateForm() — kiểm tra trường bắt buộc, departureTime > now(), arrivalTime > departureTime
    alt Lỗi validate phía UI
        DLG-->>Manager: Alert "Dữ liệu không hợp lệ" (không gửi request đến server)
    end

    DLG->>DLG: SessionManager.getInstance().getEmployeeId() → employeeId
    DLG->>DLG: Build ScheduleUpdateDTO(requestEmployeeId, scheduleId, trainId, routeId, departureTime, arrivalTime)
    DLG->>SC: Task[Response]: send(new Request(UPDATE_SCHEDULE, scheduleUpdateDTO))
    SC->>SRV: ObjectOutputStream.writeObject(request)

    SRV->>RR: router.route(request)
    RR->>SVC: updateSchedule(castData(request, ScheduleUpdateDTO.class))

    SVC->>SVC: ValidationUtils.validate(dto) — @NotBlank/@NotNull, isDepartureTimeInFuture(), isArrivalTimeAfterDepartureTime()
    alt Lỗi Bean Validation
        SVC-->>RR: Response.error("...")
        RR-->>SRV: Response
        SRV-->>SC: out.writeObject(response) + out.reset()
        SC-->>DLG: response.isSuccess() = false
        DLG-->>Manager: Alert lỗi
    end

    SVC->>EMP_REPO: requireActiveManager — findEmployeeById(em, requestEmployeeId)
    EMP_REPO->>DB: SELECT e FROM Employee e WHERE e.id = :id
    DB-->>EMP_REPO: Employee (hoặc null)
    EMP_REPO-->>SVC: employee

    alt Employee null, status != ACTIVE, hoặc isManager = false
        SVC-->>RR: Response.error("Bạn không có quyền thực hiện thao tác này")
        RR-->>SRV: Response
        SRV-->>SC: out.writeObject(response) + out.reset()
        SC-->>DLG: response.isSuccess() = false
        DLG-->>Manager: Alert "Không có quyền"
    end

    Note over SVC: transactional(em -> {...})

    SVC->>SCH_REPO: findScheduleById(em, scheduleId)
    SCH_REPO->>DB: SELECT s FROM Schedule s JOIN FETCH s.route r JOIN FETCH r.departureStation JOIN FETCH r.destinationStation JOIN FETCH s.train WHERE s.id = :id
    DB-->>SCH_REPO: Schedule (hoặc null)
    SCH_REPO-->>SVC: existingSchedule

    alt existingSchedule = null
        SVC-->>RR: Response.error("Không tìm thấy lịch trình: id=...")
        RR-->>SRV: Response
        SRV-->>SC: out.writeObject(response) + out.reset()
        SC-->>DLG: response.isSuccess() = false
        DLG-->>Manager: Alert lỗi
    end

    alt existingSchedule.status != DRAFT
        SVC-->>RR: Response.error("Chỉ được phép sửa lịch trình khi đang ở trạng thái Nháp")
        RR-->>SRV: Response
        SRV-->>SC: out.writeObject(response) + out.reset()
        SC-->>DLG: response.isSuccess() = false
        DLG-->>Manager: Alert lỗi trạng thái
    end

    SVC->>SVC: departureTime != null và !departureTime.isAfter(now()) → lỗi
    SVC->>SVC: Tính effectiveDeparture (dto.departureTime nếu có, không thì giữ nguyên cũ)
    SVC->>SVC: arrivalTime != null và !arrivalTime.isAfter(effectiveDeparture) → lỗi

    SVC->>DB: em.find(Train.class, trainId)
    DB-->>SVC: Train (hoặc null)
    SVC->>SVC: validateActiveTrain — Train != null và status == ACTIVE
    alt Train null hoặc status != ACTIVE
        SVC-->>RR: Response.error("Không tìm thấy tàu / Tàu không đang hoạt động")
        RR-->>SRV: Response
        SRV-->>SC: out.writeObject(response) + out.reset()
        SC-->>DLG: response.isSuccess() = false
        DLG-->>Manager: Alert lỗi
    end

    SVC->>DB: em.find(Route.class, routeId)
    DB-->>SVC: Route (hoặc null)
    SVC->>SVC: validateActiveRoute — Route != null và status == ACTIVE
    alt Route null hoặc status != ACTIVE
        SVC-->>RR: Response.error("Không tìm thấy tuyến / Tuyến không đang hoạt động")
        RR-->>SRV: Response
        SRV-->>SC: out.writeObject(response) + out.reset()
        SC-->>DLG: response.isSuccess() = false
        DLG-->>Manager: Alert lỗi
    end

    SVC->>SVC: ScheduleMapper.toEntityForUpdate(dto) → scheduleToUpdate (partial entity)
    SVC->>SCH_REPO: updateSchedule(em, scheduleToUpdate)

    SCH_REPO->>DB: em.find(Schedule, scheduleId) → managed entity
    DB-->>SCH_REPO: existingSchedule
    SCH_REPO->>SCH_REPO: So sánh trainId cũ vs mới, routeId cũ vs mới

    alt trainId HOẶC routeId thay đổi
        SCH_REPO->>DB: DELETE FROM schedule_details WHERE schedule_id = :scheduleId
        SCH_REPO->>DB: SELECT seat.id FROM Seat s WHERE s.carriage.train.id = :newTrainId
        DB-->>SCH_REPO: List[seatId] (N ghế)
        loop Tạo lại ScheduleDetail (batch size 50)
            SCH_REPO->>DB: em.persist(ScheduleDetail(schedule, seat, priceSeat=0, routeStop=null))
            SCH_REPO->>DB: em.flush() + em.clear() [mỗi 50 bản ghi]
        end
    end

    SCH_REPO->>SCH_REPO: existingSchedule.set(train, route, departureTime, arrivalTime)
    SCH_REPO->>DB: dirty check → UPDATE schedules SET ...
    DB-->>SCH_REPO: OK
    SCH_REPO-->>SVC: true

    SVC-->>RR: Response.success("Cập nhật lịch trình thành công", scheduleId)
    RR-->>SRV: Response
    SRV-->>SC: out.writeObject(response) + out.reset()
    SC-->>DLG: response.isSuccess() = true
    DLG-->>Manager: Alert "Cập nhật lịch trình thành công", đóng dialog
    DLG-->>SCC: Stage.showAndWait() kết thúc
    SCC->>SCC: loadData() — tải lại danh sách lịch trình
```

---

## 3. Class Diagram

```mermaid
classDiagram
    class ScheduleUpdateDTO {
        <<DTO>>
        +String requestEmployeeId
        +String scheduleId
        +String trainId
        +String routeId
        +LocalDateTime departureTime
        +LocalDateTime arrivalTime
        +serialVersionUID : long
        +isDepartureTimeInFuture() boolean [AssertTrue]
        +isArrivalTimeAfterDepartureTime() boolean [AssertTrue]
    }

    class ScheduleDTO {
        <<DTO>>
        +String id
        +LocalDateTime departureTime
        +LocalDateTime arrivalTime
        +StatusSchedule status
        +String trainId
        +String trainName
        +String routeId
        +String routeCode
        +String departureStationName
        +String destinationStationName
    }

    class Schedule {
        <<entity>>
        +String id
        +LocalDateTime departureTime
        +LocalDateTime arrivalTime
        +StatusSchedule status
        +Train train
        +Route route
        +List~ScheduleDetail~ scheduleDetails
    }

    class ScheduleDetail {
        <<entity>>
        +String id
        +BigDecimal priceSeat
        +Seat seat
        +Schedule schedule
        +RouteStop routeStop
        +int version
    }

    class Train {
        <<entity>>
        +String id
        +String trainCode
        +TrainStatus status
        +List~Carriage~ carriages
    }

    class Carriage {
        <<entity>>
        +String id
        +int number
        +CarriageType type
        +Train train
        +List~Seat~ seats
    }

    class Seat {
        <<entity>>
        +String id
        +int number
        +SeatType type
        +Carriage carriage
    }

    class Route {
        <<entity>>
        +String id
        +String routeCode
        +Station departureStation
        +Station destinationStation
        +RouteStatus status
    }

    class ScheduleMapper {
        <<mapper>>
        +toDto(Schedule) ScheduleDTO
        +toEntityForUpdate(ScheduleUpdateDTO) Schedule
        +toDtoList(List~Schedule~) List~ScheduleDTO~
    }

    ScheduleUpdateDTO ..> Schedule : maps to via ScheduleMapper.toEntityForUpdate()
    ScheduleMapper ..> ScheduleDTO : produces
    ScheduleMapper ..> Schedule : reads
    Schedule "1" --> "1" Train
    Schedule "1" --> "1" Route
    Schedule "1" --> "*" ScheduleDetail
    ScheduleDetail "*" --> "1" Seat
    Train "1" --> "*" Carriage
    Carriage "1" --> "*" Seat
```
