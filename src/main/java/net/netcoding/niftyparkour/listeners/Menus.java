package net.netcoding.niftyparkour.listeners;

import net.netcoding.niftybukkit.minecraft.BukkitHelper;
import net.netcoding.niftybukkit.minecraft.inventory.FakeInventoryInstance;
import net.netcoding.niftybukkit.minecraft.inventory.FakeInventoryListener;
import net.netcoding.niftybukkit.minecraft.inventory.events.InventoryClickEvent;
import net.netcoding.niftybukkit.minecraft.inventory.events.InventoryCloseEvent;
import net.netcoding.niftybukkit.minecraft.inventory.events.InventoryItemInteractEvent;
import net.netcoding.niftybukkit.minecraft.inventory.events.InventoryOpenEvent;
import net.netcoding.niftybukkit.minecraft.items.ItemData;
import net.netcoding.niftybukkit.mojang.BukkitMojangProfile;
import net.netcoding.niftycore.minecraft.ChatColor;
import net.netcoding.niftycore.util.StringUtil;
import net.netcoding.niftyparkour.NiftyParkour;
import net.netcoding.niftyparkour.cache.MapConfig;
import net.netcoding.niftyparkour.cache.PlayerConfig;
import net.netcoding.niftyparkour.cache.UserParkourData;
import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Map;

public class Menus extends BukkitHelper implements FakeInventoryListener {

	public Menus(JavaPlugin plugin) {
		super(plugin);
	}

	public static void openMapSelection(BukkitMojangProfile profile) {
		FakeInventoryInstance inventory = NiftyParkour.getMenuInventory().newInstance(profile);
		Map<String, MapConfig> maps = NiftyParkour.getMaps().getAllMaps();

		for (MapConfig map : maps.values()) {
			ItemData item = getItem(Material.PAPER, (short)0, StringUtil.format("&r{0}{1}", ChatColor.GREEN, map.getName()), "");
			item.putNbtPath("parkour.map", map.getName());
			item.putNbtPath("parkour.action", "view");
			item.putNbtPath("parkour.page", 1);
			inventory.add(item);
		}

		inventory.open();
	}

	private static ItemData getItem(Material material, short durability, String displayName, String lore) {
		ItemData itemData = new ItemData(material, durability);
		ItemMeta itemMeta = itemData.getItemMeta();
		itemMeta.setDisplayName(displayName);

		if (StringUtil.notEmpty(lore))
			itemMeta.setLore(Arrays.asList(lore));

		itemData.setItemMeta(itemMeta);
		return itemData;
	}

	@Override
	public void onInventoryClick(InventoryClickEvent event) {
		ItemData clickedItem = event.getClickedItem(true);
		BukkitMojangProfile profile = event.getProfile();
		UserParkourData userData = UserParkourData.getCache(profile);
		FakeInventoryInstance inventory = NiftyParkour.getMenuInventory().newInstance(profile);
		String mapName = clickedItem.getNbtPath("parkour.map");
		String action = clickedItem.getNbtPath("parkour.action");

		switch (action) {
			case "view":
				MapConfig map = NiftyParkour.getMaps().getMap(mapName);
				PlayerConfig config = userData.getPlayerConfig();
				inventory.setTitle(StringUtil.format("{0} Checkpoints", map.getName()));
				ItemData spawn = getItem(Material.COMPASS, (short)0, StringUtil.format("&r&a{0} Spawn", map.getName()), StringUtil.format("&3Go to {0} spawn", map.getName()));
				spawn.putNbtPath("parkour.map", map.getName());
				spawn.putNbtPath("parkour.action", "spawn");
				inventory.add(spawn);
				int totalSize = Math.min(map.getCheckpoints().size(), 50);
				int page = clickedItem.getNbtPath("parkour.page");
				int start = 1 + ((page - 1) * 50);

				for (int i = start; i <= totalSize; i++) {
					boolean hasCheckpoint = userData.isAdminMode() || config.hasCheckpoint(map.getName(), i);
					String displayName = StringUtil.format("&r{0}{1}", (hasCheckpoint ? ChatColor.GREEN : ChatColor.RED), i);
					String lore = StringUtil.format("&3{0}ocked", (hasCheckpoint ? "Unl" : "L"));
					ItemData item = getItem(Material.STAINED_CLAY, (short)(hasCheckpoint ? 5 : 14), displayName, lore);
					item.putNbtPath("parkour.map", map.getName());
					item.putNbtPath("parkour.action", "checkpoint");
					item.putNbtPath("parkour.checkpoint", i);
					inventory.add(item);
				}

				ItemData back = getItem(Material.CHEST, (short)0, StringUtil.format("&r&cBack"), StringUtil.format("&3Go back to map selection"));
				back.putNbtPath("parkour.map", map.getName());
				back.putNbtPath("parkour.action", "back");
				inventory.add(back);

				if (page > 1) {
					ItemData pageBack = getItem(Material.BOOK, (short)0, StringUtil.format("&r&cPage {0}", page), StringUtil.format("&3Go to page {0}", page));
					pageBack.putNbtPath("parkour.map", map.getName());
					pageBack.putNbtPath("parkour.action", "view");
					pageBack.putNbtPath("parkour.page", page);
					inventory.add(pageBack);
				}

				if (1 + (page * 50) <= totalSize) {
					ItemData pageNext = getItem(Material.BOOK_AND_QUILL, (short)0, StringUtil.format("&r&cPage {0}", page + 1), StringUtil.format("&3Go to page {0}", page + 1));
					pageNext.putNbtPath("parkour.map", map.getName());
					pageNext.putNbtPath("parkour.action", "view");
					pageNext.putNbtPath("parkour.page", page + 1);
					inventory.add(pageNext);
				}

				inventory.open();
				break;
			case "back":
				openMapSelection(profile);
				break;
			case "spawn":
				NiftyParkour.getMenuInventory().close(profile);
				userData.teleportTo(mapName);
				break;
			case "checkpoint":
				int checkpoint = clickedItem.getNbtPath("parkour.checkpoint");
				userData.teleportTo(mapName, checkpoint);
				break;
		}
	}

	@Override
	public void onInventoryClose(InventoryCloseEvent event) {

	}

	@Override
	public void onInventoryItemInteract(InventoryItemInteractEvent event) {
		openMapSelection(event.getProfile());
	}

	@Override
	public void onInventoryOpen(InventoryOpenEvent event) {

	}

}