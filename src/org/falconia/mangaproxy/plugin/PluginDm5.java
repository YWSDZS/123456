package org.falconia.mangaproxy.plugin;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.falconia.mangaproxy.App;
import org.falconia.mangaproxy.data.Chapter;
import org.falconia.mangaproxy.data.ChapterList;
import org.falconia.mangaproxy.data.Genre;
import org.falconia.mangaproxy.data.GenreList;
import org.falconia.mangaproxy.data.GenreSearch;
import org.falconia.mangaproxy.data.Manga;
import org.falconia.mangaproxy.data.MangaList;
import org.falconia.mangaproxy.utils.FormatUtils;
import org.falconia.mangaproxy.utils.Regex;

import android.text.TextUtils;

public final class PluginDm5 extends PluginBase {
	protected static final String GENRE_ALL_ID = "new";

	protected static final String SEARCH_URL_FORMAT = "search?title=%s&page=%d";
	protected static final String MANGA_URL_PREFIX = "manhua-";
	protected static final String PAGE_REDIRECT_URL_PREFIX = "chapterimagefun.ashx";

	public PluginDm5(int siteId) {
		super(siteId);
	}

	@Override
	public String getName() {
		return "DM5";
	}

	@Override
	public String getDisplayname() {
		return "动漫屋";
	}

	@Override
	public String getCharset() {
		return CHARSET_UTF8;
	}

	@Override
	public TimeZone getTimeZone() {
		return TimeZone.getTimeZone("GMT+08:00");
	}

	@Override
	public String getUrlBase() {
		return "http://tel.dm5.com/";
	}

	@Override
	public boolean hasSearchEngine() {
		return true;
	}

	@Override
	public boolean hasGenreList() {
		return true;
	}

	@Override
	public boolean usingImgRedirect() {
		return true;
	}

	@Override
	public boolean usingDynamicImgServer() {
		return false;
	}

	@Override
	public String getGenreListUrl() {
		String url = getUrlBase() + "manhua-new/";
		logI(Get_URL_of_GenreList, url);
		return url;
	}

	@Override
	public String getGenreUrl(Genre genre, int page) {
		String url = getUrlBase();
		String id = genre.isGenreAll() ? GENRE_ALL_ID : genre.genreId;
		if (page == 1) {
			url = String.format("%smanhua-%s/", url, id);
		} else {
			url = String.format("%smanhua-%s-p%d/", url, id, page);
		}
		if (genre.isGenreAll()) {
			logI(Get_URL_of_AllMangaList, url);
		} else {
			logI(Get_URL_of_MangaList, genre.genreId, url);
		}
		return url;
	}

	@Override
	public String getGenreUrl(Genre genre) {
		return getGenreUrl(genre, 1);
	}

	@Override
	public String getGenreAllUrl(int page) {
		return getGenreUrl(getGenreAll(), page);
	}

	@Override
	public String getSearchUrl(GenreSearch genreSearch, int page) {
		String url = getUrlBase() + String.format(SEARCH_URL_FORMAT, genreSearch.search, page);
		logI(Get_URL_of_SearchMangaList, url);
		return url;
	}

	@Override
	public String getMangaUrlPrefix() {
		return MANGA_URL_PREFIX;
	}

	@Override
	public String getMangaUrlPostfix() {
		return DEFAULT_MANGA_URL_POSTFIX;
	}

	@Override
	public String getChapterUrl(Chapter chapter, Manga manga) {
		String url = getUrlBase() + String.format("m%s/", chapter.chapterId);
		logI(Get_URL_of_Chapter, chapter.chapterId, url);
		return url;
	}

	protected GregorianCalendar parseDate(String string) {
		GregorianCalendar calendar = null;
		calendar = parseDateTime(string, "(\\d+)[年-](\\d+)[月-](\\d+)日?{'YY','M','D'}");
		return calendar;
	}

	protected GregorianCalendar parseDateTime(String string) {
		GregorianCalendar calendar = null;
		calendar = parseDateTime(string, "(\\d+)-(\\d+)-(\\d+)\\s+(\\d+)\\:(\\d+)(?:\\:(\\d+))?{'YY','M','D','h','m','s'}");
		return calendar;
	}

	protected String parseAuthorName(String string) {
		string = string.replaceAll("(?si)</?a[^<>]*>", "");
		return parseName(string);
	}

