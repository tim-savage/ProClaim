package com.winterhaven_mc.proclaim.storage;

import com.winterhaven_mc.proclaim.PluginMain;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class PlayerStateCache {
	
	// reference to main class
	private final PluginMain plugin;
	
	// in-memory cache for player state by UUID
	private final ConcurrentHashMap<UUID, CachedPlayerState> playerStateMap;
	
	// cache lookup table for playerName
//	private final ConcurrentHashMap<String, UUID> playerNameIndex;
	

	/**
	 * Private class that implements a wrapper for a PlayerState object
	 * @author Tim Savage
	 *
	 */
	private final class CachedPlayerState {
	
		private final PlayerState playerState;
		private Instant lastCacheHit;

		CachedPlayerState(final PlayerState playerState) {
			
			this.playerState = playerState;
			this.lastCacheHit = Instant.now();
		}
		
		@SuppressWarnings("unused")
		final Instant getLastCacheHit() {
			return lastCacheHit;
		}

		final void setLastCacheHit() {
			this.lastCacheHit = Instant.now();
		}
		
		final PlayerState getPlayerState() {
			return this.playerState;
		}
		
		final String getName() {
			if (this.getPlayerState() == null) {
				return null;
			}
			return this.getPlayerState().getName();
		}
	}
	
	
	/**
	 * Class constructor
	 * @param plugin reference to plugin main class
	 */
	PlayerStateCache(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// instantiate player state map
		playerStateMap = new ConcurrentHashMap<>();
		
		// instantiate player name index
//		playerNameIndex = new ConcurrentHashMap<>();
	}
	
	
	/**
	 * Retrieve player state from the cache by player UUID
	 * @param playerUUID UUID of player to retrieve from cache
	 * @return PlayerState object or null if not found in cache
	 */
	final PlayerState fetch(final UUID playerUUID) {
		
		if (playerUUID == null) {
			if (plugin.debug) {
				plugin.getLogger().info("PlayerStateCache was passed a null UUID for lookup.");
			}
			return null;
		}
		
		final CachedPlayerState cachedPlayerState = playerStateMap.get(playerUUID);
		
		if (cachedPlayerState != null && cachedPlayerState.getPlayerState() != null) {
			cachedPlayerState.setLastCacheHit();
			return cachedPlayerState.getPlayerState();
		}
		return null;
	}
	
	
//	/**
//	 * Retrieve player state from the cache by playerName
//	 * @param playerName
//	 * @return PlayerState object or null if not found in cache
//	 */
//	final PlayerState fetch(final String playerName) {
//
//		if (playerName == null || playerName.isEmpty()) {
//			if (plugin.debug) {
//				plugin.getLogger().info("PlayerStateCache was passed a null or empty name for lookup.");
//			}
//			return null;
//		}
//
//		final UUID playerUUID = playerNameIndex.get(playerName);
//
//		if (playerUUID == null) {
//			return null;
//		}
//
//		final CachedPlayerState cachedPlayerState = playerStateMap.get(playerUUID);
//
//		if (cachedPlayerState != null && cachedPlayerState.getPlayerState() != null) {
//			cachedPlayerState.setLastCacheHit();
//			return cachedPlayerState.getPlayerState();
//		}
//		return null;
//	}
	
	
//	/**
//	 * Retrieve player state from the cache by playerName
//	 * without updating last cache hit
//	 * @param playerName player name to retrieve from cache
//	 * @return PlayerData
//	 */
//	final PlayerState peek(final String playerName) {
//
//		CachedPlayerState cachedPlayerState = null;
//		final UUID playerUUID = playerNameIndex.get(playerName);
//
//		if (playerUUID != null) {
//			cachedPlayerState = playerStateMap.get(playerUUID);
//		}
//		if (cachedPlayerState != null) {
//			return cachedPlayerState.getPlayerState();
//		}
//		return null;
//	}
	
	
	/**
	 * Store player state in the cache
	 * @param playerState player state record to store in cache
	 */
	final void store(final PlayerState playerState) {

		// if player state is null, do nothing and return
		if (playerState == null) {
			if (plugin.debug) {
				plugin.getLogger().warning("Attempted to insert null player state in the cache.");
			}
			return;
		}
		
		// if player UUID is null, do nothing and return
		if (playerState.getPlayerUUID() == null) {
			if (plugin.debug) {
				plugin.getLogger().warning("Attempted to insert null player UUID in the cache.");
			}
			return;
		}

		final String playerName = playerState.getName();
		final UUID playerUUID = playerState.getPlayerUUID();
		
		// insert playerState into cache
		playerStateMap.put(playerUUID, new CachedPlayerState(playerState));
		
		// if playerName is null or empty, don't put it in the playerName index map
		if (playerName == null || playerName.isEmpty()) {
			if (plugin.debug) {
				plugin.getLogger().warning("Attempted to insert null or blank player name in the cache index.");
			}
//			return;
		}
		
		// not removing name index entries on update. if a player changes their name within the cache lifetime,
		// references using the old name will still work. if another player with the old name enters 
		// the cache, the name index will be updated then to refer to the new player with that name.
		// this is acceptable cache behavior. if unforeseen issues arise, it will be changed.
		
		// put player name in index
//		playerNameIndex.put(playerName, playerState.getPlayerUUID());
	}


	/**
	 * Remove cached player state from memory by player UUID<br>
	 * remove playerName index mappings too
	 * @param playerUUID UUID of player record to remove from cache
	 */
	final void flush(final UUID playerUUID) {
		
		// get playerName
//		final String playerName = playerStateMap.get(playerUUID).getName();
		
		// remove entry from cache map
		playerStateMap.remove(playerUUID);
		
		// remove entry from name index map
//		if (playerName != null) {
//			playerNameIndex.remove(playerName);
//		}
	}

	
//	/**
//	 * Remove player state from cache by player name<br>
//	 * remove playerName index mappings too
//	 * @param playerName player name to remove from cache
//	 */
//	final void flush(final String playerName) {
//
//		// get playerUUID
//		final UUID playerUUID = playerNameIndex.get(playerName);
//
//		// remove entry from cache map
//		playerStateMap.remove(playerUUID);
//
//		// remove entry from name index map
//		playerNameIndex.remove(playerName);
//	}

	
	/**
	 * Get number of cache map entries
	 * @return integer number of entries in cache
	 */
	final int getSize() {
		return playerStateMap.keySet().size();
	}
	

//	/**
//	 * Get cache map keys
//	 * @return
//	 */
//	final Set<UUID> getCacheMapKeys() {
//		return Collections.unmodifiableSet(playerStateMap.keySet());
//	}

	
//	/**
//	 * Get name index keys
//	 * @return
//	 */
//	final Set<String> getPlayerNameIndexKeys() {
//		return Collections.unmodifiableSet(playerNameIndex.keySet());
//	}
	
}
