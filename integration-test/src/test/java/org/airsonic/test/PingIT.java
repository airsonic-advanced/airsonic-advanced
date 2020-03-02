package org.airsonic.test;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.xmlunit.builder.Input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.xmlunit.matchers.CompareMatcher.isIdenticalTo;

public class PingIT {
    String container = System.getProperty("dockerTestingContainer");

    @Test
    public void pingMissingAuthTest() throws Exception {
        HttpGet httpGet = new HttpGet(container + ":4040/rest/ping");

        try (CloseableHttpClient client = HttpClientBuilder.create().build();
                CloseableHttpResponse response = client.execute(httpGet);) {
            assertEquals(response.getStatusLine().getStatusCode(), 200);
            HttpEntity entity = response.getEntity();
            String actual = EntityUtils.toString(entity);
            assertThat(actual,
                    isIdenticalTo(Input.fromStream(getClass().getResourceAsStream("/blobs/ping/missing-auth.xml"))).ignoreWhitespace());
        }
    }
}
