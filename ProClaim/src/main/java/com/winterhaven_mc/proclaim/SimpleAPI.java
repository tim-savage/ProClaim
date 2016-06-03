package com.winterhaven_mc.proclaim;

import com.winterhaven_mc.proclaim.objects.PermissionLevel;
import com.winterhaven_mc.proclaim.storage.Claim;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

@SuppressWarnings("unused")
public class SimpleAPI {
	
	/**
	 * Private constructor to prevent instantiation of this class
	 */
	private SimpleAPI() {
		throw new AssertionError();
	}

	private static final PluginMain plugin = PluginMain.instance;
	
	/**
	 * Check if a player has access trust at a given location
	 * @param player
	 * @param location
	 * @return true if player has access trust at location or no claim exists at location;
	 * false if a claim exists at location and player does not have access trust
	 */
	public static boolean hasAccessTrust(final Player player, final Location location) {
		
		// get claim at location
		Claim claim = plugin.dataStore.getClaimAt(location);
		
		// if no claim at location, return true
		if (claim == null) {
			return true;
		}
		
		// if player is claim owner, return true
		if (player.getUniqueId().equals(claim.getOwnerUUID())) {
			return true;
		}
		
		// return result of permission check
		return claim.allows(player.getUniqueId(), PermissionLevel.ACCESS);
	}
	
	/**
	 * Check if a player has container trust at a given location
	 * @param player
	 * @param location
	 * @return true if player has container trust at location or no claim exists at location;
	 * false if a claim exists at location and player does not have container trust
	 */
	public static boolean hasContainerTrust(final Player player, final Location location) {
		
		// get claim at location
		Claim claim = plugin.dataStore.getClaimAt(location);
		
		// if no claim at location, return true
		if (claim == null) {
			return true;
		}
		
		// if player is claim owner, return true
		if (player.getUniqueId().equals(claim.getOwnerUUID())) {
			return true;
		}
		
		// return result of permission check
		return claim.allows(player.getUniqueId(), PermissionLevel.CONTAINER);
	}
	
	/**
	 * Check if a player has build trust at a given location
	 * @param player
	 * @param location
	 * @return true if player has build trust at location or no claim exists at location;
	 * false if a claim exists at location and player does not have build trust
	 */
	public static boolean hasBuildTrust(final Player player, final Location location) {

		// get claim at location
		Claim claim = plugin.dataStore.getClaimAt(location);
		
		// if no claim at location, return true
		if (claim == null) {
			return true;
		}
		
		// if player is claim owner, return true
		if (player.getUniqueId().equals(claim.getOwnerUUID())) {
			return true;
		}
		
		// return result of permission check
		return claim.allows(player.getUniqueId(), PermissionLevel.BUILD);
	}
	
	/**
	 * Check if a player has grant trust at a given location
	 * @param player
	 * @param location
	 * @return true if player has grant trust at location or no claim exists at location;
	 * false if a claim exists at location and player does not have grant trust
	 */
	public static boolean hasGrantTrust(final Player player, final Location location) {
		
		// get claim at location
		Claim claim = plugin.dataStore.getClaimAt(location);
		
		// if no claim at location, return true
		if (claim == null) {
			return true;
		}
		
		// if player is claim owner, return true
		if (player.getUniqueId().equals(claim.getOwnerUUID())) {
			return true;
		}
		
		// return result of permission check
		return claim.allows(player.getUniqueId(), PermissionLevel.GRANT);
	}
	
	/**
	 * Get the player UUID for the owner of a claim at a given location 
	 * @param location
	 * @return player UUID for owner of a claim. If no claim exists at location, returns null.
	 * If claim is an admin (server owned) claim, UUID will be all zeros.
	 * An all zero UUID can be created for comparison by using the 
	 * java.util.UUID class constructor:<br>
	 * {@code UUID allZeroUUID = new UUID(0,0);}
	 */
	public static UUID getClaimOwnerUUID(final Location location) {
		return plugin.dataStore.getClaimAt(location).getOwnerUUID(); 
	}
}
