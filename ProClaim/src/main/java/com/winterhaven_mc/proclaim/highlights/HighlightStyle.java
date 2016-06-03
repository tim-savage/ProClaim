package com.winterhaven_mc.proclaim.highlights;

import org.bukkit.Material;

public enum HighlightStyle {

	ADMIN_CLAIM(Material.GLOWSTONE, Material.PUMPKIN),
	BASIC_CLAIM(Material.GLOWSTONE, Material.GOLD_BLOCK),
	DELETE_CLAIM(Material.LAPIS_BLOCK, Material.LAPIS_ORE),
	ERROR_CLAIM(Material.GLOWING_REDSTONE_ORE, Material.NETHERRACK),
	START_CLAIM(Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK),
	SUB_CLAIM(Material.IRON_BLOCK, Material.WOOL);
	
	
	private Material cornerMaterial;
	private Material accentMaterial;
	
	HighlightStyle(Material cornerMaterial, Material accentMaterial) {

		this.setCornerMaterial(cornerMaterial);
		this.setAccentMaterial(accentMaterial);
	}

	public Material getCornerMaterial() {
		return cornerMaterial;
	}

	public void setCornerMaterial(Material cornerMaterial) {
		this.cornerMaterial = cornerMaterial;
	}

	public Material getAccentMaterial() {
		return accentMaterial;
	}

	public void setAccentMaterial(Material accentMaterial) {
		this.accentMaterial = accentMaterial;
	}
	
}
