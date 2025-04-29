// Filename: RelicHunter/src/main/java/com/relichunter/RelicHunterPlugin.java
// Content:
package com.relichunter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.relichunter.unlock.UnlockData;
import com.relichunter.unlock.UnlockDatabaseRoot;
import com.relichunter.unlock.UnlockType;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
// ParamComposition import removed previously


@Slf4j
@PluginDescriptor(
        name = "Relic Hunter Helper",
        description = "Helps track progress and restrictions for the Relic Hunter Ironman mode.",
        tags = {"ironman", "relic", "hunter", "helper", "restriction", "unique"}
)
public class RelicHunterPlugin extends Plugin
{
    static final String CONFIG_GROUP = "relichunter";
    private static final String UNLOCK_DATABASE_JSON_PATH = "/relic_hunter_unlock_database.json";
    private static final String PLUGIN_CHAT_NAME = "Relic Hunter";

    // Injected Dependencies
    @Inject private Client client;
    @Inject private RelicHunterConfig config;
    @Inject private ClientToolbar clientToolbar;
    @Inject private ConfigManager configManager;
    @Inject private Gson gson;
    @Inject private OverlayManager overlayManager;
    @Inject private ItemRestrictionOverlay itemRestrictionOverlay;
    @Inject private SkillTabOverlay skillTabOverlay;
    @Inject private ClientThread clientThread;
    @Inject private ItemManager itemManager;
    @Inject private UnlockManager unlockManager; // Inject UnlockManager

    // Plugin State
    private RelicHunterPanel panel;
    private NavigationButton navButton;
    private final Random random = new Random();
    private boolean initialPanelUpdateDone = false;
    private int lastPlayerRegionId = -1;
    private final Map<Skill, Integer> previousSkillXp = new EnumMap<>(Skill.class);
    private final Map<Integer, Integer> recentlyProcessedNpcs = new HashMap<>();
    private static final int NPC_PROCESS_COOLDOWN_TICKS = 5;
    private boolean loginTick = false;

    private static final Pattern CLUE_SCROLL_COMPLETION_PATTERN = Pattern.compile("You have completed (.+) Treasure Trail!");


