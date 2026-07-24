package com.ooru.model;

import jakarta.persistence.*;

/**
 * A plain lookup table — bus timings aren't a booking, nobody "requests" one, they just search it.
 * This is deliberately simple; a real deployment would replace this with a live transit data feed
 * rather than a hand-seeded table.
 */
@Entity
@Table(name = "bus_routes")
public class BusRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String routeNumber;

    @Column(nullable = false)
    private String fromStop;

    @Column(nullable = false)
    private String toStop;

    // Kept as a simple human-readable string ("5:40am, 6:10am, every 20 min till 10pm") rather
    // than a rigid schedule model — good enough for a lookup table, not for real-time tracking.
    @Column(nullable = false, length = 1000)
    private String departures;

    public BusRoute() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRouteNumber() { return routeNumber; }
    public void setRouteNumber(String routeNumber) { this.routeNumber = routeNumber; }
    public String getFromStop() { return fromStop; }
    public void setFromStop(String fromStop) { this.fromStop = fromStop; }
    public String getToStop() { return toStop; }
    public void setToStop(String toStop) { this.toStop = toStop; }
    public String getDepartures() { return departures; }
    public void setDepartures(String departures) { this.departures = departures; }
}
