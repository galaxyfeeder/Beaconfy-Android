package com.tomorrowdev.beacons;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.JsonObjectRequest;
import com.tomorrowdev.beacons.BeaconFlyer.OnDoingThings;
import com.tomorrowdev.beacons.db.FavBeaconsDataSource;
import com.tomorrowdev.beacons.volley.RequestController;
import com.viewpagerindicator.CirclePageIndicator;

public class FragmentTabFavorites extends Fragment {

	FlyersViewPager mBeaconScroll;
	BeaconScrollAdapter adapter = null;
	CirclePageIndicator titleIndicator;
	
	private SharedPreferences prefs;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = new BeaconScrollAdapter(getActivity());

		//SharedPreferences init
		prefs = getActivity().getSharedPreferences("beaconsSettings", Context.MODE_PRIVATE);
		
		FavBeaconsDataSource db = new FavBeaconsDataSource(getActivity());
		db.open();
		ArrayList<MM> mm = db.getAllFavBeacon();
		for(int i = 0; i < mm.size(); i++){
			if(MainActivity.isOnline(getActivity())){
				sendBeaconInformationRequest(mm.get(i).getMajor(), mm.get(i).getMinor(), -1);
			}
		}
		db.close();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.tab_favorites, container, false);		
		
		//getting the instance of the viewpager by id and setting the adapter
		mBeaconScroll = (FlyersViewPager)view.findViewById(R.id.beaconsScrollFav);
		adapter.setViewPager(mBeaconScroll);
		mBeaconScroll.setAdapter(adapter);
		
		//getting the instance of the indicator by id
		titleIndicator = (CirclePageIndicator)view.findViewById(R.id.titlesFav);
		titleIndicator.setViewPager(mBeaconScroll);
		titleIndicator.setFillColor(Color.rgb(50, 50, 50));
		
		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public void addBeacon(BeaconFlyer flyerToAdd){
		//adapter.add(flyerToAdd);
		//el porto a la primera vista de totes, perque en teoria s'afegeixen al principi
		//sino mirar els metodes mostrats a http://stackoverflow.com/questions/13664155/dynamically-add-and-remove-view-to-viewpager
		
		int pageIndex = adapter.add(flyerToAdd, 0);
		mBeaconScroll.setCurrentItem(pageIndex, true);
		mBeaconScroll.setOffscreenPageLimit(adapter.mBeaconsInView.size());
	}
	
	public void removeBeacon(int position){
		adapter.removeView(position);
	}
	
	public void updateBeaconsInView(){
		
		adapter.filterByCategories();
		
		FavBeaconsDataSource db = new FavBeaconsDataSource(getActivity());
		db.open();
		ArrayList<MM> mm = db.getAllFavBeacon();
		for(int i = 0; i < mm.size(); i++){
			boolean shouldBeAdded = true;
			for(int a = 0; a < adapter.mBeaconsInView.size(); a++){
				if(adapter.mBeaconsInView.get(a).getMajor() == mm.get(i).getMajor()
						&& adapter.mBeaconsInView.get(a).getMinor() == mm.get(i).getMinor()){
					shouldBeAdded = false;
				}
			}			
			if(MainActivity.isOnline(getActivity()) && shouldBeAdded){
				if(adapter.isFiltered(mm.get(i).getMajor(), mm.get(i).getMinor())){
					adapter.showView(mm.get(i).getMajor(), mm.get(i).getMinor());
				}else{
					sendBeaconInformationRequest(mm.get(i).getMajor(), mm.get(i).getMinor(), -1);
				}
			}
		}
		db.close();
	}
	
	/**
	 * Asks for beacon information when a favorited beacon should be added.
	 */
	private void sendBeaconInformationRequest(final int major, final int minor, final double distance){
		// Tag used to cancel the request
		String tag_json_obj = "json_obj_beacon_information";
		 
		String url = getResources().getString(R.string.base_domain)+"/api/beacons_info.php";
		
		Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {		 
			@Override
			public void onResponse(JSONObject json) {
				Log.e("SERVER-FAVORITES", json.toString());
				try {
					if(json.getBoolean("success")){
						//Filtering the beacons by categories, and adding it depending on the ones that are checked
			    		JSONArray categoriesArray = new JSONArray(prefs.getString("filterList", new JSONArray().toString()));
			    		for(int x = 0; x < categoriesArray.length(); x++){
							if(categoriesArray.getJSONObject(x).getString("name").equals(json.getString("beacon_category"))){
								if(categoriesArray.getJSONObject(x).getBoolean("check")){
									//Adding the beacon because its category is checked
									final BeaconFlyer flyer = new BeaconFlyer(getActivity());
	                				flyer.setTag(major+"_"+minor);
	                				flyer.setListener(new OnDoingThings() {													
										@Override
										public void onRemove() {
											
										}
										@Override
										public void onExpand() {
											getActivity().getActionBar().hide();
											mBeaconScroll.setPagingEnabled(false);
											adapter.setFlyerExtended(true);
											titleIndicator.setVisibility(View.GONE);
										}
										@Override
										public void onDexpand() {
											getActivity().getActionBar().show();
											mBeaconScroll.setPagingEnabled(true);
											adapter.setFlyerExtended(false);
											titleIndicator.setVisibility(View.VISIBLE);
										}
									});
	                				flyer.createBeacon(major, minor);
	                				flyer.setBeaconProperties(json);
	                				flyer.setDistance(distance);
	                				flyer.setShouldSwype(false);
	                				flyer.setInFavoriteView(true);
	                				
	                				if(!adapter.mBeaconsInView.contains(flyer)){
		                				addBeacon(flyer);
									}
								}
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
		RequestController.getInstance(getActivity()).addToRequestQueue(jsonObjReq, tag_json_obj);
	}
}
