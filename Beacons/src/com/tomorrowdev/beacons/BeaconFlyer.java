package com.tomorrowdev.beacons;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import com.android.volley.toolbox.JsonObjectRequest;
import com.tomorrowdev.beacons.db.FavBeaconsDataSource;
import com.tomorrowdev.beacons.dialogs.ShareDialog;
import com.tomorrowdev.beacons.volley.RequestController;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * The view that manages the graphic interface that represents the beacon.
 * 
 * @author Gabriel Esteban
 *
 */
public class BeaconFlyer extends LinearLayout implements OnTouchListener, OnClickListener{
	
	//--------------------------------------------------------------------------
	//---------------------INTERFACES AND RELATED METHODS-----------------------
	//--------------------------------------------------------------------------
	
	/**
	 * Listener that is used by the activity for executing some methods that can't be runned here.
	 * 
	 * @author Gabriel Esteban
	 *
	 */
	public interface OnDoingThings{
		public void onRemove();
		public void onExpand();
		public void onDexpand();
	}
	
	private OnDoingThings mListener;
	
	public void setListener(OnDoingThings list){
		mListener = list;	
	}
	
	//--------------------------------------------------------------------------
	//-------------------------OBJECTS AND VARIABLES----------------------------
	//--------------------------------------------------------------------------
	
	//View objects
	private WebView web;
	private ImageView logo, share, star, fullscreen;
	private TextView title;
	private RelativeLayout header;
	
	//Stored data
	private int major, minor, detectRange;
	private double distance;
	private String category, subcategory;
	private int deathCount = 0;
	private String theme = "black";
	private String shareURL = null;
	
	//Analytics data
	private boolean isFavorited = false;
	private boolean isInFavoriteView = false;
	public int facebook_shared = 0;
	public int twitter_shared = 0;
	public int mail_shared = 0;
	public int sms_shared = 0;
	private JSONArray array = new JSONArray();
	private int proximity;
	
	//Proximity constants
	public static final int PROXIMITY_IMMEDIATE = 0;
	public static final int PROXIMITY_NEAR = 1;
	public static final int PROXIMITY_FAR = 2;
	
	//Swype constants
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	
	private boolean swipeDown = true;
	private boolean isExpanded = false;
	private boolean shouldSwype = true;
	
	private Context context;
	private Resources res;
	
	//--------------------------------------------------------------------------
	//------------------------------CONSTRUCTORS--------------------------------
	//--------------------------------------------------------------------------
	
	/**
	 * Called when the view is created.
	 * Also for having a functional flyer, createBeacon() and setBeaconProperties() should be called
	 * 
	 * This method instantiates all the view objects and sets the properties like listeners or webSettings
	 * @param context The context application
	 */
	@SuppressLint("SetJavaScriptEnabled")
	public BeaconFlyer(Context context) {
		super(context);
		this.context = context;
		
		res = context.getResources();
		
		LinearLayout.LayoutParams flyer_params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		setLayoutParams(flyer_params);
		
		View view = inflate(context, R.layout.view_flyer, null);
		addView(view);
		
		web = (WebView)findViewById(R.id.beaconContent);
		logo = (ImageView)findViewById(R.id.logo);
		title = (TextView)findViewById(R.id.beaconTitle);
		share = (ImageView)findViewById(R.id.share);
		star = (ImageView)findViewById(R.id.star);
		fullscreen = (ImageView)findViewById(R.id.fullscreen);
		header = (RelativeLayout)findViewById(R.id.header);
		
		//webview properties javascript is used by youtube
		web.getSettings().setUseWideViewPort(true);
		web.getSettings().setJavaScriptEnabled(true);
		
		setOnTouchListener(this);
		
		star.setOnClickListener(this);
		share.setOnClickListener(this);
		fullscreen.setOnClickListener(this);
	}
	
	//--------------------------------------------------------------------------
	//--------------------------------METHODS-----------------------------------
	//--------------------------------------------------------------------------
	
	/**
	 * Sets the minor and major to this BeaconFlyer.
	 * 
	 * @param major minor of this beacon
	 * @param minor major of this beacon
	 */
	public void createBeacon(int major, int minor){
		this.major = major;
		this.minor = minor;
	}
	
