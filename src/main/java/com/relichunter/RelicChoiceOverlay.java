// Filename: RelicHunter/src/main/java/com/relichunter/RelicChoiceOverlay.java
package com.relichunter;

import com.relichunter.unlock.UnlockData;
import com.relichunter.unlock.UnlockType;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point; // Ensure java.awt.Point is imported
import java.awt.Rectangle;
import java.awt.event.MouseEvent; // *** ADDED MouseEvent import ***
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.MouseListener; // *** ADDED MouseListener import ***
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;
// Removed BackgroundComponent import as we use an image now
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

@Slf4j
// *** IMPLEMENT MouseListener ***
public class RelicChoiceOverlay extends Overlay implements MouseListener {

    private static final int PADDING = 10;
    private static final int BOTTOM_PADDING = 40;
    private static final int CHOICE_BOX_HEIGHT = 60;
    private static final int CHOICE_BOX_WIDTH = 200;
    private static final int CHOICE_BOX_SPACING = 15;
    private static final int ICON_SIZE = 32;
    private static final Color BORDER_COLOR = Color.DARK_GRAY;
    private static final Color HOVER_BORDER_COLOR = Color.ORANGE;
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color DESC_TEXT_COLOR = Color.LIGHT_GRAY;
    private static final int TEXT_RIGHT_MARGIN = 5;

    private final Client client;
    private final RelicHunterPlugin plugin;
    private final RelicHunterConfig config;
    private final UnlockManager unlockManager;
    private final ItemManager itemManager;
    private final SpriteManager spriteManager;

    @Setter
    private List<Unlockable> currentChoices = null;
    private final List<Rectangle> choiceBounds = new ArrayList<>();
    private net.runelite.api.Point mousePosition;

    private final Map<Integer, BufferedImage> spriteIconCache = new ConcurrentHashMap<>();
    private BufferedImage choiceBackgroundImage;
    private BufferedImage overlayBackgroundImage;
    private Rectangle overlayTotalBounds = null;

