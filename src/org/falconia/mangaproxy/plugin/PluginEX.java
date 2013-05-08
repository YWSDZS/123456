package org.falconia.mangaproxy.plugin;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.impl.cookie.BasicClientCookie;
//import org.apache.http.cookie.Cookie;
import org.falconia.mangaproxy.App;
import org.falconia.mangaproxy.data.Chapter;
import org.falconia.mangaproxy.data.ChapterList;
import org.falconia.mangaproxy.data.Genre;
import org.falconia.mangaproxy.data.GenreList;
import org.falconia.mangaproxy.data.GenreSearch;
import org.falconia.mangaproxy.data.Manga;
import org.falconia.mangaproxy.data.MangaList;
import org.falconia.mangaproxy.utils.FormatUtils;
import org.falconia.mangaproxy.utils.HttpUtils;
import org.falconia.mangaproxy.utils.Regex;

import android.text.TextUtils;

public final class PluginEX extends PluginBase {
	protected static final String GENRE_ALL_ID = "new";
    protected static final String UPDATE_BASE_URL = "http://exhentai.org/";
	protected static final String SEARCH_URL_FORMAT = "?f_search=%s&page=%d";
	protected static final String MANGA_URL_PREFIX = "";
	protected static final String MANGA_URL_POSTFIX = "/?inline_set=tr_2";
	protected static final String PAGE_REDIRECT_URL_PREFIX = "chapterimagefun.ashx";
	protected static final int TIME_OUT_CONNECT = 10000;
	protected static final int TIME_OUT_READ = 10000;
	private static final String DESKTOP_AGENT = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.64 Safari/537.31";
	private static final int MAX_BUFFER_SIZE = 1024;

	public PluginEX(int siteId) {
		super(siteId);
	}

	@Override
	public String getName() {
		return "EX";
	}

	@Override
	public String getDisplayname() {
		return "EX漫画";
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
		return "http://exhentai.org/";
	}

	@Override
	public boolean hasSearchEngine() {
		return true;
	}
	
	@Override
	public boolean needLogin() {
		return true;
	}
	

	
	 public static String getCookiesFromConn(HttpURLConnection conn) {  
	        StringBuffer cookies = new StringBuffer();  
	        String headName;  
	        for (int i = 1; (headName = conn.getHeaderField(i)) != null; i++) {  
	            StringTokenizer st = new StringTokenizer(headName, "; ");  
	            while (st.hasMoreTokens()) {  
	                cookies.append(st.nextToken() + "; ");  
	            }  
	        }  
	        return cookies.toString();  
	    }  
	 
	 
	@Override
	public String getCookies()
	{
		return this.cookies;
	}
		
