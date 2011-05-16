package com.eastagile.android;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

public class DetailAddressActivity extends Activity{
	TextView textShowAddress;
	public static final String REFERRENCE_SHOW_ADD_NAME = "Traffic Alert-Reference: Show Address Detail";
	public static final String STRING_SHOW_ADD = "Show Address";
	@Override
  protected void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(R.layout.demo);
	  textShowAddress = (TextView) findViewById(R.id.textShowAddress);
	  SharedPreferences preSetting = getSharedPreferences(REFERRENCE_SHOW_ADD_NAME, MODE_PRIVATE);
	  textShowAddress.setText(preSetting.getString(STRING_SHOW_ADD, ""));
  }
	
}
