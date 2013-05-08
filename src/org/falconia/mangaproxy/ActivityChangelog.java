package org.falconia.mangaproxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.falconia.mangaproxyex.R;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.TextView;

public final class ActivityChangelog extends Activity implements OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.activity_changelog);
		setTitle(String.format("%s - Changelog", App.NAME));

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("changelog.txt")));
			StringBuilder builder = new StringBuilder();
			String text;
			while ((text = reader.readLine()) != null) {
				builder.append(text + AppCache.NEW_LINE);
			}
			reader.close();
			text = builder.toString().trim();
			if (!TextUtils.isEmpty(text)) {
				((TextView) findViewById(R.id.mtvMain)).setText(text);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		findViewById(R.id.mbtnOk).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		App.setVersionCode();
		finish();
	}

}