	@Override
	public boolean login(String user, String password){
		
		if (this.cookies.length() > 0 )
		{
			return true;
		}
		String url = "https://forums.e-hentai.org/index.php?act=Login&CODE=01";
		String post = "";
		try {
		  post +=  URLEncoder.encode("referer", "UTF-8") + "="  + "https://forums.e-hentai.org/index.php";
		  post +=  "&" + URLEncoder.encode("UserName", "UTF-8") + "="  + URLEncoder.encode(user, "UTF-8");
		  post +=  "&" + URLEncoder.encode("PassWord", "UTF-8") + "="  + URLEncoder.encode(password, "UTF-8");
		  post +=  "&" + URLEncoder.encode("CookieDate", "UTF-8") + "="  + URLEncoder.encode("1","UTF-8");
		  

		}
		catch (Exception e)
		{
			
		}
		//logE(this + "Post DATA: " + post);
		url = HttpUtils.urlencode(url,"UTF-8");
		HttpURLConnection connection;
		int chByte = 0;  
		InputStream in = null;  
	    OutputStream out = null;
		int mFileSize;
		int mDownloaded = 0;
		String result = "";
		String mReferer = "https://forums.e-hentai.org/index.php&UserName=" + user + "&PassWord=" + password + "&CookieDate=1";
		try {
			connection = (HttpURLConnection) (new URL(url)).openConnection();
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setUseCaches(false);
			connection.setRequestMethod("POST");
			connection.setConnectTimeout(TIME_OUT_CONNECT);
			connection.setReadTimeout(TIME_OUT_READ);
			connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
			connection.setRequestProperty("User-Agent", DESKTOP_AGENT);
			connection.setRequestProperty("Accept", "*/*");
		    //connection.setRequestProperty("referer", mReferer);
			
			
			connection.getOutputStream().write(post.getBytes());
			connection.getOutputStream().flush();
			connection.getOutputStream().close();


			int statusCode = connection.getResponseCode();
			if (statusCode >= 400) {
				logE("Invalid Status Code: " + statusCode);
				return false;
			} else {
				logD("Status Code: " + statusCode);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			logE("Invalid URL: " + e.getMessage());
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			logE("Fail to open the connection: " + e.getMessage());
			return false;
		}
		

		InputStream input;
		try {
			input = connection.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
			logE("IOException(InputStream): " + e.getMessage());
			return false;
		}

		mFileSize = connection.getContentLength();
		logD(String.format("Content-Length: %d", mFileSize));
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		


		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(input)); 
			   StringBuffer buffer = new StringBuffer(); 
			   String line = ""; 
			   while ((line = reader.readLine()) != null){ 
			     buffer.append(line); 
			   } 
	        result  = buffer.toString();
	        String cookies = getCookiesFromConn(connection);  
	        System.out.println(cookies);  
			logD(this + "Downloaded length: " + result.length());
			logD(this + "cookies: " + cookies);
			output.flush();
			output.close();
			input.close();
			connection.disconnect();
			//logD("login result" + result);
			if (result.indexOf("You are now logged in as") > 0)
			{
				this.cookies = cookies;
				return true;
				
			}
			return false;
			
		} catch (IOException e) {
			e.printStackTrace();
			logE("IOException(InputStream): " + e.getMessage());
		}
        return false;

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
		String url = getUrlBase();
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
				url = String.format("%s/%s", url, genre.genreId);
			} else {
				url = String.format("%s/%s/%d", url, genre.genreId, page-1);
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
			url = getUrlBase();
		} else {
			url = String.format("%s?page=%d", url, page-1);
			
		}
		logI(Get_URL_of_AllMangaList, url);
		return url;
	}

	@Override
	public String getSearchUrl(GenreSearch genreSearch, int page) {
		String url = UPDATE_BASE_URL + String.format(SEARCH_URL_FORMAT, genreSearch.search.replace(" ",""), page-1);
		logI(Get_URL_of_SearchMangaList, url);
		return url;
	}
	
	@Override
	public String getMangaUrl(Manga manga) {
		String url = getUrlBase() + getMangaUrlPrefix() + manga.section  + manga.mangaId + getMangaUrlPostfix();

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
		//String url = this.getMangaUrl(manga);
		String url = chapter.chapterId;
		//String url = "http://exhentai.org/g/406976/f9aac3b8db/?inline_set=tr_2";
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
		/*
		list.add("test","test");
		return list;
		*/
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
            pattern = "<img id=\"f_([a-z-]+?)_img\".+?/>";
			//pattern = "(?is)<ul class=\"dm_nav\".*?>(.+?)</ul>.+?<div id=\"nav_fl2\">(.+?</div>).+?<div class=\"nav_zm[^<>]+>(.+?)</div>";
            matches = Regex.matchAll(pattern, source);
			section = "type";
			logD(Catched_count_in_section, matches.size(), section);

			for (ArrayList<String> match : matches) {
				genreId = parseGenreId(match.get(1));
				list.add(genreId, parseGenreName(match.get(1)));
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
			
			// Section 1
			pattern = "<td onclick=\"sp\\(([0-9]+?)\\)\"><a[^<>]+?>[0-9]+?</a></td><td onclick=\"sp\\(1\\)\">";
			groups = Regex.match(pattern, source);
			//pattern = "(.+?)";
			//groups = Regex.match(pattern, source);
			//logD(Catched_sections, groups.size() - 1);
			//Pattern regex = Pattern.compile("<div class=\"itd1\".+?><div class=\"itd2\"><a href=\".+?org/([^<>]+?)/\">([^<>]+?)</a></div><div class=\"itd3\".+?><a href=\"([^<>\"]+?)\"><img src=\"(.+?)\"[^<>]+?></a></div>.+?<div class=\"itd4\">", Pattern.MULTILINE);

			Pattern regex = Pattern.compile("<tr class=\"gtr[01]\">.+?<td class=\"itd\".+?>(.+?)</td>.+?<div class=\"it1\"><a href=\".+?org/(.+?)/\".+?</div><div class=\"it2\".+?>init(.+?)</div><div class=\"it3\">.*?<a href=\"([^<>\"]+?)\">(.+?)</a></div>.+?</tr>", Pattern.MULTILINE);
			Matcher regexMatcher = regex.matcher(source);
			//logD(Catched_count_in_section, matches.size(), "Mangas");
			while (regexMatcher.find()) {
				Manga manga = new Manga(parseId(regexMatcher.group(2)), parseName(regexMatcher.group(5)), "", getSiteId());
				manga.updatedAt = parseDate(regexMatcher.group(1));
				manga.details = "";
				manga.author = "unknown";
				manga.chapterDisplayname = "chapter";
				manga.latestChapterDisplayname = manga.chapterDisplayname;
				manga.setDetailsTemplate("%author%\n%chapterDisplayname%, %updatedAt%");
				String[] urls = regexMatcher.group(3).split("~");
				manga.thumbUrl = "http://" +  urls[1] + "/" + urls[2];
				list.add(manga, true);
			
			} 
			if (groups.size() >=1)
			{
				list.pageIndexMax = parseInt(groups.get(1));
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
			ArrayList<ArrayList<String>> matches;
			
			// Section 1
			pattern = "<td onclick=\"sp\\(([0-9]+?)\\)\"><a[^<>]+?>[0-9]+?</a></td><td onclick=\"sp\\(1\\)\">";
			groups = Regex.match(pattern, source);
			//pattern = "(.+?)";
			//groups = Regex.match(pattern, source);
			//logD(Catched_sections, groups.size() - 1);
			Pattern regex = Pattern.compile("<tr class=\"gtr[01]\">.+?<td class=\"itd\".+?>(.+?)</td>.+?<div class=\"it1\"><a href=\".+?org/(.+?)/\".+?</div><div class=\"it2\".+?>.+?</div><div class=\"it3\">.*?<a href=\"([^<>\"]+?)\">(.+?)</a></div>.+?</tr>", Pattern.MULTILINE);
			Matcher regexMatcher = regex.matcher(source);
			//logD(Catched_count_in_section, matches.size(), "Mangas");
			while (regexMatcher.find()) {
				Manga manga = new Manga(parseId(regexMatcher.group(2)), parseName(regexMatcher.group(4)), "", getSiteId());
				manga.updatedAt = parseDate(regexMatcher.group(1));
				manga.details = "";
				manga.author = "unknown";
				manga.chapterDisplayname = "chapter";
				manga.latestChapterDisplayname = manga.chapterDisplayname;
				manga.setDetailsTemplate("%author%\n%chapterDisplayname%, %updatedAt%");
				list.add(manga, true);
			
			} 
			if (groups.size() >=1)
			{
				list.pageIndexMax = parseInt(groups.get(1));
			}
			

			
			//logV(Catched_in_section, groups.get(2), 2, "PageIndexMax", list.pageIndexMax);

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
		/*
		Chapter chapter = new Chapter(manga.mangaId + "/chapter", "1", manga);
		chapter.typeId = parseChapterType(chapter.displayname);
		list.add(chapter);
		return list;
		*/
		
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
           
			pattern = "<table class=\"ptt\".+?>(.+?)</table>";
			groups = Regex.match(pattern, source);
			logD(Catched_sections, groups.size() - 1);
			/*
			section = "IsCompleted";
			manga.isCompleted = parseIsCompleted(groups.get(1));
			logV(Catched_in_section, groups.get(2), 2, section, manga.isCompleted);

			section = "UpdatedAt";
			manga.updatedAt = parseDateTime(groups.get(2));
			logV(Catched_in_section, groups.get(3), 3, section, manga.updatedAt.getTime());
            */
			section = "ChapterList";
			// logV(groups.get(4));
			pattern = "<a href=\"(.+?)\".+?>([0-9]+?)</a>";
			matches = Regex.matchAll(pattern, groups.get(1));
			logD(Catched_count_in_section, matches.size(), section);

			for (ArrayList<String> groups2 : matches) {
				Chapter chapter = new Chapter(groups2.get(1), groups2.get(2), manga);
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

			ArrayList<String> matchList = new ArrayList<String>();
			Pattern regex = Pattern.compile("<div.+?margin:1px.+?href=\"(.+?)\">.+?</div>", Pattern.MULTILINE);
			Matcher regexMatcher = regex.matcher(source);
			while (regexMatcher.find()) {
				matchList.add(regexMatcher.group(1));
			}
	
			pageUrls = new String[matchList.size()]; 
			int count = 0;
			for (String turl : matchList) {
				pageUrls[count] = turl;

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
        pattern = "<img id=\"img\" src=\"([^<>]+?)\".+?/>";
        groups = Regex.match(pattern, source);
        newUrl = groups.get(1).replace("&amp;","&");
        
             
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
