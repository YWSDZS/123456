package org.falconia.mangaproxy;

import java.util.HashMap;

import org.falconia.mangaproxy.data.Chapter;
import org.falconia.mangaproxy.data.ChapterList;
import org.falconia.mangaproxy.data.Manga;
import org.falconia.mangaproxy.ui.BaseHeadersAdapter;
import org.falconia.mangaproxyex.R;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

public final class ActivityChapterList extends ActivityBase {

	public final static class IntentHandler {

		private static final String BUNDLE_KEY_MANGA_DATA = "BUNDLE_KEY_MANGA_DATA";

		private static Intent getIntent(Context context, Manga manga) {
			Bundle bundle = new Bundle();
			bundle.putSerializable(BUNDLE_KEY_MANGA_DATA, manga);
			Intent i = new Intent(context, ActivityChapterList.class);
			i.putExtras(bundle);
			return i;
		}

		protected static Manga getManga(ActivityChapterList activity) {
			return (Manga) activity.getIntent().getExtras().getSerializable(BUNDLE_KEY_MANGA_DATA);
		}
		


		public static void startActivityMangaList(Context context, Manga manga) {
			context.startActivity(getIntent(context, manga));
		}

	}

	private final class ChapterListAdapter extends BaseHeadersAdapter {

		final class ViewHolder {
			public TextView tvDisplayname;

			public TextView tvDetails;
			public TextView tvIsVolume;
			public ImageView imIsRead;
		}

		private LayoutInflater mInflater;

		public ChapterListAdapter() {
			mInflater = LayoutInflater.from(ActivityChapterList.this);
		}

		@Override
		public int getCount() {
			if (mChapterList == null) {
				return 0;
			}
			return mChapterList.size();
		}

		@Override
		public Chapter getItem(int position) {
			return mChapterList.getAt(position);
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).chapterId.hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final Chapter chapter = getItem(position);
			final ViewHolder holder;

			if (mFavoriteChapterList != null && mFavoriteChapterList.containsKey(chapter.chapterId)) {
				final Chapter chapterFav = mFavoriteChapterList.get(chapter.chapterId);
				chapter._id = chapterFav._id;
				chapter.pageIndexMax = chapterFav.pageIndexMax;
				chapter.pageIndexLastRead = chapterFav.pageIndexLastRead;
				chapter.isFavorite = true;
			}

			if (convertView == null) {
				holder = new ViewHolder();
				if (mManga.isFavorite) {
					convertView = mInflater.inflate(R.layout.list_item_chapter_favorite, null);
					holder.tvDisplayname = (TextView) convertView.findViewById(R.id.mtvDisplayname);
					holder.tvDetails = (TextView) convertView.findViewById(R.id.mtvDetails);
					holder.tvIsVolume = (TextView) convertView.findViewById(R.id.mtvIsVolume);
					holder.imIsRead = (ImageView) convertView.findViewById(R.id.mimIsRead);
				} else {
					convertView = mInflater.inflate(R.layout.list_item_chapter, null);
					holder.tvDisplayname = (TextView) convertView.findViewById(R.id.mtvDisplayname);
				}
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.tvDisplayname.setText(chapter.displayname);
			if (mManga.isFavorite) {
				if (chapter.typeId == Chapter.TYPE_ID_VOLUME) {
					holder.tvIsVolume.setVisibility(View.VISIBLE);
				} else {
					holder.tvIsVolume.setVisibility(View.GONE);
				}
				if (chapter.isFavorite) {
					holder.tvDetails.setText(String.format(getString(R.string.ui_pages_format),
							chapter.pageIndexLastRead, chapter.pageIndexMax));
					holder.imIsRead.setVisibility(View.VISIBLE);
				} else {
					holder.tvDetails.setText(R.string.ui_pages);
					holder.imIsRead.setVisibility(View.GONE);
				}
			} else {
				if (chapter.typeId == Chapter.TYPE_ID_VOLUME) {
					holder.tvDisplayname.setTextColor(getResources().getColor(R.color.highlight));
				} else {
					holder.tvDisplayname.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
				}
			}

			return convertView;
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// TODO Auto-generated method stub

		}

