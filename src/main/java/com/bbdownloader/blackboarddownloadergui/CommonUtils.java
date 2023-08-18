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

    public boolean hasConnection(String destinationURL, HashMap<String, String> loadedCookies, boolean bShowGUIWarnings) throws IOException {
        try {
            Connection.Response response =
                    Jsoup.connect(destinationURL).cookies(loadedCookies).timeout(10000).followRedirects(false).ignoreContentType(true).execute();
            boolean bValidConnection = (response.statusCode() != 302);
            if (!bValidConnection && bShowGUIWarnings) {
                customWarning("Unexpected HTML response code received. Response code: " + response.statusCode());
            }
            return bValidConnection; // any status codes other than 302 may indicate invalid connection
        } catch (UnknownHostException e) {
            if (bShowGUIWarnings) {
                customWarning("Blackboard unreachable. Possibly there is no Internet connection.");
            }
            return false;
        } catch (HttpStatusException e) {
            if (bShowGUIWarnings) {
                customWarning("Blackboard unreachable. Possibly cookies have expired.");
            }
            return false;
        } catch (Exception e) {
            if (bShowGUIWarnings) {
                customWarning("Unexpected connection related issue.");
            }
            return false;
        }
    }
}
