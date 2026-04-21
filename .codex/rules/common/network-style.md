# Network Layer Code Style

The network layer receives `Request` from the socket, routes by `ActionType`, and returns `Response`. It contains no business logic.

## Structure

```
server/network/
├── Server.java          # ServerSocket accept loop + thread pool
└── RequestRouter.java   # switch on ActionType → delegates to service
```

`Server.java` owns the socket lifecycle. `RequestRouter.java` owns the routing logic. Keep them separate.

## Server.java Pattern

```java
public class Server {
    public static final int SERVER_PORT = 9090;
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final RequestRouter router = new RequestRouter();

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            log.info("Server started on port {}", SERVER_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            log.error("Server failed to start", e);
        }
    }

    private void handleClient(Socket clientSocket) {
        log.info("Client connected: {}", clientSocket.getRemoteSocketAddress());
        try (
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in  = new ObjectInputStream(clientSocket.getInputStream())
        ) {
            clientSocket.setSoTimeout(30_000);
            while (true) {
                Request request = (Request) in.readObject();
                log.debug("Received action: {}", request.getAction());
                Response response = router.route(request);
                out.writeObject(response);
                out.reset();
            }
        } catch (EOFException | SocketException e) {
            log.info("Client disconnected: {}", clientSocket.getRemoteSocketAddress());
        } catch (SocketTimeoutException e) {
            log.warn("Client timed out: {}", clientSocket.getRemoteSocketAddress());
        } catch (Exception e) {
            log.error("Error handling client: {}", clientSocket.getRemoteSocketAddress(), e);
        }
    }
}
```

## RequestRouter.java Pattern

```java
public class RequestRouter {
    private final TicketService ticketService = new TicketService();
    private final CustomerService customerService = new CustomerService();
    // ... other services

    public Response route(Request request) {
        if (request == null || request.getAction() == null) {
            return Response.error("Invalid request");
        }
        return switch (request.getAction()) {
            case CREATE_TICKET      -> ticketService.createTicket(castData(request, TicketDTO.class));
            case FIND_TICKET_BY_ID  -> ticketService.findTicketById(castData(request, String.class));
            case FIND_ALL_TICKETS   -> ticketService.findAllTickets();
            case LOGIN              -> customerService.login(castData(request, LoginRequestDTO.class));
            default                 -> Response.error("Unknown action: " + request.getAction());
        };
    }

    private <T> T castData(Request request, Class<T> type) {
        try {
            return type.cast(request.getData());
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(
                "Expected " + type.getSimpleName() + " for action " + request.getAction()
            );
        }
    }
}
```

## Rules

- **No business logic** in the network layer — routing only. If you find yourself writing an `if` for a business rule here, move it to the service.
- **Always use `castData()` helper** — never cast `request.getData()` inline without a try/catch
- **Always call `out.reset()`** after every `out.writeObject()` — prevents stale object references
- **Always set `setSoTimeout(30_000)`** on the client socket — no silent hangs
- **Group `ActionType` cases by entity** in the switch — keeps the router readable as it grows
- **Never instantiate a service inside `route()`** — services are fields, initialized once
- **Default case must always exist** in the switch — return `Response.error("Unknown action")`
- Log every incoming `ActionType` at `DEBUG`, every connect/disconnect at `INFO`
