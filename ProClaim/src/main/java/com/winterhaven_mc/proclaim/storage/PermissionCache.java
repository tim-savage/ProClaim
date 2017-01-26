package com.winterhaven_mc.proclaim.storage;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import com.winterhaven_mc.proclaim.PluginMain;

final class PermissionCache {
	
	// reference to main class
	private final PluginMain plugin;

	// Map of permission records by record key
	private final Map<Integer,ClaimPermission> permissionMap;
	
	// index of permission records by claim key
	private final Map<Integer,HashSet<Integer>> claimIndex;
	
	/**
	 * Class constructor
	 * @param plugin reference to plugin main class
	 */
	PermissionCache(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// initialize permission record map
		permissionMap = new ConcurrentHashMap<>();
		
		// initialize claim key index
		claimIndex = new ConcurrentHashMap<>();
	}

	
	/**
	 * Retrieve permission record from cache by permission record key
	 * @param permissionRecordKey key of record to retrieve from cache
	 * @return permission record object or null if not found
	 */
	final ClaimPermission fetch(final Integer permissionRecordKey) {

		// if permissionRecordKey is null, return null record
		if (permissionRecordKey == null) {
			if (plugin.debug) {
				plugin.getLogger().info("Could not fetch permission record from cache by key "
						+ "because the passed key is null.");
			}
			return null;
		}
		return permissionMap.get(permissionRecordKey);
	}
	
	
	/**
	 * Retrieve permission record from cache by claim key and player uuid
	 * @param claimKey key of claim to retrieve from cache
	 * @return permission record object or null if not found
	 */
	final ClaimPermission fetch(final Integer claimKey, final UUID playerUUID) {

		// if claim key is null, log error and return null record
		if (claimKey == null) {
			if (plugin.debug) {
				plugin.getLogger().info("Could not fetch permission record from cache because "
						+ "the passed claim key is null.");
			}
			return null;
		} 
		
		// if player key is null, log error and return null record
		if (playerUUID == null) {
			if (plugin.debug) {
				plugin.getLogger().info("Could not fetch permission record from cache because "
						+ "the passed player UUID is null.");
			}
			return null;
		}

		// iterate through permission records by claim key and return record with matching player key
		if (claimIndex.get(claimKey) != null) {
			for (Integer key : claimIndex.get(claimKey)) {
				if (permissionMap.get(key).getPlayerUUID().equals(playerUUID)) {
					return permissionMap.get(key);
				}
			}
		}
		
		// if no match found, return null
		return null;
	}
	
	
	/**
	 * Insert a permission record into the cache
	 * @param claimPermission record to insert into cache
	 */
	final void store(final ClaimPermission claimPermission) {
		
		// if permission record is null, log error and return
		if (claimPermission == null) {
			plugin.getLogger().warning("Could not store permission record in cache because "
					+ "permission record is null.");
			return;
		}
		
		// if permission record key is null, do not store in cache
		if (claimPermission.getKey() == null) {
			plugin.getLogger().warning("Could not store permission record in cache because "
					+ "permission key is null.");
			return;
		}
		
		// if claimKey is null, do not store in cache
		if (claimPermission.getClaimKey() == null) {
			plugin.getLogger().warning("Could not store permission record in cache because "
					+ "claim key is null.");
			return;
		}
		
		// if playerKey is null, do not store in cache
		if (claimPermission.getPlayerUUID() == null) {
			plugin.getLogger().warning("Could not store permission record in cache because "
					+ "player uuid is null.");
			return;
		}
		
		// insert claim in claim map by permission record key
		permissionMap.put(claimPermission.getKey(), claimPermission);
		
		// TODO: confirm this is correct and necessary
		// remove all index entries for permission record
		for (Integer key : claimIndex.keySet()) {
			claimIndex.get(key).remove(claimPermission.getKey());
		}
		
		// if no index entry for claim key exists, create empty HashSet and insert into index
		if (!claimIndex.containsKey(claimPermission.getClaimKey())) {
			claimIndex.put(claimPermission.getClaimKey(), new HashSet<>());
		}
		
		// insert claim key to record key index entry
		claimIndex.get(claimPermission.getClaimKey()).add(claimPermission.getKey());
	}
	
	
	/**
	 * Remove permission record from cache by permission record key
	 * @param permissionRecordKey key of record to remove from cache
	 */
	final void flush(final Integer permissionRecordKey) {
		
		if (permissionRecordKey == null) {
			if (plugin.debug) {
				plugin.getLogger().info("Could not flush permission record because "
						+ "permission key is null.");
			}
			return;
		}
		
		// get claim key from permission record
		Integer claimKey = permissionMap.get(permissionRecordKey).getClaimKey();
		
		// remove the permission record from the permission map
		permissionMap.remove(permissionRecordKey);
		
		// remove the permission record from the claim index
		claimIndex.get(claimKey).remove(permissionRecordKey);
	}


//	/**
//	 * Remove permission record from cache by permission claim key and player uuid
//	 * @param permissionRecordKey
//	 */
//	final void flush(final Integer claimKey, final Integer playerUUID) {
//
//		if (claimKey == null) {
//			if (plugin.debug) {
//				plugin.getLogger().info("Could not flush permission record from cache because "
//						+ "claim key is null.");
//			}
//			return;
//		}
//		if (playerUUID == null) {
//			if (plugin.debug) {
//				plugin.getLogger().info("Could not flush permission record from cache because "
//						+ "player uuid is null.");
//			}
//			return;
//		}
//
//		int count = 0;
//
//		// iterate through all permission record keys indexed by claim key in map
//		for (Integer permissionRecordKey : claimIndex.get(claimKey)) {
//
//			// if permission record has matching player key
//			if (permissionMap.get(permissionRecordKey).getPlayerUUID().equals(playerUUID)) {
//
//				// remove the permission record from the permission map
//				permissionMap.remove(permissionRecordKey);
//
//				// remove the permission record from the claim index
//				claimIndex.get(claimKey).remove(permissionRecordKey);
//
//				count++;
//			}
//		}
//		if (plugin.debug) {
//			if (count > 0) {
//				plugin.getLogger().info(count + " matching permission record(s) removed from cache.");
//			}
//			else {
//				plugin.getLogger().info("No matching permission record found in cache. "
//						+ "No records removed from cache.");
//			}
//		}
//	}
	
	
//	/**
//	 * Get cache size
//	 * @return integer number of records in map
//	 */
//	private int getSize() {
//		return permissionMap.keySet().size();
//	}


	/**
	 * Get permission map keys for a claim
	 * @return Set of Integer map keys, or empty set if no index entry exists
	 */
	final Set<Integer> getCacheMapKeys(final Integer claimKey) {
		
		// if claim key is null, return empty set
		if (claimKey == null) {
			if (plugin.debug) {
				plugin.getLogger().info("Could not fetch cache map keys because claim key is null.");
			}
			return Collections.emptySet();
		}
		
		// if index for claim key is null, return empty set
		if (claimIndex.get(claimKey) == null) {
			return Collections.emptySet();
		}

		// return unmodifiable set of index entries for claim key
		return Collections.unmodifiableSet(claimIndex.get(claimKey));
	}
}