	/**
	 * When a BeaconFlyer is created, in it's parent fragment it's executed an API call that returns
	 * all the properties that should be set to this flyer.
	 * 
	 * Also checks if this particular beacon is saved in the db, if it's, we change the button image.
	 * 
	 * @param jsonProps The response received from the server
	 */
	public void setBeaconProperties(JSONObject jsonProps){
		
		Typeface arial = Typeface.createFromAsset(context.getAssets(), "fonts/arial.ttf");
		Typeface courier = Typeface.createFromAsset(context.getAssets(), "fonts/courier.ttf");
		Typeface futura = Typeface.createFromAsset(context.getAssets(), "fonts/futura.ttf");
		Typeface georgia = Typeface.createFromAsset(context.getAssets(), "fonts/georgia.ttf");
		Typeface gillsans = Typeface.createFromAsset(context.getAssets(), "fonts/gillsans.ttf");
		Typeface helvetica = Typeface.createFromAsset(context.getAssets(), "fonts/helvetica.ttf");
		Typeface snellroundhand = Typeface.createFromAsset(context.getAssets(), "fonts/snellroundhand.ttf");
		Typeface trebuchet = Typeface.createFromAsset(context.getAssets(), "fonts/trebuchet.ttf");
		
		try {
			web.loadUrl(jsonProps.getString("content_path"));
			title.setText(jsonProps.getString("header_title"));
			title.setTextColor(Color.parseColor(jsonProps.getString("title_color")));
			category = jsonProps.getString("beacon_category");
			subcategory = jsonProps.getString("beacon_subcategory");
			detectRange = jsonProps.getInt("beacon_detectrange");
			theme = jsonProps.getString("header_theme");
			header.setBackgroundColor(Color.parseColor(jsonProps.getString("header_color")));
			shareURL = jsonProps.getString("share_link");
			
			if(jsonProps.getInt("beacon_shareable") != 1){
				removeView(share);
			}
			
			if(jsonProps.getString("header_typography").equals("Arial")){
				title.setTypeface(arial);
			} else if(jsonProps.getString("header_typography").equals("Courier")){
				title.setTypeface(courier);
			} else if(jsonProps.getString("header_typography").equals("Futura")){
				title.setTypeface(futura);
			} else if(jsonProps.getString("header_typography").equals("Georgia")){
				title.setTypeface(georgia);
			} else if(jsonProps.getString("header_typography").equals("GillSans")){
				title.setTypeface(gillsans);
			} else if(jsonProps.getString("header_typography").equals("Helvetica")){
				title.setTypeface(helvetica);
			} else if(jsonProps.getString("header_typography").equals("SnellRoundhand")){
				title.setTypeface(snellroundhand);
			} else if(jsonProps.getString("header_typography").equals("TrebuchetMS")){
				title.setTypeface(trebuchet);
			}
			
			//TODO content_type no es fa servir
			
			if(theme.equals("black")){
				star.setImageResource(R.drawable.ic_action_not_favorite_dark);
				fullscreen.setImageResource(R.drawable.ic_action_fullscreen_dark);
				share.setImageResource(R.drawable.ic_action_share_dark);
			}else if(theme.equals("white")){
				star.setImageResource(R.drawable.ic_action_not_favorite_light);
				fullscreen.setImageResource(R.drawable.ic_action_fullscreen_light);
				share.setImageResource(R.drawable.ic_action_share_light);				
			}
			
			//Check if it's favorited or not
			FavBeaconsDataSource db = new FavBeaconsDataSource(context);
			db.open();
			if(db.isBeaconFavorited(major, minor)){
				if(theme.equals("black")){
					star.setImageResource(R.drawable.ic_action_favorite_dark);
				}else if(theme.equals("white")){
					star.setImageResource(R.drawable.ic_action_favorite_light);
				}				
				isFavorited = true;
			}
			db.close();
			
			if(MainActivity.isOnline(context)){
				downloadFavicon(jsonProps.getString("header_favicon"));			
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Called when a button is clicked, first sorts between buttons and then execute its action.
	 */
	@Override
	public void onClick(View v) {
		if(v == star){
			onFavoriteClick();
		} else if(v == fullscreen){
			onFullScreenClick();
		} else if(v == share){
			onShareClick();
		}
	}
	
	/**
	 * Called when the favorite button is clicked.
	 * 
	 * Check if it's favorited, and then, changes:
	 * 			- image button
	 * 			- database information
	 * 
	 * Also, if the beacon is in favorite view, when the user tries to defavorite one beacon,
	 * it shows a dialog of confirmation
	 */
	private void onFavoriteClick(){
		FavBeaconsDataSource db = new FavBeaconsDataSource(context);
		db.open();
		//check if beacon is favorited
		if(!db.isBeaconFavorited(major, minor)){ //not favorited
			// add beacon to the database
			db.addFavBeacon(major, minor);
			//change icon to favorite according with the beacon theme
			if(theme.equals("black")){					
				star.setImageResource(R.drawable.ic_action_favorite_dark);
			}else if(theme.equals("white")){
				star.setImageResource(R.drawable.ic_action_favorite_light);
			}
			isFavorited = true;
		} else { //already favorited
			//check if we are in favorite view
			if(isInFavoriteView){ // it's in favorite view
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
   				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
   			        public void onClick(DialogInterface d, int id) {
   			        	//We create another FavBeaconsDataSource because we are inside an
   			        	//interface, so we can't use the instance started before.
   			        	FavBeaconsDataSource db = new FavBeaconsDataSource(context);
   						db.open();
   						// remove from the database
   						db.removeFavBeacon(major, minor);
   						// change icon to not favorite according to the beacon theme
   						if(theme.equals("black")){
       						star.setImageResource(R.drawable.ic_action_not_favorite_dark);
   						}else if(theme.equals("white")){
   							star.setImageResource(R.drawable.ic_action_not_favorite_light);
   						}
   						isFavorited = false;
   						db.close();
   			        }
   			    });
   				builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
   			        public void onClick(DialogInterface dialog, int id) {
   			        	   
   			        }
   			    });
   				builder.setTitle(res.getString(R.string.dialog_defavorite_title));
       			builder.setMessage(res.getString(R.string.dialog_defavorite_message));
       			builder.setCancelable(false);
   				builder.show();
			}else{ //it's not in favorite view, so it's in nearby
				// remove from the database
				db.removeFavBeacon(major, minor);
				// change icon to not favorite according to the beacon theme
				if(theme.equals("black")){
					star.setImageResource(R.drawable.ic_action_not_favorite_dark);
				}else if(theme.equals("white")){
					star.setImageResource(R.drawable.ic_action_not_favorite_light);
				}					
				isFavorited = false;
			}
		}
		db.close();
	}
	
	/**
	 * Called when the FullScreen button is clicked or by the adapter for minimize
	 * 
	 * Send the order of changing to maximized/minimized using the listener,
	 * and changes the icon according to the theme and if it's expanded or not.
	 * 
	 * public for allowing minimize from the {@link: BeaconScrollAdapter}
	 */
	public void onFullScreenClick(){
		if(!isExpanded){
			mListener.onExpand();
			isExpanded = true;
			if(theme.equals("black")){
				fullscreen.setImageResource(R.drawable.ic_action_return_from_fullscreen_dark);
			}else if(theme.equals("white")){
				fullscreen.setImageResource(R.drawable.ic_action_return_from_fullscreen_light);
			}				
		} else{
			mListener.onDexpand();
			isExpanded = false;
			if(theme.equals("black")){
				fullscreen.setImageResource(R.drawable.ic_action_fullscreen_dark);
			}else if(theme.equals("white")){
				fullscreen.setImageResource(R.drawable.ic_action_fullscreen_light);
			}				
		}
	}
	
	/**
	 * Called when the share button is clicked.
	 * 
	 * Starts a new ShareDialog object, and show it.
	 */
	private void onShareClick() {
		ShareDialog dialog = new ShareDialog(context);
		dialog.setActualBeacon(this);
		dialog.show();
	}
	
	/**
	 * Called by the app when a beacon will be destroyed.
	 * 
	 * Starts the AsyncTask request.
	 */
	public void sendAnalytics(){
		if(MainActivity.isOnline(context)){
			sendAnalyticsRequest();
		}
	}
	
	/**
	 * Called by this class when a distance is set.
	 * 
	 * It updates the proximity field according to the distance.
	 */
	private void checkProximity(){
		double accurateDistance = distance;
		
		if(accurateDistance >= 5){
			proximity = PROXIMITY_FAR;
		} else if (accurateDistance >= 1){
			proximity = PROXIMITY_NEAR;			
		} else {
			proximity = PROXIMITY_IMMEDIATE;			
		}
		
		//TODO remove LOG
		Log.e("RANGING", major+" - "+minor+" - "+proximity);
	}
	
	/**
	 * Called by this class when a distance is set.
	 * 
	 * It adds the time and proximity information to the analytics array.
	 */
	private void addTimeInterval(){
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		String time = df.format(Calendar.getInstance().getTime());
		
		try {			
			boolean addIt = false;
			
			if(array.length() <= 0){
				addIt = true;
			}else{
				JSONObject lastObject = new JSONObject(array.getString(array.length()-1));
				
				if(proximity != lastObject.getInt("distance")){
					addIt = true;
				}
			}
						
			if(addIt){
				JSONObject objToAdd = new JSONObject();
				objToAdd.put("start_time", time);
				objToAdd.put("distance", proximity);
				array.put(objToAdd.toString());
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	//--------------------------------------------------------------------------
	//--------------------------GETTERS AND SETTERS-----------------------------
	//--------------------------------------------------------------------------
	
	public int getMinor() {
		return minor;
	}

	public int getMajor() {
		return major;
	}

	public int getDeathCount() {
		return deathCount;
	}

	public void setDeathCount(int deathCount) {
		this.deathCount = deathCount;
	}
	
	public void increaseDeathCountByOne() {
		deathCount++;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
		checkProximity();
		addTimeInterval();
	}

	public boolean isSwypying() {
		return shouldSwype;
	}

	public void setShouldSwype(boolean shouldSwype) {
		this.shouldSwype = shouldSwype;
	}

	public String getCategory() {
		return category;
	}

	public String getSubcategory() {
		return subcategory;
	}
	
	public int getDetectRange() {
		return detectRange;
	}
	
	public boolean isInFavoriteView() {
		return isInFavoriteView;
	}

	public void setInFavoriteView(boolean isInFavoriteView) {
		this.isInFavoriteView = isInFavoriteView;
	}
	
	public boolean isExpanded() {
		return isExpanded;
	}
	
	public String getShareURL() {
		return shareURL;
	}
	
	public String getTitle(){
		return (String) title.getText();
	}
	
	//--------------------------------------------------------------------------
	//-----------------------------OTHER METHODS--------------------------------
	//--------------------------------------------------------------------------

	//TODO pensar si realment cal eliminacio per swype	
	float progress, e1x, e1y, vx, vy;
	int t1, t2, et;
	MotionEvent e2 = null;
	Calendar c = Calendar.getInstance();
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(shouldSwype){
			switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				e1x = event.getX();
				e1y = event.getY();
				t1 = (c.get(Calendar.SECOND)+60*c.get(Calendar.MINUTE))*1000+c.get(Calendar.MILLISECOND);
				break;			
			case MotionEvent.ACTION_MOVE:
				//Actualitzem totes les dades
				e2 = event;
				t2 = (c.get(Calendar.SECOND)+60*c.get(Calendar.MINUTE))*1000+c.get(Calendar.MILLISECOND);
				et = t2-t1;
				vx = (e2.getX()-e1x)/et*1000;
				vy = (e2.getY()-e1y)/et*1000;
				
				//Comprova que no et desviis, que facis un moviment "recte", si no ho es, apaga i vamonos --> animacio tornar amunt
				if((e1x - e2.getX()) > SWIPE_MAX_OFF_PATH){
					swipeDown = false;
				}
				
				//TODO afegir mes condicons que aniran eliminant posibles swipes "erronis"
				
				//Comprova que comencis amunt i acabis a baix, és a dir, que facis un swipe down
				if((e2.getY() - e1y) > SWIPE_MIN_DISTANCE
					&& Math.abs(vy) > SWIPE_THRESHOLD_VELOCITY && swipeDown){
					
					progress = (e2.getY() - e1y)/(200);
					Log.d("FLYER", "P: "+progress);
					if(progress <= 1){
						setAlpha(1-progress);
						setY(getY()+e2.getY() - e1y);
					}else{
						mListener.onRemove();
					}				
					mListener.onRemove();				
				}
				break;			
			case MotionEvent.ACTION_UP:			
				//TODO Continuar animacio sortida a la mateixa velocitat
				swipeDown = true;
				e1x = -1;
				e1y = -1;
				t1 = -1;
				t2 = -1;
				et = -1;
				vx = -1;
				vy = -1;
				e2 = null;
				break;
			default:
				break;
			}	
		}		
		
		return true;
	}
	
	/**
	 * Overrided method used by the list to check if it's the same object.
	 */
	@Override
	public boolean equals(Object object){
		
		boolean sameSame = false;
		
		if(object != null && object instanceof BeaconFlyer){
			sameSame = this.major == ((BeaconFlyer) object).getMajor() && this.minor == ((BeaconFlyer) object).getMinor();
		}
		
		return sameSame;		
	}
	
	//--------------------------------------------------------------------------
	//----------------------------SERVER REQUESTS-------------------------------
	//--------------------------------------------------------------------------
	
	/**
	 * AsyncTask for downloading the favicon image.
	 * 
	 * This method doesn't save it to the SD Card/internal memory.
	 */
	private void downloadFavicon(String url){
		ImageLoader imageLoader = RequestController.getInstance(context).getImageLoader();
		 
		// If you are using normal ImageView
		imageLoader.get(url, new ImageListener() {
		 
		    @Override
		    public void onErrorResponse(VolleyError error) {
		    }
		 
		    @Override
		    public void onResponse(ImageContainer response, boolean arg1) {
		        if (response.getBitmap() != null) {
		        	logo.setImageBitmap(response.getBitmap());
		        }
		    }
		});
	}
	
	/**
	 * Send analytics using volley.
	 */
	private void sendAnalyticsRequest(){
		// Tag used to cancel the request
		String tag_json_obj = "json_obj_analytics_request";
		 
		String url = getResources().getString(R.string.base_domain)+"/api/analytics.php";
		
		Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {		 
			@Override
			public void onResponse(JSONObject response) {
				Log.e("SERVER-ANALYTICS", response.toString());
				try {
					if(response.getBoolean("success")){
						//me la peta
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
		
		final int beacon_favorited = isInFavoriteView?2:(isFavorited?1:0);
		
		final JSONObject share = new JSONObject();
		try {
			share.put("Facebook", facebook_shared);
			share.put("Twitter", twitter_shared);
			share.put("Mail", mail_shared);
			share.put("Message", sms_shared);
		} catch (JSONException e) {
			e.printStackTrace();
		}
 		
 		//TODO remove LOG
 		Log.e("SERVER-ANALYTICS", "major "+major);
 		Log.e("SERVER-ANALYTICS", "minor "+minor);
 		Log.e("SERVER-ANALYTICS", "userid "+context.getSharedPreferences("beaconsSettings", Context.MODE_PRIVATE).getInt("userid", 0));
 		Log.e("SERVER-ANALYTICS", "beacon_favorited "+beacon_favorited);
 		Log.e("SERVER-ANALYTICS", "beacon_shared "+share.toString());
 		Log.e("SERVER-ANALYTICS", "beacon_tappedLinks "+null);
 		Log.e("SERVER-ANALYTICS", "beacon_time_intervals "+(isInFavoriteView?null:array.toString()));
 		
 		//TODO tappedLinks ara sempre és null, buscar manera de implementar-ho
		
		// Creating the json request
		JsonObjectRequest jsonObjReq = new JsonObjectRequest(Method.PUT, url, null, responseListener, responseErrorListener){
			@Override
			public byte[] getBody() {
				String params = "&major="+major+
								"&minor="+minor+
								"&userid="+context.getSharedPreferences("beaconsSettings", Context.MODE_PRIVATE).getInt("userid", 0)+
								"&beacon_favorited="+beacon_favorited+
								"&beacon_shared="+share.toString()+
								"&beacon_tappedLinks="+null+
								"&beacon_time_intervals="+(isInFavoriteView?null:array.toString());
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
}
