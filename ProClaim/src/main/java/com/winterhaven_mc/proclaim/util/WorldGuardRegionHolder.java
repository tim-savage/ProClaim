package com.winterhaven_mc.proclaim.util;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.bukkit.BukkitUtil;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import com.winterhaven_mc.proclaim.storage.Claim;

final class WorldGuardRegionHolder {

	private final ProtectedRegion region;

	/**
	 * Class constructor
	 * @param claim the claim for which to create a WorldGuard region
	 */
	WorldGuardRegionHolder(final Claim claim) {

		// make a WorldGuard region from a claim

		// create unique region name using claim key
		final String regionName = "ProClaim-" + claim.getKey();

		// convert claim corners to BlockVector
		final BlockVector lowerCornerBlockVector = BukkitUtil.toVector(claim.getLowerCorner().getBlock());
		final BlockVector upperCornerBlockVector = BukkitUtil.toVector(claim.getUpperCorner().getBlock());

		this.region = new ProtectedCuboidRegion(regionName, lowerCornerBlockVector, upperCornerBlockVector);

		// set region priority
		this.region.setPriority(10000);

		// set build flag to allow
		this.region.setFlag(DefaultFlag.BUILD, StateFlag.State.ALLOW);
		
		this.region.setDirty(true);
		
	}


	final ProtectedRegion getRegion() {
		return region;
	}


}
