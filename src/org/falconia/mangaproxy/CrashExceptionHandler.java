package org.falconia.mangaproxy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import org.falconia.mangaproxyex.R;

public final class CrashExceptionHandler implements UncaughtExceptionHandler {

	private UncaughtExceptionHandler defaultUEH;

	public CrashExceptionHandler() {
		defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		AppUtils.logE(this, "Uncaught Exception: " + ex.getMessage());

		// Date date = Calendar.getInstance().getTime();
		// String timestamp = new
		// SimpleDateFormat("yyyy-MM-dd HH:mmZ").format(date);
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		ex.printStackTrace(printWriter);
		String stacktrace = result.toString();
		printWriter.close();

		writeToFile(stacktrace);

		defaultUEH.uncaughtException(thread, ex);

		// thread.getThreadGroup().destroy();
	}

	private void writeToFile(String stacktrace) {
		try {
			File file = new File(App.APP_EXTERNAL_FILES_DIR, "crash.txt");
			file.delete();
			BufferedWriter bos = new BufferedWriter(new FileWriter(file));
			bos.write(stacktrace);
			bos.flush();
			bos.close();
			AppUtils.popupMessage(App.CONTEXT, R.string.popup_write_crash_log);
		} catch (Exception e) {
			e.printStackTrace();
			AppUtils.logE(this, "Fail to write crash file.");
		}
		// try {
		// Intent i = new Intent(App.CONTEXT, CrashActivity.class);
		// i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// App.CONTEXT.startActivity(i);
		// App.CONTEXT.startActivity(i);
		// } catch (Exception e) {
		// e.printStackTrace();
		// AppUtils.logE(this, "Fail to start CrashActivity: " +
		// e.getMessage());
		// }
	}

}
