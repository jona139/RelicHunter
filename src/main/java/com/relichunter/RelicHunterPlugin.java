// Filename: RelicHunter/src/main/java/com/relichunter/RelicHunterPlugin.java
package com.relichunter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.relichunter.unlock.UnlockData;
import com.relichunter.unlock.UnlockDatabaseRoot;
import com.relichunter.unlock.UnlockType;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
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
import net.runelite.client.game.SpriteManager;
// Removed MouseListener import
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import javax.swing.*;
import java.awt.Color;
// Removed MouseEvent import
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


@Slf4j
@PluginDescriptor(
        name = "Relic Hunter Helper",
        description = "Helps track progress and restrictions for the Relic Hunter Ironman mode.",
        tags = {"ironman", "relic", "hunter", "helper", "restriction", "unique"}
)
// *** REMOVED implements MouseListener ***
public class RelicHunterPlugin extends Plugin
{
    static final String CONFIG_GROUP = "relichunter";
    private static final String F2P_DB_PATH = "/relic_hunter_unlock_database_f2p.json";
    private static final String P2P_DB_PATH = "/relic_hunter_unlock_database.json";
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
    @Inject private QuestLogOverlay questLogOverlay;
    @Inject private RelicChoiceOverlay relicChoiceOverlay;
    @Inject private ClientThread clientThread;
    @Inject private ItemManager itemManager;
    @Inject private UnlockManager unlockManager;
    @Inject private SpriteManager spriteManager;
    @Inject private MouseManager mouseManager; // Keep MouseManager injection

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

    // Constants for visual effects (Using Integer IDs)
    private static final int UNLOCK_ANIMATION_ID = 7751;
    private static final int UNLOCK_GRAPHIC_ID = 1414;
    private static final int RELIC_DROP_GRAPHIC_ID = 111;
    private static final int RELIC_DROP_SOUND_ID = 202;
    private static final int RELIC_UNLOCK_CONFIRM_SOUND_ID = 202;


    @Override
    protected void startUp() throws Exception {
        log.info("Relic Hunter Helper starting up...");
        loadUnlockDatabase();

        panel = new RelicHunterPanel(this, config, unlockManager, itemManager, spriteManager);

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
        overlayManager.add(questLogOverlay);
        overlayManager.add(relicChoiceOverlay);
        // *** Register the OVERLAY as the listener ***
        mouseManager.registerMouseListener(relicChoiceOverlay);

        initialPanelUpdateDone = false;
        lastPlayerRegionId = -1;
        previousSkillXp.clear();
        recentlyProcessedNpcs.clear();
        loginTick = true;
        if (client.getGameState() == GameState.LOGGED_IN) {
            initializePreviousXpMap();
            clientThread.invokeLater(() -> {
                if (panel != null) {
                    panel.updateRelicCounts();
                    panel.updateUnlockedDisplay();
                    panel.updateProgressionTiersDisplay();
                    initialPanelUpdateDone = true;
                }
            });
        }
        log.info("Relic Hunter Helper startup complete.");
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Relic Hunter Helper stopped!");
        overlayManager.remove(itemRestrictionOverlay);
        overlayManager.remove(skillTabOverlay);
        overlayManager.remove(questLogOverlay);
        overlayManager.remove(relicChoiceOverlay);
        // *** Unregister the OVERLAY as the listener ***
        mouseManager.unregisterMouseListener(relicChoiceOverlay);
        clientToolbar.removeNavigation(navButton);
        if (panel != null) { panel.shutdown(); }
        panel = null;
        navButton = null;
        lastPlayerRegionId = -1;
        previousSkillXp.clear();
        recentlyProcessedNpcs.clear();
        unlockManager.clearDatabase();
        relicChoiceOverlay.clearChoices();
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
        String dbPath = config.useF2PDatabase() ? F2P_DB_PATH : P2P_DB_PATH;
        log.info("Loading unlock database from JSON: {}", dbPath);
        try (InputStream is = getClass().getResourceAsStream(dbPath)) {
            if (is == null) {
                log.error("Could not find unlock database JSON file at path: {}", dbPath);
                sendChatMessage("Error: Failed to load essential unlock data. Plugin may not function correctly.", ChatMessageType.CONSOLE);
                unlockManager.loadData(Collections.emptyList());
                return;
            }
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            Type dataType = new TypeToken<UnlockDatabaseRoot>(){}.getType();
            UnlockDatabaseRoot parsedData = gson.fromJson(reader, dataType);

            if (parsedData != null && parsedData.getUnlocks() != null) {
                unlockManager.loadData(parsedData.getUnlocks());
                log.info("Successfully loaded {} unlock definitions from {}.", parsedData.getUnlocks().size(), dbPath);
            } else {
                log.error("Parsed unlock database or unlocks list was null from {}.", dbPath);
                sendChatMessage("Error: Failed to parse unlock data. Plugin may not function correctly.", ChatMessageType.CONSOLE);
                unlockManager.loadData(Collections.emptyList());
            }
        } catch (Exception e) {
            log.error("Error loading or parsing unlock database JSON from " + dbPath, e);
            sendChatMessage("Error: Exception loading unlock data. Plugin may not function correctly.", ChatMessageType.CONSOLE);
            unlockManager.loadData(Collections.emptyList());
        }
    }


