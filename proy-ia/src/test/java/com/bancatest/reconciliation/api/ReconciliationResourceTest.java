package com.bancatest.reconciliation.api;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ReconciliationResourceTest {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    @TestHTTPResource("/v1")
    URI baseUri;

    @Test
    void registersPaymentResolvesAndReturnsCanonicalTerminalStateOnRetry() throws Exception {
        String reference = "integration-" + System.nanoTime();
        HttpResponse<String> payment = post("/payments", """
                {
                  "operationReference": "%s",
                  "accountNumber": "001234",
                  "amount": 1250.50,
                  "currency": "PEN",
                  "occurredAt": "2026-11-05T15:30:00-05:00"
                }
                """.formatted(reference));
        assertEquals(201, payment.statusCode(), payment.body());
        assertTrue(payment.body().contains("\"status\":\"PENDING\""));
        assertTrue(payment.body().contains("\"paymentAmount\":1250.50"));

        HttpResponse<String> core = post("/core-transactions", """
                {
                  "operationReference": "%s",
                  "amount": 1250.00,
                  "currency": "PEN",
                  "occurredAt": "2026-11-05T15:31:00-05:00"
                }
                """.formatted(reference));
        assertEquals(200, core.statusCode(), core.body());
        assertTrue(core.body().contains("\"status\":\"MATCHED\""));
        assertTrue(core.body().contains("\"difference\":0.50"));
        assertTrue(core.body().contains("\"resolvedAt\""));

        HttpResponse<String> retry = post("/core-transactions", """
                {
                  "operationReference": "%s",
                  "amount": 999.00,
                  "currency": "PEN",
                  "occurredAt": "2026-11-05T15:32:00-05:00"
                }
                """.formatted(reference));
        assertEquals(200, retry.statusCode(), retry.body());
        assertTrue(retry.body().contains("\"status\":\"MATCHED\""));
        assertTrue(retry.body().contains("\"coreAmount\":1250.00"));

        HttpResponse<String> get = CLIENT.send(HttpRequest.newBuilder(uri("/reconciliations/" + reference))
                        .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, get.statusCode(), get.body());
        assertTrue(get.body().contains("\"operationReference\":\"" + reference + "\""));
    }

    @Test
    void returnsProblemJsonForMissingPaymentAndUnknownProperties() throws Exception {
        String absentReference = "absent-" + System.nanoTime();
        HttpResponse<String> missingPayment = post("/core-transactions", """
                {
                  "operationReference": "%s",
                  "amount": 1.00,
                  "currency": "PEN",
                  "occurredAt": "2026-11-05T15:31:00-05:00"
                }
                """.formatted(absentReference));
        assertEquals(404, missingPayment.statusCode(), missingPayment.body());
        assertTrue(missingPayment.headers().firstValue("Content-Type").orElse("")
                .startsWith("application/problem+json"));
        assertTrue(missingPayment.body().contains("\"requestId\""));

        HttpResponse<String> unknownProperty = post("/payments", """
                {
                  "operationReference": "unknown-%s",
                  "accountNumber": "001234",
                  "amount": 1.00,
                  "currency": "PEN",
                  "occurredAt": "2026-11-05T15:30:00-05:00",
                  "sensitiveExtra": "must be rejected"
                }
                """.formatted(System.nanoTime()));
        assertEquals(400, unknownProperty.statusCode(), unknownProperty.body());
        assertTrue(unknownProperty.headers().firstValue("Content-Type").orElse("")
                .startsWith("application/problem+json"),
                () -> "Unexpected response: " + unknownProperty.headers().firstValue("Content-Type").orElse("")
                        + " " + unknownProperty.body());

        HttpResponse<String> malformedJson = post("/payments", "{\"operationReference\":");
        assertEquals(400, malformedJson.statusCode(), malformedJson.body());
        assertTrue(malformedJson.headers().firstValue("Content-Type").orElse("")
                .startsWith("application/problem+json"));

        HttpRequest invalidAmountRequest = HttpRequest.newBuilder(uri("/payments"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "operationReference": "invalid-%s",
                          "accountNumber": "001234",
                          "amount": 0,
                          "currency": "PEN",
                          "occurredAt": "2026-11-05T15:30:00-05:00"
                        }
                        """.formatted(System.nanoTime())))
                .build();
        HttpResponse<String> invalidAmount = CLIENT.send(invalidAmountRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(400, invalidAmount.statusCode(), invalidAmount.body());
        assertTrue(invalidAmount.headers().firstValue("Content-Type").orElse("")
                .startsWith("application/problem+json"));
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create(baseUri.toString().replaceAll("/+$", "") + path);
    }
}
