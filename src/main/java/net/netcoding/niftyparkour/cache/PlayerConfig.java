package net.netcoding.niftyparkour.cache;

import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftybukkit.yaml.BukkitConfig;
import net.netcoding.niftycore.util.StringUtil;
import net.netcoding.niftycore.util.concurrent.ConcurrentList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerConfig extends BukkitConfig {

	private ConcurrentHashMap<String, ConcurrentList<Integer>> checkpoints = new ConcurrentHashMap<>();

	public PlayerConfig(JavaPlugin plugin, BukkitMojangProfile profile) {
		this(plugin, profile.getUniqueId());
	}

	PlayerConfig(JavaPlugin plugin, UUID uniqueId) {
		super(plugin.getDataFolder(), StringUtil.format("players/{0}", uniqueId));
	}

	public void addCheckpoint(String mapName, int checkpoint) {
		if (!this.checkpoints.containsKey(mapName))
			this.checkpoints.put(mapName, new ConcurrentList<>(Collections.singletonList(checkpoint)));
		else {
			ConcurrentList<Integer> checkpoints = this.checkpoints.get(mapName);

			if (!checkpoints.contains(checkpoint)) {
				checkpoints.add(checkpoint);
				Collections.sort(checkpoints);
				this.checkpoints.put(mapName, checkpoints);
			}
		}
	}

	public List<Integer> getCheckpoints(String mapName) {
		return Collections.unmodifiableList(this.checkpoints.get(mapName));
	}

	public boolean hasCheckpoint(String mapName, int checkpoint) {
		return this.checkpoints.containsKey(mapName) && this.checkpoints.get(mapName).contains(checkpoint);
	}

	public void removeCheckpoint(String mapName, int checkpoint) {
		if (this.checkpoints.containsKey(mapName))
			this.checkpoints.get(mapName).remove((Object)checkpoint);
	}

	public void removeCheckpoints(String mapName) {
		if (this.checkpoints.containsKey(mapName))
			this.checkpoints.remove(mapName);
	}

}