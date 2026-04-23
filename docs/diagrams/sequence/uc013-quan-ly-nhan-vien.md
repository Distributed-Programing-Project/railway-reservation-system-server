# UC013 — Quản lý nhân viên

## 1. System Architecture Diagram

```mermaid
graph TB
    subgraph CLIENT ["Client (JavaFX)"]
        UI["EmployeeManagementController"]
        SC["SocketClient\n(ObjectOutputStream / ObjectInputStream)"]
    end

    subgraph COMMON ["Common Module"]
        REQ["Request\n{ ActionType action; Object data; }"]
        RES["Response\n{ boolean success; String message; Object data; }"]
        AT["ActionType\nCREATE_EMPLOYEE\nCREATE_EMPLOYEE_ACCOUNT\nDELETE_EMPLOYEE\nFIND_ALL_EMPLOYEES\nRESET_EMPLOYEE_PASSWORD"]
    end

    subgraph SERVER ["Server"]
        SRV["Server.java\nServerSocket :9090\nThreadPool"]
        RR["RequestRouter.java\nswitch ActionType"]
        ES["EmployeeService\nbusiness logic + tx"]
        ER["EmployeeRepository\nJPQL queries"]
        EM["EmployeeMapper"]
    end

    subgraph DB ["Persistence"]
        JPA["JPA / Hibernate 7"]
        MDB[("MariaDB\nlocalhost:3307")]
    end

    UI -->|"build Request"| SC
    SC -->|"writeObject Request"| SRV
    SRV -->|"submit to ThreadPool"| RR
    RR -->|"castData to DTO"| ES
    ES -->|"validate + begin tx"| ER
    ER -->|"TypedQuery persist"| JPA
    JPA -->|"SQL"| MDB
    MDB -->|"ResultSet"| JPA
    JPA -->|"Employee entity"| ER
    ER -->|"Employee"| ES
    ES -->|"map to DTO"| EM
    EM -->|"EmployeeDTO"| ES
    ES -->|"Response.success/error"| RR
    RR -->|"Response"| SRV
    SRV -->|"writeObject Response"| SC
    SC -->|"Response"| UI
```

---

## 2. Sequence Diagrams

### Luồng A — Tạo hồ sơ nhân viên

```mermaid
sequenceDiagram
    actor User as Nhân viên quản lý
    participant UI as EmployeeManagementController
    participant SC as SocketClient
    participant Router as RequestRouter
    participant Svc as EmployeeService
    participant Repo as EmployeeRepository
    participant DB as MariaDB

    User->>UI: Đăng nhập thành công
    User->>UI: Chọn màn hình "Quản lý nhân viên"
    User->>UI: Nhập thông tin nhân viên → nhấn Lưu
    UI->>UI: validate client-side (format email, SĐT, CCCD)
    UI->>SC: sendRequest(new Request(CREATE_EMPLOYEE, employeeDTO))
    SC->>Router: ObjectInputStream.readObject() → Request
    Router->>Router: castData(request, EmployeeDTO.class)
    Router->>Svc: createEmployee(employeeDTO)
    Svc->>Svc: validate(employeeDTO) — null checks, format
    Svc->>Repo: existsByNationalId(nationalId)
    Repo-->>Svc: false
    Svc->>Repo: existsByEmail(email)
    Repo-->>Svc: false
    Svc->>Repo: generateEmployeeCode(isManager)
    Repo-->>Svc: "NV001" hoặc "QL001"
    Svc->>Svc: em.getTransaction().begin()
    Svc->>Repo: saveEmployee(employee)
    Repo->>DB: INSERT INTO employees (...)
    DB-->>Repo: OK
    Svc->>Svc: em.getTransaction().commit()
    Svc->>Svc: EmployeeMapper.toDto(employee)
    Svc-->>Router: Response.success("Tạo nhân viên thành công", employeeDTO)
    Router-->>SC: ObjectOutputStream.writeObject(response) + out.reset()
    SC-->>UI: Response { success=true, data=EmployeeDTO }
    UI-->>User: Hiển thị thông báo + employeeCode được sinh
```

### Luồng B — Cấp tài khoản

