package com.winterhaven_mc.proclaim.listeners;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.objects.PermissionLevel;
import com.winterhaven_mc.proclaim.storage.Claim;
import com.winterhaven_mc.proclaim.storage.PlayerState;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.List;


public final class EntityEventListener implements Listener {

	// reference to main class
	private final PluginMain plugin;

	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	public EntityEventListener(final PluginMain plugin) {

		// assign reference to main class
		this.plugin = plugin;

		// register events in this class
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	
	// prevent armor stand damage, probably animals here too
	@EventHandler
	public final void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {

		// if event is already cancelled, do nothing and return
		if (event.isCancelled()) {
			if (plugin.debug) {
				plugin.getLogger().info(event.getEventName() + " already cancelled. Skipping all checks."); 
			}
			return;
		}

		// get damaged entity
		final Entity entity = event.getEntity();
		
		// get damaged entity location
		final Location location = entity.getLocation();
		
		// if world is disabled, do nothing and return
		if (!plugin.worldManager.isEnabled(location.getWorld())) {
			return;
		}

		// get claim at entity location
		final Claim claim = plugin.dataStore.getClaimAt(location);

		// if entity damage did not occur in a claim, do nothing and return
		if (claim == null) {
			return;
		}

		// if entity is a monster, do nothing and return
		if (entity instanceof Monster) {
			return;
		}
		
		// if entity is the killer bunny, do nothing and return
		//TODO: FIX THIS
		if (entity instanceof Rabbit) {
			Rabbit rabbit = (Rabbit) entity;
			if (rabbit.getType().equals(Rabbit.Type.THE_KILLER_BUNNY)) {
				return;
			}
		}
		
		Player attacker = null;
		
		final Entity damager = event.getDamager();

		// if damage caused by a projectile, get source
		if (damager instanceof Projectile) {

			// get shooter
			ProjectileSource shooter = ((Projectile) damager).getShooter();

			// if shooter is a player, set attacker
			if (shooter instanceof Player) {
				attacker = (Player) shooter;
			}
		}
		else if (damager instanceof Player) {
			attacker = (Player) damager;
		}

		// if attacker is not null, it is a player
		if (attacker != null) {

			// if attacker is claim owner, do nothing and return
			if (claim.getOwnerUUID().equals(attacker.getUniqueId())) {
				return;
			}

			// get player data
			PlayerState playerState = PlayerState.getPlayerState(attacker.getUniqueId());

			// if attacker is in admin mode, do nothing and return
			if (playerState.isAdminMode()) {
				return;
			}

			// if attacker has claim permission that would allow entity damage, do nothing and return
			if (claim.allows(attacker.getUniqueId(), PermissionLevel.CONTAINER)) {
				return;
			}
			
			// send player message
			plugin.messageManager.sendPlayerMessage(attacker, "ACTION_DENY_DAMAGE_ENTITY",claim);
		}
		
		// cancel event
		event.setCancelled(true);
	}


	@EventHandler
	public final void onHangingPlace(final HangingPlaceEvent event) {
		
		// get location
		final Location location = event.getBlock().getLocation();
		
		// get player, player data
		final Player player = event.getPlayer();
		final PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
		
		// get claim at entity location
		final Claim claim = plugin.dataStore.getClaimAt(location);
		
		// if no claim at location, do nothing and return
		if (claim == null) {
			return;
		}
		
		// if player is in admin mode, do nothing and return
		if (playerState.isAdminMode()) {
			return;
		}
		
		// if player is claim owner, do nothing and return
		if (claim.getOwnerUUID().equals(player.getUniqueId())) {
			return;
		}
		
		// check if entity is a leash hitch
		if (event.getEntity().getType().equals(EntityType.LEASH_HITCH)) {
			
			// if player has claim permission that allows attaching leashes, do nothing and return
			if (claim.allows(player.getUniqueId(), PermissionLevel.ACCESS)) {
				return;
			}
		}
		
		// if player has claim permission that allows hanging objects, do nothing and return
		if (claim.allows(player.getUniqueId(), PermissionLevel.BUILD)) {
			return;
		}
		
		// cancel event
		event.setCancelled(true);
		
		// send player message
		if (event.getEntity().getType().equals(EntityType.LEASH_HITCH)) {
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_PLACE_LEASH_HITCH",claim);
		}
		else if (event.getEntity().getType().equals(EntityType.PAINTING)){
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_PLACE_PAINTING",claim);
		}
		else if (event.getEntity().getType().equals(EntityType.ITEM_FRAME)){
			plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_PLACE_ITEM_FRAME",claim);
		}
	}


	/**
	 * Prevent removal of hanging items in claims except by claim owner or players with permission
	 * This event is fired when a player LEFT-CLICKS a hanging entity (ItemFrame, LeashHitch, Painting)
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onHangingBreakByEntity(final HangingBreakByEntityEvent event) {
		
		// send debug message detailing the entity clicked
		if (plugin.debug) {
			plugin.getLogger().info(event.getEventName() + " detected. " 
					+ "Entity clicked: " + event.getEntity().getType().toString());
		}
		
		// get location
		final Location location = event.getEntity().getLocation();
		
		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(location.getWorld())) {
			return;
		}
		
		// get claim at location
		final Claim claim = plugin.dataStore.getClaimAt(location);
		
		// if no claim at location, do nothing and return
		if (claim == null) {
			return;
		}
		
		// get hanging item remover
		final Entity remover = event.getRemover();
		
		Player player;
		
		if (remover != null) {
			
			if (remover instanceof Player) {
				
				player = (Player) remover;
				PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
				
				// if player is in admin mode, do nothing and return
				if (playerState.isAdminMode()) {
					return;
				}
				
				// if player is claim owner, do nothing and return
				if (claim.getOwnerUUID().equals(player.getUniqueId())) {
					return;
				}
				
				// check if entity is a leash hitch
				if (event.getEntity().getType().equals(EntityType.LEASH_HITCH)) {
					
					// get all entities within leash distance of hitch (10 blocks)
					final List<Entity> nearbyEntities = event.getEntity().getNearbyEntities(10, 10, 10);
					
					// iterate through all nearby entities
					for (Entity entity : nearbyEntities) {

						// if entity is not a living entity, skip
						if (!(entity instanceof LivingEntity)) {
							continue;
						}

						final LivingEntity livingEntity = (LivingEntity)entity;

						// if entity is not leashed, skip
						if (!(livingEntity.isLeashed())) {
							continue;
						}

						// if entity is not leashed to the leash hitch being broken, skip
						if (!livingEntity.getLeashHolder().equals(event.getEntity())) {
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
				
				// if player has claim permission that allows breaking hanging items, do nothing and return
				if (claim.allows(player.getUniqueId(), PermissionLevel.BUILD)) {
					return;
				}
				
				// send player message
				if (event.getEntity().getType().equals(EntityType.LEASH_HITCH)) {
					plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_BREAK_LEASH_HITCH",claim);
				}
				else if (event.getEntity().getType().equals(EntityType.PAINTING)) {
					plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_BREAK_PAINTING",claim);
				}
				else if (event.getEntity().getType().equals(EntityType.ITEM_FRAME)) {
					plugin.messageManager.sendPlayerMessage(player, "ACTION_DENY_BREAK_ITEM_FRAME",claim);
				}
			}
		}
		
		// cancel event
		event.setCancelled(true);
	}


	/**
	 * Prevent entity explosions from damaging claims, blocks above sea level
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public final void onEntityExplode(final EntityExplodeEvent event) {

		// process this event even if already cancelled
		
		// get event location
		final Location location = event.getLocation();
		
		// if world is not enabled, do nothing and return
		if (!plugin.worldManager.isEnabled(location.getWorld())) {
			return;
		}
		
		// get list of exploded blocks
		final List<Block> explodedBlocks = new ArrayList<>(event.blockList());
		
		// iterate through all exploded blocks
		for (Block block : explodedBlocks) {

			// check if block is within a claim
			if (plugin.dataStore.getClaimAt(block.getLocation()) != null) {

				// remove block from event blockList
				event.blockList().remove(block);
			}
		}
	}

}
