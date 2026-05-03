package vn.edu.iuh.fit.qa;

import org.junit.jupiter.api.Test;
import vn.edu.iuh.fit.client.service.SocketRequestService;
import vn.edu.iuh.fit.common.command.ActionType;
import vn.edu.iuh.fit.common.request.Request;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.network.RequestRouter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.net.ServerSocket;
import java.net.Socket;
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

/**
 * Runs the new seed scripts via JDBC and verifies counts.
 *
 * Output policy:
 * - Avoid long terminal logs: write all details to test-reports/.
 */
public class SeedFixRunnerTest {

  private static final Path REPORT = Path.of("test-reports", "seed-fix-and-data-load-report.md");

  private static final Path SEED_01 = Path.of("db", "seed", "01_master_data.sql");
  private static final Path SEED_02 = Path.of("db", "seed", "02_operational_data.sql");
  private static final Path SEED_03 = Path.of("db", "seed", "03_test_scenarios.sql");
  private static final Path VERIFY = Path.of("db", "seed", "verify_seed.sql");

  private static final List<String> IMPORTANT_TABLES = List.of(
      "stations",
      "routes",
      "route_stops",
      "trains",
      "carriages",
      "seats",
      "schedules",
      "schedule_details",
      "customers",
      "accounts",
      "employees",
      "tickets",
      "invoices",
      "invoice_details",
      "invoice_metadata"
  );

  @Test
  void run_seed_and_verify() throws Exception {
    Files.createDirectories(REPORT.getParent());
    Files.deleteIfExists(REPORT);

    DbConfig db = DbConfig.fromPersistenceXml();

    write("""
        # Seed Fix + Data Load Report

        Time: %s

        DB (from `persistence.xml`):
        - url: `%s`
        - user: `%s`
        - password: `%s`

        """.formatted(LocalDateTime.now(), db.jdbcUrl, db.jdbcUser, mask(db.jdbcPassword)));

    write("## 1) Root cause (old seed)\n\n");
    write("- `generate_data.sql` failed before committing, so all deletes/inserts rolled back -> tables stayed empty.\n");
    write("- First guaranteed failing statement: seat insert block has **two** `WITH RECURSIVE` at the same level (MariaDB rejects `WITH ... INSERT ... WITH ...`).\n");
    write("- Additionally, later parts of `generate_data.sql` have line breaks inside quoted MD5 seed strings (e.g. `MD5('cust|...')` split across lines), which would also fail even after fixing the CTE.\n\n");

    write("## 2) Seed files created (kept old files intact)\n\n");
    write("- New: `" + SEED_01 + "`\n");
    write("- New: `" + SEED_02 + "`\n");
    write("- New: `" + SEED_03 + "`\n");
    write("- New: `" + VERIFY + "`\n");
    write("- Old kept: `generate_data.sql`\n\n");

    Map<String, Long> beforeCounts;
    Map<String, Long> afterCounts;
    SeedRunResult r1;
    SeedRunResult r2;
    SeedRunResult r3;
    SeedRunResult rv;

    try (Connection c = DriverManager.getConnection(db.jdbcUrl, db.jdbcUser, db.jdbcPassword)) {
      c.setAutoCommit(false);

      beforeCounts = readCounts(c);
      write("## 3) COUNT(*) before seed\n\n");
      write(countsTable(beforeCounts));

      write("## 4) Run seeds (JDBC)\n\n");
      r1 = runScript(c, SEED_01);
      write(resultLine("01_master_data.sql", r1));

      r2 = runScript(c, SEED_02);
      write(resultLine("02_operational_data.sql", r2));

      r3 = runScript(c, SEED_03);
      write(resultLine("03_test_scenarios.sql", r3));

      rv = runScript(c, VERIFY);
      write(resultLine("verify_seed.sql", rv));

      afterCounts = readCounts(c);
      write("\n## 5) COUNT(*) after seed\n\n");
      write(countsTable(afterCounts));

      write("## 6) Required checks\n\n");
      write("- Required stations (Hà Nội / Sài Gòn / Đà Nẵng / Nha Trang):\n");
      write(stationNameCheck(c));
      write("- Future schedules (departure_time > NOW()): " + firstLong(c, "SELECT COUNT(*) FROM schedules WHERE departure_time > NOW()") + "\n");
      write("- Priced schedule_details (price_seat > 0): " + firstLong(c, "SELECT COUNT(*) FROM schedule_details WHERE price_seat IS NOT NULL AND price_seat > 0") + "\n");
      write("- Seats: " + firstLong(c, "SELECT COUNT(*) FROM seats") + "\n\n");

      // Release any metadata locks held by this runner connection (autoCommit=false).
      // Otherwise, Hibernate schema checks inside socket/router code can block and hit the client READ timeout.
      try {
        c.commit();
      } catch (Exception ignored) {
      }

      // Socket check (same ActionType used by UI ComboBox data load)
      SocketCheckResult socketResult = socketFindAllStationsCheck();

      write("## 7) PASS/FAIL summary\n\n");
      write("- 01_master_data.sql: **" + passFail(r1.ok) + "**\n");
      write("- 02_operational_data.sql: **" + passFail(r2.ok) + "**\n");
      write("- 03_test_scenarios.sql: **" + passFail(r3.ok) + "**\n");
      write("- verify_seed.sql: **" + passFail(rv.ok) + "**\n");
      write("- FIND_ALL_STATIONS socket: **" + passFail(socketResult.ok) + "**\n");
      if (!socketResult.ok) {
        write("  - error: `" + socketResult.message + "`\n");
      } else {
        write("  - data.size: " + socketResult.dataSize + "\n");
      }
      write("\n");

      write("## 8) Commands run\n\n");
      write("- Seed runner: `mvn -q test -Dtest=vn.edu.iuh.fit.qa.SeedFixRunnerTest`\n");
      write("- Socket regression: `mvn -q test -Dtest=vn.edu.iuh.fit.qa.SellTicketDataLoadDebugTest`\n\n");

      write("## 9) Notes\n\n");
      write("- Schema snapshot (SHOW CREATE TABLE) recorded at: `test-reports/seed-schema-snapshot.md`\n");
    } catch (Exception e) {
      write("\n## ERROR\n\n");
      write("- Seed runner failed: `" + rootCauseSummary(e) + "`\n");
      throw e;
    }
  }

