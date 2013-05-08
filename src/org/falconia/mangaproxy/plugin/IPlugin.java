package org.falconia.mangaproxy.plugin;

import java.util.ArrayList;
import java.util.TimeZone;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.falconia.mangaproxy.data.Chapter;
import org.falconia.mangaproxy.data.ChapterList;
import org.falconia.mangaproxy.data.Genre;
import org.falconia.mangaproxy.data.GenreList;
import org.falconia.mangaproxy.data.GenreSearch;
import org.falconia.mangaproxy.data.Manga;
import org.falconia.mangaproxy.data.MangaList;

public interface IPlugin {

	int getSiteId();

	String getName();
	
	boolean login(String user, String password);
	
	String getDisplayname();

	String getCharset();

	TimeZone getTimeZone();

	String getUrlBase();

	boolean hasSearchEngine();
	
	boolean needLogin();

	boolean hasGenreList();

	boolean usingImgRedirect();

	boolean usingDynamicImgServer();

	String getGenreListUrl();

	String getGenreUrl(Genre genre, int page);

	String getGenreUrl(Genre genre);

	String getGenreAllUrl(int page);

	String getGenreAllUrl();

	String getSearchUrl(GenreSearch genreSearch, int page);

	String getSearchUrl(GenreSearch genreSearch);

	String getMangaUrl(Manga manga);

	String getMangaUrlPrefix();

	String getMangaUrlPostfix();

	String getChapterUrl(Chapter chapter, Manga manga);

	Genre getGenreAll();

	GenreSearch getGenreSearch(String search);

	GenreList getGenreList(String source, String url);

	MangaList getMangaList(String source, String url, Genre genre);

	MangaList getAllMangaList(String source, String url);

	MangaList getSearchMangaList(String source, String url);

	ChapterList getChapterList(String source, String url, Manga manga);

	String[] getChapterPages(String source, String url, Chapter chapter);

	String getPageRedirectUrl(String source, String url);

	boolean setDynamicImgServers(String source, String url, Chapter chapter);

	String getCookies();

}
