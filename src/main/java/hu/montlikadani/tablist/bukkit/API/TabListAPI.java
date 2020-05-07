package hu.montlikadani.tablist.bukkit.API;

import java.lang.reflect.Field;
import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import hu.montlikadani.tablist.bukkit.FakePlayers;
import hu.montlikadani.tablist.bukkit.TabList;
import hu.montlikadani.tablist.bukkit.TeamHandler;
import hu.montlikadani.tablist.bukkit.tablist.TabManager;
import hu.montlikadani.tablist.bukkit.tablist.TabTitle;
import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;

/**
 * The API methods for TabList.
 * 
 * @author montlikadani
 *
 */
public class TabListAPI {

	/**
	 * Returns TabListAPI as a plugin
	 * 
	 * @return Plugin
	 */
	public static TabList getPlugin() {
		return JavaPlugin.getPlugin(TabList.class);
	}

	/**
	 * Gets the plugin instance.
	 * 
	 * @return TabList instance
	 */
	public static TabList getInstance() {
		return TabList.getInstance();
	}

	/**
	 * Checks whatever the tablist toggled for the specified player uuid. This is
	 * much slower to respond because it converts the string to uuid.
	 * 
	 * @param uuid Player UUID
	 * @return true if toggled
	 */
	public static boolean isTabListToggled(String uuid) {
		Validate.notNull(uuid, "Player UUID can't be null!");
		Validate.notEmpty(uuid, "Player UUID can't be empty!");

		return isTabListToggled(UUID.fromString(uuid));
	}

	/**
	 * Checks whatever the tablist toggled for the specified player.
	 * 
	 * @param uuid Player
	 * @return true if toggled
	 */
	public static boolean isTabListToggled(Player player) {
		Validate.notNull(player, "Player can't be null!");

		return isTabListToggled(player.getUniqueId());
	}

	/**
	 * Checks whatever the tablist toggled for the specified player uuid.
	 * 
	 * @param uuid Player UUID
	 * @return true if toggled
	 */
	public static boolean isTabListToggled(UUID uuid) {
		Validate.notNull(uuid, "Player UUID can't be null!");

		return TabManager.TABENABLED.containsKey(uuid) && TabManager.TABENABLED.get(uuid);
	}

	/**
	 * Sends the tab header and footer to the given player
	 * 
	 * @param p Player
	 * @param string Header
	 * @param string Footer
	 */
	public static void sendTabList(Player p, String header, String footer) {
		Validate.notNull(p, "Player can't be null!");

		TabTitle.sendTabTitle(p, header, footer);
	}

	/**
	 * Sends the tab header and footer to all online players
	 * 
	 * @param string Header
	 * @param string Footer
	 */
	public static void sendTabList(String header, String footer) {
		Bukkit.getOnlinePlayers().forEach(pls -> TabTitle.sendTabTitle(pls, header, footer));
	}

	/**
	 * Creates a new fake player that only appear in tablist.
	 * 
	 * @param player the player who's own that player
	 * @param name the fake player name
	 * @return {@link FakePlayers}
	 */
	public static FakePlayers createFakePlayer(Player player, String name) {
		FakePlayers fp = new FakePlayers(name);
		fp.createFakeplayer(player);
		return fp;
	}

	/**
	 * Gets a group by name.
	 * 
	 * @param name Group name
	 * @return {@link TeamHandler} if exists
	 */
	public static TeamHandler getGroup(String name) {
		return getInstance().getGroups().getTeam(name);
	}

	/**
	 * Gets the current ping of player
	 * 
	 * @param player Player
	 * @return Ping integer
	 */
	public static int getPing(Player p) {
		Validate.notNull(p, "Player can't be null!");

		int pingInt = 0;
		try {
			Object nmsPlayer = ReflectionUtils.getNMSPlayer(p);
			Field ping = ReflectionUtils.getField(nmsPlayer, "ping", false);
			pingInt = ping.getInt(nmsPlayer);
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return pingInt;
	}

	/**
	 * Gets the current tps of server.
	 * 
	 * @return The current TPS
	 */
	public static double getTPS() {
		double tps = 0d;
		try {
			Object mc = ReflectionUtils.invokeMethod(Bukkit.getServer(), "getServer", false);
			Field rec = ReflectionUtils.getField(mc, "recentTps", false);
			double[] recentTps = (double[]) rec.get(mc);
			tps = recentTps[0];
		} catch (Throwable t) {// TODO: Fix CraftBukkit TPS bug
		}

		return tps;
	}
}
