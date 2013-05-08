/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.falconia.mangaproxy.ui;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Like AnalogClock, but digital. Shows seconds.
 * 
 * FIXME: implement separate views for hours/minutes/seconds, so proportional
 * fonts don't shake rendering
 */

public class SystemInfo extends TextView {

	private Calendar mCalendar;

	private final static String m12 = "h:mm AA";
	private final static String m24 = "k:mm";
	private FormatChangeObserver mFormatChangeObserver;

	private Runnable mTicker;
	private Handler mHandler;
	private BroadcastReceiver mBatInfoReceiver;

	private boolean mTickerStopped = false;

	private String mFormat;
	private String mBattery;
	private String mTime;

	public SystemInfo(Context context) {
		super(context);
		initClock(context);
	}

	public SystemInfo(Context context, AttributeSet attrs) {
		super(context, attrs);
		initClock(context);
	}

	private void initClock(Context context) {
		if (mCalendar == null) {
			mCalendar = Calendar.getInstance();
		}

		mFormatChangeObserver = new FormatChangeObserver();
		getContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, false,
				mFormatChangeObserver);

		setFormat();

		mBattery = "";
		mTime = "";
	}

	@Override
	protected void onAttachedToWindow() {
		mTickerStopped = false;
		super.onAttachedToWindow();
		mHandler = new Handler();

		/**
		 * requests a tick on the next hard-second boundary
		 */
		mTicker = new Runnable() {
			@Override
			public void run() {
				if (mTickerStopped) {
					return;
				}
				mCalendar.setTimeInMillis(System.currentTimeMillis());
				mTime = DateFormat.format(mFormat, mCalendar).toString().toUpperCase();
				updateText();
				invalidate();
				long now = SystemClock.uptimeMillis();
				long next = now + (60000 - now % 60000);
				mHandler.postAtTime(mTicker, next);
			}
		};
		mTicker.run();

		mBatInfoReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int level = intent.getIntExtra("level", 0);
				mBattery = String.format("%d%%", level);
				updateText();
				invalidate();
			}
		};
		getContext().registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	}

	@Override
	protected void onDetachedFromWindow() {
		mTickerStopped = true;
		getContext().unregisterReceiver(mBatInfoReceiver);
		getContext().getContentResolver().unregisterContentObserver(mFormatChangeObserver);
		super.onDetachedFromWindow();
	}

	private void updateText() {
		setText(String.format("%s  %s", mBattery, mTime));
	}

	/**
	 * Pulls 12/24 mode from system settings
	 */
	private boolean get24HourMode() {
		return DateFormat.is24HourFormat(getContext());
	}

	private void setFormat() {
		if (get24HourMode()) {
			mFormat = m24;
		} else {
			mFormat = m12;
		}
	}

	private class FormatChangeObserver extends ContentObserver {
		public FormatChangeObserver() {
			super(new Handler());
		}

		@Override
		public void onChange(boolean selfChange) {
			setFormat();
		}
	}
}
