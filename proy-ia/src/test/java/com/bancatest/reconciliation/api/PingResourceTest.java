package com.bancatest.reconciliation.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.http.TestHTTPResource;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class PingResourceTest {

    @TestHTTPResource("/v1/ping")
    URI pingUri;

    @Test
    void startsApplicationAndAnswersPing() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(pingUri).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("pong", response.body());
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("text/plain"));
        assertTrue(response.headers().firstValue("X-Request-ID").isPresent());
    }
}
