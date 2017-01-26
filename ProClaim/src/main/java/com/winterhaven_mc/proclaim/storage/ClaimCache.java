package com.winterhaven_mc.proclaim.storage;

import com.winterhaven_mc.proclaim.PluginMain;
import org.bukkit.Location;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ClaimCache {
	
	// reference to main class
	private final PluginMain plugin;

	// Map of claims by claim key
	private final ConcurrentHashMap<Integer,Claim> claimMap;
	
	// Index of claims by owner UUID to claim key
	private final ConcurrentHashMap<UUID,HashSet<Integer>> claimOwnerIndex;
	
	
	/**
	 * Class constructor
	 * @param plugin reference to plugin main class
	 */
	ClaimCache(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// initialize claim map
		claimMap = new ConcurrentHashMap<>();
		
		// initialize claim owner index
		claimOwnerIndex = new ConcurrentHashMap<>();
	}

	
	/**
	 * Get all claims from claim cache map
	 * @return Set of all claims in the cache
	 */
	private Set<Claim> fetchAllClaims() {
		
		// if claim map is empty, return empty set
		if (claimMap.values().isEmpty()) {
			return Collections.emptySet();
		}
		
		// get set of claim map values for return
		final Set<Claim> claims = new HashSet<>(claimMap.values());
		
		// return unmodifiable set of claim map values
		return Collections.unmodifiableSet(claims);
	}
	
//	/**
//	 * Load all claims from datastore into claim cache and populate claim owner index
//	 */
//	final void storeAllClaims() {
//
//		for (Claim claim : plugin.dataStore.getAllClaims()) {
//			claim.setStatus(ClaimStatus.ACTIVE);
//			store(claim);
//		}
//	}

	
	/**
	 * Retrieve a claim by location
	 * @param location location to retreive claim
	 * @return  subclaim if one exists at location,	otherwise top level claim if one exists at location, 
	 * 			otherwise returns null
	 */
	// this may be replaced by a more efficient method in the future, likely using a claim-chunk cache
	final Claim fetchClaimAt(final Location location, final boolean ignoreHeight) {
		
		Claim returnClaim = null;
		
		// iterate through all claims in cache and check for location match
		for (Claim claim : fetchAllClaims()) {
			
			// if claim contains location, set return claim
			if (claim.contains(location, ignoreHeight)) {
				returnClaim = claim;
				
				// if claim is subclaim, stop searching
				if (claim.isSubClaim()) {
					break;
				}
			}
		}
		return returnClaim;
	}

	
//	/**
//	 * Retrieve a top level claim by location
//	 * @param location
//	 * @return top level claim if one exists at location, otherwise null
//	 */
//	// this may be replaced by a more efficient method in the future, likely using a claim-chunk cache
//	final Claim fetchTopClaimAt(final Location location, final boolean ignoreHeight) {
//
//		Claim returnClaim = null;
//
//		// iterate through all claims in cache and check for location match
//		for (Claim claim : fetchAllClaims()) {
//
//			// if claim contains location and is not a subclaim, set return claim and stop searching
//			if (claim.contains(location, ignoreHeight) && !claim.isSubClaim()) {
//				returnClaim = claim;
//				break;
//			}
//		}
//		return returnClaim;
//	}

	
	/**
	 * Retrieve claim from cache by claim key
	 * @param claimKey key for claim to be retrieved from cache
	 * @return claim object or null if not found
	 */
	final Claim fetch(final Integer claimKey) {

		// if claim key is not null, fetch from map
		if (claimKey == null) {
			return null;
		}
		return claimMap.get(claimKey);
	}
	
	
	/**
	 * Get claim keys for owner
	 * @param ownerUUID player UUID for which to retrieve claim keys
	 * @return unmodifiable set of Integer claim keys, or empty set if no keys found
	 */
	private Set<Integer> fetchClaimKeysByOwner(final UUID ownerUUID) {
		
		// if owner uuid is null, return empty set
		if (ownerUUID == null) {
			if (plugin.debug) {
				plugin.getLogger().warning("fetchClaimKeysByOwner was passed a null ownerUUID.");
			}
			return Collections.emptySet();
		}
		
		// return unmodifiable set of claim keys for owner uuid
		return Collections.unmodifiableSet(claimOwnerIndex.get(ownerUUID));
	}
	
	
	/**
	 * Get claims for owner
	 * @param ownerUUID player UUID for which to retrieve owned claims
	 * @return unmodifiable set of claim records, or empty set if no records found
	 */
	final Set<Claim> fetchClaimsByOwner(final UUID ownerUUID) {

		// if owner uuid is null, return empty Set
		if (ownerUUID == null) {
			if (plugin.debug) {
				plugin.getLogger().warning("Could not fetch claims for null owner uuid.");
			}
			return Collections.emptySet();
		}

		// if no matching claims for owner uuid, return empty Set
		if (fetchClaimKeysByOwner(ownerUUID).isEmpty()) {
			return Collections.emptySet();
		}
		
		// create empty HashSet for return
		final Set<Claim> returnSet = new HashSet<>();
		
		// add each claim with matching owner uuid to return set
		for (Integer claimKey : fetchClaimKeysByOwner(ownerUUID)) {
			returnSet.add(claimMap.get(claimKey));
		}

		// return set of claims
		return Collections.unmodifiableSet(returnSet);
	}


	/**
	 * Retrieve a set of all child claims
	 * @param claimKey key of claim to retrieve child claims
	 * @return set of child claims
	 */
	final Set<Claim> fetchChildClaims(final Integer claimKey) {
		
		// if claim key is null, return empty Set
		if (claimKey == null) {
			if (plugin.debug) {
				plugin.getLogger().warning("Could not fetch child claims for null claim key.");
			}
			return Collections.emptySet();
		}

		// create new HashSet for return
		HashSet<Claim> returnSet = new HashSet<>();
		
		// iterate over all claims and add to return set if parent key matches parameter claimKey
		for (Claim claim : fetchAllClaims()) {
			if (claim.getParentKey().equals(claimKey)) {
				returnSet.add(claim);
			}
		}
		return Collections.unmodifiableSet(returnSet);
	}
	
	
	/**
	 * Insert a claim into the cache
	 * @param claim record to store in cache
	 */
	final void store(final Claim claim) {
		
		final Integer claimKey = claim.getKey();
		final UUID ownerUUID = claim.getOwnerUUID();
		
		// if claimKey is null, do not store in cache
		if (claimKey == null) {
			plugin.getLogger().warning("Could not store claim in cache because "
					+ "claim key is null.");
			return;
		}
		// insert claim in claim map by claim key
		claimMap.put(claimKey, claim);
		
		// if ownerUUID is null, do not index
		if (ownerUUID == null) {
			if (plugin.debug) {
				plugin.getLogger().info("Could not index ownerUUID for claim " 
						+ claimKey + " because ownerUUID is null.");
			}
			return;
		}
		
		// remove claim key from all index entries, in case claim owner has changed
		for (UUID playerUUID : claimOwnerIndex.keySet()) {
			claimOwnerIndex.get(playerUUID).remove(claimKey);
		}
		
		// if claim owner index does not contain owner UUID, create empty set
		if (!claimOwnerIndex.containsKey(ownerUUID)) { 
			claimOwnerIndex.put(ownerUUID, new HashSet<>());
		}
		
		// insert claim key into index by owner UUID
		claimOwnerIndex.get(ownerUUID).add(claimKey);
	}
	
	
	/**
	 * Remove claim from cache by claimKey<br>
	 * also remove claim owner index entry
	 * @param claimKey key of claim record to be removed from cache
	 */
	final void flush(final Integer claimKey) {
		
		// get claim
		final Claim claim = fetch(claimKey);

		// get the owner uuid of the claim
		final UUID ownerUUID = claim.getOwnerUUID();

		// remove the claim from the owner index
		claimOwnerIndex.get(ownerUUID).remove(claimKey);
		
		// remove the claim from the claim map
		claimMap.remove(claimKey);
	}

	/**
	 * Get number of claims stored in cache
	 * @return integer number of claims in cache
	 */
	final int getSize() {
		return claimMap.keySet().size();
	}

//	/**
//	 * Get claim map keys
//	 * @return unmodifiable set of Integer map keys
//	 */
//	final Set<Integer> getCacheMapKeys() {
//		return Collections.unmodifiableSet(claimMap.keySet());
//	}
	
	
//	/**
//	 * Get claim owner index keys
//	 * @return unmodifiable set of UUID claim owner index keys
//	 */
//	final Set<UUID> getClaimOwnerIndexKeys() {
//		return Collections.unmodifiableSet(claimOwnerIndex.keySet());
//	}

}
