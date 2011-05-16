package com.eastagile.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.eastagile.android.service.AlertService;
import com.eastagile.android.util.DisplayItemOverLay;
import com.eastagile.android.util.util;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class FriendsExplorerActivity extends MapActivity implements LocationListener {
	int REQUEST_GPS_CODE = 0;
	static final String tag = "Test"; // for Log
	public static final String PREFERENCE_NAME = "Preference Setting for Friends Explorer"; // for
	                                                                                        // Log
//	static final String HOST = "http://friendexplorer.heroku.com";
	 static final String HOST = "http://192.168.25.174:3000";
	static MapView mapView;
	static MapController mapController;
	static GeoPoint myCurrentGeoPoint;
	static LocationManager locationManager;
	StringBuilder sb;
	int noOfFixes = 0;
	double myCurrentLat;
	double myCurrentLong;
	String uuid;
	String[] arrayStringName;
	String[] arrayStringLong;
	String[] arrayStringLat;
	List<Overlay> listOfOverlays;
	DisplayLocationTask displayLocationTask;
	Boolean continueRunning = true;
	private static final int SETTING = 0;
	private static final int ALERT = 1;
	private static final int ABOUT = 2;
	private static final int QUIT = 3;
	Timer timer;
	SharedPreferences preSetting;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// launchGPSOptions();
		mapView = (MapView) findViewById(R.id.myMap);
		LinearLayout zoomLayout = (LinearLayout) findViewById(R.id.zoom);
		View zoomView = mapView.getZoomControls();
		mapView.displayZoomControls(true);
		mapView.setStreetView(true);
		mapController = mapView.getController();
		mapController.setZoom(14);
		mapView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				return onTouchEvent((MapView) arg0, arg1);
			}
		});
		listOfOverlays = mapView.getOverlays();
		zoomLayout.addView(zoomView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
		final String tmDevice, tmSerial, androidId;
		tmDevice = "" + tm.getDeviceId();
		tmSerial = "" + tm.getSimSerialNumber();
		androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
		UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
		uuid = deviceUuid.toString();
		logging("UUID " + uuid);
		preSetting = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
		startService(new Intent(AlertService.class.getName()));
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 10f, this);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 100, 10f, this);
		displayLocationTask = new DisplayLocationTask();
		timer = new Timer("UpdateOtherLocationTimer");
		checkLocationAndAction();
	}

	private void checkLocationAndAction() {
		if ((locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) && (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))&&(haveInternet(this))) {
			Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (loc == null) {
				loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}
			myCurrentLat = loc.getLatitude();
			myCurrentLong = loc.getLongitude();
			timer.schedule(updateTask, 100L, 5 * 60 * 1000L);
		} else {
			buildAlertMessageNoGps();
		}
	}

	private TimerTask updateTask = new TimerTask() {
		@Override
		public void run() {
			if (displayLocationTask != null) {
				displayLocationTask.cancel(true);
			}
			displayLocationTask = new DisplayLocationTask();
			displayLocationTask.execute();
		}
	};

	private void buildAlertMessageNoGps() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Your GPS or WiFi or 3G seems to be disabled, you MUST enable all of them to continue!").setCancelable(false)
		    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
				    launchGPSOptions();
			    }
		    });
		final AlertDialog alert = builder.create();
		alert.show();
	}

	protected void launchGPSOptions() {
		Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivityForResult(intent, REQUEST_GPS_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_GPS_CODE && resultCode == 0) {
			checkLocationAndAction();
		}
	}

	public class DisplayLocationTask extends AsyncTask<Void, Integer, String> {

		@Override
		protected void onProgressUpdate(Integer... progress) {
			updateMyLocation();
		}

		@Override
		public void onPreExecute() {
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		public void onPostExecute(String photoHtml) {
		}

		@Override
		protected String doInBackground(Void... arg0) {
			logging("doInBackground");
			String url = HOST + "/user_location/update?name=" + uuid + "&long=" + Double.toString(myCurrentLong) + "&lat=" + Double.toString(myCurrentLat);
			try {
				HttpClient client = new DefaultHttpClient();
				HttpGet method = new HttpGet(url);
				HttpResponse response = client.execute(method);
				String stringResponse = util.getResponseBody(response);
				String[] items = stringResponse.split("<item>");
				int numberLocation = items.length - 1;
				if (numberLocation > 0) {
					arrayStringName = new String[numberLocation];
					arrayStringLong = new String[numberLocation];
					arrayStringLat = new String[numberLocation];
					for (int i = 1; i < numberLocation + 1; i++) {
						arrayStringName[i - 1] = util.getValueByTag(items[i], "Name");
						arrayStringLong[i - 1] = util.getValueByTag(items[i], "Long");
						arrayStringLat[i - 1] = util.getValueByTag(items[i], "Lat");
					}
				}
				listOfOverlays.clear();
				Bitmap bit = getBitmapFromAsset("pushpinFriend.gif");
				if (arrayStringLong != null) {
					for (int i = 0; i < arrayStringLong.length; i++) {
						double lat = Double.parseDouble(arrayStringLat[i]);
						double lng = Double.parseDouble(arrayStringLong[i]);
						GeoPoint geoPoint = new GeoPoint((int) (lat * 1E6), (int) (lng * 1E6));
						DisplayItemOverLay mapOverlayFriend = new DisplayItemOverLay(FriendsExplorerActivity.this, geoPoint, bit);
						listOfOverlays.add(mapOverlayFriend);
					}
				}
				myCurrentGeoPoint = new GeoPoint((int) (myCurrentLat * 1E6), (int) (myCurrentLong * 1E6));
				bit = getBitmapFromAsset("pushpin.gif");
				DisplayItemOverLay mapOverlayMe = new DisplayItemOverLay(FriendsExplorerActivity.this, myCurrentGeoPoint, bit);
				listOfOverlays.add(mapOverlayMe);
				publishProgress(0);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			return url;
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		myCurrentLat = location.getLatitude();
		myCurrentLong = location.getLongitude();
		myCurrentGeoPoint = new GeoPoint((int) (myCurrentLat * 1E6), (int) (myCurrentLong * 1E6));
		updateMyLocation();
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

	public void updateMyLocation() {
		mapController.animateTo(myCurrentGeoPoint);
		mapView.invalidate();
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
	public void onStatusChanged(String provider, int status, Bundle extras) {
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
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, SETTING, 0, "Setting");
		menu.add(0, ALERT, 0, "Alert");
		menu.add(0, ABOUT, 0, "About");
		menu.add(0, QUIT, 0, "Quit");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case SETTING:
			final SharedPreferences.Editor edSetting = preSetting.edit();
			int itemSelectedSetting = preSetting.getInt("FriendsExplorer-Setting", 0);
			final CharSequence[] alertSetting = { "Enable alert", "Disable alert" };
			AlertDialog.Builder settingAlertDialog = new AlertDialog.Builder(this);
			settingAlertDialog.setTitle("Setting");
			settingAlertDialog.setSingleChoiceItems(alertSetting, itemSelectedSetting, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					dialog.cancel();
					if (item == 0) {

					} else {

					}
					edSetting.putInt("FriendsExplorer-Setting", item);
					edSetting.commit();
				}
			});
			settingAlertDialog.create().show();
			break;
		case ALERT:
			final SharedPreferences.Editor edAlert = preSetting.edit();
			int itemSelectedAlert = preSetting.getInt("FriendsExplorer-Alert", 0);
			final CharSequence[] alertType = { "Traffic", "Cop" };
			AlertDialog.Builder alertTypeDialog = new AlertDialog.Builder(this);
			alertTypeDialog.setTitle("Alert type");
			alertTypeDialog.setSingleChoiceItems(alertType, itemSelectedAlert, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					dialog.cancel();
					sendAlertToServer(alertType[item]);
					edAlert.putInt("FriendsExplorer-Alert", item);
					edAlert.commit();
				}
			});
			alertTypeDialog.create().show();
			break;
		case ABOUT:
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("A product of Esat Agile company \n http://www.eastagile.com ").setCancelable(false)
			    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
				    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
					    dialog.cancel();
				    }
			    });
			builder.create().show();
			break;
		case QUIT:
			finish();
		default:
			break;
		}
		return true;
	}

	protected void sendAlertToServer(CharSequence charSequence) {
		String url = HOST + "/alert/receive?name=" + uuid + "&long=" + Double.toString(myCurrentLong) + "&lat=" + Double.toString(myCurrentLat) + "&type="
		    + charSequence.toString();
		logging(url);
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet method = new HttpGet(url);
			HttpResponse response = client.execute(method);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onDestroy() {
		if (displayLocationTask != null) {
			displayLocationTask.cancel(true);
		}
		timer.cancel();
		timer = null;
		super.onDestroy();
	}

	private Bitmap getBitmapFromAsset(String strName) throws IOException {
		AssetManager assetManager = getAssets();
		InputStream istr = assetManager.open(strName);
		Bitmap bitmap = BitmapFactory.decodeStream(istr);
		return bitmap;
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

	@Override
	public void onProviderDisabled(String s) {
	}

	@Override
	public void onProviderEnabled(String s) {
	}

	public SharedPreferences getSharedPreference() {
		return this.preSetting;
	}

	public static boolean haveInternet(Context ctx) {
//		NetworkInfo info = (NetworkInfo) ((ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
//		if (info == null || !info.isConnected()) {
//			return false;
//		}
//		if (info.isRoaming()) {
//			return false;
//		}
		return true;
	}

}