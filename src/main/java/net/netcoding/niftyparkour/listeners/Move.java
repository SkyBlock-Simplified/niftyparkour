package net.netcoding.niftyparkour.listeners;

import net.netcoding.niftybukkit.minecraft.BukkitListener;
import org.bukkit.plugin.java.JavaPlugin;

public class Move extends BukkitListener {

	public Move(JavaPlugin plugin) {
		super(plugin);
	}

	/*@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();

		if (player.getLocation().getBlockY() <= 0) {
			BukkitMojangProfile profile = NiftyBukkit.getMojangRepository().searchByPlayer(player);
			UserParkourData userData = UserParkourData.getCache(profile);
			userData.teleportToLast();
		}
	}*/

}