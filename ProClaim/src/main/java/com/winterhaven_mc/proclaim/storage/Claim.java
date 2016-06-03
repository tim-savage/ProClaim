package com.winterhaven_mc.proclaim.storage;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.objects.ClaimResult;
import com.winterhaven_mc.proclaim.objects.ClaimStatus;
import com.winterhaven_mc.proclaim.objects.PermissionLevel;
import org.bukkit.Location;
import org.bukkit.World;

import java.time.Instant;
import java.util.*;


public final class Claim {

	// static reference to main class
	private final static PluginMain plugin = PluginMain.instance;
	
	// claim primary key
	private Integer key;

	// claim owner UUID
	private UUID ownerUUID;

	// parent claim primary key
	private Integer parentKey;

	// group primary key
	private Integer groupKey;

	// claim boundaries
	private Location lowerCorner;
	private Location upperCorner;
	
	// locked status
	private Boolean locked;
	
	// resizeable status
	private Boolean resizeable = true;

	// creation date
	private Instant createdDate;

	// modified date
	private Instant modifiedDate;

	// active status
	private ClaimStatus status = ClaimStatus.ACTIVE;
	
	// constant for admin owner UUID, all zeros
	private static final UUID zeroUUID = new UUID(0,0);

	/**
	 * Class constructor
	 */
	public Claim() {
		this.setCreatedDate(Instant.now());
		this.setModifiedDate(this.getCreatedDate());
	}


	/**
	 * Class constructor
	 * @param loc1
	 * @param loc2
	 */
	public Claim(final Location loc1, final Location loc2) {
		
		// set creation and modified date
		this.setCreatedDate(Instant.now());
		this.setModifiedDate(this.getCreatedDate());

		// only set locations if non-null and in same world
		if (loc1 != null && loc2 != null && loc1.getWorld().equals(loc2.getWorld())) {
			
			int lowerX = Math.min(loc1.getBlockX(), loc2.getBlockX());
			int upperX = Math.max(loc1.getBlockX(), loc2.getBlockX());

			int lowerY = Math.min(loc1.getBlockY(), loc2.getBlockY());
			int upperY = loc1.getWorld().getMaxHeight() - 1;
//			int upperY = Math.max(loc1.getBlockY(), loc2.getBlockY());

			int lowerZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
			int upperZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

			this.setLowerCorner(new Location(loc1.getWorld(),lowerX,lowerY,lowerZ));
			this.setUpperCorner(new Location(loc1.getWorld(),upperX,upperY,upperZ));
		}
	}

	public final Integer getKey() {
		return key;
	}

	public final void setKey(final Integer claimKey) {
		this.key = claimKey;
	}


	public final UUID getOwnerUUID() {
		return ownerUUID;
	}


	public final void setOwnerUUID(final UUID ownerUUID) {
		this.ownerUUID = ownerUUID;
	}


	public final Integer getParentKey() {
		return parentKey;
	}

	public final void setParentKey(final Integer parentKey) {
		this.parentKey = parentKey;
	}

	public final Integer getGroupKey() {
		return groupKey;
	}

	public final void setGroupKey(final Integer groupKey) {
		this.groupKey = groupKey;
	}

	public final Location getUpperCorner() {
		return upperCorner;
	}

	public final void setUpperCorner(final Location upperCorner) {
		this.upperCorner = upperCorner;
	}

	public final Location getLowerCorner() {
		return lowerCorner;
	}

	public final void setLowerCorner(final Location lowerCorner) {
		this.lowerCorner = lowerCorner;
	}

	public final Boolean isLocked() {
		return locked;
	}


	public final void setLocked(final Boolean locked) {
		this.locked = locked;
	}


	public final Boolean isResizeable() {
		return resizeable;
	}


	public final Boolean getResizeable() {
		return resizeable;
	}


	public final void setResizeable(final Boolean resizeable) {
		this.resizeable = resizeable;
	}


	public final Instant getCreatedDate() {
		return createdDate;
	}

	public final void setCreatedDate(final Instant instant) {
		this.createdDate = instant;
	}

	public final Instant getModifiedDate() {
		return modifiedDate;
	}

	public final void setModifiedDate(final Instant instant) {
		this.modifiedDate = instant;
	}
	
	public boolean isActive() {
		if (this.status == null || this.status.equals(ClaimStatus.ACTIVE)) {
			return true;
		}
		return false;
	}

