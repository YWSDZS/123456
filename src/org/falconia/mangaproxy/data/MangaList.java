package org.falconia.mangaproxy.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import android.text.TextUtils;

public final class MangaList implements Serializable, ISiteId, Collection<Manga> {

	private static final long serialVersionUID = 1L;

	private static final int DEFAULT_PAGE_MAX = 0;

	public int pageIndexMax;

	// TODO: Set return list default to null, instead searchEmpty
	public boolean searchEmpty = false;

	private final ArrayList<String> mMangaKeyList;
	private final HashMap<String, Manga> mMangaList;
	private final int mSiteId;

	public MangaList(int siteId) {
		this(siteId, DEFAULT_PAGE_MAX);
	}

	public MangaList(int siteId, int pageMax) {
		pageIndexMax = pageMax;

		mMangaKeyList = new ArrayList<String>();
		mMangaList = new HashMap<String, Manga>();
		mSiteId = siteId;
	}

	@Override
	public int getSiteId() {
		return mSiteId;
	}

	public String getMangaId(int position) {
		return mMangaKeyList.get(position);
	}

	public Manga get(String mangaId) {
		return mMangaList.get(mangaId);
	}

	public Manga getAt(int position) {
		return get(getMangaId(position));
	}

	public Manga getLast() {
		return get(getMangaId(mMangaKeyList.size() - 1));
	}

	@Override
	public boolean add(Manga manga) {
		String key = manga.mangaId;
		return add(manga, key);
	}

	public boolean add(Manga manga, boolean dump) {
		String key = manga.mangaId;
		while (dump && contains(key)) {
			key += "_DUMP";
		}
		return add(manga, key);
	}

	public boolean add(Manga manga, String key) {
		if (contains(key)) {
			return false;
		}
		mMangaList.put(key, manga);
		mMangaKeyList.add(key);
		return true;
	}

	public boolean add(String mangaId, String displayname, String inital) {
		return add(mangaId, displayname, inital, false);
	}

	public boolean add(String mangaId, String displayname, String inital, boolean dump) {
		return add(new Manga(mangaId, displayname, inital, mSiteId), dump);
	}

	@Override
	public boolean addAll(Collection<? extends Manga> mangas) {
		return addAll(mangas, false);
	}

	public boolean addAll(Collection<? extends Manga> mangas, boolean dump) {
		boolean modified = false;
		for (Manga manga : mangas) {
			if (add(manga, dump) && !modified) {
				modified = true;
			}
		}
		return modified;
	}

	public Manga update(Manga manga) {
		String key = manga.mangaId;
		if (contains(key)) {
			return mMangaList.put(key, manga);
		} else {
			add(manga);
			return null;
		}
	}

	public Manga update(String mangaId, String displayname, String inital) {
		return update(new Manga(mangaId, displayname, inital, mSiteId));
	}

	public ArrayList<Manga> updateAll(Collection<? extends Manga> mangas) {
		ArrayList<Manga> previousList = new ArrayList<Manga>();
		for (Manga manga : mangas) {
			Manga previous = update(manga);
			if (previous != null) {
				previousList.add(previous);
			}
		}
		return previousList;
	}

	@Override
	public boolean contains(Object object) {
		if (object instanceof Manga) {
			return contains(((Manga) object).mangaId);
		}
		throw new ClassCastException();
	}

	public boolean contains(String mangaId) {
		return mMangaList.containsKey(mangaId);
	}

	@Override
	public boolean containsAll(Collection<?> collection) {
		for (Object object : collection) {
			if (!contains(object)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean remove(Object object) {
		String key = ((Manga) object).mangaId;
		return remove(key);
	}

	public boolean remove(String mangaId) {
		if (!contains(mangaId)) {
			return false;
		}
		mMangaList.remove(mangaId);
		mMangaKeyList.remove(mangaId);
		return true;
	}

	public boolean removeAt(int position) {
		if (position < 0 || position >= size()) {
			return false;
		}
		String key = getMangaId(position);
		mMangaList.remove(key);
		mMangaKeyList.remove(position);
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		boolean modified = false;
		for (Object object : collection) {
			if (remove(object) && !modified) {
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public boolean retainAll(Collection<?> collection) {
		boolean modified = false;
		ArrayList<String> mangaIds = new ArrayList<String>();
		for (Object object : collection) {
			mangaIds.add(((Manga) object).mangaId);
		}
		for (String mangaId : mMangaKeyList) {
			if (mangaIds.contains(mangaId)) {
				if (remove(mangaId) && !modified) {
					modified = true;
				}
			}
		}
		return modified;
	}

	@Override
	public void clear() {
		pageIndexMax = DEFAULT_PAGE_MAX;
		mMangaKeyList.clear();
		mMangaList.clear();
	}

	@Override
	public int size() {
		return mMangaKeyList.size();
	}

	@Override
	public boolean isEmpty() {
		return mMangaKeyList.isEmpty();
	}

	@Override
	public Object[] toArray() {
		return toArrayList().toArray();
	}

	@Override
	public <T> T[] toArray(T[] array) {
		return toArrayList().toArray(array);
	}

	public ArrayList<Manga> toArrayList() {
		ArrayList<Manga> mangas = new ArrayList<Manga>();
		for (String mangaId : mMangaKeyList) {
			mangas.add(mMangaList.get(mangaId));
		}
		return mangas;
	}

	@Override
	public Iterator<Manga> iterator() {
		return new Iterator<Manga>() {

			Iterator<String> keys = mMangaKeyList.iterator();

			@Override
			public boolean hasNext() {
				return keys.hasNext();
			}

			@Override
			public Manga next() {
				return get(keys.next());
			}

			@Override
			public void remove() {
				keys.remove();
			}

		};
	}

	@Override
	public String toString() {
		if (mMangaKeyList.size() > 24) {
			return String.format("{ SiteId:%d, Size:%d, MaxPage:%d }", mSiteId, size(), pageIndexMax);
		}

		ArrayList<String> strings = new ArrayList<String>();
		for (String mangaId : mMangaKeyList) {
			strings.add(get(mangaId).toString());
		}
		return String.format("{ %s }", TextUtils.join(", ", strings));
	}

}
