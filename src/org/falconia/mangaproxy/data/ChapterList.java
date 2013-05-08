package org.falconia.mangaproxy.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public final class ChapterList implements Serializable, ISiteId, Collection<Chapter> {

	private static final long serialVersionUID = 1L;

	private final ArrayList<String> mChapterKeyList;
	private final HashMap<String, Chapter> mChapterList;
	private final Manga mManga;

	private int mSizeVolume = 0, mSizeChapter = 0, mSizeUnknow = 0;

	public ChapterList(Manga manga) {
		mChapterKeyList = new ArrayList<String>();
		mChapterList = new HashMap<String, Chapter>();
		mManga = manga;
	}

	@Override
	public int getSiteId() {
		return mManga.siteId;
	}

	public String getDisplayname(int position) {
		return getAt(position).displayname;
	}

	public String getChapterId(int position) {
		return mChapterKeyList.get(position);
	}

	public Chapter get(String chapterId) {
		return mChapterList.get(chapterId);
	}

	public Chapter getAt(int position) {
		return get(getChapterId(position));
	}

	public int indexOfChapterId(String chapterId) {
		return mChapterKeyList.indexOf(chapterId);
	}

	@Override
	public boolean add(Chapter chapter) {
		String key = chapter.chapterId;
		return add(chapter, key);
	}

	public boolean add(Chapter chapter, String key) {
		if (contains(key)) {
			return false;
		}
		countType(chapter);
		mChapterList.put(key, chapter);
		mChapterKeyList.add(key);
		return true;
	}

	public boolean add(String chapterId, String displayname) {
		return add(new Chapter(chapterId, displayname, mManga));
	}

	@Override
	public boolean addAll(Collection<? extends Chapter> chapters) {
		boolean modified = false;
		for (Chapter chapter : chapters) {
			if (add(chapter)) {
				modified = true;
			}
		}
		return modified;
	}

	public boolean insert(int position, Chapter chapter) {
		String key = chapter.chapterId;
		if (contains(key)) {
			return false;
		}
		countType(chapter);
		mChapterList.put(key, chapter);
		mChapterKeyList.add(key);
		return true;
	}

	public boolean insert(int position, String chapterId, String displayname) {
		return insert(position, new Chapter(chapterId, displayname, mManga));
	}

	public Chapter update(Chapter chapter) {
		String key = chapter.chapterId;
		if (contains(key)) {
			return mChapterList.put(key, chapter);
		} else {
			add(chapter);
			return null;
		}
	}

	public Chapter update(String chapterId, String displayname) {
		return update(new Chapter(chapterId, displayname, mManga));
	}

	public ArrayList<Chapter> updateAll(Collection<? extends Chapter> chapters) {
		ArrayList<Chapter> previousList = new ArrayList<Chapter>();
		for (Chapter chapter : chapters) {
			Chapter previous = update(chapter);
			if (previous != null) {
				previousList.add(previous);
			}
		}
		return previousList;
	}

	@Override
	public boolean contains(Object object) {
		if (object instanceof Chapter) {
			return contains(((Chapter) object).chapterId);
		}
		throw new ClassCastException();
	}

	public boolean contains(String chapterId) {
		return mChapterList.containsKey(chapterId);
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
		String key = ((Chapter) object).chapterId;
		return remove(key);
	}

	public boolean remove(String chapterId) {
		if (!contains(chapterId)) {
			return false;
		}
		mChapterList.remove(chapterId);
		mChapterKeyList.remove(chapterId);
		return true;
	}

	public boolean removeAt(int position) {
		if (position < 0 || position >= size()) {
			return false;
		}
		String key = getChapterId(position);
		mChapterList.remove(key);
		mChapterKeyList.remove(position);
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
		ArrayList<String> chapterIds = new ArrayList<String>();
		for (Object object : collection) {
			chapterIds.add(((Chapter) object).chapterId);
		}
		for (String chapterId : mChapterKeyList) {
			if (chapterIds.contains(chapterId)) {
				if (remove(chapterId) && !modified) {
					modified = true;
				}
			}
		}
		return modified;
	}

	@Override
	public void clear() {
		mSizeVolume = 0;
		mSizeChapter = 0;
		mSizeUnknow = 0;
		mChapterKeyList.clear();
		mChapterList.clear();
	}

	@Override
	public int size() {
		return mChapterKeyList.size();
	}

	@Override
	public boolean isEmpty() {
		return mChapterKeyList.isEmpty();
	}

	@Override
	public Object[] toArray() {
		return toArrayList().toArray();
	}

	@Override
	public <T> T[] toArray(T[] array) {
		return toArrayList().toArray(array);
	}

	public ArrayList<Chapter> toArrayList() {
		ArrayList<Chapter> mangas = new ArrayList<Chapter>();
		for (String mangaId : mChapterKeyList) {
			mangas.add(mChapterList.get(mangaId));
		}
		return mangas;
	}

	@Override
	public Iterator<Chapter> iterator() {
		return new Iterator<Chapter>() {

			Iterator<String> keys = mChapterKeyList.iterator();

			@Override
			public boolean hasNext() {
				return keys.hasNext();
			}

			@Override
			public Chapter next() {
				return get(keys.next());
			}

			@Override
			public void remove() {
				keys.remove();
			}

		};
	}

	private void countType(Chapter chapter) {
		switch (chapter.typeId) {
		case Chapter.TYPE_ID_VOLUME:
			mSizeVolume++;
			break;
		case Chapter.TYPE_ID_CHAPTER:
			mSizeChapter++;
			break;
		case Chapter.TYPE_ID_UNKNOW:
			mSizeUnknow++;
			break;
		}
	}

	@Override
	public String toString() {
		return String.format("{ SiteId:%d, MangaId:'%s', Size:%d, Volume:%d, Chapter:%d, Unknow:%d }", mManga.siteId,
				mManga.mangaId, size(), mSizeVolume, mSizeChapter, mSizeUnknow);
	}

}
