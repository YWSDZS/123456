package org.falconia.mangaproxy;

import org.apache.http.util.EncodingUtils;
import org.falconia.mangaproxy.plugin.Plugins;
import org.falconia.mangaproxy.task.DownloadTask;
import org.falconia.mangaproxy.task.OnDownloadListener;
import org.falconia.mangaproxy.task.OnSourceProcessListener;
import org.falconia.mangaproxy.task.SourceProcessTask;
import org.falconia.mangaproxy.ui.BaseHeadersAdapter;
import org.falconia.mangaproxy.ui.PinnedHeaderListView;
import org.falconia.mangaproxy.utils.FormatUtils;
import org.falconia.mangaproxyex.R;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public abstract class ActivityBase extends ListActivity implements OnFocusChangeListener, OnTouchListener,
		OnItemClickListener, OnItemLongClickListener, OnSourceProcessListener {

	protected final class SourceDownloader implements OnDownloadListener, OnCancelListener {

		private final String mCharset;

		private boolean mFinishOnCancel;
		private String mUrl;
		private DownloadTask mDownloader;
		private ProgressDialog mDownloadDialog;
		private String mMessage;

		public SourceDownloader() {
			mCharset = Plugins.getPlugin(getSiteId()).getCharset();
		}

		@Override
		public void onPreDownload() {
			AppUtils.logV(this, "onPreDownload()");
			setProgressBarIndeterminateVisibility(true);

			showDialog(DIALOG_DOWNLOAD_ID);
		}

		@Override
		public void onPostDownload(byte[] result) {
			AppUtils.logV(this, "onPostDownload()");


			try {
				dismissDialog(DIALOG_DOWNLOAD_ID);
			} catch (Exception e) {
				AppUtils.logE(this, "dismissDialog(DIALOG_DOWNLOAD_ID)");
			}
			setProgressBarIndeterminateVisibility(false);

			if (result == null || result.length == 0) {
				AppUtils.logE(this, "Downloaded empty source.");
				setNoItemsMessage(String.format(getString(R.string.ui_error_on_download), getSiteName()));
				return;
			}

			String source = EncodingUtils.getString(result, mCharset);
			// AppUtils.logV(this, source);

			startProcessSource(source, mUrl);
		}

		@Override
		public void onDownloadProgressUpdate(int value, int total) {
			mDownloadDialog.setMessage(String.format(mMessage, FormatUtils.getFileSizeBtoKB(value)));
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			AppUtils.logD(this, "onCancel() @DownloadDialog");
			cancelDownload();
			if (mFinishOnCancel && !mProcessed) {
				finish();
			}
		}

		public ProgressDialog createDownloadDialog(CharSequence what) {
			CharSequence title, message;
			if (TextUtils.isEmpty(what)) {
				title = null;
				message = getString(R.string.dialog_download_message);
			} else {
				title = getString(R.string.dialog_download_title);
				message = String.format(getString(R.string.dialog_download_message_format), what, "0.000KB");
			}
			mMessage = String.format(getString(R.string.dialog_download_message_format), what, "%s");
			ProgressDialog dialog = createProgressDialog(title, message, true);
			dialog.setOnCancelListener(this);
			mDownloadDialog = dialog;
			return dialog;
		}

		public ProgressDialog createDownloadDialog(int whatResId) {
			return createDownloadDialog(getString(whatResId));
		}

		public void download(String url) {
			download(url, true);
		}

		public void download(String url, boolean finishOnCancel) {
			mFinishOnCancel = finishOnCancel;
			mUrl = url;
			mDownloader = new DownloadTask(this,"",Plugins.getPlugin(getSiteId()).getCookies());
			mDownloader.execute(url);
		}

		public void cancelDownload() {
			if (mDownloader != null && mDownloader.getStatus() == AsyncTask.Status.RUNNING) {
				AppUtils.logD(this, "Cancel DownloadTask.");
				mDownloader.cancelDownload();
			}
		}
	}

	protected static final String BUNDLE_KEY_IS_PROCESSED = "BUNDLE_KEY_IS_PROCESSED";

	protected static final int DIALOG_CLOSE_ID = -2;
	protected static final int DIALOG_LOADING_ID = 0;
	protected static final int DIALOG_DOWNLOAD_ID = 1;
	protected static final int DIALOG_PROCESS_ID = 2;

	protected SourceDownloader mSourceDownloader;
	protected SourceProcessTask mSourceProcessTask;
	protected boolean mShowProcessDialog = true;
	protected boolean mProcessed = false;

	protected BaseAdapter mListAdapter;

	abstract int getSiteId();

	abstract String getSiteName();

	abstract String getSiteDisplayname();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AppUtils.logV(this, "onCreate()");

		mProcessed = getProcessed(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	}

	@Override
	protected void onStart() {
		super.onStart();
		AppUtils.logV(this, "onStart()");
	}

	@Override
	protected void onResume() {
		System.gc();

		super.onResume();
		AppUtils.logV(this, "onResume()");
	}

	@Override
	protected void onPause() {
		super.onPause();
		AppUtils.logV(this, "onPause()");
	}

	@Override
	protected void onStop() {
		super.onStop();
		AppUtils.logV(this, "onStop()");
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		AppUtils.logV(this, "onRestart()");
	}

	@Override
	protected void onDestroy() {
		System.gc();

		super.onDestroy();
		AppUtils.logV(this, "onDestroy()");

		stopTask();

		final ListView list = getListView();
		list.setOnScrollListener(null);
		list.setOnFocusChangeListener(null);
		list.setOnTouchListener(null);
		list.setOnItemClickListener(null);
		list.setOnItemLongClickListener(null);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		AppUtils.logV(this, "onSaveInstanceState()");

		stopTask();

		outState.putBoolean(BUNDLE_KEY_IS_PROCESSED, mProcessed);
	}

	protected boolean getProcessed(Bundle savedInstanceState) {
		boolean processed = false;
		if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_KEY_IS_PROCESSED)) {
			processed = savedInstanceState.getBoolean(BUNDLE_KEY_IS_PROCESSED);
		}
		return processed;
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		AppUtils.logV(this, "onRestoreInstanceState()");
	}

	/**
	 * Dismisses the soft keyboard when the list takes focus.
	 */
	@Override
	public void onFocusChange(View view, boolean hasFocus) {
		if (view == getListView() && hasFocus) {
			hideSoftKeyboard();
		}
	}

	/**
	 * Dismisses the soft keyboard when the list takes focus.
	 */
	@Override
	public boolean onTouch(View view, MotionEvent event) {
		if (view == getListView()) {
			hideSoftKeyboard();
		}
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub

		return false;
	}

	@Override
	public int onSourceProcess(String source, String url) {
		AppUtils.logV(this, "onSourceProcess()");
		return 0;
	}

	@Override
	public void onPreSourceProcess() {
		AppUtils.logV(this, "onPreSourceProcess()");

		if (mSourceProcessTask == null) {
			AppUtils.logE(this, "ProcessDataTask is not initialized.");
			return;
		}

		mProcessed = false;

		if (mShowProcessDialog) {
			showDialog(DIALOG_PROCESS_ID);
		}
		setProgressBarIndeterminateVisibility(true);
	}

	@Override
	public void onPostSourceProcess(int size) {
		AppUtils.logV(this, "onPostSourceProcess()");

		mSourceProcessTask = null;

		if (mShowProcessDialog) {
			dismissDialog(DIALOG_PROCESS_ID);
		}
		setProgressBarIndeterminateVisibility(false);

		if (size <= 0) {
			setNoItemsMessage(String.format(getString(R.string.ui_error_on_process), getSiteName()));
			return;
		}

		mProcessed = true;
	}

	protected void startProcessSource(String source, String url) {
		mSourceProcessTask = new SourceProcessTask(ActivityBase.this);
		mSourceProcessTask.execute(source, url);
	}

	protected void stopTask() {
		if (mSourceDownloader != null) {
			mSourceDownloader.cancelDownload();
		}
		if (mSourceProcessTask != null) {
			mSourceProcessTask.cancel(true);
		}
		removeDialog(DIALOG_DOWNLOAD_ID);
		removeDialog(DIALOG_PROCESS_ID);
	}

	protected void setCustomTitle(String string) {
		String str = getString(R.string.app_name);
		if (!TextUtils.isEmpty(string)) {
			str = getSiteDisplayname() + " - " + string;
		}
		setTitle(str);
	}

	protected void setNoItemsMessage(String msg) {
		((TextView) findViewById(R.id.mtvNoItems)).setText(msg);
	}

	protected void setNoItemsMessage(int resId) {
		setNoItemsMessage(getString(resId));
	}

	protected void setupListView(BaseAdapter adapter) {
		mListAdapter = adapter;

		setListAdapter(mListAdapter);

		final ListView list = getListView();
		final LayoutInflater inflater = getLayoutInflater();

		// mHighlightingAnimation = new NameHighlightingAnimation(list,
		// TEXT_HIGHLIGHTING_ANIMATION_DURATION);

		// Tell list view to not show dividers. We'll do it ourself so that we
		// can *not* show
		// them when an A-Z headers is visible.
		// list.setDividerHeight(0);
		// list.setOnCreateContextMenuListener(this);

		list.setEmptyView(findViewById(R.id.mvgEmpty));

		if (mListAdapter instanceof BaseHeadersAdapter) {
			if (list instanceof PinnedHeaderListView
					&& ((BaseHeadersAdapter) mListAdapter).getDisplaySectionHeadersEnabled()) {
				// mPinnedHeaderBackgroundColor =
				// getResources().getColor(R.color.pinned_header_background);
				PinnedHeaderListView pinnedHeaderList = (PinnedHeaderListView) list;
				View pinnedHeader = inflater.inflate(R.layout.list_section, list, false);
				pinnedHeaderList.setPinnedHeaderView(pinnedHeader);
			}

			list.setOnScrollListener((BaseHeadersAdapter) mListAdapter);
		}

		list.setOnFocusChangeListener(this);
		list.setOnTouchListener(this);
		list.setOnItemClickListener(this);
		list.setOnItemLongClickListener(this);

		// We manually save/restore the listview state
		// list.setSaveEnabled(false);
	}

	protected ProgressDialog createProgressDialog(CharSequence title, CharSequence message, boolean cancelable) {
		ProgressDialog dialog = new ProgressDialog(this);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setCancelable(cancelable);
		return dialog;
	}

	protected ProgressDialog createProgressDialog(CharSequence title, CharSequence message) {
		return createProgressDialog(title, message, false);
	}

	protected ProgressDialog createLoadingDialog(CharSequence what) {
		CharSequence title, message;
		if (TextUtils.isEmpty(what)) {
			title = null;
			message = getString(R.string.dialog_loading_message);
		} else {
			title = getString(R.string.dialog_loading_title);
			message = String.format(getString(R.string.dialog_loading_message_format), what);
		}
		return createProgressDialog(title, message, false);
	}

	protected ProgressDialog createLoadingDialog(int whatResId) {
		return createLoadingDialog(getString(whatResId));
	}

	protected ProgressDialog createLoadingDialog() {
		return createLoadingDialog(null);
	}

	protected ProgressDialog createProcessDialog(CharSequence what) {
		CharSequence title, message;
		if (TextUtils.isEmpty(what)) {
			title = null;
			message = getString(R.string.dialog_process_message);
		} else {
			title = getString(R.string.dialog_process_title);
			message = String.format(getString(R.string.dialog_process_message_format), what);
		}
		return createProgressDialog(title, message, false);
	}

	protected ProgressDialog createProcessDialog(int whatResId) {
		return createProcessDialog(getString(whatResId));
	}

	private void hideSoftKeyboard() {
		// Hide soft keyboard, if visible
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(getListView().getWindowToken(), 0);
	}

}
