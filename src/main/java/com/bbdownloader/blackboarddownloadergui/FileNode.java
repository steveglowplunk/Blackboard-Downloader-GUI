package com.bbdownloader.blackboarddownloadergui;

import java.util.ArrayList;

public class FileNode extends FolderFileWrapper {
    private String url;
    private String serverFileName;
    private String displayedName;
    private String folderPath;


    public FileNode(String url, String serverFileName, String displayedName, String folderPath) {
        this.url = url;
        this.serverFileName = serverFileName;
        this.displayedName = displayedName;
        this.folderPath = folderPath;
    }

    @Override
    public String toString() {
        return getDisplayedName();
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getDisplayedName() {
        return displayedName;
    }

    @Override
    public String getServerFileName() {
        return serverFileName;
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    @Override
    public ArrayList<FileNode> getFileList() {
        return null;
    }

    @Override
    public ArrayList<FolderNode> getFolderNodeList() {
        return null;
    }

    @Override
    public String getFolderPath() {
        return folderPath;
    }
}
