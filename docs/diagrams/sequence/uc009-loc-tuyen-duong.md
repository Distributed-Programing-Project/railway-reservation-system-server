# Diagrams — UC009: Lọc tuyến đường

---

## 1. System Architecture

```mermaid
graph TB
    subgraph CLIENT ["Client (JavaFX)"]
        UI["RouteController"]
        SC["SocketClient"]
    end

    subgraph TRANSPORT ["TCP Socket Transport"]
        OOS["ObjectOutputStream.writeObject(Request)"]
        OIS["ObjectInputStream.readObject() -> Response"]
    end

    subgraph SERVER ["Server"]
        SRV["Server.handleClient()"]
        RR["RequestRouter.route(Request)"]
        SVC["RouteServiceImpl\n.findAllRoutes() / searchRoutes(...)"]
        REPO["RouteRepositoryImpl\n.findAllRoutes(em) / searchRoutes(em,...)"]
    end

    subgraph DB ["MariaDB"]
        T_ROUTE[("routes")]
        T_STATION[("stations")]
    end

    UI -- "route search request" --> SC
    SC --> OOS
    OOS --> SRV
    SRV --> RR
    RR --> SVC
    SVC --> REPO
    REPO --> T_ROUTE
    REPO --> T_STATION
    SVC --> RR
    RR --> SRV
    SRV --> OIS
    OIS --> SC
    SC --> UI
```

---

## 2. Sequence Diagram

```mermaid
sequenceDiagram
    actor Manager as Nhân viên quản lý
    participant Dashboard as DashboardController
    participant UI as RouteController
    participant SC as SocketClient
    participant SRV as Server.handleClient()
    participant RR as RequestRouter
    participant SVC as RouteServiceImpl
    participant REPO as RouteRepositoryImpl
    participant DB as MariaDB

    Manager->>Dashboard: Chọn chức năng "Quản lý tuyến đường"
    Dashboard->>UI: Mở màn hình quản lý tuyến đường

    UI->>SC: sendRequest(Yêu cầu lấy danh sách tuyến mặc định)
    SC->>SRV: ObjectOutputStream.writeObject(request)
    SRV->>RR: route(request)
    RR->>SVC: findAllRoutes()
    SVC->>REPO: findAllRoutes(em)
    REPO->>DB: SELECT r FROM Route r
    DB-->>REPO: List<Route>
    REPO-->>SVC: List<Route>
    SVC-->>RR: Response.success("Lấy danh sách tuyến thành công", List<RouteDTO>)
    RR-->>SRV: Response
    SRV-->>SC: ObjectInputStream.readObject()
    SC-->>UI: Response(data=danhSachMacDinh)
    UI-->>Manager: Hiển thị danh sách tuyến đường

    Manager->>UI: Chọn bộ lọc tuyến đường
    Manager->>UI: Nhập ga đi, ga đến, trạng thái và nhấn "Xác nhận"
    UI->>SC: sendRequest(Yêu cầu lọc tuyến đường)
    SC->>SRV: ObjectOutputStream.writeObject(request)
    SRV->>RR: route(request)
    RR->>SVC: searchRoutes(departureStationId, destinationStationId, status)
    SVC->>REPO: searchRoutes(em, departureStationId, destinationStationId, status)
    REPO->>DB: SELECT r FROM Route r WHERE 1=1 AND departureStation/destinationStation/status match
    DB-->>REPO: List<Route>
    REPO-->>SVC: List<Route>

    alt Không có tuyến nào phù hợp
        SVC-->>RR: Response.success("Không tìm thấy tuyến phù hợp", [])
        RR-->>SRV: Response
        SRV-->>SC: ObjectInputStream.readObject()
        SC-->>UI: Response(data=[])
        UI-->>Manager: Hiển thị danh sách rỗng hoặc thông báo không có dữ liệu
    else Có kết quả
        SVC-->>RR: Response.success("Lọc tuyến đường thành công", List<RouteDTO>)
        RR-->>SRV: Response
        SRV-->>SC: ObjectInputStream.readObject()
        SC-->>UI: Response(data=danhSachDaLoc)
        UI-->>Manager: Hiển thị danh sách tuyến đường đã lọc
    end
```

---

## 3. Class Diagram

```mermaid
classDiagram
    class Route {
        <<entity>>
        +String id
        +String routeCode
        +Station departureStation
        +Station destinationStation
        +RouteStatus status
        +Double priceBasic
        +List~RouteStop~ routeStops
        +List~Schedule~ schedules
    }

    class Station {
        <<entity>>
        +String id
        +String name
        +Float destinationKm
    }

    class RouteStatus {
        <<enum>>
        DRAFT
        ACTIVE
        PAUSED
        CANCELLED
    }

    class RouteDTO {
        <<DTO>>
        +String id
        +String routeCode
        +String departureStationId
        +String destinationStationId
        +RouteStatus status
        +Double priceBasic
    }

    class RouteService {
        <<service>>
        +findAllRoutes() List~Route~
        +searchRoutes(String departureStationId, String destinationStationId, String status) List~Route~
    }

    class RouteRepository {
        <<repository>>
        +findAllRoutes(EntityManager em) List~Route~
        +searchRoutes(EntityManager em, String departureStationId, String destinationStationId, String status) List~Route~
    }

    Route --> Station : departureStation
    Route --> Station : destinationStation
    Route --> RouteStatus : status
    RouteDTO ..> Route : mapped from
    RouteService ..> RouteRepository : uses
```
