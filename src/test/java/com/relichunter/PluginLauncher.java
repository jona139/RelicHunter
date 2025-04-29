// Filename: RelicHunter/src/test/java/com/relichunter/PluginLauncher.java
// Content:
package com.relichunter;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PluginLauncher
{
	public static void main(String[] args) throws Exception
	{
		// Load the RelicHunterPlugin instead of ExamplePlugin
		ExternalPluginManager.loadBuiltin(RelicHunterPlugin.class);
		RuneLite.main(args);
	}
}