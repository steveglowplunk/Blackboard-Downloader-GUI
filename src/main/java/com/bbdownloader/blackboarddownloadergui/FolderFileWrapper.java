package com.bbdownloader.blackboarddownloadergui;

import java.util.ArrayList;

public abstract class FolderFileWrapper {
    public abstract String toString();

    public abstract String getUrl();

    public abstract String getDisplayedName();

    public abstract String getServerFileName();

    public abstract void setDisplayedName(String name);

    public abstract boolean isFolder();

    public abstract ArrayList<FileNode> getFileList();

    public abstract ArrayList<AssignmentNode> getAssignmentList();

    public abstract ArrayList<FolderNode> getFolderNodeList();

    public abstract String getFolderPath();

    public abstract int getFileSizeInBytes();

    public abstract void setFileSizeInBytes(int sizeInBytes);
}
