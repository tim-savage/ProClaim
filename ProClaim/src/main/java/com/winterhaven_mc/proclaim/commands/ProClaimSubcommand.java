package com.winterhaven_mc.proclaim.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

enum ProClaimSubcommand {
	
	ADMIN	(
			"/proclaim admin",
			"Puts player in ProClaim admin mode."
			),
	
	HELP	(
			"/proclaim help [command]",
			"Displays help for ProClaim commands."
			),
	
	RELOAD	(
			"/proclaim reload",
			"Reloads the configuration without needing to restart the server."
			),
	
	SHOWCACHE (
			"/proclaim showcache [player|claim]",
			"Displays cache statistics."
			),
	
	STATUS	(
			"/proclaim status",
			"Displays current configuration settings."
			);
	

	private final String usageString;
	private final String helpString;
	
	private final static ChatColor usageColor = ChatColor.GOLD;
	
	/**
	 * Class constructor
	 * @param usageString subcommand usage string
	 * @param helpString subcommand help string
	 */
	ProClaimSubcommand(final String usageString, final String helpString) {
	
		this.usageString = usageString;
		this.helpString = helpString;
	}
	
	@Override
	public String toString() {
		return this.name().toLowerCase();
	}
	
	public String getUsageString() {
		return this.usageString;
	}
	
	public String getHelpString() {
		return this.helpString;
	}
	
	public static ProClaimSubcommand getMatch(final String subcommand) {
		
		for (ProClaimSubcommand pcsc : ProClaimSubcommand.values()) {
			if (subcommand.equalsIgnoreCase(pcsc.toString())) {
				return pcsc;
			}
		}
		return null;
	}


	/**
	 *
	 * @param sender command sender
	 * @param commandName command for which to display usage
	 */
	// TODO: should this be removed?
	@SuppressWarnings("unused")
	void displayUsage(final CommandSender sender, final String commandName) {

		// copy of commandName parameter
		String cmdName = commandName;
		
		if (cmdName.isEmpty() || cmdName.equalsIgnoreCase("help")) {
			cmdName = "all";
		}
		for (ProClaimSubcommand subcmd : ProClaimSubcommand.values()) {
			if ((subcmd.toString().equalsIgnoreCase(cmdName) || cmdName.equals("all"))
					&& sender.hasPermission("proclaim.command." + subcmd.toString().toLowerCase())) {
				sender.sendMessage(usageColor + subcmd.getUsageString());
			}
		}
	}

}
