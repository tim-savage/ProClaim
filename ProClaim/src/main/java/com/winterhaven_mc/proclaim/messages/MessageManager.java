package com.winterhaven_mc.proclaim.messages;

import com.winterhaven_mc.proclaim.PluginMain;
import com.winterhaven_mc.proclaim.objects.ClaimTool;
import com.winterhaven_mc.proclaim.storage.Claim;
import com.winterhaven_mc.proclaim.storage.ClaimGroup;
import com.winterhaven_mc.proclaim.storage.PlayerState;

import com.winterhaven_mc.util.StringUtil;
import com.winterhaven_mc.util.LanguageManager;
import com.winterhaven_mc.util.YamlLanguageManager;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Implements message manager for {@code SavageDeathSpawn}.
 * 
 * @author      Tim Savage
 * @version		1.0
 *  
 */
public final class MessageManager {

	// reference to main class
	private final PluginMain plugin;

	// message cooldown hashmap
	private final ConcurrentHashMap<UUID, EnumMap<MessageId,Long>> messageCooldownMap;

	// language manager
	private LanguageManager languageManager;

	// configuration file manager for messages
	private Configuration messages;


	/**
	 * Constructor method for class
	 * 
	 * @param plugin reference to main class
	 */
	public MessageManager(final PluginMain plugin) {

		// set reference to main class
		this.plugin = plugin;

		// initialize messageCooldownMap
		this.messageCooldownMap = new ConcurrentHashMap<>();

		// instantiate language manager
		languageManager = new YamlLanguageManager(plugin);

		// load messages from file
		this.messages = languageManager.loadMessages();
	}


	/**
	 *  Send message to player
	 * 
	 * @param sender			player receiving message
	 * @param messageId			message identifier in messages file
	 */
	public final void sendPlayerMessage(final CommandSender sender, final MessageId messageId) {
		this.sendPlayerMessage(sender, messageId, null, null);
	}

	
	/**
	 * Send message to Player
	 * @param sender player to send message
	 * @param messageId message identifier
	 * @param claim the claim whose parameters may be used in the message
	 */
	public final void sendPlayerMessage(final CommandSender sender, final MessageId messageId, final Claim claim) {
		this.sendPlayerMessage(sender, messageId, claim, null);
	}

	
	/**
	 * Send message to player
	 * @param sender player to send message
	 * @param messageId message identifier
	 * @param targetPlayerUUID uuid of the target player to be used in the message
	 */
	public final void sendPlayerMessage(final CommandSender sender, final MessageId messageId, final UUID targetPlayerUUID) {
		this.sendPlayerMessage(sender, messageId, null, targetPlayerUUID);
	}

	
	/**
	 * Send message to player
	 * @param sender player to send message
	 * @param messageId message identifier
	 * @param claim the claim whose parameters may be used in the message
	 * @param targetPlayerUUID uuid of the target player to be used in the message
	 */
	public final void sendPlayerMessage(final CommandSender sender, final MessageId messageId,
			final Claim claim, final UUID targetPlayerUUID) {

		// if message is not enabled in messages file, do nothing and return
		if (!messages.getBoolean("messages." + messageId + ".enabled")) {
			return;
		}

		// set substitution variable defaults			
		String playerName = "console";
		String targetName = "unknown";
		String worldName = "world";
		String ownerName = "unknown";
		String claimGroupName = "none";
		String createdTime = "unknown";
		String claimKey = "unknown";
		String claimLength = "0";
		String claimWidth = "0";
		String claimArea = "0";
		String claimResizeable = "unknown";
		String claimLocked = "unknown";
		String playerBlocks = "unknown";
		String targetPlayerBlocks = "unknown";
		
		// if sender is a player...
		if (sender instanceof Player) {

			Player player = (Player) sender;

			// get message cooldown time remaining
			long lastDisplayed = getMessageCooldown(player,messageId);

			// get message repeat delay
			int messageRepeatDelay = messages.getInt("messages." + messageId + ".repeat-delay");

			// if message has repeat delay value and was displayed to player more recently, do nothing and return
			if (lastDisplayed > System.currentTimeMillis() - messageRepeatDelay * 1000) {
				return;
			}

			// if repeat delay value is greater than zero, add entry to messageCooldownMap
			if (messageRepeatDelay > 0) {
				putMessageCooldown(player,messageId);
			}

			// assign player dependent variables
			playerName = player.getName();
			worldName = player.getWorld().getName();
 			playerBlocks = String.valueOf(PlayerState.getPlayerState(player.getUniqueId()).getTotalClaimBlocks());

		}

		// get message text from file
		String message = messages.getString("messages." + messageId.toString() + ".text");
		
		// get world name from worldManager
		worldName = plugin.worldManager.getWorldName(worldName);

		// get target player variables
		if (targetPlayerUUID != null) {
			// get target player from uuid
			OfflinePlayer targetPlayer = plugin.getServer().getOfflinePlayer(targetPlayerUUID);

			if (targetPlayer != null) {

				targetName = targetPlayer.getName();

				if (targetPlayer.isOnline()) {
					Player onlineTargetPlayer = (Player)targetPlayer;
					worldName = onlineTargetPlayer.getWorld().getName();
				}

				PlayerState targetPlayerState = PlayerState.getPlayerState(targetPlayerUUID);

				if (targetPlayerState != null) {
					targetPlayerBlocks = String.valueOf(targetPlayerState.getTotalClaimBlocks());
				}
			}
		}
		
		// get claim variables
		if (claim != null) {
			if (claim.isAdminClaim()) {
				ownerName = this.getAdminName();
			}
			else {
				ownerName = PlayerState.getPlayerState(claim.getOwnerUUID()).getName();
			}
			if (claim.getKey() != null) {
				claimKey = claim.getKey().toString();
			}

			claimLength = claim.getLength().toString();

			claimWidth = claim.getWidth().toString();

			claimArea = claim.getArea().toString();

			if (claim.getResizeable() != null) {
				claimResizeable = claim.getResizeable().toString();
			}

			if (claim.isLocked() != null) {
				claimLocked = claim.isLocked().toString();
			}
			
			// format created time/date string
			createdTime = DateTimeFormatter.RFC_1123_DATE_TIME.format(
					ZonedDateTime.ofInstant(claim.getCreatedDate(), ZoneId.systemDefault()));

			// get claim group name
			if (claim.getGroupKey() != null && claim.getGroupKey() != 0) {
				ClaimGroup claimGroup = ClaimGroup.getClaimGroup(claim.getGroupKey());
				if (claimGroup != null) {
					claimGroupName = claimGroup.getName();
				}
			}
		}
		
		// do variable substitutions
		if (message.contains("%")) {
			message = StringUtil.replace(message,"%PLAYER_NAME%", playerName);
			message = StringUtil.replace(message,"%TARGET_PLAYER_NAME%", targetName);
			message = StringUtil.replace(message,"%TARGET_PLAYER_BLOCKS%", targetPlayerBlocks);
			message = StringUtil.replace(message,"%WORLD_NAME%", worldName);
			message = StringUtil.replace(message,"%OWNER_NAME%", ownerName);
			message = StringUtil.replace(message,"%CLAIM_KEY%", claimKey);
			message = StringUtil.replace(message,"%CLAIM_GROUP%", claimGroupName);
			message = StringUtil.replace(message,"%CLAIM_LENGTH%", claimLength);
			message = StringUtil.replace(message,"%CLAIM_WIDTH%", claimWidth);
			message = StringUtil.replace(message,"%CLAIM_AREA%", claimArea);
			message = StringUtil.replace(message,"%CLAIM_RESIZEABLE%", claimResizeable);
			message = StringUtil.replace(message,"%CLAIM_LOCKED%", claimLocked);
			message = StringUtil.replace(message,"%CLAIM_CREATED%", createdTime);
			message = StringUtil.replace(message, "%PLAYER_BLOCKS%", playerBlocks);
		}
		// send message to player
		sender.sendMessage(ChatColor.translateAlternateColorCodes('&',message));
	}


