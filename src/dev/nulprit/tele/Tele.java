package dev.nulprit.tele;

import java.util.Collections;
import java.util.Set;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Jedis;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import dev.nulprit.tele.GoCommand;
import dev.nulprit.tele.SetCommand;
import dev.nulprit.tele.PlacesCompleter;
import dev.nulprit.tele.DeathListener;

public class Tele extends JavaPlugin {
	private JedisPool pool;
	public FileConfiguration config = getConfig();

	@Override
	public void onEnable() {
		saveDefaultConfig();

		String host = config.getString("redis.host");
		int port = config.getInt("redis.port");
		String auth = config.getString("redis.auth");

		pool = new JedisPool(new GenericObjectPoolConfig(), host, port, 60, auth);

		getCommand("go").setExecutor(new GoCommand(this));
		getCommand("go").setTabCompleter(new PlacesCompleter(this, true));

		getCommand("set").setExecutor(new SetCommand(this));
		getCommand("set").setTabCompleter(new PlacesCompleter(this, false));

		getServer().getPluginManager().registerEvents(new DeathListener(this), this);
	}

	@Override
	public void onDisable() {
		pool.close();
	}

	static String encodeLocation(Location l) {
		return String.format("%s/%f/%f/%f/%f/%f", l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
	}

	static Location decodeLocation(String str) {
		if (str == null) return null;
		String[] parts = str.split("/");
		if (parts.length != 6) return null;

		World world = Bukkit.getWorld(parts[0]);
		Double x = Double.parseDouble(parts[1]);
		Double y = Double.parseDouble(parts[2]);
		Double z = Double.parseDouble(parts[3]);
		Float pitch = Float.parseFloat(parts[4]);
		Float yaw = Float.parseFloat(parts[5]);
		return new Location(world, x, y, z, pitch, yaw);
	}

	private String buildPath(Player p, String part) {
		return config.getString("redis.prefix") + ":" + p.getUniqueId() + ":" + part;
	}

	public Long cooldown(Player p) {
		try (Jedis jedis = pool.getResource()) {
			String path = buildPath(p, "cooldown");
			Long result = jedis.ttl(path);

			if (result == -1) return Long.MAX_VALUE; // no expiry, max length
			if (result == -2 || result == 0) { // nonexistent or about to expire
				jedis.setex(path, config.getInt("cooldown"), "");
				return 0l;
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return Long.MAX_VALUE;
		}
	}

	public Location getBackLocation(Player p) {
		try (Jedis jedis = pool.getResource()) {
			String back = jedis.get(buildPath(p, "back"));
			if (back == null) return null;
			return decodeLocation(back);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Set<String> getPlaceNames(Player p) {
		try (Jedis jedis = pool.getResource()) {
			return jedis.hkeys(buildPath(p, "places"));
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.<String>emptySet();
		}
	}

	public Long getPlaceCount(Player p) {
		try (Jedis jedis = pool.getResource()) {
			return jedis.hlen(buildPath(p, "places"));
		} catch (Exception e) {
			e.printStackTrace();
			return 0l;
		}
	}

	public Location getPlace(Player p, String name) {
		try (Jedis jedis = pool.getResource()) {
			String place = jedis.hget(buildPath(p, "places"), name);
			if (place == null) return null;
			return decodeLocation(place);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void setPlace(Player p, String name, Location place) {
		try (Jedis jedis = pool.getResource()) {
			String encoded = encodeLocation(place);
			jedis.hset(buildPath(p, "places"), name, encoded);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveBack(Player p) {
		try (Jedis jedis = pool.getResource()) {
			String back = encodeLocation(p.getLocation());

			int expire = config.getInt("back.expire");
			if (expire > 0) {
				jedis.setex(buildPath(p, "back"), expire, back);
			} else {
				jedis.set(buildPath(p, "back"), back);
			}
		} catch (Exception e) {
			e.printStackTrace();
			p.sendMessage("Couldn't save \"back\" location");
		}
	}

	public void teleport(Player p, Location place) {
		if (config.getBoolean("back.enabled")) {
			saveBack(p);
		}
		p.teleport(place);
	}
}
