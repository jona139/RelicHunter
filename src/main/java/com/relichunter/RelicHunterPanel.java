// Filename: RelicHunter/src/main/java/com/relichunter/RelicHunterPanel.java
// Content:
package com.relichunter;

import com.relichunter.unlock.UnlockData;
import com.relichunter.unlock.UnlockType;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder; // *** Import TitledBorder ***
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
public class RelicHunterPanel extends PluginPanel {

    private final RelicHunterPlugin plugin;
    private final RelicHunterConfig config;
    private final UnlockManager unlockManager;
    private final ItemManager itemManager;
    private final SpriteManager spriteManager;

    // UI Components
    private final JPanel contentPanel;
    private final JPanel relicCountsDisplayPanel;
    private final JButton activateButton = new JButton("Activate Relic");
    private final JPanel choicePanel = new JPanel();
    private final JButton confirmButton = new JButton("Confirm Selection");
    private final JButton cancelButton = new JButton("Cancel");
    private final PluginErrorPanel errorPanel = new PluginErrorPanel();
    private final JPanel unlockedItemsPanel;
    private final JScrollPane unlockedScrollPane;
    private final JPanel progressionTiersPanel;
    private final JLabel meleeGearTierLabel = new JLabel();
    private final Map<Skill, JLabel> skillTierLabels = new EnumMap<>(Skill.class);
    private final Map<Skill, JLabel> skillIconLabels = new EnumMap<>(Skill.class);
    private final JLabel meleeGearIconLabel = new JLabel();

