package net.netcoding.nifty.parkour.listeners;

import net.netcoding.nifty.common.api.inventory.FakeInventoryInstance;
import net.netcoding.nifty.common.api.inventory.FakeInventoryListener;
import net.netcoding.nifty.common.api.inventory.events.FakeInventoryClickEvent;
import net.netcoding.nifty.common.api.inventory.events.FakeInventoryCloseEvent;
import net.netcoding.nifty.common.api.inventory.events.FakeInventoryOpenEvent;
import net.netcoding.nifty.common.api.inventory.events.FakeItemInteractEvent;
import net.netcoding.nifty.common.api.plugin.MinecraftHelper;
import net.netcoding.nifty.common.api.plugin.MinecraftPlugin;
import net.netcoding.nifty.common.minecraft.inventory.item.ItemStack;
import net.netcoding.nifty.common.minecraft.inventory.item.meta.ItemMeta;
import net.netcoding.nifty.common.minecraft.material.Material;
import net.netcoding.nifty.common.mojang.MinecraftMojangProfile;
import net.netcoding.nifty.core.api.color.ChatColor;
import net.netcoding.nifty.core.util.StringUtil;
import net.netcoding.nifty.parkour.NiftyParkour;
import net.netcoding.nifty.parkour.cache.MapConfig;
import net.netcoding.nifty.parkour.cache.PlayerConfig;
import net.netcoding.nifty.parkour.cache.UserParkourData;

import java.util.Collections;
import java.util.Map;

public class Menus extends MinecraftHelper implements FakeInventoryListener {

	public Menus(MinecraftPlugin plugin) {
		super(plugin);
	}

	public static void openMapSelection(MinecraftMojangProfile profile) {
		FakeInventoryInstance instance = NiftyParkour.getMenuInventory().newInstance(profile);
		instance.setTitle("Maps");
		Map<String, MapConfig> maps = NiftyParkour.getMaps().getAllMaps();

		for (MapConfig map : maps.values()) {
			ItemStack item = getItem(Material.PAPER, (short)0, StringUtil.format("&r{0}{1}", ChatColor.GREEN, map.getName()), "");
			item.getNbt().putPath("parkour.map", map.getName());
			item.getNbt().putPath("parkour.action", "view");
			item.getNbt().putPath("parkour.page", 1);
			instance.add(item);
		}

		instance.open();
	}

	private static ItemStack getItem(Material material, short durability, String displayName, String lore) {
		ItemStack item = ItemStack.of(material, durability);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(displayName);

		if (StringUtil.notEmpty(lore))
			meta.setLore(Collections.singletonList(lore));

		item.setItemMeta(meta);
		return item;
	}

	@Override
	public void onInventoryClick(FakeInventoryClickEvent event) {
		ItemStack clickedItem = event.getClickedItem();
		MinecraftMojangProfile profile = event.getProfile();
		UserParkourData userData = UserParkourData.getCache(profile);
		String mapName = clickedItem.getNbt().getPath("parkour.map");
		String action = clickedItem.getNbt().getPath("parkour.action");

		switch (action) {
			case "view":
				FakeInventoryInstance instance = NiftyParkour.getMenuInventory().newInstance(profile);
				MapConfig map = NiftyParkour.getMaps().getMap(mapName);
				PlayerConfig config = userData.getPlayerConfig();
				instance.setTitle(StringUtil.format("{0} Checkpoints", map.getName()));

				ItemStack back = getItem(Material.CHEST, (short)0, StringUtil.format("&r&cBack"), StringUtil.format("&3Go back to map selection"));
				back.getNbt().putPath("parkour.map", map.getName());
				back.getNbt().putPath("parkour.action", "back");
				instance.add(back);

				ItemStack spawn = getItem(Material.COMPASS, (short)0, StringUtil.format("&r&a{0} Spawn", map.getName()), StringUtil.format("&3Go to {0} spawn", map.getName()));
				spawn.getNbt().putPath("parkour.map", map.getName());
				spawn.getNbt().putPath("parkour.action", "spawn");
				instance.add(spawn);

				//int totalSize = Math.min(map.getCheckpoints().size(), 50);
				//int page = clickedItem.getNbtPath("parkour.page");
				//int start = 1 + ((page - 1) * 50);

				for (int i = 1; i <= 65/*map.getCheckpoints().size()*/; i++) {
					boolean hasCheckpoint = userData.isAdminMode() || config.hasCheckpoint(map.getName(), i);
					String displayName = StringUtil.format("&r{0}{1}", (hasCheckpoint ? ChatColor.GREEN : ChatColor.RED), i);
					String lore = StringUtil.format("&3{0}ocked", (hasCheckpoint ? "Unl" : "L"));
					ItemStack item = getItem(Material.STAINED_CLAY, (short)(hasCheckpoint ? 5 : 14), displayName, lore);
					item.getNbt().putPath("parkour.map", map.getName());
					item.getNbt().putPath("parkour.action", "checkpoint");
					item.getNbt().putPath("parkour.checkpoint", i);
					instance.add(item);
				}

				/*if (page > 1) {
					ItemData pageBack = getItem(Material.BOOK, (short)0, StringUtil.format("&r&cPage {0}", page), StringUtil.format("&3Go to page {0}", page));
					pageBack.putNbtPath("parkour.map", map.getName());
					pageBack.putNbtPath("parkour.action", "view");
					pageBack.putNbtPath("parkour.page", page);
					items.add(pageBack);
				}

				if (1 + (page * 50) <= totalSize) {
					ItemData pageNext = getItem(Material.BOOK_AND_QUILL, (short)0, StringUtil.format("&r&cPage {0}", page + 1), StringUtil.format("&3Go to page {0}", page + 1));
					pageNext.putNbtPath("parkour.map", map.getName());
					pageNext.putNbtPath("parkour.action", "view");
					pageNext.putNbtPath("parkour.page", page + 1);
					items.add(pageNext);
				}*/

				instance.open();
				break;
			case "back":
				openMapSelection(profile);
				break;
			case "spawn":
				NiftyParkour.getMenuInventory().close(profile);
				userData.teleportTo(mapName);
				break;
			case "checkpoint":
				int checkpoint = clickedItem.getNbt().getPath("parkour.checkpoint");

				if (userData.getPlayerConfig().hasCheckpoint(mapName, checkpoint))
					userData.teleportTo(mapName, checkpoint);

				break;
		}
	}

	@Override
	public void onInventoryClose(FakeInventoryCloseEvent event) { }

	@Override
	public void onItemInteract(FakeItemInteractEvent event) {
		openMapSelection(event.getProfile());
	}

	@Override
	public void onInventoryOpen(FakeInventoryOpenEvent event) { }

}