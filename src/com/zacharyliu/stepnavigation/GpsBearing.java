package com.zacharyliu.stepnavigation;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class GpsBearing implements ICustomSensor, IGpsBearing {

	private LocationManager mLocationManager;
	private GpsBearingListener mListener;
	private boolean enabled = true;

	public GpsBearing(Context context, GpsBearingListener listener) {
		mLocationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		mListener = listener;
	}

	protected GpsBearing() {
	}

	public interface GpsBearingListener {
		public void onBearingUpdate(double bearing);

		public void onLocationUpdate(double[] loc);
	}

	private LocationListener mLocationListener = new LocationListener() {
		private float bearing;

		@Override
		public void onLocationChanged(Location location) {
			bearing = location.getBearing();
			mListener.onBearingUpdate(bearing);

			double[] loc = { location.getLatitude(), location.getLongitude() };
			mListener.onLocationUpdate(loc);
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

	public void resume() {
		if (enabled)
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
	}

	public void pause() {
		if (enabled)
			mLocationManager.removeUpdates(mLocationListener);
	}

	public void on() {
		if (enabled)
			return;
		enabled = true;
		resume();
	}

	public void off() {
		if (!enabled)
			return;
		enabled = false;
		pause();
	}

}
