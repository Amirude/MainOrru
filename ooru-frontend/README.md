# Altenul One Frontend — Phase 1 MVP

A real React app (Vite + React Router) that calls the actual endpoints in `../ooru-backend` —
not the browser-only prototype from earlier in this project. This is the piece the backend
README flagged as missing.

## Running it

2. Get `../ooru-backend` running first (see its own README) — this app has nothing to talk to
   without it.
3. `cp .env.example .env` and fill in `VITE_API_BASE_URL` (defaults to `http://localhost:8080/api`,
   which matches the backend's default Docker Compose setup) and `VITE_RAZORPAY_KEY_ID` (your
   Razorpay *public* key id — safe to expose in frontend code, unlike the secret key).
   Push notifications need the `VITE_FIREBASE_*` variables too — see "Notifications" below.
4. `npm install`
5. `npm run dev` — opens on `http://localhost:5173`

## What's in here

- **Auth flow** — register → verify OTP (the backend logs the OTP to its console for now, see
  its `OtpService.java`) → log in → JWT stored and attached to every request automatically.
- **Booking** — the same 5 Phase 1 categories as the backend, calling the real
  `POST /api/bookings` endpoint.
- **Real Razorpay Checkout** — after a booking is created, "Pay now" opens Razorpay's actual
  checkout widget (`checkout.js`, loaded from Razorpay's CDN), and the payment is verified
  server-side afterward — not just trusted because the browser said so.
- **My Bookings** — a customer's own booking history and live status, including the
  pickup-or-delivery choice for finished tailor orders.
- **Tailor booking** — pick a shop, pick a real open time slot (no double-booking), fill in
  garment details. Once the shop marks it ready, a notification fires and the choice appears here.
- **Menu item photos** — shop owners can attach an image URL to each item; customers see it
  while browsing.
- **Live updates, no polling** — My Bookings opens a WebSocket connection and updates the moment
  the backend pushes a status change.
- **Reviews** — a star rating + comment form appears on completed bookings you haven't reviewed
  yet; shop pages show the average rating.
- **Search** — a debounced search box in the nav bar hits the real backend search endpoint
  across shop names and menu items.
- **"Order again"** — Food & Grocery shows your own most-ordered items at the top, built from
  your real order history, not a recommendation model.
- **Delivery partner dashboard** — claim an unclaimed delivery, then share live GPS while en
  route; the customer sees your marker move on a real map in My Bookings.
- **Admin analytics** — real counts (bookings by status/category, shops, revenue) on the admin
  dashboard.
- **Receipts** — a real printable receipt per completed booking, using the browser's native
  print-to-PDF rather than a PDF-generation library.
- **New categories** — healthcare, agriculture, government-adjacent, and now **Fashion &
  Accessories** (a third cart-based tab alongside Food/Grocery — clothing, men's/women's
  accessories, stationery, gift items, browsed the same way).
- **Categories are database-driven now** — "Book a service" fetches its entire grid and each
  form's fields from `GET /api/categories`.
- **Dynamic pricing, real distance-based delivery fee** — ₹10/km from your actual checkout
  location to the shop, not a flat guess, with real-time surge on top.
- **Cash on Delivery** — a real payment method choice alongside online, no gateway involved.
- **Login by phone or email**, forgot/reset password, and account deletion (anonymizes rather
  than a destructive hard delete — see backend README for why).
- **Delivery partner dashboard now shows real earnings** — today/week/month/year, summed from
  actual completed deliveries.
- **Delivery partner ratings** alongside shop ratings, on the same review form.
- **Help & support ticket system**, plus an admin queue to respond.
- **Admin can manage everything** — categories (create/edit/retire), shops (suspend/reinstate —
  the safe version of "delete"), analytics, support tickets, all from one dashboard.
- **Homepage now has an offers banner, an About section, and a site-wide footer** with
  placeholder company/contact details — replace them with your real ones.
- **UI polish** — real fade-in animations, skeleton loading states, a working mobile hamburger menu.

## What I deliberately deferred to a follow-up pass

- **A realistic visual print preview for Xerox** (page stack, binding style, add/remove pages) —
  not built yet; this needs its own focused pass rather than being squeezed into this one.
- **Distinctly Zomato/Zepto/Flipkart-styled sections** for Food/Grocery/Fashion — right now all
  three share `FoodOrder.jsx` with just a tab switch; a real visual redesign per section (colors,
  layout rhythm, imagery) is a separate, sizeable design pass.
- The offers banner on the homepage is static marketing content, not a real coupon/discount engine
  — no discount code system was built.
- **Shop dashboard** — register a shop, see its approval status, and once approved, accept/reject
  and progress bookings routed to it.
- **Admin dashboard** — the shop approval queue.
- **Push notifications** — the "🔔 Notify me" button in the nav requests browser permission,
  registers a Firebase Cloud Messaging token, and saves it to the backend. Once a Firebase project
  is set up (see below), booking status changes actually arrive as real push notifications.

## Setting up notifications

1. Create a Firebase project (console.firebase.google.com), add a web app to it, and copy its
   config values into `VITE_FIREBASE_*` in `.env`.
2. Generate a Web Push certificate (Project Settings → Cloud Messaging) and put its key in
   `VITE_FIREBASE_VAPID_KEY`.
3. Copy the exact same config values into `public/firebase-messaging-sw.js` — service workers
   can't read `.env` files, so that file needs the plain values, not `import.meta.env` references.
4. On the backend, generate a service account key (Project Settings → Service Accounts) and point
   `FIREBASE_SERVICE_ACCOUNT_PATH` at it — see `../ooru-backend/README.md`.

Until all of that is done, the "🔔 Notify me" button just says notifications aren't configured
yet — it won't error out or break anything else.

## Fixing "404 on refresh"

This is a single-page-app issue, not a bug — refreshing a route like `/food` sends a fresh
request straight to whatever's hosting the site, and there's no real file at that path; only
`index.html` plus React Router's client-side routing knows what `/food` means. Every static host
needs telling to serve `index.html` for any unmatched path instead of 404ing:

- **Vercel** — `vercel.json` in this folder already does this (`rewrites` → `index.html`).
- **Netlify, or Render's static site hosting** — `public/_redirects` already does this
  (`/* /index.html 200`); Vite copies it into the build output automatically.
- **Self-hosting behind Nginx** — add this to your server block:
  ```nginx
  location / {
    try_files $uri $uri/ /index.html;
  }
  ```
- **Local dev (`npm run dev` / `npm run preview`)** — this shouldn't happen; Vite's own server
  already does SPA fallback by default. If it's happening locally, check you're not serving the
  `dist/` folder with a plain static file server that doesn't know about this.

## What's still missing

- **Maps** — Nearby Shops now renders a real Google Map (`MapView.jsx`) with a marker for you and
  each shop, once `VITE_GOOGLE_MAPS_API_KEY` is set. Falls back to the plain distance list if it
  isn't configured — never breaks the page either way.
- **Design polish** — this reuses the brand's color tokens and fonts, but the layouts are
  functional, not final. Treat it as a working skeleton, not finished UI.
- **Testing** — written directly to files without a live npm/browser environment to run it in
  (no internet access in the environment that built this). Run `npm install && npm run dev` and
  work through anything small before relying on it.
