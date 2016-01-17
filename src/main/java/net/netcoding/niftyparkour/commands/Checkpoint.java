package net.netcoding.niftyparkour.commands;

import net.netcoding.niftybukkit.minecraft.BukkitCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Checkpoint extends BukkitCommand {

	public Checkpoint(JavaPlugin plugin) {
		super(plugin, "checkpoint");
		this.setPlayerOnly();
		this.setMinimumArgsLength(2);
		this.editUsage(1, "list", "");
		this.editUsage(1, "move", "<map> <number> <new number>");
		this.editUsage(1, "remove", "<map> <number>");
	}

	@Override
	protected void onCommand(CommandSender sender, String alias, String[] args) throws Exception {
		final String action = args[0];
		final String mapName = args[1];

		if ("list".equals(action)) {
			// TODO: Output player checkpoints
		} else {

			// add|list|move|remove
		}
	}

}