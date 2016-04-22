package net.netcoding.niftyparkour.cache;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.items.ItemData;
import net.netcoding.niftybukkit.util.LocationUtil;
import net.netcoding.niftybukkit.yaml.BukkitConfig;
import net.netcoding.niftycore.yaml.annotations.Comment;
import net.netcoding.niftycore.yaml.annotations.Path;
import net.netcoding.niftycore.yaml.exceptions.InvalidConfigurationException;
import net.netcoding.niftyparkour.NiftyParkour;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public class Config extends BukkitConfig {

	public static final Location DEFAULT_SPAWN = Bukkit.getWorlds().get(0).getSpawnLocation();

	@Path("spawn-point")
	@Comment("Where players spawn server wide")
	private Location spawnPoint = DEFAULT_SPAWN;

	@Path("fire-burn-duration")
	@Comment("How long a player will burn for in seconds")
	private Integer fireBurnDuration = 3;

	@Path("item-opener")
	@Comment("Adds item to your inventory to open the chest of servers")
	private ItemData itemOpener = NiftyBukkit.getItemDatabase().get("0");

	public Config(JavaPlugin plugin) {
		super (plugin.getDataFolder(), "config");
	}

	public int getFireBurnDuration() {
		return this.fireBurnDuration + 1;
	}

	public ItemData getItemOpener() {
		return this.itemOpener;
	}

	public Location getSpawnPoint() {
		return this.spawnPoint;
	}

	@Override
	public void reload() throws InvalidConfigurationException {
		super.reload();

		if (NiftyParkour.getMenuInventory() != null) {
			NiftyParkour.getMenuInventory().closeAll();
			NiftyParkour.getMenuInventory().setTitle("Maps");
			NiftyParkour.getMenuInventory().setItemOpener(this.getItemOpener());
		}
	}

	public void setSpawnPoint(Location location) {
		location = LocationUtil.level(location); // Level
		location = LocationUtil.straighten(location); // Straight
		location = LocationUtil.center(location); // Center
		this.spawnPoint = location;
		super.save();
	}

}