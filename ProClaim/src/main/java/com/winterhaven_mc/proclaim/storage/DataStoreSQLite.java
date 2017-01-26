package com.winterhaven_mc.proclaim.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.objects.PermissionLevel;


public final class DataStoreSQLite extends DataStore {

	// reference to main class
	private final PluginMain plugin;

	// database connection object
	private Connection connection;
	
	// player state cache
	private final PlayerStateCache playerStateCache;
	
	// claim cache
	private final ClaimCache claimCache;
	
	// permission cache
	private final PermissionCache permissionCache;
	
	// claim group cache
	private final ClaimGroupCache claimGroupCache;

	
	/**
	 * Class constructor
	 * @param plugin refernce to plugin main class
	 */
	DataStoreSQLite (final PluginMain plugin) {

		// reference to main class
		this.plugin = plugin;

		// set datastore type
		this.type = DataStoreType.SQLITE;
		
		// set datastore filename
		this.filename = "proclaim.db";
		
		// initialize player state cache
		this.playerStateCache = new PlayerStateCache(plugin);

		// initialize claim cache
		this.claimCache = new ClaimCache(plugin);
		
		// initialize permission cache
		this.permissionCache = new PermissionCache(plugin);
		
		// initialize claim group cache
		this.claimGroupCache = new ClaimGroupCache(plugin);
	}


	/**
	 * Initialize the SQLite datastore<br>
	 * Creates database tables if they don't already exist<br>
	 * Inserts player 0 (public) in player table if not already there
	 * @throws SQLException if datastore can not be initialized
	 */
	@Override
	final void initialize() throws SQLException {
		
		// register the driver 
		String jdbcDriverName = "org.sqlite.JDBC";

		try {
			Class.forName(jdbcDriverName);
		}
		catch (Exception e) {
			plugin.getLogger().severe(e.getMessage());
			return;
		}

		// create database url
		String databaseFile = plugin.getDataFolder() + File.separator + this.getFilename();
		String jdbc = "jdbc:sqlite";
		String dbUrl = jdbc + ":" + databaseFile;

		// create a database connection
		connection = DriverManager.getConnection(dbUrl);
		Statement statement = connection.createStatement();

		// execute players table creation statement
		statement.executeUpdate(Queries.getQuery("CreatePlayerTable"));

		// execute claims table creation statement
		statement.executeUpdate(Queries.getQuery("CreateClaimTable"));

		// execute permissions table creation statement
		statement.executeUpdate(Queries.getQuery("CreatePermissionTable"));
		
		// execute group table creation statement
		statement.executeUpdate(Queries.getQuery("CreateClaimGroupTable"));
		
		// insert public player into players table with all zero UUID
		statement.executeUpdate(MessageFormat.format(Queries.getQuery("InsertPublicPlayer"), zeroUUID.toString()));
		
		// set initialized true
		setInitialized(true);
		plugin.getLogger().info(this.getDisplayName() + " datastore initialized.");

		// load all claim records into claim cache
		cacheAllClaimRecords();
		
		// load all permission records into permission cache
		cacheAllClaimPermissions();
		
		// load all claim group records into claim group cache
		cacheAllClaimGroupRecords();

	}
	
	
	/**
	 * Close datastore
	 */
	@Override
	public final void close() {
	
		try {
			connection.close();
			plugin.getLogger().info(this.getDisplayName() + " datastore connection closed.");
		}
		catch (Exception e) {
	
			// output simple error message
			plugin.getLogger().warning("An error occured while closing the SQLite datastore.");
			plugin.getLogger().warning(e.getMessage());
	
			// if debugging is enabled, output stack trace
			if (plugin.debug) {
				e.getStackTrace();
			}
		}
		setInitialized(false);
	}


	/**
	 * Sync datastore (not necessary for sqlite)
	 */
	@Override
	final void sync() {
	
		// no action necessary for this storage type
	
	}


	/**
	 * Delete datastore file
	 */
	@Override
	final void delete() {
	
		// get path name to data store file
		File dataStoreFile = new File(plugin.getDataFolder() + File.separator + this.getFilename());
		if (dataStoreFile.exists()) {
			//noinspection ResultOfMethodCallIgnored
			dataStoreFile.delete();
		}
	}


	/**
	 * Check if datastore file exists
	 */
	@Override
	final boolean exists() {
	
		// get path name to data store file
		File dataStoreFile = new File(plugin.getDataFolder() + File.separator + this.getFilename());
		return dataStoreFile.exists();
	}


	@Override
	final PlayerState getPlayerState(final UUID playerUUID) {
		
		// if playerKey is null, return null record
		if (playerUUID == null) {
			return null;
		}
	
		// try cache first
		PlayerState playerState = playerStateCache.fetch(playerUUID);
		
		// if player state found in cache, return
		if (playerState != null) {
			return playerState;
		}
	
		// get player state from datastore
		playerState = selectPlayerRecord(playerUUID);
		
		// insert player state in cache
		if (playerState != null) {
			playerStateCache.store(playerState);
		}
		return playerState;
	}


	/**
	 * Get HashSet of all player records<br>
	 * Used by convertDataStore method in DataStoreFactory
	 */
	@Override
	final Set<PlayerState> getAllPlayerRecords() {
	
		Set<PlayerState> returnSet = new HashSet<>();
	
		try {
			PreparedStatement preparedStatement = 
					connection.prepareStatement(Queries.getQuery("SelectAllPlayerRecords"));
	
			// execute sql query
			ResultSet rs = preparedStatement.executeQuery();
	
			while (rs.next()) {
				
				PlayerState playerState = new PlayerState();
				
				// test if uuid is null or empty and skip, otherwise try to set from string and catch exception
				String playerUUID = rs.getString("playeruuid");
				if (playerUUID != null && !playerUUID.isEmpty()) {
					try {
						playerState.setPlayerUUID(UUID.fromString(rs.getString("playeruuid")));
					}
					catch (Exception e) {
						plugin.getLogger().warning("Invalid UUID for player " 
								+ playerState.getName() 
								+ " in SQLite datastore.");
					}
				}
				playerState.setName(rs.getString("playername"));
				playerState.setLastLogin(java.time.Instant.ofEpochMilli(rs.getLong("lastlogin")));
				playerState.setEarnedClaimBlocks(rs.getInt("earnedblocks"));
				playerState.setPurchasedClaimBlocks(rs.getInt("purchasedblocks"));
				playerState.setBonusClaimBlocks(rs.getInt("bonusblocks"));
				
				returnSet.add(playerState);
			}
		}
		catch (Exception e) {
	
			// output simple error message
			plugin.getLogger().warning("An error occurred while trying to "
					+ "fetch all player records from the SQLite datastore.");
			plugin.getLogger().warning(e.getLocalizedMessage());
	
			// if debugging is enabled, output stack trace
			if (plugin.debug) {
				e.getStackTrace();
			}
		}
		// return results
		return Collections.unmodifiableSet(returnSet);	
	}


	/**
	 * Get player records by playerName
	 */
	@Override
	final Set<PlayerState> getPlayerRecords(final String playerName) {
		
		// if playerKey is null, return null record
		if (playerName == null) {
			return null;
		}
		
		// create new hash set for return data
		Set<PlayerState> returnSet = new HashSet<>();
		
		try {
			PreparedStatement preparedStatement = 
					connection.prepareStatement(Queries.getQuery("SelectPlayerRecordsByName"));
			
			preparedStatement.setString(1, playerName.toLowerCase());
			
			// execute sql query
			ResultSet rs = preparedStatement.executeQuery();
			
			// it is possible for more than one record to match, so put results in a HashSet
			while (rs.next()) {
				
				// create new player record to read data into
				PlayerState playerState = new PlayerState();
				
				// test if uuid is null or empty and skip, otherwise try to set from string and catch exception
				String playerUUID = rs.getString("playeruuid");
				if (playerUUID != null && !playerUUID.isEmpty()) {
					try {
						playerState.setPlayerUUID(UUID.fromString(rs.getString("playeruuid")));
					}
					catch (Exception e) {
						plugin.getLogger().warning("Invalid UUID for player " 
								+ playerState.getName() 
								+ " in " + getDisplayName() + " datastore.");
					}
				}
				playerState.setName(rs.getString("playername"));
				playerState.setLastLogin(java.time.Instant.ofEpochMilli(rs.getLong("lastlogin")));
				playerState.setEarnedClaimBlocks(rs.getInt("earnedblocks"));
				playerState.setPurchasedClaimBlocks(rs.getInt("purchasedblocks"));
				playerState.setBonusClaimBlocks(rs.getInt("bonusblocks"));
				
				// put player record into return hash set
				returnSet.add(playerState);
			}
		
			// if no matching record found for playerName, write log message and return null
			if (returnSet.isEmpty()) {
				plugin.getLogger().info("No records found for player name " + playerName + ".");
				returnSet = null;
			}
		}
		catch (SQLException e) {
			plugin.getLogger().warning("An error occured while attempting to " 
					+ "read player records by name from " + getDisplayName() + " storage.");
			plugin.getLogger().warning(e.getMessage());
			if (plugin.debug) {
				e.printStackTrace();
			}
		}
		//noinspection ConstantConditions
		return Collections.unmodifiableSet(returnSet);
	}


