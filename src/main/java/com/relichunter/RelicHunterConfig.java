// Filename: RelicHunter/src/main/java/com/relichunter/RelicHunterConfig.java
// Content:
package com.relichunter;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

@ConfigGroup(RelicHunterPlugin.CONFIG_GROUP)
public interface RelicHunterConfig extends Config
{
    // --- Sections ---

    @ConfigSection(
            name = "General",
            description = "General plugin settings.",
            position = -2
    )
    String generalSection = "general";

    @ConfigSection(
            name = "Relic Counts",
            description = "Tracks the number of each Relic type and tier currently held.",
            position = 0,
            closedByDefault = false
    )
    String relicCountsSection = "relicCounts";

    @ConfigSection(
            name = "Progression Tiers",
            description = "Current unlocked tier for skills (level cap) or gear (melee equip).",
            position = 10,
            closedByDefault = true
    )
    String progressionTiersSection = "progressionTiers";

    @ConfigSection(
            name = "Relic Acquisition",
            description = "Configure chances and parameters for obtaining relics.",
            position = 20,
            closedByDefault = true
    )
    String acquisitionSection = "acquisition";

    @ConfigSection(
            name = "Visual Overlays",
            description = "Settings for visual indicators and overlays.",
            position = 50,
            closedByDefault = false
    )
    String visualOverlaysSection = "visualOverlays";

    @ConfigSection(
            name = "Visual Effects",
            description = "Configure extra visual effects for unlocks.",
            position = 60, // Position after Overlays
            closedByDefault = false
    )
    String visualEffectsSection = "visualEffects";

    @ConfigSection(
            name = "Warnings & Blocking",
            description = "Configure warnings for restricted actions and experimental blocking.",
            position = 75,
            closedByDefault = false
    )
    String warningsSection = "warnings";


    @ConfigSection(
            name = "Management",
            description = "Actions for managing plugin state.",
            position = 100,
            closedByDefault = true
    )
    String managementSection = "management";


    // --- General Settings ---
    @ConfigItem(
            keyName = "useF2PDatabase",
            name = "Use F2P Unlock Database",
            description = "Check this box to use the F2P unlock database. Uncheck for the default (P2P) database. Requires plugin restart or reload.",
            position = -1,
            section = generalSection
    )
    default boolean useF2PDatabase() { return false; }


    // --- Tiered Relic Counts ---
    // (Keep existing relic count items)
    @ConfigItem(keyName = "skillingRelicsApprentice", name = "Skilling (Apprentice)", description = "Number of Apprentice Skilling Relics held.", position = 1, section = relicCountsSection)
    default int skillingRelicsApprentice() { return 0; }
    void setSkillingRelicsApprentice(int count);

    @ConfigItem(keyName = "skillingRelicsJourneyman", name = "Skilling (Journeyman)", description = "Number of Journeyman Skilling Relics held.", position = 2, section = relicCountsSection)
    default int skillingRelicsJourneyman() { return 0; }
    void setSkillingRelicsJourneyman(int count);

    @ConfigItem(keyName = "skillingRelicsExpert", name = "Skilling (Expert)", description = "Number of Expert Skilling Relics held.", position = 3, section = relicCountsSection)
    default int skillingRelicsExpert() { return 0; }
    void setSkillingRelicsExpert(int count);

    @ConfigItem(keyName = "skillingRelicsMaster", name = "Skilling (Master)", description = "Number of Master Skilling Relics held.", position = 4, section = relicCountsSection)
    default int skillingRelicsMaster() { return 0; }
    void setSkillingRelicsMaster(int count);

    @ConfigItem(keyName = "skillingRelicsGrandmaster", name = "Skilling (Grandmaster)", description = "Number of Grandmaster Skilling Relics held.", position = 5, section = relicCountsSection)
    default int skillingRelicsGrandmaster() { return 0; }
    void setSkillingRelicsGrandmaster(int count);


    @ConfigItem(keyName = "combatRelicsApprentice", name = "Combat (Apprentice)", description = "Number of Apprentice Combat Relics held.", position = 6, section = relicCountsSection)
    default int combatRelicsApprentice() { return 0; }
    void setCombatRelicsApprentice(int count);

