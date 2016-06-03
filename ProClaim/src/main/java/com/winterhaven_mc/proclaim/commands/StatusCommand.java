package com.winterhaven_mc.proclaim.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.winterhaven_mc.proclaim.PluginMain;


final class StatusCommand implements CommandExecutor {
	
	// reference to main class
	private final PluginMain plugin;
	
	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	StatusCommand(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// register this class as command executor
		plugin.getCommand("pcstatus").setExecutor(this);
	}

	/**
	 * Display plugin status
	 */
	@Override
	public final boolean onCommand(final CommandSender sender, final Command command, 
			final String label, final String[] args) {

		// if sender does not have permission for status command, output error message and return true
		if (!sender.hasPermission("proclaim.command.transferclaim")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_STATUS_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}

		// output config settings
		final String versionString = plugin.getDescription().getVersion();
		sender.sendMessage(ChatColor.DARK_AQUA 
				+ "[" + plugin.getName() + "] " + ChatColor.AQUA + "Version: " + ChatColor.RESET + versionString);
		if (plugin.debug) {
			sender.sendMessage(ChatColor.DARK_RED + "DEBUG: true");
		}
		sender.sendMessage(ChatColor.GREEN + "Language: " 
				+ ChatColor.RESET + plugin.messageManager.getLanguage());
		sender.sendMessage(ChatColor.GREEN + "Storage type: " 
				+ ChatColor.RESET + plugin.dataStore.getDisplayName());
		sender.sendMessage(ChatColor.GREEN + "Enabled Words: " 
				+ ChatColor.RESET + plugin.worldManager.getEnabledWorldNames().toString());
		return true;
	}

}
