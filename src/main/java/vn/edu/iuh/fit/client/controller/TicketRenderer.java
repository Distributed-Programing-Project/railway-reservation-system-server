package vn.edu.iuh.fit.client.controller;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import vn.edu.iuh.fit.common.dto.IssuedTicketDTO;

final class TicketRenderer {

  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
  private static final NumberFormat MONEY = NumberFormat.getInstance(new Locale("vi", "VN"));

  private TicketRenderer() {
  }

  static Image renderToImage(IssuedTicketDTO dto, String title) {
    VBox root = buildTicketNode(dto);
    SnapshotParameters params = new SnapshotParameters();
    params.setFill(Color.TRANSPARENT);
    WritableImage img = root.snapshot(params, null);
    return img;
  }

  private static VBox buildTicketNode(IssuedTicketDTO dto) {
    VBox root = new VBox(10);
    root.setStyle("-fx-background-color: white; -fx-padding: 18; -fx-border-color: #d5dde6; -fx-border-radius: 12; -fx-background-radius: 12;");
    root.setPrefWidth(420);

    Label h1 = new Label("THẺ LÊN TÀU HỎA");
    h1.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-alignment: center;");
    Label h2 = new Label("BOARDING PASS");
    h2.setStyle("-fx-font-size: 11px; -fx-text-fill: #4a5568; -fx-alignment: center;");
    VBox header = new VBox(2, h1, h2);
    header.setStyle("-fx-alignment: center;");

    StackPane qrBox = new StackPane();
    Rectangle rect = new Rectangle(120, 120);
    rect.setFill(Color.WHITE);
    rect.setStroke(Color.web("#d5dde6"));
    rect.setArcWidth(10);
    rect.setArcHeight(10);
    Label qr = new Label(dto != null ? safe(dto.getQrCode()) : "--");
    qr.setWrapText(true);
    qr.setMaxWidth(110);
    qr.setStyle("-fx-font-size: 10px; -fx-text-fill: #1f2d3d; -fx-alignment: center;");
    qrBox.getChildren().addAll(rect, qr);
    qrBox.setStyle("-fx-alignment: center;");

    Label ticketId = new Label("Mã vé/TicketID: " + safe(dto.getTicketId()));
    ticketId.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-alignment: center;");

    HBox fromTo = new HBox(10,
        kv("Ga đi / From", safe(dto.getDepartureStation()), true),
        spacer(),
        kv("Ga đến / To", safe(dto.getDestinationStation()), true));
    fromTo.setStyle("-fx-alignment: center;");

    LocalDateTime departure = dto != null ? dto.getDepartureTime() : null;
    String ngay = departure == null ? "--" : departure.format(DATE);
    String gio = departure == null ? "--" : departure.format(TIME);

    VBox info = new VBox(6,
        line("Tàu/Train: ", safe(dto.getTrainCode())),
        line("Ngày đi/Date: ", ngay),
        line("Giờ đi/Time: ", gio),
        line("Toa/Coach: ", safe(dto.getCarriageName())),
        line("Chỗ/Seat: ", safe(dto.getSeatNumber())),
        line("Loại chỗ/Class: ", dto != null && dto.getSeatType() != null ? dto.getSeatType().getName() : "--"),
        line("Loại vé/Type: ", dto != null && dto.getTicketType() != null ? dto.getTicketType().name() : "--"),
        line("Họ tên/Name: ", safe(dto.getPassengerName())),
        line("Giấy tờ/Passport: ", safe(dto.getPassengerDocument()))
    );

    if (dto != null && dto.isChildUnder6()) {
      info.getChildren().add(line("Đi kèm vé người lớn: ", safe(dto.getAccompanyAdultTicketId())));
      info.getChildren().add(line("Ghi chú: ", "Trẻ <6 miễn phí, không chiếm ghế"));
    }

    Label price = new Label("Giá/Price: " + formatMoney(dto != null ? dto.getPrice() : 0));
    price.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #c0392b;");

    Label note = new Label("Vui lòng mang theo giấy tờ tùy thân khi lên tàu.");
    note.setStyle("-fx-font-size: 11px; -fx-text-fill: #4a5568; -fx-font-style: italic;");
    note.setWrapText(true);

    root.getChildren().addAll(header, qrBox, ticketId, fromTo, info, price, note);
    return root;
  }

  private static VBox kv(String k, String v, boolean boldValue) {
    Label kLbl = new Label(k);
    kLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #4a5568;");
    Label vLbl = new Label(v);
    vLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: " + (boldValue ? "800" : "400") + ";");
    return new VBox(2, kLbl, vLbl);
  }

  private static Label line(String label, String value) {
    Label l = new Label(label + value);
    l.setStyle("-fx-font-size: 12px;");
    return l;
  }

  private static Region spacer() {
    Region r = new Region();
    HBox.setHgrow(r, javafx.scene.layout.Priority.ALWAYS);
    return r;
  }

  private static String safe(String v) {
    return v == null || v.isBlank() ? "--" : v;
  }

  private static String formatMoney(double v) {
    return MONEY.format(Math.round(v)) + " đ";
  }
}
