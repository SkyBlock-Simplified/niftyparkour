package net.netcoding.nifty.parkour.cache;

import net.netcoding.nifty.common.api.plugin.MinecraftHelper;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.core.util.concurrent.Concurrent;
import net.netcoding.nifty.core.util.concurrent.ConcurrentMap;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class Maps extends MinecraftHelper {

	private final ConcurrentMap<String, MapConfig> maps = Concurrent.newMap();

	public Maps(MinecraftPlugin plugin) {
		super(plugin);
		File pluginDirectory = plugin.getDataFolder();
		File mapsDirectory = new File(pluginDirectory, "maps");

		if (!mapsDirectory.exists())
			mapsDirectory.mkdirs();

		String[] mapNames = mapsDirectory.list();

		for (String mapName : mapNames) {
			if (mapName.endsWith(".disabled"))
				continue;

			this.addMap(mapName);
		}
	}

	public boolean addMap(String mapName) {
		if (!this.maps.containsKey(mapName)) {
			MapConfig config = new MapConfig(this.getPlugin(), mapName);

			try {
				config.init();
			} catch (Exception ex) {
				this.getLog().console("Failed to load map file {0}, disabling map!", ex, mapName);
				config.disable();
				return false;
			}

			try {
				config.startWatcher();
			} catch (Exception ex) {
				this.getLog().console("Unable to monitor {0} map config! Changes will require a restart!", ex, mapName);
			}

			this.maps.put(config.getName(), config);
		}

		return true;
	}

	public boolean checkExists(String mapName) {
		return this.maps.containsKey(mapName);
	}

	public Map<String, MapConfig> getAllMaps() {
		return Collections.unmodifiableMap(this.maps);
	}

	public MapConfig getMap(String mapName) {
		return this.hasMap(mapName) ? this.maps.get(mapName) : null;
	}

	public boolean hasMap(String mapName) {
		return this.maps.containsKey(mapName);
	}

	public void removeMap(String mapName) {
		if (this.maps.containsKey(mapName)) {
			MapConfig config = this.maps.remove(mapName);
			config.stopWatcher();
			config.delete();

			for (UserParkourData userData : UserParkourData.getCache())
				userData.getPlayerConfig().removeCheckpoints(mapName);
		}
	}

}