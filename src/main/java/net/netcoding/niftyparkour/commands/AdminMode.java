package net.netcoding.niftyparkour.commands;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftycore.util.ListUtil;
import net.netcoding.niftyparkour.NiftyParkour;
import net.netcoding.niftyparkour.cache.Keys;
import net.netcoding.niftyparkour.cache.UserParkourData;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class AdminMode extends BukkitCommand {

	public AdminMode(JavaPlugin plugin) {
		super(plugin, "adminmode");
		this.setMinimumArgsLength(0);
		this.setMaximumArgsLength(1);
	}

	@Override
	protected void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		if (ListUtil.isEmpty(args) && isConsole(sender)) {
			this.getLog().error(sender, "You must pass a player name!");
			return;
		}

		BukkitMojangProfile profile = NiftyBukkit.getMojangRepository().searchByUsername(ListUtil.isEmpty(args) ? sender.getName() : args[0]);

		if (!profile.getName().equals(sender.getName()) && !this.hasPermissions(sender, "adminmode", "others")) {
			this.getLog().error(sender, "You do not have permission to set others to admin mode!");
			return;
		}

		if (!profile.isOnlineLocally()) {
			this.getLog().error(sender, "Unable to locate {{0}} on this server!");
			return;
		}

		UserParkourData userData = UserParkourData.getCache(profile);
		userData.toggleAdminMode();
		this.getLog().message(sender, "{{0}} is no{1} in admin mode.", profile.getName(), (userData.isAdminMode() ? "w" : " longer"));
		NiftyParkour.sendSignUpdate(profile, Keys.CHECKPOINT);
	}

}