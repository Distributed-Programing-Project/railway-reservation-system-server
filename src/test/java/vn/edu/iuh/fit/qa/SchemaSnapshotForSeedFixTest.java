package vn.edu.iuh.fit.qa;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Snapshot Hibernate-created MariaDB schema (SHOW CREATE TABLE) so seed scripts can match the real table/column names.
 *
 * Writes everything to test-reports/ (no long terminal output).
 */
public class SchemaSnapshotForSeedFixTest {

  private static final Path REPORT = Path.of("test-reports", "seed-schema-snapshot.md");

  private static final String SCHEMA = "distributed_db";

  private static final List<String> TABLES = List.of(
      "stations",
      "routes",
      "route_stops",
      "trains",
      "carriages",
      "seats",
      "schedules",
      "schedule_details",
      "accounts",
      "employees",
      "customers",
      "tickets",
      "invoices",
      "invoice_details",
      "invoice_metadata"
  );

  @Test
  void snapshot_show_create_tables_and_counts() throws Exception {
    Files.createDirectories(REPORT.getParent());
    Files.deleteIfExists(REPORT);

    DbConfig db = DbConfig.fromPersistenceXml();
    write("""
        # Seed Fix Schema Snapshot

        Time: %s

        JDBC:
        - url: `%s`
        - user: `%s`
        - password: `%s`

        """.formatted(LocalDateTime.now(), db.jdbcUrl, db.jdbcUser, mask(db.jdbcPassword)));

    try (Connection c = DriverManager.getConnection(db.jdbcUrl, db.jdbcUser, db.jdbcPassword);
        Statement st = c.createStatement()) {

      write("## A. Table existence + COUNT(*)\n\n");
      for (String table : TABLES) {
        boolean exists = tableExists(st, SCHEMA, table);
        write("- `" + table + "`: " + (exists ? "EXISTS" : "MISSING") + "\n");
        if (exists) {
          long count = firstLong(st, "SELECT COUNT(*) FROM " + table);
          write("  - COUNT: " + count + "\n");
        }
      }

      write("\n## B. SHOW CREATE TABLE\n\n");
      for (String table : TABLES) {
        boolean exists = tableExists(st, SCHEMA, table);
        write("### `" + table + "`\n\n");
        if (!exists) {
          write("- (missing)\n\n");
          continue;
        }
        String ddl = showCreateTable(st, table);
        write("```sql\n" + ddl + "\n```\n\n");
      }

      write("## C. Column map (quick)\n\n");
      Map<String, List<String>> columns = new LinkedHashMap<>();
      for (String table : TABLES) {
        if (!tableExists(st, SCHEMA, table)) continue;
        columns.put(table, listColumns(st, SCHEMA, table));
      }
      for (var e : columns.entrySet()) {
        write("- `" + e.getKey() + "` columns:\n");
        for (String col : e.getValue()) {
          write("  - " + col + "\n");
        }
      }
    } catch (Exception e) {
      write("\n## ERROR\n\n");
      write("- Cannot connect / snapshot schema: `" + rootCauseSummary(e) + "`\n");
      // Do not throw: allow seed scripts to be authored even when DB is temporarily unavailable.
    }
  }

  private static List<String> listColumns(Statement st, String schema, String table) throws Exception {
    try (ResultSet rs = st.executeQuery("""
        SELECT column_name
        FROM information_schema.columns
        WHERE table_schema='%s' AND table_name='%s'
        ORDER BY ordinal_position
        """.formatted(schema.replace("'", "''"), table.replace("'", "''")))) {
      java.util.ArrayList<String> out = new java.util.ArrayList<>();
      while (rs.next()) {
        out.add("`" + rs.getString(1) + "`");
      }
      return out;
    }
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
      if (!rs.next()) return "(no rows)";
      return rs.getString(2);
    }
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
      try (InputStream in = SchemaSnapshotForSeedFixTest.class.getClassLoader().getResourceAsStream(path)) {
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

