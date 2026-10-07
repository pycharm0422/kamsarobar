# Architecture

```
 Browser (React SPA)
        │  HTTPS, JSON, "Authorization: Bearer <JWT>"
        ▼
 nginx (static files + /api reverse proxy)      ← can sit behind any load balancer
        │
        ▼
 Spring Boot API  ×N instances (stateless)
        │  JDBC (HikariCP pool)
        ▼
 PostgreSQL (schema managed by Flyway)
```

## Backend layout: package by feature

Each feature is a self-contained package with its own entity, repository, service, controller and DTOs:

| Package | Responsibility |
|---|---|
| `auth` | Register (form 1) and log in, issuing a JWT |
| `user` | `User` entity, `Role` (MEMBER / CITY_ADMIN / MAIN_ADMIN), editing basic info and password |
| `profile` | `UserProfile` (form 2), and the `Company` and `Skill` tag tables with autocomplete |
| `directory` | "Who can refer me to X?" and "Who knows Y?" search |
| `post` | Posts (text, photos, seminars and events) and comments, with ownership and moderation rules. Event attendees. `PostResponseAssembler` builds a whole page of responses with a fixed number of queries. |
| `event` | "My upcoming events" and discovering upcoming events and seminars |
| `notification` | Push notifications: registered phones, members' choices, who is notified about a new post or event, and the event reminder job. Sending goes through a `PushSender` interface, with an Expo implementation by default. |
| `media` | Photo upload, validation by file contents, storage behind the `ImageStorage` interface, and an hourly cleanup of unused photos |
| `donation` | Donations (pending → verified/rejected), campaigns, city-wise totals |
| `city` | Cities, WhatsApp group link, bank details |
| `admin` | Main admin operations: appoint or revoke city admins, manage cities, list members |
| `access` | **All authorisation rules in one place** |
| `security` | JWT filter and token service |
| `common` | Error handling, paging, text and phone normalisation |

Controllers are thin (HTTP ↔ DTO). Services hold the business logic and transactions. Repositories only do data access.

## SOLID in practice

**S: Single Responsibility.**
- `AuthService` only registers and logs in. `UserService` only edits users. `ProfileService` only handles form 2.
- `PhoneNumberNormalizer` only normalises numbers. `TextNormalizer` only cleans text. `GlobalExceptionHandler` only maps errors to HTTP responses.
- Authorisation lives in `AccessPolicy`. Services ask *"may this user do this?"* instead of each re-implementing role checks.

**O: Open/Closed.**
- Search is built on `DirectorySearchStrategy`. `ReferralSearchStrategy` and `ExpertiseSearchStrategy` are two implementations. `DirectoryService` receives every strategy bean from Spring and picks one by `SearchType`. To add a new search (say, "by current position"), you add one class and the existing code doesn't change.
- Photo storage is behind `ImageStorage`. `FileSystemImageStorage` is the default. Adding an S3 or Google Cloud Storage implementation needs no other code changes.
- `TagResolver<T>` is one generic find-or-create-and-suggest implementation used for both companies and skills. A new tag type, such as *colleges*, is one entity, one repository and one bean.
- On the frontend, `DirectorySearch` is a generic component. *Find referral* and *Find expert* are just configuration objects (labels, API call, message builder).

**L: Liskov Substitution.** Every `DirectorySearchStrategy` and every `TagRepository` subtype can be used wherever the base type is expected, without special cases. `RoleBasedAccessPolicy` can be swapped for any other `AccessPolicy` implementation.

**I: Interface Segregation.** Interfaces are small and focused. `TokenService` has two methods. `AccessPolicy` has two questions plus helpers. `DirectorySearchStrategy` has two methods. On the frontend each resource has its own small API object (`postApi`, `donationApi`...), so pages depend only on what they use.

**D: Dependency Inversion.**
- Services depend on abstractions such as `TokenService` (not JJWT directly), `AccessPolicy` (not role checks) and `PasswordEncoder`. All of them are wired through constructor injection.
- `JwtAuthenticationFilter` depends on `TokenService`. You could switch to opaque tokens or OAuth without touching the filter's callers.

## Scalability

