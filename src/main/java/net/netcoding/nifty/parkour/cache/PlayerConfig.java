package net.netcoding.nifty.parkour.cache;

import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.common.yaml.BukkitConfig;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.core.util.concurrent.Concurrent;
import net.netcoding.nifty.core.util.concurrent.ConcurrentList;
import net.netcoding.nifty.core.util.concurrent.ConcurrentMap;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PlayerConfig extends BukkitConfig {

	private ConcurrentMap<String, ConcurrentList<Integer>> checkpoints = Concurrent.newMap();

	public PlayerConfig(MinecraftPlugin plugin, MinecraftMojangProfile profile) {
		this(plugin, profile.getUniqueId());
	}

	PlayerConfig(MinecraftPlugin plugin, UUID uniqueId) {
		super(plugin.getDataFolder(), StringUtil.format("players/{0}", uniqueId));
	}

	public void addCheckpoint(String mapName, int checkpoint) {
		if (!this.checkpoints.containsKey(mapName))
			this.checkpoints.put(mapName, Concurrent.newList(checkpoint));
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