	public final ClaimStatus getStatus() {
		return status;
	}

	public final void setStatus(final ClaimStatus status) {
		this.status = status;
	}

	
	/**
	 * Check if a claim is an admin claim<br>
	 * admin claims have a ownerUUID of all zeros; we also test for null
	 * @return
	 */
	public final boolean isAdminClaim() {
		
		if (this.getOwnerUUID() == null || this.getOwnerUUID().equals(zeroUUID)) {
			return true;
		}
		return false;
	}

	
	/**
	 * Change a claim into an admin claim<br>
	 * admin claims have a ownerUUID of all zeros
	 * @return
	 */
	public final void setAdminClaim() {
		this.setOwnerUUID(zeroUUID);
	}

	
	/**
	 * Check if a claim is a subclaim<br>
	 * subclaims have a non-null, non-zero parent claim key
	 * @return {@code true} if claim is a subclaim, {@code false} if it is not
	 */
	public final boolean isSubClaim() {

		return this.getParentKey() != null && this.getParentKey() != 0;
	}

	
	/**
	 * Check if a location is within this claim
	 * @param location
	 * @param ignoreHeight
	 * @return true if location is within claim, false if location is outside claim
	 */
	public final boolean contains(final Location location, final boolean ignoreHeight) {

		// if claim is not in the same world as location return false
		if (!location.getWorld().equals(this.lowerCorner.getWorld())) {
			return false;
		}

		// check if location coordinates are within claim boundaries
		return (ignoreHeight || location.getBlockY() >= this.lowerCorner.getBlockY()) 
				&& location.getBlockX() >= this.lowerCorner.getBlockX()
				&& location.getBlockX() <= this.upperCorner.getBlockX()
				&& location.getBlockZ() >= this.lowerCorner.getBlockZ()
				&& location.getBlockZ() <= this.upperCorner.getBlockZ();
	}
	
	
	/**
	 * Test if a claim fully contains another claim
	 * @param otherClaim
	 * @return boolean
	 */
	public final boolean contains(final Claim otherClaim) {

		// check if other claim boundaries are within this claim boundaries
		return (this.getWorld().equals(otherClaim.getWorld())
				&& this.getUpperCorner().getBlockX() >= otherClaim.getUpperCorner().getBlockX()
				&& this.getUpperCorner().getBlockZ() >= otherClaim.getUpperCorner().getBlockZ()
				&& this.getLowerCorner().getBlockX() <= otherClaim.getLowerCorner().getBlockX()
				&& this.getLowerCorner().getBlockZ() <= otherClaim.getLowerCorner().getBlockZ());
	}
	
	
	/**
	 * Test if claim overlaps another claim in two dimensions
	 * @param otherClaim
	 * @return true if overlap, false if no overlap or claims are in different worlds
	 */
	public final boolean overlaps(final Claim otherClaim) {
		
		// if claims are in different worlds, they don't overlap
		if (!this.getWorld().equals(otherClaim.getWorld())) {
			return false;
		}
		
		return ( !(Math.max(this.getLowerCorner().getBlockX(),this.getUpperCorner().getBlockX()) 
					< Math.min(otherClaim.getLowerCorner().getBlockX(),otherClaim.getUpperCorner().getBlockX())
				|| Math.min(this.getLowerCorner().getBlockX(),this.getUpperCorner().getBlockX()) 
					> Math.max(otherClaim.getLowerCorner().getBlockX(),otherClaim.getUpperCorner().getBlockX())
				|| Math.max(this.getLowerCorner().getBlockZ(),this.getUpperCorner().getBlockZ()) 
					< Math.min(otherClaim.getLowerCorner().getBlockZ(),otherClaim.getUpperCorner().getBlockZ())
				|| Math.min(this.getLowerCorner().getBlockZ(),this.getUpperCorner().getBlockZ()) 
					> Math.max(otherClaim.getLowerCorner().getBlockZ(),otherClaim.getUpperCorner().getBlockZ())));
	}
	

