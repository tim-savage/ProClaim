package com.winterhaven_mc.proclaim.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.objects.ClaimTool;
import com.winterhaven_mc.proclaim.storage.PlayerState;

final class SubclaimCommand implements CommandExecutor {

	private final PluginMain plugin;
	
	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	SubclaimCommand(final PluginMain plugin) {
		
		// set reference to main
		this.plugin = plugin;
		
		// register this class as command executor
		plugin.getCommand("subclaim").setExecutor(this);
	}


	@Override
	public final boolean onCommand(final CommandSender sender, final Command command, 
			final String label, final String[] args) {
		
		// if sender does not have permission for subclaim mode, output error message and return true
		if (!sender.hasPermission("proclaim.command.subclaim")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_SUBCLAIM_PERMISSION");
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
		
		// Toggle tool between subclaim tool and basic tool or admin tool, depending on player admin mode
		
		// if current tool is not subclaim tool, switch to subclaim tool and set player tool mode
		if (!playerState.getCurrentToolMode().equals(ClaimTool.SUBCLAIM)) {
			ClaimTool.SUBCLAIM.onEquip(player);
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "COMMAND_SUCCESS_SUBCLAIM_TOOL_EQUIPPED");
		}
		else {
			// current tool mode is subclaim, switch back to basic or admin depending on player admin status
			if (playerState.isAdminMode()) {
				ClaimTool.ADMIN.onEquip(player);
			}
			else {
				ClaimTool.BASIC.onEquip(player);
			}
		}
		return true;
	}

}
