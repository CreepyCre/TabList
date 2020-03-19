package hu.montlikadani.tablist.bukkit;

import static hu.montlikadani.tablist.bukkit.utils.Util.colorMsg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;
import ru.tehkode.permissions.bukkit.PermissionsEx;

@SuppressWarnings("deprecation")
public class Groups {

	private TabList plugin;

	private BukkitTask animationTask = null;
	private Integer simpleTask = -1;

	private final List<TeamHandler> groupsList = new ArrayList<>();

	public Groups(TabList plugin) {
		this.plugin = plugin;
	}

	public List<TeamHandler> getGroupsList() {
		return groupsList;
	}

	protected void load() {
		cancelUpdate(true);

		String path = "change-prefix-suffix-in-tablist.";
		if (!plugin.getC().getBoolean(path + "enable")) {
			return;
		}

		plugin.getConf().createGroupsFile();

		if (plugin.getGS().contains("groups")) {
			for (String g : plugin.getGS().getConfigurationSection("groups").getKeys(false)) {
				if (g.equalsIgnoreCase("exampleGroup")) {
					continue;
				}

				String prefPath = "groups." + g + ".prefix";
				String sufPath = "groups." + g + ".suffix";

				String prefix = plugin.getGS().getString(prefPath, "");
				String suffix = plugin.getGS().getString(sufPath, "");

				int priority = plugin.getGS().getInt("groups." + g + ".sort-priority", 0);

				groupsList.add(new TeamHandler(g, prefix, suffix, priority));
			}
		}

		// Sort groups by priority (Not sure this needed)
		/*for (int i = 0; i < groupsList.size(); i++) {
			for (int j = groupsList.size() - 1; j > i; j--) {
				int p1 = groupsList.get(i).getPriority();
				int p2 = groupsList.get(j).getPriority();

				if (p1 > p2) {
					TeamHandler t = groupsList.get(i);
					groupsList.set(i, groupsList.get(j));
					groupsList.set(j, t);
				}
			}
		}*/

		int refInt = plugin.getC().getInt(path + "refresh-interval");
		startTask(refInt, null);
	}

	public void loadGroupForPlayer(final Player p) {
		removeGroup(p);

		String path = "change-prefix-suffix-in-tablist.";
		if (!plugin.getC().getBoolean(path + "enable")) {
			return;
		}

		int refInt = plugin.getC().getInt(path + "refresh-interval");
		startTask(refInt, p);
	}

	private void setGroup(Player p) {
		for (final TeamHandler team : groupsList) {
			String name = team.getTeam();

			if (name.equalsIgnoreCase(p.getName())) {
				setName(p, team);
				break;
			}

			boolean change = false;
			if (plugin.getC().getBoolean("change-prefix-suffix-in-tablist.use-vault-group-names", false)
					&& plugin.isPluginEnabled("Vault")) {
				for (String gn : plugin.getVaultPerm().getPlayerGroups(p)) {
					if (gn.equalsIgnoreCase(name)) {
						change = true;
						break;
					}
				}
			} else if (plugin.isPluginEnabled("PermissionsEx")) {
				String gPerm = plugin.getGS().getString("groups." + name + ".permission", "");
				if (gPerm.trim().isEmpty()) {
					gPerm = "tablist." + name;
				}

				if (plugin.getGS().getBoolean("groups." + name + ".bypass-operator", false)) {
					if (p.hasPermission(new org.bukkit.permissions.Permission(gPerm, PermissionDefault.FALSE))) {
						change = true;
					}
				} else if (PermissionsEx.getPermissionManager().has(p, gPerm)) {
					change = true;
				}
			} else {
				String gPerm = plugin.getGS().getString("groups." + name + ".permission", "");
				if (gPerm.trim().isEmpty()) {
					gPerm = "tablist." + name;
				}

				// TODO Fix the operator player if has not permission then gets the default group
				// Solution: You need to add "-" mark before the permission or writing to false
				if (plugin.getGS().getBoolean("groups." + name + ".bypass-operator", false)) {
					if (p.hasPermission(new org.bukkit.permissions.Permission(gPerm, PermissionDefault.FALSE))) {
						change = true;
					}
				} else if (p.hasPermission(gPerm)) {
					change = true;
				}
			}

			if (change) {
				setName(p, team);
				break;
			}
		}
	}

