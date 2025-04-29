// Filename: RelicHunter/src/main/java/com/relichunter/UnlockManager.java
// Content:
package com.relichunter;

import net.runelite.api.Skill;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Manages the master list (pool) of all defined Unlockable content.
 * Provides methods to query this pool.
 */
@Slf4j
public class UnlockManager {

    // Map to store all unlockables, keyed by their unique ID.
    private static final Map<String, Unlockable> UNLOCK_POOL;

    /**
     * Helper method to generate Skill Tier Unlock IDs consistently. Made public static.
     * @param skill The OSRS skill.
     * @param tier The SkillTier.
     * @return The generated ID string (e.g., SKILL_WOODCUTTING_APPRENTICE).
     */
    public static String getSkillTierId(Skill skill, SkillTier tier) {
        if (tier == SkillTier.LOCKED) {
            log.warn("Attempted to generate Skill Tier ID for LOCKED state, this might not be intended.");
        }
        if (skill == null) {
            log.error("Attempted to generate Skill Tier ID with null skill!");
            return "SKILL_NULL_" + (tier != null ? tier.name() : "NULLTIER");
        }
        if (skill == Skill.OVERALL) {
            log.warn("Attempted to generate Skill Tier ID for OVERALL skill.");
            return "SKILL_OVERALL_" + tier.name();
        }
        return "SKILL_" + skill.getName().toUpperCase() + "_" + tier.name();
    }

    /**
     * Helper method to generate Gear Tier Unlock IDs consistently.
     * @param tier The GearTier.
     * @return The generated ID string (e.g., GEAR_MITHRIL).
     */
    public static String getGearTierId(GearTier tier) {
        if (tier == null || tier == GearTier.NONE) {
            log.warn("Attempted to generate Gear Tier ID for null or NONE tier.");
            return "GEAR_INVALID";
        }
        return "GEAR_" + tier.name();
    }


