package org.falconia.mangaproxy.plugin;

import java.util.HashMap;

public final class Plugins {
	private static HashMap<Integer, IPlugin> Plugins;

	static {
		Plugins = new HashMap<Integer, IPlugin>();
		//Plugins.put(1000, new Plugin99770(1000));
		//Plugins.put(1001, new PluginDm5(1001));
		//Plugins.put(1002, new Plugin131(1002));
		//Plugins.put(1003, new PluginHhcomic(1003));
		Plugins.put(1004, new PluginEX(1004));
	}

	public static Integer[] getPluginIds() {
		return Plugins.keySet().toArray(new Integer[0]);
	}

	public static IPlugin getPlugin(int id) {
		return Plugins.get(id);
	}
}
