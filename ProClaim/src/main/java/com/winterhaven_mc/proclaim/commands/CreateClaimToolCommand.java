package com.winterhaven_mc.proclaim.commands;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.objects.ClaimTool;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;


final class CreateClaimToolCommand implements CommandExecutor {
	
	// reference to main class
	private final PluginMain plugin;
	
	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	CreateClaimToolCommand(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// register this class as command executor
		plugin.getCommand("claimtool").setExecutor(this);
	}

	/**
	 * Place new tool in player inventory
	 * @param sender command sender
	 * @param args command arguments
	 * @return always returns {@code true}, to prevent bukkit usage message
	 */
	@Override
	public final boolean onCommand(final CommandSender sender, final Command command, 
			final String label, final String[] args) {

		// sender must be player
		if (!(sender instanceof Player)) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_CONSOLE");
			return true;
		}
		
		// get player
		final Player player = (Player) sender;
		
		// player world must be enabled
		if (!plugin.worldManager.isEnabled(player.getWorld())) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_WORLD_NOT_ENABLED");
			return true;
		}
		
		// check player permissions
		if (!player.hasPermission("proclaim.command.claimtool")) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_CLAIMTOOL_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}
		
		// if one-tool-inventory is configured true and player already has item in inventory, send message and return
		if (plugin.getConfig().getBoolean("one-tool-inventory") 
				&& ClaimTool.getInventoryTool(player) != null) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_CLAIMTOOL_INVENTORY_TOOL_EXISTS");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}
		
		// put tool in player's inventory
		final HashMap<Integer,ItemStack> noFit = player.getInventory().addItem(ClaimTool.create(ClaimTool.BASIC));
		
		// if player inventory is full, send message and play command fail sound
		if (!noFit.isEmpty()) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_CLAIMTOOL_INVENTORY_FULL");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}
		// if sound effects enabled, play success sound to player
		plugin.soundManager.playerSound(sender, "give-tool");
		return true;
	}
	
}