		@Override
		public void notifyDataSetChanged() {
			if (mManga.isFavorite) {
				final AppSQLite db = App.DATABASE.open();
				try {
					mFavoriteChapterList = db.getChaptersByManga(mManga);
				} catch (SQLException e) {
					AppUtils.logE(this, e.getMessage());
				}
				db.close();
			}
			super.notifyDataSetChanged();
		}

	}

	private static final String BUNDLE_KEY_CHAPTER_LIST = "BUNDLE_KEY_CHAPTER_LIST";

	private Manga mManga;
	private ChapterList mChapterList;
	private HashMap<String, Chapter> mFavoriteChapterList;
	private int mCurrentChapterIdx = -1;
	
	public int getCurrentChapterIdx(){
		return this.mCurrentChapterIdx;
	}

	@Override
	public int getSiteId() {
		return mManga.siteId;
	}

	@Override
	String getSiteName() {
		return mManga.getSiteName();
	}

	@Override
	String getSiteDisplayname() {
		return mManga.getSiteDisplayname();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mManga = IntentHandler.getManga(this);
		if (mManga == null) {
			finish();
		}

		setContentView(R.layout.activity_chapter_list);
		setCustomTitle(mManga.displayname);

		// this.mbShowProcessDialog = false;

		setupListView(new ChapterListAdapter());

		if (!mProcessed) {
			loadChapterList();
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();

		mListAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putSerializable(BUNDLE_KEY_CHAPTER_LIST, mChapterList);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		mChapterList = (ChapterList) savedInstanceState.getSerializable(BUNDLE_KEY_CHAPTER_LIST);
		mListAdapter.notifyDataSetChanged();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_DOWNLOAD_ID:
			dialog = mSourceDownloader.createDownloadDialog(R.string.source_of_chapter_list);
			break;
		case DIALOG_PROCESS_ID:
			dialog = createProcessDialog(R.string.source_of_chapter_list);
			break;
		default:
			dialog = super.onCreateDialog(id);
		}
		return dialog;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_base_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.mmiRefresh:
			mProcessed = false;
			mSourceDownloader.download(mManga.getUrl());
			return true;
		case R.id.mmiPreferences:
			startActivity(new Intent(this, ActivityPreference.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ActivityChapter.IntentHandler.startActivityChapter(this, mManga, mChapterList.getAt(position), this.mChapterList , position);
		this.mCurrentChapterIdx = position;
	}

	@Override
	public int onSourceProcess(String source, String url) {
		mChapterList = mManga.getChapterList(source, url);
		mManga.chapterList = mChapterList;
		return mChapterList.size();
	}

	@Override
	public void onPostSourceProcess(int result) {
		// Update manga in database
		if (mManga.isFavorite && result > 0) {
			mManga.latestChapterId = mChapterList.getChapterId(0);
			mManga.latestChapterDisplayname = mChapterList.getDisplayname(0);

			final AppSQLite db = App.DATABASE.open();
			try {
				AppUtils.logI(this, "Update Manga in Favorite.");
				if (db.updateManga(mManga) == 0) {
					AppUtils.logE(this, "Fail to update Manga in Favorite.");
				}
			} catch (SQLException e) {
				AppUtils.logE(this, e.getMessage());
			}
			db.close();
		}

		mListAdapter.notifyDataSetChanged();
		getListView().requestFocus();

		if (mManga.isFavorite && result > 0 && mManga.lastReadChapterId != null) {
			getListView().setSelection(mChapterList.indexOfChapterId(mManga.lastReadChapterId) - 1);
		}

		super.onPostSourceProcess(result);
	}

	private void loadChapterList() {
		mSourceDownloader = new SourceDownloader();
		mSourceDownloader.download(mManga.getUrl());
	}

}
