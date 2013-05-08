package org.falconia.mangaproxy;

import java.io.File;
import java.io.IOException;
import org.falconia.mangaproxyex.R;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.text.TextUtils;

public final class AppEnv {

	public static boolean isExternalStorageMounted() {
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
	}

	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager manager = (ConnectivityManager) context.getSystemService("connectivity");
		return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isAvailable();
	}

	public static File getExternalFilesDir() throws IOException {
		if (isExternalStorageMounted()) {
			return App.CONTEXT.getExternalFilesDir(null);
		}
		if (App.APP_EXTERNAL_FILES_DIR != null) {
			return App.APP_EXTERNAL_FILES_DIR;
		}
		if (TextUtils.isEmpty(App.PACKAGE)) {
			throw new IOException("Invalid package name.");
		}
		return new File(Environment.getExternalStorageDirectory(), String.format("Android/data/%s", App.PACKAGE));
	}

	public static File getExternalCacheDir() throws IOException {
		if (isExternalStorageMounted()) {
			return App.CONTEXT.getExternalCacheDir();
		}
		if (App.APP_EXTERNAL_CACHE_DIR != null) {
			return App.APP_EXTERNAL_CACHE_DIR;
		}
		return new File(getExternalFilesDir(), "cache");
	}

}
