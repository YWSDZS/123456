package org.falconia.mangaproxy.ui;

import org.falconia.mangaproxy.App;
import org.falconia.mangaproxy.AppUtils;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;

import com.sonyericsson.zoom.DynamicZoomControl;
import com.sonyericsson.zoom.ImageZoomView;
import com.sonyericsson.zoom.ZoomState;

@Deprecated
public final class ZoomViewOnGestureListener extends SimpleOnGestureListener {

	private static final String GESTURE_TAG = "Gesture";

	private long time;

	private ImageZoomView mZoomView;
	private DynamicZoomControl mZoomControl;

	private boolean isEdgeLeft;
	private boolean isEdgeRight;

	@SuppressWarnings("unused")
	private float scrollX;
	@SuppressWarnings("unused")
	private float scrollY;

	// private Handler flingHandler;
	// private Runnable flingRunnable;

	public ZoomViewOnGestureListener(Context context) {
		super();

		// flingHandler = new Handler();
		// flingRunnable = new Runnable() {
		// @Override
		// public void run() {
		// Log.i(GESTURE_TAG, "Fling over screen.");
		// startFling(0, 0, scrollX, scrollY);
		// }
		// };
	}

	@Override
	public boolean onDown(MotionEvent e) {
		Log.v(GESTURE_TAG, "onDown() point:" + e.getPointerCount());

		time = System.currentTimeMillis();

		if (mZoomControl.getPanMinX() == mZoomControl.getPanMaxX()) {
			isEdgeLeft = true;
			isEdgeRight = true;
		} else {
			isEdgeLeft = (getZoomState().getPanX() <= mZoomControl.getPanMinX());
			isEdgeRight = (getZoomState().getPanX() >= mZoomControl.getPanMaxX());
		}
		Log.d(GESTURE_TAG, String.format("isEdgeLeft:%b, isEdgeRight:%b", isEdgeLeft, isEdgeRight));

		scrollX = 0;
		scrollY = 0;

		return true;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		Log.v(GESTURE_TAG, "onSingleTapConfirmed()");
		return super.onSingleTapConfirmed(e);
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		Log.v(GESTURE_TAG, "onSingleTapUp()");
		return super.onSingleTapUp(e);
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		Log.v(GESTURE_TAG, "onDoubleTap()");

		if (getZoomState().getZoom() != getZoomState().getDefaultZoom()) {
			Log.i(GESTURE_TAG, "Double Tap");
			AppUtils.popupMessage(App.CONTEXT, "Double Tap");
			return true;
		}

		return false;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		Log.v(GESTURE_TAG, "onDoubleTapEvent()");
		return super.onDoubleTapEvent(e);
	}

	@Override
	public void onShowPress(MotionEvent e) {
		Log.v(GESTURE_TAG, "onShowPress()");
	}

	@Override
	public void onLongPress(MotionEvent e) {
		Log.v(GESTURE_TAG, "onLongPress()");
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		Log.v(GESTURE_TAG,
				String.format("onScroll() - wait:%dms - point:%d", (System.currentTimeMillis() - time),
						e2.getPointerCount()));
		time = System.currentTimeMillis();

		// flingHandler.removeCallbacks(flingRunnable);

		scrollX += distanceX;
		scrollY += distanceY;

		mZoomControl.pan(distanceX / mZoomView.getWidth(), distanceY / mZoomView.getHeight());
		// flingHandler.postDelayed(flingRunnable, 100);

		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		Log.v(GESTURE_TAG, "onFling()");

		// flingHandler.removeCallbacks(flingRunnable);
		startFling(velocityX, velocityY, e2.getX() - e1.getX(), e2.getY() - e1.getY());

		return true;
	}

	private void startFling(float vx, float vy, float dx, float dy) {
		Log.d(GESTURE_TAG, String.format("vx:%f, vy:%f, dx:%f, dy:%f", vx, vy, dx, dy));
		if (((isEdgeLeft && dx > 0) || (isEdgeRight && dx < 0)) && Math.abs(dx) > Math.abs(dy) * 1.2f
				&& Math.abs(dx) > Math.min(0.5f * mZoomView.getWidth(), 0.5f * mZoomView.getHeight())) {
			if (dx > 0) {
				Log.i(GESTURE_TAG, "Next Page");
				AppUtils.popupMessage(App.CONTEXT, "Next Page");
			} else {
				Log.i(GESTURE_TAG, "Prev Page");
				AppUtils.popupMessage(App.CONTEXT, "Prev Page");
			}
		} else {
			mZoomControl.startFling(-vx / mZoomView.getWidth(), -vy / mZoomView.getHeight());
		}
	}

	public void setZoomView(ImageZoomView view) {
		mZoomView = view;
	}

	public void setZoomControl(DynamicZoomControl control) {
		mZoomControl = control;
	}

	private ZoomState getZoomState() {
		return mZoomControl.getZoomState();
	}
}
