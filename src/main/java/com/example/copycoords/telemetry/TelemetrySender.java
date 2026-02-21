package com.example.copycoords.telemetry;

import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class TelemetrySender {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private TelemetrySender() {
    }

    public static CompletableFuture<Void> send(URI endpoint, JsonObject payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(endpoint)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "CopyCoords/telemetry")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenCompose(response -> {
                        int status = response.statusCode();
                        if (status >= 200 && status < 300) {
                            return CompletableFuture.completedFuture(null);
                        }
                        return CompletableFuture.failedFuture(new RuntimeException("HTTP " + status));
                    });
        } catch (Exception exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }
}