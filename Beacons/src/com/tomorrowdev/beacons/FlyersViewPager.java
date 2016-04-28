package com.tomorrowdev.beacons;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Custom implementation of {@link: ViewPager} that allows to disable the paging.
 * 
 * @author Gabriel Esteban
 *
 */
public class FlyersViewPager extends ViewPager {

	private boolean pagingEnabled;
	
	/**
	 * Public constructor for this custom ViewPager
	 * 
	 * @param context the application context
	 * @param attrs the {@link: AttributeSet} for working via XML
	 */
	public FlyersViewPager(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    this.pagingEnabled = true;
	}
	
	/**
	 * Overrided method for handling the paging disabling of the ViewPager.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
	    if (this.pagingEnabled) {
	        return super.onTouchEvent(event);
	    }	
	    return false;
	}
	
	/**
	 * Overrided method for handling the paging disabling of the ViewPager.
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
	    if (this.pagingEnabled) {
	        return super.onInterceptTouchEvent(event);
	    }
	
	    return false;
	}
	
	/**
	 * Enables or disables the paging of the {@link: ViewPager}
	 * 
	 * @param enabled whether we want the paging enabled or disabled
	 */
	public void setPagingEnabled(boolean enabled) {
	    this.pagingEnabled = enabled;
	}
}