	protected String parseChapterName(String string, String manga) {
		if (string.startsWith(manga + "漫画")) {
			string = string.substring(manga.length() + 2);
		} else if (string.startsWith(manga)) {
			string = string.substring(manga.length());
		}
		return parseName(string);
	}

	@Override
	protected int parseChapterType(String string) {
		if (Regex.isMatch("(?i)卷$|^VOL\\.", string)) {
			return Chapter.TYPE_ID_VOLUME;
		}
		if (Regex.isMatch("(?i)话$|回$|^CH\\.", string)) {
			return Chapter.TYPE_ID_CHAPTER;
		}
		return Chapter.TYPE_ID_UNKNOW;
	}

	@Override
	public Genre getGenreAll() {
		return new Genre(Genre.GENRE_ALL_ID, String.format("%s (今日漫画)", App.UI_GENRE_ALL_TEXT_ZH), getSiteId());
	}

	@Override
	public GenreList getGenreList(String source, String url) {
		GenreList list = new GenreList(getSiteId());

		logI(Get_GenreList);
		logD(Get_Source_Size_GenreList, FormatUtils.getFileSize(source, getCharset()));

		if (TextUtils.isEmpty(source)) {
			logE(Source_is_empty);
			return list;
		}

		try {
			long time = System.currentTimeMillis();

			String genreId, section;
			String pattern;
			ArrayList<String> groups;
			ArrayList<ArrayList<String>> matches;

			pattern = "(?is)<ul class=\"dm_nav\".*?>(.+?)</ul>.+?<div id=\"nav_fl2\">(.+?</div>).+?<div class=\"nav_zm[^<>]+>(.+?)</div>";
			groups = Regex.match(pattern, source);
			logD(Catched_sections, groups.size() - 1);

			// Section 1

			// Section 2
			section = "nav_fl2";
			pattern = "(?is)<a href=\"/manhua-([^\"]+)/\" title=\"([^\"]+)\"\\s*>(.+?)</a>";
			matches = Regex.matchAll(pattern, groups.get(2));
			logD(Catched_count_in_section, matches.size(), section);

			for (ArrayList<String> match : matches) {
				genreId = parseGenreId(match.get(1));
				list.add(genreId, parseGenreName(match.get(3)));
			}

			// Section 3
			section = "nav_zm";
			pattern = "(?is)<a href=\"/manhua-([^\"]+)/\">(.+?)</a>";
			matches = Regex.matchAll(pattern, groups.get(3));
			logD(Catched_count_in_section, matches.size(), section);

			for (ArrayList<String> match : matches) {
				genreId = parseGenreId(match.get(1));
				list.add(genreId, parseGenreName(match.get(2)));
			}

			time = System.currentTimeMillis() - time;
			logD(Process_Time_GenreList, time);

			logV(list.toString());

		} catch (Exception e) {
			e.printStackTrace();
			logE(Fail_to_process, "GenreList", url);
		}

		return list;
	}

	@Override
	public MangaList getMangaList(String source, String url, Genre genre) {
		logI(Get_MangaList, genre.genreId);
		logD(Get_Source_Size_MangaList, FormatUtils.getFileSize(source, getCharset()));

		return getMangaListBase(source, url, genre);
	}

	@Override
	public MangaList getAllMangaList(String source, String url) {
		logI(Get_AllMangaList);
		logD(Get_Source_Size_AllMangaList, FormatUtils.getFileSize(source, getCharset()));

		return getMangaListBase(source, url, getGenreAll());
	}

