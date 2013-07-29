package com.zacharyliu.stepnavigation;

import android.content.Context;
import android.os.Handler;

import com.zacharyliu.stepnavigation.GpsBearing.GpsBearingListener;

public class FakeGpsBearing implements IGpsBearing {
	
	private final int DELAY = 1000;
	private final double BEARING = 317.0;
	private final double[] LOCATION = {40.468138, -74.445318};
	private boolean enabled = true;

	private GpsBearingListener mListener;
	
	public FakeGpsBearing(Context context, GpsBearingListener listener) {
		mListener = listener;
	}
	
	@Override
	public void resume() {
		final Handler handler = new Handler();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				mListener.onBearingUpdate(BEARING);
				mListener.onLocationUpdate(LOCATION);
				if (enabled)
					handler.postDelayed(this, DELAY);
			}
		};
		handler.postDelayed(runnable, DELAY);
	}

	@Override
	public void pause() {
		enabled = false;
	}

	@Override
	public void on() {
		if (!enabled) {
			enabled = true;
			resume();
		}
	}

	@Override
	public void off() {
		pause();
	}
	
}
