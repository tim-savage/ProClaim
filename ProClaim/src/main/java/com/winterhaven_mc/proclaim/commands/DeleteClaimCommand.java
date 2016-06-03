package com.winterhaven_mc.proclaim.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.objects.ClaimTool;
import com.winterhaven_mc.proclaim.storage.PlayerState;


final class DeleteClaimCommand implements CommandExecutor {
	
	// reference to main class
	private final PluginMain plugin;
	
	
	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	DeleteClaimCommand(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// register this class as command executor
		plugin.getCommand("deleteclaim").setExecutor(this);
	}

	/**
	 * Change player claim tool to delete mode
	 * @param sender command sender
	 * @param args command arguments
	 * @return always returns {@code true}, to prevent bukkit usage message
	 */
	@Override
	public final boolean onCommand(final CommandSender sender, final Command command, 
			final String label, final String[] args) {

		// if sender does not have permission for delete mode, output error message and return true
		if (!sender.hasPermission("proclaim.command.deleteclaim")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_DELETECLAIM_PERMISSION");
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
		
		// get player state
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
		
		// if current tool in inventory is not delete tool, switch to delete tool
		if (!ClaimTool.getInventoryTool(player).equals(ClaimTool.DELETE)) {
			ClaimTool.DELETE.onEquip(player);
			
			// send player delete tool equipped message
			plugin.messageManager.sendPlayerMessage(player, "COMMAND_SUCCESS_DELETE_TOOL_EQUIPPED");
		}
		else {
			// change tool in player inventory back to player current tool mode (BASIC, ADMIN, SUBCLAIM)
			ClaimTool.changeInventoryTool(player,playerState.getCurrentToolMode());
		}
		return true;
	}

}
