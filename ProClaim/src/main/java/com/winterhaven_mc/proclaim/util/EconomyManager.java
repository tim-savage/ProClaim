package com.winterhaven_mc.proclaim.util;

import org.bukkit.plugin.RegisteredServiceProvider;

import com.winterhaven_mc.proclaim.PluginMain;

import net.milkbowl.vault.economy.Economy;

public final class EconomyManager {

	private final PluginMain plugin;

	private boolean vaultPresent = false;
	private Economy economy = null;


	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	public EconomyManager(final PluginMain plugin) {

		// set reference to main class
		this.plugin = plugin;

		// set for vaultPresent
		vaultPresent = checkVaultPresent();

		// check if vault is present
		if (vaultPresent) {
			plugin.getLogger().info("Vault detected.");

			// check that economy is registered
			if (setupEconomy()) {
				plugin.getLogger().info(economy.getName() + " detected.");	
			}
			else {
				plugin.getLogger().warning("No compatible economy plugin detected [Vault].");
			}
		}
	}

	/**
	 * Check if vault is installed
	 * @return {@code true} if Vault is installed, {@code false} if not
	 */
	private boolean checkVaultPresent() {
		vaultPresent = !(plugin.getServer().getPluginManager().getPlugin("Vault") == null); 
		return vaultPresent;
	}


	/**
	 * Setup economy provider
	 * @return {@code true} if Vault is installed, {@code false} if not
	 */
	private boolean setupEconomy() {
		if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		economy = rsp.getProvider();
		return economy != null;
	}


	public final boolean isEconomyRegistered() {
		return economy != null;
	}


	public final Economy getEconomy() {
		return this.economy;
	}

}
