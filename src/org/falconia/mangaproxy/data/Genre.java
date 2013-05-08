package org.falconia.mangaproxy.data;

import java.io.Serializable;
import java.util.TimeZone;

import org.falconia.mangaproxy.plugin.IPlugin;
import org.falconia.mangaproxy.plugin.Plugins;

public class Genre implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String GENRE_UNKNOWN_ID = "GENRE_UNKNOWN";
	public static final String GENRE_ALL_ID = "GENRE_ALL";
	public static final String GENRE_SEARCH_ID = "GENRE_SEARCH";

	public final int siteId;
	public final String genreId;
	public final String displayname;

	public Genre(String genreId, String displayname, int siteId) {
		this.genreId = genreId;
		this.displayname = displayname;
		this.siteId = siteId;
	}

	protected IPlugin getPlugin() {
		return Plugins.getPlugin(siteId);
	}

	public String getSiteName() {
		return getPlugin().getName();
	}

	public String getSiteDisplayname() {
		return getPlugin().getDisplayname();
	}

	public TimeZone getTimeZone() {
		return getPlugin().getTimeZone();
	}

	public String getUrl(int page) {
		if (isGenreAll()) {
			return getPlugin().getGenreAllUrl(page);
		} else {
			return getPlugin().getGenreUrl(this, page);
		}
	}

	public String getUrl() {
		return getUrl(1);
	}

	public boolean isGenreAll() {
		return genreId.equals(GENRE_ALL_ID);
	}

	public boolean isGenreSearch() {
		return genreId.equals(GENRE_SEARCH_ID);
	}

	public MangaList getMangaList(String source, String url) {
		if (isGenreAll()) {
			return getPlugin().getAllMangaList(source, url);
		} else {
			return getPlugin().getMangaList(source, url, this);
		}
	}

	@Override
	public String toString() {
		return String.format("{%s:%s}", genreId, displayname);
	}

}
