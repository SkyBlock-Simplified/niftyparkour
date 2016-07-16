package net.netcoding.nifty.parkour.listeners;

import net.netcoding.nifty.common.api.plugin.MinecraftHelper;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.api.signs.SignListener;
import net.netcoding.nifty.common.api.signs.events.SignBreakEvent;
import net.netcoding.nifty.common.api.signs.events.SignCreateEvent;
import net.netcoding.nifty.common.api.signs.events.SignInteractEvent;
import net.netcoding.nifty.common.api.signs.events.SignUpdateEvent;
import net.netcoding.nifty.common.minecraft.entity.living.human.Player;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.api.color.ChatColor;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.parkour.NiftyParkour;
import net.netcoding.nifty.parkour.cache.Keys;
import net.netcoding.nifty.parkour.cache.MapConfig;
import net.netcoding.nifty.parkour.cache.PlayerConfig;
import net.netcoding.nifty.parkour.cache.UserParkourData;

public final class Signs extends MinecraftHelper implements SignListener {

	public Signs(MinecraftPlugin plugin) {
		super(plugin);
	}

	@Override
	public void onSignBreak(SignBreakEvent event) {
		UserParkourData userData = UserParkourData.getCache(event.getProfile());
		Player player = userData.getOfflinePlayer().getPlayer();

		if (!this.hasPermissions(player, "manage", "signs")) {
			this.getLog().error(player, "You do not have permission to break this sign!");
			event.setCancelled(true);
			return;
		}

		if (!userData.isAdminMode()) {
			this.getLog().error(player, "You must be in admin mode to manage signs!");
			event.setCancelled(true);
		}
	}

	@Override
	public void onSignCreate(SignCreateEvent event) {
		UserParkourData userData = UserParkourData.getCache(event.getProfile());
		Player player = userData.getOfflinePlayer().getPlayer();

		if (!this.hasPermissions(player, "manage", "signs")) {
			this.getLog().error(player, "You do not have permission to create this sign!");
			event.setCancelled(true);
			return;
		}

		if (!userData.isAdminMode()) {
			this.getLog().error(player, "You must be in admin mode to manage signs!");
			event.setCancelled(true);
			return;
		}

		if (event.getKey().matches(StringUtil.format("^({2}|{1})$", Keys.WARP, Keys.CHECKPOINT))) {
			String mapName = event.getLine(1);

			if (StringUtil.isEmpty(mapName)) {
				this.getLog().error(player, "You must provide a map name!");
				event.setCancelled(true);
				return;
			}

			if (!NiftyParkour.getMaps().hasMap(mapName)) {
				this.getLog().error(player, "There is no map {{0}}!", mapName);
				event.setCancelled(true);
				return;
			}
		}

		if (Keys.isKey(Keys.CHECKPOINT, event.getKey())) {
			MapConfig map = NiftyParkour.getMaps().getMap(event.getLine(1));
			String number = event.getLine(2);

			try {
				Integer checkpoint = Integer.parseInt(number);

				if (checkpoint < 1) {
					this.getLog().error(player, "Checkpoints start at 1!");
					event.setCancelled(true);
					return;
				}

				if (!map.hasCheckpoint(checkpoint)) {
					this.getLog().error(player, "There is no checkpoint {{0}}!", checkpoint);
					event.setCancelled(true);
				}
			} catch (Exception ex) {
				this.getLog().error(player, "The value {{0}} is not a valid number!", number);
				event.setCancelled(true);
			}
		}
	}

