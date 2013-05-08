package org.falconia.mangaproxy.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import android.text.TextUtils;

public final class GenreList implements Serializable, ISiteId, Iterable<Genre> {

	private static final long serialVersionUID = 1L;

	private final ArrayList<Genre> mGenreList;
	private final int mSiteId;

	public GenreList(int siteId) {
		mGenreList = new ArrayList<Genre>();
		mSiteId = siteId;
	}

	@Override
	public int getSiteId() {
		return mSiteId;
	}

	public String getDisplayname(int position) {
		return getAt(position).displayname;
	}

	@Override
	public Iterator<Genre> iterator() {
		return mGenreList.iterator();
	}

	public void add(Genre genre) {
		mGenreList.add(genre);
	}

	public void add(String genreId, String displayname) {
		mGenreList.add(new Genre(genreId, displayname, mSiteId));
	}

	public void addAll(Collection<Genre> genres) {
		mGenreList.addAll(genres);
	}

	public void insert(int position, Genre genre) {
		mGenreList.add(position, genre);
	}

	public void insert(int position, String genreId, String displayname) {
		mGenreList.add(position, new Genre(genreId, displayname, mSiteId));
	}

	public Genre getAt(int position) {
		return mGenreList.get(position);
	}

	public void removeAt(int position) {
		mGenreList.remove(position);
	}

	public int size() {
		return mGenreList.size();
	}

	public ArrayList<Genre> toArray() {
		return mGenreList;
	}

	@Override
	public String toString() {
		ArrayList<String> strings = new ArrayList<String>();
		for (Genre genre : mGenreList) {
			strings.add(genre.toString());
		}
		return String.format("{ %s }", TextUtils.join(", ", strings));
	}

}
