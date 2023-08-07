package com.bbdownloader.blackboarddownloadergui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import me.marnic.jdl.CombinedSpeedProgressDownloadHandler;
import me.marnic.jdl.Downloader;
import me.marnic.jdl.SizeUtil;
import org.controlsfx.control.CheckTreeView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MainController {
    @FXML
    private RadioMenuItem radioMenuItem_AskDownloadPath;

    @FXML
    public AnchorPane anchorPane_MainWindow;

    @FXML
    private AnchorPane anchorPane_MainContentContainer;

    @FXML
    private AnchorPane anchorPane_CourseContentContainer;

    @FXML
    private VBox vbox_ButtonList;

    @FXML
    private VBox vbox_CourseTreeHolder;

    @FXML
    private Button btn_LoadCourse;

    private File cookieFile;
    private String cookiesLocation;
    private Downloader downloader;
    private final String domainFilter = "blackboard.cuhk.edu.hk";
    private HashMap<String, String> loadedCookies;
    private CommonUtils commonUtils = new CommonUtils();
    private JSONObject userInfo, courseListObj;
    private JSONArray courseList;
    private String userID;
    private ChangeListener<Scene> windowExistsListener;
    private ChangeListener<Number> windowSizeListener_MainContentContainer;
    private String selectedHomePageURL, selectedCourseName;


//    -------------------------------------
    // MainContentContainer
//    -------------------------------------


    public void initialize() {
        // set up UI component resize by percentage
        hBox_RetrieveStatus.setVisible(false);

        windowSizeListener_MainContentContainer = (observable2, oldValue2, newValue2) -> {
            updateWidthConstraints_MainContentContainer(newValue2.doubleValue());
            updateWidthConstraints_CourseContentContainer(newValue2.doubleValue());
        };
        windowExistsListener = new ChangeListener<>() {
            @Override
            public void changed(ObservableValue observable, Scene oldScene, Scene newScene) {
                if (newScene != null) {
                    anchorPane_MainWindow.getScene().widthProperty().addListener(windowSizeListener_MainContentContainer);
                }
            }
        };
        if (anchorPane_MainContentContainer != null) {
            anchorPane_MainWindow.sceneProperty().addListener(windowExistsListener);
        }
    }

    private void updateWidthConstraints_MainContentContainer(double width) {
        AnchorPane.setRightAnchor(vbox_CourseTreeHolder, commonUtils.calcPercentage(width, 0.6, 10,
                "vbox_CourseTreeHolder"));
        AnchorPane.setLeftAnchor(vbox_ButtonList, commonUtils.calcPercentage(width, 0.4, 20, "vbox_ButtonList"));
    }

    private void updateWidthConstraints_CourseContentContainer(double width) {
        AnchorPane.setRightAnchor(vbox_CourseContentTreeHolder, commonUtils.calcPercentage(width, 0.6, 10,
                "vbox_CourseContentTreeHolder"));
        AnchorPane.setRightAnchor(hbox_CourseContentHeader, commonUtils.calcPercentage(width, 0.6, 10,
                "hbox_CourseContentHeader"));
        AnchorPane.setLeftAnchor(vbox_ButtonList_CourseContentContainer, commonUtils.calcPercentage(width, 0.4, 20,
                "vbox_ButtonList_CourseContentContainer"));
    }

    @FXML
    void on_menuItem_OpenCookie_clicked(ActionEvent event) throws IOException {
        // load cookies into downloader object and retrieve course list
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("txt files", "*.txt"));
        cookieFile = fc.showOpenDialog(null);

        if (cookieFile != null) {
            cookiesLocation = cookieFile.getAbsolutePath();
            downloader = new Downloader(true, cookiesLocation, domainFilter);
            loadedCookies = downloader.getCookiesMap();
            if (!loadedCookies.isEmpty()) {
                if (!commonUtils.hasConnection("https://blackboard.cuhk.edu.hk/learn/api/v1/users/me", loadedCookies)) {
                    return;
                }
                String tempDownloadJSONString = downloader.downloadJSONString("https://blackboard.cuhk.edu" +
                        ".hk/learn/api/v1/users/me");
                if (tempDownloadJSONString != null && !tempDownloadJSONString.isEmpty()) {
                    userInfo = new JSONObject(tempDownloadJSONString);
                    userID = userInfo.get("id").toString();
                    String courseListURL = "https://blackboard.cuhk.edu.hk/learn/api/v1/users/" + userID +
                            "/memberships?expand=course.effectiveAvailability,course.permissions," +
                            "courseRole&includeCount=true&limit=10000";
                    courseListObj = new JSONObject(downloader.downloadJSONString(courseListURL));
                    courseList = courseListObj.getJSONArray("results");
                    ArrayList<CourseRecord> courseRecords = new ArrayList<>();
                    for (int i = 0; i < courseList.length(); i++) {
                        courseRecords.add(new CourseRecord(courseList.getJSONObject(i).getJSONObject("course").get(
                                "isAvailable").toString().equals("true"), courseList.getJSONObject(i).getJSONObject(
                                        "course").get("displayName").toString(),
                                courseList.getJSONObject(i).getJSONObject("course").get("homePageUrl").toString()));
                    }

                    ListView<CourseRecord> listView_courseTree = new ListView<>();

                    for (CourseRecord courseRecord : courseRecords) {
                        if (courseRecord.isAvailable()) {
                            listView_courseTree.getItems().add(courseRecord);
                        }
                    }

                    // listen for item selection
                    listView_courseTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue,
                                                                                                newValue) -> {
                        CourseRecord selectedCourseRecord = listView_courseTree.getSelectionModel().getSelectedItem();
//                        System.out.println(selectedCourseRecord.detailedInfo());
                        selectedHomePageURL = selectedCourseRecord.getHomePageUrl();
                        selectedCourseName = selectedCourseRecord.getDisplayName();
                        btn_LoadCourse.setDisable(false);
                    });

                    vbox_CourseTreeHolder.getChildren().clear();
                    VBox.setVgrow(listView_courseTree, Priority.ALWAYS);
                    vbox_CourseTreeHolder.getChildren().add(listView_courseTree);
                } else {
                    commonUtils.customWarning("Could not retrieve user information");
                }
            } else {
                commonUtils.customWarning("Invalid cookies file");
            }
        }
    }

    @FXML
    void on_menuItem_Close_clicked(ActionEvent event) {
        Stage stage = (Stage) anchorPane_MainWindow.getScene().getWindow();
        stage.close();
    }

    public void loadPage(int option) {
        switch (option) {
            case 0:
                anchorPane_CourseContentContainer.setVisible(false);
                anchorPane_MainContentContainer.setVisible(true);
                if (fileDownloadProgressStage != null) {
                    fileDownloadProgressStage.close();
                }
                break;
            case 1:
                anchorPane_MainContentContainer.setVisible(false);
                anchorPane_CourseContentContainer.setVisible(true);
                try {
                    on_CourseContentContainer_opened();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default: {
//                System.out.println("option out of bounds");
                break;
            }
        }

    }

    @FXML
    void on_btn_LoadCourse_clicked(ActionEvent event) throws IOException {
        loadPage(1);
    }

//    -------------------------------------
    // CourseContentContainer
//    -------------------------------------

    @FXML
    private Button btn_DownloadFiles;

    @FXML
    private VBox vbox_ButtonList_CourseContentContainer;

    @FXML
    private VBox vbox_CourseContentTreeHolder;

    @FXML
    private TextField textField_SelectedDownloadPath;

    @FXML
    private TextField textField_SelectedItemName;

    @FXML
    private TextField textField_SelectedURL;

    @FXML
    private HBox hBox_RetrieveStatus;

    @FXML
    private HBox hbox_CourseContentHeader;

    @FXML
    private Label label_NumOfSelectedItems;

    @FXML
    private Pane pane_veil;

    private String downloadPath;
    private ArrayList<String> visitedFolderLinks = new ArrayList<>();
    private FolderNode rootFolderNode;
    private CheckBoxTreeItem<FolderFileWrapper> ti_courseContentRoot;
    private ArrayList<FolderFileWrapper> filesToDownloadList = new ArrayList<>();
    private Service loadCourseContentService;
    private Service downloadFileService;
    private DownloadProgressController downloadProgressController;
    private Stage fileDownloadProgressStage;
    private boolean bDirectoryChooserCancelled = false;

    void on_CourseContentContainer_opened() throws IOException {
        hBox_RetrieveStatus.setVisible(true);
        vbox_CourseContentTreeHolder.getChildren().get(0).setDisable(true);

        loadCourseContentService = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() throws IOException {
                        visitedFolderLinks.clear();

                        // extract folders and files from course home page
                        rootFolderNode = new FolderNode(selectedHomePageURL, selectedCourseName);
                        visitedFolderLinks.add(selectedHomePageURL);
                        traverseFolder(rootFolderNode, ""); // populate rootFolderNode
                        ti_courseContentRoot = new CheckBoxTreeItem<>(rootFolderNode);
                        ti_courseContentRoot.setExpanded(true);

                        if (rootFolderNode.getFolderNodeList().size() != 0) {
                            for (FolderNode tempFolderNode : rootFolderNode.getFolderNodeList()) {
                                // populate ti_courseContentRoot using rootFolderNode
                                createFolderTree(ti_courseContentRoot, tempFolderNode);
                            }

                            CheckTreeView<FolderFileWrapper> cTreeView_fileTree =
                                    new CheckTreeView<>(ti_courseContentRoot);
                            cTreeView_fileTree.getCheckModel().getCheckedItems().addListener((ListChangeListener<TreeItem<FolderFileWrapper>>) c -> {
                                ObservableList<TreeItem<FolderFileWrapper>> checkedItemList =
                                        cTreeView_fileTree.getCheckModel().getCheckedItems();
                                filesToDownloadList.clear();
                                for (TreeItem<FolderFileWrapper> tempCheckedItem : checkedItemList) {
                                    if (!tempCheckedItem.getValue().isFolder()) { // if checked item is file
                                        filesToDownloadList.add(tempCheckedItem.getValue());
                                    }
                                }
                                label_NumOfSelectedItems.setText(filesToDownloadList.size() + " Files Selected");
//                                System.out.println(filesToDownloadList);
                            });
                            cTreeView_fileTree.getSelectionModel().selectedItemProperty().addListener((observable,
                                                                                                       oldValue,
                                                                                                       newValue) -> {
                                Platform.runLater(() -> {
                                    textField_SelectedItemName.setText(newValue.getValue().getDisplayedName());
                                    textField_SelectedURL.setText(newValue.getValue().getUrl());
                                    ;
                                });
                            });

                            Platform.runLater(() -> {
                                vbox_CourseContentTreeHolder.getChildren().clear();
                                VBox.setVgrow(cTreeView_fileTree, Priority.ALWAYS);
                                vbox_CourseContentTreeHolder.getChildren().add(cTreeView_fileTree);
                                vbox_CourseContentTreeHolder.getChildren().get(0).setDisable(false);
                            });
                        } else {
                            Platform.runLater(() -> {
                                commonUtils.customWarning(selectedCourseName + " has no folders or files");
                            });
                        }

//                        System.out.println("traverse folders finished");
                        hBox_RetrieveStatus.setVisible(false);
                        return null;
                    }
                };
            }
        };

        loadCourseContentService.start();
    }

    private void traverseFolder(FolderNode folderNode, String lastLevelFolderName) throws IOException {
        visitedFolderLinks.add(folderNode.getFolderURL());
        String currentLevelFolderName = lastLevelFolderName + folderNode.getFolderName() + "\\"; // TODO: PLATFORM
        // DEPENDENT

        Document doc = null;

        try {
            doc = Jsoup.connect(folderNode.getFolderURL()).cookies(loadedCookies).get();
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
//            System.out.println("URL provided is invalid");
        }

        Elements linkElements = doc.select("a");

        for (Element linkElement : linkElements) {
            String absHref = linkElement.attr("abs:href");
            if (absHref.contains("bbcswebdav")) { // indicates files
                String serverFileName = "dummy server file name";
                String displayedName = linkElement.text();
                folderNode.getFileList().add(new FileNode(absHref, serverFileName, displayedName,
                        currentLevelFolderName));
            } else if (absHref.contains("listContent")) { // indicates folders
                boolean bVisited = false;
                for (String tempURL : visitedFolderLinks) { // prevent visiting old links to prevent duplicates
                    if (absHref.contains(tempURL)) {
                        bVisited = true;
                        break;
                    }
                }
                if (!bVisited) {
                    folderNode.getFolderNodeList().add(new FolderNode(absHref, linkElement.text()));
                    visitedFolderLinks.add(absHref);
                }
            }
        }

//        System.out.println("Traversal finished for " + folderNode.getFolderName());
        for (int i = 0; i < folderNode.getFolderNodeList().size(); i++) {
//            System.out.println("Traversing " + folderNode.getFolderNodeList().get(i).getFolderURL() + ", " +
//            folderNode.getFolderNodeList().get(i).getFolderName());
            traverseFolder(folderNode.getFolderNodeList().get(i), currentLevelFolderName);
        }
    }

    private void createFolderTree(CheckBoxTreeItem<FolderFileWrapper> checkBoxTreeItem,
                                  FolderFileWrapper folderOrFile) {
        if (!folderOrFile.isFolder()) { // if folderOrFile is file
            checkBoxTreeItem.getChildren().add(new CheckBoxTreeItem<>(folderOrFile)); // add file
        } else { // if folderOrFile is folder
            CheckBoxTreeItem<FolderFileWrapper> currentCheckBoxTreeItem = new CheckBoxTreeItem<>(folderOrFile);
            currentCheckBoxTreeItem.setExpanded(true);
            checkBoxTreeItem.getChildren().add(currentCheckBoxTreeItem); // add current folder
            for (FileNode tempFileNode : folderOrFile.getFileList()) {
                currentCheckBoxTreeItem.getChildren().add(new CheckBoxTreeItem<>(tempFileNode)); // add files, if any
            }
            for (FolderNode tempFolderNode : folderOrFile.getFolderNodeList()) {
                createFolderTree(currentCheckBoxTreeItem, tempFolderNode); // traverse deeper into sub-folders, if any
            }
        }
    }

    @FXML
    void on_btn_DownloadFiles_clicked(ActionEvent event) {
        if (filesToDownloadList.isEmpty()) {
            commonUtils.customWarning("No files selected for downloading");
            return;
        }

        // ask download path every time
        if (radioMenuItem_AskDownloadPath.isSelected()) {
            on_btn_SelectDownloadPath_clicked(null);
            if (bDirectoryChooserCancelled) {
                return;
            }
        }

        downloader.setbIsCancelled(false);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("download-progress-view.fxml"));
        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        downloadProgressController = loader.getController();

        fileDownloadProgressStage = new Stage();
        fileDownloadProgressStage.setScene(new Scene(root));
        fileDownloadProgressStage.setTitle("File Download Progress");
        fileDownloadProgressStage.setResizable(false);

        if (downloadProgressController != null) {
            downloadProgressController.setTotalNumOfFiles(filesToDownloadList.size());
            downloadProgressController.setCurrentFileIndex(0);
            downloadProgressController.updateLabel_numOfFiles();
            downloadProgressController.isCancelledProperty().addListener((observable, oldValue, newValue) -> {
                // listen for cancel button broadcast from downloadProgressController
                downloader.setbIsCancelled(newValue);
                downloadFileService.cancel();
            });
        }

        fileDownloadProgressStage.show();
        // disable background window when progress window is showing
        pane_veil.visibleProperty().bind(fileDownloadProgressStage.showingProperty());

        handleDownloadFile();
    }

    private void handleDownloadFile() {
        downloadFileService = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() {
                        if (downloadProgressController == null) {
//                            System.out.println("download controller is null");
                            return null;
                        }
                        for (int i = 0; i < filesToDownloadList.size(); i++) {
                            if (isCancelled()) {
                                downloadProgressController.disableBtn_abort(true);
                                downloadProgressController.disableBtn_close(false);
                                downloadProgressController.setLabel_status("Download cancelled");
                                return null;
                            }

                            String absHref = filesToDownloadList.get(i).getUrl();
                            String serverFileName = downloader.getServerFileName(absHref);

                            downloadProgressController.setCurrentFileIndex(i + 1);
                            downloadProgressController.setDownloadProgress(0);

                            downloader.setDownloadHandler(new CombinedSpeedProgressDownloadHandler(downloader) {
                                @Override
                                public void onDownloadStart() {
                                    super.onDownloadStart();
                                    downloadProgressController.setLabel_FileName(serverFileName);
                                    downloadProgressController.updateLabel_numOfFiles();
                                    downloadProgressController.setLabel_status("Download in progress...");
                                }

                                @Override
                                public void onDownloadSpeedProgress(int downloaded, int maxDownload, int percent,
                                                                    int bytesPerSec) {
                                    if (isCancelled()) {
                                        downloader.setbIsCancelled(true);
                                    }
                                    String speedText = SizeUtil.toMBFB(bytesPerSec) + " MB/s";
                                    Platform.runLater(() -> {
                                        downloadProgressController.setLabel_Speed(speedText);
                                        downloadProgressController.setLabel_percentage(percent + "%");
                                        downloadProgressController.setDownloadProgress((double) percent / 100);
                                    });
                                }

                                @Override
                                public void onDownloadFinish() {
                                    super.onDownloadFinish();
                                    Platform.runLater(() -> {
                                        downloadProgressController.setLabel_percentage("100%");
                                        downloadProgressController.setDownloadProgress(1);
                                    });
                                }
                            });

                            downloader.downloadFileToLocation(absHref,
                                    downloadPath + filesToDownloadList.get(i).getFolderPath());
                        }
                        if (!isCancelled()) {
                            downloadProgressController.setLabel_status("Download finished");
                        } else {
                            downloadProgressController.setLabel_status("Download cancelled");
                        }
                        downloadProgressController.disableBtn_close(false);
                        downloadProgressController.disableBtn_abort(true);
                        return null;
                    }
                };
            }
        };

        downloadFileService.start();
    }

    @FXML
    void on_btn_OpenCourseList_clicked(ActionEvent event) {
        loadPage(0);
    }

    @FXML
    void on_btn_SelectDownloadPath_clicked(ActionEvent event) {
        bDirectoryChooserCancelled = false;
        DirectoryChooser dc = new DirectoryChooser();
        File downloadPath_file;
        downloadPath_file = dc.showDialog(null);
        if (downloadPath_file != null) {
            downloadPath = downloadPath_file.getAbsolutePath() + "\\"; // TODO: PLATFORM DEPENDENT
            textField_SelectedDownloadPath.setText(downloadPath);
            btn_DownloadFiles.setDisable(false);
        } else {
            bDirectoryChooserCancelled = true;
        }
    }

    @FXML
    void on_menuItem_about_clicked(ActionEvent event) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("about-view.fxml"));
        Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Stage aboutStage = new Stage();
        aboutStage.setScene(new Scene(root));
        aboutStage.setTitle("About");
        aboutStage.setResizable(false);

        aboutStage.show();
    }
}