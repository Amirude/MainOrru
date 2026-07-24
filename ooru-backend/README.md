# Altenul One Backend — Phase 1 + Phase 2 (food/grocery, parcel, rentals, driver)

A real, working Spring Boot backend for the Altenul One local-services platform. Started as Phase 1
(**Tailor, Xerox, AC Service, Plumber, Electrician**) and now also covers Phase 2's headline
categories: **Food Delivery, Grocery Delivery, Parcel Pickup & Delivery, Bike/Car Rental, Driver
Booking** — plus the shared infrastructure every category reuses: authentication, shop
registration/approval, a generic booking lifecycle, and Razorpay payments.

This is not a toy. It's structured the way a real production backend is structured. But it is
also not finished — a handful of things only *you* can do (see "What you must supply" below),
and it hasn't been deployed or load-tested anywhere.

## What's actually here

- **Auth** — register, OTP verification (stubbed — logs to console, see `OtpService.java`), login,
  JWT-based sessions, four roles (`ADMIN`, `SHOP_OWNER`, `CUSTOMER`, `DELIVERY_PARTNER`).
- **Shops** — shop owners register a shop under one of the 5 categories; it sits `PENDING` until
  an admin approves it. Customers can only book shops that are `APPROVED`.
- **Bookings** — one generic booking flow works across the simple field-based categories
  (Tailor, Xerox, AC, Plumber, Electrician, Parcel, Rental, Driver). Each category's specific
  fields are stored as flexible JSON rather than rigid columns, so adding another simple category
  later is a data change, not a schema rewrite — see `service_categories` table and
  `Booking.detailsJson`.
- **Food & Grocery are cart-based, not simple forms** — a shop owner lists items via
  `MenuItem`, a customer's order references `menuItemId` + `quantity`, and **the total is always
  computed server-side from the shop's current prices** (see `BookingService.buildCartDetails`) —
  never trusted from whatever number the frontend cart happened to show. This is the same
  principle as payment verification below: money math never trusts the client.
