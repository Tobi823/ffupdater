package de.marmaro.krt.ffupdater;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class ApkMirrorHelper {

    public static LocalDateTime getLatestPubDate(String feedUrl) throws IOException, ParserConfigurationException, SAXException {
        Document document = getDocument(feedUrl);
        NodeList items = document.getElementsByTagName("item");
        Element latestItem = ((Element) items.item(0));
        String pubDateString = latestItem.getElementsByTagName("pubDate").item(0).getTextContent();
        return LocalDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(pubDateString));
    }

    public static String getLatestTitle(String feedUrl) throws IOException, ParserConfigurationException, SAXException {
        Document document = getDocument(feedUrl);
        NodeList items = document.getElementsByTagName("item");
        Element latestItem = ((Element) items.item(0));
        return latestItem.getElementsByTagName("title").item(0).getTextContent();
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
}
