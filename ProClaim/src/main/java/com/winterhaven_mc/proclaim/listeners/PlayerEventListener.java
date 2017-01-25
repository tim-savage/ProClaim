package com.winterhaven_mc.proclaim.listeners;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.highlights.HighlightStyle;
import com.winterhaven_mc.proclaim.highlights.Visualization;
import com.winterhaven_mc.proclaim.objects.ClaimTool;
import com.winterhaven_mc.proclaim.objects.PermissionLevel;
import com.winterhaven_mc.proclaim.storage.Claim;
import com.winterhaven_mc.proclaim.storage.ClaimGroup;
import com.winterhaven_mc.proclaim.storage.PlayerState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;


public final class PlayerEventListener implements Listener {

	// reference to main class
	private final PluginMain plugin;
	
	
	/**
	 * Class constructor for PlayerEventListener
	 * @param plugin reference to main class
	 */
	public PlayerEventListener(final PluginMain plugin) {
		
		// reference to main
		this.plugin = plugin;
		
		// register events in this class
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}


	@EventHandler
	public final void onPlayerLogin (final PlayerLoginEvent event) {

		// get event player object
		final Player player = event.getPlayer();
		
		// get player state
		PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
		
		// if the player has not been seen before
		if (playerState == null) {
			
			// create new player state object from player
			playerState = new PlayerState(player);
			
			// insert new player record in the datastore
			playerState.insert();
		}
		
		// if player has been seen before
		else {
			
			// update last login
			playerState.setLastLogin();
			
			// if name has changed since last login, set new name
			if (!player.getName().equalsIgnoreCase(playerState.getName())) {
				playerState.setName(player.getName());
			}
			
			// update player record in the datastore
			playerState.update();
		}
		
		// now do what we do when any player, new or old, logs in
		
		// resolve name conflicts
		playerState.resolveNameConflicts();
		
		// set player last afk location
		playerState.setLastAfkCheckLocation(player.getLocation());
		
		// start player earned blocks task
		plugin.taskManager.startPlayerEarnedBlocksTask(player);
		
	}

	
	/**
	 * General cleanup tasks when a player quits<br>
	 * - reset player tool mode to basic<br>
	 * - update player record in datastore
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onPlayerQuit(final PlayerQuitEvent event) {
		
		// fetch player state and update to ensure any cached player state is permanently stored
		// I'm not sure this is needed at this point. What gets updated that isn't already saved?
		final PlayerState playerState = PlayerState.getPlayerState(event.getPlayer().getUniqueId());
		
		// reset last tool location, so it isn't still set on next player login
		playerState.setLastToolLocation(null);
		
		// reset player tool mode to basic
		playerState.setCurrentToolMode(ClaimTool.BASIC);
		
		// reset player admin mode
		playerState.setAdminMode(false);
		
		// reset tool type in inventory to basic tool
		ClaimTool.changeInventoryTool(event.getPlayer(), ClaimTool.BASIC);
		
		// update player record in datastore
		playerState.update();
		
		// cancel all player tasks
		plugin.taskManager.cancelPlayerTasks(event.getPlayer());
		
	}
	
	
	/**
	 * Handle player events that could not be handled elsewhere<br>
	 * - claim tool use<br>
	 * - most claim permission checks
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onPlayerInteract(final PlayerInteractEvent event) {
		
		// get player
		final Player player = event.getPlayer();
		
		// get player state
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
		
		// get clicked block
		Block clickedBlock = event.getClickedBlock();
		
		// if event is air/block click with proClaim tool, begin tool use procedure
		if (ClaimTool.isTool(event.getItem()) && !event.getAction().equals(Action.PHYSICAL)) {

			// if world is not enabled, send message and return
			if (!plugin.worldManager.isEnabled(player.getWorld())) {
				plugin.messageManager.sendPlayerMessage(event.getPlayer(), "TOOL_FAIL_WORLD_DISABLED");
				return;
			}
			
			// if clicked block is tool transparent material, try to find non-air block along line of sight
			if (clickedBlock == null || ClaimTool.toolTransparentMaterials.contains(clickedBlock.getType())) {

				// RH says this can sometimes throw an exception, so using try..catch block
				try {
					clickedBlock = player.getTargetBlock(ClaimTool.toolTransparentMaterials, 250);
				} catch (Exception e) {
					plugin.getLogger().info("player.getTargetBlock() threw an exception.");
					plugin.getLogger().info(e.getLocalizedMessage());
				}
			}

			// if no block detected, stop here
			if (clickedBlock == null) {
				return;
			}

			// if clicked block is air, the actual clicked block was too far away
			if (clickedBlock.getType().equals(Material.AIR)) {
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_DISTANCE_EXCEEDED");
				return;
			}
			
			// cancel event so tool does not break anything
			event.setCancelled(true);
			
			// if tool is in off-hand, do nothing and return
			if (event.getHand().equals(EquipmentSlot.OFF_HAND)) {
				return;
			}

			// get claimTool from inventory item used
			ClaimTool claimTool = ClaimTool.getType(event.getItem());
			
			// get event action to test for left or right click
			Action action = event.getAction();

			// if left click, inspect claim
			if (action.equals(Action.LEFT_CLICK_BLOCK) || action.equals(Action.LEFT_CLICK_AIR)) {
				claimTool.inspect(player, clickedBlock);
				return;
			}
			else if (action.equals(Action.RIGHT_CLICK_BLOCK) || action.equals(Action.RIGHT_CLICK_AIR)) {
				
				// if player does not have WorldGuard build permission for the clicked block, send message and return
				if (!plugin.worldGuardHelper.canBuild(player, clickedBlock)) {
					
					// send player message
					plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_WORLDGUARD");
				}

				// get claim at clicked block location, if any (ignore height)
				Claim claim = plugin.dataStore.getClaimAt(clickedBlock.getLocation(), true);

				// if first click and claim corner and item in hand is not delete tool, set claim tool to resize tool
				if (playerState.getLastToolLocation() == null 
						&& claim != null
						&& claim.isCorner(clickedBlock.getLocation())
						&& !ClaimTool.getType(player.getInventory().getItemInMainHand()).equals(ClaimTool.DELETE)) {
					
					// set claim tool to resize tool
					claimTool = ClaimTool.RESIZE;
				}
				
				// change player inventory tool type to match selected claim tool
				claimTool.changeInventoryTool(player);
				
			}
			
			// use selected tool tool
			claimTool.useTool(player,clickedBlock);
			return;		
		}
		
		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(player.getWorld())) {
			return;
		}

		// left-clicking should be handled elsewhere, so do nothing and return
		if (event.getAction().equals(Action.LEFT_CLICK_AIR) || event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
			return;
		}
		
		// if clicked block is null, do nothing and return
		if (clickedBlock == null) {
			return;
		}
		
		// get block material
		final Material blockType = clickedBlock.getType();
		
		// if block is a sign, do sign interact method
		if (blockType.equals(Material.SIGN_POST) || blockType.equals(Material.WALL_SIGN)) {
			onSignInteract(event);
			return;
		}

		// get claim at location
		Claim claim = plugin.dataStore.getClaimAt(clickedBlock.getLocation());
		
		// if no claim at location, do nothing and return
		// NOTE: any checks for interactions outside claims should go before this check
		if (claim == null) {
			return;
		}
		
		// if player is claim owner, do nothing and return
		if (claim.getOwnerUUID().equals(player.getUniqueId())) {
			return;
		}

		// if player is in admin mode, do nothing and return
		if (playerState.isAdminMode()) {
			return;
		}

		// Prevent placement of armor stands in a claim
		if (event.getItem() != null && event.getItem().getType().equals(Material.ARMOR_STAND)) {
			
			// if player has claim permissions that allow placing armor stands do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.BUILD)) {
				return;
			}
			
			// send player denied message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_BUILD_PERMISSION",claim);

			// cancel event and return
			event.setCancelled(true);
			return;
		}
		
		// prevent chest access in claims
		if (blockType.equals(Material.CHEST)) {
			
			// if player has claim permission that allows chest access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.CONTAINER)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_CHEST", claim);
			
			// cancel event
			event.setCancelled(true);
			return;
		}
		
		// check if clicked block is a workbench
		if (blockType.equals(Material.WORKBENCH)) {
			
			// if player has claim permission that allows workbench access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.CONTAINER)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_WORKBENCH", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}

		// check if clicked block is a dispenser
		if (blockType.equals(Material.FURNACE)) {
			
			// if player has claim permission that allows dispenser access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.CONTAINER)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_FURNACE", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}

		// check if clicked block is a dispenser
		if (blockType.equals(Material.DISPENSER)) {
			
			// if player has claim permission that allows dispenser access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.CONTAINER)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_DISPENSER", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}

		// check if clicked block is a hopper
		if (blockType.equals(Material.HOPPER)) {
			
			// if player has claim permission that allows hopper access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.CONTAINER)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_HOPPER", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}

		// check if clicked block is a dropper
		if (blockType.equals(Material.DROPPER)) {
			
			// if player has claim permission that allows dropper access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.CONTAINER)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_DROPPER", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}
		
		// check if clicked block is a brewing stand
		if (blockType.equals(Material.BREWING_STAND)) {
			
			// if player has claim permission that allows brewing stand access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.CONTAINER)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_BREWING_STAND", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}
		
		// check if clicked block is a cauldron
		if (blockType.equals(Material.CAULDRON)) {
			
			// if player has claim permission that allows cauldron access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.CONTAINER)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_CAULDRON", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}
		
		// check if clicked block is a anvil
		if (blockType.equals(Material.ANVIL)) {
			
			// if player has claim permission that allows anvil access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.CONTAINER)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_ANVIL", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}
		
		// check if clicked block is a beacon
		if (blockType.equals(Material.BEACON)) {
			
			// if player has claim permission that allows beacon access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.CONTAINER)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_BEACON", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}
		
		// check if clicked block is a jukebox
		if (blockType.equals(Material.JUKEBOX)) {
			
			// if player has claim permission that allows jukebox access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.ACCESS)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_JUKEBOX", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}
		
		// check if clicked block is a cake
		if (blockType.equals(Material.CAKE_BLOCK)) {
			
			// if player has claim permission that allows cake access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.ACCESS)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_CAKE_BLOCK", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}
		
		// check if clicked block is a button
		if (blockType.equals(Material.WOOD_BUTTON) || blockType.equals(Material.STONE_BUTTON)) {
			
			// if player has claim permission that allows button access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.ACCESS)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_BUTTON", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}
		
		// check if clicked block is a lever
		if (blockType.equals(Material.LEVER)) {
			
			// if player has claim permission that allows lever access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.ACCESS)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_LEVER", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}

		// check if clicked block is a note block
		if (blockType.equals(Material.NOTE_BLOCK)) {
			
			// if player has claim permission that allows note block access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.ACCESS)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_NOTE_BLOCK", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}

		// check if clicked block is a diode block
		if (blockType.equals(Material.DIODE_BLOCK_ON) || blockType.equals(Material.DIODE_BLOCK_OFF)) {
			
			// if player has claim permission that allows diode access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.ACCESS)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_DIODE_BLOCK", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}

		// check if clicked block is a comparator block
		if (blockType.equals(Material.REDSTONE_COMPARATOR) 
				|| blockType.equals(Material.REDSTONE_COMPARATOR_ON)
				|| blockType.equals(Material.REDSTONE_COMPARATOR_OFF)) {
			
			// if player has claim permission that allows diode access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.ACCESS)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_COMPARATOR_BLOCK", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}

		// check if clicked block is a daylight detector
		if (blockType.equals(Material.DAYLIGHT_DETECTOR)
				|| blockType.equals(Material.DAYLIGHT_DETECTOR_INVERTED)) {
			
			// if player has claim permission that allows note block access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.ACCESS)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_DAYLIGHT_DETECTOR_BLOCK", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}

		// check if clicked block is a wooden door
		if (blockType.equals(Material.WOODEN_DOOR) 
				|| blockType.equals(Material.ACACIA_DOOR)
				|| blockType.equals(Material.BIRCH_DOOR)
				|| blockType.equals(Material.DARK_OAK_DOOR)
				|| blockType.equals(Material.JUNGLE_DOOR)
				|| blockType.equals(Material.SPRUCE_DOOR)) {
			
			// if player has claim permission that allows wooden door access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.ACCESS)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_WOODEN_DOOR", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}
		
		// check if clicked block is a wooden gate
		if (blockType.equals(Material.FENCE_GATE) 
				|| blockType.equals(Material.ACACIA_FENCE_GATE)
				|| blockType.equals(Material.BIRCH_FENCE_GATE)
				|| blockType.equals(Material.DARK_OAK_FENCE_GATE)
				|| blockType.equals(Material.JUNGLE_FENCE_GATE)
				|| blockType.equals(Material.SPRUCE_FENCE_GATE)) {
			
			// if player has claim permission that allows fence gate access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.ACCESS)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_FENCE_GATE", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}
		
		// check if clicked block is a wooden trap door
		if (blockType.equals(Material.TRAP_DOOR)) {
			
			// if player has claim permission that allows wooden trap door access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.ACCESS)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_TRAP_DOOR", claim);
			
			// cancel event and return
			event.setCancelled(true);
			return;
		}
		
		// check if interacted block is a plate
		if (blockType.equals(Material.WOOD_PLATE)
				|| blockType.equals(Material.STONE_PLATE)
				|| blockType.equals(Material.IRON_PLATE)
				|| blockType.equals(Material.GOLD_PLATE)) {

			// if player has claim permission that allows activator plate access, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.ACCESS)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ACCESS_PLATE", claim);
			
			// cancel event
			event.setCancelled(true);
		}
		
	}

	
	@EventHandler
	public final void PlayerInteractEntity(final PlayerInteractEntityEvent event) {
		
		// if event is already cancelled, skip all checks
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " is already cancelled, skipping all checks.");
			}
			return;
		}
		
		// send debug message detailing the entity clicked
		if (plugin.debug) {
			plugin.getLogger().info(event.getEventName() + " detected. " 
					+ "Entity right-clicked: " + event.getRightClicked().getType().toString());
		}

		// allow interaction with NPCs
		// if right clicked entity is type player, do nothing and return
		if (event.getRightClicked().getType().equals(EntityType.PLAYER)) {
			return;
		}
		
		// get entity location
		Location location = event.getRightClicked().getLocation();
		
		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(location.getWorld())) {
			return;
		}

		// get claim at entity location
		final Claim claim = plugin.dataStore.getClaimAt(location);
		
		// if no claim at entity location, do nothing and return
		if (claim == null) {
			return;
		}
		
		// get player
		final Player player = event.getPlayer();
		
		// if player is claim owner, do nothing and return
		if (claim.getOwnerUUID().equals(player.getUniqueId())) {
			return;
		}
		
		// get player state
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());

		// if player is in admin mode, do nothing and return
		if (playerState.isAdminMode()) {
			return;
		}
		
		// if player has claim permission for event, do nothing and return
		//TODO: PERMISSIONS NOT IMPLEMENTED YET
		
		// players can attach leashes to animals they own on other player's claims		
		// get entity
		Entity entity = event.getRightClicked();

		// if entity is owned by player, do nothing and return
		if (entity instanceof Tameable 
				&& ((Tameable) entity).getOwner() != null 
				&& ((Tameable) entity).getOwner().getUniqueId().equals(player.getUniqueId())) {
			return;
		}
		
		// players can untie leash hitches if leash is attached to an animal they own on other player's claims
		// check if entity is a leash hitch
		if (entity.getType().equals(EntityType.LEASH_HITCH)) {
			
			// get all entities within leash distance of hitch (10 blocks)
			final List<Entity> nearbyEntities = entity.getNearbyEntities(10, 10, 10);
			
			// iterate through all nearby entities
			for (Entity nearbyEntity : nearbyEntities) {

				// if entity is not a living entity, skip
				if (!(nearbyEntity instanceof LivingEntity)) {
					continue;
				}

				final LivingEntity livingEntity = (LivingEntity)nearbyEntity;

				// if entity is not leashed, skip
				if (!(livingEntity.isLeashed())) {
					continue;
				}

				// if entity is not leashed to the leash hitch being broken, skip
				if (!livingEntity.getLeashHolder().equals(entity)) {
					continue;
				}

				// if entity is not tameable, skip
				if (!(livingEntity instanceof Tameable)) {
					continue;
				}

				final Tameable tameableEntity = (Tameable)livingEntity;
							
				// if entity is owned by player, do nothing and return
				if (tameableEntity.getOwner() != null && tameableEntity.getOwner().equals(player)) {
					return;
				}
				
				// if player has container access on claim, allow breaking hitch
				if (claim.allows(player.getUniqueId(), PermissionLevel.CONTAINER)) {
					return;
				}
			}
		}
		
		// send player message
		if (entity.getType().equals(EntityType.ITEM_FRAME)) {
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_INTERACT_ITEM_FRAME", claim);			
		}
		else if (entity.getType().equals(EntityType.PAINTING)) {
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_INTERACT_PAINTING", claim);			
		}
		else if (entity.getType().equals(EntityType.HORSE)) {
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_INTERACT_HORSE", claim);			
		}
		else if (entity.getType().equals(EntityType.LEASH_HITCH)) {
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_INTERACT_LEASH", claim);			
		}
		else {
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_INTERACT_ENTITY", claim);
		}
		
		// cancel event
		event.setCancelled(true);
	}
	
	
	/**
	 * Prevent interaction with armor stands and other entities in claims
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onPlayerInteractAtEntity(final PlayerInteractAtEntityEvent event) {
	
		// if event is already cancelled, skip all checks
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " is already cancelled, skipping all checks.");
			}
			return;
		}

		final EntityType entityType = event.getRightClicked().getType();
		
		// only armor stand interactions are being handled by this event handler at this time
		if (!event.getRightClicked().getType().equals(EntityType.ARMOR_STAND)) {
			return;
		}
		
		// get event player
		final Player player = event.getPlayer();
		
		// get player state for event player
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
		
		// get location
		final Location location = event.getRightClicked().getLocation();
		
		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(location.getWorld())) {
			return;
		}
	
		// get claim at entity location
		final Claim claim = plugin.dataStore.getClaimAt(location);
		
		// if no claim at entity location, do nothing and return
		if (claim == null) {
			return;
		}
		
		// if player is claim owner, do nothing and return
		if (claim.getOwnerUUID().equals(player.getUniqueId())) {
			return;
		}
	
		// if player is in admin mode, do nothing and return
		if (playerState.isAdminMode()) {
			return;
		}
		
		// if player has claim permission for event, do nothing and return
		//TODO: PERMISSIONS NOT IMPLEMENTED YET
		
		// send player message
		if (entityType != null && event.getRightClicked().getType().equals(EntityType.ARMOR_STAND)) {
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_INTERACT_ARMOR_STAND", claim);			
		}
		
		// cancel event
		event.setCancelled(true);
	}


	/**
	 * Reset tool and clear visualizations when switching away from claim tool
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onPlayerItemHeld(final PlayerItemHeldEvent event) {
		
		final Player player = event.getPlayer();
		final ItemStack previousItem = player.getInventory().getItem(event.getPreviousSlot());
		final ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
		
		// if either item is null, do nothing and return
		if (previousItem == null || newItem == null) {
			return;
		}

		// check if previous held item is a claim tool and current held item is not
		if (ClaimTool.isTool(previousItem) && !ClaimTool.isTool(newItem)) {
			
			// get previous held item claim tool type
			ClaimTool claimTool = ClaimTool.getType(previousItem);
			
			// execute onSwitchFrom method for claim tool
			claimTool.onUnequip(player);
		}
	}

	
	/**
	 * Destroy tool on drop if configured
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onPlayerDropItem(final PlayerDropItemEvent event) {
		
		final Player player = event.getPlayer();
		final Item dropItem = event.getItemDrop();
		
		if (ClaimTool.isTool(dropItem.getItemStack())) {
			if (plugin.getConfig().getBoolean("destroy-on-drop")) {
				dropItem.remove();
				
				// remove dropped item
				event.getItemDrop().remove();
				
				// play item_break sound to player if sound effects enabled in config
				plugin.soundManager.playerSound(player, "ITEM_DROP_BREAK");
			}
			else {
				// convert dropped tool to basic tool
				dropItem.setItemStack(ClaimTool.create(ClaimTool.BASIC));
				
				// set player tool mode to basic
				PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
				playerState.setAdminMode(false);
				playerState.setCurrentToolMode(ClaimTool.BASIC);
			}
		}
		
	}
	
	
	/**
	 * Prevent player from picking up claim tool if one-item-inventory is configured true
	 * and player already has claim tool in inventory
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onPlayerPickupItem(final PlayerPickupItemEvent event) {
		
		final Player player = event.getPlayer();
		final Item pickupItem = event.getItem();
		
		// check if item is a claim tool
		if (ClaimTool.isTool(pickupItem.getItemStack())) {
			
			// check if one-tool-inventory is configured
			if (plugin.getConfig().getBoolean("one-tool-inventory")) {

				// check if player inventory already contains a claim tool
				if (ClaimTool.getInventoryTool(player) != null) {

					// cancel event
					event.setCancelled(true);
					
					// send player message
					plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_PICKUP_TOOL");
				}
			}
		}
	}
	
	
	/**
	 * Prevent emptying buckets on claims where player does not have permission
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onPlayerBucketEmpty(final PlayerBucketEmptyEvent event) {
		
		// if event is already cancelled, skip all checks
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " is already cancelled, skipping all checks.");
			}
			return;
		}
		
		// get location of block clicked
		final Location location = event.getBlockClicked().getLocation();
		
		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(location.getWorld())) {
			return;
		}
		
		// get claim at location of block clicked
		final Claim claim = plugin.dataStore.getClaimAt(location);
		
		// if no claim at location, do nothing and return
		if (claim == null) {
			return;
		}
		
		// get player
		final Player player = event.getPlayer();
		
		// if bucketeer is claim owner, do nothing and return
		if (claim.getOwnerUUID().equals(player.getUniqueId())) {
			return;
		}
		
		// get player state
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
		
		// if player is in admin mode, do nothing and return
		if (playerState.isAdminMode()) {
			return;
		}
		
		// if bucketeer has permission to dump buckets in claim, do nothing and return
		if (claim.allows(player.getUniqueId(), PermissionLevel.BUILD)) {
			return;
		}
		
		// send player message
		plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_EMPTY_BUCKET", claim);
		
		// cancel event
		event.setCancelled(true);
	}
	
	
	/**
	 * Prevent filling buckets on claims where player does not have permission
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onPlayerBucketFill(final PlayerBucketFillEvent event) {
		
		// if event is already cancelled, skip all checks
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " is already cancelled, skipping all checks.");
			}
			return;
		}
		
		// get location of block clicked
		final Location location = event.getBlockClicked().getLocation();
		
		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(location.getWorld())) {
			return;
		}
		
		// get claim at location of block clicked
		final Claim claim = plugin.dataStore.getClaimAt(location);
		
		// if no claim at location, do nothing and return
		if (claim == null) {
			return;
		}
		
		// get player, player state
		final Player player = event.getPlayer();
		
		// if bucketeer is claim owner, do nothing and return
		if (claim.getOwnerUUID().equals(player.getUniqueId())) {
			return;
		}
		
		// get player state
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
		
		// if player is in admin mode, do nothing and return
		if (playerState.isAdminMode()) {
			return;
		}
		
		// if bucketeer has permission to fill buckets in claim, do nothing and return
		if (claim.allows(player.getUniqueId(), PermissionLevel.BUILD)) {
			return;
		}
		
		// send player message
		plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_FILL_BUCKET", claim);
		
		// cancel event
		event.setCancelled(true);
	}
	
	
	/**
	 * Prevent players from using beds in claims
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onPlayerBedEnter(final PlayerBedEnterEvent event) {

		// if event is already cancelled, skip all checks
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " is already cancelled, skipping all checks.");
			}
			return;
		}
		
		// get player
		final Player player = event.getPlayer();
		
		// get bed location
		final Location location = event.getBed().getLocation();
		
		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(location.getWorld())) {
			return;
		}

		// get claim at entity location
		final Claim claim = plugin.dataStore.getClaimAt(location);
		
		// if claim is null (no claim exists at location) do nothing and return
		if (claim == null) {
			return;
		}
		
		// if claim is owned by player, do nothing and return
		if (claim.getOwnerUUID().equals(player.getUniqueId())) {
			return;
		}
		
		// get player state
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
		
		// if player is in admin mode, do nothing and return
		if (playerState.isAdminMode()) {
			return;
		}
		
		// if player has claim permission that allows bed access, do nothing and return
		if (claim.allows(player.getUniqueId(), PermissionLevel.ACCESS)) {
			return;
		}
		
		// output bed denied message and cancel event
		plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_ENTER_BED");
		event.setCancelled(true);
	}
	

	
	@EventHandler
	public final void onSignChange(final SignChangeEvent event) {

		// get player
		final Player player = event.getPlayer();
		
		// if player world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(player.getWorld())) {
			return;
		}

		// if first line of sign is not our marker, do nothing and return
		if (!event.getLine(0).equalsIgnoreCase("[ProClaim]")) {
			return;
		}
		
		// if economy not registered, send message and return
		if (!plugin.economyManager.isEconomyRegistered()) {
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "SIGN_FAIL_SELL_NO_ECONOMY");
			return;
		}

		// check that player has permission to sell claims
		if(!player.hasPermission("proclaim.claims.sell")) {
			plugin.messageManager.sendPlayerMessage(player, "SIGN_FAIL_SELL_PERMISSION");
			event.setCancelled(true);
			return;
		}
		
		// get sign location
		final Location signLocation = event.getBlock().getLocation();
		
		// get claim at sign location
		final Claim claim = plugin.dataStore.getClaimAt(signLocation);
		
		// if no claim at sign location, send error message and return
		if (claim == null) {
			plugin.messageManager.sendPlayerMessage(player, "SIGN_FAIL_SELL_NO_CLAIM");
			event.setCancelled(true);
			return;
		}
		
		// get player state
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
		
		// if player is not claim owner or admin, send error message and return
		if (!playerState.isAdminMode() && !claim.getOwnerUUID().equals(playerState.getPlayerUUID())) {
			plugin.messageManager.sendPlayerMessage(player, "SIGN_FAIL_SELL_NOT_CLAIM_OWNER");
			event.setCancelled(true);
			return;
		}
		
		// check that player has entered a price on the second line
		if (event.getLine(1).isEmpty()) {
			plugin.messageManager.sendPlayerMessage(player, "SIGN_FAIL_SELL_NO_PRICE");
			event.setCancelled(true);
			return;
		} 

		// get price line
		final String priceString = event.getLine(1);
		double price;
		
		// check that the price is a valid integer
		try {
			price = Double.parseDouble(priceString);
		} catch (NumberFormatException e) {
			plugin.messageManager.sendPlayerMessage(player, "SIGN_FAIL_SELL_INVALID_PRICE");
			event.setCancelled(true);
			return;
		}

		// get owner name, or admin name if claim is admin claim
		String claimOwnerName;
		if (claim.isAdminClaim()) {
			claimOwnerName = plugin.messageManager.getAdminName();
		}
		else {
			PlayerState claimOwnerState = PlayerState.getPlayerState(claim.getOwnerUUID());
			claimOwnerName = claimOwnerState.getName();
		}
		
		// if claim is a parent claim, make for sale sign
		if (!claim.isSubClaim()) {

			final ClaimGroup claimGroup = ClaimGroup.getClaimGroup(claim.getGroupKey());

			event.setLine(0, "§2[FOR SALE]");
			event.setLine(1, plugin.economyManager.getEconomy().format(price));
			event.setLine(2, claimOwnerName);
			if (claimGroup != null) {
				event.setLine(3, claimGroup.getName());
			}
		}
		// claim is a subclaim
		else {
			event.setLine(0, "§2[FOR LEASE]");
			event.setLine(1, plugin.economyManager.getEconomy().format(price));
			event.setLine(2, claimOwnerName);
		}
	}
	
	
	/**
	 * Handle sign clicking event
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onSignInteract(final PlayerInteractEvent event) {
		
		// if not a right click on a sign, do nothing and return
		if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			return;
		}
		
		// get sign
		final Sign sign = (Sign) event.getClickedBlock().getState();

		// get sign tag
		final String signTag = sign.getLine(0);
		
		// if sign is not a ProClaim real estate sign, do nothing and return
		if (!(signTag.equals("§2[FOR SALE]") || signTag.equals("§2[FOR LEASE]"))) {
			return;
		}
					
		// cancel event to prevent placing item in hand
		event.setCancelled(true);
		
		// get player
		final Player player = event.getPlayer();
		
		// if economy not registered, send message and return
		if (!plugin.economyManager.isEconomyRegistered()) {
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "SIGN_FAIL_BUY_NO_ECONOMY");
			return;
		}
		
		// if player does not have permission to buy claims, send message and return
		if (!player.hasPermission("proclaim.claims.buy")) {
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "SIGN_FAIL_BUY_PERMISSION");
			return;
		}
		
		// get sign location
		final Location signLocation = event.getClickedBlock().getLocation(); 

		// get claim at sign location
		Claim claim = plugin.dataStore.getClaimAt(signLocation);

		// if no claim at sign location, send player message and return
		if (claim == null) {
			plugin.messageManager.sendPlayerMessage(player, "SIGN_FAIL_BUY_NO_CLAIM");
			return;
		}
		
		// if claim is subclaim, but sign says for sale, get the parent claim
		if (claim.isSubClaim() && sign.getLine(0).equals("§2[FOR SALE]")) {
			claim = plugin.dataStore.getClaim(claim.getParentKey());
		}
		
		// if claim is top level claim but sign says for lease, send message and return
		if (!claim.isSubClaim() && sign.getLine(0).equals("§2[FOR LEASE]")) {
			plugin.messageManager.sendPlayerMessage(player, "SIGN_FAIL_BUY_TOP_LEVEL_NO_LEASE", claim);
			return;
		}
		
		// get playerUUID for name on sign
		final String signName = sign.getLine(2);
		
		// get player state for claim owner
		final PlayerState claimOwner = PlayerState.getPlayerState(claim.getOwnerUUID());
		
		// if admin claim,
		if (claim.isAdminClaim()) {
			
			// check that sign has admin name
			if (!signName.equals(plugin.messageManager.getAdminName())) {
				
				// send player message and return
				plugin.messageManager.sendPlayerMessage(player, "SIGN_FAIL_BUY_OWNER_MISMATCH", claim);
				return;
			}
		}
		// if not admin claim,
		else {
			
			// check that sign has claimOwner name
			if (claimOwner == null || !claimOwner.getName().equals(signName)) {
				
				// send player message and return
				plugin.messageManager.sendPlayerMessage(player, "SIGN_FAIL_BUY_OWNER_MISMATCH", claim);
				return;
			}
		}
		
		// check if player is already the claim owner
		if (claim.getOwnerUUID().equals(player.getUniqueId())) {
			
			// send player message and return
			plugin.messageManager.sendPlayerMessage(player, "SIGN_FAIL_BUY_PLAYER_IS_OWNER");
			return;
		}
		
		// get claim price from sign, stripping non numeric characters
		final String priceLine = sign.getLine(1).replaceAll("[^0-9]", "");
		
		// try to parse price string to double
		double price;
		try {
			price = Double.parseDouble(priceLine);
		} catch (NumberFormatException e1) {
			// invalid price on sign, send player error message and return
			plugin.messageManager.sendPlayerMessage(player, "SIGN_FAIL_BUY_INVALID_PRICE", claim);
			return;
		}
		
		// if player does not have sufficient funds, send message and return
		if (!plugin.economyManager.getEconomy().has(player, price)) {
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "SIGN_FAIL_BUY_INSUFFICIENT_FUNDS", claim);
			return;
		}
		
		// get player state for purchaser
		final PlayerState buyerState = PlayerState.getPlayerState(player.getUniqueId());

		// get claim group from claim record
		final ClaimGroup claimGroup = ClaimGroup.getClaimGroup(claim.getGroupKey());
		
		// if purchaser has already reached claim group limit, send message and return
		if (claimGroup != null 
				&& ClaimGroup.getPlayerClaimGroupCount(claimGroup, buyerState.getPlayerUUID()) 
					>= claimGroup.getClaimLimit()) {
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "SIGN_FAIL_BUY_CLAIMGROUP_LIMIT_EXCEEDED", claim);
			return;
		}

		// get offline player for seller
		final OfflinePlayer seller = plugin.getServer().getOfflinePlayer(claim.getOwnerUUID());
		
		// if top level claim,
		if (!claim.isSubClaim()) {
			
			// remove funds from buyer account
			plugin.economyManager.getEconomy().withdrawPlayer(player, price);
			
			// if not admin claim, deposit funds in seller account
			if (!claim.isAdminClaim() && seller != null) {
				plugin.economyManager.getEconomy().depositPlayer(seller, price);
			}
			
			// transfer claim ownership
			claim.transfer(player.getUniqueId());
			
			// remove sign
			event.getClickedBlock().setType(Material.AIR);
			
			// send success message
			plugin.messageManager.sendPlayerMessage(player, "SIGN_SUCCESS_BUY", claim);
		}
		
		// else subclaim
		else {

			// remove funds from buyer account
			plugin.economyManager.getEconomy().withdrawPlayer(player, price);
			
			// if not admin claim, deposit funds in seller account
			if (!claim.isAdminClaim() && seller != null) {
				plugin.economyManager.getEconomy().depositPlayer(seller, price);
			}
			
			// remove all permissions from subclaim
			claim.removeAllPermissions();
			
			// giver buyer build, grant permissions for subclaim
			claim.setPermission(player.getUniqueId(), PermissionLevel.BUILD_GRANT);
			
			// remove sign
			sign.setType(Material.AIR);
			
			// send success message
			plugin.messageManager.sendPlayerMessage(player, "SIGN_SUCCESS_LEASE", claim);
		}
		
		// visualize top level claim boundary
		if (claim.isSubClaim()) {
			claim = plugin.dataStore.getClaim(claim.getParentKey());
		}
		final Visualization visualization = Visualization.fromClaim(claim,
				player.getEyeLocation().getBlockY(),
				HighlightStyle.BASIC_CLAIM, player.getLocation());
		Visualization.Apply(player, visualization);
	}

}
