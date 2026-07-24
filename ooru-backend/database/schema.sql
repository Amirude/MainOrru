-- Altenul One Phase 1 database schema (PostgreSQL)
-- This mirrors what Hibernate would generate from the JPA entities. Use this file for a real
-- migration tool (Flyway/Liquibase) once you move off ddl-auto: update in application.yml.

CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    phone           VARCHAR(20)  NOT NULL UNIQUE,
    email           VARCHAR(255) UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(30)  NOT NULL CHECK (role IN ('ADMIN','SHOP_OWNER','CUSTOMER','DELIVERY_PARTNER')),
    phone_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    fcm_token       VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS service_categories (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(50)  NOT NULL UNIQUE,
    display_name  VARCHAR(100) NOT NULL,
    icon          VARCHAR(10),
    fields_json   TEXT,
    active        BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS shops (
    id             BIGSERIAL PRIMARY KEY,
    owner_user_id  BIGINT       NOT NULL REFERENCES users(id),
    shop_name      VARCHAR(255) NOT NULL,
    category_code  VARCHAR(50)  NOT NULL,
    address        VARCHAR(500) NOT NULL,
    latitude       DOUBLE PRECISION,
    longitude      DOUBLE PRECISION,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED','SUSPENDED')),
    gst_percent    DOUBLE PRECISION,
    handling_fee_paise BIGINT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS bookings (
    id                BIGSERIAL PRIMARY KEY,
    reference         VARCHAR(20)  NOT NULL UNIQUE,
    customer_user_id  BIGINT       NOT NULL REFERENCES users(id),
    shop_id           BIGINT       REFERENCES shops(id),
    slot_id           BIGINT       REFERENCES appointment_slots(id),
    delivery_partner_id BIGINT     REFERENCES users(id),
    delivery_lat      DOUBLE PRECISION,
    delivery_lng      DOUBLE PRECISION,
    category_code     VARCHAR(50)  NOT NULL,
    details_json      TEXT         NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'REQUESTED'
                       CHECK (status IN ('REQUESTED','ACCEPTED','REJECTED','IN_PROGRESS','COMPLETED','CANCELLED')),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS payments (
    id                   BIGSERIAL PRIMARY KEY,
    booking_id           BIGINT      NOT NULL UNIQUE REFERENCES bookings(id),
    amount_paise         BIGINT      NOT NULL,
    currency             VARCHAR(3)  NOT NULL DEFAULT 'INR',
    razorpay_order_id    VARCHAR(100),
    razorpay_payment_id  VARCHAR(100),
    status               VARCHAR(20) NOT NULL DEFAULT 'CREATED' CHECK (status IN ('CREATED','PAID','FAILED','REFUNDED','COD_PENDING')),
    method               VARCHAR(20) NOT NULL DEFAULT 'ONLINE' CHECK (method IN ('ONLINE','COD')),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS menu_items (
    id           BIGSERIAL PRIMARY KEY,
    shop_id      BIGINT       NOT NULL REFERENCES shops(id),
    name         VARCHAR(255) NOT NULL,
    price_paise  BIGINT       NOT NULL,
    image_url    VARCHAR(1000),
    active       BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_menu_items_shop ON menu_items(shop_id);

CREATE TABLE IF NOT EXISTS appointment_slots (
    id          BIGSERIAL PRIMARY KEY,
    shop_id     BIGINT      NOT NULL REFERENCES shops(id),
    date        DATE        NOT NULL,
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    booked      BOOLEAN     NOT NULL DEFAULT FALSE,
    version     BIGINT      NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_slots_shop ON appointment_slots(shop_id);

CREATE TABLE IF NOT EXISTS reviews (
    id          BIGSERIAL PRIMARY KEY,
    booking_id  BIGINT      NOT NULL UNIQUE REFERENCES bookings(id),
    shop_id     BIGINT      NOT NULL REFERENCES shops(id),
    customer_id BIGINT      NOT NULL REFERENCES users(id),
    rating      SMALLINT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    delivery_partner_id BIGINT REFERENCES users(id),
    delivery_partner_rating SMALLINT CHECK (delivery_partner_rating BETWEEN 1 AND 5),
    comment     VARCHAR(1000),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_reviews_shop ON reviews(shop_id);

CREATE TABLE IF NOT EXISTS complaints (
    id               BIGSERIAL PRIMARY KEY,
    raised_by_user_id BIGINT     NOT NULL REFERENCES users(id),
    booking_id       BIGINT      REFERENCES bookings(id),
    subject          VARCHAR(255) NOT NULL,
    description      VARCHAR(2000) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED')),
    admin_response   VARCHAR(2000),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at      TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_complaints_user ON complaints(raised_by_user_id);

-- A plain lookup table, not tied to bookings — see BusRoute.java for why.
CREATE TABLE IF NOT EXISTS bus_routes (
    id            BIGSERIAL PRIMARY KEY,
    route_number  VARCHAR(20)   NOT NULL,
    from_stop     VARCHAR(255)  NOT NULL,
    to_stop       VARCHAR(255)  NOT NULL,
    departures    VARCHAR(1000) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bookings_customer ON bookings(customer_user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_shop ON bookings(shop_id);
CREATE INDEX IF NOT EXISTS idx_shops_category_status ON shops(category_code, status);

-- Phase 1 categories.
INSERT INTO service_categories (code, display_name) VALUES
    ('tailor', 'Tailor'),
    ('xerox', 'Xerox / Printout'),
    ('ac', 'AC Service'),
    ('plumber', 'Plumber'),
    ('electrician', 'Electrician')
ON CONFLICT (code) DO NOTHING;

-- Phase 2 categories. "food" and "grocery" are cart-based (see menu_items + BookingService's
-- CART_BASED_CATEGORIES) — every other category here uses the same simple field-based booking
-- as Phase 1.
INSERT INTO service_categories (code, display_name) VALUES
    ('food', 'Food Delivery'),
    ('grocery', 'Grocery Delivery'),
    ('parcel', 'Parcel Pickup & Delivery'),
    ('rental', 'Bike/Car Rental'),
    ('driver', 'Driver Booking')
ON CONFLICT (code) DO NOTHING;

-- Fashion is cart-based like food/grocery (see BookingService.CART_BASED_CATEGORIES) — one
-- retail vertical covering clothing, men's/women's accessories, stationery, and gift items,
-- browsed Flipkart-style. A shop under this category lists products (via the same MenuItem
-- model food/grocery shops use) rather than 5 separate booking categories for each product type.
INSERT INTO service_categories (code, display_name) VALUES
    ('fashion', 'Fashion & Accessories')
ON CONFLICT (code) DO NOTHING;

-- Phase 3 categories — House Rent, Hotel, Scrap Collection, and the two donation categories are
-- simple field-based bookings, same pattern as Phase 1. Bus Timings and Petrol Bunks are NOT
-- bookings at all (see bus_routes above, and the "petrol" shop category below) — nobody "books"
-- a bus timing or a petrol pump, they just look one up.
INSERT INTO service_categories (code, display_name) VALUES
    ('houserent', 'House Rent / Lease'),
    ('hotel', 'Hotel Booking'),
    ('scrap', 'Scrap Collection'),
    ('fooddonation', 'Food Donation'),
    ('oldclothes', 'Old Clothes Donation')
ON CONFLICT (code) DO NOTHING;

-- Healthcare categories are REQUEST FORMS ONLY — a customer describes what they need and a real
-- clinic/lab/nurse follows up. Nothing here dispenses medical advice, diagnoses, or prescribes.
-- Ambulance booking and anything else requiring real-time dispatch was deliberately left out —
-- a booking form is not real dispatch infrastructure and shouldn't pretend to be for anything
-- life-safety-critical.
INSERT INTO service_categories (code, display_name) VALUES
    ('doctor', 'Doctor Appointment'),
    ('dentist', 'Dentist Booking'),
    ('labtest', 'Diagnostic Lab Test'),
    ('homenurse', 'Home Nurse'),
    ('physio', 'Physiotherapist')
ON CONFLICT (code) DO NOTHING;

-- Agriculture — same request-form pattern.
INSERT INTO service_categories (code, display_name) VALUES
    ('tractor', 'Tractor Rental'),
    ('farmequipment', 'Farm Equipment Rental')
ON CONFLICT (code) DO NOTHING;

-- Government-adjacent — kept to plain reminders/requests a person forwards to the real authority
-- themselves. Nothing here files anything on a government system or claims legal effect.
INSERT INTO service_categories (code, display_name) VALUES
    ('propertytax', 'Property Tax Reminder'),
    ('gasbooking', 'Gas Cylinder Booking'),
    ('civiccomplaint', 'Civic Complaint Registration')
ON CONFLICT (code) DO NOTHING;

-- Sample bus routes for the lookup demo. Replace with a live transit data feed for real use.
INSERT INTO bus_routes (route_number, from_stop, to_stop, departures) VALUES
    ('21G', 'Broadway', 'Tambaram', '5:40am, 6:10am, 6:40am, then every 20 min till 10pm'),
    ('M23', 'Anna Nagar', 'Tambaram', '5:50am, 6:20am, 6:55am, then every 25 min till 9:30pm'),
    ('570', 'CMBT', 'Mahabalipuram', '6:00am, 8:00am, 10:00am, 2:00pm, 5:00pm'),
    ('18A', 'Broadway', 'Anna Nagar', 'every 15 min, 5:45am till 11pm'),
    ('55', 'Tambaram', 'Velachery', '6:05am, 6:40am, 7:10am, then every 20 min till 10pm');

-- A demo shop owner + a few approved petrol bunks, purely so the "nearby" lookup has something to
-- return out of the box. This account is intentionally low-stakes (SHOP_OWNER, not ADMIN) so it's
-- fine as seed data, but it uses the same placeholder hash as everyone who clones this repo — the
-- password is "change-me-immediately". Don't reuse this pattern for anything that matters; this
-- is here only so the nearby-shops demo has data without you registering a shop by hand first.
INSERT INTO users (name, phone, email, password_hash, role, phone_verified)
VALUES ('Demo Fuel Retailer', '9888888888', 'demo-fuel@example.com',
        '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5c4/hnZQ8/n8QG/kQpvT4kZa6SwjO',
        'SHOP_OWNER', TRUE)
ON CONFLICT (phone) DO NOTHING;

INSERT INTO shops (owner_user_id, shop_name, category_code, address, latitude, longitude, status)
SELECT u.id, v.shop_name, 'petrol', v.address, v.lat, v.lng, 'APPROVED'
FROM users u,
     (VALUES
        ('IOC Petrol Bunk, Anna Salai', 'Anna Salai, Chennai', 13.0604, 80.2496),
        ('HP Petrol Pump, T Nagar', 'T Nagar, Chennai', 13.0418, 80.2341),
        ('Bharat Petroleum, Velachery', 'Velachery, Chennai', 12.9789, 80.2201)
     ) AS v(shop_name, address, lat, lng)
WHERE u.phone = '9888888888';

-- The admin account is no longer seeded here with a hardcoded password hash — a hash shipped in
-- source control is barely a secret at all, since it's the same for everyone who clones this
-- repo. See AdminBootstrapRunner.java: set real ADMIN_BOOTSTRAP_PHONE / ADMIN_BOOTSTRAP_PASSWORD
-- environment variables (never committed anywhere) and the app creates the admin account itself
-- on startup, hashing the password for real at that point.
-- Backfill icon + fields_json for every simple (non-dedicated-page) category, so the
-- generic booking form can be rendered ENTIRELY from database data — no code change
-- needed to add, edit, or retire a category from here on. tailor/food/grocery stay NULL
-- here on purpose — they have dedicated pages (real appointment slots / cart+menu), not
-- the generic field-based form.
UPDATE service_categories SET icon = '📠', fields_json = '[{"id": "copies", "label": "Copies / pages", "type": "text"}, {"id": "color", "label": "Print type", "type": "select", "options": ["Black & white", "Color"]}, {"id": "binding", "label": "Binding", "type": "select", "options": ["None", "Stapled", "Spiral bound"]}, {"id": "mode", "label": "Pickup or delivery", "type": "select", "options": ["I will pick up", "Deliver to my address"]}, {"id": "address", "label": "Address (if delivery)", "type": "text"}]' WHERE code = 'xerox';
UPDATE service_categories SET icon = '❄️', fields_json = '[{"id": "work", "label": "Service type", "type": "select", "options": ["Regular service", "Gas refill", "Repair", "New installation"]}, {"id": "address", "label": "Address", "type": "text"}, {"id": "date", "label": "Date", "type": "date"}, {"id": "time", "label": "Time slot", "type": "time"}]' WHERE code = 'ac';
UPDATE service_categories SET icon = '🔧', fields_json = '[{"id": "issue", "label": "Issue", "type": "select", "options": ["Leak", "Blocked drain", "Tap / fitting", "New pipeline", "Other"]}, {"id": "urgency", "label": "Urgency", "type": "select", "options": ["Urgent — today", "Within 2-3 days", "Whenever convenient"]}, {"id": "address", "label": "Address", "type": "text"}, {"id": "date", "label": "Preferred date", "type": "date"}, {"id": "time", "label": "Preferred time", "type": "time"}]' WHERE code = 'plumber';
UPDATE service_categories SET icon = '💡', fields_json = '[{"id": "issue", "label": "Issue", "type": "select", "options": ["Wiring problem", "Switch/socket repair", "New fitting installation", "Fan/light issue", "Other"]}, {"id": "urgency", "label": "Urgency", "type": "select", "options": ["Urgent — today", "Within 2-3 days", "Whenever convenient"]}, {"id": "address", "label": "Address", "type": "text"}, {"id": "date", "label": "Preferred date", "type": "date"}, {"id": "time", "label": "Preferred time", "type": "time"}]' WHERE code = 'electrician';
UPDATE service_categories SET icon = '📦', fields_json = '[{"id": "pickupAddress", "label": "Pickup address", "type": "text"}, {"id": "dropAddress", "label": "Drop address", "type": "text"}, {"id": "itemDescription", "label": "What are you sending", "type": "text"}, {"id": "date", "label": "Date", "type": "date"}, {"id": "time", "label": "Preferred pickup time", "type": "time"}]' WHERE code = 'parcel';
UPDATE service_categories SET icon = '🚐', fields_json = '[{"id": "vehicleType", "label": "Vehicle type", "type": "select", "options": ["Bike", "Car (hatchback)", "Car (sedan/SUV)", "Van"]}, {"id": "pickupLocation", "label": "Pickup location", "type": "text"}, {"id": "pickupDate", "label": "Pickup date", "type": "date"}, {"id": "returnDate", "label": "Return date", "type": "date"}]' WHERE code = 'rental';
UPDATE service_categories SET icon = '🚗', fields_json = '[{"id": "tripType", "label": "Trip type", "type": "select", "options": ["One-way", "Round trip", "Monthly / long-term"]}, {"id": "pickupLocation", "label": "Pickup location", "type": "text"}, {"id": "dropLocation", "label": "Drop location", "type": "text"}, {"id": "date", "label": "Date", "type": "date"}, {"id": "time", "label": "Time", "type": "time"}]' WHERE code = 'driver';
UPDATE service_categories SET icon = '🏠', fields_json = '[{"id": "propertyType", "label": "Looking for", "type": "select", "options": ["1 BHK", "2 BHK", "3 BHK", "Independent house", "Commercial space"]}, {"id": "area", "label": "Preferred area", "type": "text"}, {"id": "budget", "label": "Monthly budget (₹)", "type": "text"}, {"id": "contact", "label": "Contact number", "type": "text"}]' WHERE code = 'houserent';
UPDATE service_categories SET icon = '🛏️', fields_json = '[{"id": "city", "label": "City", "type": "text"}, {"id": "checkin", "label": "Check-in", "type": "date"}, {"id": "checkout", "label": "Check-out", "type": "date"}, {"id": "guests", "label": "Guests", "type": "text"}]' WHERE code = 'hotel';
UPDATE service_categories SET icon = '♻️', fields_json = '[{"id": "items", "label": "What are you selling", "type": "text"}, {"id": "estimatedWeight", "label": "Approx. weight (kg)", "type": "text"}, {"id": "address", "label": "Pickup address", "type": "text"}, {"id": "date", "label": "Preferred pickup date", "type": "date"}]' WHERE code = 'scrap';
UPDATE service_categories SET icon = '🍛', fields_json = '[{"id": "recipient", "label": "Who is this for", "type": "select", "options": ["An elderly person", "A person with a disability", "A widow in need", "A family I know"]}, {"id": "quantity", "label": "Meals / quantity", "type": "text"}, {"id": "address", "label": "Address", "type": "text"}, {"id": "date", "label": "Starting date", "type": "date"}]' WHERE code = 'fooddonation';
UPDATE service_categories SET icon = '👕', fields_json = '[{"id": "quantity", "label": "Approx. quantity", "type": "text"}, {"id": "address", "label": "Pickup address", "type": "text"}, {"id": "date", "label": "Preferred pickup date", "type": "date"}]' WHERE code = 'oldclothes';
UPDATE service_categories SET icon = '🩺', fields_json = '[{"id": "specialty", "label": "Specialty (if known)", "type": "text"}, {"id": "reason", "label": "Reason for visit", "type": "text"}, {"id": "preferredDate", "label": "Preferred date", "type": "date"}]' WHERE code = 'doctor';
UPDATE service_categories SET icon = '🦷', fields_json = '[{"id": "reason", "label": "Reason for visit", "type": "text"}, {"id": "preferredDate", "label": "Preferred date", "type": "date"}]' WHERE code = 'dentist';
UPDATE service_categories SET icon = '🧪', fields_json = '[{"id": "testType", "label": "Test(s) needed", "type": "text"}, {"id": "homeCollection", "label": "Sample collection", "type": "select", "options": ["Home collection", "Visit the lab"]}, {"id": "address", "label": "Address", "type": "text"}, {"id": "preferredDate", "label": "Preferred date", "type": "date"}]' WHERE code = 'labtest';
UPDATE service_categories SET icon = '🧑‍⚕️', fields_json = '[{"id": "careNeeded", "label": "Type of care needed", "type": "text"}, {"id": "duration", "label": "Duration", "type": "select", "options": ["A few hours", "Full day", "Live-in", "Ongoing / weekly"]}, {"id": "address", "label": "Address", "type": "text"}, {"id": "startDate", "label": "Start date", "type": "date"}]' WHERE code = 'homenurse';
UPDATE service_categories SET icon = '🏃', fields_json = '[{"id": "reason", "label": "Reason for visit", "type": "text"}, {"id": "mode", "label": "Visit type", "type": "select", "options": ["Home visit", "Clinic visit"]}, {"id": "address", "label": "Address", "type": "text"}, {"id": "preferredDate", "label": "Preferred date", "type": "date"}]' WHERE code = 'physio';
UPDATE service_categories SET icon = '🚜', fields_json = '[{"id": "workType", "label": "Type of work", "type": "text"}, {"id": "acreage", "label": "Approx. area", "type": "text"}, {"id": "location", "label": "Location", "type": "text"}, {"id": "neededDate", "label": "Needed on", "type": "date"}]' WHERE code = 'tractor';
UPDATE service_categories SET icon = '🌾', fields_json = '[{"id": "equipment", "label": "Equipment needed", "type": "text"}, {"id": "location", "label": "Location", "type": "text"}, {"id": "neededDate", "label": "Needed on", "type": "date"}]' WHERE code = 'farmequipment';
UPDATE service_categories SET icon = '🏛️', fields_json = '[{"id": "propertyAddress", "label": "Property address", "type": "text"}, {"id": "dueDate", "label": "Due date (if known)", "type": "date"}]' WHERE code = 'propertytax';
UPDATE service_categories SET icon = '🔥', fields_json = '[{"id": "provider", "label": "Gas provider", "type": "text"}, {"id": "consumerNumber", "label": "Consumer number", "type": "text"}, {"id": "address", "label": "Delivery address", "type": "text"}]' WHERE code = 'gasbooking';
UPDATE service_categories SET icon = '📢', fields_json = '[{"id": "issueType", "label": "Issue type", "type": "select", "options": ["Streetlight", "Garbage collection", "Road/pothole", "Water supply", "Drainage", "Other"]}, {"id": "location", "label": "Location", "type": "text"}, {"id": "description", "label": "Description", "type": "text"}]' WHERE code = 'civiccomplaint';

-- Icons only for the dedicated-page categories (no fields_json — their forms aren't generic).
UPDATE service_categories SET icon = '🧵' WHERE code = 'tailor';
UPDATE service_categories SET icon = '🍲' WHERE code = 'food';
UPDATE service_categories SET icon = '🛒' WHERE code = 'grocery';
UPDATE service_categories SET icon = '👗' WHERE code = 'fashion';
