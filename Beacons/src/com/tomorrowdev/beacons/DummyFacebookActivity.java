package com.tomorrowdev.beacons;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class DummyFacebookActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = new Intent("com.tomorrowdev.beacons.BeaconFlyer.FACEBOOK");
		intent.putExtra(Intent.EXTRA_TEXT, getIntent().getStringExtra(Intent.EXTRA_TEXT));
		intent.putExtra(Intent.EXTRA_SUBJECT, getIntent().getStringExtra(Intent.EXTRA_SUBJECT));
		intent.putExtra("EXTRA_LINK", getIntent().getStringExtra("EXTRA_LINK"));
        sendBroadcast(intent);
		finish();
        
	}
}
