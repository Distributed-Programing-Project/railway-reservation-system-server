package vn.edu.iuh.fit.client.controller;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class PdfViewerController {

  @FXML private VBox pageContainer;
  @FXML private ImageView pageImageView;
  @FXML private Label zoomLabel;
  @FXML private Label pageLabel;
  @FXML private Button prevPageButton;
  @FXML private Button nextPageButton;
  @FXML private Button btnPrint;

  private final List<Image> pages = new ArrayList<>();
  private int index = 0;
  private double zoom = 1.0;

  public void setPages(List<Image> images, String title) {
    pages.clear();
    if (images != null) pages.addAll(images);
    index = 0;
    zoom = 1.0;
    render();
  }

  @FXML
  public void handlePrev() {
    if (index > 0) {
      index--;
      render();
    }
  }

  @FXML
  public void handleNext() {
    if (index < pages.size() - 1) {
      index++;
      render();
    }
  }

  @FXML
  public void handleZoomIn() {
    zoom = Math.min(2.5, zoom + 0.1);
    render();
  }

  @FXML
  public void handleZoomOut() {
    zoom = Math.max(0.5, zoom - 0.1);
    render();
  }

  @FXML
  public void handlePrint() {
    if (pages.isEmpty()) return;
    PrinterJob job = PrinterJob.createPrinterJob();
    if (job == null) return;
    boolean proceed = job.showPrintDialog(pageContainer.getScene().getWindow());
    if (!proceed) return;

    try {
      for (Image image : pages) {
        ImageView iv = new ImageView(image);
        iv.setPreserveRatio(true);
        iv.setFitWidth(job.getJobSettings().getPageLayout().getPrintableWidth());
        boolean ok = job.printPage(iv);
        if (!ok) break;
      }
    } finally {
      job.endJob();
    }
  }

  private void render() {
    Image image = pages.isEmpty() ? null : pages.get(index);
    pageImageView.setImage(image);
    pageImageView.setScaleX(zoom);
    pageImageView.setScaleY(zoom);
    zoomLabel.setText(Math.round(zoom * 100) + "%");
    pageLabel.setText("Trang " + (pages.isEmpty() ? 0 : (index + 1)) + " / " + pages.size());
    prevPageButton.setDisable(index <= 0);
    nextPageButton.setDisable(index >= pages.size() - 1);
    btnPrint.setDisable(pages.isEmpty());
  }
}