    @ConfigItem(keyName = "combatRelicsJourneyman", name = "Combat (Journeyman)", description = "Number of Journeyman Combat Relics held.", position = 7, section = relicCountsSection)
    default int combatRelicsJourneyman() { return 0; }
    void setCombatRelicsJourneyman(int count);

    @ConfigItem(keyName = "combatRelicsExpert", name = "Combat (Expert)", description = "Number of Expert Combat Relics held.", position = 8, section = relicCountsSection)
    default int combatRelicsExpert() { return 0; }
    void setCombatRelicsExpert(int count);

    @ConfigItem(keyName = "combatRelicsMaster", name = "Combat (Master)", description = "Number of Master Combat Relics held.", position = 9, section = relicCountsSection)
    default int combatRelicsMaster() { return 0; }
    void setCombatRelicsMaster(int count);

    @ConfigItem(keyName = "combatRelicsGrandmaster", name = "Combat (Grandmaster)", description = "Number of Grandmaster Combat Relics held.", position = 10, section = relicCountsSection)
    default int combatRelicsGrandmaster() { return 0; }
    void setCombatRelicsGrandmaster(int count);


    @ConfigItem(keyName = "explorationRelicsApprentice", name = "Exploration (Apprentice)", description = "Number of Apprentice Exploration Relics held.", position = 11, section = relicCountsSection)
    default int explorationRelicsApprentice() { return 0; }
    void setExplorationRelicsApprentice(int count);

    @ConfigItem(keyName = "explorationRelicsJourneyman", name = "Exploration (Journeyman)", description = "Number of Journeyman Exploration Relics held.", position = 12, section = relicCountsSection)
    default int explorationRelicsJourneyman() { return 0; }
    void setExplorationRelicsJourneyman(int count);

    @ConfigItem(keyName = "explorationRelicsExpert", name = "Exploration (Expert)", description = "Number of Expert Exploration Relics held.", position = 13, section = relicCountsSection)
    default int explorationRelicsExpert() { return 0; }
    void setExplorationRelicsExpert(int count);

    @ConfigItem(keyName = "explorationRelicsMaster", name = "Exploration (Master)", description = "Number of Master Exploration Relics held.", position = 14, section = relicCountsSection)
    default int explorationRelicsMaster() { return 0; }
    void setExplorationRelicsMaster(int count);

    @ConfigItem(keyName = "explorationRelicsGrandmaster", name = "Exploration (Grandmaster)", description = "Number of Grandmaster Exploration Relics held.", position = 15, section = relicCountsSection)
    default int explorationRelicsGrandmaster() { return 0; }
    void setExplorationRelicsGrandmaster(int count);


    // --- Progression Tiers ---
    // (Keep existing progression tier items)
    @ConfigItem(keyName = "meleeGearTier", name = "Melee Gear Tier", description = "Highest tier of melee equipment allowed.", position = 11, section = progressionTiersSection)
    default GearTier meleeGearTier() { return GearTier.STEEL; }
    void setMeleeGearTier(GearTier tier);

    @ConfigItem(keyName = "rangedTier", name = "Ranged Tier", description = "Current unlocked tier for Ranged.", position = 15, section = progressionTiersSection)
    default SkillTier rangedTier() { return SkillTier.LOCKED; }
    void setRangedTier(SkillTier tier);

    @ConfigItem(keyName = "prayerTier", name = "Prayer Tier", description = "Current unlocked tier for Prayer.", position = 16, section = progressionTiersSection)
    default SkillTier prayerTier() { return SkillTier.LOCKED; }
    void setPrayerTier(SkillTier tier);

    @ConfigItem(keyName = "magicTier", name = "Magic Tier", description = "Current unlocked tier for Magic.", position = 17, section = progressionTiersSection)
    default SkillTier magicTier() { return SkillTier.LOCKED; }
    void setMagicTier(SkillTier tier);

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
    default SkillTier smithingTier() { return SkillTier.APPRENTICE; }
    void setSmithingTier(SkillTier tier);

