package net.netcoding.nifty.parkour.cache;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.inventory.item.ItemStack;
import net.netcoding.nifty.common.minecraft.region.Location;
import net.netcoding.nifty.common.util.LocationUtil;
import net.netcoding.nifty.common.yaml.BukkitConfig;
import net.netcoding.nifty.core.yaml.annotations.Comment;
import net.netcoding.nifty.core.yaml.annotations.Path;
import net.netcoding.nifty.core.yaml.exceptions.InvalidConfigurationException;
import net.netcoding.nifty.parkour.NiftyParkour;

public class Config extends BukkitConfig {

	public static final Location DEFAULT_SPAWN = Nifty.getServer().getWorlds().get(0).getSpawnLocation();

	@Path("spawn-point")
	@Comment("Where players spawn server wide")
	private Location spawnPoint = DEFAULT_SPAWN;

	@Path("fire-burn-duration")
	@Comment("How long a player will burn for in seconds")
	private Integer fireBurnDuration = 3;

	@Path("item-opener")
	@Comment("Adds item to your inventory to open the chest of servers")
	private ItemStack itemOpener = Nifty.getItemDatabase().get("0");

	public Config(MinecraftPlugin plugin) {
		super (plugin.getDataFolder(), "config");
	}

	public int getFireBurnDuration() {
		return this.fireBurnDuration + 1;
	}

	public ItemStack getItemOpener() {
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