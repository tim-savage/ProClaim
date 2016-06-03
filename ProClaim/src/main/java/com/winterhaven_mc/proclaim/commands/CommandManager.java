package com.winterhaven_mc.proclaim.commands;

import java.util.List;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.winterhaven_mc.proclaim.PluginMain;


/**
 * Instantiates command executor classes for all ProClaim commands and provides helper methods for commands.
 * 
 * @author      Tim Savage
 * @version		1.0
 *  
 */
public final class CommandManager {
	
	// reference to main class
	private final PluginMain plugin;
	
	final static UUID zeroUUID = new UUID(0,0);
	
	/**
	 * constructor method for {@code CommandManager} class
	 * 
	 * @param plugin reference to main class
	 */
	public CommandManager(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// register commands
		new ProClaimCommand(plugin);

		new CreateClaimToolCommand(plugin);
		new AdminCommand(plugin);
		new SubclaimCommand(plugin);
		new DeleteClaimCommand(plugin);
		
		new AbandonClaimCommand(plugin);
		new TransferClaimCommand(plugin);

		new TrustCommand(plugin);
		new AccessTrustCommand(plugin);
		new ContainerTrustCommand(plugin);
		new PermissionTrustCommand(plugin);
		new UntrustCommand(plugin);
		new TrustListCommand(plugin);
		
		new ClaimGroupCommand(plugin);
		
		new StatusCommand(plugin);
		
		new ClaimBlocksCommand(plugin);
		
	}

	
	@SuppressWarnings("deprecation")
	final UUID matchPlayer(final String targetPlayerName) {

		UUID targetPlayerUUID = null;
		
		// check if targetPlayerName is a ProClaim group
		if (targetPlayerName.startsWith("[") && targetPlayerName.endsWith("]")) {
			return zeroUUID;
		}
		
		// first try to match online player
		final List<Player> playerList = plugin.getServer().matchPlayer(targetPlayerName);
		if (playerList.size() == 1) {
			return playerList.get(0).getUniqueId();
		}

		// check all known players for a match
		final OfflinePlayer[] offlinePlayers = plugin.getServer().getOfflinePlayers();
		for (OfflinePlayer offlinePlayer : offlinePlayers) {
			if (targetPlayerName.equalsIgnoreCase(offlinePlayer.getName())) {
				targetPlayerUUID = offlinePlayer.getUniqueId();
			}
		}
		
		// return target player or null if no match
		return targetPlayerUUID;
	}


}
