package com.bbdownloader.blackboarddownloadergui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.setTitle("Blackboard Downloader GUI");
        if (System.getProperty("java.version").startsWith("1.8")) {
            // Java 8 window size is different somehow
            stage.setWidth(1100);
            stage.setHeight(650 + 30);
            stage.setMinWidth(1100);
            stage.setMinHeight(650 + 30);
        } else {
            // others
            stage.setWidth(900);
            stage.setHeight(550 + 30);
            stage.setMinWidth(900);
            stage.setMinHeight(550 + 30);
        }

        // on close, force cancel running concurrent services (e.g. download service)
        stage.setOnHidden(event -> {
            Platform.exit();
            System.exit(0);
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}