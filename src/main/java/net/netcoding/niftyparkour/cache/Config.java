package net.netcoding.niftyparkour.cache;

import net.netcoding.niftybukkit.util.LocationUtil;
import net.netcoding.niftybukkit.yaml.BukkitConfig;
import net.netcoding.niftycore.yaml.annotations.Path;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public class Config extends BukkitConfig {

	public static final Location DEFAULT_SPAWN = Bukkit.getWorlds().get(0).getSpawnLocation();

	@Path("spawn-point")
	private Location spawnPoint = DEFAULT_SPAWN;

	@Path("fire-burn-duration")
	private Integer fireBurnDuration = 3;

	public Config(JavaPlugin plugin) {
		super (plugin.getDataFolder(), "config.yml");
	}

	public int getFireBurnDuration() {
		return this.fireBurnDuration + 1;
	}

	public Location getSpawnPoint() {
		return this.spawnPoint;
	}

	public void setSpawnPoint(Location location) {
		location = LocationUtil.level(location); // Level
		location = LocationUtil.straighten(location); // Straight
		location = LocationUtil.center(location); // Center
		this.spawnPoint = location;
		super.save();
	}

}