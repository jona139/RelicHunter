// Filename: RelicHunter/src/main/java/com/relichunter/RelicHunterPlugin.java
// Content:
package com.relichunter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.relichunter.data.ItemDataRoot;
import com.relichunter.data.TierData;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread; // Import ClientThread
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
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
import java.util.stream.Collectors;


@Slf4j
@PluginDescriptor(
        name = "Relic Hunter Helper",
        description = "Helps track progress and restrictions for the Relic Hunter Ironman mode.",
        tags = {"ironman", "relic", "hunter", "helper", "restriction", "unique"}
)
public class RelicHunterPlugin extends Plugin
{
    static final String CONFIG_GROUP = "relichunter";
    private static final String ITEM_DATA_JSON_PATH = "/relic_hunter_item_data.json";

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

    // Plugin State
    private RelicHunterPanel panel;
    private NavigationButton navButton;
    private final Random random = new Random();
    private boolean initialPanelUpdateDone = false;
    private Map<GearTier, Map<String, Set<Integer>>> loadedGearData = new EnumMap<>(GearTier.class);


    @Override
    protected void startUp() throws Exception {
        log.info("Relic Hunter Helper starting up...");
        loadItemData();
        panel = new RelicHunterPanel(this, config);
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
        log.info("Relic Hunter Helper startup complete.");
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Relic Hunter Helper stopped!");
        overlayManager.remove(itemRestrictionOverlay);
        overlayManager.remove(skillTabOverlay);
        loadedGearData.clear();
        clientToolbar.removeNavigation(navButton);
        if (panel != null) { panel.shutdown(); }
        panel = null;
        navButton = null;
    }

