package com.winterhaven_mc.proclaim.tasks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import com.winterhaven_mc.proclaim.PluginMain;

public final class TaskManager {

	// reference to main class
	private final PluginMain plugin;
	
	private final HashMap<UUID,HashSet<BukkitTask>> playerTaskMap;
	
	/**
	 * Class constructor
	 * @param plugin reference to plugin main class
	 */
	public TaskManager(final PluginMain plugin) {
		this.plugin = plugin;
		
		// initalize playerTaskMap
		this.playerTaskMap = new HashMap<>();
		
		// start expire claims task, repeat every hour after initial no delay run
		new ExpireClaimsTask(plugin).runTaskTimer(plugin,0,72000);
	}
	
	/**
	 * Start task to accrue player earned blocks
	 * @param player player to accrue earned blocks
	 */
	public final void startPlayerEarnedBlocksTask(final Player player) {
		
		// start task, repeat every five minutes after five minute start delay
        final BukkitTask playerTask = new PlayerEarnedBlocksTask(plugin,player).runTaskTimer(plugin,6000,6000);
        
        // get player task hashset from map
        HashSet<BukkitTask> playerTasks = playerTaskMap.get(player.getUniqueId());

        // if playerTasks HashSet is null, create a new one
        if (playerTasks == null) {
        	playerTasks = new HashSet<>();
        }
        
        // add task to hashset
        playerTasks.add(playerTask);
        
        // put task in hashmap
        playerTaskMap.putIfAbsent(player.getUniqueId(), playerTasks);
        
        if (plugin.debug) {
        	plugin.getLogger().info("Started player earned blocks task for " + player.getName() + ".");
        }
        
	}
	
	
	/**
	 * Cancel all player tasks
	 * @param player cancel recurring tasks for this player
	 */
	public final void cancelPlayerTasks(final Player player) {
		
		// get HashSet of tasks from playerTaskMap
		final HashSet<BukkitTask> playerTasks = playerTaskMap.get(player.getUniqueId());
		
		// cancel each task
		for (BukkitTask playerTask : playerTasks) {
			playerTask.cancel();
		}
		
		// remove player from TaskMap
		playerTaskMap.remove(player.getUniqueId());
	
        if (plugin.debug) {
        	plugin.getLogger().info("Cancelled all player tasks for " + player.getName() + ".");
        }
	}
	
}
