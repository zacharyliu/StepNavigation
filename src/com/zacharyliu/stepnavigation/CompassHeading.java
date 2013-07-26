package com.zacharyliu.stepnavigation;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class CompassHeading implements ICustomSensor {
	
	private final String TAG = "CompassHeading";
	private SensorManager mSensorManager;
	private Sensor accelerometer;
	private Sensor magnetometer;
	private CompassHeadingListener mListener;

	public CompassHeading(Context context, CompassHeadingListener listener) {
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_GRAVITY);
		magnetometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mListener = listener;
	}
	
	public interface CompassHeadingListener {
		public void onHeadingUpdate(double heading);
	}
	
	private int count = 0;
	private final int COUNT_MAX = 5;
	private SensorEventListener mSensorEventListener = new SensorEventListener() {
		private float[] accelReadings;
		private float[] magnetReadings;
		private double azimuth;
		private double z;
		private boolean azimuthReady;
		private boolean accelReady = false;
		private boolean magnetReady = false;

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}

		@Override
		public void onSensorChanged(SensorEvent event) {
//			if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//				float[] readings = event.values;
//				double angle = Math.toDegrees(Math.atan(readings[2] / readings[0]));
//				angle = 360 - (angle + 90);
//				mListener.onHeadingUpdate(angle);
//			}
			
			switch (event.sensor.getType()) {
				case Sensor.TYPE_GRAVITY:
					accelReadings = event.values.clone();
					accelReady = true;
					break;
				case Sensor.TYPE_MAGNETIC_FIELD:
					magnetReadings = event.values.clone();
					magnetReady = true;
					break;
			}
			if (accelReady == true && magnetReady == true) {
				float[] R = new float[9];
				float[] I = new float[9];
				boolean success = SensorManager.getRotationMatrix(R, I, accelReadings, magnetReadings);
				if (success) {
					float[] values = new float[3];
					SensorManager.getOrientation(R, values);
					azimuth = Math.toDegrees(values[0]);
//					roll = Math.toDegrees(values[2]);
//					if (roll > 90 || roll < -90) {
//						// Upside down, flip azimuth
//						azimuth += 180;
//					}
					count++;
					z = accelReadings[2];
					if (z < 0) {
						if (count == COUNT_MAX)
							Log.d(TAG, "Flip");
						azimuth += 180;
					}
					if (azimuth < 0) {
						azimuth += 360;
					} else if (azimuth > 360) {
						azimuth -= 360;
					}
					if (!azimuthReady) azimuthReady = true;
					mListener.onHeadingUpdate(azimuth);
					if (count == COUNT_MAX) {
						Log.d(TAG, Double.toString(azimuth));
						count = 0;
					}
				}
				
				// Require a set of new values for each sensor
				accelReady = false;
				magnetReady = false;
			}
		}
	};
	
	public void resume() {
		mSensorManager.registerListener(mSensorEventListener, accelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(mSensorEventListener, magnetometer,
				SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	public void pause() {
		mSensorManager.unregisterListener(mSensorEventListener);
	}
}
