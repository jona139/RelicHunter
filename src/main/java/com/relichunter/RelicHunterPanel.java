// Filename: RelicHunter/src/main/java/com/relichunter/RelicHunterPanel.java
// Content:
package com.relichunter;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.SwingUtil;

import javax.inject.Inject; // Import Inject
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
    private final UnlockManager unlockManager; // *** ADDED: Inject UnlockManager ***

    // UI Components
    private final JPanel contentPanel;
    private final JLabel skillingRelicsApprenticeLabel = new JLabel();
    private final JLabel skillingRelicsJourneymanLabel = new JLabel();
    private final JLabel skillingRelicsExpertLabel = new JLabel(); // Added
    private final JLabel skillingRelicsMasterLabel = new JLabel(); // Added
    private final JLabel skillingRelicsGrandmasterLabel = new JLabel(); // Added
    private final JLabel combatRelicsApprenticeLabel = new JLabel();
    private final JLabel combatRelicsJourneymanLabel = new JLabel();
    private final JLabel combatRelicsExpertLabel = new JLabel(); // Added
    private final JLabel combatRelicsMasterLabel = new JLabel(); // Added
    private final JLabel combatRelicsGrandmasterLabel = new JLabel(); // Added
    private final JLabel explorationRelicsApprenticeLabel = new JLabel();
    private final JLabel explorationRelicsJourneymanLabel = new JLabel();
    private final JLabel explorationRelicsExpertLabel = new JLabel(); // Added
    private final JLabel explorationRelicsMasterLabel = new JLabel(); // Added
    private final JLabel explorationRelicsGrandmasterLabel = new JLabel(); // Added
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
    private Unlockable selectedUnlock = null; // Panel still uses Unlockable for display
    private final List<JButton> choiceButtons = new ArrayList<>();
    private final Border defaultButtonBorder = UIManager.getBorder("Button.border");
    private final Border selectedButtonBorder = BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE, 2);


    // Constructor
    // *** ADDED: UnlockManager injection ***
    @Inject
    public RelicHunterPanel(RelicHunterPlugin plugin, RelicHunterConfig config, UnlockManager unlockManager) {
        super(false); // Non-scrollable base, content goes into scroll pane
        this.plugin = plugin;
        this.config = config;
        this.unlockManager = unlockManager; // Store injected manager

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
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 3)); // rows=0 means variable rows
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

        // Skilling
        panel.add(new JLabel("Skilling (App):")); panel.add(skillingRelicsApprenticeLabel);
        panel.add(new JLabel("Skilling (Jour):")); panel.add(skillingRelicsJourneymanLabel);
        panel.add(new JLabel("Skilling (Exp):")); panel.add(skillingRelicsExpertLabel);
        panel.add(new JLabel("Skilling (Mas):")); panel.add(skillingRelicsMasterLabel);
        panel.add(new JLabel("Skilling (GM):")); panel.add(skillingRelicsGrandmasterLabel);
        // Combat
        panel.add(new JLabel("Combat (App):")); panel.add(combatRelicsApprenticeLabel);
        panel.add(new JLabel("Combat (Jour):")); panel.add(combatRelicsJourneymanLabel);
        panel.add(new JLabel("Combat (Exp):")); panel.add(combatRelicsExpertLabel);
        panel.add(new JLabel("Combat (Mas):")); panel.add(combatRelicsMasterLabel);
        panel.add(new JLabel("Combat (GM):")); panel.add(combatRelicsGrandmasterLabel);
        // Exploration
        panel.add(new JLabel("Exploration (App):")); panel.add(explorationRelicsApprenticeLabel);
        panel.add(new JLabel("Exploration (Jour):")); panel.add(explorationRelicsJourneymanLabel);
        panel.add(new JLabel("Exploration (Exp):")); panel.add(explorationRelicsExpertLabel);
        panel.add(new JLabel("Exploration (Mas):")); panel.add(explorationRelicsMasterLabel);
        panel.add(new JLabel("Exploration (GM):")); panel.add(explorationRelicsGrandmasterLabel);

        for(Component comp : panel.getComponents()) {
            if (comp instanceof JLabel) {
                configureCountLabel((JLabel)comp);
                // Bold the count labels (every second label)
                if (java.util.Arrays.asList(panel.getComponents()).indexOf(comp) % 2 != 0) {
                    comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                }
            }
        }
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Set max width to prevent horizontal stretching
        panel.setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, panel.getPreferredSize().height));
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

        // Add labels for all skills except Overall, Attack, Strength, Defence
        for (Skill skill : Skill.values()) {
            if (skill == Skill.OVERALL || skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE) continue;
            JLabel label = new JLabel(skill.getName() + ": Loading...");
            configureLabel(label);
            skillTierLabels.put(skill, label);
            panel.add(label);
            panel.add(Box.createRigidArea(new Dimension(0, 2)));
        }
        // Set max width
        panel.setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, panel.getPreferredSize().height));
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
            // Skilling
            skillingRelicsApprenticeLabel.setText(String.valueOf(config.skillingRelicsApprentice()));
            skillingRelicsJourneymanLabel.setText(String.valueOf(config.skillingRelicsJourneyman()));
            skillingRelicsExpertLabel.setText(String.valueOf(config.skillingRelicsExpert()));
            skillingRelicsMasterLabel.setText(String.valueOf(config.skillingRelicsMaster()));
            skillingRelicsGrandmasterLabel.setText(String.valueOf(config.skillingRelicsGrandmaster()));
            // Combat
            combatRelicsApprenticeLabel.setText(String.valueOf(config.combatRelicsApprentice()));
            combatRelicsJourneymanLabel.setText(String.valueOf(config.combatRelicsJourneyman()));
            combatRelicsExpertLabel.setText(String.valueOf(config.combatRelicsExpert()));
            combatRelicsMasterLabel.setText(String.valueOf(config.combatRelicsMaster()));
            combatRelicsGrandmasterLabel.setText(String.valueOf(config.combatRelicsGrandmaster()));
            // Exploration
            explorationRelicsApprenticeLabel.setText(String.valueOf(config.explorationRelicsApprentice()));
            explorationRelicsJourneymanLabel.setText(String.valueOf(config.explorationRelicsJourneyman()));
            explorationRelicsExpertLabel.setText(String.valueOf(config.explorationRelicsExpert()));
            explorationRelicsMasterLabel.setText(String.valueOf(config.explorationRelicsMaster()));
            explorationRelicsGrandmasterLabel.setText(String.valueOf(config.explorationRelicsGrandmaster()));

            // Enable activate button if any relic count > 0
            boolean canActivate = config.skillingRelicsApprentice() > 0 || config.skillingRelicsJourneyman() > 0 || config.skillingRelicsExpert() > 0 || config.skillingRelicsMaster() > 0 || config.skillingRelicsGrandmaster() > 0 ||
                    config.combatRelicsApprentice() > 0 || config.combatRelicsJourneyman() > 0 || config.combatRelicsExpert() > 0 || config.combatRelicsMaster() > 0 || config.combatRelicsGrandmaster() > 0 ||
                    config.explorationRelicsApprentice() > 0 || config.explorationRelicsJourneyman() > 0 || config.explorationRelicsExpert() > 0 || config.explorationRelicsMaster() > 0 || config.explorationRelicsGrandmaster() > 0;

            activateButton.setEnabled(canActivate);
        });
    }

    public void updateUnlockedDisplay() {
        SwingUtilities.invokeLater(() -> {
            if (config == null || unlockManager == null) { // Check unlockManager too
                unlockedRelicsArea.setText("Error: Config or UnlockManager not loaded.");
                return;
            }
            Set<String> unlockedIds = config.unlockedRelics();
            if (unlockedIds.isEmpty()) {
                unlockedRelicsArea.setText("  None");
                return;
            }
            StringBuilder sb = new StringBuilder();

            // *** FIXED: Use lambda with injected unlockManager ***
            unlockedIds.stream()
                    .map(id -> unlockManager.getUnlockById(id)) // Use lambda
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    // Convert UnlockData back to Unlockable for sorting/display (or update sorting)
                    .map(ud -> new Unlockable(ud.getId(), ud.getName(), ud.getDescription(), ud.getRelicType(), ud.getRequiredTier(), ud.getPrerequisites()))
                    .sorted(Comparator.comparing(Unlockable::getName))
                    .forEach(unlock -> sb.append("• ").append(unlock.getName()).append("\n"));

            unlockedRelicsArea.setText(sb.toString());
            unlockedRelicsArea.setCaretPosition(0); // Scroll to top
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
                GearTier meleeTier = config.meleeGearTier(); // TODO: Add Ranged/Magic gear tiers
                meleeGearTierLabel.setText(String.format("Melee Gear: %s", meleeTier.getDisplayName()));
                meleeGearTierLabel.setToolTipText(meleeTier.getDescription());
                meleeGearTierLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

                for (Skill skill : skillTierLabels.keySet()) {
                    JLabel label = skillTierLabels.get(skill);
                    SkillTier tier = plugin.getSkillTier(skill); // Get tier from plugin helper
                    label.setText(String.format("%s: %s (%d)", skill.getName(), tier.getDisplayName(), tier.getLevelCap()));
                    label.setForeground(tier == SkillTier.LOCKED ? ColorScheme.DARK_GRAY_COLOR : ColorScheme.LIGHT_GRAY_COLOR);
                }
                progressionTiersPanel.revalidate();
                progressionTiersPanel.repaint();
            } catch (Exception e) {
                log.error("Unexpected error during updateProgressionTiersDisplay", e);
                // Set error text on labels in case of failure
                meleeGearTierLabel.setText("Melee Gear: Error");
                skillTierLabels.values().forEach(label -> label.setText("Error"));
            }
        });
    }


    // --- Choice/Error Display ---
    // Panel still uses Unlockable for display logic, conversion happens in RelicHunterPlugin
    public void displayChoices(final List<Unlockable> choices) {
        SwingUtilities.invokeLater(() -> {
            prepareChoiceArea();
            if (choices == null || choices.isEmpty()) {
                displayError("No valid unlocks available for this relic type.");
                return;
            }
            for (Unlockable choice : choices) {
                // Use HTML for basic formatting within the button
                JButton choiceButton = new JButton("<html><body style='text-align:center; width: 150px;'>" // Added width for wrapping
                        + "<b>" + choice.getName() + "</b>"
                        + "<br><p style='color:gray; font-size: smaller;'>" + choice.getDescription() + "</p>"
                        + "</body></html>");
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
            confirmButton.setEnabled(false); // Disable confirm until a choice is made
            choicePanel.setVisible(true);
            choicePanel.revalidate();
            choicePanel.repaint();
            contentPanel.revalidate();
            contentPanel.repaint();
            // Ensure the parent scroll pane updates if content size changes
            if (getParent() instanceof JScrollPane) {
                ((JScrollPane) getParent()).getViewport().getView().revalidate();
                ((JScrollPane) getParent()).getViewport().getView().repaint();
            } else {
                revalidate();
                repaint();
            }
        });
    }

    public void displayError(String message) {
        SwingUtilities.invokeLater(() -> {
            prepareChoiceArea(); // Clear previous choices first
            errorPanel.setContent("Activation Error", message);
            choicePanel.add(errorPanel);
            choicePanel.setVisible(true);
            choicePanel.revalidate();
            choicePanel.repaint();
            contentPanel.revalidate();
            contentPanel.repaint();
            if (getParent() instanceof JScrollPane) {
                ((JScrollPane) getParent()).getViewport().getView().revalidate();
                ((JScrollPane) getParent()).getViewport().getView().repaint();
            } else {
                revalidate();
                repaint();
            }
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
        selectedUnlock = null; // Reset selection
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
            if (getParent() instanceof JScrollPane) {
                ((JScrollPane) getParent()).getViewport().getView().revalidate();
                ((JScrollPane) getParent()).getViewport().getView().repaint();
            } else {
                revalidate();
                repaint();
            }
        });
    }

    // --- Action Handlers ---
    private void onChoiceSelected(Unlockable choice, JButton clickedButton) {
        selectedUnlock = choice;
        // Highlight selected button
        for (JButton btn : choiceButtons) {
            btn.setBorder(btn == clickedButton ? selectedButtonBorder : defaultButtonBorder);
        }
        confirmButton.setEnabled(true); // Enable confirm button
    }

    private void onConfirmClicked(ActionEvent event) {
        if (selectedUnlock != null) {
            plugin.completeRelicActivation(selectedUnlock); // Pass the selected Unlockable
            clearChoiceDisplay(); // Clear the choice UI
        } else {
            log.warn("Confirm clicked but no selection was made.");
            // Optionally show a brief message to the user?
        }
    }

    private void onCancelClicked(ActionEvent event) {
        clearChoiceDisplay(); // Just clear the choice UI
    }

    // --- Shutdown ---
    // Called by Plugin's shutdown method
    public void shutdown() {
        // Perform any specific panel cleanup if needed
        log.debug("Relic Hunter Panel shutting down.");
    }
}
