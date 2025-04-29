// Filename: RelicHunter/src/main/java/com/relichunter/SkillTabOverlay.java
// Content:
package com.relichunter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
// import java.awt.Point; // <<< Remove AWT Point import
import net.runelite.api.Point; // <<< Import RuneLite API Point
import java.awt.Rectangle;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap; // Keep HashMap import if needed elsewhere, maybe not? Check imports
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ColorUtil;

/**
 * Overlay that draws indications on the skills tab based on unlocked tiers.
 */
public class SkillTabOverlay extends Overlay {

    private final Client client;
    private final RelicHunterPlugin plugin;
    private final RelicHunterConfig config;

    private static final Color LOCKED_TINT_COLOR = ColorUtil.colorWithAlpha(Color.BLACK, 150);
    private static final Color LEVEL_CAP_COLOR = Color.YELLOW;

    // Widget Mapping Logic (Needs verification via Dev Tools)
    private static final Map<Skill, Integer> SKILL_WIDGET_INDEX_MAP;
    static {
        Map<Skill, Integer> map = new EnumMap<>(Skill.class);
        map.put(Skill.ATTACK, 0); map.put(Skill.STRENGTH, 1); map.put(Skill.DEFENCE, 2);
        map.put(Skill.RANGED, 3); map.put(Skill.PRAYER, 4); map.put(Skill.MAGIC, 5);
        map.put(Skill.RUNECRAFT, 6); map.put(Skill.CONSTRUCTION, 7); map.put(Skill.HITPOINTS, 8);
        map.put(Skill.AGILITY, 9); map.put(Skill.HERBLORE, 10); map.put(Skill.THIEVING, 11);
        map.put(Skill.CRAFTING, 12); map.put(Skill.FLETCHING, 13); map.put(Skill.SLAYER, 14);
        map.put(Skill.HUNTER, 15); map.put(Skill.MINING, 16); map.put(Skill.SMITHING, 17);
        map.put(Skill.FISHING, 18); map.put(Skill.COOKING, 19); map.put(Skill.FIREMAKING, 20);
        map.put(Skill.WOODCUTTING, 21); map.put(Skill.FARMING, 22);
        SKILL_WIDGET_INDEX_MAP = Collections.unmodifiableMap(map);
    }

    @Inject
    private SkillTabOverlay(Client client, RelicHunterPlugin plugin, RelicHunterConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showSkillTierVisuals() || client.getGameState() != GameState.LOGGED_IN) {
            return null;
        }

        Widget skillsContainer = client.getWidget(WidgetInfo.SKILLS_CONTAINER);
        if (skillsContainer == null || skillsContainer.isHidden()) {
            return null;
        }

        Widget[] skillWidgets = skillsContainer.getStaticChildren();
        if (skillWidgets == null || skillWidgets.length == 0) {
            skillWidgets = skillsContainer.getDynamicChildren();
        }
        if (skillWidgets == null || skillWidgets.length == 0) {
            return null;
        }

        for (Skill skill : Skill.values()) {
            if (skill == Skill.OVERALL) continue;

            Integer widgetIndex = SKILL_WIDGET_INDEX_MAP.get(skill);
            Widget skillWidget = null;
            if (widgetIndex != null && widgetIndex >= 0 && widgetIndex < skillWidgets.length) {
                skillWidget = skillWidgets[widgetIndex];
            }

            if (skillWidget != null && !skillWidget.isHidden()) {
                SkillTier currentTier = plugin.getSkillTier(skill);
                boolean isMeleeSkill = (skill == Skill.ATTACK || skill == Skill.STRENGTH || skill == Skill.DEFENCE);

                Rectangle bounds = skillWidget.getBounds();
                // Check for valid bounds
                if (bounds == null || bounds.width <= 0 || bounds.height <= 0) continue;

                // Draw tint if skill is LOCKED (only for non-melee skills)
                if (!isMeleeSkill && currentTier == SkillTier.LOCKED) {
                    graphics.setColor(LOCKED_TINT_COLOR);
                    graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                }

                // Draw level cap for non-melee skills using SkillTier leveling
                if (!isMeleeSkill && currentTier != SkillTier.LOCKED && currentTier != SkillTier.GRANDMASTER) {
                    String levelCapText = String.valueOf(currentTier.getLevelCap());
                    int textWidth = graphics.getFontMetrics().stringWidth(levelCapText);

                    // --- NEW POSITIONING (e.g., Top-Right) ---
                    // Adjust X to be near the right edge
                    // Adjust Y to be near the top edge (considering font ascent)
                    int textX = bounds.x + bounds.width - textWidth - 2; // X: Right-aligned minus padding
                    int textY = bounds.y + graphics.getFontMetrics().getAscent() + 1; // Y: Top-aligned plus padding below ascent
                    Point textPoint = new Point(textX, textY);
                    // --- End NEW POSITIONING ---

                    // Calculate shadow point using integer coordinates from textPoint
                    Point shadowPoint = new Point(textPoint.getX() + 1, textPoint.getY() + 1);
                    OverlayUtil.renderTextLocation(graphics, shadowPoint, levelCapText, Color.BLACK); // Shadow
                    OverlayUtil.renderTextLocation(graphics, textPoint, levelCapText, LEVEL_CAP_COLOR); // Text
                }
            }
        }
        return null;
    }
}