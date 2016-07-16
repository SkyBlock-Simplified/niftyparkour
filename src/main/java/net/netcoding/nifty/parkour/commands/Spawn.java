package net.netcoding.nifty.parkour.commands;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.Command;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.command.CommandSource;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.util.ListUtil;
import net.netcoding.nifty.parkour.NiftyParkour;
import net.netcoding.nifty.parkour.cache.UserParkourData;

public class Spawn extends MinecraftListener {

	public Spawn(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Command(name = "spawn",
			checkPerms = false,
			minimumArgs = 0,
			maximumArgs = 1,
			aliases = { "setspawn" },
			usages = {
					@Command.Usage(index = 0, match = "setspawn")
			}
	)
	protected void onCommand(CommandSource source, String alias, String[] args) throws Exception {
		if ("setspawn".equals(alias) && isConsole(source)) {
			this.getLog().error(source, "You cannot set the server spawn from console!");
			return;
		}

		if (ListUtil.isEmpty(args) && isConsole(source)) {
			this.getLog().error(source, "You must pass a player name!");
			return;
		}

		MinecraftMojangProfile profile = Nifty.getMojangRepository().searchByUsername(ListUtil.isEmpty(args) ? source.getName() : args[0]);

		if (!profile.isOnlineLocally()) {
			this.getLog().error(source, "Unable to locate {{0}} on this server!");
			return;
		}

		if ("setspawn".equals(alias) && this.hasPermissions(source, "setspawn")) {
			profile = Nifty.getMojangRepository().searchByUsername(source.getName());
			NiftyParkour.getPluginConfig().setSpawnPoint(profile.getOfflinePlayer().getPlayer().getLocation());
			this.getLog().message(source, "Spawn point set!");
		} else {
			UserParkourData userData = UserParkourData.getCache(profile);
			userData.teleportToSpawn();
		}
	}

}