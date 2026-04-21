package vn.edu.iuh.fit.client.controller;


import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import vn.edu.iuh.fit.server.dto.ScheduleDTO;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScheduleController {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private ComboBox<String> startStationCombo;

    @FXML
    private ComboBox<String> endStationCombo;

    @FXML
    private ComboBox<String> trainCombo;

    @FXML
    private ComboBox<String> statusCombo;

    @FXML
    private TableView<ScheduleDTO> scheduleTable;

    @FXML
    private TableColumn<ScheduleDTO, String> idColumn;

    @FXML
    private TableColumn<ScheduleDTO, String> trainColumn;

    @FXML
    private TableColumn<ScheduleDTO, String> routeColumn;

    @FXML
    private TableColumn<ScheduleDTO, String> departureColumn;

    @FXML
    private TableColumn<ScheduleDTO, String> arrivalColumn;

    @FXML
    private TableColumn<ScheduleDTO, String> priceColumn;

    @FXML
    private TableColumn<ScheduleDTO, String> seatsColumn;

    @FXML
    private TableColumn<ScheduleDTO, String> statusColumn;

    @FXML
    private Label pageLabel;

    @FXML
    private StackPane loadingOverlay;

    private int currentPage = 1;
    private int totalPages = 1;

    @FXML
    public void initialize() {
        setupFilterData();
        setupTableColumns();
        scheduleTable.setItems(FXCollections.observableArrayList());
        updatePageLabel();
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(false);
        }
    }

    public void setParentController() {
        // Reserved for integration with a parent dashboard controller.
    }

    public void setAddMode() {
        showInfo("Mode", "Đang ở chế độ thêm lịch trình.");
    }

    public void setEditMode(String scheduleId) {
        showInfo("Mode", "Đang ở chế độ sửa lịch trình: " + scheduleId);
    }

    @FXML
    public void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            updatePageLabel();
        }
    }

    @FXML
    public void handleNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            updatePageLabel();
        }
    }

    @FXML
    public void handleAddSchedule() {
        openDialog("/client/ui/views/add-schedule-dialog.fxml", "Thêm lịch trình");
    }

    @FXML
    public void handleEditSchedule() {
        ScheduleDTO selectedSchedule = scheduleTable.getSelectionModel().getSelectedItem();
        if (selectedSchedule == null) {
            showWarning("Sửa lịch trình", "Vui lòng chọn một lịch trình để sửa.");
            return;
        }

        openDialog("/client/ui/views/add-schedule-dialog.fxml", "Sửa lịch trình");
    }

    @FXML
    public void handleDeleteSchedule() {
        ScheduleDTO selectedSchedule = scheduleTable.getSelectionModel().getSelectedItem();
        if (selectedSchedule == null) {
            showWarning("Xóa lịch trình", "Vui lòng chọn một lịch trình để xóa.");
            return;
        }

        scheduleTable.getItems().remove(selectedSchedule);
        showInfo("Xóa lịch trình", "Đã xóa lịch trình khỏi danh sách hiển thị.");
    }

    @FXML
    public void handleGenerateSchedules() {
        openDialog("/client/ui/views/generate-schedules-dialog.fxml", "Tạo lịch trình tự động");
    }

    @FXML
    public void handleRefresh() {
        scheduleTable.refresh();
        showInfo("Làm mới", "Đã làm mới dữ liệu lịch trình.");
    }

    @FXML
    public void handleViewSeatsDetail() {
        ScheduleDTO selectedSchedule = scheduleTable.getSelectionModel().getSelectedItem();
        if (selectedSchedule == null) {
            showWarning("Chi tiết lịch trình", "Vui lòng chọn một lịch trình để xem chi tiết.");
            return;
        }

        showInfo("Chi tiết lịch trình", "Chức năng chi tiết ghế sẽ được nối dữ liệu ở bước tiếp theo.");
    }

    @FXML
    public void handleSave() {
        closeCurrentWindow();
    }

    @FXML
    public void handleGenerate() {
        closeCurrentWindow();
    }

    @FXML
    public void handleCancel() {
        closeCurrentWindow();
    }

    private void setupFilterData() {
        startStationCombo.setItems(FXCollections.observableArrayList("Tất cả điểm đi", "Sài Gòn", "Đà Nẵng", "Hà Nội"));
        endStationCombo.setItems(FXCollections.observableArrayList("Tất cả điểm đến", "Sài Gòn", "Đà Nẵng", "Hà Nội"));
        trainCombo.setItems(FXCollections.observableArrayList("Tất cả mã tàu", "SE1", "SE2", "SE3"));
        statusCombo.setItems(FXCollections.observableArrayList("Tất cả trạng thái", "DRAFT", "NOT_STARTED", "IN_PROGRESS", "PAUSED", "READY", "COMPLETED"));
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getId()));
        trainColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getTrainName()));
        routeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getRouteCode()));
        departureColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(formatDateTime(data.getValue().getDepartureTime())));
        arrivalColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(formatDateTime(data.getValue().getArrivalTime())));
        priceColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper("-"));
        seatsColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper("-"));
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getStatus() == null ? "" : data.getValue().getStatus().name()));
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_TIME_FORMATTER);
    }

    private void updatePageLabel() {
        pageLabel.setText("Trang " + currentPage + "/" + totalPages);
    }

    private void openDialog(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            showError("Không thể mở giao diện", "Lỗi khi load FXML: " + fxmlPath + "\n" + e.getMessage());
        }
    }

    private void closeCurrentWindow() {
        Stage stage = (Stage) scheduleTable.getScene().getWindow();
        stage.close();
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
