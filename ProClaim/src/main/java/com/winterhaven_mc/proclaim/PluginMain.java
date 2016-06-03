package com.winterhaven_mc.proclaim;

import com.winterhaven_mc.util.SoundManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.winterhaven_mc.proclaim.commands.CommandManager;
import com.winterhaven_mc.proclaim.listeners.*;
import com.winterhaven_mc.proclaim.storage.DataStore;
import com.winterhaven_mc.proclaim.storage.DataStoreFactory;
import com.winterhaven_mc.proclaim.tasks.TaskManager;
import com.winterhaven_mc.proclaim.util.EconomyManager;
import com.winterhaven_mc.proclaim.util.MessageManager;
import com.winterhaven_mc.proclaim.util.WorldGuardHelper;
import com.winterhaven_mc.util.WorldManager;

public class PluginMain extends JavaPlugin {

	// static reference to the instance of this plugin
	public static PluginMain instance;

	// set debug flag from config file
	public boolean debug = getConfig().getBoolean("debug");
	
	// data storage object for players, claims, claim groups and permissions
	public DataStore dataStore;

	public WorldManager worldManager;
	public WorldGuardHelper worldGuardHelper;
	public CommandManager commandManager;
	public MessageManager messageManager;
	public SoundManager soundManager;
	public EconomyManager economyManager;
	public TaskManager taskManager;
	
	@Override
	public void onEnable() {

		// set static reference to main class
		instance = this;
		
		// install default config.yml if not present  
		saveDefaultConfig();
		
		// instantiate world manager
		worldManager = new WorldManager(this);
		
		// instantiate WorldGuard helper
		worldGuardHelper = new WorldGuardHelper(this);
		
		// get initialized storage object
		dataStore = DataStoreFactory.create();
		
		// instantiate economy manager
		economyManager = new EconomyManager(this);
		
		// instantiate message manager
		messageManager = new MessageManager(this);

		// instantiate sound manager
		soundManager = new SoundManager(this);
		
		// instantiate command manager
		commandManager = new CommandManager(this);
		
		// instantiate task manager
		taskManager = new TaskManager(this);
		
		// instantiate event listeners
		new PlayerEventListener(this);
		new BlockEventListener(this);
		new EntityEventListener(this);
	}
	
	@Override
	public void onDisable() {
		
		// close datastore
		dataStore.close();
	}
	
}
