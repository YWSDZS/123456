package org.falconia.mangaproxy.data;

public final class GenreSearch extends Genre {

	private static final long serialVersionUID = 1L;

	public final String search;

	public GenreSearch(String search, int siteId) {
		super(GENRE_SEARCH_ID, search, siteId);
		this.search = search;
	}

	@Override
	public String getUrl(int page) {
		return getPlugin().getSearchUrl(this, page);
	}

	@Override
	public MangaList getMangaList(String source, String url) {
		return getPlugin().getSearchMangaList(source, url);
	}

	@Override
	public String toString() {
		return String.format("{%s:%s}", genreId, search);
	}

}
