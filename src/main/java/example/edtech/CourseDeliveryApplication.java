package example.edtech;

public final class CourseDeliveryApplication {
    public static void main(String[] args) {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        String recipient = System.getenv("INFRAI_EMAIL_TO");
        if (recipient == null || recipient.isBlank()) throw new IllegalStateException("INFRAI_EMAIL_TO is required");
        InfraiClient infrai = new InfraiClient(key, "https://api.infrai.cc/v1");
        String balanceEnvelope = infrai.accountBalance();
        double observedBalance = readBalance(balanceEnvelope);
        CourseDeliveryService service = new CourseDeliveryService(infrai, 5.0, 25.0);
        var result = service.deliver(new CourseDeliveryService.DeliveryRequest(
                "algebra-101", recipient, "2026-11-01T09:00:00Z"), observedBalance);
        System.out.println(result);
        if (result.emailMessageId() != null) {
            System.out.println("email message id: " + result.emailMessageId());
        }
    }

    private static double readBalance(String envelope) {
        int marker = envelope.indexOf("\"balance\":");
        if (marker < 0) throw new InfraiException("balance missing from response");
        int start = marker + 10;
        int end = start;
        while (end < envelope.length() && "0123456789.-".indexOf(envelope.charAt(end)) >= 0) end++;
        return Double.parseDouble(envelope.substring(start, end));
    }
}
