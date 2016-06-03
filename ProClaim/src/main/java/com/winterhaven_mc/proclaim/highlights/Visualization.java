package com.winterhaven_mc.proclaim.highlights;

/*
GriefPrevention Server Plugin for Minecraft
Copyright (C) 2012 Ryan Hamshire

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/


import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.storage.Claim;
import com.winterhaven_mc.proclaim.storage.PlayerState;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.*;

//represents a visualization sent to a player
//FEATURE: to show players visually where claim boundaries are, we send them fake block change packets
//the result is that those players see new blocks, but the world hasn't been changed.
//other players can't see the new blocks, either.
public class Visualization {

	// reference to main plugin
	private final static PluginMain plugin = PluginMain.instance;

	// unmodifiable set of materials to be regarded as transparent
	private final static Set<Material> TRANSPARENT_MATERIALS =
			Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
					Material.AIR,
					Material.SNOW,
					Material.LONG_GRASS,
					Material.FENCE,
					Material.ACACIA_FENCE,
					Material.BIRCH_FENCE,
					Material.DARK_OAK_FENCE,
					Material.JUNGLE_FENCE,
					Material.NETHER_FENCE,
					Material.SPRUCE_FENCE,
					Material.FENCE_GATE,
					Material.ACACIA_FENCE_GATE,
					Material.BIRCH_FENCE_GATE,
					Material.DARK_OAK_FENCE_GATE,
					Material.SPRUCE_FENCE_GATE,
					Material.JUNGLE_FENCE_GATE,
					Material.SIGN,
					Material.SIGN_POST,
					Material.WALL_SIGN )));
	
	ArrayList<VisualizationElement> elements = new ArrayList<>();
	
	//sends a visualization to a player
	public static void Apply(Player player, Visualization visualization) {

		PlayerState playerData = PlayerState.getPlayerState(player.getUniqueId());

		//if he has any current visualization, clear it first
		if (playerData.getCurrentVisualization() != null) {
			Visualization.Revert(player);
		}

		//if he's online, create a task to send him the visualization
		if (player.isOnline() && visualization.elements.size() > 0 
				&& visualization.elements.get(0).getLocation().getWorld().equals(player.getWorld())) {
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new VisualizationApplicationTask(player, playerData, visualization), 1L);
		}
	}

	//reverts a visualization by sending another block change list, this time with the real world block values
	@SuppressWarnings("deprecation")
	public static void Revert(Player player) {

		if (!player.isOnline()) {
			return;
		}

		PlayerState playerData = PlayerState.getPlayerState(player.getUniqueId());

		Visualization visualization = playerData.getCurrentVisualization();

		if (playerData.getCurrentVisualization() != null) {

			//locality
			int minx = player.getLocation().getBlockX() - 100;
			int minz = player.getLocation().getBlockZ() - 100;
			int maxx = player.getLocation().getBlockX() + 100;
			int maxz = player.getLocation().getBlockZ() + 100;

			//remove any elements which are too far away
			visualization.removeElementsOutOfRange(visualization.elements, minx, minz, maxx, maxz);

			//send real block information for any remaining elements
			for (int i = 0; i < visualization.elements.size(); i++) {

				VisualizationElement element = visualization.elements.get(i);

				//check player still in world where visualization exists
				if (i == 0) {
					if(!player.getWorld().equals(element.getLocation().getWorld())) return;
				}

				player.sendBlockChange(element.getLocation(), element.realMaterial, element.realData);
			}

			playerData.setCurrentVisualization(null);  
		}
	}

	// convenience method to build a visualization from a claim
	// highlightStyle determines the style (gold blocks, silver, red, diamond, etc)
	public static Visualization fromClaim(Claim claim,
			int height,
			HighlightStyle highlightStyle,
			Location locality) {

		//visualize only top level claims
		if (claim.getParentKey() != null && !claim.getParentKey().equals(0)) {

			Claim parent = plugin.dataStore.getClaim(claim.getParentKey());
			return fromClaim(parent, height, highlightStyle, locality);
		}

		Visualization visualization = new Visualization();

		// get list of child claims
		Collection<Claim> children = plugin.dataStore.getChildClaims(claim.getKey());

		//add subdivisions first
		for (Claim child : children) {

			//	    if(!child.inDataStore) continue;
			visualization.addClaimElements(child, height, HighlightStyle.SUB_CLAIM, locality);
		}

		// add top level last so that it takes precedence
		// (it shows on top when the child claim boundaries overlap with its boundaries)
		visualization.addClaimElements(claim, height, highlightStyle, locality);

		return visualization;
	}

	//adds a claim's visualization to the current visualization
	//handy for combining several visualizations together, as when visualization a top level claim with several subdivisions inside
	//locality is a performance consideration.  only create visualization blocks for around 100 blocks of the locality
	@SuppressWarnings("deprecation")
	private void addClaimElements(Claim claim,
			int height,
			HighlightStyle highlightStyle,
			Location locality) {

		Location lowerCorner = claim.getLowerCorner();
		Location upperCorner = claim.getUpperCorner();
		World world = lowerCorner.getWorld();
		boolean waterIsTransparent = locality.getBlock().getType().equals(Material.STATIONARY_WATER);

		int smallx = lowerCorner.getBlockX();
		int smallz = lowerCorner.getBlockZ();
		int bigx = upperCorner.getBlockX();
		int bigz = upperCorner.getBlockZ();

		Material cornerMaterial = highlightStyle.getCornerMaterial();
		Material accentMaterial = highlightStyle.getAccentMaterial();

		ArrayList<VisualizationElement> newElements = new ArrayList<>();


		//initialize visualization elements without Y values and real data
		//that will be added later for only the visualization elements within visualization range

		//locality
		int minx = locality.getBlockX() - 75;
		int minz = locality.getBlockZ() - 75;
		int maxx = locality.getBlockX() + 75;
		int maxz = locality.getBlockZ() + 75;

		final int STEP = 10;

		//top line		
		newElements.add(new VisualizationElement(new Location(world, smallx, 0, bigz), cornerMaterial, (byte)0, Material.AIR, (byte)0));
		newElements.add(new VisualizationElement(new Location(world, smallx + 1, 0, bigz), accentMaterial, (byte)0, Material.AIR, (byte)0));
		for(int x = smallx + STEP; x < bigx - STEP / 2; x += STEP)
		{
			if(x > minx && x < maxx)
				newElements.add(new VisualizationElement(new Location(world, x, 0, bigz), accentMaterial, (byte)0, Material.AIR, (byte)0));
		}
		newElements.add(new VisualizationElement(new Location(world, bigx - 1, 0, bigz), accentMaterial, (byte)0, Material.AIR, (byte)0));

		//bottom line
		newElements.add(new VisualizationElement(new Location(world, smallx + 1, 0, smallz), accentMaterial, (byte)0, Material.AIR, (byte)0));
		for(int x = smallx + STEP; x < bigx - STEP / 2; x += STEP)
		{
			if(x > minx && x < maxx)
				newElements.add(new VisualizationElement(new Location(world, x, 0, smallz), accentMaterial, (byte)0, Material.AIR, (byte)0));
		}
		newElements.add(new VisualizationElement(new Location(world, bigx - 1, 0, smallz), accentMaterial, (byte)0, Material.AIR, (byte)0));

		//left line
		newElements.add(new VisualizationElement(new Location(world, smallx, 0, smallz), cornerMaterial, (byte)0, Material.AIR, (byte)0));
		newElements.add(new VisualizationElement(new Location(world, smallx, 0, smallz + 1), accentMaterial, (byte)0, Material.AIR, (byte)0));
		for(int z = smallz + STEP; z < bigz - STEP / 2; z += STEP)
		{
			if(z > minz && z < maxz)
				newElements.add(new VisualizationElement(new Location(world, smallx, 0, z), accentMaterial, (byte)0, Material.AIR, (byte)0));
		}
		newElements.add(new VisualizationElement(new Location(world, smallx, 0, bigz - 1), accentMaterial, (byte)0, Material.AIR, (byte)0));

		//right line
		newElements.add(new VisualizationElement(new Location(world, bigx, 0, smallz), cornerMaterial, (byte)0, Material.AIR, (byte)0));
		newElements.add(new VisualizationElement(new Location(world, bigx, 0, smallz + 1), accentMaterial, (byte)0, Material.AIR, (byte)0));
		for(int z = smallz + STEP; z < bigz - STEP / 2; z += STEP)
		{
			if(z > minz && z < maxz)
				newElements.add(new VisualizationElement(new Location(world, bigx, 0, z), accentMaterial, (byte)0, Material.AIR, (byte)0));
		}
		newElements.add(new VisualizationElement(new Location(world, bigx, 0, bigz - 1), accentMaterial, (byte)0, Material.AIR, (byte)0));
		newElements.add(new VisualizationElement(new Location(world, bigx, 0, bigz), cornerMaterial, (byte)0, Material.AIR, (byte)0));

		//remove any out of range elements
		this.removeElementsOutOfRange(newElements, minx, minz, maxx, maxz);

		//remove any elements outside the claim
		for(int i = 0; i < newElements.size(); i++) {
			VisualizationElement element = newElements.get(i);
			if(!claim.contains(element.getLocation(), true)) {
				newElements.remove(i--);
			}
		}

		//set Y values and real block information for any remaining visualization blocks
		for(VisualizationElement element : newElements)
		{
			Location tempLocation = element.getLocation();
			element.setLocation(getVisibleLocation(tempLocation.getWorld(), tempLocation.getBlockX(), height, tempLocation.getBlockZ(), waterIsTransparent));
			height = element.getLocation().getBlockY();
			element.realMaterial = element.getLocation().getBlock().getType();
			element.realData = element.getLocation().getBlock().getData();
		}

		this.elements.addAll(newElements);
	}

	//removes any elements which are out of visualization range
	private void removeElementsOutOfRange(ArrayList<VisualizationElement> elements, int minx, int minz, int maxx, int maxz) {

		for(int i = 0; i < elements.size(); i++)
		{
			Location location = elements.get(i).getLocation();
			if(location.getX() < minx || location.getX() > maxx || location.getZ() < minz || location.getZ() > maxz)
			{
				elements.remove(i--);
			}
		}
	}

	//finds a block the player can probably see.  this is how visualizations "cling" to the ground or ceiling
	private static Location getVisibleLocation(World world, int x, int y, int z, boolean waterIsTransparent)
	{
		Block block = world.getBlockAt(x,  y, z);
		BlockFace direction = (isTransparent(block, waterIsTransparent)) ? BlockFace.DOWN : BlockFace.UP;

		while(	block.getY() >= 1 && 
				block.getY() < world.getMaxHeight() - 1 &&
				(!isTransparent(block.getRelative(BlockFace.UP), waterIsTransparent) || isTransparent(block, waterIsTransparent)))
		{
			block = block.getRelative(direction);
		}

		return block.getLocation();
	}

	
	private static boolean isTransparent(Block block, boolean waterIsTransparent) {
		
		// if block type is stationary water, return passed boolean value waterIsTransparent
		if (block.getType().equals(Material.STATIONARY_WATER)) {
			return waterIsTransparent;
		}

		// if block type is a transparent material, return true
		if (block.getType().isTransparent()) {
			return true;
		}

		// if block type is in set of transparent materials, return true
		if (TRANSPARENT_MATERIALS.contains(block.getType())) {
			return true;
		}
		
		// if none of the above, return false
		return false;
	}


	public static Visualization fromClaims(Iterable<Claim> claims, int height, HighlightStyle type, Location locality) {
		Visualization visualization = new Visualization();

		for(Claim claim : claims) {
			visualization.addClaimElements(claim, height, type, locality);
		}  
		return visualization;
	}
	
}