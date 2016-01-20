package net.netcoding.niftyparkour.cache;

import net.netcoding.niftybukkit.mojang.BukkitMojangCache;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftycore.util.concurrent.ConcurrentSet;
import net.netcoding.niftyparkour.NiftyParkour;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;

public class UserParkourData extends BukkitMojangCache<BukkitMojangProfile> {

	private static final transient ConcurrentSet<UserParkourData> CACHE = new ConcurrentSet<>();
	private boolean adminMode = false;
	private final PlayerConfig playerConfig;
	private String lastMap = "";
	private Integer lastCheckpoint = -1;
	private Integer secondsOnFire = 0;

	public UserParkourData(JavaPlugin plugin, BukkitMojangProfile profile) {
		this(plugin, profile, true);
	}

	private UserParkourData(JavaPlugin plugin, BukkitMojangProfile profile, boolean addToCache) {
		super(plugin, profile);
		(this.playerConfig = new PlayerConfig(plugin, profile)).init();
		if (addToCache) CACHE.add(this);
	}

	public static ConcurrentSet<UserParkourData> getCache() {
		for (UserParkourData data : CACHE) {
			if (!data.isOnlineLocally())
				CACHE.remove(data);
		}

		return CACHE;
	}

	public static UserParkourData getCache(BukkitMojangProfile profile) {
		for (UserParkourData data : getCache()) {
			if (profile.equals(data.getProfile()))
				return data;
		}

		return new UserParkourData(NiftyParkour.getPlugin(NiftyParkour.class), profile, false);
	}

	public boolean isAdminMode() {
		return this.adminMode;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public Integer getSecondsOnFire() {
		return this.secondsOnFire;
	}

	public void incSecondsOnFire() {
		this.secondsOnFire++;
	}

	public static void removeCache(BukkitMojangProfile profile) {
		for (UserParkourData data : CACHE) {
			if (data.getProfile().equals(profile)) {
				data.getPlayerConfig().save();
				CACHE.remove(data);
				break;
			}
		}
	}

	public void resetSecondsOnFire() {
		this.secondsOnFire = 0;
	}

	public void teleportTo(String mapName) {
		this.teleportTo(mapName, 0);
	}

	public void teleportTo(String mapName, Integer checkpoint) {
		this.lastMap = mapName;
		this.lastCheckpoint = checkpoint;
		MapConfig map = NiftyParkour.getMaps().getMap(mapName);
		this.teleportTo(map.getCheckpoint(checkpoint));
	}

	public void teleportToLast() {
		this.teleportTo(this.lastMap, this.lastCheckpoint);
	}

	private void teleportTo(Location location) {
		Player player = this.getOfflinePlayer().getPlayer();
		this.resetSecondsOnFire();
		player.setFireTicks(0);
		player.setHealth(player.getMaxHealth());
		player.setExhaustion(0f);
		player.setFallDistance(0f);
		Collection<PotionEffect> potions = player.getActivePotionEffects();

		for (PotionEffect potion : potions)
			player.removePotionEffect(potion.getType());

		this.getOfflinePlayer().getPlayer().teleport(location);
	}

	public void teleportToSpawn() {
		this.lastMap = "";
		this.lastCheckpoint = -1;
		this.teleportTo(NiftyParkour.getPluginConfig().getSpawnPoint());
	}

	public void toggleAdminMode() {
		this.adminMode = !this.adminMode;
	}

}