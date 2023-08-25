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
        stage.setTitle("Blackboard Downloader GUI");
        stage.setWidth(900);
        stage.setHeight(550 + 30);
        stage.setMinWidth(900);
        stage.setMinHeight(550 + 30);
        stage.setScene(scene);

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