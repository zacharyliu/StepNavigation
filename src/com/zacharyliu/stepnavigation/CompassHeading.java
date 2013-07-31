package com.zacharyliu.stepnavigation;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

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
	private final int AVERAGE_SIZE = 5;
	private int count = 0;
	private final double TWOPI = 2*Math.PI;
	
	private CompassHeadingQueue queue = new CompassHeadingQueue();

	public CompassHeading(Context context, CompassHeadingListener listener) {
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		mListener = listener;
	}
	
	public interface CompassHeadingListener {
		public void onHeadingUpdate(double heading);
	}
	
	private SensorEventListener mSensorEventListener = new SensorEventListener() {
		private double azimuth;
		private boolean azimuthReady;

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}

		// TODO: fix average function to work with 0-360 wrapped values
//		private double average() {
//			double sum = 0.0;
//			for (int i=0; i<history.length; i++) {
//				sum += history[i];
//			}
//			return sum / history.length;
//		}
		
		@Override
		public void onSensorChanged(SensorEvent event) {
			azimuth = -event.values[2];
			if (!azimuthReady) azimuthReady = true;
			queue.update(azimuth);
		}
	};
	
	private class CompassHeadingQueue {
		private LinkedList<Double> averagesX = new LinkedList<Double>();
		private LinkedList<Double> averagesY = new LinkedList<Double>();
		private final double ALPHA = 0.2;
		private final double[] weights = {ALPHA, 1-ALPHA};
		private final int SIZE = 20;
		
		public void update(double headingRadians) {
			add(headingRadians);
			double result = Math.toDegrees(getMidpoint());
			mListener.onHeadingUpdate(result);
			
			if (++count == AVERAGE_SIZE) {
				count = 0;
				Log.v(TAG, String.format("Compass: %.2f degrees", result));
			}
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
	
	public static double circularMean(double[] nums) {
		double sumX = 0.0;
		double sumY = 0.0;
		
		for (int i=0; i<nums.length; i++) {
			sumX += Math.cos(nums[i]);
			sumY += Math.sin(nums[i]);
		}
		
		return Math.atan2(sumY/nums.length, sumX/nums.length);
	}
	
	public static double circularWeightedMean(double[] nums, double[] weights) {
		double sumX = 0.0;
		double sumY = 0.0;
		double sumWeights = 0.0;
		
		for (int i=0; i<nums.length; i++) {
			sumX += Math.cos(nums[i]) * weights[i];
			sumY += Math.sin(nums[i]) * weights[i];
			sumWeights += weights[i];
		}
		
		return Math.atan2(sumY/sumWeights, sumX/sumWeights);
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
