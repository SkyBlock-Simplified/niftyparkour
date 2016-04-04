package net.netcoding.niftyparkour;

import net.netcoding.niftybukkit.minecraft.BukkitPlugin;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftybukkit.signs.SignMonitor;
import net.netcoding.niftyparkour.cache.Config;
import net.netcoding.niftyparkour.cache.Maps;
import net.netcoding.niftyparkour.commands.AdminMode;
import net.netcoding.niftyparkour.commands.Checkpoint;
import net.netcoding.niftyparkour.commands.Map;
import net.netcoding.niftyparkour.commands.Spawn;
import net.netcoding.niftyparkour.listeners.Blocks;
import net.netcoding.niftyparkour.listeners.Connections;
import net.netcoding.niftyparkour.listeners.Damage;
import net.netcoding.niftyparkour.listeners.Move;
import net.netcoding.niftyparkour.listeners.Signs;
import org.bukkit.World;

public class NiftyParkour extends BukkitPlugin {

	private static transient Config PLUGIN_CONFIG;
	private static transient Maps MAPS;
	private static transient SignMonitor SIGN_MONITOR;

	@Override
	public void onEnable() {
		this.getLog().console("Loading Config");
		MAPS = new Maps(this);
		try {
			(PLUGIN_CONFIG = new Config(this)).init();
			PLUGIN_CONFIG.startWatcher();
		} catch (Exception ex) {
			this.getLog().console("Unable to monitor config! Changes will require a restart!", ex);
		}

		this.getLog().console("Registering Gamerules");
		for (World world : this.getServer().getWorlds()) {
			world.setGameRuleValue("showDeathMessages", "false");
			world.setGameRuleValue("doEntityDrops", "false");
			world.setGameRuleValue("keepInventory", "false");
			world.setGameRuleValue("doMobLoot", "false");
			world.setGameRuleValue("doMobSpawning", "false");
		}

		this.getLog().console("Registering Commands");
		new AdminMode(this);
		new Checkpoint(this);
		new Map(this);
		new Spawn(this);

		this.getLog().console("Registering Listeners");
		new Blocks(this);
		new Connections(this);
		new Damage(this);
		new Move(this);
		SIGN_MONITOR = new SignMonitor(this);
		SIGN_MONITOR.addListener(new Signs(this), "spawn2", "warp2", "menu2", "checkpoint2");
		SIGN_MONITOR.start();
	}

	@Override
	public void onDisable() {
		SIGN_MONITOR.stop();
	}

	public static Maps getMaps() {
		return MAPS;
	}

	public static Config getPluginConfig() {
		return PLUGIN_CONFIG;
	}

	public static void sendCheckpointSignUpdate(BukkitMojangProfile profile) {
		SIGN_MONITOR.sendSignUpdate(profile, "checkpoint2");
	}

}