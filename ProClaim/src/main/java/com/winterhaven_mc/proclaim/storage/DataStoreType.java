package com.winterhaven_mc.proclaim.storage;

import com.winterhaven_mc.proclaim.PluginMain;

public enum DataStoreType {

	SQLITE("SQLite") {

		@Override
		public DataStore create() {
			
			// create new sqlite datastore object
			return new DataStoreSQLite(plugin);
		}
	};

	private String friendlyName;

	private final static PluginMain plugin = PluginMain.instance;
	
	private final static DataStoreType defaultType = DataStoreType.SQLITE;
	

	/**
	 * Class constructor
	 * @param friendlyName the display name of the datastore
	 */
	DataStoreType(final String friendlyName) {
		this.friendlyName = friendlyName;
	}


	public abstract DataStore create();

	@Override
	public String toString() {
		return friendlyName;
	}


	public static DataStoreType match(final String configName) {
		for (DataStoreType type : DataStoreType.values()) {
			if (type.name().equalsIgnoreCase(configName)) {
				return type;
			}
		}
		// no match; return default type
		// only display this log message if more than one datastore type is defined
		if (DataStoreType.values().length > 1) {
			plugin.getLogger().info("No match for configured datastore type '" + configName + "'. "
					+ "Using default type '" + defaultType.name() + "'.");
		}
		
		return defaultType;
	}

	public static DataStoreType getDefaultType() {
		return defaultType;
	}
	
}