	@Override
	public void onSignInteract(SignInteractEvent event) {
		MinecraftMojangProfile profile = event.getProfile();
		UserParkourData userData = UserParkourData.getCache(profile);
		Player player = userData.getOfflinePlayer().getPlayer();

		if (!this.hasPermissions(player, event.getKey())) {
			this.getLog().error(player, "You cannot interact with {{0}} signs!", event.getKey());
			return;
		}

		try {
			if (Keys.isKey(Keys.SPAWN, event.getKey()))
				userData.teleportToSpawn();
			else if (Keys.isKey(Keys.MENU, event.getKey()))
				Menus.openMapSelection(profile);
			else {
				String mapName = event.getLine(1);

				if (NiftyParkour.getMaps().hasMap(mapName)) {
					MapConfig map = NiftyParkour.getMaps().getMap(event.getLine(1));

					if (Keys.isKey(Keys.WARP, event.getKey()))
						userData.teleportTo(map.getName());
					else if (Keys.isKey(Keys.CHECKPOINT, event.getKey())) {
						int checkpoint = Integer.parseInt(event.getLine(2));

						if (map.hasCheckpoint(checkpoint)) {
							PlayerConfig config = userData.getPlayerConfig();
							boolean has = config.hasCheckpoint(map.getName(), checkpoint);

							if (!has) {
								config.addCheckpoint(map.getName(), checkpoint);
								config.save();
							}

							this.getLog().message(player, "Checkpoint {{0}}{1} unlocked for {{2}}!", checkpoint, (has ? " already" : ""), map.getName());
						} else
							this.getLog().error(player, "CHeckpoint {{0}} does not exist for {{1}}!", checkpoint, map.getName());
					}
				} else
					this.getLog().error(player, "Unable to interact with sign! Invalid map {{0}}!", mapName);
			}
		} catch (Exception ex) {
			this.getLog().error(player, "Unable to interact with sign! Please notify staff!", ex);
		}
	}

	@Override
	public void onSignUpdate(SignUpdateEvent event) {
		if (Keys.isKey(Keys.SPAWN, event.getKey())) {
			event.setLine(0, this.colorfy("Spawn", true));
			event.setLine(1, this.colorfy("Click here to"));
			event.setLine(2, this.colorfy("go back to"));
			event.setLine(3, this.colorfy("spawn"));
		} else if (Keys.isKey(Keys.MENU, event.getKey())) {
			event.setLine(0, this.colorfy("Main Menu", true));
			event.setLine(1, this.colorfy("Click here to"));
			event.setLine(2, this.colorfy("open up the"));
			event.setLine(3, this.colorfy("menu"));
		} else {
			String mapName = event.getLine(1);
			event.setLine(0, this.colorfy((Keys.isKey(Keys.WARP, event.getKey()) ? "Warp" : "Checkpoint"), true));
			event.setLine(1, mapName);

			if (NiftyParkour.getMaps().hasMap(mapName)) {
				MapConfig map = NiftyParkour.getMaps().getMap(mapName);

				if (Keys.isKey(Keys.CHECKPOINT, event.getKey())) {
					UserParkourData userData = UserParkourData.getCache(event.getProfile());
					Integer checkpoint = Integer.parseInt(event.getLine(2));

					if (map.hasCheckpoint(checkpoint)) {
						boolean hasCheckpoint = userData.getPlayerConfig().hasCheckpoint(map.getName(), checkpoint);

						if (userData.isAdminMode())
							event.setLine(3, this.colorfy("Admin Mode", true));
						else
							event.setLine(3, this.colorfy(StringUtil.format("{0}ocked", (hasCheckpoint ? "Unl" : "L")), false, !hasCheckpoint));
					} else
						event.setLine(3, this.colorfy("!! CP !!", true, true));
				} else
					event.setLine(3, "");
			} else
				event.setLine(3, this.colorfy("!! MAP !!", true, true));
		}
	}

	private String colorfy(String value) {
		return this.colorfy(value, false, false);
	}

	private String colorfy(String value, boolean bold) {
		return this.colorfy(value, bold, false);
	}

	private String colorfy(String value, boolean bold, boolean red) {
		return StringUtil.format("{0}{1}{2}", (red ? ChatColor.RED : ChatColor.GREEN), (bold ? ChatColor.BOLD : ""), value);
	}

}