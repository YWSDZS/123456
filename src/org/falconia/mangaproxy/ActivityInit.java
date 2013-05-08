package org.falconia.mangaproxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.falconia.mangaproxy.data.Chapter;
import org.falconia.mangaproxy.data.ChapterList;
import org.falconia.mangaproxy.data.Genre;
import org.falconia.mangaproxy.data.Manga;
import org.falconia.mangaproxy.data.Site;
import org.falconia.mangaproxy.plugin.Plugins;
import org.falconia.mangaproxyex.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public final class ActivityInit extends Activity implements OnClickListener {

	protected static final int DIALOG_UNMOUNTED_ID = -1;

	private boolean mExit = false;

	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// initialize static members
		// App.CONTEXT = getApplicationContext();
		// App.APP_NAME = getString(R.string.app_name);
		// App.APP_PACKAGE = getClass().getPackage().getName();
		// App.APP_FILES_DIR = getFilesDir();
		// App.APP_CACHE_DIR = getCacheDir();
		// App.APP_EXTERNAL_FILES_DIR = App.CONTEXT.getExternalFilesDir(null);
		// App.APP_EXTERNAL_CACHE_DIR = App.CONTEXT.getExternalCacheDir();
		// App.GENRE_ALL_TEXT = getString(R.string.genre_all);
		// App.UI_CHAPTER_COUNT = getString(R.string.ui_chapter_count);
		// App.UI_LAST_UPDATE = getString(R.string.ui_last_update);

		super.onCreate(savedInstanceState);
		AppUtils.logV(this, "onCreate()");

		checkExternalStorageMounted();
	}

	private void initalOnCreate() {
		if (App.DEBUG == -2) {
			startActivity(new Intent(this, ActivityChangelog.class));
			return;
		} else if (App.DEBUG >= 0) {
			sitchMode(App.DEBUG);
			return;
		}

		setContentView(R.layout.main);
		setTitle(String.format("%s (Alpha, Test only)", App.NAME));

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("alpha.txt")));
			StringBuilder builder = new StringBuilder();
			String text;
			while ((text = reader.readLine()) != null) {
				builder.append(text + AppCache.NEW_LINE);
			}
			reader.close();
			text = builder.toString().trim();
			if (!TextUtils.isEmpty(text)) {
				((TextView) findViewById(R.id.mtvMain)).setText(text);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		Button mbtn1 = (Button) findViewById(R.id.mbtn1);
		Button mbtn2 = (Button) findViewById(R.id.mbtn2);
		Button mbtn3 = (Button) findViewById(R.id.mbtn3);
		Button mbtn4 = (Button) findViewById(R.id.mbtn4);
		mbtn1.setText("Normal Mode");
		mbtn2.setText("MangaList");
		mbtn3.setText("PageView");
		mbtn4.setText("ZoomView");
		mbtn1.setOnClickListener(this);
		mbtn2.setOnClickListener(this);
		mbtn3.setOnClickListener(this);
		mbtn4.setOnClickListener(this);
	}

	private void checkExternalStorageMounted() {
		if (!AppEnv.isExternalStorageMounted()) {
			showDialog(DIALOG_UNMOUNTED_ID);
		} else {
			App.APP_EXTERNAL_FILES_DIR = App.CONTEXT.getExternalFilesDir(null);
			App.APP_EXTERNAL_CACHE_DIR = App.CONTEXT.getExternalCacheDir();
			if (App.APP_EXTERNAL_CACHE_DIR == null || App.APP_EXTERNAL_FILES_DIR == null
					|| !App.APP_EXTERNAL_CACHE_DIR.canWrite() || !App.APP_EXTERNAL_FILES_DIR.canWrite()) {
				showDialog(DIALOG_UNMOUNTED_ID);
			} else {
				initalOnCreate();
			}
		}
	}

	private void sitchMode(int mode) {
		if (!AppEnv.isExternalStorageMounted()) {
			return;
		}

		switch (mode) {
		// normal
		case 0:
			startActivity(new Intent(this, ActivityFavoriteList.class));
			break;
		// start ActivityMangaList
		case 1: {
			Site site = new Site(Plugins.getPlugin(1000));
			Genre genre = new Genre("new", "点击排行", site.getSiteId());
			ActivityMangaList.IntentHandler.startActivityMangaList(this, genre);
			break;
		}
		// start ActivityChapter
		case 2: {
			Site site = new Site(Plugins.getPlugin(1000));
			Manga manga = new Manga("174", "魔法先生", null, site.getSiteId());
			manga.isCompleted = false;
			manga.chapterCount = 323;
			Chapter chapter = new Chapter("73996", "魔法先生323集", manga);
			chapter.typeId = Chapter.TYPE_ID_CHAPTER;
			chapter.setDynamicImgServerId(3);
			manga.chapterList = new ChapterList(manga);
			manga.chapterList.add(chapter);
			ActivityChapter.IntentHandler.startActivityChapter(this, manga, chapter);
			break;
		}
		// start DebugActivity
		case 3:
			startActivity(new Intent(this, DebugActivity.class));
			break;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		AppUtils.logV(this, "onStart()");
	}

	@Override
	protected void onResume() {
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

		if (App.DEBUG == 0) {
			finish();
		}
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

		if (mExit) {
			System.exit(0);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		AppUtils.logV(this, "onSaveInstanceState()");
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		AppUtils.logV(this, "onRestoreInstanceState()");
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AppUtils.logV(this, "onCreateDialog()");
		Dialog dialog;
		switch (id) {
		case DIALOG_UNMOUNTED_ID:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.dialog_unmounted_title).setMessage(R.string.dialog_unmounted_message)
					.setCancelable(false)
					.setPositiveButton(R.string.dialog_unmounted_try_again, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface paramDialogInterface, int paramInt) {
							checkExternalStorageMounted();
						}
					}).setNegativeButton(R.string.dialog_unmounted_exit, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface paramDialogInterface, int paramInt) {
							dismissDialog(DIALOG_UNMOUNTED_ID);
							mExit = true;
							finish();
						}
					});
			dialog = builder.create();
			dialog.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface paramDialogInterface) {
					checkExternalStorageMounted();
				}
			});
			break;
		default:
			dialog = super.onCreateDialog(id);
		}
		return dialog;
	}

	@Override
	public void onBackPressed() {
		mExit = true;
		super.onBackPressed();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.mbtn1:
			sitchMode(App.DEBUG = 0);
			break;
		case R.id.mbtn2:
			sitchMode(App.DEBUG = 1);
			break;
		case R.id.mbtn3:
			sitchMode(App.DEBUG = 2);
			break;
		case R.id.mbtn4:
			sitchMode(App.DEBUG = 3);
			break;
		}
	}
}