package org.falconia.mangaproxy;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import org.falconia.mangaproxyex.R;

public final class AppUtils {

	public static void popupMessage(Context context, int resId) {
		Toast.makeText(context.getApplicationContext(), resId, Toast.LENGTH_SHORT).show();
	}

	public static void popupMessage(Context context, CharSequence text) {
		Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_SHORT).show();
	}

	public static void log(int priority, String tag, String msg) {
		Log.println(priority, App.NAME, String.format("[%s] %s", tag, msg));
	}

	public static void log(int priority, Object src, String msg) {
		if (src instanceof String) {
			log(priority, ((String) src), msg);
		} else if (src instanceof ITag) {
			log(priority, ((ITag) src).getTag(), msg);
		} else {
			log(priority, src.getClass().getSimpleName(), msg);
		}
	}

	public static void logV(Object src, String msg) {
		// DEBUG = 2
		log(Log.VERBOSE, src, msg);
	}

	public static void logD(Object src, String msg) {
		// DEBUG = 3
		log(Log.DEBUG, src, msg);
	}

	public static void logI(Object src, String msg) {
		// INFO = 4
		log(Log.INFO, src, msg);
	}

	public static void logW(Object src, String msg) {
		// WARN = 5
		log(Log.WARN, src, msg);
	}

	public static void logE(Object src, String msg) {
		// ERROR = 6
		log(Log.ERROR, src, msg);
	}

}