    // Static initializer block to populate the pool when the class is loaded.
    static {
        List<Unlockable> unlocks = new ArrayList<>();
        List<Skill> allSkills = Arrays.asList(Skill.values());

        // --- Define Skill Tier Unlocks (Now split by Relic Type) ---

        // Skills unlocked via SKILLING relics (Skilling skills)
        List<Skill> skillingSkills = List.of(
                Skill.COOKING, Skill.WOODCUTTING, Skill.FLETCHING, Skill.FISHING,
                Skill.FIREMAKING, Skill.CRAFTING, Skill.SMITHING, Skill.MINING,
                Skill.HERBLORE, Skill.AGILITY, Skill.THIEVING, Skill.SLAYER, // Slayer could be Combat? TBD
                Skill.FARMING, Skill.RUNECRAFT, Skill.HUNTER, Skill.CONSTRUCTION
        );
        for (Skill skill : skillingSkills) {
            // Apprentice Tier (Skilling Relic)
            unlocks.add(new Unlockable(
                    getSkillTierId(skill, SkillTier.APPRENTICE),
                    skill.getName() + " (" + SkillTier.APPRENTICE.getDisplayName() + ")",
                    "Unlocks " + skill.getName() + " up to level " + SkillTier.APPRENTICE.getLevelCap() + ".",
                    RelicType.SKILLING, // Type
                    SkillTier.APPRENTICE // Target Tier
            ));
            // Journeyman Tier (Skilling Relic)
            unlocks.add(new Unlockable(
                    getSkillTierId(skill, SkillTier.JOURNEYMAN),
                    skill.getName() + " (" + SkillTier.JOURNEYMAN.getDisplayName() + ")",
                    "Unlocks " + skill.getName() + " up to level " + SkillTier.JOURNEYMAN.getLevelCap() + ".",
                    RelicType.SKILLING, // Type
                    SkillTier.JOURNEYMAN, // Target Tier
                    List.of(getSkillTierId(skill, SkillTier.APPRENTICE)) // Prerequisite
            ));
            // TODO: Add Expert, Master, Grandmaster tiers for Skilling skills
        }

        // Skills unlocked via COMBAT relics (Ranged, Magic, Prayer, Hitpoints)
        // Attack, Strength, Defence tiers are REMOVED - use Gear Tiers instead.
        List<Skill> combatLevelSkills = List.of(
                Skill.RANGED, Skill.MAGIC, Skill.PRAYER, Skill.HITPOINTS
        );
        for (Skill skill : combatLevelSkills) {
            // Apprentice Tier (Combat Relic)
            unlocks.add(new Unlockable(
                    getSkillTierId(skill, SkillTier.APPRENTICE),
                    skill.getName() + " (" + SkillTier.APPRENTICE.getDisplayName() + ")",
                    "Unlocks " + skill.getName() + " up to level " + SkillTier.APPRENTICE.getLevelCap() + ".",
                    RelicType.COMBAT, // <<< Type is COMBAT
                    SkillTier.APPRENTICE // Target Tier
            ));
            // Journeyman Tier (Combat Relic)
            unlocks.add(new Unlockable(
                    getSkillTierId(skill, SkillTier.JOURNEYMAN),
                    skill.getName() + " (" + SkillTier.JOURNEYMAN.getDisplayName() + ")",
                    "Unlocks " + skill.getName() + " up to level " + SkillTier.JOURNEYMAN.getLevelCap() + ".",
                    RelicType.COMBAT, // <<< Type is COMBAT
                    SkillTier.JOURNEYMAN, // Target Tier
                    List.of(getSkillTierId(skill, SkillTier.APPRENTICE)) // Prerequisite
            ));
            // TODO: Add Expert, Master, Grandmaster tiers for these Combat skills
        }


        // --- Define Melee Gear Tier Unlocks (Combat Relics) ---
        // Define prerequisites between gear tiers
        // Assuming BASIC (Bronze/Iron/Steel) is default allowed, no unlock needed for it.
        unlocks.add(new Unlockable(
                getGearTierId(GearTier.MITHRIL), // ID: GEAR_MITHRIL
                "Equip Mithril Gear", // Name
                "Allows equipping Melee armour and weapons up to Mithril.", // Desc
                RelicType.COMBAT, // Type
                SkillTier.APPRENTICE // Target Tier (Requires Apprentice Combat Relic)
                // No gear prerequisite for Mithril, assuming BASIC is default start
        ));
        unlocks.add(new Unlockable(
                getGearTierId(GearTier.ADAMANT), // ID: GEAR_ADAMANT
                "Equip Adamant Gear", // Name
                "Allows equipping Melee armour and weapons up to Adamant.", // Desc
                RelicType.COMBAT, // Type
                SkillTier.JOURNEYMAN, // Target Tier (Requires Journeyman Combat Relic)
                List.of(getGearTierId(GearTier.MITHRIL)) // Prerequisite: Mithril Gear unlocked
        ));
        // TODO: Add unlocks for Rune, Dragon, Barrows etc. following the pattern


        // --- Define Other Test Unlocks (Update Prerequisites/Types/Tiers if needed) ---
        unlocks.add(new Unlockable(
                "METHOD_OAK_TREES", // ID
                "Chop Oak Trees", // Name
                "Allows chopping Oak trees.", // Desc
                RelicType.SKILLING, // Type remains Skilling
                SkillTier.APPRENTICE, // Still an Apprentice level unlock
                List.of(getSkillTierId(Skill.WOODCUTTING, SkillTier.APPRENTICE)) // Requires Woodcutting Apprentice
        ));

        unlocks.add(new Unlockable(
                "AREA_LUMBRIDGE_SWAMP_FISHING", // ID
                "Access Lumbridge Swamp Fishing", // Name
                "Grants access to Lumbridge Swamp fishing spots.", // Desc
                RelicType.SKILLING, // Or Exploration? Let's keep Skilling for now
                SkillTier.APPRENTICE, // Still an Apprentice level unlock
                List.of(getSkillTierId(Skill.FISHING, SkillTier.APPRENTICE)) // Requires Fishing Apprentice
        ));

        // --- TODO: Add more Combat Relic Unlocks (Bosses, Prayers, etc.) with appropriate Tiers ---
        // --- TODO: Add Exploration Relic Unlocks (Areas, travel, quests, etc.) with appropriate Tiers ---


        // --- Finalize Pool ---
        try {
            UNLOCK_POOL = Collections.unmodifiableMap(unlocks.stream()
                    .collect(Collectors.toMap(Unlockable::getId, unlock -> unlock)));
        } catch (IllegalStateException e) {
            log.error("Duplicate Unlockable ID detected during UnlockManager initialization!", e);
            throw e;
        }

        log.info("Loaded {} unlockables into the pool.", UNLOCK_POOL.size());
    }

    public static Optional<Unlockable> getUnlockById(String id) {
        return Optional.ofNullable(UNLOCK_POOL.get(id));
    }

    public static List<Unlockable> getUnlocksByType(RelicType type) {
        return UNLOCK_POOL.values().stream()
                .filter(unlock -> unlock.getType() == type)
                .collect(Collectors.toList());
    }

    public static List<Unlockable> getUnlocksByTypeAndTier(RelicType type, SkillTier tier) {
        return UNLOCK_POOL.values().stream()
                .filter(unlock -> unlock.getType() == type)
                .filter(unlock -> unlock.getTargetTier() == tier)
                .collect(Collectors.toList());
    }

    public static List<Unlockable> getAllUnlocks() {
        return List.copyOf(UNLOCK_POOL.values());
    }

    private UnlockManager() {}
}