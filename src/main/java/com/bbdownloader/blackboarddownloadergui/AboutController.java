package com.bbdownloader.blackboarddownloadergui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class AboutController {
    @FXML
    private Hyperlink hyperlink_githubLink;

    @FXML
    void on_hyperlink_githubLink_clicked(ActionEvent event) throws IOException {
        URI uri = URI.create(hyperlink_githubLink.getText());
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri);
            }
        }
    }
}
