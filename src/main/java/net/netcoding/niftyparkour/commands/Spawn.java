package net.netcoding.niftyparkour.commands;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftycore.util.ListUtil;
import net.netcoding.niftyparkour.NiftyParkour;
import net.netcoding.niftyparkour.cache.UserParkourData;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Spawn extends BukkitCommand {

	public Spawn(JavaPlugin plugin) {
		super(plugin, "spawn");
		this.setCheckPerms(false);
		this.setMinimumArgsLength(0);
		this.setMaximumArgsLength(1);
		this.editUsage(0, "setspawn", "");
	}

	@Override
	protected void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		if ("setspawn".equals(alias) && isConsole(sender)) {
			this.getLog().error(sender, "You cannot set the server spawn from console!");
			return;
		}

		if (ListUtil.isEmpty(args) && isConsole(sender)) {
			this.getLog().error(sender, "You must pass a player name!");
			return;
		}

		BukkitMojangProfile profile = NiftyBukkit.getMojangRepository().searchByUsername(ListUtil.isEmpty(args) ? sender.getName() : args[0]);

		if (!profile.isOnlineLocally()) {
			this.getLog().error(sender, "Unable to locate {{0}} on this server!");
			return;
		}

		if ("setspawn".equals(alias) && this.hasPermissions(sender, "setspawn")) {
			profile = NiftyBukkit.getMojangRepository().searchByUsername(sender.getName());
			NiftyParkour.getPluginConfig().setSpawnPoint(profile.getOfflinePlayer().getPlayer().getLocation());
			this.getLog().message(sender, "Spawn point set!");
		} else {
			UserParkourData userData = UserParkourData.getCache(profile);
			userData.teleportToSpawn();
		}
	}

}