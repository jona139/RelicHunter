// Filename: RelicHunter/src/main/java/com/relichunter/UnlockManager.java
// Content:
package com.relichunter;

import com.google.inject.Singleton;
import com.relichunter.unlock.UnlockData;
import com.relichunter.unlock.UnlockType;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill; // Keep Skill import

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the master database of all defined Unlockable content, loaded from JSON.
 * Provides methods to query this database and check prerequisites/permissions.
 */
@Slf4j
@Singleton
public class UnlockManager {

    private Map<String, UnlockData> unlockDatabase = Collections.emptyMap();
    private Map<String, Set<String>> prerequisiteCache = Collections.emptyMap();
    // Cache mapping Item ID -> Specific Item Unlock ID (if one exists)
    private Map<Integer, String> itemSpecificUnlockIdCache = Collections.emptyMap();
    // Cache mapping Item ID -> Set of Gear Tier Unlock IDs it belongs to
    private Map<Integer, Set<String>> itemGearTierUnlockIdsCache = Collections.emptyMap();


    @Inject
    private UnlockManager() {
        // Initialization logic moved to loadData
    }

    /**
     * Loads and processes the unlock data from the parsed list.
     * Should be called by the plugin during startup.
     * @param loadedUnlocks List of UnlockData parsed from JSON.
     */
    public synchronized void loadData(List<UnlockData> loadedUnlocks) {
        if (loadedUnlocks == null || loadedUnlocks.isEmpty()) {
            log.warn("Unlock database is empty or null. Clearing existing data.");
            this.unlockDatabase = Collections.emptyMap();
            this.prerequisiteCache = Collections.emptyMap();
            this.itemSpecificUnlockIdCache = Collections.emptyMap();
            this.itemGearTierUnlockIdsCache = Collections.emptyMap();
            return;
        }

        Map<String, UnlockData> newDatabase = new ConcurrentHashMap<>();
        Map<String, Set<String>> newPrereqCache = new ConcurrentHashMap<>();
        Map<Integer, String> newSpecificItemCache = new ConcurrentHashMap<>();
        Map<Integer, Set<String>> newGearTierCache = new ConcurrentHashMap<>();

        for (UnlockData unlock : loadedUnlocks) {
            if (unlock.getId() == null || unlock.getId().isBlank()) {
                log.warn("Skipping unlock with missing or blank ID: {}", unlock.getName());
                continue;
            }
            if (newDatabase.containsKey(unlock.getId())) {
                log.warn("Duplicate Unlock ID detected! Skipping duplicate entry for ID: {}", unlock.getId());
                continue;
            }

            newDatabase.put(unlock.getId(), unlock);

            // Populate prerequisite cache
            newPrereqCache.put(unlock.getId(), unlock.getPrerequisites() != null ? Set.copyOf(unlock.getPrerequisites()) : Collections.emptySet());

            // Populate item caches
            if (unlock.getItemIds() != null && !unlock.getItemIds().isEmpty()) {
                if (unlock.getCategory() == UnlockType.SPECIFIC_ITEM) {
                    for (Integer itemId : unlock.getItemIds()) {
                        // Warn if an item is defined in multiple SPECIFIC_ITEM unlocks (usually shouldn't happen)
                        if (newSpecificItemCache.containsKey(itemId)) {
                            log.warn("Item ID {} is defined in multiple SPECIFIC_ITEM unlocks ({} and {}). Using the latter.",
                                    itemId, newSpecificItemCache.get(itemId), unlock.getId());
                        }
                        newSpecificItemCache.put(itemId, unlock.getId());
                    }
                } else if (unlock.getCategory() == UnlockType.GEAR_TIER) {
                    for (Integer itemId : unlock.getItemIds()) {
                        // Add the gear tier ID to the set for this item
                        newGearTierCache.computeIfAbsent(itemId, k -> ConcurrentHashMap.newKeySet()).add(unlock.getId());
                    }
                }
            }
        }

        this.unlockDatabase = newDatabase;
        this.prerequisiteCache = newPrereqCache;
        this.itemSpecificUnlockIdCache = newSpecificItemCache;
        this.itemGearTierUnlockIdsCache = newGearTierCache;

        log.info("Processed {} unlocks into the database.", this.unlockDatabase.size());
    }

    /** Clears the loaded database. */
    public synchronized void clearDatabase() {
        this.unlockDatabase = Collections.emptyMap();
        this.prerequisiteCache = Collections.emptyMap();
        this.itemSpecificUnlockIdCache = Collections.emptyMap();
        this.itemGearTierUnlockIdsCache = Collections.emptyMap();
        log.info("Cleared unlock database.");
    }

