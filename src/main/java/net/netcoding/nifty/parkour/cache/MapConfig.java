package net.netcoding.nifty.parkour.cache;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.block.Block;
import net.netcoding.nifty.common.minecraft.block.state.Sign;
import net.netcoding.nifty.common.minecraft.material.Material;
import net.netcoding.nifty.common.minecraft.region.Chunk;
import net.netcoding.nifty.common.minecraft.region.Location;
import net.netcoding.nifty.common.minecraft.region.World;
import net.netcoding.nifty.common.util.LocationUtil;
import net.netcoding.nifty.common.yaml.BukkitConfig;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.core.util.concurrent.Concurrent;
import net.netcoding.nifty.core.util.concurrent.ConcurrentList;
import net.netcoding.nifty.core.util.concurrent.ConcurrentSet;
import net.netcoding.nifty.core.yaml.annotations.Path;
import net.netcoding.nifty.core.yaml.exceptions.InvalidConfigurationException;
import net.netcoding.nifty.parkour.NiftyParkour;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MapConfig extends BukkitConfig {

	private transient volatile boolean updating = false;
	private transient ConcurrentSet<Location> updatedSigns = Concurrent.newSet();

	@Path("locked")
	private boolean locked = true;

	@Path("spawn-point")
	private Location spawnPoint = Config.DEFAULT_SPAWN;

	private ConcurrentList<Location> checkpoints = Concurrent.newList();

	public MapConfig(MinecraftPlugin plugin, String name) {
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
		Nifty.getScheduler().runAsync(() -> {
			File pluginDirectory = MapConfig.this.getParentDirectory();
			File mapsDirectory = new File(pluginDirectory, "players");
			String[] playerConfigs = mapsDirectory.list();
			String mapName = getName();
			int minimum = Math.min(checkpoint, newCheckpoint);
			int maximum = Math.max(checkpoint, newCheckpoint);

			// Update File Cache
			for (String playerUUID : playerConfigs) {
				PlayerConfig playerConfig = new PlayerConfig(NiftyParkour.getPlugin(NiftyParkour.class), UUID.fromString(playerUUID.replace(".yml", "")));

				try {
					playerConfig.init();
					List<Integer> checkpoints1 = playerConfig.getCheckpoints(mapName);
					boolean needsUpdate = false;
					int pMaximum = 0;

					for (Integer pCheckpoint : checkpoints1) {
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
			Nifty.getScheduler().schedule(() -> {
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
				Nifty.getServer().getWorlds().forEach(World::save);

				// Update Online Players
				for (UserParkourData userData : UserParkourData.getCache())
					NiftyParkour.sendSignUpdate(userData.getProfile(), Keys.CHECKPOINT);

				// Clearout
				save();
				updatedSigns.clear();
				updating = false;
			});
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

		chunk.unload();
	}

	public void setLocked(boolean value) {
		this.locked = value;
	}

	public void setSpawnPoint(Location spawnPoint) {
		this.spawnPoint = spawnPoint;
	}

}