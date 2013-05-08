package org.falconia.mangaproxy.task;

public interface OnDownloadListener {

	void onPreDownload();

	void onPostDownload(byte[] result);

	void onDownloadProgressUpdate(int value, int total);

}
