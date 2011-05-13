package com.eastagile.android.service;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.eastagile.android.FriendsExplorerActivity;
import com.eastagile.android.R;
import com.eastagile.android.util.util;

public class AlertService extends Service implements LocationListener {
	private static final String TAG = "Test";
	private static final int HELLO_ID = 1;
	private static final int COP_NOTIFY_ID = 2;
	private static final int TRAFFIC_NOTIFY_ID = 3;
	private Timer timer;
	private LocationManager locationManager;
	private double myCurrentLat;
	private double myCurrentLong;
	static final String HOST = "http://friendexplorer.heroku.com";
	// static final String HOST = "http://192.168.25.174:3000";
	Notification notification;
	NotificationManager mNotificationManager;
	PendingIntent contentIntent;
	FriendsExplorerActivity friendActivity;
	private String[] arrayStringType;
	private String[] arrayStringLong;
	private String[] arrayStringLat;
	private String uuid;

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		String ns = Context.NOTIFICATION_SERVICE;
		mNotificationManager = (NotificationManager) getSystemService(ns);
		long when = System.currentTimeMillis();
		notification = new Notification(R.drawable.icon, "FriendsExplorer Notification", when);
		Intent notificationIntent = new Intent(this, FriendsExplorerActivity.class);
		contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.defaults |= Notification.DEFAULT_LIGHTS;
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 10f, this);
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			myCurrentLat = loc.getLatitude();
			myCurrentLong = loc.getLongitude();
			notification.setLatestEventInfo(getApplicationContext(), "FriendsExplorer notification", "FriendsExplorer is running!", contentIntent);
		} else {
			notification.setLatestEventInfo(getApplicationContext(), "FriendsExplorer notification", "You shoule enable GPS!", contentIntent);
		}
		mNotificationManager.notify(HELLO_ID, notification);
		final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
		final String tmDevice, tmSerial, androidId;
		tmDevice = "" + tm.getDeviceId();
		tmSerial = "" + tm.getSimSerialNumber();
		androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
		UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
		uuid = deviceUuid.toString();
	}

	@Override
	public IBinder onBind(Intent intent) {
		logging("Service is on Bind");
		return null;
	}

	protected void checkAlertFromServer() {
		logging("Service is on checkAlertFromServer");
		String url = HOST + "/alert/get?name=" + uuid;
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet method = new HttpGet(url);
			HttpResponse response = client.execute(method);
			String stringResponse = util.getResponseBody(response);
			logging(stringResponse);
			String[] items = stringResponse.split("<item>");
			int numberLocation = items.length - 1;
			if (numberLocation > 0) {
				arrayStringType = new String[numberLocation];
				arrayStringLong = new String[numberLocation];
				arrayStringLat = new String[numberLocation];
				for (int i = 1; i < numberLocation + 1; i++) {
					arrayStringType[i - 1] = util.getValueByTag(items[i], "Type");
					arrayStringLong[i - 1] = util.getValueByTag(items[i], "Long");
					arrayStringLat[i - 1] = util.getValueByTag(items[i], "Lat");
				}
			}
			Location locationOfMine = new Location("");
			locationOfMine.setLatitude(myCurrentLat);
			locationOfMine.setLongitude(myCurrentLong);
			Boolean cop = false;
			Boolean traffic = false;
			if (arrayStringLong != null) {
				logging("" + arrayStringLong.length);
				for (int i = 0; i < arrayStringLong.length; i++) {
					double lat = Double.parseDouble(arrayStringLat[i]);
					double lng = Double.parseDouble(arrayStringLong[i]);
					Location locationOfCop = new Location("");
					locationOfCop.setLatitude(lat);
					locationOfCop.setLongitude(lng);
					float distance_meters = locationOfMine.distanceTo(locationOfCop);
					if (distance_meters <= 2000) {
						if (arrayStringType[i].toLowerCase().equals("cop")) {
							cop = true;
						} else {
							traffic = true;
						}
					}

				}
			}
			if (cop) {
				notification.setLatestEventInfo(getApplicationContext(), "FriendsExplorer COP", "You're near a COP!", contentIntent);
				mNotificationManager.notify(COP_NOTIFY_ID, notification);
			}
			if (traffic) {
				notification.setLatestEventInfo(getApplicationContext(), "FriendsExplorer TRAFFIC", "You're near a TRAFFIC!", contentIntent);
				mNotificationManager.notify(TRAFFIC_NOTIFY_ID, notification);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			// throw new RuntimeException(ex);
		}
	}

	private TimerTask updateTask = new TimerTask() {
		@Override
		public void run() {
			logging("Timer task doing work");
			if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				checkAlertFromServer();
			} else {
				notification.setLatestEventInfo(getApplicationContext(), "FriendsExplorer notification", "You shoule enable GPS!", contentIntent);
				mNotificationManager.notify(HELLO_ID, notification);
			}
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		logging("Service creating");
		timer = new Timer("GetAlertFromTimer");
		timer.schedule(updateTask, 10000L, 60 * 1000L);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		logging("Service destroying");
		timer.cancel();
		timer = null;
	}

	public static void logging(String input) {
		Log.d(TAG, input);
	}

	@Override
	public void onLocationChanged(Location arg0) {
		myCurrentLat = arg0.getLatitude();
		myCurrentLong = arg0.getLongitude();
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub

	}
}
