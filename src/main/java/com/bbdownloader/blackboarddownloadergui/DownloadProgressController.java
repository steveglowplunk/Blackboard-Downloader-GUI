package com.bbdownloader.blackboarddownloadergui;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

public class DownloadProgressController {

    @FXML
    private ProgressBar pBar_downloadProgress;

    @FXML
    private Label label_FileName;

    @FXML
    private Label label_Speed;

    @FXML
    private Label label_status;

    @FXML
    private Label label_numOfFiles;

    @FXML
    private Label label_percentage;

    @FXML
    private Button btn_close;

    @FXML
    private Button btn_abort;

    private int currentFileIndex, totalNumOfFiles;

    private final ReadOnlyObjectWrapper<Boolean> isCancelled = new ReadOnlyObjectWrapper<>(false);

    public void initialize() {
        Platform.runLater(() -> {
            label_status.setText("Starting download...");
            label_FileName.setText("No files are downloading");
            label_Speed.setText("0.00 MB/s");
            label_percentage.setText("0%");
            pBar_downloadProgress.setProgress(0);
            disableBtn_close(true);
            disableBtn_abort(false);
        });
    }

    public ReadOnlyObjectProperty<Boolean> isCancelledProperty() {
        return isCancelled.getReadOnlyProperty(); // broadcast cancelled status to parent controller
    }

    public void setCurrentFileIndex(int currentFileIndex) {
        this.currentFileIndex = currentFileIndex;
    }

    public void setTotalNumOfFiles(int totalNumOfFiles) {
        this.totalNumOfFiles = totalNumOfFiles;
    }

    public void updateLabel_numOfFiles() {
        Platform.runLater(() -> {
            label_numOfFiles.setText(currentFileIndex + " of " + totalNumOfFiles);
        });
    }

    public void setLabel_FileName(String input) {
        Platform.runLater(() -> {
            label_FileName.setText(input);
        });
    }

    public void setLabel_Speed(String input) {
        Platform.runLater(() -> {
            label_Speed.setText(input);
        });
    }

    public void setLabel_percentage(String input) {
        Platform.runLater(() -> {
            label_percentage.setText(input);
        });
    }

    public void setDownloadProgress(double progressValue) {
        Platform.runLater(() -> {
            pBar_downloadProgress.setProgress(progressValue);
        });
    }

    public void setLabel_status(String input) {
        Platform.runLater(() -> {
            label_status.setText(input);
        });
    }

    public void disableBtn_close(boolean bDisabled) {
        Platform.runLater(() -> {
            btn_close.setDisable(bDisabled);
        });
    }

    public void disableBtn_abort(boolean bDisabled) {
        Platform.runLater(() -> {
            btn_abort.setDisable(bDisabled);
        });
    }

    @FXML
    void on_btn_abort_clicked(ActionEvent event) {
        isCancelled.set(true);
    }

    @FXML
    void on_btn_close_clicked(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
