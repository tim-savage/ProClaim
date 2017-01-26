package com.winterhaven_mc.proclaim.util;

import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.storage.Claim;

public final class WorldGuardHelper {

	// reference to main class
	@SuppressWarnings("FieldCanBeLocal")
	private final PluginMain plugin;

	private boolean worldGuardPresent;

	private WorldGuardWrapper worldGuardWrapper;
	
	/**
	 * Class constructor
	 * @param plugin reference to plugin main class
	 */
	public WorldGuardHelper(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// get WorldGuard wrapper instance if WorldGuard is installed
		if (this.plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
			
			try {
				this.worldGuardWrapper = new WorldGuardWrapper(plugin);
				this.worldGuardPresent = true;
				this.plugin.getLogger().info("WorldGuard v"
						+ this.plugin.getServer().getPluginManager().getPlugin("WorldGuard")
						.getDescription().getVersion() + " detected.");
			}
			catch (Exception e) {
				this.plugin.getLogger().warning("WorldGuard v"
						+ this.plugin.getServer().getPluginManager().getPlugin("WorldGuard")
						.getDescription().getVersion() + " detected, but could not establish reference.");
				this.worldGuardPresent = false;
				e.printStackTrace();
			}
		}
		else {
			this.plugin.getLogger().info("WorldGuard not detected.");
		}
	}
	
	
	public final boolean isWorldGuardPresent() {
		return this.worldGuardPresent;
	}
	
	@SuppressWarnings("unused")
	public final boolean canBuild(final Player player, final Location location) {
		return !worldGuardPresent || worldGuardWrapper.canBuild(player, location);
	}
	
	public final boolean canBuild(final Player player, final Block block) {
		return !worldGuardPresent || worldGuardWrapper.canBuild(player, block);
	}
	
	/**
	 * Create WorldGuard region for claim
	 * @param claim claim to create WorldGuard region
	 */
	public final void createRegion(final Claim claim) {

		if (worldGuardPresent) {
			worldGuardWrapper.createRegion(claim);
		}
	}
	
	
	/**
	 * Synchronize WorldGuard region for claim
	 * @param claim claim to synchronize WorldGuard region
	 */
	public final void syncRegion(final Claim claim) {

		if (worldGuardPresent) {
			worldGuardWrapper.syncRegion(claim);
		}
	}
	
	
	/**
	 * Remove WorldGuard region for claim
	 * @param claim claim to remove WorldGuard region
	 */
	public final void removeRegion(final Claim claim) {
		
		if (worldGuardPresent) {
			
			// get new HashSet of child claims
			HashSet<Claim> removedClaims = new HashSet<>(claim.getChildClaims());
			
			// add this claim to HashSet
			removedClaims.add(claim);
			
			// iterate through HashSet and remove all claim regions
			for (Claim removedClaim : removedClaims) {
				worldGuardWrapper.removeRegion(removedClaim);
			}
		}
	}
	
	
	public final boolean overlaps(final Claim claim, final Player player) {
		return worldGuardPresent && worldGuardWrapper.overlaps(claim, player);
	}
}
