module com.bbdownloader.blackboarddownloadergui {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.jsoup;
    requires org.json;
    requires java.desktop;

    opens com.bbdownloader.blackboarddownloadergui to javafx.fxml;
    exports com.bbdownloader.blackboarddownloadergui;
}