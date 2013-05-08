package org.falconia.mangaproxy.task;

import org.falconia.mangaproxy.AppUtils;

import android.os.AsyncTask;

public class SourceProcessTask extends AsyncTask<String, Void, Integer> {

	private OnSourceProcessListener mListener;

	private boolean mCancelled = false;

	public SourceProcessTask(OnSourceProcessListener listener) {
		mListener = listener;
	}

	@Override
	protected Integer doInBackground(String... params) {
		mCancelled = false;
		if (params.length != 2) {
			AppUtils.logE(this, "Invalid number of arguments.");
			throw new IllegalArgumentException("Invalid number of arguments.");
		}
		int result = mListener.onSourceProcess(params[0], params[1]);
		if (!mCancelled && mListener != null) {
			return result;
		} else {
			AppUtils.logD(this, "doInBackground()");
			AppUtils.logW(this, "Cancelled or null listener.");
			return 0;
		}
	}

	@Override
	protected void onPreExecute() {
		if (!mCancelled && mListener != null) {
			mListener.onPreSourceProcess();
		} else {
			AppUtils.logD(this, "onPreExecute()");
			AppUtils.logW(this, "Cancelled or null listener.");
		}
	}

	@Override
	protected void onPostExecute(Integer result) {
		if (!mCancelled && mListener != null) {
			mListener.onPostSourceProcess(result);
		} else {
			AppUtils.logD(this, "onPostExecute()");
			AppUtils.logW(this, "Cancelled or null listener.");
		}
	}

	@Override
	protected void onCancelled() {
		AppUtils.logD(this, "Process cancelled.");
		mCancelled = true;
	}
}
