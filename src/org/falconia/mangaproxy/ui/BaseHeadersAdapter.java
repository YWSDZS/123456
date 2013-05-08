package org.falconia.mangaproxy.ui;

import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;

public abstract class BaseHeadersAdapter extends BaseAdapter implements OnScrollListener {
	private boolean mDisplaySectionHeaders = false;

	public boolean getDisplaySectionHeadersEnabled() {
		return mDisplaySectionHeaders;
	}

}
