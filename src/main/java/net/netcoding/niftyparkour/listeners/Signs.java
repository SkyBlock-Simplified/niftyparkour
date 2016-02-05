package net.netcoding.niftyparkour.listeners;

import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftybukkit.signs.events.SignBreakEvent;
import net.netcoding.niftybukkit.signs.events.SignCreateEvent;
import net.netcoding.niftybukkit.signs.events.SignInteractEvent;
import net.netcoding.niftybukkit.signs.events.SignUpdateEvent;
import net.netcoding.niftycore.minecraft.ChatColor;
import net.netcoding.niftycore.util.StringUtil;
import net.netcoding.niftyparkour.NiftyParkour;
import net.netcoding.niftyparkour.cache.MapConfig;
import net.netcoding.niftyparkour.cache.PlayerConfig;
import net.netcoding.niftyparkour.cache.UserParkourData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Signs extends BukkitHelper implements net.netcoding.niftybukkit.signs.SignListener {

	public Signs(JavaPlugin plugin) {
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

		if (event.getKey().matches("^(warp2|checkpoint2)$")) {
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

		if ("checkpoint2".equals(event.getKey())) {
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
		UserParkourData userData = UserParkourData.getCache(event.getProfile());
		Player player = userData.getOfflinePlayer().getPlayer();

		if (!this.hasPermissions(player, event.getKey())) {
			this.getLog().error(player, "You cannot interact with {{0}} signs!", event.getKey());
			return;
		}

		try {
			if ("spawn2".equals(event.getKey())) {
				userData.teleportToSpawn();
			} else if ("menu2".equals(event.getKey())) {
				// TODO: Open menu
			} else {
				MapConfig map = NiftyParkour.getMaps().getMap(event.getLine(1));

				if ("warp2".equals(event.getKey()))
					userData.teleportTo(map.getName());
				else if ("checkpoint2".equals(event.getKey())) {
					int checkpoint = Integer.parseInt(event.getLine(2));
					PlayerConfig config = userData.getPlayerConfig();
					boolean has = config.hasCheckpoint(map.getName(), checkpoint);

					if (!has) {
						config.addCheckpoint(map.getName(), checkpoint);
						config.save();
					}

					this.getLog().message(player, "Checkpoint {{0}}{1} unlocked for {{2}}!", checkpoint, (has ? " already" : ""), map.getName());
				}
			}
		} catch (Exception ex) {
			this.getLog().error(player, "Unable to interact with sign! Please notify staff!", ex);
		}
	}

	@Override
	public void onSignUpdate(SignUpdateEvent event) {
		if ("spawn2".equals(event.getKey())) {
			event.setLine(0, this.colorfy("Spawn", true));
			event.setLine(1, this.colorfy("Click here to"));
			event.setLine(2, this.colorfy("go back to"));
			event.setLine(3, this.colorfy("spawn"));
		} else if ("menu2".equals(event.getKey())) {
			event.setLine(0, this.colorfy("Main Menu", true));
			event.setLine(1, this.colorfy("Click here to"));
			event.setLine(2, this.colorfy("open up the"));
			event.setLine(3, this.colorfy("menu"));
		} else {
			MapConfig map = NiftyParkour.getMaps().getMap(event.getLine(1));
			event.setLine(0, this.colorfy(("warp2".equals(event.getKey()) ? "Warp" : "Checkpoint"), true));
			event.setLine(1, map.getName());

			if ("checkpoint2".equals(event.getKey())) {
				UserParkourData userData = UserParkourData.getCache(event.getProfile());
				Integer checkpoint = Integer.parseInt(event.getLine(2));
				boolean hasCheckpoint = userData.getPlayerConfig().hasCheckpoint(map.getName(), checkpoint);

				if (userData.isAdminMode())
					event.setLine(3, this.colorfy("Admin Mode", true));
				else
					event.setLine(3, this.colorfy(StringUtil.format("{0}ocked", (hasCheckpoint ? "Unl" : "L")), false, !hasCheckpoint));
			}
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