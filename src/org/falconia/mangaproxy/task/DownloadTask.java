package org.falconia.mangaproxy.task;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.impl.cookie.BasicClientCookie;
import org.falconia.mangaproxy.App;
import org.falconia.mangaproxy.AppEnv;
import org.falconia.mangaproxy.plugin.Plugins;
import org.falconia.mangaproxy.utils.HttpUtils;

import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

public class DownloadTask extends AsyncTask<String, Integer, byte[]> {

	private static final String DESKTOP_AGENT = "Mozilla/5.0 (Windows NT 6.1; rv:7.0.1) Gecko/20100101 Firefox/7.0";
	
	static {
		// Use desktop User-Agent
		System.setProperty("http.agent", DESKTOP_AGENT);

		disableConnectionReuseIfNecessary();
		enableHttpResponseCache();
	}

	private static void disableConnectionReuseIfNecessary() {
		// HTTP connection reuse which was buggy pre-froyo
		if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}
	}

	private static void enableHttpResponseCache() {
		// Ice Cream Sandwich
		try {
			long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
			File httpCacheDir = new File(AppEnv.getExternalCacheDir(), "http");
			Class.forName("android.net.http.HttpResponseCache").getMethod("install", File.class, long.class)
					.invoke(null, httpCacheDir, httpCacheSize);
		} catch (Exception httpResponseCacheNotAvailable) {
		}
	}

	protected static final int TIME_OUT_CONNECT = 10000;
	protected static final int TIME_OUT_READ = 10000;

	private static final int MAX_BUFFER_SIZE = 1024;

	public static byte[] DownloadBytes;

	private int mFileSize;
	private int mDownloaded;
	private OnDownloadListener mListener;
	private String mReferer;
    private String cookies = "";
	private boolean mCancelled = false;

	public DownloadTask(OnDownloadListener listener) {
		mFileSize = 0;
		mDownloaded = 0;
		mListener = listener;
	}

	public DownloadTask(OnDownloadListener listener, String referer) {
		this(listener);
		mReferer = referer;
		DownloadBytes = new byte[0];
	}
	
	public DownloadTask(OnDownloadListener listener, String referer, String cookies) {
		this(listener);
		mReferer = referer;
		DownloadBytes = new byte[0];
		this.cookies = cookies;
	}


	@Override
	protected byte[] doInBackground(String... params) {
		mCancelled = false;
		byte[] bytes = download(params[0]);
		DownloadBytes = bytes;
		return bytes;
	}

	@Override
	protected void onPreExecute() {
		logD("Download start.");
		if (mListener != null) {
			mListener.onPreDownload();
		} else {
			logD("onPreExecute()");
			logW("Cancelled or null listener.");
		}
	}

	@Override
	protected void onPostExecute(byte[] result) {
		logD("Download done.");
		if (!mCancelled && mListener != null) {
			mListener.onPostDownload(result);
		} else {
			logD("onPostExecute()");
			logW("Cancelled or null listener.");
		}
	}

	@Override
	protected void onCancelled() {
		logD("Download cancelled.");
		mCancelled = true;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		if (!mCancelled && mListener != null) {
			mListener.onDownloadProgressUpdate(values[0], mFileSize);
		} else {
			logD("onProgressUpdate()");
			logW("Cancelled or null listener.");
		}
	}

	protected byte[] download(String url) {
		logD("Downloading: " + url);
		url = HttpUtils.urlencode(url);
		HttpURLConnection connection;
		try {
			connection = (HttpURLConnection) (new URL(url)).openConnection();
			logD("cookies: " + this.cookies);
			if (this.cookies != null || this.cookies.length() > 0)
			{
				
				connection.setRequestProperty("Cookie", this.cookies);
			}
			connection.setConnectTimeout(TIME_OUT_CONNECT);
			connection.setReadTimeout(TIME_OUT_READ);
			connection.setRequestProperty("User-Agent", DESKTOP_AGENT);
			if (!TextUtils.isEmpty(mReferer)) {
				connection.setRequestProperty("Referer", mReferer);
			}

			int statusCode = connection.getResponseCode();
			if (statusCode >= 400) {
				logE("Invalid Status Code: " + statusCode);
				return null;
			} else {
				logD("Status Code: " + statusCode);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			logE("Invalid URL: " + e.getMessage());
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			logE("Fail to open the connection: " + e.getMessage());
			return null;
		}

		InputStream input;
		try {
			input = connection.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
			logE("IOException(InputStream): " + e.getMessage());
			return null;
		}

		mFileSize = connection.getContentLength();
		logD(String.format("Content-Length: %d", mFileSize));
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		publishProgress(0);

		try {
			int readed = 0;
			while (readed != -1 || mFileSize > mDownloaded) {
				if (mCancelled) {
					output.close();
					input.close();
					connection.disconnect();
					logD("Connection cancelled.");
					return null;
				}
				byte[] buffer = new byte[MAX_BUFFER_SIZE];
				readed = input.read(buffer);
				if (readed == -1) {
					publishProgress(mDownloaded);
					break;
				}
				output.write(buffer, 0, readed);
				mDownloaded += readed;
				publishProgress(mDownloaded);
			}
			byte[] result = output.toByteArray();
			logD("Downloaded length: " + result.length);
			output.flush();
			output.close();
			input.close();
			connection.disconnect();
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			logE("IOException(InputStream): " + e.getMessage());
		}

		return null;
	}
	
	public void cancelDownload() {
		logD("Download cancelling.");
		mCancelled = true;
		cancel(true);
	}

	private String getTag() {
		return getClass().getSimpleName();
	}

	private void log(int priority, String msg) {
		Log.println(priority, App.NAME, String.format("[%s] %s", getTag(), msg));
	}

	private void logD(String msg) {
		log(Log.DEBUG, msg);
	}

	private void logW(String msg) {
		log(Log.WARN, msg);
	}

	private void logE(String msg) {
		log(Log.ERROR, msg);
	}
}
