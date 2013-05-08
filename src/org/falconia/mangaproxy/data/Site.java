package org.falconia.mangaproxy.data;

import java.io.Serializable;
import java.util.HashMap;

import org.falconia.mangaproxy.plugin.IPlugin;
import org.falconia.mangaproxy.plugin.Plugins;

public final class Site implements Serializable {

	private static final long serialVersionUID = 1L;

	public final static int SITE_ID_FAVORITE = -1;

	private final static HashMap<Integer, Site> mSites;

	static {
		mSites = new HashMap<Integer, Site>();
		Integer[] ids = Plugins.getPluginIds();
		for (int i = 0; i < ids.length; i++) {
			mSites.put(ids[i], new Site(Plugins.getPlugin(ids[i])));
		}
	}

	public static boolean contains(int id) {
		return mSites.containsKey(id);
	}

	public static Site get(int id) {
		return mSites.get(id);
	}

	public static Integer[] getIds() {
		return mSites.keySet().toArray(new Integer[0]);
	}

	private final IPlugin mPlugin;
	private final int mSiteId;

	public Site(IPlugin plugin) {
		mPlugin = plugin;
		mSiteId = plugin.getSiteId();
	}

	public Site(int siteId) {
		mPlugin = Plugins.getPlugin(siteId);
		mSiteId = siteId;
	}

	public String getName() {
		return mPlugin.getName();
	}

	public String getDisplayname() {
		return mPlugin.getDisplayname();
	}

	public String getGenreListUrl() {
		return mPlugin.getGenreListUrl();
	}

	public Genre getGenreAll() {
		return mPlugin.getGenreAll();
	}

	public GenreSearch getGenreSearch(String search) {
		return mPlugin.getGenreSearch(search);
	}

	public GenreList getGenreList(String source, String url) {
		GenreList list = new GenreList(mSiteId);
		GenreList listParsed = mPlugin.getGenreList(source, url);
		if (listParsed != null && listParsed.size() > 0) {
			if (getGenreAll() != null)
				list.add(getGenreAll());
			list.addAll(listParsed.toArray());
		}

		return list;
	}

	public int getSiteId() {
		return mSiteId;
	}

	public boolean hasGenreList() {
		return mPlugin.hasGenreList();
	}

	public boolean hasSearchEngine() {
		return mPlugin.hasSearchEngine();
	}

}
