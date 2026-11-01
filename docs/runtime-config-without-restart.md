# Changing Configuration at Runtime (Without Restarting the Application)

Applies to: `BadmintonCourtManagement` (Spring Boot 3.5.3, Java 21, WAR on external Tomcat)

## 1. The core rule

Spring reads configuration **once**, during `Environment` preparation and bean creation.
"Changing config at runtime" is therefore really three separate problems:

1. Where does the value live (so it can be edited while the JVM runs)?
2. Who reads it, and *when* do they read it?
3. How is the change propagated (per-node refresh vs. broadcast)?

A value only changes at runtime if it is read **at the moment it is used**, not
captured into a singleton bean at startup.

| Where the value lives | Changeable without restart? |
|---|---|
| Field injected by `@Value` / constructor arg into a singleton bean | ❌ frozen at instantiation |
| `@ConfigurationProperties` bean without `@RefreshScope` | ❌ frozen after binding |
| Bean annotated `@RefreshScope` | ✅ on explicit refresh |
| Value read from the DB on each use | ✅ always |
| Logger level (`logging.level.*`) | ✅ via `/actuator/loggers` |

---

## 2. Option 1 — Keep the value in the DB (recommended for business values)

This is the pattern already used for the hourly rental rate and the e-invoice
toggle, and it is the right choice for anything an admin tunes.

```java
// com/badminton/core/billing/CoreBillingService.java
public BillConfig loadConfig() {
    return billConfigRepository.findFirstByOrderByConfigIdAsc().orElseGet(BillConfig::new);
}
```

Because the row is queried per call (fresh transaction, no stale cache),
`PUT /api/billing/config` takes effect on the **next request**. No refresh
infrastructure, no new dependency, and it works unchanged under external Tomcat.

**Caveats**

- Only for cheap lookups that do not run in a hot loop.
- Do not add repository calls to paths that already query per entity.

---

## 3. Option 2 — `@RefreshScope` + `POST /actuator/refresh` (Spring Cloud)

The "official" answer. Refreshing re-creates the annotated beans, so their
`@Value` / `@ConfigurationProperties` are re-bound from the `Environment`.

### Setup

1. Add `spring-boot-starter-actuator` and `spring-cloud-starter`
   (spring-cloud-commons) using the release train matching Boot 3.5 (`2025.x`).
2. Annotate the holder bean:

   ```java
   @Data
   @Component
   @RefreshScope          // <-- add this
   @ConfigurationProperties(prefix = "einvoice.misa")
   public class EInvoiceProperties { ... }
   ```

3. Point the app at an **external, editable** file. The properties packaged
   inside the WAR are read at startup and are not editable in place, so this
   step is mandatory, not optional:

   ```properties
   spring.config.additional-location=optional:file:/opt/caulong-tc/config/override.properties
   ```

4. Trigger the refresh:

   ```bash
   curl -X POST http://localhost:8080/caulong-tc/actuator/refresh \
        -H "Authorization: Bearer <token>"
   ```

### Traps specific to this codebase

- `JwtService` receives `@Value("${jwt.access-token-ttl:PT30M}") Duration` as a
  **constructor parameter**. `@RefreshScope` on the service works, but an
  invalid TTL only surfaces at refresh time — validate it.
- `JwtKeyConfig` materializes `RSAPrivateKey` / `RSAPublicKey` beans from
  `Resource`s. Key **rotation** therefore needs `@RefreshScope` on those beans
  (or a dedicated reload endpoint), not just an edit to the key file.
- `SecurityConfig.corsConfigurationSource(...)` is a `@Bean` inside a
  `@Configuration` class. `@RefreshScope` on `@Bean` methods is supported but
  discards and rebuilds the whole `UrlBasedCorsConfigurationSource`.
- `spring-cloud-commons` >= 3.1 re-runs *ConfigData* loading on refresh, so
  `spring.config.additional-location` files are genuinely re-read. Older stacks
  only re-bind already-loaded `PropertySource`s and silently keep stale values —
  verify with `/actuator/env` after the first refresh.
- `POST /actuator/env` is **disabled by default** in Spring Boot 3; `/actuator/refresh`
  is the supported path.

---

## 4. Option 3 — Volatile runtime-config holder (no new dependency)

If Spring Cloud should not be pulled into the WAR, make mutable values an
explicit object. This is the most predictable approach for a single-node Tomcat
deployment.

```java
@Component
public class RuntimeConfig {

    private volatile EInvoiceSettings settings;   // immutable record inside

    /** Called by a @Scheduled poll or an authenticated admin endpoint. */
    public void reload() { ... }

    public EInvoiceSettings get() { return settings; }
}
```

Consumers inject `RuntimeConfig` and read `config.get()` per call. The reload is
driven either by `@Scheduled(fixedDelay = ...)` polling (`@EnableScheduling` is
already on) or by an authenticated `POST /api/config/reload`, and the source can
be the DB row from Option 1 or `file:/opt/caulong-tc/config/override.properties`.

Trade-off: it is manual, but there is no actuator surface exposed, it survives on
external Tomcat, and the failure mode is obvious.

---

## 5. Option 4 — Fleet-wide (Config Server + Bus)

Only worth it when running several instances: a Spring Cloud Config Server holds
the values, and a Kafka/RabbitMQ `spring-cloud-bus` broadcasts a
`RefreshRemoteApplicationEvent` so every instance re-binds its `@RefreshScope`
beans at once.

This is also the natural home for `einvoice.misa.username` / `password`, which
currently sit in `application.properties` (lines 30–33) — a stronger reason to
move them out than the refresh convenience itself.

---

## 6. What cannot change at runtime

Stated explicitly so time is not wasted on these:

| Property | Why it is fixed |
|---|---|
| `spring.profiles.active`, `server.port`, `server.servlet.context-path` | bound to the server/container lifecycle |
| `spring.datasource.*` (URL, credentials) | the pool is already built; only pool sizes are adjustable, via Hikari's JMX MBean |
| `spring.jpa.*`, `hibernate.dialect` | `EntityManagerFactory` is fixed at creation |
| Liquibase changesets | immutable by design (see Liquibase Changeset Management Policy) |
| `spring.servlet.multipart.max-file-size` | applied when the Tomcat connector initializes |
| `server.servlet.session.timeout` | applied at session creation; existing sessions keep their original value |

**Exception worth knowing**: logging levels *are* runtime-mutable —
`POST /actuator/loggers/com.badminton` with `{"configuredLevel":"DEBUG"}`, or
programmatically via `LoggingSystem.get(classLoader).setLogLevel(...)`. If that
is the only goal, expose actuator with `loggers`, `health`, `info` only.

---

## 7. Recommendation for this project

| Need | Approach |
|---|---|
| Business values (hourly rate, e-invoice toggle, grace periods) | DB row + per-use lookup — extend `BillConfig` instead of adding properties |
| Credentials / integration endpoints (`einvoice.misa.*`) | Move out of `application.properties` into the DB or `/opt/caulong-tc/config/override.properties`, then add `@RefreshScope` once actuator + spring-cloud-commons are in |
| Already-started singletons such as `jwt.access-token-ttl` | Leave as restart-required — TTL changes are rare, and a versioned release is safer than a mid-flight refresh |
| Logging troubleshooting | Actuator `loggers` endpoint only, if actually needed |

> Local development note: `spring-boot-devtools` (already in `pom.xml`) gives a
> *fast* restart, which is still a restart, and does nothing on external Tomcat.
> Adding a new bean class or changing a constructor signature requires a full
> JVM restart, not a hot restart.
