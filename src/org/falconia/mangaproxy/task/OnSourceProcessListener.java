package org.falconia.mangaproxy.task;

public interface OnSourceProcessListener {

	int onSourceProcess(String source, String url);

	void onPreSourceProcess();

	void onPostSourceProcess(int size);

}
