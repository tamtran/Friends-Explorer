package com.eastagile.android;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.eastagile.android.R;

public class FriendsExplorerActivity extends MapActivity implements LocationListener {

	static final String tag = "Test"; // for Log
	static MapView mapView;
	static MapController mc;
	static GeoPoint p;
	static LocationManager lm;
	StringBuilder sb;
	int noOfFixes = 0;
	double lat;
	double lng;
	String uuid;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mapView = (MapView) findViewById(R.id.myMap);
		LinearLayout zoomLayout = (LinearLayout) findViewById(R.id.zoom);
		View zoomView = mapView.getZoomControls();
		zoomLayout.addView(zoomView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		Location loc = null;
		loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (loc == null)
			loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		lat = loc.getLatitude();
		lng = loc.getLongitude();
//		TelephonyManager tManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
//		uuid = tManager.getDeviceId();
		final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

    final String tmDevice, tmSerial, tmPhone, androidId;
    tmDevice = "" + tm.getDeviceId();
    tmSerial = "" + tm.getSimSerialNumber();
    androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

    UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
    uuid = deviceUuid.toString();
		logging("UUID " + uuid);
		getStartupData();
		displayCurrentLocation();
		mapView.displayZoomControls(true);
		mapView.setSatellite(true);
		mapView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				return onTouchEvent((MapView) arg0, arg1);
			}
		});
		mapView.invalidate();
	}

	public void getStartupData(){
		HttpClient httpclient = new DefaultHttpClient();
//		String url = "http://friendexplorer.heroku.com/user_location/update?name="+uuid+"&long="+Double.toString(lng)+"&lat="+Double.toString(lat);
		String url = "http://192.168.25.174:3000/user_location/update?name="+uuid+"&long="+Double.toString(lng)+"&lat="+Double.toString(lat);
		logging(url);
		try {
			HttpClient client = new DefaultHttpClient();
      HttpGet method = new HttpGet(url);
      @SuppressWarnings("unused")
      HttpResponse response = client.execute(method);
		} catch (IOException e) {
			logging("getStartupData IOException");
		}
	}

	@Override
	protected void onResume() {
		/*
		 * onResume is is always called after onStart, even if the app hasn't been
		 * paused
		 * 
		 * add location listener and request updates every 1000ms or 10m
		 */
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, this);
		super.onResume();
	}

	@Override
	protected void onPause() {
		/* GPS, as it turns out, consumes battery like crazy */
		lm.removeUpdates(this);
		super.onResume();
	}

	@Override
	public void onLocationChanged(Location location) {
		Log.v(tag, "Location Changed");
		sb = new StringBuilder(512);
		noOfFixes++;
		sb.append("No. of Fixes: ");
		sb.append(noOfFixes);
		sb.append('\n');
		sb.append('\n');
		sb.append("Londitude: ");
		sb.append(location.getLongitude());
		sb.append('\n');
		sb.append("Latitude: ");
		sb.append(location.getLatitude());
		sb.append('\n');
		sb.append("Altitiude: ");
		sb.append(location.getAltitude());
		sb.append('\n');
		sb.append("Accuracy: ");
		sb.append(location.getAccuracy());
		sb.append('\n');
		sb.append("Timestamp: ");
		sb.append(location.getTime());
		sb.append('\n');
	}

	public boolean onTouchEvent(MapView mapView, MotionEvent event) {
		if (event.getAction() == 1) {
			GeoPoint p = mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());
			Geocoder geoCoder = new Geocoder(getBaseContext(), Locale.getDefault());
			try {
				List<Address> addresses = geoCoder.getFromLocation(p.getLatitudeE6() / 1E6, p.getLongitudeE6() / 1E6, 1);
				String add = "";
				if (addresses.size() > 0) {
					for (int i = 0; i < addresses.get(0).getMaxAddressLineIndex(); i++)
						add += addresses.get(0).getAddressLine(i) + "\n";
				}

				Toast.makeText(getBaseContext(), add, Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		} else
			return false;
	}

	@Override
	public void onProviderDisabled(String provider) {
		/* this is called if/when the GPS is disabled in settings */
		Log.v(tag, "Disabled");

		/* bring up the GPS settings */
		Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.v(tag, "Enabled");
		Toast.makeText(this, "GPS Enabled", Toast.LENGTH_SHORT).show();

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		/* This is called when the GPS status alters */
		switch (status) {
		case LocationProvider.OUT_OF_SERVICE:
			Log.v(tag, "Status Changed: Out of Service");
			Toast.makeText(this, "Status Changed: Out of Service", Toast.LENGTH_SHORT).show();
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			Log.v(tag, "Status Changed: Temporarily Unavailable");
			Toast.makeText(this, "Status Changed: Temporarily Unavailable", Toast.LENGTH_SHORT).show();
			break;
		case LocationProvider.AVAILABLE:
			Log.v(tag, "Status Changed: Available");
			Toast.makeText(this, "Status Changed: Available", Toast.LENGTH_SHORT).show();
			break;
		}
	}

	@Override
	protected void onStop() {
		/*
		 * may as well just finish since saving the state is not important for this
		 * toy app
		 */
		finish();
		super.onStop();
	}

	public void displayCurrentLocation() {
		mc = mapView.getController();
		p = new GeoPoint((int) (lat * 1E6), (int) (lng * 1E6));
		mc.animateTo(p);
		mc.setZoom(18);
		MapOverlay mapOverlay = new MapOverlay(this);
		List<Overlay> listOfOverlays = mapView.getOverlays();
		listOfOverlays.clear();
		listOfOverlays.add(mapOverlay);
	}

	static class MapOverlay extends com.google.android.maps.Overlay {
		static FriendsExplorerActivity acti;

		public MapOverlay(FriendsExplorerActivity friendsExplorerActivity) {
			acti = friendsExplorerActivity;
		}

		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
			super.draw(canvas, mapView, shadow);
			Point screenPts = new Point();
			mapView.getProjection().toPixels(p, screenPts);
			Bitmap bmp = BitmapFactory.decodeResource(acti.getResources(), R.drawable.pushpin);
			canvas.drawBitmap(bmp, screenPts.x, screenPts.y - 50, null);
			return true;
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		MapController mc = mapView.getController();
		switch (keyCode) {
		case KeyEvent.KEYCODE_I:
			mc.zoomIn();
			break;
		case KeyEvent.KEYCODE_O:
			mc.zoomOut();
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	public static void logging(String input) {
		Log.d(tag, input);
	}

}