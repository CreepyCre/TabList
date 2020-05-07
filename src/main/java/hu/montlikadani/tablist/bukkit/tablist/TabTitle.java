package hu.montlikadani.tablist.bukkit.tablist;

import org.bukkit.entity.Player;

import hu.montlikadani.tablist.bukkit.utils.ReflectionUtils;
import hu.montlikadani.tablist.bukkit.utils.Util;
import hu.montlikadani.tablist.bukkit.utils.ServerVersion.Version;

public class TabTitle {

	public static void sendTabTitle(Player player, String header, String footer) {
		if (header == null) header = "";
		if (footer == null) footer = "";

		header = Util.colorMsg(header);
		footer = Util.colorMsg(footer);

		try {
			java.lang.reflect.Constructor<?> titleConstructor = null;
			try {
				Object tabHeader = ReflectionUtils.getAsIChatBaseComponent(header);
				Object tabFooter = ReflectionUtils.getAsIChatBaseComponent(footer);
				titleConstructor = ReflectionUtils.getNMSClass("PacketPlayOutPlayerListHeaderFooter").getConstructor();
				Object packet = titleConstructor.newInstance();
				if (Version.isCurrentEqualOrHigher(Version.v1_13_R1)) {
					ReflectionUtils.setField(packet, "header", tabHeader);
					ReflectionUtils.setField(packet, "footer", tabFooter);
				} else {
					ReflectionUtils.setField(packet, "a", tabHeader);
					ReflectionUtils.setField(packet, "b", tabFooter);
				}
				ReflectionUtils.sendPacket(player, packet);
			} catch (Exception f) {
				if (Version.isCurrentEqualOrHigher(Version.v1_12_R1)) {
					titleConstructor = ReflectionUtils.getNMSClass("PacketPlayOutPlayerListHeaderFooter")
							.getConstructor();
				} else {
					Object tabHeader = ReflectionUtils.getAsIChatBaseComponent(header);
					titleConstructor = ReflectionUtils.getNMSClass("PacketPlayOutPlayerListHeaderFooter")
							.getConstructor(new Class[] { tabHeader.getClass() });
				}

				Object tabFooter = ReflectionUtils.getAsIChatBaseComponent(footer);
				try {
					ReflectionUtils.setField(titleConstructor, "b", tabFooter);
				} catch (Throwable e) {
					e.printStackTrace();
				}

				ReflectionUtils.sendPacket(player, titleConstructor);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}