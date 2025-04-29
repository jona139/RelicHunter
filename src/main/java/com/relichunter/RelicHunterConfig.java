// Filename: RelicHunter/src/main/java/com/relichunter/RelicHunterConfig.java
// Content:
package com.relichunter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.util.HashSet;
import java.util.Set;

@ConfigGroup(RelicHunterPlugin.CONFIG_GROUP)
public interface RelicHunterConfig extends Config
{
    // --- Sections ---

    @ConfigSection(
            name = "Relic Counts",
            description = "Tracks the number of each Relic type and tier currently held.",
            position = 0,
            closedByDefault = false
    )
    String relicCountsSection = "relicCounts";

    // Renamed section for clarity
    @ConfigSection(
            name = "Progression Tiers",
            description = "Current unlocked tier for skills (level cap) or gear (melee equip).",
            position = 10,
            closedByDefault = true
    )
    String progressionTiersSection = "progressionTiers";


    @ConfigSection(
            name = "Management",
            description = "Actions for managing plugin state.",
            position = 100,
            closedByDefault = true
    )
    String managementSection = "management";


    // --- Tiered Relic Counts --- (Unchanged from previous version)

    // Skilling Relics
    @ConfigItem(keyName = "skillingRelicsApprentice", name = "Skilling (Apprentice)", description = "Number of Apprentice Skilling Relics held.", position = 1, section = relicCountsSection)
    default int skillingRelicsApprentice() { return 0; }
    void setSkillingRelicsApprentice(int count);

    @ConfigItem(keyName = "skillingRelicsJourneyman", name = "Skilling (Journeyman)", description = "Number of Journeyman Skilling Relics held.", position = 2, section = relicCountsSection)
    default int skillingRelicsJourneyman() { return 0; }
    void setSkillingRelicsJourneyman(int count);

    // TODO: Add Expert, Master, Grandmaster counts for Skilling relics

    // Combat Relics
    @ConfigItem(keyName = "combatRelicsApprentice", name = "Combat (Apprentice)", description = "Number of Apprentice Combat Relics held.", position = 5, section = relicCountsSection)
    default int combatRelicsApprentice() { return 0; }
    void setCombatRelicsApprentice(int count);

    @ConfigItem(keyName = "combatRelicsJourneyman", name = "Combat (Journeyman)", description = "Number of Journeyman Combat Relics held.", position = 6, section = relicCountsSection)
    default int combatRelicsJourneyman() { return 0; }
    void setCombatRelicsJourneyman(int count);

    // TODO: Add Expert, Master, Grandmaster counts for Combat relics

    // Exploration Relics
    @ConfigItem(keyName = "explorationRelicsApprentice", name = "Exploration (Apprentice)", description = "Number of Apprentice Exploration Relics held.", position = 9, section = relicCountsSection)
    default int explorationRelicsApprentice() { return 0; }
    void setExplorationRelicsApprentice(int count);

    @ConfigItem(keyName = "explorationRelicsJourneyman", name = "Exploration (Journeyman)", description = "Number of Journeyman Exploration Relics held.", position = 10, section = relicCountsSection)
    default int explorationRelicsJourneyman() { return 0; }
    void setExplorationRelicsJourneyman(int count);

    // TODO: Add Expert, Master, Grandmaster counts for Exploration relics


    // --- Progression Tiers ---

    // Melee Gear Tier (Used for Attack, Strength, Defence equip restrictions)
    @ConfigItem(keyName = "meleeGearTier", name = "Melee Gear Tier", description = "Highest tier of melee equipment allowed (Bronze/Iron/Steel default).", position = 11, section = progressionTiersSection)
    default GearTier meleeGearTier() { return GearTier.BASIC; } // Default to basic gear allowed
    void setMeleeGearTier(GearTier tier);

    // REMOVED: Attack, Strength, Defence Skill Tiers (replaced by meleeGearTier)
	/*
	@ConfigItem(keyName = "attackTier", name = "Attack Tier", description = "Current unlocked tier for Attack.", position = 11, section = progressionTiersSection)
	default SkillTier attackTier() { return SkillTier.APPRENTICE; }
	void setAttackTier(SkillTier tier);

	@ConfigItem(keyName = "strengthTier", name = "Strength Tier", description = "Current unlocked tier for Strength.", position = 12, section = progressionTiersSection)
	default SkillTier strengthTier() { return SkillTier.APPRENTICE; }
	void setStrengthTier(SkillTier tier);

	@ConfigItem(keyName = "defenceTier", name = "Defence Tier", description = "Current unlocked tier for Defence.", position = 13, section = progressionTiersSection)
	default SkillTier defenceTier() { return SkillTier.APPRENTICE; }
	void setDefenceTier(SkillTier tier);
	*/

    // Hitpoints Tier (Still uses level cap via SkillTier)
    @ConfigItem(keyName = "hitpointsTier", name = "Hitpoints Tier", description = "Current unlocked tier for Hitpoints.", position = 14, section = progressionTiersSection)
    default SkillTier hitpointsTier() { return SkillTier.APPRENTICE; } // Still starts unlocked
    void setHitpointsTier(SkillTier tier);

    // Ranged/Magic/Prayer Tiers (Still use level cap via SkillTier, unlocked by Combat relics)
    @ConfigItem(keyName = "rangedTier", name = "Ranged Tier", description = "Current unlocked tier for Ranged.", position = 15, section = progressionTiersSection)
    default SkillTier rangedTier() { return SkillTier.LOCKED; }
    void setRangedTier(SkillTier tier);

    @ConfigItem(keyName = "prayerTier", name = "Prayer Tier", description = "Current unlocked tier for Prayer.", position = 16, section = progressionTiersSection)
    default SkillTier prayerTier() { return SkillTier.LOCKED; }
    void setPrayerTier(SkillTier tier);

