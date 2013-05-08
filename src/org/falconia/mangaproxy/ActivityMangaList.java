package org.falconia.mangaproxy;

import java.util.GregorianCalendar;
import java.util.HashMap;

import org.apache.http.util.EncodingUtils;
import org.falconia.mangaproxy.data.Genre;
import org.falconia.mangaproxy.data.Manga;
import org.falconia.mangaproxy.data.MangaList;
import org.falconia.mangaproxy.data.Site;
import org.falconia.mangaproxy.plugin.Plugins;
import org.falconia.mangaproxy.task.DownloadTask;
import org.falconia.mangaproxy.task.OnDownloadListener;
import org.falconia.mangaproxy.task.OnSourceProcessListener;
import org.falconia.mangaproxy.task.SourceProcessTask;
import org.falconia.mangaproxy.ui.BaseHeadersAdapter;
import org.falconia.mangaproxyex.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

public final class ActivityMangaList extends ActivityBase implements OnClickListener {

	public final static class IntentHandler {

		private static final String BUNDLE_KEY_GENRE_DATA = "BUNDLE_KEY_GENRE_DATA";

		private static Intent getIntent(Context context, Genre genre) {
			Bundle bundle = new Bundle();
			bundle.putSerializable(BUNDLE_KEY_GENRE_DATA, genre);
			Intent i = new Intent(context, ActivityMangaList.class);
			i.putExtras(bundle);
			return i;
		}

		protected static Genre getGenre(ActivityMangaList activity) {
			return (Genre) activity.getIntent().getExtras().getSerializable(BUNDLE_KEY_GENRE_DATA);
		}

		public static void startActivityMangaList(Context context, Genre genre) {
			context.startActivity(getIntent(context, genre));
		}

		public static void startActivityAllMangaList(Context context, int siteId) {
			context.startActivity(getIntent(context, (new Site(siteId)).getGenreAll()));
		}

	}

	private final static class Configuration {
		private MangaList mMangaList;
		private int mPageMax;
		private int mPageLoaded;
	}

	private final class NextPageDownloader implements OnDownloadListener, OnSourceProcessListener {

		public static final int MODE_DEFAULT = 0;
		public static final int MODE_DOWNLOAD = 1;
		public static final int MODE_PROCESS = 2;
		public static final int MODE_DOWNLOAD_ERROR = 3;
		public static final int MODE_PROCESS_ERROR = 4;

		private DownloadTask mDownloader;
		private final String mCharset;

		private View mListItem;
		private ProgressBar mProgress;
		private TextView mMessage;
		private TextView mDescribe;

		private int mMode;

		public NextPageDownloader() {
			mCharset = Plugins.getPlugin(getSiteId()).getCharset();

			mListItem = getLayoutInflater().inflate(R.layout.list_item_load, null);
			mProgress = (ProgressBar) mListItem.findViewById(R.id.mpbProgress);
			mMessage = (TextView) mListItem.findViewById(R.id.mtvMessage);
			mDescribe = (TextView) mListItem.findViewById(R.id.mtvDescribe);
			setMode(MODE_DEFAULT);
		}

		@Override
		public void onPreDownload() {
			AppUtils.logV(this, "onPreDownload()");
			setMode(MODE_DOWNLOAD);
		}

		@Override
		public void onPostDownload(byte[] result) {
			AppUtils.logV(this, "onPostDownload()");
			if (result == null || result.length == 0) {
				AppUtils.logE(this, "Downloaded empty source.");
				setNoItemsMessage(String.format(getString(R.string.ui_error_on_download), getSiteName()));
				setMode(MODE_DOWNLOAD_ERROR);
				return;
			}

			String source = EncodingUtils.getString(result, mCharset);

			setMode(MODE_PROCESS);
			ActivityMangaList.this.mSourceProcessTask = new SourceProcessTask(this);
			ActivityMangaList.this.mSourceProcessTask.execute(source, mGenre.getUrl(mPageLoaded + 1));
		}

		@Override
		public void onDownloadProgressUpdate(int value, int total) {
			if (mMode == MODE_DOWNLOAD) {
				mMessage.setText(String.format(getString(R.string.ui_download_page), (value) / 1024.0f));
			}
		}

		@Override
		public int onSourceProcess(String source, String url) {
			MangaList mangaList = mGenre.getMangaList(source, url);
			int size = mangaList.size();
			if (size > 0) {
				mMangaList.addAll(mangaList);
			}
			return size;
		}

