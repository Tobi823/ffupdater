package de.marmaro.krt.ffupdater;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class ApkMirrorHelper {

    public static LocalDate getPubDateFromApkMirror(String feedUrl) throws IOException, ParserConfigurationException, SAXException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) new URL(feedUrl).openConnection();
        try (InputStream original = urlConnection.getInputStream()) {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(original);
            document.getDocumentElement().normalize();

            NodeList items = document.getElementsByTagName("item");
            Element latestItem = ((Element) items.item(0));
            String pubDateString = latestItem.getElementsByTagName("pubDate").item(0).getTextContent();
            LocalDateTime pubDate = LocalDateTime.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(pubDateString));
            return pubDate.toLocalDate();
        } finally {
            urlConnection.disconnect();
        }
    }
}