    @ConfigItem(keyName = "magicTier", name = "Magic Tier", description = "Current unlocked tier for Magic.", position = 17, section = progressionTiersSection)
    default SkillTier magicTier() { return SkillTier.LOCKED; }
    void setMagicTier(SkillTier tier);

    // Skilling Skill Tiers (Remain unchanged, unlocked by Skilling relics)
    @ConfigItem(keyName = "cookingTier", name = "Cooking Tier", description = "Current unlocked tier for Cooking.", position = 18, section = progressionTiersSection)
    default SkillTier cookingTier() { return SkillTier.LOCKED; }
    void setCookingTier(SkillTier tier);

    @ConfigItem(keyName = "woodcuttingTier", name = "Woodcutting Tier", description = "Current unlocked tier for Woodcutting.", position = 19, section = progressionTiersSection)
    default SkillTier woodcuttingTier() { return SkillTier.LOCKED; }
    void setWoodcuttingTier(SkillTier tier);

    @ConfigItem(keyName = "fletchingTier", name = "Fletching Tier", description = "Current unlocked tier for Fletching.", position = 20, section = progressionTiersSection)
    default SkillTier fletchingTier() { return SkillTier.LOCKED; }
    void setFletchingTier(SkillTier tier);

    @ConfigItem(keyName = "fishingTier", name = "Fishing Tier", description = "Current unlocked tier for Fishing.", position = 21, section = progressionTiersSection)
    default SkillTier fishingTier() { return SkillTier.LOCKED; }
    void setFishingTier(SkillTier tier);

    @ConfigItem(keyName = "firemakingTier", name = "Firemaking Tier", description = "Current unlocked tier for Firemaking.", position = 22, section = progressionTiersSection)
    default SkillTier firemakingTier() { return SkillTier.LOCKED; }
    void setFiremakingTier(SkillTier tier);

    @ConfigItem(keyName = "craftingTier", name = "Crafting Tier", description = "Current unlocked tier for Crafting.", position = 23, section = progressionTiersSection)
    default SkillTier craftingTier() { return SkillTier.LOCKED; }
    void setCraftingTier(SkillTier tier);

    @ConfigItem(keyName = "smithingTier", name = "Smithing Tier", description = "Current unlocked tier for Smithing.", position = 24, section = progressionTiersSection)
    default SkillTier smithingTier() { return SkillTier.APPRENTICE; } // Starting skill
    void setSmithingTier(SkillTier tier);

    @ConfigItem(keyName = "miningTier", name = "Mining Tier", description = "Current unlocked tier for Mining.", position = 25, section = progressionTiersSection)
    default SkillTier miningTier() { return SkillTier.APPRENTICE; } // Starting skill
    void setMiningTier(SkillTier tier);

    @ConfigItem(keyName = "herbloreTier", name = "Herblore Tier", description = "Current unlocked tier for Herblore.", position = 26, section = progressionTiersSection)
    default SkillTier herbloreTier() { return SkillTier.LOCKED; }
    void setHerbloreTier(SkillTier tier);

    @ConfigItem(keyName = "agilityTier", name = "Agility Tier", description = "Current unlocked tier for Agility.", position = 27, section = progressionTiersSection)
    default SkillTier agilityTier() { return SkillTier.LOCKED; }
    void setAgilityTier(SkillTier tier);

    @ConfigItem(keyName = "thievingTier", name = "Thieving Tier", description = "Current unlocked tier for Thieving.", position = 28, section = progressionTiersSection)
    default SkillTier thievingTier() { return SkillTier.LOCKED; }
    void setThievingTier(SkillTier tier);

    @ConfigItem(keyName = "slayerTier", name = "Slayer Tier", description = "Current unlocked tier for Slayer.", position = 29, section = progressionTiersSection)
    default SkillTier slayerTier() { return SkillTier.LOCKED; }
    void setSlayerTier(SkillTier tier);

    @ConfigItem(keyName = "farmingTier", name = "Farming Tier", description = "Current unlocked tier for Farming.", position = 30, section = progressionTiersSection)
    default SkillTier farmingTier() { return SkillTier.LOCKED; }
    void setFarmingTier(SkillTier tier);

    @ConfigItem(keyName = "runecraftTier", name = "Runecraft Tier", description = "Current unlocked tier for Runecraft.", position = 31, section = progressionTiersSection)
    default SkillTier runecraftTier() { return SkillTier.LOCKED; }
    void setRunecraftTier(SkillTier tier);

    @ConfigItem(keyName = "hunterTier", name = "Hunter Tier", description = "Current unlocked tier for Hunter.", position = 32, section = progressionTiersSection)
    default SkillTier hunterTier() { return SkillTier.LOCKED; }
    void setHunterTier(SkillTier tier);

    @ConfigItem(keyName = "constructionTier", name = "Construction Tier", description = "Current unlocked tier for Construction.", position = 33, section = progressionTiersSection)
    default SkillTier constructionTier() { return SkillTier.LOCKED; }
    void setConstructionTier(SkillTier tier);


    // --- Testing & Management ---

    @ConfigItem(keyName = "resetProgressionButton", name = "Reset Progression", description = "Resets all unlocked relics and relic counts to zero. Requires confirmation.", position = 101, section = managementSection, warning = "This will completely reset your Relic Hunter progress tracked by this plugin!")
    default boolean resetProgressionButton() { return false; }


    // --- Internal State ---

    @ConfigItem(keyName = "unlockedRelics", name = "", description = "Stores the set of unique IDs for unlocked content.", hidden = true)
    default Set<String> unlockedRelics() { return new HashSet<>(); }
    void setUnlockedRelics(Set<String> unlockedIds);
}