```mermaid
sequenceDiagram
    actor User as Nhân viên quản lý
    participant UI as EmployeeManagementController
    participant SC as SocketClient
    participant Router as RequestRouter
    participant Svc as EmployeeService
    participant Repo as EmployeeRepository
    participant DB as MariaDB

    User->>UI: Đăng nhập thành công
    User->>UI: Chọn màn hình "Quản lý nhân viên"
    User->>UI: Chọn nhân viên chưa có tài khoản → Cấp tài khoản
    UI->>UI: Xác nhận hành động
    UI->>SC: sendRequest(new Request(CREATE_EMPLOYEE_ACCOUNT, employeeId))
    SC->>Router: ObjectInputStream.readObject() → Request
    Router->>Router: castData(request, String.class)
    Router->>Svc: createEmployeeAccount(employeeId)
    Svc->>Repo: findEmployeeById(employeeId)
    Repo-->>Svc: Employee
    Svc->>Svc: check employee.getAccount() == null
    Svc->>Svc: generate rawPassword (UUID random)
    Svc->>Svc: BCrypt.hashpw(rawPassword, BCrypt.gensalt())
    Svc->>Svc: build Account { username=employeeCode, password=hashed, active=true }
    Svc->>Svc: em.getTransaction().begin()
    Svc->>Repo: saveAccount(account)
    DB-->>Repo: INSERT INTO accounts (...)
    Svc->>Repo: linkAccountToEmployee(employee, account)
    DB-->>Repo: UPDATE employees SET account_id=...
    Svc->>Svc: em.getTransaction().commit()
    Svc-->>Router: Response.success("Cấp tài khoản thành công", AccountCreatedDTO { username, temporaryPassword=rawPassword })
    Router-->>SC: writeObject(response) + reset()
    SC-->>UI: Response { data=AccountCreatedDTO }
    UI-->>User: Hiển thị username + mật khẩu tạm thời (1 lần duy nhất)
```

### Luồng C — Xoá mềm nhân viên

```mermaid
sequenceDiagram
    actor User as Nhân viên quản lý
    participant UI as EmployeeManagementController
    participant SC as SocketClient
    participant Router as RequestRouter
    participant Svc as EmployeeService
    participant Repo as EmployeeRepository
    participant DB as MariaDB

    User->>UI: Đăng nhập thành công
    User->>UI: Chọn màn hình "Quản lý nhân viên"
    User->>UI: Chọn nhân viên → Xác nhận xoá
    UI->>SC: sendRequest(new Request(DELETE_EMPLOYEE, employeeId))
    SC->>Router: readObject() → Request
    Router->>Svc: softDeleteEmployee(employeeId, currentManagerId)
    Svc->>Svc: check employeeId != currentManagerId
    Svc->>Repo: findEmployeeById(employeeId)
    Repo-->>Svc: Employee
    Svc->>Svc: em.getTransaction().begin()
    Svc->>Svc: employee.setEmployeeStatus(INACTIVE)
    Svc->>Svc: employee.setUpdatedAt(LocalDate.now())
    alt account != null
        Svc->>Svc: employee.getAccount().setActive(false)
    end
    Svc->>DB: UPDATE employees SET employment_status='INACTIVE', updated_at=...
    Svc->>Svc: em.getTransaction().commit()
    Svc-->>Router: Response.success("Xoá mềm thành công", employeeDTO)
    Router-->>SC: writeObject(response) + reset()
    SC-->>UI: Response { success=true }
    UI-->>User: Thông báo xoá mềm thành công
```

### Luồng D — Reset mật khẩu

```mermaid
sequenceDiagram
    actor User as Nhân viên quản lý
    participant UI as EmployeeManagementController
    participant SC as SocketClient
    participant Router as RequestRouter
    participant Svc as EmployeeService
    participant Repo as EmployeeRepository
    participant DB as MariaDB

    User->>UI: Đăng nhập thành công
    User->>UI: Chọn màn hình "Quản lý nhân viên"
    User->>UI: Chọn nhân viên → Reset mật khẩu → Xác nhận
    UI->>SC: sendRequest(new Request(RESET_EMPLOYEE_PASSWORD, employeeId))
    SC->>Router: readObject() → Request
    Router->>Svc: resetEmployeePassword(employeeId)
    Svc->>Repo: findEmployeeById(employeeId)
    Repo-->>Svc: Employee
    Svc->>Svc: check employee.getAccount() != null
    Svc->>Svc: generate rawPassword (UUID random)
    Svc->>Svc: BCrypt.hashpw(rawPassword, BCrypt.gensalt())
    Svc->>Svc: em.getTransaction().begin()
    Svc->>DB: UPDATE accounts SET password=hashed WHERE account_id=...
    Svc->>Svc: em.getTransaction().commit()
    Svc-->>Router: Response.success("Reset thành công", AccountCreatedDTO { username, temporaryPassword=rawPassword })
    Router-->>SC: writeObject(response) + reset()
    SC-->>UI: Response { data=AccountCreatedDTO }
    UI-->>User: Hiển thị mật khẩu mới (1 lần duy nhất)
```

### Luồng E — Xem danh sách nhân viên

