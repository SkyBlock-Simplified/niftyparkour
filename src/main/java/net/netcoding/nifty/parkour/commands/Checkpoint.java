package net.netcoding.nifty.parkour.commands;

import net.netcoding.nifty.common.Nifty;
import net.netcoding.nifty.common.api.plugin.Command;
import net.netcoding.nifty.common.api.plugin.MinecraftListener;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.command.CommandSource;
import net.netcoding.nifty.common.minecraft.entity.living.human.Player;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.api.color.ChatColor;
import net.netcoding.nifty.core.mojang.exceptions.ProfileNotFoundException;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.parkour.NiftyParkour;
import net.netcoding.nifty.parkour.cache.Keys;
import net.netcoding.nifty.parkour.cache.MapConfig;
import net.netcoding.nifty.parkour.cache.UserParkourData;

import java.util.ArrayList;
import java.util.List;

public class Checkpoint extends MinecraftListener {

	public Checkpoint(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Command(name = "checkpoint",
			playerOnly = true,
			minimumArgs = 2,
			usages = {
					@Command.Usage(match = "add", replace = "<map>"),
					@Command.Usage(match = "list", replace = "[player] <map>"),
					@Command.Usage(match = "move", replace = "<number> <new number> <map>"),
					@Command.Usage(match = "remove", replace = "<number> <map>"),
			}
	)
	protected void onCommand(CommandSource source, String alias, String[] args) throws Exception {
		final String action = args[0];

		if ("list".equals(action)) {
			MinecraftMojangProfile profile;
			String mapName = Map.parseMapName(args, 1);
			boolean mapExists = NiftyParkour.getMaps().checkExists(mapName);
			String user = (mapExists ? source.getName() : args[1]);

			try {
				profile = Nifty.getMojangRepository().searchByUsername(user);
			} catch (ProfileNotFoundException pnfex) {
				this.getLog().error(source, "Unable to locate the profile {{0}}!", user);
				return;
			}

			if (!mapExists) {
				mapName = Map.parseMapName(args, 2);

				if (!NiftyParkour.getMaps().checkExists(mapName)) {
					this.getLog().error(source, "The map {{0}} does not exist, create it first!", mapName);
					return;
				}
			}

			if (!(profile.getName().equals(source.getName()) || this.hasPermissions(source, "checkpoint", "other"))) {
				this.getLog().error(source, "You do not have permission to view other players checkpoints!");
				return;
			}

			MapConfig map = NiftyParkour.getMaps().getMap(mapName);
			UserParkourData userData = UserParkourData.getCache(profile);
			boolean isAdmin = profile.getName().equals(source.getName()) && userData.isAdminMode();
			List<String> checkpointList = new ArrayList<>();

			if (map.getCheckpoints().size() > 0) {
				for (int i = 0; i < map.getCheckpoints().size(); i++) {
					boolean has = userData.getPlayerConfig().hasCheckpoint(map.getName(), i + 1);
					checkpointList.add(StringUtil.format("{0}{1}", (has ? ChatColor.GREEN : (isAdmin ? ChatColor.YELLOW : ChatColor.RED)), i + 1));
				}
			} else
				checkpointList.add(StringUtil.format("{{0}}", "No checkpoints available!"));

			String forWhom = (profile.getName().equals(source.getName()) ? "" : StringUtil.format(" for {{0}}", profile.getName()));
			this.getLog().message(source, "{{0}} checkpoints{1}: {2}", map.getName(), forWhom, StringUtil.implode(ChatColor.GRAY + ", ", checkpointList));
		} else if (action.matches("^(add|(re)?move)$")) {
			if (!this.hasPermissions(source, "checkpoint", "manage")) {
				this.getLog().error(source, "You do not have permission to manage checkpoints!");
				return;
			}

			if (args.length < 2) {
				this.showUsage(source);
				return;
			}

			MinecraftMojangProfile profile = Nifty.getMojangRepository().searchByUsername(source.getName());
			UserParkourData userData = UserParkourData.getCache(profile);
			Player player = profile.getOfflinePlayer().getPlayer();
			String mapName = Map.parseMapName(args, ("add".equals(action) ? 1 : ("move".equals(action) ? 3 : 2)));
			MapConfig map = NiftyParkour.getMaps().getMap(mapName);

			if (!userData.isAdminMode()) {
				this.getLog().error(source, "You must be in admin mode to manage checkpoints!");
				return;
			}

			if ("add".equals(action)) {
				map.addCheckpoint(player.getLocation());
				this.getLog().message(source, "Checkpoint {{0}} added for {{1}}!", map.getCheckpoints().size(), mapName);
			} else if ("move".equals(action)) {
				if (args.length < 4) {
					this.showUsage(source);
					return;
				}

				try {
					int checkpoint = Integer.parseInt(args[1]);
					int newCheckpoint = Integer.parseInt(args[2]);

					if (checkpoint == newCheckpoint) {
						this.getLog().error(source, "You cannot move a checkpoint to itself!");
						return;
					}

					if (!map.hasCheckpoint(checkpoint)) {
						this.getLog().error(source, "{{0}} does not have checkpoint {{1}}!", map.getName(), checkpoint);
						return;
					}

					if (!map.hasCheckpoint(newCheckpoint)) {
						this.getLog().error(source, "{{0}} does not have checkpoint {{1}}!", map.getName(), newCheckpoint);
						return;
					}

					map.moveCheckpoint(checkpoint, newCheckpoint/*, sender*/);
					this.getLog().message(source, "Checkpoint {{0}} moving to {{1}} for {{2}}...", checkpoint, newCheckpoint, mapName);
					// TODO: Callback for completion
				} catch (NumberFormatException nfex) {
					this.getLog().error(source, "The values {{0}} and {{1}} must be valid integers!", args[1], args[2]);
				}
			} else if ("remove".equals(action)) {
				if (args.length < 3) {
					this.showUsage(source);
					return;
				}

				try {
					int checkpoint = Integer.parseInt(args[1]);

					if (!map.hasCheckpoint(checkpoint)) {
						this.getLog().error(source, "{{0}} does not have checkpoint {{1}}!", map.getName(), checkpoint);
						return;
					}

					map.removeCheckpoint(checkpoint);
					this.getLog().message(source, "Checkpoint {{0}} removed for {{1}}!", checkpoint, mapName);
				} catch (NumberFormatException nfex) {
					this.getLog().error(source, "The value {{0}} must be a valid integer!", args[1]);
				}
			}

			for (MinecraftMojangProfile other : Nifty.getBungeeHelper().getPlayerList())
				NiftyParkour.sendSignUpdate(other, Keys.CHECKPOINT);

			map.save();
		} else
			this.showUsage(source);
	}

}