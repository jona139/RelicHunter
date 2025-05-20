# Relic Hunter Helper

**Author:** GamecubeJona

## Description

Helps track progress and restrictions for the Relic Hunter Ironman mode.

## Features

*   Tracks acquired relics (Skilling, Combat, Exploration) across different tiers (Apprentice, Journeyman, Expert, Master, Grandmaster).
*   Manages a comprehensive database of unlockable game content, including items, skill tier upgrades, gear tier advancements, specific quests, game areas, and unique mechanics.
*   Relics are discovered through various in-game activities:
    *   **Skilling:** Gaining experience in non-combat skills.
    *   **Combat:** Defeating NPCs.
    *   **Exploration:** Completing Treasure Trails (clue scrolls).
*   Activate found relics to unlock new content. When multiple unlocks are available for a relic type and tier, the plugin presents a choice of up to 3 random options.
*   Enforces item usage restrictions based on your unlocked content.
*   Provides warnings for:
    *   Gaining XP in skills that are still locked.
    *   Exceeding the level cap for your current skill tier.
    *   Attempting to equip or use restricted items.
*   Integrates a RuneLite side panel to display:
    *   Current counts of each type and tier of relic held.
    *   A searchable list of all your unlocked content and their descriptions.
    *   Current progression status for skill tiers and combat gear tiers.
*   Features an overlay for selecting your desired unlock when activating a relic.
*   Offers extensive configuration options, such as:
    *   Selection between Free-to-Play (F2P) and Pay-to-Play (P2P) unlock databases.
    *   Toggling visual effects (emotes, graphics) and sound effects for plugin events.
    *   Adjusting the base chance and scaling for relic acquisition rates.
*   Delivers informative chat messages for plugin events and console warnings for restricted actions or errors.

## How it Works (Overview)

The Relic Hunter Helper plugin is designed for a special Ironman game mode where progression is tied to finding and activating relics. Here's a general idea of the gameplay loop:

1.  **Acquire Relics:** Engage in various in-game activities to find relics.
    *   **Skilling Relics:** Awarded randomly when gaining XP in non-combat skills. The tier of relic can depend on the amount of XP gained in one go.
    *   **Combat Relics:** Can be dropped by NPCs upon defeat. The tier of relic can depend on the combat level of the defeated NPC.
    *   **Exploration Relics:** Obtained by completing clue scrolls of different difficulties (Easy to Master).
2.  **Activate Relics:** Once you have a relic, you can activate it via the plugin's interface (e.g., the side panel). This consumes the relic.
3.  **Choose Unlocks:** Upon activation, the plugin will look up potential unlocks associated with the relic's type (Skilling, Combat, Exploration) and tier (e.g., Apprentice to Grandmaster).
    *   If multiple new unlocks are available and your prerequisites are met, you'll be presented with up to three random choices.
    *   Selecting an unlock will permanently add it to your character's progression.
4.  **Benefit from Unlocks:** Unlocks can grant various benefits, such as:
    *   The ability to use higher tiers of equipment.
    *   Access to new skills or increased level caps in existing skills.
    *   Permission to enter new game areas.
    *   Access to specific game mechanics or quests.
5.  **Track Progress & Restrictions:** The plugin automatically tracks your unlocked content and will:
    *   Restrict you from using items or equipment you haven't unlocked.
    *   Warn you if you gain XP in a skill that's still locked or if you exceed a skill's current level cap.
    *   Display your current relic inventory and unlocked abilities in the side panel.

The goal is to strategically choose your unlocks to expand your capabilities and progress your Relic Hunter Ironman account.

## Installation

1.  Ensure you have RuneLite installed. If not, download it from the official RuneLite website.
2.  Open RuneLite.
3.  Click on the "Configuration" wrench icon on the top-right of the RuneLite window to open the settings panel.
4.  At the top of the Configuration panel, click on "Plugin Hub".
5.  In the Plugin Hub search bar, type "Relic Hunter Helper".
6.  The plugin should appear in the list. Click the "Install" button next to it.
7.  The plugin will be installed, and you can find its settings by searching for "Relic Hunter Helper" in the main Configuration panel (outside the Plugin Hub). A side panel icon (a clue scroll) should also appear on the RuneLite sidebar.

## Configuration

The Relic Hunter Helper plugin offers a variety of options to customize your experience. You can access these by opening the RuneLite Configuration panel (wrench icon) and searching for "Relic Hunter Helper".

Key configuration settings include:

*   **Database Selection:**
    *   `Use F2P Database`: Switch between a database of unlocks tailored for Free-to-Play worlds and one for Pay-to-Play worlds. This significantly changes the available unlocks.
*   **Relic Acquisition:**
    *   `Skilling Relic Base Chance (1 in X)`: Sets the base probability for finding a skilling relic.
    *   `Combat Relic Base Chance (1 in X)`: Sets the base probability for finding a combat relic from NPC drops.
    *   `Exploration Relic Base Chance (1 in X)`: Sets the base probability for finding an exploration relic from clue scrolls.
    *   `Enable Scaling Drop Rate`: If enabled, the chances of getting a relic can improve based on the number of currently available (but not yet obtained) unlocks for that relic's tier.
    *   Minimum XP/NPC Level requirements for different relic tiers (e.g., `Skilling Relic Min XP (Apprentice)`, `Combat Relic NPC Level (Apprentice)`).
*   **User Interface & Feedback:**
    *   `Show Choice Overlay`: Toggles the overlay that appears when you activate a relic and have choices for your unlock.
    *   `Play Unlock Emote`: Determines if your character performs an animation when unlocking a relic.
    *   `Show Unlock Graphic`: Determines if a graphic effect is displayed on your character when unlocking a relic.
*   **Restrictions & Warnings:**
    *   `Warn On Restricted Item Use`: Provides a chat/console message if you attempt to use an item you haven't unlocked.
    *   `Attempt Block Restricted Item Use`: Tries to prevent the action (e.g., equipping) if an item is restricted.
    *   `Warn On Restricted XP Gain`: Notifies you if you gain XP in a skill that is currently locked.
    *   `Warn On Restricted Area Entry`: (If area restrictions are implemented and detected) Warns you upon entering a restricted area.
*   **Progression Management:**
    *   `Reset Progression Button`: A button that, when clicked in the config panel, will prompt you to confirm resetting all your tracked unlocks and relic counts for the plugin. Use with caution!

It's recommended to explore these settings to tailor the plugin to your preferred gameplay style and challenge level.

## Contributing

Contributions are welcome! If you have suggestions, bug reports, or would like to contribute to the development, please check out the project repository (if available) or contact the author.

## License

Please refer to the project repository or contact the author for license information.
