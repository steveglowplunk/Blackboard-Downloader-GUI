package com.bbdownloader.blackboarddownloadergui;

import javafx.scene.control.Alert;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;

public class CommonUtils {
    public void customWarning(String headerContent) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Error");
        alert.setHeaderText(headerContent);
        alert.showAndWait();
    }

    public double calcPercentage(double width, double ratio, double padding, String msg) {
        double temp = width * (1 - ratio) + padding;
//        if (!msg.isEmpty()) {
//            System.out.println(msg + ": " + temp);
//        }
        return temp;
    }

    public boolean hasConnection(String destinationURL, HashMap<String, String> loadedCookies) throws IOException {
        try {
            Connection.Response response =
                    Jsoup.connect(destinationURL).cookies(loadedCookies).timeout(10000).followRedirects(false).ignoreContentType(true).execute();
//            System.out.println("check if internet available : " + response.statusCode());
            boolean bValidConnection = (response.statusCode() != 302);
            if (!bValidConnection) {
                customWarning("Unexpected HTML response code received. Response code: " + response.statusCode());
            }
            return bValidConnection; // status codes other than 302 may indicate valid connection
        } catch (UnknownHostException | HttpStatusException e) {
            customWarning("Blackboard unreachable. Possibly cookies have expired, or there is no Internet connection");
            return false;
        }
    }
}
