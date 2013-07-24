package com.zacharyliu.stepnavigation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.zacharyliu.stepnavigation.CompassHeading.CompassHeadingListener;
import com.zacharyliu.stepnavigation.GpsBearing.GpsBearingListener;
import com.zacharyliu.stepnavigation.StepDetector.StepDetectorListener;

public class StepNavigationService extends Service {

	private final String TAG = "StepNavigationService";
	private final double STEP_LENGTH_METERS = 0.8; // http://www.wolframalpha.com/input/?i=step+length+in+meters
	private final double EARTH_RADIUS_KILOMETERS = 6371;
	private double mHeading = 0.0;
	private double mBearing = 0.0;
	private boolean isCalibrating;
	private final int HISTORY_COUNT = 30;
	private Queue<Double> history = new LinkedList<Double>();
	private double historyAvg = 0.0;
	private List<ICustomSensor> sensors = new ArrayList<ICustomSensor>();
	private double corrected;
	private double[] currentLoc = {40.468184, -74.445385};
	private final IBinder mBinder = new StepNavigationBinder();
	private List<StepNavigationListener> listeners = new ArrayList<StepNavigationListener>();
	
	public interface StepNavigationListener {
		public void onLocationUpdate(double latitude, double longitude);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "Service bound");
		sensors.add(new CompassHeading(this, new CompassHeadingListener() {
			@Override
			public void onHeadingUpdate(double heading) {
				mHeading = heading;
				onDirectionUpdate();
			}
		}));
		sensors.add(new GpsBearing(this, new GpsBearingListener() {
			@Override
			public void onBearingUpdate(double bearing) {
				mBearing = bearing;
				onDirectionUpdate();
			}

			@Override
			public void onLocationUpdate(double[] loc) {
				if (isCalibrating)
					callListeners(loc);
			}
		}));
		sensors.add(new StepDetector(this, new StepDetectorListener() {
			@Override
			public void onStep() {
				StepNavigationService.this.onStep();
			}
		}));
		
		for (ICustomSensor sensor : sensors) {
			sensor.resume();
		}
		
		return mBinder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		for (ICustomSensor sensor : sensors) {
			sensor.pause();
		}
	}
	
	private void onDirectionUpdate() {
		if (isCalibrating) {
			double diff = mBearing - mHeading;
			history.add(diff);
			while (history.size() > HISTORY_COUNT) {
				history.remove();
			}
			double sum = 0.0;
			for (double item : history) {
				sum += item;
			}
			historyAvg = sum / history.size();
		}

		corrected = mHeading + historyAvg;
		if (corrected > 360)
			corrected -= 360;
		if (corrected < 0)
			corrected += 360;
	}

	private void onStep() {
		Log.d(TAG, "step");
		
		if (currentLoc == null || isCalibrating)
			return;

		// Calculate new location
		// Formula: http://www.movable-type.co.uk/scripts/latlong.html#destPoint
		// (angles in radians)
		double lat1 = Math.toRadians(currentLoc[0]); // starting latitude
		double lon1 = Math.toRadians(currentLoc[1]); // starting longitude
		double brng = Math.toRadians(corrected); // bearing
		double d = STEP_LENGTH_METERS / 1000; // distance traveled
		double R = EARTH_RADIUS_KILOMETERS; // radius of Earth
		double lat2 = Math.asin(Math.sin(lat1) * Math.cos(d / R)
				+ Math.cos(lat1) * Math.sin(d / R) * Math.cos(brng));
		double lon2 = lon1
				+ Math.atan2(Math.sin(brng) * Math.sin(d / R) * Math.cos(lat1),
						Math.cos(d / R) - Math.sin(lat1) * Math.sin(lat2));
		lat2 = Math.toDegrees(lat2);
		lon2 = Math.toDegrees(lon2);
		
		Log.v(TAG, "delta lat: " + Double.toString(lat2-currentLoc[0]) + ", delta lon: " + Double.toString(lon2-currentLoc[1])); 
		
		double[] newLoc = {lat2, lon2};
		currentLoc = newLoc;
		callListeners(newLoc);
	}

	private void callListeners(double[] loc) {
		for (StepNavigationListener listener : listeners) {
			listener.onLocationUpdate(loc[0], loc[1]);
		}
	}
	
	public class StepNavigationBinder extends Binder {
		public StepNavigationService getService() {
			return StepNavigationService.this;
		}
	}
	
	public void register(StepNavigationListener listener) {
		listeners.add(listener);
	}
	
	public void unregister(StepNavigationListener listener) {
		listeners.remove(listener);
	}

}
