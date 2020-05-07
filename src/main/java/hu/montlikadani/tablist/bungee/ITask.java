package hu.montlikadani.tablist.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public interface ITask {

	void start();

	ScheduledTask getTask();

	void update(ProxiedPlayer player);

	void cancel();
}