		@Override
		public void onPreSourceProcess() {
			AppUtils.logV(this, "onPreSourceProcess()");
		}

		@Override
		public void onPostSourceProcess(int size) {
			AppUtils.logV(this, "onPostSourceProcess()");
			if (size > 0) {
				// ((MangaListAdapter) mListAdapter).setMangaList(mMangaList);
				mListAdapter.notifyDataSetChanged();
				mPageLoaded++;
				AppUtils.popupMessage(ActivityMangaList.this,
						String.format(getString(R.string.popup_loaded_page), mPageLoaded));
				setMode(MODE_DEFAULT);
			} else {
				setMode(MODE_PROCESS_ERROR);
				return;
			}
			if (mPageLoaded == mPageMax) {
				getListView().removeFooterView(getFooter());
				setCustomTitle(mGenre.displayname);
			}
		}

		public View getFooter() {
			return mListItem;
		}

		public void setMode(int mode) {
			mMode = mode;
			switch (mMode) {
			case MODE_DEFAULT:
				if (mPageLoaded < mPageMax) {
					setCustomTitle(String.format("%s (%d/%d)", mGenre.displayname, mPageLoaded, mPageMax));
				}
				setProgressBarIndeterminateVisibility(false);
				mProgress.setVisibility(View.GONE);
				mMessage.setText(R.string.ui_load_next_page);
				mDescribe.setText(String.format("(%d/%d)", mPageLoaded + 1, mPageMax));
				break;
			case MODE_DOWNLOAD:
				setProgressBarIndeterminateVisibility(true);
				mProgress.setVisibility(View.VISIBLE);
				mMessage.setText(String.format(getString(R.string.ui_download_page), 0.0f));
				mDescribe.setText(String.format("(%s)", getString(R.string.ui_click_to_cancel)));
				break;
			case MODE_PROCESS:
				setProgressBarIndeterminateVisibility(true);
				mProgress.setVisibility(View.VISIBLE);
				mMessage.setText(R.string.ui_process_page);
				mDescribe.setText(String.format("(%d/%d)", mPageLoaded + 1, mPageMax));
				break;
			case MODE_DOWNLOAD_ERROR:
				setProgressBarIndeterminateVisibility(false);
				mProgress.setVisibility(View.GONE);
				mMessage.setText(R.string.ui_error);
				mDescribe.setText(String.format("(%s)",
						String.format(getString(R.string.ui_fail_to_download), mPageLoaded + 1)));
				break;
			case MODE_PROCESS_ERROR:
				setProgressBarIndeterminateVisibility(false);
				mProgress.setVisibility(View.GONE);
				mMessage.setText(R.string.ui_error);
				mDescribe.setText(String.format("(%s)",
						String.format(getString(R.string.ui_fail_to_process), mPageLoaded + 1)));
				break;
			}
		}

		public void click() {
			switch (mMode) {
			case MODE_DEFAULT:
				AppUtils.logV(this, "click() @MODE_DEFAULT");
				AppUtils.logD(this, "Download Page " + (mPageLoaded + 1));
				mDownloader = new DownloadTask(this, "",Plugins.getPlugin(getSiteId()).getCookies());
				mDownloader.execute(mGenre.getUrl(mPageLoaded + 1));
				break;
			case MODE_DOWNLOAD:
				AppUtils.logV(this, "click() @MODE_DOWNLOAD");
				if (mDownloader != null && mDownloader.getStatus() == AsyncTask.Status.RUNNING) {
					mDownloader.cancelDownload();
				}
				setMode(MODE_DEFAULT);
				break;
			case MODE_DOWNLOAD_ERROR:
			case MODE_PROCESS_ERROR:
				AppUtils.logV(this, "click() @MODE_ERROR");
				setMode(MODE_DEFAULT);
				break;
			}
		}

		public void cancelDownload() {
			if (mDownloader != null && mDownloader.getStatus() == AsyncTask.Status.RUNNING) {
				AppUtils.logD(this, "Cancel DownloadTask.");
				mDownloader.cancelDownload();
			}
		}
	}

	private final class MangaListAdapter extends BaseHeadersAdapter {

		final class ViewHolder {
			public TextView tvDisplayname;
			public TextView tvDetails;
			public TextView tvCompleted;
			public CheckBox cbFavorite;
		}

