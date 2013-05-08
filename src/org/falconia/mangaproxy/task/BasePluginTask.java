package org.falconia.mangaproxy.task;

import org.falconia.mangaproxy.plugin.IPlugin;
import org.falconia.mangaproxy.plugin.Plugins;

import android.os.AsyncTask;

public abstract class BasePluginTask<T1, T2, T3> extends AsyncTask<T1, T2, T3> {

	protected final IPlugin mPlugin;

	public BasePluginTask(IPlugin plugin) {
		this.mPlugin = plugin;
	}

	public BasePluginTask(int pluginId) {
		this(Plugins.getPlugin(pluginId));
	}

}