	/**
	 * Check if location is a corner of claim
	 * @param location
	 * @return boolean
	 */
	public final boolean isCorner(final Location location) {
		
		return (location.getBlockX() == this.getLowerCorner().getBlockX()
				|| location.getBlockX() == this.getUpperCorner().getBlockX()) 
				&& (location.getBlockZ() == this.getLowerCorner().getBlockZ() 
				|| location.getBlockZ() == this.getUpperCorner().getBlockZ());
	}
	
	
	/**
	 * Get claim width (east/west dimension)
	 * @return Integer
	 */
	public final Integer getWidth() {

		if (this.getUpperCorner() != null && this.getLowerCorner() != null) {
			return this.getUpperCorner().getBlockX() - this.getLowerCorner().getBlockX() + 1;
		}
		return 0;
	}

	
	/**
	 * get claim length (north/south dimension)
	 * @return Integer
	 */
	public final Integer getLength() {

		if (this.getUpperCorner() != null && this.getLowerCorner() != null) {
			return this.getUpperCorner().getBlockZ() - this.getLowerCorner().getBlockZ() + 1;
		}
		return 0;
	}
	
	
	/**
	 * Get claim area in square blocks
	 * @return Integer
	 */
	public final Integer getArea() {

		if (this.getUpperCorner() != null && this.getLowerCorner() != null) {
			return this.getWidth() * this.getLength();
		}
		return 0;
	}
	
	
	/**
	 * Get world in which the claim resides
	 * @return World object or null if no location set for claim upper corner
	 */
	public final World getWorld() {
		if (this.getUpperCorner() == null) {
			return null;
		}
		return this.getUpperCorner().getWorld();
	}
	
	
	/**
	 * Create a new claim
	 * @param loc1
	 * @param loc2
	 * @param ownerData
	 * @return CreateClaimResult
	 */
	public final static ClaimResult create(final Location loc1, final Location loc2, final PlayerState ownerData) {
	
		final ClaimResult result = new ClaimResult();
		
		// check that locations are in the same world
		if (!loc1.getWorld().equals(loc2.getWorld())) {
			
			// worlds don't match; return unsuccessful result object
			result.setSuccess(false);
			return result;
		}
		
		// create new claim object
		final Claim newClaim = new Claim(loc1,loc2);
		
		// set lower corner Y auto-depth from config
		newClaim.setLowerCorner(newClaim.getLowerCorner()
				.subtract(0,plugin.getConfig().getInt("claim-auto-depth"),0));
		
		// set owner data
		newClaim.setOwnerUUID(ownerData.getPlayerUUID());
	
		// set result claim to new claim
		result.setResultClaim(newClaim);
		
		// check if claim overlaps any top level claims in world
		for (Claim testClaim : plugin.dataStore.getAllClaims()) {
			
			if (testClaim.getWorld().equals(newClaim.getWorld()) 
					&& !testClaim.isSubClaim()
					&& newClaim.overlaps(testClaim)) {
				result.addOverlapClaim(testClaim);
				result.setSuccess(false);
			}
		}
		
		// return ClaimResult
		return result;
	}


	/**
	 * Create a subclaim inside an existing claim
	 * @param loc1 first corner selected
	 * @param loc2 second corner selected
	 * @param parentClaim
	 * @return CreateClaimResult
	 */
	public final static ClaimResult createSubClaim(final Location loc1, final Location loc2, final Claim parentClaim) {
		
		// new CreateClaimResult object
		final ClaimResult result = new ClaimResult();
		result.setSuccess(true);
	
		// check that locations are in the same world
		if (!loc1.getWorld().equals(loc2.getWorld())) {
			
			// worlds don't match; return unsuccessful result object
			result.setSuccess(false);
			return result;
		}
		
		// create new claim object
		final Claim newClaim = new Claim(loc1,loc2);
		
		// set lower corner Y auto-depth from config
		newClaim.setLowerCorner(newClaim.getLowerCorner()
				.subtract(0,Math.max(0,plugin.getConfig().getInt("claim-auto-depth")),0));
		
		// get owner data from parent claim
		final PlayerState ownerData = plugin.dataStore.getPlayerState(parentClaim.getOwnerUUID());
		
		// set owner data in new claim
		newClaim.setOwnerUUID(ownerData.getPlayerUUID());
	
		// set claim in result claim
		result.setResultClaim(newClaim);
		
		// get list of sibling claims
		final Set<Claim> siblings = plugin.dataStore.getChildClaims(parentClaim.getKey());
	
		// check each sibling for overlap, add overlapping claims to result set
		for (Claim sibling : siblings) {
			if (newClaim.overlaps(sibling)) {
				result.addOverlapClaim(sibling);
				result.setSuccess(false);
			}
		}
		
		// return ClaimResult
		return result;
	}

	
	/**
	 * Get all claims
	 * @return Collection of Claims
	 */
	public final static Collection<Claim> getAllClaims() {		
		return plugin.dataStore.getAllClaims();
	}

	
	/**
	 * Insert a new claim record in the datastore
	 */
	public final void insert() {
	
		// insert claim in datastore
		plugin.dataStore.insertClaim(this);
		
		// create worldguard region for claim
		plugin.worldGuardHelper.createRegion(this);
	}

	
	/**
	 * Update an existing claim record in the datastore
	 */
	public final void update() {
		
		// update claim in datastore
		plugin.dataStore.updateClaim(this);
		
		// update worldguard region for claim
		plugin.worldGuardHelper.syncRegion(this);
	}

	
	/**
	 * Delete a claim record from the datastore. All child claims will also be deleted.
	 */
	public final void delete() {
		
		// set status to pending delete
		this.setStatus(ClaimStatus.PENDING_DELETE);
		
		// set status for all child claims to pending delete
		for (Claim claim : this.getChildClaims()) {
			claim.setStatus(ClaimStatus.PENDING_DELETE);
		}
		
		// remove worldguard region for claim and any children
		plugin.worldGuardHelper.removeRegion(this);
		
		// remove claim and any children from datastore
		plugin.dataStore.deleteClaim(this.getKey());
	}


