package com.tomorrowdev.beacons;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.JsonObjectRequest;
import com.tomorrowdev.beacons.volley.RequestController;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class BackgroundService extends Service implements BeaconConsumer{

	protected static final String TAG = "BeaconService";
    private BeaconManager iBeaconManager = BeaconManager.getInstanceForApplication(this);
	private SharedPreferences prefs;
	
	private boolean shouldStop = true;
    
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		prefs = getSharedPreferences("beaconsSettings", MODE_PRIVATE);
		
		//Setting the parser for recognizing the iBeacon protocol
        iBeaconManager.getBeaconParsers().add(MainActivity.parser);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
		    // Device does not support Bluetooth
		} else {
		    if (mBluetoothAdapter.isEnabled()) {
		        iBeaconManager.bind(this);
		    }else{
		    	if(shouldStop){
			    	stopSelf();
		    	}
		    }
		}		
	    return Service.START_STICKY;
	}
	
	@Override
	public void onDestroy() {
        super.onDestroy();
        iBeaconManager.unbind(this);
    }
	int minorAnalytics;
	int majorAnalytics;
	@Override
	public void onBeaconServiceConnect() {
		
		iBeaconManager.setMonitorNotifier(new MonitorNotifier() {			
			@Override
			public void didExitRegion(Region region) {
			}
			
			@Override
			public void didEnterRegion(Region region) {
			}
			
			@Override
			public void didDetermineStateForRegion(final int state, Region region) {
				if(!prefs.getBoolean("isActivityRunning", false)){
					iBeaconManager.setRangeNotifier(new RangeNotifier() {			
						@Override
						public void didRangeBeaconsInRegion(Collection<Beacon> iBeacons, Region region) {

							Iterator<Beacon> it = iBeacons.iterator();
			        		if(it.hasNext()){
			        			Beacon b = iBeacons.iterator().next();

								if(MainActivity.isOnline(BackgroundService.this)){
									if(state == MonitorNotifier.INSIDE){
										sendBackGroundRequest(BackgroundService.this, b.getId2().toInt(), b.getId3().toInt(), "in");
										majorAnalytics = b.getId2().toInt();
										minorAnalytics = b.getId3().toInt();
										shouldStop = false;
									}else{
										sendBackGroundRequest(BackgroundService.this, b.getId2().toInt(), b.getId3().toInt(), "out");
										shouldStop = true;
									}								
								}
								
								try {
									iBeaconManager.stopRangingBeaconsInRegion(MainActivity.region);
								} catch (RemoteException e) {
									e.printStackTrace();
								}
			        		}
						}
					});
					
					try {
						iBeaconManager.startRangingBeaconsInRegion(MainActivity.region);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		});
		
		try {
            iBeaconManager.startMonitoringBeaconsInRegion(MainActivity.region);
        } catch (RemoteException e) {
        	
        }
	}
	
	/**
	 * Sends data for being analyzed in the server and gets a notification to send if
	 * the app is in background. Uses volley framework.
	 * 
	 * @param context the application context, as it's an static method
	 * @param major the beacon major
	 * @param minor the beacon minor
	 * @param state the new state, in or out
	 */
	public static void sendBackGroundRequest(final Context context, final int major, final int minor, final String state){
		// Tag used to cancel the request
		String tag_json_obj = "json_obj_background_request";
		
		String url = context.getResources().getString(R.string.base_domain)+"/api/background.php";
		
		final NotificationManager notificationManager = (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
		final SharedPreferences prefs = context.getSharedPreferences("beaconsSettigns", Context.MODE_PRIVATE);
		
		Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {		 
			@Override
			public void onResponse(JSONObject response) {
				Log.e("SERVER-BACKGROUND", response.toString());
				try {
		    		if(response.getBoolean("success")){
		    			if(!response.getString("notification").equals("")
		    						|| response.getString("notification") != null
		    						||!response.getString("title").equals("")
		    						|| response.getString("title") != null){

        					if(prefs.getBoolean("send_notifications", true) && !prefs.getBoolean("isActivityRunning", false)){
        						notificationManager.notify(response.getString("notification").hashCode(), createNotification(context, response.getString("title"), response.getString("notification")));
        					}
		    			}
		    		}
				} catch (JSONException e) {
					e.printStackTrace();
				}				
            }
		};
		
		Response.ErrorListener responseErrorListener = new Response.ErrorListener() {		 
			@Override
        	public void onErrorResponse(VolleyError error) {
			}
		};
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		final String time = df.format(Calendar.getInstance().getTime());
		
		// Creating the json request
		JsonObjectRequest jsonObjReq = new JsonObjectRequest(Method.PUT, url, null, responseListener, responseErrorListener){
			@Override
			public byte[] getBody() {
				String params = "&major="+major+
								"&minor="+minor+
								"&user_id="+prefs.getInt("userid", 0)+
								"&state="+state+
								"&time="+time+
								"&lang="+Locale.getDefault().getLanguage();
				try {
					return params.getBytes("UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return null;
			}
		};
		 
		// Adding request to request queue
		RequestController.getInstance(context).addToRequestQueue(jsonObjReq, tag_json_obj);
	}
	
	public static Notification createNotification(Context context, String title, String body) {
		Intent intent = new Intent(context, MainActivity.class);
		PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);

		Notification n  = new Notification.Builder(context)
		        .setContentTitle(title)
		        .setContentText(body)
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentIntent(pIntent)
		        .setAutoCancel(true)
		        .setDefaults(Notification.DEFAULT_ALL)
		        .setStyle(new Notification.BigTextStyle()
                .bigText(body))
		        .build();
		
		return n;
	 }
}
