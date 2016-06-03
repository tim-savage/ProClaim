package com.winterhaven_mc.proclaim.commands;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.storage.PlayerState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;


final class ClaimBlocksCommand implements CommandExecutor {
	
	// reference to main class
	private final PluginMain plugin;
	
	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	ClaimBlocksCommand(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// register this class as command executor
		plugin.getCommand("claimblocks").setExecutor(this);
	}

	/**
	 * Manage claim blocks
	 */
	@SuppressWarnings("SimplifiableIfStatement")
	@Override
	public final boolean onCommand(final CommandSender sender, final Command command, 
			final String label, final String[] args) {

		// if sender does not have permission for claimblock command, output error message and return true
		if (!sender.hasPermission("proclaim.command.claimblocks")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMBLOCK_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}
		
		// argument limits
		final int maxArgs = 4;
		
		// check max arguments
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_OVER");
			return false;
		}
		
		// if no arguments, display claim blocks for sender
		if (args.length == 0) {
			return displayClaimBlocks(sender, args);
		}
		
		// get subcommand
		String subcommand = args[0];


		if (subcommand.equalsIgnoreCase("show")) {
			return displayClaimBlocks(sender, args);
		}
		else if (subcommand.equalsIgnoreCase("give")) {
			return giveClaimBlocks(sender, args);
		}
		else {
			return false;
		}
	}

	
	/**
	 * Display claim blocks remaining for a player
	 * @param sender the command sender
	 * @param args the command arguments
	 * @return always returns {@code true}, to prevent bukkit usage message
	 */
	private boolean displayClaimBlocks(CommandSender sender, String[] args) {

		// if sender does not have permission for claimblocks show subcommand, output error message and return true
		if (!sender.hasPermission("proclaim.command.claimblocks.show")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMBLOCKS_SHOW_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}

		// argument limits
		int maxArgs = 2;
		
		// check max arguments
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_OVER");
			return false;
		}		
		
		// if no arguments, display claim blocks for sender
		
		// if no player given, use sender as player
		if (args.length == 0) {

			// sender must be in game player
			if (!(sender instanceof Player)) {
				plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CONSOLE");
				return true;
			}
			
			// send player message showing claim blocks for self
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_SUCCESS_CLAIMBLOCKS_SELF_TOTAL");
			return true;
		}
		
		// minimum arguments
		int minArgs = 2;
		
		// check min arguments
		if (args.length < minArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_UNDER");
			return false;
		}

		// get player name from arguments
		String playerName = args[1];

		// get uuid for player name
		UUID playerUUID = plugin.commandManager.matchPlayer(playerName);
		
		// get player state for player uuid
		PlayerState playerState = PlayerState.getPlayerState(playerUUID);
		
		// if no player match found, send message and return true
		if (playerState == null) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_PLAYER_NOT_FOUND");
			return true;
		}
		
		// send player blocks given message
		plugin.messageManager.sendPlayerMessage(sender, "COMMAND_SUCCESS_CLAIMBLOCKS_OTHER_TOTAL", playerUUID);
		return true;
	}

	
	/**
	 * Give claim blocks to a player
	 * @param sender the command sender
	 * @param args the command arguments
	 * @return always returns {@code true}, to prevent bukkit usage message
	 */
	private boolean giveClaimBlocks(CommandSender sender, String[] args) {
		
		// if sender does not have permission for claimblocks show subcommand, output error message and return true
		if (!sender.hasPermission("proclaim.command.claimblocks.give")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMBLOCKS_GIVE_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}

		// argument limits
		int minArgs = 3;
		int maxArgs = 3;
		
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
		
		// get target player name from arguments
		String targetPlayerName = args[1];
		
		// get target player uuid
		UUID targetPlayerUUID = plugin.commandManager.matchPlayer(targetPlayerName);
		
		// match targetName with player
		PlayerState playerState = PlayerState.getPlayerState(targetPlayerUUID);
		
		// if no player match found, send message and return true
		if (playerState == null) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_PLAYER_NOT_FOUND");
			return true;
		}

		//noinspection UnusedAssignment
		int quantity = 0;

		// try to parse args[2] as int
		try {
			quantity = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			
			// send player integer parse error message and return
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_SET_INVALID_INTEGER");
			return false;
		}

		// add quantity to player claim blocks
		playerState.setBonusClaimBlocks(playerState.getBonusClaimBlocks() + quantity);
		
		// update player state in datastore
		playerState.update();
		
		// send message to giver
		plugin.messageManager.sendPlayerMessage(sender, "COMMAND_SUCCESS_CLAIMBLOCKS_GIVE", targetPlayerUUID);
		
		return true;
	}
	
}
