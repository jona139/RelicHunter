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
import java.util.Comparator;
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

        // *** Enhanced Loop for Debugging ***
        for (int i = 0; i < loadedUnlocks.size(); i++) {
            UnlockData unlock = loadedUnlocks.get(i);
            String currentId = "UNKNOWN_ID_AT_INDEX_" + i; // Default ID for logging if unlock or its ID is null

            try {
                if (unlock == null) {
                    log.error("Encountered a null UnlockData object in the loaded list at index {}! Skipping.", i);
                    continue;
                }
                currentId = unlock.getId(); // Get ID early

                if (currentId == null || currentId.isBlank()) {
                    log.warn("Skipping unlock at index {} with missing or blank ID (Name: {}).", i, unlock.getName());
                    continue;
                }
                if (newDatabase.containsKey(currentId)) {
                    log.warn("Duplicate Unlock ID detected! Skipping duplicate entry for ID: {} at index {}.", currentId, i);
                    continue;
                }

                // *** More Detailed Logging ***
                SkillTier parsedTier = unlock.getRequiredTier(); // Get the tier parsed by Gson
                log.trace("Processing index: {}, Unlock ID: {}. Parsed requiredTier by Gson: {}", i, currentId, parsedTier);

                // *** Line 64 Check (where the error occurs) ***
                if (parsedTier == null) {
                    log.error(">>> NullPointerException about to occur! Unlock ID {} (at index {}) has a null requiredTier field after Gson parsing! Check the JSON entry for this ID. Skipping this entry.", currentId, i);
                    // You could add more details here if UnlockData has the raw JSON string stored, e.g.:
                    // log.error("Problematic JSON snippet (if available): {}", unlock.getRawJsonSnippet());
                    continue; // Skip this invalid entry
                }
                // *** End of Detailed Logging ***

                // If we reach here, parsedTier is not null
                newDatabase.put(currentId, unlock);

                // Populate prerequisite cache
                newPrereqCache.put(currentId, unlock.getPrerequisites() != null ? Set.copyOf(unlock.getPrerequisites()) : Collections.emptySet());

                // Populate item caches
                if (unlock.getItemIds() != null && !unlock.getItemIds().isEmpty()) {
                    if (unlock.getCategory() == UnlockType.SPECIFIC_ITEM) {
                        for (Integer itemId : unlock.getItemIds()) {
                            if (newSpecificItemCache.containsKey(itemId)) {
                                log.warn("Item ID {} is defined in multiple SPECIFIC_ITEM unlocks ({} and {}). Using the latter.",
                                        itemId, newSpecificItemCache.get(itemId), currentId);
                            }
                            newSpecificItemCache.put(itemId, currentId);
                        }
                    } else if (unlock.getCategory() == UnlockType.GEAR_TIER) {
                        for (Integer itemId : unlock.getItemIds()) {
                            newGearTierCache.computeIfAbsent(itemId, k -> ConcurrentHashMap.newKeySet()).add(currentId);
                        }
                    }
                }
            } catch (Exception e) {
                // Catch any unexpected exception during the processing of a single unlock
                log.error("Unexpected exception processing unlock with ID '{}' (at index {}):", currentId, i, e);
                // Continue to the next unlock if possible
            }
        } // End of for loop


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
                .filter(unlock -> unlock.getRequiredTier() == tier) // Direct enum comparison
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
            boolean permitted = currentlyUnlockedIds.contains(specificUnlockId);
            log.trace("Item {} specific unlock {} {} unlocked.", itemId, specificUnlockId, permitted ? "IS" : "IS NOT");
            return permitted;
        }

        // 2. Check GEAR_TIER unlock cache
        Set<String> relevantGearTierIds = itemGearTierUnlockIdsCache.get(itemId);
        if (relevantGearTierIds != null && !relevantGearTierIds.isEmpty()) {
            log.trace("Item {} found in gear tiers: {}.", itemId, relevantGearTierIds);

            // Find the *minimum* required SkillTier among all gear tiers this item belongs to.
            SkillTier minimumRequiredTier = relevantGearTierIds.stream()
                    .map(this::getUnlockById) // Get UnlockData for each tier ID
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(UnlockData::getRequiredTier) // Get the SkillTier required for that unlock
                    .filter(java.util.Objects::nonNull) // Ensure tier is not null
                    .min(Comparator.comparing(SkillTier::ordinal)) // Find the lowest tier (based on enum order)
                    .orElse(null); // Should not happen if cache is populated correctly, but handle defensively

            if (minimumRequiredTier == null) {
                log.error("Could not determine minimum required tier for item {} despite being in gear tier cache {}. Assuming restricted.", itemId, relevantGearTierIds);
                return false; // Treat as restricted if we can't determine the minimum requirement
            }

            log.trace("Item {} minimum required SkillTier determined as: {}", itemId, minimumRequiredTier);

            // Check if *any* currently unlocked gear tier is >= the minimum required tier.
            boolean hasSufficientTierUnlocked = currentlyUnlockedIds.stream()
                    .map(this::getUnlockById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(ud -> ud.getCategory() == UnlockType.GEAR_TIER) // Only consider unlocked GEAR_TIERs
                    .map(UnlockData::getRequiredTier)
                    .filter(java.util.Objects::nonNull)
                    .anyMatch(unlockedTier -> unlockedTier.ordinal() >= minimumRequiredTier.ordinal()); // Check if unlocked tier is >= minimum required

            if (hasSufficientTierUnlocked) {
                log.trace("Item {} permitted because a gear tier >= {} is unlocked.", itemId, minimumRequiredTier);
                return true;
            } else {
                log.trace("Item {} restricted because no unlocked gear tier is >= {}.", itemId, minimumRequiredTier);
                return false;
            }
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
        // *** MODIFIED: Handle level-based tiers if needed, though JSON uses specific IDs now ***
        // Example: If using level-based IDs like LEVEL_10, adjust generation here or rely on JSON IDs.
        // For now, assuming JSON uses tier names like APPRENTICE or specific level IDs.
        return "SKILL_" + skill.getName().toUpperCase() + "_" + tier.name();
    }

    /** Helper method to generate Gear Tier Unlock IDs consistently. */
    public static String getGearTierId(GearTier tier) {
        if (tier == null || tier == GearTier.NONE) { log.warn("Attempted to generate Gear Tier ID for null or NONE tier."); return "GEAR_INVALID"; }
        // Assume a prefix like GEAR_MELEE_ for now, needs refinement based on JSON structure
        // *** TODO: Refine based on actual JSON structure (MELEE/RANGED/MAGIC?) ***
        // Example: If JSON uses GEAR_RANGED_GREEN_DHIDE, generation needs to match.
        // For now, assuming JSON uses IDs like GEAR_MELEE_MITHRIL
        return "GEAR_MELEE_" + tier.name();
    }

    // Removed config injection - no longer needed here
}
