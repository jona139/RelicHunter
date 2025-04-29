// Filename: RelicHunter/src/main/java/com/relichunter/SkillTier.java
// Content:
package com.relichunter;

/**
 * Represents the unlockable tiers for skills in Relic Hunter mode.
 * Each tier defines a level cap.
 */
public enum SkillTier {
    // Define tiers in progression order
    LOCKED("Locked", 0), // Represents a skill that is completely unusable
    APPRENTICE("Apprentice", 20),
    JOURNEYMAN("Journeyman", 40),
    EXPERT("Expert", 60),
    MASTER("Master", 80),
    GRANDMASTER("Grandmaster", 99); // Assuming Grandmaster allows full leveling

    private final String displayName;
    private final int levelCap;

    SkillTier(String displayName, int levelCap) {
        this.displayName = displayName;
        this.levelCap = levelCap;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevelCap() {
        return levelCap;
    }

    /**
     * Gets the next tier in the progression.
     * Returns null if already at the highest tier.
     */
    public SkillTier getNextTier() {
        switch (this) {
            case LOCKED: return APPRENTICE;
            case APPRENTICE: return JOURNEYMAN;
            case JOURNEYMAN: return EXPERT;
            case EXPERT: return MASTER;
            case MASTER: return GRANDMASTER;
            case GRANDMASTER: return null; // No higher tier
            default: return null;
        }
    }

    /**
     * Finds a SkillTier by its display name (case-insensitive).
     * Useful for storing/retrieving from config if using names.
     * @param name The display name to find.
     * @return The matching SkillTier, or LOCKED if not found.
     */
    public static SkillTier fromDisplayName(String name) {
        if (name == null) return LOCKED;
        for (SkillTier tier : values()) {
            if (tier.displayName.equalsIgnoreCase(name)) {
                return tier;
            }
        }
        return LOCKED; // Default to LOCKED if name doesn't match
    }
}