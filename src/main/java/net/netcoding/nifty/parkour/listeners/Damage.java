package net.netcoding.nifty.parkour.listeners;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.Event;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.entity.living.human.Player;
import net.netcoding.nifty.common.minecraft.event.entity.EntityDamageEvent;
import net.netcoding.nifty.common.minecraft.event.player.PlayerDeathEvent;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.parkour.NiftyParkour;
import net.netcoding.nifty.parkour.cache.UserParkourData;

public class Damage extends MinecraftListener {

	public Damage(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Event
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player)event.getEntity();
			MinecraftMojangProfile profile = Nifty.getMojangRepository().searchByPlayer(player);
			UserParkourData userData = UserParkourData.getCache(profile);

			if (EntityDamageEvent.Damage.Cause.VOID == event.getCause()) {
				event.setCancelled(true);
				event.setDamage(0D);
				userData.teleportToLast();
			} else {
				if (EntityDamageEvent.Damage.Cause.FIRE_TICK == event.getCause()) {
					userData.incSecondsOnFire();

					if (userData.getSecondsOnFire() >= NiftyParkour.getPluginConfig().getFireBurnDuration()) {
						event.setCancelled(true);
						player.setFireTicks(0);
						userData.resetSecondsOnFire();
					}
				}

				if (event.getFinalDamage() >= player.getHealth()) {
					event.setCancelled(true);
					event.setDamage(0D);
					userData.teleportToLast();
				}
			}
		}
	}

	@Event
	public void onPlayerDeath(PlayerDeathEvent event) {
		event.setDeathMessage(null);
		event.setDroppedExperience(0);
	}

}