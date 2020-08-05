package de.marmaro.krt.ffupdater.version;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import javax.net.ssl.HttpsURLConnection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

public class MozillaCISConsumerIT {

    @Test
    public void getTimestamp_fenixProduction_timestampMustNotBeOld() throws IOException {
        MozillaCIConsumer consumer = MozillaCIConsumer.findLatest(
                "mobile.v2.fenix.fennec-production.latest.arm64-v8a",
                "build/arm64-v8a/geckoProduction/target.apk");
        System.out.println(consumer.getTimestamp());
        LocalDateTime timestamp = LocalDateTime.parse(consumer.getTimestamp(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        System.out.println(timestamp);
        assertThat(ChronoUnit.DAYS.between(LocalDateTime.now(), timestamp), lessThan(31L));
        checkConnection(consumer.getDownloadUrl());
    }

    private void checkConnection(String url) throws IOException {
        HttpsURLConnection urlConnection = (HttpsURLConnection) new URL(url).openConnection();
        try {
            assertThat(urlConnection.getResponseCode(), is(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
        } finally {
            urlConnection.disconnect();
        }
    }
}