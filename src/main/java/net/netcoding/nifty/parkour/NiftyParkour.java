package net.netcoding.nifty.parkour;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.inventory.FakeInventory;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.api.signs.SignMonitor;
import net.netcoding.nifty.common.minecraft.region.World;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.api.plugin.PluginDescription;
import net.netcoding.nifty.parkour.cache.Config;
import net.netcoding.nifty.parkour.cache.Keys;
import net.netcoding.nifty.parkour.cache.Maps;
import net.netcoding.nifty.parkour.commands.AdminMode;
import net.netcoding.nifty.parkour.commands.Checkpoint;
import net.netcoding.nifty.parkour.commands.Map;
import net.netcoding.nifty.parkour.commands.Spawn;
import net.netcoding.nifty.parkour.listeners.Blocks;
import net.netcoding.nifty.parkour.listeners.Connections;
import net.netcoding.nifty.parkour.listeners.Damage;
import net.netcoding.nifty.parkour.listeners.Menus;
import net.netcoding.nifty.parkour.listeners.Signs;

public class NiftyParkour extends MinecraftPlugin {

	private PluginDescription description;
	private static transient Config PLUGIN_CONFIG;
	private static transient Maps MAPS;
	private static transient SignMonitor SIGN_MONITOR;
	private static transient FakeInventory MENU_INVENTORY;

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
		for (World world : Nifty.getServer().getWorlds()) {
			world.setGameRuleValue("showDeathMessages", "false");
			world.setGameRuleValue("doEntityDrops", "false");
			world.setGameRuleValue("keepInventory", "true");
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
		SIGN_MONITOR = new SignMonitor(this);
		SIGN_MONITOR.addListener(new Signs(this), Keys.SPAWN.toString(), Keys.WARP.toString(), Keys.MENU.toString(), Keys.CHECKPOINT.toString());
		SIGN_MONITOR.start();
		MENU_INVENTORY = new FakeInventory(this, new Menus(this));
		MENU_INVENTORY.setTitle("Maps");
		MENU_INVENTORY.setItemOpener(getPluginConfig().getItemOpener());
	}

	@Override
	public void onDisable() {
		SIGN_MONITOR.stop();
	}

	public static Maps getMaps() {
		return MAPS;
	}

	public static FakeInventory getMenuInventory() {
		return MENU_INVENTORY;
	}

	public static Config getPluginConfig() {
		return PLUGIN_CONFIG;
	}

	public static void sendSignUpdate(MinecraftMojangProfile profile, Keys key) {
		SIGN_MONITOR.sendSignUpdate(profile, key.toString());
	}

}