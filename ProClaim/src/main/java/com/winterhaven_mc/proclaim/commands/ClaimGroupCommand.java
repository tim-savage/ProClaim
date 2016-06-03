package com.winterhaven_mc.proclaim.commands;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.storage.Claim;
import com.winterhaven_mc.proclaim.storage.ClaimGroup;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


final class ClaimGroupCommand implements CommandExecutor, TabCompleter {
	
	// reference to main class
	private final PluginMain plugin;
	
	// list of subcommands
	private final static List<String> SUBCOMMANDS = 
			Collections.unmodifiableList(new ArrayList<>(
					Arrays.asList("set","list","create","rename","setlimit","delete")));

	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	ClaimGroupCommand(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
		
		// register this class as command executor
		plugin.getCommand("claimgroup").setExecutor(this);
		
		// register this class as tab completer
		plugin.getCommand("claimgroup").setTabCompleter(this);
		
	}

	/**
	 * Manage claim groups
	 */
	@Override
	public final boolean onCommand(final CommandSender sender, final Command command, 
			final String label, final String[] args) {

		// if sender does not have permission for claimgroup command, output error message and return true
		if (!sender.hasPermission("proclaim.command.claimgroup")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}

		// argument limits
		final int minArgs = 1;
		final int maxArgs = 4;
		
		// check min arguments
		if (args.length < minArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_UNDER");
			return false;
		}
	
		// check max arguments
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_OVER");
			return false;
		}
		
		// get subcommand from first argument
		String subcommand = args[0];
		