	/**
	 * Extend claim depth along with parent claim and siblings or children<br>
	 * Note that this method does not check if a player has permission for the claim
	 * @param depth
	 */
	public final void extend(final Integer depth) {
	
		// create HashSet of claims to extend
		final HashSet<Claim> extendedClaims = new HashSet<Claim>();
	
		Claim parentClaim = null;
	
		// if claim is a subclaim, get parent claim
		if (this.isSubClaim()) {
			parentClaim = plugin.dataStore.getClaim(this.getParentKey());
		}
		// else claim is parentClaim
		else {
			parentClaim = this;
		}
	
		// add children to extendClaims HashSet
		extendedClaims.addAll(this.getChildClaims());
	
		// add parent claim to extendClaims HashSet
		extendedClaims.add(parentClaim);
	
		// get new Y for lower corner
		final int newY = depth - plugin.getConfig().getInt("claim-auto-depth");
	
		// extend all claims in HashSet
		for (Claim extendedClaim : extendedClaims) {
	
			// get lower corner
			final Location lowerCorner = extendedClaim.getLowerCorner();
	
			// set new Y for lower corner
			lowerCorner.setY(newY);
	
			// set new lower corner in claim
			extendedClaim.setLowerCorner(lowerCorner);
	
			// update claim in datastore
			extendedClaim.update();
		}
		return;
	}

	
	/**
	 * Abandon claim. If claim is a member of a claim group, transfers ownership of claim
	 * and any subclaims to the admin user. If claim is not a member of any claim groups,
	 * the claim is deleted along with any subclaims.
	 */
	public final void abandon() {
		
		// get owner player state
		final PlayerState playerState = plugin.dataStore.getPlayerState(this.getOwnerUUID());
		
		// credit player with blocks
		playerState.setEarnedClaimBlocks(playerState.getEarnedClaimBlocks() + this.getArea());

		// if claim has a claim group, transfer claim and children to admin
		if (this.getGroupKey() != null && this.getGroupKey() != 0) {
			
			// set claim to admin claim
			this.setAdminClaim();
			
			// remove all claim permissions
			this.removeAllPermissions();
		
			// update claim in datastore
			this.update();
			
			// add all child claim keys to abandonedKeys HashSet
			final HashSet<Integer> abandonedKeys = 
					new HashSet<Integer>(plugin.dataStore.getChildKeys(this.getKey()));
			
			// iterate through all keys in abandonedKeys HashSet
			for (Integer key : abandonedKeys) {

				// get claim
				final Claim abandonedClaim = plugin.dataStore.getClaim(key);
				
				// set claim to admin claim
				abandonedClaim.setAdminClaim();
				
				// update claim in datastore
				abandonedClaim.update();
			}
		}
		else {
			// if no claim group, delete claim and children
			this.delete();
		}
	}

	
	/**
	 * Transfer claim ownership
	 * @param newOwnerUUID
	 */
	public final void transfer(final UUID newOwnerUUID) {
		
		// remove all permissions from claim
		this.removeAllPermissions();
		
		// initialize HashSet to store parent and all siblings/children
		final Set<Claim> transferredClaims = new HashSet<Claim>();
		
		// add all children to HashSet
		transferredClaims.addAll(plugin.dataStore.getChildClaims(this.getKey()));
			
		// add claim itself to HashSet
		transferredClaims.add(this);

		// update each claim in datastore
		for (Claim transferredClaim : transferredClaims) {
			
			// update claim owner uuid to recipient uuid
			transferredClaim.setOwnerUUID(newOwnerUUID);
			
			// update claim in datastore
			transferredClaim.update();
		}
	}

	
	/**
	 * Check if player has permission level for claim
	 * @param playerUUID
	 * @param permissionLevel
	 * @return true if player has permission level in claim, false if they do not
	 */
	public final boolean allows(final UUID playerUUID, final PermissionLevel permissionLevel) {

		// get permission record from datastore
		ClaimPermission claimPermission = plugin.dataStore.getClaimPermission(this.getKey(), playerUUID);
		
		// if permission record is null, try public group
		if (claimPermission == null) {
			
			// get claim permission record for public group
			claimPermission = plugin.dataStore.getClaimPermission(this.getKey(), zeroUUID);

			// if no record for public, return false
			if (claimPermission == null) {
				return false;
			}
		}
		
		// if permission record permission level is null, return false
		if (claimPermission.getPermissionLevel() == null) {
			return false;
		}

		// return permission level result
		return claimPermission.allows(permissionLevel);
	}
	
	
	/**
	 * Insert or update a permission record
	 * @param playerUUID
	 * @param permissionLevel
	 */
	public final void setPermission(final UUID playerUUID, final PermissionLevel permissionLevel) {
		
		// if claim key is null, do nothing and return
		if (this.getKey() == null) {
			if (plugin.debug) {
				plugin.getLogger().info("Cannot set permission because claim key is null.");
			}
			return;
		}
		
		// if player uuid is null, do nothing and return
		if (playerUUID == null) {
			if (plugin.debug) {
				plugin.getLogger().info("Cannot set permission because player uuid is null.");
			}
			return;
		}
		
		// if permission level is null, do nothing and return
		if (permissionLevel == null) {
			if (plugin.debug) {
				plugin.getLogger().info("Cannot set permission because permission level is null.");
			}
			return;
		}
		
		// get existing record from datastore
		ClaimPermission claimPermission = plugin.dataStore.getClaimPermission(this.getKey(), playerUUID);
		
		// if record does not exists for claim / player, insert new record
		if (claimPermission == null) {

			// create new record
			claimPermission = new ClaimPermission(this.getKey(), playerUUID, permissionLevel);

			// insert new record in datastore
			claimPermission.insert();
		}
		else {
			// set new permission level in existing record
			claimPermission.setPermissionLevel(permissionLevel);
			
			// update permission record in datastore
			claimPermission.update();
		}
	}
	

