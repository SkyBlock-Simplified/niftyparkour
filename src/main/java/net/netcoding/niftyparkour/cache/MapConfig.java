package net.netcoding.niftyparkour.cache;

import net.netcoding.niftybukkit.util.LocationUtil;
import net.netcoding.niftybukkit.yaml.BukkitConfig;
import net.netcoding.niftycore.minecraft.scheduler.MinecraftScheduler;
import net.netcoding.niftycore.util.StringUtil;
import net.netcoding.niftycore.util.concurrent.ConcurrentList;
import net.netcoding.niftycore.util.concurrent.ConcurrentSet;
import net.netcoding.niftycore.yaml.annotations.Path;
import net.netcoding.niftycore.yaml.exceptions.InvalidConfigurationException;
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
import java.util.Set;
import java.util.UUID;

public class MapConfig extends BukkitConfig {

	private transient volatile boolean updating = false;
	private transient ConcurrentSet<Location> updatedSigns = new ConcurrentSet<>();

	@Path("locked")
	private boolean locked = true;

	@Path("spawn-point")
	private Location spawnPoint = Config.DEFAULT_SPAWN;

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

	void disable() {
		File mapFile = new File(this.getParentDirectory(), this.getFullName());
		mapFile.renameTo(new File(this.getParentDirectory(), this.getFullName() + ".disabled"));
	}

	public Location getCheckpoint(int checkpoint) {
		return this.checkpoints.get(checkpoint - 1);
	}

	public List<Location> getCheckpoints() {
		return Collections.unmodifiableList(this.checkpoints);
	}

	private static Set<Block> getNearestBlocks(Location location, int radius, Material... materials) {
		final World world = location.getWorld();
		Set<Material> materialList = new HashSet<>(Arrays.asList(materials));
		Set<Block> blocks = new HashSet<>();

		for (int y = radius; y > -radius; y--) {
			for (int x = radius; x > -radius; x--) {
				for (int z = radius; z > -radius; z--) {
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
		return this.spawnPoint;
	}

	public boolean hasCheckpoint(int checkpoint) {
		return checkpoint > 0 && checkpoint <= this.checkpoints.size();
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
		if (checkpoint < 1 || checkpoint > this.checkpoints.size()) return;
		if (newCheckpoint < 1 || newCheckpoint > this.checkpoints.size()) return;
		Location removed = this.checkpoints.remove(checkpoint - 1);
		this.checkpoints.add(newCheckpoint - 1, removed);
		this.sendPlayerUpdate(checkpoint, newCheckpoint, false);
	}

	public void removeCheckpoint(int checkpoint) {
		if (this.isUpdating()) return;
		if (checkpoint < 1 || checkpoint > this.checkpoints.size()) return;
		this.checkpoints.remove(checkpoint - 1);
		this.sendPlayerUpdate(checkpoint, this.checkpoints.size(), true);
	}

	private void sendPlayerUpdate(final int checkpoint, final int newCheckpoint, final boolean delete) {
		this.updating = true;

		// Players
		MinecraftScheduler.runAsync(new Runnable() {
			@Override
			public void run() {
				JavaPlugin plugin = NiftyParkour.getPlugin(NiftyParkour.class);
				File pluginDirectory = plugin.getDataFolder();
				File mapsDirectory = new File(pluginDirectory, "players");
				String[] playerConfigs = mapsDirectory.list();
				String mapName = getName();
				int minimum = Math.min(checkpoint, newCheckpoint);
				int maximum = Math.max(checkpoint, newCheckpoint);

				// Update File Cache
				for (String playerUUID : playerConfigs) {
					PlayerConfig playerConfig = new PlayerConfig(plugin, UUID.fromString(playerUUID.replace(".yml", "")));

					try {
						playerConfig.init();
						List<Integer> checkpoints = playerConfig.getCheckpoints(mapName);
						boolean needsUpdate = false;
						int pMaximum = 0;

						for (Integer pCheckpoint : checkpoints) {
							pMaximum = Math.max(pMaximum, pCheckpoint);

							if (pCheckpoint >= minimum)
								needsUpdate = true;
						}

						if (needsUpdate && pMaximum < maximum) {
							if (checkpoint <= newCheckpoint)
								playerConfig.removeCheckpoint(mapName, pMaximum);
							else if (checkpoint > newCheckpoint)
								playerConfig.addCheckpoint(mapName, pMaximum + 1);

							playerConfig.save();
						}
					} catch (InvalidConfigurationException icex) {
						NiftyParkour.getPlugin(NiftyParkour.class).getLog().console("Unable to load player configuration file {0} for modification!", playerConfig.getFullName());
					}
				}

				// Update Online Cache
				for (UserParkourData userData : UserParkourData.getCache())
					userData.getPlayerConfig().reload();

				// Signs
				MinecraftScheduler.schedule(new Runnable() {
					@Override
					public void run() {
						// Update Signs (Beware: Squirly Shit)
						sendSignMoveUpdate(checkpoint, (newCheckpoint - checkpoint), delete);

						if (checkpoint <= newCheckpoint) {
							for (int i = checkpoint + 1; i <= newCheckpoint; i++)
								sendSignMoveUpdate(i, -1, delete);
						} else {
							for (int i = checkpoint - 1; i > newCheckpoint; i--)
								sendSignMoveUpdate(i, 1, delete);
						}

						// Save Worlds
						for (World world : Bukkit.getWorlds())
							world.save();

						// Update Online Players
						for (UserParkourData userData : UserParkourData.getCache())
							NiftyParkour.sendCheckpointSignUpdate(userData.getProfile());

						// Clearout
						save();
						updatedSigns.clear();
						updating = false;
					}
				});
			}
		});
	}

	private void sendSignMoveUpdate(int checkpoint, int add, boolean delete) {
		Location location = this.checkpoints.get(checkpoint - 1);
		Chunk chunk = location.getChunk();

		/*if (!chunk.isLoaded()) {
			if (!chunk.load(false)) {
				NiftyParkour.getPlugin(NiftyParkour.class).getLog().console("Unable to load chunk at checkpoint {0}!", checkpoint);
				return;
			}
		}*/

		Set<Block> blocks = getNearestBlocks(location, 10, Material.SIGN_POST, Material.WALL_SIGN);

		for (Block block : blocks) {
			Sign sign = (Sign)block.getState();
			Location signLocation = sign.getLocation();

			if (!this.updatedSigns.contains(signLocation)) {
				if (StringUtil.format("[{0}]", Keys.CHECKPOINT).equalsIgnoreCase(sign.getLine(0))) {
					Integer current = Integer.parseInt(sign.getLine(2));

					if (current == checkpoint) {
						if (delete)
							block.setType(Material.AIR);
						else
							sign.setLine(2, ("" + current + add));

						this.updatedSigns.add(signLocation);
					}
				}
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