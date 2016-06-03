package com.winterhaven_mc.proclaim.objects;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.highlights.HighlightStyle;
import com.winterhaven_mc.proclaim.highlights.Visualization;
import com.winterhaven_mc.proclaim.storage.Claim;
import com.winterhaven_mc.proclaim.storage.PlayerState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;


public enum ClaimTool {

	BASIC(Material.GOLD_SPADE) {

		@Override
		public final void onFirstClick(final Player player, final Block clickedBlock) {

			/* starting a new claim
			 * - check player permission create permission
			 * - check player claim limit
			 * - check for overlapping claims
			 * - check for overlapping worldguard regions
			 */

			// if the player doesn't have create claim permission, display message and return
			if (plugin.getConfig().getBoolean("require-permission-create")
					&& !player.hasPermission("proclaim.claims.create")) {
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_BASIC_PERMISSIONS");
				return;
			}

			// check for existing claims at clicked location, ignoring height
			Claim existingClaim = plugin.dataStore.getClaimAt(clickedBlock.getLocation(), true);
			
			// if existing claim at clicked location...
			if (existingClaim != null) {
				
				// visualize existing claim with error border
				Visualization visualization = Visualization.fromClaim(existingClaim,
						clickedBlock.getY(), HighlightStyle.ERROR_CLAIM, player.getLocation());
				Visualization.Apply(player, visualization);

				// send message to player
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_BASIC_OVERLAP");
				
				return;
			}
			
			// if player does not have WorldGuard build permission for the clicked block, send message and return
			if (!plugin.worldGuardHelper.canBuild(player, clickedBlock)) {
				
				// send player message
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_BASIC_WORLDGUARD");
				
				return;
			}
			
			// send create start success message
			plugin.messageManager.sendPlayerMessage(player,"TOOL_SUCCESS_BASIC_START");

			// visualize start claim location for player
			Claim visualizeThis = new Claim(clickedBlock.getLocation(),clickedBlock.getLocation());
			Visualization visualization = Visualization.fromClaim(visualizeThis, clickedBlock.getY(), HighlightStyle.START_CLAIM, player.getLocation());
			Visualization.Apply(player, visualization);

			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());

			// set last tool location
			playerState.setLastToolLocation(clickedBlock.getLocation());

			// start claim procedure is finished
		}

		@Override
		public final void onSecondClick(final Player player, final Block clickedBlock) {

			/* creating a new claim
			 * - check player has required create claim permission
			 * - check that last tool location is valid (not null, in same world)
			 * - check player claim limit
			 * - create claim object
			 * - check claim result object for overlapping claims
			 * - check for other fail modes
			 * - check for overlapping worldguard regions
			 * - check claim dimensions are valid
			 * - insert valid claim object in datastore
			 */

			// if the player doesn't have create claim permission, display message and return
			if (plugin.getConfig().getBoolean("require-permission-create")
					&& !player.hasPermission("proclaim.claims.create")) {
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_BASIC_PERMISSIONS");
				return;
			}

			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());

			// if last tool location is null, we shouldn't have gotten here; restart create claim workflow
			if (playerState.getLastToolLocation() == null) {
				if (plugin.debug) {
					plugin.getLogger().info("Last tool location is null at beginning of onSecondClick method.");
				}
				
				// do onFirstClick and return
				this.onFirstClick(player, clickedBlock);
				return;
			}

			// if last tool location was in a different world, restart create claim workflow
			if (!playerState.getLastToolLocation().getWorld().equals(clickedBlock.getWorld())) {

				if (plugin.debug) {
					plugin.getLogger().info("clicked locations are in different worlds. Starting create action over.");
				}
				
				// reset player's last tool location to null
				playerState.setLastToolLocation(null);
				
				// do onFirstClick and return
				this.onFirstClick(player, clickedBlock);
				return;
			}

			// if player is at the claim per player limit and doesn't have bypass permission, display an error message
			 if (plugin.getConfig().getInt("max-claims") > 0 
			 		&& !player.hasPermission("proclaim.override.claimcountlimit")
			 		&& plugin.dataStore.getPlayerClaims(player.getUniqueId()).size() 
			 			>= plugin.getConfig().getInt("max-claims")) {
			 	plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_BASIC_LIMIT");
			 	
			 	// reset last tool location
			 	playerState.setLastToolLocation(null);
			 	
			 	return;
			 }

			// try to create a new claim
			ClaimResult result = Claim.create(playerState.getLastToolLocation(),
					clickedBlock.getLocation(),
					playerState);

			Claim resultClaim = result.getResultClaim();

			// if result is successful, continue claim validity checks
			if (result.isSuccess()) {

				// test for overlapping WorldGuard regions that player does not have build permission in
				if (plugin.worldGuardHelper.overlaps(resultClaim,player)) {
					
					//send player message
					plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_BASIC_WORLDGUARD");
					
					// revert visualization
					Visualization.Revert(player);
					
					// reset last tool location
					playerState.setLastToolLocation(null);
					
					return;
				}
				
				// test for minimum width/length
				if (resultClaim.getWidth() < plugin.getConfig().getInt("min-width") 
						|| resultClaim.getLength() < plugin.getConfig().getInt("min-length")) {

					// this IF block is a workaround for craftbukkit bug which fires two events for one interaction
					// NOTE: I don't think that's a bug; it's a side effect of using right-click, which auto-repeats
					// we should add a right-click cooldown to work around that, I doubt this fix is sufficient
					if (resultClaim.getWidth() != 1 && resultClaim.getLength() != 1) {
						plugin.messageManager.sendPlayerMessage(player,"TOOL_FAIL_BASIC_NARROW");
						player.sendMessage("Claim dimensions: " 
								+ resultClaim.getWidth() + " x " 
								+ resultClaim.getLength() + " = "
								+ resultClaim.getArea());
					}
					
					// visualize the too narrow claim
					Visualization visualization = Visualization.fromClaim(result.getResultClaim(),
							clickedBlock.getY(),
							HighlightStyle.ERROR_CLAIM,
							player.getLocation());
					Visualization.Apply(player, visualization);
					
					// stay in finishCreateClaim mode (don't reset last tool location)
					return;
				}

				// test for minimum area
				if (resultClaim.getArea() < plugin.getConfig().getInt("min-area")) {

					// this IF block is a workaround for craftbukkit bug which fires two events for one interaction
					// NOTE: I don't think that's a bug; it's a side effect of using right-click, which auto-repeats.
					if (resultClaim.getArea() != 1) {
						plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_BASIC_TOO_SMALL");
						player.sendMessage("Claim dimensions: " 
								+ resultClaim.getWidth() + " x " 
								+ resultClaim.getLength() + " = "
								+ resultClaim.getArea());
					}
					
					// visualize the too small claim
					Visualization visualization = Visualization.fromClaim(result.getResultClaim(),
							clickedBlock.getY(),
							HighlightStyle.ERROR_CLAIM,
							player.getLocation());
					Visualization.Apply(player, visualization);

					// stay in finishCreateClaim mode (don't reset last tool location)
					return;
				}

				// test for sufficient player blocks
				// get player remaining blocks
				int remainingBlocks = playerState.getEarnedClaimBlocks();

				if (resultClaim.getArea() > remainingBlocks) {

					// send player message insufficient blocks
					plugin.messageManager.sendPlayerMessage(player,"TOOL_FAIL_BASIC_INSUFFICIENT_BLOCKS");

					// stay in finish create claim mode (don't reset last tool location)
					return;
				}
				
				// remove player's claim blocks equal to the area of the new claim
				playerState.removeClaimBlocks(resultClaim.getArea());
				
				// update player record
				playerState.update();
				
				// send success message
				plugin.messageManager.sendPlayerMessage(player,"TOOL_SUCCESS_BASIC_FINISH",resultClaim);

				// visualize new claim
				Visualization visualization = Visualization.fromClaim(result.getResultClaim(),
						clickedBlock.getY(),
						HighlightStyle.BASIC_CLAIM,
						player.getLocation());
				Visualization.Apply(player, visualization);

				// clear last tool location
				playerState.setLastToolLocation(null);

				// insert claim into datastore
				resultClaim.insert();
				
				// finished creating new basic claim
				return;
			}

