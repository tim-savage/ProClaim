package com.winterhaven_mc.proclaim.util;

import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.storage.Claim;

public final class WorldGuardHelper {

	// reference to main class
	private final PluginMain plugin;
	
	private boolean worldGuardPresent;

	private WorldGuardWrapper worldGuardWrapper;
	
	/**
	 * Class constructor
	 * @param plugin
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
	
	public final boolean canBuild(final Player player, final Location location) {
		
		if (worldGuardPresent) {
			return worldGuardWrapper.canBuild(player, location);
		}
		return true;
	}
	
	public final boolean canBuild(final Player player, final Block block) {
		
		if (worldGuardPresent) {
			return worldGuardWrapper.canBuild(player, block);
		}
		return true;
	}
	
	/**
	 * Create WorldGuard region for claim
	 * @param claim
	 */
	public final void createRegion(final Claim claim) {

		if (worldGuardPresent) {
			worldGuardWrapper.createRegion(claim);
		}
	}
	
	
	/**
	 * Synchronize WorldGuard region for claim
	 * @param claim
	 */
	public final void syncRegion(final Claim claim) {

		if (worldGuardPresent) {
			worldGuardWrapper.syncRegion(claim);
		}
	}
	
	
	/**
	 * Remove WorldGuard region for claim
	 * @param claim
	 */
	public final void removeRegion(final Claim claim) {
		
		if (worldGuardPresent) {
			
			// get new HashSet of child claims
			HashSet<Claim> removedClaims = new HashSet<Claim>(claim.getChildClaims());
			
			// add this claim to HashSet
			removedClaims.add(claim);
			
			// iterate through HashSet and remove all claim regions
			for (Claim removedClaim : removedClaims) {
				worldGuardWrapper.removeRegion(removedClaim);
			}
		}
	}
	
	
	public final boolean overlaps(final Claim claim, final Player player) {
		
		if (worldGuardPresent) {
			return worldGuardWrapper.overlaps(claim, player);
		}
		else {
			return false;
		}
	}
}
