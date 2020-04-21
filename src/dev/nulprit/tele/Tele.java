package dev.nulprit.tele;

import java.util.Collections;
import java.util.Set;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Jedis;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import dev.nulprit.tele.GoCommand;
import dev.nulprit.tele.SetCommand;
import dev.nulprit.tele.PlacesCompleter;

public class Tele extends JavaPlugin {
	private JedisPool pool;
	public FileConfiguration config = getConfig();

	@Override
	public void onEnable() {
		this.saveDefaultConfig();

		pool = new JedisPool("localhost");

		this.getCommand("go").setExecutor(new GoCommand(this));
		this.getCommand("go").setTabCompleter(new PlacesCompleter(this, true));

		this.getCommand("set").setExecutor(new SetCommand(this));
		this.getCommand("set").setTabCompleter(new PlacesCompleter(this, false));
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

	public Long cooldown(Player p) {
		try (Jedis jedis = pool.getResource()) {
			Long result = jedis.ttl("tele:" + p.getUniqueId() + ":cooldown");

			if (result == -1) return Long.MAX_VALUE; // no expiry, max length
			if (result == -2 || result == 0) { // nonexistent or about to expire
				if (config.getInt("cooldown") > 0) {
					jedis.setex("tele:" + p.getUniqueId() + ":cooldown", config.getInt("cooldown"), "");
				}
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
			String back = jedis.get("tele:" + p.getUniqueId() + ":back");
			if (back == null) return null;
			return decodeLocation(back);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public Set<String> getPlaceNames(Player p) {
		try (Jedis jedis = pool.getResource()) {
			return jedis.hkeys("tele:" + p.getUniqueId() + ":places");
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.<String>emptySet();
		}
	}

	public Long getPlaceCount(Player p) {
		try (Jedis jedis = pool.getResource()) {
			return jedis.hlen("tele:" + p.getUniqueId() + ":places");
		} catch (Exception e) {
			e.printStackTrace();
			return 0l;
		}
	}

	public Location getPlace(Player p, String name) {
		try (Jedis jedis = pool.getResource()) {
			String place = jedis.hget("tele:" + p.getUniqueId() + ":places", name);
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
			jedis.hset("tele:" + p.getUniqueId() + ":places", name, encoded);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void teleport(Player p, Location place) {
		if (config.getBoolean("enable-back")) {
			try (Jedis jedis = pool.getResource()) {
				String back = encodeLocation(p.getLocation());

				int expire = config.getInt("back-expire");
				if (expire > 0) {
					jedis.setex("tele:" + p.getUniqueId() + ":back", expire, back);
				} else {
					jedis.set("tele:" + p.getUniqueId() + ":back", back);
				}
			} catch (Exception e) {
				e.printStackTrace();
				p.sendMessage("Couldn't save \"back\" location");
			}
		}
		p.teleport(place);
	}
}