	/**
	 * Add grant permission for a player in this claim
	 * @param playerUUID
	 */
	public final void addGrant(final UUID playerUUID) {
		
		// if claim key is null, do nothing and return
		if (this.getKey() == null) {
			if (plugin.debug) {
				plugin.getLogger().info("Cannot set permission because claim key is null.");
			}
			return;
		}
		
		// if player uuid is null, do nothing and return
		if (playerUUID == null) {
			if (plugin.debug) {
				plugin.getLogger().info("Cannot set permission because player uuid is null.");
			}
			return;
		}
		
		// get existing record from datastore
		ClaimPermission claimPermission = plugin.dataStore.getClaimPermission(this.getKey(), playerUUID);
		
		// if record does not exists for claim / player, insert new record
		if (claimPermission == null) {

			// create new record
			claimPermission = new ClaimPermission(this.getKey(), playerUUID, PermissionLevel.GRANT);

			// insert new record in datastore
			claimPermission.insert();
		}
		else {
			// set new permission level in existing record
			claimPermission.addGrant();
			
			// update permission record in datastore
			claimPermission.update();
		}
	}
	

	/**
	 * Remove permission for a player in this claim
	 * @param playerUUID
	 */
	public final void removePlayerPermission(final UUID playerUUID) {
		plugin.dataStore.deletePlayerClaimPermission(this.getKey(), playerUUID);
	}


