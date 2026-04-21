# Socket Server

## Thread Pool

Every client connection must run on its own thread from a pool — never on the accept loop thread.

```java
ExecutorService threadPool = Executors.newCachedThreadPool();

try (ServerSocket serverSocket = new ServerSocket(PORT)) {
    while (true) {
        Socket clientSocket = serverSocket.accept();
        threadPool.submit(() -> handleClient(clientSocket));
    }
}
```

- Use `newCachedThreadPool()` for variable load (creates threads on demand, reuses idle ones)
- Use `newFixedThreadPool(N)` if you need to cap concurrent connections
- Never call `handleClient()` directly on the accept loop — it blocks the next connection

## Connection Timeout

Always set timeouts — a client that hangs silently will hold a thread forever.

```java
clientSocket.setSoTimeout(30_000); // 30s read timeout
```

If `SocketTimeoutException` is caught during read → log it, close the socket, release the thread.

## Heartbeat / Keepalive

For long-lived connections, enable TCP keepalive so dead connections are detected by the OS:

```java
clientSocket.setKeepAlive(true);
```

If the application needs application-level heartbeat (e.g. ping every 15s), define a dedicated `ActionType.PING` that returns `Response.success("pong", null)`.

## Client Disconnect Handling

A client that disconnects mid-stream throws `EOFException` or `SocketException`. Catch these specifically — they are normal events, not bugs.

```java
private void handleClient(Socket clientSocket) {
    try (
        ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
        ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())
    ) {
        while (true) {
            Request request = (Request) in.readObject();
            Response response = router.route(request);
            out.writeObject(response);
            out.reset(); // prevent ObjectOutputStream caching stale references
        }
    } catch (EOFException | SocketException e) {
        // client disconnected — normal, no stack trace needed
        log.info("Client disconnected: {}", clientSocket.getRemoteSocketAddress());
    } catch (Exception e) {
        log.error("Unexpected error handling client", e);
    }
}
```

**Always call `out.reset()`** after each write — otherwise `ObjectOutputStream` caches object references and the client receives stale data on repeated writes of the same object.

## Graceful Shutdown

```java
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    threadPool.shutdown();
    try {
        threadPool.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
        threadPool.shutdownNow();
    }
}));
```

## Port

Default server port should be defined as a constant, not hardcoded inline:

```java
public static final int SERVER_PORT = 9090;
```
