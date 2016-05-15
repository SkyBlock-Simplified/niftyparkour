package net.netcoding.niftyparkour.commands;

import net.netcoding.niftybukkit.NiftyBukkit;
import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftycore.minecraft.ChatColor;
import net.netcoding.niftycore.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.niftycore.util.StringUtil;
import net.netcoding.niftyparkour.NiftyParkour;
import net.netcoding.niftyparkour.cache.MapConfig;
import net.netcoding.niftyparkour.cache.UserParkourData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Checkpoint extends BukkitCommand {

	public Checkpoint(JavaPlugin plugin) {
		super(plugin, "checkpoint");
		this.setPlayerOnly();
		this.setMinimumArgsLength(2);
		this.editUsage(1, "add", "<map>");
		this.editUsage(1, "list", "[player] <map>");
		this.editUsage(1, "move", "<number> <new number> <map>");
		this.editUsage(1, "remove", "<number> <map>");
	}

	@Override
	protected void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		final String action = args[0];

		if ("list".equals(action)) {
			BukkitMojangProfile profile;
			String mapName = Map.parseMapName(args, 1);
			boolean mapExists = NiftyParkour.getMaps().checkExists(mapName);
			String user = (mapExists ? sender.getName() : args[1]);

			try {
				profile = NiftyBukkit.getMojangRepository().searchByUsername(user);
			} catch (ProfileNotFoundException pnfex) {
				this.getLog().error(sender, "Unable to locate the profile {{0}}!", user);
				return;
			}

			if (!mapExists) {
				mapName = Map.parseMapName(args, 2);

				if (!NiftyParkour.getMaps().checkExists(mapName)) {
					this.getLog().error(sender, "The map {{0}} does not exist, create it first!", mapName);
					return;
				}
			}

			if (!(profile.getName().equals(sender.getName()) || this.hasPermissions(sender, "checkpoint", "other"))) {
				this.getLog().error(sender, "You do not have permission to view other players checkpoints!");
				return;
			}

			MapConfig map = NiftyParkour.getMaps().getMap(mapName);
			UserParkourData userData = UserParkourData.getCache(profile);
			boolean isAdmin = profile.getName().equals(sender.getName()) && userData.isAdminMode();
			List<String> checkpointList = new ArrayList<>();

			if (map.getCheckpoints().size() > 0) {
				for (int i = 0; i < map.getCheckpoints().size(); i++) {
					boolean has = userData.getPlayerConfig().hasCheckpoint(map.getName(), i + 1);
					checkpointList.add(StringUtil.format("{0}{1}", (has ? ChatColor.GREEN : (isAdmin ? ChatColor.YELLOW : ChatColor.RED)), i + 1));
				}
			} else
				checkpointList.add(StringUtil.format("{{0}}", "No checkpoints available!"));

			String forWhom = (profile.getName().equals(sender.getName()) ? "" : StringUtil.format(" for {{0}}", profile.getName()));
			this.getLog().message(sender, "{{0}} checkpoints{1}: {2}", map.getName(), forWhom, StringUtil.implode(ChatColor.GRAY + ", ", checkpointList));
		} else if (action.matches("^(add|(re)?move)$")) {
			if (!this.hasPermissions(sender, "checkpoint", "manage")) {
				this.getLog().error(sender, "You do not have permission to manage checkpoints!");
				return;
			}

			if (args.length < 2) {
				this.showUsage(sender);
				return;
			}

			BukkitMojangProfile profile = NiftyBukkit.getMojangRepository().searchByUsername(sender.getName());
			UserParkourData userData = UserParkourData.getCache(profile);
			Player player = profile.getOfflinePlayer().getPlayer();
			String mapName = Map.parseMapName(args, ("add".equals(action) ? 1 : ("move".equals(action) ? 3 : 2)));
			MapConfig map = NiftyParkour.getMaps().getMap(mapName);

			if (!userData.isAdminMode()) {
				this.getLog().error(sender, "You must be in admin mode to manage checkpoints!");
				return;
			}

			if ("add".equals(action)) {
				map.addCheckpoint(player.getLocation());
				this.getLog().message(sender, "Checkpoint {{0}} added for {{1}}!", map.getCheckpoints().size(), mapName);
			} else if ("move".equals(action)) {
				if (args.length < 4) {
					this.showUsage(sender);
					return;
				}

				try {
					int checkpoint = Integer.parseInt(args[1]);
					int newCheckpoint = Integer.parseInt(args[2]);

					if (checkpoint == newCheckpoint) {
						this.getLog().error(sender, "You cannot move a checkpoint to itself!");
						return;
					}

					if (!map.hasCheckpoint(checkpoint)) {
						this.getLog().error(sender, "{{0}} does not have checkpoint {{1}}!", map.getName(), checkpoint);
						return;
					}

					if (!map.hasCheckpoint(newCheckpoint)) {
						this.getLog().error(sender, "{{0}} does not have checkpoint {{1}}!", map.getName(), newCheckpoint);
						return;
					}

					map.moveCheckpoint(checkpoint, newCheckpoint/*, sender*/);
					this.getLog().message(sender, "Checkpoint {{0}} moving to {{1}} for {{2}}...", checkpoint, newCheckpoint, mapName);
					// TODO: Callback for completion
				} catch (NumberFormatException nfex) {
					this.getLog().error(sender, "The values {{0}} and {{1}} must be valid integers!", args[1], args[2]);
				}
			} else if ("remove".equals(action)) {
				if (args.length < 3) {
					this.showUsage(sender);
					return;
				}

				try {
					int checkpoint = Integer.parseInt(args[1]);

					if (!map.hasCheckpoint(checkpoint)) {
						this.getLog().error(sender, "{{0}} does not have checkpoint {{1}}!", map.getName(), checkpoint);
						return;
					}

					map.removeCheckpoint(checkpoint);
					this.getLog().message(sender, "Checkpoint {{0}} removed for {{1}}!", checkpoint, mapName);
				} catch (NumberFormatException nfex) {
					this.getLog().error(sender, "The value {{0}} must be a valid integer!", args[1]);
				}
			}

			map.save();
		} else
			this.showUsage(sender);
	}

}