package org.falconia.mangaproxy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.zip.CRC32;

import org.apache.http.util.EncodingUtils;
import org.falconia.mangaproxy.App.ZoomMode;
import org.falconia.mangaproxy.data.Chapter;
import org.falconia.mangaproxy.data.ChapterList;
import org.falconia.mangaproxy.data.Manga;
import org.falconia.mangaproxy.plugin.Plugins;
import org.falconia.mangaproxy.task.DownloadTask;
import org.falconia.mangaproxy.task.OnDownloadListener;
import org.falconia.mangaproxy.ui.ZoomViewOnTouchListener;
import org.falconia.mangaproxy.utils.FormatUtils;
import org.falconia.mangaproxy.utils.HttpUtils;
import org.falconia.mangaproxyex.R;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.sonyericsson.zoom.DynamicZoomControl;
import com.sonyericsson.zoom.ImageZoomView;
import com.sonyericsson.zoom.ZoomState.AlignX;
import com.sonyericsson.zoom.ZoomState.AlignY;

public final class ActivityChapter extends Activity implements OnClickListener, OnCancelListener,
		OnSharedPreferenceChangeListener {

	public final static class IntentHandler {

		private static final String BUNDLE_KEY_MANGA_DATA = "BUNDLE_KEY_MANGA_DATA";
		private static final String BUNDLE_KEY_CHAPTER_DATA = "BUNDLE_KEY_CHAPTER_DATA";
		private static final String BUNDLE_KEY_PAGE_URLS_DATA = "BUNDLE_KEY_PAGE_URLS_DATA";
		private static final String BUNDLE_KEY_CHAPTERLIST_DATA = "BUNDLE_KEY_CHAPTERLIST_DATA";
		private static final String BUNDLE_KEY_CURRENT_CHAPTER_IDX_DATA = "BUNDLE_KEY_CURRENT_CHAPTER_IDX_DATA";
		

		private static Intent getIntent(Context context, Manga manga, Chapter chapter, String[] pageUrls) {
			Bundle bundle = new Bundle();
			bundle.putSerializable(BUNDLE_KEY_MANGA_DATA, manga);
			bundle.putSerializable(BUNDLE_KEY_CHAPTER_DATA, chapter);
			bundle.putStringArray(BUNDLE_KEY_PAGE_URLS_DATA, pageUrls);
			Intent i = new Intent(context, ActivityChapter.class);
			i.putExtras(bundle);
			return i;
		}
		
		private static Intent getIntent(Context context, Manga manga, Chapter chapter, String[] pageUrls, ChapterList chapterList, int currentChapterIdx) {
			Bundle bundle = new Bundle();
			bundle.putSerializable(BUNDLE_KEY_MANGA_DATA, manga);
			bundle.putSerializable(BUNDLE_KEY_CHAPTER_DATA, chapter);
			bundle.putSerializable(BUNDLE_KEY_CHAPTERLIST_DATA, chapterList);
			bundle.putStringArray(BUNDLE_KEY_PAGE_URLS_DATA, pageUrls);
			bundle.putInt(BUNDLE_KEY_CURRENT_CHAPTER_IDX_DATA, currentChapterIdx);
			Intent i = new Intent(context, ActivityChapter.class);
			i.putExtras(bundle);
			return i;
		}
		

		protected static Manga getManga(ActivityChapter activity) {
			return (Manga) activity.getIntent().getExtras().getSerializable(BUNDLE_KEY_MANGA_DATA);
		}

		protected static Chapter getChapter(ActivityChapter activity) {
			return (Chapter) activity.getIntent().getExtras().getSerializable(BUNDLE_KEY_CHAPTER_DATA);
		}

		protected static String[] getPageUrls(ActivityChapter activity) {
			return activity.getIntent().getExtras().getStringArray(BUNDLE_KEY_PAGE_URLS_DATA);
		}
		
		protected static ChapterList getChapterList(ActivityChapter activity) {
			return (ChapterList) activity.getIntent().getExtras().getSerializable(BUNDLE_KEY_CHAPTERLIST_DATA);
		}
		
		protected static int getCurrentChapterIdx(ActivityChapter activity) {
			return  activity.getIntent().getExtras().getInt(BUNDLE_KEY_CURRENT_CHAPTER_IDX_DATA);
		}

		public static void startActivityChapter(Context context, Manga manga, Chapter chapter) {
			context.startActivity(getIntent(context, manga, chapter, null));
			AppCache.wipeCacheForImages(TYPE);
		}
		
		public static void startActivityChapter(Context context, Manga manga, Chapter chapter, ChapterList chapterList, int currentChapterIdx) {
			context.startActivity(getIntent(context, manga, chapter, null, chapterList,  currentChapterIdx));
			AppCache.wipeCacheForImages(TYPE);
		}

	}

	private final static class Configuration {
		private boolean mProcessed;

		private boolean mIsFavorite;

		private String[] mPageUrls;
		private HashMap<Integer, Page> mPages;
		private int mPageIndexMax;
		private int mPageIndexCurrent;
		private int mPageIndexLoading;

		private CharSequence mtvDebugText;
		private int msvScrollerVisibility;

		private int mvgTitleBarVisibility;
	}

	private final class PageViewOnTouchListener extends ZoomViewOnTouchListener {

		public PageViewOnTouchListener() {
			super(getApplicationContext());
		}

		@Override
		public boolean onSingleTap() {
			if (mvgTitleBar.isShown()) {
				hideTitleBar();
			} else {
				showTitleBar();
			}
			return true;
		}

		@Override
		public void onNextPage() {
			changePage(true);
		}

		@Override
		public void onPrevPage() {
			changePage(false);
		}
	}

	private final class SourceDownloader implements OnDownloadListener {

		protected static final int MODE_CHAPTER = 0;
		protected static final int MODE_IMG_SERVERS = 1;

		private final DownloadTask mDownloader;

		private final int mMode;
		private final String mCharset;

		private String mUrl;

		public SourceDownloader(int mode) {
			mDownloader = new DownloadTask(this,"",Plugins.getPlugin(getSiteId()).getCookies());
			mMode = mode;
			mCharset = Plugins.getPlugin(getSiteId()).getCharset();
		}

		@Override
		public void onPreDownload() {
			AppUtils.logV(this, "onPreDownload()");

			switch (mMode) {
			case MODE_CHAPTER:
				break;
			case MODE_IMG_SERVERS:
				mLoadingDialog.setMessage(String.format(getString(R.string.dialog_loading_imgsvrs_message_format),
						"0.000KB"));
				break;
			}

			showDialog(DIALOG_LOADING_ID);
		}

		@Override
		public void onPostDownload(byte[] result) {
			AppUtils.logV(this, "onPostDownload()");

			try {
				dismissDialog(DIALOG_LOADING_ID);
			} catch (Exception e) {
				AppUtils.logE(this, "dismissDialog(DIALOG_LOADING_ID)");
			}

			if (result == null || result.length == 0) {
				AppUtils.logE(this, "Downloaded empty source.");
				setMessage(String.format(getString(R.string.ui_error_on_download), getSiteName()));
				return;
			}

			String source = EncodingUtils.getString(result, mCharset);

			switch (mMode) {
			case MODE_CHAPTER:
				processChapterSource(source, mUrl);
				break;
			case MODE_IMG_SERVERS:
				// Debug
				printDebug(mUrl, "Caching");

				AppCache.writeCacheForData(source, mUrl);
				processImgServersSource(source, mUrl);
				break;
			}
		}

		@Override
		public void onDownloadProgressUpdate(int value, int total) {
			if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
				String message = null;
				String filesize = FormatUtils.getFileSizeBtoKB(value);

				switch (mMode) {
				case MODE_CHAPTER:
					message = String.format(getString(R.string.dialog_loading_chapter_message_format), filesize);
					break;
				case MODE_IMG_SERVERS:
					message = String.format(getString(R.string.dialog_loading_imgsvrs_message_format), filesize);
					break;
				}

				if (message != null) {
					mLoadingDialog.setMessage(message);
				}
			}
		}

		public void download(String url) {
			mUrl = url;
			mDownloader.execute(url);
		}

		public void cancelDownload() {
			if (mDownloader != null && mDownloader.getStatus() == AsyncTask.Status.RUNNING) {
				AppUtils.logD(this, "Cancel DownloadTask.");
				mDownloader.cancelDownload();
			}
		}
	}

	private final class Page implements OnDownloadListener {

		private final String mCharset;

		private DownloadTask mDownloader;

		private final int mPageIndex;
		private final String mUrl;
		private final boolean mIsRedirect;
		private String mUrlRedirected;

		private int mRetriedTimes;

		private boolean mIsDownloaded;
		private boolean mIsDownloading;
		private boolean mIsCancelled;

		private Bitmap mBitmap;

		public Page(int pageIndex, String url, boolean isRedirect) {
			mCharset = mChapter.getSiteCharset();

			mPageIndex = pageIndex;
			mUrl = url;
			mIsRedirect = isRedirect;

			mRetriedTimes = 0;

			mIsDownloaded = false;
			mIsDownloading = false;
			mIsCancelled = false;

			checkCache();
		}

		public Page(Page page) {
			this(page.mPageIndex, page.mUrl, page.mIsRedirect);
		}

		@Override
		public void onPreDownload() {
			AppUtils.logV(this, "onPreDownload()");
			mIsDownloading = true;

			// Debug
			printDebug(mUrl, "Downloading");

			if (mPageIndex == mPageIndexLoading) {
				showStatusBar();
			}
		}

		@Override
		public void onPostDownload(byte[] result) {
			AppUtils.logV(this, "onPostDownload()");

			if (mIsCancelled) {
				AppUtils.logE(this, "Downloaded cancelled.");
				return;
			}

			if (result == null || result.length == 0) {
				AppUtils.logE(this, "Downloaded empty source.");
				// setMessage(String.format(getString(R.string.ui_error_on_download),
				// getSiteName()));

				// TODO Retry to downlaod
				if (mRetriedTimes < App.MAX_RETRY_DOWNLOAD_IMG) {
					mRetriedTimes++;
					AppUtils.logI(this, String.format("Retry %d times to download image: %s", mRetriedTimes, mUrl));
					mIsDownloading = false;
					download();
				} else {
					mRetriedTimes = 0;
					AppUtils.popupMessage(ActivityChapter.this,
							String.format(getString(R.string.popup_fail_to_download_page), mPageIndex));
					mDownloader = null;
					mIsDownloading = false;
					notifyPageDownloaded(this);
				}
				return;
			}

			if (checkDummyPic(result)) {
				AppUtils.logE(this, "Dummy picture: " + mUrl);
			}

			if (!AppCache.writeCacheForImage(result, mUrl, TYPE)) {
				AppUtils.popupMessage(ActivityChapter.this,
						String.format(getString(R.string.popup_fail_to_cache_page), mPageIndex));

				mBitmap = BitmapFactory.decodeByteArray(result, 0, result.length);
			}

			mDownloader = null;
			mRetriedTimes = 0;
			mIsDownloaded = true;
			mIsDownloading = false;

			// Debug
			printDebug(mUrl, "Downloaded");

			// System.gc();

			notifyPageDownloaded(this);
		}

		@Override
		public void onDownloadProgressUpdate(int value, int total) {
			if (mPageIndex == mPageIndexLoading) {
				if (value == 0 || mpbDownload.getMax() != total) {
					mpbDownload.setMax(total);
				}
				mpbDownload.setProgress(value);
				mtvDownloaded.setText(String.format("%s / %s", FormatUtils.getFileSizeBtoKB(value),
						FormatUtils.getFileSizeBtoKB(total)));
			}
		}

		public boolean checkCache() {
			boolean cached = AppCache.checkCacheForImage(mUrl, TYPE);
			// this.mIsDownloaded = this.mIsDownloaded || cached;
			return cached;
		}

		public boolean isDownloaded() {
			if (mIsDownloading) {
				return false;
			}
			if (mIsDownloaded) {
				return true;
			}
			if (mBitmap != null) {
				return true;
			}
			return checkCache();
		}

		public void download(boolean refresh) {
			if (refresh) {
				AppUtils.logI(this, String.format("Force refresh image: %s", mUrl));
				cancelDownload();
				AppCache.wipeCacheForImage(mUrl, TYPE);
			} else {
				if (mIsDownloading) {
					if (mPageIndex == mPageIndexLoading) {
						showStatusBar();
					}
					return;
				}
				if (isDownloaded()) {
					// Debug
					printDebug(mUrl, "Cached");

					notifyPageDownloaded(this);
					return;
				}
			}

			mIsCancelled = false;

			if (mIsRedirect) {
				if (!mIsDownloaded || TextUtils.isEmpty(mUrlRedirected)) {
					AppUtils.logI(this, String.format("Download redirect URL: %s", mUrl));

					OnDownloadListener listener = new OnDownloadListener() {
						@Override
						public void onPreDownload() {
						}

						@Override
						public void onPostDownload(byte[] result) {

							if (result == null || result.length == 0) {
								AppUtils.logE("Page.OnDownloadListener", "Downloaded empty source for PageRedirectUrl.");
								Page.this.onPostDownload(result);
								return;
							}

							String source = EncodingUtils.getString(result, mCharset);
							String url = mChapter.getPageRedirectUrl(source, mUrl);
							if (TextUtils.isEmpty(url)) {
								AppUtils.logE("Page.OnDownloadListener", "Fail to get PageRedirectUrl.");
								Page.this.onPostDownload(null);
								return;
							} else {
								mUrlRedirected = url;
							}

							AppUtils.logI(this, String.format("Download image: %s", mUrlRedirected));

							mDownloader = new DownloadTask(Page.this, mChapter.getUrl(),Plugins.getPlugin(getSiteId()).getCookies());
							mDownloader.execute(mUrlRedirected);
						}

						@Override
						public void onDownloadProgressUpdate(int value, int total) {
						}
					};

					mDownloader = new DownloadTask(listener, mChapter.getUrl(),Plugins.getPlugin(getSiteId()).getCookies());
					mDownloader.execute(mUrl);

					return;
				}

				AppUtils.logI(this, String.format("Download image: %s", mUrlRedirected));

				mDownloader = new DownloadTask(this, mChapter.getUrl(),Plugins.getPlugin(getSiteId()).getCookies());
				mDownloader.execute(mUrlRedirected);
			} else {
				AppUtils.logI(this, String.format("Download image: %s", mUrl));

				mDownloader = new DownloadTask(this, mChapter.getUrl(),Plugins.getPlugin(getSiteId()).getCookies());
				mDownloader.execute(mUrl);
			}
		}

		public void download() {
			download(false);
		}

		public synchronized void cancelDownload() {
			if (mDownloader != null && mDownloader.getStatus() == AsyncTask.Status.RUNNING) {
				AppUtils.logD(this, "Cancel DownloadTask.");
				mDownloader.cancelDownload();
			}
			mIsCancelled = true;
			mDownloader = null;
			mUrlRedirected = null;
			mRetriedTimes = 0;
			mIsDownloaded = false;
			mIsDownloading = false;
			recycle();
		}

		public void recycle() {
			if (mBitmap != null) {
				mBitmap.recycle();
				mBitmap = null;
			}
		}

		public Bitmap getBitmap() {
			if (mBitmap != null) {
				return mBitmap;
			}

			// Debug
			printDebug(mUrl, "Loading");

			return AppCache.readCacheForImage(mUrl, TYPE);
		}

		private void showStatusBar() {
			mpbDownload.setProgress(0);
			mtvDownloaded.setText("");
			if (mRetriedTimes > 0) {
				mtvDownloading.setText(String.format(getString(R.string.ui_downloading_page_retry), mPageIndexLoading,
						mRetriedTimes));
			} else {
				mtvDownloading.setText(String.format(getString(R.string.ui_downloading_page), mPageIndexLoading));
			}
			mvgStatusBar.setVisibility(View.VISIBLE);
		}
	}

	public enum ChangePageMode {
		NONE, NEXT, PREV
	}

	private static final int DIALOG_LOADING_ID = 0;

	private static final HashSet<String> DUMMY_PIC_CRC32 = new HashSet<String>();

	private static final String TYPE = "page";

	private SourceDownloader mSourceDownloader;

	private Manga mManga;
	private Chapter mChapter;
	private int mCurrentChapterIdx;
	private ChapterList mChapterList;

	private Bitmap mBitmap;

	private boolean mProcessed;
	private boolean mDead;

	private ChangePageMode mChangePageMode;

	private String[] mPageUrls;
	private HashMap<Integer, Page> mPages;
	// private int mPageIndexCurrent;
	private int mPageIndexLoading;
	private int mPreloadMaxPages;
	private LinkedList<Integer> mPreloadPageIndexQueue;

	private TextView mtvDebug;
	private ScrollView msvScroller;

	private ProgressDialog mLoadingDialog;

	private ZoomMode mZoomMode;
	private ImageZoomView mPageView;
	private DynamicZoomControl mZoomControl;
	private PageViewOnTouchListener mZoomListener;

	private LinearLayout mvgTitleBar;
	private TextView mtvTitle;
	private LinearLayout mvgStatusBar;
	private TextView mtvDownloading;
	private TextView mtvDownloaded;
	private ProgressBar mpbDownload;

	private MenuItem mmiZoomFitWidthOrHeight;
	private MenuItem mmiZoomFitWidthAutoSplit;
	private MenuItem mmiZoomFitWidth;
	private MenuItem mmiZoomFitHeight;
	private MenuItem mmiZoomFitScreen;

	private final Handler mHideScrollerHandler;
	private final Handler mHideTitleBarHandler;
	private final Runnable mHideScrollerRunnable;
	private final Runnable mHideTitleBarRunnable;

	public ActivityChapter() {

		mProcessed = false;
		mDead = false;
		mCurrentChapterIdx = -1;
		mChangePageMode = ChangePageMode.NONE;
		// mPageIndexCurrent = 0;
		mPageIndexLoading = 0;
		mZoomMode = ZoomMode.FIT_WIDTH_OR_HEIGHT;
		mPreloadPageIndexQueue = new LinkedList<Integer>();
		mHideScrollerHandler = new Handler();
		mHideTitleBarHandler = new Handler();
		mHideScrollerRunnable = new Runnable() {
			@Override
			public void run() {
				if (msvScroller != null) {
					msvScroller.setVisibility(View.GONE);
					mHideScrollerHandler.removeCallbacks(this);
				}
			}
		};
		mHideTitleBarRunnable = new Runnable() {
			@Override
			public void run() {
				if (mvgTitleBar != null) {
					mvgTitleBar.setVisibility(View.GONE);
					mHideTitleBarHandler.removeCallbacks(this);
				}
			}
		};
	}

	public int getSiteId() {
		return mManga.siteId;
	}

	public String getSiteName() {
		return mManga.getSiteName();
	}

	protected void loadNextChapter(){
		
		AppUtils.logV(this, "onLoadNextChapter()");
		if(this.mCurrentChapterIdx +1 >= this.mChapterList.size())
		{
		   return;
		}
		mCurrentChapterIdx += 1;
		this.mChapter = this.mChapterList.getAt(this.mCurrentChapterIdx);
		if (mChapter == null || mManga == null) {
			finish();
			return;
		}
		mManga.lastReadChapterId = mChapter.chapterId;
		mChapter.manga = mManga;
		mProcessed = false;
		final Configuration conf = (Configuration) getLastNonConfigurationInstance();
		if (conf != null) {
			mProcessed = conf.mProcessed;
			mChapter.isFavorite = conf.mIsFavorite;
			mChapter.pageIndexMax = conf.mPageIndexMax;
			mChapter.pageIndexLastRead = conf.mPageIndexCurrent;
			mPageIndexLoading = conf.mPageIndexLoading;
		}
		if (!mProcessed) {
			loadChapter();
		} else if ((mPageUrls = IntentHandler.getPageUrls(this)) != null) {
			mProcessed = true;
			processPageUrls();
		} else {
			if(conf != null)
			{
				mPageUrls = conf.mPageUrls;
				mPages = new HashMap<Integer, Page>();
				for (int key : conf.mPages.keySet()) {
					mPages.put(key, new Page(conf.mPages.get(key)));
				}


				if (mChapter.pageIndexLastRead != mPageIndexLoading) {
					AppUtils.logW(this, "mPageIndexCurrent != mPageIndexLoading");
				} else {
					AppUtils.logW(this, "mPageIndexCurrent == mPageIndexLoading");
				}

			}
			changePage(mPageIndexLoading);
	   }
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AppUtils.logV(this, "onCreate()");

		mChapter = IntentHandler.getChapter(this);
		mManga = IntentHandler.getManga(this);
		mPageUrls = IntentHandler.getPageUrls(this);
		mCurrentChapterIdx = IntentHandler.getCurrentChapterIdx(this);
	    mChapterList = IntentHandler.getChapterList(this);
		if (mChapter == null || mManga == null) {
			finish();
			return;
		}
		mManga.lastReadChapterId = mChapter.chapterId;
		mChapter.manga = mManga;

		getWindow().addFlags(LayoutParams.FLAG_LAYOUT_NO_LIMITS);
		getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(App.getPageOrientation());
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_chapter);
		// setTitle(getCustomTitle());

		final Configuration conf = (Configuration) getLastNonConfigurationInstance();
		if (conf != null) {
			mProcessed = conf.mProcessed;
			mChapter.isFavorite = conf.mIsFavorite;
			mChapter.pageIndexMax = conf.mPageIndexMax;
			mChapter.pageIndexLastRead = conf.mPageIndexCurrent;
			mPageIndexLoading = conf.mPageIndexLoading;
		}
		mZoomMode = App.getPageZoomMode();
		mPreloadMaxPages = App.getPreloadPages();
		App.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		// Debug controls
		mtvDebug = (TextView) findViewById(R.id.mtvDebug);
		msvScroller = (ScrollView) findViewById(R.id.msvScroller);
		msvScroller.setVisibility(View.GONE);
		// Buttons
		findViewById(R.id.mbtnNext).setOnClickListener(this);
		findViewById(R.id.mbtnPrev).setOnClickListener(this);
		// Title bar
		mvgTitleBar = (LinearLayout) findViewById(R.id.mvgTitleBar);
		mvgTitleBar.setVisibility(View.GONE);
		mtvTitle = (TextView) findViewById(R.id.mtvTitle);
		mtvTitle.setText(getCustomTitle());
		// Status bar
		mvgStatusBar = (LinearLayout) findViewById(R.id.mvgStatusBar);
		mvgStatusBar.setVisibility(View.GONE);
		mtvDownloading = (TextView) findViewById(R.id.mtvDownloading);
		mtvDownloading.setText(String.format(getString(R.string.ui_downloading_page), 0));
		mtvDownloaded = (TextView) findViewById(R.id.mtvDownloaded);
		mpbDownload = (ProgressBar) findViewById(R.id.mpbDownload);
		mpbDownload.setProgress(0);
		// Page image
		mZoomControl = new DynamicZoomControl();
		mZoomListener = new PageViewOnTouchListener();
		mZoomListener.setZoomControl(mZoomControl);
		mPageView = (ImageZoomView) findViewById(R.id.mivPage);
		mPageView.setZoomState(mZoomControl.getZoomState());
		mPageView.setOnTouchListener(mZoomListener);
		mZoomControl.setAspectQuotient(mPageView.getAspectQuotient());
		setupZoomState();

		if (App.DEBUG > 0) {
			msvScroller.setVisibility(View.VISIBLE);

			// Listener
			mtvDebug.setClickable(true);
			mtvDebug.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					msvScroller.setVisibility(View.GONE);
				}
			});
			mtvTitle.setClickable(true);
			mtvTitle.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// mtvTitle.setOnClickListener(null);
					// mtvTitle.setClickable(false);
					msvScroller.setVisibility(View.VISIBLE);
					msvScroller.fullScroll(View.FOCUS_DOWN);
				}
			});
		}

		if (!mProcessed) {
			loadChapter();
		} else if ((mPageUrls = IntentHandler.getPageUrls(this)) != null) {
			mProcessed = true;
			processPageUrls();
		} else {
			mPageUrls = conf.mPageUrls;
			mPages = new HashMap<Integer, Page>();
			for (int key : conf.mPages.keySet()) {
				mPages.put(key, new Page(conf.mPages.get(key)));
			}

			if (App.DEBUG > 0) {
				mtvDebug.setText(conf.mtvDebugText);
				msvScroller.post(new Runnable() {
					@Override
					public void run() {
						msvScroller.fullScroll(View.FOCUS_DOWN);
					}
				});
				msvScroller.setVisibility(conf.msvScrollerVisibility);
			}

			mvgTitleBar.setVisibility(conf.mvgTitleBarVisibility);

			// Set image
			mPageView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					AppUtils.logV(ActivityChapter.this, "onGlobalLayout()");
					mPageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					setImage(mBitmap);
				}
			});

			mHideScrollerHandler.postAtTime(mHideScrollerRunnable, 2000);
			mHideTitleBarHandler.postAtTime(mHideTitleBarRunnable, 2000);

			if (mChapter.pageIndexLastRead != mPageIndexLoading) {
				AppUtils.logW(this, "mPageIndexCurrent != mPageIndexLoading");
			} else {
				AppUtils.logW(this, "mPageIndexCurrent == mPageIndexLoading");
			}
			changePage(mPageIndexLoading);
		}
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
		super.onDestroy();
		AppUtils.logV(this, "onStop()");

		stopTask();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		AppUtils.logV(this, "onDestroy()");

		if (mBitmap != null) {
			mBitmap.recycle();
			mBitmap = null;
		}

		if (mPages != null) {
			for (int key : mPages.keySet()) {
				Page page = mPages.get(key);
				if (page != null) {
					page.recycle();
				}
			}
		}

		App.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		// Debug controls
		mHideScrollerHandler.removeCallbacks(mHideScrollerRunnable);
		mtvDebug.setOnClickListener(null);
		// Buttons
		findViewById(R.id.mbtnNext).setOnClickListener(null);
		findViewById(R.id.mbtnPrev).setOnClickListener(null);
		// Title bar
		mHideTitleBarHandler.removeCallbacks(mHideTitleBarRunnable);
		mtvTitle.setOnClickListener(null);
		// Page image
		mPageView.setOnTouchListener(null);
		mPageView.setImage(null);
		mZoomControl.getZoomState().deleteObservers();
		mZoomControl = null;
		mZoomListener = null;

		System.gc();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		AppUtils.logV(this, "onSaveInstanceState()");

		removeDialog(DIALOG_LOADING_ID);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		AppUtils.logV(this, "onRestoreInstanceState()");

		mpbDownload.setProgress(0);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		AppUtils.logV(this, "onRetainNonConfigurationInstance()");

		Configuration conf = new Configuration();

		conf.mProcessed = mProcessed;

		conf.mIsFavorite = mChapter.isFavorite;

		conf.mPageUrls = mPageUrls;
		conf.mPages = mPages;
		conf.mPageIndexMax = mChapter.pageIndexMax;
		conf.mPageIndexCurrent = mChapter.pageIndexLastRead;
		conf.mPageIndexLoading = mPageIndexLoading;

		conf.mtvDebugText = mtvDebug.getText();
		conf.msvScrollerVisibility = msvScroller.getVisibility();

		conf.mvgTitleBarVisibility = mvgTitleBar.getVisibility();

		return conf;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_LOADING_ID:
			mLoadingDialog = createLoadingDialog();
			return mLoadingDialog;
		default:
			dialog = super.onCreateDialog(id);
		}
		return dialog;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_chapter, menu);
		mmiZoomFitWidthOrHeight = menu.findItem(R.id.mmiZoomFitWidthOrHeight);
		mmiZoomFitWidthAutoSplit = menu.findItem(R.id.mmiZoomFitWidthAutoSplit);
		mmiZoomFitWidth = menu.findItem(R.id.mmiZoomFitWidth);
		mmiZoomFitHeight = menu.findItem(R.id.mmiZoomFitHeight);
		mmiZoomFitScreen = menu.findItem(R.id.mmiZoomFitScreen);
		switch (mZoomMode) {
		case FIT_WIDTH_OR_HEIGHT:
			mmiZoomFitWidthOrHeight.setChecked(true);
			break;
		case FIT_WIDTH_AUTO_SPLIT:
			mmiZoomFitWidthAutoSplit.setChecked(true);
			break;
		case FIT_WIDTH:
			mmiZoomFitWidth.setChecked(true);
			break;
		case FIT_HEIGHT:
			mmiZoomFitHeight.setChecked(true);
			break;
		case FIT_SCREEN:
			mmiZoomFitScreen.setChecked(true);
			break;
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mDead) {
			for (int i = 0; i < menu.size(); i++) {
				menu.getItem(i).setEnabled(false);
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDead) {
			return true;
		}

		if (item.getGroupId() == R.id.mmgZoomGroup) {
			switch (item.getItemId()) {
			case R.id.mmiZoomFitWidthOrHeight:
				mZoomMode = ZoomMode.FIT_WIDTH_OR_HEIGHT;
				break;
			case R.id.mmiZoomFitWidthAutoSplit:
				mZoomMode = ZoomMode.FIT_WIDTH_AUTO_SPLIT;
				break;
			case R.id.mmiZoomFitWidth:
				mZoomMode = ZoomMode.FIT_WIDTH;
				break;
			case R.id.mmiZoomFitHeight:
				mZoomMode = ZoomMode.FIT_HEIGHT;
				break;
			case R.id.mmiZoomFitScreen:
				mZoomMode = ZoomMode.FIT_SCREEN;
				break;
			}
			App.setPageZoomMode(mZoomMode);
		} else {
			switch (item.getItemId()) {
			case R.id.mmiRefresh:
				refreshPage();
				break;
			case R.id.mmiPreferences:
				startActivity(new Intent(this, ActivityPreference.class));
				break;
			default:
				return super.onOptionsItemSelected(item);
			}
		}
		return true;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		if (mSourceDownloader != null) {
			mSourceDownloader.cancelDownload();
		}
		finish();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.mbtnNext:
			changePage(true);
			break;
		case R.id.mbtnPrev:
			changePage(false);
			break;
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("iPageOrientation")) {
			setRequestedOrientation(App.getPageOrientation());
		} else if (key.equals("iPageZoomMode")) {
			mZoomMode = App.getPageZoomMode();

			if (mmiZoomFitWidthAutoSplit != null) {
				switch (mZoomMode) {
				case FIT_WIDTH_OR_HEIGHT:
					mmiZoomFitWidthOrHeight.setChecked(true);
					break;
				case FIT_WIDTH_AUTO_SPLIT:
					mmiZoomFitWidthAutoSplit.setChecked(true);
					break;
				case FIT_WIDTH:
					mmiZoomFitWidth.setChecked(true);
					break;
				case FIT_HEIGHT:
					mmiZoomFitHeight.setChecked(true);
					break;
				case FIT_SCREEN:
					mmiZoomFitScreen.setChecked(true);
					break;
				}
			}

			mZoomControl.stopFling();
			mZoomControl.getZoomState().setDefaultZoom(computeDefaultZoom(mZoomMode, mPageView));
			mZoomControl.getZoomState().notifyObservers();
			mZoomControl.startFling(0, 0);
		} else if (key.equals("iPreloadPages")) {
			mPreloadMaxPages = App.getPreloadPages();
		}
	}

	private void stopTask() {
		mtvDebug.setOnClickListener(null);
		mtvTitle.setOnClickListener(null);

		if (mSourceDownloader != null) {
			mSourceDownloader.cancelDownload();
		}

		if (mPages != null) {
			for (int key : mPages.keySet()) {
				Page page = mPages.get(key);
				if (page != null) {
					page.cancelDownload();
				}
			}
		}
	}

	private String getCustomTitle() {
		String title = String.format("%s - %s", mManga.displayname, mChapter.displayname);
		if (mChapter.pageIndexLastRead > 0) {
			title += String.format(" - " + getString(R.string.ui_pages_format), mChapter.pageIndexLastRead,
					mChapter.pageIndexMax);
		}
		return title;
	}

	private void setMessage(String msg) {
		((TextView) findViewById(R.id.mtvMessage)).setText(msg);
	}

	private ProgressDialog createLoadingDialog() {
		ProgressDialog dialog = new ProgressDialog(this);
		dialog.setTitle(mChapter.displayname);
		dialog.setMessage(String.format(getString(R.string.dialog_loading_chapter_message_format), "0.000KB"));
		dialog.setCancelable(true);
		dialog.setOnCancelListener(this);
		return dialog;
	}

	private void loadChapter() {
		String url = mChapter.getUrl();
		mSourceDownloader = new SourceDownloader(SourceDownloader.MODE_CHAPTER);
		mSourceDownloader.download(url);

		// Debug
		printDebug(url, "Downloading");
	}

	private void processChapterSource(String source, String url) {
		mPageUrls = mChapter.getPageUrls(source, url);

		if (mPageUrls == null) {
			setMessage(String.format(getString(R.string.ui_error_on_process), getSiteName()));
			mChapter.pageIndexMax = 0;
			return;
		}

		// Using Dynamic Image Server
		if (mManga.usingDynamicImgServer()) {
			String urlImgServers = mChapter.getDynamicImgServersUrl();
			if (TextUtils.isEmpty(urlImgServers)) {
				setMessage(String.format(getString(R.string.ui_error_on_imgsvr_url), getSiteName()));
			} else {
				if (AppCache.checkCacheForData(urlImgServers, 3600)) {
					// Debug
					printDebug(urlImgServers, "Loading");

					source = AppCache.readCacheForData(urlImgServers);
					processImgServersSource(source, url);
				} else {
					// Debug
					printDebug(urlImgServers, "Downloading");

					mSourceDownloader.cancelDownload();
					mSourceDownloader = new SourceDownloader(SourceDownloader.MODE_IMG_SERVERS);
					mSourceDownloader.download(urlImgServers);
				}
			}
		}
		// Direct/Redirect image URL
		else {
			processPageUrls();
		}
	}

	private void processImgServersSource(String source, String url) {
		if (TextUtils.isEmpty(source)) {
			setMessage(String.format(getString(R.string.ui_error_on_imgsvr_download), getSiteName()));
			return;
		}

		if (!mChapter.setDynamicImgServers(source, url)) {
			setMessage(String.format(getString(R.string.ui_error_on_imgsvr_process), getSiteName()));
			return;
		}

		if (!mChapter.hasDynamicImgServer()) {
			AppUtils.logE(this, "No DynamicImgServer.");
			return;
		}

		mPages = new HashMap<Integer, Page>();
		String imgServer = mChapter.getDynamicImgServer();

		// Debug
		printDebug(imgServer, "Get DynamicImgServer");

		for (int i = 0; i < mPageUrls.length; i++) {
			mPageUrls[i] = HttpUtils.joinUrl(imgServer, mPageUrls[i]);
		}

		processPageUrls();
	}

	private void processPageUrls() {
		if (mPageUrls.length == 0) {
			setMessage(getString(R.string.ui_no_pages));
		}

		mPages = new HashMap<Integer, Page>();
		for (int i = 0; i < mPageUrls.length; i++) {
			Page page = new Page(i + 1, mPageUrls[i], mManga.usingImgRedirect());
			mPages.put(i + 1, page);
		}

		mChapter.pageIndexMax = mPages.size();
		mProcessed = true;

		initalPage();
	}

	private void initalPage() {
		// TODO Update database
		if (mManga.isFavorite) {
			final AppSQLite db = App.DATABASE.open();
			AppUtils.logI(this, "Add to Favorite(Chapter).");
			try {
				long id = db.insertChapter(mChapter);
				mChapter._id = id;
				mChapter.isFavorite = true;
				AppUtils.logD(this, "Add as ID " + id + ".");
			} catch (SQLException e) {
				AppUtils.logE(this, e.getMessage());
			}
			db.close();
		}

		changePage(Math.max(1, mChapter.pageIndexLastRead));
	}

	private void changePage(int pageIndex) {
		if (pageIndex <= 0 || pageIndex > mChapter.pageIndexMax) {
			mChangePageMode = ChangePageMode.NONE;
			return;
		}

		AppUtils.logI(this, String.format("Change to Page %d.", pageIndex));

		mPageIndexLoading = pageIndex;
		mZoomListener.setFlingable(false);
		mPreloadPageIndexQueue.clear();
		for (int i = 1; i <= mPreloadMaxPages; i++) {
			mPreloadPageIndexQueue.add(mPageIndexLoading + i);
		}

		Page page = mPages.get(mPageIndexLoading);
		page.download();
	}

	private void changePage(boolean nextpage) {
		// Empty
		if (mChapter.pageIndexMax == 0) {
			return;
		}
		// Loading
		if (mPageIndexLoading != mChapter.pageIndexLastRead) {
			if (mPageIndexLoading <= 1 || mPageIndexLoading >= mChapter.pageIndexMax) {
				return;
			}
		}

		int mPageIndexGoto = mChapter.pageIndexLastRead + (nextpage ? 1 : -1);

		// Loading
		if (mPageIndexLoading != mChapter.pageIndexLastRead) {
			mPages.get(mPageIndexLoading).cancelDownload();
			mChapter.pageIndexLastRead = mPageIndexLoading;
			mPageIndexGoto = mChapter.pageIndexLastRead + (nextpage ? 1 : -1);
			mtvTitle.setText(String.format(getString(R.string.ui_goto_pages_format), mPageIndexGoto,
					mChapter.pageIndexMax));
			hideStatusBar();
			showTitleBar();
		}

		// Prev chapter
		if (mPageIndexGoto < 1) {
			AppUtils.popupMessage(this, "First Page");
			mZoomControl.startFling(0, 0);
		}
		// Next chapter
		else if (mPageIndexGoto > mChapter.pageIndexMax) {
			AppUtils.popupMessage(this, "Last Page");
			mZoomControl.startFling(0, 0);
			this.loadNextChapter();
		} else {
			mChangePageMode = nextpage ? ChangePageMode.NEXT : ChangePageMode.PREV;
			mZoomControl.startFling(0, 0);
			changePage(mPageIndexGoto);
		}
	}

	private void preloadPage(int pageIndex) {
		if (pageIndex - mPageIndexLoading <= mPreloadMaxPages && pageIndex <= mChapter.pageIndexMax) {
			AppUtils.logI(this, String.format("Preload Page %d.", pageIndex));
			mPages.get(pageIndex).download();
		} else {
			hideScroller();
		}
	}

	private void refreshPage() {
		setMessage("");

		if (mPages == null || mPages.size() == 0) {
			loadChapter();
		} else if (mPageIndexLoading == 0) {
			changePage(1);
		} else {
			Page page = mPages.get(mPageIndexLoading);
			page.download(true);
		}
	}

	private void notifyPageDownloaded(Page page) {
		AppUtils.logI(this, String.format("Notify that Page %d downloaded.", page.mPageIndex));

		// Current page
		if (page.mPageIndex == mPageIndexLoading) {
			// Recycle image
			if (mBitmap != null) {
				mBitmap.recycle();
				mBitmap = null;
			}

			if (page.isDownloaded()) {
				// Get image
				try {
					mBitmap = page.getBitmap();
				} catch (OutOfMemoryError e) {
					AppUtils.logE(this, "Out of Memory: " + e.getMessage());
					AppUtils.popupMessage(App.CONTEXT, R.string.popup_out_of_memory);
					setMessage(getString(R.string.ui_error_out_of_memory));
					mDead = true;
					if (mBitmap != null) {
						mBitmap.recycle();
						mBitmap = null;
					}
					setImage(mBitmap);
					findViewById(R.id.mbtnNext).setClickable(false);
					findViewById(R.id.mbtnPrev).setClickable(false);
					mvgTitleBar.setVisibility(View.VISIBLE);
					mPageView.setOnTouchListener(null);
					// finish();
					return;
				}
				if (mBitmap == null) {
					AppUtils.logE(this, "Invalid bitmap.");
					setMessage(getString(R.string.ui_error_invalid_image));
				}
			} else {
				setMessage(String.format(getString(R.string.ui_error_on_page_download), mPageIndexLoading,
						getSiteName()));
			}

			// Set image
			mChapter.pageIndexLastRead = mPageIndexLoading;
			setImage(mBitmap);
			mtvTitle.setText(getCustomTitle());
			showTitleBar();
			hideStatusBar();
			mZoomListener.setFlingable(true);
			// mZoomListener.onTouch(v, event);

			// TODO Update database
			if (mChapter.isFavorite) {
				final AppSQLite db = App.DATABASE.open();
				AppUtils.logI(this, "Update Chapter in Favorite.");
				try {
					if (db.updateChapter(mChapter) == 0) {
						AppUtils.logE(this, "Fail to update Chapter in Favorite.");
					}
				} catch (SQLException e) {
					AppUtils.logE(this, e.getMessage());
				}
				db.close();
			}
		}

		// Preload page
		if (mPreloadPageIndexQueue.isEmpty()) {
			if (App.DEBUG > 0) {
				hideScroller();
			}
		} else {
			if (App.DEBUG > 0) {
				mHideScrollerHandler.removeCallbacks(mHideScrollerRunnable);
				msvScroller.setVisibility(View.VISIBLE);
			}
			int pageIndexPreload = mPreloadPageIndexQueue.poll();
			preloadPage(pageIndexPreload);
		}
	}

	private boolean checkDummyPic(byte[] data) {
		if (DUMMY_PIC_CRC32.size() == 0) {
			String[] array = getResources().getStringArray(R.array.dummy_pic_crc32);
			for (int i = 0; i < array.length; i++) {
				DUMMY_PIC_CRC32.add(array[i]);
			}
		}
		CRC32 crc = new CRC32();
		crc.update(data);
		String hash = Integer.toHexString((int) crc.getValue()).toUpperCase();
		return DUMMY_PIC_CRC32.contains(hash);
	}

	private void setupZoomState() {
		if (mBitmap != null) {
			mBitmap.recycle();
		}
		mBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_4444);
		mPageView.setImage(mBitmap);
		mZoomControl.getZoomState().setAlignX(AlignX.Right);
		mZoomControl.getZoomState().setAlignY(AlignY.Top);
		mZoomControl.getZoomState().setPanX(0.0f);
		mZoomControl.getZoomState().setPanY(0.0f);
		mZoomControl.getZoomState().notifyObservers();
	}

	private void setImage(Bitmap bitmap) {
		if (bitmap != null) {
			setMessage("");
		}

		// AppUtils.logD(this, "ZoomView Width: " + mPageView.getWidth());
		// AppUtils.logD(this, "ZoomView Height: " + mPageView.getHeight());
		// AppUtils.logD(this, "AspectQuotient: " +
		// mPageView.getAspectQuotient().get());

		mZoomControl.stopFling();
		mZoomControl.getZoomState().setPanX(0.0f);
		mZoomControl.getZoomState().setPanY(0.0f);
		mPageView.setImage(bitmap);
		mZoomControl.getZoomState().setDefaultZoom(computeDefaultZoom(mZoomMode, mPageView));
		mZoomControl.getZoomState().notifyObservers();
		switch (mChangePageMode) {
		case NEXT:
			mZoomControl.pan(0.5f, 0f);
			break;
		case PREV:
			mZoomControl.pan(-0.5f, 0f);
			break;
		default:
			break;
		}
		mZoomControl.startFling(0, 0);
	}

	private float computeDefaultZoom(ZoomMode mode, ImageZoomView view) {
		final Bitmap bitmap = view.getImage();

		if (view.getAspectQuotient() == null || view.getAspectQuotient().get() == Float.NaN) {
			return 1f;
		}
		if (view == null || view.getWidth() == 0 || view.getHeight() == 0) {
			return 1f;
		}
		if (bitmap == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
			return 1f;
		}

		if (mode == ZoomMode.FIT_SCREEN) {
			return 1f;
		}

		// aq = (bW / bH) / (vW / vH)
		final float aq = view.getAspectQuotient().get();
		float zoom = 1f;

		if (mode == ZoomMode.FIT_WIDTH_OR_HEIGHT) {
			// Over width
			if (aq > 1f) {
				zoom = aq;
			}
			// Over height
			else {
				zoom = 1f / aq;
			}
		} else if (mode == ZoomMode.FIT_WIDTH || mode == ZoomMode.FIT_WIDTH_AUTO_SPLIT) {
			// Over height
			if (aq < 1f) {
				zoom = 1f / aq;
			} else {
				zoom = 1f;
			}

			if (mode == ZoomMode.FIT_WIDTH_AUTO_SPLIT) {
				if (1f * bitmap.getWidth() / view.getWidth() > App.WIDTH_AUTO_SPLIT_THRESHOLD
						&& bitmap.getWidth() > bitmap.getHeight()) {
					zoom *= (2f + App.WIDTH_AUTO_SPLIT_MARGIN) / (1f + App.WIDTH_AUTO_SPLIT_MARGIN);
				}
			}
		} else if (mode == ZoomMode.FIT_HEIGHT) {
			// Over width
			if (aq > 1f) {
				zoom = aq;
			} else {
				zoom = 1f;
			}
		}

		return zoom;
	}

	private void printDebug(String msg, String tag) {
		if (App.DEBUG == 0) {
			return;
		}

		mtvDebug.append("\n");
		SpannableString text;
		if (TextUtils.isEmpty(tag)) {
			text = new SpannableString(msg);
		} else {
			text = new SpannableString(tag + ": " + msg);
			text.setSpan(new StyleSpan(Typeface.BOLD), 0, tag.length() + 1, 0);
		}
		mtvDebug.append(text);
		msvScroller.post(new Runnable() {
			@Override
			public void run() {
				if (msvScroller != null) {
					msvScroller.smoothScrollBy(0, 1000);
				}
			}
		});
	}

	private void hideScroller() {
		mHideScrollerHandler.removeCallbacks(mHideScrollerRunnable);
		mHideScrollerHandler.postDelayed(mHideScrollerRunnable, App.TIME_AUTO_HIDE);
	}

	private void showTitleBar() {
		mHideTitleBarHandler.removeCallbacks(mHideTitleBarRunnable);
		mHideTitleBarHandler.postDelayed(mHideTitleBarRunnable, App.TIME_AUTO_HIDE);
		mvgTitleBar.setVisibility(View.VISIBLE);
		mtvTitle.requestFocus();
	}

	private void hideTitleBar() {
		mHideTitleBarHandler.removeCallbacks(mHideTitleBarRunnable);
		mvgTitleBar.setVisibility(View.GONE);
	}

	private void hideStatusBar() {
		mvgStatusBar.setVisibility(View.GONE);
	}

	// private void printDebug(String msg) {
	// printDebug(msg, null);
	// }
}
