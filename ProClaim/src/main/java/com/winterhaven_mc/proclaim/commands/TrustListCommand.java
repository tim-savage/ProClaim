package com.winterhaven_mc.proclaim.commands;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.objects.PermissionLevel;
import com.winterhaven_mc.proclaim.storage.Claim;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;


final class TrustListCommand implements CommandExecutor {
	
	// reference to main class
	private final PluginMain plugin;
	
	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	TrustListCommand(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// register this class as command executor
		plugin.getCommand("trustlist").setExecutor(this);
	}

	/**
	 * Display trust list
	 */
	@Override
	public final boolean onCommand(final CommandSender sender, final Command command, 
			final String label, final String[] args) {

		// if sender does not have permission for trust list command, output error message and return true
		if (!sender.hasPermission("proclaim.command.trust")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_TRUST_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}

		// sender must be in game player
		if (!(sender instanceof Player)) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CONSOLE");
			return true;
		}

		// get player
		final Player player = (Player) sender;
		
		// player world must be enabled
		if (!plugin.worldManager.isEnabled(player.getWorld())) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_WORLD_NOT_ENABLED");
			return true;
		}
		
		// argument limits
		final int minArgs = 0;
		final int maxArgs = 0;
		
		// check min arguments
		if (args.length < minArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_UNDER");
			return false;
		}
	
		// check max arguments
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_OVER");
			return false;
		}
		
		// get claim at player location, ignoring height
		final Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true);

		// if no claim at player location, send message and return
		if (claim == null) {
			plugin.messageManager.sendPlayerMessage(player, "COMMAND_FAIL_TRUSTLIST_NO_CLAIM");
			return true;
		}

		//TODO: if no permission to view permissions, send error message and return

		final HashMap<PermissionLevel,ArrayList<String>> permissionsMap = claim.getPermissionMap();

		player.sendMessage("Explicit permissions here:");

		StringBuilder permissionsString = new StringBuilder();
		permissionsString.append(ChatColor.GOLD);
		permissionsString.append("M: ");

		for (String playerName : permissionsMap.get(PermissionLevel.GRANT)) {
			permissionsString.append(playerName);
			permissionsString.append(" ");
		}
		for (String playerName : permissionsMap.get(PermissionLevel.ACCESS_GRANT)) {
			permissionsString.append(playerName);
			permissionsString.append(" ");
		}
		for (String playerName : permissionsMap.get(PermissionLevel.CONTAINER_GRANT)) {
			permissionsString.append(playerName);
			permissionsString.append(" ");
		}
		for (String playerName : permissionsMap.get(PermissionLevel.BUILD_GRANT)) {
			permissionsString.append(playerName);
			permissionsString.append(" ");
		}

		player.sendMessage(permissionsString.toString());

		permissionsString = new StringBuilder();
		permissionsString.append(ChatColor.YELLOW);
		permissionsString.append("B: ");

		for (String playerName : permissionsMap.get(PermissionLevel.BUILD)) {
			permissionsString.append(playerName);
			permissionsString.append(" ");
		}
		for (String playerName : permissionsMap.get(PermissionLevel.BUILD_GRANT)) {
			permissionsString.append(playerName);
			permissionsString.append(" ");
		}
		player.sendMessage(permissionsString.toString());

		permissionsString = new StringBuilder();
		permissionsString.append(ChatColor.GREEN);
		permissionsString.append("C: ");

		for (String playerName : permissionsMap.get(PermissionLevel.CONTAINER)) {
			permissionsString.append(playerName);
			permissionsString.append(" ");
		}
		for (String playerName : permissionsMap.get(PermissionLevel.CONTAINER_GRANT)) {
			permissionsString.append(playerName);
			permissionsString.append(" ");
		}
		player.sendMessage(permissionsString.toString());

		permissionsString = new StringBuilder();
		permissionsString.append(ChatColor.BLUE);
		permissionsString.append("A: ");

		for (String playerName : permissionsMap.get(PermissionLevel.ACCESS)) {
			permissionsString.append(playerName);
			permissionsString.append(" ");
		}
		for (String playerName : permissionsMap.get(PermissionLevel.ACCESS_GRANT)) {
			permissionsString.append(playerName);
			permissionsString.append(" ");
		}
		player.sendMessage(permissionsString.toString());

		player.sendMessage("(M-manager, B-builder, C-containers, A-access)");

		return true;
	}

}
