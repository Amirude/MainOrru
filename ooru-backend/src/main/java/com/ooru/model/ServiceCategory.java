package com.ooru.model;

import jakarta.persistence.*;

/**
 * Phase 1 ships with a fixed row per category (tailor, xerox, ac, plumber, electrician).
 * Adding a new category later in Phase 2/3 is a data change, not a code change — insert
 * a new row here and the generic booking endpoints will handle it.
 */
@Entity
@Table(name = "service_categories")
public class ServiceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // e.g. "tailor", "xerox", "ac", "plumber", "electrician"

    @Column(nullable = false)
    private String displayName;

    private String icon; // an emoji, shown in the UI

    // JSON array of {id, label, type, options} — the exact field schema the generic booking form
    // renders. NULL for categories with a dedicated page instead (tailor, food, grocery).
    @Lob
    private String fieldsJson;

    @Column(nullable = false)
    private boolean active = true;

    public ServiceCategory() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getFieldsJson() { return fieldsJson; }
    public void setFieldsJson(String fieldsJson) { this.fieldsJson = fieldsJson; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
