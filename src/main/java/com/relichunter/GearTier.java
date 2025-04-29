// Filename: RelicHunter/src/main/java/com/relichunter/GearTier.java
// Content:
package com.relichunter;

import java.util.List;
import java.util.Objects; // Import Objects for null checks later if needed

/**
 * Represents the tiers of melee equipment allowed to be equipped.
 * Tiers added/renamed to better match common progression milestones.
 */
public enum GearTier {
    // Define tiers in approximate progression order
    NONE("None", "Cannot equip any tiered melee gear."), // Starting point before any unlocks?
    STEEL("Steel", "Up to Steel/Black/White"),           // Approx Lvl 5-10 Req
    MITHRIL("Mithril", "Up to Mithril"),                // Approx Lvl 20 Req
    ADAMANT("Adamant", "Up to Adamant"),              // Approx Lvl 30 Req
    RUNE("Rune", "Up to Rune"),                    // Approx Lvl 40 Req
    GRANITE("Granite/Mystic", "Up to Granite/Mystic"), // Approx Lvl 50 Req (Grouping)
    DRAGON("Dragon/Obsidian", "Up to Dragon/Obsidian"), // Approx Lvl 60 Req
    ABYSSAL_BARROWS("Abyssal/Barrows", "Up to Barrows/Abyssal/Crystal"), // Approx Lvl 70 Req
    GODSWORD("Godsword", "Up to Godswords/Toxic Staff"), // Approx Lvl 75 Req
    HIGH_TIER("High Tier", "Level 80+ Gear");        // Approx Lvl 80+ Req (Rapier, Scythe, etc.)
    // Future: Could add more specific high tiers (Nex, Torva etc.)

    private final String displayName;
    private final String description;

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
     * Gets the next gear tier in the progression based on enum declaration order.
     * Returns null if already at the highest defined tier.
     */
    public GearTier getNextTier() {
        GearTier[] tiers = values();
        int currentIndex = this.ordinal(); // Use ordinal() for index based on declaration order
        if (currentIndex < tiers.length - 1) {
            return tiers[currentIndex + 1];
        }
        return null; // No higher tier defined
    }

    /**
     * Finds a GearTier by its display name (case-insensitive).
     * Useful for retrieving from config if using names (though storing the enum itself is better).
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

    /**
     * Finds a GearTier by its enum constant name (case-insensitive).
     * Useful for loading from data files where keys might match enum names.
     * @param enumName The enum constant name (e.g., "MITHRIL").
     * @return The matching GearTier, or NONE if not found.
     */
    public static GearTier fromEnumName(String enumName) {
        if (enumName == null) return NONE;
        for (GearTier tier : values()) {
            if (tier.name().equalsIgnoreCase(enumName)) {
                return tier;
            }
        }
        return NONE;
    }
}