		if (subcommand.equalsIgnoreCase("create")) {
			return createClaimGroup(sender, args);
		}
		else if (subcommand.equalsIgnoreCase("delete")) {
			return deleteClaimGroup(sender, args);
		}
		else if (subcommand.equalsIgnoreCase("rename")) {
			return renameClaimGroup(sender, args);
		}
		else if (subcommand.equalsIgnoreCase("setlimit")) {
			return setLimitClaimGroup(sender, args);
		}
		else if (subcommand.equalsIgnoreCase("list")) {
			return listClaimGroup(sender, args);
		}
		else if (subcommand.equalsIgnoreCase("set")) {
			return setClaimGroup(sender, args);
		}
		else if (subcommand.equalsIgnoreCase("unset")) {
			return unsetClaimGroup(sender, args);
		}
		else {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_INVALID_SUBCOMMAND");
			return false;
		}
	}

	
	private boolean createClaimGroup(final CommandSender sender, final String args[]) {
		
		// if sender does not have permission for claimgroup create subcommand, output error message and return true
		if (!sender.hasPermission("proclaim.command.claimgroup.create")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_CREATE_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}

		// argument limits
		final int minArgs = 2;
		final int maxArgs = 3;
		
		// check min arguments
		if (args.length < minArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_UNDER");
			return false;
		}
	
		// check max arguments
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_OVER");
			return false;
		}		
		
		String claimGroupName = args[1];
		
		if (claimGroupName.length() > 15) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_NAME_TOO_LONG");
			return true;
		}
	
		// set claim limit to configured default
		Integer limit = plugin.getConfig().getInt("claimgroup-default-limit");

		// if third argument passed, try to parse as integer claim limit
		if (args.length == 3) {
			
			try {
				limit = Integer.parseInt(args[2]);
			} catch (NumberFormatException e) {

				// if error parsing limit as integer, send message and return false
				plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_INVALID_INTEGER");
				return false;
			}
		}
		
		// check if claim group already exists
		if (ClaimGroup.getClaimGroup(claimGroupName) != null) {
			
			// send existing claim group error message
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_CREATE_EXISTING");
			return true;
		}
		
		// create new claimgroup with name and limit
		final ClaimGroup claimGroup = new ClaimGroup(claimGroupName,limit);
		
		// insert new claim group in datastore
		claimGroup.insert();
		
		// send success message
		plugin.messageManager.sendPlayerMessage(sender, "COMMAND_SUCCESS_CLAIMGROUP_CREATE");
		
		return true;
	}
	
	private boolean deleteClaimGroup(final CommandSender sender, final String args[]) {
		
		// if sender does not have permission for claimgroup delete subcommand, output error message and return true
		if (!sender.hasPermission("proclaim.command.claimgroup.delete")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_DELETE_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}

		// argument limits
		final int minArgs = 2;
		final int maxArgs = 2;
		
		// check min arguments
		if (args.length < minArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_UNDER");
			return false;
		}
	
		// check max arguments
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_OVER");
			return false;
		}
		
		String claimGroupName = args[1];
		
		// get claim group by name
		final ClaimGroup claimGroup = ClaimGroup.getClaimGroup(claimGroupName);
		
		// if claim group does not exist, send error message and return
		if (claimGroup == null) {
			
			// send player message
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_DELETE_NOT_FOUND");
			return true;
		}
		
		// delete the claim group from the datastore
		claimGroup.delete();

		// send player message
		plugin.messageManager.sendPlayerMessage(sender, "COMMAND_SUCCESS_CLAIMGROUP_DELETE");
		
		return true;
	}
	
	private boolean renameClaimGroup(final CommandSender sender, final String args[]) {
		
		// if sender does not have permission for claimgroup rename subcommand, output error message and return true
		if (!sender.hasPermission("proclaim.command.claimgroup.rename")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_RENAME_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}

		// argument limits
		final int minArgs = 3;
		final int maxArgs = 3;
		
		// check min arguments
		if (args.length < minArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_UNDER");
			return false;
		}
	
		// check max arguments
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_OVER");
			return false;
		}
		
		// get old and new names from args
		String oldName = args[1];
		String newName = args[2];
		
		// if new name is longer 15 chars, send error message and return
		if (newName.length() > 15) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_NAME_TOO_LONG");
			return true;
		}
		
		// get claim group by old name
		final ClaimGroup claimGroup = ClaimGroup.getClaimGroup(oldName);
		
		// if no claim group found, send error message and return
		if (claimGroup == null) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_NOT_FOUND");
			return true;
		}
		
		// set new name in claim group
		claimGroup.setName(newName);
		
		// update claim group in datastore
		claimGroup.update();
		
		// send success message
		plugin.messageManager.sendPlayerMessage(sender, "COMMAND_SUCCESS_CLAIMGROUP_RENAME");
		
		return true;
	}
	
	private boolean setLimitClaimGroup(final CommandSender sender, final String args[]) {
		
		// if sender does not have permission for claimgroup setlimit subcommand, output error message and return true
		if (!sender.hasPermission("proclaim.command.claimgroup.setlimit")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_SETLIMIT_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}

		// argument limits
		final int minArgs = 3;
		final int maxArgs = 3;
		
		// check min arguments
		if (args.length < minArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_UNDER");
			return false;
		}
	
		// check max arguments
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_OVER");
			return false;
		}
		
		String claimGroupName = args[1];
		//noinspection UnusedAssignment
		Integer limit = plugin.getConfig().getInt("claimgroup-default-limit");
		
		// try to parse arg[2] as integer claim limit
		try {
			limit = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			
			// if error parsing limit as integer, send message and return false
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_INVALID_INTEGER");
			return false;
		}

		// get claim group by name
		final ClaimGroup claimGroup = ClaimGroup.getClaimGroup(claimGroupName);
		
		// set new limit
		claimGroup.setClaimLimit(limit);
		
		// update claim group in datastore
		claimGroup.update();
		
		// send player success message
		plugin.messageManager.sendPlayerMessage(sender, "COMMAND_SUCCESS_CLAIMGROUP_SETLIMIT");
		
		return true;
	}

	private boolean listClaimGroup(final CommandSender sender, final String args[]) {
		
		// if sender does not have permission for claimgroup list subcommand, output error message and return true
		if (!sender.hasPermission("proclaim.command.claimgroup.list")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_LIST_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}

		
		// argument limits
		final int minArgs = 1;
		final int maxArgs = 1;
		
		// check min arguments
		if (args.length < minArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_UNDER");
			return false;
		}
	
		// check max arguments
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_OVER");
			return false;
		}
		
		// get all claim groups from datastore
		final List<ClaimGroup> claimGroups = new ArrayList<>(ClaimGroup.getAllClaimGroups());
		
		plugin.messageManager.sendPlayerMessage(sender, "COMMAND_INFO_CLAIMGROUP_LIST_HEADING");

		// display name and limit for each claimgroup in list
		for (ClaimGroup claimGroup : claimGroups) {
			sender.sendMessage(claimGroup.getName() + ": " + claimGroup.getClaimLimit());
		}
		
		return true;
	}

	
	private boolean setClaimGroup(final CommandSender sender, final String args[]) {
		
		// if sender does not have permission for claimgroup set subcommand, output error message and return true
		if (!sender.hasPermission("proclaim.command.claimgroup.set")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_SET_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}

		// sender must be in game player
		if (!(sender instanceof Player)) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CONSOLE");
			return true;
		}
		
		// argument limits
		final int minArgs = 2;
		final int maxArgs = 2;
		
		// check min arguments
		if (args.length < minArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_UNDER");
			return false;
		}
	
		// check max arguments
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_OVER");
			return false;
		}
		
		// get claim group name from args
		String claimGroupName = args[1];
		
		// get claim group from datastore
		final ClaimGroup claimGroup = ClaimGroup.getClaimGroup(claimGroupName);
		
		// if claim group does not exist, send error message and return
		if (claimGroup == null) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_SET_NO_CLAIMGROUP");
			return true;
		}
		
		// get player
		final Player player = (Player) sender;
		
		// get claim at player location, ignoring height
		Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true);
		
		// if no claim at player location, send message and return
		if (claim == null) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_SET_NO_CLAIM");
			return true;
		}

		// if claim is subclaim, get parent claim instead
		if (claim.isSubClaim()) {
			claim = plugin.dataStore.getClaim(claim.getParentKey());
		}
		
		// only allow players in adminMode to set claim groups?
		
		// set claimgroup key in claim record
		claim.setGroupKey(claimGroup.getKey());
		
		// update claim record
		claim.update();
		
		// send success message
		plugin.messageManager.sendPlayerMessage(sender, "COMMAND_SUCCESS_CLAIMGROUP_SET", claim);
		
		return true;
	}

	
	private boolean unsetClaimGroup(final CommandSender sender, final String args[]) {
		
		// if sender does not have permission for claimgroup set subcommand, output error message and return true
		if (!sender.hasPermission("proclaim.command.claimgroup.set")) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_UNSET_PERMISSION");
			plugin.soundManager.playerSound(sender, "command-fail");
			return true;
		}

		// sender must be in game player
		if (!(sender instanceof Player)) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CONSOLE");
			return true;
		}
		
		// argument limits
		final int minArgs = 1;
		final int maxArgs = 1;
		
		// check min arguments
		if (args.length < minArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_UNDER");
			return false;
		}
	
		// check max arguments
		if (args.length > maxArgs) {
			plugin.messageManager.sendPlayerMessage(sender,"COMMAND_FAIL_ARGS_COUNT_OVER");
			return false;
		}
		
		// get player
		final Player player = (Player) sender;
		
		// get claim at player location, ignoring height
		Claim claim = plugin.dataStore.getClaimAt(player.getLocation(), true);
		
		// if no claim at player location, send message and return
		if (claim == null) {
			plugin.messageManager.sendPlayerMessage(sender, "COMMAND_FAIL_CLAIMGROUP_SET_NO_CLAIM");
			return true;
		}
		
		// if claim is subclaim, get parent claim instead
		if (claim.isSubClaim()) {
			claim = plugin.dataStore.getClaim(claim.getParentKey());
		}
		
		// TODO: only allow players in adminMode to unset claim groups? 
		// probably yes, but we're covered by permission check for now
		
		// set claimgroup key in claim record to zero
		claim.setGroupKey(0);
		
		// update claim record
		claim.update();
		
		// send success message
		plugin.messageManager.sendPlayerMessage(sender, "COMMAND_SUCCESS_CLAIMGROUP_UNSET", claim);
		
		return true;
	}

	
	@Override
	public final List<String> onTabComplete(final CommandSender sender, final Command command, 
			final String alias, final String[] args) {
		
		// create empty return list
		final List<String> returnList = new ArrayList<>();
		
		// if completing first argument, return list of matching subcommands
		if (args.length == 1) {

			//noinspection Convert2streamapi
			for (String subcommand : SUBCOMMANDS) {
				if (sender.hasPermission("proclaim.command.claimgroup." + subcommand)
						&& subcommand.startsWith(args[0].toLowerCase())) {
					returnList.add(subcommand);
				}
			}
		}
		return returnList;
	}
	
}
