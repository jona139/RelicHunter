// Filename: RelicHunter/src/main/java/com/relichunter/ItemRestrictionOverlay.java
// Content:
package com.relichunter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
// import java.awt.image.BufferedImage; // Keep if using icons later
import javax.inject.Inject;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
// Removed ColorUtil import as we get color directly from config

/**
 * Overlay to visually indicate items restricted by the player's current progression.
 * Uses configuration settings to enable/disable and set tint color.
 */
public class ItemRestrictionOverlay extends WidgetItemOverlay {

    private final RelicHunterPlugin plugin;
    private final RelicHunterConfig config;

    @Inject
    private ItemRestrictionOverlay(RelicHunterPlugin plugin, RelicHunterConfig config) {
        this.plugin = plugin;
        this.config = config;
        // Show overlay only on Inventory and Equipment widgets initially
        showOnInventory();
        showOnEquipment();
    }

    /**
     * Called for each item widget displayed in the configured containers.
     */
    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem) {
        // Check if the overlay feature is enabled in config
        if (!config.showItemRestrictions()) {
            return;
        }

        // Check if the item is restricted using the plugin's helper method
        if (!plugin.isMeleeItemAllowed(itemId)) {
            Rectangle bounds = widgetItem.getCanvasBounds();
            if (bounds != null && bounds.width > 0 && bounds.height > 0) {
                // Get tint color from config
                Color tintColor = config.itemRestrictionColor();
                // Draw the tint
                graphics.setColor(tintColor);
                graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

                // TODO: Optionally add config for showing an icon instead/as well
            }
        }
    }
}