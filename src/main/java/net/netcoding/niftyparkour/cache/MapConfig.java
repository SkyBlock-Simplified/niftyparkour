package net.netcoding.niftyparkour.cache;

import net.netcoding.niftybukkit.util.LocationUtil;
import net.netcoding.niftybukkit.yaml.BukkitConfig;
import net.netcoding.niftycore.minecraft.scheduler.MinecraftScheduler;
import net.netcoding.niftycore.util.StringUtil;
import net.netcoding.niftycore.util.concurrent.ConcurrentList;
import net.netcoding.niftycore.yaml.annotations.Path;
import net.netcoding.niftyparkour.NiftyParkour;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class MapConfig extends BukkitConfig {

	@Path("settings.locked")
	private boolean locked = true;

	@Path("settings.spawn-point")
	private Location spawnPoint;

	private transient volatile boolean updating = false;

	private ConcurrentList<Location> checkpoints = new ConcurrentList<>();

	public MapConfig(JavaPlugin plugin, String name) {
		super(plugin.getDataFolder(), StringUtil.format("maps/{0}", name));
	}

	public void addCheckpoint(Location location) {
		location = LocationUtil.level(location); // Level
		location = LocationUtil.straighten(location); // Straight
		location = LocationUtil.center(location); // Center
		this.checkpoints.add(location);
	}

	public Location getCheckpoint(int checkpoint) {
		return this.checkpoints.get(checkpoint);
	}

	public List<Location> getCheckpoints() {
		return Collections.unmodifiableList(this.checkpoints);
	}

	private HashSet<Block> getNearestBlocks(Location location, int radius, Material... materials) {
		final World world = location.getWorld();
		HashSet<Material> materialList = new HashSet<>(Arrays.asList(materials));
		HashSet<Block> blocks = new HashSet<>();

		for (int y = 1; y > -radius; y--) {
			for (int x = 1; x > -radius; x--) {
				for (int z = 1; z > -radius; z--) {
					int cy = location.getBlockY() + y;

					if (cy < 0 || cy > world.getMaxHeight())
						continue;

					Block scan = world.getBlockAt(location.getBlockX() + x, cy, location.getBlockZ() + z);

					if (materialList.contains(scan.getType()))
						blocks.add(scan);
				}
			}
		}

		return blocks;
	}

	public Location getSpawnPoint() {
		return spawnPoint;
	}

	public boolean hasCheckpoint(int checkpoint) {
		return checkpoint < this.checkpoints.size();
	}

	public boolean isLocked() {
		return this.locked;
	}

	public boolean isUpdating() {
		return this.updating;
	}

	public void moveCheckpoint(final int checkpoint, final int newCheckpoint) {
		if (this.isUpdating()) return;
		if (checkpoint == newCheckpoint) return;
		if (checkpoint < 0 || checkpoint > this.checkpoints.size()) return;
		this.updating = true;
		Location removed = this.checkpoints.remove(checkpoint);
		this.checkpoints.add(newCheckpoint - 1, removed);
		this.sendPlayerUpdate(checkpoint, newCheckpoint);
	}

	public void removeCheckpoint(int checkpoint) {
		if (this.isUpdating()) return;
		this.checkpoints.remove(checkpoint);
		int total = this.checkpoints.size();

		if (checkpoint < total)
			this.sendPlayerUpdate(checkpoint, total);
	}

	private void sendPlayerUpdate(final int checkpoint, final int newCheckpoint) {
		// Players
		MinecraftScheduler.runAsync(new Runnable() {
			@Override
			public void run() {
				JavaPlugin plugin = NiftyParkour.getPlugin(NiftyParkour.class);
				File pluginDirectory = plugin.getDataFolder();
				File mapsDirectory = new File(pluginDirectory, "players");
				String[] playerConfigs = mapsDirectory.list();
				String mapName = getName();

				// Update File Cache
				for (String playerUUID : playerConfigs) {
					PlayerConfig playerConfig = new PlayerConfig(plugin, UUID.fromString(playerUUID));
					playerConfig.init();

					if (checkpoint < newCheckpoint)
						playerConfig.removeCheckpoint(mapName, playerConfig.getCheckpoints().size());
					else
						playerConfig.addCheckpoint(mapName, playerConfig.getCheckpoints().size() + 1);

					playerConfig.save();
				}

				// Update Online Cache
				for (UserParkourData userData : UserParkourData.getCache())
					userData.getPlayerConfig().reload();

				// Signs
				MinecraftScheduler.schedule(new Runnable() {
					@Override
					public void run() {
						if (checkpoint < newCheckpoint) {
							for (int i = checkpoint; i < newCheckpoint; i++)
								sendSignMoveUpdate(checkpoint, 1);
						} else {
							for (int i = checkpoint; i > newCheckpoint; i--)
								sendSignMoveUpdate(checkpoint, -1);
						}

						for (World world : Bukkit.getWorlds())
							world.save();

						save();
						updating = false;
					}
				});
			}
		}, 0);
	}

	private void sendSignMoveUpdate(int checkpoint, int add) {
		Location location = this.checkpoints.get(checkpoint);
		Chunk chunk = location.getChunk();

		/*if (!chunk.isLoaded()) {
			if (!chunk.load(false)) {
				NiftyParkour.getPlugin(NiftyParkour.class).getLog().error(Bukkit.getConsoleSender(), "Unable to load chunk at checkpoint {0}!", checkpoint);
				return;
			}
		}*/

		HashSet<Block> blocks = this.getNearestBlocks(location, 7, Material.SIGN, Material.SIGN_POST, Material.WALL_SIGN);

		for (Block block : blocks) {
			Sign sign = (Sign)block.getState();

			if ("[checkpoint2]".equalsIgnoreCase(sign.getLine(0))) {
				Integer current = Integer.parseInt(sign.getLine(2));
				sign.setLine(2, ("" + current + add));
			}
		}

		chunk.unload(true, true);
	}

	public void setLocked(boolean value) {
		this.locked = value;
	}

	public void setSpawnPoint(Location spawnPoint) {
		this.spawnPoint = spawnPoint;
	}

}