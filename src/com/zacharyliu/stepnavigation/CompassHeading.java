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
	
	private SensorEventListener mSensorEventListener = new SensorEventListener() {
		private float[] accelReadings;
		private float[] magnetReadings;
		private double azimuth;
		private boolean azimuthReady;
		private boolean accelReady = false;
		private boolean magnetReady = false;
		final private int AVERAGE_SIZE = 3;
		private double[] history = new double[AVERAGE_SIZE];
		private int historyIndex = 0;
		private float y;
		private float z;

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}

		private double average() {
			double sum = 0.0;
			for (int i=0; i<history.length; i++) {
				sum += history[i];
			}
			return sum / history.length;
		}
		
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
					y = accelReadings[1];
					z = accelReadings[2];
					accelReadings[2] = -y;
					accelReadings[1] = z;
					accelReady = true;
					break;
				case Sensor.TYPE_MAGNETIC_FIELD:
					magnetReadings = event.values.clone();
					y = magnetReadings[1];
					z = magnetReadings[2];
					magnetReadings[2] = -y;
					magnetReadings[1] = z;
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
//					z = accelReadings[2];
//					if (z < 0) {
//						Log.v(TAG, "Flip");
//						azimuth += 180;
//					}
					if (azimuth > 360) {
						azimuth -= 360;
					} else if (azimuth < 0) {
						azimuth += 360;
					}
					if (!azimuthReady) azimuthReady = true;
					history[historyIndex] = azimuth;
					if (++historyIndex == AVERAGE_SIZE) historyIndex = 0;
					double average = average();
					mListener.onHeadingUpdate(azimuth);
					Log.v(TAG, String.format("Compass: %.2f", azimuth));
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
