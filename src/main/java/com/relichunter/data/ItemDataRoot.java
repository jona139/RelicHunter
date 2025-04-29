// Filename: RelicHunter/src/main/java/com/relichunter/data/ItemDataRoot.java
// Content:
package com.relichunter.data;

import java.util.Map;
import lombok.Data; // Using Lombok @Data for boilerplate code (getters, setters, etc.)

/**
 * Represents the root structure of the item data JSON file.
 */
@Data // Lombok annotation to generate getters, setters, equals, hashCode, toString
public class ItemDataRoot {
    // Key is the GearTier enum name as a String (e.g., "MITHRIL")
    private Map<String, TierData> gearTiers;
    // TODO: Add other categories later (e.g., rangedGearTiers)
}