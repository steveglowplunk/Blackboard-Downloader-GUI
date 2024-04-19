package com.bbdownloader.blackboarddownloadergui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

    enum Pages {
        COURSE_LIST, COURSE_CONTENT
    }

    private Downloader downloader;
    private final CommonUtils commonUtils = new CommonUtils();
    private String selectedHomePageURL, selectedCourseName;

//    -------------------------------------
    // MainContentContainer
//    -------------------------------------

    public void initialize() {
        // set up UI component resize by percentage
        hBox_RetrieveStatus.setVisible(false);

        ChangeListener<Number> windowSizeListener_MainContentContainer = (observable2, oldValue2, newValue2) -> {
            updateWidthConstraints_MainContentContainer(newValue2.doubleValue());
            updateWidthConstraints_CourseContentContainer(newValue2.doubleValue());
        };
        ChangeListener<Scene> windowExistsListener = new ChangeListener<>() {
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
        AnchorPane.setRightAnchor(vbox_CourseTreeHolder, commonUtils.calcPercentage(width, 0.6, 10, "vbox_CourseTreeHolder"));
        AnchorPane.setLeftAnchor(vbox_ButtonList, commonUtils.calcPercentage(width, 0.4, 20, "vbox_ButtonList"));
    }

    private void updateWidthConstraints_CourseContentContainer(double width) {
        AnchorPane.setRightAnchor(vbox_CourseContentTreeHolder, commonUtils.calcPercentage(width, 0.6, 10, "vbox_CourseContentTreeHolder"));
        AnchorPane.setRightAnchor(hbox_CourseContentHeader, commonUtils.calcPercentage(width, 0.6, 10, "hbox_CourseContentHeader"));
        AnchorPane.setLeftAnchor(vbox_ButtonList_CourseContentContainer, commonUtils.calcPercentage(width, 0.4, 20, "vbox_ButtonList_CourseContentContainer"));
        AnchorPane.setLeftAnchor(hbox_bottom_CourseContentContainer, commonUtils.calcPercentage(width, 0.4, 20, "hbox_bottom_CourseContentContainer"));
    }

    void handleCookieFileLoading(File cookieFile) throws IOException {
        if (cookieFile != null) {
            downloader = new Downloader(true);
            downloader.setDomainFilter("blackboard.cuhk.edu.hk");  // ensures cookies file comes from blackboard
            downloader.setCookies(cookieFile);
            if (!downloader.getCookiesMap().isEmpty()) {
                if (!commonUtils.hasConnection("https://blackboard.cuhk.edu.hk/learn/api/v1/users/me", downloader.getCookiesMap(), true)) {
                    return;
                }
                String tempDownloadJSONString = downloader.downloadJSONString(
                        "https://blackboard.cuhk.edu.hk/learn/api/v1/users/me"
                );
                if (tempDownloadJSONString != null && !tempDownloadJSONString.isEmpty()) {
                    JSONObject userInfo = new JSONObject(tempDownloadJSONString);
                    String userID = userInfo.get("id").toString();
                    String courseListURL =
                            "https://blackboard.cuhk.edu.hk/learn/api/v1/users/" + userID +
                                    "/memberships?expand=course.effectiveAvailability,course.permissions,courseRole" +
                                    "&includeCount=true&limit=10000";
                    JSONObject courseListObj = new JSONObject(downloader.downloadJSONString(courseListURL));
                    JSONArray courseList = courseListObj.getJSONArray("results");
                    ArrayList<CourseRecord> courseRecords = new ArrayList<>();
                    for (int i = 0; i < courseList.length(); i++) {
                        courseRecords.add(new CourseRecord(
                                courseList.getJSONObject(i).getJSONObject("course").get("isAvailable").toString().equals("true"),
                                courseList.getJSONObject(i).getJSONObject("course").get("displayName").toString(),
                                courseList.getJSONObject(i).getJSONObject("course").get("homePageUrl").toString())
                        );
                    }

                    ListView<CourseRecord> listView_courseTree = new ListView<>();

                    for (CourseRecord courseRecord : courseRecords) {
                        if (courseRecord.isAvailable()) {
                            listView_courseTree.getItems().add(courseRecord);
                        }
                    }

                    // listen for list item selection
                    listView_courseTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                        CourseRecord selectedCourseRecord = listView_courseTree.getSelectionModel().getSelectedItem();
                        selectedHomePageURL = selectedCourseRecord.getHomePageUrl();
                        selectedCourseName = selectedCourseRecord.getDisplayName();
                        btn_LoadCourse.setDisable(false);
                    });

                    // replace placeholder with tree in UI
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
    void on_menuItem_OpenCookie_clicked(ActionEvent event) {
        // load cookies into downloader object and retrieve course list
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("txt files", "*.txt"));
        File cookieFile = fc.showOpenDialog(null);
        try {
            handleCookieFileLoading(cookieFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void on_btn_LoadCookiesFile_dragover(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
        } else {
            event.consume();
        }
    }

    @FXML
    void on_btn_LoadCookiesFile_dropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            System.out.println(db.getFiles().get(0));
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
        File cookieFile = db.getFiles().get(0);
        try {
            handleCookieFileLoading(cookieFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void on_menuItem_Close_clicked(ActionEvent event) {
        Stage stage = (Stage) anchorPane_MainWindow.getScene().getWindow();
        stage.close();
    }

    private void loadPage(Pages option) {
        switch (option) {
            case COURSE_LIST -> {
                anchorPane_CourseContentContainer.setVisible(false);
                anchorPane_MainContentContainer.setVisible(true);
                if (fileDownloadProgressStage != null) {
                    fileDownloadProgressStage.close();
                }
            }
            case COURSE_CONTENT -> {
                anchorPane_MainContentContainer.setVisible(false);
                anchorPane_CourseContentContainer.setVisible(true);
                try {
                    on_CourseContentContainer_opened();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @FXML
    void on_btn_LoadCourse_clicked(ActionEvent event) {
        loadPage(Pages.COURSE_CONTENT);
    }

//    -------------------------------------
    // CourseContentContainer
//    -------------------------------------

    @FXML
    private Button btn_DownloadFiles;

    @FXML
    private Button btn_RefreshTotalFileSize;

    @FXML
    private Button btn_GetAllFileSize;

    @FXML
    private VBox vbox_ButtonList_CourseContentContainer;

    @FXML
    private VBox vbox_CourseContentTreeHolder;

    @FXML
    private TextField textField_SelectedDownloadPath;

    @FXML
    private TextField textField_SelectedDisplayedName;

    @FXML
    private TextField textField_SelectedServerName;

    @FXML
    private TextField textField_SelectedURL;

    @FXML
    private TextField textField_SelectedFileSize;

    @FXML
    private TextField textField_TotalFileSize;

    @FXML
    private HBox hBox_RetrieveStatus;

    @FXML
    private HBox hbox_CourseContentHeader;

    @FXML
    private HBox hbox_bottom_CourseContentContainer;

    @FXML
    private Label label_NumOfSelectedItems;

    @FXML
    private Label label_RetrievingText;

    @FXML
    private Pane pane_veil;

    @FXML
    private CheckBox checkbox_IncludeAssignments;

    private String downloadPath;
    private final ArrayList<String> visitedFolderLinks = new ArrayList<>();
    private FolderNode rootFolderNode;
    private CheckBoxTreeItem<FolderFileWrapper> ti_courseContentRoot;
    private final ArrayList<FolderFileWrapper> filesToDownloadList = new ArrayList<>();
    private Service<Void> downloadFileService;
    private DownloadProgressController downloadProgressController;
    private Stage fileDownloadProgressStage;
    private boolean bDirectoryChooserCancelled = false;
    FolderFileWrapper selectedFolderOrFile;
    Service<Void> loadSingleFileInfoService = new Service<Void>() {
        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() {
                    textField_SelectedServerName.setText("Loading...");
                    if (!selectedFolderOrFile.isFolder()) {
                        String fileSize;
                        if (selectedFolderOrFile.getFileSizeInBytes() == -1) {
                            textField_SelectedFileSize.setText("Loading...");
                            fileSize = SizeUtil.toHumanReadableFromBytes(downloader.getDownloadLength(selectedFolderOrFile.getUrl()));
                        } else {
                            fileSize = SizeUtil.toHumanReadableFromBytes(selectedFolderOrFile.getFileSizeInBytes());
                        }
                        textField_SelectedFileSize.setText(fileSize);
                        String serverName = downloader.getServerFileName(selectedFolderOrFile.getUrl());
                        textField_SelectedServerName.setText(serverName);
                    } else {
                        textField_SelectedFileSize.setText("No size");
                        textField_SelectedServerName.setText(textField_SelectedDisplayedName.getText());  // folders only have one name
                    }
                    return null;
                }
            };
        }
    };

    void on_CourseContentContainer_opened() throws InterruptedException, IOException {
        reset_courseContent_pageValues();
        vbox_CourseContentTreeHolder.getChildren().get(0).setDisable(true);  // disable file tree UI until finished loading
        if (!commonUtils.hasConnection("https://blackboard.cuhk.edu.hk/learn/api/v1/users/me", downloader.getCookiesMap(), true)) {
            return;
        }
        hBox_RetrieveStatus.setVisible(true);

        Service<Void> loadCourseContentService = new Service<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() {
                        visitedFolderLinks.clear();

                        // extract folders and files from course home page
                        rootFolderNode = new FolderNode(selectedHomePageURL, selectedCourseName);
                        visitedFolderLinks.add(selectedHomePageURL);
                        traverseFolder(rootFolderNode, ""); // populate rootFolderNode
                        ti_courseContentRoot = new CheckBoxTreeItem<>(rootFolderNode);
                        ti_courseContentRoot.setExpanded(true);

                        if (!rootFolderNode.getFolderNodeList().isEmpty()) {
                            for (FolderNode tempFolderNode : rootFolderNode.getFolderNodeList()) {
                                // populate ti_courseContentRoot by traversing each folder under rootFolderNode
                                createFolderTree(ti_courseContentRoot, tempFolderNode);
                            }

                            CheckTreeView<FolderFileWrapper> cTreeView_fileTree = new CheckTreeView<>(ti_courseContentRoot);
                            setFileTreeListeners(cTreeView_fileTree);

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

                        hBox_RetrieveStatus.setVisible(false);
                        return null;
                    }
                };
            }
        };

        loadCourseContentService.start();
    }

    private void setFileTreeListeners(CheckTreeView<FolderFileWrapper> cTreeView_fileTree) {
        // listen for checkboxes being checked
        cTreeView_fileTree.getCheckModel().getCheckedItems().addListener((ListChangeListener<TreeItem<FolderFileWrapper>>) c -> {
            ObservableList<TreeItem<FolderFileWrapper>> checkedItemList = cTreeView_fileTree.getCheckModel().getCheckedItems();
            filesToDownloadList.clear();
            for (TreeItem<FolderFileWrapper> tempCheckedItem : checkedItemList) {
                if (!tempCheckedItem.getValue().isFolder()) { // if checked item is file
                    filesToDownloadList.add(tempCheckedItem.getValue());
                }
            }
            label_NumOfSelectedItems.setText(filesToDownloadList.size() + " Files Selected");
            if (!filesToDownloadList.isEmpty() && (downloadPath != null && !downloadPath.isEmpty())) {
                btn_DownloadFiles.setDisable(false);
            }
        });

        // listen for list item selection
        cTreeView_fileTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedFolderOrFile = newValue.getValue();
            Platform.runLater(() -> {
                textField_SelectedDisplayedName.setText(selectedFolderOrFile.getDisplayedName());
                textField_SelectedURL.setText(selectedFolderOrFile.getUrl());
                loadSingleFileInfoService.restart();
            });
        });
    }

    private volatile boolean bLoadTotalFileSizeService_cancelled = false;

    private void loadTotalDownloadSize() {
        ExecutorService executor = Executors.newFixedThreadPool(10);  // get 10 file sizes at a time to avoid congestion

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < flattenedAllFileList.size(); i++) {
            final int index = i;
            Callable<Void> task = () -> {
                if (Thread.currentThread().isInterrupted() || bLoadTotalFileSizeService_cancelled) {
                    return null;
                }
//                if (!filesToDownloadList_copy.get(index).isFolder()) {
//                    downloadSizeInBytes.addAndGet(downloader.getDownloadLength(filesToDownloadList_copy.get(index).getUrl()));
//                }
                int individualFileSize = downloader.getDownloadLength(flattenedAllFileList.get(index).getUrl());
                flattenedAllFileList.get(index).setFileSizeInBytes(individualFileSize);
                flattenedAllFileList.get(index).setDisplayedName(flattenedAllFileList.get(index).getDisplayedName() +
                        " (" + SizeUtil.toHumanReadableFromBytes(individualFileSize) + ")");
                return null;
            };
            tasks.add(task);
        }

        try {
            executor.invokeAll(tasks);
            executor.shutdownNow();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            // create new file tree with new file names
            CheckTreeView<FolderFileWrapper> cTreeView_fileTree = new CheckTreeView<>(ti_courseContentRoot);

            setFileTreeListeners(cTreeView_fileTree);

            Platform.runLater(() -> {
                vbox_CourseContentTreeHolder.getChildren().clear();
                VBox.setVgrow(cTreeView_fileTree, Priority.ALWAYS);
                vbox_CourseContentTreeHolder.getChildren().add(cTreeView_fileTree);
                vbox_CourseContentTreeHolder.getChildren().get(0).setDisable(false);
            });

        } catch (InterruptedException e) {
            // shutdown previous executor when previous executor tasks are interrupted,
            // to allow new loadTotalFileSizeService to start
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void traverseFolder(FolderNode folderNode, String lastLevelFolderName) {
        visitedFolderLinks.add(folderNode.getFolderURL());
        String currentLevelFolderName = lastLevelFolderName + folderNode.getFolderName() + File.separator;

        Document doc = null;

        try {
            doc = Jsoup.connect(folderNode.getFolderURL()).cookies(downloader.getCookiesMap()).get();
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
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
            } else if (absHref.contains("uploadAssignment")) { // indicates assignments
                if (checkbox_IncludeAssignments.isSelected()) {
                    String displayedName = linkElement.text();
                    folderNode.getAssignmentList().add(new AssignmentNode(
                            absHref, displayedName, currentLevelFolderName
                    ));
                }
            }
        }

        if (checkbox_IncludeAssignments.isSelected()) {
            for (int i = 0; i < folderNode.getAssignmentList().size(); i++) {
                AssignmentNode childAssignmentNode = folderNode.getAssignmentList().get(i);
                traverseAssignment(childAssignmentNode);
            }
        }

        for (int i = 0; i < folderNode.getFolderNodeList().size(); i++) {
            FolderNode childFolderNode = folderNode.getFolderNodeList().get(i);
            traverseFolder(childFolderNode, currentLevelFolderName);
        }
    }

    private void traverseAssignment(AssignmentNode assignmentNode) {
        Document doc = null;

        try {
            doc = Jsoup.connect(assignmentNode.getUrl()).cookies(downloader.getCookiesMap()).get();
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
        }

        Elements linkElements = null;
        if (doc != null) {
            linkElements = doc.select("a");
        }

        for (Element linkElement : linkElements) {
            String absHref = linkElement.attr("abs:href");
            if (absHref.contains("assignment/download")) {
                try {
                    URL url = new URL(absHref);
                    String query = url.getQuery();
                    String serverFileName = null;
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("fileName=")) {
                            serverFileName = URLDecoder.decode(param.substring("fileName=".length()), "UTF-8");
                            break;
                        }
                    }
                    if (serverFileName != null) {
                        assignmentNode.getFileList().add(new FileNode(
                                absHref, serverFileName, serverFileName,
                                assignmentNode.getFolderPath() + assignmentNode.getDisplayedName() + File.separator
                        ));
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private final ArrayList<FolderFileWrapper> flattenedAllFileList = new ArrayList<>();

    private void createFolderTree(CheckBoxTreeItem<FolderFileWrapper> checkBoxTreeItem,
                                  FolderFileWrapper folderOrFile) {
        if (!folderOrFile.isFolder()) { // if folderOrFile is file
            checkBoxTreeItem.getChildren().add(new CheckBoxTreeItem<>(folderOrFile)); // add file
            flattenedAllFileList.add(folderOrFile);
        } else { // if folderOrFile is folder
            CheckBoxTreeItem<FolderFileWrapper> currentCheckBoxTreeItem = new CheckBoxTreeItem<>(folderOrFile);
            currentCheckBoxTreeItem.setExpanded(true);
            checkBoxTreeItem.getChildren().add(currentCheckBoxTreeItem); // add current folder
            for (FileNode tempFileNode : folderOrFile.getFileList()) {
                currentCheckBoxTreeItem.getChildren().add(new CheckBoxTreeItem<>(tempFileNode)); // add files, if any
                flattenedAllFileList.add(tempFileNode);
            }
            for (AssignmentNode tempAssignmentNode : folderOrFile.getAssignmentList()) {
                CheckBoxTreeItem<FolderFileWrapper> tempAssignmentCheckBoxTreeItem = new CheckBoxTreeItem<>(tempAssignmentNode);
                tempAssignmentCheckBoxTreeItem.setExpanded(true);
                currentCheckBoxTreeItem.getChildren().add(tempAssignmentCheckBoxTreeItem); // add assignments, if any
                for (FileNode tempFileNode : tempAssignmentNode.getFileList()) {
                    tempAssignmentCheckBoxTreeItem.getChildren().add(new CheckBoxTreeItem<>(tempFileNode)); // add files, if any
                    flattenedAllFileList.add(tempFileNode);
                }
            }
            for (FolderNode tempFolderNode : folderOrFile.getFolderNodeList()) {
                createFolderTree(currentCheckBoxTreeItem, tempFolderNode); // traverse deeper into sub-folders, if any
            }
        }
    }

    @FXML
    void on_btn_DownloadFiles_clicked(ActionEvent event) throws IOException {
        if (!commonUtils.hasConnection("https://blackboard.cuhk.edu.hk/learn/api/v1/users/me", downloader.getCookiesMap(), true)) {
            return;
        }

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
        fileDownloadProgressStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                if (bDownloadInProgress) {
                    // consume event
                    event.consume();

                    // show close dialog
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Abort Download Confirmation");
                    alert.setHeaderText("Closing the progress window will abort the ongoing download. Continue?");
                    alert.initOwner(fileDownloadProgressStage);

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.get() == ButtonType.OK) {
                        downloadProgressController.on_btn_abort_clicked(null);
                        downloadProgressController.on_btn_close_clicked(null);
                    }
                }
            }
        });

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

    private boolean bDownloadInProgress = false;

    private void handleDownloadFile() {
        downloadFileService = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() throws UnsupportedEncodingException {
                        if (downloadProgressController == null) {
                            return null;
                        }
                        bDownloadInProgress = true;
                        boolean bCheckDuplicatedFiles = true;
                        boolean bSkipAllDuplicatedFiles = false;
                        for (int i = 0; i < filesToDownloadList.size(); i++) {
                            if (isCancelled()) {
                                downloadProgressController.disableBtn_abort(true);
                                downloadProgressController.disableBtn_close(false);
                                bDownloadInProgress = false;
                                downloadProgressController.setLabel_status("Download cancelled");
                                return null;
                            }

                            String absHref = filesToDownloadList.get(i).getUrl();
                            String serverFileName = downloader.getServerFileName(absHref);

                            downloadProgressController.setCurrentFileIndex(i + 1);
                            downloadProgressController.setDownloadProgress(0);

                            String pathToDownload = downloadPath + filesToDownloadList.get(i).getFolderPath();
                            String pathToDownloadCheck = pathToDownload + serverFileName;
                            if (bCheckDuplicatedFiles) {
                                File checkFile = new File(pathToDownloadCheck);
                                if (checkFile.isFile() && !bSkipAllDuplicatedFiles) { // check if file exists
                                    AtomicReference<Optional<ButtonType>> result = new AtomicReference<>(Optional.empty());
                                    CompletableFuture<Void> future = new CompletableFuture<>();
                                    Platform.runLater(() -> {
                                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                                        alert.setTitle("Duplicated File");
                                        alert.setHeaderText("File already exists in the directory.");
                                        alert.setContentText("File: " + serverFileName + "\nPath: " + pathToDownloadCheck);
                                        alert.getButtonTypes().clear();
                                        alert.getButtonTypes().addAll(new ButtonType("Overwrite once"), new ButtonType("Overwrite all"),
                                            new ButtonType("Skip once"), new ButtonType("Skip all"), new ButtonType("Cancel Download"));

                                        // Make the alert resizable
                                        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                                        alert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
                                        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                                        stage.setResizable(true);

                                        result.set(alert.showAndWait());
                                        future.complete(null); // block until user makes a choice
                                    });

                                    future.join();

                                    if (result.get().isEmpty() || result.get().get().getText().equals("Cancel Download")) {
                                        Platform.runLater(() -> {
                                            downloadProgressController.disableBtn_abort(true);
                                            downloadProgressController.disableBtn_close(false);
                                            bDownloadInProgress = false;
                                            downloadProgressController.setLabel_status("Download cancelled");
                                            downloadProgressController.on_btn_abort_clicked(null);
                                        });
                                        break;
                                    } else if (result.get().get().getText().equals("Skip once")) {
                                        continue;
                                    } else if (result.get().get().getText().equals("Skip all")) {
                                        bSkipAllDuplicatedFiles = true;
                                        continue;
                                    } else if (result.get().get().getText().equals("Overwrite once")) {
                                        // do nothing
                                    } else if (result.get().get().getText().equals("Overwrite all")) {
                                        bCheckDuplicatedFiles = false;
                                    }
                                } else if (checkFile.isFile() && bSkipAllDuplicatedFiles) {
                                    continue;
                                }
                            }

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
                                        downloader.cancelDownload();
                                    }
                                    String speedText = SizeUtil.toHumanReadableFromBytes(bytesPerSec) + "/s";
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

                            if (!isCancelled()) {
                                downloader.downloadFileToLocation(
                                        absHref,
                                        pathToDownload,
                                        serverFileName);
                            }
                        }
                        if (!isCancelled()) {
                            downloadProgressController.setLabel_status("Download finished");
                        } else {
                            downloadProgressController.setLabel_status("Download cancelled");
                        }
                        bDownloadInProgress = false;
                        downloadProgressController.disableBtn_close(false);
                        downloadProgressController.disableBtn_abort(true);
                        return null;
                    }
                };
            }
        };

        downloadFileService.start();
    }

    Service<Void> loadTotalFileSizeService = new Service<Void>() {
        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() {
                    if (!flattenedAllFileList.isEmpty()) {
                        Platform.runLater(() -> {
                            label_RetrievingText.setText("Loading... (for " + flattenedAllFileList.size() + " files)");
                            hBox_RetrieveStatus.setVisible(true);
                        });
                        loadTotalDownloadSize();
                        Platform.runLater(() -> {
                            hBox_RetrieveStatus.setVisible(false);
                            btn_RefreshTotalFileSize.setDisable(false);
                        });
                    } else {
                        Platform.runLater(() -> {
                            commonUtils.customWarning("There is no file in this course");
                            vbox_CourseContentTreeHolder.getChildren().get(0).setDisable(false);
                        });
                    }
                    return null;
                }
            };
        }
    };

    private void cancelLoadTotalFileSizeService() throws InterruptedException {
        if (loadTotalFileSizeService.isRunning()) {
            loadTotalFileSizeService.cancel();
            bLoadTotalFileSizeService_cancelled = true;
            while (loadTotalFileSizeService.isRunning()) {
                // Wait for service to be cancelled
                Thread.sleep(100);
            }
            bLoadTotalFileSizeService_cancelled = false;
        }
    }

    private void reset_courseContent_pageValues() throws InterruptedException {
        cancelLoadTotalFileSizeService();
        flattenedAllFileList.clear();
        Platform.runLater(() -> {
            btn_RefreshTotalFileSize.setDisable(true);
            btn_GetAllFileSize.setDisable(false);
        });
        label_RetrievingText.setText("Retrieving file information...");
        textField_SelectedDisplayedName.setText("None Selected");
        textField_SelectedServerName.setText("None Selected");
        textField_SelectedURL.setText("None Selected");
        textField_SelectedFileSize.setText("None Selected");
        label_NumOfSelectedItems.setText("0 Files Selected");
        textField_TotalFileSize.setText("No data yet");

        filesToDownloadList.clear();
        btn_DownloadFiles.setDisable(true);
    }

    @FXML
    void on_btn_OpenCourseList_clicked(ActionEvent event) {
        // Shutdown all active threads in executorService
//        executorService.shutdownNow();
//        try {
//            // Wait for all threads to terminate
//            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
//                // Optional: Log or handle failure to stop threads.
//            }
//        } catch (InterruptedException e) {
//            // Optional: Log interruption.
//            executorService.shutdownNow();
//            Thread.currentThread().interrupt();
//        }
        loadPage(Pages.COURSE_LIST);
    }

    @FXML
    void on_btn_SelectDownloadPath_clicked(ActionEvent event) {
        bDirectoryChooserCancelled = false;
        DirectoryChooser dc = new DirectoryChooser();
        File downloadPath_file;
        downloadPath_file = dc.showDialog(null);
        if (downloadPath_file != null) {
            downloadPath = downloadPath_file.getAbsolutePath() + File.separator;
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

    @FXML
    void on_btn_RefreshTotalFileSize_clicked(ActionEvent event) {
        int totalSizeInBytes = 0;
        for (FolderFileWrapper tempFile : filesToDownloadList) {
            totalSizeInBytes += tempFile.getFileSizeInBytes();
        }
        textField_TotalFileSize.setText(SizeUtil.toHumanReadableFromBytes(totalSizeInBytes));
    }

    @FXML
    void on_btn_GetAllFileSize_clicked(ActionEvent event) throws InterruptedException {
        btn_GetAllFileSize.setDisable(true);
        cancelLoadTotalFileSizeService();
        vbox_CourseContentTreeHolder.getChildren().get(0).setDisable(true);  // disable file tree UI until finished loading
        loadTotalFileSizeService.restart();
    }

    @FXML
    void on_checkbox_IncludeAssignments_clicked(ActionEvent event) throws IOException, InterruptedException {
        on_CourseContentContainer_opened();
    }
}