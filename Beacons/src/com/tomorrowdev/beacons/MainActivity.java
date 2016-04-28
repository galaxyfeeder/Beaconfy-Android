package com.tomorrowdev.beacons;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;
import com.tomorrowdev.beacons.BeaconFlyer.OnDoingThings;
import com.tomorrowdev.beacons.dialogs.FilterDialog;
import com.tomorrowdev.beacons.dialogs.SettingsDialog;
import com.tomorrowdev.beacons.volley.RequestController;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class MainActivity extends Activity implements BeaconConsumer{
    
	//Objects related to UI
    private ActionBar.Tab tabDiscover, tabNearby, tabFavorites;
    private FragmentTabDiscover mFragmentTabDiscover = new FragmentTabDiscover();
    private FragmentTabNearby mFragmentTabNearby = new FragmentTabNearby();
    private FragmentTabFavorites mFragmentTabFavorites = new FragmentTabFavorites();    
    private RelativeLayout container;
    private int actualTab = 1;
    
    //Objects used by the algorithm that adds or removes beacons
    /* mMMInView -- The ones that are in mBeaconsInView of NearbyTab
     * mMMActual -- The ones that we received by bluetooth
     * mMMRemoved -- The ones that the user has removed manually
     * mMMRequested -- The ones that have been requested but haven't get yet a success */
    private BeaconManager iBeaconManager = BeaconManager.getInstanceForApplication(this);
    private List<BeaconFlyer> mBeaconsInView;
    private List<MM> mMMInView, mMMActual;
    private List<MM> mMMRemoved = new ArrayList<MM>();
    private List<MM> mMMRequested = new ArrayList<MM>();
    final static int TIMES_TO_DIE = 15;
    private Object mLockArrays = new Object();
    
    //Objects used for starting the service
    private Boolean automaticService = true;
	
    //Objects used for sharing on Facebook
    private UiLifecycleHelper uiHelper;
    private BroadcastReceiver facebook_share;
    
    //SharedPreferences and resource instance
    private SharedPreferences prefs;
    private Resources res;
    
    //Objects that define our beacon search
    public static BeaconParser parser = new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24");
    public static Region region = new Region("regionUniqueId", Identifier.parse("00000000-0000-0000-0000-000000000000"), null, null);
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);	
		
		//Setting the parser for recognizing the iBeacon protocol
        iBeaconManager.getBeaconParsers().add(parser);
        
        //Getting the instance of the resources for getting strings
        res = getResources();
        
        //SharedPreferences init
      	prefs = getSharedPreferences("beaconsSettings", MODE_PRIVATE);        
      	      	
        if(isOnline(this)){
        	//initial request for getting userid
          	sendInitialRequest();
            //start request for downloading categories
          	sendCategoriesRequest();
          	//Start request for downloading the images catgories (the ones that haven't been downloaded yet
          	downloadCategoryPhotos();
        }      	
      	
		//Creating the tab fragments and setting up the tba listeners
		ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        tabDiscover = actionBar.newTab().setText(res.getString(R.string.tab_discover_name));
        tabNearby = actionBar.newTab().setText(res.getString(R.string.tab_nearby_name));
        tabFavorites = actionBar.newTab().setText(res.getString(R.string.tab_favorites_name));
        
        tabDiscover.setTabListener(new mTabListener(mFragmentTabDiscover));
        tabNearby.setTabListener(new mTabListener(mFragmentTabNearby));
        tabFavorites.setTabListener(new mTabListener(mFragmentTabFavorites));
        
        //adding and hiding the tabs for not destroying the in a ft.resplace() method
        getFragmentManager().beginTransaction().add(R.id.fragment_container, mFragmentTabDiscover)
        		.hide(mFragmentTabDiscover)
       			.add(R.id.fragment_container, mFragmentTabFavorites)
       			.hide(mFragmentTabFavorites)
       			.add(R.id.fragment_container, mFragmentTabNearby).commit();
        
        //adding the tabs to the actionBar and setting as default the nearby tab
        actionBar.addTab(tabDiscover);
        actionBar.addTab(tabNearby);
        actionBar.addTab(tabFavorites);
        actionBar.setSelectedNavigationItem(1);
        
        //Setting the title and hiding the app logo of the ActionBar
        actionBar.setTitle(tabNearby.getText());
        actionBar.setDisplayShowHomeEnabled(false);
		
		// TASK: dialeg als telefons amb problemes bluetooth detectats
		
		//facebook share
		uiHelper = new UiLifecycleHelper(this, null);
	    uiHelper.onCreate(savedInstanceState);
	    facebook_share = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action_name = intent.getAction();
                if (action_name.equals("com.tomorrowdev.beacons.BeaconFlyer.FACEBOOK")) {
                	FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(MainActivity.this)
                		.setName(intent.getStringExtra(Intent.EXTRA_SUBJECT))
                		.setDescription(intent.getStringExtra(Intent.EXTRA_TEXT))
                		.setLink(intent.getStringExtra("EXTRA_LINK"))
                		.build();
                	uiHelper.trackPendingDialogCall(shareDialog.present());
                }
            };
        };
        //registering the broadcastReceiver that the dummy Activity will send
	    registerReceiver(facebook_share, new IntentFilter("com.tomorrowdev.beacons.BeaconFlyer.FACEBOOK"));
	}
	
	@Override 
    protected void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
	    unregisterReceiver(facebook_share);
	    mFragmentTabFavorites.adapter.sendAllAnalytics();
	    mFragmentTabNearby.adapter.sendAllAnalytics();
    }
	
	@Override
	protected void onResume() {
	    super.onResume();
	    uiHelper.onResume();
	    
	    //TODO bluetooth/wifi check dynamic not only at start
        //Checking bluetooth and internet connection, showing alert views if they are disabled
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		container = (RelativeLayout)findViewById(R.id.activity_container);		
		if (mBluetoothAdapter == null) { //device doens't support bluetooth
		    
		} else { //device support bluetooth
		    if (mBluetoothAdapter.isEnabled()) { //bluetooth is enabled
		    	if(isOnline(this)){ //bluetooth and internet are available
			        iBeaconManager.bind(this);
		    	}else{ //bluetooth is available but not internet
		    		showNoInternetView();
		    	}
		    }else{ //bluetooth is disabled
		    	showNoBluetoothView();
		    }
		}	    
	}

	@Override
	public void onPause() {
	    super.onPause();
	    uiHelper.onPause();
        iBeaconManager.unbind(this);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    uiHelper.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
		Editor e = prefs.edit();
		e.putBoolean("isActivityRunning", true);
		e.commit();
		
		//stopping service because it can cause problems with ranging here.
		//background analytics are also included in the RangingNotifier created here.
        stopService(new Intent(MainActivity.this, BackgroundService.class));
	}

	@Override
	protected void onStop() {
		super.onStop();

		Editor e = prefs.edit();
		e.putBoolean("isActivityRunning", false);
		e.commit();
		
		//starting the service that sends nots and sends analytics on backgrond
		startService();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		
		if (id == R.id.action_settings) {
			SettingsDialog dialog = new SettingsDialog(this);
			dialog.show();
			return true;
		} else if(id == R.id.action_filter){
			FilterDialog dialog = new FilterDialog(this);
			dialog.show();
			dialog.setOnDismissListener(new OnDismissListener() {				
				@Override
				public void onDismiss(DialogInterface dialog) {
					mFragmentTabDiscover.updateMarkers();
					mFragmentTabNearby.updateBeaconsInView();
					mFragmentTabFavorites.updateBeaconsInView();
				}
			});
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * It shows the view that inform that there is not internet connection.
	 */
	private void showNoInternetView(){
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		final View internet = LayoutInflater.from(this).inflate(R.layout.view_internet, null);
		internet.setLayoutParams(params);
		Button but = (Button)internet.findViewById(R.id.enable_wireless);
		but.setOnClickListener(new OnClickListener() {						
			@Override
			public void onClick(View v) {
				if(isOnline(MainActivity.this)){
					container.removeView(internet);
					iBeaconManager.bind(MainActivity.this);
		    		getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
				}
			}
		});
		container.addView(internet);
		getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}
	
	/**
	 * It shows the view that inform that the bluetooth is not enabled
	 */
	private void showNoBluetoothView(){
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		final View bluetooth = LayoutInflater.from(this).inflate(R.layout.view_bluetooth, null);
    	bluetooth.setLayoutParams(params);
		Button but = (Button)bluetooth.findViewById(R.id.enable_bluetooth);
		but.setOnClickListener(new OnClickListener() {						
			@Override
			public void onClick(View v) {
				//TODO setBluetooth(true);
				container.removeView(bluetooth);
				if(isOnline(MainActivity.this)){
		    		getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			        iBeaconManager.bind(MainActivity.this);	
				}else{
					showNoInternetView();
				}
			}
		});
		container.addView(bluetooth);
		getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}
	
	/**
	 * Changes the bluetooth state to the requested.
	 * 
	 * @param enable The requested state
	 * @return Return true when the action is completed without problems or the bluetooth already have that stat.
	 * Otherwise returns false when there is an immediate problem like airplane mode that prevents from starting it.
	 */
	public static boolean setBluetooth(boolean enable) {
	    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    boolean isEnabled = bluetoothAdapter.isEnabled();
	    if (enable && !isEnabled) {
	        return bluetoothAdapter.enable(); 
	    }
	    else if(!enable && isEnabled) {
	        return bluetoothAdapter.disable();
	    }
	    return true;
	}

	/**
	 * Starts the service for sending notifications and getting data from the server in background
	 */
	private void startService(){
		Intent i= new Intent(this, BackgroundService.class);
		if(automaticService){
			Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, 10);
    
            PendingIntent pintent = PendingIntent.getService(this, 0, i, 0);
           
            AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            //start service every minute
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                         1*60*1000, pintent);
		}		
		startService(i); 
	}
	
	/**
	 * Overrided for handling the "minimize" of the flyers using the back button.
	 */
	@Override
	public void onBackPressed() {
		if(mFragmentTabFavorites.adapter.hasFlyersExtended()){
			mFragmentTabFavorites.adapter.minimizeFlyerExtended();
		}else if(mFragmentTabNearby.adapter.hasFlyersExtended()) {
			mFragmentTabNearby.adapter.minimizeFlyerExtended();
		}else{
			super.onBackPressed();
		}
	}

	/**
	 * Custom listener for the tabs. When a tab is selected, hides the previous and shows the new.
	 * Is used by every tab in the ActionBar.
	 * 
	 * @author Gabriel Esteban
	 *
	 */
	public class mTabListener implements ActionBar.TabListener {
		Fragment fragment;
		
		/**
		 * Public constructor for this listener
		 * 
		 * @param fragment the fragment that contains the UI of this tab
		 */
		public mTabListener(Fragment fragment) {
			this.fragment = fragment;
		}
		
		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			ft.show(fragment);
			getActionBar().setTitle(tab.getText());
			if(tab == tabDiscover){
				actualTab = 0;
			} else if(tab == tabNearby){
				actualTab = 1;
			} else if(tab == tabFavorites){
				actualTab = 2;
				mFragmentTabFavorites.updateBeaconsInView();
			}
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			ft.hide(fragment);
		}
	}
	
	//--------------------------------------------------------------------------
	//----------------------DETECTING BEACONS ALGORITHM-------------------------
	//--------------------------------------------------------------------------
	
	int minorAnalytics;
	int majorAnalytics;
	boolean shouldBackground = false;
	int mState;
	@Override
	public void onBeaconServiceConnect() {
		
		//MonitorNotifier used to emulate the background analytics on the foreground
		iBeaconManager.setMonitorNotifier(new MonitorNotifier() {			
			@Override
			public void didExitRegion(Region region) {}			
			@Override
			public void didEnterRegion(Region region) {}			
			@Override
			public void didDetermineStateForRegion(int state, Region region) {
				mState = state;
				shouldBackground = true;
			}
		});
		
	    iBeaconManager.setRangeNotifier(new RangeNotifier() {
	        @Override 
	        public void didRangeBeaconsInRegion(final Collection<Beacon> iBeacons, Region region) {
	        	synchronized (mLockArrays) {
	        		mMMActual = new ArrayList<MM>();
		        	mMMInView = new ArrayList<MM>();
		        	
		        	new Thread(new Runnable() {
						@Override
						public void run() {
							//inside try/catch block because the first line can cause problems
			            	try {
			            		mBeaconsInView = mFragmentTabNearby.adapter.mBeaconsInView;
			                	
			            		//updating mMMActual
			            		Iterator<Beacon> it = iBeacons.iterator();
			            		while (it.hasNext()) {
			            			Beacon b = it.next();
			    					mMMActual.add(new MM(b.getId2().toInt(), b.getId3().toInt(), b.getDistance()));
								}
			            		
			            		//TODO remove tests
			            		//mMMActual.add(new MM(1, 1, 1));
			            		//mMMActual.add(new MM(1, 2, 50));
			            		
			                	//updating mMMInView
			                	for(BeaconFlyer flyer : mBeaconsInView){
			                		mMMInView.add(new MM(flyer.getMajor(), flyer.getMinor()));
			                	}
			                	
			                	//arrays aren't equal
			                	if(mMMInView.equals(mMMActual) == false){
			                		//Compare the displayed beacons with the actual - removing & updating
			                		for(int i = 0; i < mMMInView.size(); i++){
			                			if(mMMActual.contains(mMMInView.get(i))){ //update data                				
			                				//try-catch block for preventing crashes when the beacon is just removed
			                				try {
			                					for(int x = 0; x < mMMActual.size(); x++){
				                					if(mMMActual.get(x).equals(mMMInView.get(i))){
				                						mBeaconsInView.get(i).setDistance(mMMActual.get(x).getDistance());
				                					}
				                				}
				                				mBeaconsInView.get(i).setDeathCount(0);
											} catch (IndexOutOfBoundsException e) {
												e.printStackTrace();
											}
			                			}else{ //Beacon lost
			                				mBeaconsInView.get(i).increaseDeathCountByOne();
			                				if(mBeaconsInView.get(i).getDeathCount() == TIMES_TO_DIE){
			                					boolean sameMajor = false;
			                					for(int h = 0; h < mBeaconsInView.size(); h++){
			                						if(mBeaconsInView.get(h).getMajor() == mBeaconsInView.get(i).getMinor() && mBeaconsInView.get(h) != mBeaconsInView.get(i)){
			                							sameMajor = true;
			                						}
			                					}
			                					if(!sameMajor){
					                				final int a = i;
			                						runOnUiThread(new Runnable() {												
														@Override
														public void run() {
															mFragmentTabNearby.adapter.mBeaconsInView.get(a).sendAnalytics();
															Animation exit_anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.flyer_exit);
															mFragmentTabNearby.adapter.mBeaconsInView.get(a).startAnimation(exit_anim);
													        exit_anim.setAnimationListener(new AnimationListener() {										
																@Override
																public void onAnimationStart(Animation animation) {}										
																@Override
																public void onAnimationRepeat(Animation animation) {}										
																@Override
																public void onAnimationEnd(Animation animation) {
								                					mFragmentTabNearby.removeBeacon(a);
																}
															});
														}
													});
			                					}		                					
			                				}		    							
			                			}
			                		}
			                		//Compare the actual with the displaying - adding
			                		for(int i = 0; i < mMMActual.size(); i++){
			                			if(!mMMInView.contains(mMMActual.get(i)) 
			                			&& !mMMRemoved.contains(mMMActual.get(i))
			                			&& !mMMRequested.contains(mMMActual.get(i))
			                			&& !mFragmentTabNearby.adapter.isFiltered(mMMActual.get(i).getMajor(), mMMActual.get(i).getMinor())){
			                				//This beacon is not in view. So we add it!
			                				final int a = i;
			                				runOnUiThread(new Runnable() {											
												@Override
												public void run() {
													if(isOnline(MainActivity.this)){
														try {
															Log.e("BEACON", "Request started "+mMMActual.get(a).getMajor()+" "+mMMActual.get(a).getMinor());
															//TASK: new beaconInformationRequest(mMMActual.get(a).getMajor(), mMMActual.get(a).getMinor(), mMMActual.get(a).getDistance()).execute();
															sendBeaconInformationRequest(mMMActual.get(a).getMajor(), mMMActual.get(a).getMinor(), mMMActual.get(a).getDistance());
														} catch (IndexOutOfBoundsException e) {
															e.printStackTrace();
														}
													}
												}
											});		                				
			                			}
			                			
			                			if(mFragmentTabNearby.adapter.isFiltered(mMMActual.get(i).getMajor(), mMMActual.get(i).getMinor())){
			                				final int a = i;
			                				runOnUiThread(new Runnable() {												
												@Override
												public void run() {
					                				mFragmentTabNearby.adapter.showView(mMMActual.get(a).getMajor(), mMMActual.get(a).getMinor());													
												}
											});
			                			}
			                		}
			                	}else{
	                				//try-catch block for preventing crashes when the beacon is just removed
			                		try{
			                			//updating data for all beacons
				                		for(int i = 0; i < mMMInView.size(); i++){
				                			for(int x = 0; x < mMMActual.size(); x++){
				                				if(mMMActual.get(x).equals(mMMInView.get(i))){
			                						mBeaconsInView.get(i).setDistance(mMMActual.get(x).getDistance());
			                					} 
			                				}
				                		}
			                		} catch (IndexOutOfBoundsException e) {
										e.printStackTrace();
									}		                		
			                	}
			                	
			                	//Sorting the flyers by distance
			                	if(mBeaconsInView.size() > 1){
			                		runOnUiThread(new Runnable() {								
										@Override
										public void run() {
						                	mFragmentTabNearby.sortByDistance();
										}
									});
			                	}		                	
			                	
			                	//We check if there are no beacons in view and we show the ActionBar
			                	if(mBeaconsInView.isEmpty() && !getActionBar().isShowing() && actualTab == 1){
			                		runOnUiThread(new Runnable() {
										public void run() {
					                		getActionBar().show();
											mFragmentTabNearby.mBeaconScroll.setPagingEnabled(true);
											mFragmentTabNearby.titleIndicator.setVisibility(View.VISIBLE);
										}
									});
			                	}
			                	
							} catch (java.lang.NullPointerException e) {
								e.printStackTrace();
							}
						}
					}).start();
	        	}
	        	
	        	//Sends the background analytics as the BackgroundService does
	        	if(shouldBackground){
	        		Iterator<Beacon> it = iBeacons.iterator();
	        		if(it.hasNext()){
	        			Beacon b = iBeacons.iterator().next();

						if(isOnline(MainActivity.this)){
							if(mState == MonitorNotifier.INSIDE){
								BackgroundService.sendBackGroundRequest(MainActivity.this, b.getId2().toInt(), b.getId3().toInt(), "in");
								majorAnalytics = b.getId2().toInt();
								minorAnalytics = b.getId3().toInt();
							}else{
								BackgroundService.sendBackGroundRequest(MainActivity.this, majorAnalytics, minorAnalytics, "out");
							}
						}
						shouldBackground = false;
	        		}
	        	}
	        }
	    });

		//Start searching beacons in this concrete region determined by UUID
        try {
            iBeaconManager.startRangingBeaconsInRegion(region);
            iBeaconManager.startMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {
           	
        }
	}
	
	//--------------------------------------------------------------------------
	//----------------------------SERVER REQUESTS-------------------------------
	//--------------------------------------------------------------------------	
	
	/**
	 * Check if the user have internet connection available.
	 * 
	 * @param context The context application, as this is an static method
	 * @return whether it's online or not
	 */
	public static boolean isOnline(Context context) {
	    ConnectivityManager cm =
	        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}
	
	/**
	 * Asks server for the categories using the volley framework.
	 * The server returns false when the user have the last categories version,
	 * and returns true when should be  updated.
	 */
	private void sendCategoriesRequest(){
		// Tag used to cancel the request
		String tag_json_obj = "json_obj_categories_request";
		 
		String url = getResources().getString(R.string.base_domain)+"/api/category_list.php"
		+"?lang="+Locale.getDefault().getLanguage()
		+"&version="+prefs.getInt("categories_version", 0);
				
		Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {		 
			@Override
				public void onResponse(JSONObject response) {
				Log.e("SERVER-CATEGORIES", response.toString());
				try {
					if(response.getBoolean("success")){
						final JSONArray prefsArray;
						final JSONArray arrayToSave;
						final JSONArray array;
						try {
							prefsArray = new JSONArray(prefs.getString("filterList", new JSONArray().toString()));

							//Creating the array that we will save in the prefs
							arrayToSave = new JSONArray();			
							
				    		array = response.getJSONArray("categories");
				    		
				    		for(int i = 0; i < array.length(); i++){
				    			
				    			final URI url = new URI(array.getJSONObject(i).getString("image"));
				    			
				    			JSONObject obj = new JSONObject();
        						obj.put("image_url", url);
        						obj.put("image_path", null);
        						obj.put("name", array.getJSONObject(i).getString("name"));
        						
        						obj.put("check", true);
        						try {
        							for(int x = 0; x < prefsArray.length(); x++){
        								if(prefsArray.getJSONObject(x).getString("name").equals(array.getJSONObject(i).getString("name"))){
        									if(prefsArray.getJSONObject(x).has("check")){
        										obj.put("check", prefsArray.getJSONObject(x).getBoolean("check"));
        									} else {
        										obj.put("check", true);
        									}
        								}
        							}
        						} catch (JSONException e) {
        							e.printStackTrace();
        						}
        						
        						arrayToSave.put(i, obj);
        						
        						//TODO categories.subcategories no es fa servir
        						
        						Editor editor = prefs.edit();
        				    	editor.putString("filterList", arrayToSave.toString());
        						editor.putInt("categories_version", response.getInt("new_version"));
        				    	editor.commit();
        				    	
				    		}
						} catch (JSONException e) {
							e.printStackTrace();
						} catch (URISyntaxException e){
							e.printStackTrace();
						}
						
						downloadCategoryPhotos();
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
				
		// Creating the json request
		JsonObjectRequest jsonObjReq = new JsonObjectRequest(Method.GET, url, null, responseListener, responseErrorListener);
			 
		// Adding request to request queue
		RequestController.getInstance(this).addToRequestQueue(jsonObjReq, tag_json_obj);
	}
	
	/**
	 * Downloads all the category photos and save them into ./beacons
	 * Uses the volley framework.
	 */
	private void downloadCategoryPhotos(){
		ImageLoader imageLoader = RequestController.getInstance(this).getImageLoader();
		 
		//Starting sharedPreferences for getting the check value
		try {
			final JSONArray prefsArray = new JSONArray(prefs.getString("filterList", new JSONArray().toString()));
    		
    		for(int i = 0; i < prefsArray.length(); i++){
    			
    			if(!prefsArray.getJSONObject(i).has("image_path")){
    				final JSONObject prefsObject = prefsArray.getJSONObject(i);
    				final URI url = new URI(prefsArray.getJSONObject(i).getString("image_url"));
    				
        			imageLoader.get(url.toString(), new ImageListener() {
        				 
        			    @Override
        			    public void onErrorResponse(VolleyError error) {
        			    	Log.e("IMAGE-DOWNLOAD", "Error response");
        			    }    			 
        			    
        			    @Override
        			    public void onResponse(ImageContainer response, boolean arg1) {
        			    	Log.e("IMAGE-DOWNLOAD", "Good response");
        			    	
        			        if (response.getBitmap() != null) {
        			        	try{
        			        		final Bitmap bitmap = response.getBitmap();
            			        	File sd = Environment.getExternalStorageDirectory();
            						
            						File dir = new File(sd + "/.beacons");
            						//Creates the directory if it doesn't exist
            				        dir.mkdirs();
            				        File f = new File(dir, URLUtil.guessFileName(url.toURL().toString(), null, null));
            						
            				        final String path = f.getPath();
            						
            						FileOutputStream out = null;
            						try {
            							out = new FileOutputStream(f);
            						    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            						} catch (Exception e) {
            						    e.printStackTrace();
            						} finally {
            						    try{
            						    	out.close();
            						    } catch(Throwable ignore) {
            						    }
            						}
            			        	
            						JSONObject obj = new JSONObject();
            						obj.put("image_path", path);
            						obj.put("image_url", prefsObject.getString("image_url"));
            						obj.put("name", prefsObject.getString("name"));
            						obj.put("check", prefsObject.getString("check"));
            						
            						JSONArray arrayToSave = prefsArray;
            						try {
            							for(int x = 0; x < prefsArray.length(); x++){
            								if(prefsArray.getJSONObject(x).getString("name").equals(prefsObject.getString("name"))){
            									arrayToSave.put(x, obj);
            								}
            							}
            						} catch (JSONException e) {
            							e.printStackTrace();
            						}
            						
            						//TODO categories.subcategories no es fa servir
            						
            						Editor editor = prefs.edit();
            				    	editor.putString("filterList", arrayToSave.toString());
            				    	editor.commit();
            				    	
        			        	} catch (JSONException e){
        			        		e.printStackTrace();
        			        	} catch (MalformedURLException e){
        			        		e.printStackTrace();
        			        	}
        			        }
        			    }
        			});
    			}		
    		}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (URISyntaxException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Asks for beacon information when a new beacon is detected.
	 * 
	 * @param major the beacon major which we want to know more
	 * @param minor the beacon minor which we want to know more
	 * @param distance the beacon distance or being analyzed
	 */
	private void sendBeaconInformationRequest(final int major, final int minor, final double distance){
		// Tag used to cancel the request
		String tag_json_obj = "json_obj_beacon_information";
		 
		String url = getResources().getString(R.string.base_domain)+"/api/beacons_info.php";
		
		Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {		 
			@Override
			public void onResponse(JSONObject json) {
				Log.e("SERVER-NEARBY", json.toString());
				try {
					if(json.getBoolean("success")){
				    	boolean shouldBeAdded = true;
			    		//Filtering the beacons by categories, and don't adding it depending on the ones that are checked
			    		JSONArray categoriesArray = new JSONArray(prefs.getString("filterList", new JSONArray().toString()));
			    		for(int x = 0; x < categoriesArray.length(); x++){
							if(categoriesArray.getJSONObject(x).getString("name").equals(json.getString("beacon_category"))){
								if(!categoriesArray.getJSONObject(x).getBoolean("check")){
									shouldBeAdded = false;
								}
							}
						}
			    		if(shouldBeAdded){
			    			//Adding the beacon because its category is checked
							final BeaconFlyer flyer = new BeaconFlyer(MainActivity.this);
	        				flyer.setTag(major+"_"+minor);
	        				flyer.setListener(new OnDoingThings() {													
								@Override
								public void onRemove() {
									flyer.sendAnalytics();
									Animation exit_anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.flyer_exit);
							        flyer.startAnimation(exit_anim);
							        exit_anim.setAnimationListener(new AnimationListener() {										
										@Override
										public void onAnimationStart(Animation animation) {}										
										@Override
										public void onAnimationRepeat(Animation animation) {}										
										@Override
										public void onAnimationEnd(Animation animation) {
											new Handler().post(new Runnable() {
										        public void run() {
										        	mFragmentTabNearby.removeBeacon(mFragmentTabNearby.adapter.mBeaconsInView.indexOf(flyer));
													synchronized (mLockArrays) {
														mMMInView.remove(new MM(flyer.getMajor(), flyer.getMinor()));
														mMMRemoved.add(new MM(flyer.getMajor(), flyer.getMinor()));
													}
										        }
										    });											
										}
									});
								}
								@Override
								public void onExpand() {
									getActionBar().hide();
									mFragmentTabNearby.mBeaconScroll.setPagingEnabled(false);
									mFragmentTabNearby.adapter.setFlyerExtended(true);
									mFragmentTabNearby.titleIndicator.setVisibility(View.GONE);
								}
								@Override
								public void onDexpand() {
									getActionBar().show();
									mFragmentTabNearby.mBeaconScroll.setPagingEnabled(true);
									mFragmentTabNearby.adapter.setFlyerExtended(false);
									mFragmentTabNearby.titleIndicator.setVisibility(View.VISIBLE);
								}
							});
	        				flyer.createBeacon(major, minor);
	        				flyer.setBeaconProperties(json);
	        				flyer.setDistance(distance);
	        				
	        				if(!mBeaconsInView.contains(flyer)){
	            				mFragmentTabNearby.addBeacon(flyer);
	            				Animation entry_anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.flyer_entry);
	            				flyer.startAnimation(entry_anim);
							}
	        				
	        				synchronized (mLockArrays) {
	        				    mMMRequested.remove(new MM(major, minor));				
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
		
		// Creating the json request
		JsonObjectRequest jsonObjReq = new JsonObjectRequest(Method.PUT, url, null, responseListener, responseErrorListener){
			@Override
			public byte[] getBody() {
				String params = "user_id="+prefs.getInt("userid", 0)+
								"&minor="+minor+
								"&major="+major+
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
		RequestController.getInstance(this).addToRequestQueue(jsonObjReq, tag_json_obj);
	}
	
	/**
	 * Send initial request using volley.
	 * 
	 * It's called at startup, and checks the user_id and updating information about the user.
	 */
	private void sendInitialRequest(){
		// Tag used to cancel the request
		String tag_json_obj = "json_obj_initial_request";
		 
		String url = getResources().getString(R.string.base_domain)+"/api/initial.php";
		
		Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {		 
			@Override
			public void onResponse(JSONObject response) {
				Log.e("SERVER-INITIAL", response.toString());
				try {
					if(response.getBoolean("success")){
		    			if(response.has("user_id")){
		    				Editor e = prefs.edit();
		    				e.putInt("userid", response.getInt("user_id"));
		    				e.commit();
		    			}
		    			if(response.has("should_update")){
		    				if(response.getBoolean("should_update")){
			    				//TODO dialeg actualiatzar
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
		
		// Creating the json request
		JsonObjectRequest jsonObjReq = new JsonObjectRequest(Method.PUT, url, null, responseListener, responseErrorListener){
			@Override
			public byte[] getBody() {
				PackageInfo pInfo = null;
				try {
					pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}
				String params = "app_version="+(pInfo != null?pInfo.versionName:null)+
								"&os="+"Android"+
								"&os_version="+Build.VERSION.RELEASE+
								"&user_id="+prefs.getInt("userid", 0);
				try {
					return params.getBytes("UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return null;
			}
		};
		 
		// Adding request to request queue
		RequestController.getInstance(this).addToRequestQueue(jsonObjReq, tag_json_obj);
	}
}
