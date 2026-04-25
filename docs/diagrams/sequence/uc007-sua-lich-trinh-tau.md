# Diagrams — UC007: Sửa lịch trình tàu

---

## 1. System Architecture

```mermaid
graph TB
    subgraph CLIENT ["Client (JavaFX)"]
        UI["ScheduleManagementController"]
        SC["SocketClient"]
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
        JPA["JPAUtils.getEntityManager()"]
    end

    subgraph DB ["MariaDB"]
        T_EMP["employees"]
        T_SCH["schedules"]
        T_DET["schedule_details"]
        T_SEAT["seats / carriages"]
    end

    UI -- "new Request(UPDATE_SCHEDULE, ScheduleUpdateDTO)" --> SC
    SC --> OOS
    OOS -- "TCP Socket :9090" --> SRV
    SRV -- "castData(request, ScheduleUpdateDTO.class)" --> RR
    RR --> SVC
    SVC --> JPA
    SVC --> EMP_REPO
    EMP_REPO -- "SELECT e FROM Employee WHERE id" --> T_EMP
    SVC -- "check isManager\ncheck status=DRAFT\ncheck departureTime > now\ncheck arrivalTime > departureTime" --> SVC
    SVC --> SCH_REPO
    SCH_REPO -- "SELECT s FROM Schedule JOIN FETCH..." --> T_SCH
    SCH_REPO -- "UPDATE schedules SET ..." --> T_SCH
    SCH_REPO -- "DELETE FROM schedule_details\n(nếu train/route thay đổi)" --> T_DET
    SCH_REPO -- "SELECT seats + em.persist(ScheduleDetail) x N\n(nếu train/route thay đổi)" --> T_SEAT
    SVC -- "ScheduleMapper.toDto(schedule) → ScheduleDTO" --> SVC
    SVC -- "Response.success(ScheduleDTO)" --> RR
    RR -- "Response" --> SRV
    SRV -- "out.writeObject(response) + out.reset()" --> OIS
    OIS -- "TCP Socket" --> SC
    SC -- "response.isSuccess()" --> UI
```

---

## 2. Sequence Diagram

