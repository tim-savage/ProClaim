package com.winterhaven_mc.proclaim.storage;

import org.bukkit.Location;

import java.util.Set;
import java.util.UUID;


public abstract class DataStore {

	@SuppressWarnings("WeakerAccess")
	protected boolean initialized;
	
	protected DataStoreType type;

	@SuppressWarnings("WeakerAccess")
	protected String filename;

	final static UUID zeroUUID = new UUID(0,0);
	
	/**
	 * Initialize storage
	 * @throws Exception if datastore can not be initialized
	 */
	abstract void initialize() throws Exception;
	
	/**
	 * get all player data records
	 * @return Set of all player records
	 */
	abstract Set<PlayerState> getAllPlayerRecords();

	/**
	 * Get player record by player UUID
	 * @param playerUUID UUID of player to retrieve state
	 * @return PlayerState object or null if no matching record
	 */
	abstract PlayerState getPlayerState(final UUID playerUUID);
	
	/**
	 * Get player records by player name
	 * @param playerName string name of player to retrieve records
	 * @return Set of PlayerState objects; empty if no results found
	 */
	abstract Set<PlayerState> getPlayerRecords(final String playerName);
	
	/**
	 * Insert new player record
	 * @param playerState player state object to insert in datastore
	 */
	abstract void insertPlayerStateBlocking(final PlayerState playerState);

	/**
	 * Insert new player record asynchronously
	 * @param playerState player state object to insert in datastore
	 */
	abstract void insertPlayerState(final PlayerState playerState);

	/**
	 * Update player record
	 * @param playerState player state object to update in datastore
	 */
	abstract void updatePlayerState(final PlayerState playerState);

	/**
	 * Delete player record by player UUID
	 * @param playerUUID UUID of record to be deleted
	 */	
	abstract void deletePlayerState(final UUID playerUUID);

	/**
	 * get all claim records
	 * @return Set of claim objects
	 */
	public abstract Set<Claim> getAllClaims();

	/**
	 * get claim by location
	 * @param location to retrieve claim from datastore
	 * @return subclaim at location if one exists;
	 *  else top level claim at location;
	 *  else null if no claim exists at location
	 */
	public abstract Claim getClaimAt(final Location location);

	/**
	 * get claim by location
	 * @param location location to retrieve from datastore
	 * @param ignoreHeight if true ignore vertical axis when searching for claim in datastore
	 * @return subclaim at location if one exists;
	 *  else top level claim at location;
	 *  else null if no claim exists at location
	 */
	public abstract Claim getClaimAt(final Location location, final boolean ignoreHeight);

	/**
	 * get claim by claim key
	 * @return Claim
	 */
	public abstract Claim getClaim(final Integer claimKey);

	/**
	 * Get claims by owner UUID
	 * @param ownerUUID UUID of player owner of claims to retrieve from datastore
	 * @return Immutable Set of claims; empty HashSet if no records found
	 */
	public abstract Set<Claim> getPlayerClaims(final UUID ownerUUID);
	
	/**
	 * Get child claim records for claim by key
	 * @param claimKey integer key of claim to retrieve child claims
	 * @return Immutable Set of child claims; empty HashSet if no records found
	 */
	public abstract Set<Claim> getChildClaims(final Integer claimKey);

	/**
	 * Get child claim keys for claim by claim key
	 * @param claimKey integer key of claim to retrieve child claim keys
	 * @return Immutable Set of keys; empty HashSet if no records found
	 */
	abstract Set<Integer> getChildKeys(final Integer claimKey);
	
	/**
	 * insert new claim record synchronously
	 * @param claim claim object to create record in datastore
	 */
	abstract void insertClaimBlocking(final Claim claim);

	/**
	 * insert new claim record asynchronously
	 * @param claim claim object to create record in datastore
	 */
	abstract void insertClaim(final Claim claim);

	/**
	 * update existing claim record
	 * @param claim claim object to update in datastore
	 */
	abstract void updateClaim(final Claim claim);
	
	/**
	 * delete claim record
	 * @param claimKey integer key for claim to delete from datastore
	 */
	abstract void deleteClaim(final Integer claimKey);

	/**
	 * Get claim permission for player by claim key and player uuid
	 * @param claimKey integer key of claim to use as search parameter for claim permission
	 * @param playerUUID UUID of player to use as search parameter for claim permission
	 * @return ClaimPermission or null if no permission found
	 */
	abstract ClaimPermission getClaimPermission(final Integer claimKey, final UUID playerUUID);

//	/**
//	 * Get claim permission for player by claim key and player key
//	 * @param claimKey
//	 * @param playerKey
//	 * @return ClaimPermission or null if no permission found
//	 */
//	public abstract ClaimPermission getClaimPermission(final Integer claimKey, final Integer playerKey);

