# UC015 — Quản lý tàu

## 1. System Architecture

```mermaid
graph TB
    subgraph CLIENT ["🖥 Client — JavaFX"]
        UI["TrainManagementController"]
        SC["SocketClient.sendRequest()"]
    end

    subgraph TRANSPORT ["🔌 TCP Socket — ObjectStream"]
        REQ["Request { action: FIND_ALL_TRAINS | CREATE_TRAIN\n           | CREATE_CARRIAGE | UPDATE_TRAIN_CARRIAGES\n           | UPDATE_TRAIN_STATUS | FIND_TRAIN_BY_CODE\n           | FIND_UNASSIGNED_CARRIAGES\n data: TrainFilterDTO | CreateTrainDTO | ... }"]
        RES["Response { success, message\n data: List&lt;TrainDTO&gt; | TrainDTO | List&lt;CarriageDTO&gt; }"]
    end

    subgraph SERVER ["⚙️ Server — Java 21"]
        SV["Server.handleClient()\n ObjectInputStream / ObjectOutputStream"]
        RR["RequestRouter.route()\n case FIND_ALL_TRAINS / CREATE_TRAIN / ..."]
        TS["TrainService\n .findAllTrains() / .createTrain()\n .createCarriage() / .updateTrainCarriages()\n .updateTrainStatus() / .findTrainByCode()\n .findUnassignedCarriages()"]
        TR["TrainRepository\n .findAllTrains() / .findByCode()\n .existsByTrainCode() / .save()"]
        CR["CarriageRepository\n .findUnassigned() / .findByIds()\n .saveCarriage() / .updateCarriage()"]
        SR["ScheduleRepository\n .countFutureActiveByTrainId()"]
    end

    subgraph DB ["🗄 MariaDB — localhost:3307"]
        TRAINS["trains"]
        CARRIAGES["carriages"]
        SEATS["seats"]
        SCHEDULES["schedules"]
    end

    UI -->|"1. build DTO + Request"| SC
    SC -->|"ObjectOutputStream.writeObject()"| REQ
    REQ -->|"TCP"| SV
    SV -->|"route(request)"| RR
    RR -->|"castData → DTO"| TS
    TS --> TR
    TS --> CR
    TS --> SR
    TR -->|"JPQL"| TRAINS
    CR -->|"JPQL"| CARRIAGES
    CR -->|"persist Seat"| SEATS
    SR -->|"COUNT query"| SCHEDULES
    TS -->|"TrainMapper.toDto()"| RR
    RR -->|"Response.success(dto)"| SV
    SV -->|"ObjectOutputStream.writeObject()\nout.reset()"| RES
    RES -->|"TCP"| SC
    SC -->|"2. update UI"| UI
```

---

## 2. Sequence Diagram

