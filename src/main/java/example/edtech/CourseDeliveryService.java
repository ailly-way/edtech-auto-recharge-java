package example.edtech;

public final class CourseDeliveryService {
    public record DeliveryRequest(String course, String learner, String deadline) {}
    public record DeliveryDecision(boolean ready, boolean rechargeConfigured, String reason,
                                   String emailMessageId) {}

    private final InfraiClient infrai;
    private final double rechargeTrigger;
    private final double rechargeAmount;

    public CourseDeliveryService(InfraiClient infrai, double rechargeTrigger, double rechargeAmount) {
        this.infrai = infrai;
        this.rechargeTrigger = rechargeTrigger;
        this.rechargeAmount = rechargeAmount;
    }

    public DeliveryDecision deliver(DeliveryRequest request, double observedBalance) {
        if (observedBalance > rechargeTrigger) {
            return new DeliveryDecision(true, false, "balance covers " + request.course(), null);
        }
        String requestKey = request.course() + "-" + request.learner();
        String emailEnvelope = infrai.sendEmail(request.learner(), "Course balance requires attention",
                "Balance is at or below " + rechargeTrigger + " for " + request.course()
                        + " before " + request.deadline(), requestKey);
        return new DeliveryDecision(false, false, "balance requires manual recharge",
                readMessageId(emailEnvelope));
    }

    private static String readMessageId(String envelope) {
        String marker = "\"message_id\":\"";
        int start = envelope.indexOf(marker);
        if (start < 0) throw new InfraiException("email message id missing from response");
        start += marker.length();
        int end = envelope.indexOf('"', start);
        if (end < 0) throw new InfraiException("email message id malformed in response");
        return envelope.substring(start, end);
    }
}