			// anything below here is the result of an unsuccessful create claim

			if (!result.isSuccess()) {
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_BASIC_OVERLAP");

				// visualize overlapping claims if available
				if (!result.getOverlapClaims().isEmpty()) {
						Visualization visualization = Visualization.fromClaims(result.getOverlapClaims(),
							clickedBlock.getY(),
							HighlightStyle.ERROR_CLAIM,
							player.getLocation());
						Visualization.Apply(player, visualization);
				}
				
				// reset last tool location to force restart of create claim workflow
				playerState.setLastToolLocation(null);
			}
		}
		
		@Override
		public final void onEquip(final Player player) {
			
			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
			
			// set player admin mode to false
			playerState.setAdminMode(false);
			
			// set player tool mode to basic
			playerState.setCurrentToolMode(this);
			
			// change tool in player inventory to basic tool
			this.changeInventoryTool(player);
		}
		
		@Override
		public final void onUnequip(final Player player) {
			
			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
			
			// if player was creating a claim, send cancelled message and reset stuff
			if (playerState.getLastToolLocation() != null) {
				
				// send player message
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_BASIC_CLAIM_CANCELLED");
				
				// revert visualization
				Visualization.Revert(player);
				
				// reset last tool location
				playerState.setLastToolLocation(null);
				
				// reset working claim
				playerState.setWorkingClaim(null);
			}
		}

	},

	ADMIN(Material.DIAMOND_SPADE) {

		@Override
		public final void onFirstClick(final Player player, final Block clickedBlock) {
			
			// if the player doesn't have admin claim permission, display message and return
			if (!player.hasPermission("proclaim.claims.admin.create")) {
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_ADMIN_PERMISSION");
				return;
			}

			// check for existing claims at clicked location, ignoring height
			Claim existingClaim = plugin.dataStore.getClaimAt(clickedBlock.getLocation(), true);
			
			// if existing claim exists..
			if (existingClaim != null) {
				
				// if claim is subclaim, get parent claim instead
				if (existingClaim.isSubClaim()) {
					existingClaim = plugin.dataStore.getClaim(existingClaim.getParentKey());
				}
				
				// visualize existing claim with error border
				Visualization visualization = Visualization.fromClaim(existingClaim,
						clickedBlock.getY(), HighlightStyle.ERROR_CLAIM, player.getLocation());
				Visualization.Apply(player, visualization);

				// send player message
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_ADMIN_OVERLAP");
				return;
			}

			// if admin-overrides-worldguard is configured false, do WorldGuard permission check
			if (!plugin.getConfig().getBoolean("admin-overrides-worldguard")) {
				
				// if player does not have WorldGuard build permission for the clicked block, send message and return
				if (!plugin.worldGuardHelper.canBuild(player, clickedBlock)) {

					// send player message
					plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_BASIC_WORLDGUARD");
					return;
				}
			}
			
			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());

			// send create start message
			plugin.messageManager.sendPlayerMessage(player,"TOOL_SUCCESS_ADMIN_START");

			// visualize start claim location for player
			Claim visualizeThis = new Claim(clickedBlock.getLocation(),clickedBlock.getLocation());
			Visualization visualization = Visualization.fromClaim(visualizeThis, clickedBlock.getY(), HighlightStyle.START_CLAIM, player.getLocation());
			Visualization.Apply(player, visualization);

			// set last tool location
			playerState.setLastToolLocation(clickedBlock.getLocation());

			// start admin claim procedure is finished
		}

		@Override
		public final void onSecondClick(final Player player, final Block clickedBlock) {

			/* we are finishing a new admin claim
			 * - check player has admin create permission
			 * - check that last tool location is valid (not null, in same world)
			 * - create claim object
			 * - check claim result object for overlapping claims, other fail modes
			 * - check for overlapping worldguard regions
			 * - insert valid claim object in datastore
			 */

			// if the player doesn't have create admin claim permission, display message and return
			if (!player.hasPermission("proclaim.claims.admin.create")) {
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_ADMIN_PERMISSION");
				return;
			}

			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());

			// if last tool location is null, we shouldn't have gotten here; restart create admin claim workflow
			if (playerState.getLastToolLocation() == null) {
				if (plugin.debug) {
					plugin.getLogger().info("Last tool location is null at beginning of onSecondClick method.");
				}
				
				// do onFirstClick and return
				this.onFirstClick(player, clickedBlock);
				return;
			}

			// if last tool location was in a different world, assume the player is restarting the create claim workflow over
			if (!playerState.getLastToolLocation().getWorld().equals(clickedBlock.getWorld())) {

				// set player last tool location to clicked block and call startCreateClaim
				if (plugin.debug) {
					plugin.getLogger().info("clicked locations are in different worlds. Starting admin create action over.");
				}
				
				// reset player's last tool location to null
				playerState.setLastToolLocation(null);
				
				// do onFirstClick and return
				this.onFirstClick(player, clickedBlock);
				return;
			}

			// try to create a new claim
			ClaimResult result = Claim.create(playerState.getLastToolLocation(),
					clickedBlock.getLocation(),playerState);
			
			// get result claim
			Claim resultClaim = result.getResultClaim();

			// if result is successful, continue claim validity checks
			if (result.isSuccess()) {

				// if admin-overrides-worldguard is configured false, do WorldGuard permission check
				if (!plugin.getConfig().getBoolean("admin-overrides-worldguard")) {

					// test for overlapping WorldGuard regions that player does not have build permission in
					if (plugin.worldGuardHelper.overlaps(resultClaim,player)) {

						//send player message
						plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_BASIC_WORLDGUARD");

						// revert visualization
						Visualization.Revert(player);

						// reset last tool location
						playerState.setLastToolLocation(null);

						return;
					}
				}
				
				// send success message
				plugin.messageManager.sendPlayerMessage(player,"TOOL_SUCCESS_ADMIN_FINISH");

				// visualize new claim
				Visualization visualization = Visualization.fromClaim(result.getResultClaim(),
						clickedBlock.getY(),
						HighlightStyle.ADMIN_CLAIM,
						player.getLocation());
				Visualization.Apply(player, visualization);

				// clear last tool location
				playerState.setLastToolLocation(null);

				// set claim owner uuid to admin uuid
				resultClaim.setAdminClaim();
				
				// set resizeable false on new adminclaims
				resultClaim.setResizeable(false);
				
				// insert claim into datastore
				resultClaim.insert();
				
				// finished creating new admin claim
				return;
			}

			// anything below here is the result of an unsuccessful create admin claim

			if (!result.isSuccess()) {
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_ADMIN_OVERLAP");

				// visualize overlapping claim if available
				if (result.getResultClaim() != null) {
					Visualization visualization = Visualization.fromClaims(result.getOverlapClaims(),
							clickedBlock.getY(),
							HighlightStyle.ERROR_CLAIM,
							player.getLocation());
					Visualization.Apply(player, visualization);
				}
				
				// reset last tool location to force restart of create claim workflow
				playerState.setLastToolLocation(null);
			}
		}

		public final void onEquip(final Player player) {
			
			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());

			// set player admin mode to true
			playerState.setAdminMode(true);
			
			// set player tool mode to admin
			playerState.setCurrentToolMode(this);
			
			// change tool in player inventory to admin tool
			this.changeInventoryTool(player);

		}
		
		@Override
		public final void onUnequip(final Player player) {
			
			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
			
			// if player was creating an admin claim, send cancelled message and reset stuff
			if (playerState.getLastToolLocation() != null) {
				
				// send player admin create cancelled message
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_ADMIN_CLAIM_CANCELLED");
				
				// revert visualization
				Visualization.Revert(player);
				
				// reset last tool location
				playerState.setLastToolLocation(null);
				
				// reset working claim
				playerState.setWorkingClaim(null);
			}
		}

	},
	
	SUBCLAIM(Material.IRON_SPADE) {

		@Override
		public final void onFirstClick(final Player player, final Block clickedBlock) {

			/*
			 * Subclaim first click rules
			 * - player must have subclaim permission node if require-permission-create is configured true
			 * - clickedBlock must be inside a top level claim
			 * - player must be owner of top level claim, or admin
			 * - clickedBlock cannot overlap other subclaims
			 */
			
			// if the player doesn't have required create subclaim permission, display message and return
			if (plugin.getConfig().getBoolean("require-permission-create")
					&& !player.hasPermission("proclaim.claims.subclaim")) {
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_SUBCLAIM_PERMISSION");
				return;
			}

			// check for existing claims at clicked location, ignoring height
			Claim clickedClaim = plugin.dataStore.getClaimAt(clickedBlock.getLocation(), true);

			// if clicked location was outside an existing claim, send error message and stop
			if (clickedClaim == null) {
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_SUBCLAIM_NO_PARENT");
				// not resetting tool mode here, so player can stay in subclaim mode
				// last tool location should still be null (or we wouldn't be here)
				// player.workingClaim has not been set by this method, it should be null here
				return;
			}
			
			if (plugin.debug) {
				plugin.getLogger().info("Testing if clicked block is within a subclaim:");
			}
			
			//if the clicked claim is within a subclaim, send error message and stop
			if (clickedClaim.isSubClaim()) {
				
				if (plugin.debug) {
					plugin.getLogger().info("Clicked block is within a subclaim.");
				}
				
				// send player message
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_SUBCLAIM_OVERLAPS_SUBCLAIM");
				
				// get clickedClaim parent for visualization
				Claim clickedClaimParent = plugin.dataStore.getClaim(clickedClaim.getParentKey());
				
				// visualize clicked claim with error border
				Visualization visualization = Visualization.fromClaim(clickedClaimParent,
						clickedBlock.getY(), HighlightStyle.ERROR_CLAIM, player.getLocation());
				Visualization.Apply(player, visualization);
				return;
			}

			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
			
			// if player is not owner of clicked claim or admin, send error message and stop
			if (!playerState.isAdminMode() &&
					!clickedClaim.getOwnerUUID().equals(player.getUniqueId())) {
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_SUBCLAIM_PARENT_PERMISSION", clickedClaim);
				return;
			}
			
			// set player last tool location to clicked location
			playerState.setLastToolLocation(clickedBlock.getLocation());

			// set player working claim to parent claim
			playerState.setWorkingClaim(clickedClaim);

			// send player subclaim start success message
			plugin.messageManager.sendPlayerMessage(player, "TOOL_SUCCESS_SUBCLAIM_START");
			
			// create fake 1x1 block claim for visualization of start spot
			Claim startLocation = new Claim(clickedBlock.getLocation(),clickedBlock.getLocation());
			
			// visualize fake claim
			Visualization visualization = Visualization.fromClaim(
					startLocation,
					clickedBlock.getY(),
					HighlightStyle.SUB_CLAIM,
					player.getLocation());
			Visualization.Apply(player, visualization);
		}

		@Override
		public final void onSecondClick(final Player player, final Block clickedBlock) {

			/*
			 * Subclaim second-click rules:
			 * - clickedBlock and lastToolLocation must be in same world
			 * - top level claim must be owned by player
			 * - subclaim must be within boundaries of its parent claim
			 * - subclaim can not overlap any siblings
			 * - possible minimum size restrictions, not yet implemented
			 */
			
			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
			
			// if last tool location is null, we shouldn't have gotten here; restart create claim workflow
			if (playerState.getLastToolLocation() == null) {
				
				if (plugin.debug) {
					plugin.getLogger().info("Last tool location is null at " 
							+ "beginning of SUBCLAIM.onSecondClick() method. " 
							+ "Starting create subclaim workflow over.");
				}
				
				// do onFirstClick and return
				this.onFirstClick(player, clickedBlock);
				return;
			}

			// if last tool location was in a different world, restart create claim workflow
			if (!playerState.getLastToolLocation().getWorld().equals(clickedBlock.getWorld())) {

				if (plugin.debug) {
					plugin.getLogger().info("clicked locations are in different worlds. " 
							+ "Starting create subclaim workflow over.");
				}
				
				// reset player's last tool location to null
				playerState.setLastToolLocation(null);
				
				// do onFirstClick and return
				this.onFirstClick(player, clickedBlock);
				return;
			}

			// get parent claim
			Claim parentClaim = playerState.getWorkingClaim();
			
			// check for null parent claim
			if (parentClaim == null) {
				plugin.getLogger().warning("parent claim is null in SUBCLAIM.onSecondClick(). Aborting.");
				playerState.setLastToolLocation(null);
				return;
			}
			
			// if click location is not within parent claim, send message and stop
			if (!parentClaim.contains(clickedBlock.getLocation(), true)) {
				
				// send player message
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_SUBCLAIM_OUTSIDE_PARENT");

				// not resetting last tool location or working claim, let player try second click again				
				return;
			}
			
			// try to create a new subclaim
			ClaimResult result = Claim.createSubClaim(playerState.getLastToolLocation(),
					clickedBlock.getLocation(), parentClaim);

			Claim resultClaim = result.getResultClaim();

			// if result is successful, continue claim validity checks
			if (result.isSuccess()) {
				// send success message
				plugin.messageManager.sendPlayerMessage(player, "TOOL_SUCCESS_SUBCLAIM_FINISH");

				// visualize new claim
				Visualization visualization = Visualization.fromClaim(result.getResultClaim(),
						clickedBlock.getY(),
						HighlightStyle.SUB_CLAIM,
						player.getLocation());
				Visualization.Apply(player, visualization);

				// clear last tool location
				playerState.setLastToolLocation(null);
				
				// clear working claim
				playerState.setWorkingClaim(null);

				// set claim parent key to parent claim key
				resultClaim.setParentKey(parentClaim.getKey());

				// insert claim into datastore
				resultClaim.insert();

				// finished creating new subclaim
			}
			else {
			// anything below here is the result of an unsuccessful create subclaim

				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_SUBCLAIM_OVERLAP");

				// visualize overlapping claim if available
				if (result.getResultClaim() != null) {
					Visualization visualization = Visualization.fromClaims(result.getOverlapClaims(),
							clickedBlock.getY(),
							HighlightStyle.ERROR_CLAIM,
							player.getLocation());
					Visualization.Apply(player, visualization);
				}
				
				// reset last tool location to force restart of create claim workflow
				playerState.setLastToolLocation(null);
	
			}
		}
		
		@Override
		public final void onEquip(final Player player) {

			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
			
			// set player tool mode to subclaim
			playerState.setCurrentToolMode(this);

			// change tool in player inventory
			this.changeInventoryTool(player);
		}
		
		@Override
		public final void onUnequip(final Player player) {
			
			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
			
			// if player was creating a subclaim, send cancelled message and reset stuff
			if (playerState.getLastToolLocation() != null) {
				
				// send player message
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_SUBCLAIM_CANCELLED");
				
				// revert visualization
				Visualization.Revert(player);
				
				// reset last tool location
				playerState.setLastToolLocation(null);
			}
			
			// always reset working claim
			playerState.setWorkingClaim(null);
		}

	},
	
	RESIZE(Material.WOOD_SPADE) {

		@Override
		public final void onFirstClick(final Player player, final Block clickedBlock) {

			// get claim at clicked location
			Claim claim = plugin.dataStore.getClaimAt(clickedBlock.getLocation(), true);
			
			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
			
			// if claim is null, we shouldn't have gotten here. this check is for debugging.
			if (claim == null) {
				plugin.getLogger().warning("[ClaimTool.RESIZE] claim at clicked location is null.");
				return;
			}
			
			// if claim is admin claim and player does not have admin claim resize permission
			if (claim.isAdminClaim() && !player.hasPermission("proclaim.claims.admin.resize")) {
				
				// visualize error claim 
				Visualization visualization = Visualization.fromClaim(claim, clickedBlock.getY(),
						HighlightStyle.ERROR_CLAIM, player.getLocation());
				Visualization.Apply(player, visualization);
				
				// send player message
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_RESIZE_ADMIN_PERMISSION");
				
				// reset player last tool location (it should be null here already)
				playerState.setLastToolLocation(null);
				
				// reset player tool in inventory to current tool mode
				playerState.getCurrentToolMode().changeInventoryTool(player);
				return;
			}
			
			// if claim is not resizable and player is not in admin mode, send error message and return
			if (!claim.isResizeable() && !playerState.isAdminMode()) {
				
				// send player message
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_RESIZE_CLAIM_NOT_RESIZEABLE", claim);
				
				// reset player last tool location (it should be null here already)
				playerState.setLastToolLocation(null);
				
				// reset player tool in inventory to current tool mode
				playerState.getCurrentToolMode().changeInventoryTool(player);
				return;
			}
			
			// if player is not owner of claim or admin, send error message and stop
			if (!playerState.isAdminMode() &&
					!claim.getOwnerUUID().equals(player.getUniqueId())) {
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_RESIZE_NOT_OWNER", claim);
				
				// reset player tool
				playerState.getCurrentToolMode().changeInventoryTool(player);
				return;
			}
			
			// set highlight style for basic claim
			HighlightStyle highlightStyle = HighlightStyle.BASIC_CLAIM;
			
			// if admin claim, set visualization type to admin claim
			if (claim.isAdminClaim()) {
				highlightStyle = HighlightStyle.ADMIN_CLAIM;
			}
			
			// visualize claim being resized
			Visualization visualization = Visualization.fromClaim(claim, clickedBlock.getY(),
					highlightStyle, player.getLocation());
			Visualization.Apply(player, visualization);
			
			// send start resize success message
			plugin.messageManager.sendPlayerMessage(player, "TOOL_SUCCESS_RESIZE_START");
			
			// set last tool location
			playerState.setLastToolLocation(clickedBlock.getLocation());

			// set player's working claim
			playerState.setWorkingClaim(claim);
			
			// resize claim first click procedure is finished
		}

		@Override
		public final void onSecondClick(final Player player, final Block clickedBlock) {

			/*
			 * Resize Rules:
			 * Top Level Claims:
			 * if shrinking:
			 * - claim must still contain all child claims
			 * - remove surface fluids
			 * if not shrinking:
			 * - claim cannot overlap any other top level claims in world
			 * 
			 * Subclaims:
			 * - claim must be contained by parent claim
			 * - claim must not overlap any siblings
			 */
			
			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
			
			// set first click / second click locations
			Location firstClick = playerState.getLastToolLocation();
			Location secondClick = clickedBlock.getLocation();
			
			// if player clicked the same block, no resizing is necessary so return
			if (firstClick.equals(secondClick)) {
				
				// do we want to reset last tool location here? or let them keep trying to resize?
				return;
			}
			
			// if last tool location was in a different world, cancel resize
			if (!firstClick.getWorld().equals(secondClick.getWorld())) {

				if (plugin.debug) {
					plugin.getLogger().info("clicked locations are in different worlds. Cancelling resize.");
				}
				
				// reset player's last tool location to null
				playerState.setLastToolLocation(null);
				
				// reset player's working claim
				playerState.setWorkingClaim(null);
				
				// reset player tool in inventory to current tool mode
				playerState.getCurrentToolMode().changeInventoryTool(player);
				return;
			}

			// get old claim from player working claim
			Claim oldClaim = playerState.getWorkingClaim();
			
			// check if claim has been removed since player's first resize click
			if (oldClaim == null || !oldClaim.isActive()) {
				
				// send player message
				plugin.messageManager.sendPlayerMessage(player,"TOOL_FAIL_RESIZE_CLAIM_INACTIVE");
				
				// reset player's last tool location to null
				playerState.setLastToolLocation(null);
				
				// reset player's working claim
				playerState.setWorkingClaim(null);

				// reset player tool in inventory to current tool mode
				playerState.getCurrentToolMode().changeInventoryTool(player);
				return;
			}
			
			// get resized check claim
			Claim newClaim = oldClaim.getResizeCheckClaim(firstClick, secondClick);
			
			// if newClaim is null, resized corner overlapped opposite border
			if (newClaim == null) {
				
				// send player message
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_RESIZE_OVERLAP_SELF");
				
				// reset player's last tool location to null
				playerState.setLastToolLocation(null);
				
				// reset player's working claim
				playerState.setWorkingClaim(null);
				return;
			}
			
			// if first clicked location is corner of subclaim AND parent claim,
			// check if claim was enlarged in either direction, and get a new resized claim using parent claim

			// if old claim is a subclaim, get parent claim
			if (oldClaim.isSubClaim()) {

				Claim parentClaim = plugin.dataStore.getClaim(oldClaim.getParentKey());
				
				// if parent claim shares the clicked corner,
				// and new claim is larger in either length or width, use parent for resize claim
				if (parentClaim.isCorner(firstClick) 
						&& (newClaim.getLength() > oldClaim.getLength()
						|| newClaim.getWidth() > oldClaim.getWidth())) {
					
					// get a new resized claim using parent claim
					newClaim = parentClaim.getResizeCheckClaim(firstClick, secondClick);
					
					// set oldClaim to parentClaim
					oldClaim = parentClaim;
				}
			}

			// initialize claim result
			ClaimResult result = new ClaimResult();
			
			// if claim is top level claim
			if (!oldClaim.isSubClaim()) {
				
				// if old claim is not an admin claim, do checks for minimum size and player claim blocks
				if (!oldClaim.isAdminClaim() && !playerState.isAdminMode()) {
				
					// check minimum size requirements
					if (newClaim != null && (newClaim.getWidth() < plugin.getConfig().getInt("min-width") ||
							newClaim.getLength() < plugin.getConfig().getInt("min-length") ||
							newClaim.getArea() < plugin.getConfig().getInt("min-area"))) {
						plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_RESIZE_TOO_SMALL");
						//TODO: reset last tool location here?
						return;
					}

					// check that player has sufficient claim blocks for resize
					int blockDifference = 0;
					if (newClaim != null) {
						blockDifference = newClaim.getArea() - oldClaim.getArea();
					}

					if (blockDifference > playerState.getTotalClaimBlocks()) {
						
						if (plugin.debug) {
							plugin.getLogger().info("resize failed because player has insufficient claim blocks.");
						}
						
						// send player message
						plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_RESIZE_INSUFFICIENT_BLOCKS", oldClaim);

						// reset last tool location
						playerState.setLastToolLocation(null);
					
						// reset working claim
						playerState.setWorkingClaim(null);
				
						return;
					}
					else if (blockDifference > 0) {
						
						// subtract difference from player blocks
						playerState.removeClaimBlocks(blockDifference);
						
						//send message showing number of claim blocks removed / remaining
						plugin.messageManager.sendPlayerMessage(player, "PLAYER_INFO_BLOCKS_TOTAL");
					}
				}
			
				// if the resized top level claim is smaller than original in at least one dimension,
				// check that all child claims are still contained by resized claim
				if (newClaim != null
						&& (newClaim.getLength() < oldClaim.getLength()
						|| newClaim.getWidth() < oldClaim.getWidth())) {

					plugin.getLogger().info("Resized claim is smaller than original in at least one dimension.");

					//TODO: remove surface fluids about to be unclaimed
					//					oldClaim.removeSurfaceFluids();

					// get list of child claims
					Set<Claim> children = plugin.dataStore.getChildClaims(oldClaim.getKey());

					// check each child claim for containment
					for (Claim child : children) {

						// if new claim does not fully contain child, add to claim result overlap list
						if (!newClaim.contains(child)) {
							result.addOverlapClaim(child);
							result.setSuccess(false);
						}
					}

					// if any children are in result list, display message and visualize
					if (!result.isSuccess()) {

						// send player message
						plugin.messageManager.sendPlayerMessage(player,
								"TOOL_FAIL_RESIZE_PARENT_INSIDE_SUBCLAIM", oldClaim);

						// create visualization of outside children
						Visualization visualization = Visualization.fromClaims(result.getOverlapClaims(),
								clickedBlock.getY(), HighlightStyle.ERROR_CLAIM, player.getLocation());
						Visualization.Apply(player, visualization);

						// RESET STUFF ?
						// not resetting, so player can try resize second click again
						return;
					}
				}

				// if the resized top level claim is larger than original in at least one dimension
				if (newClaim != null
						&& (newClaim.getLength() > oldClaim.getLength()
						|| newClaim.getWidth() > oldClaim.getWidth())) {

					// check for overlap of all other top level claims
					for (Claim testClaim : plugin.dataStore.getAllClaims()) {

						// skip claim being resized
						if (testClaim.getKey().equals(oldClaim.getKey())) {
							continue;
						}

						// skip subclaims
						if (testClaim.isSubClaim()) {
							continue;
						}

						// test for overlap
						if (newClaim.overlaps(testClaim)) {
							result.addOverlapClaim(testClaim);
							result.setSuccess(false);
						}
					}

					// if success...
					if (result.isSuccess()) {

						// check if WorldGuard is enabled
						if (plugin.worldGuardHelper.isWorldGuardPresent()) {

							// if player is not admin or admin-overrides-worldguard is configured false, do wg check
							if (!playerState.isAdminMode()
									|| !plugin.getConfig().getBoolean("admin-overrides-worldguard")) {

								// test for overlapping WorldGuard regions that player does not have build permission in
								if (plugin.worldGuardHelper.overlaps(newClaim, player)) {

									//send player message
									plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_RESIZE_WORLDGUARD");

									// revert visualization
									Visualization.Revert(player);

									// reset last tool location
									playerState.setLastToolLocation(null);

									// reset tool in inventory
									changeInventoryTool(player, playerState.getCurrentToolMode());

									return;
								}
							}
						}
					}
				}
			}
			// resized claim is a subclaim;
			// check that parent still contains resized subclaim
			// check for overlap with siblings
			else {
				// get parent claim of old claim
				Claim parentClaim = plugin.dataStore.getClaim(oldClaim.getParentKey());
			
				// if parent claim does not fully contain new subclaim with its boundaries
				if (!parentClaim.contains(newClaim)) {
					
					// send player message
					plugin.messageManager.sendPlayerMessage(player,
							"TOOL_FAIL_RESIZE_SUBCLAIM_OUTSIDE_PARENT",oldClaim);
					
					// create visualization
					Visualization visualization = Visualization.fromClaim(oldClaim,
							clickedBlock.getY(), HighlightStyle.ERROR_CLAIM, player.getLocation());
					Visualization.Apply(player, visualization);
					
					// not resetting claim tool, so player can try resize second click again
					return;
				}
				
				// check if subclaim overlaps any siblings not including itself
				for (Claim sibling : plugin.dataStore.getChildClaims(parentClaim.getKey())) {
					if (newClaim != null
							&& !sibling.equals(oldClaim)
							&& newClaim.overlaps(sibling)) {
						result.addOverlapClaim(sibling);
						result.setSuccess(false);
					}
				}
			}
			
			// if no overlap, set old claim dimensions to new claim dimensions
			if (result.isSuccess()) {

				if (newClaim != null) {
					oldClaim.setLowerCorner(newClaim.getLowerCorner());
					oldClaim.setUpperCorner(newClaim.getUpperCorner());
				}

				// put old claim with new dimensions in claim result object
				result.setResultClaim(oldClaim);
				
				// save resized claim
				oldClaim.update();

				// inform and show the player
				plugin.messageManager.sendPlayerMessage(player,"TOOL_SUCCESS_RESIZE_FINISH");
				
				// set the correct visualization type for resized claim
				HighlightStyle vType = HighlightStyle.BASIC_CLAIM;
				if (oldClaim.isAdminClaim()) {
					vType = HighlightStyle.ADMIN_CLAIM;
				}
				
				// visualize resized claim
				Visualization visualization = Visualization.fromClaim(result.getResultClaim(),
						clickedBlock.getY(), vType, player.getLocation());
				Visualization.Apply(player, visualization);
				
				// clean up
				playerState.setWorkingClaim(null);
				playerState.setLastToolLocation(null);
			}
			else if (!result.isSuccess()) {
				
				if (plugin.debug) {
					plugin.getLogger().info("Overlapping claims: ");
					for (Claim claim : result.getOverlapClaims()) {
						plugin.getLogger().info("Claim " + claim.getKey());
					}
				}
				
				// inform player of overlap
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_RESIZE_OVERLAP");
				
				// show the player the conflicting claims if not null
				if (!result.getOverlapClaims().isEmpty()) {
						Visualization visualization = Visualization.fromClaims(result.getOverlapClaims(),
							clickedBlock.getY(), HighlightStyle.ERROR_CLAIM, player.getLocation());
						Visualization.Apply(player, visualization);
				}
				
				// reset last tool location to force restart of workflow after unsuccessful resize
				playerState.setLastToolLocation(null);
				
			}
			
			// reset tool in inventory to player current tool mode
			ClaimTool.changeInventoryTool(player,playerState.getCurrentToolMode());

			// resize finished
		}

		@Override
		public final void onEquip(final Player player) {

			// change tool in player inventory to admin tool
			this.changeInventoryTool(player);

		}

		@Override
		public final void onUnequip(final Player player) {
			
			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
			
			// if player was resizing a claim, send cancelled message and reset stuff
			if (playerState.getLastToolLocation() != null) {
				
				// send player message
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_RESIZE_CLAIM_CANCELLED");
				
				// revert visualization
				Visualization.Revert(player);
			}

			// reset inventory tool
			ClaimTool.changeInventoryTool(player, playerState.getCurrentToolMode());

			// reset last tool location
			playerState.setLastToolLocation(null);

			// reset working claim
			playerState.setWorkingClaim(null);
		}

	},
	
	DELETE(Material.SHEARS) {

		@Override
		public final void onFirstClick(final Player player, final Block clickedBlock) {

			// if the player doesn't have delete claim permission, display message and return
			if (!player.hasPermission("proclaim.claims.delete")) {
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_DELETE_PERMISSION");
				return;
			}

			// get claim at clicked location, ignoring height
			Claim clickedClaim = plugin.dataStore.getClaimAt(clickedBlock.getLocation(), true);
			
			// if no claim at clicked location, send error message and return
			if (clickedClaim == null) {
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_DELETE_NO_CLAIM");
				return;
			}

			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
			
			// if player is not owner of clicked claim or admin, send error message and return
			if (!playerState.isAdminMode() &&
					!clickedClaim.getOwnerUUID().equals(player.getUniqueId())) {
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_DELETE_NOT_OWNER", clickedClaim);
				return;
			}
			
			// set player last tool location to clicked location
			playerState.setLastToolLocation(clickedBlock.getLocation());

			// set player working claim to clicked claim
			playerState.setWorkingClaim(clickedClaim);

			// send player subclaim start success message
			plugin.messageManager.sendPlayerMessage(player, "TOOL_SUCCESS_DELETE_START", clickedClaim);

			// create fake claim for visualization
			Claim fakeClaim = new Claim(clickedClaim.getUpperCorner(),clickedClaim.getLowerCorner());
			
			// visualize the claim
			Visualization visualization = Visualization.fromClaim(
					fakeClaim,
					clickedBlock.getY(),
					HighlightStyle.DELETE_CLAIM,
					player.getLocation());
			Visualization.Apply(player, visualization);

			if (plugin.debug) {
				plugin.getLogger().info("Claim " + clickedClaim.getKey() 
					+ " selected for delete pending confirmation click.");
			}
		}
		
		@Override
		public final void onSecondClick(final Player player, final Block clickedBlock) {
			
			// if the player doesn't have delete claim permission, display message and return
			if (!player.hasPermission("proclaim.claims.delete")) {
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_DELETE_PERMISSION");
				return;
			}

			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
			
			// if last tool location is null, we shouldn't have gotten here; restart delete claim workflow
			if (playerState.getLastToolLocation() == null) {
				
				if (plugin.debug) {
					plugin.getLogger().info("Last tool location is null at " 
							+ "beginning of DELETE.onSecondClick() method. " 
							+ "Starting delete claim workflow over.");
				}
				
				// do onFirstClick and return
				this.onFirstClick(player, clickedBlock);
				return;
			}

			// if last tool location was in a different world, restart delete claim workflow
			if (!playerState.getLastToolLocation().getWorld().equals(clickedBlock.getWorld())) {

				if (plugin.debug) {
					plugin.getLogger().info("clicked locations are in different worlds. " 
							+ "Starting delete claim workflow over.");
				}
				
				// reset player's last tool location to null
				playerState.setLastToolLocation(null);
				
				// do onFirstClick and return
				this.onFirstClick(player, clickedBlock);
				return;
			}
			
			// get claim that was selected with first clicked
			Claim claim = playerState.getWorkingClaim();

			// check that second click is within same claim as first click, ignoring height
			if (!claim.contains(clickedBlock.getLocation(),true)) {
				
				// send player message
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_DELETE_CONFIRM", claim);
				
				// reset working claim
				playerState.setWorkingClaim(null);
				
				// reset last tool location
				playerState.setLastToolLocation(null);
				
				// revert visualization
				Visualization.Revert(player);
				
				return;
			}
			
			// delete the claim
			claim.delete();
			
			// reset working claim
			playerState.setWorkingClaim(null);
			
			// reset last tool location
			playerState.setLastToolLocation(null);
			
			// revert visualization
			Visualization.Revert(player);
			
			// send player message
			plugin.messageManager.sendPlayerMessage(player, "TOOL_SUCCESS_DELETE_FINISH", claim);
		}
		
		@Override
		public final void onEquip(final Player player) {

			// change tool in player inventory to delete tool
			this.changeInventoryTool(player);
		}
		
		@Override
		public final void onUnequip(final Player player) {
			
			// get player state
			PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());
			
			// if player was deleting a claim, send cancelled message and reset stuff
			if (playerState.getLastToolLocation() != null) {
				
				// send player message
				plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_DELETE_CLAIM_CANCELLED");
				
				// revert visualization
				Visualization.Revert(player);
			}
			
			// reset inventory tool
			ClaimTool.changeInventoryTool(player, playerState.getCurrentToolMode());

			// reset last tool location
			playerState.setLastToolLocation(null);

			// reset working claim
			playerState.setWorkingClaim(null);
		}
		
	};


	// reference to main class
	protected final PluginMain plugin = PluginMain.instance;

	// default tool material
	private final Material defaultMaterial;

	// current tool material
	private Material currentMaterial;

	// config prefix string for tool materials
	private final String configPrefix = "tool-material-";

	private String toolName;

	private List<String> toolLore;

	public static final Set<Material> toolTransparentMaterials = 
			Collections.unmodifiableSet(new HashSet<Material>(Arrays.asList(
					Material.AIR,
					Material.SNOW,
					Material.LONG_GRASS
				)));


	/**
	 * Class constructor
	 * @param defaultMaterial
	 */
	ClaimTool(final Material defaultMaterial) {
		this.defaultMaterial = defaultMaterial;
		this.currentMaterial = getConfigMaterial();
		this.toolName = getConfigName();
		this.toolLore = getConfigLore();
	}

	private Material getMaterial() {
		return currentMaterial;
	}


	private String getName() {
		return toolName;
	}


	private List<String> getLore() {
		return toolLore;
	}

	/**
	 * Get material from config.yml
	 * @return configured material or default material if no match
	 */
	private Material getConfigMaterial() {

		// get the material name for this ToolMode from the config.yml
		String configMaterialString = plugin.getConfig().getString(configPrefix + this.toString().toLowerCase());

		// try to match material name to a material
		Material matchMaterial = Material.matchMaterial(configMaterialString);

		if (matchMaterial == null) {
			matchMaterial = this.defaultMaterial;
		}
		return matchMaterial;
	}


	/**
	 * Get tool name from config.yml
	 * @return String configured tool name
	 */
	private String getConfigName() {

		// get the name for this ToolMode from the language file
		String configName = plugin.messageManager.getToolName(this);

		// if configured name is null or empty, set default name
		if (configName.isEmpty()) {
			configName = "ProClaim Tool";
		}
		return configName;
	}


	/**
	 * Get tool lore from language file
	 * @return String configured tool lore
	 */
	private List<String> getConfigLore() {

		// get the material name for this ToolMode from the config.yml
		List<String> configLore = plugin.messageManager.getToolLore(this);

		// if configured lore is null, set empty lore
		if (configLore == null) {
			configLore = new ArrayList<>();
		}
		return configLore;
	}


	/**
	 * Reload all tool settings and string from config.yml and language file
	 */
	public static void reload() {

		for (ClaimTool tool : ClaimTool.values()) {
			tool.currentMaterial = tool.getConfigMaterial();
			if (tool.currentMaterial == null) {
				tool.currentMaterial = tool.defaultMaterial;
			}
			tool.toolName = tool.getConfigName();
			if (tool.name() == null || tool.name().isEmpty()) {
				tool.toolName = "ProClaim Tool";
			}			
			tool.toolLore = tool.getConfigLore();
		}
	}


	/**
	 * Get tool type from item stack<br>
	 * checks item material and tool name for match
	 * @param item the item to determine claim tool type
	 * @return ToolMode or null if not a valid tool
	 */
	public static ClaimTool getType(final ItemStack item) {

		if (item != null) {
			for (ClaimTool mode : ClaimTool.values()) {
				if (item.getType().equals(mode.currentMaterial) 
						&& item.hasItemMeta()
						&& item.getItemMeta().getDisplayName().equals(mode.getName())) {
					return mode;
				}
			}
		}
		return null;
	}


	/**
	 * Check if an item is a valid tool<br>
	 * checks item material and tool name for match
	 * @param item the item to test if claim tool
	 * @return true if valid tool, false if not
	 */
	public static boolean isTool(final ItemStack item) {

		return getType(item) != null;
	}


	/**
	 * Create a ProClaim tool of the specified type
	 * @param toolType the type of claim tool to return
	 * @return ItemStack
	 */
	public static ItemStack create(final ClaimTool toolType) {

		Material toolMaterial = toolType.getMaterial();

		// create itemStack for tool
		ItemStack proClaimTool = new ItemStack(toolMaterial);

		// set tool display name and lore
		ItemMeta metaData = proClaimTool.getItemMeta();
		metaData.setDisplayName(toolType.getName());
		metaData.setLore(toolType.getLore());
		metaData.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		metaData.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		metaData.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
		proClaimTool.setItemMeta(metaData);

		//return custom tool ItemStack
		return proClaimTool;
	}


	/**
	 * Method executed when a player uses a tool with no current working claim set
	 * @param player
	 * @param clickedBlock
	 */
	abstract public void onFirstClick(final Player player, final Block clickedBlock);

	/**
	 * Method executed when a player uses a tool with a current working claim set
	 * @param player
	 * @param clickedBlock
	 */
	abstract public void onSecondClick(final Player player, final Block clickedBlock);
	
	/**
	 * Method executed when a player equips a tool, by command or action
	 * @param player
	 */
	abstract public void onEquip(final Player player);

	/**
	 * Method executed when a player switches away from a tool in inventory
	 * @param player
	 */
	abstract public void onUnequip(final Player player);

	public final void inspect(final Player player, final Block clickedBlock) {

		// get claim at click location
		Claim claim = plugin.dataStore.getClaimAt(clickedBlock.getLocation(), true);

		// if claim found, highlight and send message
		if (claim != null) {

			HighlightStyle highlightStyle = HighlightStyle.BASIC_CLAIM;
			
			// if claim is admin claim, set highlight style
			if (claim.isAdminClaim()) {
				highlightStyle = HighlightStyle.ADMIN_CLAIM;
			}

			// highlight claim border
			Visualization visualization = Visualization.fromClaim(claim,
					player.getEyeLocation().getBlockY(),
					highlightStyle, player.getLocation());
			Visualization.Apply(player, visualization);

			// send claim information message
			plugin.messageManager.sendPlayerMessage(player, "TOOL_SUCCESS_INSPECT", claim);
		}
		else {
			Visualization.Revert(player);

			// send no claim at location message
			plugin.messageManager.sendPlayerMessage(player, "TOOL_FAIL_INSPECT_NO_CLAIM");
		}
	}


	public final void useTool(final Player player, final Block clickedBlock) {

		// get player state
		PlayerState playerState = PlayerState.getPlayerState(player.getUniqueId());

		// if player's last tool location is null, then this is the starting click
		if (playerState.getLastToolLocation() == null) {			
			this.onFirstClick(player, clickedBlock);
		}
		else {
			// player's last tool location is not null, so this is the finishing click
			this.onSecondClick(player, clickedBlock);
		}
	}
	

	/**
 	 * changes all claim tools in player inventory to this type
	 * @param player
	 */
	public final void changeInventoryTool(final Player player) {
		
		// if tool in hand is proclaim tool, just switch that one
		if (isTool(player.getInventory().getItemInMainHand())) {
			player.getInventory().setItemInMainHand(create(this));
			return;
		}
		// if item in hand wasn't a proclaim tool, change all proclaim tools in player inventory
		Inventory playerInventory = player.getInventory();
		
		// iterate over every slot in player inventory
		for (int i = 0; i< playerInventory.getSize(); i++) {
			
			// if item in slot is a tool, replace with claimTool
			if(playerInventory.getItem(i) != null && ClaimTool.isTool(playerInventory.getItem(i))) {
				
				// if tools match, no swap necessary
				// TODO: FIX THIS
				if (!playerInventory.getItem(i).equals(this)) {
					playerInventory.setItem(i, ClaimTool.create(this));
				}
			}
		}
	}
	
	/**
 	 * Static version of method, changes all claim tools in player inventory to specified type
	 * @param player
	 * @param claimTool
	 */
	public static void changeInventoryTool(final Player player, final ClaimTool claimTool) {
		
		// if tool in hand is proclaim tool, just switch that one
		if (isTool(player.getInventory().getItemInMainHand())) {
			player.getInventory().setItemInMainHand(create(claimTool));
			return;
		}
		// if item in hand wasn't a proclaim tool, change all proclaim tools in player inventory
		Inventory playerInventory = player.getInventory();
		
		// iterate over every slot in player inventory
		for (int i = 0; i< playerInventory.getSize(); i++) {
			
			// if item in slot is a tool, replace with claimTool
			if(playerInventory.getItem(i) != null && ClaimTool.isTool(playerInventory.getItem(i))) {
				
				// if tools match, no swap necessary
				//TODO: FIX THIS
				if (!claimTool.equals(playerInventory.getItem(i))) {
					playerInventory.setItem(i, ClaimTool.create(claimTool));
				}
			}
		}
	}
	
	
	/**
	 * Get claim tool type in player inventory
	 * @param player the player whose inventory to search for claim tool
	 * @return claim tool of type first found in inventory, or null if none found
	 */
	public static ClaimTool getInventoryTool(final Player player) {
		
		ClaimTool returnTool = null;
		
		Inventory playerInventory = player.getInventory();
		
		// iterate over every slot in player inventory
		for (int i = 0; i< playerInventory.getSize(); i++) {
			
			// if item in slot is a tool set return tool
			if(playerInventory.getItem(i) != null && ClaimTool.isTool(playerInventory.getItem(i))) {
				
				returnTool = ClaimTool.getType(playerInventory.getItem(i));
				break;
			}
		}
		
		return returnTool;
	}

}