    @Inject
    private RelicChoiceOverlay(Client client, RelicHunterPlugin plugin, RelicHunterConfig config, UnlockManager unlockManager, ItemManager itemManager, SpriteManager spriteManager) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        this.unlockManager = unlockManager;
        this.itemManager = itemManager;
        this.spriteManager = spriteManager;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.HIGH);
        loadChoiceBackgroundImage();
        loadOverlayBackgroundImage();
        log.debug("RelicChoiceOverlay initialized.");
    }

    private void loadChoiceBackgroundImage() {
        try {
            choiceBackgroundImage = ImageUtil.loadImageResource(getClass(), "/button.png");
            if (choiceBackgroundImage == null) {
                log.error("Failed to load choice background image: /button.png not found.");
            } else {
                log.debug("Successfully loaded choice background image.");
            }
        } catch (Exception e) {
            log.error("Error loading choice background image", e);
            choiceBackgroundImage = null;
        }
    }

    private void loadOverlayBackgroundImage() {
        try {
            overlayBackgroundImage = ImageUtil.loadImageResource(getClass(), "/display_box.png");
            if (overlayBackgroundImage == null) {
                log.error("Failed to load overlay background image: /display_box.png not found.");
            } else {
                log.debug("Successfully loaded overlay background image.");
            }
        } catch (Exception e) {
            log.error("Error loading overlay background image", e);
            overlayBackgroundImage = null;
        }
    }


    public void clearChoices() {
        log.debug("Clearing choices in RelicChoiceOverlay.");
        this.currentChoices = null;
        this.choiceBounds.clear();
        this.overlayTotalBounds = null;
    }

    public boolean hasChoices() {
        return currentChoices != null && !currentChoices.isEmpty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        log.trace("RelicChoiceOverlay render called. Has choices: {}, Config enabled: {}", hasChoices(), config.showChoiceOverlay());

        if (!hasChoices() || !config.showChoiceOverlay()) {
            overlayTotalBounds = null;
            return null;
        }

        mousePosition = client.getMouseCanvasPosition();
        if (mousePosition == null) {
            mousePosition = new net.runelite.api.Point(-1, -1);
        }

        int choicesCount = currentChoices.size();
        Font titleFont = FontManager.getRunescapeBoldFont();
        FontMetrics fmTitle = graphics.getFontMetrics(titleFont);
        int titleHeight = fmTitle.getHeight();
        int totalContentHeight = titleHeight + (CHOICE_BOX_HEIGHT * choicesCount) + (CHOICE_BOX_SPACING * Math.max(0, choicesCount));
        int totalHeight = PADDING + totalContentHeight + BOTTOM_PADDING;
        int totalWidth = CHOICE_BOX_WIDTH + PADDING * 2;

        Dimension canvasSize = client.getRealDimensions();
        if (canvasSize == null) {
            log.warn("Canvas dimensions are null, cannot render overlay.");
            overlayTotalBounds = null;
            return null;
        }
        int startX = (canvasSize.width - totalWidth) / 2;
        int startY = (canvasSize.height - totalHeight) / 2;

        this.overlayTotalBounds = new Rectangle(startX, startY, totalWidth, totalHeight);
        log.trace("Calculated overlay bounds: {}", this.overlayTotalBounds);

        // Draw the main overlay background image
        if (overlayBackgroundImage != null) {
            graphics.drawImage(overlayBackgroundImage, startX, startY, totalWidth, totalHeight, null);
        } else {
            graphics.setColor(new Color(50,50,50, 200));
            graphics.fillRect(startX, startY, totalWidth, totalHeight);
            graphics.setColor(Color.BLACK);
            graphics.drawRect(startX, startY, totalWidth, totalHeight);
        }

        // --- Draw Title and Choices ON TOP of the background ---

        String titleText = "Choose Your Unlock";
        TextComponent titleComponent = new TextComponent();
        titleComponent.setText(titleText);
        titleComponent.setColor(TEXT_COLOR);
        titleComponent.setFont(titleFont);

        int titleWidth = fmTitle.stringWidth(titleText);
        titleComponent.setPosition(new java.awt.Point(startX + (totalWidth - titleWidth) / 2, startY + PADDING + fmTitle.getAscent()));
        titleComponent.render(graphics);

        int currentY = startY + PADDING + titleHeight + CHOICE_BOX_SPACING;

        choiceBounds.clear();

        for (int i = 0; i < choicesCount; i++) {
            Unlockable choice = currentChoices.get(i);
            int boxX = startX + PADDING;
            int boxY = currentY;
            Rectangle bounds = new Rectangle(boxX, boxY, CHOICE_BOX_WIDTH, CHOICE_BOX_HEIGHT);
            choiceBounds.add(bounds);

            boolean isHovered = bounds.contains(mousePosition.getX(), mousePosition.getY());

            // Draw individual choice background (button.png)
            if (choiceBackgroundImage != null) {
                graphics.drawImage(choiceBackgroundImage, bounds.x, bounds.y, bounds.width, bounds.height, null);
            } else {
                graphics.setColor(new Color(80,80,80,220));
                graphics.fill(bounds);
            }

            graphics.setColor(isHovered ? HOVER_BORDER_COLOR : BORDER_COLOR);
            graphics.draw(bounds);

            int iconX = boxX + PADDING;
            int iconY = boxY + (CHOICE_BOX_HEIGHT - ICON_SIZE) / 2;
            drawUnlockIcon(graphics, choice, iconX, iconY);

            int textX = iconX + ICON_SIZE + PADDING;
            int textStartY = boxY + PADDING;
            Font smallBoldFont = FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD);
            Font smallFont = FontManager.getRunescapeSmallFont();
            FontMetrics fmBold = graphics.getFontMetrics(smallBoldFont);
            FontMetrics fmSmall = graphics.getFontMetrics(smallFont);

            // Calculate available width for text inside the button
            int textAvailableWidth = CHOICE_BOX_WIDTH - (iconX + ICON_SIZE - boxX) - PADDING - TEXT_RIGHT_MARGIN; // Width minus icon, left padding, right margin

            // Draw Name (Truncate if needed)
            graphics.setFont(smallBoldFont);
            graphics.setColor(TEXT_COLOR);
            String nameText = choice.getName();
            if (fmBold.stringWidth(nameText) > textAvailableWidth) {
                while (fmBold.stringWidth(nameText + "...") > textAvailableWidth && nameText.length() > 0) {
                    nameText = nameText.substring(0, nameText.length() - 1);
                }
                nameText += "...";
            }
            graphics.drawString(nameText, textX, textStartY + fmBold.getAscent());


            // Draw Description (Limited lines and truncated)
            graphics.setFont(smallFont);
            graphics.setColor(DESC_TEXT_COLOR);
            int descY = textStartY + fmBold.getHeight() + 2; // Start below name
            String[] words = choice.getDescription().split(" ");
            StringBuilder line = new StringBuilder();
            int linesDrawn = 0;
            final int MAX_DESC_LINES = 2; // Limit description to 2 lines

            for (String word : words) {
                if (linesDrawn >= MAX_DESC_LINES) break; // Stop if max lines reached

                String testLine = line.length() > 0 ? line.toString() + " " + word : word;
                if (fmSmall.stringWidth(testLine) <= textAvailableWidth) {
                    // Word fits on the current line
                    line.append(line.length() > 0 ? " " : "").append(word);
                } else {
                    // Word doesn't fit, draw the current line
                    graphics.drawString(line.toString(), textX, descY + fmSmall.getAscent());
                    linesDrawn++;
                    descY += fmSmall.getHeight();
                    line = new StringBuilder(word); // Start new line with the current word

                    // Check if the new line itself is too long (unlikely for single words but safety check)
                    if (linesDrawn < MAX_DESC_LINES && fmSmall.stringWidth(line.toString()) > textAvailableWidth) {
                        while (fmSmall.stringWidth(line.toString() + "...") > textAvailableWidth && line.length() > 0) {
                            line.setLength(line.length() - 1);
                        }
                        graphics.drawString(line.toString() + "...", textX, descY + fmSmall.getAscent());
                        linesDrawn++; // Count the truncated line
                        line.setLength(0); // Clear line as it was truncated
                        break; // Stop processing words
                    }
                }
            }
            // Draw the last remaining line if it's not empty and haven't exceeded max lines
            if (line.length() > 0 && linesDrawn < MAX_DESC_LINES) {
                graphics.drawString(line.toString(), textX, descY + fmSmall.getAscent());
            }

            currentY += CHOICE_BOX_HEIGHT + CHOICE_BOX_SPACING;
        }
        log.trace("RelicChoiceOverlay render finished drawing {} choices.", choicesCount);
        return null;
    }

    // --- MouseListener Implementation ---

    @Override
    public MouseEvent mouseClicked(MouseEvent mouseEvent) {
        log.trace("RelicChoiceOverlay mouseClicked AWT event received at {}. Has choices: {}, Config enabled: {}", mouseEvent.getPoint(), hasChoices(), config.showChoiceOverlay());

        if (!hasChoices() || !config.showChoiceOverlay()) {
            return mouseEvent; // Don't handle if not visible/active
        }

        // Convert AWT Point to RuneLite Point for contains check
        net.runelite.api.Point clickPoint = new net.runelite.api.Point(mouseEvent.getX(), mouseEvent.getY());

        // Check against individual choice bounds first
        for (int i = 0; i < choiceBounds.size(); i++) {
            Rectangle bounds = choiceBounds.get(i);
            log.trace("Checking click against bounds {}: {}", i, bounds);
            if (bounds.contains(clickPoint.getX(), clickPoint.getY())) {
                Unlockable chosenUnlock = currentChoices.get(i);
                log.debug("Relic choice overlay clicked: {}", chosenUnlock.getName());
                plugin.completeRelicActivation(chosenUnlock);
                clearChoices(); // Clear choices after handling
                mouseEvent.consume(); // Consume the event
                log.trace("Click consumed by choice button {}", i);
                return mouseEvent;
            }
        }
        log.trace("Click was not within any choice bounds.");

        // Check if click was within the overall background bounds
        if (this.overlayTotalBounds != null && this.overlayTotalBounds.contains(clickPoint.getX(), clickPoint.getY())) {
            log.trace("Click was inside overlay background but outside choices. Consuming.");
            // Optionally add cancel logic here if desired
            // plugin.clearChoiceDisplay();
            mouseEvent.consume(); // Consume the click even if not on a button
        } else {
            log.trace("Click was outside overlay bounds.");
        }

        return mouseEvent; // Return (potentially consumed) event
    }

    @Override
    public MouseEvent mousePressed(MouseEvent mouseEvent) {
        // Consume press events within the overlay bounds to prevent click-through
        if (hasChoices() && config.showChoiceOverlay() && this.overlayTotalBounds != null) {
            if (this.overlayTotalBounds.contains(mouseEvent.getX(), mouseEvent.getY())) {
                log.trace("mousePressed event consumed by RelicChoiceOverlay bounds check");
                mouseEvent.consume();
            }
        }
        return mouseEvent;
    }

    @Override
    public MouseEvent mouseReleased(MouseEvent mouseEvent) {
        // Consume release events within the overlay bounds as well
        if (hasChoices() && config.showChoiceOverlay() && this.overlayTotalBounds != null) {
            if (this.overlayTotalBounds.contains(mouseEvent.getX(), mouseEvent.getY())) {
                log.trace("mouseReleased event consumed by RelicChoiceOverlay bounds check");
                mouseEvent.consume();
            }
        }
        return mouseEvent;
    }

    // Other MouseListener methods (empty implementations or return event)
    @Override
    public MouseEvent mouseEntered(MouseEvent mouseEvent) { return mouseEvent; }
    @Override
    public MouseEvent mouseExited(MouseEvent mouseEvent) { return mouseEvent; }
    @Override
    public MouseEvent mouseDragged(MouseEvent mouseEvent) { return mouseEvent; }
    @Override
    public MouseEvent mouseMoved(MouseEvent mouseEvent) { return mouseEvent; }


    // --- Icon Drawing Helper ---
    private void drawUnlockIcon(Graphics2D graphics, Unlockable unlock, int x, int y) {
        Optional<UnlockData> dataOpt = unlockManager.getUnlockById(unlock.getId());
        if (!dataOpt.isPresent()) {
            drawPlaceholderIcon(graphics, "?", x, y);
            return;
        }
        UnlockData unlockData = dataOpt.get();
        BufferedImage icon = null;

        switch (unlockData.getCategory()) {
            case SKILL_TIER:
                String skillName = unlockData.getId().split("_")[1];
                try {
                    Skill skill = Skill.valueOf(skillName);
                    int spriteId = RelicHunterPanel.SkillIconManager.getSkillSpriteId(skill);
                    icon = spriteIconCache.get(spriteId);
                    if (icon == null) {
                        spriteManager.getSpriteAsync(spriteId, 0, img -> {
                            if (img != null) {
                                spriteIconCache.put(spriteId, img);
                            } else {
                                log.warn("Failed to load sprite for skill: {}", skill);
                            }
                        });
                        drawPlaceholderIcon(graphics, skill.getName().substring(0,1), x, y);
                        return;
                    }
                } catch (Exception e) {
                    log.error("Error getting skill icon for {}", skillName, e);
                    drawPlaceholderIcon(graphics, "Sk", x, y);
                    return;
                }
                break;

            case GEAR_TIER:
            case SPECIFIC_ITEM:
                if (unlockData.getItemIds() != null && !unlockData.getItemIds().isEmpty()) {
                    int itemId = unlockData.getItemIds().iterator().next();
                    icon = itemManager.getImage(itemId, 1, false);
                    if (icon == null) {
                        log.warn("Failed to get image for item ID: {}", itemId);
                        drawPlaceholderIcon(graphics, unlockData.getCategory() == UnlockType.GEAR_TIER ? "G" : "It", x, y);
                        return;
                    }
                } else {
                    drawPlaceholderIcon(graphics, unlockData.getCategory() == UnlockType.GEAR_TIER ? "G" : "It", x, y);
                    return;
                }
                break;

            case AREA: drawPlaceholderIcon(graphics, "A", x, y); return;
            case QUEST: drawPlaceholderIcon(graphics, "Q", x, y); return;
            case MECHANIC: drawPlaceholderIcon(graphics, "M", x, y); return;
            case BOSS: drawPlaceholderIcon(graphics, "B", x, y); return;
            default: drawPlaceholderIcon(graphics, "?", x, y); return;
        }

        if (icon != null) {
            drawIcon(graphics, icon, x, y);
        }
    }

    private void drawIcon(Graphics2D graphics, BufferedImage icon, int x, int y) {
        if (icon == null) {
            log.warn("Attempted to draw a null icon at {}, {}", x, y);
            drawPlaceholderIcon(graphics, "!", x, y);
            return;
        }
        BufferedImage resizedIcon = ImageUtil.resizeImage(icon, ICON_SIZE, ICON_SIZE);
        graphics.drawImage(resizedIcon, x, y, null);
    }

    private void drawPlaceholderIcon(Graphics2D graphics, String text, int x, int y) {
        graphics.setColor(Color.DARK_GRAY);
        graphics.fillRect(x, y, ICON_SIZE, ICON_SIZE);
        graphics.setColor(Color.WHITE);
        graphics.drawRect(x, y, ICON_SIZE, ICON_SIZE);
        graphics.setFont(FontManager.getRunescapeSmallFont());
        FontMetrics fm = graphics.getFontMetrics();
        int textX = x + (ICON_SIZE - fm.stringWidth(text)) / 2;
        int textY = y + ((ICON_SIZE - fm.getHeight()) / 2) + fm.getAscent();
        graphics.drawString(text, textX, textY);
    }
}