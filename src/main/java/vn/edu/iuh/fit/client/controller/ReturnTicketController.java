package vn.edu.iuh.fit.client.controller;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import vn.edu.iuh.fit.client.service.ReturnTicketClientService;
import vn.edu.iuh.fit.client.session.ClientSessionContext;
import vn.edu.iuh.fit.common.dto.IssuedTicketDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketPreviewDTO;
import vn.edu.iuh.fit.common.dto.ReturnTicketTicketDTO;
import vn.edu.iuh.fit.common.response.Response;

public class ReturnTicketController {

  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final NumberFormat MONEY = NumberFormat.getInstance(new Locale("vi", "VN"));
  private static final long MINUTES_24H = 24 * 60;

  private final ReturnTicketClientService returnTicketClientService = new ReturnTicketClientService();

  private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
    Thread t = new Thread(r, "return-ticket-worker");
    t.setDaemon(true);
    return t;
  });
  private final AtomicBoolean cleanupDone = new AtomicBoolean(false);
  private final AtomicLong searchSeq = new AtomicLong(0);
  private final AtomicLong previewSeq = new AtomicLong(0);
  private final AtomicLong confirmSeq = new AtomicLong(0);

  private Timeline countdownTimeline;
  private ReturnTicketPreviewDTO lastPreview;
  private String lastSearchIdCard;

  @FXML
  private TextField txtSearchCCCD;
  @FXML
  private Button btnTimKiem;

  @FXML
  private TableView<ReturnTicketTicketDTO> tblDanhSachVe;
  @FXML
  private TableColumn<ReturnTicketTicketDTO, String> colMaVe;
  @FXML
  private TableColumn<ReturnTicketTicketDTO, String> colTau;
  @FXML
  private TableColumn<ReturnTicketTicketDTO, String> colHanhTrinh;
  @FXML
  private TableColumn<ReturnTicketTicketDTO, String> colNgayDi;
  @FXML
  private TableColumn<ReturnTicketTicketDTO, String> colGhe;
  @FXML
  private TableColumn<ReturnTicketTicketDTO, String> colGiaVe;
  @FXML
  private TableColumn<ReturnTicketTicketDTO, String> colTrangThai;

  @FXML
  private Label lblMaVeChon;
  @FXML
  private Label lblTau;
  @FXML
  private Label lblHanhTrinh;
  @FXML
  private Label lblNgayDi;
  @FXML
  private Label lblGhe;
  @FXML
  private Label lblThoiGianConLai;

  @FXML
  private Label lblThongBaoLoi;
  @FXML
  private Label lblDieuKienVe;
  @FXML
  private Label lblGiaVeGoc;
  @FXML
  private Label lblPhiTraVe;
  @FXML
  private Label lblTienHoanLai;

  @FXML
  private Button btnXacNhanTra;
  @FXML
  private Button btnDoiVe;

  @FXML
  public void initialize() {
    setupTable();
    registerCloseCleanupHook();

    btnTimKiem.setOnAction(e -> handleSearch());
    txtSearchCCCD.setOnAction(e -> handleSearch());
    btnXacNhanTra.setOnAction(e -> handleConfirmReturn());

    resetDetail();
    resetPreview();
    setBusy(false);
  }

  private void setupTable() {
    if (tblDanhSachVe == null)
      return;
    tblDanhSachVe.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    colMaVe.setCellValueFactory(c -> s(c.getValue() != null ? c.getValue().getId() : null));
    colTau.setCellValueFactory(c -> s(c.getValue() != null ? c.getValue().getTrainCode() : null));
    colHanhTrinh.setCellValueFactory(c -> {
      ReturnTicketTicketDTO dto = c.getValue();
      if (dto == null)
        return s(null);
      String from = safe(dto.getDepartureStation());
      String to = safe(dto.getDestinationStation());
      if ("--".equals(from) && "--".equals(to))
        return s("--");
      return s(from + " → " + to);
    });
    colNgayDi.setCellValueFactory(c -> {
      LocalDateTime dt = c.getValue() != null ? c.getValue().getDepartureTime() : null;
      return s(dt == null ? null : DATE_TIME.format(dt));
    });
    colGhe.setCellValueFactory(c -> {
      ReturnTicketTicketDTO dto = c.getValue();
      if (dto == null)
        return s(null);
      String carriage = safe(dto.getCarriageName());
      String seat = safe(dto.getSeatNumber());
      if ("--".equals(carriage) && "--".equals(seat))
        return s("--");
      return s("Toa " + carriage + " - Ghế " + seat);
    });
    colGiaVe.setCellValueFactory(c -> s(formatMoney(c.getValue() != null ? c.getValue().getTicketPrice() : 0)));
    colTrangThai.setCellValueFactory(c -> {
      ReturnTicketTicketDTO dto = c.getValue();
      return s(dto != null && dto.getStatus() != null ? dto.getStatus().getName() : null);
    });

    tblDanhSachVe.getSelectionModel().getSelectedItems()
        .addListener((ListChangeListener<ReturnTicketTicketDTO>) change -> {
          handleSelectionChanged();
        });
    tblDanhSachVe.setItems(FXCollections.observableArrayList());
  }

  private void handleSearch() {
    String idCard = normalize(txtSearchCCCD != null ? txtSearchCCCD.getText() : null);
    if (idCard == null) {
      showWarning("Trả vé", "Vui lòng nhập CCCD/Hộ chiếu.");
      return;
    }
    lastSearchIdCard = idCard;
    doSearchAsync(idCard);
  }

  private void doSearchAsync(String idCard) {
    final long seq = searchSeq.incrementAndGet();
    setBusy(true);
    setInlineError(null);
    resetDetail();
    resetPreview();

    Task<Response> task = new Task<>() {
      @Override
      protected Response call() {
        return returnTicketClientService.searchTicketsForReturn(idCard);
      }
    };

    task.setOnSucceeded(e -> {
      if (seq != searchSeq.get())
        return;
      setBusy(false);
      Response res = task.getValue();
      if (res != null && res.isSuccess() && res.getData() instanceof List<?> list) {
        List<ReturnTicketTicketDTO> items = new ArrayList<>();
        for (Object o : list) {
          if (o instanceof ReturnTicketTicketDTO dto) {
            items.add(dto);
          }
        }
        tblDanhSachVe.getItems().setAll(items);
        if (items.isEmpty()) {
          setInlineError("Không tìm thấy lịch sử mua vé cho giấy tờ này.");
        }
      } else {
        setInlineError(res == null ? "Không nhận được phản hồi." : res.getMessage());
      }
    });

    task.setOnFailed(e -> {
      if (seq != searchSeq.get())
        return;
      setBusy(false);
      setInlineError("Lỗi khi tra cứu vé.");
    });

    start(task, "return-ticket-search");
  }

  private void handleSelectionChanged() {
    List<ReturnTicketTicketDTO> selected = getSelectedTickets();
    renderDetail(selected);
    startOrStopCountdown(selected);

    if (selected.isEmpty()) {
      resetPreview();
      return;
    }
    doPreviewAsync(selected.stream().map(ReturnTicketTicketDTO::getId).toList(), selected);
  }

  private void doPreviewAsync(List<String> ticketIds, List<ReturnTicketTicketDTO> selectionSnapshot) {
    final long seq = previewSeq.incrementAndGet();
    setBusy(true);
    setInlineError(null);
    btnXacNhanTra.setDisable(true);

    Task<Response> task = new Task<>() {
      @Override
      protected Response call() {
        return returnTicketClientService.previewReturnTickets(ticketIds);
      }
    };

    task.setOnSucceeded(e -> {
      if (seq != previewSeq.get())
        return;
      setBusy(false);
      Response res = task.getValue();
      if (res != null && res.isSuccess() && res.getData() instanceof ReturnTicketPreviewDTO dto) {
        lastPreview = dto;
        lblGiaVeGoc.setText(formatMoney(dto.getTotalTicketPrice()));
        lblPhiTraVe.setText(formatMoney(dto.getRefundFee()));
        lblTienHoanLai.setText(formatMoney(dto.getRefundAmount()));
        lblDieuKienVe.setText(buildConditionText(selectionSnapshot));
        btnXacNhanTra.setDisable(false);
      } else {
        lastPreview = null;
        resetPreviewAmounts();
        setInlineError(res == null ? "Không nhận được phản hồi." : res.getMessage());
      }
    });

    task.setOnFailed(e -> {
      if (seq != previewSeq.get())
        return;
      setBusy(false);
      lastPreview = null;
      resetPreviewAmounts();
      setInlineError("Lỗi khi tính toán hoàn tiền.");
    });

    start(task, "return-ticket-preview");
  }

  private void handleConfirmReturn() {
    List<String> ticketIds = getSelectedTickets().stream().map(ReturnTicketTicketDTO::getId).toList();
    if (ticketIds.isEmpty()) {
      showWarning("Trả vé", "Vui lòng chọn ít nhất 1 vé.");
      return;
    }
    if (lastPreview == null) {
      showWarning("Trả vé", "Vui lòng tính toán hoàn tiền trước khi xác nhận.");
      return;
    }
    String employeeId = ClientSessionContext.getInstance().getEmployeeId();
    if (employeeId == null || employeeId.isBlank()) {
      showError("Trả vé", "Không xác định được nhân viên đang đăng nhập.");
      return;
    }

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Trả vé");
    confirm.setHeaderText(null);
    confirm.setContentText(
        "Xác nhận trả " + ticketIds.size() + " vé?\nSố tiền hoàn: " + formatMoney(lastPreview.getRefundAmount()));
    if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
      return;

    doConfirmAsync(ticketIds, lastPreview.getRefundAmount(), employeeId);
  }

  private void doConfirmAsync(List<String> ticketIds, double refundAmount, String employeeId) {
    final long seq = confirmSeq.incrementAndGet();
    setBusy(true);
    setInlineError(null);
    btnXacNhanTra.setDisable(true);

    Task<Response> task = new Task<>() {
      @Override
      protected Response call() {
        return returnTicketClientService.confirmReturnTickets(ticketIds, refundAmount, employeeId);
      }
    };

    task.setOnSucceeded(e -> {
      if (seq != confirmSeq.get())
        return;
      setBusy(false);
      Response res = task.getValue();
      if (res != null && res.isSuccess()) {
        String invoiceId = res.getData() != null ? String.valueOf(res.getData()) : null;
        showInfo("Trả vé", res.getMessage() + (invoiceId != null ? ("\nMã biên lai: " + invoiceId) : ""));
        resetDetail();
        resetPreview();
        if (lastSearchIdCard != null) {
          doSearchAsync(lastSearchIdCard);
        }
        if (invoiceId != null) {
          askAndPrintRefundReceipt(invoiceId, refundAmount);
        }
      } else {
        btnXacNhanTra.setDisable(false);
        setInlineError(res == null ? "Không nhận được phản hồi." : res.getMessage());
      }
    });

    task.setOnFailed(e -> {
      if (seq != confirmSeq.get())
        return;
      setBusy(false);
      btnXacNhanTra.setDisable(false);
      setInlineError("Lỗi khi xác nhận trả vé.");
    });

    start(task, "return-ticket-confirm");
  }

  private void askAndPrintRefundReceipt(String invoiceId, double refundAmount) {
    Alert ask = new Alert(Alert.AlertType.CONFIRMATION);
    ask.setTitle("In biên lai");
    ask.setHeaderText(null);
    ask.setContentText("Khách có yêu cầu in biên lai hoàn tiền không?");
    if (ask.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
      return;

    IssuedTicketDTO summary = IssuedTicketDTO.builder()
        .ticketId("REFUND-" + invoiceId)
        .passengerName(ClientSessionContext.getInstance().getUsername())
        .passengerDocument("")
        .trainCode("REFUND")
        .departureStation("")
        .destinationStation("")
        .departureTime(LocalDateTime.now())
        .carriageName("")
        .seatNumber("")
        .ticketType(vn.edu.iuh.fit.common.constant.TicketType.NORMAL)
        .price(refundAmount)
        .qrCode(invoiceId)
        .build();
    openPreview("Biên lai hoàn tiền (tóm tắt)", List.of(summary));
  }

  private void openPreview(String title, List<IssuedTicketDTO> tickets) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/ui/views/pdf-viewer.fxml"));
      Parent root = loader.load();
      Object controller = loader.getController();
      if (controller instanceof PdfViewerController pdf) {
        Task<List<Image>> renderTask = new Task<>() {
          @Override
          protected List<Image> call() {
            return tickets.stream().map(t -> TicketRenderer.renderToImage(t, title)).toList();
          }
        };
        renderTask.setOnSucceeded(e -> pdf.setPages(renderTask.getValue(), title));
        renderTask
            .setOnFailed(e -> showError("In", "Không thể render xem trước: " + renderTask.getException().getMessage()));
        start(renderTask, "return-ticket-render-preview");
      }
      javafx.stage.Stage stage = new javafx.stage.Stage();
      stage.setTitle(title);
      stage.initModality(Modality.APPLICATION_MODAL);
      stage.setScene(new Scene(root, 1000, 800));
      stage.show();
    } catch (Exception e) {
      showError("In", "Không thể mở xem trước: " + e.getMessage());
    }
  }

  private void renderDetail(List<ReturnTicketTicketDTO> selected) {
    if (selected == null || selected.isEmpty()) {
      resetDetail();
      return;
    }
    if (selected.size() == 1) {
      ReturnTicketTicketDTO dto = selected.get(0);
      lblMaVeChon.setText(safe(dto.getId()));
      lblTau.setText(safe(dto.getTrainCode()));
      lblHanhTrinh.setText(safe(dto.getDepartureStation()) + " → " + safe(dto.getDestinationStation()));
      lblNgayDi.setText(dto.getDepartureTime() == null ? "--" : DATE_TIME.format(dto.getDepartureTime()));
      lblGhe.setText("Toa " + safe(dto.getCarriageName()) + " - Ghế " + safe(dto.getSeatNumber()));
      return;
    }
    lblMaVeChon.setText(selected.size() + " vé");
    lblTau.setText("Nhiều vé");
    lblHanhTrinh.setText("Nhiều hành trình");
    lblNgayDi.setText("--");
    lblGhe.setText("--");
  }

  private void startOrStopCountdown(List<ReturnTicketTicketDTO> selected) {
    stopCountdown();
    if (selected == null || selected.isEmpty()) {
      lblThoiGianConLai.setText("--");
      return;
    }
    LocalDateTime nearest = selected.stream()
        .map(ReturnTicketTicketDTO::getDepartureTime)
        .filter(d -> d != null)
        .min(Comparator.naturalOrder())
        .orElse(null);
    if (nearest == null) {
      lblThoiGianConLai.setText("--");
      return;
    }
    updateCountdownLabel(nearest);
    countdownTimeline = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> updateCountdownLabel(nearest)));
    countdownTimeline.setCycleCount(Timeline.INDEFINITE);
    countdownTimeline.play();
  }

  private void updateCountdownLabel(LocalDateTime departureTime) {
    if (departureTime == null) {
      lblThoiGianConLai.setText("--");
      return;
    }
    long minutes = Duration.between(LocalDateTime.now(), departureTime).toMinutes();
    if (minutes <= 0) {
      lblThoiGianConLai.setText("Đã khởi hành");
      return;
    }
    long hours = minutes / 60;
    long mins = minutes % 60;
    lblThoiGianConLai.setText(hours + "h " + mins + "p");
  }

  private void stopCountdown() {
    if (countdownTimeline != null) {
      countdownTimeline.stop();
      countdownTimeline = null;
    }
  }

  private String buildConditionText(List<ReturnTicketTicketDTO> selected) {
    if (selected == null || selected.isEmpty())
      return "--";
    boolean exchanged = selected.stream()
        .anyMatch(t -> t != null && t.getOriginalTicketId() != null && !t.getOriginalTicketId().isBlank());
    if (exchanged) {
      return "Vé đã đổi: phí 30%";
    }
    LocalDateTime nearest = selected.stream()
        .map(ReturnTicketTicketDTO::getDepartureTime)
        .filter(d -> d != null)
        .min(Comparator.naturalOrder())
        .orElse(null);
    if (nearest == null)
      return "--";
    long minutes = Duration.between(LocalDateTime.now(), nearest).toMinutes();
    return minutes < MINUTES_24H ? "Phí 20% (dưới 24h)" : "Phí 10% (từ 24h)";
  }

  private List<ReturnTicketTicketDTO> getSelectedTickets() {
    if (tblDanhSachVe == null)
      return List.of();
    return List.copyOf(tblDanhSachVe.getSelectionModel().getSelectedItems());
  }

  private void resetDetail() {
    lblMaVeChon.setText("--");
    lblTau.setText("--");
    lblHanhTrinh.setText("--");
    lblNgayDi.setText("--");
    lblGhe.setText("--");
    lblThoiGianConLai.setText("--");
    stopCountdown();
  }

  private void resetPreview() {
    lastPreview = null;
    lblDieuKienVe.setText("--");
    resetPreviewAmounts();
    btnXacNhanTra.setDisable(true);
  }

  private void resetPreviewAmounts() {
    lblGiaVeGoc.setText(formatMoney(0));
    lblPhiTraVe.setText(formatMoney(0));
    lblTienHoanLai.setText(formatMoney(0));
  }

  private void setInlineError(String message) {
    if (lblThongBaoLoi == null)
      return;
    lblThongBaoLoi.setText(message == null ? "" : message);
  }

  private void setBusy(boolean busy) {
    if (btnTimKiem != null)
      btnTimKiem.setDisable(busy);
    if (txtSearchCCCD != null)
      txtSearchCCCD.setDisable(busy);
  }

  private void registerCloseCleanupHook() {
    if (tblDanhSachVe == null)
      return;
    tblDanhSachVe.sceneProperty().addListener((obs, oldScene, newScene) -> {
      if (oldScene != null && newScene == null) {
        cleanupOnClose();
        return;
      }
      if (newScene == null)
        return;

      Window existing = newScene.getWindow();
      if (existing != null) {
        existing.addEventHandler(WindowEvent.WINDOW_HIDDEN, e -> cleanupOnClose());
      }

      newScene.windowProperty().addListener((obsWin, oldWin, newWin) -> {
        if (newWin == null)
          return;
        newWin.addEventHandler(WindowEvent.WINDOW_HIDDEN, e -> cleanupOnClose());
      });
    });
  }

  private void cleanupOnClose() {
    if (!cleanupDone.compareAndSet(false, true))
      return;
    stopCountdown();
    executor.shutdown();
    if (tblDanhSachVe != null) {
      tblDanhSachVe.getItems().clear();
    }
  }

  private void start(Task<?> task, String name) {
    executor.execute(() -> {
      String oldName = Thread.currentThread().getName();
      Thread.currentThread().setName(name + "-" + UUID.randomUUID());
      try {
        task.run();
      } finally {
        Thread.currentThread().setName(oldName);
      }
    });
  }

  private static javafx.beans.property.SimpleStringProperty s(String v) {
    return new javafx.beans.property.SimpleStringProperty(v == null ? "" : v);
  }

  private static String normalize(String value) {
    if (value == null)
      return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String safe(String v) {
    return v == null || v.isBlank() ? "--" : v;
  }

  private static String formatMoney(double v) {
    return MONEY.format(Math.round(v)) + " đ";
  }

  private void showInfo(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }

  private void showWarning(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }

  private void showError(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}