    // State
    private Unlockable selectedUnlock = null;
    private final List<JButton> choiceButtons = new ArrayList<>();
    private final Border defaultButtonBorder = UIManager.getBorder("Button.border");
    private final Border selectedButtonBorder = BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE, 2);

    // Icons
    private ImageIcon skillingRelicIcon;
    private ImageIcon combatRelicIcon;
    private ImageIcon explorationRelicIcon;
    private ImageIcon lockedIcon;

    private final Map<Map.Entry<RelicType, SkillTier>, JLabel> relicCountLabels = new HashMap<>();


    @Inject
    public RelicHunterPanel(RelicHunterPlugin plugin,
                            RelicHunterConfig config,
                            UnlockManager unlockManager,
                            ItemManager itemManager,
                            SpriteManager spriteManager) {
        super(false);
        this.plugin = plugin;
        this.config = config;
        this.unlockManager = unlockManager;
        this.itemManager = itemManager;
        this.spriteManager = spriteManager;

        loadIcons();

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbcMain = new GridBagConstraints();
        gbcMain.fill = GridBagConstraints.HORIZONTAL;
        gbcMain.weightx = 1.0;
        gbcMain.gridx = 0;
        gbcMain.gridy = 0;
        gbcMain.anchor = GridBagConstraints.NORTHWEST;
        gbcMain.insets = new Insets(0, 0, 10, 0);

        // Title
        JLabel title = new JLabel("Relic Hunter Status");
        title.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
        title.setForeground(Color.WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel.add(title, gbcMain);
        gbcMain.gridy++;
        gbcMain.insets = new Insets(0, 0, 5, 0);

        // Relic Counts
        relicCountsDisplayPanel = createRelicCountPanel();
        contentPanel.add(relicCountsDisplayPanel, gbcMain);
        gbcMain.gridy++;

        // Activation Button
        styleButton(activateButton);
        activateButton.addActionListener(e -> {
            log.debug("Activate Relic button clicked.");
            prepareChoiceArea(); // Prepare the area *before* calling the plugin
            plugin.initiateActivationSequence();
        });
        JPanel activateButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        activateButtonPanel.setBackground(contentPanel.getBackground());
        activateButtonPanel.add(activateButton);
        contentPanel.add(activateButtonPanel, gbcMain);
        gbcMain.gridy++;

        // Choice Panel Setup
        choicePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.Y_AXIS));
        choicePanel.setVisible(false);
        styleButton(confirmButton);
        confirmButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmButton.addActionListener(this::onConfirmClicked);
        styleButton(cancelButton);
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelButton.addActionListener(this::onCancelClicked);
        contentPanel.add(choicePanel, gbcMain);
        gbcMain.gridy++;

        // Unlocked Content Display Setup
        JLabel unlockedTitle = new JLabel("Unlocked Content");
        unlockedTitle.setFont(FontManager.getRunescapeBoldFont());
        unlockedTitle.setForeground(Color.WHITE);
        unlockedTitle.setBorder(new EmptyBorder(10, 0, 5, 0));
        contentPanel.add(unlockedTitle, gbcMain);
        gbcMain.gridy++;
        gbcMain.insets = new Insets(0, 0, 5, 0);

        unlockedItemsPanel = new JPanel();
        unlockedItemsPanel.setLayout(new BoxLayout(unlockedItemsPanel, BoxLayout.Y_AXIS));
        unlockedItemsPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        unlockedItemsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        unlockedScrollPane = new JScrollPane(unlockedItemsPanel);
        unlockedScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        unlockedScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        unlockedScrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
        gbcMain.fill = GridBagConstraints.BOTH;
        gbcMain.weighty = 0.4;
        contentPanel.add(unlockedScrollPane, gbcMain);
        gbcMain.gridy++;
        gbcMain.fill = GridBagConstraints.HORIZONTAL;
        gbcMain.weighty = 0;
        gbcMain.insets = new Insets(0, 0, 10, 0);

        // Progression Tiers Display Setup
        progressionTiersPanel = createProgressionTiersPanel();
        contentPanel.add(progressionTiersPanel, gbcMain);
        gbcMain.gridy++;

        // Error Panel Setup
        errorPanel.setContent("Relic Hunter Helper", "Welcome! Activate a relic to begin.");

        // Filler
        gbcMain.weighty = 1.0;
        gbcMain.fill = GridBagConstraints.VERTICAL;
        contentPanel.add(Box.createVerticalGlue(), gbcMain);

        // Final Panel Assembly
        JScrollPane mainScrollPane = new JScrollPane(contentPanel);
        mainScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainScrollPane.setBorder(null);
        add(mainScrollPane, BorderLayout.CENTER);
    }

    // --- Icon Loading Helper ---
    private void loadIcons() {
        try {
            if (skillingRelicIcon == null) skillingRelicIcon = createPlaceholderIcon("S");
            if (combatRelicIcon == null) combatRelicIcon = createPlaceholderIcon("C");
            if (explorationRelicIcon == null) explorationRelicIcon = createPlaceholderIcon("E");
            lockedIcon = createPlaceholderIcon("X", Color.RED);
        } catch (Exception e) {
            log.error("Failed to load icons", e);
            skillingRelicIcon = createPlaceholderIcon("S");
            combatRelicIcon = createPlaceholderIcon("C");
            explorationRelicIcon = createPlaceholderIcon("E");
            lockedIcon = createPlaceholderIcon("X", Color.RED);
        }
    }

    private ImageIcon createPlaceholderIcon(String text, Color color) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);
        g2d.setFont(FontManager.getRunescapeSmallFont());
        FontMetrics fm = g2d.getFontMetrics();
        int x = (img.getWidth() - fm.stringWidth(text)) / 2;
        int y = ((img.getHeight() - fm.getHeight()) / 2) + fm.getAscent();
        g2d.drawString(text, x, y);
        g2d.dispose();
        return new ImageIcon(img);
    }
    private ImageIcon createPlaceholderIcon(String text) {
        return createPlaceholderIcon(text, ColorScheme.LIGHT_GRAY_COLOR);
    }


    // --- UI Creation Helpers ---

    private JPanel createRelicCountPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(), "Relics Held",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR),
                new EmptyBorder(5, 5, 5, 5)
        ));
        ((javax.swing.border.TitledBorder) ((CompoundBorder) panel.getBorder()).getOutsideBorder()).setTitleColor(ColorScheme.LIGHT_GRAY_COLOR);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(1, 3, 1, 3);

        int gridy = 0;
        for (RelicType type : RelicType.values()) {
            ImageIcon typeIcon = getTypeIcon(type);
            for (SkillTier tier : SkillTier.values()) {
                if (tier == SkillTier.LOCKED || tier.name().startsWith("LEVEL_")) continue;

                gbc.gridx = 0;
                gbc.gridy = gridy;
                gbc.weightx = 0;
                gbc.fill = GridBagConstraints.NONE;
                JLabel iconLabel = new JLabel(typeIcon);
                panel.add(iconLabel, gbc);

                gbc.gridx = 1;
                gbc.weightx = 1.0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                JLabel nameLabel = new JLabel(String.format("%s (%s):", type.name(), tier.getDisplayName()));
                configureCountLabel(nameLabel);
                panel.add(nameLabel, gbc);

                gbc.gridx = 2;
                gbc.weightx = 0;
                gbc.fill = GridBagConstraints.NONE;
                gbc.anchor = GridBagConstraints.EAST;
                JLabel countLabel = new JLabel("0");
                configureCountLabel(countLabel);
                countLabel.setFont(countLabel.getFont().deriveFont(Font.BOLD));
                countLabel.setBorder(new EmptyBorder(0, 5, 0, 0));
                panel.add(countLabel, gbc);

                relicCountLabels.put(Map.entry(type, tier), countLabel);
                gridy++;
            }
            if (type != RelicType.values()[RelicType.values().length - 1]) {
                gbc.gridx = 0;
                gbc.gridy = gridy;
                gbc.gridwidth = 3;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = new Insets(3, 0, 3, 0);
                panel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
                gbc.gridwidth = 1;
                gbc.insets = new Insets(1, 3, 1, 3);
                gridy++;
            }
        }
        return panel;
    }

    private ImageIcon getTypeIcon(RelicType type) {
        switch (type) {
            case SKILLING: return skillingRelicIcon;
            case COMBAT: return combatRelicIcon;
            case EXPLORATION: return explorationRelicIcon;
            default: return createPlaceholderIcon("?");
        }
    }

    private void configureCountLabel(JLabel label) {
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setFont(FontManager.getRunescapeSmallFont());
    }

    // --- Progression Tiers Panel ---
    private JPanel createProgressionTiersPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(), "Progression Tiers",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR),
                new EmptyBorder(5, 5, 5, 5)
        ));
        ((javax.swing.border.TitledBorder) ((CompoundBorder) panel.getBorder()).getOutsideBorder()).setTitleColor(ColorScheme.LIGHT_GRAY_COLOR);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(1, 3, 1, 3);
        gbc.gridy = 0;

        // Melee Gear Tier
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        meleeGearIconLabel.setIcon(createPlaceholderIcon("G"));
        panel.add(meleeGearIconLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        configureLabel(meleeGearTierLabel);
        meleeGearTierLabel.setText("Melee Gear: Loading...");
        panel.add(meleeGearTierLabel, gbc);
        gbc.gridy++;

        // Skills
        for (Skill skill : Skill.values()) {
            if (skill == Skill.OVERALL || skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE || skill == Skill.HITPOINTS) continue;

            gbc.gridx = 0;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            JLabel skillIconLabel = new JLabel();
            skillIconLabels.put(skill, skillIconLabel);
            loadSkillIconAsync(skillIconLabel, skill);
            panel.add(skillIconLabel, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            JLabel label = new JLabel(skill.getName() + ": Loading...");
            configureLabel(label);
            skillTierLabels.put(skill, label);
            panel.add(label, gbc);
            gbc.gridy++;
        }
        return panel;
    }

    private void loadSkillIconAsync(JLabel label, Skill skill) {
        spriteManager.getSpriteAsync(SkillIconManager.getSkillSpriteId(skill), 0, img -> {
            if (img != null) {
                SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(ImageUtil.resizeImage(img, 16, 16))));
            } else {
                SwingUtilities.invokeLater(() -> label.setIcon(createPlaceholderIcon(skill.getName().substring(0,1))));
            }
        });
    }

    private void configureLabel(JLabel label) {
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setFont(FontManager.getRunescapeSmallFont());
    }

    // --- UI Update Methods ---
    public void updateRelicCounts() {
        SwingUtilities.invokeLater(() -> {
            if (config == null || plugin == null) {
                log.warn("Config or Plugin is null, cannot update relic counts.");
                return;
            }

            boolean canActivateAny = false;

            for (RelicType type : RelicType.values()) {
                for (SkillTier tier : SkillTier.values()) {
                    if (tier == SkillTier.LOCKED || tier.name().startsWith("LEVEL_")) continue;

                    Map.Entry<RelicType, SkillTier> key = Map.entry(type, tier);
                    int count = plugin.getRelicCount(type, tier);
                    if (count > 0) {
                        canActivateAny = true;
                    }
                    JLabel label = relicCountLabels.get(key);
                    if (label != null) {
                        label.setText(String.valueOf(count));
                        label.setForeground(count > 0 ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
                    }
                }
            }

            activateButton.setEnabled(canActivateAny);
            log.trace("Activate button enabled state set to: {}", canActivateAny);
        });
    }

    // Removed getAvailableRelicCounts

    public void updateUnlockedDisplay() {
        SwingUtilities.invokeLater(() -> {
            unlockedItemsPanel.removeAll();

            if (config == null || unlockManager == null) {
                JLabel errorLabel = new JLabel("Error: Config or UnlockManager not loaded.");
                configureLabel(errorLabel);
                unlockedItemsPanel.add(errorLabel);
            } else {
                Set<String> unlockedIds = config.unlockedRelics();
                if (unlockedIds.isEmpty()) {
                    JLabel noneLabel = new JLabel("  None");
                    configureLabel(noneLabel);
                    unlockedItemsPanel.add(noneLabel);
                } else {
                    unlockedIds.stream()
                            .map(unlockManager::getUnlockById)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .sorted(Comparator.comparing(UnlockData::getName))
                            .forEach(this::addUnlockedItemEntry);
                }
            }
            unlockedItemsPanel.revalidate();
            unlockedItemsPanel.repaint();
            SwingUtilities.invokeLater(() -> {
                JScrollBar verticalScrollBar = unlockedScrollPane.getVerticalScrollBar();
                if (verticalScrollBar != null) {
                    verticalScrollBar.setValue(verticalScrollBar.getMinimum());
                }
            });
        });
    }

    private void addUnlockedItemEntry(UnlockData unlock) {
        JPanel entryPanel = new JPanel(new BorderLayout(5, 0));
        entryPanel.setBackground(unlockedItemsPanel.getBackground());
        entryPanel.setOpaque(false);

        JLabel iconLabel = new JLabel();
        setUnlockIcon(iconLabel, unlock);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        entryPanel.add(iconLabel, BorderLayout.WEST);

        JLabel nameLabel = new JLabel("<html><body style='width: " + (PluginPanel.PANEL_WIDTH - 70) + "px'>" + unlock.getName() + "</body></html>");
        nameLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        nameLabel.setFont(FontManager.getRunescapeSmallFont());
        nameLabel.setToolTipText(unlock.getDescription());
        nameLabel.setVerticalAlignment(SwingConstants.CENTER);
        entryPanel.add(nameLabel, BorderLayout.CENTER);

        entryPanel.setBorder(new EmptyBorder(2, 0, 2, 0));

        unlockedItemsPanel.add(entryPanel);
    }


    public void updateProgressionTiersDisplay() {
        SwingUtilities.invokeLater(() -> {
            if (config == null || plugin == null) {
                log.warn("Config or Plugin is null, cannot update progression tiers.");
                meleeGearTierLabel.setText("Melee Gear: Error");
                skillTierLabels.values().forEach(label -> label.setText("Error"));
                return;
            }
            try {
                GearTier meleeTier = config.meleeGearTier();
                meleeGearTierLabel.setText(String.format("Melee Gear: %s", meleeTier.getDisplayName()));
                meleeGearTierLabel.setToolTipText(meleeTier.getDescription());
                meleeGearTierLabel.setForeground(getTierColor(meleeTier));

                BufferedImage meleeIconImage = itemManager.getImage(ItemID.RUNE_PLATEBODY, 1, false);
                if (meleeIconImage != null) {
                    SwingUtilities.invokeLater(() -> meleeGearIconLabel.setIcon(new ImageIcon(ImageUtil.resizeImage(meleeIconImage, 16, 16))));
                } else {
                    SwingUtilities.invokeLater(() -> meleeGearIconLabel.setIcon(createPlaceholderIcon("G")));
                }


                for (Skill skill : skillTierLabels.keySet()) {
                    JLabel label = skillTierLabels.get(skill);
                    SkillTier tier = plugin.getSkillTier(skill);
                    label.setText(String.format("%s: %s (%d)", skill.getName(), tier.getDisplayName(), tier.getLevelCap()));
                    label.setForeground(tier == SkillTier.LOCKED ? ColorScheme.DARK_GRAY_COLOR.darker() : getTierColor(tier));

                    JLabel iconLabel = skillIconLabels.get(skill);
                    if (tier == SkillTier.LOCKED && iconLabel != null) {
                        iconLabel.setIcon(lockedIcon);
                    } else if (iconLabel != null && iconLabel.getIcon() == lockedIcon) {
                        loadSkillIconAsync(iconLabel, skill);
                    }
                }
                progressionTiersPanel.revalidate();
                progressionTiersPanel.repaint();
            } catch (Exception e) {
                log.error("Unexpected error during updateProgressionTiersDisplay", e);
                meleeGearTierLabel.setText("Melee Gear: Error");
                skillTierLabels.values().forEach(label -> label.setText("Error"));
            }
        });
    }

    // Helper to get color based on SkillTier
    private Color getTierColor(SkillTier tier) {
        switch (tier) {
            case LOCKED: return ColorScheme.DARK_GRAY_COLOR.darker();
            case APPRENTICE: case LEVEL_10: case LEVEL_20: return ColorScheme.PROGRESS_ERROR_COLOR;
            case JOURNEYMAN: case LEVEL_30: case LEVEL_40: return ColorScheme.LIGHT_GRAY_COLOR;
            case EXPERT: case LEVEL_50: case LEVEL_60: return ColorScheme.BRAND_ORANGE_TRANSPARENT;
            case MASTER: case LEVEL_70: case LEVEL_80: return ColorScheme.PROGRESS_INPROGRESS_COLOR;
            case GRANDMASTER: case LEVEL_90: return ColorScheme.PROGRESS_COMPLETE_COLOR;
            default: return ColorScheme.LIGHT_GRAY_COLOR;
        }
    }
    // Overload for GearTier
    private Color getTierColor(GearTier tier) {
        switch (tier) {
            case NONE: return ColorScheme.DARK_GRAY_COLOR.darker();
            case STEEL: return ColorScheme.LIGHT_GRAY_COLOR;
            case MITHRIL: return ColorScheme.BRAND_ORANGE_TRANSPARENT;
            case ADAMANT: return Color.GREEN.darker();
            case RUNE: return ColorScheme.PROGRESS_INPROGRESS_COLOR;
            case GRANITE: return Color.CYAN.darker();
            case DRAGON: return Color.RED;
            case ABYSSAL_BARROWS: return Color.MAGENTA;
            case GODSWORD: return Color.ORANGE;
            case HIGH_TIER: return Color.WHITE;
            default: return ColorScheme.LIGHT_GRAY_COLOR;
        }
    }


    // --- Choice/Error Display ---
    public void displayChoices(final List<Unlockable> choices) {
        SwingUtilities.invokeLater(() -> {
            prepareChoiceArea();
            if (choices == null || choices.isEmpty()) {
                displayError("No valid unlocks available for this relic type.");
                return;
            }

            JLabel chooseTitle = new JLabel("Select Your Unlock:");
            chooseTitle.setFont(FontManager.getRunescapeSmallFont());
            chooseTitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            chooseTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
            choicePanel.add(chooseTitle);
            choicePanel.add(Box.createRigidArea(new Dimension(0, 8)));

            for (Unlockable choice : choices) {
                JButton choiceButton = new JButton();
                styleChoiceButton(choiceButton, choice);
                choiceButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                choiceButton.addActionListener(e -> onChoiceSelected(choice, choiceButton));
                choiceButtons.add(choiceButton);
                choicePanel.add(choiceButton);
                choicePanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
            choicePanel.add(Box.createRigidArea(new Dimension(0, 10)));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            buttonPanel.setBackground(choicePanel.getBackground());
            buttonPanel.add(confirmButton);
            buttonPanel.add(cancelButton);
            buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            buttonPanel.setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH, confirmButton.getPreferredSize().height + 5));

            choicePanel.add(buttonPanel);
            confirmButton.setEnabled(false);
            choicePanel.setVisible(true);
            choicePanel.revalidate();
            choicePanel.repaint();
            contentPanel.revalidate();
            contentPanel.repaint();
        });
    }

    private void styleChoiceButton(JButton button, Unlockable choice) {
        button.setLayout(new BorderLayout(5, 0));
        button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        button.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 40, 50));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR),
                new EmptyBorder(5, 10, 5, 10)
        ));
        button.setOpaque(true);
        button.setFocusPainted(false);

        JLabel iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(20, 20));
        Optional<UnlockData> dataOpt = unlockManager.getUnlockById(choice.getId());
        if (dataOpt.isPresent()) {
            setUnlockIcon(iconLabel, dataOpt.get());
        } else {
            iconLabel.setIcon(createPlaceholderIcon("?"));
        }
        button.add(iconLabel, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel(choice.getName());
        nameLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
        nameLabel.setForeground(Color.WHITE);
        textPanel.add(nameLabel);

        JLabel descLabel = new JLabel(choice.getDescription());
        descLabel.setFont(FontManager.getRunescapeSmallFont().deriveFont(10f));
        descLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        textPanel.add(descLabel);

        button.add(textPanel, BorderLayout.CENTER);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                }
            }
        });
    }

    // Helper to set icon based on UnlockData (consolidated logic)
    private void setUnlockIcon(JLabel label, UnlockData unlock) {
        switch (unlock.getCategory()) {
            case SKILL_TIER:
                String skillName = unlock.getId().split("_")[1];
                try {
                    Skill skill = Skill.valueOf(skillName);
                    loadSkillIconAsync(label, skill); // Use helper
                } catch (Exception e) { label.setIcon(createPlaceholderIcon("Sk")); }
                break;
            case GEAR_TIER:
            case SPECIFIC_ITEM:
                if (unlock.getItemIds() != null && !unlock.getItemIds().isEmpty()) {
                    int itemId = unlock.getItemIds().iterator().next();
                    BufferedImage itemImage = itemManager.getImage(itemId, 1, false);
                    if (itemImage != null) {
                        SwingUtilities.invokeLater(() -> label.setIcon(new ImageIcon(ImageUtil.resizeImage(itemImage, 16, 16))));
                    } else {
                        SwingUtilities.invokeLater(() -> label.setIcon(createPlaceholderIcon(unlock.getCategory() == UnlockType.GEAR_TIER ? "G" : "It")));
                    }
                } else {
                    label.setIcon(createPlaceholderIcon(unlock.getCategory() == UnlockType.GEAR_TIER ? "G" : "It"));
                }
                break;
            case AREA: label.setIcon(createPlaceholderIcon("A")); break;
            case QUEST: label.setIcon(createPlaceholderIcon("Q")); break;
            case MECHANIC: label.setIcon(createPlaceholderIcon("M")); break;
            case BOSS: label.setIcon(createPlaceholderIcon("B")); break;
            default: label.setIcon(createPlaceholderIcon("?")); break;
        }
    }


    public void displayError(String message) {
        SwingUtilities.invokeLater(() -> {
            prepareChoiceArea();
            errorPanel.setContent("Activation Error", message);
            choicePanel.add(errorPanel);
            choicePanel.setVisible(true);
            choicePanel.revalidate();
            choicePanel.repaint();
            contentPanel.revalidate();
            contentPanel.repaint();
        });
    }

    private void prepareChoiceArea() {
        choicePanel.removeAll();
        // *** FIXED: Corrected border creation and title setting ***
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Choose Your Unlock",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                FontManager.getRunescapeSmallFont(),
                ColorScheme.LIGHT_GRAY_COLOR);
        // Set title color on the TitledBorder instance
        titledBorder.setTitleColor(ColorScheme.LIGHT_GRAY_COLOR);

        choicePanel.setBorder(BorderFactory.createCompoundBorder(
                titledBorder, // Use the TitledBorder we just created and configured
                new EmptyBorder(10, 5, 10, 5) // Inner padding
        ));
        // *** Removed the incorrect cast and setTitleColor call here ***
        // ((javax.swing.border.TitledBorder) choicePanel.getBorder()).setTitleColor(ColorScheme.LIGHT_GRAY_COLOR);
        choiceButtons.clear();
        selectedUnlock = null;
    }

    public void clearChoiceDisplay() {
        SwingUtilities.invokeLater(() -> {
            choicePanel.removeAll();
            choicePanel.setVisible(false);
            choiceButtons.clear();
            selectedUnlock = null;
            choicePanel.revalidate();
            choicePanel.repaint();
            contentPanel.revalidate();
            contentPanel.repaint();
        });
    }

    // --- Action Handlers ---
    private void onChoiceSelected(Unlockable choice, JButton clickedButton) {
        selectedUnlock = choice;
        for (JButton btn : choiceButtons) {
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(btn == clickedButton ? ColorScheme.BRAND_ORANGE : ColorScheme.DARK_GRAY_COLOR, btn == clickedButton ? 2 : 1),
                    new EmptyBorder(5, 10, 5, 10)
            ));
            btn.setBackground(btn == clickedButton ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
        }
        confirmButton.setEnabled(true);
    }

    private void onConfirmClicked(ActionEvent event) {
        if (selectedUnlock != null) {
            plugin.completeRelicActivation(selectedUnlock);
            clearChoiceDisplay();
        } else {
            log.warn("Confirm clicked but no selection was made.");
        }
    }

    private void onCancelClicked(ActionEvent event) {
        clearChoiceDisplay();
    }

    // Helper to style main buttons
    private void styleButton(JButton button) {
        button.setFont(FontManager.getRunescapeSmallFont());
        button.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR, 1),
                new EmptyBorder(5, 10, 5, 10)
        ));
        button.setFocusPainted(false);

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
                }
            }
        });
    }

    // --- Shutdown ---
    public void shutdown() {
        log.debug("Relic Hunter Panel shutting down.");
    }

    // Static inner class for Skill Icon mapping
    private static class SkillIconManager {
        private static final Map<Skill, Integer> skillSpriteIds = new EnumMap<>(Skill.class);
        static {
            skillSpriteIds.put(Skill.ATTACK, SpriteID.SKILL_ATTACK);
            skillSpriteIds.put(Skill.STRENGTH, SpriteID.SKILL_STRENGTH);
            skillSpriteIds.put(Skill.DEFENCE, SpriteID.SKILL_DEFENCE);
            skillSpriteIds.put(Skill.RANGED, SpriteID.SKILL_RANGED);
            skillSpriteIds.put(Skill.PRAYER, SpriteID.SKILL_PRAYER);
            skillSpriteIds.put(Skill.MAGIC, SpriteID.SKILL_MAGIC);
            skillSpriteIds.put(Skill.RUNECRAFT, SpriteID.SKILL_RUNECRAFT);
            skillSpriteIds.put(Skill.CONSTRUCTION, SpriteID.SKILL_CONSTRUCTION);
            skillSpriteIds.put(Skill.HITPOINTS, SpriteID.SKILL_HITPOINTS);
            skillSpriteIds.put(Skill.AGILITY, SpriteID.SKILL_AGILITY);
            skillSpriteIds.put(Skill.HERBLORE, SpriteID.SKILL_HERBLORE);
            skillSpriteIds.put(Skill.THIEVING, SpriteID.SKILL_THIEVING);
            skillSpriteIds.put(Skill.CRAFTING, SpriteID.SKILL_CRAFTING);
            skillSpriteIds.put(Skill.FLETCHING, SpriteID.SKILL_FLETCHING);
            skillSpriteIds.put(Skill.SLAYER, SpriteID.SKILL_SLAYER);
            skillSpriteIds.put(Skill.HUNTER, SpriteID.SKILL_HUNTER);
            skillSpriteIds.put(Skill.MINING, SpriteID.SKILL_MINING);
            skillSpriteIds.put(Skill.SMITHING, SpriteID.SKILL_SMITHING);
            skillSpriteIds.put(Skill.FISHING, SpriteID.SKILL_FISHING);
            skillSpriteIds.put(Skill.COOKING, SpriteID.SKILL_COOKING);
            skillSpriteIds.put(Skill.FIREMAKING, SpriteID.SKILL_FIREMAKING);
            skillSpriteIds.put(Skill.WOODCUTTING, SpriteID.SKILL_WOODCUTTING);
            skillSpriteIds.put(Skill.FARMING, SpriteID.SKILL_FARMING);
        }
        public static int getSkillSpriteId(Skill skill) {
            return skillSpriteIds.getOrDefault(skill, SpriteID.GE_SEARCH);
        }
    }
}
