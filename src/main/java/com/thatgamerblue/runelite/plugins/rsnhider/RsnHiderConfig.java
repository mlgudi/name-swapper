package com.thatgamerblue.runelite.plugins.rsnhider;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("namechanger")
public interface RsnHiderConfig extends Config
{
	@ConfigItem(
		name = "Change names in widgets (Lag warning)",
		keyName = "changeWidgets",
		description = "Change names everywhere. Might lag your game."
	)
	default boolean changeWidgets()
	{
		return false;
	}

	@ConfigItem(
		name = "Names to Swap",
		keyName = "namesToSwap",
		description = "Use the panel to modify this, unless you are pasting a pre-made config."
	)
	default String namesToSwap()
	{
		return "";
	}

	void namesToSwap(String namesToSwap);
}
