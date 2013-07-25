package com.zacharyliu.stepnavigation;

import android.content.Context;
import android.os.Handler;

public class FakeGpsBearing extends GpsBearing {
	
	private final int DELAY = 500;
	private final double BEARING = 0.0;
	private final double[] LOCATION = {40.468184, -74.445385};

	private GpsBearingListener mListener;
	
	public FakeGpsBearing(Context context, GpsBearingListener listener) {
		mListener = listener;
		final Handler handler = new Handler();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				mListener.onBearingUpdate(BEARING);
				mListener.onLocationUpdate(LOCATION);
				handler.postDelayed(this, DELAY);
			}
		};
		handler.postDelayed(runnable, DELAY);
	}
	
	@Override
	public void resume() {}

	@Override
	public void pause() {}
	
}
