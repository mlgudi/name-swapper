/*
 * Copyright (c) 2020, ThatGamerBlue <thatgamerblue@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.thatgamerblue.runelite.plugins.rsnhider;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.ScriptID;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/*
Mental breakdown 2: electric boogaloo

Alexa, play sea shanty two.
Peace to:
	r189, he.cc
*/
@PluginDescriptor(
	name = "Name Changer",
	description = "Change the names of your player and other players.",
	tags = {"twitch", "youtube"},
	enabledByDefault = false
)
public class RsnHiderPlugin extends Plugin
{
	private static final BufferedImage ICON = ImageUtil.loadImageResource(RsnHiderPlugin.class, "icon.png");

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private RsnHiderConfig config;

	private RsnHiderPanel panel;
	private NavigationButton navButton;

	public HashMap<String, String> namesToSwap;
	private boolean forceUpdate = false;

	@Provides
	private RsnHiderConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(RsnHiderConfig.class);
	}

	public void loadNames() {
		namesToSwap.clear();
		String[] lines = config.namesToSwap().split("\n");

		for (String line : lines) {
			String[] parts = line.split(",", 2);

			if (parts.length == 2) {
				namesToSwap.put(parts[0].trim(), parts[1].trim());
			}
		}
	}

	public void saveNames() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : namesToSwap.entrySet()) {
			sb.append(entry.getKey()).append(",").append(entry.getValue()).append("\n");

		}

		if (sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}
		config.namesToSwap(sb.toString());
		configManager.setConfiguration("namechanger", "namesToSwap", sb.toString());
	}

	@Override
	public void startUp()
	{
		namesToSwap = new HashMap<>();
		loadNames();

		panel = new RsnHiderPanel(this);

		navButton = NavigationButton.builder()
				.tooltip("Name Swapper")
				.icon(ICON)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	public void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		namesToSwap.clear();
		panel = null;
		navButton = null;
		clientThread.invokeLater(() -> client.runScript(ScriptID.CHAT_PROMPT_INIT));
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("nameswapper"))
		{
			return;
		}

		loadNames();
	}

	@Subscribe
	private void onBeforeRender(BeforeRender event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		swapHoverText();

		if (config.changeWidgets())
		{
			// do every widget
			for (Widget widgetRoot : client.getWidgetRoots())
			{
				processWidget(widgetRoot);
			}
		}
		else
		{
			updateChatbox();
		}
	}

	public void swapHoverText()
	{
		if (client.isMenuOpen())
		{
			return;
		}

		MenuEntry[] menuEntries = client.getMenuEntries();
		int last = menuEntries.length - 1;

		if (last < 0)
		{
			return;
		}

		MenuEntry hover = menuEntries[last];

		String target = hover.getTarget();
		String option = hover.getOption();
		String swappedTarget = swapNames(target);
		String swappedOption = swapNames(option);
		hover.setTarget(swappedTarget);
		hover.setOption(swappedOption);

	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		MenuEntry[] entries = event.getMenuEntries();

		for (MenuEntry entry : entries)
		{
			String target = entry.getTarget();
			String option = entry.getOption();
			String swappedTarget = swapNames(target);
			String swappedOption = swapNames(option);
			entry.setTarget(swappedTarget);
			entry.setOption(swappedOption);
		}
	}

	private String swapNames(String textIn) {
		for (Map.Entry<String, String> entry : namesToSwap.entrySet()) {
			String originalName = entry.getKey();
			String newName = entry.getValue();

			String standardized = Text.standardize(originalName);

			while (Text.standardize(textIn).contains(standardized)) {
				int idx = textIn.replace("\u00A0", " ").toLowerCase().indexOf(originalName.toLowerCase());
				int length = originalName.length();
				String partOne = textIn.substring(0, idx);
				String partTwo = textIn.substring(idx + length);
				textIn = partOne + newName + partTwo;
			}
		}

		return textIn;
	}

	/**
	 * Recursively traverses widgets looking for text containing the players name, replacing it if necessary
	 * @param widget The root widget to process
	 */
	private void processWidget(Widget widget)
	{
		if (widget == null)
		{
			return;
		}

		if (widget.getText() != null)
		{
			widget.setText(swapNames(widget.getText()));
		}

		for (Widget child : widget.getStaticChildren())
		{
			processWidget(child);
		}

		for (Widget dynamicChild : widget.getDynamicChildren())
		{
			processWidget(dynamicChild);
		}

		for (Widget nestedChild : widget.getNestedChildren())
		{
			processWidget(nestedChild);
		}
	}

	private void updateChatbox()
	{
		Widget chatboxTypedText = client.getWidget(WidgetInfo.CHATBOX_INPUT);
		if (chatboxTypedText == null || chatboxTypedText.isHidden())
		{
			return;
		}
		String[] chatbox = chatboxTypedText.getText().split(":", 2);

		//noinspection ConstantConditions
		String chatboxPlayerName = chatbox[0];
		String swappedName = swapNames(chatboxPlayerName);
		if (forceUpdate || !Text.standardize(swappedName).equals(Text.standardize(chatboxPlayerName)))
		{
			chatbox[0] = swappedName;
		}

		forceUpdate = false;
		chatboxTypedText.setText(chatbox[0] + ":" + (chatbox.length > 1 ? chatbox[1] : ""));
	}

	@Subscribe
	private void onChatMessage(ChatMessage event)
	{
		//noinspection ConstantConditions
		if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
		{
			return;
		}

		String replaced = swapNames(event.getMessage());
		event.setMessage(replaced);
		event.getMessageNode().setValue(replaced);

		if (event.getName() == null) {
			return;
		}

		String newName = swapNames(event.getName());
		event.setName(newName);
		event.getMessageNode().setName(newName);
	}

	@Subscribe
	private void onOverheadTextChanged(OverheadTextChanged event)
	{
		event.getActor().setOverheadText(swapNames(event.getOverheadText()));
	}
}
