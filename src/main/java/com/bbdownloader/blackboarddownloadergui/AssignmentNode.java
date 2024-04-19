package com.bbdownloader.blackboarddownloadergui;

import java.util.ArrayList;

public class AssignmentNode extends FolderFileWrapper {
    private String assignmentURL;
    private String assignmentName;
    private ArrayList<FileNode> fileList = new ArrayList<>();
    private String folderPath;

    public AssignmentNode(String assignmentURL, String assignmentName, String folderPath) {
        this.assignmentURL = assignmentURL;
        this.assignmentName = assignmentName;
        this.folderPath = folderPath;
    }

    private String getAssignmentURL() {
        return assignmentURL;
    }

    @Override
    public String getUrl() {
        return getAssignmentURL();
    }

    private String getAssignmentName() {
        return assignmentName;
    }

    @Override
    public String getDisplayedName() {
        return getAssignmentName();
    }

    @Override
    public void setDisplayedName(String name) {
        this.assignmentName = name;
    }

    @Override
    public String toString() {
        return "\uD83D\uDCDD " + getAssignmentName();
    }

    @Override
    public String getServerFileName() {
        return "Is assignment";
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public ArrayList<FileNode> getFileList() {
        return fileList;
    }

    @Override
    public ArrayList<AssignmentNode> getAssignmentList() {
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

    @Override
    public int getFileSizeInBytes() {
        return 0;
    }

    @Override
    public void setFileSizeInBytes(int sizeInBytes) {
        System.err.println("Assignment page cannot have size");
    }
}