	private MangaList getMangaListBase(String source, String url, Genre genre) {
		MangaList list = new MangaList(getSiteId());

		if (TextUtils.isEmpty(source)) {
			logE(Source_is_empty);
			return list;
		}

		try {
			long time = System.currentTimeMillis();

			String pattern;
			ArrayList<String> groups;
			ArrayList<ArrayList<String>> matches;

			pattern = "(?is)<div class=\"innr3\">(.+?)</div>.+?当前第\\s*(?:<[^<>]+>)?\\d+/(\\d+)(?:</[^<>]+>)?\\s*页";
			groups = Regex.match(pattern, source);
			logD(Catched_sections, groups.size() - 1);

			// Section 1
			if (genre.genreId.equalsIgnoreCase("updated")) {
				pattern = "(?is)<li [^<>]+>\\s*<a href=\"/manhua-([^\"]+)/\" title=\"([^\"]+?)\"[^<>]*>.+?<strong>.+?</strong></a>.+?<br />漫画人气：(\\d+).+?\\[\\s*<a [^<>]*title=\"[^\"]+\"[^<>]*>(.+?)</a>：<a [^<>]+>(.+?)</a>\\s*\\]\\s*</li>";
				matches = Regex.matchAll(pattern, groups.get(1));
				logD(Catched_count_in_section, matches.size(), "Mangas");

				for (ArrayList<String> match : matches) {
					Manga manga = new Manga(parseId(match.get(1)), parseName(match.get(2)), null, getSiteId());
					manga.details = "HIT: " + match.get(3);
					manga.chapterDisplayname = parseChapterName(match.get(4), manga.displayname);
					manga.latestChapterDisplayname = manga.chapterDisplayname;
					manga.author = parseAuthorName(match.get(5));
					manga.setDetailsTemplate("%author%\n%chapterDisplayname%, %details%");
					list.add(manga, true);
					// logV(manga.toLongString());
				}
			} else {
				pattern = "(?is)<li [^<>]+>\\s*<a href=\"/manhua-([^\"]+)/\" title=\"([^\"]+?)\"[^<>]*>.+?<strong>.+?</strong></a>.+?<br />漫画家：<a [^<>]+>(.+?)</a>.+?\\[\\s*([\\d年月日]+)：<a [^<>]*title=\"[^\"]+\"[^<>]*>(.+?)</a>\\s*\\]\\s*</li>";
				matches = Regex.matchAll(pattern, groups.get(1));
				logD(Catched_count_in_section, matches.size(), "Mangas");

				for (ArrayList<String> match : matches) {
					Manga manga = new Manga(parseId(match.get(1)), parseName(match.get(2)), null, getSiteId());
					manga.author = parseName(match.get(3));
					manga.updatedAt = parseDate(match.get(4));
					manga.chapterDisplayname = parseName(match.get(5));
					manga.setDetailsTemplate("%author%\n%chapterDisplayname%, %updatedAt%");
					list.add(manga, true);
					// logV(manga.toLongString());
				}
			}

			// Section 2
			list.pageIndexMax = parseInt(groups.get(2));
			logV(Catched_in_section, groups.get(2), 2, "PageIndexMax", list.pageIndexMax);

			time = System.currentTimeMillis() - time;
			logD(Process_Time_MangaList, time);

			logV(list.toString());

		} catch (Exception e) {
			e.printStackTrace();
			logE(Fail_to_process, "MangaList", url);
		}

		return list;
	}

	@Override
	public MangaList getSearchMangaList(String source, String url) {
		MangaList list = new MangaList(getSiteId());

		logI(Get_SearchMangaList);
		logD(Get_Source_Size_SearchMangaList, FormatUtils.getFileSize(source, getCharset()));

		if (TextUtils.isEmpty(source)) {
			logE(Source_is_empty);
			return list;
		}

		try {
			long time = System.currentTimeMillis();

			String pattern;
			ArrayList<String> groups;
			ArrayList<ArrayList<String>> matches;

			pattern = "(?is)<div[^<>]*?\\s+id=\"search_nrl\"[^<>]*?>(.+)</div>\\s*<div[^<>]*?\\s+id=\"search_nrr\"[^<>]*?>.+?<a[^<>]*?>(\\d+)</a>\\s*<a[^<>]*?>下一页</a>";
			groups = Regex.match(pattern, source);
			logD(Catched_sections, groups.size() - 1);

			// Section 1
			pattern = "(?is)<div[^<>]*>.*?<dl[^<>]*>\\s*<a href=\"/manhua-([^\"]+)/\" title=\"([^\"]+?)\"[^<>]*>.*?</dl>.*?<dt[^<>]*>.*?漫画作者：.*?<a [^<>]+>(.+?)</a>.*?漫画人气：.*?(\\d+)次点击.*?漫画类型：(.*?)收藏人气：.*?(\\d+)次收藏.*?最后更新于 ([-0-9]+).*?（([^（）]+)）.*?最新章节.*?：.*?<a [^<>]+>(.+?)</a>.*?</dt>";
			matches = Regex.matchAll(pattern, groups.get(1));
			logD(Catched_count_in_section, matches.size(), "Mangas");

			for (ArrayList<String> match : matches) {
				Manga manga = new Manga(parseId(match.get(1)), parseName(match.get(2)), null, getSiteId());
				manga.author = parseAuthorName(match.get(3).replaceAll("(?is)<[^<>]+>", ""));
				manga.details = "HIT: " + match.get(4);
				manga.details = "类型: " + parseName(match.get(5).replaceAll("(?is)<[^<>]+>", "")) + ", " + manga.details;
				manga.updatedAt = parseDate(match.get(7));
				manga.isCompleted = parseIsCompleted(match.get(8));
				manga.chapterDisplayname = parseName(match.get(9));
				manga.setDetailsTemplate("%author%\n%details%\n%updatedAt%, %chapterDisplayname%");
				list.add(manga, true);
				// logV(manga.toLongString());
			}

			// Section 2
			list.pageIndexMax = parseInt(groups.get(2));
			logV(Catched_in_section, groups.get(2), 2, "PageIndexMax", list.pageIndexMax);

			time = System.currentTimeMillis() - time;
			logD(Process_Time_MangaList, time);

			logV(list.toString());

		} catch (Exception e) {
			e.printStackTrace();
			logE(Fail_to_process, "SearchMangaList", url);
		}

		return list;
	}

