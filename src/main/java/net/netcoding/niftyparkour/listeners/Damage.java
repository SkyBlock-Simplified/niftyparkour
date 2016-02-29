package net.netcoding.niftyparkour.listeners;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitListener;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftyparkour.NiftyParkour;
import net.netcoding.niftyparkour.cache.UserParkourData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Damage extends BukkitListener {

	public Damage(JavaPlugin plugin) {
		super(plugin);
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player)event.getEntity();
			BukkitMojangProfile profile = NiftyBukkit.getMojangRepository().searchByPlayer(player);
			UserParkourData userData = UserParkourData.getCache(profile);

			if (DamageCause.VOID == event.getCause()) {
				event.setCancelled(true);
				event.setDamage(0D);
				userData.teleportToLast();
			} else {
				if (DamageCause.FIRE_TICK == event.getCause()) {
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

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		event.setDeathMessage(null);
		event.setDroppedExp(0);
	}

}