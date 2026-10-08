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

## Photos

| Method | Path | Who | Notes |
|---|---|---|---|
| POST | `/images` (multipart, field `file`) | member | One photo, maximum 3 MB, in JPG, PNG, WEBP or GIF. The type is checked from the file contents. Returns `{ id, url, width, height }`. The frontend shrinks bigger photos first. |
| GET | `/images/{id}` | public | Serves the photo with `Cache-Control: immutable`. Ids are random UUIDs. |

Upload photos first, then send their ids in `imageIds` when you create or edit a post. Photos that are never attached to a post, or are removed from one, are deleted by an hourly job after 24 hours.

## Posts & comments

| Method | Path | Who |
|---|---|---|
| GET | `/posts?cityId=&category=&page=` | member |
| GET | `/posts/{id}` | member |
| POST | `/posts` | member (posts to their own city) |
| PUT | `/posts/{id}` | author / city admin / main admin |
| DELETE | `/posts/{id}` | author / city admin / main admin |
| GET | `/posts/{id}/comments?page=` | member |
| POST | `/posts/{id}/comments` `{ content }` | member |
| PUT | `/comments/{id}` | author / city admin / main admin |
| DELETE | `/comments/{id}` | author / city admin / main admin |

Post body:

```json
{ "category": "SEMINAR", "visibility": "CITY", "title": "Career seminar", "content": "...", "imageIds": ["<uuid>"],
  "eventStartsAt": "2026-10-12T12:00:00Z", "eventEndsAt": null,
  "eventLocation": "Community hall", "eventLink": "https://meet.google.com/..." }
```

Categories: `GENERAL`, `JOB_OPENING`, `HELP_NEEDED`, `SEMINAR`, `EVENT`, `ANNOUNCEMENT`.

- `visibility` controls who can see the post:
  - `CITY` (the default) means only members living in the author's city.
  - `EVERYONE` means members of all cities.
- Filtering by city (`/posts?cityId=`, `/events/upcoming?cityId=`) returns that city's posts **plus every post shared with `EVERYONE`**, from any city.
- A post that is `CITY` and belongs to another city is filtered out of `/posts` and `/events/*`. For an outsider, `GET /posts/{id}`, its comments and its attendance endpoints return **404**, as if the post didn't exist. That city's admin, the main admin and the author can always see it.
- A normal post needs text **or** at least one photo. The title is optional. At most 6 photos.
- `SEMINAR` and `EVENT` posts need a `title` and a future `eventStartsAt`. The `event*` fields are ignored for other categories.
- Times are ISO-8601 instants (UTC).
- Each post in a response includes `images: [{ id, url, width, height }]`. Event posts also include `event: { startsAt, endsAt, location, link, attendeeCount, attending, ended }`.

## Events & seminars

| Method | Path | Notes |
|---|---|---|
| PUT | `/posts/{id}/attendance` | Add to my upcoming events (doing it twice has no extra effect). Returns the updated post. |
| DELETE | `/posts/{id}/attendance` | Remove from my events |
| GET | `/events/mine?page=` | My upcoming events, soonest first |
| GET | `/events/upcoming?cityId=&page=` | Upcoming events in a city plus events shared with everyone from any city, or in all cities if `cityId` is omitted |

An event counts as upcoming until its end time. If it has no end time, it counts as upcoming until 6 hours after it starts.

## Donations & campaigns

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | `/cities/{id}/donation-summary` | member | `{ cityId, cityName, collected, donations, pending }`. The app shows the member's own city by default. |
| GET | `/donations/summary` | public | Overall total plus a per-city breakdown. Used for logged-out visitors (total only) and admin reports. |
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

## Member management (main admin: all cities · city admin: own city)

| Method | Path | Notes |
|---|---|---|
| GET | `/admin/members?q=&cityId=&status=ACTIVE\|BLOCKED&page=` | Lists members. For a city admin, results are limited to the city they manage, and asking for another city returns 403. |
| GET | `/admin/members/{id}` | `{ member, profile, activity: { posts, comments, donations, donatedVerified, eventsAdded }, block: { blocked, reason, blockedAt, blockedBy }, canBlock }` |
| POST | `/admin/members/{id}/block` | `{ reason }` (required). Who can block: the main admin can block anyone but themselves; a city admin can block ordinary members of their own city. Nobody can block the main admin. |
| DELETE | `/admin/members/{id}/block` | Unblock, which restores everything |

**What blocking does:**
- The member's existing login stops working at once (401), and logging in returns 403 with a "blocked" message.
- Registering again with the same number returns 409.
- They're left out of directory search.
- Their posts and comments are hidden everywhere, including comment counts.
- Nothing is deleted.

## Push notifications (mobile app)

| Method | Path | Notes |
|---|---|---|
| PUT | `/notifications/devices` | `{ token: "ExponentPushToken[...]", platform: "android"\|"ios" }`. Registers this phone for the logged-in member. If the phone is already registered to someone else, it moves to the current member. |
| DELETE | `/notifications/devices?token=...` | On logout |
| GET | `/notifications/settings` | `{ cityPosts, cityEvents, allEvents, eventReminders }` |
| PUT | `/notifications/settings` | Same shape as GET |

**Who receives what:**
- **New post:** members living in the post's city with `cityPosts` on.
- **New event or seminar:** members of the city with `cityEvents` on. If the event is shared with everyone, also members of other cities with `allEvents` on.
- **Reminder:** about 60 minutes before an event the member added (`eventReminders`). Each reminder is sent once.
- The author and blocked members are never notified.

Each notification's `data` is `{ type: "post"|"event"|"reminder", postId }`. The app uses it to open the post.

## Health

`GET /actuator/health`, plus `/actuator/health/liveness` and `/actuator/health/readiness`.
