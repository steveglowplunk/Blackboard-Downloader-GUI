package com.bbdownloader.blackboarddownloadergui;

import java.util.ArrayList;

public class FolderNode extends FolderFileWrapper {
    private ArrayList<FileNode> fileList = new ArrayList<>();
    private ArrayList<FolderNode> folderNodeList = new ArrayList<>();
    private ArrayList<AssignmentNode> assignmentList = new ArrayList<>();
    private String folderURL;
    private String folderName;

    public FolderNode(String folderURL, String folderName) {
        this.folderURL = folderURL;
        this.folderName = folderName;
    }

    @Override
    public ArrayList<FileNode> getFileList() {
        return fileList;
    }

    @Override
    public ArrayList<AssignmentNode> getAssignmentList() {
        return assignmentList;
    }

    @Override
    public ArrayList<FolderNode> getFolderNodeList() {
        return folderNodeList;
    }

    public String getFolderURL() {
        return folderURL;
    }

    @Override
    public String getUrl() {
        return getFolderURL();
    }

    public String getFolderName() {
        return folderName;
    }

    @Override
    public String getDisplayedName() {
        return getFolderName();
    }

    @Override
    public void setDisplayedName(String name) {
        this.folderName = name;
    }

    @Override
    public String toString() {
        return "\uD83D\uDCC1 " + getFolderName();
    }

    @Override
    public String getServerFileName() {
        return "Is folder";
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public String getFolderPath() {
        return "Is folder";
    }

    @Override
    public int getFileSizeInBytes() {
        return 0;
    }

    @Override
    public void setFileSizeInBytes(int sizeInBytes) {
        System.err.println("Folders cannot have size");
    }
}
