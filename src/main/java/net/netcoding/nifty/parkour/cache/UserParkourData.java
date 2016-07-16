package net.netcoding.nifty.parkour.cache;

import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.entity.living.human.Player;
import net.netcoding.nifty.common.minecraft.region.Location;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.common.mojang.MinecraftMojangCache;
import net.netcoding.nifty.core.util.concurrent.Concurrent;
import net.netcoding.nifty.core.util.concurrent.ConcurrentSet;
import net.netcoding.nifty.parkour.NiftyParkour;

public class UserParkourData extends MinecraftMojangCache<MinecraftMojangProfile> {

	private static final transient ConcurrentSet<UserParkourData> CACHE = Concurrent.newSet();
	private boolean adminMode = false;
	private final PlayerConfig playerConfig;
	private String lastMap = "";
	private Integer lastCheckpoint = -1;
	private Integer secondsOnFire = 0;

	public UserParkourData(MinecraftPlugin plugin, MinecraftMojangProfile profile) {
		this(plugin, profile, true);
	}

	private UserParkourData(MinecraftPlugin plugin, MinecraftMojangProfile profile, boolean addToCache) {
		super(plugin, profile);
		(this.playerConfig = new PlayerConfig(plugin, profile)).init();

		if (addToCache)
			CACHE.add(this);
	}

	public static ConcurrentSet<UserParkourData> getCache() {
		CACHE.stream().filter(data -> !data.isOnlineLocally()).forEach(CACHE::remove);
		return CACHE;
	}

	public static UserParkourData getCache(MinecraftMojangProfile profile) {
		for (UserParkourData data : getCache()) {
			if (profile.equals(data.getProfile()))
				return data;
		}

		return new UserParkourData(NiftyParkour.getPlugin(NiftyParkour.class), profile, false);
	}

	public String getLastMap() {
		return this.lastMap;
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

	public static void removeCache(MinecraftMojangProfile profile) {
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

		if (checkpoint < 1)
			this.teleportTo(map.getSpawnPoint());
		else
			this.teleportTo(map.getCheckpoint(checkpoint));
	}

	public void teleportToLast() {
		if (this.lastCheckpoint >= 0)
			this.teleportTo(this.getLastMap(), this.lastCheckpoint);
		else
			this.teleportToSpawn();
	}

	private void teleportTo(Location location) {
		if (this.isOnlineLocally()) {
			Player player = this.getOfflinePlayer().getPlayer();
			this.resetSecondsOnFire();
			player.setFireTicks(0);
			player.setHealth(player.getMaxHealth());
			player.setExhaustion(0f);
			player.setFallDistance(0f);
			player.getActivePotionEffects().stream().forEach(potion -> player.removePotionEffect(potion.getType()));
			player.teleport(location);
		}
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