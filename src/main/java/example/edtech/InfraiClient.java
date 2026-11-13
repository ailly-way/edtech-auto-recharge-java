package example.edtech;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class InfraiClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String key;
    private final String baseUrl;

    public InfraiClient(String key, String baseUrl) {
        this.key = key;
        this.baseUrl = baseUrl;
    }

    public String accountBalance() { return request("GET", "/account/balance", "{}"); }

    public String configureAutorecharge(double trigger, double amount, String requestKey) {
        return request("PUT", "/account/autorecharge/configure",
                "{\"trigger_balance\":" + trigger + ",\"recharge_amount\":" + amount
                        + ",\"idempotency_key\":\"" + jsonEscape(requestKey) + "\"}");
    }

    public String sendEmail(String to, String subject, String body, String requestKey) {
        return request("POST", "/email/send",
                "{\"to\":\"" + jsonEscape(to) + "\",\"subject\":\"" + jsonEscape(subject)
                        + "\",\"body\":\"" + jsonEscape(body) + "\",\"idempotency_key\":\""
                        + jsonEscape(requestKey) + "\"}");
    }

    private static String jsonEscape(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private String request(String method, String path, String body) {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + key)
                        .header("Content-Type", "application/json").method(method,
                                HttpRequest.BodyPublishers.ofString(body));
                HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                String envelope = response.body();
                if (envelope.contains("\"ok\":true")) return envelope;
                if (response.statusCode() != 429 || attempt == 2) {
                    throw new InfraiException(envelope);
                }
                waitBeforeRetry(response, attempt);
            } catch (IOException e) {
                throw new InfraiException("transport error: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InfraiException("request interrupted");
            }
        }
        throw new InfraiException("request did not complete");
    }

    private static void waitBeforeRetry(HttpResponse<String> response, int attempt)
            throws InterruptedException {
        long delayMillis = retryAfterMillis(response, attempt);
        TimeUnit.MILLISECONDS.sleep(delayMillis);
    }

    private static long retryAfterMillis(HttpResponse<String> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse("").trim();
        try {
            return Math.max(0L, Long.parseLong(retryAfter) * 1000L);
        } catch (NumberFormatException ignored) {
            return 1000L * (1L << attempt);
        }
    }
}
