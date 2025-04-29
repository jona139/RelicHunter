// Filename: RelicHunter/src/main/java/com/relichunter/unlock/UnlockType.java
// NEW FILE: Defines categories for unlocks
package com.relichunter.unlock;

public enum UnlockType {
    SKILL_TIER,
    GEAR_TIER, // Represents unlocking *up to* a tier (e.g., Mithril, Rune)
    SPECIFIC_ITEM, // Represents unlocking a single specific item (e.g., Dragon Scimitar)
    AREA,
    QUEST,
    MECHANIC, // e.g., Fairy Rings, Spirit Trees, POH features
    SKILLING_METHOD, // e.g., Specific tree, ore vein, fishing spot
    BOSS, // Access to a specific boss instance/area
    OTHER;
}
