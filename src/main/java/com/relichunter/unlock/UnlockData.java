// Filename: RelicHunter/src/main/java/com/relichunter/unlock/UnlockData.java
// NEW FILE: Represents a single entry in the unlock database
package com.relichunter.unlock;

import com.relichunter.RelicType;
import com.relichunter.SkillTier;
import lombok.Data;
import java.util.List;
import java.util.Set;
import java.util.Collections;

@Data // Lombok for getters, setters, equals, hashCode, toString
public class UnlockData {
    private String id; // Unique identifier (e.g., "SKILL_AGILITY_EXPERT", "GEAR_MELEE_MITHRIL", "AREA_KARAMJA")
    private String name; // User-friendly display name
    private String description; // Short description for UI
    private UnlockType category = UnlockType.OTHER; // Broad category (default to OTHER)
    private RelicType relicType; // Type of relic required (SKILLING, COMBAT, EXPLORATION)
    private SkillTier requiredTier; // Tier of the relic needed to roll this unlock
    private List<String> prerequisites = Collections.emptyList(); // List of other UnlockData IDs required first

    // Optional details based on category
    private Set<Integer> itemIds; // For GEAR_TIER or SPECIFIC_ITEM unlocks
    private String areaDefinition; // For AREA unlocks (e.g., region IDs, chunk coords - format TBD)
    private Integer questId; // For QUEST unlocks (using OSRS Quest ID)
    // Add other fields as needed for different categories (e.g., skill name for SKILL_TIER)
}

