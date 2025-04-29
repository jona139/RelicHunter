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
import java.util.EnumMap; // Use EnumMap

/**
 * Manages the master list (pool) of all defined Unlockable content.
 * Provides methods to query this pool.
 * NOTE: This currently hardcodes unlocks. Refactor to load from JSON later.
 */
@Slf4j
public class UnlockManager {

    private static final Map<String, Unlockable> UNLOCK_POOL;

    /** Helper method to generate Skill Tier Unlock IDs consistently. */
    public static String getSkillTierId(Skill skill, SkillTier tier) {
        if (tier == SkillTier.LOCKED) { log.warn("Attempted to generate Skill Tier ID for LOCKED state."); }
        if (skill == null) { log.error("Attempted to generate Skill Tier ID with null skill!"); return "SKILL_NULL_" + (tier != null ? tier.name() : "NULLTIER"); }
        if (skill == Skill.OVERALL) { log.warn("Attempted to generate Skill Tier ID for OVERALL skill."); return "SKILL_OVERALL_" + tier.name(); }
        return "SKILL_" + skill.getName().toUpperCase() + "_" + tier.name();
    }

    /** Helper method to generate Gear Tier Unlock IDs consistently. */
    public static String getGearTierId(GearTier tier) {
        if (tier == null || tier == GearTier.NONE) { log.warn("Attempted to generate Gear Tier ID for null or NONE tier."); return "GEAR_INVALID"; }
        return "GEAR_" + tier.name();
    }

    static {
        List<Unlockable> unlocks = new ArrayList<>();

        // --- Define Skill Tier Unlocks (Split by Relic Type) ---
        List<Skill> skillingSkills = List.of(
                Skill.COOKING, Skill.WOODCUTTING, Skill.FLETCHING, Skill.FISHING,
                Skill.FIREMAKING, Skill.CRAFTING, Skill.SMITHING, Skill.MINING,
                Skill.HERBLORE, Skill.AGILITY, Skill.THIEVING, Skill.SLAYER,
                Skill.FARMING, Skill.RUNECRAFT, Skill.HUNTER, Skill.CONSTRUCTION
        );
        for (Skill skill : skillingSkills) {
            // Apprentice Tier (Skilling Relic)
            unlocks.add(new Unlockable( getSkillTierId(skill, SkillTier.APPRENTICE), skill.getName() + " (" + SkillTier.APPRENTICE.getDisplayName() + ")", "Unlocks " + skill.getName() + " up to level " + SkillTier.APPRENTICE.getLevelCap() + ".", RelicType.SKILLING, SkillTier.APPRENTICE ));
            // Journeyman Tier (Skilling Relic)
            unlocks.add(new Unlockable( getSkillTierId(skill, SkillTier.JOURNEYMAN), skill.getName() + " (" + SkillTier.JOURNEYMAN.getDisplayName() + ")", "Unlocks " + skill.getName() + " up to level " + SkillTier.JOURNEYMAN.getLevelCap() + ".", RelicType.SKILLING, SkillTier.JOURNEYMAN, List.of(getSkillTierId(skill, SkillTier.APPRENTICE)) ));
            // TODO: Add Expert, Master, Grandmaster tiers for Skilling skills
        }

        // Skills unlocked via COMBAT relics (Ranged, Magic, Prayer, Hitpoints - HP now uses level cap again based on prior fix?)
        // Let's assume HP *does* have tiers unlocked by Combat Relics, but uses SkillTier level caps.
        // If HP should NOT have tiers, remove it from this list.
        List<Skill> combatLevelSkills = List.of( Skill.RANGED, Skill.MAGIC, Skill.PRAYER, Skill.HITPOINTS );
        for (Skill skill : combatLevelSkills) {
            // Apprentice Tier (Combat Relic)
            unlocks.add(new Unlockable( getSkillTierId(skill, SkillTier.APPRENTICE), skill.getName() + " (" + SkillTier.APPRENTICE.getDisplayName() + ")", "Unlocks " + skill.getName() + " up to level " + SkillTier.APPRENTICE.getLevelCap() + ".", RelicType.COMBAT, SkillTier.APPRENTICE ));
            // Journeyman Tier (Combat Relic)
            unlocks.add(new Unlockable( getSkillTierId(skill, SkillTier.JOURNEYMAN), skill.getName() + " (" + SkillTier.JOURNEYMAN.getDisplayName() + ")", "Unlocks " + skill.getName() + " up to level " + SkillTier.JOURNEYMAN.getLevelCap() + ".", RelicType.COMBAT, SkillTier.JOURNEYMAN, List.of(getSkillTierId(skill, SkillTier.APPRENTICE)) ));
            // TODO: Add Expert, Master, Grandmaster tiers for these Combat skills
        }


        // --- Define Melee Gear Tier Unlocks (Combat Relics) ---
        unlocks.add(new Unlockable( getGearTierId(GearTier.MITHRIL), "Equip Mithril Gear", "Allows equipping Melee armour and weapons up to Mithril.", RelicType.COMBAT, SkillTier.APPRENTICE )); // Requires App Combat Relic
        unlocks.add(new Unlockable( getGearTierId(GearTier.ADAMANT), "Equip Adamant Gear", "Allows equipping Melee armour and weapons up to Adamant.", RelicType.COMBAT, SkillTier.JOURNEYMAN, List.of(getGearTierId(GearTier.MITHRIL)) )); // Requires Jour Combat Relic + Mithril unlock
        // TODO: Add unlocks for Rune, Dragon, Barrows etc.

        // --- Define Other Test Unlocks ---
        unlocks.add(new Unlockable( "METHOD_OAK_TREES", "Chop Oak Trees", "Allows chopping Oak trees.", RelicType.SKILLING, SkillTier.APPRENTICE, List.of(getSkillTierId(Skill.WOODCUTTING, SkillTier.APPRENTICE)) ));
        unlocks.add(new Unlockable( "AREA_LUMBRIDGE_SWAMP_FISHING", "Access Lumbridge Swamp Fishing", "Grants access to Lumbridge Swamp fishing spots.", RelicType.SKILLING, SkillTier.APPRENTICE, List.of(getSkillTierId(Skill.FISHING, SkillTier.APPRENTICE)) ));

        // --- TODO: Add more Combat Relic Unlocks ---
        // --- TODO: Add Exploration Relic Unlocks ---

        // --- Finalize Pool ---
        try {
            UNLOCK_POOL = Collections.unmodifiableMap(unlocks.stream().collect(Collectors.toMap(Unlockable::getId, unlock -> unlock)));
        } catch (IllegalStateException e) { log.error("Duplicate Unlockable ID detected!", e); throw e; }
        log.info("Loaded {} unlockables into the pool.", UNLOCK_POOL.size());
    }

    // --- Accessor Methods ---
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