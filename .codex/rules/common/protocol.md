# Communication Protocol

All socket messages use Java `ObjectOutputStream` / `ObjectInputStream`. Both `Request` and `Response` implement `Serializable`.

```java
// client → server
class Request implements Serializable {
    ActionType action;
    Object data;
}

// server → client
class Response implements Serializable {
    boolean success;
    String message;
    Object data;    // a DTO, or null
}
```

**Static factories on `Response`:**
```java
Response.success("message", data);
Response.error("message");
```

---

## ActionType — The Client↔Server Bridge

Every action the client can request must be declared in `common/command/ActionType.java`. The server switches on this enum to route to the correct service.

Naming convention: `VERB_ENTITY` or `VERB_ENTITY_BY_FIELD`

```java
public enum ActionType {
    LOGIN,

    CREATE_TICKET,
    UPDATE_TICKET,
    DELETE_TICKET,
    FIND_TICKET_BY_ID,
    FIND_ALL_TICKETS,
}
```

---

## Full Request Flow

```java
// Client
Request req = new Request(ActionType.LOGIN, loginDTO);
Response res = socketClient.sendRequest(req);

// Server (network layer)
switch (request.getAction()) {
    case LOGIN -> {
        LoginDTO dto = (LoginDTO) request.getData();
        return employeeService.login(dto);
    }
}
```
