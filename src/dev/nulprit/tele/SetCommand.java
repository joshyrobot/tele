package dev.nulprit.tele;

import java.lang.String;
import java.util.Arrays;
import java.util.Set;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;

import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.ChatColor;

import dev.nulprit.tele.Tele;

public class SetCommand implements CommandExecutor {
	private Tele plugin;
	private int limit;

	public SetCommand(Tele plugin) {
		this.plugin = plugin;
		limit = plugin.config.getInt("places-limit");

		if (limit == 0) limit = Integer.MAX_VALUE;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Must be run by a player");
			return false;
		}
		Player player = (Player) sender;

		if (args.length != 1) {
			return false;
		}
		String name = args[0];

		Long count = plugin.getPlaceCount(player);
		if (count >= limit) {
			player.sendMessage(String.format("You have too many places saved (%d out of the %d allowed)", count, limit));
			return true;
		}

		if (name.equals("bed")) {
			player.sendMessage("This name is reserved for going to your most recent bed location");
			return true;
		}

		if (name.equals("back")) {
			player.sendMessage("This name is reserved for going back to a previous location");
			return true;
		}

		plugin.setPlace(player, name, player.getLocation());
		player.sendMessage("Saved this spot as \"" + name + "\"");
		return true;
	}
}