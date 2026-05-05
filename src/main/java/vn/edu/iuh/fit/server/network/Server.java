package vn.edu.iuh.fit.server.network;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import vn.edu.iuh.fit.common.request.Request;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.util.JPAUtils;

public class Server {

    public static final int SERVER_PORT = 9090;
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private final ExecutorService threadPool = Executors.newFixedThreadPool(100);
    private final RequestRouter router = new RequestRouter();

    public void start() {
        registerShutdownHook();
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
        try {
            clientSocket.setSoTimeout(30_000);
            clientSocket.setKeepAlive(true);
        } catch (Exception e) {
            log.error("Failed to configure socket: {}", clientSocket.getRemoteSocketAddress(), e);
            return;
        }
        try (
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
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

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            threadPool.shutdown();
            try {
                threadPool.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
            }
        }));
    }

    public static void main(String[] args) {
        try {
            JPAUtils.getFactory();
            new Server().start();
        } catch (Throwable e) {
            log.error("Server startup failed. Check database connection in persistence.xml and ensure MariaDB is running on localhost:3307 before starting the socket server on port {}.", SERVER_PORT, e);
        }
    }
}
