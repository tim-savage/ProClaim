package com.winterhaven_mc.proclaim.storage;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.highlights.Visualization;
import com.winterhaven_mc.proclaim.objects.ClaimTool;

public final class PlayerState {
	
	// static reference to main class
	private static final PluginMain plugin = PluginMain.instance;

	// mojang assigned globally unique player ID
	private UUID playerUUID;
	
	// player name string
	private String playerName;
	
	// player last login
	private Instant lastLogin = Instant.EPOCH;
	
	// number of claim blocks the player has earned via play time
	private int earnedClaimBlocks = 0;

	// number of claim blocks the player has purchased 
	private int purchasedClaimBlocks = 0;

	// number of claim blocks the player has been granted as bonus blocks 
	private int bonusClaimBlocks = 0;

	// location of player the last time check for earned claim blocks ran
	private Location lastAfkCheckLocation;

	// keep track of player's admin mode status, used to return to correct mode after resizing or deleting
	private boolean adminMode = false;
	
	// tool mode the player is currently in
	private ClaimTool currentToolMode = ClaimTool.BASIC;
	
	// last location the player used the claim tool
	private Location lastToolLocation;

	private Visualization currentVisualization;

	// claim currently being manipulated
	private Claim workingClaim;

	/**
	 * Class constructor
	 * Create empty PlayerState object
	 */
	public PlayerState() { }
	
	/**
	 * Class constructor
	 * create PlayerState object from bukkit player object
	 * @param player bukkit player object used to create new PlayerState object
	 */
	public PlayerState(final Player player) {
		
		this.setPlayerUUID(player.getUniqueId());
		this.setName(player.getName());
		this.setEarnedClaimBlocks(plugin.getConfig().getInt("initial-blocks"));
		this.setPurchasedClaimBlocks(0);
		this.setLastLogin();
		this.setCurrentToolMode(ClaimTool.BASIC);

	}

	public final UUID getPlayerUUID() {
		return playerUUID;
	}

	public final void setPlayerUUID(final UUID playerUUID) {
		this.playerUUID = playerUUID;
	}

	public final String getName() {
		return playerName;
	}

	public final void setName(final String playerName) {
		this.playerName = playerName;
	}

	public final Instant getLastLogin() {
		return lastLogin;
	}

	public final void setLastLogin() {
		this.lastLogin = Instant.now();
	}

	final void setLastLogin(final Instant lastLogin) {
		this.lastLogin = lastLogin;
	}

	final void setLastLogin(final Long lastLogin) {
		this.lastLogin = Instant.ofEpochMilli(lastLogin);
	}

	public final int getEarnedClaimBlocks() {
		return earnedClaimBlocks;
	}

	@SuppressWarnings("unused")
	public final void setEarnedClaimBlocks() {
		this.earnedClaimBlocks = 0;
	}

	public final void setEarnedClaimBlocks(final int earnedClaimBlocks) {
		this.earnedClaimBlocks = earnedClaimBlocks;
	}

	final int getPurchasedClaimBlocks() {
		return purchasedClaimBlocks;
	}

	@SuppressWarnings("unused")
	public final void setPurchasedClaimBlocks() {
		this.purchasedClaimBlocks = 0;
	}

	final void setPurchasedClaimBlocks(final int purchasedClaimBlocks) {
		this.purchasedClaimBlocks = purchasedClaimBlocks;
	}

	public final int getBonusClaimBlocks() {
		return bonusClaimBlocks;
	}

	@SuppressWarnings("unused")
	public void setBonusClaimBlocks() {
		this.bonusClaimBlocks = 0;
	}

	public final void setBonusClaimBlocks(final int bonusClaimBlocks) {
		this.bonusClaimBlocks = bonusClaimBlocks;
	}

	public final Location getLastRecordedLocation() {
		return lastAfkCheckLocation;
	}

	public void setLastAfkCheckLocation(final Location lastAfkCheckLocation) {
		this.lastAfkCheckLocation = lastAfkCheckLocation;
	}

	public final boolean isAdminMode() {
		return adminMode;
	}

	public final void setAdminMode(final boolean adminMode) {
		this.adminMode = adminMode;
	}

	public final ClaimTool getCurrentToolMode() {

		// if null, return basic tool
		if (currentToolMode == null) {
			return ClaimTool.BASIC;
		}
		else {
			return currentToolMode;
		}
	}

	public final void setCurrentToolMode(final ClaimTool currentToolMode) {
		this.currentToolMode = currentToolMode;
	}

	public final Location getLastToolLocation() {
		return lastToolLocation;
	}

