// Filename: RelicHunter/src/main/java/com/relichunter/QuestLogOverlay.java
// Content:
package com.relichunter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Set;
import javax.inject.Inject;
// Lombok/Slf4j import removed
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest; // Import Quest enum
import net.runelite.api.QuestState;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ColorUtil;

// Slf4j annotation removed
public class QuestLogOverlay extends Overlay {

    private final Client client;
    private final RelicHunterPlugin plugin;
    private final RelicHunterConfig config;
    private final UnlockManager unlockManager;

    // *** Define the correct intermediate container ID based on Dev Tools ***
    private static final int QUEST_TEXT_CONTAINER_GROUP_ID = 399;
    private static final int QUEST_TEXT_CONTAINER_CHILD_ID = 7;

    private static final Color LOCKED_QUEST_TINT = ColorUtil.colorWithAlpha(Color.BLACK, 180);
    // LOCKED_QUEST_TEXT_COLOR is unused, can be removed if desired
    // private static final Color LOCKED_QUEST_TEXT_COLOR = Color.GRAY;

    @Inject
    private QuestLogOverlay(Client client, RelicHunterPlugin plugin, RelicHunterConfig config, UnlockManager unlockManager) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.unlockManager = unlockManager;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS); // Draw over the quest list
        setPriority(OverlayPriority.HIGH);
        // Initialization log removed
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return null;
        }
        QuestRestrictionMode currentMode = config.questRestrictionMode();
        if (currentMode == QuestRestrictionMode.NONE) {
            return null;
        }

        Widget questTextContainer = client.getWidget(QUEST_TEXT_CONTAINER_GROUP_ID, QUEST_TEXT_CONTAINER_CHILD_ID);

        if (questTextContainer == null || questTextContainer.isHidden()) {
            // Warning log removed
            return null;
        }

        Widget[] questWidgets = questTextContainer.getChildren();
        if (questWidgets == null) {
            // Warning log removed
            return null;
        }

        Set<String> unlockedIds = config.unlockedRelics();

        for (Widget questWidget : questWidgets) {
            if (questWidget == null) continue;

            if (questWidget.getType() != net.runelite.api.widgets.WidgetType.TEXT) {
                continue;
            }

            // Check visibility *before* getting text/processing
            if (questWidget.isHidden() || questWidget.isSelfHidden() || questWidget.getText() == null || questWidget.getText().isEmpty()) {
                continue;
            }

            String questName = questWidget.getText();
            Quest quest = findQuestByName(questName);

            if (quest != null) {
                String questUnlockId = "QUEST_" + quest.name();
                boolean isUnlocked = unlockedIds.contains(questUnlockId);

                if (!isUnlocked) {
                    applyRestriction(graphics, questWidget, currentMode);
                } else {
                    // If unlocked and mode is HIDE, explicitly try to unhide it
                    if (currentMode == QuestRestrictionMode.HIDE && questWidget.isSelfHidden()) {
                        // Trace log removed
                        questWidget.setHidden(false); // Try to make it visible
                    }
                }
            }
            // Log for unmappable quests removed
        }
        // Log for processed count removed
        return null;
    }

    private void applyRestriction(Graphics2D graphics, Widget widget, QuestRestrictionMode mode) {
        switch (mode) {
            case HIDE:
                if (!widget.isSelfHidden()) {
                    // Info log removed
                    widget.setHidden(true); // Attempt to hide
                }
                break;
            case DIM:
                // Info log removed
                drawDimOverlay(graphics, widget);
                break;
            case NONE:
            default:
                break;
        }
    }

    // Helper method to draw the dimming overlay
    private void drawDimOverlay(Graphics2D graphics, Widget widget) {
        Rectangle bounds = widget.getBounds();
        if (bounds != null && bounds.width > 0 && bounds.height > 0) {
            graphics.setColor(LOCKED_QUEST_TINT);
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        } else {
            // Warning log removed
        }
    }

    // Helper method to find the OSRS Quest enum constant by its name string
    private Quest findQuestByName(String name) {
        for (Quest q : Quest.values()) {
            if (q.getName().equalsIgnoreCase(name)) {
                return q;
            }
        }
        // Trace log removed
        return null;
    }
}
