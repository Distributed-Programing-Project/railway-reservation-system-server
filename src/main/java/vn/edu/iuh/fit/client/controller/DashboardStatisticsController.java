package vn.edu.iuh.fit.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DashboardStatisticsController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private Label welcomeSubtitle;

    private Runnable onOpenSchedule;

    @FXML
    public void initialize() {
        updateSubtitle("Dữ liệu cập nhật lúc " + DATE_TIME_FORMATTER.format(LocalDateTime.now()));
    }

    public void setLoggedInUsername(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        updateSubtitle("Xin chào " + username + " - cập nhật lúc " + DATE_TIME_FORMATTER.format(LocalDateTime.now()));
    }

    @FXML
    public void handleRefreshData() {
        updateSubtitle("Dữ liệu cập nhật lúc " + DATE_TIME_FORMATTER.format(LocalDateTime.now()));
    }

    @FXML
    public void handleOpenSchedule() {
        if (onOpenSchedule != null) {
            onOpenSchedule.run();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/ui/views/schedule-management.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) welcomeSubtitle.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 700));
            stage.setTitle("Train Station - Schedule Management");
            stage.show();
        } catch (IOException e) {
            showError("Chuyển màn hình", "Không thể mở giao diện lịch trình: " + e.getMessage());
        }
    }

    public void setOnOpenSchedule(Runnable onOpenSchedule) {
        this.onOpenSchedule = onOpenSchedule;
    }

    private void updateSubtitle(String text) {
        if (welcomeSubtitle != null) {
            welcomeSubtitle.setText(text);
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
