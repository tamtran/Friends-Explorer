package com.eastagile.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootService extends BroadcastReceiver {
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Intent serviceIntent = new Intent(AlertService.class.getName());
		arg0.startService(serviceIntent);
	}
}