```mermaid
sequenceDiagram
    actor User as Nhân viên quản lý
    participant UI as EmployeeManagementController
    participant SC as SocketClient
    participant Router as RequestRouter
    participant Svc as EmployeeService
    participant Repo as EmployeeRepository
    participant DB as MariaDB

    User->>UI: Đăng nhập thành công
    User->>UI: Chọn màn hình "Quản lý nhân viên"
    User->>UI: Vào màn hình Quản lý nhân viên
    UI->>SC: sendRequest(new Request(FIND_ALL_EMPLOYEES, EmployeeFilterDTO { page=0, size=20, statusFilter=null }))
    SC->>Router: readObject() → Request
    Router->>Router: castData(request, EmployeeFilterDTO.class)
    Router->>Svc: findAllEmployees(filterDTO)
    Svc->>Repo: findAllEmployees(page, size, statusFilter)
    Repo->>DB: SELECT e FROM Employee e [WHERE e.employeeStatus = :status] ORDER BY e.createdAt DESC
    DB-->>Repo: List<Employee>
    Repo->>DB: SELECT COUNT(e) FROM Employee e [WHERE e.employeeStatus = :status]
    DB-->>Repo: totalElements
    Repo-->>Svc: List<Employee> + totalElements
    Svc->>Svc: EmployeeMapper.toDtoList(employees)
    Svc-->>Router: Response.success("OK", EmployeePageDTO { content, totalElements, totalPages, currentPage })
    Router-->>SC: writeObject(response) + reset()
    SC-->>UI: Response { data=EmployeePageDTO }
    UI-->>User: Hiển thị bảng danh sách nhân viên
```

---

## 3. Class Diagram

```mermaid
classDiagram
    class Employee {
        <<entity>>
        +String employeeId
        +String employeeCode
        +String employeeName
        +String nationalId
        +LocalDate dateOfBirth
        +Boolean gender
        +String address
        +String phoneNumber
        +String email
        +Boolean isManager
        +EmployeeStatus employeeStatus
        +LocalDate createdAt
        +LocalDate updatedAt
        +Account account
        +List~Invoice~ invoiceList
    }

    class Account {
        <<entity>>
        +String id
        +String username
        +String password
        +boolean active
    }

    class EmployeeStatus {
        <<enum>>
        ACTIVE
        PAUSE
        INACTIVE
    }

    class EmployeeDTO {
        <<DTO>>
        +String employeeId
        +String employeeCode
        +String employeeName
        +String nationalId
        +LocalDate dateOfBirth
        +Boolean gender
        +String address
        +String phoneNumber
        +String email
        +Boolean isManager
        +String employeeStatus
        +LocalDate createdAt
        +LocalDate updatedAt
        +String accountId
    }

    class EmployeeFilterDTO {
        <<DTO>>
        +int page
        +int size
        +EmployeeStatus statusFilter
    }

    class AccountCreatedDTO {
        <<DTO>>
        +String username
        +String temporaryPassword
    }

    class EmployeePageDTO {
        <<DTO>>
        +List~EmployeeDTO~ content
        +long totalElements
        +int totalPages
        +int currentPage
    }

    class EmployeeService {
        <<service>>
        +createEmployee(EmployeeDTO) Response
        +createEmployeeAccount(String employeeId) Response
        +softDeleteEmployee(String employeeId, String currentManagerId) Response
        +resetEmployeePassword(String employeeId) Response
        +findAllEmployees(EmployeeFilterDTO) Response
    }

    class EmployeeRepository {
        <<repository>>
        +saveEmployee(Employee) Employee
        +saveAccount(Account) Account
        +findEmployeeById(String) Employee
        +findAllEmployees(int page, int size, EmployeeStatus status) List~Employee~
        +countEmployees(EmployeeStatus status) long
        +existsByNationalId(String) boolean
        +existsByEmail(String) boolean
        +generateEmployeeCode(Boolean isManager) String
    }

    class EmployeeMapper {
        <<mapper>>
        +toDto(Employee) EmployeeDTO
        +toEntity(EmployeeDTO) Employee
        +toDtoList(List~Employee~) List~EmployeeDTO~
    }

    Employee "1" --> "0..1" Account : account
    Employee --> EmployeeStatus
    EmployeeService --> EmployeeRepository
    EmployeeService --> EmployeeMapper
    EmployeeMapper ..> Employee
    EmployeeMapper ..> EmployeeDTO
    EmployeeRepository ..> Employee
    EmployeeService ..> EmployeeDTO
    EmployeeService ..> EmployeeFilterDTO
    EmployeeService ..> AccountCreatedDTO
    EmployeeService ..> EmployeePageDTO
```
