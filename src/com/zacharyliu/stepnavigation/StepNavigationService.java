package com.zacharyliu.stepnavigation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVWriter;

import com.zacharyliu.stepnavigation.CompassHeading.CompassHeadingListener;
import com.zacharyliu.stepnavigation.GpsBearing.GpsBearingListener;
import com.zacharyliu.stepnavigation.StepDetector.StepDetectorListener;

public class StepNavigationService extends Service {

	// Constants
	private final String TAG = "StepNavigationService";
	private final double STEP_LENGTH_METERS = 0.8; // http://www.wolframalpha.com/input/?i=step+length+in+meters
	private final double EARTH_RADIUS_KILOMETERS = 6371;
	private final int HISTORY_COUNT = 10;
	private final double CALIBRATION_THRESHOLD = 10.0;

	// Object storage
	private List<ICustomSensor> sensors = new ArrayList<ICustomSensor>();
	private final IBinder mBinder = new StepNavigationBinder();
	private List<StepNavigationListener> listeners = new ArrayList<StepNavigationListener>();
	private IGpsBearing gps;

	// State variables
	private boolean calibrated = false;
	private boolean gpsReady = false;
	private double mHeading = 0.0;
	private double mBearing = 0.0;
	private Queue<Double> history = new LinkedList<Double>();
	private double correctionFactor = 0.0;
	private double realHeading = 0.0;
	private double[] currentLoc;
	private CSVWriter writer;

	// Public interfaces, classes, and methods
	public interface StepNavigationListener {
		public void onLocationUpdate(double latitude, double longitude);
	}

	public class StepNavigationBinder extends Binder {
		public StepNavigationService getService() {
			return StepNavigationService.this;
		}
	}

	public void register(StepNavigationListener listener) {
		Log.d(TAG, "Adding listener " + listener.toString());
		listeners.add(listener);
	}

	public void unregister(StepNavigationListener listener) {
		Log.d(TAG, "Removing listener " + listener.toString());
		listeners.remove(listener);
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "Service bound");
		sensors.add(new CompassHeading(this, new CompassHeadingListener() {
			@Override
			public void onHeadingUpdate(double heading) {
				mHeading = heading;
				writer.writeNext(new String[] {Long.toString(System.currentTimeMillis()), Double.toString(heading)});
				onDirectionUpdate();
			}
		}));
		gps = new GpsBearing(this, new GpsBearingListener() {
			@Override
			public void onBearingUpdate(double bearing) {
				mBearing = bearing;
				onDirectionUpdate();
			}

			@Override
			public void onLocationUpdate(double[] loc) {
				Log.d(TAG, "GPS location updated");
				gpsReady = true;
				if (!calibrated) {
					onNewLocation(loc);
				}
			}
		});
		sensors.add(gps);
		sensors.add(new StepDetector(this, new StepDetectorListener() {
			@Override
			public void onStep() {
				StepNavigationService.this.onStep();
			}
		}));

		for (ICustomSensor sensor : sensors) {
			sensor.resume();
		}
		
		String filename = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CompassHeading" + Long.toString(System.currentTimeMillis()) + ".csv";
		Toast.makeText(this, "Logging to: " + filename, Toast.LENGTH_SHORT).show();
		try {
			writer = new CSVWriter(new FileWriter(filename));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return mBinder;
	}

	@Override
	public void onDestroy() {
		for (ICustomSensor sensor : sensors) {
			sensor.pause();
		}
		super.onDestroy();
	}

	private boolean calibrate() {
		Log.d(TAG, "Calibration begin");

		// If history is not filled completely, exit
		if (history.size() != HISTORY_COUNT) {
			Log.d(TAG, "Not enough history entries");
			return false;
		}

		// Take standard deviation
		double sum = 0.0;
		for (double item : history) {
			sum += item;
		}
		double avg = sum / HISTORY_COUNT;
		double var = 0.0;
		for (double item : history) {
			var += Math.pow(item - avg, 2);
		}
		var /= HISTORY_COUNT - 1;
		double std = Math.sqrt(var);

		Log.d(TAG, "Standard deviation: " + Double.toString(std)
				+ " (threshold: " + Double.toString(CALIBRATION_THRESHOLD)
				+ ")");

		// If standard deviation above threshold, exit
		if (std > CALIBRATION_THRESHOLD)
			return false;

		// Set factors and return
		correctionFactor = avg;
		Log.d(TAG, "Calibration succeeded");
		return true;
	}

	// Private methods
	private void onDirectionUpdate() {
//		Log.v(TAG, "Compass heading: " + Double.toString(mHeading));
		realHeading = mHeading + correctionFactor;
		if (realHeading > 360)
			realHeading -= 360;
		if (realHeading < 0)
			realHeading += 360;
	}

	private void onStep() {
		Log.d(TAG, "step");
		Log.d(TAG, String.format("Compass: %.3f", mHeading));

		// If GPS is on, add to the calibration history
		if (gpsReady && mBearing != 0.0) {
			
			// Add the current heading difference to the list
			double diff = mBearing - mHeading;
			history.add(diff);
			while (history.size() > HISTORY_COUNT) {
				history.remove();
			}
		} else {
			Log.d(TAG, "GPS not ready");
		}

		// TODO: Determine if recalibration is needed
		if (calibrated) {
			// phone removed, orientation changed, etc.
			// if calibrationNeeded
			//     gps.on()
			//     gpsReady = false
			//     calibrated = false
		}

		// Try to calibrate if not yet calibrated
		if (!calibrated && calibrate()) {
			// Calibration was successful
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(200);
			calibrated = true;
			gps.off();
		}

		// Calculate new location if possible
		if (calibrated && currentLoc != null) {
			Log.d(TAG, String.format("GPS %.2f | compass %.2f | calibrated %.2f", mBearing, mHeading, realHeading));
			calculateNewLocation();
		} else {
			Log.d(TAG, String.format("GPS %.2f | compass %.2f", mBearing, mHeading));
		}
	}

	private void calculateNewLocation() {
		// Calculate new location
		// Formula: http://www.movable-type.co.uk/scripts/latlong.html#destPoint
		double lat1 = Math.toRadians(currentLoc[0]); // starting latitude
		double lon1 = Math.toRadians(currentLoc[1]); // starting longitude
		double brng = Math.toRadians(realHeading); // bearing
		double d = STEP_LENGTH_METERS / 1000; // distance traveled
		double R = EARTH_RADIUS_KILOMETERS; // radius of Earth
		double lat2 = Math.asin(Math.sin(lat1) * Math.cos(d / R)
				+ Math.cos(lat1) * Math.sin(d / R) * Math.cos(brng));
		double lon2 = lon1
				+ Math.atan2(Math.sin(brng) * Math.sin(d / R) * Math.cos(lat1),
						Math.cos(d / R) - Math.sin(lat1) * Math.sin(lat2));
		lat2 = Math.toDegrees(lat2);
		lon2 = Math.toDegrees(lon2);

		Log.v(TAG, "delta lat: " + Double.toString(lat2 - currentLoc[0])
				+ ", delta lon: " + Double.toString(lon2 - currentLoc[1]));

		double[] newLoc = { lat2, lon2 };
		onNewLocation(newLoc);
	}

	private void onNewLocation(double[] loc) {
		currentLoc = loc;
		for (StepNavigationListener listener : listeners) {
			listener.onLocationUpdate(loc[0], loc[1]);
		}
	}

}
