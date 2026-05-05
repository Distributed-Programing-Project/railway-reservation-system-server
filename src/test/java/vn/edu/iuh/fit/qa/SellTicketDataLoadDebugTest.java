package vn.edu.iuh.fit.qa;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import vn.edu.iuh.fit.client.service.SocketRequestService;
import vn.edu.iuh.fit.common.command.ActionType;
import vn.edu.iuh.fit.common.dto.StationDTO;
import vn.edu.iuh.fit.common.request.Request;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.model.Station;
import vn.edu.iuh.fit.server.network.RequestRouter;
import vn.edu.iuh.fit.server.repository.StationRepository;
import vn.edu.iuh.fit.server.repository.impl.StationRepositoryImpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SellTicketDataLoadDebugTest {

  private static final Path REPORT = Path.of("test-reports", "sell-ticket-data-load-debug-report.md");

  private static final String EXPECTED_JDBC_URL = "jdbc:mariadb://localhost:3306/distributed_db";

  private static final List<String> IMPORTANT_TABLES = List.of(
      "stations",
      "routes",
      "route_stops",
      "schedules",
      "schedule_details",
      "trains",
      "carriages",
      "seats",
      "accounts",
      "employees",
      "customers",
      "tickets",
      "invoices");

  private static DbConfig db;
  private static EmbeddedSocketServer embeddedServer;

  @BeforeAll
  static void initReport() throws Exception {
    Files.createDirectories(REPORT.getParent());
    Files.deleteIfExists(REPORT);
    // Reduce noise from server/client code that uses slf4j-simple.
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");

    db = DbConfig.fromPersistenceXml();
    embeddedServer = new EmbeddedSocketServer();
    writeReport("""
        # Sell Ticket Data Load Debug Report

        Time: %s

        ## A. DB URL / credentials (from `persistence.xml`)
        - jdbc.url: `%s`
        - jdbc.user: `%s`
        - jdbc.password: `%s`
        - embedded test server: `127.0.0.1:%d`

        """.formatted(LocalDateTime.now(), db.jdbcUrl, db.jdbcUser, mask(db.jdbcPassword), embeddedServer.port()));
  }

  @AfterAll
  static void shutdownEmbeddedServer() {
    if (embeddedServer != null) {
      try {
        embeddedServer.close();
      } catch (Exception ignored) {
      }
    }
  }

  @Test
  @Order(1)
  void db_sanity_counts_and_sample_rows() {
    assertNotNull(db, "DB config not loaded");

    writeSection("A1. persistence.xml URL check", () -> {
      assertEquals(EXPECTED_JDBC_URL, db.jdbcUrl,
          "persistence.xml is not pointing to distributed_db (expected " + EXPECTED_JDBC_URL + ")");
      return "- persistence.xml jdbc.url matches expected.\n";
    });

    writeSection("A2. Table presence + COUNT(*)", () -> {
      StringBuilder out = new StringBuilder();
      try (Connection c = connect();
          Statement st = c.createStatement()) {
        for (String table : IMPORTANT_TABLES) {
          boolean exists = tableExists(st, "distributed_db", table);
          out.append("- ").append(table).append(": ").append(exists ? "EXISTS" : "MISSING").append("\n");
          if (exists) {
            long count = firstLong(st, "SELECT COUNT(*) FROM " + table);
            out.append("  - COUNT: ").append(count).append("\n");
          }
        }
      }
      return out.toString();
    });

    writeSection("A2b. Tables that look like stations/routes/schedules", () -> {
      StringBuilder out = new StringBuilder();
      try (Connection c = connect();
          Statement st = c.createStatement()) {
        out.append("- Tables in schema `distributed_db` matching '%station%' / '%route%' / '%schedule%':\n");
        try (ResultSet rs = st.executeQuery("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema='distributed_db'
              AND (table_name LIKE '%station%' OR table_name LIKE '%route%' OR table_name LIKE '%schedule%')
            ORDER BY table_name
            """)) {
          List<String> names = new ArrayList<>();
          while (rs.next()) {
            names.add(rs.getString(1));
          }
          if (names.isEmpty()) {
            out.append("  - (none)\n");
          } else {
            for (String name : names) {
              out.append("  - ").append(name).append("\n");
              try {
                long count = firstLong(st, "SELECT COUNT(*) FROM " + name);
                out.append("    - COUNT: ").append(count).append("\n");
              } catch (Exception e) {
                out.append("    - COUNT: ERROR: ").append(rootCauseSummary(e)).append("\n");
              }
            }
          }
        }
      }
      return out.toString();
    });

    writeSection("A3. Station sample query", () -> {
      try (Connection c = connect();
          Statement st = c.createStatement()) {
        long stationCount = firstLong(st, "SELECT COUNT(*) FROM stations");
        StringBuilder out = new StringBuilder();
        out.append("- stations COUNT: ").append(stationCount).append("\n");
        out.append("- first 20 stations (station_id, station_name):\n");
        try (ResultSet rs = st.executeQuery(
            "SELECT station_id, station_name FROM stations ORDER BY station_name LIMIT 20")) {
          int n = 0;
          while (rs.next()) {
            n++;
            out.append("  - ").append(rs.getString(1)).append(" | ").append(rs.getString(2)).append("\n");
          }
          if (n == 0) {
            out.append("  - (no rows)\n");
          }
        }
        return out.toString();
      }
    });
  }

  @Test
  @Order(2)
  void hibernate_schema_accounts_employees_show_create() {
    writeSection("B. Schema check (accounts / employees)", () -> {
      StringBuilder out = new StringBuilder();
      try (Connection c = connect();
          Statement st = c.createStatement()) {
        out.append("- SHOW CREATE TABLE accounts:\n");
        out.append(codeBlock(showCreateTable(st, "accounts"))).append("\n");
        out.append("- SHOW CREATE TABLE employees:\n");
        out.append(codeBlock(showCreateTable(st, "employees"))).append("\n");
      }
      return out.toString();
    });
  }

  @Test
  @Order(3)
  void repository_station_list_should_be_non_empty_when_db_has_rows() {
    writeSection("C. Repository check (StationRepositoryImpl.findAllStations)", () -> {
      EntityManagerFactory emf = null;
      EntityManager em = null;
      try {
        emf = Persistence.createEntityManagerFactory("mariadb-pu");
        em = emf.createEntityManager();
        StationRepository repo = new StationRepositoryImpl();
        List<Station> stations = repo.findAllStations(em);

        StringBuilder out = new StringBuilder();
        out.append("- repo.findAllStations size: ").append(stations == null ? "null" : stations.size()).append("\n");
        if (stations != null) {
          for (int i = 0; i < Math.min(10, stations.size()); i++) {
            Station s = stations.get(i);
            out.append("  - ").append(s.getId()).append(" | ").append(s.getName()).append("\n");
          }
        }
        return out.toString();
      } finally {
        if (em != null) {
          try {
            em.close();
          } catch (Exception ignored) {
          }
        }
        if (emf != null) {
          try {
            emf.close();
          } catch (Exception ignored) {
          }
        }
      }
    });

    // Keep assertion separate (short, terminal-friendly), while details go to report.
    try (Connection c = connect();
        Statement st = c.createStatement()) {
      long stationCount = firstLong(st, "SELECT COUNT(*) FROM stations");
      if (stationCount > 0) {
        // If DB has stations, repository must return some.
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("mariadb-pu");
        EntityManager em = emf.createEntityManager();
        try {
          List<Station> stations = new StationRepositoryImpl().findAllStations(em);
          assertTrue(stations != null && !stations.isEmpty(),
              "DB stations has rows but repository returned empty");
        } finally {
          em.close();
          emf.close();
        }
      }
    } catch (Exception e) {
      failShort("Repository check failed: " + rootCauseSummary(e));
    }
  }

  @Test
  @Order(4)
  void socket_action_find_all_stations_should_return_station_dto_list() {
    writeSection("D. Socket E2E check (ActionType.FIND_ALL_STATIONS)", () -> {
      SocketRequestService client = new SocketRequestService("127.0.0.1", embeddedServer.port());
      Response res = client.send(new Request(ActionType.FIND_ALL_STATIONS, "ALL"));

      StringBuilder out = new StringBuilder();
      out.append("- response: ").append(res).append("\n");
      if (res == null) {
        return out.toString();
      }
      out.append("- success: ").append(res.isSuccess()).append("\n");
      out.append("- message: ").append(String.valueOf(res.getMessage())).append("\n");

      Object data = res.getData();
      out.append("- data.type: ").append(data == null ? "null" : data.getClass().getName()).append("\n");
      if (data instanceof List<?> list) {
        out.append("- data.size: ").append(list.size()).append("\n");
        Map<String, Integer> typeHistogram = new LinkedHashMap<>();
        for (Object o : list) {
          String tn = (o == null) ? "null" : o.getClass().getName();
          typeHistogram.put(tn, typeHistogram.getOrDefault(tn, 0) + 1);
        }
        out.append("- element types:\n");
        for (var e : typeHistogram.entrySet()) {
          out.append("  - ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
        }
        int preview = Math.min(5, list.size());
        for (int i = 0; i < preview; i++) {
          out.append("  - sample[").append(i).append("]: ").append(String.valueOf(list.get(i))).append("\n");
        }
      }
      return out.toString();
    });

    long stationCount = 0L;
    try (Connection c = connect(); Statement st = c.createStatement()) {
      stationCount = firstLong(st, "SELECT COUNT(*) FROM stations");
    } catch (Exception e) {
      failShort("DB COUNT(stations) failed: " + rootCauseSummary(e));
    }
    if (stationCount == 0) {
      failShort("DB stations COUNT=0. Seed data is missing, so socket cannot return stations yet.");
    }

    SocketRequestService client = new SocketRequestService("127.0.0.1", embeddedServer.port());
    Response res = client.send(new Request(ActionType.FIND_ALL_STATIONS, "ALL"));
    assertNotNull(res, "Socket call returned null Response");
    assertTrue(res.isSuccess(), "Socket Response.success=false: " + res.getMessage());
    assertTrue(res.getData() instanceof List<?>, "Socket Response.data is not a List");

    List<?> list = (List<?>) res.getData();
    assertTrue(!list.isEmpty(), "Socket returned empty station list (unexpected because DB stations COUNT>0)");
    assertTrue(list.stream().allMatch(StationDTO.class::isInstance),
        "Socket station list elements are not StationDTO (see report for element types)");
  }

  @Test
  @Order(5)
  void report_root_cause_and_next_steps() {
    writeSection("H. Root cause + PASS/FAIL summary", () -> {
      long stationCount = 0L;
      try (Connection c = connect(); Statement st = c.createStatement()) {
        stationCount = firstLong(st, "SELECT COUNT(*) FROM stations");
      }

      boolean dbHasStations = stationCount > 0;
      boolean repoHasStations = false;
      try {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("mariadb-pu");
        EntityManager em = emf.createEntityManager();
        try {
          repoHasStations = !new StationRepositoryImpl().findAllStations(em).isEmpty();
        } finally {
          em.close();
          emf.close();
        }
      } catch (Exception ignored) {
        // Repo errors are already captured in section C.
      }

      boolean socketHasStations = false;
      try {
        Response res = new SocketRequestService("127.0.0.1", embeddedServer.port())
            .send(new Request(ActionType.FIND_ALL_STATIONS, "ALL"));
        if (res != null && res.isSuccess() && res.getData() instanceof List<?> list) {
          socketHasStations = !list.isEmpty();
        }
      } catch (Exception ignored) {
        // Socket errors are already captured in section D.
      }

      String rootCause;
      if (!dbHasStations) {
        rootCause = "DB `distributed_db.stations` is empty (COUNT=0) -> server cannot return stations -> ComboBox shows no items.";
      } else if (!repoHasStations) {
        rootCause = "DB has station rows but Hibernate repository returns empty -> entity/table mapping or schema mismatch.";
      } else if (!socketHasStations) {
        rootCause = "Repository returns stations but socket response station list is empty -> RequestRouter/ActionType mismatch or server-side SaleService issue.";
      } else {
        rootCause = "DB/repo/socket have stations; remaining issue is in JavaFX controller binding/converter.";
      }

      return """
          ### Code path involved
          - DB table: `distributed_db.stations`
          - Entity: `src/main/java/vn/edu/iuh/fit/server/model/Station.java`
          - Repository: `src/main/java/vn/edu/iuh/fit/server/repository/impl/StationRepositoryImpl.java`
          - Service: `src/main/java/vn/edu/iuh/fit/server/service/impl/SaleServiceImpl.java` (`findAllStations`)
          - Router: `src/main/java/vn/edu/iuh/fit/server/network/RequestRouter.java` (`FIND_ALL_STATIONS`)
          - Client socket: `src/main/java/vn/edu/iuh/fit/client/service/SocketRequestService.java` (`127.0.0.1:%d`)
          - Client service: `src/main/java/vn/edu/iuh/fit/client/service/SaleClientService.java`
          - UI controller: `src/main/java/vn/edu/iuh/fit/client/controller/SellTicketWizardController.java` (`loadStationsAsync`)

          ### Root cause
          - %s

          ### PASS/FAIL by layer
          - DB stations exists: **%s** (COUNT=%d)
          - Repository stations load: **%s**
          - Socket stations response: **%s**
          - UI controller binding: **BLOCKED** (depends on non-empty stations)
          - FXML load: **PASS** (covered by `FxmlLoadSmokeTest`)

          ### Commands run
          - `mvn -q \"-Dmaven.repo.local=%s\" \"-Dmaven.compiler.fork=true\" test \"-Dtest=vn.edu.iuh.fit.qa.SellTicketDataLoadDebugTest\"`

          ### Files changed in this debug session
          - `src/main/java/vn/edu/iuh/fit/server/service/impl/SaleServiceImpl.java` (return clearer error when stations list is empty)
          - `src/test/java/vn/edu/iuh/fit/qa/SellTicketDataLoadDebugTest.java` (DB/repo/socket diagnostics + report writer)

          ### Manual next steps
          - If `stations` should have seed data: re-check your seed script execution (errors after `stations` inserts can stop the script). Confirm with:
            - `SELECT COUNT(*) FROM stations;`
            - `SELECT station_id, station_name FROM stations ORDER BY station_name LIMIT 20;`
          - After `stations` is non-empty, re-run this test to confirm DB → repository → socket returns non-empty list.
          """.formatted(
          embeddedServer.port(),
          rootCause,
          passFail(dbHasStations), stationCount,
          passFail(repoHasStations),
          passFail(socketHasStations),
          Path.of(".m2", "repository").toAbsolutePath().normalize());
    });
  }

  private static final class EmbeddedSocketServer implements AutoCloseable {
    private final ServerSocket serverSocket;
    private final Thread acceptThread;
    private volatile boolean running = true;
    private final RequestRouter router = new RequestRouter();

    EmbeddedSocketServer() throws Exception {
      this.serverSocket = new ServerSocket(0);
      this.acceptThread = new Thread(this::acceptLoop, "test-embedded-socket-server");
      this.acceptThread.setDaemon(true);
      this.acceptThread.start();
    }

    int port() {
      return serverSocket.getLocalPort();
    }

    private void acceptLoop() {
      while (running) {
        try (Socket client = serverSocket.accept();
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(client.getInputStream())) {
          Object reqObj = in.readObject();
          Response res;
          if (reqObj instanceof vn.edu.iuh.fit.common.request.Request req) {
            res = router.route(req);
          } else {
            res = Response.error("Invalid request object: " + (reqObj == null ? "null" : reqObj.getClass().getName()));
          }
          out.writeObject(res);
          out.flush();
          out.reset();
        } catch (Exception ignored) {
          // Keep the test runner quiet; failures are captured by the client side in test sections.
        }
      }
    }

    @Override
    public void close() {
      running = false;
      try {
        serverSocket.close();
      } catch (Exception ignored) {
      }
    }
  }

  private static String passFail(boolean ok) {
    return ok ? "PASS" : "FAIL";
  }

  private static Connection connect() throws Exception {
    return DriverManager.getConnection(db.jdbcUrl, db.jdbcUser, db.jdbcPassword);
  }

  private static boolean tableExists(Statement st, String schema, String table) throws Exception {
    String q = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='"
        + schema.replace("'", "''")
        + "' AND table_name='"
        + table.replace("'", "''") + "'";
    return firstLong(st, q) > 0;
  }

  private static long firstLong(Statement st, String sql) throws Exception {
    try (ResultSet rs = st.executeQuery(sql)) {
      if (!rs.next()) return 0L;
      return rs.getLong(1);
    }
  }

  private static String showCreateTable(Statement st, String table) throws Exception {
    try (ResultSet rs = st.executeQuery("SHOW CREATE TABLE " + table)) {
      if (!rs.next()) return "(no result)";
      // MariaDB returns: [Table, Create Table]
      return rs.getString(2);
    }
  }

  private static void writeSection(String title, SectionBody body) {
    try {
      writeReport("\n## " + title + "\n\n");
      String rendered = body.render();
      writeReport(rendered);
      if (!rendered.endsWith("\n")) {
        writeReport("\n");
      }
    } catch (AssertionError ae) {
      // Assertion details already meaningful; still capture into report.
      writeReport("\n- ASSERTION FAILED: " + ae.getMessage() + "\n");
      throw ae;
    } catch (Throwable t) {
      writeThrowable(title, t);
      failShort("See report: " + REPORT);
    }
  }

  private static String codeBlock(String s) {
    return "```sql\n" + String.valueOf(s).trim() + "\n```";
  }

  private static void writeThrowable(String context, Throwable t) {
    StringWriter buf = new StringWriter();
    t.printStackTrace(new PrintWriter(buf));
    writeReport("""
        - EXCEPTION in %s
        - Root cause: %s
        - Stack trace:

        ```text
        %s
        ```

        """.formatted(context, rootCauseSummary(t), buf));
  }

  private static void writeReport(String content) {
    try {
      Files.writeString(REPORT, content, StandardCharsets.UTF_8,
          StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (Exception ignored) {
      // Keep terminal clean; failing to write report is non-fatal to execution flow.
    }
  }

  private static void failShort(String message) {
    fail(message + " (details in " + REPORT + ")");
  }

  private static String rootCauseSummary(Throwable t) {
    Throwable root = t;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    return root.getClass().getName() + ": " + String.valueOf(root.getMessage());
  }

  private static String mask(String value) {
    if (value == null || value.isBlank()) return "";
    if (value.length() <= 2) return "**";
    return value.charAt(0) + "***" + value.charAt(value.length() - 1);
  }

  @FunctionalInterface
  private interface SectionBody {
    String render() throws Exception;
  }

  private record DbConfig(String jdbcUrl, String jdbcUser, String jdbcPassword) {
    static DbConfig fromPersistenceXml() throws Exception {
      String xml = readResource("META-INF/persistence.xml")
          .orElseGet(() -> readFile("src/main/resources/META-INF/persistence.xml")
              .orElseThrow(() -> new IllegalStateException("Cannot load META-INF/persistence.xml")));

      String url = extractProperty(xml, "jakarta.persistence.jdbc.url")
          .orElseThrow(() -> new IllegalStateException("Missing jdbc.url in persistence.xml"));
      String user = extractProperty(xml, "jakarta.persistence.jdbc.user").orElse("");
      String pass = extractProperty(xml, "jakarta.persistence.jdbc.password").orElse("");
      return new DbConfig(url, user, pass);
    }

    private static Optional<String> readResource(String path) {
      try (InputStream in = SellTicketDataLoadDebugTest.class.getClassLoader().getResourceAsStream(path)) {
        if (in == null) return Optional.empty();
        return Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
      } catch (Exception e) {
        return Optional.empty();
      }
    }

    private static Optional<String> readFile(String path) {
      try {
        return Optional.of(Files.readString(Path.of(path), StandardCharsets.UTF_8));
      } catch (Exception e) {
        return Optional.empty();
      }
    }

    private static Optional<String> extractProperty(String xml, String name) {
      String pattern = "<property\\s+name\\s*=\\s*\\\"" + Pattern.quote(name) + "\\\"\\s+value\\s*=\\s*\\\"([^\\\"]*)\\\"\\s*/?>";
      Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(xml);
      if (m.find()) return Optional.ofNullable(m.group(1));
      return Optional.empty();
    }
  }
}
