# REST API reference

Base URL: `/api`. Requests and responses use JSON. Authenticated endpoints need the header `Authorization: Bearer <token>`. You get the token from register or login.

Errors always have this shape:

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "Validation failed",
  "fieldErrors": { "mobile": "Please enter a valid mobile number" } }
```

Paginated responses look like this:

```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 42, "totalPages": 3, "last": false }
```

Query parameters for paging are `page` (0-based) and `size` (maximum 50).

## Auth (public)

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/auth/register` | `{ name, mobile, cityId, password }` | Form 1. Returns `{ token, user }`. |
| POST | `/auth/login` | `{ mobile, password }` | Returns `{ token, user }` |

## Me

| Method | Path | Body |
|---|---|---|
| GET | `/users/me` | |
| PUT | `/users/me` | `{ name, mobile, cityId }`: edit form 1 |
| PUT | `/users/me/password` | `{ currentPassword, newPassword }` |
| GET | `/profile/me` | |
| PUT | `/profile/me` | Form 2: `{ linkedinUrl, currentCompany, position, yearsOfExperience, bio, openToHelp, referralCompanies: [..], expertise: [..] }` |
| GET | `/suggestions/companies?q=goo` | Autocomplete |
| GET | `/suggestions/expertise?q=ja` | Autocomplete |

## Directory search

| Method | Path | Notes |
|---|---|---|
| GET | `/directory/referrers?company=google&cityId=&page=` | Members who can refer to a matching company |
| GET | `/directory/experts?expertise=java&cityId=&page=` | Members with a matching expertise |

Each result (`MemberCard`) contains `userId, name, mobile` (digits with country code, ready for `https://wa.me/<mobile>`), `city`, `linkedinUrl`, `currentCompany`, `position`, `yearsOfExperience`, `bio`, `referralCompanies`, `expertise`, and `matched` (the tags that matched the search).

## Cities

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | `/cities` | public | Active cities |
| GET | `/cities/{id}` | member | Includes `whatsappGroupUrl` and `bank` |
| PUT | `/cities/{id}/settings` | that city's admin / main admin | `{ whatsappGroupUrl, bankAccountName, bankAccountNumber, bankIfsc, bankName, upiId }` |

## Posts & comments

| Method | Path | Who |
|---|---|---|
| GET | `/posts?cityId=&category=&page=` | member |
| GET | `/posts/{id}` | member |
| POST | `/posts` `{ category, title, content }` | member (posts to their own city) |
| PUT | `/posts/{id}` | author / city admin / main admin |
| DELETE | `/posts/{id}` | author / city admin / main admin |
| GET | `/posts/{id}/comments?page=` | member |
| POST | `/posts/{id}/comments` `{ content }` | member |
| PUT | `/comments/{id}` | author / city admin / main admin |
| DELETE | `/comments/{id}` | author / city admin / main admin |

Categories: `GENERAL`, `JOB_OPENING`, `HELP_NEEDED`, `EVENT`, `ANNOUNCEMENT`.

## Donations & campaigns

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | `/donations/summary` | public | `{ totalCollected, cities: [{ cityId, cityName, collected, donations, pending }] }` |
| POST | `/donations` | member | `{ cityId, campaignId?, amount, transactionRef, note, anonymous }`. Saved as `PENDING`. |
| GET | `/donations/mine` | member | |
| GET | `/cities/{id}/supporters` | member | Verified donors (anonymous donors are hidden) |
| GET | `/cities/{id}/donations?status=` | city admin / main admin | |
| PATCH | `/donations/{id}/status` | city admin / main admin | `{ status: "VERIFIED" \| "REJECTED" }` |
| GET | `/cities/{id}/campaigns?activeOnly=true` | member | Includes `raisedAmount` |
| POST | `/cities/{id}/campaigns` | city admin / main admin | `{ title, description, goalAmount, active }` |
| PUT | `/campaigns/{id}` | city admin / main admin | |
| DELETE | `/campaigns/{id}` | city admin / main admin | |

## Main admin (`/admin/**`)

| Method | Path | Notes |
|---|---|---|
| GET | `/admin/stats` | Counts and total collected |
| GET | `/admin/cities` | All cities, including hidden ones |
| POST | `/admin/cities` | `{ name, state, active }` |
| PUT | `/admin/cities/{id}` | `{ name, state, active }` |
| GET | `/admin/users?q=&cityId=&page=` | Search members |
| GET | `/admin/city-admins` | |
| POST | `/admin/city-admins` | `{ userId, cityId }`: appoint the head of a city |
| DELETE | `/admin/city-admins/{userId}` | Revoke |

## Health

`GET /actuator/health`, plus `/actuator/health/liveness` and `/actuator/health/readiness`.