    @ConfigItem(keyName = "miningTier", name = "Mining Tier", description = "Current unlocked tier for Mining.", position = 25, section = progressionTiersSection)
    default SkillTier miningTier() { return SkillTier.APPRENTICE; }
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


    // --- Relic Acquisition Settings ---
    @ConfigItem(
            keyName = "enableScalingDropRate",
            name = "Enable Scaling Drop Rate",
            description = "Scale relic drop rate based on the number of available unlocks for the tier. Higher available count = better chance.",
            position = 20, // Position at the top of the section
            section = acquisitionSection
    )
    default boolean enableScalingDropRate() { return true; } // Default to true as requested

    @Range(min = 1, max = 10000)
    @ConfigItem(keyName = "skillingRelicBaseChance", name = "Skilling Base Chance (1 in X)", description = "Base denominator for skilling relic chance. If scaling is enabled, this is divided by the available unlock count.", position = 21, section = acquisitionSection)
    default int skillingRelicBaseChance() { return 500; }

    @ConfigItem(keyName = "skillingRelicXpThresholdApp", name = "Skilling XP Thresh. (Apprentice)", description = "Max XP drop to allow Apprentice relics.", position = 22, section = acquisitionSection)
    default int skillingRelicXpThresholdApp() { return 10; }

    @ConfigItem(keyName = "skillingRelicXpThresholdJour", name = "Skilling XP Thresh. (Journeyman)", description = "Max XP drop to allow Journeyman relics.", position = 23, section = acquisitionSection)
    default int skillingRelicXpThresholdJour() { return 30; }

    @ConfigItem(keyName = "skillingRelicXpThresholdExp", name = "Skilling XP Thresh. (Expert)", description = "Max XP drop to allow Expert relics.", position = 24, section = acquisitionSection)
    default int skillingRelicXpThresholdExp() { return 70; }

    @ConfigItem(keyName = "skillingRelicXpThresholdMas", name = "Skilling XP Thresh. (Master)", description = "Max XP drop to allow Master relics.", position = 25, section = acquisitionSection)
    default int skillingRelicXpThresholdMas() { return 150; }

    @Range(min = 1, max = 10000)
    @ConfigItem(keyName = "combatRelicBaseChance", name = "Combat Base Chance (1 in X)", description = "Base denominator for combat relic chance. If scaling is enabled, this is divided by the available unlock count.", position = 31, section = acquisitionSection)
    default int combatRelicBaseChance() { return 200; }

    @ConfigItem(keyName = "combatRelicNpcLevelApp", name = "Combat NPC Lvl (Apprentice)", description = "Max NPC combat level to allow Apprentice relics.", position = 32, section = acquisitionSection)
    default int combatRelicNpcLevelApp() { return 20; }

    @ConfigItem(keyName = "combatRelicNpcLevelJour", name = "Combat NPC Lvl (Journeyman)", description = "Max NPC combat level to allow Journeyman relics.", position = 33, section = acquisitionSection)
    default int combatRelicNpcLevelJour() { return 50; }

    @ConfigItem(keyName = "combatRelicNpcLevelExp", name = "Combat NPC Lvl (Expert)", description = "Max NPC combat level to allow Expert relics.", position = 34, section = acquisitionSection)
    default int combatRelicNpcLevelExp() { return 90; }

    @ConfigItem(keyName = "combatRelicNpcLevelMas", name = "Combat NPC Lvl (Master)", description = "Max NPC combat level to allow Master relics.", position = 35, section = acquisitionSection)
    default int combatRelicNpcLevelMas() { return 150; }

    // *** REMOVED OLD EXPLORATION PERCENTAGE CHANCES ***
    // @Range(min = 0, max = 100)
    // @ConfigItem(keyName = "explorationRelicChanceEasy", name = "Exploration Chance (Easy %)", description = "Percent chance to get an Apprentice Exploration relic from an Easy casket.", position = 41, section = acquisitionSection)
    // default int explorationRelicChanceEasy() { return 33; }
    // ... (removed others) ...

    // *** ADDED NEW EXPLORATION BASE CHANCE ***
    @Range(min = 1, max = 10000)
    @ConfigItem(keyName = "explorationRelicBaseChance", name = "Exploration Base Chance (1 in X)", description = "Base denominator for exploration relic chance (from clues). If scaling is enabled, this is divided by the available unlock count for the clue's tier.", position = 41, section = acquisitionSection)
    default int explorationRelicBaseChance() { return 10; } // Default to a relatively high chance


