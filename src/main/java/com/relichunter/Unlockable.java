// Filename: RelicHunter/src/main/java/com/relichunter/Unlockable.java
// Content:
package com.relichunter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList; // Keep import

/**
 * Represents a single piece of content that can be unlocked via a Relic.
 * Includes the tier associated with this unlock.
 */
public final class Unlockable {

    private final String id;
    private final String name;
    private final String description;
    private final RelicType type;
    private final SkillTier targetTier; // <<< ADDED: The tier this unlock belongs to/requires
    private final List<String> prerequisiteIds;

    /**
     * Full constructor for Unlockable.
     */
    public Unlockable(String id, String name, String description, RelicType type, SkillTier targetTier, List<String> prerequisiteIds) {
        this.id = Objects.requireNonNull(id, "Unlockable ID cannot be null");
        this.name = Objects.requireNonNull(name, "Unlockable name cannot be null");
        this.description = Objects.requireNonNull(description, "Unlockable description cannot be null");
        this.type = Objects.requireNonNull(type, "Unlockable type cannot be null");
        this.targetTier = Objects.requireNonNull(targetTier, "Unlockable targetTier cannot be null"); // <<< ADDED null check
        this.prerequisiteIds = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(prerequisiteIds, "Unlockable prerequisiteIds list cannot be null")
        ));
    }

    /**
     * Convenience constructor for unlocks with no prerequisites.
     */
    public Unlockable(String id, String name, String description, RelicType type, SkillTier targetTier) {
        this(id, name, description, type, targetTier, Collections.emptyList());
    }

    // --- Getter methods ---

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public RelicType getType() { return type; }
    public SkillTier getTargetTier() { return targetTier; } // <<< ADDED getter
    public List<String> getPrerequisiteIds() { return prerequisiteIds; }

    // --- equals, hashCode, toString --- (Need to add targetTier)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Unlockable that = (Unlockable) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                type == that.type &&
                targetTier == that.targetTier && // <<< ADDED check
                Objects.equals(prerequisiteIds, that.prerequisiteIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, type, targetTier, prerequisiteIds); // <<< ADDED field
    }

    @Override
    public String toString() {
        return "Unlockable{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", targetTier=" + targetTier + // <<< ADDED field
                ", prerequisiteIds=" + prerequisiteIds +
                '}';
    }
}