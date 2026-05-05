package vn.edu.iuh.fit.client;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Smoke test to load key FXML screens without clicking the UI.
 *
 * Intentionally writes full stack traces to a file (not stdout/stderr) to avoid
 * flooding the terminal while still capturing the real root cause.
 */
public class FxmlLoadSmokeTest {

  private static final Path REPORT_DIR = Path.of("test-reports");
  private static final Path SMOKE_LOG = REPORT_DIR.resolve("fxml-load-smoke.log");

  @BeforeAll
  static void initJavaFxToolkit() throws Exception {
    Files.createDirectories(REPORT_DIR);
    // OpenJFX caches native libs under ${user.home}/.openjfx. In sandboxed/CI
    // environments, the default user.home may be non-writable, causing noisy
    // warnings and (sometimes) failures. Point it to the project root for tests.
    System.setProperty("user.home", Path.of(".").toAbsolutePath().normalize().toString());
    try {
      CountDownLatch latch = new CountDownLatch(1);
      Platform.startup(latch::countDown);
      if (!latch.await(20, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Timeout initializing JavaFX Platform");
      }
    } catch (IllegalStateException alreadyStarted) {
      // Toolkit already started in this JVM.
    }
  }

  @AfterAll
  static void tearDown() {
    // Keep the toolkit alive; calling Platform.exit() can break other tests/suites.
  }

  @Test
  void loadSellTicketWizardFxml() {
    loadFxmlOrFail("/client/ui/views/sell-ticket-wizard.fxml");
  }

  @Test
  void loadBanVeFxml() {
    loadFxmlOrFail("/client/ui/views/ban-ve.fxml");
  }

  @Test
  void loadBanVeStep1Fxml() {
    loadFxmlOrFail("/client/ui/views/step-1.fxml");
  }

  @Test
  void loadBanVeStep2Fxml() {
    loadFxmlOrFail("/client/ui/views/step-2.fxml");
  }

  @Test
  void loadBanVeStep3Fxml() {
    loadFxmlOrFail("/client/ui/views/step-3.fxml");
  }

  @Test
  void loadBanVeStep4Fxml() {
    loadFxmlOrFail("/client/ui/views/step-4.fxml");
  }

  @Test
  void loadBanVePassengerRowFxml() {
    loadFxmlOrFail("/client/ui/views/hanhkhach-row.fxml");
  }

  @Test
  void loadPdfViewerFxml() {
    loadFxmlOrFail("/client/ui/views/pdf-viewer.fxml");
  }

  @Test
  void loadPrintListViewFxml() {
    loadFxmlOrFail("/client/ui/views/print-list-view.fxml");
  }

  @Test
  void loadExchangeTicketFxml() {
    loadFxmlOrFail("/client/ui/views/doi-ve.fxml");
  }

  private static void loadFxmlOrFail(String resourcePath) {
    URL url = FxmlLoadSmokeTest.class.getResource(resourcePath);
    assertNotNull(url, "Missing FXML resource: " + resourcePath);

    AtomicReference<Throwable> error = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    Platform.runLater(() -> {
      try {
        Parent root = FXMLLoader.load(url);
        if (root == null) {
          throw new IllegalStateException("FXMLLoader returned null root for " + resourcePath);
        }
      } catch (Throwable t) {
        error.set(t);
      } finally {
        latch.countDown();
      }
    });

    try {
      if (!latch.await(30, TimeUnit.SECONDS)) {
        fail("Timeout loading FXML: " + resourcePath);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      fail("Interrupted while loading FXML: " + resourcePath);
    }

    Throwable t = error.get();
    if (t != null) {
      writeFailure(resourcePath, t);
      fail("FXML load failed for " + resourcePath + " (see " + SMOKE_LOG + ")");
    }
  }

  private static void writeFailure(String resourcePath, Throwable t) {
    StringWriter buffer = new StringWriter();
    t.printStackTrace(new PrintWriter(buffer));
    String content = """
        === FXML LOAD FAIL ===
        Time: %s
        Resource: %s
        
        Root cause:
        %s
        
        Full stack trace:
        %s
        
        """.formatted(LocalDateTime.now(), resourcePath, rootCauseSummary(t), buffer);

    try {
      Files.createDirectories(REPORT_DIR);
      Files.writeString(
          SMOKE_LOG,
          content,
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (Exception ignored) {
      // If writing fails, keep the test failure message short; dev can rerun with debug locally.
    }
  }

  private static String rootCauseSummary(Throwable t) {
    Throwable root = t;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    return root.getClass().getName() + ": " + String.valueOf(root.getMessage());
  }
}
