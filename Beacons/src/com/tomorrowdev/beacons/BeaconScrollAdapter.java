package com.tomorrowdev.beacons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;

/**
 * Custom PagerAdapter used by the FlyersViewPager to show our BeaconFlyers(views)
 * 
 * Based on: http://stackoverflow.com/questions/13664155/dynamically-add-and-remove-view-to-viewpager
 * and the source code of android.widget.ArrayAdapter
 * 
 * @author Gabriel Esteban
 *
 */
public class BeaconScrollAdapter extends PagerAdapter{
	
	List<BeaconFlyer> mBeaconsInView = new ArrayList<BeaconFlyer>();
	List<BeaconFlyer> mBeaconsFiltered = new ArrayList<BeaconFlyer>();
	private ViewPager pager;
	
	private SharedPreferences prefs;
	
	private final Object mLock = new Object();	

	private boolean hasFlyersExtended = false;
	
	/**
	 * Public constructor for creating a BeaconScrollAdapter object.
	 * 
	 * The context is used for getting XML animation resources
	 * @param context the application context
	 */
	public BeaconScrollAdapter(Context context) {
		prefs = context.getSharedPreferences("beaconsSettings", Context.MODE_PRIVATE);
	}
	
	/**
	 * Used by ViewPager
	 */
    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }
    
    /**
     * Used by ViewPager
     * Called when ViewPager needs a page to display. We pass the BeaconFlyer object from mBeaconInView depending
     * on the position that it's requested.
     * 
     * Also we start the animation, because this method will only be called at the beginning because our
     * OffScreenPageLimit is always equal to mBeaconsInView.size()+1
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        ViewPager pager = (ViewPager) container;
        View view = mBeaconsInView.get(position); 
		
        pager.addView(view);        
 
        return view;
    }

    /**
     * Used by ViewPager
     * Called when ViewPager no longer needs a page to display. In other words, it's called when we remove an item.
     */
	@Override
    public void destroyItem(ViewGroup container, int position, Object view) {
        container.removeView((View) view);
    }
	
	/**
	 * Used by ViewPager
	 * Called when the ViewPager need to know where to display an item.
	 * 
	 * @return int position from left-to-right
	 * 		   POSITION_NONE when the page no longer exists
	 */
	@Override
    public int getItemPosition(Object object) {
        if (mBeaconsInView.contains((BeaconFlyer) object)) {
            return mBeaconsInView.indexOf((BeaconFlyer) object);
        } else {
            return POSITION_NONE;
        }
    }
	
	/**
	 * Used by ViewPager
	 * 
	 * @return int total number of views that ViewPager can display
	 */
	@Override
	public int getCount() {
		return mBeaconsInView.size();
	}
	
	/**
	 * Used by the app
	 * Called for adding a flyer in last position to the ViewPager
	 * 
	 * @param flyer The flyer to add
	 * @return int position where have been added
	 */
	public int add(BeaconFlyer flyer){
	    return add(flyer, mBeaconsInView.size());
	}
	
	/**
	 * Used by the app
	 * Called for adding a flyer in a specific position
	 * 
	 * @param flyer The flyer to be added
	 * @param position The position of the flyer to be added
	 * @return int position where have been added
	 */
	public int add(BeaconFlyer flyer, int position){
		synchronized (mLock) {
			mBeaconsInView.add(position, flyer);
		    notifyDataSetChanged();
		    return position;
		}	    
	}
	
	/**
	 * Used by the app
	 * Called for removing a flyer from the ViewPager
	 * 
	 * @param position The flyer position to remove
	 */
	public void removeView(int position){
		synchronized (mLock) {
			int readingPosition = pager.getCurrentItem();
		    pager.setAdapter (null);
		    mBeaconsInView.remove(position);
		    pager.setAdapter (this);
		    if(mBeaconsInView.size() > 0){
		    	if(readingPosition > position){
			    	pager.setCurrentItem(readingPosition-1);
			    }else if(readingPosition < position){
			    	pager.setCurrentItem(readingPosition);
			    }else if(readingPosition == position){
			    	pager.setCurrentItem(0);
			    }
		    }
		}	    	    
	}
	
	/**
	 * Used by the app
	 * Called for removing a group of flyers from the ViewPager
	 * 
	 * @param positions Positions to remove
	 */
	public void removeViews (final ArrayList<Integer> positions){
		synchronized (mLock) {
			int readingPosition = pager.getCurrentItem();
			BeaconFlyer readingFlyer = null;
			if(!positions.contains(readingPosition)){
				readingFlyer = mBeaconsInView.get(readingPosition);
			}
		    pager.setAdapter (null);
		    List<BeaconFlyer> beaconsToRemove = new ArrayList<BeaconFlyer>();
			for(int i = 0; i < positions.size(); i++){
				mBeaconsInView.get(positions.get(i)).sendAnalytics();
				beaconsToRemove.add(mBeaconsInView.get(positions.get(i)));
			}
			mBeaconsInView.removeAll(beaconsToRemove);
		    pager.setAdapter (this);
		    if(!positions.contains(readingPosition)){
		    	pager.setCurrentItem(mBeaconsInView.indexOf(readingFlyer));
		    }else if(mBeaconsInView.size() > 0){
		    	pager.setCurrentItem(0);
		    }
		}
	}
	
	/**
	 * Used by the app
	 * Called for sorting the flyers by distance.
	 * 
	 * Uses the DistanceComparator
	 */
	public void sortByDistance(){
		synchronized (mLock) {
			Collections.sort(mBeaconsInView, new DistanceComparator());
			notifyDataSetChanged();
		}
	}
	
	/**
	 * Used by the app
	 * Called for removing the flyers from the categories that are not selected
	 */
	public void filterByCategories(){
		ArrayList<Integer> positionsToRemove = new ArrayList<Integer>(); 
		synchronized (mLock) {			
			try{
				JSONArray categoriesArray = new JSONArray(prefs.getString("filterList", new JSONArray().toString()));			
				for(int i = 0; i < mBeaconsInView.size(); i++){
					for(int x = 0; x < categoriesArray.length(); x++){
						if(categoriesArray.getJSONObject(x).getString("name").equals(mBeaconsInView.get(i).getCategory())){
							if(!categoriesArray.getJSONObject(x).getBoolean("check")){
								positionsToRemove.add(i);
							}
						}
					}
				}
			}catch(JSONException e){
				e.printStackTrace();
			}
		}		
		
		if(positionsToRemove.size() > 0){
			hideViews(positionsToRemove);
		}
	}
	
	/**
	 * Used by app.
	 * Called on the onDestroy for sending all the analytics from the beacons that are in view.
	 */
	public void sendAllAnalytics(){
		for(int i = 0; i < mBeaconsInView.size(); i++){
			mBeaconsInView.get(i).sendAnalytics();
		}
	}
	
	/**
	 * Used by the app.
	 * Called to set the ViewPager and be able to remove things
	 * 
	 * @param pager ViewPager to be set
	 */
	public void setViewPager(ViewPager pager){
		this.pager = pager;
	}
	
	/**
	 * Used by the app.
	 * Gets the View from mBeaconsFiltered and add it to ViewPager.
	 * 
	 * @param major The major of the flyer to be showed
	 * @param minor The minor of the flyer to be showed
	 * @return false when the view is not filtered, and true when it's
	 */
	public boolean showView(int major, int minor){
		synchronized (mLock) {
			for(int i = 0; i < mBeaconsFiltered.size(); i++){
				if(mBeaconsFiltered.get(i).getMajor() == major && mBeaconsFiltered.get(i).getMinor() == minor){					
					try{
						JSONArray categoriesArray = new JSONArray(prefs.getString("filterList", new JSONArray().toString()));			
						for(int x = 0; x < categoriesArray.length(); x++){
							if(categoriesArray.getJSONObject(x).getString("name").equals(mBeaconsFiltered.get(i).getCategory())){
								if(categoriesArray.getJSONObject(x).getBoolean("check")){
									add(mBeaconsFiltered.get(i));
									mBeaconsFiltered.remove(i);
									return true;
								}
							}
						}
					}catch(JSONException e){
						e.printStackTrace();
					}
				}
			}
			return false;
		}		
	}
	
	/**
	 * Used by the app.
	 * Gets the beacon from mBeaconInView, and save it in mBeaconsFiltered
	 * 
	 * @param position The flyer position to hide
	 */
	public void hideView(int position){
		synchronized (mLock) {
			mBeaconsFiltered.add(mBeaconsInView.get(position));
			removeView(position);			
		}
	}
	
	/**
	 * Used by the app.
	 * Gets the beacons from mBeaconInView, and save them in mBeaconsFiltered
	 * 
	 * @param positions The flyer positions to hide
	 */
	public void hideViews(final ArrayList<Integer> positions){
		synchronized (mLock) {
			for(int i = 0; i < positions.size(); i++){
				mBeaconsFiltered.add(mBeaconsInView.get(positions.get(i)));
			}
			removeViews(positions);			
		}
	}
	
	/**
	 * Used by the app.
	 * Checks whether a beacon is filter or not.
	 * 
	 * @param major the major of the flyer to be checked
	 * @param minor the minor of the flyer to be checked
	 * @return Whether it's filtered or not
	 */
	public boolean isFiltered(int major, int minor){
		for(int i = 0; i < mBeaconsFiltered.size(); i++){
			if(mBeaconsFiltered.get(i).getMajor() == major && mBeaconsFiltered.get(i).getMinor() == minor){					
				return true;
			}
		}
		return false;
	}

	/**
	 * Minimizes the flyer that is extended.
	 */
	public void minimizeFlyerExtended(){
		for(BeaconFlyer item : mBeaconsInView){
			if(item.isExpanded()){
				item.onFullScreenClick();
			}
		}
	}
	
	/**
	 * Checks if this {@link: ViewPager} has any Flyer extended.
	 * 
	 * @return whether it has a flyer extended or not
	 */
	public boolean hasFlyersExtended() {
		return hasFlyersExtended;
	}

	/**
	 * Sets a new value for hasFlyersExtended
	 * 
	 * @param hasFlyersExtended the value to update if we have flyers extended or not
	 */
	public void setFlyerExtended(boolean hasFlyersExtended) {
		this.hasFlyersExtended = hasFlyersExtended;
	}
	
	/**
	 * Used by the adapter, sortByDistance().
	 * 
	 * Compares if a distance is higher or lower.
	 * 
	 * @author Gabriel Esteban
	 */
	public class DistanceComparator implements Comparator<BeaconFlyer>{
		@Override
		public int compare(BeaconFlyer beaconA, BeaconFlyer beaconB) {
			return (int)(beaconA.getDistance() - beaconB.getDistance());
		}
	}
}