	/**
	 * Get player record by UUID
	 */
	private PlayerState selectPlayerRecord(final UUID playerUUID) {
		
		// if playerUUID is null, return null record
		if (playerUUID == null) {
			return null;
		}

		// initialize new player state object for return
		PlayerState playerState = new PlayerState();
		
		if (plugin.debug) {
			plugin.getLogger().info("Getting player record from " 
					+ getDisplayName() + " datastore by player UUID.");
		}
		
		String playerUUIDString  = playerUUID.toString();

		try {
			PreparedStatement preparedStatement = 
					connection.prepareStatement(Queries.getQuery("SelectPlayerRecordByUUID"));
			
			preparedStatement.setString(1, playerUUIDString);
			
			// execute sql query
			ResultSet rs = preparedStatement.executeQuery();
			
			boolean empty = true;

			// only zero or one record can match the unique key
			if (rs.next()) {
				try {
					playerState.setPlayerUUID(UUID.fromString(rs.getString("playeruuid")));
				}
				catch (Exception e) {
					plugin.getLogger().warning("Invalid UUID for playerid " + playerState.getName() + " in SQLite datastore.");
				}
				playerState.setName(rs.getString("playername"));
				playerState.setLastLogin(rs.getLong("lastlogin"));
				playerState.setEarnedClaimBlocks(rs.getInt("earnedblocks"));
				playerState.setPurchasedClaimBlocks(rs.getInt("purchasedblocks"));
				playerState.setBonusClaimBlocks(rs.getInt("bonusblocks"));
			    empty = false;
			}
		
			// if no matching record found for playerUUID, output log message and return null
			if (empty) {
				plugin.getLogger().info("No record found for player UUID " + playerUUID.toString() + ".");
				playerState = null;
			}
		}
		catch (SQLException e) {
			plugin.getLogger().warning("An error occured while attempting to " 
					+ "read a player record by UUID from " + getDisplayName() + " storage.");
			plugin.getLogger().warning(e.getMessage());
			if (plugin.debug) {
				e.printStackTrace();
			}
			playerState = null;
		}
		
		// return player state
		return playerState;
	}
	
	
	/**
	 * Insert new player record in the datastore
	 */
	@Override
	final void insertPlayerStateBlocking(final PlayerState playerState) {

		// if player data is null do nothing and return
		if (playerState == null) {
			return;
		}

		try {

			// convert playerUUID to string
			String stringPlayerUUID = "";
			if (playerState.getPlayerUUID() != null) {
				stringPlayerUUID = playerState.getPlayerUUID().toString();
			}

			// synchronize on database connection
			synchronized(this) {

				PreparedStatement preparedStatement;
				preparedStatement = connection.prepareStatement(Queries.getQuery("InsertPlayerRecord"));

				preparedStatement.setString(1, stringPlayerUUID);
				preparedStatement.setString(2, playerState.getName());
				preparedStatement.setLong(3, playerState.getLastLogin().toEpochMilli());
				preparedStatement.setInt(4, playerState.getEarnedClaimBlocks());
				preparedStatement.setInt(5, playerState.getPurchasedClaimBlocks());
				preparedStatement.setInt(6, playerState.getBonusClaimBlocks());

				preparedStatement.executeUpdate();
			}

			if (plugin.debug) {
				plugin.getLogger().info("Player state for " + playerState.getName() 
				+ " inserted into the " + getDisplayName() + " datastore.");
			}
		}
		catch (SQLException e) {
			plugin.getLogger().warning("An error occured while attempting to "
					+ "insert a player record into the " + getDisplayName() + " datastore.");
			plugin.getLogger().warning(e.getMessage());
			if (plugin.debug) {
				e.printStackTrace();
			}
		}
		
		// put player state record in cache
		this.playerStateCache.store(playerState);
		if (plugin.debug) {
			plugin.getLogger().info("Player state for " + playerState.getName() + " stored in cache.");
		}
	}


