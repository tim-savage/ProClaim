package com.winterhaven_mc.proclaim.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.winterhaven_mc.proclaim.PluginMain;


public final class DataStoreFactory {

	private final static PluginMain plugin = PluginMain.instance;

	public final static void reload() {
		
		// get current datastore type
		DataStoreType currentType = plugin.dataStore.getType();
		
		// get configured datastore type
		DataStoreType newType = DataStoreType.match(plugin.getConfig().getString("storage-type"));
	
		// if current datastore type does not match configured datastore type, create new datastore
		if (!currentType.equals(newType)) {
			
			// create new datastore
			plugin.dataStore = create(newType,plugin.dataStore);
		}
	}


	/**
	 * Create new data store of given type.<br>
	 * No parameter version used when no current datastore exists
	 * and datastore type should be read from configuration
	 * @return new datastore of configured type
	 */
	public final static DataStore create() {
		
		// get data store type from config
		DataStoreType dataStoreType = DataStoreType.match(plugin.getConfig().getString("storage-type"));
		if (dataStoreType == null) {
			dataStoreType = DataStoreType.getDefaultType();
		}
		return create(dataStoreType, null);
	}
	

	/**
	 * Create new data store of given type and convert old data store.<br>
	 * Two parameter version used when a datastore instance already exists
	 * @param dataStoreType		new datastore type
	 * @param oldDataStore		existing datastore reference
	 * @return
	 */
	private final static DataStore create(final DataStoreType dataStoreType, final DataStore oldDataStore) {
	
		// get new data store of specified type
		DataStore newDataStore = dataStoreType.create();
		
		// initialize new data store
		try {
			newDataStore.initialize();
		} catch (Exception e) {
			plugin.getLogger().severe("Could not initialize " + newDataStore.getDisplayName() + " datastore!");
			if (plugin.debug) {
				e.printStackTrace();
			}
		}
		
		// if old data store was passed, convert to new data store
		if (oldDataStore != null) {
			convertDataStore(oldDataStore, newDataStore);
		}
		else {
			convertAll(newDataStore);
		}
		// return initialized data store
		return newDataStore;
	}


	/**
	 * convert old data store to new data store
	 * @param oldDataStore
	 * @param newDataStore
	 */
	private final static void convertDataStore(final DataStore oldDataStore, final DataStore newDataStore) {

		// if datastores are same type, do not convert
		if (oldDataStore.getType().equals(newDataStore.getType())) {
			return;
		}
		
		// if old datastore file exists, attempt to read all records
		if (oldDataStore.exists()) {
			
			plugin.getLogger().info("Converting existing " + oldDataStore.toString() + " datastore to "
					+ newDataStore.toString() + " datastore...");
			
			// initialize old datastore if necessary
			if (!oldDataStore.isInitialized()) {
				try {
					oldDataStore.initialize();
				} catch (Exception e) {
					plugin.getLogger().warning("Could not initialize " 
							+ oldDataStore.toString() + " datastore for conversion.");
					plugin.getLogger().warning(e.getLocalizedMessage());
					return;
				}
			}
			
			// convert player records
			Set<PlayerState> allPlayerRecords = new HashSet<PlayerState>();
			
			allPlayerRecords = oldDataStore.getAllPlayerRecords();
			
			int count = 0;
			for (PlayerState record : allPlayerRecords) {
				newDataStore.insertPlayerStateBlocking(record);
				count++;
			}
			plugin.getLogger().info(count + " player records converted to " + newDataStore.toString() + " datastore.");
			
			// convert claim records
			Collection<Claim> allClaimRecords = new HashSet<Claim>();
			
			allClaimRecords = oldDataStore.getAllClaims();
			count = 0;
			for (Claim record : allClaimRecords) {
				newDataStore.insertClaimBlocking(record);
				count++;
			}
			plugin.getLogger().info(count + " claim records converted to " + newDataStore.toString() + " datastore.");
			
			// convert claim group records
			Collection<ClaimGroup> allClaimGroupRecords = new HashSet<ClaimGroup>();
			
			allClaimGroupRecords = oldDataStore.getAllClaimGroups();
			count = 0;
			for (ClaimGroup record : allClaimGroupRecords) {
				newDataStore.insertClaimGroupBlocking(record);
				count++;
			}
			plugin.getLogger().info(count + " claim group records converted to " + newDataStore.toString() + " datastore.");
			
			// convert permissions records
			Collection<ClaimPermission> allPermissionRecords = new HashSet<ClaimPermission>();

			allPermissionRecords = oldDataStore.getAllClaimPermissions();
			count = 0;
			for (ClaimPermission record : allPermissionRecords) {
				newDataStore.insertClaimPermissionBlocking(record);
				count++;
			}
			newDataStore.sync();
			oldDataStore.close();
			oldDataStore.delete();
		}
	}

	
	/**
	 * convert all existing data stores to new data store
	 * @param newDataStore
	 */
	private final static void convertAll(final DataStore newDataStore) {
		
		// get array list of all data store types
		ArrayList<DataStoreType> dataStoresTypes = new ArrayList<DataStoreType>(Arrays.asList(DataStoreType.values()));
		
		// remove newDataStore from list of types to convert
		dataStoresTypes.remove(newDataStore.getType());
		
		for (DataStoreType type : dataStoresTypes) {

			// create oldDataStore holder
			DataStore oldDataStore = type.create();

			if (oldDataStore != null && oldDataStore.exists()) {
				
				convertDataStore(oldDataStore, newDataStore);
			}
		}
	}
}
