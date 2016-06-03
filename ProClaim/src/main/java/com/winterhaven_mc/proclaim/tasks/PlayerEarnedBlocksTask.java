package com.winterhaven_mc.proclaim.tasks;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.storage.PlayerState;

public final class PlayerEarnedBlocksTask extends BukkitRunnable {

    private final PluginMain plugin;
    private final Player player;

    
    /**
     * Class constructor
     * @param plugin
     * @param player
     */
    public PlayerEarnedBlocksTask(final PluginMain plugin, final Player player) {
        this.plugin = plugin;
        this.player = player;
    }

	@Override
	public void run() {

		// get player state
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
		
		// if player has not moved more than three blocks since last run, do nothing and return
		if (player.getWorld().equals(playerState.getLastRecordedLocation().getWorld())
				&& player.getLocation().distanceSquared(playerState.getLastRecordedLocation()) < 9) {
			
			return;
		}
		
		// get configured earned blocks per hour
		final int blocksPerHour = plugin.getConfig().getInt("blocks-per-hour");
		
		// get players earned blocks
		final int playerEarnedBlocks = playerState.getEarnedClaimBlocks();
		
		// new total
		final int newTotal = playerEarnedBlocks + (blocksPerHour / 12);
		
		// give player blocks (divided by 12, as task runs every five minutes)
		playerState.setEarnedClaimBlocks(newTotal);
		
		// set player last afk location
		playerState.setLastAfkCheckLocation(player.getLocation());
	}

}