```mermaid
sequenceDiagram
    actor Manager as 👤 Quản lý
    participant UI as TrainManagementController
    participant SC as SocketClient
    participant SV as Server.handleClient()
    participant RR as RequestRouter
    participant TS as TrainServiceImpl
    participant TR as TrainRepositoryImpl
    participant CR as CarriageRepositoryImpl
    participant SR as ScheduleRepositoryImpl
    participant DB as MariaDB

    Manager->>UI: Vào màn hình Quản lý tàu

    UI->>SC: sendRequest(FIND_ALL_TRAINS, TrainFilterDTO)
    SC->>SV: ObjectOutputStream.writeObject(request)
    SV->>RR: route(request)
    RR->>TS: findAllTrains(TrainFilterDTO)
    TS->>TR: findAllTrains(statusFilter)
    TR->>DB: SELECT t FROM Train t [WHERE t.status = :status] JOIN FETCH t.carriages
    DB-->>TR: List<Train>
    TR-->>TS: List<Train>
    TS->>TS: TrainMapper.toDtoList(trains) — totalCarriages, totalSeats computed
    TS-->>RR: Response.success(List<TrainDTO>)
    SV-->>SC: writeObject(response) + reset()
    SC-->>UI: List<TrainDTO>
    UI-->>Manager: Hiển thị danh sách tàu

    alt Luồng A — Tra cứu tàu
        Manager->>UI: Nhập trainCode + "Tìm kiếm"
        UI->>SC: sendRequest(FIND_TRAIN_BY_CODE, "SE")
        SC->>SV: writeObject(request)
        SV->>RR: route(request)
        RR->>TS: findTrainByCode(String trainCode)
        TS->>TR: findByCode(trainCode) — LIKE %code% case-insensitive
        TR->>DB: SELECT t FROM Train t JOIN FETCH t.carriages WHERE LOWER(t.trainCode) LIKE :pattern
        DB-->>TR: List<Train>
        TR-->>TS: List<Train>
        TS-->>RR: Response.success(List<TrainDTO> with carriages)
        SV-->>SC: writeObject(response) + reset()
        SC-->>UI: List<TrainDTO>
        UI-->>Manager: Hiển thị kết quả tìm kiếm
    end

    alt Luồng B — Lập tàu mới
        Manager->>UI: Nhập trainCode + chọn toa + "Xác nhận"
        UI->>SC: sendRequest(CREATE_TRAIN, CreateTrainDTO)
        SC->>SV: writeObject(request)
        SV->>RR: route(request)
        RR->>TS: createTrain(CreateTrainDTO)
        TS->>TS: ValidationUtils.validate(dto)
        TS->>TR: existsByTrainCode(trainCode) — case-insensitive check
        TR->>DB: SELECT COUNT(t) WHERE LOWER(trainCode) = :code
        DB-->>TR: count
        alt trainCode trùng
            TS-->>RR: Response.error("Mác tàu đã tồn tại")
        end
        TS->>CR: findCarriagesByIds(carriageIds)
        CR->>DB: SELECT c FROM Carriage c WHERE c.id IN :ids AND c.train IS NULL
        DB-->>CR: List<Carriage>
        alt toa đã bị gán
            TS-->>RR: Response.error("Một hoặc nhiều toa đã được gán cho tàu khác")
        end
        TS->>TS: Validate số toa [3, 16]
        TS->>TR: saveTrain(Train) — doInTransaction
        TR->>DB: INSERT INTO trains
        TS->>CR: assignCarriagesToTrain(carriages, train) — set number, train
        CR->>DB: UPDATE carriages SET train_id, sequence_number
        TS-->>RR: Response.success("Tàu được tạo thành công", TrainDTO)
        SV-->>SC: writeObject(response) + reset()
        SC-->>UI: TrainDTO
        UI-->>Manager: "Tàu [SE5] được tạo thành công"
    end

    alt Luồng D — Đăng ký toa mới
        Manager->>UI: Chọn CarriageType + "Lưu"
        UI->>SC: sendRequest(CREATE_CARRIAGE, CarriageType)
        SC->>SV: writeObject(request)
        SV->>RR: route(request)
        RR->>TS: createCarriage(CarriageType)
        TS->>CR: saveCarriageWithSeats(Carriage, List<Seat>) — doInTransaction
        CR->>DB: INSERT INTO carriages (train_id=null, number=0)
        CR->>DB: INSERT INTO seats (seat 1..N theo CarriageType)
        TS-->>RR: Response.success("Toa đã được đăng ký", CarriageDTO)
        SV-->>SC: writeObject(response) + reset()
        SC-->>UI: CarriageDTO
        UI-->>Manager: "Toa mới đã được đăng ký vào hệ thống"
    end

    alt Luồng E — Cấu hình lại tàu
        Manager->>UI: Chọn tàu + "Cấu hình tàu"
        UI->>SC: sendRequest(UPDATE_TRAIN_CARRIAGES, UpdateTrainCarriagesDTO)
        SC->>SV: writeObject(request)
        SV->>RR: route(request)
        RR->>TS: updateTrainCarriages(UpdateTrainCarriagesDTO)
        TS->>SR: countFutureActiveSchedulesByTrainId(trainId)
        SR->>DB: SELECT COUNT(s) FROM Schedule s WHERE s.train.id = :id AND s.departureTime > now AND s.status != CANCELLED
        DB-->>SR: count
        alt count > 0
            TS-->>RR: Response.error("Không thể cấu hình tàu đang có lịch trình tương lai")
        end
        TS->>TS: Validate carriageIds.size() trong [3, 16]
        TS->>CR: reassignCarriages(trainId, carriageIds) — doInTransaction
        CR->>DB: UPDATE old carriages SET train_id=null, number=0
        CR->>DB: UPDATE new carriages SET train_id, number=position
        TS-->>RR: Response.success("Cấu hình tàu đã được cập nhật", TrainDTO)
        SV-->>SC: writeObject(response) + reset()
        UI-->>Manager: "Cấu hình tàu đã được cập nhật"
    end

    alt Luồng F — Đổi trạng thái tàu
        Manager->>UI: Chọn tàu + trạng thái mới + "Xác nhận"
        UI->>SC: sendRequest(UPDATE_TRAIN_STATUS, UpdateTrainStatusDTO)
        SC->>SV: writeObject(request)
        SV->>RR: route(request)
        RR->>TS: updateTrainStatus(UpdateTrainStatusDTO)
        TS->>SR: countFutureActiveSchedulesByTrainId(trainId) — nếu status != ACTIVE
        alt count > 0
            TS-->>RR: Response.error("Không thể thay đổi tàu đang có lịch trình tương lai")
        end
        TS->>TR: updateTrainStatus(trainId, status) — doInTransaction
        TR->>DB: UPDATE trains SET status = :status WHERE train_id = :id
        TS-->>RR: Response.success("Trạng thái tàu đã được cập nhật", TrainDTO)
        SV-->>SC: writeObject(response) + reset()
        UI-->>Manager: "Trạng thái tàu đã được cập nhật"
    end
```

