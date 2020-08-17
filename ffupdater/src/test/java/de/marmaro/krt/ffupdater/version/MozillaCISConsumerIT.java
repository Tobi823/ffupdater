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
        MozillaCIConsumer consumer = MozillaCIConsumer.findLatest("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/project.mobile.focus.release.latest/artifacts/public/chain-of-trust.json");
        System.out.println(consumer.getTimestamp());
        LocalDateTime timestamp = LocalDateTime.parse(consumer.getTimestamp(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertThat(ChronoUnit.DAYS.between(LocalDateTime.now(), timestamp), lessThan(31L));
    }
}