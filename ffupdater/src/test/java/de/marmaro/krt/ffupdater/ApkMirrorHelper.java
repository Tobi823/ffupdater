package de.marmaro.krt.ffupdater;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class ApkMirrorHelper {

    public static LocalDateTime getLatestPubDate(String feedUrl) throws IOException, ParserConfigurationException, SAXException {
        NodeList items = getDocument(feedUrl).getElementsByTagName("item");
        Element latestItem = ((Element) items.item(0));
        String pubDateString = latestItem.getElementsByTagName("pubDate").item(0).getTextContent();
        return LocalDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(pubDateString));
    }

    public static String getLatestTitle(String feedUrl) throws IOException, ParserConfigurationException, SAXException {
        NodeList items = getDocument(feedUrl).getElementsByTagName("item");
        Element latestItem = ((Element) items.item(0));
        return latestItem.getElementsByTagName("title").item(0).getTextContent();
    }

    public static String getAppVersionPage(String feedUrl) throws IOException, ParserConfigurationException, SAXException {
        NodeList items = getDocument(feedUrl).getElementsByTagName("item");
        Element latestItem = ((Element) items.item(0));
        return latestItem.getElementsByTagName("link").item(0).getTextContent();
    }

    private static Document getDocument(String feedUrl) throws IOException, ParserConfigurationException, SAXException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) new URL(feedUrl).openConnection();
        try (InputStream original = urlConnection.getInputStream()) {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(original);
            document.getDocumentElement().normalize();
            return document;
        } finally {
            urlConnection.disconnect();
        }
    }

    public static String extractSha256HashFromAbiVersionPage(String appVersionPage) throws IOException, ParserConfigurationException, SAXException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) new URL(appVersionPage).openConnection();

        try (InputStream original = urlConnection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(original))) {
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            String totalResponse = response.toString();

            String[] temp1 = totalResponse.split("<h4>APK file hashes</h4>");
            String temp2 = temp1[temp1.length - 1];
            String temp3 = temp2.split("<h5>Verify the file")[0];
            String[] temp4 = temp3.split("SHA-256");
            String temp5 = temp4[temp4.length - 1];
            String temp6 = temp5.split("</span>")[0];
            String[] temp7 = temp6.split(">");
            String hash = temp7[temp7.length - 1];
            return hash;
        } finally {
            urlConnection.disconnect();
        }
    }
}