	/**
	 * Get claim permission by permission record key
	 * @param permissionRecordKey integer key of permission record to retrieve from datastore
	 * @return ClaimPermission or null if no permission found
	 */
	abstract ClaimPermission getClaimPermission(final Integer permissionRecordKey);
	
	/**
	 * Get claim permission keys by claim key
	 * @param claimKey integer key of claim to retrieve permissions from datastore
	 * @return Set of claim permission keys or empty Set if no permissions for claim found
	 */
	abstract Set<Integer> getClaimPermissionKeys(final Integer claimKey);

	/**
	 * Get Set of all permissions records
	 * @return Set of ClaimPermission or empty Set if no permissions found
	 */
	abstract Set<ClaimPermission> getAllClaimPermissions();
	
	/**
	 * Insert new permission record
	 * @param claimPermission claim permission object to create in datastore
	 */
	abstract void insertClaimPermissionBlocking(final ClaimPermission claimPermission);

	/**
	 * Insert new permission record asynchronously
	 * @param claimPermission claim permisssion object to create in datastore
	 */
	abstract void insertClaimPermission(final ClaimPermission claimPermission);
	
	/**
	 * Update existing permission record
	 * @param claimPermission claim permission object to update in datastore
	 */
	abstract void updateClaimPermission(final ClaimPermission claimPermission);

	/**
	 * Delete all claim permissions by claimKey
	 * @param claimKey integer key of claim to match for deleting all claim permissions
	 */
	abstract void deleteAllClaimPermissions(final Integer claimKey);
	
	/**
	 * Delete player claim permissions by claimKey, playerKey
	 * @param claimKey integer key of claim to match for deleting permissions
	 * @param playerUUID UUID of player to match for deleting permissions
	 */
	abstract void deletePlayerClaimPermission(final Integer claimKey, final UUID playerUUID);
		
	/**
	 * Get claim group by name
	 * @param claimGroupName string name of claim group to retrieve from datastore
	 * @return claim group object
	 */
	abstract ClaimGroup getClaimGroup(final String claimGroupName);

	/**
	 * Get claim group by claimGroupKey
	 * @param claimGroupKey integer key of claim group to retrieve from datastore
	 * @return claim group object
	 */
	abstract ClaimGroup getClaimGroup(final Integer claimGroupKey);

	/**
	 * Get all claim groups
	 * @return Set of all claim groups
	 */
	abstract Set<ClaimGroup> getAllClaimGroups();

	/**
	 * Get number of claims in group owned by player, by playerUUID
	 * @param claimGroup claim group object to use as search parameter for claims
	 * @param playerUUID UUID of player to use as search parameter for claims
	 * @return int number of claims owned by player in claim group
	 */
	abstract int getPlayerClaimGroupCount(final ClaimGroup claimGroup, final UUID playerUUID);
	
	/**
	 * Insert new claim group record
	 * @param claimGroup claim group object to insert in datastore
	 */
	abstract void insertClaimGroupBlocking(final ClaimGroup claimGroup);

	/**
	 * Insert new claim group record asynchronously
	 * @param claimGroup claim group object to insert in datastore
	 */
	abstract void insertClaimGroup(final ClaimGroup claimGroup);

	/**
	 * Update existing claim group record
	 * @param claimGroup claim group object to update in datastore
	 */
	abstract void updateClaimGroup(final ClaimGroup claimGroup);

	/**
	 * Delete claim group record
	 * @param claimGroupKey integer key of claim group record to retrieve from datastore
	 */
	abstract void deleteClaimGroup(final Integer claimGroupKey);
	
	/**
	 * Close datastore connection
	 */
	public abstract void close();

	/**
	 * Sync datastore to disk if supported
	 */
	abstract void sync();
	
	/**
	 * Delete datastore
	 */
	abstract void delete();
	
	/**
	 * Check that datastore exists
	 * @return true if datastore exists, false if it does not
	 */
	abstract boolean exists();
	
	/**
	 * Get datastore type
	 * @return Enum value of DataStoreType
	 */
	DataStoreType getType() {
		return this.type;
	}
	
	/**
	 * Get datastore name, formatted for display
	 * @return String containing datastore name
	 */
	public String getDisplayName() {
		return this.getType().toString();
	}

	/**
	 * Get datastore filename or equivalent
	 * @return datastore filename
	 */
	String getFilename() {
		return this.filename;
	}

	/**
	 * Get datastore initialized field
	 * @return true if datastore is initialized, false if it is not
	 */
	boolean isInitialized() {
		return this.initialized;
	}
	
	/**
	 * Set initialized field
	 * @param initialized boolean true if datastore has been initialized, else false
	 */
	void setInitialized(final boolean initialized) {
		this.initialized = initialized;
	}

}
