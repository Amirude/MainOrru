package com.ooru.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ooru.dto.AssistantDtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;

/**
 * Ports the browser-only AI assistant prototype into a real backend call. The key difference
 * from that prototype: the Anthropic API key lives here, server-side, and is never sent to the
 * browser — calling a paid API directly from frontend JavaScript would expose whatever key you
 * used to everyone who opens dev tools.
 *
 * Only covers the simple field-based categories (Xerox, AC, Plumber, Electrician, Parcel,
 * Rental, Driver, House Rent, Hotel, Scrap, Food Donation, Old Clothes). Food, Grocery, and
 * Tailor all need something chosen first (a shop's menu, or a real appointment slot) — a
 * different interaction shape than filling in fields, so FoodOrder.jsx and TailorBooking.jsx
 * handle those directly rather than through this assistant.
 */
@Service
public class AssistantService {

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssistantService(@Value("${ooru.anthropic.api-key}") String apiKey,
                             @Value("${ooru.anthropic.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public record FieldDef(String id, String label, String type, List<String> options) {}
    public record CategoryDef(String code, String name, List<FieldDef> fields) {}

    public static final List<CategoryDef> SCHEMA = List.of(
        new CategoryDef("xerox", "Xerox / Printout", List.of(
            new FieldDef("copies", "Copies / pages", "text", null),
            new FieldDef("color", "Print type", "select", List.of("Black & white", "Color")),
            new FieldDef("mode", "Pickup or delivery", "select", List.of("I will pick up", "Deliver to my address")),
            new FieldDef("address", "Address", "text", null))),
        new CategoryDef("ac", "AC Service", List.of(
            new FieldDef("work", "Service type", "select", List.of("Regular service", "Gas refill", "Repair", "New installation")),
            new FieldDef("address", "Address", "text", null),
            new FieldDef("date", "Date", "date", null),
            new FieldDef("time", "Time", "time", null))),
        new CategoryDef("plumber", "Plumber", List.of(
            new FieldDef("issue", "Issue", "select", List.of("Leak", "Blocked drain", "Tap / fitting", "New pipeline", "Other")),
            new FieldDef("urgency", "Urgency", "select", List.of("Urgent — today", "Within 2-3 days", "Whenever convenient")),
            new FieldDef("address", "Address", "text", null),
            new FieldDef("date", "Date", "date", null),
            new FieldDef("time", "Time", "time", null))),
        new CategoryDef("electrician", "Electrician", List.of(
            new FieldDef("issue", "Issue", "select", List.of("Wiring problem", "Switch/socket repair", "New fitting installation", "Fan/light issue", "Other")),
            new FieldDef("urgency", "Urgency", "select", List.of("Urgent — today", "Within 2-3 days", "Whenever convenient")),
            new FieldDef("address", "Address", "text", null),
            new FieldDef("date", "Date", "date", null),
            new FieldDef("time", "Time", "time", null))),
        new CategoryDef("parcel", "Parcel Pickup & Delivery", List.of(
            new FieldDef("pickupAddress", "Pickup address", "text", null),
            new FieldDef("dropAddress", "Drop address", "text", null),
            new FieldDef("itemDescription", "What are you sending", "text", null),
            new FieldDef("date", "Date", "date", null))),
        new CategoryDef("rental", "Bike/Car Rental", List.of(
            new FieldDef("vehicleType", "Vehicle type", "select", List.of("Bike", "Car (hatchback)", "Car (sedan/SUV)", "Van")),
            new FieldDef("pickupLocation", "Pickup location", "text", null),
            new FieldDef("pickupDate", "Pickup date", "date", null),
            new FieldDef("returnDate", "Return date", "date", null))),
        new CategoryDef("driver", "Driver Booking", List.of(
            new FieldDef("tripType", "Trip type", "select", List.of("One-way", "Round trip", "Monthly / long-term")),
            new FieldDef("pickupLocation", "Pickup location", "text", null),
            new FieldDef("dropLocation", "Drop location", "text", null),
            new FieldDef("date", "Date", "date", null))),
        new CategoryDef("houserent", "House Rent / Lease", List.of(
            new FieldDef("propertyType", "Looking for", "select", List.of("1 BHK", "2 BHK", "3 BHK", "Independent house", "Commercial space")),
            new FieldDef("area", "Preferred area", "text", null),
            new FieldDef("budget", "Monthly budget", "text", null))),
        new CategoryDef("hotel", "Hotel Booking", List.of(
            new FieldDef("city", "City", "text", null),
            new FieldDef("checkin", "Check-in", "date", null),
            new FieldDef("checkout", "Check-out", "date", null),
            new FieldDef("guests", "Guests", "text", null))),
        new CategoryDef("scrap", "Scrap Collection", List.of(
            new FieldDef("items", "What are you selling", "text", null),
            new FieldDef("estimatedWeight", "Approx. weight (kg)", "text", null),
            new FieldDef("address", "Pickup address", "text", null),
            new FieldDef("date", "Preferred pickup date", "date", null))),
        new CategoryDef("fooddonation", "Food Donation", List.of(
            new FieldDef("recipient", "Who is this for", "select", List.of("An elderly person", "A person with a disability", "A widow in need", "A family I know")),
            new FieldDef("quantity", "Meals / quantity", "text", null),
            new FieldDef("address", "Address", "text", null))),
        new CategoryDef("oldclothes", "Old Clothes Donation", List.of(
            new FieldDef("quantity", "Approx. quantity", "text", null),
            new FieldDef("address", "Pickup address", "text", null),
            new FieldDef("date", "Preferred pickup date", "date", null))),
        new CategoryDef("doctor", "Doctor Appointment", List.of(
            new FieldDef("specialty", "Specialty (if known)", "text", null),
            new FieldDef("reason", "Reason for visit", "text", null),
            new FieldDef("preferredDate", "Preferred date", "date", null))),
        new CategoryDef("dentist", "Dentist Booking", List.of(
            new FieldDef("reason", "Reason for visit", "text", null),
            new FieldDef("preferredDate", "Preferred date", "date", null))),
        new CategoryDef("labtest", "Diagnostic Lab Test", List.of(
            new FieldDef("testType", "Test(s) needed", "text", null),
            new FieldDef("homeCollection", "Sample collection", "select", List.of("Home collection", "Visit the lab")),
            new FieldDef("address", "Address", "text", null),
            new FieldDef("preferredDate", "Preferred date", "date", null))),
        new CategoryDef("homenurse", "Home Nurse", List.of(
            new FieldDef("careNeeded", "Type of care needed", "text", null),
            new FieldDef("duration", "Duration", "select", List.of("A few hours", "Full day", "Live-in", "Ongoing / weekly")),
            new FieldDef("address", "Address", "text", null),
            new FieldDef("startDate", "Start date", "date", null))),
        new CategoryDef("physio", "Physiotherapist", List.of(
            new FieldDef("reason", "Reason for visit", "text", null),
            new FieldDef("mode", "Visit type", "select", List.of("Home visit", "Clinic visit")),
            new FieldDef("address", "Address", "text", null),
            new FieldDef("preferredDate", "Preferred date", "date", null))),
        new CategoryDef("tractor", "Tractor Rental", List.of(
            new FieldDef("workType", "Type of work", "text", null),
            new FieldDef("acreage", "Approx. area", "text", null),
            new FieldDef("location", "Location", "text", null),
            new FieldDef("neededDate", "Needed on", "date", null))),
        new CategoryDef("farmequipment", "Farm Equipment Rental", List.of(
            new FieldDef("equipment", "Equipment needed", "text", null),
            new FieldDef("location", "Location", "text", null),
            new FieldDef("neededDate", "Needed on", "date", null))),
        new CategoryDef("propertytax", "Property Tax Reminder", List.of(
            new FieldDef("propertyAddress", "Property address", "text", null),
            new FieldDef("dueDate", "Due date (if known)", "date", null))),
        new CategoryDef("gasbooking", "Gas Cylinder Booking", List.of(
            new FieldDef("provider", "Gas provider", "text", null),
            new FieldDef("consumerNumber", "Consumer number", "text", null),
            new FieldDef("address", "Delivery address", "text", null))),
        new CategoryDef("civiccomplaint", "Civic Complaint", List.of(
            new FieldDef("issueType", "Issue type", "select", List.of("Streetlight", "Garbage collection", "Road/pothole", "Water supply", "Drainage", "Other")),
            new FieldDef("location", "Location", "text", null),
            new FieldDef("description", "Description", "text", null)))
    );

    public ChatResponse chat(ChatRequest req) throws Exception {
        String schemaJson = objectMapper.writeValueAsString(SCHEMA);
        String today = LocalDate.now().toString();

        String systemPrompt = """
            You are a routing assistant inside a local-services app called Altenul One. Today's date is %s \
            (use this to resolve words like "today" or "tomorrow" into an actual YYYY-MM-DD date).

            Available service categories and their fields, as JSON:
            %s

            You will be given the current known state (which category has been matched so far, and \
            which field values have been extracted) plus the user's latest message. Update the state:
            - Pick the single best-matching category code from the list above (or keep the existing \
            one). If truly nothing matches, leave category as null.
            - Extract values for as many of that category's fields as you reasonably can. For "date" \
            fields always output YYYY-MM-DD. For "time" fields always output 24-hour HH:MM.
            - For "select" fields, only use one of the exact listed options, or leave it out if unclear.
            - If any fields are missing, ask exactly ONE short, friendly clarifying question about the \
            single most important missing field.
            - If all fields are filled, set ready to true and write a short, friendly one-line \
            confirmation-style reply (no clarifying question).
            - If no category matches at all, set ready false, category null, and ask what kind of \
            service they need.

            Respond ONLY with raw JSON in exactly this shape, no markdown, no code fences:
            {"category": "code or null", "data": {"fieldId": "value"}, "ready": true or false, "reply": "short natural sentence"}
            """.formatted(today, schemaJson);

        Map<String, Object> stateAndMessage = Map.of(
            "currentState", req.state,
            "latestMessage", req.message
        );
        String userContent = objectMapper.writeValueAsString(stateAndMessage);

        Map<String, Object> body = Map.of(
            "model", model,
            "max_tokens", 1000,
            "system", systemPrompt,
            "messages", List.of(Map.of("role", "user", "content", userContent))
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("Assistant call failed: " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String rawText = root.path("content").isArray() && root.path("content").size() > 0
                ? root.path("content").get(0).path("text").asText()
                : "";
        String cleaned = rawText.replaceAll("```json|```", "").trim();

        JsonNode parsed = objectMapper.readTree(cleaned);
        ChatResponse result = new ChatResponse();
        result.category = parsed.path("category").isNull() ? null : parsed.path("category").asText(null);
        result.ready = parsed.path("ready").asBoolean(false);
        result.reply = parsed.path("reply").asText("Let's see what I can find for that.");

        Map<String, String> data = new LinkedHashMap<>(req.state.data);
        if (parsed.has("data") && parsed.get("data").isObject()) {
            parsed.get("data").fields().forEachRemaining(e -> data.put(e.getKey(), e.getValue().asText()));
        }
        result.data = data;
        return result;
    }

    public Optional<CategoryDef> findCategory(String code) {
        return SCHEMA.stream().filter(c -> c.code().equals(code)).findFirst();
    }
}
