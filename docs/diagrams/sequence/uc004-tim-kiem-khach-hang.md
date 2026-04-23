# Diagrams — UC004: Tìm kiếm khách hàng

---

## 1. System Architecture

```mermaid
graph TB
    subgraph CLIENT ["🖥️ Client (JavaFX)"]
        UI["CustomerManagementView\n(Tìm kiếm + phân trang)"]
        SC["SocketClient"]
    end

    subgraph TRANSPORT ["🔌 TCP Socket Transport"]
        OOS["ObjectOutputStream.writeObject(Request)"]
        OIS["ObjectInputStream.readObject() → Response"]
    end

    subgraph SERVER ["⚙️ Server (Java Socket Server)"]
        SRV["Server.java\nhandleClient(Socket)"]
        RR["RequestRouter.route(Request)\n(Hiện chưa có ActionType cho UC004)"]
        SVC["CustomerServiceImpl\n.searchCustomers(CustomerSearchDTO)"]
        VAL["ValidationUtils.validate(dto)"]
        JPA["JPAUtils.getEntityManager()"]
        REPO["CustomerRepositoryImpl\n.searchActiveCustomers(...)\n.countActiveCustomers(...)"]
    end

    subgraph DB ["🗄️ MariaDB"]
        T_CUS["customers"]
    end

    UI -- "new Request(UC004_SEARCH_CUSTOMERS?, CustomerSearchDTO)" --> SC
    SC --> OOS
    OOS -- "TCP Socket" --> SRV
    SRV --> RR
    RR --> SVC

    SVC --> VAL
    SVC --> JPA
    SVC --> REPO

    REPO -- "JPQL SELECT new CustomerDTO(...) WHERE isActive=true AND (kw LIKE ...)" --> T_CUS
    REPO -- "JPQL COUNT(Customer) WHERE isActive=true AND (kw LIKE ...)" --> T_CUS
```

---

## 2. Sequence Diagram

```mermaid
sequenceDiagram
    actor Clerk as 👤 Nhân viên
    participant UI as CustomerManagementView
    participant Socket as SocketClient
    participant Server as Server.java
    participant Router as RequestRouter
    participant Service as CustomerServiceImpl
    participant Repo as CustomerRepositoryImpl
    participant DB as MariaDB

    Clerk->>UI: Nhập keyword, chọn page/size, bấm "Tìm kiếm"
    UI->>UI: new CustomerSearchDTO(keyword, page, size)
    UI->>Socket: sendRequest(new Request(UC004_SEARCH_CUSTOMERS?, searchDTO))
    note over Socket,Router: Code hiện tại chưa có ActionType/RequestRouter cho UC004.\nSequence mô tả đường gọi tới CustomerServiceImpl.searchCustomers().
    Socket->>Server: ObjectOutputStream.writeObject(request)
    Server->>Router: route(request)
    Router->>Service: searchCustomers(searchDTO)

    Service->>Service: request = (searchDTO != null ? searchDTO : new CustomerSearchDTO())
    Service->>Service: ValidationUtils.validate(request)
    alt Lỗi validation (@Min page/size)
        Service-->>Router: Response.error(errors)
    else Hợp lệ
        Service->>Service: JPAUtils.getEntityManager() → em
        Service->>Repo: searchActiveCustomers(em, keyword, page, size)
        Repo->>DB: JPQL SELECT new CustomerDTO(c.id,c.name,c.idCard,c.phoneNumber,c.email,c.isActive) FROM Customer c WHERE c.isActive=true [AND kw] ORDER BY c.name
        DB-->>Repo: List<CustomerDTO> customers
        Repo-->>Service: customers

        Service->>Repo: countActiveCustomers(em, keyword)
        Repo->>DB: JPQL SELECT COUNT(c) FROM Customer c WHERE c.isActive=true [AND kw]
        DB-->>Repo: totalElements
        Repo-->>Service: totalElements

        Service->>Service: totalPages = ceil(totalElements/size)
        Service->>Service: build CustomerPageDTO(customers,totalElements,totalPages,currentPage)
        Service-->>Router: Response.success("Tìm kiếm khách hàng thành công", CustomerPageDTO)
    end

    alt Exception
        Service-->>Router: Response.error("Lỗi khi tìm kiếm khách hàng: " + e.message)
    end
```

---

## 3. Class Diagram

```mermaid
classDiagram
    class CustomerSearchDTO {
        <<DTO>>
        +String keyword
        +int page
        +int size
        +serialVersionUID : long
    }

    class CustomerPageDTO {
        <<DTO>>
        +List~CustomerDTO~ customers
        +long totalElements
        +int totalPages
        +int currentPage
        +serialVersionUID : long
    }

    class CustomerDTO {
        <<DTO>>
        +String customerId
        +String fullName
        +String idCard
        +String phone
        +String email
        +Boolean isActive
        +serialVersionUID : long
    }

    class Customer {
        <<entity>>
        +String id
        +String name
        +String idCard
        +String phoneNumber
        +String email
        +boolean isActive
    }

    class CustomerServiceImpl {
        +Response searchCustomers(CustomerSearchDTO)
    }

    class CustomerRepository {
        <<interface>>
        +List~CustomerDTO~ searchActiveCustomers(em, keyword, page, size)
        +long countActiveCustomers(em, keyword)
    }

    class CustomerRepositoryImpl {
        +List~CustomerDTO~ searchActiveCustomers(...)
        +long countActiveCustomers(...)
    }

    class Request {
        <<common>>
        +ActionType action
        +Object data
        +serialVersionUID : long
    }

    class Response {
        <<common>>
        +boolean success
        +String message
        +Object data
        +serialVersionUID : long
        +success(message, data)$
        +error(message)$
    }

    CustomerSearchDTO ..> Request : "data field"
    Request --> Response : "socket cycle"

    CustomerServiceImpl --> CustomerRepository : uses
    CustomerRepositoryImpl ..|> CustomerRepository
    CustomerRepository ..> CustomerDTO : returns
    CustomerDTO ..> Customer : "projected via JPQL new CustomerDTO(...)"
```
