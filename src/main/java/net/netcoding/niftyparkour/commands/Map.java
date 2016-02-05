package net.netcoding.niftyparkour.commands;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftycore.minecraft.ChatColor;
import net.netcoding.niftycore.util.StringUtil;
import net.netcoding.niftyparkour.NiftyParkour;
import net.netcoding.niftyparkour.cache.Maps;
import net.netcoding.niftyparkour.cache.UserParkourData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Map extends BukkitCommand {

	public Map(JavaPlugin plugin) {
		super(plugin, "map");
		this.setPlayerOnly();
		this.editUsage(1, "list", "");
	}

	public static String parseMapName(String[] args, int index) {
		String mapName = StringUtil.implode(args, index).replace("_", " ");
		return (mapName.length() > 15 ? mapName.substring(0, 15) : mapName);
	}

	@Override
	protected void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		final String action = args[0];

		if ("list".equals(action)) {
			BukkitMojangProfile profile = NiftyBukkit.getMojangRepository().searchByUsername(sender.getName());
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

			this.getLog().message(sender, "Maps: {0}", StringUtil.implode(ChatColor.GRAY + ", ", mapList));
		} else if (action.matches("^((un)?lock|add|create|setspawn|(remov|delet)e)$")) {
			if ("lock".equals(action)) {
				if (!this.hasPermissions(sender, "map", "lock")) {
					this.getLog().error(sender, "You do not have permission to lock maps!");
					return;
				}
			} else {
				if (!this.hasPermissions(sender, "map", "manage")) {
					this.getLog().error(sender, "You do not have permission to manage maps!");
					return;
				}
			}

			if (args.length < 2) {
				this.showUsage(sender);
				return;
			}

			BukkitMojangProfile profile = NiftyBukkit.getMojangRepository().searchByUsername(sender.getName());
			UserParkourData userData = UserParkourData.getCache(profile);
			String mapName = parseMapName(args, 1);
			Maps maps = NiftyParkour.getMaps();

			if (!userData.isAdminMode()) {
				this.getLog().error(sender, "You must be in admin mode to manage maps!");
				return;
			}

			if (!action.matches("^(add|create)$") && !maps.checkExists(mapName)) {
				this.getLog().error(sender, "The map {{0}} does not exist, create it first!", mapName);
				return;
			}

			if (action.matches("^(un)?lock$")) {
				maps.getMap(mapName).setLocked("lock".equals(action));
				this.getLog().message(sender, "The map {{0}} has been {{1}ed}!", mapName, action);

				if (maps.getMap(mapName).isLocked()) {
					for (UserParkourData playerData : UserParkourData.getCache()) {
						if (mapName.equalsIgnoreCase(playerData.getLastMap())) {
							Player player = playerData.getOfflinePlayer().getPlayer();
							player.closeInventory();
							this.getLog().message(player, "{{0}} has just been locked!", mapName);
							playerData.teleportToSpawn();
						}
					}
				}

				// TODO: Update open inventories
			} else if (action.matches("^(add|create)$")) {
				if (maps.checkExists(mapName)) {
					this.getLog().error(sender, "Unable to create {{0}}, it already exists!", mapName);
					return;
				}

				maps.addMap(mapName);
				this.getLog().message(sender, "The map {{0}} has been created!", mapName);
			} else if (action.matches("^(remov|delet)e$")) {
				// TODO: Request confirmation
				//NiftyParkour.getMaps().removeMap(mapName);
				//this.getLog().message(sender, "The map {{0}} has been removed!");
				this.getLog().message(sender, "This feature is currently disabled! Please delete manually!");
			} else if ("setspawn".equals(action)) {
				NiftyParkour.getMaps().getMap(mapName).setSpawnPoint(profile.getOfflinePlayer().getPlayer().getLocation());
				this.getLog().message(sender, "The spawn point for {{0}} has been set!", mapName);
			}

			maps.getMap(mapName).save();
		} else
			this.showUsage(sender);
	}

}