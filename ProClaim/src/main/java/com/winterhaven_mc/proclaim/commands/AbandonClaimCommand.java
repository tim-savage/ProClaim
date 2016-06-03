package com.winterhaven_mc.proclaim.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.storage.Claim;
import com.winterhaven_mc.proclaim.storage.PlayerState;


final class AbandonClaimCommand implements CommandExecutor {
	
	// reference to main class
	private final PluginMain plugin;
	
	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	AbandonClaimCommand(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// register this class as command executor
		plugin.getCommand("abandonclaim").setExecutor(this);
	}

	/**
	 * Abandon claim ownership
	 */
	@Override
	public final boolean onCommand(final CommandSender sender, final Command command, 
			final String label, final String[] args) {

		// if sender does not have permission for abandonclaim command, output error message and return true
		if (!sender.hasPermission("proclaim.command.abandonclaim")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_ABANDONCLAIM_PERMISSION");
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
		Player player = (Player) sender;
		
		// player world must be enabled
		if (!plugin.worldManager.isEnabled(player.getWorld())) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_WORLD_NOT_ENABLED");
			return true;
		}
		
		// get claim at player location, ignoring height
		final Claim claim = plugin.dataStore.getClaimAt(player.getLocation(),true);
		
		// if no claim at player location, send message and return
		if (claim == null) {
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "COMMAND_FAIL_ABANDONCLAIM_NO_CLAIM");
			return true;
		}
		
		// if claim is a subclaim, send message and return (subclaims cannot be abandoned)
		if (claim.isSubClaim()) {
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "COMMAND_FAIL_ABANDONCLAIM_SUBCLAIM", claim);
			return true;
		}
		
		// get player state
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
		
		// if player is not claim owner or admin, send message and return
		if (!player.getUniqueId().equals(claim.getOwnerUUID())
				|| playerState.isAdminMode()) {
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "COMMAND_FAIL_ABANDONCLAIM_NOT_OWNER", claim);
			return true;
		}

		// abandon claim
		claim.abandon();

		// send player message
		plugin.messageManager.sendPlayerMessage(sender, "COMMAND_SUCCESS_ABANDONCLAIM");
		return true;
	}

}