    // --- Relic Activation Logic ---
    public void initiateActivationSequence() {
        log.debug("Initiating activation sequence...");
        clearChoiceDisplay();

        Map<RelicType, Map<SkillTier, Integer>> availableRelics = getAvailableRelicCounts();
        List<Map.Entry<RelicType, SkillTier>> usableOptions = new ArrayList<>();
        availableRelics.forEach((type, tierMap) -> tierMap.forEach((tier, count) -> {
            if (count > 0) usableOptions.add(Map.entry(type, tier));
        }));

        if (usableOptions.isEmpty()) {
            log.warn("Activation initiated but no relics are held.");
            if (panel != null) {
                panel.displayError("You do not have any relics to activate!");
            } else {
                sendChatMessage("You do not have any relics to activate!", ChatMessageType.GAMEMESSAGE);
            }
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
                log.debug("User cancelled relic type/tier selection prompt.");
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
        if (relicCount <= 0) {
            log.error("startRelicActivation called for {} {} but count is zero.", type, tier);
            if (panel != null) panel.displayError("Internal Error: Relic count is zero");
            else sendChatMessage("Internal Error: Relic count is zero.", ChatMessageType.GAMEMESSAGE);
            return;
        }

        List<UnlockData> potentialUnlocks = unlockManager.getPotentialUnlocks(type, tier);
        if (potentialUnlocks.isEmpty()) {
            log.warn("No unlocks defined in database for Relic Type {} and Tier {}", type, tier);
            if (panel != null) panel.displayError("Internal error: No unlocks defined for this relic type/tier.");
            else sendChatMessage("Internal error: No unlocks defined for this relic type/tier.", ChatMessageType.GAMEMESSAGE);
            return;
        }
        log.debug("Potential unlocks for {} {}: {}", type, tier, potentialUnlocks.stream().map(UnlockData::getId).collect(Collectors.toList()));


        Set<String> currentlyUnlockedIds = config.unlockedRelics();
        log.debug("Currently unlocked IDs: {}", currentlyUnlockedIds);

        List<UnlockData> validUnlocks = potentialUnlocks.stream()
                .filter(unlock -> {
                    boolean alreadyUnlocked = currentlyUnlockedIds.contains(unlock.getId());
                    if (alreadyUnlocked) {
                        log.trace("Filtering out already unlocked: {}", unlock.getId());
                    }
                    return !alreadyUnlocked;
                })
                .filter(unlock -> {
                    boolean prereqsMet = checkPrerequisites(unlock, currentlyUnlockedIds);
                    if (!prereqsMet) {
                        log.trace("Filtering out due to unmet prerequisites: {} (needs {})", unlock.getId(), unlock.getPrerequisites());
                    }
                    return prereqsMet;
                })
                .collect(Collectors.toList());

        log.debug("Filtered unlocks. Valid unlocks count: {}. IDs: {}",
                validUnlocks.size(), validUnlocks.stream().map(UnlockData::getId).collect(Collectors.toList()));

        if (validUnlocks.isEmpty()) {
            log.warn("No valid unlocks available after filtering for {} {}", type, tier);
            if (panel != null) panel.displayError("No new unlocks available for this relic type/tier (already unlocked or prerequisites not met).");
            else sendChatMessage("No new unlocks available for this relic type/tier.", ChatMessageType.GAMEMESSAGE);
            return;
        }

        Collections.shuffle(validUnlocks, random);
        List<UnlockData> choices = validUnlocks.stream().limit(3).collect(Collectors.toList());

        List<Unlockable> displayChoices = choices.stream()
                .map(ud -> new Unlockable(ud.getId(), ud.getName(), ud.getDescription(), ud.getRelicType(), ud.getRequiredTier(), ud.getPrerequisites()))
                .collect(Collectors.toList());

        log.debug("Preparing to display choices. Overlay enabled: {}. Display choices count: {}",
                config.showChoiceOverlay(), displayChoices.size());

        if (config.showChoiceOverlay()) {
            relicChoiceOverlay.setCurrentChoices(displayChoices);
            log.debug("Called setCurrentChoices on overlay.");
        } else if (panel != null) {
            log.warn("Relic choice overlay disabled, but panel choice display is removed. Cannot show choices.");
            sendChatMessage("Enable 'Show Choice Overlay' in config to see choices.", ChatMessageType.GAMEMESSAGE);
        } else {
            log.error("Cannot display choices - Choice overlay disabled and panel is null.");
            sendChatMessage("Error: Could not display relic choices.", ChatMessageType.GAMEMESSAGE);
        }
    }

    private boolean checkPrerequisites(UnlockData unlock, Set<String> currentlyUnlockedIds) {
        return unlockManager.arePrerequisitesMet(unlock.getId(), currentlyUnlockedIds);
    }

    public void completeRelicActivation(Unlockable chosenPanelUnlock) {
        UnlockData chosenUnlockData = unlockManager.getUnlockById(chosenPanelUnlock.getId()).orElse(null);

        if (chosenUnlockData == null) {
            log.error("Could not find UnlockData for chosen panel unlock ID: {}", chosenPanelUnlock.getId());
            if (panel != null) panel.displayError("Internal Error: Could not process selection.");
            else sendChatMessage("Internal Error: Could not process selection.", ChatMessageType.GAMEMESSAGE);
            return;
        }

        log.info("Completing activation for: {}", chosenUnlockData.getName());
        RelicType consumedRelicType = chosenUnlockData.getRelicType();
        SkillTier consumedRelicTier = chosenUnlockData.getRequiredTier();

        if (getRelicCount(consumedRelicType, consumedRelicTier) <= 0) {
            log.error("Relic count mismatch! Trying to consume {} {} but count is zero.", consumedRelicType, consumedRelicTier);
            if (panel != null) panel.displayError("Internal Error: Relic count mismatch");
            else sendChatMessage("Internal Error: Relic count mismatch.", ChatMessageType.GAMEMESSAGE);
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

        triggerUnlockEffects();
        clearChoiceDisplay();
    }

    private void triggerUnlockEffects() {
        clientThread.invoke(() -> {
            Player localPlayer = client.getLocalPlayer();
            if (localPlayer != null) {
                if (config.playUnlockEmote()) {
                    if (localPlayer.getAnimation() == -1 || localPlayer.getAnimation() == AnimationID.IDLE) {
                        localPlayer.setAnimation(UNLOCK_ANIMATION_ID);
                        localPlayer.setAnimationFrame(0);
                        log.debug("Triggering unlock animation: {}", UNLOCK_ANIMATION_ID);
                    } else {
                        log.debug("Player already animating, skipping unlock emote.");
                    }
                }

                if (config.showUnlockGraphic()) {
                    localPlayer.setGraphic(UNLOCK_GRAPHIC_ID);
                    log.debug("Triggering unlock graphic: {}", UNLOCK_GRAPHIC_ID);
                }

                client.playSoundEffect(RELIC_UNLOCK_CONFIRM_SOUND_ID);
                log.debug("Played unlock confirmation sound: {}", RELIC_UNLOCK_CONFIRM_SOUND_ID);

            }
        });
    }

    private void updateProgressionState(UnlockData unlockedData) {
        log.debug("Updating progression state for unlock: {} ({})", unlockedData.getName(), unlockedData.getId());
        switch (unlockedData.getCategory()) {
            case SKILL_TIER:
                String[] parts = unlockedData.getId().split("_");
                if (parts.length >= 3 && parts[0].equals("SKILL")) {
                    try {
                        Skill skill = Skill.valueOf(parts[1]);
                        String tierPart = String.join("_", java.util.Arrays.copyOfRange(parts, 2, parts.length));
                        SkillTier tier = null;
                        try {
                            tier = SkillTier.valueOf(tierPart);
                        } catch (IllegalArgumentException e) {
                            if (tierPart.startsWith("LEVEL_")) {
                                int level = Integer.parseInt(tierPart.substring(6));
                                tier = SkillTier.getByLevelCap(level);
                                if (tier == null) {
                                    log.error("Could not map level {} to a SkillTier for ID: {}", level, unlockedData.getId());
                                }
                            } else {
                                log.error("Could not parse tier part '{}' from ID: {}", tierPart, unlockedData.getId());
                            }
                        }

                        if (tier != null) {
                            setSkillTier(skill, tier);
                            log.info("Set skill {} to tier {}", skill, tier);
                        }
                    } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                        log.error("Could not parse skill/tier from ID: {}", unlockedData.getId(), e);
                    }
                } else {
                    log.error("Could not parse SKILL_TIER ID format: {}", unlockedData.getId());
                }
                break;
            case GEAR_TIER:
                String[] gearParts = unlockedData.getId().split("_");
                if (gearParts.length >= 3 && gearParts[0].equals("GEAR")) {
                    String gearType = gearParts[1];
                    String tierPart = String.join("_", java.util.Arrays.copyOfRange(gearParts, 2, gearParts.length));
                    try {
                        GearTier gearTier = GearTier.fromEnumName(tierPart);
                        if (gearTier == GearTier.NONE) {
                            log.error("Could not parse gear tier enum name '{}' from ID: {}", tierPart, unlockedData.getId());
                            break;
                        }

                        if ("MELEE".equalsIgnoreCase(gearType)) {
                            setMeleeGearTier(gearTier);
                            log.info("Set MELEE gear tier to {}", gearTier);
                        } else if ("RANGED".equalsIgnoreCase(gearType)) {
                            log.info("Set RANGED gear tier to {}", gearTier);
                        } else if ("MAGIC".equalsIgnoreCase(gearType)) {
                            log.info("Set MAGIC gear tier to {}", gearTier);
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
                log.info("Unlocked specific item: {} (ID: {}). Restrictions handled by isItemAllowed.", unlockedData.getName(), unlockedData.getId());
                break;
            case AREA:
                log.info("Unlocked area: {} (ID: {}). (Area restriction logic needs implementation)", unlockedData.getName(), unlockedData.getId());
                break;
            case QUEST:
                log.info("Unlocked quest: {} (ID: {}). (Quest tracking handled by QuestLogOverlay)", unlockedData.getName(), unlockedData.getId());
                break;
            case MECHANIC:
                log.info("Unlocked mechanic: {} (ID: {}). (Mechanic restriction logic needs implementation)", unlockedData.getName(), unlockedData.getId());
                break;
            default:
                log.warn("No specific progression state update logic implemented for category: {}", unlockedData.getCategory());
                break;
        }
        if (panel != null) {
            clientThread.invokeLater(() -> {
                panel.updateUnlockedDisplay();
                panel.updateProgressionTiersDisplay();
                panel.updateRelicCounts();
            });
        }
    }


    // --- Restriction Checking ---
    public boolean isItemAllowed(int itemId) {
        Set<String> unlockedIds = config.unlockedRelics();
        if (!unlockManager.isItemPermittedByUnlocks(itemId, unlockedIds)) {
            log.trace("Item {} denied by specific/gear tier unlocks.", itemId);
            return false;
        }
        log.trace("Item {} permitted (specific/gear tier checks passed).", itemId);
        return true;
    }


    private boolean isAreaRestricted(WorldPoint playerLocation) {
        if (playerLocation == null) {
            return false;
        }
        return false;
    }


    // --- Relic Acquisition Helpers ---
    private void addRelic(RelicType type, SkillTier tier, RelicSource source, @Nullable WorldPoint effectLocation) {
        if (tier == SkillTier.LOCKED) return;
        incrementRelicCount(type, tier);
        String tierName = tier.getDisplayName();
        String typeName = type.name().substring(0, 1).toUpperCase() + type.name().substring(1).toLowerCase();
        String message = String.format("You found a %s %s Relic!", tierName, typeName);
        sendChatMessage(message, ChatMessageType.GAMEMESSAGE);
        clientThread.invoke(() -> {
            Player localPlayer = client.getLocalPlayer();
            if (localPlayer != null) {
                log.debug("Attempting to set graphic {} on player for {} relic drop.", RELIC_DROP_GRAPHIC_ID, source);
                localPlayer.setGraphic(RELIC_DROP_GRAPHIC_ID);
                client.playSoundEffect(RELIC_DROP_SOUND_ID);
                log.debug("Set graphic and played sound for {} relic drop.", source);
            } else {
                log.warn("Could not get local player to apply {} relic graphic.", source);
            }
        });
        if (panel != null) {
            clientThread.invokeLater(panel::updateRelicCounts);
        }
    }

    private int calculateAvailableUnlocks(RelicType type, SkillTier tier) {
        List<UnlockData> potentialUnlocks = unlockManager.getPotentialUnlocks(type, tier);
        if (potentialUnlocks.isEmpty()) {
            log.trace("calculateAvailableUnlocks: No potential unlocks found for {} {}", type, tier);
            return 0;
        }
        Set<String> currentlyUnlockedIds = config.unlockedRelics();
        long count = potentialUnlocks.stream()
                .filter(unlock -> !currentlyUnlockedIds.contains(unlock.getId()))
                .filter(unlock -> checkPrerequisites(unlock, currentlyUnlockedIds))
                .count();
        log.debug("Calculated {} available unlocks for {} {}", count, type, tier);
        return (int) count;
    }


    private void incrementRelicCount(RelicType type, SkillTier tier) {
        String configKey = getConfigKeyForRelic(type, tier);
        if (configKey == null) {
            log.error("Could not find config key for Relic: {} {}", type, tier);
            return;
        }
        int currentCount = getRelicCount(type, tier);
        configManager.setConfiguration(CONFIG_GROUP, configKey, currentCount + 1);
        log.info("Incremented Relic Count: {} {} (New count: {})", tier.getDisplayName(), type.name(), currentCount + 1);
    }

    // --- Helper Methods ---
    public int getRelicCount(RelicType type, SkillTier tier) {
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

    public Map<RelicType, Map<SkillTier, Integer>> getAvailableRelicCounts() {
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
                final String coloredMessage = ColorUtil.wrapWithColorTag(message, Color.MAGENTA);
                client.addChatMessage(type, PLUGIN_CHAT_NAME, coloredMessage, null);
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
                    "Reset all Relic Hunter progression?\nThis affects tracked unlocks and relic counts ONLY.", "Confirm Reset",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                log.info("Resetting Relic Hunter progression.");
                configManager.unsetConfiguration(CONFIG_GROUP, "unlockedRelics");
                for (RelicType type : RelicType.values()) {
                    for (SkillTier tier : SkillTier.values()) {
                        if (tier == SkillTier.LOCKED || tier.name().startsWith("LEVEL_")) continue;
                        String key = getConfigKeyForRelic(type, tier);
                        if (key != null) configManager.unsetConfiguration(CONFIG_GROUP, key);
                    }
                }
                configManager.unsetConfiguration(CONFIG_GROUP, "meleeGearTier");
                for (Skill skill : Skill.values()) {
                    if (skill == Skill.OVERALL || skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE || skill == Skill.HITPOINTS) continue;
                    String key = getSkillTierConfigKey(skill);
                    if (key != null) configManager.unsetConfiguration(CONFIG_GROUP, key);
                }
                sendChatMessage("Relic Hunter progression has been reset.", ChatMessageType.GAMEMESSAGE);
                if (panel != null) {
                    panel.updateRelicCounts();
                    panel.updateUnlockedDisplay();
                    panel.updateProgressionTiersDisplay();
                }
                lastPlayerRegionId = -1;
                previousSkillXp.clear();
                if (client.getGameState() == GameState.LOGGED_IN) {
                    initializePreviousXpMap();
                }
            }
            configManager.setConfiguration(CONFIG_GROUP, "resetProgressionButton", false);
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

    @Nullable
    private String getSkillTierConfigKey(Skill skill) {
        switch (skill) {
            case RANGED: return "rangedTier"; case PRAYER: return "prayerTier";
            case MAGIC: return "magicTier"; case RUNECRAFT: return "runecraftTier";
            case CONSTRUCTION: return "constructionTier";
            case AGILITY: return "agilityTier"; case HERBLORE: return "herbloreTier";
            case THIEVING: return "thievingTier"; case CRAFTING: return "craftingTier";
            case FLETCHING: return "fletchingTier"; case SLAYER: return "slayerTier";
            case HUNTER: return "hunterTier"; case MINING: return "miningTier";
            case SMITHING: return "smithingTier"; case FISHING: return "fishingTier";
            case COOKING: return "cookingTier"; case FIREMAKING: return "firemakingTier";
            case WOODCUTTING: return "woodcuttingTier"; case FARMING: return "farmingTier";
            default: return null;
        }
    }

    private void setSkillTier(Skill skill, SkillTier tier) {
        String key = getSkillTierConfigKey(skill);
        if (key != null) {
            configManager.setConfiguration(CONFIG_GROUP, key, tier);
        }
    }
    public GearTier getMeleeGearTier() { return config.meleeGearTier(); }
    private void setMeleeGearTier(GearTier tier) { configManager.setConfiguration(CONFIG_GROUP, "meleeGearTier", tier); }

    public void clearChoiceDisplay() {
        if (relicChoiceOverlay != null) {
            relicChoiceOverlay.clearChoices();
        }
        if(panel != null) {
            panel.clearChoiceDisplay();
        }
    }


    // --- Event Subscribers ---
    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals(CONFIG_GROUP)) { return; }

        if (event.getKey().equals("showChoiceOverlay") && "false".equals(event.getNewValue())) {
            clearChoiceDisplay();
        }

        if (event.getKey().equals("useF2PDatabase")) {
            log.info("Database selection changed, reloading...");
            loadUnlockDatabase();
            if (panel != null) {
                clientThread.invokeLater(() -> {
                    panel.updateRelicCounts();
                    panel.updateUnlockedDisplay();
                    panel.updateProgressionTiersDisplay();
                });
            }
            return;
        }
        if (event.getKey().equals("resetProgressionButton")) {
            if (Boolean.parseBoolean(event.getNewValue())) {
                resetProgression();
            }
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
                clientThread.invokeLater(() -> {
                    panel.updateRelicCounts();
                    panel.updateUnlockedDisplay();
                    panel.updateProgressionTiersDisplay();
                    initialPanelUpdateDone = true;
                });
            } else {
                clearChoiceDisplay();
            }
        } else if (gameStateChanged.getGameState() == GameState.HOPPING) {
            lastPlayerRegionId = -1;
            clearChoiceDisplay();
        } else if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN || gameStateChanged.getGameState() == GameState.STARTING) {
            initialPanelUpdateDone = false;
            lastPlayerRegionId = -1;
            previousSkillXp.clear();
            recentlyProcessedNpcs.clear();
            loginTick = true;
            clearChoiceDisplay();
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
            } else {
                lastPlayerRegionId = -1;
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
    public void onStatChanged(StatChanged event) {
        Skill skill = event.getSkill();
        int currentXp = event.getXp();
        int previousXp = previousSkillXp.getOrDefault(skill, 0);

        previousSkillXp.put(skill, currentXp);

        if (loginTick || currentXp <= previousXp) {
            return;
        }
        int xpGained = currentXp - previousXp;

        if (config.warnOnRestrictedXpGain()) {
            SkillTier currentTier = getSkillTier(skill);
            if (currentTier == SkillTier.LOCKED && !(skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE || skill == Skill.HITPOINTS)) {
                if (previousXp > 0) {
                    sendChatMessage("Warning: Gained XP in locked skill: " + skill.getName() + "!", ChatMessageType.CONSOLE);
                }
            }
            else if (currentTier != SkillTier.LOCKED && currentTier != SkillTier.GRANDMASTER) {
                int currentLevel = Experience.getLevelForXp(currentXp);
                int levelCap = currentTier.getLevelCap();
                if (currentLevel > levelCap) {
                    sendChatMessage("Warning: Exceeded level cap (" + levelCap + ") for " + skill.getName() + " (Tier: " + currentTier.getDisplayName() + ")!", ChatMessageType.CONSOLE);
                }
            }
        }

        boolean isCombatSkillForRelic = (skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE || skill == Skill.RANGED || skill == Skill.PRAYER || skill == Skill.MAGIC);
        if (isCombatSkillForRelic || skill == Skill.HITPOINTS || skill == Skill.OVERALL) {
            return;
        }
        if (previousXp == 0) {
            log.trace("Ignoring potential skilling relic drop for {} due to previous XP being 0.", skill.getName());
            return;
        }

        SkillTier maxTier = determineMaxTierFromMinXp(xpGained);
        if (maxTier == null) {
            log.trace("XP gain {} too low for any relic tier, no skilling relic roll performed.", xpGained);
            return;
        }

        int availableUnlockCount = calculateAvailableUnlocks(RelicType.SKILLING, maxTier);
        if (availableUnlockCount <= 0) {
            log.debug("No available skilling unlocks for tier {}, no roll performed.", maxTier);
            return;
        }

        int baseChance = config.skillingRelicBaseChance();
        if (baseChance <= 0) baseChance = 500;

        int rollThreshold = config.enableScalingDropRate()
                ? Math.max(1, baseChance / availableUnlockCount)
                : baseChance;

        log.debug("Attempting skilling relic roll. Skill={}, MaxTier={}, Available={}, BaseChance={}, Scaling={}, Threshold=1 in {}",
                skill.getName(), maxTier, availableUnlockCount, baseChance, config.enableScalingDropRate(), rollThreshold);

        if (random.nextInt(rollThreshold) == 0) {
            log.debug("Skilling relic base roll SUCCESS (1 in {}) for skill {} (XP Gained: {})", rollThreshold, skill.getName(), xpGained);
            SkillTier receivedTier = rollRelicTier(maxTier);
            log.trace("Weighted tier roll result: {}", receivedTier);
            if (receivedTier != null) {
                addRelic(RelicType.SKILLING, receivedTier, RelicSource.SKILLING, null);
            } else { log.warn("Weighted tier roll returned null for max tier {}", maxTier); }
        } else { log.trace("Skilling relic base roll FAILED (1 in {}) for skill {} (XP Gained: {})", rollThreshold, skill.getName(), xpGained); }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        NPC npc = event.getNpc();
        if (npc == null || npc.getCombatLevel() <= 0 || !npc.isDead()) {
            return;
        }
        int currentTick = client.getTickCount();
        int npcIndex = npc.getIndex();
        if (recentlyProcessedNpcs.getOrDefault(npcIndex, -NPC_PROCESS_COOLDOWN_TICKS) > currentTick - NPC_PROCESS_COOLDOWN_TICKS) {
            return;
        }
        WorldPoint deathLocation = npc.getWorldLocation();
        if (deathLocation == null) {
            log.warn("NPC {} (Index {}) despawned at null location.", npc.getId(), npcIndex);
            return;
        }
        log.debug("Processing potential kill for NPC {} (Index {}, Level {}) at {}", npc.getId(), npcIndex, npc.getCombatLevel(), deathLocation);
        recentlyProcessedNpcs.put(npcIndex, currentTick);

        int combatLevel = npc.getCombatLevel();
        SkillTier maxTier = determineMaxTierFromNpcLevel(combatLevel);
        if (maxTier == null || maxTier == SkillTier.LOCKED) {
            log.trace("Max tier was null or LOCKED based on NPC level {}, no combat relic tier roll performed.", combatLevel);
            return;
        }

        int availableUnlockCount = calculateAvailableUnlocks(RelicType.COMBAT, maxTier);
        if (availableUnlockCount <= 0) {
            log.debug("No available combat unlocks for tier {}, no roll performed.", maxTier);
            return;
        }

        int baseChance = config.combatRelicBaseChance();
        if (baseChance <= 0) baseChance = 200;

        int rollThreshold = config.enableScalingDropRate()
                ? Math.max(1, baseChance / availableUnlockCount)
                : baseChance;

        log.debug("Attempting combat relic roll. NPC={}, MaxTier={}, Available={}, BaseChance={}, Scaling={}, Threshold=1 in {}",
                npc.getId(), maxTier, availableUnlockCount, baseChance, config.enableScalingDropRate(), rollThreshold);

        if (random.nextInt(rollThreshold) == 0) {
            log.debug("Combat relic base roll SUCCESS (1 in {}) for NPC {}!", rollThreshold, npc.getId());
            SkillTier receivedTier = rollRelicTier(maxTier);
            log.trace("Weighted tier roll result: {}", receivedTier);
            if (receivedTier != null) {
                addRelic(RelicType.COMBAT, receivedTier, RelicSource.COMBAT, deathLocation);
            } else { log.warn("Weighted tier roll returned null for max tier {}", maxTier); }
        } else { log.trace("Combat relic base roll FAILED (1 in {}) for NPC {}.", rollThreshold, npc.getId()); }
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
            switch (clueTierString) {
                case "beginner": return;
                case "easy":       relicTier = SkillTier.APPRENTICE; break;
                case "medium":     relicTier = SkillTier.JOURNEYMAN; break;
                case "hard":       relicTier = SkillTier.EXPERT; break;
                case "elite":      relicTier = SkillTier.MASTER; break;
                case "master":     relicTier = SkillTier.GRANDMASTER; break;
                default:
                    log.warn("Unknown clue scroll tier detected: {}", clueTierString);
                    return;
            }

            int availableUnlockCount = calculateAvailableUnlocks(RelicType.EXPLORATION, relicTier);
            if (availableUnlockCount <= 0) {
                log.debug("No available exploration unlocks for tier {}, no roll performed for {} clue.", relicTier, clueTierString);
                return;
            }

            int baseChance = config.explorationRelicBaseChance();
            if (baseChance <= 0) baseChance = 10;

            int rollThreshold = config.enableScalingDropRate()
                    ? Math.max(1, baseChance / availableUnlockCount)
                    : baseChance;

            log.debug("Attempting exploration relic roll. Clue={}, RelicTier={}, Available={}, BaseChance={}, Scaling={}, Threshold=1 in {}",
                    clueTierString, relicTier, availableUnlockCount, baseChance, config.enableScalingDropRate(), rollThreshold);

            if (random.nextInt(rollThreshold) == 0) {
                log.debug("Exploration relic roll SUCCESS (1 in {}) for {} clue.", rollThreshold, clueTierString);
                addRelic(RelicType.EXPLORATION, relicTier, RelicSource.EXPLORATION, null);
            } else { log.trace("Exploration relic roll FAILED (1 in {}) for {} clue.", rollThreshold, clueTierString); }
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        // Check if the choice overlay handled the click first
        // The event consumption now happens in the mouseClicked listener
        if (relicChoiceOverlay.hasChoices() && config.showChoiceOverlay()) {
            Point mousePos = client.getMouseCanvasPosition();
            // Check if the click originated within the overlay bounds
            if (mousePos != null && relicChoiceOverlay.getBounds() != null && relicChoiceOverlay.getBounds().contains(mousePos.getX(), mousePos.getY())) {
                // If the listener already consumed it, we don't need to do anything more
                if (event.isConsumed()) {
                    log.trace("MenuOptionClicked event already consumed by RelicChoiceOverlay listener.");
                    return;
                }
                // If the listener DIDN'T consume it (e.g., click on background),
                // but it was still inside the overlay, consume it here to prevent game actions.
                log.trace("MenuOptionClicked consuming event because click was within overlay bounds but not handled by a button.");
                event.consume();
                return; // Prevent further processing like item restrictions
            }
        }


        // Only handle Item Restriction Blocking if the choice overlay didn't consume the click
        if (config.warnOnRestrictedItemUse() || config.attemptBlockRestrictedItemUse()) {
            String clickedOption = event.getMenuOption();
            MenuAction clickedAction = event.getMenuAction();

            boolean isEquipAction = clickedOption.equalsIgnoreCase("Wield") || clickedOption.equalsIgnoreCase("Wear");
            boolean isInventoryEquipAction = (clickedAction == MenuAction.CC_OP || clickedAction == MenuAction.CC_OP_LOW_PRIORITY)
                    && event.getWidget() != null
                    && event.getWidget().getParentId() == WidgetInfo.INVENTORY.getGroupId();

            if (isEquipAction || isInventoryEquipAction) {
                int itemIdToCheck = event.getItemId();
                if (itemIdToCheck == -1 && event.getWidget() != null) {
                    itemIdToCheck = event.getWidget().getItemId();
                }

                if (itemIdToCheck != -1 && !isItemAllowed(itemIdToCheck)) {
                    ItemComposition itemComp = itemManager.getItemComposition(itemIdToCheck);
                    String itemName = itemComp != null ? itemComp.getName() : "Unknown Item (ID: " + itemIdToCheck + ")";

                    if (config.warnOnRestrictedItemUse()) {
                        sendChatMessage("Warning: Cannot use restricted item: " + itemName + "!", ChatMessageType.CONSOLE);
                    }
                    if (config.attemptBlockRestrictedItemUse()) {
                        log.debug("Attempting to block use of restricted item: {} (ID: {}) via action: {}", itemName, itemIdToCheck, clickedAction);
                        event.consume();
                    }
                }
            }
        }
    }


    // --- Relic Acquisition Tier Determination ---
    @Nullable
    private SkillTier determineMaxTierFromMinXp(int xpGained) {
        if (xpGained < config.skillingRelicMinXpApp()) return null;

        if (xpGained >= config.skillingRelicMinXpGra()) return SkillTier.GRANDMASTER;
        if (xpGained >= config.skillingRelicMinXpMas()) return SkillTier.MASTER;
        if (xpGained >= config.skillingRelicMinXpExp()) return SkillTier.EXPERT;
        if (xpGained >= config.skillingRelicMinXpJour()) return SkillTier.JOURNEYMAN;
        return SkillTier.APPRENTICE;
    }


    private SkillTier determineMaxTierFromNpcLevel(int npcLevel) {
        if (npcLevel <= 0) return null;
        if (npcLevel > config.combatRelicNpcLevelMas()) return SkillTier.GRANDMASTER;
        if (npcLevel > config.combatRelicNpcLevelExp()) return SkillTier.MASTER;
        if (npcLevel > config.combatRelicNpcLevelJour()) return SkillTier.EXPERT;
        if (npcLevel > config.combatRelicNpcLevelApp()) return SkillTier.JOURNEYMAN;
        return SkillTier.APPRENTICE;
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
        Map<SkillTier, Integer> applicableWeights = new EnumMap<>(SkillTier.class);
        for (SkillTier tier : SkillTier.values()) {
            if (tier == SkillTier.LOCKED || tier.name().startsWith("LEVEL_")) continue;
            if (tier.ordinal() <= maxPossibleTier.ordinal()) {
                int weight = weights.getOrDefault(tier, 0);
                if (weight > 0) {
                    applicableWeights.put(tier, weight);
                    totalWeight += weight;
                }
            }
        }
        if (totalWeight <= 0 || applicableWeights.isEmpty()) {
            log.warn("No valid tiers/weights for rolling up to max tier {}. Defaulting to max.", maxPossibleTier);
            return maxPossibleTier;
        }
        int roll = random.nextInt(totalWeight);
        int cumulativeWeight = 0;
        List<SkillTier> sortedTiers = applicableWeights.keySet().stream()
                .sorted(Comparator.comparing(SkillTier::ordinal))
                .collect(Collectors.toList());
        for (SkillTier tier : sortedTiers) {
            int weight = applicableWeights.get(tier);
            cumulativeWeight += weight;
            if (roll < cumulativeWeight) {
                return tier;
            }
        }
        log.error("Relic tier roll failed unexpectedly. Falling back to max tier.");
        return maxPossibleTier;
    }


    // --- Guice Provider ---
    @Provides
    RelicHunterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(RelicHunterConfig.class);
    }

    // --- REMOVED MouseListener Implementation ---

}