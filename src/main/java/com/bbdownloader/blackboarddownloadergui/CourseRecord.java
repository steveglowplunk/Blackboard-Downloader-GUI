package com.bbdownloader.blackboarddownloadergui;

public class CourseRecord {
    private boolean isAvailable;
    private String displayName;
    private String homePageUrl;

    public CourseRecord(boolean isAvailable, String displayName, String homePageUrl) {
        this.isAvailable = isAvailable;
        this.displayName = displayName;
        this.homePageUrl = "https://blackboard.cuhk.edu.hk" + homePageUrl;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHomePageUrl() {
        return homePageUrl;
    }

    public String toString() {
        return getDisplayName();
    }

    public String detailedInfo() {
        return "Name: " + getDisplayName() + ", isAvailable: " + isAvailable() + ", homePageUrl: " + getHomePageUrl();
    }
}
