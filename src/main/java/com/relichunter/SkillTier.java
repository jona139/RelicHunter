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
    // *** NOTE: Added level-specific caps for F2P compatibility ***
    // These might overlap with named tiers, ensure consistent usage/parsing
    LEVEL_10("Level 10", 10),
    APPRENTICE("Apprentice", 20), // Level 20 Cap
    LEVEL_20("Level 20", 20),
    LEVEL_30("Level 30", 30),
    JOURNEYMAN("Journeyman", 40), // Level 40 Cap
    LEVEL_40("Level 40", 40),
    LEVEL_50("Level 50", 50),
    EXPERT("Expert", 60),         // Level 60 Cap
    LEVEL_60("Level 60", 60),
    LEVEL_70("Level 70", 70),
    MASTER("Master", 80),         // Level 80 Cap
    LEVEL_80("Level 80", 80),
    LEVEL_90("Level 90", 90),
    GRANDMASTER("Grandmaster", 99); // Assuming Grandmaster allows full leveling
    // LEVEL_99("Level 99", 99); // Redundant with GRANDMASTER

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
     * Gets the next tier in the progression based on level cap.
     * Returns null if already at the highest tier (99).
     * Note: This might skip over level-specific tiers if named tiers exist between them.
     * Consider if a more robust progression logic is needed.
     */
    public SkillTier getNextTier() {
        int nextLevelCap = -1;
        switch (this) {
            // Handle both named and level-specific tiers if needed
            case LOCKED: return LEVEL_10; // Or APPRENTICE if LEVEL_10 isn't used
            case LEVEL_10: return LEVEL_20; // Or APPRENTICE
            case LEVEL_20:
            case APPRENTICE: return LEVEL_30; // Or JOURNEYMAN
            case LEVEL_30: return LEVEL_40; // Or JOURNEYMAN
            case LEVEL_40:
            case JOURNEYMAN: return LEVEL_50; // Or EXPERT
            case LEVEL_50: return LEVEL_60; // Or EXPERT
            case LEVEL_60:
            case EXPERT: return LEVEL_70; // Or MASTER
            case LEVEL_70: return LEVEL_80; // Or MASTER
            case LEVEL_80:
            case MASTER: return LEVEL_90; // Or GRANDMASTER
            case LEVEL_90: return GRANDMASTER;
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

    /**
     * *** ADDED: Finds a SkillTier by its exact level cap. ***
     * Useful for parsing level-based unlock IDs (e.g., SKILL_MINING_LEVEL_10).
     * Prefers named tiers (Apprentice, Journeyman, etc.) if multiple enums share the same cap.
     *
     * @param levelCap The exact level cap to match.
     * @return The matching SkillTier, or null if no tier has that exact level cap.
     */
    public static SkillTier getByLevelCap(int levelCap) {
        SkillTier foundTier = null;
        for (SkillTier tier : values()) {
            if (tier.levelCap == levelCap) {
                // Prioritize named tiers over level-specific ones if caps match
                if (tier == APPRENTICE || tier == JOURNEYMAN || tier == EXPERT || tier == MASTER || tier == GRANDMASTER) {
                    return tier; // Return the named tier immediately
                }
                if (foundTier == null) {
                    foundTier = tier; // Store the first matching level-specific tier
                }
            }
        }
        // Return the found tier (either the named one or the first level-specific one)
        return foundTier;
    }
}
