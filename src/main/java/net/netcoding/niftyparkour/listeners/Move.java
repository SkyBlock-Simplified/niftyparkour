package net.netcoding.niftyparkour.listeners;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftyparkour.cache.UserParkourData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Move extends BukkitListener {

	public Move(JavaPlugin plugin) {
		super(plugin);
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();

		if (player.getLocation().getBlockY() <= 0) {
			BukkitMojangProfile profile = NiftyBukkit.getMojangRepository().searchByPlayer(player);
			UserParkourData userData = UserParkourData.getCache(profile);
			userData.teleportToLast();
		}
	}

}