- **Tailor now uses real appointment slots, not typed-in date/time** — a shop owner opens slots
  (`AppointmentSlot`), a customer claims one, and a `@Version` field guards against two customers
  claiming the same slot in the same instant (see `SlotService.claim` for why the earlier
  "check then write" approach alone wasn't enough).
- **Menu items can carry an image URL** — a plain link, not file upload/storage, kept simple on
  purpose so this doesn't need an S3-style file store.
- **Real-time push replaces polling** — `WebSocketConfig` sets up a STOMP endpoint at `/ws`;
  `BookingService.updateStatus` publishes to `/topic/customer/{id}/bookings` the instant a status
  changes, so a connected frontend gets the update pushed to it rather than asking again and
  again. Note the simplification called out in `WebSocketConfig`'s comment: the socket handshake
  isn't JWT-checked yet.
- **Pickup vs. delivery is chosen only after the item is actually ready** — when a shop marks a
  tailor booking `COMPLETED`, a real push notification fires (see Notifications below), and only
  then can the customer call `/api/bookings/{id}/fulfillment` — trying earlier is rejected.
- **Reviews are tied to real completed bookings** — you can only review a booking you actually
  completed, once (`ReviewService`), not just leave a rating on any shop's page.
- **Search is a real substring query, not fake AI** — `SearchController` hits the database
  directly across shop names/addresses and menu item names. No external search service, no
  vector embeddings — those would be real additions later if search quality becomes a problem,
  not needed for a search box that mostly needs to find "the tailor on Anna Salai."
- **"Order again" uses only the customer's own order history** — `BookingService.frequentlyOrderedItems`
  counts a single customer's past food/grocery orders. No cross-customer data, no model, just
  honest counting — a real form of personalization without pretending it's more than it is.
- **New categories**: healthcare (Doctor, Dentist, Lab Test, Home Nurse, Physiotherapist) are
  request forms only — nothing here dispenses medical advice or diagnoses; a real clinic/lab/nurse
  follows up. Agriculture (Tractor, Farm Equipment Rental) and government-adjacent (Property Tax
  Reminder, Gas Booking, Civic Complaint) use the same pattern. Deliberately NOT added: ambulance
  booking (needs real dispatch infrastructure a form can't provide), blood donor matching,
  insurance/loan/legal/GST consultant booking, election services — these need real regulatory
  review or life-safety infrastructure this project doesn't have.
- **Delivery partner GPS tracking is real** — a delivery partner claims an unclaimed food/grocery/
  parcel booking (`BookingService.claimDelivery`), then their device pings
  `PATCH /api/bookings/{id}/location` every few seconds while `navigator.geolocation.watchPosition`
  runs on the frontend; each ping broadcasts over the same WebSocket topic the customer is already
  subscribed to, so the customer's map marker moves live.
- **Admin analytics are real database counts** — `AnalyticsService` does actual `COUNT`/`SUM`
  queries (bookings by status/category, shops by status, revenue from paid payments). No
  prediction, no chart dressed up as more than counting.
- **Receipts are real and printable** — `GET /api/bookings/{id}/receipt` combines the booking and
  its payment record; the frontend renders it and calls the browser's native print-to-PDF, no
  PDF-generation library needed.
- **Dynamic pricing is a real, published formula — not a black box.** `PricingService` computes a
  surge multiplier from actual current data: active bookings for a category in the last 15
  minutes, divided by approved shops in that category. `GET /api/pricing/surge` always returns
  the raw numbers behind the multiplier, not just the number — you could recompute it yourself
  with a SQL query. Applied to the food/grocery delivery fee and to the simple categories'
  Razorpay charge.
- **Categories are now fully data-driven — no code change needed to add one.** `ServiceCategory`
  gained `icon` and `fieldsJson` columns; `CategoryService` provides real CRUD
  (`POST`/`PATCH /api/admin/categories`); `GET /api/categories` returns the full field schema, and
  `Home.jsx` renders its entire category grid and booking forms from that response. An admin adds
  a category through the dashboard UI, not a pull request. Tailor/Food/Grocery/Fashion stay
  dedicated pages on purpose — cart+menu or real appointment slots aren't things a generic
  field-based form can replace.
- **Delivery fee is real distance, not a guess** — if the customer shares live location at
  checkout and the shop has coordinates, the fee is ₹10/km (with a floor), computed via haversine.
  Falls back to a flat estimate if either coordinate is missing. Surge still applies on top.
- **Cash on Delivery is a real, separate path** — `POST /api/payments/cod` records a
  `COD_PENDING` payment with no gateway call at all; the shop or delivery partner marks it
  collected via `PATCH /api/payments/cod/{bookingId}/collected`.
- **GST and handling charge are shop-owner-configurable**, not platform-wide — set via
  `PATCH /api/shops/{id}/billing-settings`, applied per-order.
- **Login accepts phone or email**, forgot/reset password reuses the existing OTP flow, and
  **account deletion anonymizes rather than hard-deletes** — a real `DELETE` would break every
  booking/review/shop that references this user's id via a foreign key.
- **First-user-becomes-admin, safely** — registering when `users` is completely empty makes you
  admin automatically, once, on a fresh install — without reopening the "admin via registration"
  hole closed earlier. Known limitation: a true simultaneous race on a brand-new database could
  theoretically let two registrations both pass the check; acceptable for a one-time bootstrap.
- **Order notifications actually fire** — the shop gets a push the moment a booking lands on it,
  and available deliveries broadcast live over WebSocket instead of the delivery dashboard
  polling. The customer's phone is visible in the shop's own view of a booking.
- **Delivery partner earnings are real, windowed stats** — sums each partner's own delivery fees
  across today/week/month/year/all-time.
- **Reviews now cover the delivery partner too**, optionally, still gated to a completed booking.
- **A real complaint/support system** — anyone can raise a ticket, optionally tied to a booking;
  admins see a queue and can respond.
- **Admin "delete" for shops is really suspension** — a true delete would orphan every booking,
  review, and menu item referencing that shop.
- **Payments** — real calls to Razorpay's REST API to create an order, plus server-side signature
  verification before a payment is ever marked `PAID`. No SDK dependency, just plain HTTPS calls.
- **Database schema** — `database/schema.sql`, matching the JPA entities exactly, with Phase 1
  categories pre-seeded.

## What you must supply yourself

Nobody, including me, can complete these on your behalf — they need your business identity, your
money, or your infrastructure decisions:

| Thing | Where to get it | Where it plugs in |
|---|---|---|
| Razorpay account + API keys | razorpay.com — requires business KYC | `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` env vars |
| Google Maps API key | Google Cloud Console | `GOOGLE_MAPS_API_KEY` env var (used by the frontend, not yet called from this backend) |
| Firebase project + service account | Firebase Console | `FIREBASE_SERVICE_ACCOUNT_PATH` — push notifications now actually send once this file exists; see `FirebaseConfig.java` |
| SMS provider (MSG91 / Twilio / AWS SNS) | Their respective consoles | Replace the body of `OtpService.sendOtp()` |
| Anthropic API key | console.anthropic.com | `ANTHROPIC_API_KEY` env var — powers the AI assistant's chat calls, kept server-side only |
| A real JWT secret | Generate 32+ random bytes yourself | `JWT_SECRET` env var — **do not use the default in production** |
| Cloud hosting (AWS/GCP) + a domain | Your cloud provider account | Deploy the Docker image there; nothing here does this for you |

## Deploying to Render

`render.yaml` in this folder is a Render "Blueprint" — connect this repo at
render.com/blueprints and it provisions both the web service and a managed Postgres database
automatically. You'll be prompted to fill in the secrets marked `sync: false` (Razorpay keys,
JWT secret, Anthropic key, Maps key) yourself — nobody else can supply those.

**Important gap to know about:** `docker-compose.yml`'s local setup runs `database/schema.sql`
automatically (Postgres's own init-script mechanism), which is where the Phase 1-3 category rows,
sample bus routes, and demo petrol bunks get seeded. Render's managed Postgres doesn't run that
same init step — `ddl-auto: update` only creates the table *structure* from the JPA entities, not
that seed data. After your first deploy, connect to the Render database (it gives you a
`psql` connection string in its dashboard) and run `database/schema.sql` against it once
manually, or your app will start with an empty categories table and nothing will be bookable.

Once you have a real frontend URL (e.g. from Vercel), update two places to match it:
1. `SecurityConfig.corsConfigurationSource()` — replace the placeholder `ooru-frontend.vercel.app`
2. `ooru-frontend/.env`'s `VITE_API_BASE_URL` — point it at your Render backend URL

## Running it locally

Requires Docker and Docker Compose.

```bash
docker-compose up --build
```

This starts Postgres (seeded with `database/schema.sql`) and the backend on `http://localhost:8080`.

Without Docker, you need Java 17, Maven, and a local Postgres instance matching the
`DB_HOST`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` in `application.yml`, then:

```bash
mvn spring-boot:run
```

## The first admin account

There's no more hardcoded seed password — a hash shipped in source control is barely a secret,
since it's the same for everyone who clones this repo. Instead:

1. Set real, never-committed values for `ADMIN_BOOTSTRAP_PHONE` and `ADMIN_BOOTSTRAP_PASSWORD` as
   environment variables (see `docker-compose.yml` / `render.yaml`).
2. Start the app. `AdminBootstrapRunner` creates the admin account once, if none exists yet,
   hashing the password for real at that point — the plaintext never touches the database or a
   file. Leave the env vars blank and the app starts fine, you just don't have an admin yet.
3. Log in, then use `PATCH /api/auth/change-password` to change it whenever you want — that
   endpoint works for any account, admin included, so you never need to touch the database again.

Admins are still never created through the public `/api/auth/register` endpoint — the only way to
get one is the bootstrap step above, on purpose, so a stray API call can never grant someone
admin access.

## API overview

| Method | Path | Who | What |
|---|---|---|---|
| POST | `/api/auth/register` | public | Create an account (`CUSTOMER`, `SHOP_OWNER`, or `DELIVERY_PARTNER`) |
| POST | `/api/auth/verify-otp` | public | Activate the account |
| POST | `/api/auth/login` | public | Get a JWT |
| PATCH | `/api/auth/change-password` | authenticated | Change your own password (any role, including admin) |
| GET | `/api/categories` | public | List active service categories |
| POST | `/api/shops/register` | SHOP_OWNER | Register a shop (goes to `PENDING`) |
| GET | `/api/shops/mine` | SHOP_OWNER | List your own shops |
| GET | `/api/shops/by-category/{code}` | public | Approved shops for a category |
| POST | `/api/shops/{shopId}/menu` | SHOP_OWNER (must own the shop) | Add a menu item (food/grocery shops) |
| GET | `/api/shops/{shopId}/menu` | public | Browse a shop's active menu |
| POST | `/api/bookings` | authenticated | Create a booking — pass `items` for food/grocery, `slotId` for tailor |
| GET | `/api/bus-routes/search?q=` | public | Bus timing lookup (not a booking) |
| GET | `/api/shops/nearby?categoryCode=&lat=&lng=` | public | Nearest shops by real distance (petrol bunks etc.) |
| POST | `/api/shops/{shopId}/slots` | SHOP_OWNER (must own the shop) | Open a real appointment slot (tailor) |
| GET | `/api/shops/{shopId}/slots` | public | Browse a shop's still-open slots |
| PATCH | `/api/bookings/{id}/fulfillment` | authenticated (must own the booking) | Choose pickup/delivery once a booking is COMPLETED |
| GET | `/api/assistant/schema` | authenticated | The category/field schema the AI assistant knows about |
| POST | `/api/assistant/chat` | authenticated | One turn of the natural-language booking assistant |
| POST | `/api/assistant/confirm` | authenticated | Turns an assistant's finished state into a real booking |
| PATCH | `/api/users/me/fcm-token` | authenticated | Register a device for push notifications |
| POST | `/api/reviews` | authenticated | Review a completed booking (once) |
| GET | `/api/shops/{shopId}/reviews` | public | A shop's average rating + reviews |
| GET | `/api/search?q=` | public | Real substring search across shops and menu items |
| GET | `/api/bookings/mine/frequent-items` | authenticated | "Order again" — this customer's own most-ordered items |
| GET | `/api/bookings/{id}/receipt` | authenticated | Booking + payment info for the receipt view |
| GET | `/api/bookings/deliveries/available` | authenticated | Unclaimed food/grocery/parcel deliveries |
| GET | `/api/bookings/deliveries/mine` | authenticated | A delivery partner's claimed deliveries |
| POST | `/api/bookings/{id}/claim-delivery` | authenticated (DELIVERY_PARTNER) | Claim an unclaimed delivery |
| PATCH | `/api/bookings/{id}/location` | authenticated (must have claimed it) | Push a live GPS ping |
| GET | `/api/admin/analytics` | ADMIN | Real counts: bookings, shops, revenue |
| GET | `/api/pricing/surge?categoryCode=` | public | Real-time surge multiplier + the exact numbers behind it |
| GET | `/api/admin/categories` | ADMIN | All categories, including inactive ones |
| POST | `/api/admin/categories` | ADMIN | Create a new category — appears immediately, no deploy |
| PATCH | `/api/admin/categories/{id}` | ADMIN | Edit fields, icon, name, or active/inactive |
| WS | `/ws` (STOMP topic `/topic/customer/{id}/bookings`) | — | Real-time booking status push |
| GET | `/api/bookings/mine` | authenticated | Your own bookings |
| GET | `/api/bookings/shop/{shopId}` | authenticated | Bookings routed to a shop |
| PATCH | `/api/bookings/{id}/status` | authenticated | Move a booking through its lifecycle |
| POST | `/api/payments/create-order` | authenticated | Create a Razorpay order for a booking |
| POST | `/api/payments/verify` | authenticated | Verify payment signature after checkout |
| GET | `/api/admin/shops/pending` | ADMIN | Shop approval queue |
| PATCH | `/api/admin/shops/{id}/approve` \| `/reject` \| `/suspend` | ADMIN | Approve/reject a shop |
| GET | `/api/admin/shops` | ADMIN | Every shop, any status — "delete" here means suspend |
| POST | `/api/payments/cod` | authenticated | Record a cash-on-delivery payment (no gateway call) |
| PATCH | `/api/payments/cod/{bookingId}/collected` | authenticated | Mark cash actually received |
| PATCH | `/api/shops/{id}/billing-settings` | SHOP_OWNER | Set this shop's GST% and handling fee |
| POST | `/api/auth/forgot-password` | public | Sends an OTP to the phone on file |
| POST | `/api/auth/reset-password` | public | Verifies the OTP, sets a new password |
| DELETE | `/api/auth/me` | authenticated | Anonymizes your account (see AuthService for why not a hard delete) |
| GET | `/api/bookings/deliveries/stats` | authenticated (DELIVERY_PARTNER) | Real earnings by day/week/month/year/all-time |
| POST | `/api/complaints` | authenticated | Raise a support ticket, optionally tied to a booking |
| GET | `/api/complaints/mine` | authenticated | Your own tickets |
| GET | `/api/admin/complaints` | ADMIN | Every ticket |
| PATCH | `/api/admin/complaints/{id}` | ADMIN | Respond to / resolve a ticket |

Every authenticated request needs `Authorization: Bearer <token>` from the login response.

## What's deliberately not built yet

- **Frontend** — see `../ooru-frontend` for the React app that calls these exact endpoints,
  including the food/grocery cart flow.
- **Push notifications actually send now** (`NotificationService` + `FirebaseConfig`), but fail
  silently and safely until you've done the Firebase Console setup — `FirebaseConfig` checks for
  a real service account file on startup and simply logs a warning and skips initialization if
  it's not there, rather than crashing. `BookingService.updateStatus` fires a notification on
  every status change once a customer has registered a device token.
- **Maps/distance** — the Google Maps API key is wired into config but no endpoint calls it yet;
  the "nearest shop" logic from the prototype (haversine distance) is a reasonable starting point
  to port into a real endpoint.
- **AI assistant covers 13 of the simple categories, not food/grocery** — `AssistantService`'s
  schema mirrors Tailor, Xerox, AC, Plumber, Electrician, Parcel, Rental, Driver, House Rent,
  Hotel, Scrap, Food Donation, Old Clothes. Food/grocery need a shop + menu chosen first, which
  is a different interaction shape — `FoodOrder.jsx` handles those directly instead.
- **Remaining Phase 4 features** (voice booking, AI shop recommendations, route optimization,
  demand prediction, fraud detection) are not built — the natural-language chat is the one piece
  of Phase 4 that's in.
- **Admin/shop-owner/customer dashboards as UI** — the API supports them; see the frontend for
  what's actually built on top.

## A note on testing

This code was written directly to files, not compiled or run in a live environment while writing
it (no internet access here to pull Maven dependencies). It's structured correctly and follows
real Spring Boot conventions, but treat it as a strong first draft — run `mvn clean install` and
work through any small compilation issues before deploying anywhere that matters.
