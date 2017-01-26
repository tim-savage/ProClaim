package com.winterhaven_mc.proclaim.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.storage.Claim;

final class WorldGuardWrapper {

	// reference to main class
	private final PluginMain plugin;
	
	// reference to WorldGuard plugin
	private WorldGuardPlugin worldGuardPlugin;
	
	// WorldGuard region container
	private RegionContainer regionContainer;

	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	WorldGuardWrapper(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// get WorldGuard plugin reference if installed
		if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
			
			this.worldGuardPlugin = (WorldGuardPlugin) plugin.getServer().getPluginManager().getPlugin("WorldGuard");

			// get WorldGuard region container
		    this.regionContainer = worldGuardPlugin.getRegionContainer();
		}
	}
	
	
	final boolean canBuild(final Player player, final Location location) {
		return !worldGuardPlugin.isEnabled() || worldGuardPlugin.canBuild(player, location);
	}


	final boolean canBuild(final Player player, final Block block) {
		return !worldGuardPlugin.isEnabled() || worldGuardPlugin.canBuild(player, block);
	}
	
	/**
	 * Create WorldGuard region for claim
	 * @param claim the claim for which to create a WorldGuard region
	 */
	final void createRegion(final Claim claim) {
		
		// check if WorldGuard is enabled
		if (worldGuardPlugin != null) {
			
			// do not create region for subclaims
			if (claim.isSubClaim()) {
				return;
			}
			
			// create WorldGuard region for claim
			WorldGuardRegionHolder regionHolder = new WorldGuardRegionHolder(claim);

			RegionManager regions = null;

			// get WorldGuard region manager for claim world
			if (claim.getWorld() != null) {
				regions = this.regionContainer.get(claim.getWorld());
			}

			// add claim region to region manager
			if (regions != null) {
				regions.addRegion(regionHolder.getRegion());
			}
		}
	}
	
	
	/**
	 * synchronize WorldGuard region for claim
	 * @param claim claim to synchronize WorldGuard region
	 */
	final void syncRegion(final Claim claim) {
		
		/*
		 * If region for claim exists, check coordinates and recreate region only if they differ
		 * else if region does not exist, create new region
		 */
		
		// check if WorldGuard is enabled
		if (worldGuardPlugin != null) {
			
			// do not create region for subclaims
			if (claim.isSubClaim()) {
				return;
			}
			
			RegionManager regions = null;

			// get WorldGuard region manager for claim world
			if (claim.getWorld() != null) {
				regions = this.regionContainer.get(claim.getWorld());
			}

			if (regions != null) {

				// if claim region exists...
				if (regions.hasRegion("proclaim-" + claim.getKey())) {
					
					// get existing claim region
					ProtectedRegion claimRegion = regions.getRegion("proclaim-" + claim.getKey());
					
					// if region coordinates match claim coordinates, do nothing and return
					if (claimRegion != null && claimRegion.getMaximumPoint() != null
							&& claim.getUpperCorner().getBlockX() == claimRegion.getMaximumPoint().getBlockX()
							&& claim.getUpperCorner().getBlockZ() == claimRegion.getMaximumPoint().getBlockZ()
							&& claim.getUpperCorner().getBlockY() == claimRegion.getMaximumPoint().getBlockY()) {

						return;
					}
				}
				
				// create (or re-create) region
				this.createRegion(claim);
				if (plugin.debug) {
					plugin.getLogger().info("Region synced for claim " + claim.getKey() + ".");
				}
			}
		}
	}
	
	
	/**
	 * Remove WorldGuard region for claim
	 * @param claim to remove WorldGuard region
	 */
	final void removeRegion(final Claim claim) {

		// check if WorldGuard is enabled
		if (worldGuardPlugin != null) {

			RegionManager regions = null;

			// get WorldGuard region manager for claim world
			if (claim.getWorld() != null) {
				regions = this.regionContainer.get(claim.getWorld());
			}

			// remove claim region from region manager
			if (regions != null) {
				regions.removeRegion("proclaim-" + claim.getKey());
			}
		}
	}
	
	
	/**
	 * Check if a claim overlaps a WorldGuard region in which the player does not have build permission
	 * @param claim claim to check for WorldGuard region overlap
	 * @param player player to check for build permission in overlapping WorldGuard region
	 * @return boolean
	 */
	final boolean overlaps(final Claim claim, final Player player) {
		
		if (worldGuardPlugin == null) {
			return false;
		}
		
		// convert player to WorldGuard LocalPlayer
		LocalPlayer localPlayer = worldGuardPlugin.wrapPlayer(player);

		RegionManager regions = null;

		// get WorldGuard region manager for claim world
		if (claim.getWorld() != null) {
			regions = this.regionContainer.get(claim.getWorld());
		}

		// expand claim coordinates vertically to limits (upper Y is already world max height)
		Location lowerCorner = claim.getLowerCorner().clone();
		lowerCorner.setY(0);
		
		// convert claim corners to BlockVector
		BlockVector lowerCornerBlockVector = BukkitUtil.toVector(lowerCorner.getBlock());
		BlockVector upperCornerBlockVector = BukkitUtil.toVector(claim.getUpperCorner().getBlock());
		
		// create test region from claim coordinates
		ProtectedRegion test = new ProtectedCuboidRegion("proclaim-test-region",
				lowerCornerBlockVector, upperCornerBlockVector);

		ApplicableRegionSet set = null;
		if (regions != null) {
			set = regions.getApplicableRegions(test);
		}

		boolean doesOverlap = false;
		
		// if region build flag is null and player is NOT member then overlap = true
		// else if region build flag is deny then overlap = true
		// (implicit) else overlap = false

		if (set != null) {
			for (ProtectedRegion region : set) {
				if (region.getFlag(DefaultFlag.BUILD) == null) {
					if (!region.isMember(localPlayer)) {
						doesOverlap = true;
						break;
					}
				} else if (State.DENY.equals(region.getFlag(DefaultFlag.BUILD))) {
					doesOverlap = true;
					break;
				}
			}
		}
		return doesOverlap;
	}
}