	public final void setLastToolLocation(final Location lastToolLocation) {
		this.lastToolLocation = lastToolLocation;
	}
	
	public final Visualization getCurrentVisualization() {
		return currentVisualization;
	}

	public final void setCurrentVisualization(final Visualization currentVisualization) {
		this.currentVisualization = currentVisualization;
	}

	public final Claim getWorkingClaim() {
		return workingClaim;
	}

	public final void setWorkingClaim(final Claim workingClaim) {
		this.workingClaim = workingClaim;
	}

	/**
	 * Insert a new player state record in the datastore
	 */
	public final void insert() {
		plugin.dataStore.insertPlayerState(this);
	}
	
	/**
	 * Update an existing player state record in the datastore
	 */
	public final void update() {
		plugin.dataStore.updatePlayerState(this);
	}
	
	/**
	 * Delete an existing player state record from the datastore
	 */
	public final void delete() {
		plugin.dataStore.deletePlayerState(this.getPlayerUUID());
	}

	public final void removeClaimBlocks(final int blocks) {
		
		int blocksToRemove = blocks;
		
		// if blocks to be removed is zero or less, do nothing and return
		if (blocksToRemove <= 0) {
			return;
		}
		
		// remove blocks from bonus claim blocks first
		// if player has sufficient bonus blocks to cover difference, remove difference and return
		if (blocksToRemove <= this.getBonusClaimBlocks()) {
			this.setBonusClaimBlocks(this.getBonusClaimBlocks() - blocksToRemove);
			return;
		}
		// otherwise, remove all bonus blocks, set blocksToRemove to remainder and continue
		else {
			int bonusClaimBlocksRemoved = this.getBonusClaimBlocks();
			this.setBonusClaimBlocks(0);
			blocksToRemove = blocksToRemove - bonusClaimBlocksRemoved;
		}
		
		// next remove blocks from purchased blocks
		// if player has sufficient purchased blocks to cover difference, remove difference and return
		if (blocksToRemove <= this.getPurchasedClaimBlocks()) {
			this.setPurchasedClaimBlocks(this.getPurchasedClaimBlocks() - blocksToRemove);
			return;
		}
		// otherwise, remove all purchased blocks, set blocksToRemove to remainder and continue
		else {
			int purchasedClaimBlocksRemoved = this.getPurchasedClaimBlocks();
			this.setPurchasedClaimBlocks(0);
			blocksToRemove = blocksToRemove - purchasedClaimBlocksRemoved;
		}
		
		// finally, remove blocks from earned blocks
		if (blocksToRemove <= this.getEarnedClaimBlocks()) {
			this.setEarnedClaimBlocks(this.getEarnedClaimBlocks() - blocksToRemove);
		}
		else {
			this.setEarnedClaimBlocks(0);
		}
	}
	
	
	public final int getTotalClaimBlocks() {	
		return this.getBonusClaimBlocks() + this.getPurchasedClaimBlocks() + this.getEarnedClaimBlocks();
	}

	
	/**
	 * Change conflicting player names to a temporary name until next login
	 */
	public final void resolveNameConflicts() {
	
		if (plugin.debug) {
			plugin.getLogger().info("Resolving name conflicts for " + this.getName());
		}
		
		// get all records whose name matches this player
		final Collection<PlayerState> matchedPlayers = plugin.dataStore.getPlayerRecords(this.getName());
		
		// if result set is empty or null, do nothing and return
		if (matchedPlayers == null || matchedPlayers.isEmpty()) {
			return;
		}
		
		for (PlayerState matchedPlayerState : matchedPlayers) {
			
			// if player with same name has different UUID, change the returned player name
			if (!this.getPlayerUUID().equals(matchedPlayerState.getPlayerUUID())) {
				
				if (plugin.debug) {
					plugin.getLogger().info("A unique player with the same name was found in the datastore.");
				}
				
				// create new name for old player with prefix 'old-' and player key as suffix to guarantee uniqueness
				String newName = "old-" + this.getName();
				matchedPlayerState.setName(newName);
	
				if (plugin.debug) {
					plugin.getLogger().info("Updating " + newName + " in the datastore.");
				}
				// update matchedPlayerData record with new name in datastore
				plugin.dataStore.updatePlayerState(matchedPlayerState);
			}
		}
	}

	
	/**
	 * Get player state by player uuid
	 * @param playerUUID player UUID to retrieve record
	 * @return player state record
	 */
	public static PlayerState getPlayerState(final UUID playerUUID) {
		return plugin.dataStore.getPlayerState(playerUUID);
	}

}
