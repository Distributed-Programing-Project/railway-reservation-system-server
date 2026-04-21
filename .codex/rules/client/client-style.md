# Client / JavaFX UI Guidelines

This document outlines the strict rules for developing the JavaFX client interface.

## 1. Architectural Boundaries (CRITICAL)

The Client is a completely separate application from the Server. They only communicate over the network (TCP Sockets).

- **NO Entities:** You MUST NOT import any classes from `vn.edu.iuh.fit.server.model.*` (JPA Entities). The Client does not know about the database.
- **NO Direct DB Access:** You MUST NOT use Hibernate, `EntityManager`, or JDBC in the Client.
- **NO Direct Service Calls:** You MUST NOT instantiate or inject classes from `vn.edu.iuh.fit.server.service.*`.
- **Use DTOs:** All data sent to or received from the server must be a DTO (Data Transfer Object) from the `dto` package.

## 2. Package & Resource Location

- **Java Code:** All UI code (Controllers, Views, Client-side utilities) MUST live inside:
  `src/main/java/vn/edu/iuh/fit/client/`
- **Resources:** All `.fxml`, `.css`, and images MUST live inside:
  `src/main/resources/client/`

## 3. Controller Design

- **No Interface/Impl Pattern:** JavaFX controllers map directly to `.fxml` files via the `FXMLLoader`. You MUST write a single concrete class (e.g., `RouteController.java`). Do NOT create interfaces (e.g., `RouteController` + `RouteControllerImpl`). This is an anti-pattern for UI controllers.
- **Naming Convention:** Suffix all controller classes with `Controller` (e.g., `LoginController`, `ScheduleManagementController`).
- **FXML Binding:** Use the `@FXML` annotation for all UI components and event handler methods. Do not make them `public` unless absolutely necessary; keep them `private` or package-private.

## 4. Communication with Server

To perform an action (e.g., saving data or fetching a list), the Controller must:
1. Collect data from the JavaFX UI components.
2. Package the data into a DTO.
3. Create a `Request` object with the appropriate `ActionType` (e.g., `ActionType.ADD_ROUTE`).
4. Send the `Request` over the TCP Socket to the Server.
5. Wait for the `Response` object.
6. Check `response.isSuccess()`.
7. Update the UI or show an Alert box based on the response.

*Example:*
```java
// CORRECT
RouteDTO newRoute = new RouteDTO(txtRouteCode.getText(), ...);
Request req = new Request(ActionType.ADD_ROUTE, newRoute);
Response res = SocketClient.getInstance().sendRequest(req);
if(res.isSuccess()) {
    showAlert("Thành công");
}

// INCORRECT (Architecture Violation)
RouteService service = new RouteServiceImpl(); // NEVER DO THIS IN CLIENT
service.addRoute(newRoute);
```

## 5. Threading (JavaFX Application Thread)

- **Network Calls:** If a socket request takes a long time, consider running it on a background thread (`Task<T>`) to prevent freezing the UI.
- **UI Updates:** Any updates to JavaFX components MUST be done on the JavaFX Application Thread. If you are returning from a background network thread, use `Platform.runLater(() -> { ... })`.
