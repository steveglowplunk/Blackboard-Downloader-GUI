package com.bbdownloader.blackboarddownloadergui;

import java.util.ArrayList;

public abstract class FolderFileWrapper {
    public abstract String toString();

    public abstract String getUrl();

    public abstract String getDisplayedName();

    public abstract String getServerFileName();

    public abstract boolean isFolder();

    public abstract ArrayList<FileNode> getFileList();

    public abstract ArrayList<FolderNode> getFolderNodeList();

    public abstract String getFolderPath();
}
