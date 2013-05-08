package org.falconia.mangaproxy;

import java.util.ArrayList;
import java.util.HashMap;

import org.falconia.mangaproxy.data.Chapter;
import org.falconia.mangaproxy.data.Manga;
import org.falconia.mangaproxyex.R;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public final class AppSQLite {

	public static final int UPDATE_NONE = 0;
	public static final int UPDATE_LAST_READ = 1;

	// Database

	public static final String DATABASE_NAME = "mangaproxy";
	public static final String DATABASE_TABLE_MANGA = "tManga";
	public static final String DATABASE_TABLE_CHAPTER = "tChapter";
	public static final int DATABASE_VERSION = 3;

	public static final String KEY_ROW_ID = BaseColumns._ID;

	// Manga

	public static final String KEY_SITE_ID = "iSiteId";
	public static final String KEY_MANGA_ID = "sMangaId";
	public static final String KEY_SECTION_ID = "sSectionId";
	public static final String KEY_DISPLAYNAME = "sDisplayname";

	public static final String KEY_IS_COMPLETED = "bIsCompleted";
	public static final String KEY_CHAPTER_COUNT = "iChapterCount";
	public static final String KEY_HAS_NEW_CHAPTER = "bHasNewChapter";

	public static final String KEY_LATEST_CHAPTER_ID = "sLatestChapterId";
	public static final String KEY_LATEST_CHAPTER_DISPLAYNAME = "sLatestChapterDisplayname";
	public static final String KEY_LAST_READ_CHAPTER_ID = "sLastReadChapterId";

	public static final String KEY_UPDATED_AT = "iUpdatedAt";
	public static final String KEY_UPDATED_AT_TIMEZONE = "iUpdatedAtTimeZone";

	// Chapter

	public static final String KEY_MANGA_REF_ID = "iMangaId";
	public static final String KEY_CHAPTER_ID = "sChapterId";

	public static final String KEY_PAGE_MAX = "iPageMax";
	public static final String KEY_PAGE_LAST_READ = "iPageLastRead";

	// Create

	public static final String DATABASE_TABLE_MANGA_CREATE = ""
			+ String.format("CREATE TABLE %s (", DATABASE_TABLE_MANGA)
			+ String.format("%s INTEGER PRIMARY KEY AUTOINCREMENT, ", KEY_ROW_ID)
			+ String.format("%s INT NOT NULL, ", KEY_SITE_ID) + String.format("%s TEXT NOT NULL, ", KEY_MANGA_ID)
			+ String.format("%s TEXT, ", KEY_DISPLAYNAME) + String.format("%s INT1 DEFAULT 0, ", KEY_IS_COMPLETED)
			+ String.format("%s NUM DEFAULT 0, ", KEY_CHAPTER_COUNT)
			+ String.format("%s INT1 DEFAULT 0, ", KEY_HAS_NEW_CHAPTER)
			+ String.format("%s TEXT, ", KEY_LATEST_CHAPTER_ID)
			+ String.format("%s TEXT, ", KEY_LATEST_CHAPTER_DISPLAYNAME)
			+ String.format("%s TEXT, ", KEY_LAST_READ_CHAPTER_ID)
			+ String.format("%s DATETIME DEFAULT 0, ", KEY_UPDATED_AT)
			+ String.format("%s TEXT, ", KEY_UPDATED_AT_TIMEZONE)
	        + String.format("%s TEXT )", KEY_SECTION_ID);
	public static final String DATABASE_TABLE_CHAPTER_CREATE = ""
			+ String.format("CREATE TABLE %s (", DATABASE_TABLE_CHAPTER)
			+ String.format("%s INTEGER PRIMARY KEY AUTOINCREMENT, ", KEY_ROW_ID)
			+ String.format("%s INT NOT NULL, ", KEY_MANGA_REF_ID)
			+ String.format("%s TEXT NOT NULL, ", KEY_CHAPTER_ID) + String.format("%s TEXT, ", KEY_DISPLAYNAME)
			+ String.format("%s NUM DEFAULT 0, ", KEY_PAGE_MAX)
			+ String.format("%s NUM DEFAULT 0 )", KEY_PAGE_LAST_READ);


	public class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper() {
			super(mContext, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				db.execSQL(DATABASE_TABLE_MANGA_CREATE);
				db.execSQL(DATABASE_TABLE_CHAPTER_CREATE);
				AppUtils.logI(this, "Created initial database structure.");
			} catch (SQLException e) {
				AppUtils.logE(this, e.getMessage());
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion == 2 && newVersion == 3) {
				db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_MANGA);
				db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_CHAPTER);
				onCreate(db);
				AppUtils.logI(this, "Update database structure (2 -> 3).");
			}
		}
	}

	public DatabaseHelper dbHelper;
	public SQLiteDatabase db;

	private final Context mContext;

	public AppSQLite(Context context) {
		mContext = context;
		dbHelper = new DatabaseHelper();
	}

	public AppSQLite open() throws SQLException {
		db = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		dbHelper.close();
	}

	public boolean isOpen() {
		return db != null && db.isOpen();
	}

	// For manga

	public Cursor getMangasCursor(String selection, String orderBy) throws SQLException {
		String[] columns = new String[] { KEY_ROW_ID };
		Cursor cursor = db.query(true, DATABASE_TABLE_MANGA, columns, selection, null, null, null, orderBy, null);

		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	public Cursor getAllMangasCursor() throws SQLException {
		return getMangasCursor(null,
				String.format("%s DESC, %s DESC, %s", KEY_HAS_NEW_CHAPTER, KEY_UPDATED_AT, KEY_DISPLAYNAME));
	}

	public ArrayList<Manga> getAllMangas(String orderBy) throws SQLException {
		Cursor cursor = getMangasCursor(null, orderBy);

		if (cursor == null || cursor.getCount() == 0) {
			return null;
		}

		ArrayList<Manga> mangaList = new ArrayList<Manga>();
		int count = cursor.getCount();
		for (int i = 0; i < count; i++) {
			Manga manga = getManga(cursor);
			mangaList.add(manga);
			cursor.moveToNext();
		}
		cursor.close();
		return mangaList;
	}

	public ArrayList<Manga> getAllMangas() throws SQLException {
		return getAllMangas(null);
	}

	public HashMap<String, Manga> getMangasBySite(int siteId) throws SQLException {
		String selection = String.format("%s=%d", KEY_SITE_ID, siteId);
		Cursor cursor = getMangasCursor(selection, null);

		if (cursor == null || cursor.getCount() == 0) {
			return null;
		}

		HashMap<String, Manga> mangaList = new HashMap<String, Manga>();
		int count = cursor.getCount();
		for (int i = 0; i < count; i++) {
			Manga manga = getManga(cursor);
			mangaList.put(manga.mangaId, manga);
			cursor.moveToNext();
		}
		cursor.close();
		return mangaList;
	}

	public Manga getManga(Cursor cursor) throws SQLException {
		long id = cursor.getLong(0);

		return getManga(id);
	}

	public Manga getManga(long id) throws SQLException {
		String selection = String.format("%s=%d", KEY_ROW_ID, id);
		Cursor cursor = db.query(true, DATABASE_TABLE_MANGA, null, selection, null, null, null, null, null);

		if (cursor != null) {
			cursor.moveToFirst();
		}
        
		Manga manga = getMangaFromRow(cursor);
		cursor.close();

		return manga;
	}

	public Manga getMangaFromRow(Cursor cursor) throws SQLException {
		if (cursor == null || cursor.getCount() == 0) {
			return null;
		}

		Manga manga = Manga
				.getFavoriteManga(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getString(3),
						cursor.getInt(4) != 0, cursor.getInt(5), cursor.getInt(6) != 0, cursor.getString(7),
						cursor.getString(8), cursor.getString(9), cursor.getLong(10), cursor.getString(11),cursor.getString(12));
		return manga;
	}

	public long containsManga(Manga manga) throws SQLException {
		String[] columns = new String[] { KEY_ROW_ID };
		String selection = String.format("%s=%d AND %s='%s'", KEY_SITE_ID, manga.siteId, KEY_MANGA_ID, manga.mangaId);
		Cursor cursor = db.query(true, DATABASE_TABLE_MANGA, columns, selection, null, null, null, null, null);

		if (cursor == null || cursor.getCount() == 0) {
			return -1;
		} else {
			cursor.moveToFirst();
		}

		long id = cursor.getLong(0);
		cursor.close();
		return id;
	}

	public long insertManga(Manga manga) throws SQLException {
		final long id = containsManga(manga);
		if (id > -1) {
			return id;
		}

		ContentValues values = new ContentValues();
		values.put(KEY_SITE_ID, manga.siteId);
		values.put(KEY_MANGA_ID, manga.mangaId);
		values.put(KEY_DISPLAYNAME, manga.displayname);
		values.put(KEY_IS_COMPLETED, manga.isCompleted);
		values.put(KEY_CHAPTER_COUNT, manga.chapterCount);
		values.put(KEY_HAS_NEW_CHAPTER, manga.hasNewChapter);
		values.put(KEY_LATEST_CHAPTER_DISPLAYNAME, manga.latestChapterDisplayname);
		if (manga.updatedAt != null) {
			values.put(KEY_UPDATED_AT, manga.updatedAt.getTimeInMillis());
			values.put(KEY_UPDATED_AT_TIMEZONE, manga.updatedAt.getTimeZone().getID());
		}
		values.put(KEY_SECTION_ID, manga.section);
		return db.insertOrThrow(DATABASE_TABLE_MANGA, null, values);
	}

	public int updateManga(Manga manga) throws SQLException {
		String selection = String.format("%s=%d", KEY_ROW_ID, manga._id);
		ContentValues values = new ContentValues();
		values.put(KEY_IS_COMPLETED, manga.isCompleted);
		values.put(KEY_CHAPTER_COUNT, manga.chapterCount);
		manga.hasNewChapter = manga.lastReadChapterId == null || !manga.lastReadChapterId.equals(manga.latestChapterId);
		values.put(KEY_HAS_NEW_CHAPTER, manga.hasNewChapter);
		values.put(KEY_LATEST_CHAPTER_DISPLAYNAME, manga.latestChapterDisplayname);
		if (manga.updatedAt != null) {
			values.put(KEY_UPDATED_AT, manga.updatedAt.getTimeInMillis());
			values.put(KEY_UPDATED_AT_TIMEZONE, manga.updatedAt.getTimeZone().getID());
		}
		values.put(KEY_SECTION_ID, manga.section);
		return db.update(DATABASE_TABLE_MANGA, values, selection, null);
	}

	public int updateMangaLastReadChapter(Manga manga, String chapterId) throws SQLException {
		String selection = String.format("%s=%d", KEY_ROW_ID, manga._id);
		ContentValues values = new ContentValues();
		manga.hasNewChapter = manga.lastReadChapterId == null || !manga.lastReadChapterId.equals(manga.latestChapterId);
		values.put(KEY_HAS_NEW_CHAPTER, manga.hasNewChapter);
		values.put(KEY_LAST_READ_CHAPTER_ID, chapterId);
		return db.update(DATABASE_TABLE_MANGA, values, selection, null);
	}

	public int deleteManga(Manga manga) throws SQLException {
		String selection = String.format("%s=%d", KEY_MANGA_REF_ID, manga._id);
		int delChapters = db.delete(DATABASE_TABLE_CHAPTER, selection, null);

		selection = String.format("%s=%d AND %s='%s'", KEY_SITE_ID, manga.siteId, KEY_MANGA_ID, manga.mangaId);
		int delMangas = db.delete(DATABASE_TABLE_MANGA, selection, null);

		return delMangas * 1000 + delChapters;
	}

	// For chapter

	public HashMap<String, Chapter> getChaptersByManga(Manga manga) throws SQLException {
		String selection = String.format("%s=%d", KEY_MANGA_REF_ID, manga._id);
		Cursor cursor = db.query(true, DATABASE_TABLE_CHAPTER, null, selection, null, null, null, null, null);

		if (cursor == null || cursor.getCount() == 0) {
			return null;
		} else {
			cursor.moveToFirst();
		}

		HashMap<String, Chapter> chapterList = new HashMap<String, Chapter>();
		int count = cursor.getCount();
		for (int i = 0; i < count; i++) {
			Chapter chapter = getChapterFromRow(cursor);
			chapterList.put(chapter.chapterId, chapter);
			cursor.moveToNext();
		}
		cursor.close();
		return chapterList;
	}

	public Chapter getChapterFromRow(Cursor cursor, Manga manga) throws SQLException {
		if (cursor == null || cursor.getCount() == 0) {
			return null;
		}

		if (manga == null) {
			manga = getManga(cursor.getInt(1));
		}

		Chapter chapter = Chapter.getFavoriteChapter(cursor.getInt(0), manga, cursor.getString(2), cursor.getString(3),
				cursor.getInt(4), cursor.getInt(5));
		return chapter;
	}

	public Chapter getChapterFromRow(Cursor cursor) throws SQLException {
		return getChapterFromRow(cursor, null);
	}

	public long containsChapter(Chapter chapter) throws SQLException {
		String[] columns = new String[] { KEY_ROW_ID };
		String selection = String.format("%s=%d AND %s='%s'", KEY_MANGA_REF_ID, chapter.manga._id, KEY_CHAPTER_ID,
				chapter.chapterId);
		Cursor cursor = db.query(true, DATABASE_TABLE_CHAPTER, columns, selection, null, null, null, null, null);

		if (cursor == null || cursor.getCount() == 0) {
			return -1;
		} else {
			cursor.moveToFirst();
		}

		long id = cursor.getLong(0);
		cursor.close();
		return id;
	}

	public long insertChapter(Chapter chapter) throws SQLException {
		long id = containsChapter(chapter);
		if (id > -1) {
			updateMangaLastReadChapter(chapter.manga, chapter.chapterId);
			return id;
		}

		ContentValues values = new ContentValues();
		values.put(KEY_MANGA_REF_ID, chapter.manga._id);
		values.put(KEY_CHAPTER_ID, chapter.chapterId);
		values.put(KEY_DISPLAYNAME, chapter.displayname);
		values.put(KEY_PAGE_MAX, chapter.pageIndexMax);
		values.put(KEY_PAGE_LAST_READ, chapter.pageIndexLastRead);
		id = db.insertOrThrow(DATABASE_TABLE_CHAPTER, null, values);

		updateMangaLastReadChapter(chapter.manga, chapter.chapterId);

		return id;
	}

	public int updateChapter(Chapter chapter) throws SQLException {
		String selection = String.format("%s=%d", KEY_ROW_ID, chapter._id);
		ContentValues values = new ContentValues();
		values.put(KEY_PAGE_MAX, chapter.pageIndexMax);
		values.put(KEY_PAGE_LAST_READ, chapter.pageIndexLastRead);
		return db.update(DATABASE_TABLE_CHAPTER, values, selection, null);
	}

}
