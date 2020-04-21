package dev.nulprit.tele;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class PlacesCompleter implements TabCompleter {
	private Tele plugin;
	private boolean includeReserved;

	public PlacesCompleter(Tele plugin, boolean includeReserved) {
		this.plugin = plugin;
		this.includeReserved = includeReserved;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
		List<String> suggestions = new ArrayList<String>();

		if (!(sender instanceof Player)) return suggestions;
		Player player = (Player) sender;

		if (args.length > 1) return suggestions;

		if (includeReserved) {
			suggestions.add("bed");
			suggestions.add("back");
		}
		suggestions.addAll(plugin.getPlaceNames(player));
		suggestions.removeIf(s -> !s.startsWith(args[0]));
		Collections.sort(suggestions);
		return suggestions;
	}
}
