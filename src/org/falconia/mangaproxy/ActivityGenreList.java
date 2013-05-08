package org.falconia.mangaproxy;

import org.falconia.mangaproxy.data.Genre;
import org.falconia.mangaproxy.data.GenreList;
import org.falconia.mangaproxy.data.GenreSearch;
import org.falconia.mangaproxy.data.Site;
import org.falconia.mangaproxy.plugin.Plugins;
import org.falconia.mangaproxy.ui.BaseHeadersAdapter;
import org.falconia.mangaproxyex.R;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public final class ActivityGenreList extends ActivityBase implements OnClickListener {

	public final static class IntentHandler {

		private static final String BUNDLE_KEY_SITE_ID = "BUNDLE_KEY_SITE_ID";

		private static Intent getIntent(Context context, int siteId) {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_KEY_SITE_ID, siteId);
			Intent i = new Intent(context, ActivityGenreList.class);
			i.putExtras(bundle);
			return i;
		}

		protected static int getSiteId(ActivityGenreList activity) {
			return activity.getIntent().getExtras().getInt(BUNDLE_KEY_SITE_ID);
		}

		public static void startActivityGenreList(Context context, int siteId) {
			context.startActivity(getIntent(context, siteId));
		}

	}

	private final class GenreListAdapter extends BaseHeadersAdapter {

		final class ViewHolder {
			public TextView tvDisplayname;
		}

		private LayoutInflater mInflater;

		public GenreListAdapter() {
			mInflater = LayoutInflater.from(ActivityGenreList.this);
		}

		@Override
		public int getCount() {
			if (mGenreList == null) {
				return 0;
			}
			return mGenreList.size();
		}

		@Override
		public Genre getItem(int position) {
			return mGenreList.getAt(position);
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).genreId.hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.list_item_genre, null);
				holder.tvDisplayname = (TextView) convertView.findViewById(R.id.mtvDisplayname);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.tvDisplayname.setText(mGenreList.getDisplayname(position));

			return convertView;
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// TODO Auto-generated method stub

		}

	}

	private static final String BUNDLE_KEY_GENRE_LIST = "BUNDLE_KEY_GENRE_LIST";

	private int mSiteId;
	private Site mSite;
	private GenreList mGenreList;

	InputMethodManager mIMM;
	private EditText metSearch;
	private ImageButton mbtnSearch;

	@Override
	public int getSiteId() {
		return mSiteId;
	}

	@Override
	public String getSiteName() {
		return mSite.getName();
	}

	@Override
	String getSiteDisplayname() {
		return mSite.getDisplayname();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSiteId = IntentHandler.getSiteId(this);
		mSite = Site.get(mSiteId);

		setContentView(R.layout.activity_genre_list);
		setCustomTitle(getString(R.string.genre));

		// mShowProcessDialog = false;
		
		mIMM = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		metSearch = (EditText) findViewById(R.id.metSearch);
		mbtnSearch = (ImageButton) findViewById(R.id.mbtnSearch);

		setupListView(new GenreListAdapter());

		if (mSite.hasSearchEngine()) {
			findViewById(R.id.mvgSearch).setVisibility(View.VISIBLE);
			mbtnSearch.setOnClickListener(this);
			metSearch.setImeActionLabel(getString(R.string.ui_search), KeyEvent.KEYCODE_ENTER);
			OnKeyListener listener = new OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
						onClick(v);
						return true;
					}
					return false;
				}
			};
			metSearch.setOnKeyListener(listener);
		} else {
			findViewById(R.id.mvgSearch).setVisibility(View.GONE);
		}

		if (!mProcessed) {
			loadGenreList();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putSerializable(BUNDLE_KEY_GENRE_LIST, mGenreList);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		mGenreList = (GenreList) savedInstanceState.getSerializable(BUNDLE_KEY_GENRE_LIST);
		mListAdapter.notifyDataSetChanged();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_DOWNLOAD_ID:
			dialog = mSourceDownloader.createDownloadDialog(R.string.source_of_genre_list);
			break;
		case DIALOG_PROCESS_ID:
			dialog = createProcessDialog(R.string.source_of_genre_list);
			break;
		default:
			dialog = super.onCreateDialog(id);
		}
		return dialog;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_base_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.mmiRefresh:
			mProcessed = false;
			mSourceDownloader.download(mSite.getGenreListUrl());
			return true;
		case R.id.mmiPreferences:
			startActivity(new Intent(this, ActivityPreference.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Genre genre = mGenreList.getAt(position);
		ActivityMangaList.IntentHandler.startActivityMangaList(this, genre);
	}

	@Override
	public void onClick(View view) {
		String search = metSearch.getText().toString();
		if (search.length() == 0)
			return;
		mIMM.hideSoftInputFromWindow(metSearch.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
		GenreSearch genreSearch = mSite.getGenreSearch(search);
		ActivityMangaList.IntentHandler.startActivityMangaList(this, genreSearch);
	}

	@Override
	public int onSourceProcess(String source, String url) {
		mGenreList = mSite.getGenreList(source, url);
		return mGenreList.size();
	}

	@Override
	public void onPostSourceProcess(int result) {
		mListAdapter.notifyDataSetChanged();
		getListView().requestFocus();

		super.onPostSourceProcess(result);
	}

	private void loadGenreList() {
		
		
		mSourceDownloader = new SourceDownloader();
		mSourceDownloader.download(mSite.getGenreListUrl(), !mSite.hasSearchEngine());
		
	}

}
