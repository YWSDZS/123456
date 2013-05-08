package org.falconia.mangaproxy;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import org.falconia.mangaproxyex.R;

public final class ActivityPreference extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
		setTitle(String.format("%s - %s", getString(R.string.app_name), getTitle()));
	}

}
