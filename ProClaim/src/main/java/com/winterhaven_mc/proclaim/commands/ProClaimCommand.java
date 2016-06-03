package com.winterhaven_mc.proclaim.commands;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.objects.ClaimTool;
import com.winterhaven_mc.proclaim.storage.DataStoreFactory;
import com.winterhaven_mc.proclaim.storage.PlayerState;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;


final class ProClaimCommand implements CommandExecutor, TabCompleter {

	private final PluginMain plugin;

	private final static ChatColor helpColor = ChatColor.YELLOW;
	private final static ChatColor usageColor = ChatColor.GOLD;

	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	ProClaimCommand(final PluginMain plugin) {

		// set reference to main class
		this.plugin = plugin;

		// register this class as command executor
		plugin.getCommand("proclaim").setExecutor(this);

		// register this class as tab completer
		plugin.getCommand("proclaim").setTabCompleter(this);
	}


	@Override
	public final List<String> onTabComplete(final CommandSender sender, final Command command, 
			final String alias, final String[] args) {

		final List<String> returnList = new ArrayList<>();

		// return list of valid matching subcommands
		if (args.length == 1) {

			for (ProClaimSubcommand subcmd : ProClaimSubcommand.values()) {
				if (sender.hasPermission("proclaim.command." + subcmd.toString()) 
						&& subcmd.toString().startsWith(args[0].toLowerCase())) {
					returnList.add(subcmd.toString());
				}
			}
		}
		return returnList;
	}

	@Override
	public final boolean onCommand(final CommandSender sender, final Command command, 
			final String label, final String[] args) {

		String subcmd;

		// get subcommand
		if (args.length > 0) {
			subcmd = args[0];
		}
		// if no arguments, display usage for all commands
		else {
			displayUsage(sender,"all");
			return true;
		}

		// reload command
		if (subcmd.equalsIgnoreCase("admin")) {
			return adminCommand(sender,args);
		}

		// reload command
		if (subcmd.equalsIgnoreCase("reload")) {
			return reloadCommand(sender,args);
		}

//		// show command
//		if (subcmd.equalsIgnoreCase("showcache")) {
//			return showcacheCommand(sender,args);
//		}

		// status command
		if (subcmd.equalsIgnoreCase("status")) {
			return statusCommand(sender,args);
		}
		
		// help command
		if (subcmd.equalsIgnoreCase("help")) {
			return helpCommand(sender,args);
		}

		plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_INVALID");
		plugin.soundManager.playerSound(sender, "command-fail");
		displayUsage(sender,"help");
		return true;
	}
	
		
	private boolean adminCommand(final CommandSender sender, final String args[]) {
		
		// if sender does not have permission for admin mode, output error message and return true
		if (!sender.hasPermission("proclaim.command.admin")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_ADMINCLAIM_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}
		
		// sender must be in game player
		if (!(sender instanceof Player)) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CONSOLE");
			return true;
		}
		
		// argument limits
		final int maxArgs = 0;
		
		// check max arguments
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_OVER");
			return false;
		}
		
		// get player object for command sender
		final Player player = (Player) sender;
		
		// get player state
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
		
		// if player is not in admin mode, set admin mode true and tool to admin tool
		if (!playerState.isAdminMode()) {
			
			// set player admin mode to true
			playerState.setAdminMode(true);
			
			// set player tool mode to admin
			playerState.setCurrentToolMode(ClaimTool.ADMIN);
			
			// change tool in player inventory to admin tool
			ClaimTool.changeInventoryTool(player,ClaimTool.ADMIN);
		}
		else {
			
			// set player admin mode to false
			playerState.setAdminMode(false);
			
			// set player tool mode to basic
			playerState.setCurrentToolMode(ClaimTool.BASIC);
			
			// change tool in player inventory to basic tool
			ClaimTool.changeInventoryTool(player,ClaimTool.BASIC);
		}
		return true;
	}

	
	/**
	 * Display help message for subcommands
	 * @param sender command sender
	 * @param args command arguments
	 * @return always returns {@code true}, to prevent bukkit usage message
	 */
	private boolean helpCommand(final CommandSender sender, final String args[]) {
	
		// if command sender does not have permission to display help, output error message and return true
		if (!sender.hasPermission("proclaim.command.help")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_HELP_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}
	
		String commandName = "help";
		String helpMessage = "That is not a valid command.";
		
		if (args.length > 1) {
			commandName = args[1]; 
		}

		ProClaimSubcommand match = ProClaimSubcommand.getMatch(commandName);
		
		if (match != null) {
			helpMessage = match.getHelpString();
		}
		
		sender.sendMessage(helpColor + helpMessage);
		displayUsage(sender,commandName);
		return true;
	}


	/**
	 * Reload plugin settings
	 * @param sender command sender
	 * @param args command arguments
	 * @return always returns {@code true}, to prevent bukkit usage message
	 */
	private boolean reloadCommand(final CommandSender sender, final String args[]) {
		
		// if sender does not have permission to reload config, send error message and return true
		if (!sender.hasPermission("proclaim.command.reload")) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_RELOAD_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}
	
		String subcmd = args[0];
		
		// argument limits
		final int minArgs = 1;
		final int maxArgs = 1;
		
		// check min arguments
		if (args.length < minArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_UNDER");
			displayUsage(sender, subcmd);
			return true;
		}
	
		// check max arguments
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_OVER");
			displayUsage(sender, subcmd);
			return true;
		}
		
		// reinstall main configuration file if not present
		plugin.saveDefaultConfig();
		
		// reload main configuration
		plugin.reloadConfig();
	
		// reload enabled worlds
		plugin.worldManager.reload();
		
		// reload messages
		plugin.messageManager.reload();
		
		// reload tool names / lore
		ClaimTool.reload();
	
		// reload datastore
		DataStoreFactory.reload();
		
		// set debug field
		plugin.debug = plugin.getConfig().getBoolean("debug");
		
		// send reloaded message
		plugin.messageManager.sendPlayerMessage(sender,"COMMAND_SUCCESS_RELOAD");
		return true;
	}


	/**
	 * Display plugin settings
	 * @param sender command sender
	 * @param args command arguments
	 * @return always returns {@code true}, to prevent bukkit usage message
	 */
	@SuppressWarnings("UnusedParameters")
	private boolean statusCommand(final CommandSender sender, final String args[]) {
		
		// if command sender does not have permission to view status, output error message and return true
		if (!sender.hasPermission("proclaim.command.status")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_STATUS_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}

		// output config settings
		String versionString = plugin.getDescription().getVersion();

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
	

	/**
	 * Display subcommand usage
	 * @param sender command sender
	 * @param commandName command for which to display usage
	 */
	private void displayUsage(final CommandSender sender, final String commandName) {

		// initialize string to hold copy of commandName
		String cmdName = commandName;
		
		if (commandName.isEmpty() || commandName.equalsIgnoreCase("help")) {
			cmdName = "all";
		}
		
		for (ProClaimSubcommand subcmd : ProClaimSubcommand.values()) {
			if ((subcmd.toString().equalsIgnoreCase(cmdName) || cmdName.equals("all"))
					&& sender.hasPermission("proclaim.command." + subcmd.toString().toLowerCase())) {
				sender.sendMessage(usageColor + subcmd.getUsageString());
			}
		}
	}

}
