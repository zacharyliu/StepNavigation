package com.zacharyliu.stepnavigation;

import java.util.Collections;
import java.util.LinkedList;

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
	private final int AVERAGE_SIZE = 10;
	private int count = 0;
	
	private CompassHeadingQueue queue = new CompassHeadingQueue();

	public CompassHeading(Context context, CompassHeadingListener listener) {
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		mListener = listener;
	}
	
	public interface CompassHeadingListener {
		public void onHeadingUpdate(double heading, double headingRaw);
	}
	
	private SensorEventListener mSensorEventListener = new SensorEventListener() {
		private double angle;
		private boolean azimuthReady;

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}
		
		@Override
		public void onSensorChanged(SensorEvent event) {
			angle = 2.0 * Math.asin(event.values[2]);
			if (!azimuthReady) azimuthReady = true;
			queue.update(angle);
		}
	};
	
	private class CompassHeadingQueue {
		private LinkedList<Double> averagesX = new LinkedList<Double>();
		private LinkedList<Double> averagesY = new LinkedList<Double>();
		private final double ALPHA = 0.1;
		private final int SIZE = 20;
		
		public void update(double rawAngle) {
			add(rawAngle);
			double newAngle = getMidpoint();
			mListener.onHeadingUpdate(angleToHeading(newAngle), angleToHeading(rawAngle));
			
			if (++count == AVERAGE_SIZE) {
				count = 0;
				Log.v(TAG, String.format("Compass: %.2f degrees", angleToHeading(newAngle)));
			}
		}
		
		/**
		 * Converts an angle in range [-pi, pi] to heading in range [0,360]
		 * Note: does not perform axes rotation, still good for relative angles
		 * 
		 * @param angle (radians)
		 * @return heading (degrees)
		 */
		private double angleToHeading(double angle) {
			return Math.toDegrees(-angle + Math.PI);
		}
		
		public void add(double item) {
			double itemX = Math.cos(item);
			double itemY = Math.sin(item);
			
			if (!averagesX.isEmpty()) {
				itemX = ALPHA * itemX + (1-ALPHA) * averagesX.getLast();
				itemY = ALPHA * itemY + (1-ALPHA) * averagesY.getLast();
			}
			
			averagesX.add(itemX);
			averagesY.add(itemY);
			
			trim(averagesX);
			trim(averagesY);
		}
		
		private void trim(LinkedList<?> list) {
			while (list.size() > SIZE)
				list.remove();
		}
		
		public double getMidpoint() {
			double maxX = Collections.max(averagesX);
			double maxY = Collections.max(averagesY);
			double minX = Collections.min(averagesX);
			double minY = Collections.min(averagesY);
			
			double midX = (maxX - minX) / 2 + minX;
			double midY = (maxY - minY) / 2 + minY;
			
			return Math.atan2(midY, midX);
		}
	}
	
	public void resume() {
		mSensorManager.registerListener(mSensorEventListener, accelerometer,
				SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(mSensorEventListener, magnetometer,
				SensorManager.SENSOR_DELAY_UI);
	}
	
	public void pause() {
		mSensorManager.unregisterListener(mSensorEventListener);
	}
}
