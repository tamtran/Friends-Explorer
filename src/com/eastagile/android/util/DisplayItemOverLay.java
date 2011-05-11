package com.eastagile.android.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;

import com.eastagile.android.FriendsExplorerActivity;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

public class DisplayItemOverLay extends com.google.android.maps.Overlay {
	static FriendsExplorerActivity acti;
	GeoPoint geo;
	Bitmap bitmap;

	public DisplayItemOverLay(FriendsExplorerActivity friendsExplorerActivity, GeoPoint geoPoint, Bitmap bit) {
		acti = friendsExplorerActivity;
		geo = geoPoint;
		bitmap = bit;
	}

	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
		super.draw(canvas, mapView, shadow);
		Point screenPts = new Point();
		mapView.getProjection().toPixels(geo, screenPts);
		canvas.drawBitmap(bitmap, screenPts.x, screenPts.y - 50, null);
		return true;
	}
}