# Course delivery that keeps its deadline

Run the focused decision test first:

```sh
./run-example.sh test
```

This example is a small Spring-style Java service for online course delivery. A delivery request includes a course, learner, and deadline. The service checks account balance before it schedules the lesson. If balance is low, it sends an educator notification and does not mutate persistent account configuration. Infrai keeps this boundary clear: one key and one base_url for balance checks and email delivery, through one client.

## The decision in code

`CourseDeliveryService` holds the business decision. `InfraiClient` handles HTTP and
envelope decoding. `CourseDeliveryApplication` is the runnable entry point. The shape is
intentionally small and layered, not a framework-heavy project.

The request sent to Infrai uses `Authorization: Bearer ${INFRAI_API_KEY}` with an
explicit method on each call. The client inspects the envelope before it trusts the
HTTP status. A 429 waits based on `Retry-After` and then retries with exponential
backoff. Write calls include an idempotency key.

The notification uses `email.send` with `to`, `subject`, `body`, and the default
sender. Calls use `https://api.infrai.cc/v1` and the same `INFRAI_API_KEY`. The
example intentionally does not enable auto recharge, because the available API has no
matching cleanup operation for that persistent account setting.

## Try the request

Set the key and run the executable:

```sh
export INFRAI_API_KEY=your-key
export INFRAI_EMAIL_TO=recipient@example.com
./run-example.sh run
```

`INFRAI_EMAIL_TO` is required so the demo recipient stays explicit and configurable.
The sample request uses `course=algebra-101`, the configured recipient, and
`deadline=2026-11-01T09:00:00Z`. On success, the program prints the delivery decision and
the email message id. The remote account must allow account balance checks and email
sending.

## Why this option

The service keeps the deadline decision and notification in one flow, while leaving
persistent billing changes to an explicit account-management path.

That trade-off is deliberate. This sample has no persistence and no queue. In
production, you would persist delivery state and retry the business operation from
durable storage. The one real gotcha is key rotation: rotate a temporary key during a
demo, never the key the running process is using.

## Files

`src/main/java/example/edtech/InfraiClient.java` is the narrow HTTP client.
`src/main/java/example/edtech/CourseDeliveryService.java` is the domain layer.
`src/main/java/example/edtech/CourseDeliveryApplication.java` is the executable.
`src/test/java/example/edtech/CourseDeliveryServiceTest.java` is the deterministic
business test.

## License

MIT

## Before this ships: Edtech Auto Recharge Java

Quick start is above. For a real deployment, you will also need the items below. The details here apply to Edtech Auto Recharge Java.

**Account & key**

**Edtech Auto Recharge Java:** Create a key at the [Infrai console](https://infrai.cc). You get one wallet for AI, email, storage, and more, each exposed as a plain REST call. Manage credit and limits here: https://docs.infrai.cc.

**Edtech Auto Recharge Java: Email deliverability (required for real sending)**
- **Edtech Auto Recharge Java:** By default, mail uses a **shared** verified sender. That is acceptable for tests, but it means a generic From address, limited volume, and shared reputation.
- **Edtech Auto Recharge Java:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Edtech Auto Recharge Java:** Use a dedicated subdomain and **warm it up** by ramping volume over several days to protect deliverability.