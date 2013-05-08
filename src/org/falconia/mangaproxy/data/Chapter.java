package org.falconia.mangaproxy.data;

import java.io.Serializable;

import org.falconia.mangaproxy.AppUtils;
import org.falconia.mangaproxy.plugin.IPlugin;
import org.falconia.mangaproxy.plugin.Plugins;
import org.falconia.mangaproxy.utils.HttpUtils;

import android.text.TextUtils;

public final class Chapter implements Serializable {

	private static final long serialVersionUID = 1L;

	public static Chapter getFavoriteChapter(int _id, Manga manga, String chapterId, String displayname,
			int pageIndexMax, int pageIndexLastRead) {
		Chapter chapter = new Chapter(chapterId, displayname, manga);
		chapter._id = _id;
		chapter.pageIndexMax = pageIndexMax;
		chapter.pageIndexLastRead = pageIndexLastRead;
		chapter.isFavorite = true;
		return chapter;
	}

	public static final int TYPE_ID_VOLUME = 0;
	public static final int TYPE_ID_CHAPTER = 1;
	public static final int TYPE_ID_UNKNOW = 2;

	public static final int IMG_SERVER_ID_NONE = -1;

	public final int siteId;
	public transient Manga manga;
	public final String chapterId;
	public final String displayname;

	public int pageIndexMax;
	public int pageIndexLastRead;

	// for Favorite
	public boolean isFavorite = false;

	// for Database
	public long _id = -1;

	// for Other
	public int typeId = TYPE_ID_UNKNOW;

	private transient String dynamicImgServersUrl;
	private transient String[] dynamicImgServers;
	private int dynamicImgServerId = IMG_SERVER_ID_NONE;

	public Chapter(String chapterId, String displayname, Manga manga) {
		this.chapterId = chapterId;
		this.displayname = displayname;
		this.manga = manga;
		siteId = manga.siteId;
		pageIndexMax = 0;
		pageIndexLastRead = 0;
	}

	private IPlugin getPlugin() {
		return Plugins.getPlugin(siteId);
	}

	public String getSiteCharset() {
		return getPlugin().getCharset();
	}

	public String getUrl() {
		return getPlugin().getChapterUrl(this, manga);
	}

	public void setDynamicImgServersUrl(String spec) {
		if (TextUtils.isEmpty(spec)) {
			AppUtils.logE(this, "Invalid Dynamic ImgServers URL.");
			return;
		}
		String url = HttpUtils.joinUrl(getPlugin().getUrlBase(), spec);
		if (!TextUtils.isEmpty(url)) {
			dynamicImgServersUrl = url.toString();
		}
	}

	public void setDynamicImgServers(String[] imgServers) {
		dynamicImgServers = imgServers;
	}

	public void setDynamicImgServerId(int imgServerId) {
		dynamicImgServerId = imgServerId;
	}

	public boolean hasDynamicImgServersUrl() {
		return TextUtils.isEmpty(dynamicImgServersUrl);
	}

	public boolean hasDynamicImgServers() {
		return dynamicImgServers != null && dynamicImgServers.length > 0;
	}

	public boolean hasDynamicImgServerId() {
		return dynamicImgServerId != IMG_SERVER_ID_NONE;
	}

	public boolean hasDynamicImgServer() {
		return hasDynamicImgServerId() && hasDynamicImgServers() && dynamicImgServerId >= 0
				&& dynamicImgServerId < dynamicImgServers.length;
	}

	public String getDynamicImgServersUrl() {
		return dynamicImgServersUrl;
	}

	public int getDynamicImgServerId() {
		return dynamicImgServerId;
	}

	public String getDynamicImgServer() {
		if (!hasDynamicImgServer()) {
			throw new NullPointerException("Dynamic Img Server is not set.");
		}
		return dynamicImgServers[dynamicImgServerId];
	}

	public String[] getPageUrls(String source, String url) {
		return getPlugin().getChapterPages(source, url, this);
	}

	public String getPageRedirectUrl(String source, String url) {
		return getPlugin().getPageRedirectUrl(source, url);
	}

	public boolean setDynamicImgServers(String source, String url) {
		return getPlugin().setDynamicImgServers(source, url, this);
	}

	@Override
	public String toString() {
		return String.format("{%s:%s}", chapterId, displayname);
	}

	public String toLongString() {
		return String.format(
				"{ SiteID:%d, MangaID:'%s', ChapterId:'%s', Name:'%s', TypeId:%d, ImgServerId:%d, ImgServers:%d }",
				siteId, manga.mangaId, chapterId, displayname, typeId, dynamicImgServerId,
				(dynamicImgServers == null ? 0 : dynamicImgServers.length));
	}

}