	private void setName(Player p, TeamHandler team) {
		String prefix = plugin.getPlaceholders().replaceVariables(p, plugin.makeAnim(team.getPrefix()));
		String suffix = plugin.getPlaceholders().replaceVariables(p, plugin.makeAnim(team.getSuffix()));

		String phPath = "placeholder-format.afk-status.";
		if (plugin.getC().getBoolean(phPath + "enable") && plugin.isAfk(p, false)
				&& !plugin.getC().getBoolean(phPath + "show-player-group")) {
			return;
		}

		final boolean rightLeft = plugin.getC().getBoolean(phPath + "show-in-right-or-left-side");

		if (plugin.getChangeType().equals("scoreboard")) {
			if (plugin.getC().getBoolean(phPath + "enable")) {
				if (rightLeft) {
					suffix = suffix + colorMsg(
							plugin.getC().getString(phPath + "format-" + (plugin.isAfk(p, false) ? "yes" : "no"), ""));
				} else {
					prefix = colorMsg(
							plugin.getC().getString(phPath + "format-" + (plugin.isAfk(p, false) ? "yes" : "no"), ""))
							+ prefix;
				}
			}

			// TODO: Improve sort-priority work ability
			if (plugin.getC().getBoolean("change-prefix-suffix-in-tablist.use-improved-group-sorting", false)) {
				final String pref = prefix;
				final String suf = suffix;
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
						() -> setFixedPlayerTeam(p, pref, suf, team.getFullTeamName()));
			} else {
				setPlayerTeam(p, prefix, suffix, team.getFullTeamName());
			}
		} else if (plugin.getChangeType().equals("namer")) {
			String result = "";

			if (plugin.getC().getBoolean(phPath + "enable")) {
				if (plugin.isAfk(p, false)) {
					result = colorMsg(rightLeft
							? prefix + p.getName() + suffix + plugin.getC().getString(phPath + "format-yes", "")
							: plugin.getC().getString(phPath + "format-yes", "") + prefix + p.getName() + suffix);
				} else {
					prefix = colorMsg(rightLeft ? prefix + plugin.getC().getString(phPath + "format-no", "")
							: plugin.getC().getString(phPath + "format-no", "") + prefix);
					suffix = colorMsg(rightLeft ? suffix + plugin.getC().getString(phPath + "format-no", "")
							: plugin.getC().getString(phPath + "format-no", "") + suffix);
				}
			}

			if (result.isEmpty()) {
				if (plugin.isPluginEnabled("Essentials")) {
					User user = JavaPlugin.getPlugin(Essentials.class).getUser(p);
					if (plugin.getC().getBoolean("change-prefix-suffix-in-tablist.use-essentials-nickname")
							&& user.getNickname() != null) {
						result = prefix + user.getNickname() + suffix;
					} else {
						result = prefix + p.getName() + suffix;
					}
				} else {
					result = prefix + p.getName() + suffix;
				}
			}

			if (!result.isEmpty()) {
				p.setPlayerListName(result);
			}
		}
	}

	private void setFixedPlayerTeam(Player player, String prefix, String suffix, String name) {
		Scoreboard tboard = Bukkit.getScoreboardManager().getNewScoreboard();
		Team team = tboard.getTeam(name);
		if (team == null) {
			team = tboard.registerNewTeam(name);
		}

		Objective objective = tboard.getObjective("tabl");
		if (objective == null) {
			objective = tboard.registerNewObjective("tabl", "dummy", "Tab");
		}

		prefix = Util.splitStringByVersion(prefix);
		suffix = Util.splitStringByVersion(suffix);

		if (Version.isCurrentLower(Version.v1_9_R1)) {
			if (!team.hasPlayer(player)) {
				team.addPlayer(player);
			}
		} else if (!team.hasEntry(player.getName())) {
			team.addEntry(player.getName());
		}

		player.setPlayerListName(prefix + player.getName() + suffix);
		objective.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.PLAYER_LIST);
		for (Player pl : Bukkit.getOnlinePlayers()) {
			pl.setScoreboard(tboard);
		}
		//player.setScoreboard(tboard);
	}

	private void setPlayerTeam(Player player, String prefix, String suffix, String name) {
		Scoreboard tboard = player.getScoreboard();
		Team team = tboard.getTeam(name);
		if (team == null) {
			team = tboard.registerNewTeam(name);
		}

		prefix = Util.splitStringByVersion(prefix);
		suffix = Util.splitStringByVersion(suffix);

		team.setPrefix(prefix);
		team.setSuffix(suffix);

		if (Version.isCurrentEqualOrHigher(Version.v1_13_R1)) {
			team.setColor(fromPrefix(prefix));
		}

		if (Version.isCurrentLower(Version.v1_9_R1)) {
			if (!team.hasPlayer(player)) {
				team.addPlayer(player);
			}
		} else if (!team.hasEntry(player.getName())) {
			team.addEntry(player.getName());
		}

		player.setScoreboard(tboard);
	}

	public void removeGroupsFromAll() {
		Bukkit.getOnlinePlayers().forEach(this::removeGroup);
		groupsList.clear();
	}

	public void removeGroup(Player p) {
		if (p == null) {
			return;
		}

		for (Iterator<TeamHandler> it = groupsList.iterator(); it.hasNext();) {
			TeamHandler th = it.next();
			if (th == null) {
				continue;
			}

			if (plugin.getChangeType().equals("scoreboard")) {
				Scoreboard tboard = p.getScoreboard();
				Team team = tboard.getTeam(th.getFullTeamName());
				if (team == null) {
					continue;
				}

				if (Version.isCurrentLower(Version.v1_9_R1)) {
					if (team.hasPlayer(p)) {
						team.removePlayer(p);
					}
				} else if (team.hasEntry(p.getName())) {
					team.removeEntry(p.getName());
				}

				// do not unregister team to prevent removing teams from others
				/*
				 * team.setPrefix(""); team.setSuffix(""); try { team.unregister(); } catch
				 * (IllegalStateException e) { }
				 */

				p.setScoreboard(tboard);
			} else if (plugin.getChangeType().equals("namer")) {
				p.setPlayerListName(p.getName());
			}
		}
	}

	public void cancelUpdate() {
		cancelUpdate(false);
	}

	public void cancelUpdate(boolean removeGroup) {
		if (removeGroup) {
			removeGroupsFromAll();
		}

		if (simpleTask != -1) {
			Bukkit.getServer().getScheduler().cancelTask(simpleTask);
			simpleTask = -1;
		}

		if (animationTask != null) {
			animationTask.cancel();
			animationTask = null;
		}
	}

	public ChatColor fromPrefix(String prefix) {
		char colour = 0;
		char[] chars = prefix.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			char at = chars[i];
			if ((at == '\u00a7' || at == '&') && i + 1 < chars.length) {
				char code = chars[i + 1];
				if (ChatColor.getByChar(code) != null) {
					colour = code;
				}
			}
		}

		return colour == 0 ? ChatColor.RESET : ChatColor.getByChar(colour);
	}

	private void startTask(final int refreshInt, Player p) {
		if (refreshInt < 1) {
			if (p != null) {
				if (isPlayerCanSeeGroup(p)) {
					setGroup(p);
				}
			} else {
				for (final Player pla : Bukkit.getOnlinePlayers()) {
					if (isPlayerCanSeeGroup(pla)) {
						setGroup(pla);
					}
				}
			}

			return;
		}

		if (plugin.getC().getBoolean("change-prefix-suffix-in-tablist.enable-animation")) {
			if (animationTask == null) {
				animationTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
					if (Bukkit.getOnlinePlayers().isEmpty()) {
						animationTask.cancel();
						animationTask = null;
						return;
					}

					for (Player pl : Bukkit.getOnlinePlayers()) {
						if (isPlayerCanSeeGroup(pl)) {
							setGroup(pl);
						}
					}
				}, refreshInt, refreshInt);
			}
		} else {
			if (simpleTask == -1) {
				simpleTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
					@Override
					public void run() {
						if (Bukkit.getOnlinePlayers().isEmpty()) {
							Bukkit.getServer().getScheduler().cancelTask(simpleTask);
							simpleTask = -1;
							return;
						}

						for (Player pl : Bukkit.getOnlinePlayers()) {
							if (isPlayerCanSeeGroup(pl)) {
								setGroup(pl);
							}
						}
					}
				}, 0L, refreshInt * 20L);
			}
		}
	}

	private boolean isPlayerCanSeeGroup(Player p) {
		String path = "change-prefix-suffix-in-tablist.";
		if (plugin.getC().getBoolean(path + "disabled-worlds.use-as-whitelist", false)) {
			if (!plugin.getC().getStringList(path + "disabled-worlds.list").contains(p.getWorld().getName())) {
				return false;
			}
		} else {
			if (plugin.getC().getStringList(path + "disabled-worlds.list").contains(p.getWorld().getName())) {
				return false;
			}
		}

		if (plugin.isHookPreventTask(p)) {
			return false;
		}

		if (plugin.getC().getBoolean(path + "hide-group-when-player-vanished") && plugin.isVanished(p, false)) {
			removeGroup(p);
			return false;
		}

		if (plugin.getC().getBoolean(path + "hide-group-when-player-afk") && plugin.isAfk(p, false)) {
			removeGroup(p);
			return false;
		}

		if (plugin.getC().getBoolean(path + "use-displayname")) {
			p.setPlayerListName(p.getDisplayName());
			return false;
		}

		return true;
	}
}
