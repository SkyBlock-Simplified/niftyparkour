package net.netcoding.nifty.parkour.commands;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.Command;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.command.CommandSource;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.util.ListUtil;
import net.netcoding.nifty.parkour.NiftyParkour;
import net.netcoding.nifty.parkour.cache.Keys;
import net.netcoding.nifty.parkour.cache.UserParkourData;

public class AdminMode extends MinecraftListener {

	public AdminMode(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Command(name = "adminmode",
			minimumArgs = 0,
			maximumArgs = 1
	)
	protected void onCommand(CommandSource source, String alias, String[] args) throws Exception {
		if (ListUtil.isEmpty(args) && isConsole(source)) {
			this.getLog().error(source, "You must pass a player name!");
			return;
		}

		MinecraftMojangProfile profile = Nifty.getMojangRepository().searchByUsername(ListUtil.isEmpty(args) ? source.getName() : args[0]);

		if (!profile.getName().equals(source.getName()) && !this.hasPermissions(source, "adminmode", "others")) {
			this.getLog().error(source, "You do not have permission to set others to admin mode!");
			return;
		}

		if (!profile.isOnlineLocally()) {
			this.getLog().error(source, "Unable to locate {{0}} on this server!");
			return;
		}

		UserParkourData userData = UserParkourData.getCache(profile);
		userData.toggleAdminMode();
		this.getLog().message(source, "{{0}} is no{1} in admin mode.", profile.getName(), (userData.isAdminMode() ? "w" : " longer"));
		NiftyParkour.sendSignUpdate(profile, Keys.CHECKPOINT);
	}

}