```mermaid
sequenceDiagram
    actor Manager as Nhân viên quản lý
    participant UI as ScheduleManagementController
    participant SC as SocketClient
    participant SRV as Server.handleClient()
    participant RR as RequestRouter
    participant SVC as ScheduleServiceImpl
    participant EMP_REPO as EmployeeRepositoryImpl
    participant SCH_REPO as ScheduleRepositoryImpl
    participant DB as MariaDB

    User->>UI: Đăng nhập thành công
    User->>UI: Chọn màn hình "Quản lý lịch trình"
    User->>UI: Nhấn "Sửa" → điền form → nhấn "Xác nhận"
    UI ->> UI: Tạo ScheduleUpdateDTO(requestEmployeeId, scheduleId, trainId, routeId, departureTime, arrivalTime)
    UI ->> SC: sendRequest(new Request(UPDATE_SCHEDULE, scheduleUpdateDTO))
    SC ->> SRV: ObjectOutputStream.writeObject(request)

    SRV ->> RR: router.route(request)
    RR ->> SVC: updateSchedule(castData(request, ScheduleUpdateDTO.class))

    SVC ->> SVC: ValidationUtils.validate(scheduleUpdateDTO)
    alt Validation thất bại
        SVC -->> RR: Response.error("Lỗi: ...")
        RR -->> SRV: Response
        SRV -->> SC: out.writeObject(response) + out.reset()
        SC -->> UI: response.isSuccess() = false
        UI -->> Manager: Hiển thị thông báo lỗi
    end

    SVC ->> EMP_REPO: findEmployeeById(em, requestEmployeeId)
    EMP_REPO ->> DB: SELECT e FROM Employee e WHERE e.id = :id
    DB -->> EMP_REPO: Employee (hoặc null)
    EMP_REPO -->> SVC: employee

    alt employee null hoặc isManager = false
        SVC -->> RR: Response.error("Bạn không có quyền thực hiện thao tác này")
        RR -->> SRV: Response
        SRV -->> SC: out.writeObject(response) + out.reset()
        SC -->> UI: response.isSuccess() = false
        UI -->> Manager: Thông báo không có quyền
    end

    SVC ->> SVC: em = JPAUtils.getEntityManager(), tx.begin()

    SVC ->> SCH_REPO: findScheduleById(em, scheduleId)
    SCH_REPO ->> DB: SELECT s FROM Schedule s JOIN FETCH s.route r JOIN FETCH r.departureStation JOIN FETCH r.destinationStation JOIN FETCH s.train WHERE s.id = :id
    DB -->> SCH_REPO: Schedule (hoặc null)
    SCH_REPO -->> SVC: existingSchedule

    alt existingSchedule = null
        SVC ->> SVC: tx.rollback()
        SVC -->> RR: Response.error("Không tìm thấy lịch trình: id=...")
        RR -->> SRV: Response
        SRV -->> SC: out.writeObject(response) + out.reset()
        SC -->> UI: response.isSuccess() = false
        UI -->> Manager: Thông báo lỗi
    end

    alt existingSchedule.status != DRAFT
        SVC ->> SVC: tx.rollback()
        SVC -->> RR: Response.error("Chỉ được phép sửa lịch trình khi đang ở trạng thái Nháp")
        RR -->> SRV: Response
        SRV -->> SC: out.writeObject(response) + out.reset()
        SC -->> UI: response.isSuccess() = false
        UI -->> Manager: Thông báo lỗi trạng thái
    end

    SVC ->> SCH_REPO: updateSchedule(em, scheduleToUpdate)

    SCH_REPO ->> DB: em.find(Schedule, scheduleId)
    DB -->> SCH_REPO: existingSchedule (với train, route đầy đủ)
    SCH_REPO ->> SCH_REPO: So sánh trainId cũ vs mới, routeId cũ vs mới

    alt trainId HOẶC routeId thay đổi
        SCH_REPO ->> DB: DELETE FROM schedule_details WHERE schedule.id = :scheduleId
        SCH_REPO ->> DB: SELECT s.id FROM Seat s WHERE s.carriage.train.id = :newTrainId
        DB -->> SCH_REPO: List[seatId] (N ghế)
        loop Tạo lại ScheduleDetail (batch size 50)
            SCH_REPO ->> DB: em.persist(ScheduleDetail(schedule, seat, priceSeat=0, routeStop=null))
        end
    end

    SCH_REPO ->> DB: existingSchedule.set(train, route, departureTime, arrivalTime) → dirty check → UPDATE schedules
    DB -->> SCH_REPO: OK
    SCH_REPO -->> SVC: true

    SVC ->> SVC: tx.commit()
    SVC ->> SVC: ScheduleMapper.toDto(existingSchedule) → scheduleDTO
    SVC -->> RR: Response.success("Cập nhật lịch trình thành công", scheduleDTO)
    RR -->> SRV: Response
    SRV -->> SC: out.writeObject(response) + out.reset()
    SC -->> UI: response.isSuccess() = true
    UI -->> Manager: Đóng form, hiển thị "Cập nhật lịch trình thành công", tải lại danh sách
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
        +boolean isDepartureTimeInFuture()
        +boolean isArrivalTimeAfterDepartureTime()
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
        +toEntity(ScheduleDTO) Schedule
        +toDtoList(List~Schedule~) List~ScheduleDTO~
    }

    ScheduleUpdateDTO ..> Schedule : maps to via service
    ScheduleMapper ..> ScheduleDTO : produces
    ScheduleMapper ..> Schedule : reads

    Schedule --> Train : ManyToOne
    Schedule --> Route : ManyToOne
    Schedule "1" --> "N" ScheduleDetail : OneToMany cascade REMOVE

    ScheduleDetail --> Seat : ManyToOne
    ScheduleDetail --> Schedule : ManyToOne

    Seat --> Carriage : ManyToOne
    Carriage --> Train : ManyToOne
```
