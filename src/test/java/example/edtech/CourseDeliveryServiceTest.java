package example.edtech;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public final class CourseDeliveryServiceTest {
    public static void main(String[] args) throws Exception {
        InfraiClient offline = new InfraiClient("test", "http://127.0.0.1:1");
        CourseDeliveryService service = new CourseDeliveryService(offline, 5.0, 25.0);
        var request = new CourseDeliveryService.DeliveryRequest("algebra-101", "learner@example.com", "2026-11-01T09:00:00Z");
        var result = service.deliver(request, 12.0);
        if (!result.ready() || result.rechargeConfigured()) throw new AssertionError(result);
        if (!result.reason().contains("balance covers")) throw new AssertionError(result.reason());
        if (!java.nio.file.Files.isExecutable(java.nio.file.Path.of("run-example.sh"))) {
            throw new AssertionError("run-example.sh must be executable");
        }

        AtomicReference<String> calledPath = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/", exchange -> {
            calledPath.set(exchange.getRequestURI().getPath());
            byte[] response = "{\"ok\":true,\"message_id\":\"test-message\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            InfraiClient local = new InfraiClient("test",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
            var lowBalance = new CourseDeliveryService(local, 5.0, 25.0).deliver(request, 5.0);
            if (lowBalance.ready() || lowBalance.rechargeConfigured()) throw new AssertionError(lowBalance);
            if (!"/v1/email/send".equals(calledPath.get())) throw new AssertionError(calledPath.get());
        } finally {
            server.stop(0);
        }
        System.out.println("CourseDeliveryServiceTest passed");
    }
}
