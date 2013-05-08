package org.falconia.mangaproxy.data;

import java.io.Serializable;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.falconia.mangaproxy.App;
import org.falconia.mangaproxy.plugin.IPlugin;
import org.falconia.mangaproxy.plugin.Plugins;


import android.graphics.Bitmap;
import android.text.TextUtils;

public final class Manga implements Serializable {

	private static final long serialVersionUID = 1L;

	public static Manga getFavoriteManga(int _id, int siteId, String mangaId, String displayname, boolean isCompleted,
			int chapterCount, boolean hasNewChapter, String latestChapterId, String latestChapterDisplayname,
			String lastReadChapterId, long updatedAt, String updateAtTimeZone) {
		Manga manga = new Manga(mangaId, displayname, null, siteId);
		manga._id = _id;
		manga.isCompleted = isCompleted;
		manga.chapterCount = chapterCount;
		manga.hasNewChapter = hasNewChapter;
		manga.latestChapterId = latestChapterId;
		manga.latestChapterDisplayname = latestChapterDisplayname;
		manga.lastReadChapterId = lastReadChapterId;
		if (updatedAt > 0 && !TextUtils.isEmpty(updateAtTimeZone)) {
			manga.updatedAt = new GregorianCalendar(TimeZone.getTimeZone(updateAtTimeZone));
			manga.updatedAt.setTimeInMillis(updatedAt);
		}
		manga.isFavorite = true;
		return manga;
	}
	
	public static Manga getFavoriteManga(int _id, int siteId, String mangaId, String displayname, boolean isCompleted,
			int chapterCount, boolean hasNewChapter, String latestChapterId, String latestChapterDisplayname,
			String lastReadChapterId, long updatedAt, String updateAtTimeZone, String section) {
		Manga manga = new Manga(mangaId, displayname, null, siteId);
		manga._id = _id;
		manga.isCompleted = isCompleted;
		manga.chapterCount = chapterCount;
		manga.hasNewChapter = hasNewChapter;
		manga.latestChapterId = latestChapterId;
		manga.latestChapterDisplayname = latestChapterDisplayname;
		manga.lastReadChapterId = lastReadChapterId;
		if (updatedAt > 0 && !TextUtils.isEmpty(updateAtTimeZone)) {
			manga.updatedAt = new GregorianCalendar(TimeZone.getTimeZone(updateAtTimeZone));
			manga.updatedAt.setTimeInMillis(updatedAt);
		}
		manga.isFavorite = true;
		manga.section = section;
		return manga;
	}
	public final int siteId;
	public final String mangaId;
	public final String displayname;

	public String section;
	public boolean isCompleted = false;
	public GregorianCalendar updatedAt;
	public int chapterCount;
	public String author;
	public String chapterDisplayname;
	public String details;
	public String thumbUrl = "";

	private String mDetailsTemplate;

	// for Intent
	public ChapterList chapterList = null;

	// for Favorite
	public boolean isFavorite = false;
	public boolean hasNewChapter = true;
	public String latestChapterId = null;
	public String latestChapterDisplayname = null;
	public String lastReadChapterId = null;

	// for Database
	public long _id = -1;

	// for Extra Info
	public transient Bitmap extraInfoCoverBitmap = null;
	public transient String extraInfoArtist = null;
	public transient String extraInfoAuthor = null;
	public transient String extraInfoGenre = null;
	public transient String extraInfoSummary = null;

	public Manga(String mangaId, String displayname, String section, int siteId) {
		this.mangaId = mangaId;
		this.displayname = displayname;
		this.section = section;
		this.siteId = siteId;
		
	}

	public int getId() {
		return String.format("%s - %s", getSiteName(), mangaId).hashCode();
	}

	private IPlugin getPlugin() {
		return Plugins.getPlugin(siteId);
	}

	public String getSiteName() {
		return getPlugin().getName();
	}

	public String getSiteDisplayname() {
		return getPlugin().getDisplayname();
	}

	public String getSiteCharset() {
		return getPlugin().getCharset();
	}

	public String getUrl() {
		return getPlugin().getMangaUrl(this);
	}

	public boolean usingDynamicImgServer() {
		return getPlugin().usingDynamicImgServer();
	}

	public boolean usingImgRedirect() {
		return getPlugin().usingImgRedirect();
	}

	public String setDetailsTemplate(String template) {
		return mDetailsTemplate = template;
	}

	public String getDetails() {
		if (!TextUtils.isEmpty(mDetailsTemplate)) {
			String result = mDetailsTemplate;
			result = result.replaceAll("%chapterDisplayname%", chapterDisplayname);
			result = result.replaceAll("%chapterCount%",
					String.format(App.UI_CHAPTER_COUNT, chapterCount == 0 ? "??" : chapterCount));
			result = result.replaceAll("%author%", String.format(App.UI_AUTHOR, author));
			result = result.replaceAll("%updatedAt%", String.format(App.UI_LAST_UPDATE, updatedAt));
			result = result.replaceAll("%details%", details);
			return result;
		}
		return TextUtils.isEmpty(details) ? "-" : details;
	}

	// for Favorite

	public void setLatestChapter(Chapter chapter) {
		// TODO Update database
		latestChapterId = chapter.chapterId;
		latestChapterDisplayname = chapter.displayname;
	}

	// for extra info

	public void resetExtraInfo() {
		extraInfoAuthor = null;
		extraInfoArtist = null;
		extraInfoGenre = null;
		extraInfoSummary = null;
		if (extraInfoCoverBitmap != null) {
			extraInfoCoverBitmap.recycle();
			extraInfoCoverBitmap = null;
		}
	}

	public ChapterList getChapterList(String source, String url) {
		return getPlugin().getChapterList(source, url, this);
	}

	@Override
	public String toString() {
		return String.format("{%s:%s}", mangaId, displayname);
	}

	public String toLongString() {
		return String
				.format("{ SiteID:%d, MangaID:'%s', Name:'%s', Section:'%s', UpdatedAt:'%tF', Chapter:'%s', ChapterCount:%d, Author:%s, IsCompleted:%b, HasNewChapter:%b }",
						siteId, mangaId, displayname, section, updatedAt, chapterDisplayname, chapterCount, author,
						isCompleted, hasNewChapter);
	}

}
