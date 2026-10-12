# How the website is made: a code tutorial

This tutorial follows **one real action** through the code. A member writes a post, adds a photo and taps **Post**. We go file by file:
- from the click in the browser,
- through the backend's security, controller and business rules,
- into the database,
- and back to the screen.

Every snippet is copied from this repository. Lines left out are marked `// ...`.

Once you can follow this one action, every other feature reads the same way. [Part E](#part-e-every-other-feature-works-the-same-way) maps them all, and [Part F](#part-f-building-it-yourself-from-an-empty-folder) shows how to build the whole thing from an empty folder.

Related documents:
- [ARCHITECTURE.md](ARCHITECTURE.md): the design and the reasons behind it.
- [CODE_FLOW.md](CODE_FLOW.md): a shorter overview of every flow.
- [API.md](API.md): every endpoint.

## Contents

- [Before you start](#before-you-start)
- [The journey at a glance](#the-journey-at-a-glance)
- [Part A: In the browser](#part-a-in-the-browser)
  - [Step 1. The website starts](#step-1-the-website-starts)
  - [Step 2. The form keeps what you type](#step-2-the-form-keeps-what-you-type)
  - [Step 3. Photos are shrunk and uploaded first](#step-3-photos-are-shrunk-and-uploaded-first)
  - [Step 4. The page sends the post](#step-4-the-page-sends-the-post)
  - [Step 5. The request travels to the backend](#step-5-the-request-travels-to-the-backend)
- [Part B: In the backend](#part-b-in-the-backend)
  - [Step 6. The backend is already running](#step-6-the-backend-is-already-running)
  - [Step 7. Who is calling? The JWT filter](#step-7-who-is-calling-the-jwt-filter)
  - [Step 8. May they call this address? Security rules](#step-8-may-they-call-this-address-security-rules)
  - [Step 9. The controller turns HTTP into Java](#step-9-the-controller-turns-http-into-java)
  - [Step 10. The service applies the business rules](#step-10-the-service-applies-the-business-rules)
  - [Step 11. Saving to the database](#step-11-saving-to-the-database)
  - [Step 12. Building the answer](#step-12-building-the-answer)
  - [Step 13. After the answer: notifications in the background](#step-13-after-the-answer-notifications-in-the-background)
- [Part C: Back in the browser](#part-c-back-in-the-browser)
  - [Step 14. The page reloads the feed](#step-14-the-page-reloads-the-feed)
  - [Step 15. React draws the posts](#step-15-react-draws-the-posts)
- [Part D: Permissions and errors](#part-d-permissions-and-errors)
  - [Step 16. Editing someone else's post: AccessPolicy](#step-16-editing-someone-elses-post-accesspolicy)
  - [Step 17. When something goes wrong](#step-17-when-something-goes-wrong)
- [Part E: Every other feature works the same way](#part-e-every-other-feature-works-the-same-way)
- [Part F: Building it yourself from an empty folder](#part-f-building-it-yourself-from-an-empty-folder)
- [Part G: Exercises](#part-g-exercises)

---

## Before you start

**Run it.** Start the backend (`cd backend && mvn spring-boot:run`) and the website (`cd frontend && npm install && npm run dev`), then open http://localhost:5173. The [README](../README.md#option-b-run-backend-and-frontend-directly-for-development) has the details. Doing the action yourself while you read helps a lot.

**Keep two folders open:**
- `frontend/src/`: the website (React).
- `backend/src/main/java/com/kamsarobar/`: the backend (Spring Boot). Each subfolder is one feature.

**Words used in this tutorial:**

| Word | Meaning here |
| --- | --- |
| REST API | The backend's list of addresses (`POST /api/posts`, `GET /api/posts/42`...). The website and the app send JSON to them and get JSON back. |
| JSON | The text format the two sides exchange: `{ "content": "Hello", "visibility": "CITY" }`. |
| JWT | The login token. A signed string the backend gives you at login. The browser sends it with every request so the backend knows who you are. |
| Component | A React function that returns what to show on screen, for example `PostForm` or `PostCard`. |
| State | Data a component remembers, such as what you typed. Changing it makes React redraw the screen. |
| Controller | A Java class that receives HTTP requests and passes them on. It contains no rules. |
| Service | A Java class that holds the business rules: what is allowed and what happens. |
| Repository | A Java interface that reads and writes database rows. |
| Entity | A Java class that matches one database table, for example `Post` ↔ `posts`. |
| DTO | A "data transfer object": a small record shaped for the API, such as `PostRequest` in and `PostResponse` out. Entities never leave the backend. |
| Migration | A numbered SQL file that creates or changes tables. It runs once, automatically. |
| Bean | An object Spring creates and hands to whoever needs it (dependency injection). |

---

## The journey at a glance

Here is the whole trip. The sections below follow it in the same order.

```mermaid
sequenceDiagram
    autonumber
    actor M as Member
    participant UI as PostForm + PostsPage<br/>(React)
    participant AX as api/client.js<br/>(axios)
    participant F as JwtAuthenticationFilter
    participant C as Controllers<br/>(ImageController,<br/>PostController)
    participant S as PostService
    participant R as PostRepository
    participant DB as Database
    participant N as NotificationService

    M->>UI: types text, picks a photo
    UI->>AX: imageApi.upload(photo) (shrunk to ≤ 3 MB)
    AX->>C: POST /api/images → ImageController saves it, returns an id
    M->>UI: taps Post
    UI->>AX: postApi.create({ content, visibility, imageIds })
    AX->>F: POST /api/posts + "Authorization: Bearer <token>"
    F->>DB: who is user 7? (reload by id)
    F->>C: request, now logged in as user 7
    C->>S: PostController.create(PostRequest, user 7)
    S->>S: check rules, set defaults
    S->>R: save(post)
    R->>DB: INSERT INTO posts ...
    S-->>C: PostResponse
    C-->>AX: 201 Created + JSON
    S--)N: PostCreatedEvent (after commit, in background)
    AX-->>UI: the new post
    UI->>AX: postApi.feed() → GET /api/posts
    AX-->>UI: page of posts
    UI-->>M: the feed shows the new post
```

---

## Part A: In the browser

### Step 1. The website starts

When you open the site, the browser loads `index.html` and one JavaScript bundle. React starts in `frontend/src/main.jsx`:

```jsx
// frontend/src/main.jsx
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
);
```

The three wrappers each do one job:
- `BrowserRouter` follows the address bar (`/posts`, `/profile`...).
- `AuthProvider` remembers who is logged in.
- `App` picks the page to show.

**Which page?** `App.jsx` maps addresses to pages. Pages that need a login are wrapped in `ProtectedRoute`:

```jsx
// frontend/src/App.jsx
const member = (element) => <ProtectedRoute>{element}</ProtectedRoute>;
// ...
<Route path="/posts" element={member(<PostsPage />)} />
<Route path="/admin" element={<ProtectedRoute roles={['MAIN_ADMIN', 'CITY_ADMIN']}><AdminPage /></ProtectedRoute>} />
```

```jsx
// frontend/src/components/ProtectedRoute.jsx
export default function ProtectedRoute({ children, roles }) {
  const { user, ready, signedOut } = useAuth();
  // ...
  if (!ready) return <Spinner />;
  if (!user) {
    return signedOut ? <Navigate to="/" replace /> : <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (roles && !roles.includes(user.role)) return <Navigate to="/posts" replace />;
  return children;
}
```

**Who is logged in?** `AuthContext.jsx` keeps the user in React state. When you log in, the backend answers `{ token, user }`, and `handleAuth` saves both:

```jsx
// frontend/src/context/AuthContext.jsx
const handleAuth = useCallback(
  (response) => {
    tokenStore.set(response.token);   // saved in localStorage
    setSignedOut(false);
    setUser(response.user);
    return response.user;
  },
  [setUser],
);
// ...
login: (body) => authApi.login(body).then(handleAuth),
```

On every start-up it also asks the backend `GET /api/users/me`, so a role that changed since last time (for example, being made a city admin) shows straight away.

> **The website hides buttons, but the backend decides.** `ProtectedRoute` only stops you from seeing a page. Every rule is checked again on the server (steps 7, 8 and 16), so changing the JavaScript can't get anyone around them.

### Step 2. The form keeps what you type

The Posts page (`pages/PostsPage.jsx`) shows `PostForm`. The form holds what you type in **state**:

```jsx
// frontend/src/components/PostForm.jsx
const EMPTY = { category: 'GENERAL', visibility: 'CITY', title: '', content: '', images: [], eventStartsAt: '', /* ... */ };

export default function PostForm({ initial, onSubmit, onCancel, submitLabel = 'Post' }) {
  const [form, setForm] = useState(initial || EMPTY);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });
  // ...
```

Each input is wired with `value={form.content}` and `onChange={set('content')}`. Every key press updates the state, and React redraws the input. This is a *controlled input*.

When you tap **Post**, `handle` runs. It checks the obvious mistake (an empty post) without bothering the server. Then it hands a clean object to `onSubmit`, a function the page passed in:

```jsx
// frontend/src/components/PostForm.jsx
const handle = async (e) => {
  e.preventDefault();                        // stop the browser reloading the page
  if (!isEvent && !form.content.trim() && form.images.length === 0) {
    setError('Write something or add a photo.');
    return;
  }
  setBusy(true);
  setError('');
  try {
    await onSubmit({
      category: form.category,
      visibility: form.visibility,           // 'CITY' or 'EVERYONE'
      title: form.title,
      content: form.content,
      imageIds: form.images.map((img) => img.id),   // photos were uploaded already (step 3)
      ...(isEvent && { eventStartsAt: fromLocalInput(form.eventStartsAt), /* ... */ }),
    });
    if (!initial) setForm({ ...EMPTY, category: form.category, visibility: form.visibility });
  } catch (err) {
    setError(errorMessage(err));             // show the server's message (step 17)
  } finally {
    setBusy(false);
  }
};
```

The form doesn't know *where* the post goes. The page decides, through `onSubmit`. That is why the same `PostForm` is reused for editing a post: `PostCard` passes a different `onSubmit`.

### Step 3. Photos are shrunk and uploaded first

Photos are sent **before** the post, one at a time, as soon as you pick them. When you tap Post, only their ids travel with the text.

```jsx
// frontend/src/components/ImagePicker.jsx
for (const file of files) {
  // ...
  const ready = await prepareImage(file);        // shrink to ≤ 3 MB, keep the shape
  const uploaded = await imageApi.upload(ready); // POST /api/images → { id, url, width, height }
  latest.current = [...latest.current, uploaded];
  onChange(latest.current);                      // PostForm stores it in form.images
}
```

`prepareImage` (`utils/imageResize.js`) works like this:
- A photo already under 3 MB in a normal format is sent untouched.
- A bigger one is redrawn on a canvas at its original proportions, rotated the right way up from its EXIF data, and saved as high-quality JPEG.
- If that's still too big, it reduces the size step by step.

On the backend, `ImageController` → `ImageService.upload` checks the photo again, because the server never trusts the browser:

```java
// backend/.../media/ImageService.java
public ImageResponse upload(MultipartFile file, Long uploaderId) {
    // ...
    if (file.getSize() > maxBytes) {
        throw new BadRequestException("Photo must be 3 MB or smaller");
    }
    // ...
    ImageFormat format = ImageFormat.detect(bytes)      // looks at the file's first bytes, not its name
            .orElseThrow(() -> new BadRequestException("Only JPG, PNG, WEBP or GIF photos are allowed"));
    // ...
    String id = UUID.randomUUID().toString();
    String key = id.substring(0, 2) + "/" + id + "." + format.extension();
    storage.save(key, bytes);                            // ImageStorage: a folder on disk today
    Image image = new Image(id, uploaderId, key, format.contentType(), bytes.length, /* width, height */);
    return ImageResponse.from(imageRepository.save(image));
}
```

The photo is saved with no post yet. Step 10 attaches it. A job deletes photos still unattached after 24 hours, so abandoned uploads don't pile up.

### Step 4. The page sends the post

`PostsPage` gave the form this `onSubmit`:

```jsx
// frontend/src/pages/PostsPage.jsx
const createPost = async (payload) => {
  await postApi.create(payload);
  if (cityId && cityId !== String(user.city.id)) setCityId(String(user.city.id));  // show your own city
  else load(0);                                                                    // reload the feed (step 14)
};
```

`postApi` is one small object per resource in `api/index.js`. Each function is one HTTP call:

```js
// frontend/src/api/index.js
const data = (request) => request.then((response) => response.data);   // keep only the JSON body

export const postApi = {
  feed: (params) => data(client.get('/posts', { params })),
  create: (body) => data(client.post('/posts', body)),
  update: (id, body) => data(client.put(`/posts/${id}`, body)),
  // ...
};
```

Every call goes through **one** axios instance, `client`. It adds the login token to every request, and logs everyone out if the token stops working:

```js
// frontend/src/api/client.js
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
});

client.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    // An expired / revoked token: sign the user out everywhere in the app.
    if (error.response?.status === 401 && tokenStore.get()) {
      tokenStore.clear();
      window.dispatchEvent(new Event('kob:logout'));
    }
    return Promise.reject(error);
  },
);
```

So the browser sends:

```http
POST /api/posts HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI3Iiwicm9sZSI6Ik1FTUJFUiIs...
Content-Type: application/json

{ "category": "GENERAL", "visibility": "CITY", "title": "", "content": "Blood donors needed at the city hospital",
  "imageIds": ["3f1c2a9e-..."] }
```

### Step 5. The request travels to the backend

The address is `/api/posts` on the **same** site as the website. Something in between forwards it to the backend:

- **In development,** the Vite dev server (port 5173) forwards it to port 8080:

  ```js
  // frontend/vite.config.js
  server: {
    port: 5173,
    proxy: {
      '/api': { target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080', changeOrigin: true },
    },
  },
  ```

- **In production,** nginx serves the website's files and forwards `/api/` to the backend container:

  ```nginx
  # frontend/nginx.conf
  location /api/ {
      proxy_pass http://backend:8080;
      # ...
  }
  location / {
      try_files $uri $uri/ /index.html;   # any other address → the React app, which routes it
  }
  ```

Because the website and the API share one address, the browser needs no special cross-site (CORS) permission.

---

## Part B: In the backend

### Step 6. The backend is already running

The backend started earlier with `mvn spring-boot:run` (or `java -jar` in Docker). Everything starts from one class:

```java
// backend/.../KamsarobarApplication.java
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class KamsarobarApplication {
    public static void main(String[] args) {
        SpringApplication.run(KamsarobarApplication.class, args);
    }
}
```

At start-up Spring Boot does four things:

1. **Reads the settings** in `application.yml`. The `dev` profile uses an H2 database file, and `prod` uses PostgreSQL. Values like `${JWT_SECRET:...}` come from environment variables, with a default after the colon.
2. **Runs Flyway.** Every `db/migration/V*.sql` file not yet applied runs once, in order. Then Hibernate checks that the tables match the entities (`ddl-auto: validate`). It never changes them itself.
3. **Creates the beans.** It finds every class marked `@RestController`, `@Service`, `@Component` or `@Configuration`, creates one of each, and passes each one what its constructor asks for. This is **dependency injection**:

   ```java
   // backend/.../post/PostService.java
   @Service
   @Transactional(readOnly = true)
   public class PostService {
       private final PostRepository postRepository;
       private final UserService userService;
       private final ImageService imageService;
       private final AccessPolicy accessPolicy;
       private final PostResponseAssembler assembler;
       private final ApplicationEventPublisher events;

       public PostService(PostRepository postRepository, UserService userService, ImageService imageService,
                          AccessPolicy accessPolicy, PostResponseAssembler assembler, ApplicationEventPublisher events) {
           // ... Spring calls this with the right objects
       }
   ```

   `PostService` never writes `new PostRepository()`. It asks for what it needs, which is why a test or a new implementation (say, S3 instead of disk for photos) can be swapped in without changing it.
4. **Starts the web server** (Tomcat, inside the jar) on port 8080. It also creates the main admin account if it doesn't exist (`config/BootstrapAdminInitializer`).

Now our request arrives.

### Step 7. Who is calling? The JWT filter

Before any controller sees the request, it passes through Spring Security's filter chain. Our `JwtAuthenticationFilter` is in that chain:

```java
// backend/.../security/JwtAuthenticationFilter.java
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith(BEARER)
            && SecurityContextHolder.getContext().getAuthentication() == null) {
        tokenService.resolveUserId(header.substring(BEARER.length()))   // 1. check the signature, read the id
                .flatMap(userRepository::findWithCitiesById)            // 2. reload the user from the database
                .filter(user -> !user.isBlocked())                      // 3. blocked users are not logged in
                .map(UserPrincipal::from)                               // 4. a small "who is this" object
                .ifPresent(principal -> {
                    var auth = new UsernamePasswordAuthenticationToken(principal, null,
                            principal.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);   // 5. remember for this request
                });
    }
    chain.doFilter(request, response);   // always continue; step 8 decides whether a login was needed
}
```

The token is checked by `JwtTokenService`. A token is the user's id plus their role and an expiry date, signed with the server's secret key (`JWT_SECRET`). Nobody can change it without breaking the signature:

```java
// backend/.../security/JwtTokenService.java
public String issueToken(UserPrincipal principal) {          // called at login and register
    Instant now = Instant.now();
    return Jwts.builder()
            .subject(String.valueOf(principal.id()))
            .claim("role", principal.role().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(validity)))         // 7 days by default
            .signWith(key)
            .compact();
}

public Optional<Long> resolveUserId(String token) {          // called on every request
    try {
        String subject = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
        return Optional.of(Long.parseLong(subject));
    } catch (JwtException | IllegalArgumentException ex) {
        return Optional.empty();                               // forged or expired → not logged in
    }
}
```

> **Why reload the user every time?** The token says "user 7", but the database says what user 7 is *now*. If an admin blocks them or changes their role, it takes effect on their very next request, not when the token expires. It costs one lookup by primary key.

The result is a `UserPrincipal`: id, role, home city and managed city. Controllers receive it as a parameter (step 9), and services pass it to `AccessPolicy` (step 16).

### Step 8. May they call this address? Security rules

`SecurityConfig` lists the few public addresses. Everything else under `/api` needs a login:

```java
// backend/.../config/SecurityConfig.java
http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))   // no server session
        .exceptionHandling(eh -> eh.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()                         // login, register
                .requestMatchers(HttpMethod.GET, "/api/cities").permitAll()          // the sign-up form's city list
                .requestMatchers(HttpMethod.GET, "/api/donations/summary").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/images/*").permitAll()        // photos in <img> tags
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/**").authenticated()                         // everything else
                .anyRequest().permitAll())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

`POST /api/posts` needs a login. Step 7 logged in user 7, so the request goes on. Without a valid token it would stop here with **401**, and the website's axios interceptor (step 4) would log the person out.

Admin controllers add a role check on top:

```java
// backend/.../admin/AdminController.java
@PreAuthorize("hasRole('MAIN_ADMIN')")
public class AdminController { /* ... */ }
```

Security works in three levels: the **URL** (this step), the **role** (`@PreAuthorize`), and the **data** (`AccessPolicy`, step 16).

### Step 9. The controller turns HTTP into Java

Spring finds the method whose annotations match `POST /api/posts`:

```java
// backend/.../post/PostController.java
@RestController
@RequestMapping("/api")
public class PostController {

    private final PostService postService;
    // ...

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@Valid @RequestBody PostRequest request, @AuthenticationPrincipal UserPrincipal actor) {
        return postService.create(request, actor);
    }
```

That one method does four things through its annotations:
- `@RequestBody`: Jackson turns the JSON body into a `PostRequest` object.
- `@Valid`: checks the annotations on `PostRequest` *before* the method runs.
- `@AuthenticationPrincipal`: hands over the `UserPrincipal` from step 7.
- The return value becomes the JSON response, with status 201.

The request DTO is a Java **record**: a plain, read-only bundle of fields with its validation rules:

```java
// backend/.../post/dto/PostRequest.java
public record PostRequest(
        PostCategory category,
        /** CITY (default) = only members living in the author's city; EVERYONE = all cities. */
        PostVisibility visibility,
        @Size(max = 200) String title,
        @Size(max = 5000) String content,
        @Size(max = 6, message = "You can add at most 6 photos") List<String> imageIds,
        Instant eventStartsAt,
        Instant eventEndsAt,
        @Size(max = 300) String eventLocation,
        @Size(max = 500) @Pattern(regexp = "^$|^https?://.+$", message = "Online link must start with http:// or https://")
        String eventLink) {
}
```

If the content were 6,000 characters, `@Valid` would fail and the controller method would never run. The error handler would answer 400 with `fieldErrors: { "content": "..." }` (step 17).

> **Controllers stay thin.** A controller has no `if` about business rules. It only converts and delegates. The rules live in the service, where the website, the app and the tests all reach them the same way.

### Step 10. The service applies the business rules

This is the heart of the feature:

```java
// backend/.../post/PostService.java
@Transactional
public PostResponse create(PostRequest request, UserPrincipal actor) {
    User author = userService.getEntity(actor.id());
    PostCategory category = categoryOrDefault(request);          // no category → GENERAL
    validate(request, category, true);                           // the rules below
    Post post = new Post(author, author.getCity(), category, TextNormalizer.clean(request.title()),
            trimToNull(request.content()));                      // posted in the AUTHOR'S city
    post.setVisibility(visibilityOrDefault(request));            // no visibility → CITY
    applyEvent(post, request, category);                         // event date/place only for seminars and events
    postRepository.save(post);                                   // step 11
    imageService.setPostImages(post, request.imageIds(), actor.id());   // attach the photos from step 3
    events.publishEvent(new PostCreatedEvent(post.getId()));     // step 13
    return assembler.toResponse(post, actor);                    // step 12
}
```

The business rules, in plain words:

| Rule | Where in the code |
| --- | --- |
| A post goes to the city the author **lives in**. They can't post into another city. | `new Post(author, author.getCity(), ...)` |
| If no one chose, a post is for **their city only**. | `visibilityOrDefault`: `null → PostVisibility.CITY` |
| A normal post needs text **or** a photo. | `validate`, below |
| A seminar or event needs a title and a start time in the future, and must end after it starts. | `validate`, below |
| Photos must be your own fresh uploads, or already on this post, and at most 6. | `ImageService.setPostImages` |
| Everyone who should hear about it is notified, after it is saved. | `PostCreatedEvent` → step 13 |

```java
// backend/.../post/PostService.java
private static void validate(PostRequest request, PostCategory category, boolean creating) {
    boolean hasText = trimToNull(request.content()) != null;
    boolean hasPhotos = request.imageIds() != null && !request.imageIds().isEmpty();
    if (!category.isEvent() && !hasText && !hasPhotos) {
        throw new BadRequestException("Write something or add a photo");
    }
    if (!category.isEvent()) {
        return;
    }
    if (TextNormalizer.clean(request.title()) == null) {
        throw new BadRequestException("Please give the event a title");
    }
    if (request.eventStartsAt() == null) {
        throw new BadRequestException("Please add the date and time of the event");
    }
    if (creating && request.eventStartsAt().isBefore(Instant.now())) {
        throw new BadRequestException("The event date must be in the future");
    }
    if (request.eventEndsAt() != null && !request.eventEndsAt().isAfter(request.eventStartsAt())) {
        throw new BadRequestException("The event must end after it starts");
    }
}
```

Two things here are worth understanding.

**Why check "text or photo" here when the form already did?** The form's check is a courtesy, so you get a quick message. This check is the real one. A request can also come from the mobile app, an old browser tab or a script, and they all pass through here.

**What `@Transactional` gives you.** The class is marked `@Transactional(readOnly = true)`, and write methods like `create` say `@Transactional` again to allow writing. Spring opens a database transaction when `create` starts and commits it when `create` returns. If anything throws halfway (say, a photo id is invalid), **everything is rolled back**, and no post is left without its photos.

### Step 11. Saving to the database

`postRepository.save(post)` is all the service writes. The repository is an **interface**. Spring Data writes the implementation at start-up:

```java
// backend/.../post/PostRepository.java
public interface PostRepository extends JpaRepository<Post, Long> {
    // save, findById, delete, count... come for free from JpaRepository
    // custom queries are declared with @Query (see step 14)
}
```

The entity says how a `Post` object maps to the `posts` table:

```java
// backend/.../post/Post.java
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)        // the database numbers the rows
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)          // posts.author_id → users.id
    private User author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "city_id", nullable = false)            // posts.city_id → cities.id
    private City city;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private PostCategory category;                             // stored as text: 'GENERAL', 'SEMINAR'...

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private PostVisibility visibility = PostVisibility.CITY;
    // ...
    @OneToMany(mappedBy = "post")
    private List<Image> images;                                // images.post_id → posts.id
    // ...
    @CreationTimestamp
    private LocalDateTime createdAt;                           // filled in automatically
```

The table itself was created by a migration, and later migrations added columns:

```sql
-- backend/src/main/resources/db/migration/V1__init_schema.sql
CREATE TABLE posts (
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    author_id  BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    city_id    BIGINT        NOT NULL REFERENCES cities (id),
    category   VARCHAR(30)   NOT NULL,
    -- ...
);
-- V3__post_images_and_events.sql adds event_starts_at, event_ends_at, event_location, event_link
-- V4__post_visibility.sql adds visibility
```

Hibernate (the JPA implementation) turns `save` into SQL, roughly `INSERT INTO posts (author_id, city_id, category, visibility, content, ...) VALUES (7, 3, 'GENERAL', 'CITY', 'Blood donors needed...', ...)`. Because the database numbers the rows (`GenerationType.IDENTITY`), this INSERT runs straight away, so the post has its id for the next lines. Attaching the photos is just changing objects: `image.attachTo(post, position)` sets each image's post. Hibernate notices the change and writes `UPDATE images SET post_id = ...` when the transaction commits. No SQL is written by hand for any of this.

### Step 12. Building the answer

The service doesn't return the `Post` entity. It returns a `PostResponse` DTO built by `PostResponseAssembler`. There are two reasons:

1. **The answer is shaped for the screen, not the table.** The screen needs the comment count, whether *you* may edit, and, for events, how many are going and whether you are. None of those are columns of `posts`.
2. **Nothing private leaks.** An entity would drag along everything linked to it, such as the author's password hash. A DTO lists exactly what is sent.

```java
// backend/.../post/PostResponseAssembler.java
public List<PostResponse> toResponses(List<Post> posts, UserPrincipal viewer) {
    // ...
    List<Long> ids = posts.stream().map(Post::getId).toList();
    Map<Long, Long> comments = counts(commentRepository.countByPostIds(ids));          // ONE query for all posts
    // ... the same for attendee counts and "am I going"
    return posts.stream().map(post -> PostResponse.from(post,
            comments.getOrDefault(post.getId(), 0L),
            accessPolicy.canModifyContent(viewer, post.getAuthor().getId(), post.getCity().getId()),   // canEdit
            EventInfo.of(post, attendees.getOrDefault(post.getId(), 0L), attending.contains(post.getId()))))
            .toList();
}
```

```java
// backend/.../post/dto/PostResponse.java
public record PostResponse(Long id, PostCategory category, PostVisibility visibility, String title, String content,
                           AuthorSummary author, CitySummary city, List<ImageResponse> images, EventInfo event,
                           long commentCount, boolean canEdit, LocalDateTime createdAt, LocalDateTime updatedAt) {
    // ...
}
```

> **The "N+1 queries" trap.** Counting comments with one query *per post* would mean 21 queries for a feed of 20 posts. The assembler counts for all of them at once (`countByPostIds(ids)` is a `GROUP BY` query), so a page of any size costs the same few queries.

Spring turns the record into JSON, and the controller's `@ResponseStatus(HttpStatus.CREATED)` makes it a 201:

```json
{ "id": 42, "category": "GENERAL", "visibility": "CITY", "title": null,
  "content": "Blood donors needed at the city hospital",
  "author": { "id": 7, "name": "Imran Khan" }, "city": { "id": 3, "name": "Delhi", "state": "Delhi", "active": true },
  "images": [{ "id": "3f1c2a9e-...", "url": "/images/3f1c2a9e-...", "width": 1600, "height": 1200 }],
  "event": null, "commentCount": 0, "canEdit": true, "createdAt": "2026-10-12T10:15:03", "updatedAt": "..." }
```

### Step 13. After the answer: notifications in the background

Back in step 10, the service published a `PostCreatedEvent`. The service doesn't know who listens. The notification package does:

```java
// backend/.../notification/NotificationService.java
@Async                                                              // on a background thread
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)  // only once the post is really saved
public void onPostCreated(PostCreatedEvent event) {
    // load the post; find the phones of members who should hear about it, 500 at a time;
    // send them through PushSender (Expo) in batches of 100; forget phones that uninstalled the app
}
```

The two annotations matter:
- **`AFTER_COMMIT`:** if saving failed and was rolled back, nobody is told about a post that doesn't exist.
- **`@Async`:** the member's request doesn't wait while thousands of phones are notified. Posting stays fast.

Who receives it is a business rule too: members of the post's city who left *new posts* switched on, never the author, never blocked members. `PushSender` is an interface. `ExpoPushSender` sends real notifications, and `LoggingPushSender` (`PUSH_PROVIDER=log`) only writes to the log, which is handy without internet.

---

## Part C: Back in the browser

### Step 14. The page reloads the feed

`postApi.create` resolved, so `createPost` (step 4) calls `load(0)`:

```jsx
// frontend/src/pages/PostsPage.jsx
const load = useCallback(
  (page = 0) => {
    setFeed(null);                                   // show a spinner
    postApi
      .feed({ cityId: cityId || undefined, category: category || undefined, page })
      .then(setFeed)                                 // store the page of posts in state
      .catch((e) => setError(errorMessage(e)));
  },
  [cityId, category],
);

useEffect(() => load(0), [load]);                    // also runs whenever the city or category filter changes
```

That is `GET /api/posts?cityId=3&page=0`. It passes through steps 7–9 the same way and reaches:

```java
// backend/.../post/PostService.java
public PageResponse<PostResponse> feed(Long cityId, PostCategory category, Pageable pageable, UserPrincipal viewer) {
    return assembler.toPage(postRepository.findFeed(cityId, category, PostAudience.of(viewer), pageable), viewer);
}
```

The query is where "city-segregated" really happens. It has two filters:

```java
// backend/.../post/PostRepository.java
@EntityGraph(attributePaths = {"author", "city"})            // load author and city in the same query
@Query(value = """
        select p from Post p
        where (:cityId is null or p.city.id = :cityId or p.visibility = com.kamsarobar.post.PostVisibility.EVERYONE)
          and (:category is null or p.category = :category)
          and """ + PostAudience.JPQL_FILTER + """

        order by p.createdAt desc
        """, /* countQuery = ... */)
Page<Post> findFeed(/* ... */);
```

- **The city filter** (first line): posts from the chosen city, plus every post shared with everyone.
- **The audience filter** (`PostAudience`): never return a post this viewer isn't allowed to see.

```java
// backend/.../post/PostAudience.java
public static final String JPQL_FILTER = """
        (p.author.blocked = false and (:seesAll = true
         or p.visibility = com.kamsarobar.post.PostVisibility.EVERYONE
         or p.city.id = :viewerCityId
         or p.city.id = :viewerManagedCityId
         or p.author.id = :viewerId))""";

public boolean canSee(Post post) {          // the same rule in Java, for opening one post by id
    if (post.getAuthor().isBlocked()) {
        return false;
    }
    Long postCity = post.getCity().getId();
    return seesAll
            || post.getVisibility() == PostVisibility.EVERYONE
            || Objects.equals(postCity, cityId)
            || Objects.equals(postCity, managedCityId)
            || Objects.equals(post.getAuthor().getId(), viewerId);
}
```

The rule lives in **one class**, written once for the database and once in Java, side by side, so they can't drift apart. Filtering in the database (rather than loading everything and throwing rows away in Java) keeps paging correct and never loads a hidden post.

The answer is a page:

```json
{ "content": [ { "id": 42, "content": "Blood donors needed...", "canEdit": true, "...": "..." }, "..." ],
  "page": 0, "size": 10, "totalElements": 57, "totalPages": 6, "last": false }
```

### Step 15. React draws the posts

`setFeed(page)` changes state, so React runs `PostsPage` again and draws one `PostCard` per post:

```jsx
// frontend/src/pages/PostsPage.jsx (simplified)
{feed.content.map((p) => (
  <PostCard key={p.id} post={p} onUpdate={updatePost} onDelete={deletePost} onChange={replace} />
))}
```

`PostCard` shows the author, the city, "🏙 Delhi only" or "🌐 Everyone", the photos (`PostImages`), the date and place for events (`EventDetails`), and the comment count. It shows **Edit** and **Delete** only when the server said `canEdit: true`:

```jsx
// frontend/src/components/PostCard.jsx
{post.canEdit && (
  // ... Edit button
  <button className="link-btn danger" onClick={() => window.confirm('Delete this post?') && onDelete(post.id)}>Delete</button>
)}
```

That's the full trip: click → state → API client → proxy → filter → security → controller → service → repository → database → DTO → JSON → state → screen.

---

## Part D: Permissions and errors

### Step 16. Editing someone else's post: AccessPolicy

Creating a post needs no special permission. Editing does. `PUT /api/posts/42` reaches `PostService.update`, which asks **one** class before changing anything:

```java
// backend/.../post/PostService.java
@Transactional
public PostResponse update(Long id, PostRequest request, UserPrincipal actor) {
    Post post = getEntity(id);                                                         // 404 if it doesn't exist
    accessPolicy.requireContentModifier(actor, post.getAuthor().getId(), post.getCity().getId());   // 403 if not allowed
    // ... validate and apply the change
}
```

```java
// backend/.../access/RoleBasedAccessPolicy.java
public boolean canManageCity(UserPrincipal user, Long cityId) {
    if (user == null) {
        return false;
    }
    return user.isMainAdmin()
            || (user.role() == Role.CITY_ADMIN && Objects.equals(user.managedCityId(), cityId));
}

public boolean canModifyContent(UserPrincipal user, Long authorId, Long cityId) {
    if (user == null) {
        return false;
    }
    return Objects.equals(user.id(), authorId) || canManageCity(user, cityId);   // the author, or the city's admin
}
```

`requireContentModifier` (a default method on the `AccessPolicy` interface) throws `ForbiddenException("You can only change your own content")` when the answer is no.

Every permission in the app goes through this class: editing and deleting posts and comments, managing a city's settings and donations, and blocking members. The same method also fills `canEdit` in step 12, so the button and the rule can't disagree. To change a rule, you change one method, and `RoleBasedAccessPolicyTest` checks it.

### Step 17. When something goes wrong

Services never build error responses. They **throw** an exception that says what went wrong:

| Exception | Thrown when | Becomes |
| --- | --- | --- |
| `BadRequestException` | A rule in the input is broken ("Write something or add a photo") | 400 |
| `MethodArgumentNotValidException` (by `@Valid`) | A DTO annotation failed | 400 with `fieldErrors` |
| (no exception: Spring Security answers) | No login, or a bad token | 401 |
| `ForbiddenException` | `AccessPolicy` said no | 403 |
| `ResourceNotFoundException` | No such post, or one you may not see | 404 |
| `ConflictException`, `CityHasAdminException` | Mobile already registered, city already has an admin | 409, plus `code` |
| anything else | A bug | 500 with a polite message, and the details in the log |

One class catches them all and builds the same JSON shape:

```java
// backend/.../common/exception/GlobalExceptionHandler.java
@ExceptionHandler({BadRequestException.class, MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class})
public ResponseEntity<ApiError> handleBadRequest(Exception ex) {
    String message = ex instanceof BadRequestException ? ex.getMessage() : "Malformed request";
    return build(HttpStatus.BAD_REQUEST, message, null);
}

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
        errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
    }
    return build(HttpStatus.BAD_REQUEST, "Validation failed", errors);
}
// ... one handler per kind, and a last one for Exception.class
```

```json
{ "timestamp": "2026-10-12T10:15:03", "status": 400, "error": "Bad Request",
  "message": "Write something or add a photo", "fieldErrors": null, "code": null }
```

Because the transaction is rolled back (step 10), a failed request leaves nothing half-saved.

In the browser, the axios call rejects. `PostForm`'s `catch` passes the error to `errorMessage`, which picks the friendliest text available:

```js
// frontend/src/utils/errors.js
export function errorMessage(error) {
  const body = error?.response?.data;
  if (body?.fieldErrors) {
    return Object.values(body.fieldErrors).join('. ');
  }
  if (body?.message) {
    return body.message;
  }
  if (error?.code === 'ECONNABORTED' || !error?.response) {
    return 'Cannot reach the server. Please check your connection.';
  }
  return 'Something went wrong. Please try again.';
}
```

The form shows the message in an `Alert`, and the member's text stays in the form, so nothing is lost.

---

## Part E: Every other feature works the same way

Every feature follows the same layers. Once you know where a feature's page is, you can find every other part of it:

| Feature | Page / component (`frontend/src/`) | API object | Controller → Service | Repository, tables |
| --- | --- | --- | --- | --- |
| Register, log in | `pages/RegisterPage.jsx`, `LoginPage.jsx`, `context/AuthContext.jsx` | `authApi` | `auth/AuthController` → `AuthService` | `UserRepository`; `users` |
| Basic info, password | `pages/ProfilePage.jsx` | `userApi` | `user/UserController` → `UserService` | `UserRepository`; `users` |
| Work profile (form 2) | `pages/ProfilePage.jsx`, `components/TagInput.jsx` | `profileApi` | `profile/ProfileController` → `ProfileService`, `TagResolver` | `UserProfileRepository`, `CompanyRepository`, `SkillRepository` |
| Referral and expert search | `pages/FindReferralPage.jsx`, `FindExpertPage.jsx`, `components/DirectorySearch.jsx` | `directoryApi` | `directory/DirectoryController` → `DirectoryService` → a `DirectorySearchStrategy` | `UserProfileRepository` |
| Posts | `pages/PostsPage.jsx`, `PostDetailPage.jsx`, `components/PostForm.jsx`, `PostCard.jsx` | `postApi` | `post/PostController` → `PostService` | `PostRepository`; `posts` |
| Comments | `pages/PostDetailPage.jsx` | `postApi` | `post/PostController` → `CommentService` | `CommentRepository`; `comments` |
| Photos | `components/ImagePicker.jsx`, `PostImages.jsx` | `imageApi` | `media/ImageController` → `ImageService`, `ImageStorage` | `ImageRepository`; `images` |
| Events | `pages/EventsPage.jsx`, `components/EventActions.jsx` | `eventApi` | `event/EventController` → `EventService` | `PostRepository`, `EventAttendeeRepository` |
| Donations, causes | `pages/DonatePage.jsx`, `pages/admin/CityManagePanel.jsx` | `donationApi`, `campaignApi` | `donation/DonationController` → `DonationService`, `CampaignService` | `DonationRepository`, `CampaignRepository` |
| City settings | `pages/admin/CityManagePanel.jsx` | `cityApi` | `city/CityController` → `CityService` | `CityRepository`; `cities` |
| Main admin | `pages/admin/*` | `adminApi` | `admin/AdminController` → `AdminService` | users, cities |
| Members, blocking | `pages/admin/MembersPanel.jsx`, `MemberPage.jsx` | `adminApi` | `admin/MemberAdminController` → `MemberAdminService` | users, posts, comments |
| Push notifications | mobile app | (mobile `notificationApi`) | `notification/NotificationController` → `DeviceService`, `NotificationService` | `DeviceTokenRepository`; `device_tokens` |

Three design patterns appear across features. They are worth recognising when you read the code.

**1. Strategy: one search, several ways to search.** Referral search and expert search are the same screen and the same service. Only the query differs. Each query is a class implementing `DirectorySearchStrategy`, and the service picks one by type:

```java
// backend/.../directory/DirectoryService.java
public DirectoryService(List<DirectorySearchStrategy> strategies) {          // Spring passes every strategy bean
    strategies.forEach(strategy -> this.strategies.put(strategy.type(), strategy));
}

public PageResponse<MemberCard> search(SearchType type, String term, Long cityId, Long viewerId, Pageable pageable) {
    String key = TextNormalizer.key(term);
    if (key == null || key.length() < 2) {
        throw new BadRequestException("Type at least 2 characters to search");
    }
    DirectorySearchStrategy strategy = strategies.get(type);
    // ...
    Page<UserProfile> page = strategy.search(TextNormalizer.likeContains(key), cityId, viewerId, pageable);
    return PageResponse.of(page, profile -> MemberCard.from(profile, matchedTags(type, profile, key)));
}
```

A third kind of search is one new class. Nothing else changes. On the website, `DirectorySearch.jsx` is the matching generic component, and the two pages only pass it labels and a message builder.

**2. An interface at the edge.** Photo storage and push sending talk to the outside world, so they hide behind small interfaces:

```java
// backend/.../media/ImageStorage.java
public interface ImageStorage {
    void save(String key, byte[] content);
    Resource load(String key);
    void delete(String key);
}
```

`FileSystemImageStorage` is today's implementation. An `S3ImageStorage` would be a new class, and `ImageService` wouldn't change, because it only knows the interface (step 6). `TokenService`, `PushSender` and `AccessPolicy` follow the same idea.

**3. Events between features.** When a member moves city, their city-only posts move with them. The user package must not depend on posts, so it announces the move instead:

```java
// backend/.../user/UserService.java
public void moveToCity(User user, City city) {
    Long from = user.getCity().getId();
    if (from.equals(city.getId())) {
        return;
    }
    user.setCity(city);
    events.publishEvent(new MemberMovedEvent(user.getId(), from, city.getId()));
}
```

```java
// backend/.../post/MemberPostsMover.java
@EventListener                                   // runs inside the same transaction as the move
public void onMemberMoved(MemberMovedEvent event) {
    Instant now = Instant.now();
    postRepository.moveCityOnlyPosts(event.userId(), event.fromCityId(), event.toCityId(), now,
            now.minus(Post.DEFAULT_EVENT_DURATION));
}
```

Compare with step 13. There, `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` runs *after* saving, in the background. Here, a plain `@EventListener` runs *inside* the same transaction, so the move and the posts commit or roll back together. Choose the first for side effects such as notifications, and the second for changes that must stay consistent.

---

## Part F: Building it yourself from an empty folder

This is the order the app was built in, with the files to look at for the finished version of each step. Build and test each step before the next.

**Backend**

1. **Create the project.** At [start.spring.io](https://start.spring.io) choose:
   - Maven, Java 21, group `com.kamsarobar`;
   - the dependencies **Spring Web, Spring Data JPA, Spring Security, Validation, Spring Cache Abstraction, Spring Boot Actuator, Flyway Migration, PostgreSQL Driver, H2 Database**.

   Then add `caffeine` and the three `jjwt` artifacts to `pom.xml` by hand (see `backend/pom.xml`). Run `mvn spring-boot:run` to check it starts.
2. **Settings and profiles.** Write `application.yml`:
   - a `dev` profile with an H2 file database (`MODE=PostgreSQL`, so the same SQL works on both);
   - a `prod` profile reading `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`;
   - `ddl-auto: validate`.
3. **The first migration.** `db/migration/V1__init_schema.sql` with the `cities` and `users` tables, then `V2__seed_cities.sql`. Start the app and check the tables exist (the H2 console is at `/h2-console` in dev).
4. **The first entity and repository.** `city/City.java` (`@Entity`) and `CityRepository extends JpaRepository<City, Long>`.
5. **The shared pieces.** `common/exception/*` and `GlobalExceptionHandler` first, so every later feature gets proper errors for free. Then `common/web/PageResponse` and `Pages`.
6. **The first endpoint.** `CityService` + `CityController` with `GET /api/cities`. Test it: `curl http://localhost:8080/api/cities`.
7. **Users and security.** In this order:
   1. `user/User.java` and `Role`;
   2. a BCrypt `PasswordEncoder` bean (in `SecurityConfig`);
   3. `security/TokenService` + `JwtTokenService`;
   4. `UserPrincipal`;
   5. `JwtAuthenticationFilter`;
   6. `config/SecurityConfig`;
   7. `auth/AuthService` + `AuthController` (register and login).

   Test: register with curl, copy the token, then call a protected address with `-H "Authorization: Bearer <token>"`.
8. **Permissions in one place.** `access/AccessPolicy` + `RoleBasedAccessPolicy`, with a unit test (`RoleBasedAccessPolicyTest`).
9. **Features, one at a time:** profile, directory search, posts and comments, donations, admin. Each follows the [recipe](CODE_FLOW.md#recipe-adding-a-new-feature): migration → entity + repository → DTOs → service → controller → integration test.
10. **Integration tests.** `@SpringBootTest` + `MockMvc` tests that call the real API on an in-memory H2 database (see `backend/src/test/java/com/kamsarobar/*IntegrationTest.java`). Run them with `mvn test` after every change.

**Website**

11. **Create the project:**

    ```bash
    npm create vite@latest frontend -- --template react
    cd frontend
    npm install axios react-router-dom
    ```

12. **Proxy `/api` to the backend** in `vite.config.js` (step 5).
13. **The API client.** `api/client.js` (axios + token interceptor), then `api/index.js` with one object per resource.
14. **Login state.** `context/AuthContext.jsx`, then `LoginPage` and `RegisterPage`, then `ProtectedRoute` and the routes in `App.jsx`.
15. **One page per feature,** pulling repeated pieces into `components/` (`PostCard`, `MemberCard`, `CitySelect`...) and helpers into `utils/` (WhatsApp links, formatting, photo shrinking).
16. **Styling.** One `styles.css` with CSS variables for colours and spacing, and `@media` rules for small screens.

**Packaging**

17. **Docker.** A two-stage `Dockerfile` for each side: build with Maven or Node, then run on a small JRE or nginx image. Add `frontend/nginx.conf` (step 5) and `docker-compose.yml` with `db`, `backend` and `frontend`. Then `docker compose up --build` runs everything.

**Later additions** each followed the same recipe with a new migration (`V3` to `V6`): photos and events, post visibility, blocking, and push notifications. Then the mobile app was added, in its own repository.

---

## Part G: Exercises

Small changes are the best way to check you understand the flow. Each exercise touches several layers. Run `mvn test` and `npm run build` after each one.

1. **Show the author's job on post cards** ("Software Engineer at Google").
   - *Backend:* add `position` and `company` to `post/dto/AuthorSummary.java`, read from the author's `UserProfile`. Watch for extra queries: load the profiles for the whole page at once, the way `PostResponseAssembler` loads comment counts.
   - *Website:* show it in `PostCard.jsx`.
2. **Limit posts to 2,000 characters.**
   - *Backend:* change `@Size(max = 5000)` on `content` in `PostRequest`, and add a test that posts 2,001 characters and expects 400.
   - *Website:* add `maxLength={2000}` to the textarea in `PostForm.jsx`, so people see the limit before the server enforces it.
3. **Add a "Lost and found" post category.**
   - Add `LOST_AND_FOUND` to `post/PostCategory.java`.
   - Add a label to `CATEGORY_LABELS` in `frontend/src/utils/format.js`, and to the same object in the mobile app's `src/utils/format.js`.
   - No migration is needed: `category` is stored as text.
   - Question to answer from the code: does it appear in the feed's category filter automatically?
4. **Let city admins pin one announcement to the top of their city's feed.**
   - A new migration adds `pinned BOOLEAN DEFAULT FALSE` to `posts`.
   - Add a new `AccessPolicy` rule (`canManageCity`), a new endpoint, and an `order by p.pinned desc, p.createdAt desc` in `findFeed`.
   - Add a pin button for admins on `PostCard`.
   - Write the integration test first.
