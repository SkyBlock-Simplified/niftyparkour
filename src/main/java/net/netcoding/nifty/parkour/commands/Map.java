package net.netcoding.nifty.parkour.commands;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.Command;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.command.CommandSource;
import net.netcoding.nifty.common.minecraft.entity.living.human.Player;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.api.color.ChatColor;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.parkour.NiftyParkour;
import net.netcoding.nifty.parkour.cache.Keys;
import net.netcoding.nifty.parkour.cache.Maps;
import net.netcoding.nifty.parkour.cache.UserParkourData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Map extends MinecraftListener {

	public Map(MinecraftPlugin plugin) {
		super(plugin);
	}

	public static String parseMapName(String[] args, int index) {
		String mapName = StringUtil.implode(args, index).replace("_", " ");
		return (mapName.length() > 15 ? mapName.substring(0, 15) : mapName);
	}

	@Command(name = "map",
			playerOnly = true,
			usages = {
					@Command.Usage(match = "add|(un)?lock|rem(ove)?|setspawn", replace = "<map>"),
					@Command.Usage(match = "list|spawn")
			}
	)
	protected void onCommand(CommandSource source, String alias, String[] args) throws Exception {
		final String action = args[0];

		if ("list".equals(action)) {
			MinecraftMojangProfile profile = Nifty.getMojangRepository().searchByUsername(source.getName());
			UserParkourData userData = UserParkourData.getCache(profile);
			Set<String> mapNames = NiftyParkour.getMaps().getAllMaps().keySet();
			List<String> mapList = new ArrayList<>();

			if (mapNames.size() > 0) {
				for (String mapName : mapNames) {
					boolean unlocked = userData.isAdminMode() || !NiftyParkour.getMaps().getMap(mapName).isLocked();
					mapList.add(StringUtil.format("{0}{1}", (unlocked ? ChatColor.GREEN : ChatColor.RED), mapName));
				}
			} else
				mapList.add(StringUtil.format("{{0}}", "No maps available!"));

			this.getLog().message(source, "Maps: {0}", StringUtil.implode(ChatColor.GRAY + ", ", mapList));
		} else if (action.matches("^((un)?lock|add|create|setspawn|(remov|delet)e)$")) {
			if ("lock".equals(action)) {
				if (!this.hasPermissions(source, "map", "lock")) {
					this.getLog().error(source, "You do not have permission to lock maps!");
					return;
				}
			} else {
				if (!this.hasPermissions(source, "map", "manage")) {
					this.getLog().error(source, "You do not have permission to manage maps!");
					return;
				}
			}

			if (args.length < 2) {
				this.showUsage(source);
				return;
			}

			MinecraftMojangProfile profile = Nifty.getMojangRepository().searchByUsername(source.getName());
			UserParkourData userData = UserParkourData.getCache(profile);
			String mapName = parseMapName(args, 1);
			Maps maps = NiftyParkour.getMaps();

			if (!userData.isAdminMode()) {
				this.getLog().error(source, "You must be in admin mode to manage maps!");
				return;
			}

			if (!action.matches("^(add|create)$") && !maps.checkExists(mapName)) {
				this.getLog().error(source, "The map {{0}} does not exist, create it first!", mapName);
				return;
			}

			if (action.matches("^(un)?lock$")) {
				maps.getMap(mapName).setLocked("lock".equals(action));
				this.getLog().message(source, "The map {{0}} has been {{1}ed}!", mapName, action);

				if (maps.getMap(mapName).isLocked()) {
					UserParkourData.getCache().stream().filter(playerData -> mapName.equalsIgnoreCase(playerData.getLastMap())).forEach(playerData -> {
						Player player = playerData.getOfflinePlayer().getPlayer();
						player.closeInventory();
						this.getLog().message(player, "{{0}} has just been locked!", mapName);
						playerData.teleportToSpawn();
					});
				}

				// TODO: Update open inventories
			} else if (action.matches("^(add|create)$")) {
				if (maps.checkExists(mapName)) {
					this.getLog().error(source, "Unable to create {{0}}, it already exists!", mapName);
					return;
				}

				if (maps.addMap(mapName)) {
					for (MinecraftMojangProfile other : Nifty.getBungeeHelper().getPlayerList()) {
						NiftyParkour.sendSignUpdate(other, Keys.WARP);
						NiftyParkour.sendSignUpdate(other, Keys.CHECKPOINT);
					}

					this.getLog().message(source, "The map {{0}} has been created!", mapName);
				} else
					this.getLog().error(source, "Something went wrong when creating map {{0}}!", mapName);
			} else if (action.matches("^(remov|delet)e$")) {
				// TODO: Request confirmation
				//NiftyParkour.getMaps().removeMap(mapName);
				//this.getLog().message(source, "The map {{0}} has been removed!");
				this.getLog().message(source, "This feature is currently disabled! Please delete manually!");
			} else if ("setspawn".equals(action)) {
				NiftyParkour.getMaps().getMap(mapName).setSpawnPoint(profile.getOfflinePlayer().getPlayer().getLocation());
				this.getLog().message(source, "The spawn point for {{0}} has been set!", mapName);
			}

			maps.getMap(mapName).save();
		} else
			this.showUsage(source);
	}

}