package org.falconia.mangaproxy.utils;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.TimeZone;

import org.falconia.mangaproxy.App;
import org.falconia.mangaproxyex.R;

import android.util.Log;

public final class FormatUtils {

	public enum FileSizeUnit {
		b, B, KB, MB, GB, TB
	}

	public static final String[] FileSizeUnits = { "k", "B", "KB", "MB", "GB", "TB" };

	public static final long MILLISECOND_OF_MINUTE = 1000 * 60;
	public static final long MILLISECOND_OF_HOUR = MILLISECOND_OF_MINUTE * 60;
	public static final long MILLISECOND_OF_DAY = MILLISECOND_OF_HOUR * 24;

	public static String getFileSize(double size, FileSizeUnit in) {
		switch (in) {
		case TB:
			size *= 1024;
		case GB:
			size *= 1024;
		case MB:
			size *= 1024;
		case KB:
			size *= 1024;
		case B:
			break;
		case b:
			size /= 8;
		}

		int i;
		for (i = 1; size >= 1000; i++) {
			size /= 1024;
		}

		return String.format("%.3f%s", size, FileSizeUnits[i]);
	}

	public static String getFileSize(String file, String charset) {
		int size = 0;
		try {
			size = file.getBytes(charset).length;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Log.e(App.NAME, e.toString() + ": " + e.getMessage());
		}
		return getFileSize(size, FileSizeUnit.B);
	}

	public static String getFileSizeBtoKB(double size) {
		return String.format("%.3fKB", size / 1024d);
	}

	public static String getCountDownDateTime(Calendar time) {
		String format = "";

		if (time == null) {
			return format;
		}
		time.setTimeZone(TimeZone.getDefault());
		// format = String.format("%1$tF %1$tT%1$tz", time);

		Calendar now = Calendar.getInstance();
		long offset = now.getTimeInMillis() - time.getTimeInMillis();
		long offsetYear = TimeUtils.getCalendarOffset(time, now, Calendar.YEAR);
		long offsetMonth = TimeUtils.getCalendarOffset(time, now, Calendar.MONTH);
		long offsetDay = TimeUtils.getCalendarOffset(time, now, Calendar.DAY_OF_MONTH);

		if (offset < MILLISECOND_OF_MINUTE) {
			format = App.CONTEXT.getString(R.string.datetime_countdown_seconts);
		} else if (offset < MILLISECOND_OF_HOUR) {
			format = String.format(App.CONTEXT.getString(R.string.datetime_countdown_minutes), offset
					/ MILLISECOND_OF_MINUTE);
		} else if (offset < MILLISECOND_OF_DAY) {
			format = String.format(App.CONTEXT.getString(R.string.datetime_countdown_hours), offset
					/ MILLISECOND_OF_HOUR);
		} else if (offsetYear == 0 && offsetMonth == 0 && offsetDay <= 1) {
			format = App.CONTEXT.getString(R.string.datetime_countdown_1day);
		} else if (offsetYear == 0 && offsetMonth == 0 && offsetDay <= 2) {
			format = App.CONTEXT.getString(R.string.datetime_countdown_2day);
		} else if (offsetYear == 0) {
			format = String.format(App.CONTEXT.getString(R.string.datetime_countdown_days),
					time.get(Calendar.MONTH) + 1, time.get(Calendar.DAY_OF_MONTH));
		} else {
			format = String.format("%tF", time);
		}
		return format;
	}

}