    /**
     * Gets an UnlockData entry by its unique ID.
     * @param id The unique ID.
     * @return Optional containing the UnlockData if found.
     */
    public Optional<UnlockData> getUnlockById(String id) {
        return Optional.ofNullable(unlockDatabase.get(id));
    }

    /**
     * Gets all potential unlocks matching the specified Relic type and tier.
     * @param type The RelicType required.
     * @param tier The SkillTier of the relic being used.
     * @return A list of matching UnlockData entries.
     */
    public List<UnlockData> getPotentialUnlocks(RelicType type, SkillTier tier) {
        return unlockDatabase.values().stream()
                .filter(unlock -> unlock.getRelicType() == type)
                .filter(unlock -> unlock.getRequiredTier() == tier)
                .collect(Collectors.toList());
    }

    /**
     * Checks if all prerequisites for a given unlock ID are met based on the set of currently unlocked IDs.
     * Uses a cache for efficiency.
     * @param unlockId The ID of the unlock to check.
     * @param currentlyUnlockedIds The set of IDs the player has already unlocked.
     * @return true if all prerequisites are met, false otherwise.
     */
    public boolean arePrerequisitesMet(String unlockId, Set<String> currentlyUnlockedIds) {
        Set<String> prereqs = prerequisiteCache.getOrDefault(unlockId, Collections.emptySet());
        if (prereqs.isEmpty()) {
            return true; // No prerequisites
        }
        return currentlyUnlockedIds.containsAll(prereqs);
    }

    /**
     * Checks if a specific item is permitted based ONLY on SPECIFIC_ITEM or GEAR_TIER unlocks
     * recorded in the database and the player's current unlocks.
     * Does NOT check skill requirements.
     *
     * @param itemId The item ID to check.
     * @param currentlyUnlockedIds The set of IDs the player has already unlocked.
     * @return true if the item is permitted by a specific or gear tier unlock, false otherwise.
     * Returns true if the item is not found in any relevant unlock definition (assumed unrestricted).
     */
    public boolean isItemPermittedByUnlocks(int itemId, Set<String> currentlyUnlockedIds) {
        // 1. Check SPECIFIC_ITEM unlock cache
        String specificUnlockId = itemSpecificUnlockIdCache.get(itemId);
        if (specificUnlockId != null) {
            log.trace("Item {} found specific unlock ID {}. Checking if unlocked...", itemId, specificUnlockId);
            // If a specific unlock exists for this item, permission depends *only* on whether that specific unlock is active.
            return currentlyUnlockedIds.contains(specificUnlockId);
        }

        // 2. Check GEAR_TIER unlock cache
        Set<String> relevantGearTierIds = itemGearTierUnlockIdsCache.get(itemId);
        if (relevantGearTierIds != null && !relevantGearTierIds.isEmpty()) {
            log.trace("Item {} found in gear tiers: {}. Checking if any are unlocked...", itemId, relevantGearTierIds);
            // If the item is part of any gear tier definition, it's permitted if *at least one* of those tiers is unlocked.
            // This assumes the itemIds in the JSON for GEAR_TIER are cumulative (e.g., Mithril includes Steel).
            // If they are tier-specific, this logic needs changing to find the *lowest* tier the item belongs to
            // and check if that tier *or higher* is unlocked.
            for (String gearTierId : relevantGearTierIds) {
                if (currentlyUnlockedIds.contains(gearTierId)) {
                    log.trace("Item {} permitted by unlocked gear tier {}", itemId, gearTierId);
                    return true; // Permitted by at least one unlocked gear tier
                }
            }
            // If the item was found in gear tiers, but none of them are unlocked, it's restricted.
            log.trace("Item {} found in gear tiers, but none are unlocked. Item restricted.", itemId);
            return false;
        }

        // 3. Default: Item not found in specific unlocks or gear tiers - assume permitted.
        log.trace("Item {} not found in specific or gear tier unlocks. Assuming permitted.", itemId);
        return true;
    }


    // --- Static Helper Methods (kept from original for ID generation consistency) ---
    // These might need adjustment depending on final ID format in JSON

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
        // Assume a prefix like GEAR_MELEE_ for now, needs refinement
        return "GEAR_MELEE_" + tier.name(); // TODO: Refine based on actual JSON structure (MELEE/RANGED/MAGIC?)
    }

    // Removed config injection - no longer needed here
}
