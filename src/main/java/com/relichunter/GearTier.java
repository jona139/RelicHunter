// Filename: RelicHunter/src/main/java/com/relichunter/GearTier.java
// Content:
package com.relichunter;

import java.util.List;

/**
 * Represents the tiers of melee equipment allowed to be equipped.
 * We can add specific item IDs or level requirements later if needed.
 */
public enum GearTier {
    // Define tiers, maybe map them roughly to SkillTiers for unlock purposes?
    NONE("None", "Cannot equip any tiered melee gear."), // Could be starting point if desired
    BASIC("Basic", "Bronze, Iron, Steel"), // Default starting tier?
    MITHRIL("Mithril", "Up to Mithril"),
    ADAMANT("Adamant", "Up to Adamant"),
    RUNE("Rune", "Up to Rune"),
    DRAGON("Dragon", "Up to Dragon"), // Example higher tier
    BARROWS("Barrows", "Up to Barrows"), // Example higher tier
    BANDOS("Bandos", "Up to Bandos/GWD"); // Example higher tier
    // Add more tiers as needed (Crystal, Torva etc for Grandmaster?)

    private final String displayName;
    private final String description; // Simple description for now

    GearTier(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Gets the next gear tier in the progression.
     * Returns null if already at the highest defined tier.
     * NOTE: Order is important here!
     */
    public GearTier getNextTier() {
        List<GearTier> tiers = List.of(values());
        int currentIndex = tiers.indexOf(this);
        if (currentIndex >= 0 && currentIndex < tiers.size() - 1) {
            return tiers.get(currentIndex + 1);
        }
        return null; // No higher tier defined
    }

    /**
     * Finds a GearTier by its display name (case-insensitive).
     * Useful for storing/retrieving from config if using names.
     * @param name The display name to find.
     * @return The matching GearTier, or NONE if not found.
     */
    public static GearTier fromDisplayName(String name) {
        if (name == null) return NONE;
        for (GearTier tier : values()) {
            if (tier.displayName.equalsIgnoreCase(name)) {
                return tier;
            }
        }
        return NONE; // Default to NONE if name doesn't match
    }
}