---

## 3. Class Diagram

```mermaid
classDiagram
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

    class Schedule {
        <<entity>>
        +String id
        +LocalDateTime departureTime
        +StatusSchedule status
        +Train train
    }

    class TrainStatus {
        <<enum>>
        ACTIVE
        MAINTENANCE
        INACTIVE
    }

    class CarriageType {
        <<enum>>
        HARD_SEAT
        SOFT_SEAT
        SOFT_SEAT_AC
        BERTH_6
        BERTH_4
    }

    class SeatType {
        <<enum>>
        HARD_SEAT
        SOFT_SEAT
        VIP_SEAT
        BERTH_6
        BERTH_4
    }

    class TrainDTO {
        <<DTO>>
        +String id
        +String trainCode
        +TrainStatus status
        +int totalCarriages
        +int totalSeats
        +List~CarriageDTO~ carriages
        +long serialVersionUID = 1L
    }

    class CarriageDTO {
        <<DTO>>
        +String id
        +int number
        +CarriageType type
        +String trainId
        +long serialVersionUID = 1L
    }

    class TrainFilterDTO {
        <<DTO>>
        +TrainStatus statusFilter
        +long serialVersionUID = 1L
    }

    class CreateTrainDTO {
        <<DTO>>
        +String trainCode
        +List~String~ carriageIds
        +long serialVersionUID = 1L
    }

    class UpdateTrainCarriagesDTO {
        <<DTO>>
        +String trainId
        +List~String~ carriageIds
        +long serialVersionUID = 1L
    }

    class UpdateTrainStatusDTO {
        <<DTO>>
        +String trainId
        +TrainStatus status
        +long serialVersionUID = 1L
    }

    Train "1" --> "many" Carriage : carriages
    Carriage "1" --> "many" Seat : seats
    Schedule --> Train : train
    Train --> TrainStatus : status
    Carriage --> CarriageType : type
    Seat --> SeatType : type

    TrainDTO --> TrainStatus : status
    TrainDTO "1" --> "many" CarriageDTO : carriages
    TrainFilterDTO --> TrainStatus : statusFilter
    UpdateTrainStatusDTO --> TrainStatus : status
```
