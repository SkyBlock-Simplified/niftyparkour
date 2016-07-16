package net.netcoding.nifty.parkour.listeners;

import net.netcoding.nifty.common.api.plugin.Event;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.event.player.PlayerJoinEvent;
import net.netcoding.nifty.common.minecraft.event.player.PlayerQuitEvent;
import net.netcoding.nifty.parkour.cache.UserParkourData;

public class Connections extends MinecraftListener {

	public Connections(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Event
	public void onProfileJoin(PlayerJoinEvent event) {
		UserParkourData userData = new UserParkourData(this.getPlugin(), event.getProfile());
		userData.teleportToSpawn();
	}

	@Event
	public void onProfileQuit(PlayerQuitEvent event) {
		UserParkourData.removeCache(event.getProfile());
	}

}