    // --- Visual Overlays ---
    @ConfigItem( keyName = "showItemRestrictions", name = "Highlight Restricted Items", description = "Enable to visually tint items you cannot currently use.", position = 51, section = visualOverlaysSection )
    default boolean showItemRestrictions() { return true; }
    void setShowItemRestrictions(boolean show);

    @Alpha
    @ConfigItem( keyName = "itemRestrictionColor", name = "Restriction Tint Color", description = "Color used to tint restricted items.", position = 52, section = visualOverlaysSection )
    default Color itemRestrictionColor() { return new Color(255, 0, 0, 100); }
    void setItemRestrictionColor(Color color);

    @ConfigItem( keyName = "showSkillTierVisuals", name = "Show Skill Tier Visuals", description = "Enable to show tier status (Locked tint, level cap) on the Skills tab.", position = 53, section = visualOverlaysSection )
    default boolean showSkillTierVisuals() { return true; }

    @ConfigItem(
            keyName = "questRestrictionMode",
            name = "Quest Log Restriction",
            description = "How to display quests in the quest log that are not yet unlocked by the plugin.",
            position = 54, // Place after skill tier visuals
            section = visualOverlaysSection
    )
    default QuestRestrictionMode questRestrictionMode() { return QuestRestrictionMode.DIM; } // Default to dimming


    // --- Visual Effects ---
    @ConfigItem(
            keyName = "playUnlockEmote",
            name = "Play Unlock Emote",
            description = "Play an animation when confirming a relic unlock.",
            position = 61,
            section = visualEffectsSection
    )
    default boolean playUnlockEmote() { return true; }

    @ConfigItem(
            keyName = "showUnlockGraphic",
            name = "Show Unlock Graphic",
            description = "Show a graphic effect on the player when confirming a relic unlock.",
            position = 62,
            section = visualEffectsSection
    )
    default boolean showUnlockGraphic() { return true; }


    // --- Warnings & Blocking ---
    @ConfigItem(keyName = "warnOnRestrictedXpGain", name = "Warn on Restricted XP Gain", description = "Show a chat warning if you gain XP in a skill that is locked or above its tier cap.", position = 76, section = warningsSection)
    default boolean warnOnRestrictedXpGain() { return true; }

    @ConfigItem(keyName = "warnOnRestrictedAreaEntry", name = "Warn on Restricted Area Entry", description = "Show a chat warning if you enter a geographical area that is currently locked.", position = 77, section = warningsSection)
    default boolean warnOnRestrictedAreaEntry() { return true; }

    @ConfigItem(keyName = "warnOnRestrictedItemUse", name = "Warn on Restricted Item Use", description = "Show a chat warning if you attempt to wield or wear a restricted item.", position = 78, section = warningsSection)
    default boolean warnOnRestrictedItemUse() { return true; }

    @ConfigItem(keyName = "attemptBlockRestrictedItemUse", name = "Attempt to Block Restricted Item Use (Experimental)", description = "Tries to prevent wielding/wearing restricted items. This is experimental, may not work reliably, and could break with game updates.", position = 79, section = warningsSection, warning = "Experimental: This feature attempts to block actions client-side and might be unreliable or interfere with gameplay.")
    default boolean attemptBlockRestrictedItemUse() { return false; }


    // --- Testing & Management ---
    @ConfigItem(keyName = "resetProgressionButton", name = "Reset Progression", description = "Resets all unlocked relics and relic counts to zero. Requires confirmation.", position = 101, section = managementSection, warning = "This will completely reset your Relic Hunter progress tracked by this plugin!")
    default boolean resetProgressionButton() { return false; }


    // --- Internal State ---
    @ConfigItem(keyName = "unlockedRelics", name = "", description = "Stores the set of unique IDs for unlocked content.", hidden = true)
    default Set<String> unlockedRelics() { return new HashSet<>(); }
    void setUnlockedRelics(Set<String> unlockedIds);
}