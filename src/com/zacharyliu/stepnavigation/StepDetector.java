package com.zacharyliu.stepnavigation;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

public class StepDetector implements ICustomSensor {

	private final double ACCELERATION_THRESHOLD = 1.3;
	private final int RELEASE_THRESHOLD = 100;
	private SensorManager mSensorManager;
	private Sensor accelerometer;
	private StepDetectorListener mListener;
	private long lastActive = 0;

	public StepDetector(Context context, StepDetectorListener listener) {
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mListener = listener;
	}
	
	public interface StepDetectorListener {
		public void onStep();
	}
	
	private SensorEventListener mSensorEventListener = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			float[] readings = event.values;
			// Calculate norm of acceleration vector
			double acceleration = 0.0;
			for (int i=0; i<3; i++) acceleration += Math.pow(readings[i], 2);
			acceleration = Math.sqrt(acceleration) / 9.8;
//			Log.d(TAG, Double.toString(acceleration));
			if (acceleration > ACCELERATION_THRESHOLD) {
				long time = SystemClock.elapsedRealtime();
				if (time - lastActive > RELEASE_THRESHOLD) {
					mListener.onStep();
				}
				lastActive = time;
			}
		}
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	};
	
	public void resume() {
		mSensorManager.registerListener(mSensorEventListener, accelerometer,
				SensorManager.SENSOR_DELAY_UI);
	}
	
	public void pause() {
		mSensorManager.unregisterListener(mSensorEventListener);
	}
}
