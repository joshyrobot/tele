package dev.nulprit.tele;

import java.lang.String;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.ChatColor;

import dev.nulprit.tele.Tele;

public class GoCommand implements CommandExecutor {
	private Tele plugin;

	public GoCommand(Tele plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("Must be run by a player");
			return false;
		}
		Player player = (Player) sender;

		if (args.length == 0) {
			ComponentBuilder message = new ComponentBuilder();
			message.append("Available places:");

			// Create bed button
			Location bed = player.getBedSpawnLocation();
			message.append("\n - ").color(ChatColor.RESET);
			message.append("[bed]");
			ComponentBuilder bed_tooltip = new ComponentBuilder();
			if (bed == null) {
				bed_tooltip.append("You don't have a bed to teleport to");
				message.color(ChatColor.DARK_GRAY);
			} else {
				bed_tooltip.append("Go to your most recent bed");
				message
					.color(ChatColor.GOLD)
					.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tele:go bed"));
			}
			message.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, bed_tooltip.create()));

			if (plugin.config.getBoolean("back.enabled")) {
				// Create back button
				Location back = plugin.getBackLocation(player);
				message.append("\n - ").color(ChatColor.RESET);
				message.append("[back]");
				ComponentBuilder back_tooltip = new ComponentBuilder();
				if (back == null) {
					back_tooltip.append("You haven't teleported recently");
					message.color(ChatColor.DARK_GRAY);
				} else {
					back_tooltip.append("Go back to your last location");
					message
						.color(ChatColor.GOLD)
						.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tele:go back"));
				}
				message.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, back_tooltip.create()));
			}

			// Create places buttons
			Set<String> places = plugin.getPlaceNames(player);
			for (String place : places) {
				message.append("\n - ").color(ChatColor.RESET);

				BaseComponent[] tooltip = new ComponentBuilder().append("Go to \"" + place + "\"").create();

				message
					.append("[" + place + "]")
					.color(ChatColor.GREEN)
					.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tele:go " + place))
					.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip));
			}

			player.spigot().sendMessage(message.create());
			return true;
		}
		if (args.length > 1) {
			return false;
		}
		String name = args[0];

		if (plugin.config.getInt("cooldown") > 0) {
			Long cooldown = plugin.cooldown(player);
			if (cooldown > 0) {
				if (cooldown == Long.MAX_VALUE) {
					player.sendMessage("You are on indefinite cooldown for this command");
				} else {
					player.sendMessage("You must wait " + cooldown + " seconds to use this command again");
				}
				return true;
			}
		}

		if (name.equals("bed")) {
			Location bed = player.getBedSpawnLocation();
			if (bed == null) {
				player.sendMessage("You don't have a bed to teleport to");
				return true;
			}
			plugin.teleport(player, bed);
			player.sendMessage("Went to your bed");
			return true;
		}

		if (name.equals("back")) {
			if (!plugin.config.getBoolean("back.enabled")) {
				player.sendMessage("Going back isn't enabled");
				return true;
			}
			Location back = plugin.getBackLocation(player);
			if (back == null) {
				player.sendMessage("You haven't teleported recently");
				return true;
			}
			plugin.teleport(player, back);
			player.sendMessage("Went back");
			return true;
		}

		Location place = plugin.getPlace(player, name);
		if (place == null) {
			player.sendMessage("You haven't saved a place called \"" + name + "\"");
			return true;
		}
		plugin.teleport(player, place);
		player.sendMessage("Went to \"" + name + "\"");
		return true;
	}
}
