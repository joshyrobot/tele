package dev.nulprit.tele;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import dev.nulprit.tele.Tele;

public class DeathListener implements Listener {
	private Tele plugin;

	public DeathListener(Tele plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent e) {
		if (plugin.config.getBoolean("back.enabled") && plugin.config.getBoolean("back.save-death")) {
			plugin.saveBack(e.getEntity());
		}
	}
}