	/**
	 * Add entry to message cooldown map
	 * @param player player to add to cooldown map
	 * @param messageId message identifier to add to cooldown map
	 */
	private void putMessageCooldown(final Player player, final MessageId messageId) {

		EnumMap<MessageId,Long> tempMap = new EnumMap<>(MessageId.class);
		tempMap.put(messageId, System.currentTimeMillis());
		messageCooldownMap.put(player.getUniqueId(), tempMap);
	}


	/**
	 * get entry from message cooldown map
	 * @param player player for whom to retrieve expire time
	 * @param messageId message identifier for which to retrieve expire time
	 * @return cooldown expire time
	 */
	private long getMessageCooldown(final Player player, final MessageId messageId) {

		// check if player is in message cooldown hashmap
		if (messageCooldownMap.containsKey(player.getUniqueId())) {

			// check if messageID is in player's cooldown hashmap
			if (messageCooldownMap.get(player.getUniqueId()).containsKey(messageId)) {

				// return cooldown time
				return messageCooldownMap.get(player.getUniqueId()).get(messageId);
			}
		}
		return 0L;
	}


//	/**
//	 * Remove player from message cooldown map
//	 * @param player
//	 */
//	final void removePlayerCooldown(final Player player) {
//		messageCooldownMap.remove(player.getUniqueId());
//	}


	/**
	 * Reload messages
	 */
	public final void reload() {

		// reload messages
		this.messages = languageManager.loadMessages();
	}


	public final String getToolName(final ClaimTool toolType) {
		String configPrefix = "tool-name-";
		return ChatColor.translateAlternateColorCodes('&',messages
				.getString(configPrefix + toolType.toString().toLowerCase()));
	}

	
	public final List<String> getToolLore(final ClaimTool toolType) {
		String configPrefix = "tool-lore-";
		List<String> lore = messages.getStringList(configPrefix + toolType.toString().toLowerCase());
		int lineNumber = 0;
		while (lineNumber < lore.size()) {
			lore.set(lineNumber, ChatColor.translateAlternateColorCodes('&',lore.get(lineNumber)));
			lineNumber++;
		}
		return lore;
	}
	
	public final String getAdminName() {
		return ChatColor.translateAlternateColorCodes('&',messages.getString("admin-name"));
	}

}
