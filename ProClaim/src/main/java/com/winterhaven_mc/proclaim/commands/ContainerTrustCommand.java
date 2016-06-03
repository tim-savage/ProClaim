package com.winterhaven_mc.proclaim.commands;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.objects.PermissionLevel;
import com.winterhaven_mc.proclaim.storage.Claim;
import com.winterhaven_mc.proclaim.storage.ClaimPermission;
import com.winterhaven_mc.proclaim.storage.PlayerState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


final class ContainerTrustCommand implements CommandExecutor, TabCompleter {
	
	// reference to main class
	private final PluginMain plugin;
	
	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	ContainerTrustCommand(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// register this class as command executor
		plugin.getCommand("containertrust").setExecutor(this);
	}

	@Override
	public final List<String> onTabComplete(final CommandSender sender, final Command command, 
			final String alias, final String[] args) {

		// get command sender uuid
		UUID senderUUID = null;
		if (sender instanceof Player) {
			senderUUID = ((Player) sender).getUniqueId();
		}
		
		// create empty return list
		final List<String> returnList = new ArrayList<>();

		if (args.length == 1) {

			// return list of matching online users
			@SuppressWarnings("deprecation")
			final List<Player> matchingPlayers = plugin.getServer().matchPlayer(args[0]);

			// add matching players to return list, omitting command sender
			for (Player player : matchingPlayers) {
				if (senderUUID == null || !senderUUID.equals(player.getUniqueId())) {
					returnList.add(player.getName());
				}
			}
		}
		return returnList;
	}

	/**
	 * Give build trust for a claim to another player
	 */
	@Override
	public final boolean onCommand(final CommandSender sender, final Command command, 
			final String label, final String[] args) {

		// if sender does not have permission for trust command, output error message and return true
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
		final String recipientName = args[0];

		// get player state for command sender
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
		
		// get claim at player location, ignoring claim height
		final Claim claim = plugin.dataStore.getClaimAt(player.getLocation(),true);
		
		// if no claim at location, send error message and return
		if (claim == null) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_TRUST_NO_CLAIM");
			return true;
		}

		// get claim permission for player
		final ClaimPermission claimPermission = 
				ClaimPermission.getClaimPermission(claim.getKey(), playerState.getPlayerUUID());
		
		// check if player is claim owner or admin or has grant for container permission
		if (!playerState.isAdminMode() && !claim.getOwnerUUID().equals(playerState.getPlayerUUID())
				&& !claimPermission.canGrant(PermissionLevel.CONTAINER)) {
			
			// send not owner message and return
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_GRANT_CONTAINER_TRUST",claim);
			return true;
		}

		// initialize recipientUUID
		UUID recipientUUID;
		
		// check for trust to public
		if (recipientName.equals("[public]")) {
			recipientUUID = CommandManager.zeroUUID;
		}
		else {
			// find player uuid for recipient
			recipientUUID = plugin.commandManager.matchPlayer(recipientName);

			// if no matching player, send error message and return
			if (recipientUUID == null) {
				plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_PLAYER_NOT_FOUND");
				return true;
			}
		}
		
		// get recipient playerState
		final PlayerState recipientPlayerState = PlayerState.getPlayerState(recipientUUID);
		
		if (recipientPlayerState == null) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_PLAYER_NOT_FOUND");
			return true;
		}
		
		// get recipient current permission level for claim
		final ClaimPermission recipientClaimPermission = 
				ClaimPermission.getClaimPermission(claim.getKey(), recipientUUID);
		
		// if recipient has grant permission, set permission level to container + grant
		if (recipientClaimPermission != null && recipientClaimPermission.allows(PermissionLevel.GRANT)) {
				claim.setPermission(recipientUUID, PermissionLevel.CONTAINER_GRANT);
		}
		// else set permission level to container
		else {
			claim.setPermission(recipientUUID, PermissionLevel.CONTAINER);
		}

		// send player success message
		plugin.messageManager.sendPlayerMessage(sender, "COMMAND_SUCCESS_CONTAINER_TRUST",claim);

		// TODO: if recipient is online player, send message
		
		return true;
	}

	
}