	/**
	 * Insert new player record in the datastore
	 */
	@Override
	final void insertPlayerState(final PlayerState playerState) {

		// if player state object is null do nothing and return
		if (playerState == null) {
			if (plugin.debug) {
				plugin.getLogger().info("Could not insert null player state in " 
						+ getDisplayName() + " datastore.");
			}
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {

				try {

					// convert non-null playerUUID to string
					String playerUUID = null;
					if (playerState.getPlayerUUID() != null) {
						playerUUID = playerState.getPlayerUUID().toString();
					}
					
					// synchronize on database connection
					synchronized(this) {
						
						PreparedStatement preparedStatement;
						preparedStatement = connection.prepareStatement(Queries.getQuery("InsertPlayerRecord"));

						preparedStatement.setString(1, playerUUID);
						preparedStatement.setString(2, playerState.getName());
						preparedStatement.setLong(3, playerState.getLastLogin().toEpochMilli());
						preparedStatement.setInt(4, playerState.getEarnedClaimBlocks());
						preparedStatement.setInt(5, playerState.getPurchasedClaimBlocks());
						preparedStatement.setInt(6, playerState.getBonusClaimBlocks());

						preparedStatement.executeUpdate();
					}
					
					if (plugin.debug) {
						plugin.getLogger().info("Player record for " + playerState.getName() 
								+ " inserted into the " + getDisplayName() + " datastore.");
					}
				}
				catch (SQLException e) {
					plugin.getLogger().warning("An error occured while attempting to "
							+ "insert a player record into the " + getDisplayName() + " datastore.");
					plugin.getLogger().warning(e.getMessage());
					if (plugin.debug) {
						e.printStackTrace();
					}
				}
				new BukkitRunnable() {
					@Override
					public void run() {
						// insert player state record in cache
						playerStateCache.store(playerState);
						if (plugin.debug) {
							plugin.getLogger().info("Player record for " + playerState.getName() + " stored in cache.");
						}
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}


	/**
	 * Update existing player record in datastore
	 */
	@Override
	final void updatePlayerState(final PlayerState playerState) {
	
		// if player state object is null do nothing and return
		if (playerState == null) {
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {
		
				try {
					
					// synchronize on database connection
					synchronized(this) {
						
						PreparedStatement preparedStatement;
						preparedStatement = connection.prepareStatement(Queries.getQuery("UpdatePlayerRecord"));

						preparedStatement.setString(1, playerState.getName());
						preparedStatement.setLong(2, playerState.getLastLogin().toEpochMilli());
						preparedStatement.setInt(3, playerState.getEarnedClaimBlocks());
						preparedStatement.setInt(4, playerState.getPurchasedClaimBlocks());
						preparedStatement.setInt(5, playerState.getBonusClaimBlocks());
						preparedStatement.setString(6, playerState.getPlayerUUID().toString());

						preparedStatement.executeUpdate();
					}
					if (plugin.debug) {
						plugin.getLogger().info("Player record for " + playerState.getName() 
						+ " updated in the " + getDisplayName() + " datastore.");
					}
				}
				catch (SQLException e) {
					plugin.getLogger().warning("An error occured while "
							+ "updating a player record in the " + getDisplayName() + " datastore.");
					plugin.getLogger().warning(e.getMessage());
					if (plugin.debug) {
						plugin.getLogger().warning(e.getMessage());
					}
				}
				new BukkitRunnable() {
					@Override
					public void run() {
						// insert player state record in cache
						playerStateCache.store(playerState);
						if (plugin.debug) {
							plugin.getLogger().info("Player state for " 
									+ playerState.getName() + " stored in cache.");
						}
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}


	/**
	 * Delete player record from datastore by player uuid
	 */
	@Override
	final void deletePlayerState(final UUID playerUUID) {
	
		// if key is null or empty, return null record
		if (playerUUID == null) {
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {

				int rowsAffected;

				try {

					// synchronize on database connection
					synchronized(this) {

						// create prepared statement
						PreparedStatement preparedStatement;
						preparedStatement = connection.prepareStatement(Queries.getQuery("DeletePlayerRecord"));

						preparedStatement.setString(1, playerUUID.toString());

						// execute prepared statement
						rowsAffected = preparedStatement.executeUpdate();

						// output debugging information
						if (plugin.debug) {
							plugin.getLogger().info(rowsAffected + " player records deleted.");
						}

						// delete player permission records
						preparedStatement =
								connection.prepareStatement(Queries.getQuery("DeletePlayerPermissions"));

						preparedStatement.setString(1, playerUUID.toString());

						// execute prepared statement
						rowsAffected = preparedStatement.executeUpdate();
					}

					// output debugging information
					if (plugin.debug) {
						plugin.getLogger().info(rowsAffected + " player permission records deleted.");
					}
				}
				catch (Exception e) {

					// output simple error message
					plugin.getLogger().warning("An error occurred while attempting to "
							+ "delete a player record from the " + getDisplayName() + " datastore.");
					plugin.getLogger().warning(e.getLocalizedMessage());

					// if debugging is enabled, output stack trace
					if (plugin.debug) {
						e.getStackTrace();
					}
				}
				new BukkitRunnable() {
					@Override
					public void run() {
						// remove player state record from cache
						playerStateCache.flush(playerUUID);
						if (plugin.debug) {
							plugin.getLogger().info("Player state removed from cache.");
						}
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);

	}


	private void cacheAllClaimRecords() {
		
		int count = 0;
		for (Claim claim : this.getAllClaims()) {
			claimCache.store(claim);
			plugin.worldGuardHelper.syncRegion(claim);
			count++;
		}
		if (plugin.debug) {
			plugin.getLogger().info(count + " claim records stored in cache.");
		}
	}

	
	/**
	 * Read all claim records from datastore into HashSet
	 */
	@Override
	final public Set<Claim> getAllClaims() {
		
		final Set<Claim> returnSet = new HashSet<>();

		try {
			
			PreparedStatement preparedStatement;
			preparedStatement = connection.prepareStatement(Queries.getQuery("SelectAllClaimRecords"));

			ResultSet rs = preparedStatement.executeQuery();
			
			while( rs.next() ) {
				
				Claim claim = new Claim();

				claim.setKey(rs.getInt("claimkey"));

				// test for null or invalid owneruuid
				String ownerUUID = rs.getString("owneruuid");
				if (ownerUUID != null && !ownerUUID.isEmpty()) {
					try {
						claim.setOwnerUUID(UUID.fromString(ownerUUID));
					}
					catch (Exception e) {
						plugin.getLogger().warning("Invalid owner UUID for claim " 
								+ claim.getKey() 
								+ " in " + getDisplayName() + " datastore.");
					}
				}
				else {
					claim.setOwnerUUID(null);
				}
				
				claim.setGroupKey(rs.getInt("claimgroupkey"));
				claim.setLocked(rs.getBoolean("locked"));
				claim.setResizeable(rs.getBoolean("resizable"));
				claim.setParentKey(rs.getInt("parentclaimkey"));
				claim.setCreatedDate(Instant.ofEpochMilli(rs.getLong("ctime")));
				claim.setModifiedDate(Instant.ofEpochMilli(rs.getLong("mtime")));
				
				String worldName = rs.getString("worldname");
				int x1 = rs.getInt("x1");
				int y1 = rs.getInt("y1");
				int z1 = rs.getInt("z1");
				int x2 = rs.getInt("x2");
				int y2 = rs.getInt("y2");
				int z2 = rs.getInt("z2");
				claim.setLowerCorner(new Location(plugin.getServer().getWorld(worldName),x1,y1,z1));
				claim.setUpperCorner(new Location(plugin.getServer().getWorld(worldName),x2,y2,z2));

				// add record to return list
				returnSet.add(claim);
			}
		}
		catch (SQLException e) {
			plugin.getLogger().warning("An error occured while reading claims from the SQLite datastore.");
			plugin.getLogger().warning(e.getMessage());
			if (plugin.debug) {
				e.printStackTrace();
			}
		}
		return Collections.unmodifiableSet(returnSet);
	}

	
	final public Claim getClaim(final Integer claimKey) {
		
		// if claim key is null, return null record
		if (claimKey == null) {
			if (plugin.debug) {
				plugin.getLogger().warning("Could not get claim record for null key.");
			}
			return null;
		}
		
		// try to retrieve claim record from cache
		final Claim claim = claimCache.fetch(claimKey);
		
		// output success or fail message to log if debugging is enabled
		if (plugin.debug) {
			if (claim != null) {
				plugin.getLogger().info("Retrieved claim record from cache.");
			}
			else {
				plugin.getLogger().info("No matching claim record found in cache.");
			}
		}
		
		// all claim records are stored in cache, so not trying datastore at this point.
		// for caching claim records on demand, a negative result would also need to be cached
		// to prevent excessive database lookups when no record exists

		// return claim record; will be null if no record found
		return claim;
	}
	
	
	/**
	 * Get child keys of claim
	 * @param claimKey integer key of claim to retrieve child keys
	 * @return Set of Integer child keys; empty Set if no records found 
	 */
	@Override
	final Set<Integer> getChildKeys(final Integer claimKey) {
	
		// if claim key is null, return empty set
		if (claimKey == null) {
			if (plugin.debug) {
				plugin.getLogger().warning("Could not get child claim records for null key.");
			}
			return Collections.emptySet();
		}
		
		// create empty set for return
		HashSet<Integer> returnSet = new HashSet<>();
		
		// get set of child claims
		Set<Claim> childClaims = plugin.dataStore.getChildClaims(claimKey);
		
		// add each child claim key to return set
		for (Claim childClaim : childClaims) {
			returnSet.add(childClaim.getKey());
		}

		// return unmodifiable set of child keys
		return Collections.unmodifiableSet(returnSet);
	}

	
	@Override
	public final Set<Claim> getChildClaims(final Integer claimKey) {

		// if claim key is null, return empty set
		if (claimKey == null) {
			if (plugin.debug) {
				plugin.getLogger().warning("Could not get child claim records for null key.");
			}
			return Collections.emptySet();
		}
		
		if (claimCache.fetchChildClaims(claimKey).isEmpty()) {
			return Collections.emptySet();
		}
		
		// return unmodifiable set of child claims from cache
		return Collections.unmodifiableSet(claimCache.fetchChildClaims(claimKey));
		
		// all claim records are stored in cache, so not trying datastore at this point.
		// for caching claim records on demand, a negative result would also need to be cached
		// to prevent excessive database lookups when no record exists
	}

	
	@Override
	public final Set<Claim> getPlayerClaims(final UUID ownerUUID) {
		
		// if ownerUUID is null, return null record
		if (ownerUUID == null) {
			if (plugin.debug) {
				plugin.getLogger().warning("Could not get claim records for null player uuid.");
			}
			return Collections.emptySet();
		}
		
		// retrieve claim records from cache
		return Collections.unmodifiableSet(claimCache.fetchClaimsByOwner(ownerUUID));
		
		// all claim records are stored in cache, so not trying datastore at this point.
		// for caching claim records on demand, a negative result would also need to be cached
		// to prevent excessive database lookups when no record exists
	}
	
	
	/**
	 * Get claim record by claim key
	 */
	@SuppressWarnings("unused")
	private Claim selectClaimRecord(final Integer claimKey) {
		
		// if claim key is null, return null record
		if (claimKey == null) {
			return null;
		}
		
		Claim claim = new Claim();
		
		//final String sqlSelectClaimRecordByKey = "SELECT * FROM claims WHERE claimkey = ?";

		try {
			PreparedStatement preparedStatement;
			preparedStatement = connection.prepareStatement(Queries.getQuery("SelectClaimRecordByKey"));
			
			preparedStatement.setInt(1, claimKey);
			
			// execute sql query
			ResultSet rs = preparedStatement.executeQuery();
			
			boolean empty = true;

			// only zero or one record can match the unique key
			if (rs.next()) {
				claim.setKey(rs.getInt("claimkey"));
				
				// test for null or invalid owner uuid
				String ownerUUID = rs.getString("owneruuid");
				if (ownerUUID != null && !ownerUUID.isEmpty()) {
					try {
						claim.setOwnerUUID(UUID.fromString(rs.getString("owneruuid")));
					}
					catch (Exception e) {
						plugin.getLogger().warning("Invalid owner UUID for claim " 
								+ claim.getKey() 
								+ " in " + this.getDisplayName() + " datastore.");
					}
				}

				claim.setGroupKey(rs.getInt("claimgroupkey"));
				claim.setLocked(rs.getBoolean("locked"));
				claim.setResizeable(rs.getBoolean("resizable"));
				claim.setParentKey(rs.getInt("parentclaimkey"));
				claim.setCreatedDate(Instant.ofEpochMilli(rs.getLong("ctime")));
				claim.setModifiedDate(Instant.ofEpochMilli(rs.getLong("mtime")));
				
				String worldName = rs.getString("worldname");
				int x1 = rs.getInt("x1");
				int y1 = rs.getInt("y1");
				int z1 = rs.getInt("z1");
				int x2 = rs.getInt("x2");
				int y2 = rs.getInt("y2");
				int z2 = rs.getInt("z2");
				claim.setLowerCorner(new Location(plugin.getServer().getWorld(worldName),x1,y1,z1));
				claim.setUpperCorner(new Location(plugin.getServer().getWorld(worldName),x2,y2,z2));
				
				if (plugin.debug) {
					plugin.getLogger().info("Claim loaded from " 
							+ this.getDisplayName() + "datastore with worldname " + worldName);
				}
			    empty = false;
			}
			
			// if no matching record found for claim key, output log message and set return claim to null
			if (empty) {
				plugin.getLogger().info("No record found for claim key " + claimKey + ".");
				claim = null;
			}
		}
		catch (SQLException e) {
			plugin.getLogger().warning("An error occured while reading a claim from the SQLite datastore.");
			plugin.getLogger().warning(e.getMessage());
			if (plugin.debug) {
				e.printStackTrace();
			}
		}
		return claim;
	}
	
	@Override
	public final Claim getClaimAt(final Location location) {
		return getClaimAt(location,false);
	}

	@Override
	public final Claim getClaimAt(final Location location, final boolean ignoreHeight) {

		// if location is null, return null record
		if (location == null) {
			if (plugin.debug) {
				plugin.getLogger().info("Could not retrieve claim because passed location is null.");
			}
		}
		return claimCache.fetchClaimAt(location, ignoreHeight);
	}
	
	@SuppressWarnings("unused")
	private Claim selectClaimAt(final Location location, final boolean ignoreHeight) {
	
		Claim claim = new Claim();
		
		final String worldName = location.getWorld().getName();
		final int x = location.getBlockX();
		final int y = location.getBlockY();
		final int z = location.getBlockZ();
		
		// subclaims will always have a higher claimkey than parents,
		// so select first match in descending order
		//final String sqlGetClaimAtLocationIgnoringHeight = "SELECT * FROM claims "
		//		+ "WHERE worldname = ? AND x1 <= ? AND x2 >= ? AND z1 <= ? AND z2 >= ? "
		//		+ "ORDER BY claimkey DESC limit 1";
		
		//final String sqlGetClaimAtLocationObservingHeight = "SELECT * FROM claims "
		//		+ "WHERE worldname = ? AND x1 <= ? AND x2 >= ? AND z1 <= ? AND z2 >= ? AND y1 <= ? "
		//		+ "ORDER BY claimkey DESC limit 1";
		
		try {
			
			PreparedStatement preparedStatement;
			
			if (ignoreHeight) {
				preparedStatement = 
						connection.prepareStatement(Queries.getQuery("SelectClaimAtLocationIgnoringHeight"));
				
				preparedStatement.setString(1,worldName);
				preparedStatement.setInt(2, x);
				preparedStatement.setInt(3, x);
				preparedStatement.setInt(4, z);
				preparedStatement.setInt(5, z);
			}
			else {
			
				preparedStatement = 
						connection.prepareStatement(Queries.getQuery("SelectClaimAtLocationObservingHeight"));
	
				preparedStatement.setString(1,worldName);
				preparedStatement.setInt(2, x);
				preparedStatement.setInt(3, x);
				preparedStatement.setInt(4, z);
				preparedStatement.setInt(5, z);
				//noinspection SuspiciousNameCombination
				preparedStatement.setInt(6, y);
			}
			
			ResultSet rs = preparedStatement.executeQuery();
	
			boolean empty = true;
	
			if (rs.next()) {
				claim.setKey(rs.getInt("claimkey"));
				claim.setGroupKey(rs.getInt("claimgroupkey"));
				claim.setLocked(rs.getBoolean("locked"));
				claim.setResizeable(rs.getBoolean("resizable"));
				claim.setParentKey(rs.getInt("parentclaimkey"));
				claim.setCreatedDate(Instant.ofEpochMilli(rs.getLong("ctime")));
				claim.setModifiedDate(Instant.ofEpochMilli(rs.getLong("mtime")));
				
				try {
					claim.setOwnerUUID(UUID.fromString(rs.getString("owneruuid")));
				} catch (Exception e) {
					claim.setOwnerUUID(null);
				}
				
				String storedWorldName = rs.getString("worldname");
				int x1 = rs.getInt("x1");
				int y1 = rs.getInt("y1");
				int z1 = rs.getInt("z1");
				int x2 = rs.getInt("x2");
				int y2 = rs.getInt("y2");
				int z2 = rs.getInt("z2");
				claim.setLowerCorner(new Location(plugin.getServer().getWorld(storedWorldName),x1,y1,z1));
				claim.setUpperCorner(new Location(plugin.getServer().getWorld(storedWorldName),x2,y2,z2));
				
				if (plugin.debug) {
					plugin.getLogger().info("Claim loaded from " 
							+ getDisplayName() + " datastore with worldname " + storedWorldName);
				}
			    empty = false;
			}
			
			// if no claims found return null
			if (empty) {
				return null;
			}
			
		} catch (SQLException e) {
			plugin.getLogger().warning("There was an error while attempting to"
					+ " find a claim at a location in the " + getDisplayName() + " datastore:");
			plugin.getLogger().warning(e.getMessage());
			if (plugin.debug) {
				e.printStackTrace();
			}
		}
		return claim;
	}


	/**
	 * Inserts a new claim record in the sqlite database
	 */
	@Override
	final void insertClaimBlocking(final Claim claim) {

		// if claim is null, do nothing and return
		if (claim == null) {
			return;
		}

		// get the current system time
		final long currentTime = System.currentTimeMillis();

		if (claim.isLocked() == null) {
			claim.setLocked(false);
		}
		if (claim.getResizeable() == null) {
			claim.setResizeable(true);
		}
		if (claim.getGroupKey() == null) {
			claim.setGroupKey(0);
		}
		if (claim.getParentKey() == null) {
			claim.setParentKey(0);
		}

		//PreparedStatement preparedStatement;

		try {

			// if claim owner uuid is null, insert all zero uuid
			String ownerUUIDString;
			if (claim.getOwnerUUID() != null) {
				ownerUUIDString = claim.getOwnerUUID().toString();
			}
			else {
				ownerUUIDString = zeroUUID.toString();
			}

			// synchronize on database connection
			synchronized(this) {

				PreparedStatement preparedStatement;
				preparedStatement = connection.prepareStatement(Queries.getQuery("InsertClaimRecord"),
						Statement.RETURN_GENERATED_KEYS);

				preparedStatement.setString(1, ownerUUIDString);
				preparedStatement.setInt(2, claim.getParentKey());
				preparedStatement.setInt(3, claim.getGroupKey());
				preparedStatement.setBoolean(4, claim.isLocked());
				preparedStatement.setBoolean(5, claim.getResizeable());
				preparedStatement.setString(6, claim.getLowerCorner().getWorld().getName());
				preparedStatement.setInt(7, claim.getLowerCorner().getBlockX());
				preparedStatement.setInt(8, claim.getLowerCorner().getBlockY());
				preparedStatement.setInt(9, claim.getLowerCorner().getBlockZ());
				preparedStatement.setInt(10, claim.getUpperCorner().getBlockX());
				preparedStatement.setInt(11, claim.getUpperCorner().getBlockY());
				preparedStatement.setInt(12, claim.getUpperCorner().getBlockZ());
				preparedStatement.setLong(13, currentTime);
				preparedStatement.setLong(14, currentTime);

				int affectedRows = preparedStatement.executeUpdate();

				if (plugin.debug) {
					plugin.getLogger().info("Inserted " + affectedRows + " new claim record(s) in the "
							+ getDisplayName() + " datastore.");
				}

				final ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

				// set newClaimKey to newly minted primary key
				if (generatedKeys.next()) {
					claim.setKey(generatedKeys.getInt(1));
				}
			}
		}
		catch (SQLException e) {
			plugin.getLogger().warning("An error occured while "
					+ "inserting a new claim into the SQLite datastore.");
			plugin.getLogger().warning(e.getMessage());
			if (plugin.debug) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * Inserts a new claim record in the sqlite database asynchronously
	 */
	@Override
	final void insertClaim(final Claim claim) {

		// if claim is null, do nothing and return
		if (claim == null) {
			return;
		}

		// get the current system time
		final long currentTime = System.currentTimeMillis();
		
		if (claim.isLocked() == null) {
			claim.setLocked(false);
		}
		if (claim.getResizeable() == null) {
			claim.setResizeable(true);
		}
		if (claim.getGroupKey() == null) {
			claim.setGroupKey(0);
		}
		if (claim.getParentKey() == null) {
			claim.setParentKey(0);
		}

		new BukkitRunnable() {
			@Override
			public void run() {

				try {

					// if claim owner uuid is null, insert all zero uuid
					String ownerUUIDString;
					if (claim.getOwnerUUID() != null) {
						ownerUUIDString = claim.getOwnerUUID().toString();
					}
					else {
						ownerUUIDString = zeroUUID.toString();
					}

					// synchronize on connection database connection
					synchronized(this) {

						PreparedStatement preparedStatement;
						preparedStatement = connection.prepareStatement(Queries.getQuery("InsertClaimRecord"),
								Statement.RETURN_GENERATED_KEYS);

						preparedStatement.setString(1, ownerUUIDString);
						preparedStatement.setInt(2, claim.getParentKey());
						preparedStatement.setInt(3, claim.getGroupKey());
						preparedStatement.setBoolean(4, claim.isLocked());
						preparedStatement.setBoolean(5, claim.getResizeable());
						preparedStatement.setString(6, claim.getLowerCorner().getWorld().getName());
						preparedStatement.setInt(7, claim.getLowerCorner().getBlockX());
						preparedStatement.setInt(8, claim.getLowerCorner().getBlockY());
						preparedStatement.setInt(9, claim.getLowerCorner().getBlockZ());
						preparedStatement.setInt(10, claim.getUpperCorner().getBlockX());
						preparedStatement.setInt(11, claim.getUpperCorner().getBlockY());
						preparedStatement.setInt(12, claim.getUpperCorner().getBlockZ());
						preparedStatement.setLong(13, currentTime);
						preparedStatement.setLong(14, currentTime);

						int affectedRows = preparedStatement.executeUpdate();

						if (plugin.debug) {
							plugin.getLogger().info("Inserted " + affectedRows + " new claim record(s) in the SQLite datastore.");
						}

						ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

						// set newClaimKey to newly minted primary key
						if (generatedKeys.next()) {
							claim.setKey(generatedKeys.getInt(1));
						}
					}
				}
				catch (SQLException e) {
					plugin.getLogger().warning("An error occured while "
							+ "inserting a new claim into the SQLite datastore.");
					plugin.getLogger().warning(e.getMessage());
					if (plugin.debug) {
						e.printStackTrace();
					}
				}
				
				// run task in main thread to insert claim record in cache
				new BukkitRunnable() {
					@Override
					public void run() {
						// insert claim record in cache
						claimCache.store(claim);
						if (plugin.debug) {
							plugin.getLogger().info("Claim record stored in cache.");
						}
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}
	
	
	/**
	 * Updates an existing claim record in the sqlite database
	 */
	@Override
	final void updateClaim(final Claim claim) {
		
		// if claim is null or claim key is null, do nothing and return
		if (claim == null || claim.getKey() == null) {
			return;
		}

		// set claim modified date to current time
		claim.setModifiedDate(Instant.now());

		// set non-null defaults
		if (claim.isLocked() == null) {
			claim.setLocked(false);
		}
		if (claim.getResizeable() == null) {
			claim.setResizeable(true);
		}
		if (claim.getGroupKey() == null) {
			claim.setGroupKey(0);
		}
		if (claim.getParentKey() == null) {
			claim.setParentKey(0);
		}

		new BukkitRunnable() {
			@Override
			public void run() {
		

				try {
					
					// if claim owner uuid is null, insert all zero uuid
					String ownerUUIDString;
					if (claim.getOwnerUUID() != null) {
						ownerUUIDString = claim.getOwnerUUID().toString();
					}
					else {
						ownerUUIDString = zeroUUID.toString();
					}
					
					// synchronize on database connection
					synchronized(this) {

						PreparedStatement preparedStatement;
						preparedStatement = connection.prepareStatement(Queries.getQuery("UpdateClaimRecord"));

						preparedStatement.setString(1, ownerUUIDString);
						preparedStatement.setInt(2, claim.getParentKey());
						preparedStatement.setInt(3, claim.getGroupKey());
						preparedStatement.setBoolean(4, claim.isLocked());
						preparedStatement.setBoolean(5, claim.getResizeable());
						preparedStatement.setString(6, claim.getLowerCorner().getWorld().getName());
						preparedStatement.setInt(7, claim.getLowerCorner().getBlockX());
						preparedStatement.setInt(8, claim.getLowerCorner().getBlockY());
						preparedStatement.setInt(9, claim.getLowerCorner().getBlockZ());
						preparedStatement.setInt(10, claim.getUpperCorner().getBlockX());
						preparedStatement.setInt(11, claim.getUpperCorner().getBlockY());
						preparedStatement.setInt(12, claim.getUpperCorner().getBlockZ());
						preparedStatement.setLong(13, claim.getModifiedDate().toEpochMilli());
						preparedStatement.setInt(14, claim.getKey());

						int rowsAffected = preparedStatement.executeUpdate();

						if (plugin.debug) {
							plugin.getLogger().info("Successfully updated " + rowsAffected 
									+ " claim(s) in the " + getDisplayName() + " datastore.");
						}
					}
				}
				catch (SQLException e) {
					plugin.getLogger().warning("An error occured while "
							+ "updating claim data in the " + getDisplayName() + " datastore.");
					plugin.getLogger().warning(e.getMessage());
					if (plugin.debug) {
						e.getStackTrace();
					}
				}

				// run task in main thread to update claim record in cache
				new BukkitRunnable() {
					@Override
					public void run() {
						// insert claim record in cache
						claimCache.store(claim);
						if (plugin.debug) {
							plugin.getLogger().info("Claim record updated in cache.");
						}
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}
	
	
	/**
	 * Delete a claim record from the datastore by claim key
	 * and all child claim records
	 * and all permissions records
	 */
	@Override
	final void deleteClaim(final Integer claimKey) {

		// if key is null or empty, do nothing and return
		if (claimKey == null) {
			plugin.getLogger().warning(getDisplayName() + " datastore could not delete claim because "
					+ "passed key is null.");
			return;
		}
		
		new BukkitRunnable() {
			@Override
			public void run() {

				int rowsAffected;

				try {

					// synchronize on database connection
					synchronized(this) {

						// create prepared statement
						PreparedStatement preparedStatement;
						preparedStatement = connection.prepareStatement(Queries.getQuery("DeleteClaimRecord"));

						preparedStatement.setInt(1, claimKey);
						preparedStatement.setInt(2, claimKey);

						// execute prepared statement
						rowsAffected = preparedStatement.executeUpdate();

						// output debugging information
						if (plugin.debug) {
							plugin.getLogger().info(rowsAffected + " claim records deleted from the "
									+ getDisplayName() + " datastore.");
						}

						// delete claim permissions
						preparedStatement = connection.prepareStatement(Queries.getQuery("DeleteClaimPermissions"));

						preparedStatement.setInt(1, claimKey);

						// execute prepared statement
						rowsAffected = preparedStatement.executeUpdate();

						// output debugging information
						if (plugin.debug) {
							plugin.getLogger().info(rowsAffected + " claim permission records for claim "
									+ claimKey + " deleted from the " + getDisplayName() + " datastore.");
						}
					}
				}
				catch (Exception e) {

					// output simple error message
					plugin.getLogger().warning("An error occurred while attempting to "
							+ "delete a claim record from the " + getDisplayName() + " datastore.");
					plugin.getLogger().warning(e.getLocalizedMessage());

					// if debugging is enabled, output stack trace
					if (plugin.debug) {
						e.getStackTrace();
					}
				}
				
				// run task in main thread to remove claim record from cache
				new BukkitRunnable() {
					@Override
					public void run() {
						
						// get all child claim keys, also to be removed
						Set<Integer> deleteKeys = new HashSet<>(getChildKeys(claimKey));
						
						// add claim key to set
						deleteKeys.add(claimKey);
						
						// iterate over set and remove all claims from cache
						int count = 0;
						for (Integer deleteKey : deleteKeys) {
							
							// remove claim record from cache
							claimCache.flush(deleteKey);
							count++;
						}
						// send debug message to log
						if (plugin.debug) {
							plugin.getLogger().info(count + " claim record(s) removed from cache.");
						}
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);

	}
	
	/**
	 * Load all claim permissions into cache
	 */
	private void cacheAllClaimPermissions() {
		
		int count = 0;
		for (ClaimPermission claimPermission : this.getAllClaimPermissions()) {
			permissionCache.store(claimPermission);
			count++;
		}
		if (plugin.debug) {
			plugin.getLogger().info(count + " permission records stored in cache.");
		}
	}
	
	/**
	 * Get all permissions from datastore
	 */
	@Override
	final Set<ClaimPermission> getAllClaimPermissions() {

		// create new HashSet for return
		Set<ClaimPermission> returnSet = new HashSet<>();
		
		PreparedStatement preparedStatement;
		
		try {
			preparedStatement = connection.prepareStatement(Queries.getQuery("SelectAllPermissions"));
	
			ResultSet rs = preparedStatement.executeQuery();
	
			while (rs.next()) {

				ClaimPermission claimPermission = new ClaimPermission();
				
				claimPermission.setKey(rs.getInt("permissionkey"));
				claimPermission.setClaimKey(rs.getInt("claimkey"));
				claimPermission.setPlayerUUID(UUID.fromString(rs.getString("playeruuid")));
				claimPermission.setPermissionLevel(PermissionLevel.fromByte(rs.getByte("permission")));
				
				returnSet.add(claimPermission);
			}
		}
		catch (SQLException e) {
			plugin.getLogger().warning("An error occured while attempting to read permission data from the " 
					+ getDisplayName() + " datastore.");
		}
		
		return Collections.unmodifiableSet(returnSet);
	}

	@Override
	final ClaimPermission getClaimPermission(final Integer claimKey, final UUID playerUUID) {
		
		// if claim key is null, return null record
		if (claimKey == null) {
			if (plugin.debug) {
				plugin.getLogger().warning("Could not get permission record for null claim key.");
			}
			return null;
		}
		
		// if player key is null, return null record
		if (playerUUID == null) {
			if (plugin.debug) {
				plugin.getLogger().warning("Could not get permission record for null player uuid.");
			}
			return null;
		}
		
		// get permission record from permission cache; will be null if no record found
		return permissionCache.fetch(claimKey, playerUUID);
		
		// all permission records are stored in cache, so not trying datastore at this point.
		// for caching permission records on demand, a negative result would also need to be cached
		// to prevent excessive database lookups when no record exists
	}
	

	@Override
	final ClaimPermission getClaimPermission(final Integer permissionRecordKey) {
		
		// if permission record key is null, return null record
		if (permissionRecordKey == null) {
			if (plugin.debug) {
				plugin.getLogger().warning("Could not get permission record for null key.");
			}
			return null;
		}
		
		// return record from cache; will be null if no record found
		return permissionCache.fetch(permissionRecordKey);
	}
	

	@Override
	final Set<Integer> getClaimPermissionKeys(final Integer claimKey) {
		
		// get all permission record keys for a claim
		return Collections.unmodifiableSet((permissionCache.getCacheMapKeys(claimKey)));
	}
	
	
	/**
	 * get player permission level for claim by claimKey, playerUUID 
	 */
	@SuppressWarnings("unused")
	private ClaimPermission selectPermissionRecord(final Integer claimKey, final UUID playerUUID) {
	
		// if claim key is null, return null record
		if (claimKey == null) {
			if (plugin.debug) {
				plugin.getLogger().warning("Could not select permission record for null claim key.");
			}
			return null;
		}
		
		// if player uuid is null, return null record
		if (playerUUID == null) {
			if (plugin.debug) {
				plugin.getLogger().warning("Could not select permission record for null player uuid.");
			}
			return null;
		}
		
		// initialize new permission record
		ClaimPermission claimPermission = new ClaimPermission();
		
		if (plugin.debug) {
			plugin.getLogger().info("Getting permission record from " 
					+ getDisplayName() + " datastore by claim key and player uuid.");
		}

		PreparedStatement preparedStatement;
		
		try {
			preparedStatement = 
					connection.prepareStatement(Queries.getQuery("SelectPermissionRecord"));
			
			preparedStatement.setInt(1, claimKey);
			preparedStatement.setString(2, playerUUID.toString());
			
			ResultSet rs = preparedStatement.executeQuery();
			
			// only zero or one record can match unique (claimKey,playerUUID)
			if( rs.next() ) {
				
				claimPermission.setClaimKey(claimKey);
				claimPermission.setPlayerUUID(playerUUID);
				claimPermission.setPermissionLevel(PermissionLevel.fromByte(rs.getByte("permission")));
			}
	
		}
		catch (SQLException e) {
			plugin.getLogger().warning("An error occured while attempting to "
					+ "select a permission record from the " + getDisplayName() + " datastore.");
		}
		
		// if permission level is null, return null record
		if (claimPermission.getPermissionLevel() == null) {
			return null;
		}
		
		// return permission record
		return claimPermission;
	}

	
	/**
	 * get player (or group) permission level for claim by permission record key
	 */
	@SuppressWarnings("unused")
	private ClaimPermission selectPermissionRecord(final Integer permissionRecordKey) {
	
		// if permission record key is null, return null record
		if (permissionRecordKey == null) {
			if (plugin.debug) {
				plugin.getLogger().warning("Could not select permission record for null key.");
			}
			return null;
		}
		
		// initialize new permission record
		ClaimPermission claimPermission = new ClaimPermission();
		
		if (plugin.debug) {
			plugin.getLogger().info("Getting permission record from " 
					+ getDisplayName() + " datastore by permission record key.");
		}

		PreparedStatement preparedStatement;
		
		try {
			preparedStatement = connection.prepareStatement(Queries.getQuery("SelectPermissionRecordByKey"));
			
			preparedStatement.setInt(1, permissionRecordKey);
			
			ResultSet rs = preparedStatement.executeQuery();
			
			// only zero or one record can match unique (claimKey,playerKey)
			if( rs.next() ) {
				
				claimPermission.setClaimKey(rs.getInt("claimkey"));
				claimPermission.setPlayerUUID(UUID.fromString(rs.getString("playeruuid")));
				claimPermission.setPermissionLevel(PermissionLevel.fromByte(rs.getByte("permission")));
			}
	
		}
		catch (SQLException e) {
			plugin.getLogger().warning("An error occured while attempting to "
					+ "select a permission record from the " + getDisplayName() + " datastore.");
		}
		
		// if permission level is null, return null record
		if (claimPermission.getPermissionLevel() == null) {
			return null;
		}
		
		// return permission record
		return claimPermission;
	}

	
	@Override
	final void insertClaimPermissionBlocking(final ClaimPermission claimPermission) {
		
		// if player data is null do nothing and return
		if (claimPermission == null) {
			return;
		}


		try {

			// synchronize on database connection
			synchronized(this) {

				PreparedStatement preparedStatement;
				preparedStatement = connection.prepareStatement(Queries.getQuery("InsertPermissionRecord"),
						Statement.RETURN_GENERATED_KEYS);

				preparedStatement.setInt(1, claimPermission.getClaimKey());
				preparedStatement.setString(2, claimPermission.getPlayerUUID().toString());
				preparedStatement.setByte(3, claimPermission.getPermissionLevel().toByte());

				preparedStatement.executeUpdate();

				ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

				// set permissionRecordKey to newly minted primary key
				if (generatedKeys.next()) {
					Integer newKey = generatedKeys.getInt(1);
					claimPermission.setKey(newKey);
				}
			}
			
			if (plugin.debug) {
				plugin.getLogger().info("Permission record inserted into " + getDisplayName() + " datastore.");
			}
		}
		catch (SQLException e) {
			plugin.getLogger().warning("An error occured while attempting to "
					+ "save a permission record to the " + getDisplayName() + " datastore.");
			plugin.getLogger().warning(e.getMessage());
			if (plugin.debug) {
				e.printStackTrace();
			}
		}
		
		// insert permission record in cache
		permissionCache.store(claimPermission);
		if (plugin.debug) {
			plugin.getLogger().info("Permission record stored in cache.");
		}
	}
	
	
	@Override
	final void insertClaimPermission(final ClaimPermission claimPermission) {

		// if player data is null do nothing and return
		if (claimPermission == null) {
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {



				try {

					// synchronize on database connection
					synchronized(this) {

						PreparedStatement preparedStatement;
						preparedStatement = connection.prepareStatement(Queries.getQuery("InsertPermissionRecord"),
								Statement.RETURN_GENERATED_KEYS);

						preparedStatement.setInt(1, claimPermission.getClaimKey());
						preparedStatement.setString(2, claimPermission.getPlayerUUID().toString());
						preparedStatement.setByte(3, claimPermission.getPermissionLevel().toByte());

						preparedStatement.executeUpdate();

						ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

						// set permissionRecordKey to newly minted primary key
						if (generatedKeys.next()) {
							Integer newKey = generatedKeys.getInt(1);
							claimPermission.setKey(newKey);
						}
					}

					if (plugin.debug) {
						plugin.getLogger().info("Permission record inserted into " 
								+ getDisplayName() + " datastore.");
					}
				}
				catch (SQLException e) {
					plugin.getLogger().warning("An error occured while attempting to "
							+ "insert a permission record into the " + getDisplayName() + " datastore.");
					plugin.getLogger().warning(e.getMessage());
					if (plugin.debug) {
						e.printStackTrace();
					}
				}
				
				// run task in main thread to insert permission record in cache
				new BukkitRunnable() {
					@Override
					public void run() {
						// insert permission record in cache
						permissionCache.store(claimPermission);
						if (plugin.debug) {
							plugin.getLogger().info("Permission record stored in cache.");
						}
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}
	
	
	/**
	 * Updates an existing claim record in the sqlite database
	 */
	@Override
	final void updateClaimPermission(final ClaimPermission claimPermission) {
		
		// if permission record is null, log error and return
		if (claimPermission == null) {
			plugin.getLogger().warning("Could not update a permission record "
					+ "in the " + this.getDisplayName() + " datastore because the record was null.");
			return;
		}
		
		// if permission record key is null, log error and return
		if (claimPermission.getKey() == null) {
			plugin.getLogger().warning("Could not update a permission record "
					+ "in the " + this.getDisplayName() + " datastore because the key was null.");
			return;
		}
		
		// if claim key is null, log error and return
		if (claimPermission.getClaimKey() == null) {
			plugin.getLogger().warning("Could not update a permission record "
					+ "in the " + this.getDisplayName() + " datastore because the claim key was null.");
			return;
		}
		
		// if player key is null, log error and return
		if (claimPermission.getPlayerUUID() == null) {
			plugin.getLogger().warning("Could not update a permission record "
					+ "in the " + this.getDisplayName() + " datastore because the player uuid was null.");
			return;
		}

		// if permission level is null, log error and return
		if (claimPermission.getPermissionLevel() == null) {
			plugin.getLogger().warning("Could not update a permission record "
					+ "in the " + this.getDisplayName() + " datastore because the permission level was null.");
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {
		
				try {
					
					// synchronize on database connection
					synchronized(this) {

						PreparedStatement preparedStatement;
						preparedStatement = connection.prepareStatement(Queries.getQuery("UpdatePermissionRecord"));

						preparedStatement.setInt(1, claimPermission.getClaimKey());
						preparedStatement.setString(2, claimPermission.getPlayerUUID().toString());
						preparedStatement.setByte(3, claimPermission.getPermissionLevel().toByte());
						preparedStatement.setInt(4, claimPermission.getKey());

						int rowsAffected = preparedStatement.executeUpdate();

						if (plugin.debug) {
							plugin.getLogger().info("Successfully updated " + rowsAffected 
									+ " permission record(s) in the " + getDisplayName() + " datastore.");
						}
					}
				}
				catch (SQLException e) {
					plugin.getLogger().warning("An error occured while "
							+ "updating a permission record in the " + getDisplayName() + " datastore.");
					plugin.getLogger().warning(e.getMessage());
					if (plugin.debug) {
						plugin.getLogger().warning(e.getMessage());
					}
				}
				new BukkitRunnable() {
					@Override
					public void run() {
						// insert permission record in cache
						permissionCache.store(claimPermission);
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}


	/**
	 * Delete all permission records for claim
	 */
	@Override
	final void deleteAllClaimPermissions(final Integer claimKey) {
		
		if (claimKey == null) {
			plugin.getLogger().info("Could not delete permission record from the " 
					+ getDisplayName() + " datastore because the passed claim key was null.");
			return;
		}

		// get permission records, to get keys for cache flush
		final Set<Integer> permissionKeys = getClaimPermissionKeys(claimKey);

		new BukkitRunnable() {
			@Override
			public void run() {
	
				try {
					
					int rowsAffected;
					
					// synchronize on database connection
					synchronized(this) {

						PreparedStatement preparedStatement = 
								connection.prepareStatement(Queries.getQuery("DeletePermissionRecordsForClaim"));

						preparedStatement.setInt(1, claimKey);

						rowsAffected = preparedStatement.executeUpdate();
					}
					
					if (plugin.debug) {
						plugin.getLogger().info(rowsAffected + " permission records for claim " 
								+ claimKey + " were removed from the " + getDisplayName() + " datastore.");
					}
				}
				catch (SQLException e) {
					plugin.getLogger().warning("An error occured while attempting to"
							+ " remove permission records from the " + getDisplayName() + " datastore.");
					return;
				}
				
				// remove permission records from cache in main thread
				new BukkitRunnable() {
					@Override
					public void run() {
						// flush permission record from cache
						for (Integer key : permissionKeys) {
							permissionCache.flush(key);
						}
						if (plugin.debug) {
							plugin.getLogger().info("Claim permission records remove from cache.");
						}
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}
	

	/**
	 * Delete player's claim permissions by claimKey, playerKey
	 * @param claimKey integer key of claim to delete permission
	 * @param playerUUID UUID of player to delete permission
	 */
	@Override
	final void deletePlayerClaimPermission(final Integer claimKey, final UUID playerUUID) {
		
		if (claimKey == null) {
			plugin.getLogger().info("Could not delete permission record from the " 
					+ getDisplayName() + " datastore because the passed claim key was null.");
			return;
		}

		if (playerUUID == null) {
			plugin.getLogger().info("Could not delete permission record from the " 
					+ getDisplayName() + " datastore because the passed player uuid was null.");
			return;
		}

		// get permission record, to get key for cache flush
		final ClaimPermission claimPermission = getClaimPermission(claimKey,playerUUID);

		// if no record retrieved, no deletion is necessary
		if (claimPermission == null) {
			if (plugin.debug) {
				plugin.getLogger().info("There was no permission record to delete "
						+ "with the passed claim key and player uuid.");
			}
			return;
		}

		// delete permission record asynchronously
		new BukkitRunnable() {
			@Override
			public void run() {
	
				try {
					
					int rowsAffected;

					// synchronize on database connection
					synchronized(this) {

						PreparedStatement preparedStatement;
						preparedStatement = 
								connection.prepareStatement(Queries.getQuery("DeletePermissionRecord"));
						preparedStatement.setInt(1, claimKey);
						preparedStatement.setString(2, playerUUID.toString());

						rowsAffected = preparedStatement.executeUpdate();
					}
					
					if (plugin.debug) {
						plugin.getLogger().info(rowsAffected + " permssion records for claim " 
								+ claimKey + " were deleted from the " + getDisplayName() + " datastore.");
					}
				}
				catch (SQLException e) {
					plugin.getLogger().warning("An error occured while attempting to "
							+ "remove permissions from the " + getDisplayName() + " datastore.");
				}
				
				// remove record from cache in main thread
				new BukkitRunnable() {
					@Override
					public void run() {
						// flush permission record from cache
						permissionCache.flush(claimPermission.getKey());
						if (plugin.debug) {
							plugin.getLogger().info("Permission record remove from cache.");
						}
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}
	
	
	private void cacheAllClaimGroupRecords() {
		
		int count = 0;
		for (ClaimGroup claimGroup : this.getAllClaimGroups()) {
			claimGroupCache.store(claimGroup);
			count++;
		}
		if (plugin.debug) {
			plugin.getLogger().info(count + " claim group records stored in cache.");
		}
	}

	
	/**
	 * Get all claim group records
	 */
	//TODO: make this get claim groups from cache
	@Override
	final Set<ClaimGroup> getAllClaimGroups() {
	
		Set<ClaimGroup> returnSet = new HashSet<>();
	
		try {
			// create prepared statement
			PreparedStatement preparedStatement = 
					connection.prepareStatement(Queries.getQuery("SelectAllClaimGroups"));
	
			// execute sql query
			ResultSet rs = preparedStatement.executeQuery();
			
			while (rs.next()) {
				
				ClaimGroup claimGroup = new ClaimGroup();
				
				claimGroup.setKey(rs.getInt("claimgroupkey"));
				claimGroup.setName(rs.getString("groupname"));
				claimGroup.setClaimLimit(rs.getInt("claimlimit"));
				
				returnSet.add(claimGroup);
				
			}
		}
		catch  (SQLException e) {
			plugin.getLogger().warning("An error occured while attempting to "
					+ "read all claim group records from the " + getDisplayName() + " datastore.");
			plugin.getLogger().warning(e.getMessage());
			if (plugin.debug) {
				e.printStackTrace();
			}
		}
		
		// return unmodifiable view of return set
		return Collections.unmodifiableSet(returnSet);
	}


	/**
	 * Get number of claims in group owned by player, by playerUUID
	 * @param claimGroup claim group object to count claim membership
	 * @param playerUUID UUID of player to count claim ownership
	 * @return int number of claims owned by player in claim group
	 */
	@Override
	final int getPlayerClaimGroupCount(final ClaimGroup claimGroup, final UUID playerUUID) {
		
		int count = 0;

		// check each claim owned by player for matching claim group and increment count
		for (Claim claim : plugin.dataStore.getPlayerClaims(playerUUID)) {
			
			if (claim.getGroupKey().equals(claimGroup.getKey())) {
				count++;
			}
		}
		return count;
	}
	
	
	/**
	 * Get claim group by name
	 * @param claimGroupName string name of claim group to retrieve
	 * @return ClaimGroup
	 */
	@Override
	final ClaimGroup getClaimGroup(final String claimGroupName) {
		
		// if claim group record key is null, return null record
		if (claimGroupName == null || claimGroupName.isEmpty()) {
			if (plugin.debug) {
				plugin.getLogger().warning("Could not get claim group record for null or empty name.");
			}
			return null;
		}
		
		// all claim group records are stored in cache, so not trying datastore at this point.

		// return claim group record; will be null if no record found
		return claimGroupCache.fetch(claimGroupName);
	}

	
	@Override
	final ClaimGroup getClaimGroup(final Integer claimGroupKey) {
		
		// if claim group record key is null, return null record
		if (claimGroupKey == null) {
			if (plugin.debug) {
				plugin.getLogger().warning("Could not get claim group record for null key.");
			}
			return null;
		}
		
		// all claim group records are stored in cache, so not trying datastore at this point.
		// return claim group record; will be null if no record found
		return claimGroupCache.fetch(claimGroupKey);
	}
	
	/**
	 * Get claim group by claimGroupKey
	 */
	@SuppressWarnings("unused")
	private ClaimGroup selectClaimGroupRecord(final Integer claimGroupKey) {

		// claimGroupId is null or zero, return a null claimGroup
		if (claimGroupKey == null || claimGroupKey == 0) {
			if (plugin.debug) {
				plugin.getLogger().warning("Could not get claim group record for null key.");
			}
			return null;
		}
		
		// create new claim group for return
		ClaimGroup claimGroup = new ClaimGroup();

		try {
			// create prepared statement
			PreparedStatement preparedStatement = 
					connection.prepareStatement(Queries.getQuery("SelectClaimGroupByKey"));
	
			preparedStatement.setInt(1, claimGroupKey);
	
			// execute sql query
			ResultSet rs = preparedStatement.executeQuery();
			
			if (rs.next()) {
				claimGroup.setKey(rs.getInt("claimgroupkey"));
				claimGroup.setName(rs.getString("groupname"));
				claimGroup.setClaimLimit(rs.getInt("claimlimit"));
			}
			else {
				// return null claim group if none found
				claimGroup = null;
			}
		}
		catch  (SQLException e) {
			plugin.getLogger().warning("An error occured while attempting to "
					+ "read a claim group from the " + getDisplayName() + " datastore.");
			plugin.getLogger().warning(e.getMessage());
			if (plugin.debug) {
				e.printStackTrace();
			}
		}			
		
		return claimGroup;
	}
	
	
	/**
	 * Insert new claim group record
	 */
	@Override
	final void insertClaimGroupBlocking(final ClaimGroup claimGroup) {

		// if claim group is null, do nothing and return
		if (claimGroup == null) {
			return;
		}

		try {

			// synchronize on database connection
			synchronized(this) {

				// create prepared statement
				PreparedStatement preparedStatement;
				preparedStatement = connection.prepareStatement(Queries.getQuery("InsertClaimGroup"));

				preparedStatement.setString(1, claimGroup.getName());
				preparedStatement.setInt(2, claimGroup.getClaimLimit());

				// execute sql query
				int rowsAffected = preparedStatement.executeUpdate();

				if (plugin.debug) {
					plugin.getLogger().info("Inserted " + rowsAffected + " new claim group record(s) in the "
							+ getDisplayName() + " datastore.");
				}

				ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

				// set newClaimKey to newly minted primary key
				if (generatedKeys.next()) {
					claimGroup.setKey(generatedKeys.getInt(1));
				}
			}

		}
		catch (SQLException e) {
			plugin.getLogger().warning("An error occured while attempting to "
					+ "insert a claim group in the " + getDisplayName() + " datastore.");
			plugin.getLogger().warning(e.getMessage());
			if (plugin.debug) {
				e.printStackTrace();
			}
		}
		
		// insert claim group record in cache
		claimGroupCache.store(claimGroup);
		if (plugin.debug) {
			plugin.getLogger().info("Claim group record stored in cache.");
		}
	}


	/**
	 * Insert new claim group record in SQLite datastore asynchronously
	 */
	@Override
	final void insertClaimGroup(final ClaimGroup claimGroup) {

		// if claim group is null, do nothing and return null record
		if (claimGroup == null) {
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {

				try {

					// synchronize on database connection
					synchronized(this) {

						// create prepared statement
						PreparedStatement preparedStatement = 
								connection.prepareStatement(Queries.getQuery("InsertClaimGroupRecord"));

						preparedStatement.setString(1, claimGroup.getName());
						preparedStatement.setInt(2, claimGroup.getClaimLimit());

						// execute sql query
						int rowsAffected = preparedStatement.executeUpdate();

						if (plugin.debug) {
							plugin.getLogger().info("Inserted " + rowsAffected + " new claim group record(s) in the "
									+ getDisplayName() + " datastore.");
						}

						ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

						// set newClaimKey to newly minted primary key
						if (generatedKeys.next()) {
							claimGroup.setKey(generatedKeys.getInt(1));
						}
					}
				}
				catch (SQLException e) {
					plugin.getLogger().warning("An error occured while attempting to "
							+ "insert a claim group in the " + getDisplayName() + " datastore.");
					plugin.getLogger().warning(e.getMessage());
					if (plugin.debug) {
						e.printStackTrace();
					}
				}
				
				// run task in main thread to insert record in cache
				new BukkitRunnable() {
					@Override
					public void run() {
						// insert claim group record in cache
						claimGroupCache.store(claimGroup);
						if (plugin.debug) {
							plugin.getLogger().info("Claim group record stored in cache.");
						}
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}
	
	
	/**
	 * Update an existing claim record
	 */
	@Override
	final void updateClaimGroup(final ClaimGroup claimGroup) {
		
		// if claim is null or claim key is null, do nothing and return
		if (claimGroup == null || claimGroup.getKey() == null) {
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {
		

				try {
					
					int rowsAffected;
					
					// synchronize on database connection
					synchronized(this) {

						PreparedStatement preparedStatement;
						preparedStatement = connection.prepareStatement(Queries.getQuery("UpdateClaimGroupRecord"));

						preparedStatement.setString(1, claimGroup.getName());
						preparedStatement.setInt(2, claimGroup.getClaimLimit());
						preparedStatement.setInt(3, claimGroup.getKey());

						rowsAffected = preparedStatement.executeUpdate();
					}
					
					if (plugin.debug) {
						plugin.getLogger().info("Successfully updated " + rowsAffected + " claim(s) in the "
								+ getDisplayName() + " datastore.");
					}
				}
				catch (SQLException e) {
					plugin.getLogger().warning("An error occured while "
							+ "updating claim group data in the " + getDisplayName() + " datastore.");
					plugin.getLogger().warning(e.getMessage());
					if (plugin.debug) {
						plugin.getLogger().warning(e.getMessage());
					}
				}
				
				// run task in main thread to store claim group record in cache
				new BukkitRunnable() {
					@Override
					public void run() {
						// store claim group record in cache
						claimGroupCache.store(claimGroup);
						if (plugin.debug) {
							plugin.getLogger().info("Claim group record updated in cache.");
						}
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}
	
	
	/**
	 * Delete a claim group record
	 */
	final void deleteClaimGroup(final Integer claimGroupKey) {
		
		// if key is null, do nothing and return
		if (claimGroupKey == null) {
			plugin.getLogger().warning("Could not delete claim group from the "
					+ getDisplayName() + " datastore because passed key is null.");
			return;
		}

		new BukkitRunnable() {
			@Override
			public void run() {

				try {

					int rowsAffected;

					// synchronize on database connection
					synchronized(this) {

						// create prepared statement
						PreparedStatement preparedStatement = 
								connection.prepareStatement(Queries.getQuery("DeleteClaimGroupRecord"));

						preparedStatement.setInt(1, claimGroupKey);

						// execute prepared statement
						rowsAffected = preparedStatement.executeUpdate();

						// output debugging information
						if (plugin.debug) {
							plugin.getLogger().info(rowsAffected + " claim group records deleted.");
						}

						// remove groupkey from claim records
						preparedStatement = 
								connection.prepareStatement(Queries.getQuery("UpdateClaimGroupKeyInClaimRecords"));

						preparedStatement.setInt(1,  claimGroupKey);

						// execute prepared statement
						rowsAffected = preparedStatement.executeUpdate();
					}
					
					// output debugging information
					if (plugin.debug) {
						plugin.getLogger().info(rowsAffected + " claim group keys removed from claim records in the "
								+ getDisplayName() + " datastore.");
					}
				}
				catch (Exception e) {

					// output simple error message
					plugin.getLogger().warning("An error occurred while attempting to "
							+ "delete a claim group record from the " + getDisplayName() + " datastore.");
					plugin.getLogger().warning(e.getLocalizedMessage());

					// if debugging is enabled, output stack trace
					if (plugin.debug) {
						e.getStackTrace();
					}
				}
				
				// run task in main thread to remove claim group record from cache
				new BukkitRunnable() {
					@Override
					public void run() {
						// store claim group record in cache
						claimGroupCache.flush(claimGroupKey);
						if (plugin.debug) {
							plugin.getLogger().info("Claim group record removed from cache.");
						}
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}

}