    /**
     * Loads and parses the item restriction data from the JSON file.
     */
    private void loadItemData() {
        log.info("Loading item restriction data from JSON...");
        loadedGearData = new EnumMap<>(GearTier.class);
        try (InputStream is = getClass().getResourceAsStream(ITEM_DATA_JSON_PATH)) {
            if (is == null) {
                log.error("Could not find item data JSON file at path: {}", ITEM_DATA_JSON_PATH);
                return;
            }
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            Type dataType = new TypeToken<ItemDataRoot>(){}.getType();
            ItemDataRoot parsedData = gson.fromJson(reader, dataType);

            if (parsedData != null && parsedData.getGearTiers() != null) {
                parsedData.getGearTiers().forEach((tierName, tierContent) -> {
                    GearTier gearTier = GearTier.fromEnumName(tierName);
                    if (gearTier != GearTier.NONE) {
                        Map<String, Set<Integer>> categoryMap = new HashMap<>();
                        categoryMap.put("weapons", tierContent.getWeapons() != null ? tierContent.getWeapons() : Collections.emptySet());
                        categoryMap.put("armour", tierContent.getArmour() != null ? tierContent.getArmour() : Collections.emptySet());
                        loadedGearData.put(gearTier, categoryMap);
                    } else {
                        log.warn("Skipping unknown gear tier found in JSON: {}", tierName);
                    }
                });
                log.info("Successfully loaded item data for {} gear tiers.", loadedGearData.size());
            } else {
                log.error("Parsed item data or gearTiers map was null.");
            }
        } catch (Exception e) {
            log.error("Error loading or parsing item data JSON", e);
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
                clearChoiceArea();
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

        List<Unlockable> potentialUnlocks = UnlockManager.getUnlocksByTypeAndTier(type, tier);
        if (potentialUnlocks.isEmpty()) { if (panel != null) panel.displayError("Internal error: No unlocks defined"); return; }

        Set<String> currentlyUnlocked = config.unlockedRelics();
        List<Unlockable> validUnlocks = potentialUnlocks.stream()
                .filter(unlock -> !currentlyUnlocked.contains(unlock.getId()))
                .filter(unlock -> checkPrerequisites(unlock, currentlyUnlocked))
                .collect(Collectors.toList());

        log.info("Found {} valid unlocks after filtering.", validUnlocks.size());
        if (validUnlocks.isEmpty()) { if (panel != null) panel.displayError("No new unlocks available for this relic"); return; }

        Collections.shuffle(validUnlocks, random);
        List<Unlockable> choices = validUnlocks.stream().limit(3).collect(Collectors.toList());
        if (panel != null) panel.displayChoices(choices);
    }

    private boolean checkPrerequisites(Unlockable unlock, Set<String> currentlyUnlockedIds) {
        return unlock.getPrerequisiteIds().isEmpty() || currentlyUnlockedIds.containsAll(unlock.getPrerequisiteIds());
    }

    public void completeRelicActivation(Unlockable chosenUnlock) {
        log.info("Completing activation for: {}", chosenUnlock.getName());
        RelicType consumedRelicType = chosenUnlock.getType();
        SkillTier consumedRelicTier = chosenUnlock.getTargetTier();
        if (getRelicCount(consumedRelicType, consumedRelicTier) <= 0) { if (panel != null) panel.displayError("Internal Error: Relic count mismatch"); return; }

        decrementRelicCount(consumedRelicType, consumedRelicTier);

        Set<String> currentUnlocked = config.unlockedRelics();
        Set<String> newUnlockedSet = new HashSet<>(currentUnlocked);

        boolean isGearTierUnlock = false;
        for (GearTier gearTier : GearTier.values()) {
            if (gearTier != GearTier.NONE && chosenUnlock.getId().equals(UnlockManager.getGearTierId(gearTier))) {
                setMeleeGearTier(gearTier); isGearTierUnlock = true; break;
            }
        }
        if (!isGearTierUnlock) {
            // Loop excludes ATTACK, STRENGTH, DEFENCE, HITPOINTS, OVERALL
            for (Skill skill : Skill.values()) {
                if (skill == Skill.OVERALL || skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE || skill == Skill.HITPOINTS) continue;
                for (SkillTier tierValue : SkillTier.values()) {
                    if (tierValue != SkillTier.LOCKED && chosenUnlock.getId().equals(UnlockManager.getSkillTierId(skill, tierValue))) {
                        setSkillTier(skill, tierValue); break;
                    }
                }
            }
        }

        newUnlockedSet.add(chosenUnlock.getId());
        configManager.setConfiguration(CONFIG_GROUP, "unlockedRelics", newUnlockedSet);

        String message = "Relic consumed. Unlocked: " + chosenUnlock.getName() + "!";
        sendChatMessage(message);
    }


    // --- Helper Methods ---

    public boolean isMeleeItemAllowed(int itemId) {
        GearTier allowedTier = getMeleeGearTier();
        for (GearTier checkTier : GearTier.values()) {
            if (checkTier.ordinal() > allowedTier.ordinal()) continue;
            if (checkTier == GearTier.NONE) continue;
            Map<String, Set<Integer>> tierCategories = loadedGearData.get(checkTier);
            if (tierCategories != null) {
                if ((tierCategories.get("weapons") != null && tierCategories.get("weapons").contains(itemId)) ||
                        (tierCategories.get("armour") != null && tierCategories.get("armour").contains(itemId))) {
                    return true;
                }
            }
        }
        for (GearTier checkTier : GearTier.values()) {
            if (checkTier.ordinal() > allowedTier.ordinal()) {
                Map<String, Set<Integer>> tierCategories = loadedGearData.get(checkTier);
                if (tierCategories != null) {
                    if ((tierCategories.get("weapons") != null && tierCategories.get("weapons").contains(itemId)) ||
                            (tierCategories.get("armour") != null && tierCategories.get("armour").contains(itemId))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private int getRelicCount(RelicType type, SkillTier tier) {
        switch (type) {
            case SKILLING:
                switch (tier) {
                    case APPRENTICE: return config.skillingRelicsApprentice();
                    case JOURNEYMAN: return config.skillingRelicsJourneyman();
                    default: return 0;
                }
            case COMBAT:
                switch (tier) {
                    case APPRENTICE: return config.combatRelicsApprentice();
                    case JOURNEYMAN: return config.combatRelicsJourneyman();
                    default: return 0;
                }
            case EXPLORATION:
                switch (tier) {
                    case APPRENTICE: return config.explorationRelicsApprentice();
                    case JOURNEYMAN: return config.explorationRelicsJourneyman();
                    default: return 0;
                }
            default: return 0;
        }
    }

    private Map<RelicType, Map<SkillTier, Integer>> getAvailableRelicCounts() {
        Map<RelicType, Map<SkillTier, Integer>> counts = new EnumMap<>(RelicType.class);
        List<SkillTier> tiersToCheck = List.of(SkillTier.APPRENTICE, SkillTier.JOURNEYMAN); // TODO: Expand
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
                } break;
            case COMBAT:
                switch (tier) {
                    case APPRENTICE: return "combatRelicsApprentice";
                    case JOURNEYMAN: return "combatRelicsJourneyman";
                } break;
            case EXPLORATION:
                switch (tier) {
                    case APPRENTICE: return "explorationRelicsApprentice";
                    case JOURNEYMAN: return "explorationRelicsJourneyman";
                } break;
        }
        return null;
    }

    private void sendChatMessage(String message) {
        if (client.getGameState() != GameState.LOGGED_IN) return;
        Player localPlayer = client.getLocalPlayer();
        String playerName = (localPlayer != null) ? localPlayer.getName() : "RelicHunter";
        if (playerName == null) playerName = "RelicHunter";

        final String finalPlayerName = playerName;
        clientThread.invoke(() -> {
            if (client.getGameState() == GameState.LOGGED_IN) {
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, finalPlayerName, message, null);
                log.info("Chat message sent via ClientThread: {}", message);
            } else {
                log.warn("ClientThread invoked, but player no longer logged in. Message not sent.");
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
                configManager.setConfiguration(CONFIG_GROUP, "skillingRelicsApprentice", 0);
                configManager.setConfiguration(CONFIG_GROUP, "skillingRelicsJourneyman", 0);
                configManager.setConfiguration(CONFIG_GROUP, "combatRelicsApprentice", 0);
                configManager.setConfiguration(CONFIG_GROUP, "combatRelicsJourneyman", 0);
                configManager.setConfiguration(CONFIG_GROUP, "explorationRelicsApprentice", 0);
                configManager.setConfiguration(CONFIG_GROUP, "explorationRelicsJourneyman", 0);
                setMeleeGearTier(GearTier.STEEL); // Reset to STEEL
                for (Skill skill : Skill.values()) {
                    if (skill == Skill.OVERALL || skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE || skill == Skill.HITPOINTS) continue;
                    SkillTier defaultTier = SkillTier.LOCKED;
                    if (skill == Skill.MINING || skill == Skill.SMITHING) {
                        defaultTier = SkillTier.APPRENTICE;
                    }
                    setSkillTier(skill, defaultTier);
                }
                configManager.setConfiguration(CONFIG_GROUP, "resetProgressionButton", false);
                sendChatMessage("Relic Hunter progression has been reset.");
            } else {
                configManager.setConfiguration(CONFIG_GROUP, "resetProgressionButton", false);
            }
        });
    }

    public SkillTier getSkillTier(Skill skill) {
        // Return GRANDMASTER for melee/hp as they aren't level capped by SkillTier
        if (skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE || skill == Skill.HITPOINTS) {
            return SkillTier.GRANDMASTER;
        }
        // Return configured tier for other skills
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
            default: return SkillTier.LOCKED; // Default for OVERALL or any unexpected skill
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
            // Excluded Atk, Str, Def, HP
            default: return;
        }
        configManager.setConfiguration(CONFIG_GROUP, key, tier);
    }

    public GearTier getMeleeGearTier() { return config.meleeGearTier(); }
    private void setMeleeGearTier(GearTier tier) { configManager.setConfiguration(CONFIG_GROUP, "meleeGearTier", tier); }
    private void clearChoiceArea() { if (panel != null) panel.clearChoiceDisplay(); }


    // --- Event Subscribers ---
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
            if (event.getKey().equals("unlockedRelics")) panel.updateUnlockedDisplay();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (client.getGameState() == GameState.LOGGED_IN) {
            if (!initialPanelUpdateDone && panel != null) {
                log.info("Performing initial panel updates on first login.");
                panel.updateRelicCounts(); panel.updateUnlockedDisplay(); panel.updateProgressionTiersDisplay();
                initialPanelUpdateDone = true;
            } else if (panel != null) {
                panel.clearChoiceDisplay();
            }
        } else if (client.getGameState() == GameState.LOGIN_SCREEN || client.getGameState() == GameState.HOPPING) {
            initialPanelUpdateDone = false;
        }
    }

    // --- Guice Provider ---
    @Provides
    RelicHunterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(RelicHunterConfig.class);
    }
}