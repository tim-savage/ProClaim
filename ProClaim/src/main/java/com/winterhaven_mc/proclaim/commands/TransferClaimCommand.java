package com.winterhaven_mc.proclaim.commands;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.storage.Claim;
import com.winterhaven_mc.proclaim.storage.PlayerState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


final class TransferClaimCommand implements CommandExecutor, TabCompleter {
	
	// reference to main class
	private final PluginMain plugin;
	
	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	TransferClaimCommand(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// register this class as command executor
		plugin.getCommand("transferclaim").setExecutor(this);
	}

	@Override
	public final List<String> onTabComplete(final CommandSender sender, final Command command, 
			final String alias, final String[] args) {

		// create empty return list
		final List<String> returnList = new ArrayList<>();

		if (args.length == 1) {

			// return list of matching online users
			@SuppressWarnings("deprecation")
			final List<Player> matchingPlayers = plugin.getServer().matchPlayer(args[0]);

			//noinspection Convert2streamapi
			for (Player player : matchingPlayers) {
				returnList.add(player.getName());
			}
		}
		return returnList;
	}

	/**
	 * Transfer claim ownership to another player
	 */
	@Override
	public final boolean onCommand(final CommandSender sender, final Command command, 
			final String label, final String[] args) {

		// if sender does not have permission for transferclaim command, output error message and return true
		if (!sender.hasPermission("proclaim.command.transferclaim")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_TRANSFERCLAIM_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}

		// sender must be in game player
		if (!(sender instanceof Player)) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CONSOLE");
			return true;
		}

		// get player object for sender
		final Player player = (Player) sender;
		
		// player world must be enabled
		if (!plugin.worldManager.isEnabled(player.getWorld())) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_WORLD_NOT_ENABLED");
			return true;
		}
		
		// argument limits
		final int minArgs = 1;
		final int maxArgs = 1;
		
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
		
		// get recipient name from args
		final String newOwnerName = args[0];

		// get claim at player location, ignoring claim height
		final Claim claim = plugin.dataStore.getClaimAt(player.getLocation(),true);
		
		// if no claim at location, send error message and return
		if (claim == null) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_TRANSFERCLAIM_NO_CLAIM");
			return true;
		}

		// get player state for command sender
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
		
		// check if player is claim owner or admin
		if (!playerState.isAdminMode() && !claim.getOwnerUUID().equals(playerState.getPlayerUUID())) {
			
			// send not owner message and return
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_TRANSFERCLAIM_NOT_OWNER",claim);
			return true;
		}
		
		// check if claim is subclaim
		if (claim.isSubClaim()) {
			plugin.messageManager.sendPlayerMessage(player, "COMMAND_FAIL_TRANSFERCLAIM_SUBCLAIM", claim);
			return true;
		}
		
		// initialize new owner uuid
		UUID newOwnerUUID;
		
		// check for transfer to admin
		if (newOwnerName.equals("[admin]")) {
			newOwnerUUID = CommandManager.zeroUUID;
		}
		else {
			// find player uuid for recipient
			newOwnerUUID = plugin.commandManager.matchPlayer(newOwnerName);

			// if no matching player, send error message and return
			if (newOwnerUUID == null) {
				plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_PLAYER_NOT_FOUND");
				return true;
			}
		}
		
		// transfer claim
		claim.transfer(newOwnerUUID);
		
		// send player message
		plugin.messageManager.sendPlayerMessage(sender, "COMMAND_SUCCESS_TRANSFERCLAIM",claim);
		
		//TODO: if new owner is an online player, send message
		
		return true;
	}
	
}