	/**
	 * Remove permission for all players in this claim
	 */
	public final void removeAllPermissions() {
		plugin.dataStore.deleteAllClaimPermissions(this.getKey());
	}
	

	/**
	 * Get permissions for all players in a claim
	 * @return Hashmap with permission level as key and array of player names as value
	 */
	public final HashMap<PermissionLevel,ArrayList<String>> getPermissionMap() {
		
		// initialize return map
		final HashMap<PermissionLevel,ArrayList<String>> permissionMap = 
				new HashMap<PermissionLevel,ArrayList<String>>();
	
		// put all PermissionLevels in hashmap as keys
		for (PermissionLevel p : PermissionLevel.values()) {
			permissionMap.put(p,new ArrayList<String>());
		}

		// get all permission record keys for claim
		final HashSet<Integer> claimPermissionRecordKeys = 
				new HashSet<Integer>(plugin.dataStore.getClaimPermissionKeys(this.getKey()));

		// iterate through all permission record keys for claim
		for (Integer permissionRecordKey : claimPermissionRecordKeys) {
			
			// fetch permission record from datastore by key
			final ClaimPermission claimPermission = plugin.dataStore.getClaimPermission(permissionRecordKey);
			
			// get player key from permission record
			final UUID playerUUID = claimPermission.getPlayerUUID();
			
			// get player claim permission level from permission record
			final PermissionLevel permissionLevel = claimPermission.getPermissionLevel();

			// fetch player name by player UUID
			final String playerName = plugin.dataStore.getPlayerState(playerUUID).getName();

			// add player name to claim permission map by permission level
			permissionMap.get(permissionLevel).add(playerName);
		}
		
		// return populated map
		return permissionMap;
	}


	/**
	 * Get child claims of this claim
	 * @return  Immutable Set of Claims containing child claims; empty HashSet if no child claims exist
	 */
	public final Set<Claim> getChildClaims() {

		// if this claim is a subclaim, return empty set
		if (this.isSubClaim()) {
			return Collections.emptySet();
		}
		
		// if child claims set is empty, return empty set
		if (plugin.dataStore.getChildClaims(this.getKey()).isEmpty()) {
			return Collections.emptySet();
		}

		// return set of child claims
		return plugin.dataStore.getChildClaims(this.getKey());
	}
	
	
	/**
	 * Create a temporary claim for checking overlap/contains during claim resizing
	 * @param firstClick
	 * @param secondClick
	 * @return the temporary claim with resized coordinates
	 */
	public final Claim getResizeCheckClaim(final Location firstClick, final Location secondClick) {
		
		// determine dimensions of resized claim
		int newLowerX, newUpperX, newLowerZ, newUpperZ, newLowerY, newUpperY;
		
		if (firstClick.getBlockX() == this.getLowerCorner().getBlockX()) {
			newLowerX = secondClick.getBlockX();
		}
		else {
			newLowerX = this.getLowerCorner().getBlockX();
		}
		
		if (firstClick.getBlockX() == this.getUpperCorner().getBlockX()) {
			newUpperX = secondClick.getBlockX();
		}
		else {
			newUpperX = this.getUpperCorner().getBlockX();
		}
		
		if (firstClick.getBlockZ() == this.getLowerCorner().getBlockZ()) {
			newLowerZ = secondClick.getBlockZ();
		}
		else {
			newLowerZ = this.getLowerCorner().getBlockZ();
		}
		
		if (firstClick.getBlockZ() == this.getUpperCorner().getBlockZ()) {
			newUpperZ = secondClick.getBlockZ();
		}
		else {
			newUpperZ = this.getUpperCorner().getBlockZ();
		}
		
		// not changing Y values, just get them from old claim
		newLowerY = this.getLowerCorner().getBlockY();
		newUpperY = this.getUpperCorner().getBlockY();
		
		// if new corner is beyond opposite border, return null
		if (newLowerX > newUpperX 
				|| newUpperX < newLowerX
				|| newLowerZ > newUpperZ 
				|| newUpperZ < newLowerZ) {
			return null;
		}

		// return claim object with new coordinates
		return new Claim(
				new Location(this.getWorld(), newLowerX, newLowerY, newLowerZ), 
				new Location(this.getWorld(), newUpperX, newUpperY, newUpperZ));
	}

}
