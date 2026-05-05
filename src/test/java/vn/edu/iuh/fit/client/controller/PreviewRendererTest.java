package vn.edu.iuh.fit.client.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import vn.edu.iuh.fit.client.session.SaleWizardState;
import vn.edu.iuh.fit.common.constant.DocumentType;
import vn.edu.iuh.fit.common.constant.SeatType;
import vn.edu.iuh.fit.common.constant.TicketType;
import vn.edu.iuh.fit.common.dto.IssuedTicketDTO;
import vn.edu.iuh.fit.common.dto.SaleBuyerDTO;
import vn.edu.iuh.fit.common.dto.SaleCreateResponseDTO;

class PreviewRendererTest {

  @BeforeAll
  static void initJavaFxToolkit() throws Exception {
    System.setProperty("user.home", java.nio.file.Path.of(".").toAbsolutePath().normalize().toString());
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

  @Test
  void ticketRenderer_shouldProduceNonBlankImage() throws Exception {
    AtomicReference<Image> ref = new AtomicReference<>();
    runOnFxThread(() -> ref.set(TicketRenderer.renderToImage(sampleTicket(), "In vé")));
    Image image = ref.get();
    assertNotNull(image);
    assertTrue(image.getWidth() > 0, "ticket preview width must be > 0");
    assertTrue(image.getHeight() > 0, "ticket preview height must be > 0");
    assertTrue(hasInk(image), "ticket preview looks blank");
  }

  @Test
  void invoiceRenderer_shouldProduceNonBlankImage() throws Exception {
    AtomicReference<Image> ref = new AtomicReference<>();
    runOnFxThread(() -> ref.set(InvoiceRenderer.renderToImage(sampleSale(), sampleState())));
    Image image = ref.get();
    assertNotNull(image);
    assertTrue(image.getWidth() > 0, "invoice preview width must be > 0");
    assertTrue(image.getHeight() > 0, "invoice preview height must be > 0");
    assertTrue(hasInk(image), "invoice preview looks blank");
  }

  private static void runOnFxThread(Runnable action) throws Exception {
    if (Platform.isFxApplicationThread()) {
      action.run();
      return;
    }
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();
    Platform.runLater(() -> {
      try {
        action.run();
      } catch (Throwable t) {
        error.set(t);
      } finally {
        latch.countDown();
      }
    });
    if (!latch.await(20, TimeUnit.SECONDS)) {
      throw new IllegalStateException("Timeout waiting for FX render");
    }
    if (error.get() != null) {
      if (error.get() instanceof Exception ex) {
        throw ex;
      }
      throw new RuntimeException(error.get());
    }
  }

  private static boolean hasInk(Image image) {
    PixelReader reader = image.getPixelReader();
    if (reader == null) {
      return false;
    }
    int width = (int) Math.round(image.getWidth());
    int height = (int) Math.round(image.getHeight());
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        Color c = reader.getColor(x, y);
        if (c.getOpacity() > 0.05 && (c.getRed() < 0.97 || c.getGreen() < 0.97 || c.getBlue() < 0.97)) {
          return true;
        }
      }
    }
    return false;
  }

  private static IssuedTicketDTO sampleTicket() {
    return IssuedTicketDTO.builder()
        .ticketId("TCK-001")
        .passengerName("Nguyen Van A")
        .passengerDocument("012345678")
        .scheduleId("SCH-001")
        .trainCode("SE1")
        .departureStation("Sai Gon")
        .destinationStation("Ha Noi")
        .departureTime(LocalDateTime.of(2026, 5, 2, 8, 30))
        .carriageName("A1")
        .seatNumber("12")
        .seatType(SeatType.SOFT_SEAT)
        .ticketType(TicketType.NORMAL)
        .price(450000d)
        .qrCode("QR-TCK-001")
        .build();
  }

  private static SaleWizardState sampleState() {
    SaleWizardState state = new SaleWizardState();
    SaleWizardState.BuyerDraft buyer = new SaleWizardState.BuyerDraft();
    buyer.setFullName("Nguyen Van A");
    buyer.setDocumentNumber("012345678");
    buyer.setDocumentType(DocumentType.ID_CARD);
    state.setBuyer(buyer);
    return state;
  }

  private static SaleCreateResponseDTO sampleSale() {
    return SaleCreateResponseDTO.builder()
        .invoiceId("INV-001")
        .invoiceDate(LocalDateTime.of(2026, 5, 2, 9, 15))
        .totalAmount(450000d)
        .amountPaid(500000d)
        .changeAmount(50000d)
        .tickets(List.of(sampleTicket()))
        .childVouchers(List.of())
        .build();
  }
}
