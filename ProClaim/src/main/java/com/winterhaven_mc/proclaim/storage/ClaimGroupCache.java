package com.winterhaven_mc.proclaim.storage;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.winterhaven_mc.proclaim.PluginMain;

final class ClaimGroupCache {
	
	// reference to main class
	private final PluginMain plugin;

	// Map of claim groups by claim group key
	private final Map<Integer,ClaimGroup> claimGroupMap;
	
	// Index of claim groups by name
	private final Map<String, Integer> claimGroupNameIndex;

	
	/**
	 * Class constructor
	 * @param plugin
	 */
	ClaimGroupCache(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// initialize claim map
		claimGroupMap = new ConcurrentHashMap<Integer,ClaimGroup>();
		
		// initialize name index
		claimGroupNameIndex = new ConcurrentHashMap<String, Integer>();
	}

	
	/**
	 * Get all claims from claim map
	 * @return Collection of all claims
	 */
	final Set<ClaimGroup> fetchAllClaimGroups() {
		
		// if claim group map is empty, return empty set
		if (claimGroupMap.isEmpty()) {
			return Collections.emptySet();
		}
		
		// get new set of claim group values
		final Set<ClaimGroup> returnSet = new HashSet<ClaimGroup>(claimGroupMap.values());
		
		// return unmodifiable set of claim groups
		return Collections.unmodifiableSet(returnSet);
	}

	
	/**
	 * Retrieve claim group from cache by claim group key
	 * @param claimGroupKey
	 * @return claim group object or null if not found
	 */
	final ClaimGroup fetch(final Integer claimGroupKey) {

		// if claim key is not null, fetch claim group record from map
		if (claimGroupKey == null) {
			return null;
		}
		
		// return claim group record; will be null if no record found
		return claimGroupMap.get(claimGroupKey);
	}
	

	/**
	 * Retrieve claim group from cache by claim group name
	 * @param claimGroupName
	 * @return claim group object or null if not found
	 */
	final ClaimGroup fetch(final String claimGroupName) {

		// if claim group name is null or empty, return null record
		if (claimGroupName == null || claimGroupName.isEmpty()) {
			plugin.getLogger().warning("Could not fetch claim group from cache because "
					+ "claim group name is null or empty.");
			return null;
		}
		
		// get claim group key from name index
		final Integer claimGroupKey = claimGroupNameIndex.get(claimGroupName.toLowerCase());

		// return claim group record; will be null if no record found
		return claimGroupMap.get(claimGroupKey);
	}
	

	/**
	 * Insert a claim group into the cache
	 * @param claimGroup
	 */
	final void store(final ClaimGroup claimGroup) {
		
		final Integer claimGroupKey = claimGroup.getKey();
		final String claimGroupName = claimGroup.getName();
		
		// if claim group key is null, do not store in cache
		if (claimGroupKey == null) {
			plugin.getLogger().warning("Could not store claim group in cache because claim group key is null.");
			return;
		}
		
		// if claim group name is null or empty, do not store in cache
		if (claimGroupName == null || claimGroupName.isEmpty()) {
			plugin.getLogger().warning("Could not store claim group in cache because "
					+ "claim group name is null or empty.");
			return;
		}
		
		// insert claim group in claim group map by claim group key
		claimGroupMap.put(claimGroupKey, claimGroup);
		
		// remove all index entries of claim group name in case claim group name has changed
		for (String key : claimGroupNameIndex.keySet()) {
			if (claimGroupNameIndex.get(key).equals(claimGroupName.toLowerCase())) {
				claimGroupNameIndex.remove(key);
			}
		}
		
		// insert claim group name in index
		claimGroupNameIndex.put(claimGroupName.toLowerCase(), claimGroupKey);
	}
	
	
	/**
	 * Remove claim group from cache by key<br>
	 * @param playerName
	 */
	final void flush(final Integer claimGroupKey) {

		// get claim group name
		final String claimGroupName = plugin.dataStore.getClaimGroup(claimGroupKey).getName();
		
		// remove the claim from the claim map
		claimGroupMap.remove(claimGroupKey);
		
		// remove the claim from the name index
		claimGroupNameIndex.remove(claimGroupName);
	}


	final int getSize() {
		if (claimGroupMap == null) {
			return 0;
		}
		return claimGroupMap.keySet().size();
	}

	/**
	 * Get claim group map keys
	 * @return Set of Integer map keys
	 */
	final Set<Integer> getCacheMapKeys() {
		return Collections.unmodifiableSet(claimGroupMap.keySet());
	}

}
