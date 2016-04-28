/**
 * Created on 09/09/2014 by Gabriel Esteban
 * Tomorrow Developers SCP, 2014
 */
package com.tomorrowdev.beacons.volley;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import android.content.Context;
import android.text.TextUtils;

/**
 * Created following @link http://www.androidhive.info/2014/05/android-working-with-volley-library-1/
 * 
 * @author Gabriel Esteban
 *
 */
public class RequestController {
	 
    public static final String TAG = RequestController.class.getSimpleName();
 
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
 
    private static RequestController mInstance;
    
    private Context context;

    public static synchronized RequestController getInstance(Context context) {
    	if(mInstance == null){
    		mInstance = new RequestController();
    	}
    	mInstance.context = context;
        return mInstance;
    }
 
    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(context);
        }
 
        return mRequestQueue;
    }
 
    public ImageLoader getImageLoader() {
        getRequestQueue();
        if (mImageLoader == null) {
            mImageLoader = new ImageLoader(this.mRequestQueue,
                    new LruBitmapCache());
        }
        return this.mImageLoader;
    }
 
    public <T> void addToRequestQueue(Request<T> req, String tag) {
        // set the default tag if tag is empty
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        getRequestQueue().add(req);
    }
 
    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }
 
    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }
}
