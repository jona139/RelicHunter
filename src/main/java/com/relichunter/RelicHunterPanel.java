// Filename: RelicHunter/src/main/java/com/relichunter/RelicHunterPanel.java
// Content:
package com.relichunter;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class RelicHunterPanel extends PluginPanel {

    private final RelicHunterPlugin plugin;
    private final RelicHunterConfig config;

    // UI Components
    private final JPanel contentPanel;
    private final JLabel skillingRelicsApprenticeLabel = new JLabel();
    private final JLabel skillingRelicsJourneymanLabel = new JLabel();
    private final JLabel combatRelicsApprenticeLabel = new JLabel();
    private final JLabel combatRelicsJourneymanLabel = new JLabel();
    private final JLabel explorationRelicsApprenticeLabel = new JLabel();
    private final JLabel explorationRelicsJourneymanLabel = new JLabel();
    private final JButton activateButton = new JButton("Activate Relic");
    private final JPanel choicePanel = new JPanel();
    private final JButton confirmButton = new JButton("Confirm Selection");
    private final JButton cancelButton = new JButton("Cancel");
    private final PluginErrorPanel errorPanel = new PluginErrorPanel();
    private final JTextArea unlockedRelicsArea = new JTextArea();
    private final JScrollPane unlockedScrollPane;
    private final JPanel progressionTiersPanel;
    private final JLabel meleeGearTierLabel = new JLabel();
    private final Map<Skill, JLabel> skillTierLabels = new EnumMap<>(Skill.class);

    // State
    private Unlockable selectedUnlock = null;
    private final List<JButton> choiceButtons = new ArrayList<>();
    private final Border defaultButtonBorder = UIManager.getBorder("Button.border");
    private final Border selectedButtonBorder = BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE, 2);


    // Constructor
    public RelicHunterPanel(RelicHunterPlugin plugin, RelicHunterConfig config) {
        super(false);
        this.plugin = plugin;
        this.config = config;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Title
        JLabel title = new JLabel("Relic Hunter Status");
        title.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 16f));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(title);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Relic Counts
        JPanel relicCountsPanel = createRelicCountPanel();
        relicCountsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(relicCountsPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Activation Button
        activateButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        SwingUtil.removeButtonDecorations(activateButton);
        activateButton.addActionListener(e -> {
            prepareChoiceArea(); // Clear UI before calling plugin
            plugin.initiateActivationSequence();
        });
        contentPanel.add(activateButton);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Choice Panel Setup
        choicePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.Y_AXIS));
        choicePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        choicePanel.setVisible(false); // Start hidden
        SwingUtil.removeButtonDecorations(confirmButton);
        confirmButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmButton.addActionListener(this::onConfirmClicked);
        SwingUtil.removeButtonDecorations(cancelButton);
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelButton.addActionListener(this::onCancelClicked);
        contentPanel.add(choicePanel);

        // Unlocked Relics Display Setup
        JLabel unlockedTitle = new JLabel("Unlocked Content");
        unlockedTitle.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD));
        unlockedTitle.setForeground(Color.WHITE);
        unlockedTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        unlockedTitle.setBorder(new EmptyBorder(10, 0, 5, 0));
        unlockedRelicsArea.setEditable(false);
        unlockedRelicsArea.setLineWrap(true);
        unlockedRelicsArea.setWrapStyleWord(true);
        unlockedRelicsArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        unlockedRelicsArea.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        unlockedRelicsArea.setFont(UIManager.getFont("Label.font"));
        unlockedRelicsArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        unlockedScrollPane = new JScrollPane(unlockedRelicsArea);
        unlockedScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        unlockedScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        unlockedScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        unlockedScrollPane.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 150));
        contentPanel.add(unlockedTitle);
        contentPanel.add(unlockedScrollPane);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Progression Tiers Display Setup
        progressionTiersPanel = createProgressionTiersPanel();
        progressionTiersPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(progressionTiersPanel);

        // Error Panel Setup
        errorPanel.setContent("Relic Hunter Helper", "Welcome! Activate a relic to begin.");
        errorPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Final Panel Assembly
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        wrapperPanel.add(contentPanel, BorderLayout.NORTH);
        add(wrapperPanel, BorderLayout.CENTER); // Add wrapper to the scrollable PluginPanel
    }

    // --- UI Creation Helpers ---
    private JPanel createRelicCountPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 3));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(), "Relics Held",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        UIManager.getFont("Label.font"), ColorScheme.LIGHT_GRAY_COLOR),
                new EmptyBorder(5, 5, 5, 5)
        ));
        ((javax.swing.border.TitledBorder) ((CompoundBorder) panel.getBorder()).getOutsideBorder()).setTitleColor(ColorScheme.LIGHT_GRAY_COLOR);

        panel.add(new JLabel("Skilling (Apprentice):")); panel.add(skillingRelicsApprenticeLabel);
        panel.add(new JLabel("Skilling (Journeyman):")); panel.add(skillingRelicsJourneymanLabel);
        panel.add(new JLabel("Combat (Apprentice):")); panel.add(combatRelicsApprenticeLabel);
        panel.add(new JLabel("Combat (Journeyman):")); panel.add(combatRelicsJourneymanLabel);
        panel.add(new JLabel("Exploration (Apprentice):")); panel.add(explorationRelicsApprenticeLabel);
        panel.add(new JLabel("Exploration (Journeyman):")); panel.add(explorationRelicsJourneymanLabel);
        // TODO: Add labels for higher tiers later

        for(Component comp : panel.getComponents()) {
            if (comp instanceof JLabel) {
                configureCountLabel((JLabel)comp);
                if (java.util.Arrays.asList(panel.getComponents()).indexOf(comp) % 2 != 0) {
                    comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                }
            }
        }
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private void configureCountLabel(JLabel label) {
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setFont(UIManager.getFont("Label.font"));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private JPanel createProgressionTiersPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEtchedBorder(), "Progression Tiers",
                        javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                        javax.swing.border.TitledBorder.DEFAULT_POSITION,
                        UIManager.getFont("Label.font"), ColorScheme.LIGHT_GRAY_COLOR),
                new EmptyBorder(5, 5, 5, 5)
        ));
        ((javax.swing.border.TitledBorder) ((CompoundBorder) panel.getBorder()).getOutsideBorder()).setTitleColor(ColorScheme.LIGHT_GRAY_COLOR);

        configureLabel(meleeGearTierLabel);
        meleeGearTierLabel.setText("Melee Gear: Loading...");
        panel.add(meleeGearTierLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));

        for (Skill skill : Skill.values()) {
            if (skill == Skill.OVERALL || skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE) continue;
            JLabel label = new JLabel(skill.getName() + ": Loading...");
            configureLabel(label);
            skillTierLabels.put(skill, label);
            panel.add(label);
            panel.add(Box.createRigidArea(new Dimension(0, 2)));
        }
        return panel;
    }

    private void configureLabel(JLabel label) {
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        label.setFont(UIManager.getFont("Label.font"));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    // --- UI Update Methods ---
    public void updateRelicCounts() {
        SwingUtilities.invokeLater(() -> {
            if (config == null) { return; }
            skillingRelicsApprenticeLabel.setText(String.valueOf(config.skillingRelicsApprentice()));
            skillingRelicsJourneymanLabel.setText(String.valueOf(config.skillingRelicsJourneyman()));
            combatRelicsApprenticeLabel.setText(String.valueOf(config.combatRelicsApprentice()));
            combatRelicsJourneymanLabel.setText(String.valueOf(config.combatRelicsJourneyman()));
            explorationRelicsApprenticeLabel.setText(String.valueOf(config.explorationRelicsApprentice()));
            explorationRelicsJourneymanLabel.setText(String.valueOf(config.explorationRelicsJourneyman()));
            boolean canActivate = config.skillingRelicsApprentice() > 0 || config.skillingRelicsJourneyman() > 0 ||
                    config.combatRelicsApprentice() > 0 || config.combatRelicsJourneyman() > 0 ||
                    config.explorationRelicsApprentice() > 0 || config.explorationRelicsJourneyman() > 0;
            // TODO: Add checks for higher tiers later
            activateButton.setEnabled(canActivate);
        });
    }

    public void updateUnlockedDisplay() {
        SwingUtilities.invokeLater(() -> {
            if (config == null) { unlockedRelicsArea.setText("Error: Config not loaded."); return; }
            Set<String> unlockedIds = config.unlockedRelics();
            if (unlockedIds.isEmpty()) { unlockedRelicsArea.setText("  None"); return; }
            StringBuilder sb = new StringBuilder();
            unlockedIds.stream()
                    .map(UnlockManager::getUnlockById).filter(Optional::isPresent).map(Optional::get)
                    .sorted(Comparator.comparing(Unlockable::getName))
                    .forEach(unlock -> sb.append("• ").append(unlock.getName()).append("\n"));
            unlockedRelicsArea.setText(sb.toString());
            unlockedRelicsArea.setCaretPosition(0);
        });
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
                meleeGearTierLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

                for (Skill skill : skillTierLabels.keySet()) {
                    JLabel label = skillTierLabels.get(skill);
                    SkillTier tier = plugin.getSkillTier(skill);
                    label.setText(String.format("%s: %s (%d)", skill.getName(), tier.getDisplayName(), tier.getLevelCap()));
                    label.setForeground(tier == SkillTier.LOCKED ? ColorScheme.DARK_GRAY_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
                }
                progressionTiersPanel.revalidate();
                progressionTiersPanel.repaint();
            } catch (Exception e) {
                log.error("Unexpected error during updateProgressionTiersDisplay", e);
            }
        });
    }


    // --- Choice/Error Display ---
    public void displayChoices(final List<Unlockable> choices) {
        SwingUtilities.invokeLater(() -> {
            prepareChoiceArea();
            if (choices == null || choices.isEmpty()) {
                displayError("No valid unlocks available for this relic type.");
                return;
            }
            for (Unlockable choice : choices) {
                JButton choiceButton = new JButton("<html><body style='text-align:center'>" + choice.getName() + "<br><i style='color:gray'>" + choice.getDescription() + "</i></body></html>");
                SwingUtil.removeButtonDecorations(choiceButton);
                choiceButton.setToolTipText(choice.getDescription());
                choiceButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                choiceButton.addActionListener(e -> onChoiceSelected(choice, choiceButton));
                choiceButtons.add(choiceButton);
                choicePanel.add(choiceButton);
                choicePanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
            choicePanel.add(Box.createRigidArea(new Dimension(0, 10)));
            choicePanel.add(confirmButton);
            choicePanel.add(Box.createRigidArea(new Dimension(0, 5)));
            choicePanel.add(cancelButton);
            confirmButton.setEnabled(false);
            choicePanel.setVisible(true);
            choicePanel.revalidate();
            choicePanel.repaint();
            contentPanel.revalidate();
            contentPanel.repaint();
            if (getParent() instanceof JScrollPane) { ((JScrollPane) getParent()).getViewport().getView().repaint(); } else { repaint(); }
        });
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
            if (getParent() instanceof JScrollPane) { ((JScrollPane) getParent()).getViewport().getView().repaint(); } else { repaint(); }
        });
    }

    private void prepareChoiceArea() {
        choicePanel.removeAll();
        choicePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Choose Your Unlock",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                UIManager.getFont("Label.font"),
                ColorScheme.LIGHT_GRAY_COLOR));
        ((javax.swing.border.TitledBorder) choicePanel.getBorder()).setTitleColor(ColorScheme.LIGHT_GRAY_COLOR);
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
            if (getParent() instanceof JScrollPane) { ((JScrollPane) getParent()).getViewport().getView().repaint(); } else { repaint(); }
        });
    }

    // --- Action Handlers ---
    private void onChoiceSelected(Unlockable choice, JButton clickedButton) {
        selectedUnlock = choice;
        for (JButton btn : choiceButtons) {
            btn.setBorder(btn == clickedButton ? selectedButtonBorder : defaultButtonBorder);
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

    // --- Shutdown ---
    public void shutdown() { }
}