	@Override
	public ChapterList getChapterList(String source, String url, Manga manga) {
		ChapterList list = new ChapterList(manga);

		logI(Get_ChapterList, manga.mangaId);
		logD(Get_Source_Size_ChapterList, FormatUtils.getFileSize(source, getCharset()));

		if (TextUtils.isEmpty(source)) {
			logE(Source_is_empty);
			return list;
		}
		// logV(source);
		logV(manga.toLongString());

		try {
			long time = System.currentTimeMillis();

			
			int n;
			String pattern, section;
			ArrayList<String> groups;
			ArrayList<ArrayList<String>> matches;

			pattern = "(?is)漫画状态：([^<]+)<.+?更新时间：([\\d-]+\\s+[\\d:]+)<.+?<ul [^<>]*id=\"cbc_1\">(.+?)</ul>";

			if (Regex.isMatch(pattern, source)) {
				groups = Regex.match(pattern, source);
				logD(Catched_sections, groups.size() - 1);

				n = 1;
				section = "IsCompleted";
				manga.isCompleted = parseIsCompleted(groups.get(n));
				logV(Catched_in_section, groups.get(n), n, section, manga.isCompleted);

				n = 2;
				section = "UpdatedAt";
				manga.updatedAt = parseDateTime(groups.get(n));
				logV(Catched_in_section, groups.get(n), n, section, manga.updatedAt.getTime());
			} else {
				// http://www.gmanhua.com
				pattern = "(?is)状态：([^　\\s]+)[　\\s]+.+?更新时间：([\\d-]+\\s+[\\d:]+)[　\\s]+.+?<ul [^<>]*id=\"cbc_1\">(.+?)</ul>";
				groups = Regex.match(pattern, source);
				logD(Catched_sections, groups.size() - 1);

				n = 1;
				section = "IsCompleted";
				manga.isCompleted = parseIsCompleted(groups.get(n));
				logV(Catched_in_section, groups.get(n), n, section, manga.isCompleted);

				n = 2;
				section = "UpdatedAt";
				manga.updatedAt = parseDateTime(groups.get(n));
				logV(Catched_in_section, groups.get(n), n, section, manga.updatedAt.getTime());
			}

			n = 3;
			section = "ul";
			// logV(groups.get(3));
			pattern = "(?is)<li[^<>]*><a [^<>]*href=\"/m(\\d+)/\"[^<>]*>(.+?)</a>.+?</li>";
			matches = Regex.matchAll(pattern, groups.get(n));
			logD(Catched_count_in_section, matches.size(), section);

			for (ArrayList<String> groups2 : matches) {
				Chapter chapter = new Chapter(parseId(groups2.get(1)), parseChapterName(groups2.get(2),
						manga.displayname), manga);
				chapter.typeId = parseChapterType(chapter.displayname);
				list.add(chapter);
				// logV(chapter.toLongString());
			}

			time = System.currentTimeMillis() - time;
			logD(Process_Time_ChapterList, time);

			logV(list.toString());

		} catch (Exception e) {
			e.printStackTrace();
			logE(Fail_to_process, "ChapterList", url);
		}

		return list;
	}

