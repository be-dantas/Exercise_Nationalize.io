# Person Registry API

REST API to register people and predict their likely nationality from an
external service, with a web interface and authentication on the write
operations.

Java 21 · Spring Boot 4.1.1 · Maven · no database server, no Docker required.

---

## Running it

Requires a **JDK 21+**. Maven is not needed — the wrapper downloads it.

```bash
./mvnw -q spring-boot:run
```

On start it prints:

```
  cadastro.  ->  http://localhost:8080

  login de demonstracao: admin / admin123
  encerrar: Ctrl+C
```

The framework log is set to `WARN`, so a healthy start is quiet and only
warnings and failures show up — a failed start still prints Spring's full
diagnostic.

| What | Command |
|---|---|
| Run | `./mvnw -q spring-boot:run` |
| Test | `./mvnw -q test` |
| Build a jar | `./mvnw -q clean package` |
| Run the jar on its own | `java -jar target/*.jar` |

`-q` keeps the build quiet, so **no output means success**. Failures still
print in full: a failing test shows the assertion, a compile error shows the
file and line, and a failed start shows Spring's diagnostic. Drop the `-q` for
the complete Maven build log.

**Demo credentials:** `admin` / `admin123`
Only the BCrypt hash is stored, in `application.properties`.

---

## What the assignment asked for

| Requirement | Where |
|---|---|
| Java + a persistence mechanism | Java 21; in-memory store, explicitly allowed by the assignment |
| `POST /registrarName` with document, name, surname, e-mail | `PessoaController` |
| `GET /list` | `PessoaController` |
| `GET /list/{param}` | `PessoaController`, parameter is the document |
| `DELETE /list/{param}` | `PessoaController` |
| `GET /findNacionalityByPerson/{param}` returning the nationality **name** | `NacionalidadeController` + `NationalizeClient` |
| At least one type validation per endpoint | Value objects `Documento`, `Nome`, `Email` |
| Authentication on the most critical endpoint | `FiltroDeAutenticacao` on `POST` and `DELETE` |
| A web interface consuming at least one endpoint | `static/index.html`, consumes all of them |

---

## API

| Method | Path | Auth | Success | Errors |
|---|---|:---:|---|---|
| `POST` | `/auth/login` | — | `200` `{token, expiresInSeconds}` | `401` |
| `POST` | `/registrarName` | 🔒 | `201` + `Location: /list/{document}` | `400` `401` `409` |
| `GET` | `/list` | — | `200` array, sorted by name | — |
| `GET` | `/list/{document}` | — | `200` | `400` `404` |
| `DELETE` | `/list/{document}` | 🔒 | `204` | `400` `401` `404` |
| `GET` | `/findNacionalityByPerson/{document}` | — | `200` `{name, nationality, probability}` | `400` `404` `503` |
| `GET` | `/health` | — | `200` | — |

Every error uses the same shape:

```json
{ "error": "INVALID_DATA", "message": "e-mail invalido: nao-e-email" }
```

`INVALID_DATA` · `NOT_FOUND` · `ALREADY_EXISTS` · `UNAUTHORIZED` ·
`MALFORMED_REQUEST` · `EXTERNAL_SERVICE_UNAVAILABLE`

### Example session

```bash
TOKEN=$(curl -s -X POST localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

curl -X POST localhost:8080/registrarName \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"document":"123.456.789-00","name":"Beatriz","lastName":"Dantas","email":"be@example.com"}'
# 201  {"document":"12345678900", ...}      punctuation is normalised away

curl localhost:8080/findNacionalityByPerson/12345678900
# 200  {"name":"Beatriz Dantas","nationality":"Brazil","probability":0.665717}
```

---

## Decisions

The assignment says "at the discretion of whoever takes the test" four times,
so these are the calls I made and why.

### The `{Parametro}` is the document

It is the natural identity of the entity: unique, stable and meaningful to the
business. An auto-increment id would leak a persistence detail into the public
API.

*Trade-off:* if the document is a national id, it becomes PII in a URL path —
which lands in access logs, proxies and browser history in plain text. For this
scope the readability of `GET /list/12345678900` wins, but in a system holding
real personal data I would use an opaque id in the path.

### Document validation is generic, not CPF-specific

6–20 alphanumeric characters after stripping `.` `-` `/` and spaces. The
assignment says *"documento"*, not *"CPF"*, so validating Brazilian check digits
would reject a legitimate Argentinian DNI or Chilean RUT and look like a bug. A
country-specific validator plugs into the same value object without changing
anything else.

The raw shape is validated **before** normalising, deliberately: cleaning first
would reduce `<script>alert(1)</script>` to `scriptalert1script` and accept it
as a valid document.

### Validation lives in the types, not in Bean Validation

`Documento`, `Nome` and `Email` validate inside their constructors, so an
invalid instance cannot exist anywhere in the system.

