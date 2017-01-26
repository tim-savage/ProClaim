package com.winterhaven_mc.proclaim.storage;

import java.util.UUID;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.objects.PermissionLevel;

public final class ClaimPermission {

	private final static PluginMain plugin = PluginMain.instance;
	private Integer permissionRecordKey;
	private Integer claimKey;
	private UUID playerUUID;
	private PermissionLevel permissionLevel;
	
	
	/**
	 * Class constructor
	 */
	public ClaimPermission() { }
	
	/**
	 * Class constructor
	 * @param claimKey integer key for claim
	 * @param playerUUID UUID of player
	 * @param permissionlevel PermissionLevel object
	 */
	public ClaimPermission(final Integer claimKey, final UUID playerUUID, final PermissionLevel permissionlevel) {

		this.setClaimKey(claimKey);
		this.setPlayerUUID(playerUUID);
		this.setPermissionLevel(permissionlevel);
	}
	
	
	public final Integer getKey() {
		return this.permissionRecordKey;
	}
	
	public final void setKey(final Integer permissionRecordKey) {
		this.permissionRecordKey = permissionRecordKey;
	}
	
	final Integer getClaimKey() {
		return claimKey;
	}
	
	final void setClaimKey(final Integer claimKey) {
		this.claimKey = claimKey;
	}

	public final UUID getPlayerUUID() {
		return playerUUID;
	}

	public final void setPlayerUUID(final UUID playerUUID) {
		this.playerUUID = playerUUID;
	}

	final PermissionLevel getPermissionLevel() {
		return permissionLevel;
	}

	final void setPermissionLevel(final PermissionLevel permissionLevel) {
		this.permissionLevel = permissionLevel;
	}
	
	public final PermissionLevel addGrant() {
		this.setPermissionLevel(permissionLevel.addGrant());
		return this.getPermissionLevel();
	}

	public final boolean canGrant(final PermissionLevel permissionLevel) {
		return this.getPermissionLevel().canGrant(permissionLevel);
	}
	
	public final boolean allows(final PermissionLevel permissionLevel) {
		return this.getPermissionLevel().allows(permissionLevel);
	}
	
	/**
	 * Insert a new claim permission record in the datastore
	 */
	public final void insert() {
		plugin.dataStore.insertClaimPermission(this);
	}
	
	/**
	 * Update an existing claim permission record in the datastore
	 */
	public final void update() {
		plugin.dataStore.updateClaimPermission(this);
	}
	
	/**
	 * Delete an existing claim permission record from the datastore
	 */
	public final void delete() {
		
	}
	
	/**
	 * Get a permission record from the datastore. If null record is returned, create
	 * a record with claimKey and PlayerUUID and PermissionLevel.NONE
	 * @param claimKey integer key of claim to retrieve permission
	 * @param playerUUID UUID of player to retrieve permission
	 * @return A valid ClaimPermission record
	 */
	public static ClaimPermission getClaimPermission(final Integer claimKey, final UUID playerUUID) {
		
		if (claimKey == null) {
			throw new NullPointerException("Cannot retrieve claim permission for null claim key.");
		}
		
		if (playerUUID == null) {
			throw new NullPointerException("Cannot retrieve claim permission for null player uuid.");
		}
		
		// get claim permission record from datastore
		ClaimPermission returnRecord = plugin.dataStore.getClaimPermission(claimKey, playerUUID);
		
		// if no record returned, create a record with claimKey, playerUUID and PermissionLevel.NONE
		if (returnRecord == null) {
			returnRecord = new ClaimPermission(claimKey, playerUUID, PermissionLevel.NONE);
		}
		
		return returnRecord;
	}
	
}
