package net.netcoding.niftyparkour.listeners;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftyparkour.cache.UserParkourData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Blocks extends BukkitListener {

	public Blocks(JavaPlugin plugin) {
		super(plugin);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		BukkitMojangProfile profile = NiftyBukkit.getMojangRepository().searchByPlayer(player);
		UserParkourData userData = UserParkourData.getCache(profile);

		if (!userData.isAdminMode()) {
			this.getLog().error(player, "You must be in admin mode to break blocks!");
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onBlockPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		BukkitMojangProfile profile = NiftyBukkit.getMojangRepository().searchByPlayer(player);
		UserParkourData userData = UserParkourData.getCache(profile);

		if (!userData.isAdminMode()) {
			this.getLog().error(player, "You must be in admin mode to place blocks!");
			event.setCancelled(true);
		}
	}

}