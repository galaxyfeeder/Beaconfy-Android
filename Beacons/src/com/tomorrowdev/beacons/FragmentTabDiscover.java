package com.tomorrowdev.beacons;

import java.io.File;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tomorrowdev.beacons.volley.RequestController;

public class FragmentTabDiscover extends MapFragment implements LocationListener{

	private LocationManager locationManager;
	private String provider;
	private JSONObject serverResult = null;
	SharedPreferences prefs;
	private Location realLocation = null, lastLocation;
	
	@Override
	public void onStart() {
		super.onStart();
		getMap().setMyLocationEnabled(true);
		// Setting a custom info window adapter for the google map
        getMap().setInfoWindowAdapter(new InfoWindowAdapter() {
 
            // Use default InfoWindow frame
            @Override
            public View getInfoWindow(Marker marker) {            	
                return null;
            }
 
            // Defines the contents of the InfoWindow
            @Override
            public View getInfoContents(Marker marker) {
 
                // Getting view from the layout file map_info_view
                View v = getActivity().getLayoutInflater().inflate(R.layout.map_info_view, null);
                
                TextView title = (TextView) v.findViewById(R.id.title);
                TextView snippet = (TextView) v.findViewById(R.id.snippet);
                TextView count = (TextView) v.findViewById(R.id.count);
                ImageView icon = (ImageView) v.findViewById(R.id.categoryIcon);
                
                title.setText(marker.getTitle());
                
                try {
					JSONObject obj = new JSONObject(marker.getSnippet());
					snippet.setText(obj.getString("snippet"));
					count.setText(obj.getString("count"));
					
					JSONArray string = new JSONArray();
					try {
						string = new JSONArray(prefs.getString("filterList", new JSONArray().toString()));
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					for(int i = 0; i < string.length(); i++){			
						try {
							JSONObject category = string.getJSONObject(i);			
							if(category.getString("name").equals(obj.getString("category"))){
								File imgFile = new File(category.getString("image_path"));
						        if(imgFile.exists()){
						        	Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
						            icon.setImageBitmap(myBitmap);
						        }
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
                
                return v;
 
            }
        });
		
		// getting the location service and searching the best provider
	    locationManager = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
	    Criteria criteria = new Criteria();
	    provider = locationManager.getBestProvider(criteria, false);
	    lastLocation = locationManager.getLastKnownLocation(provider);
	    
	    if(MainActivity.isOnline(getActivity())){
		    if(realLocation != null){
		    	sendMapMarkersRequest(realLocation);
		    }else if(lastLocation != null){
		    	sendMapMarkersRequest(lastLocation);
		    }	    	
	    }
	}	
	
	@Override
	public void onResume() {
		super.onResume();
		//asking for only a update of the location
		//locationManager.requestLocationUpdates(provider, 400, 1, this);
		locationManager.requestSingleUpdate(provider, this, null);
	}
	
	private void sendMapMarkersRequest(Location loc){
		// Tag used to cancel the request
		String tag_json_obj = "json_obj_categories_request";
		 
		String url = getResources().getString(R.string.base_domain)+"/api/map.php"
		+"?latitude="+loc.getLatitude()
	 	+"&longitude="+loc.getLongitude()
	 	+"&lang="+Locale.getDefault().getLanguage();
				
		Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {		 
			@Override
				public void onResponse(JSONObject response) {
				Log.e("SERVER-MAP", response.toString());
				serverResult = response;	    	
    		    updateMarkers();
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
		RequestController.getInstance(getActivity()).addToRequestQueue(jsonObjReq, tag_json_obj);
	}
	
	public void updateMarkers(){		
		if(serverResult != null){
			
			GoogleMap map = getMap();
			if(map != null){
				map.clear();					
				
				try{
			    	prefs = getActivity().getSharedPreferences("beaconsSettings", Context.MODE_PRIVATE);
					JSONArray categoriesArray = new JSONArray(prefs.getString("filterList", new JSONArray().toString()));
					if(serverResult.getBoolean("success")){
						JSONArray array = serverResult.getJSONArray("regions");
						for(int i = 0; i < array.length(); i++){
							boolean shouldBeAdded = true;
							JSONObject obj = (JSONObject) array.get(i);
							
							String iconPath = null;
							for(int x = 0; x < categoriesArray.length(); x++){
								if(categoriesArray.getJSONObject(x).getString("name").equals(obj.getString("category"))){
									if(categoriesArray.getJSONObject(x).getBoolean("check")){
										iconPath = categoriesArray.getJSONObject(x).getString("image_path");										
									}else{
										shouldBeAdded = false;
									}
								}
							}
							
							if(shouldBeAdded){
								JSONObject objSnippet = new JSONObject();
								objSnippet.put("category", obj.getString("category"));
								objSnippet.put("snippet", obj.getString("subtitle"));
								objSnippet.put("count", obj.getString("beacons_count"));
								
								MarkerOptions marker = new MarkerOptions()
									.title(obj.getString("title"))
									.snippet(objSnippet.toString())
									.position(new LatLng(obj.getDouble("latitude"), obj.getDouble("longitude")));
								if(iconPath != null){
									marker.icon(BitmapDescriptorFactory.fromPath(iconPath));
								}
								map.addMarker(marker);
							}							
						}
					}
				}catch (JSONException e){
					e.printStackTrace();
				}
			} 
		}else{
			if(MainActivity.isOnline(getActivity())){
			    if(realLocation != null){
			    	sendMapMarkersRequest(realLocation);
			    }else if(lastLocation != null){
			    	sendMapMarkersRequest(lastLocation);
			    }	    	
		    }
		}
	}
		
	@Override
	public void onLocationChanged(Location location) {
		//aqui retorna la de veritat, pero per agilitzar les coses, la primera request la farem amb lastLocation
		//en cas de que la primera request no hagi funcionat, i aqui ja s'hagi rebut la de veritat, al seguent
		//es fara amb aquesta
		realLocation = location;	
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}
