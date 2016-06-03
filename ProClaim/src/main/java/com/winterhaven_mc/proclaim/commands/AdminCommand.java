package com.winterhaven_mc.proclaim.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.objects.ClaimTool;
import com.winterhaven_mc.proclaim.storage.PlayerState;

final class AdminCommand implements CommandExecutor {

	private final PluginMain plugin;
	
	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	AdminCommand(final PluginMain plugin) {
		
		// set reference to main
		this.plugin = plugin;
		
		// register this class as command executor
		plugin.getCommand("pcadmin").setExecutor(this);
	}

	@Override
	public final boolean onCommand(final CommandSender sender, final Command command, 
			final String label, final String[] args) {
		
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
		
		// if player is not in admin mode, switch to admin tool
		if (!playerState.isAdminMode()) {
			ClaimTool.ADMIN.onEquip(player);
			
			// send player admin tool equipped message
			plugin.messageManager.sendPlayerMessage(player, "COMMAND_SUCCESS_ADMIN_TOOL_EQUIPPED");
		}
		// otherwise switch to basic tool
		else {
			ClaimTool.BASIC.onEquip(player);
		}
		return true;
	}

}