| Concern | How it's handled |
|---|---|
| **Horizontal scaling** | The API is **stateless**: JWT auth with no HTTP session, so you can run any number of backend instances behind a load balancer. |
| **Database growth** | PostgreSQL with indexes on every foreign key and search path (`users.city_id`, `posts(city_id, created_at)`, `donations(city_id, status)`, the tag join tables...). Company and skill names are **normalised into their own tables** with a unique lower-case key, so search is a join on a short indexed table instead of scanning free text. |
| **Large result sets** | Every list endpoint is **paginated**. Page size is capped at 50 server-side (`Pages`). |
| **N+1 queries** | `@EntityGraph` fetches authors and cities with posts. Comment counts for a whole feed page come from **one grouped query**. Hibernate batch fetching (`default_batch_fetch_size: 50`) handles collections. |
| **Aggregations** | Donation totals are computed with `SUM … GROUP BY` **in the database**, not in Java. |
| **Photos** | Shrunk in the **browser** to 3 MB or less before upload. That saves members' mobile data, server bandwidth and storage. The server checks the size and the real file type again. Photos are served with `Cache-Control: immutable` (cache forever), so a CDN or nginx can serve repeat views. For several backend instances, use an object-storage `ImageStorage` (S3, GCS, Cloudflare R2) or a shared volume. |
| **Feeds** | Comment counts, attendee counts and the viewer's "going" flags for a page of posts come from **3 grouped queries in total**, not one per post. Photos are batch-fetched. |
| **Notifications** | Sent in the **background after the post is saved** (`@Async` and `@TransactionalEventListener(AFTER_COMMIT)`), so posting stays fast. Audiences are read 500 phones at a time and sent to Expo 100 per request. Phones where the app was uninstalled are removed automatically. Reminders are claimed with a single conditional `UPDATE`, so several backend instances never send the same reminder twice. |
| **Read-heavy data** | The city list is cached (Caffeine) and evicted on change. For several instances, set `spring.cache.type=redis` and add the Redis starter. No code change is needed. |
| **Schema changes** | Versioned **Flyway** migrations (`db/migration/V*.sql`). Hibernate only *validates* the schema and never alters it. |
| **Operations** | `/actuator/health` (with liveness and readiness probes for Kubernetes), graceful shutdown, a connection-pool size set by environment variable, and Docker images that run as a non-root user. |
| **Frontend** | A static build served by nginx with gzip and long-term caching of hashed assets. It can also go on any CDN. |

Next steps when traffic grows:
- Add PostgreSQL **read replicas** and route read-only transactions there (the services are already marked `@Transactional(readOnly = true)`).
- For very large member counts, put referral and expert search on **PostgreSQL trigram indexes** (`pg_trgm`) or a search engine such as OpenSearch, as a new `DirectorySearchStrategy`.
- Add **rate limiting** on `/api/auth/*`, at the nginx or gateway level.

## Security

- Passwords are hashed with BCrypt. The JWT is signed with HMAC-SHA (HS256, HS384 or HS512, depending on the length of the `JWT_SECRET` you provide).
- The user is reloaded from the database on every request (a primary-key lookup), so **revoking a city admin takes effect immediately** instead of waiting for the token to expire.
- Authorisation rules:
  - Authors can change their own posts and comments.
  - City admins can moderate their own city and manage its settings, donations and causes.
  - The main admin can do everything.
  - `/api/admin/**` is restricted with `@PreAuthorize("hasRole('MAIN_ADMIN')")`.
- **Blocking** is checked on every request. The JWT filter reloads the user and refuses blocked accounts, so a block takes effect immediately rather than when the token expires. The block rule is in `AccessPolicy.canBlockMember`. Hiding a blocked member's content uses the same `PostAudience` filter and comment queries, so it's reversible and nothing is deleted.
- Post visibility ("only my city" or "everyone") is enforced on the server, never just hidden in the page. The rule lives in one class, `PostAudience`. It provides a Java check for single posts and the matching query filter, so feed, event and comment requests never load a post the viewer may not see. A post that's hidden from someone returns 404, so its existence doesn't leak.
- Input is validated with Bean Validation, for example on WhatsApp links, IFSC codes, UPI IDs, LinkedIn URLs and amount limits. LIKE wildcards in search terms are escaped.
- Mobile numbers are visible only to logged-in members. Members can opt out of search.

## Data model

```
cities ─┬─< users >── managed_city (city admins)
        │     │
        │     └── user_profiles ─┬─< profile_referral_companies >── companies
        │                        └─< profile_skills >─────────────── skills
        ├─< posts ─┬─< comments
        │          ├─< images            (photos, ordered)
        │          └─< event_attendees >── users
        ├─< campaigns
        └─< donations >── campaigns (optional)
```

## Extending

- **OTP login or password reset.** Add an `OtpSender` interface (with an SMS or WhatsApp Business API implementation) and an `/api/auth/otp` flow in `auth`. The rest of the app only sees `TokenService`.
- **Online payments.** Today money goes directly to the city bank account and the admin verifies it. To accept payments in-app, add a `PaymentGateway` interface (for example Razorpay) in `donation`. Its webhook would call the same `review(...)` method to mark a donation as verified.
- **Notifications.** Publish Spring application events from the services (for example `DonationVerified` or `CommentAdded`) and handle them in a new `notification` package.

## Mobile app

The mobile app is a React Native app built with Expo SDK 57 and Expo Router. It lives in its own repository, [pycharm0422/kamsarobar-mobile](https://github.com/pycharm0422/kamsarobar-mobile). It uses the same REST API as the website, and its API layer, link builders (WhatsApp, UPI, calendar) and photo-size rules mirror the website's.

- The login token is kept in the phone's secure storage.
- Push tokens are registered after login and removed on logout.
- Tapping a notification opens the post it's about, including when the app was closed.

Setup and build instructions are in that repository's README.
