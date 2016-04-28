package com.tomorrowdev.beacons.dialogs;

import com.tomorrowdev.beacons.R;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class SettingsDialog extends Dialog{

	SharedPreferences prefs;
	CheckBox notifications;
	
	public SettingsDialog(Context context) {
		super(context);
		
		prefs = context.getSharedPreferences("beaconsSettings", Context.MODE_PRIVATE);
		
		setTitle(context.getResources().getString(R.string.dialog_settings_title));
		
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_settings, null);
		setContentView(layout);
		
		notifications = (CheckBox)layout.findViewById(R.id.notifications);
		
		notifications.setChecked(prefs.getBoolean("send_notifications", true));
		notifications.setOnCheckedChangeListener(new OnCheckedChangeListener() {			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Editor editor = prefs.edit();
				editor.putBoolean("send_notifications", isChecked);
				editor.commit();
			}
		});
	}
}
