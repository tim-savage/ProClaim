package com.winterhaven_mc.proclaim.listeners;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.objects.PermissionLevel;
import com.winterhaven_mc.proclaim.storage.Claim;
import com.winterhaven_mc.proclaim.storage.PlayerState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.material.Attachable;
import org.bukkit.material.MaterialData;

import java.util.List;
import java.util.UUID;

public final class BlockEventListener implements Listener {
	
	// reference to main class
	private final PluginMain plugin;
	

	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	public BlockEventListener(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// register events in this class
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	
	/**
	 * Block break event handler
	 * @param event the event being handled by this method
	 */
	@EventHandler(priority = EventPriority.LOW)
	public final void onBlockBreak(final BlockBreakEvent event) {
		
		// if event is already cancelled, skip all checks
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " is already cancelled, skipping all checks.");
			}
			return;
		}
		
		// get block location
		Location location = event.getBlock().getLocation();
		
		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(location.getWorld())) {
			return;
		}
		
		// get event player
		final Player player = event.getPlayer();
		
		// get player data
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());

		// get claim at location
		Claim claim = plugin.dataStore.getClaimAt(location);
		
		// get block broken
		final Block block = event.getBlock();
		
		// get block material data
		final MaterialData materialData = block.getState().getData();
		
		// if block is not within a claim...
		if (claim == null) {
			
	        if (materialData instanceof Attachable) {
				
	        	// get attached block face
	            BlockFace blockFace = ((Attachable) materialData).getAttachedFace();

				// get attached block
				Block attachedBlock  = block.getRelative(blockFace);

				// get claim at attached block location
				claim = plugin.dataStore.getClaimAt(attachedBlock.getLocation());
				
				// if no claim at attached block location, do nothing and return
				if (claim == null) {
					return;
				}
			}
			
			//check if claim exists above location and extend lower boundary
			else {

				// get claim at player location, ignoring height
				Claim claimAbove = plugin.dataStore.getClaimAt(location, true);

				// if claim exists above location and is owned by player, extend claim lower boundary
				if (claimAbove != null && claimAbove.getOwnerUUID().equals(player.getUniqueId())) {

					// extend claim depth including parent and sibling or child claims
					claimAbove.extend(location.getBlockY());
				}
				return;
			}
		}
		
		// if claim is owned by player, do nothing and return
		if (claim.getOwnerUUID().equals(player.getUniqueId())) {
			return;
		}
		
		// if player is in admin mode, do nothing and return
		if (playerState.isAdminMode()) {
			return;
		}
		
		// if player has claim permission level that allows block breaking, do nothing and return
		if (claim.allows(player.getUniqueId(), PermissionLevel.BUILD)) {
			return;
		}
		
		// cancel event
		event.setCancelled(true);
		
		// send player message
		if (materialData instanceof Attachable) {
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_BREAK_HANGING_GENERIC",claim);
		}
		else {
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_BREAK_GENERIC",claim);
		}		
	}
	
	
	/**
	 * Block place event handler
	 * @param event the event being handled by this method
	 */
	@EventHandler(priority = EventPriority.LOW)
	public final void onBlockPlace(final BlockPlaceEvent event) {
		
		// if event is already cancelled, skip all checks
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " is already cancelled, skipping all checks.");
			}
			return;
		}

		// get block location
		final Location location = event.getBlock().getLocation();
		
		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(location.getWorld())) {
			return;
		}

		// get event player
		final Player player = event.getPlayer();
		
		// get player data
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
		
		// get claim at block location
		Claim claim = plugin.dataStore.getClaimAt(location);
		
		// get block placed
		final Block block = event.getBlock();
		
		// get block material data
		final MaterialData materialData = block.getState().getData();
		
		// if block is not within a claim,
		if (claim == null) {
			
	        if (materialData instanceof Attachable) {
				
	            BlockFace blockFace = ((Attachable) materialData).getAttachedFace();

				// get attachable block
				Block attachedBlock  = block.getRelative(blockFace);

				// get claim at attached block location
				claim = plugin.dataStore.getClaimAt(attachedBlock.getLocation());
				
				// if no claim at attached block location, do nothing and return
				if (claim == null) {
					if (plugin.debug) {
						plugin.getLogger().info("No claim detected at attached block location.");
					}
					return;
				}
			}
			
			//check if claim exists above location and extend lower boundary
			else {
				
				// get claim at player location, ignoring height
				Claim claimAbove = plugin.dataStore.getClaimAt(location, true);

				// if claim exists and is owned by player, extend claim lower boundary
				if (claimAbove != null && claimAbove.getOwnerUUID().equals(player.getUniqueId())) {

					// extend claim depth including parent and sibling or child claims
					claimAbove.extend(location.getBlockY());
				}
				return;
			}
		}
		
		// if claim is owned by player, do nothing and return
		if (claim.getOwnerUUID().equals(playerState.getPlayerUUID())) {
			return;
		}
		
		// if player is in admin mode, do nothing and return
		if (playerState.isAdminMode()) {
			return;
		}
		
		// if player has claim permission level that allows block placing, do nothing and return
		if (claim.allows(player.getUniqueId(), PermissionLevel.BUILD)) {
			return;
		}
		
		// cancel event and send player message
		event.setCancelled(true);
		
		if (materialData instanceof Attachable) {
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_PLACE_HANGING_GENERIC",claim);
		}
		else {
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_PLACE_GENERIC",claim);
		}
	}
	
	
	/**
	 * Check multiple block placement (such as beds) for claim permission for each block
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onBlockMultiPlace(final BlockMultiPlaceEvent event) {

		// if event is already cancelled, skip all checks
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " is already cancelled, skipping all checks.");
			}
			return;
		}
		
		final Player player = event.getPlayer();
		
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
		
		final List<BlockState> replacedBlocks = event.getReplacedBlockStates();

		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(player.getWorld())) {
			return;
		}
		
		// if player is in admin mode, do nothing and return
		if (playerState.isAdminMode()) {
			return;
		}
		
		// do checks for each block that would be placed
		for (BlockState block : replacedBlocks) {

			// get claim at block location
			final Claim claim = plugin.dataStore.getClaimAt(block.getLocation());

			// if block is not within a claim, continue to next block
			if (claim == null) {
				continue;
			}

			// if claim is owned by player, continue to next block
			if (claim.getOwnerUUID().equals(playerState.getPlayerUUID())) {
				continue;
			}

			// if player has claim permission level that allows block placing, continue to next block
			if (claim.allows(player.getUniqueId(), PermissionLevel.BUILD)) {
				return;
			}
			
			// if this point is reached, cancel event and send player message
			event.setCancelled(true);
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_PLACE_GENERIC",claim);
			break;
		}
	}


	/**
	 * Check for pistons pushing blocks into claims
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onBlockPistonExtend(final BlockPistonExtendEvent event) {
		
		// if event is already cancelled, skip all checks
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " is already cancelled, skipping all checks.");
			}
			return;
		}
		
		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(event.getBlock().getWorld())) {
			return;
		}
		
		final Block pistonBlock = event.getBlock();
		
		final List<Block> movedBlocks = event.getBlocks();
		
		/// get claim at piston location
		final Claim claim = plugin.dataStore.getClaimAt(pistonBlock.getLocation());
		
		// if a piston is not in a claim but is affecting a block in a claim; cancel event, break piston and return
		if (claim == null) {
			
			if (plugin.debug) {
				plugin.getLogger().info("Piston extend detected outside any claims.");
			}
			
			// test each block that is affected by the piston
			for (Block movedBlock : movedBlocks) {

				// new block location
				Location newLocation = movedBlock.getRelative(event.getDirection()).getLocation();
				
				// if no claim at moved block location and new block location, continue to next moved block
				if (plugin.dataStore.getClaimAt(movedBlock.getLocation()) == null
						&& plugin.dataStore.getClaimAt(newLocation) == null) {
					continue;
				}
				
				else {
					event.setCancelled(true);
					pistonBlock.breakNaturally();
					return;
				}
			}
		}
		
		// if piston is inside a claim and affecting blocks in a claim with other owner, break piston and return
		// TODO: consider checking permissions for inter-claim block pushing
		else {
			
			// get the claim owner uuid
			UUID ownerUUID = claim.getOwnerUUID();

			// test each block that is affected by the piston
			for (Block movedBlock : movedBlocks) {
				
				// original block location
				final Location origLocation = movedBlock.getLocation();
				
				// new block location
				final Location newLocation = movedBlock.getRelative(event.getDirection()).getLocation();

				// if old location is a claim with a different owner
				// or new location is a claim with a different owner
				// cancel event and break piston
				if ((plugin.dataStore.getClaimAt(origLocation) != null 
						&& !plugin.dataStore.getClaimAt(origLocation).getOwnerUUID().equals(ownerUUID))
						|| (plugin.dataStore.getClaimAt(newLocation) != null
						&& !plugin.dataStore.getClaimAt(newLocation).getOwnerUUID()
							.equals(ownerUUID))) {
					
					event.setCancelled(true);
					pistonBlock.breakNaturally();
				}
			}
		}
	}
	
	
	/**
	 * Piston retract event handler
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onBlockPistonRetract(final BlockPistonRetractEvent event) {
		
		// if event is already cancelled, skip all checks
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " is already cancelled, skipping all checks.");
			}
			return;
		}

		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(event.getBlock().getWorld())) {
			return;
		}

		final Block pistonBlock = event.getBlock();
		
		final List<Block> movedBlocks = event.getBlocks();
		
		/// get claim at piston location
		final Claim claim = plugin.dataStore.getClaimAt(pistonBlock.getLocation());
		
		// if a piston is not in a claim but is affecting a block in a claim; cancel event, break piston and return
		if (claim == null) {
			
			// test each block that is affected by the piston
			for (Block movedBlock : movedBlocks) {

				// if no claim at moved block location and new block location, continue to next moved block
				if (plugin.dataStore.getClaimAt(movedBlock.getLocation()) == null) {
					continue;
				}
				
				else {
					
					// cancel event
					event.setCancelled(true);
					
					// break piston
					pistonBlock.breakNaturally();
					return;
				}
			}
		}
		
		// if piston is inside a claim and affecting blocks in a claim with other owner, break piston and return
		// TODO: consider checking permissions for inter-claim block pushing
		else {
			
			// get the claim owner uuid
			final UUID ownerUUID = claim.getOwnerUUID();

			// test each block that is affected by the piston
			for (Block movedBlock : movedBlocks) {
				
				// original block location
				final Location origLocation = movedBlock.getLocation();
				
				// check if block location is in a claim with a different owner
				if ((plugin.dataStore.getClaimAt(origLocation) != null 
						&& !plugin.dataStore.getClaimAt(origLocation).getOwnerUUID().equals(ownerUUID))) {

					// cancel event
					event.setCancelled(true);
					
					// break piston
					pistonBlock.breakNaturally();
				}
			}
		}
	}
	

	/**
	 * Prevent enderman grief
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onEntityChangeBlock(final EntityChangeBlockEvent event) {
		
		// if event is already cancelled, skip all checks
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " is already cancelled, skipping all checks.");
			}
			return;
		}
		
		// get block location
		final Location location = event.getBlock().getLocation();
		
		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(location.getWorld())) {
			return;
		}

		//TODO: CONSIDER CONFIG OPTION FOR PREVENT OFF-CLAIM ENDERMAN GRIEF
		
		// get claim at block location
		final Claim claim = plugin.dataStore.getClaimAt(location);
		
		// if block is in a claim and entity is enderman, cancel event
		if (claim != null && event.getEntityType().equals(EntityType.ENDERMAN)) {
			event.setCancelled(true);
		}
		
	}
	
	
	/**
	 * Prevent block explosions from damaging claims, blocks above sea level
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onBlockExplode(final BlockExplodeEvent event) {

		// if event is already cancelled, skip all checks
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " is already cancelled, skipping all checks.");
			}
			return;
		}
		
		// get event location
		final Location location = event.getBlock().getLocation();
		
		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(location.getWorld())) {
			return;
		}

		// iterate through event block list and remove blocks that are within a claim
		for (int i = 0; i < event.blockList().size(); i++) {
			Block block = event.blockList().get(i);
			if (plugin.dataStore.getClaimAt(block.getLocation()) != null) {
				event.blockList().remove(i);
			}
		}
	}
	
	
	/**
	 * Prevent dispensers from operating across claim boundaries, unless both claims have same owner
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onBlockDispense(final BlockDispenseEvent event) {
		
		// if event is already cancelled, skip all checks
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " is already cancelled, skipping all checks.");
			}
			return;
		}
		
		// get block location
		final Location location = event.getBlock().getLocation();
		
		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(location.getWorld())) {
			return;
		}
		
		// get claim at dispensed location
		final Claim dispensedClaim = plugin.dataStore.getClaimAt(location.clone().add(event.getVelocity()));
		
		// get claim at dispenser location; send dispensed claim as hint
		final Claim dispenserClaim = plugin.dataStore.getClaimAt(location);

		// if dispensedLocation is not in a claim, do nothing and return
		if (dispensedClaim == null) {
			return;
		}
		
		// if dispensedLocation is in a claim with same owner as dispenser claim, do nothing and return
		if (dispenserClaim != null && dispenserClaim.getOwnerUUID().equals(dispensedClaim.getOwnerUUID())) {
			return;
		}
		
		//TODO: CONSIDER CHECKING PERMISSIONS FOR INTER-CLAIM DISPENSING
		
		// cancel event
		event.setCancelled(true);
		
	}
	
	
	/**
	 * Prevent blocks in claims from being destroyed by burning
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onBlockBurn(final BlockBurnEvent event) {
		
		// if event is already cancelled, skip all checks
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " is already cancelled, skipping all checks.");
			}
			return;
		}
		
		final Location location = event.getBlock().getLocation();
		
		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(location.getWorld())) {
			return;
		}
		
		// get claim at block location
		final Claim claim = plugin.dataStore.getClaimAt(location);
		
		// if no claim at location, do nothing and return
		if (claim == null) {
			return;
		}

		// extinguish fire on adjacent blocks to prevent eternal flame, except netherrack
		for (BlockFace blockFace : BlockFace.values()) {
			final Block adjacentBlock = event.getBlock().getRelative(blockFace);
			if (adjacentBlock.getType().equals(Material.FIRE)
					&& adjacentBlock.getRelative(BlockFace.DOWN).getType() != Material.NETHERRACK) {
				adjacentBlock.breakNaturally();
			}
		}
		
		// cancel event
		event.setCancelled(true);
	}

	
	/**
	 * Prevent blocks in claims from being ignited
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onBlockIgnite(final BlockIgniteEvent event) {
		
		// if event is already cancelled, skip all checks
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " is already cancelled, skipping all checks.");
			}
			return;
		}
		
		final Location location = event.getBlock().getLocation();
		
		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(location.getWorld())) {
			return;
		}
		
		// get claim at block location
		final Claim claim = plugin.dataStore.getClaimAt(location);
		
		// if no claim at location, do nothing and return
		if (claim == null) {
			return;
		}
		
		final Player player = event.getPlayer();

		// if igniter is claim owner, do nothing and return
		if (player != null && claim.getOwnerUUID().equals(player.getUniqueId())) {
			return;
		}
		
		// if player has claim build permission, do nothing and return
//		if (plugin.permissionManager.checkPermission(claim.getKey(), player.getUniqueId(), PermissionLevel.BUILD)) {
//			return;
//		}
		
		// if event player is not null, send message
		if (player != null) {
			plugin.messageManager.sendPlayerMessage(event.getPlayer(), "ACTION_DENY_IGNITE_BLOCK", claim);
		}
		
		// cancel event
		event.setCancelled(true);
	}


	/**
	 * Prevent water and lava from flowing onto claims
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onBlockFromTo(final BlockFromToEvent event) {

		// if event is already cancelled, skip all checks
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " is already cancelled, skipping all checks.");
			}
			return;
		}

		// if flow is straight down, do nothing and return
		if(event.getFace() == BlockFace.DOWN) {
			return;
		}

		// get toBlock location
		final Location location = event.getToBlock().getLocation();

		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(location.getWorld())) {
			return;
		}

		// get claim at toBlock location
		final Claim toClaim = plugin.dataStore.getClaimAt(location);

		// if no claim at location, do nothing and return
		if (toClaim == null) {
			return;
		}

		// get claim at fromBlock location
		final Claim fromClaim = plugin.dataStore.getClaimAt(event.getBlock().getLocation());
		
		// if fromClaim has same owner as toClaim, do nothing and return
		if (fromClaim != null && fromClaim.getOwnerUUID().equals(toClaim.getOwnerUUID())) {
			return;
		}
		
		// cancel event
		event.setCancelled(true);	
	}
	
}
