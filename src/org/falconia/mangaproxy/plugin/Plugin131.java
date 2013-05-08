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

public final class Plugin131 extends PluginBase {
	protected static final String GENRE_ALL_ID = "new";
    protected static final String UPDATE_BASE_URL = "http://dynamic.comic.131.com/";
	protected static final String SEARCH_URL_FORMAT = "Search.aspx?all=%s&page=%d";
	protected static final String MANGA_URL_PREFIX = "content/";
	protected static final String MANGA_URL_POSTFIX = ".html";
	protected static final String PAGE_REDIRECT_URL_PREFIX = "chapterimagefun.ashx";

	public Plugin131(int siteId) {
		super(siteId);
	}

	@Override
	public String getName() {
		return "131";
	}

	@Override
	public String getDisplayname() {
		return "131漫画";
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
		return "http://comic.131.com/";
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
		String url = getUrlBase() + "new.html";
		logI(Get_URL_of_GenreList, url);
		return url;
	}

	@Override
	public String getGenreUrl(Genre genre, int page) {
		String url = getUrlBase();
		if (genre.isGenreAll()) {
			url = getGenreAllUrl(page);
			logI(Get_URL_of_AllMangaList, url);
		} else {
			if (page == 1) {
				url = String.format("%slz/%s/", url, genre.genreId);
			} else {
				url = String.format("%slz/%s/%d.html", url, genre.genreId, page);
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
		String url = UPDATE_BASE_URL ;
		if (page == 1) {
			url = String.format("%snew.html", getUrlBase());
		} else {
			url = String.format("%sUpdateComic.aspx?order=2&isRecommend=-1&begin=&end=&page=%d", url, page);
			
		}
		logI(Get_URL_of_AllMangaList, url);
		return url;
	}

	@Override
	public String getSearchUrl(GenreSearch genreSearch, int page) {
		String url = UPDATE_BASE_URL + String.format(SEARCH_URL_FORMAT, genreSearch.search, page);
		logI(Get_URL_of_SearchMangaList, url);
		return url;
	}
	
	@Override
	public String getMangaUrl(Manga manga) {
		String url = getUrlBase() + getMangaUrlPrefix() + manga.section + "/" + manga.mangaId + getMangaUrlPostfix();
		logI(Get_URL_of_ChapterList, manga.mangaId, url);
		return url;
	}
	
	
	@Override
	public String getMangaUrlPrefix() {
		return MANGA_URL_PREFIX;
	}

	@Override
	public String getMangaUrlPostfix() {
		return MANGA_URL_POSTFIX;
	}

	@Override
	public String getChapterUrl(Chapter chapter, Manga manga) {
		String url = getUrlBase()  + String.format("content/%s/%s/1.html", manga.mangaId, chapter.chapterId);
		logI(Get_URL_of_Chapter, chapter.chapterId, url);
		return url;
	}
    //todo//
	protected GregorianCalendar parseDate(String string) {
		GregorianCalendar calendar = null;
		calendar = parseDateTime(string, "(\\d+)[年-](\\d+)[月-](\\d+)日?{'YY','M','D'}");
		return calendar;
	}
    //todo//
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
		return new Genre(Genre.GENRE_ALL_ID, String.format("%s (最新漫画)", App.UI_GENRE_ALL_TEXT_ZH), getSiteId());
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
            pattern = "(?is) <li><em>漫画类型：</em>(.+?)</li>";
			//pattern = "(?is)<ul class=\"dm_nav\".*?>(.+?)</ul>.+?<div id=\"nav_fl2\">(.+?</div>).+?<div class=\"nav_zm[^<>]+>(.+?)</div>";
			groups = Regex.match(pattern, source);
			logD(Catched_sections, groups.size() - 1);

			// Section 1

			// Section 2
			section = "type";
			pattern = "(?is)<a href=\"http://comic.131.com/lz/([^\"]+)/\" target=\"_blank\" title=\"([^\"]+)\">(.+?)</a>";
			matches = Regex.matchAll(pattern, groups.get(1));
			logD(Catched_count_in_section, matches.size(), section);

			for (ArrayList<String> match : matches) {
				genreId = parseGenreId(match.get(1));
				list.add(genreId, parseGenreName(match.get(3)));
			}
            /*
			// Section 3
			section = "nav_zm";
			pattern = "(?is)<a href=\"/manhua-([^\"]+)/\">(.+?)</a>";
			matches = Regex.matchAll(pattern, groups.get(3));
			logD(Catched_count_in_section, matches.size(), section);

			for (ArrayList<String> match : matches) {
				genreId = parseGenreId(match.get(1));
				list.add(genreId, parseGenreName(match.get(2)));
			}
            */
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



			// Section 1
			if (genre.genreId.equalsIgnoreCase(Genre.GENRE_ALL_ID)) {
				
				pattern = "(?is)<ul id=\"ul1\">(.+?)</ul>.+?<ul id=\"ul2\".+?>(.+?)</ul>.+?<ul id=\"ul3\".+?>(.+?)</ul>.+?<div class=\"mh_fy mh_lxkk\" id=\"gd1\">(.+?)</div>";
				groups = Regex.match(pattern, source);
				logD(Catched_sections, groups.size() - 1);
				pattern = "(?is)<li><a href=\"http://comic.131.com/content/(.+?)/(.+?).html\" target=\"_blank\"><img.+?/><strong>(.+?)</strong></a><p>作者：<a.+?target=\"_blank\">(.+?)</a></p><p>更新至:<a[^<>]+><strong>(.+?)</strong></a></p>.+?更新日期：(.+?)</p></li>";
				matches = Regex.matchAll(pattern, groups.get(1));
				logD(Catched_count_in_section, matches.size(), "Mangas");

				for (ArrayList<String> match : matches) {
					Manga manga = new Manga(parseId(match.get(2)), parseName(match.get(3)), match.get(1), getSiteId());
					manga.updatedAt = parseDate(match.get(6));
					manga.details = "";
					manga.author = parseAuthorName(match.get(4));
					manga.chapterDisplayname = parseChapterName(match.get(5), manga.displayname);
					manga.latestChapterDisplayname = manga.chapterDisplayname;
					manga.setDetailsTemplate("%author%\n%chapterDisplayname%, %updatedAt%");
					list.add(manga, true);
					// logV(manga.toLongString());
				}
			} else {
				pattern = "(?is)</h4>.+?<ul>(.+?)</ul>.+?<div class=\"mh_fy mh_lxkk\" id=\"gd\">.+?<a class=\"last_page\" href=\"/.+?/.+?/(.+?).html.+?\".+?>";
				groups = Regex.match(pattern, source);
				logD(Catched_sections, groups.size() - 1);
				pattern = "(?is)<li>.+?<a href=\"/content/([^<>]+?)/([^<>]+?).html\" target=\"_blank\"><img[^<>]+?/>.+?<a[^<>]+?>更新至:<em>(.+?)</em>.+?</span>.+?<a.+?>(.+?)</a>.+?</li>";
				matches = Regex.matchAll(pattern, groups.get(1));
				logD(Catched_count_in_section, matches.size(), "Mangas");

				for (ArrayList<String> match : matches) {
					Manga manga = new Manga(parseId(match.get(2)), parseName(match.get(4)), match.get(1), getSiteId());
					//manga.updatedAt = parseDate(match.get(6));
					manga.details = "";
					manga.author = "";
					manga.chapterDisplayname = parseChapterName(match.get(3), manga.displayname);
					manga.latestChapterDisplayname = manga.chapterDisplayname;
					manga.setDetailsTemplate("%chapterDisplayname%");
					list.add(manga, true);
					list.pageIndexMax = parseInt(groups.get(2));
					// logV(manga.toLongString());
				}
			}

			// Section 2
			//list.pageIndexMax = parseInt(groups.get(4));
			//logV(Catched_in_section, groups.get(2), 2, "PageIndexMax", list.pageIndexMax);

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
			ArrayList<String> groups2;
			ArrayList<String> groups3;
			ArrayList<ArrayList<String>> matches;
			pattern = "(?is)</h4>.+?<ul>(.+?)</ul>.+?<div class=\"mh_fy mh_lxkk\" id=\"gd\">.+?>(.+?)</div>";
			groups = Regex.match(pattern, source);
			logD(Catched_sections, groups.size() - 1);

			// Section 1
			pattern = "(?is)<li>.+?<a href=\".+?/content/(.+?)/(.+?).html\" target=\"_blank\"><img.+?/>.+?<a.+?>更新至:<em>(.+?)</em>.+?</span>.+?<a.+?>(.+?)</a>.+?</li>";
			matches = Regex.matchAll(pattern, groups.get(1));
			logD(Catched_count_in_section, matches.size(), "Mangas");

			for (ArrayList<String> match : matches) {
				Manga manga = new Manga(parseId(match.get(2)), parseName(match.get(4)), match.get(1), getSiteId());
				//manga.updatedAt = parseDate(match.get(6));
				manga.details = "";
				manga.author = "";
				manga.chapterDisplayname = parseChapterName(match.get(3), manga.displayname);
				manga.latestChapterDisplayname = manga.chapterDisplayname;
				manga.setDetailsTemplate("%chapterDisplayname%");
				list.add(manga, true);
				
				// logV(manga.toLongString());
			}


            pattern = "(?is).+<a([^<>]+?)>([^<>]+?)</a>";
			groups2 = Regex.match(pattern, groups.get(2));

            if (groups2.get(2).contains("页"))
            {
               pattern = "(?is).+?page=([0-9]+?).+?";
               groups3 = Regex.match(pattern, groups2.get(1));
               list.pageIndexMax = parseInt(groups3.get(1));
            }
            else
            {
               list.pageIndexMax = parseInt(groups2.get(2));
            	
            }

			
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

			String pattern, section;
			ArrayList<String> groups;
			ArrayList<ArrayList<String>> matches;

			pattern = "(?is)<p>连载状态：<a[^<>]+>(.+?)</a>.+?<p>更新时间：<em>(.+?)</em></p>.+?<ul class=\"mh_fj\"[^<>]+>(.+)</ul>.+?<div class=\"cinnerlink\">";
			groups = Regex.match(pattern, source);
			logD(Catched_sections, groups.size() - 1);

			section = "IsCompleted";
			manga.isCompleted = parseIsCompleted(groups.get(1));
			logV(Catched_in_section, groups.get(2), 2, section, manga.isCompleted);

			section = "UpdatedAt";
			manga.updatedAt = parseDateTime(groups.get(2));
			logV(Catched_in_section, groups.get(3), 3, section, manga.updatedAt.getTime());

			section = "ChapterList";
			// logV(groups.get(4));
			pattern = "(?is)<li><a href=\"http://comic.131.com/content/(.+?)/(.+?)/1.html\"[^<>]+>(.+?)</a></li>";
			matches = Regex.matchAll(pattern, groups.get(3));
			logD(Catched_count_in_section, matches.size(), section);

			for (ArrayList<String> groups2 : matches) {
				Chapter chapter = new Chapter(groups2.get(2), groups2.get(3), manga);
				chapter.typeId = parseChapterType(chapter.displayname);
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
		ArrayList<ArrayList<String>> matches;
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
            pattern = "(?is)<select.+?>(.+?)</select>.+?<a href=\"/content/(.+?)/.+?/.+?\".+?>";
            groups = Regex.match(pattern, source);

			pattern = "(?is)<option.+?value=\"(.+?)\">";//
			matches = Regex.matchAll(pattern, groups.get(1));
			logD(Catched_sections, matches.size() - 1);
			pageUrls = new String[matches.size()];  
            int count =0;         
			for (ArrayList<String> groups2 : matches) {
				pageUrls[count] = getUrlBase()  + String.format("content/%s/%s/%s.html", groups.get(2), chapter.chapterId, groups2.get(1) );
				// logV(chapter.toLongString());
				count+=1;
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
			
		String pattern;
		ArrayList<String> groups;
        pattern = "(?is)<script id=\"imgjs\" type=\"text/javascript\" src=\"http://[^<>]+?img=([^<>]+?)\"></script>";
        groups = Regex.match(pattern, source);
        newUrl = groups.get(1);
        
             
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