`@Valid` / `@NotBlank` on the request DTO would cover `POST` only — it does not
apply to a `@PathVariable`, so `GET` and `DELETE` would be left uncovered.
Using both would duplicate each rule in two places that can drift apart. One
rule, one owner.

### The nationality endpoint returns the country name

`api.nationalize.io` returns ISO 3166-1 alpha-2 codes (`"BR"`), and the
assignment asks for the *name* of the nationality. `java.util.Locale` performs
the conversion from the JDK's bundled CLDR data — no extra dependency.

**The query uses first name + surname.** Measured against the live API during
development:

| Query | Result |
|---|---|
| `Beatriz` | 🇪🇸 Spain 19.8% — wrong |
| `Beatriz Dantas` | 🇧🇷 Brazil **66.6%** — right |
| `Beatriz Dantas da Silva` | 🇧🇷 Brazil 36.5% — right, less confident |
| `Yuki` → `Yuki Tanaka` | Japan 46.3% → **65.5%** |

Middle names dilute the prediction, so first name + surname is the sweet spot —
which is exactly the field structure the assignment specifies. The response
echoes the name that was actually sent, so the payload explains itself.

When the service has no guess, the endpoint returns `200` with a null
nationality: the person exists, only the prediction is missing. A `404` would
wrongly suggest the person is not registered.

### Storage is in memory

The assignment allows *"banco de dados, armazenamento em memória, etc."*. A
`ConcurrentHashMap` behind the `PessoaRepository` port means the evaluator runs
one command with nothing to install.

*Consequence:* data does not survive a restart. Swapping in JPA/PostgreSQL means
adding one adapter class — no use case, controller or domain test changes. That
substitutability is the point of the port.

### Authentication protects the write operations

`POST /registrarName` and `DELETE /list/{document}` require a token; the three
`GET` endpoints stay open.

`DELETE` is the most critical operation — destructive and irreversible — and
`POST` mutates state as well. Reads change nothing, and leaving them open lets
the API be evaluated without friction.

**Opaque token rather than JWT.** The application runs as a single instance, so
JWT's stateless validation would buy nothing here, while an opaque token gives
immediate revocation. Scaling out would move the token map to Redis, or switch
to JWT and accept a revocation list.

**Written by hand rather than using `spring-boot-starter-security`.** The
requirement is small and a 40-line `OncePerRequestFilter` keeps every line of
the access control visible and explainable. Only `spring-security-crypto` is
pulled in, for BCrypt — it brings no filter chain and no auto-configuration.

Details worth noting:
- The password is stored as a BCrypt hash, never in plain text.
- The hash is verified even when the username is wrong, so response time does
  not reveal which usernames exist.
- A wrong username and a wrong password return the same message.
- In the browser the token lives in memory only — not in `localStorage`, and
  never in the page source.

### Architecture: Clean Architecture with part of tactical DDD

```
domain/          entities, value objects, ports, domain errors   — plain Java
application/     use cases                                       — plain Java
infrastructure/  in-memory repository, HTTP client, auth
presentation/    controllers, DTOs, error handling, static page
```

Dependencies point inwards: `domain` and `application` import no framework at
all, which is why the use cases are tested without booting Spring and without
network access.

**Used:** value object, entity, repository port, gateway port.
**Deliberately not used:** aggregates, domain events, factories,
specifications, bounded contexts. This domain has one entity and four
attributes — applying the full catalogue would add indirection without reducing
complexity.

The five use cases are grouped into two services rather than five
single-method classes, and DTOs are nested inside the controller that uses
them. Both choices trade ceremony for readability at this size.

*Note on language:* domain names are in Portuguese (`Pessoa`, `Documento`),
matching the language of the assignment; technical suffixes and the JSON
contract are in English.

---

## Tests

```bash
./mvnw -q test
```

56 tests:

| Suite | What it covers |
|---|---|
| `DocumentoTest`, `NomeTest`, `EmailTest` | validation rules, normalisation, injection payloads |
| `NacionalidadeServiceTest` | use case with fake adapters — no network, no Spring |
| `ApiEndToEndTest` | boots the app on a real port and exercises every endpoint over HTTP, authentication included |

The end-to-end suite does not call the external service; that path is covered
by fakes, so the whole suite runs offline.

---

## Limitations, and what I would do next

- **Data is not persisted** across restarts. One adapter class away from a real
  database.
- **No cache on the external call.** `api.nationalize.io` rate-limits the free
  tier at around 100 requests/day; a cache inside the adapter would be the next
  step and would not touch any other layer.
- **One fixed user.** The assignment does not ask for user management. Real
  users would live in the repository, behind the same port.
- **No containerisation.** Running takes one command and a JDK; containerising
  would be the natural next step for deployment.
- **The document is PII in the URL path** — discussed above.