		private LayoutInflater mInflater;

		public MangaListAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			if (mMangaList == null) {
				return 0;
			}
			return mMangaList.size();
		}

		@Override
		public Manga getItem(int position) {
			return mMangaList.getAt(position);
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).mangaId.hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final Manga manga = getItem(position);
			final ViewHolder holder;

			if (convertView == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.list_item_manga, null);
				holder.tvDisplayname = (TextView) convertView.findViewById(R.id.mtvDisplayname);
				holder.tvDetails = (TextView) convertView.findViewById(R.id.mtvDetails);
				holder.tvCompleted = (TextView) convertView.findViewById(R.id.mtvCompleted);
				holder.cbFavorite = (CheckBox) convertView.findViewById(R.id.mcbFavorite);
				holder.cbFavorite.setOnClickListener(ActivityMangaList.this);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			if (mFavoriteMangaList != null && mFavoriteMangaList.containsKey(manga.mangaId)) {
				final Manga mangaFav = mFavoriteMangaList.get(manga.mangaId);
				manga._id = mangaFav._id;
				manga.lastReadChapterId = mangaFav.lastReadChapterId;
				manga.isFavorite = true;
			}

			holder.tvDisplayname.setText(manga.displayname);
			holder.tvDetails.setText(manga.getDetails());
			holder.tvCompleted.setVisibility(manga.isCompleted ? View.VISIBLE : View.GONE);
			holder.cbFavorite.setChecked(manga.isFavorite);
			holder.cbFavorite.setTag(manga);

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
			final AppSQLite db = App.DATABASE.open();
			try {
				mFavoriteMangaList = db.getMangasBySite(getSiteId());
			} catch (SQLException e) {
				AppUtils.logE(this, e.getMessage());
			}
			db.close();
			super.notifyDataSetChanged();
		}

	}

	private Genre mGenre;
	private MangaList mMangaList;
	private HashMap<String, Manga> mFavoriteMangaList;
	private String mUrl;
	private int mPageMax;
	private int mPageLoaded;

	private NextPageDownloader mNextPageDownloader;

	// private FadeAnimation mhFadeAnim;
	// private final IOnFadeEndListener mhOnFadeInEnd;
	// private final IOnFadeEndListener mhOnFadeOutEnd;

	public ActivityMangaList() {
		mPageLoaded = 0;
		mPageMax = 0;
	}

	@Override
	public int getSiteId() {
		return mGenre.siteId;
	}

	@Override
	public String getSiteName() {
		return mGenre.getSiteName();
	}

	@Override
	String getSiteDisplayname() {
		return mGenre.getSiteDisplayname();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mGenre = IntentHandler.getGenre(this);
		if (mGenre == null) {
			finish();
		}

		setContentView(R.layout.activity_manga_list);
		setCustomTitle(mGenre.displayname);

		// this.mbShowProcessDialog = false;

		setupListView(new MangaListAdapter(this));

		findViewById(R.id.mvgSearch).setVisibility(View.GONE);

		if (!mProcessed) {
			loadMangaList();
		} else {
			final Configuration conf = (Configuration) getLastNonConfigurationInstance();
			mMangaList = conf.mMangaList;
			mPageMax = conf.mPageMax;
			mPageLoaded = conf.mPageLoaded;
			if (mPageLoaded < mPageMax) {
				showFooter();
			}
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
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		mListAdapter.notifyDataSetChanged();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		AppUtils.logV(this, "onRetainNonConfigurationInstance()");

		Configuration conf = new Configuration();

		conf.mMangaList = mMangaList;
		conf.mPageMax = mPageMax;
		conf.mPageLoaded = mPageLoaded;

		return conf;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AppUtils.logV(this, "onCreateDialog()");
		Dialog dialog;
		switch (id) {
		case DIALOG_DOWNLOAD_ID:
			dialog = mSourceDownloader.createDownloadDialog(mGenre.isGenreSearch() ? R.string.source_of_search_list
					: R.string.source_of_genre_list);
			break;
		case DIALOG_PROCESS_ID:
			dialog = createProcessDialog(mGenre.isGenreSearch() ? R.string.source_of_search_list
					: R.string.source_of_genre_list);
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
			if (mPageMax > 0) {
				if (mPageMax > mPageLoaded)
					getListView().removeFooterView(mNextPageDownloader.getFooter());
				mPageLoaded = 0;
			}
			if (mSourceDownloader == null) {
				mSourceDownloader = new SourceDownloader();
			}
			mSourceDownloader.download(mUrl);
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
		if (mNextPageDownloader != null && view == mNextPageDownloader.getFooter()) {
			mNextPageDownloader.click();
		} else {
			ActivityChapterList.IntentHandler.startActivityMangaList(this, mMangaList.getAt(position));
		}
	}

	@Override
	public void onClick(View view) {
		// For favorite CheckBox in Manga item
		if (view instanceof CheckBox) {
			final CheckBox button = (CheckBox) view;
			final Manga manga = (Manga) button.getTag();
			if (button.isChecked()) {
				if (!modifiedFavorite(manga, button.isChecked())) {
					button.toggle();
				}
			} else {
				button.toggle();
				DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == DialogInterface.BUTTON_POSITIVE) {
							button.toggle();
							if (!modifiedFavorite(manga, button.isChecked())) {
								button.toggle();
							}
						}
					}
				};
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.dialog_confirm_to_remove_favorite).setCancelable(true)
						.setPositiveButton(R.string.dialog_ok, listener)
						.setNegativeButton(R.string.dialog_cancel, listener).show();
			}
		}
	}

	@Override
	public int onSourceProcess(String source, String url) {
		super.onSourceProcess(source, url);
		mMangaList = mGenre.getMangaList(source, url);
		if (mPageMax == 0) {
			mPageMax = mMangaList.pageIndexMax;
		}
		return mMangaList.size();
	}

	@Override
	public void onPostSourceProcess(int size) {
		if (mMangaList.searchEmpty) {
			size = 1;
		}

		if (size > 0) {
			mPageLoaded++;
		}
		if (mPageLoaded < mPageMax) {
			showFooter();
		}
		// ((MangaListAdapter) mListAdapter).setMangaList(mMangaList);
		mListAdapter.notifyDataSetChanged();
		getListView().requestFocus();

		super.onPostSourceProcess(size);
	}

	@Override
	protected void startProcessSource(String source, String url) {
		startProcessSource(source, url, true);
	}

	private void startProcessSource(String source, String url, boolean writeCache) {
		if (writeCache && mGenre.isGenreAll() && source.length() > 200000) {
			AppCache.writeCacheForData(source, mUrl);
			AppUtils.popupMessage(this, R.string.popup_cache_save_allmanga);
		}
		super.startProcessSource(source, url);
	}

	@Override
	protected void stopTask() {
		super.stopTask();

		if (mNextPageDownloader != null) {
			mNextPageDownloader.cancelDownload();
		}
	}

	private void loadMangaList() {
		mUrl = mGenre.getUrl();
		if (mGenre.isGenreAll() && AppCache.checkCacheForData(mUrl, 3600)) {
			String source = AppCache.readCacheForData(mUrl);
			AppUtils.popupMessage(ActivityMangaList.this, R.string.popup_cache_load_allmanga);
			startProcessSource(source, mUrl, false);
		} else {
			mSourceDownloader = new SourceDownloader();
			mSourceDownloader.download(mUrl);
		}
	}

	private void showFooter() {
		mNextPageDownloader = new NextPageDownloader();
		getListView().addFooterView(mNextPageDownloader.getFooter(), null, true);
		setListAdapter(mListAdapter);
	}

	private boolean modifiedFavorite(Manga manga, boolean add) {
		boolean modified = false;
		final AppSQLite db = App.DATABASE.open();
		if (add) {
			AppUtils.logI(this, "Add to Favorite.");
			try {
				manga.updatedAt = new GregorianCalendar(mGenre.getTimeZone());
				manga.updatedAt.setTimeInMillis(System.currentTimeMillis());

				long id = db.insertManga(manga);
				manga._id = id;
				manga.isFavorite = true;
				modified = true;
				AppUtils.logD(this, "Add as ID " + id + ".");
			} catch (SQLException e) {
				AppUtils.logE(this, e.getMessage());
			}
		} else {
			AppUtils.logI(this, "Remove from Favorite.");
			int deleted;
			if ((deleted = db.deleteManga(manga)) != 0) {
				manga.isFavorite = false;
				modified = true;
				AppUtils.logD(this, "Remove " + deleted + " mangas.");
			} else {
				AppUtils.logE(this, "Remove none.");
			}
		}
		db.close();
		return modified;
	}

}
