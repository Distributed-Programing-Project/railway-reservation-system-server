# Diagrams — UC011: Sửa tuyến đường

---

## 1. System Architecture

```mermaid
graph TB
    subgraph CLIENT ["Client (JavaFX)"]
        UI["RouteController"]
        Dialog["EditRouteDialog"]
        SC["SocketClient"]
    end

    subgraph TRANSPORT ["TCP Socket Transport"]
        OOS["ObjectOutputStream.writeObject(Request)"]
        OIS["ObjectInputStream.readObject() -> Response"]
    end

    subgraph SERVER ["Server"]
        SRV["Server.handleClient()"]
        RR["RequestRouter.route(Request)"]
        SVC["RouteServiceImpl\n.findRouteById(routeId) / updateRoute(RouteDTO)"]
        RT_REPO["RouteRepositoryImpl\n.findRouteById(em,id) / hasSchedules(em,id) / updateRoute(em,route) / searchRoutes(em,...)"]
        ST_REPO["StationRepositoryImpl\n.findAllStations(em) / findStationById(em,id)"]
    end

    subgraph DB ["MariaDB"]
        T_ROUTE[("routes")]
        T_STATION[("stations")]
        T_SCHEDULE[("schedules")]
    end

    Dialog -- "load route / update route request" --> SC
    SC --> OOS
    OOS --> SRV
    SRV --> RR
    RR --> SVC
    SVC --> RT_REPO
    SVC --> ST_REPO
    RT_REPO --> T_ROUTE
    RT_REPO --> T_SCHEDULE
    ST_REPO --> T_STATION
    SVC --> RR
    RR --> SRV
    SRV --> OIS
    OIS --> SC
    SC --> Dialog
```

---

## 2. Sequence Diagram

