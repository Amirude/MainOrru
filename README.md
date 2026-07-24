# Altenul One — Full project

Two real, working projects that talk to each other:

- **ooru-backend/** — Spring Boot + PostgreSQL API
- **ooru-frontend/** — React app that consumes that API

## This update was a big one — here's the organized rundown

**Pricing & payments**: real ₹10/km distance-based delivery fee (with surge on top), Cash on
Delivery as a genuine separate payment path, shop-configurable GST% and handling charge.

**Accounts**: login by phone or email, forgot/reset password (reuses the existing OTP flow),
account deletion (anonymizes — a hard delete would break every booking/review/shop that
references your id), and a safe "first user on a fresh install becomes admin automatically"
bootstrap that doesn't reopen the security hole closed earlier.

**New categories**: Fashion & Accessories (cart-based, third tab next to Food/Grocery — covers
clothing, men's/women's accessories, stationery, gift items).

**Operations**: shop owners get notified the instant an order lands on them; available deliveries
broadcast live over WebSocket instead of polling; the customer's phone is visible to the shop
fulfilling their order; delivery partners see real earnings by day/week/month/year; delivery
partners can be rated too, alongside the shop.

**Support & admin**: a real complaint/ticket system (raise one, admin responds); admin can
suspend/reinstate shops (the safe version of "delete") and fully manage categories, all from one
dashboard.

**Homepage**: an offers banner, an About section, and a site-wide footer with company/contact
placeholders — edit those to your real details.

## What I deliberately deferred (see ooru-frontend/README.md for detail)

- A realistic visual print preview for Xerox (page stack, binding, add/remove pages)
- Genuinely distinct Zomato/Zepto/Flipkart-style visual redesigns for Food/Grocery/Fashion
  (right now they share one page with a tab switch — the data model supports a real redesign,
  the visual work itself is a separate pass)
- A real discount/coupon engine (the homepage banner is static marketing content)

## Quick start

```bash
cd ooru-backend
docker-compose up --build
# in another terminal
cd ../ooru-frontend
cp .env.example .env
npm install
npm run dev
```

## What you must still supply yourself

- `ADMIN_BOOTSTRAP_PHONE` / `ADMIN_BOOTSTRAP_PASSWORD` (or just register first on a fresh install)
- A Razorpay account with completed business KYC, and its API keys
- An Anthropic API key — powers the AI assistant, stays server-side only
- A Firebase project (web config + VAPID key + service account) — powers push notifications
- A Google Maps API key — powers the visual maps
- A real SMS provider for OTPs (currently logs to the backend console)
- A real JWT secret for any environment beyond your own laptop
- Your actual deployed frontend URL, added to `SecurityConfig`'s CORS list
- Your real company name and contact details in `Footer.jsx`

## What's genuinely still missing

Voice booking, AI shop recommendations, route optimization, demand prediction, fraud detection —
need real usage data to be more than a fake demo. Ambulance, blood donor matching, insurance/
loan/legal/GST consulting, election services — need real regulatory review or dispatch
infrastructure this project doesn't have.
