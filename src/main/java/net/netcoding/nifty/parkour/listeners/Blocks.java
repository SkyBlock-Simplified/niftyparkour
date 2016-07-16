package net.netcoding.nifty.parkour.listeners;

import net.netcoding.nifty.common.api.plugin.Event;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.entity.living.human.Player;
import net.netcoding.nifty.common.minecraft.event.block.BlockBreakEvent;
import net.netcoding.nifty.common.minecraft.event.block.BlockPlaceEvent;
import net.netcoding.nifty.parkour.cache.UserParkourData;

public class Blocks extends MinecraftListener {

	public Blocks(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Event(priority = Event.Priority.HIGH)
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		UserParkourData userData = UserParkourData.getCache(event.getProfile());

		if (!userData.isAdminMode()) {
			this.getLog().error(player, "You must be in admin mode to break blocks!");
			event.setCancelled(true);
		}
	}

	@Event(priority = Event.Priority.HIGH)
	public void onBlockPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		UserParkourData userData = UserParkourData.getCache(event.getProfile());

		if (!userData.isAdminMode()) {
			this.getLog().error(player, "You must be in admin mode to place blocks!");
			event.setCancelled(true);
		}
	}

}