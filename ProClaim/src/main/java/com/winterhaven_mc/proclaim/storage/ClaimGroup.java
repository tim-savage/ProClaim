package com.winterhaven_mc.proclaim.storage;

import java.util.Set;
import java.util.UUID;

import com.winterhaven_mc.proclaim.PluginMain;

public final class ClaimGroup {

	private final static PluginMain plugin = PluginMain.instance;
	
	private Integer groupKey;
	
	private String groupName;
	
	private Integer claimLimit;


	/**
	 * Class constructor
	 */
	public ClaimGroup() {}
	
	/**
	 * Class constructor
	 * @param groupName string name for new ClaimGroup object
	 */
	public ClaimGroup(final String groupName) {
		
		// set claim group name
		this.groupName = groupName;
		
		// set claim group limit
		this.claimLimit = 1;
	}
	
	/**
	 * Class constructor
	 * @param groupName string name for new ClaimGroup object
	 * @param limit integer max number of claims a player may own in this group
	 */
	public ClaimGroup(final String groupName, final Integer limit) {
		
		// set claim group name
		this.groupName = groupName;
		
		// set claim group limit
		this.claimLimit = limit;
	}

	public final Integer getKey() {
		return groupKey;
	}

	public final void setKey(final Integer groupKey) {
		this.groupKey = groupKey;
	}

	public final String getName() {
		return groupName;
	}

	public final void setName(final String groupName) {
		this.groupName = groupName;
	}

	public final Integer getClaimLimit() {
		return claimLimit;
	}

	public final void setClaimLimit(final Integer claimLimit) {
		this.claimLimit = claimLimit;
	}
	
	/**
	 * Insert a new claim group in the datastore
	 */
	public final void insert() {		
		plugin.dataStore.insertClaimGroup(this);
	}
	
	/**
	 * Update an existing claim group in the datastore
	 */
	public final void update() {
		plugin.dataStore.updateClaimGroup(this);
		
	}

	/**
	 * Delete an existing claim group from the datastore
	 */
	public final void delete() {
		plugin.dataStore.deleteClaimGroup(this.getKey());
	}

	/**
	 * Get a claim group from the datastore by claim group key
	 * @param claimGroupKey integer key for ClaimGroup record to be retrieved
	 * @return claim group with matching key, or null if no matching claim group found
	 */
	public static ClaimGroup getClaimGroup(final Integer claimGroupKey) {
		return plugin.dataStore.getClaimGroup(claimGroupKey);
	}
	
	/**
	 * Get a claim group from the datastore by claim group name
	 * @param claimGroupName string name of ClaimGroup to be retrived
	 * @return claim group with matching name, or null if no matching claim group found
	 */
	public static ClaimGroup getClaimGroup(final String claimGroupName) {
		return plugin.dataStore.getClaimGroup(claimGroupName);
	}
	
	/**
	 * Get all claim groups from the datastore
	 * @return unmodifiable set of all claim groups, or empty set if no claim groups exist
	 */
	public static Set<ClaimGroup> getAllClaimGroups() {
		return plugin.dataStore.getAllClaimGroups();
	}

	public static int getPlayerClaimGroupCount(final ClaimGroup claimGroup, final UUID playerUUID) {
		return plugin.dataStore.getPlayerClaimGroupCount(claimGroup, playerUUID);
	}
}
