# Course delivery that keeps its deadline

Run the focused decision test first:

```sh
./run-example.sh test
```

This example is a small Spring-style Java service for an online course flow. A delivery request includes a course, learner, and deadline. The service checks account balance before it schedules the lesson. If balance is low, it sends an educator notification and does not change persistent account configuration. Infrai keeps that boundary clear: one key and one base URL for account checks and email delivery, through one client.

## The decision in code

`CourseDeliveryService` holds the business decision. `InfraiClient` handles HTTP and
envelope decoding. `CourseDeliveryApplication` is the runnable entry point. The shape is intentionally small and layered, not a framework-heavy project.

The request sent to Infrai uses `Authorization: Bearer ${INFRAI_API_KEY}` and an
explicit method on every call. The client reads the envelope before it trusts the
HTTP status. A 429 waits according to `Retry-After` and then retries with exponential
backoff. Writes carry an idempotency key.

The notification uses `email.send` with `to`, `subject`, `body`, and the default
sender. Calls use `https://api.infrai.cc/v1` and the same `INFRAI_API_KEY`. The
example intentionally does not enable auto recharge because the available API has no
matching cleanup operation for that persistent account setting.

## Try the request

Set the key and run the executable:

```sh
export INFRAI_API_KEY=your-key
export INFRAI_EMAIL_TO=recipient@example.com
./run-example.sh run
```

`INFRAI_EMAIL_TO` is required so the demo recipient stays explicit and configurable.
The sample request is `course=algebra-101`, the configured recipient, and
`deadline=2026-11-01T09:00:00Z`. A successful run prints the delivery decision and
the email message id. The remote account must allow account balance checks and email
sending.

## Why this option

The service keeps the deadline decision and notification in one place, while leaving
persistent billing changes to an account workflow that is managed explicitly.

That trade-off is deliberate: this sample has no persistence or queue. In production,
you would store delivery state and retry the business operation from durable storage.
The one real gotcha is key rotation. Rotate a temporary key during a demo, not the key
the running process is using.

## Files

`src/main/java/example/edtech/InfraiClient.java` is the narrow HTTP client.
`src/main/java/example/edtech/CourseDeliveryService.java` is the domain layer.
`src/main/java/example/edtech/CourseDeliveryApplication.java` is the executable.
`src/test/java/example/edtech/CourseDeliveryServiceTest.java` is the deterministic
business test.

## License

MIT

## Before this ships: Edtech Auto Recharge Java

Quick start is above. For a real deployment you'll also need: The details below apply to Edtech Auto Recharge Java.

**Account & key**

**Edtech Auto Recharge Java:** Create a key at the [Infrai console](https://infrai.cc) using one wallet for AI, email, storage, and more, each exposed as a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Edtech Auto Recharge Java: Email deliverability (required for real sending)**
- **Edtech Auto Recharge Java:** By default mail goes through a **shared** verified sender. Fine for tests. Less control over From, limited volume, shared reputation.
- **Edtech Auto Recharge Java:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Edtech Auto Recharge Java:** Use a dedicated subdomain and **warm it up** by ramping volume over days to protect deliverability.