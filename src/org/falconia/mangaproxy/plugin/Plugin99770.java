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
import android.util.SparseArray;

public final class Plugin99770 extends PluginBase {
	protected static final String GENRE_URL_PREFIX_1 = "comiclist/";
	protected static final String GENRE_URL_PREFIX_2 = "comicabc/";
	protected static final String GENRE_ALL_URL = "sitemap/";

	protected static final String URL_BASE_SEARCH = "http://so.dmwz.net/";
	// protected static final String URL_BASE_3G = "http://3gmanhua.com/";

	protected static final String SEARCH_URL_FORMAT = "?key=%s&pageindex=%d";
	protected static final String MANGA_URL_PREFIX = "comic/";

	public Plugin99770(int siteId) {
		super(siteId);
	}

	@Override
	public String getName() {
		return "99770";
	}

	@Override
	public String getDisplayname() {
		return "99770漫画";
	}

	@Override
	public String getCharset() {
		return CHARSET_GBK;
	}

	@Override
	public TimeZone getTimeZone() {
		return TimeZone.getTimeZone("GMT+08:00");
	}

	@Override
	public String getUrlBase() {
		return "http://99770.cc/";
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
		return false;
	}

	@Override
	public boolean usingDynamicImgServer() {
		return true;
	}

	@Override
	public String getGenreListUrl() {
		String url = getUrlBase() + GENRE_URL_PREFIX_1 + "1/";
		logI(Get_URL_of_GenreList, url);
		return url;
	}

	@Override
	public String getGenreUrl(Genre genre, int page) {
		String url = getUrlBase();
		if (genre.isGenreAll()) {
			url = getGenreAllUrl(page);
		} else {
			url = super.getGenreUrl(genre);
			if (page > 1 && url.endsWith("/")) {
				url += String.format("%d.htm", page);
			}
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
		String url = getUrlBase() + GENRE_ALL_URL;
		logI(Get_URL_of_AllMangaList, url);
		return url;
	}

	@Override
	public String getSearchUrl(GenreSearch genreSearch, int page) {
		String search = genreSearch.search;
		try {
			search = URLEncoder.encode(genreSearch.search, getCharset());
		} catch (Exception e) {
		}
		String url = URL_BASE_SEARCH + String.format(SEARCH_URL_FORMAT, search, page);
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
		String url = getUrlBase() + String.format("manhua/%s/%s.htm", manga.mangaId, chapter.chapterId);
		logI(Get_URL_of_Chapter, chapter.chapterId, url);
		return url;
	}

	protected GregorianCalendar parseDate(String string) {
		GregorianCalendar calendar = null;
		calendar = parseDateTime(string, "(\\d+)/(\\d+)/(\\d+){'YY','M','D'}");
		return calendar;
	}

	protected GregorianCalendar parseDateTime(String string) {
		GregorianCalendar calendar = null;
		calendar = parseDateTime(string, "(\\d+)/(\\d+)/(\\d+) (\\d+)\\:(\\d+)\\:(\\d+){'YY','M','D','h','m','s'}");
		return calendar;
	}

	@Override
	protected String parseGenreName(String string) {
		string = super.parseGenreName(string);
		string = string.replaceAll("^漫画$", "视界漫画");
		return string;
	}

	@Override
	protected boolean parseIsCompleted(String string) {
		if (string.matches("\\s*经典\\s*")) {
			return true;
		}
		return string.indexOf("完") >= 0;
	}

	@Override
	protected int parseChapterType(String string) {
		if (Regex.isMatch("卷$", string)) {
			return Chapter.TYPE_ID_VOLUME;
		}
		if (Regex.isMatch("集$", string)) {
			return Chapter.TYPE_ID_CHAPTER;
		}
		return Chapter.TYPE_ID_UNKNOW;
	}

	@Override
	public Genre getGenreAll() {
		return new Genre(Genre.GENRE_ALL_ID, App.UI_GENRE_ALL_TEXT_ZH, getSiteId());
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
			String genreId;
			String html2, html3;
			String pattern;
			ArrayList<ArrayList<String>> matches;

			pattern = "(?is)(<td.*?>按字母筛选：.*?</td>)";
			html2 = Regex.matchString(pattern, source);

			pattern = "(?is)<a\\s+href=\"/([^\"/]+?)/\"\\s*>(.+?)</a>";
			matches = Regex.matchAll(pattern, html2);

			for (ArrayList<String> groups : matches) {
				genreId = parseGenreId(groups.get(1));
				list.add(genreId, parseGenreName(groups.get(2)));
			}

			pattern = "(?is)<div id=\"menu\">(.*?)</div>";
			html3 = Regex.matchString(pattern, source);

			pattern = "(?is)<a\\s+href=\"/" + GENRE_URL_PREFIX_1 + "([^\"/]+?)/?\".*?>(.+?)</a>";
			matches = Regex.matchAll(pattern, html3);

			for (ArrayList<String> groups : matches) {
				genreId = parseGenreId(GENRE_URL_PREFIX_1 + groups.get(1));
				list.add(genreId, parseGenreName(groups.get(2)));
			}

			pattern = "(?is)<a\\s+href=\"/" + GENRE_URL_PREFIX_2 + "([^\"/]+?)/?\".*?>(.+?)</a>";
			matches = Regex.matchAll(pattern, html2);

			for (ArrayList<String> groups : matches) {
				genreId = parseGenreId(GENRE_URL_PREFIX_2 + groups.get(1));
				list.add(genreId, parseGenreName(groups.get(2)));
			}

			logV(list.toString());

		} catch (Exception e) {
			e.printStackTrace();
			logE(Fail_to_process, "GenreList", url);
		}

		return list;
	}