    @Override
    protected void startUp() throws Exception {
        log.info("Relic Hunter Helper starting up...");
        loadUnlockDatabase();

        // *** FIXED: Pass unlockManager to panel constructor ***
        panel = new RelicHunterPanel(this, config, unlockManager);

        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/clue_scroll.png");
        navButton = NavigationButton.builder()
                .tooltip("Relic Hunter Helper")
                .icon(icon)
                .priority(7)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navButton);
        overlayManager.add(itemRestrictionOverlay);
        overlayManager.add(skillTabOverlay);
        initialPanelUpdateDone = false;
        lastPlayerRegionId = -1;
        previousSkillXp.clear();
        recentlyProcessedNpcs.clear();
        loginTick = true;
        if (client.getGameState() == GameState.LOGGED_IN) {
            initializePreviousXpMap();
        }
        log.info("Relic Hunter Helper startup complete.");
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Relic Hunter Helper stopped!");
        overlayManager.remove(itemRestrictionOverlay);
        overlayManager.remove(skillTabOverlay);
        clientToolbar.removeNavigation(navButton);
        if (panel != null) { panel.shutdown(); }
        panel = null;
        navButton = null;
        lastPlayerRegionId = -1;
        previousSkillXp.clear();
        recentlyProcessedNpcs.clear();
        unlockManager.clearDatabase();
    }

    private void initializePreviousXpMap() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        previousSkillXp.clear();
        for (Skill skill : Skill.values()) {
            if (skill == Skill.OVERALL) continue;
            previousSkillXp.put(skill, client.getSkillExperience(skill));
        }
        log.debug("Initialized previous XP map.");
        loginTick = false;
    }

    private void loadUnlockDatabase() {
        log.info("Loading comprehensive unlock database from JSON...");
        try (InputStream is = getClass().getResourceAsStream(UNLOCK_DATABASE_JSON_PATH)) {
            if (is == null) {
                log.error("Could not find unlock database JSON file at path: {}", UNLOCK_DATABASE_JSON_PATH);
                sendChatMessage("Error: Failed to load essential unlock data. Plugin may not function correctly.", ChatMessageType.CONSOLE);
                unlockManager.loadData(Collections.emptyList());
                return;
            }
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            Type dataType = new TypeToken<UnlockDatabaseRoot>(){}.getType();
            UnlockDatabaseRoot parsedData = gson.fromJson(reader, dataType);

            if (parsedData != null && parsedData.getUnlocks() != null) {
                unlockManager.loadData(parsedData.getUnlocks());
                log.info("Successfully loaded {} unlock definitions.", parsedData.getUnlocks().size());
            } else {
                log.error("Parsed unlock database or unlocks list was null.");
                sendChatMessage("Error: Failed to parse unlock data. Plugin may not function correctly.", ChatMessageType.CONSOLE);
                unlockManager.loadData(Collections.emptyList());
            }
        } catch (Exception e) {
            log.error("Error loading or parsing unlock database JSON", e);
            sendChatMessage("Error: Exception loading unlock data. Plugin may not function correctly.", ChatMessageType.CONSOLE);
            unlockManager.loadData(Collections.emptyList());
        }
    }


    // --- Relic Activation Logic ---
    public void initiateActivationSequence() {
        log.debug("Initiating activation sequence...");
        Map<RelicType, Map<SkillTier, Integer>> availableRelics = getAvailableRelicCounts();
        List<Map.Entry<RelicType, SkillTier>> usableOptions = new ArrayList<>();
        availableRelics.forEach((type, tierMap) -> tierMap.forEach((tier, count) -> {
            if (count > 0) usableOptions.add(Map.entry(type, tier));
        }));

        if (usableOptions.isEmpty()) {
            if (panel != null) panel.displayError("You do not have any relics to activate!");
            return;
        }

        if (usableOptions.size() == 1) {
            Map.Entry<RelicType, SkillTier> onlyOption = usableOptions.get(0);
            startRelicActivation(onlyOption.getKey(), onlyOption.getValue());
        } else {
            promptForRelicToUse(usableOptions);
        }
    }

    private void promptForRelicToUse(List<Map.Entry<RelicType, SkillTier>> options) {
        String[] displayOptions = options.stream()
                .map(entry -> formatRelicOption(entry.getKey(), entry.getValue()))
                .sorted().toArray(String[]::new);

        SwingUtilities.invokeLater(() -> {
            String selectedOption = (String) JOptionPane.showInputDialog(
                    null, "Which relic do you want to activate?", "Choose Relic to Activate",
                    JOptionPane.QUESTION_MESSAGE, null, displayOptions, displayOptions[0]);

            if (selectedOption != null) {
                options.stream()
                        .filter(entry -> formatRelicOption(entry.getKey(), entry.getValue()).equals(selectedOption))
                        .findFirst()
                        .ifPresentOrElse(
                                chosenEntry -> startRelicActivation(chosenEntry.getKey(), chosenEntry.getValue()),
                                () -> log.error("Could not match selected option '{}'", selectedOption)
                        );
            } else {
                clearChoiceDisplay();
            }
        });
    }

    private String formatRelicOption(RelicType type, SkillTier tier) {
        String typeName = type.name().toLowerCase();
        String formattedTypeName = typeName.substring(0, 1).toUpperCase() + typeName.substring(1);
        return formattedTypeName + " (" + tier.getDisplayName() + ")";
    }

    private void startRelicActivation(RelicType type, SkillTier tier) {
        log.info("Starting activation for: Type={}, Tier={}", type, tier);
        int relicCount = getRelicCount(type, tier);
        if (relicCount <= 0) { if (panel != null) panel.displayError("Internal Error: Relic count is zero"); return; }

        List<UnlockData> potentialUnlocks = unlockManager.getPotentialUnlocks(type, tier);
        if (potentialUnlocks.isEmpty()) {
            log.warn("No unlocks defined in database for Relic Type {} and Tier {}", type, tier);
            if (panel != null) panel.displayError("Internal error: No unlocks defined for this relic type/tier.");
            return;
        }

        Set<String> currentlyUnlockedIds = config.unlockedRelics();
        List<UnlockData> validUnlocks = potentialUnlocks.stream()
                .filter(unlock -> !currentlyUnlockedIds.contains(unlock.getId()))
                .filter(unlock -> checkPrerequisites(unlock, currentlyUnlockedIds))
                .collect(Collectors.toList());

        log.info("Found {} valid unlocks after filtering.", validUnlocks.size());
        if (validUnlocks.isEmpty()) {
            if (panel != null) panel.displayError("No new unlocks available for this relic type/tier (already unlocked or prerequisites not met).");
            return;
        }

        Collections.shuffle(validUnlocks, random);
        List<UnlockData> choices = validUnlocks.stream().limit(3).collect(Collectors.toList());

        List<Unlockable> displayChoices = choices.stream()
                .map(ud -> new Unlockable(ud.getId(), ud.getName(), ud.getDescription(), ud.getRelicType(), ud.getRequiredTier(), ud.getPrerequisites()))
                .collect(Collectors.toList());

        if (panel != null) panel.displayChoices(displayChoices);
    }

    private boolean checkPrerequisites(UnlockData unlock, Set<String> currentlyUnlockedIds) {
        return unlockManager.arePrerequisitesMet(unlock.getId(), currentlyUnlockedIds);
    }

    public void completeRelicActivation(Unlockable chosenPanelUnlock) {
        UnlockData chosenUnlockData = unlockManager.getUnlockById(chosenPanelUnlock.getId()).orElse(null);

        if (chosenUnlockData == null) {
            log.error("Could not find UnlockData for chosen panel unlock ID: {}", chosenPanelUnlock.getId());
            if (panel != null) panel.displayError("Internal Error: Could not process selection.");
            return;
        }

        log.info("Completing activation for: {}", chosenUnlockData.getName());
        RelicType consumedRelicType = chosenUnlockData.getRelicType();
        SkillTier consumedRelicTier = chosenUnlockData.getRequiredTier();

        if (getRelicCount(consumedRelicType, consumedRelicTier) <= 0) {
            log.error("Relic count mismatch! Trying to consume {} {} but count is zero.", consumedRelicType, consumedRelicTier);
            if (panel != null) panel.displayError("Internal Error: Relic count mismatch");
            return;
        }

        decrementRelicCount(consumedRelicType, consumedRelicTier);

        Set<String> currentUnlocked = config.unlockedRelics();
        Set<String> newUnlockedSet = new HashSet<>(currentUnlocked);

        updateProgressionState(chosenUnlockData);

        newUnlockedSet.add(chosenUnlockData.getId());
        configManager.setConfiguration(CONFIG_GROUP, "unlockedRelics", newUnlockedSet);

        String message = "Relic consumed. Unlocked: " + chosenUnlockData.getName() + "!";
        sendChatMessage(message, ChatMessageType.GAMEMESSAGE);
    }

    private void updateProgressionState(UnlockData unlockedData) {
        log.debug("Updating progression state for unlock: {} ({})", unlockedData.getName(), unlockedData.getId());
        switch (unlockedData.getCategory()) {
            case SKILL_TIER:
                String[] parts = unlockedData.getId().split("_");
                if (parts.length == 3 && parts[0].equals("SKILL")) {
                    try {
                        Skill skill = Skill.valueOf(parts[1]);
                        SkillTier tier = SkillTier.valueOf(parts[2]);
                        setSkillTier(skill, tier);
                        log.info("Set skill {} to tier {}", skill, tier);
                    } catch (IllegalArgumentException e) {
                        log.error("Could not parse skill/tier from ID: {}", unlockedData.getId(), e);
                    }
                } else {
                    log.error("Could not parse SKILL_TIER ID format: {}", unlockedData.getId());
                }
                break;
            case GEAR_TIER:
                String[] gearParts = unlockedData.getId().split("_");
                if (gearParts.length == 3 && gearParts[0].equals("GEAR")) {
                    String gearType = gearParts[1];
                    try {
                        GearTier gearTier = GearTier.valueOf(gearParts[2]);
                        if ("MELEE".equalsIgnoreCase(gearType)) {
                            setMeleeGearTier(gearTier);
                            log.info("Set MELEE gear tier to {}", gearTier);
                        } else {
                            log.warn("Unhandled gear type in GEAR_TIER unlock: {}", gearType);
                        }
                    } catch (IllegalArgumentException e) {
                        log.error("Could not parse gear tier from ID: {}", unlockedData.getId(), e);
                    }
                } else {
                    log.error("Could not parse GEAR_TIER ID format: {}", unlockedData.getId());
                }
                break;
            case SPECIFIC_ITEM:
                log.info("Unlocked specific item: {} (ID: {}). (Handled by isItemAllowed)", unlockedData.getName(), unlockedData.getId());
                break;
            case AREA:
                log.info("Unlocked area: {} (ID: {}). (Area restriction logic TBD)", unlockedData.getName(), unlockedData.getId());
                break;
            case QUEST:
                log.info("Unlocked quest: {} (ID: {}). (Quest tracking logic TBD)", unlockedData.getName(), unlockedData.getId());
                break;
            case MECHANIC:
                log.info("Unlocked mechanic: {} (ID: {}). (Mechanic restriction logic TBD)", unlockedData.getName(), unlockedData.getId());
                break;
            default:
                log.warn("No specific progression state update logic implemented for category: {}", unlockedData.getCategory());
                break;
        }
        if (panel != null) {
            clientThread.invokeLater(panel::updateProgressionTiersDisplay);
        }
    }


    // --- Restriction Checking ---

    /**
     * Checks if an item is allowed based on specific item unlocks, gear tier unlocks,
     * and skill requirements.
     *
     * @param itemId The ID of the item to check.
     * @return true if the item is allowed, false otherwise.
     */
    public boolean isItemAllowed(int itemId) {
        Set<String> unlockedIds = config.unlockedRelics();

        // 1. Check specific/gear tier unlocks via UnlockManager
        if (!unlockManager.isItemPermittedByUnlocks(itemId, unlockedIds)) {
            log.trace("Item {} denied by specific/gear tier unlocks.", itemId);
            return false; // Denied by specific lock or unlocked gear tier
        }

        // 2. Check Skill Requirements (Temporarily Removed due to API issues)
        // ItemComposition comp = itemManager.getItemComposition(itemId);
        // Iterate through ParamIDs... check reqLevel against getSkillTier().getLevelCap()
        // ... (Code removed temporarily) ...
        log.trace("Skill requirement check temporarily bypassed for item {}.", itemId);


        // If not denied by unlocks (and skill requirements bypassed), permit it
        log.trace("Item {} permitted (skill req check bypassed).", itemId);
        return true;
    }


    // *** MODIFIED: Needs rewrite to use UnlockManager and comprehensive data ***
    private boolean isAreaRestricted(WorldPoint playerLocation) {
        if (playerLocation == null) {
            return false;
        }
        // TODO: Implement actual area restriction checking using UnlockManager:
        // 1. Get area definition(s) containing playerLocation from UnlockManager's database.
        // 2. For each definition found, check if its ID is in config.unlockedRelics().
        // 3. Return true if *any* containing area definition is *not* unlocked.

        // --- Placeholder Logic (Example: Restrict region ID 12850 - Lumbridge Swamp Caves) ---
        int currentRegionId = playerLocation.getRegionID();
        Set<Integer> lockedRegionIds = Set.of(12850); // Example locked region
        if (lockedRegionIds.contains(currentRegionId)) {
            // String areaUnlockId = "AREA_LUMBRIDGE_SWAMP_CAVES"; // Example ID
            // return !config.unlockedRelics().contains(areaUnlockId); // Check against config
            return true; // Placeholder: Assume it's locked if in this region
        }
        // --- End Placeholder Logic ---

        return false;
    }


    // --- Helper Methods ---
    // (Keep existing helper methods: getRelicCount, getAvailableRelicCounts, decrementRelicCount, getConfigKeyForRelic, sendChatMessage, resetProgression, getSkillTier, setSkillTier, getMeleeGearTier, setMeleeGearTier, clearChoiceArea)
    private int getRelicCount(RelicType type, SkillTier tier) {
        switch (type) {
            case SKILLING:
                switch (tier) {
                    case APPRENTICE: return config.skillingRelicsApprentice();
                    case JOURNEYMAN: return config.skillingRelicsJourneyman();
                    case EXPERT: return config.skillingRelicsExpert();
                    case MASTER: return config.skillingRelicsMaster();
                    case GRANDMASTER: return config.skillingRelicsGrandmaster();
                    default: return 0;
                }
            case COMBAT:
                switch (tier) {
                    case APPRENTICE: return config.combatRelicsApprentice();
                    case JOURNEYMAN: return config.combatRelicsJourneyman();
                    case EXPERT: return config.combatRelicsExpert();
                    case MASTER: return config.combatRelicsMaster();
                    case GRANDMASTER: return config.combatRelicsGrandmaster();
                    default: return 0;
                }
            case EXPLORATION:
                switch (tier) {
                    case APPRENTICE: return config.explorationRelicsApprentice();
                    case JOURNEYMAN: return config.explorationRelicsJourneyman();
                    case EXPERT: return config.explorationRelicsExpert();
                    case MASTER: return config.explorationRelicsMaster();
                    case GRANDMASTER: return config.explorationRelicsGrandmaster();
                    default: return 0;
                }
            default: return 0;
        }
    }
    private Map<RelicType, Map<SkillTier, Integer>> getAvailableRelicCounts() {
        Map<RelicType, Map<SkillTier, Integer>> counts = new EnumMap<>(RelicType.class);
        List<SkillTier> tiersToCheck = List.of(SkillTier.APPRENTICE, SkillTier.JOURNEYMAN, SkillTier.EXPERT, SkillTier.MASTER, SkillTier.GRANDMASTER);
        for (RelicType type : RelicType.values()) {
            Map<SkillTier, Integer> tierCounts = new EnumMap<>(SkillTier.class);
            for (SkillTier tier : tiersToCheck) {
                int count = getRelicCount(type, tier);
                if (count > 0) tierCounts.put(tier, count);
            }
            if (!tierCounts.isEmpty()) counts.put(type, tierCounts);
        }
        return counts;
    }
    private void decrementRelicCount(RelicType type, SkillTier tier) {
        int currentCount = getRelicCount(type, tier);
        if (currentCount <= 0) return;
        String key = getConfigKeyForRelic(type, tier);
        if (key != null) configManager.setConfiguration(CONFIG_GROUP, key, currentCount - 1);
    }
    private String getConfigKeyForRelic(RelicType type, SkillTier tier) {
        switch (type) {
            case SKILLING:
                switch (tier) {
                    case APPRENTICE: return "skillingRelicsApprentice";
                    case JOURNEYMAN: return "skillingRelicsJourneyman";
                    case EXPERT: return "skillingRelicsExpert";
                    case MASTER: return "skillingRelicsMaster";
                    case GRANDMASTER: return "skillingRelicsGrandmaster";
                } break;
            case COMBAT:
                switch (tier) {
                    case APPRENTICE: return "combatRelicsApprentice";
                    case JOURNEYMAN: return "combatRelicsJourneyman";
                    case EXPERT: return "combatRelicsExpert";
                    case MASTER: return "combatRelicsMaster";
                    case GRANDMASTER: return "combatRelicsGrandmaster";
                } break;
            case EXPLORATION:
                switch (tier) {
                    case APPRENTICE: return "explorationRelicsApprentice";
                    case JOURNEYMAN: return "explorationRelicsJourneyman";
                    case EXPERT: return "explorationRelicsExpert";
                    case MASTER: return "explorationRelicsMaster";
                    case GRANDMASTER: return "explorationRelicsGrandmaster";
                } break;
        }
        return null;
    }
    private void sendChatMessage(String message, ChatMessageType type) {
        if (client.getGameState() != GameState.LOGGED_IN) return;
        clientThread.invoke(() -> {
            if (client.getGameState() == GameState.LOGGED_IN) {
                client.addChatMessage(type, PLUGIN_CHAT_NAME, message, null);
                if (type == ChatMessageType.CONSOLE) {
                    log.warn("Relic Hunter Warning: {}", message);
                } else {
                    log.info("Relic Hunter Message: {}", message);
                }
            } else {
                log.warn("ClientThread invoked, but player no longer logged in. Message not sent: {}", message);
            }
        });
    }
    private void resetProgression() {
        SwingUtilities.invokeLater(() -> {
            int confirm = JOptionPane.showConfirmDialog( null,
                    "Reset all Relic Hunter progression?", "Confirm Reset",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                log.info("Resetting Relic Hunter progression.");
                configManager.setConfiguration(CONFIG_GROUP, "unlockedRelics", new HashSet<String>());
                for (RelicType type : RelicType.values()) {
                    for (SkillTier tier : SkillTier.values()) {
                        if (tier == SkillTier.LOCKED) continue;
                        String key = getConfigKeyForRelic(type, tier);
                        if (key != null) configManager.setConfiguration(CONFIG_GROUP, key, 0);
                    }
                }
                setMeleeGearTier(GearTier.STEEL);
                for (Skill skill : Skill.values()) {
                    if (skill == Skill.OVERALL || skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE || skill == Skill.HITPOINTS) continue;
                    SkillTier defaultTier = SkillTier.LOCKED;
                    if (skill == Skill.MINING || skill == Skill.SMITHING) {
                        defaultTier = SkillTier.APPRENTICE;
                    }
                    setSkillTier(skill, defaultTier);
                }
                configManager.setConfiguration(CONFIG_GROUP, "resetProgressionButton", false);
                sendChatMessage("Relic Hunter progression has been reset.", ChatMessageType.GAMEMESSAGE);
                if (panel != null) {
                    panel.updateRelicCounts();
                    panel.updateUnlockedDisplay();
                    panel.updateProgressionTiersDisplay();
                }
                lastPlayerRegionId = -1;
                previousSkillXp.clear();
                initializePreviousXpMap();
            } else {
                configManager.setConfiguration(CONFIG_GROUP, "resetProgressionButton", false);
            }
        });
    }
    public SkillTier getSkillTier(Skill skill) {
        if (skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE || skill == Skill.HITPOINTS) {
            return SkillTier.GRANDMASTER;
        }
        switch (skill) {
            case RANGED: return config.rangedTier(); case PRAYER: return config.prayerTier();
            case MAGIC: return config.magicTier(); case RUNECRAFT: return config.runecraftTier();
            case CONSTRUCTION: return config.constructionTier();
            case AGILITY: return config.agilityTier(); case HERBLORE: return config.herbloreTier();
            case THIEVING: return config.thievingTier(); case CRAFTING: return config.craftingTier();
            case FLETCHING: return config.fletchingTier(); case SLAYER: return config.slayerTier();
            case HUNTER: return config.hunterTier(); case MINING: return config.miningTier();
            case SMITHING: return config.smithingTier(); case FISHING: return config.fishingTier();
            case COOKING: return config.cookingTier(); case FIREMAKING: return config.firemakingTier();
            case WOODCUTTING: return config.woodcuttingTier(); case FARMING: return config.farmingTier();
            default: return SkillTier.LOCKED;
        }
    }
    private void setSkillTier(Skill skill, SkillTier tier) {
        String key;
        switch (skill) {
            case RANGED: key = "rangedTier"; break; case PRAYER: key = "prayerTier"; break;
            case MAGIC: key = "magicTier"; break; case RUNECRAFT: key = "runecraftTier"; break;
            case CONSTRUCTION: key = "constructionTier"; break;
            case AGILITY: key = "agilityTier"; break; case HERBLORE: key = "herbloreTier"; break;
            case THIEVING: key = "thievingTier"; break; case CRAFTING: key = "craftingTier"; break;
            case FLETCHING: key = "fletchingTier"; break; case SLAYER: key = "slayerTier"; break;
            case HUNTER: key = "hunterTier"; break; case MINING: key = "miningTier"; break;
            case SMITHING: key = "smithingTier"; break; case FISHING: key = "fishingTier"; break;
            case COOKING: key = "cookingTier"; break; case FIREMAKING: key = "firemakingTier"; break;
            case WOODCUTTING: key = "woodcuttingTier"; break; case FARMING: key = "farmingTier"; break;
            default: return;
        }
        configManager.setConfiguration(CONFIG_GROUP, key, tier);
    }
    public GearTier getMeleeGearTier() { return config.meleeGearTier(); }
    private void setMeleeGearTier(GearTier tier) { configManager.setConfiguration(CONFIG_GROUP, "meleeGearTier", tier); }

    // *** FIXED METHOD NAME ***
    private void clearChoiceDisplay() {
        if (panel != null) panel.clearChoiceDisplay();
    }


    // --- Event Subscribers ---
    // (Keep existing subscribers: onConfigChanged, onGameStateChanged, onGameTick, onMenuOptionClicked, onStatChanged, onNpcDespawned, onChatMessage)
    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals(CONFIG_GROUP)) { return; }
        if (event.getKey().equals("resetProgressionButton")) {
            if (Boolean.parseBoolean(event.getNewValue())) resetProgression(); else log.debug("Reset button false change ignored.");
            return;
        }
        if (panel != null) {
            if (event.getKey().toLowerCase().contains("relics") && !event.getKey().equals("unlockedRelics")) panel.updateRelicCounts();
            if (event.getKey().equals("meleeGearTier") || (event.getKey().toLowerCase().contains("tier") && !event.getKey().equals("resetProgressionButton"))) panel.updateProgressionTiersDisplay();
            if (event.getKey().equals("unlockedRelics")) {
                panel.updateUnlockedDisplay();
                panel.updateProgressionTiersDisplay();
            }
            if (event.getKey().toLowerCase().contains("chance") || event.getKey().toLowerCase().contains("thresh")) {
                log.debug("Relic acquisition config changed: {}", event.getKey());
            }
        }
    }
    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            lastPlayerRegionId = -1;
            loginTick = true;
            initializePreviousXpMap();
            if (!initialPanelUpdateDone && panel != null) {
                log.info("Performing initial panel updates on first login.");
                panel.updateRelicCounts(); panel.updateUnlockedDisplay(); panel.updateProgressionTiersDisplay();
                initialPanelUpdateDone = true;
            } else if (panel != null) {
                clearChoiceDisplay(); // *** FIXED METHOD NAME ***
            }
        } else if (gameStateChanged.getGameState() == GameState.HOPPING) {
            lastPlayerRegionId = -1;
        } else if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN || gameStateChanged.getGameState() == GameState.STARTING) {
            initialPanelUpdateDone = false;
            lastPlayerRegionId = -1;
            previousSkillXp.clear();
            recentlyProcessedNpcs.clear();
            loginTick = true;
        }
    }
    @Subscribe
    public void onGameTick(GameTick event) {
        if (config.warnOnRestrictedAreaEntry() && client.getLocalPlayer() != null) {
            WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
            if (playerLocation != null) {
                int currentRegionId = playerLocation.getRegionID();
                if (currentRegionId != lastPlayerRegionId) {
                    if (isAreaRestricted(playerLocation)) {
                        sendChatMessage("Warning: Entered restricted area (Region ID: " + currentRegionId + ")!", ChatMessageType.CONSOLE);
                    }
                    lastPlayerRegionId = currentRegionId;
                }
            }
        }
        int currentTick = client.getTickCount();
        recentlyProcessedNpcs.entrySet().removeIf(entry -> entry.getValue() < currentTick - (NPC_PROCESS_COOLDOWN_TICKS * 2));
        if (loginTick) {
            loginTick = false;
            log.debug("Login tick processed, enabling StatChanged relic checks.");
        }
    }
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (!config.warnOnRestrictedItemUse() && !config.attemptBlockRestrictedItemUse()) {
            return;
        }
        String menuOption = event.getMenuOption();
        MenuAction menuAction = event.getMenuAction();
        boolean isEquipAction = menuOption.equalsIgnoreCase("Wield") || menuOption.equalsIgnoreCase("Wear");
        boolean isInventoryEquipAction = menuAction == MenuAction.CC_OP && event.getWidget() != null && event.getWidget().getId() == WidgetInfo.INVENTORY.getId();
        if (!isEquipAction && !isInventoryEquipAction) {
            return;
        }
        int itemId = event.getItemId();
        if (itemId == -1 && event.getWidget() != null) {
            itemId = event.getWidget().getItemId();
        }
        if (itemId == -1) {
            return;
        }
        if (!isItemAllowed(itemId)) { // Calls the updated isItemAllowed
            ItemComposition itemComp = itemManager.getItemComposition(itemId);
            String itemName = itemComp != null ? itemComp.getName() : "Unknown Item (ID: " + itemId + ")";
            if (config.warnOnRestrictedItemUse()) {
                sendChatMessage("Warning: Cannot use restricted item: " + itemName + "!", ChatMessageType.CONSOLE);
            }
            if (config.attemptBlockRestrictedItemUse()) {
                log.debug("Attempting to block use of restricted item: {} (ID: {}) via action: {}", itemName, itemId, menuAction);
                event.consume();
            }
        }
    }
    @Subscribe
    public void onStatChanged(StatChanged event) {
        Skill skill = event.getSkill();
        int currentXp = event.getXp();
        int previousXp = previousSkillXp.getOrDefault(skill, 0);
        previousSkillXp.put(skill, currentXp);
        if (loginTick) {
            log.trace("Ignoring StatChanged during login tick for skill {}", skill.getName());
            return;
        }
        int xpGained = currentXp - previousXp;
        if (config.warnOnRestrictedXpGain() && xpGained > 0) {
            SkillTier currentTier = getSkillTier(skill);
            boolean isCombatSkill = (skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE || skill == Skill.RANGED || skill == Skill.PRAYER || skill == Skill.MAGIC);
            if (currentTier == SkillTier.LOCKED && !isCombatSkill && skill != Skill.HITPOINTS) {
                if (previousXp > 0) {
                    sendChatMessage("Warning: Gained XP in locked skill: " + skill.getName() + "!", ChatMessageType.CONSOLE);
                }
            }
            else if (currentTier != SkillTier.GRANDMASTER) {
                int currentLevel = Experience.getLevelForXp(currentXp);
                int levelCap = currentTier.getLevelCap();
                if (currentLevel > levelCap) {
                    sendChatMessage("Warning: Exceeded level cap (" + levelCap + ") for " + skill.getName() + " (Tier: " + currentTier.getDisplayName() + ")!", ChatMessageType.CONSOLE);
                }
            }
        }
        boolean isCombatSkillForRelic = (skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE || skill == Skill.RANGED || skill == Skill.PRAYER || skill == Skill.MAGIC);
        if (xpGained <= 0 || isCombatSkillForRelic || skill == Skill.HITPOINTS || skill == Skill.OVERALL) {
            return;
        }
        if (previousXp == 0) {
            log.trace("Ignoring potential relic drop for {} due to previous XP being 0 (likely login).", skill.getName());
            return;
        }
        int baseChance = config.skillingRelicBaseChance();
        if (baseChance <= 0) baseChance = 500;
        if (random.nextInt(baseChance) == 0) {
            log.debug("Skilling relic base roll SUCCESS for skill {} (XP Gained: {})", skill.getName(), xpGained);
            SkillTier maxTier = determineMaxTierFromXp(xpGained);
            log.trace("Max possible tier based on XP {}: {}", xpGained, maxTier);
            if (maxTier != null && maxTier != SkillTier.LOCKED) {
                SkillTier receivedTier = rollRelicTier(maxTier);
                log.trace("Weighted tier roll result: {}", receivedTier);
                if (receivedTier != null) {
                    addRelic(RelicType.SKILLING, receivedTier);
                } else {
                    log.warn("Weighted tier roll returned null for max tier {}", maxTier);
                }
            } else {
                log.debug("Max tier was null or LOCKED, no relic tier roll performed for XP {}.", xpGained);
            }
        }
    }
    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        NPC npc = event.getNpc();
        if (npc == null || npc.getCombatLevel() <= 0) {
            return;
        }
        if (!npc.isDead()) {
            return;
        }
        int currentTick = client.getTickCount();
        int npcIndex = npc.getIndex();
        if (recentlyProcessedNpcs.getOrDefault(npcIndex, -NPC_PROCESS_COOLDOWN_TICKS) > currentTick - NPC_PROCESS_COOLDOWN_TICKS) {
            return;
        }
        log.debug("Processing potential player kill for NPC {} (Level {})", npc.getId(), npc.getCombatLevel());
        recentlyProcessedNpcs.put(npcIndex, currentTick);
        int combatLevel = npc.getCombatLevel();
        int baseChance = config.combatRelicBaseChance();
        if (baseChance <= 0) baseChance = 200;
        log.trace("Rolling for combat relic: Base chance 1 in {}", baseChance);
        if (random.nextInt(baseChance) == 0) {
            log.debug("Combat relic base roll SUCCESS for NPC {}!", npc.getId());
            SkillTier maxTier = determineMaxTierFromNpcLevel(combatLevel);
            log.trace("Max possible tier based on level {}: {}", combatLevel, maxTier);
            if (maxTier != null && maxTier != SkillTier.LOCKED) {
                SkillTier receivedTier = rollRelicTier(maxTier);
                log.trace("Weighted tier roll result: {}", receivedTier);
                if (receivedTier != null) {
                    addRelic(RelicType.COMBAT, receivedTier);
                } else {
                    log.warn("Weighted tier roll returned null for max tier {}", maxTier);
                }
            } else {
                log.debug("Max tier was null or LOCKED, no relic tier roll performed.");
            }
        } else {
            log.trace("Combat relic base roll FAILED for NPC {}.", npc.getId());
        }
    }
    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM) {
            return;
        }
        String message = Text.removeTags(event.getMessage());
        Matcher matcher = CLUE_SCROLL_COMPLETION_PATTERN.matcher(message);
        if (matcher.matches()) {
            String clueTierString = matcher.group(1).toLowerCase();
            log.debug("Detected clue scroll completion: {}", clueTierString);
            SkillTier relicTier;
            int chancePercent;
            switch (clueTierString) {
                case "beginner": return;
                case "easy":
                    relicTier = SkillTier.APPRENTICE;
                    chancePercent = config.explorationRelicChanceEasy();
                    break;
                case "medium":
                    relicTier = SkillTier.JOURNEYMAN;
                    chancePercent = config.explorationRelicChanceMedium();
                    break;
                case "hard":
                    relicTier = SkillTier.EXPERT;
                    chancePercent = config.explorationRelicChanceHard();
                    break;
                case "elite":
                    relicTier = SkillTier.MASTER;
                    chancePercent = config.explorationRelicChanceElite();
                    break;
                case "master":
                    relicTier = SkillTier.GRANDMASTER;
                    chancePercent = config.explorationRelicChanceMaster();
                    break;
                default:
                    log.warn("Unknown clue scroll tier detected: {}", clueTierString);
                    return;
            }
            if (chancePercent > 0 && random.nextInt(100) < chancePercent) {
                addRelic(RelicType.EXPLORATION, relicTier);
            }
        }
    }


    // --- Relic Acquisition Helpers ---
    private SkillTier determineMaxTierFromXp(int xpGained) {
        if (xpGained <= 0) return null;
        if (xpGained <= config.skillingRelicXpThresholdApp()) return SkillTier.APPRENTICE;
        if (xpGained <= config.skillingRelicXpThresholdJour()) return SkillTier.JOURNEYMAN;
        if (xpGained <= config.skillingRelicXpThresholdExp()) return SkillTier.EXPERT;
        if (xpGained <= config.skillingRelicXpThresholdMas()) return SkillTier.MASTER;
        return SkillTier.GRANDMASTER;
    }

    private SkillTier determineMaxTierFromNpcLevel(int npcLevel) {
        if (npcLevel <= 0) return null;
        if (npcLevel <= config.combatRelicNpcLevelApp()) return SkillTier.APPRENTICE;
        if (npcLevel <= config.combatRelicNpcLevelJour()) return SkillTier.JOURNEYMAN;
        if (npcLevel <= config.combatRelicNpcLevelExp()) return SkillTier.EXPERT;
        if (npcLevel <= config.combatRelicNpcLevelMas()) return SkillTier.MASTER;
        return SkillTier.GRANDMASTER;
    }

    private SkillTier rollRelicTier(SkillTier maxPossibleTier) {
        if (maxPossibleTier == null || maxPossibleTier == SkillTier.LOCKED) {
            return null;
        }
        Map<SkillTier, Integer> weights = new EnumMap<>(SkillTier.class);
        weights.put(SkillTier.APPRENTICE, 100);
        weights.put(SkillTier.JOURNEYMAN, 50);
        weights.put(SkillTier.EXPERT, 25);
        weights.put(SkillTier.MASTER, 10);
        weights.put(SkillTier.GRANDMASTER, 5);

        int totalWeight = 0;
        List<SkillTier> possibleTiers = new ArrayList<>();
        for (SkillTier tier : SkillTier.values()) {
            if (tier != SkillTier.LOCKED && tier.ordinal() <= maxPossibleTier.ordinal()) {
                int weight = weights.getOrDefault(tier, 0);
                if (tier == maxPossibleTier && tier != SkillTier.APPRENTICE) {
                    weight = Math.max(1, weight / 5);
                }
                if (weight > 0) {
                    possibleTiers.add(tier);
                    totalWeight += weight;
                }
            }
        }

        if (totalWeight <= 0 || possibleTiers.isEmpty()) {
            log.warn("No valid tiers or weights for rolling up to {}", maxPossibleTier);
            return null;
        }
        int roll = random.nextInt(totalWeight);
        int cumulativeWeight = 0;

        for (SkillTier tier : possibleTiers) {
            int weight = weights.getOrDefault(tier, 0);
            if (tier == maxPossibleTier && tier != SkillTier.APPRENTICE) {
                weight = Math.max(1, weight / 5);
            }
            if (weight > 0) {
                cumulativeWeight += weight;
                if (roll < cumulativeWeight) {
                    return tier;
                }
            }
        }
        log.error("Relic tier roll failed unexpectedly. TotalWeight={}, Roll={}", totalWeight, roll);
        return null;
    }


    private void addRelic(RelicType type, SkillTier tier) {
        if (tier == SkillTier.LOCKED) return;
        String configKey = getConfigKeyForRelic(type, tier);
        if (configKey == null) {
            log.error("Could not find config key for Relic: {} {}", type, tier);
            return;
        }
        int currentCount = getRelicCount(type, tier);
        configManager.setConfiguration(CONFIG_GROUP, configKey, currentCount + 1);
        String message = String.format("You found a %s %s Relic!",
                tier.getDisplayName(),
                type.name().substring(0, 1).toUpperCase() + type.name().substring(1).toLowerCase());
        sendChatMessage(message, ChatMessageType.GAMEMESSAGE);
        log.info("Added Relic: {} {} (New count: {})", tier.getDisplayName(), type.name(), currentCount + 1);

        if (panel != null) {
            clientThread.invokeLater(panel::updateRelicCounts);
        }
    }


    // --- Guice Provider ---
    @Provides
    RelicHunterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(RelicHunterConfig.class);
    }
}
