package com.eastagile.android;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.eastagile.android.R;

public class FriendsExplorerActivity extends MapActivity {

	MapView mapView;
	MapController mc;
	GeoPoint p;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// Add zoom view to Google Maps
		mapView = (MapView) findViewById(R.id.myMap);
		LinearLayout zoomLayout = (LinearLayout) findViewById(R.id.zoom);
		View zoomView = mapView.getZoomControls();
		zoomLayout.addView(zoomView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		mapView.displayZoomControls(true);
		mapView.setSatellite(true);
		Location loc = getLocation(this);
		double lat = loc.getLatitude();
		double lng = loc.getLongitude();
		mc = mapView.getController();
		p = new GeoPoint((int) (lat * 1E6), (int) (lng * 1E6));
		mc.animateTo(p);
		mc.setZoom(17);
		// ---Add a location marker---
		MapOverlay mapOverlay = new MapOverlay();
		List<Overlay> listOfOverlays = mapView.getOverlays();
		listOfOverlays.clear();
		listOfOverlays.add(mapOverlay);
		mapView.invalidate();
	}

	class MapOverlay extends com.google.android.maps.Overlay {
		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
			super.draw(canvas, mapView, shadow);
			Point screenPts = new Point();
			mapView.getProjection().toPixels(p, screenPts);
			Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.pushpin);
			canvas.drawBitmap(bmp, screenPts.x, screenPts.y - 50, null);
			return true;
		}
	}

	public static Location getLocation(Context ctx) {
		LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = lm.getProviders(true);
		Location l = null;

		for (int i = providers.size() - 1; i >= 0; i--) {
			l = lm.getLastKnownLocation(providers.get(i));
			if (l != null)
				break;
		}
		return l;
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

}