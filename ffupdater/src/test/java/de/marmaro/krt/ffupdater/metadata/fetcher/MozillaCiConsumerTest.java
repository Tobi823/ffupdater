package de.marmaro.krt.ffupdater.metadata.fetcher;

import com.google.gson.Gson;

import org.exparity.hamcrest.date.ZonedDateTimeMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import de.marmaro.krt.ffupdater.metadata.ReleaseTimestamp;
import de.marmaro.krt.ffupdater.metadata.fetcher.MozillaCiConsumer.MozillaCiResult;
import de.marmaro.krt.ffupdater.metadata.fetcher.MozillaCiConsumer.Response;

import static org.exparity.hamcrest.date.ZonedDateTimeMatchers.sameInstant;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MozillaCiConsumerTest {

    private MozillaCiResult result;

    @Before
    public void setUp() throws MalformedURLException {
        final URL url = new URL("https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v2.fenix.beta.latest.arm64-v8a/artifacts/public/chain-of-trust.json");
        final ApiConsumer apiConsumer = mock(ApiConsumer.class);
        when(apiConsumer.consume(url, Response.class)).thenReturn(
                new Gson().fromJson(CHAIN_OF_TRUST_ARTIFACT_VALUE, Response.class));

        final MozillaCiConsumer mozillaCiConsumer = new MozillaCiConsumer(apiConsumer);
        result = mozillaCiConsumer.consume(url, "public/build/arm64-v8a/target.apk");
        assertNotNull(result);
    }

    @Test
    public void isTimestampCorrect() throws MalformedURLException {
        ZonedDateTime expectedTimestamp = ZonedDateTime.of(2020, 10, 16, 1, 25, 23, 565000000, ZoneId.of("UTC"));
        assertThat(result.getTimestamp().getCreated(), sameInstant(expectedTimestamp));
    }

    @Test
    public void isHashCorrect() {
        assertEquals("f56063913211d44de579b8335fe1146bd65aa0a35628d48852cb50171e9fa8fc", result.getHash().getHexString());
    }

    private final static String CHAIN_OF_TRUST_ARTIFACT_VALUE = "{\n" +
            "  'artifacts': {\n" +
            "    'public/build/arm64-v8a/target.apk': {\n" +
            "      'sha256': 'f56063913211d44de579b8335fe1146bd65aa0a35628d48852cb50171e9fa8fc'\n" +
            "    },\n" +
            "    'public/build/armeabi-v7a/target.apk': {\n" +
            "      'sha256': '3dabe7d80b3090482ecefc31725150a56a71e81566d28f24b720fb6c27c44e96'\n" +
            "    },\n" +
            "    'public/build/x86/target.apk': {\n" +
            "      'sha256': 'ca64a07b3a60e7cef96eec2e63c1b8f3b84a73ecbf0819eb306501a19d8e35ab'\n" +
            "    },\n" +
            "    'public/build/x86_64/target.apk': {\n" +
            "      'sha256': '0f6a2ef9406101462a7e17fc27926c7ff4caa5e3ce8491a7b5be4d8a7da7878b'\n" +
            "    },\n" +
            "    'public/logs/chain_of_trust.log': {\n" +
            "      'sha256': '7163dcc677e85ab6ea59add902215727d12aef588bfeab14e9f859032c9392b6'\n" +
            "    },\n" +
            "    'public/logs/live_backing.log': {\n" +
            "      'sha256': '91cda8877b731fc41434c9ab709c809a6b859ae66c0d27d55d7def02b02626a7'\n" +
            "    }\n" +
            "  },\n" +
            "  'chainOfTrustVersion': 1,\n" +
            "  'environment': {},\n" +
            "  'runId': 0,\n" +
            "  'task': {\n" +
            "    'created': '2020-10-16T01:25:23.565Z',\n" +
            "    'deadline': '2020-10-17T01:25:23.565Z',\n" +
            "    'dependencies': [\n" +
            "      'VnLcn0wkQwuICSSfVhmrVQ'\n" +
            "    ],\n" +
            "    'expires': '2021-10-16T01:25:23.565Z',\n" +
            "    'extra': {\n" +
            "      'index': {\n" +
            "        'rank': 1602811437\n" +
            "      },\n" +
            "      'parent': 'QjlOTwy1Su6LEAFzg1vrUQ',\n" +
            "      'treeherder': {\n" +
            "        'collection': {\n" +
            "          'opt': true\n" +
            "        },\n" +
            "        'groupName': 'Beta-related tasks with same APK configuration as Fennec',\n" +
            "        'groupSymbol': 'beta',\n" +
            "        'jobKind': 'build',\n" +
            "        'machine': {\n" +
            "          'platform': 'android-all'\n" +
            "        },\n" +
            "        'symbol': 'Bs',\n" +
            "        'tier': 1\n" +
            "      },\n" +
            "      'treeherder-platform': 'android-all/opt'\n" +
            "    },\n" +
            "    'metadata': {\n" +
            "      'description': 'Sign Fenix ([Treeherder push](https://treeherder.mozilla.org/#/jobs?repo=fenix&revision=d3e93fed1a4c4a25c85dadc97ef49435bce6c0f0))',\n" +
            "      'name': 'signing-beta',\n" +
            "      'owner': 'st3fan@users.noreply.github.com',\n" +
            "      'source': 'https://github.com/mozilla-mobile/fenix/blob/d3e93fed1a4c4a25c85dadc97ef49435bce6c0f0/taskcluster/ci/signing'\n" +
            "    },\n" +
            "    'payload': {\n" +
            "      'maxRunTime': 600,\n" +
            "      'upstreamArtifacts': [\n" +
            "        {\n" +
            "          'formats': [\n" +
            "            'autograph_apk'\n" +
            "          ],\n" +
            "          'paths': [\n" +
            "            'public/build/arm64-v8a/target.apk',\n" +
            "            'public/build/armeabi-v7a/target.apk',\n" +
            "            'public/build/x86/target.apk',\n" +
            "            'public/build/x86_64/target.apk'\n" +
            "          ],\n" +
            "          'taskId': 'VnLcn0wkQwuICSSfVhmrVQ',\n" +
            "          'taskType': 'build'\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    'priority': 'highest',\n" +
            "    'provisionerId': 'scriptworker-k8s',\n" +
            "    'requires': 'all-completed',\n" +
            "    'retries': 5,\n" +
            "    'routes': [\n" +
            "      'tc-treeherder.v2.fenix.d3e93fed1a4c4a25c85dadc97ef49435bce6c0f0.0',\n" +
            "      'checks'\n" +
            "    ],\n" +
            "    'schedulerId': 'mobile-level-3',\n" +
            "    'scopes': [\n" +
            "      'project:mobile:fenix:releng:signing:cert:fennec-production-signing',\n" +
            "      'project:mobile:fenix:releng:signing:format:autograph_apk'\n" +
            "    ],\n" +
            "    'tags': {\n" +
            "      'createdForUser': 'st3fan@users.noreply.github.com',\n" +
            "      'kind': 'signing',\n" +
            "      'label': 'signing-beta',\n" +
            "      'os': 'scriptworker',\n" +
            "      'worker-implementation': 'scriptworker'\n" +
            "    },\n" +
            "    'taskGroupId': 'QjlOTwy1Su6LEAFzg1vrUQ',\n" +
            "    'workerType': 'mobile-3-signing'\n" +
            "  },\n" +
            "  'taskId': 'R1GBuwB5TS61TTuWnJMWMg',\n" +
            "  'workerGroup': 'mobile-3-signing',\n" +
            "  'workerId': 'mobile-3-signing-ok1qxppssqk4onzlo3abw',\n" +
            "  'workerType': 'mobile-3-signing'\n" +
            "}";
}