	@Override
	public MangaList getMangaList(String source, String url, Genre genre) {
		MangaList list = new MangaList(getSiteId());

		logI(Get_MangaList, genre.genreId);
		logD(Get_Source_Size_MangaList, FormatUtils.getFileSize(source, getCharset()));

		if (TextUtils.isEmpty(source)) {
			logE(Source_is_empty);
			return list;
		}

		try {
			long time = System.currentTimeMillis();

			String pattern;
			ArrayList<String> groups;
			ArrayList<ArrayList<String>> matches;

			if (genre.genreId.startsWith(GENRE_URL_PREFIX_1.split("/")[0])
					|| genre.genreId.startsWith(GENRE_URL_PREFIX_2.split("/")[0])) {
				logD(Process_MangaList, genre.genreId);

				pattern = "(?is)<div class=\"replz\"\\s*>(.*?)></div>.*?<div class=\"m_list\"\\s*>\\s*<ul .*?>\\s*(.*?)\\s*</ul>\\s*</div>";
				groups = Regex.match(pattern, source);
				logD(Catched_sections, groups.size() - 1);

				// Section 1 (Max Page)
				pattern = "<a href=\"(\\d+)\\.htm\"[^<>]*?>末页</a>";
				String group = Regex.match(pattern, groups.get(1)).get(1);
				list.pageIndexMax = parseInt(group);
				logD(Catched_total_page, list.pageIndexMax);

				// Section 2 (List)
				pattern = "(?is)<li .*?<a href=\"/\\w+/(\\d+)/\" .*?<img src=\"?([^\"]+?)\"? alt=\"提示:\\[(.+?)\\]\\s+?(\\d*?)集\\(卷\\)\".*?>\\s*<h3><a .*?>(.*?)</a></h3>.*?</li>";
				matches = Regex.matchAll(pattern, groups.get(2));
				logD(Catched_count_in_section, matches.size(), "ul");

				for (ArrayList<String> match : matches) {
					Manga manga = new Manga(parseId(match.get(1)), match.get(5), null, getSiteId());
					manga.isCompleted = parseIsCompleted(match.get(3));
					manga.chapterCount = parseInt(match.get(4));
					manga.setDetailsTemplate("%chapterCount%");
					list.add(manga);
					// logV(manga.toLongString());
				}

			} else {
				logD(Process_MangaList, genre.genreId);

				pattern = "(?is)<div class=\"w100\">\\s*((?:<table .*?</table>\\s*)+)\\s*</div>";
				groups = Regex.match(pattern, source);
				logD(Catched_sections, groups.size() - 1);

				if (!genre.genreId.equals("top")) {
					// Section 1 (Genre)
					String section = "Last Updates";
					pattern = "(?is)<table .+?>[\\s\\d·]+<a href=\"/\\w+/(\\d+)/\".*?>\\s*(.+?)\\s*</a>.+?<b>(\\d*)</b><.+?>集\\(卷\\)<.+?>〖(.+?)〗<.+?>\\s*(?:<img [^<>]+>\\s*)?<.+?>([\\d/]+)<.+?</table>";
					matches = Regex.matchAll(pattern, groups.get(1));
					logD(Catched_count_in_section, matches.size(), section);

					for (ArrayList<String> match : matches) {
						// logV(match.get(0));
						Manga manga = new Manga(parseId(match.get(1)), match.get(2), section, getSiteId());
						manga.chapterCount = parseInt(match.get(3));
						manga.isCompleted = parseIsCompleted(match.get(4));
						manga.updatedAt = parseDate(match.get(5));
						manga.setDetailsTemplate("%chapterCount%, %updatedAt%");
						list.add(manga);
						// logV(manga.toLongString());
					}
				} else {
					// Section 2 (Genre Top)
					// logV("%s", groups.get(1));
					String section = "Most Hits";
					pattern = "(?is)<table .+?>[\\s\\d·]+<a href=\"/?\\w+/(\\d+)/?\".*?>\\s*(.+?)\\s*</a>.*?<font.*?>(\\d*)</font>集\\(卷\\)〖(.+?)〗\\s*<.+?>共<font.*?>(\\d+)</font>人推荐，漫友给出<font.*?>([\\d\\.]+)</font>分，([\\d/]+)<.+?</table>";
					matches = Regex.matchAll(pattern, groups.get(1));
					logD(Catched_count_in_section, matches.size(), section);

					for (ArrayList<String> match : matches) {
						// logV(match.get(0));
						Manga manga = new Manga(parseId(match.get(1)), match.get(2), section, getSiteId());
						manga.chapterCount = parseInt(match.get(3));
						manga.isCompleted = parseIsCompleted(match.get(4));
						manga.updatedAt = parseDate(match.get(7));
						manga.details = String.format(App.UI_RECOMMEND, parseInt(match.get(5))) + ", "
								+ String.format(App.UI_RATING, match.get(6));
						manga.setDetailsTemplate("%chapterCount%, %details%\n%updatedAt%");
						list.add(manga);
						// logV(manga.toLongString());
					}
				}
			}

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
	public MangaList getAllMangaList(String source, String url) {
		MangaList list = new MangaList(getSiteId());

		logI(Get_AllMangaList);
		logD(Get_Source_Size_AllMangaList, FormatUtils.getFileSize(source, getCharset()));

		if (TextUtils.isEmpty(source)) {
			logE(Source_is_empty);
			return list;
		}

		try {
			long time = System.currentTimeMillis();

			String pattern, source2;
			ArrayList<ArrayList<String>> matches;

			pattern = "(?is)(<div id='all'><div class='allf'>.*?<span class='redzi'>(.*?)</span>.*?)<div class='aa'></div>";
			matches = Regex.matchAll(pattern, source);
			logD(Catched_sections, matches.size());

			for (ArrayList<String> groups : matches) {
				source2 = groups.get(1);
				String sGenre = parseGenreName(groups.get(2));
				pattern = "(?is)<a href=\"/?" + getMangaUrlPrefix() + "(\\d+)/?\">(.*?)</a>";
				ArrayList<ArrayList<String>> matches2 = Regex.matchAll(pattern, source2);
				// logV(Catched_count_in_section, matches2.size(), sGenre);

				for (ArrayList<String> groups2 : matches2) {
					list.add(parseId(groups2.get(1)), parseName(groups2.get(2)), sGenre);
				}
			}

			time = System.currentTimeMillis() - time;
			logD(Process_Time_AllMangaList, time);

			logV(list.toString());

		} catch (Exception e) {
			e.printStackTrace();
			logE(Fail_to_process, "AllMangaList", url);
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

			pattern = "(?is)<span[^<>]*?\\s+id=\"labDataCount\"[^<>]*?>(\\d+)</span>.+?<span[^<>]*?\\s+id=\"labPageCount\"[^<>]*?>(\\d+)</span>.+?<div[^<>]*?\\s+class=\"dSHtm\"[^<>]*?>(?:(.+?)</div>\\s*)?</div>";
			groups = Regex.match(pattern, source);
			logD(Catched_sections, groups.size() - 1);

			// Section 1
			logV(Catched_in_section, groups.get(1), 1, "MangaCount", parseInt(groups.get(1)));

			// Section 2
			list.pageIndexMax = parseInt(groups.get(2));
			logV(Catched_in_section, groups.get(2), 2, "PageIndexMax", list.pageIndexMax);

			if (parseInt(groups.get(1)) == 0) {
				list.searchEmpty = true;
			} else {
				// Section 3
				pattern = "(?is)<div><a[^<>]*?\\s+href='[^']+?/\\w+/(\\d+)/?'[^<>]*?><[^<>]+>(.+?)</a>.+?〖(.+?)〗.+?>(\\d+)<.+?集";
				matches = Regex.matchAll(pattern, groups.get(3));
				logD(Catched_count_in_section, matches.size(), "Mangas");

				for (ArrayList<String> match : matches) {
					Manga manga = new Manga(parseId(match.get(1)), parseName(match.get(2)), null, getSiteId());
					manga.isCompleted = parseIsCompleted(match.get(3));
					manga.chapterCount = parseInt(match.get(4));
					manga.setDetailsTemplate("%chapterCount%");
					list.add(manga, true);
					// logV(manga.toLongString());
				}
			}

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

			String pattern, section;
			ArrayList<String> groups;
			ArrayList<ArrayList<String>> matches;

			pattern = "(?is)集数：(?:\\s*<[^<>]+>)?(\\d*?)(?:<[^<>]+>\\s*)?集\\(卷\\)\\s+\\|\\s+状态：[〖\\[](?:\\s*<[^<>]+>)?(.+?)(?:<[^<>]+>\\s*)?[〗\\]].+?>更新时间：([ \\d/:]+)<.+?<div [^<>]*?class=\"c?vol\"[^<>]*?>\\s*<ul.*?>(.+?)</ul>";
			groups = Regex.match(pattern, source);
			logD(Catched_sections, groups.size() - 1);

			section = "ChapterCount";
			manga.chapterCount = parseInt(groups.get(1));
			logV(Catched_in_section, groups.get(1), 1, section, manga.chapterCount);

			section = "IsCompleted";
			manga.isCompleted = parseIsCompleted(groups.get(2));
			logV(Catched_in_section, groups.get(2), 2, section, manga.isCompleted);

			section = "UpdatedAt";
			manga.updatedAt = parseDateTime(groups.get(3));
			logV(Catched_in_section, groups.get(3), 3, section, manga.updatedAt.getTime());

			section = "ChapterList";
			// logV(groups.get(4));
			pattern = "(?is)(?:<li>|<div.*?>)<a href=\"?/\\w+/\\d+/(\\d+)\\.htm\\?s=(\\d+)\"? .*?>(?:<.*?>)?(.*?)</a>";
			matches = Regex.matchAll(pattern, groups.get(4));
			logD(Catched_count_in_section, matches.size(), section);

			for (ArrayList<String> groups2 : matches) {
				Chapter chapter = new Chapter(groups2.get(1), groups2.get(3), manga);
				chapter.typeId = parseChapterType(chapter.displayname);
				chapter.setDynamicImgServerId(parseInt(groups2.get(2)) - 1);
				list.add(chapter);
				// logV(chapter.toLongString());
			}

			logD(Get_DynamicImgServerId, list.getAt(0).getDynamicImgServerId());

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

			pattern = "(?is)<script .*?>.*?var\\s+PicListUrl\\s*=\\s*\"/?([^\"]+)\";.*?</script>.*?<script\\s+src=\"?([^\\s\"]+?)\"?></script>";
			groups = Regex.match(pattern, source);
			logD(Catched_sections, groups.size() - 1);

			// Section 1
			pageUrls = groups.get(1).split("\\|/?");
			logV(Catched_count_in_section, pageUrls.length, "PageUrls");

			// Section 2
			String imgserversurl = groups.get(2);
			chapter.setDynamicImgServersUrl(imgserversurl);
			logV(Catched_in_section, imgserversurl, 2, "ImgServersUrl", chapter.getDynamicImgServersUrl());

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
	public boolean setDynamicImgServers(String source, String url, Chapter chapter) {
		logI(Get_DynamicImgServers);
		logD(Get_Source_Size_DynamicImgServers, FormatUtils.getFileSize(source, getCharset()));

		if (TextUtils.isEmpty(source)) {
			logE(Source_is_empty);
			return false;
		}

		try {
			long time = System.currentTimeMillis();

			String pattern;
			ArrayList<ArrayList<String>> matches;

			// Section Count
			String count = Regex.match("var\\s+ServerList\\s*=\\s*new\\s+Array\\((\\d+)\\)\\s*;", source).get(1);
			logV(Catched_in_section, count, 0, "Count", parseInt(count));

			// Section ImgServers
			pattern = "(?is)(//)?[	 ]*ServerList\\s*\\[\\s*(\\d+)\\s*\\]\\s*=\\s*\"([^\"]+)\"\\s*;";
			matches = Regex.matchAll(pattern, source);
			logD(Catched_count_in_section, matches.size(), "ImgServers");

			SparseArray<String> imgServers = new SparseArray<String>();
			for (ArrayList<String> groups : matches) {
				if (groups.get(1) == null) {
					imgServers.put(parseInt(groups.get(2)), groups.get(3));
				}
			}
			String[] imgServersArray = new String[imgServers.size()];
			for (int i = 0; i < imgServers.size(); i++)
				imgServersArray[i] = imgServers.get(i, "");

			chapter.setDynamicImgServers(imgServersArray);

			time = System.currentTimeMillis() - time;
			logD(Process_Time_DynamicImgServers, time);

			return true;

		} catch (Exception e) {
			e.printStackTrace();
			logE(Fail_to_process, "DynamicImgServers", url);
		}

		return false;
	}

}
