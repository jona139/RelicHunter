// Filename: RelicHunter/src/main/java/com/relichunter/data/TierData.java
// Content:
package com.relichunter.data;

import java.util.Set;
import lombok.Data;

/**
 * Represents the data for a single gear tier (weapons and armour lists).
 */
@Data
public class TierData {
    private Set<Integer> weapons;
    private Set<Integer> armour;
}