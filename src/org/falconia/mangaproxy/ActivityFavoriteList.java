package org.falconia.mangaproxy;

import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.http.util.EncodingUtils;
import org.falconia.mangaproxy.data.ChapterList;
import org.falconia.mangaproxy.data.Manga;
import org.falconia.mangaproxy.data.Site;
import org.falconia.mangaproxy.plugin.Plugins;
import org.falconia.mangaproxy.task.DownloadTask;
import org.falconia.mangaproxy.task.OnDownloadListener;
import org.falconia.mangaproxy.task.OnSourceProcessListener;
import org.falconia.mangaproxy.task.SourceProcessTask;
import org.falconia.mangaproxy.utils.FormatUtils;
import org.falconia.mangaproxy.utils.HttpUtils;
import org.falconia.mangaproxy.utils.Regex;
import org.falconia.mangaproxyex.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public final class ActivityFavoriteList extends ActivityBase implements OnClickListener {

	private final class Updater implements OnDownloadListener, OnSourceProcessListener {

		private final LinkedList<Manga> mQueue;

		private DownloadTask mDownloader;
		private SourceProcessTask mProcessTask;

		private Manga mUpdatedManga;
		private int mUpdated;
		private boolean mUpdating;

		public Updater() {
			mQueue = new LinkedList<Manga>();
			mUpdating = false;
		}

		@Override
		public void onPreDownload() {
			AppUtils.logV(this, "onPreDownload()");

		}

		@Override
		public void onPostDownload(byte[] result) {
			AppUtils.logV(this, "onPostDownload()");

			if (result == null || result.length == 0) {
				AppUtils.logE(this, "Downloaded empty source.");
				dequeue();
				if (mUpdating) {
					update(mQueue.peek());
				}
				return;
			}

			String source = EncodingUtils.getString(result, mUpdatedManga.getSiteCharset());
			mProcessTask = new SourceProcessTask(this);
			mProcessTask.execute(source, mUpdatedManga.getUrl());
		}

		@Override
		public void onDownloadProgressUpdate(int value, int total) {

		}

		@Override
		public int onSourceProcess(String source, String url) {
			ChapterList list = mUpdatedManga.getChapterList(source, url);
			if (list.size() > 0) {
				mUpdatedManga.latestChapterId = list.getChapterId(0);
				mUpdatedManga.latestChapterDisplayname = list.getDisplayname(0);
			}
			return list.size();
		}

		@Override
		public void onPreSourceProcess() {

		}

		@Override
		public void onPostSourceProcess(int size) {
			if (size <= 0) {
				AppUtils.logE(this, "Fail to process source.");
			} else {
				if (mDB.updateManga(mUpdatedManga) > 0) {
					AppUtils.logD(this, String.format("Updated manga %s.", mUpdatedManga));
					mListAdapter.notifyDataSetInvalidated();
				} else {
					AppUtils.logE(this, String.format("Fail to update manga %s.", mUpdatedManga));
				}
			}
			dequeue();
			if (mUpdating) {
				update(mQueue.peek());
			}
		}

		public void queue(ArrayList<Manga> allMangas) {
			if (allMangas == null) {
				return;
			}
			for (Manga manga : allMangas) {
				queue(manga);
			}
		}

		public void update() {
			if (!mUpdating && mQueue.size() > 0) {
				AppUtils.logD(this, "Update mangas.");

				mUpdating = true;
				// setProgressBarVisibility(true);
				showUpdateBar();

				update(mQueue.peek());
			}
		}

		public void cancel() {
			if (mDownloader != null && mDownloader.getStatus() == AsyncTask.Status.RUNNING) {
				AppUtils.logD(this, "Cancel DownloadTask.");
				mDownloader.cancelDownload();
			}

			clear();
		}

		public boolean isUpdating() {
			return mUpdating;
		}

		private int size() {
			return mQueue.size() + mUpdated;
		}

		private void queue(Manga manga) {
			mQueue.addLast(manga);
		}

		private void dequeue() {
			mQueue.removeFirst();
			mUpdated++;
		}

		private void clear() {
			mQueue.clear();
			// mUpdatedManga = null;
			mUpdated = 0;
			mUpdating = false;

			// setProgressBarVisibility(false);
			hideUpdateBar();
		}

		private void update(Manga manga) {
			if (manga == null) {
				AppUtils.logD(this, "Update mangas completed.");

				// setProgressBarVisibility(false);
				// setProgress(10000);
				// hideUpdateBar();
				mUpdatedManga = null;
				updateProgress();
				clear();
			} else {
				AppUtils.logD(this, String.format("Update manga %s.", manga));

				mUpdatedManga = manga;
				mDownloader = new DownloadTask(this, "",Plugins.getPlugin(getSiteId()).getCookies());
				mDownloader.execute(manga.getUrl());
				updateProgress();
			}
		}

		private void updateProgress() {
			// setProgress(10000 * mUpdated / size());
			setUpdateBarProgress(mUpdatedManga, mUpdated, size());
		}
	}

	private final class VersionChecker implements OnDownloadListener {

		private DownloadTask mDownloader;

		@Override
		public void onPreDownload() {
		}

		@Override
		public void onPostDownload(byte[] result) {
			AppUtils.logV(this, "onPostDownload()");

			if (result == null || result.length == 0) {
				AppUtils.logE(this, "Downloaded empty source.");
				return;
			}

			try {
				String source = EncodingUtils.getString(result, HttpUtils.CHARSET_UTF8);
				ArrayList<String> groups = Regex.match("Version (.+?) / VersionCode (\\d+)", source);
				String versionName = groups.get(1).trim();
				int versionCode = Integer.parseInt(groups.get(2));
				if (versionCode > App.VERSION_CODE) {
					mVersionCode = versionCode;
					updateNewVersion(versionName);
				} else {
					updateNewVersion(null);
				}
			} catch (Exception e) {
				AppUtils.logE(this, "Fail to chech new version.");
			}
		}

		@Override
		public void onDownloadProgressUpdate(int value, int total) {
		}

		public void check() {
			mDownloader = new DownloadTask(this, "",Plugins.getPlugin(getSiteId()).getCookies());
			mDownloader.execute(App.URL_LATEST_VERSION_CHECK);
		}

		public void cancel() {
			if (mDownloader != null && mDownloader.getStatus() == AsyncTask.Status.RUNNING) {
				AppUtils.logD(this, "Cancel DownloadTask.");
				mDownloader.cancelDownload();
			}
		}
	}

	private final class FavoriteListAdapter extends CursorAdapter {

		final class ViewHolder {
			public TextView tvDisplayname;
			public TextView tvDetails;
			public TextView tvCompleted;
			public TextView tvSiteName;
			public CheckBox cbFavorite;
		}

		private LayoutInflater mInflater;

		public FavoriteListAdapter(Context context, Cursor c, boolean autoRequery) {
			super(context, c, autoRequery);
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public Manga getItem(int position) {
			Cursor cursor = (Cursor) super.getItem(position);
			return App.DATABASE.getManga(cursor);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			Manga manga = App.DATABASE.getManga(cursor);
			ViewHolder holder = new ViewHolder();
			View view = mInflater.inflate(R.layout.list_item_manga, null);
			holder.tvDisplayname = (TextView) view.findViewById(R.id.mtvDisplayname);
			holder.tvDetails = (TextView) view.findViewById(R.id.mtvDetails);
			holder.tvCompleted = (TextView) view.findViewById(R.id.mtvCompleted);
			holder.tvSiteName = (TextView) view.findViewById(R.id.mtvSiteName);
			holder.tvSiteName.setVisibility(View.VISIBLE);
			holder.cbFavorite = (CheckBox) view.findViewById(R.id.mcbFavorite);
			holder.cbFavorite.setOnClickListener(ActivityFavoriteList.this);
			view.setTag(holder);

			holder.tvDisplayname.setText(manga.displayname);
			if (TextUtils.isEmpty(manga.latestChapterDisplayname)) {
				holder.tvDetails.setText("-");
			} else {
				holder.tvDetails.setText(manga.latestChapterDisplayname);
			}
			if (manga.updatedAt != null) {
				holder.tvDetails.append(String.format(", " + App.UI_LAST_UPDATE_COUNTDOWN,
						FormatUtils.getCountDownDateTime(manga.updatedAt)));
			}
			if (manga.hasNewChapter) {
				holder.tvDetails.setTextColor(getResources().getColor(R.color.highlight));
			} else {
				holder.tvDetails.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
			}
			holder.tvCompleted.setVisibility(manga.isCompleted ? View.VISIBLE : View.GONE);
			holder.tvSiteName.setText(manga.getSiteName());
			holder.cbFavorite.setChecked(manga.isFavorite);
			holder.cbFavorite.setTag(manga);

			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			Manga manga = App.DATABASE.getManga(cursor);
			ViewHolder holder = (ViewHolder) view.getTag();

			holder.tvDisplayname.setText(manga.displayname);
			if (TextUtils.isEmpty(manga.latestChapterDisplayname)) {
				holder.tvDetails.setText("-");
			} else {
				holder.tvDetails.setText(manga.latestChapterDisplayname);
			}
			if (manga.updatedAt != null) {
				holder.tvDetails.append(String.format(", " + App.UI_LAST_UPDATE_COUNTDOWN,
						FormatUtils.getCountDownDateTime(manga.updatedAt)));
			}
			if (manga.hasNewChapter) {
				holder.tvDetails.setTextColor(getResources().getColor(R.color.highlight));
			} else {
				holder.tvDetails.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
			}
			holder.tvCompleted.setVisibility(manga.isCompleted ? View.VISIBLE : View.GONE);
			holder.tvSiteName.setText(manga.getSiteName());
			holder.cbFavorite.setChecked(manga.isFavorite);
			holder.cbFavorite.setTag(manga);
		}

		@Override
		public void notifyDataSetInvalidated() {
			try {
				Cursor cursor = mDB.getAllMangasCursor();
				changeCursor(cursor);
			} catch (SQLException e) {
				AppUtils.logE(this, e.getMessage());
			}
			super.notifyDataSetInvalidated();
		}

	}

	private AppSQLite mDB;

	private Updater mUpdater;
	private VersionChecker mChecker;

	private LinearLayout mvgUpdateBar;
	private TextView mtvUpdating;
	private TextView mtvUpdated;
	private ProgressBar mpbUpdate;

	private LinearLayout mNewVersionPanel;
	private TextView mNewVersion;
	private int mVersionCode = -1;

	private boolean mExit = false;

	private final Handler mHideUpdateBarHandler;
	private final Runnable mHideUpdateBarRunnable;

	public ActivityFavoriteList() {
		mHideUpdateBarHandler = new Handler();
		mHideUpdateBarRunnable = new Runnable() {
			@Override
			public void run() {
				if (mvgUpdateBar != null) {
					mvgUpdateBar.setVisibility(View.GONE);
					mHideUpdateBarHandler.removeCallbacks(this);
				}
			}
		};
	}

	@Override
	public int getSiteId() {
		return Site.SITE_ID_FAVORITE;
	}

	@Override
	public String getSiteName() {
		return "";
	}

	@Override
	String getSiteDisplayname() {
		return "";
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.activity_favorite_list);
		setTitle(String.format("%s %s FalconIA, Mod:Vertusd", App.NAME, ""));
		setNoItemsMessage(R.string.ui_no_favorite_items);

		mvgUpdateBar = (LinearLayout) findViewById(R.id.mvgUpdateBar);
		mvgUpdateBar.setVisibility(View.GONE);
		mtvUpdating = (TextView) findViewById(R.id.mtvUpdating);
		mtvUpdated = (TextView) findViewById(R.id.mtvUpdated);
		mpbUpdate = (ProgressBar) findViewById(R.id.mpbUpdate);
		mpbUpdate.setProgress(0);

		mNewVersionPanel = (LinearLayout) findViewById(R.id.mvgNewVersion);
		mNewVersionPanel.setVisibility(View.GONE);
		mNewVersion = (TextView) findViewById(R.id.mtvNewVersion);
		findViewById(R.id.mbtnUpdate).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mVersionCode >= 0 && mVersionCode > App.VERSION_CODE) {
					downloadNewVersion();
				}
			}
		});

		if (App.getShowChangelog()) {
			startActivity(new Intent(ActivityFavoriteList.this, ActivityChangelog.class));
		}

		getListView().getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				getListView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
				if (App.FIRST_START && App.getFavoriteAutoUpdate()) {
					refresh();
				}
				App.FIRST_START = false;
			}
		});

		checkNewVersion();

		mDB = App.DATABASE.open();
		setupListView(new FavoriteListAdapter(this, null, true));
	}

	@Override
	protected void onResume() {
		System.gc();

		super.onResume();

		mListAdapter.notifyDataSetInvalidated();
	}

	@Override
	protected void onStop() {
		super.onStop();

		if (mUpdater != null) {
			mUpdater.cancel();
		}

		if (mChecker != null) {
			mChecker.cancel();
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();

		if (!mDB.isOpen()) {
			mDB = App.DATABASE.open();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		mDB.close();

		if (mExit) {
			System.exit(0);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_CLOSE_ID:
			dialog = createExitConfirmDialog();
			break;
		case DIALOG_LOADING_ID:
			dialog = createLoadingDialog(null);
			break;
		default:
			dialog = super.onCreateDialog(id);
		}
		return dialog;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_favorite_list, menu);
		MenuItem menuSource = menu.findItem(R.id.mmiSource);
		SubMenu submenuSource = menuSource.getSubMenu();
		for (int pluginId : Plugins.getPluginIds()) {
			submenuSource.add(R.id.mmgSourceGroup, pluginId, pluginId - 1000, Plugins.getPlugin(pluginId)
					.getDisplayname());
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getGroupId() == R.id.mmgSourceGroup) {
			onSourceSelected(item.getItemId());
			return true;
		}

		switch (item.getItemId()) {
		case R.id.mmiRefresh:
			refresh();
			return true;
		case R.id.mmiPreferences:
			startActivity(new Intent(this, ActivityPreference.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBackPressed() {
		showDialog(DIALOG_CLOSE_ID);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Manga manga = (Manga) mListAdapter.getItem(position);
		ActivityChapterList.IntentHandler.startActivityMangaList(this, manga);
	}

	@Override
	public void onClick(View view) {
		// For favorite CheckBox in Manga item
		if (view instanceof CheckBox) {
			final CheckBox button = (CheckBox) view;
			final Manga manga = (Manga) button.getTag();
			if (button.isChecked()) {
				modifiedFavorite(manga, button.isChecked());
			} else {
				button.toggle();
				DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == DialogInterface.BUTTON_POSITIVE) {
							button.toggle();
							modifiedFavorite(manga, button.isChecked());
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

	private void onSourceSelected(int siteId) {
		Site site = Site.get(siteId);
		ActivityRemeberPwd.IntentHandler.startActivityRemeberPwd(this, siteId);


	}

	private Dialog createExitConfirmDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.dialog_confirm_to_exit).setCancelable(false)
				.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						mExit = true;
						finish();
					}
				}).setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		return builder.create();
	}

	private void modifiedFavorite(Manga manga, boolean add) {
		if (add) {
			AppUtils.logI(this, "Add to Favorite.");
			try {
				long id = mDB.insertManga(manga);
				AppUtils.logW(this, "Add as ID " + id + ".");
			} catch (SQLException e) {
				AppUtils.logE(this, e.getMessage());
			}
		} else {
			AppUtils.logI(this, "Remove from Favorite.");
			try {
				int deleted;
				if ((deleted = mDB.deleteManga(manga)) == 0) {
					AppUtils.logE(this, "Remove none.");
				} else {
					AppUtils.logW(this, "Remove " + (deleted / 1000) + " mangas " + (deleted % 1000) + " chapters.");
				}
			} catch (SQLException e) {
				AppUtils.logE(this, e.getMessage());
			}
		}
		mListAdapter.notifyDataSetInvalidated();
	}

	private void refresh() {
		if (mUpdater == null) {
			mUpdater = new Updater();
		}
		if (!mUpdater.isUpdating()) {
			mUpdater.queue(mDB.getAllMangas());
			mUpdater.update();
		}
	}

	private void showUpdateBar() {
		mHideUpdateBarHandler.removeCallbacks(mHideUpdateBarRunnable);
		mtvUpdating.setText(String.format(getString(R.string.ui_updating_manga), ""));
		mtvUpdated.setText("0 / 0");
		mpbUpdate.setProgress(0);
		mvgUpdateBar.setVisibility(View.VISIBLE);
	}

	private void hideUpdateBar() {
		hideUpdateBar(false);
	}

	private void setUpdateBarProgress(Manga manga, int updated, int total) {
		if (manga != null) {
			mtvUpdating.setText(String.format(getString(R.string.ui_updating_manga), manga.displayname));
		} else {
			mtvUpdating.setText(R.string.ui_update_manga_completed);
		}
		mtvUpdated.setText(String.format("%d / %d", updated, total));
		mpbUpdate.setProgress(100 * updated / total);
	}

	private void hideUpdateBar(boolean now) {
		mHideUpdateBarHandler.removeCallbacks(mHideUpdateBarRunnable);
		if (now) {
			mvgUpdateBar.setVisibility(View.GONE);
		} else {
			mHideUpdateBarHandler.postDelayed(mHideUpdateBarRunnable, App.TIME_AUTO_HIDE);
		}
	}

	private void checkNewVersion() {
		if (mChecker == null) {
			mChecker = new VersionChecker();
		}
		//mChecker.check();
	}

	private void updateNewVersion(String newVersion) {
		mChecker = null;
		if (TextUtils.isEmpty(newVersion)) {
			mNewVersionPanel.setVisibility(View.GONE);
		} else {
			mNewVersion.setText(String.format(getString(R.string.ui_has_new_version_format), App.NAME, newVersion));
			mNewVersionPanel.setVisibility(View.VISIBLE);
		}
	}

	private void downloadNewVersion() {
		String url = String.format(App.URL_LATEST_VERSION_DOWNLOAD, mVersionCode);
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		i = Intent.createChooser(i, null);
		startActivity(i);
	}

}
