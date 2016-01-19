package net.netcoding.niftyparkour.listeners;

import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftybukkit.minecraft.events.PlayerDisconnectEvent;
import net.netcoding.niftybukkit.minecraft.events.PlayerPostLoginEvent;
import net.netcoding.niftyparkour.cache.UserParkourData;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;

public class Connections extends BukkitListener {

	public Connections(JavaPlugin plugin) {
		super(plugin);
	}

	@EventHandler
	public void onPlayerPostLogin(PlayerPostLoginEvent event) {
		UserParkourData userData = new UserParkourData(this.getPlugin(), event.getProfile());
		userData.teleportToSpawn();
	}

	@EventHandler
	public void onPlayerDisconnect(PlayerDisconnectEvent event) {
		UserParkourData.removeCache(event.getProfile());
	}

}