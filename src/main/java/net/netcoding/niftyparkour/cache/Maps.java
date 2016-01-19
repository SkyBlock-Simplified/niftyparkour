package net.netcoding.niftyparkour.cache;

import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Maps extends BukkitHelper {

	private final ConcurrentHashMap<String, MapConfig> maps = new ConcurrentHashMap<>();

	public Maps(JavaPlugin plugin) {
		super(plugin);
		File pluginDirectory = plugin.getDataFolder();
		File mapsDirectory = new File(pluginDirectory, "maps");

		if (!mapsDirectory.exists())
			mapsDirectory.mkdirs();

		String[] mapNames = mapsDirectory.list();

		for (String mapName : mapNames) {
			if (mapName.endsWith(".disabled"))
				continue;

			MapConfig config = new MapConfig(this.getPlugin(), mapName);

			try {
				config.init();
			} catch (Exception ex) {
				this.getLog().console("Failed to load map file {0}, disabling map!", ex, mapName);
				File mapFile = new File(mapName);
				mapFile.renameTo(new File(mapName + ".disabled"));
				continue;
			}

			try {
				config.startWatcher();
			} catch (Exception ex) {
				this.getLog().console("Unable to monitor {0} map config! Changes will require a restart!", ex, mapName);
			}

			this.maps.put(mapName, config);
		}
	}

	public void addMap(String mapName) throws Exception {
		if (!this.maps.containsKey(mapName)) {
			MapConfig config = new MapConfig(this.getPlugin(), mapName);
			config.init();
			config.startWatcher();
			this.maps.put(mapName, config);
		}
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