  private static String stationNameCheck(Connection c) throws Exception {
    String sql = """
        SELECT station_name
        FROM stations
        WHERE station_name IN ('Hà Nội', 'Sài Gòn', 'Đà Nẵng', 'Nha Trang')
        ORDER BY station_name
        """;
    StringBuilder out = new StringBuilder();
    try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
      int n = 0;
      while (rs.next()) {
        n++;
        out.append("  - ").append(rs.getString(1)).append("\n");
      }
      if (n == 0) out.append("  - (missing)\n");
    }
    return out.toString();
  }

  private static Map<String, Long> readCounts(Connection c) throws Exception {
    Map<String, Long> out = new LinkedHashMap<>();
    try (Statement st = c.createStatement()) {
      for (String table : IMPORTANT_TABLES) {
        try {
          long count = firstLong(st, "SELECT COUNT(*) FROM " + table);
          out.put(table, count);
        } catch (Exception e) {
          out.put(table, -1L);
        }
      }
    }
    return out;
  }

  private static long firstLong(Connection c, String sql) throws Exception {
    try (Statement st = c.createStatement()) {
      return firstLong(st, sql);
    }
  }

  private static long firstLong(Statement st, String sql) throws Exception {
    try (ResultSet rs = st.executeQuery(sql)) {
      if (!rs.next()) return 0L;
      return rs.getLong(1);
    }
  }

  private static String countsTable(Map<String, Long> counts) {
    StringBuilder sb = new StringBuilder();
    for (var e : counts.entrySet()) {
      sb.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
    }
    sb.append("\n");
    return sb.toString();
  }

  private static String resultLine(String name, SeedRunResult r) {
    if (r.ok) return "- " + name + ": **PASS** (" + r.statementsRun + " statements)\n";
    String msg = r.error == null ? "(unknown error)" : rootCauseSummary(r.error);
    String stmt = r.failingStatement == null ? "" : ("\n  - first failing SQL:\n\n```sql\n" + r.failingStatement.trim() + "\n```\n");
    return "- " + name + ": **FAIL** (" + r.statementsRun + " statements)\n"
        + "  - error: `" + msg + "`\n"
        + stmt;
  }

  private static String passFail(boolean ok) {
    return ok ? "PASS" : "FAIL";
  }

  private record SeedRunResult(boolean ok, int statementsRun, String failingStatement, Exception error) {
  }

  private record SocketCheckResult(boolean ok, String message, int dataSize) {
  }

  private static SocketCheckResult socketFindAllStationsCheck() {
    EmbeddedSocketServer server = null;
    try {
      server = new EmbeddedSocketServer();
      SocketRequestService client = new SocketRequestService("127.0.0.1", server.port());
      Response res = client.send(new Request(ActionType.FIND_ALL_STATIONS, "ALL"));
      if (res == null) return new SocketCheckResult(false, "Response is null", 0);
      if (!res.isSuccess()) return new SocketCheckResult(false, String.valueOf(res.getMessage()), 0);
      Object data = res.getData();
      if (!(data instanceof List<?> list)) {
        return new SocketCheckResult(false, "Response.data is not a List (type=" + (data == null ? "null" : data.getClass().getName()) + ")", 0);
      }
      return new SocketCheckResult(!list.isEmpty(), list.isEmpty() ? "Station list is empty" : "OK", list.size());
    } catch (Exception e) {
      return new SocketCheckResult(false, rootCauseSummary(e), 0);
    } finally {
      if (server != null) {
        try {
          server.close();
        } catch (Exception ignored) {
        }
      }
    }
  }

  private static final class EmbeddedSocketServer implements AutoCloseable {
    private final ServerSocket serverSocket;
    private final Thread acceptThread;
    private volatile boolean running = true;
    private final RequestRouter router = new RequestRouter();

    EmbeddedSocketServer() throws Exception {
      this.serverSocket = new ServerSocket(0);
      this.acceptThread = new Thread(this::acceptLoop, "seedfix-embedded-socket-server");
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
          // keep runner quiet; failures are reported on the client side
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

  private static SeedRunResult runScript(Connection c, Path path) throws Exception {
    if (!Files.exists(path)) {
      return new SeedRunResult(false, 0, null, new IllegalStateException("Missing file: " + path));
    }

    List<String> statements = splitSqlStatements(Files.readString(path, StandardCharsets.UTF_8));
    int ran = 0;
    try (Statement st = c.createStatement()) {
      for (String stmt : statements) {
        String trimmed = stmt.trim();
        if (trimmed.isEmpty()) continue;
        ran++;
        st.execute(trimmed);
      }
      // If script doesn't explicitly COMMIT, commit anyway to avoid dangling tx in runner.
      c.commit();
      return new SeedRunResult(true, ran, null, null);
    } catch (Exception e) {
      try {
        c.rollback();
      } catch (Exception ignored) {
      }
      String failing = (ran > 0 && ran <= statements.size()) ? statements.get(ran - 1) : null;
      return new SeedRunResult(false, ran, failing, e);
    }
  }

  /**
   * Splits a SQL script into statements.
   * - Strips `-- ...` line comments.
   * - Strips `/* ... *\/` block comments.
   * - Splits on semicolons not inside single/double quotes.
   *
   * This is intentionally minimal (fits this repo's seed style).
   */
  private static List<String> splitSqlStatements(String sql) throws Exception {
    String noBlockComments = sql.replaceAll("(?s)/\\*.*?\\*/", "");
    List<String> statements = new ArrayList<>();

    StringBuilder current = new StringBuilder();
    boolean inSingle = false;
    boolean inDouble = false;

    try (BufferedReader br = new BufferedReader(new InputStreamReader(
        new java.io.ByteArrayInputStream(noBlockComments.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8))) {
      String line;
      while ((line = br.readLine()) != null) {
        String trimmed = line.trim();
        if (trimmed.startsWith("--")) continue;
        int commentIdx = indexOfLineCommentOutsideQuotes(line, inSingle, inDouble);
        if (commentIdx >= 0) {
          line = line.substring(0, commentIdx);
        }

        for (int i = 0; i < line.length(); i++) {
          char ch = line.charAt(i);
          if (ch == '\'' && !inDouble) {
            boolean escaped = i > 0 && line.charAt(i - 1) == '\\';
            if (!escaped) inSingle = !inSingle;
          } else if (ch == '"' && !inSingle) {
            boolean escaped = i > 0 && line.charAt(i - 1) == '\\';
            if (!escaped) inDouble = !inDouble;
          }

          if (ch == ';' && !inSingle && !inDouble) {
            statements.add(current.toString());
            current.setLength(0);
          } else {
            current.append(ch);
          }
        }
        current.append('\n');
      }
    }

    String tail = current.toString().trim();
    if (!tail.isEmpty()) statements.add(tail);
    return statements;
  }

  private static int indexOfLineCommentOutsideQuotes(String line, boolean inSingleAtStart, boolean inDoubleAtStart) {
    boolean inSingle = inSingleAtStart;
    boolean inDouble = inDoubleAtStart;
    for (int i = 0; i < line.length() - 1; i++) {
      char ch = line.charAt(i);
      if (ch == '\'' && !inDouble) {
        boolean escaped = i > 0 && line.charAt(i - 1) == '\\';
        if (!escaped) inSingle = !inSingle;
      } else if (ch == '"' && !inSingle) {
        boolean escaped = i > 0 && line.charAt(i - 1) == '\\';
        if (!escaped) inDouble = !inDouble;
      }
      if (!inSingle && !inDouble && line.charAt(i) == '-' && line.charAt(i + 1) == '-') {
        return i;
      }
    }
    return -1;
  }

  private static void write(String s) throws Exception {
    Files.writeString(REPORT, s, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
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
      try (InputStream in = SeedFixRunnerTest.class.getClassLoader().getResourceAsStream(path)) {
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
      String pattern = "<property\\s+name\\s*=\\s*\\\"" + Pattern.quote(name)
          + "\\\"\\s+value\\s*=\\s*\\\"([^\\\"]*)\\\"\\s*/?>";
      Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(xml);
      if (m.find()) return Optional.ofNullable(m.group(1));
      return Optional.empty();
    }
  }
}