```mermaid
sequenceDiagram
    actor Manager as Nhân viên quản lý
    participant UI as RouteController
    participant Dialog as EditRouteDialog
    participant StationRepo as StationRepositoryImpl
    participant SC as SocketClient
    participant SRV as Server.handleClient()
    participant RR as RequestRouter
    participant SVC as RouteServiceImpl
    participant RouteRepo as RouteRepositoryImpl
    participant DB as MariaDB

    Manager->>UI: Chọn tuyến đường cần sửa và nhấn "Sửa"
    UI->>Dialog: Mở form sửa tuyến đường

    Dialog->>SC: sendRequest(Yêu cầu lấy tuyến đường theo routeId)
    SC->>SRV: ObjectOutputStream.writeObject(request)
    SRV->>RR: route(request)
    RR->>SVC: findRouteById(routeId)
    SVC->>RouteRepo: findRouteById(em, routeId)
    RouteRepo->>DB: SELECT r FROM Route r WHERE r.id = :routeId
    DB-->>RouteRepo: Route
    RouteRepo-->>SVC: route
    SVC-->>RR: Response.success("Lấy thông tin tuyến thành công", RouteDTO)
    RR-->>SRV: Response
    SRV-->>SC: ObjectInputStream.readObject()
    SC-->>Dialog: Response(data=routeDTO)

    Dialog->>StationRepo: findAllStations(em)
    StationRepo->>DB: SELECT s FROM Station s
    DB-->>StationRepo: List<Station>
    StationRepo-->>Dialog: List<Station>
    Dialog-->>Manager: Hiển thị form với dữ liệu đã điền sẵn

    Manager->>Dialog: Chỉnh sửa thông tin và nhấn "Xác nhận"
    Dialog->>SC: sendRequest(Yêu cầu cập nhật tuyến đường)
    SC->>SRV: ObjectOutputStream.writeObject(request)
    SRV->>RR: route(request)
    RR->>SVC: updateRoute(routeDTO)

    SVC->>RouteRepo: findRouteById(em, routeId)
    RouteRepo->>DB: SELECT r FROM Route r WHERE r.id = :routeId
    DB-->>RouteRepo: Route
    RouteRepo-->>SVC: existingRoute

    alt Không tìm thấy tuyến đường
        SVC-->>RR: Response.error("Không tìm thấy tuyến đường")
        RR-->>SRV: Response
        SRV-->>SC: ObjectInputStream.readObject()
        SC-->>Dialog: Response(success=false)
        Dialog-->>Manager: Hiển thị thông báo lỗi
    end

    SVC->>RouteRepo: hasSchedules(em, routeId)
    RouteRepo->>DB: SELECT COUNT(s) FROM Schedule s WHERE s.route.id = :routeId
    DB-->>RouteRepo: boolean / count
    RouteRepo-->>SVC: hasSchedules

    alt Tuyến đường đã có lịch trình
        SVC-->>RR: Response.error("Không thể sửa tuyến đường đã gắn lịch trình")
        RR-->>SRV: Response
        SRV-->>SC: ObjectInputStream.readObject()
        SC-->>Dialog: Response(success=false)
        Dialog-->>Manager: Hiển thị thông báo lỗi ràng buộc lịch trình
    end

    alt departureStationId == destinationStationId
        SVC-->>RR: Response.error("Ga đi và ga đến không được trùng nhau")
        RR-->>SRV: Response
        SRV-->>SC: ObjectInputStream.readObject()
        SC-->>Dialog: Response(success=false)
        Dialog-->>Manager: Hiển thị thông báo lỗi trùng ga
    end

    alt priceBasic < 0
        SVC-->>RR: Response.error("Giá cơ bản không hợp lệ")
        RR-->>SRV: Response
        SRV-->>SC: ObjectInputStream.readObject()
        SC-->>Dialog: Response(success=false)
        Dialog-->>Manager: Hiển thị thông báo lỗi giá cơ bản
    end

    SVC->>RouteRepo: searchRoutes(em, departureStationId, destinationStationId, null)
    RouteRepo->>DB: SELECT r FROM Route r WHERE departureStation.id = :depId AND destinationStation.id = :destId
    DB-->>RouteRepo: List<Route>
    RouteRepo-->>SVC: List<Route>

    alt Đã có tuyến đường khác trùng cặp ga đi/ga đến
        SVC-->>RR: Response.error("Đã tồn tại tuyến đường khác với cặp ga này")
        RR-->>SRV: Response
        SRV-->>SC: ObjectInputStream.readObject()
        SC-->>Dialog: Response(success=false)
        Dialog-->>Manager: Hiển thị thông báo tuyến đã tồn tại
    else Dữ liệu hợp lệ
        SVC->>StationRepo: findStationById(em, departureStationId)
        StationRepo->>DB: SELECT s FROM Station s WHERE s.id = :departureStationId
        DB-->>StationRepo: Station
        StationRepo-->>SVC: departureStation

        SVC->>StationRepo: findStationById(em, destinationStationId)
        StationRepo->>DB: SELECT s FROM Station s WHERE s.id = :destinationStationId
        DB-->>StationRepo: Station
        StationRepo-->>SVC: destinationStation

        SVC->>SVC: existingRoute.set(routeCode, departureStation, destinationStation, priceBasic)
        SVC->>DB: BEGIN TRANSACTION
        SVC->>RouteRepo: updateRoute(em, existingRoute)
        RouteRepo->>DB: UPDATE routes SET route_code=?, departure_station_id=?, destination_station_id=?, price_basic=? WHERE route_id=?
        DB-->>RouteRepo: 1 row updated
        RouteRepo-->>SVC: true
        SVC->>DB: COMMIT
        SVC-->>RR: Response.success("Cập nhật tuyến đường thành công", routeId)
        RR-->>SRV: Response
        SRV-->>SC: ObjectInputStream.readObject()
        SC-->>Dialog: Response(success=true)
        Dialog-->>Manager: Hiển thị thông báo cập nhật thành công
        Dialog->>UI: Yêu cầu tải lại danh sách tuyến đường
        UI-->>Manager: Hiển thị danh sách đã cập nhật
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
        +List~Schedule~ schedules
    }

    class Station {
        <<entity>>
        +String id
        +String name
        +Float destinationKm
    }

    class Schedule {
        <<entity>>
        +String id
        +Route route
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

    class RouteStatus {
        <<enum>>
        DRAFT
        ACTIVE
        PAUSED
        CANCELLED
    }

    class RouteRepository {
        <<repository>>
        +findRouteById(EntityManager em, String routeId) Route
        +hasSchedules(EntityManager em, String routeId) boolean
        +searchRoutes(EntityManager em, String departureStationId, String destinationStationId, String status) List~Route~
        +updateRoute(EntityManager em, Route route) boolean
    }

    Route --> Station : departureStation
    Route --> Station : destinationStation
    Route --> RouteStatus : status
    Schedule --> Route : route
    RouteDTO ..> Route : updates
    RouteRepository ..> Route : loads and saves
```
