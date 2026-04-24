# Diagrams — UC004: Thêm khách hàng

---

## 1. System Architecture

```mermaid
graph TB
    subgraph CLIENT ["Client (JavaFX)"]
        UI["CustomerManagementView\n(Thêm khách hàng form)"]
        SC["SocketClient"]
    end

    subgraph TRANSPORT ["TCP Socket Transport"]
        OOS["ObjectOutputStream.writeObject(Request)"]
        OIS["ObjectInputStream.readObject() → Response"]
    end

    subgraph SERVER ["Server (Java Socket Server)"]
        SRV["Server.java\nhandleClient(Socket)"]
        RR["RequestRouter.route(Request)\n(ActionType.CREATE_CUSTOMER)"]
        SVC["CustomerServiceImpl\n.createCustomer(CustomerDTO)"]
        VAL["ValidationUtils.validate(dto)\n(Jakarta Validation)"]
        MAP["CustomerMapper\nDTO ⇄ Entity"]
        JPA["JPAUtils.getEntityManager()"]
        REPO["CustomerRepositoryImpl\n.existsByIdCard(...)\n.existsByEmail(...)\n.createCustomer(...)"]
    end

    subgraph DB ["MariaDB"]
        T_CUS["customers"]
    end

    UI -- "new Request(CREATE_CUSTOMER, CustomerDTO)" --> SC
    SC --> OOS
    OOS -- "TCP Socket" --> SRV
    SRV --> RR
    RR --> SVC

    SVC --> VAL
    SVC --> JPA
    SVC --> REPO
    SVC --> MAP

    REPO -- "JPQL COUNT by idCard/email" --> T_CUS
    REPO -- "em.persist(Customer)" --> T_CUS
```

---

## 2. Sequence Diagram

```mermaid
sequenceDiagram
    actor Clerk as Nhân viên
    participant UI as CustomerManagementView
    participant Socket as SocketClient
    participant Server as Server.java
    participant Router as RequestRouter
    participant Service as CustomerServiceImpl
    participant Repo as CustomerRepositoryImpl
    participant DB as MariaDB

    Clerk->>UI: Đăng nhập thành công
    Clerk->>UI: Chọn màn hình "Quản lý khách hàng"
    Clerk->>UI: Chọn "Thêm khách hàng"
    UI->>UI: Nhập fullName, idCard, phone, email
    UI->>UI: new CustomerDTO(customerId=null, fullName, idCard, phone, email)
    UI->>Socket: sendRequest(new Request(CREATE_CUSTOMER, customerDTO))
    Socket->>Server: ObjectOutputStream.writeObject(request)
    Server->>Router: route(request)
    Router->>Service: createCustomer(customerDTO)

    Service->>Service: ValidationUtils.validate(customerDTO)
    alt Lỗi validation (@NotBlank/@Pattern/@Email)
        Service-->>Router: Response.error("Dữ liệu không hợp lệ: " + errors)
        Router-->>Server: Response
        Server-->>Socket: Response + out.reset()
        Socket-->>UI: Response(success=false)
    else customerDTO.customerId != null/blank
        Service-->>Router: Response.error("Customer ID phải để trống khi thêm mới")
    else Hợp lệ
        Service->>Service: JPAUtils.getEntityManager() → em

        Service->>Repo: existsByIdCard(em, idCard, exclude=null)
        Repo->>DB: JPQL SELECT COUNT(Customer) WHERE idCard=:idCard
        DB-->>Repo: count
        Repo-->>Service: true/false
        alt CCCD đã đăng ký
            Service-->>Router: Response.error("Số CCCD này đã được đăng ký...")
        else CCCD hợp lệ
            opt Email không null/blank
                Service->>Repo: existsByEmail(em, email, exclude=null)
                Repo->>DB: JPQL SELECT COUNT(Customer) WHERE email=:email
                DB-->>Repo: count
                Repo-->>Service: true/false
            end
            alt Email đã đăng ký
                Service-->>Router: Response.error("Email này đã được đăng ký...")
            else OK
                Service->>Service: em.getTransaction().begin()
                Service->>Service: CustomerMapper.toEntity(customerDTO)
                Service->>Service: customer.isActive = true
                Service->>Repo: createCustomer(em, customer)
                Repo->>DB: em.persist(Customer) → INSERT customers
                DB-->>Repo: customerId (UUID)
                Repo-->>Service: customer
                Service->>Service: em.getTransaction().commit()
                Service-->>Router: Response.success("Thêm khách hàng thành công", CustomerDTO)
            end
        end
    end

    alt Exception
        Service->>Service: rollbackQuietly(tx)
        Service-->>Router: Response.error("Lỗi khi thêm khách hàng: " + e.message)
    end
```

---

## 3. Class Diagram

```mermaid
classDiagram
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
        +String passport
        +String phoneNumber
        +String email
        +boolean isActive
    }

    class CustomerServiceImpl {
        +Response createCustomer(CustomerDTO)
    }

    class CustomerRepository {
        <<interface>>
        +boolean existsByIdCard(em, idCard, excludeCustomerId)
        +boolean existsByEmail(em, email, excludeCustomerId)
        +Customer createCustomer(em, customer)
    }

    class CustomerRepositoryImpl {
        +boolean existsByIdCard(...)
        +boolean existsByEmail(...)
        +Customer createCustomer(...)
    }

    class CustomerMapper {
        <<mapper>>
        +Customer toEntity(CustomerDTO)
        +CustomerDTO toDto(Customer)
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

    CustomerDTO ..> Request : "data field"
    Request --> Response : "socket cycle"

    CustomerServiceImpl --> CustomerRepository : uses
    CustomerRepositoryImpl ..|> CustomerRepository
    CustomerServiceImpl --> CustomerMapper : maps
    CustomerMapper ..> CustomerDTO : maps
    CustomerMapper ..> Customer : maps
```
