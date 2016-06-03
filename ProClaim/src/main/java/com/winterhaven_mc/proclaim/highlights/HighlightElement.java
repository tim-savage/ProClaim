package com.winterhaven_mc.proclaim.highlights;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;


public class HighlightElement {
	
	// element location
	private Location location;
	
	// highlight element material
	private Material highlightMaterial;
	
	
	/**
	 * Class constructor
	 * @param location
	 * @param highlightMaterial
	 */
	public HighlightElement(Location location, Material highlightMaterial) {
		
		this.setLocation(location);
		this.setHighlightMaterial(highlightMaterial);	
	}

	public Location getLocation() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public Material getHighlightMaterial() {
		return highlightMaterial;
	}

	public void setHighlightMaterial(Material highlightMaterial) {
		this.highlightMaterial = highlightMaterial;
	}

	@SuppressWarnings("deprecation")
	void show(Player player) {
		player.sendBlockChange(this.location, this.highlightMaterial, (byte) 0);
	}
	
	@SuppressWarnings("deprecation")
	void hide(Player player) {
		
		Block block = this.location.getBlock();
		player.sendBlockChange(location, block.getType(), block.getData());
	}
	
}