	@Override
	public String[] getChapterPages(String source, String url, Chapter chapter) {
		String[] pageUrls = null;

		logI(Get_Chapter, chapter.chapterId);
		logD(Get_Source_Size_Chapter, FormatUtils.getFileSize(source, getCharset()));

		if (TextUtils.isEmpty(source)) {
			logE(Source_is_empty);
			return pageUrls;
		}

		try {
			long time = System.currentTimeMillis();

			String pattern;
			ArrayList<String> groups;

			pattern = "(?is)var DM5_CID=(\\d+);\\s+var DM5_IMAGE_COUNT=(\\d+);.+?id=\"dm5_key\" value=\"(.*?)\"(?:.+?("
					+ PACKED_PATTERN + "))?";//
			groups = Regex.match(pattern, source);
			logD(Catched_sections, groups.size() - 1);

			// Section 1
			int cid = parseInt(groups.get(1));
			logV(Catched_in_section, groups.get(1), 1, "DM5_CID", cid);

			// Section 2
			int count = parseInt(groups.get(2));
			logV(Catched_in_section, groups.get(2), 1, "DM5_IMAGE_COUNT", count);

			// Section 3
			String key = groups.get(3);
			logV(Catched_in_section, groups.get(3), 1, "DM5_KEY", key);

			// Section 4
			if (groups.get(4) != null) {
				String _p = decodePackedJs(groups.get(4));
				_p = Regex.match(";.+?=(.+?);", _p).get(1);
				key = _p.replaceAll("(^'|'$|'\\+')", "");
			}

			pageUrls = new String[count];
			for (int i = 0; i < count; i++) {
				pageUrls[i] = String.format("%s%s?cid=%d&page=%d&key=%s", getUrlBase(), PAGE_REDIRECT_URL_PREFIX, cid,
						i + 1, URLEncoder.encode(key));
			}

			time = System.currentTimeMillis() - time;
			logD(Process_Time_ChapterPages, time);

			// logV(chapter.toString());

		} catch (Exception e) {
			e.printStackTrace();
			logE(Fail_to_process, "ChapterPages", url);
		}

		return pageUrls;
	}

	@Override
	public String getPageRedirectUrl(String source, String url) {
		String newUrl = null;
		try {
			if (source.matches("(?is)" + PACKED_PATTERN + ".+")) {
				source = decodePackedJs(source);
			}

			if (source.matches("(?is)function .+var pvalue=.+")) { // 2012-10-23
				ArrayList<ArrayList<String>> matches = Regex.matchAll("(?is)\"(?:http://)?([^\"]+)\"|cid=(\\d+)", source);
				newUrl = "http://" + matches.get(2).get(1) + matches.get(3).get(1) + "?cid=" + matches.get(0).get(2) + "&key=" + matches.get(1).get(1);
			} else if (source.matches("(?is)function .+")) {
				ArrayList<ArrayList<String>> matches = Regex.matchAll("(?is)\"(?:http://)?([^\"]+)\"", source);
				newUrl = "http://" + matches.get(0).get(1) + matches.get(1).get(1);
			} else if (source.matches("(?is)^http://.+http://.+,http://.+http://.+")) {
				ArrayList<ArrayList<String>> matches = Regex.matchAll("(?is)(http://.+?\\.(?:png|jpg|gif|bmp|tga))",
						source.split(",")[0]);
				newUrl = matches.get(0).get(1);
			} else if (source.matches("(?is)^http://.+,http://.+")) {
				newUrl = source.split(",")[0];
			} else if (source.matches("(?is)^http://.+")) {
				newUrl = source;
			} else {
				logE(Fail_to_process, "RedirectPageUrl", url);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logE(Fail_to_process, "RedirectPageUrl", url);
		}
		return newUrl;
	}

	private static final String PACKED_PATTERN = "eval\\(function\\(p,a,c,k,e,d\\)\\{.+?return p;\\}\\('.+?(?<!\\\\)',\\d+,\\d+,'[^']+?'.split\\('\\|'\\),0,\\{\\}\\)\\)";
	private static final String PACKED_MATCHES_PATTERN = "return p;\\}\\('(.+?)(?<!\\\\)',(\\d+),(\\d+),'([^']+?)'";

	private String decodePackedJs(String js) {
		ArrayList<String> groups = Regex.match(PACKED_MATCHES_PATTERN, js);
		String _p = groups.get(1).replace("\\'", "'");
		String[] _d = groups.get(4).split("\\|");
		for (int i = 0; i < _d.length; i++) {
			if (_d[i].length() == 0)
				continue;
			_p = _p.replaceAll("\\b" + intToBase36(i) + "\\b", _d[i]);
		}
		return _p;
	}
}
