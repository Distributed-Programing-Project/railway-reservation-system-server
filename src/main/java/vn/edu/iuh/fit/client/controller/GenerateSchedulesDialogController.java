package vn.edu.iuh.fit.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class GenerateSchedulesDialogController {

    @FXML
    public void handleGenerate(ActionEvent event) {
        showInfo("Tạo lịch trình", "Đã nhận thao tác tạo lịch trình tự động. Bước tiếp theo là nối API generate.");
        closeDialog(event);
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        closeDialog(event);
    }

    private void closeDialog(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
