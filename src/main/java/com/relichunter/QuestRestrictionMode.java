// Filename: RelicHunter/src/main/java/com/relichunter/QuestRestrictionMode.java
// NEW FILE
package com.relichunter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestRestrictionMode {
    NONE("None", "Show all quests normally."),
    DIM("Dim", "Dim quests that are not unlocked."),
    HIDE("Hide (Experimental)", "Attempt to hide quests that are not unlocked (may cause layout issues).");

    private final String name;
    private final String description;

    @Override
    public String toString() {
        return name;
    }
}
