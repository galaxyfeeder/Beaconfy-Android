package com.tomorrowdev.beacons.dialogs;

import java.util.ArrayList;
import java.util.List;

import com.tomorrowdev.beacons.BeaconFlyer;
import com.tomorrowdev.beacons.DummyFacebookActivity;
import com.tomorrowdev.beacons.R;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ShareDialog extends Dialog{

	private ListView shareList;
	private ArrayList<AppDetails> details;
	private BeaconFlyer flyer;
	private Context context;
	private Resources res;

	/**
	 * Constructor for this dialog.
	 * For updating the analytics of the beacon who requested this dialog is necessary
	 * to call {@link: setActualBeacon()} after this constructor.
	 * 
	 * @param context the application context
	 */
	public ShareDialog(Context context) {
		super(context);
		this.context = context;
		
		res = context.getResources();
		
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_share, null);
		setContentView(layout);
		setTitle(res.getString(R.string.dialog_share_title));
		shareList = (ListView) findViewById(R.id.shareList);		
	}
	
	/**
	 * Set the flyer who called the dialog for updating the analytics data.
	 * 
	 * @param flyer the flyer who started the dialog
	 */
	public void setActualBeacon(BeaconFlyer flyer){
		this.flyer = flyer;
		addApps();
	}
	
	private void addApps() {
		details = new ArrayList<AppDetails>();
		
		AppDetails mFacebook = null;
		AppDetails mTwitter = null;
		AppDetails mGmail = null;
		AppDetails mMail = null;
		
		//checking the apps that are installed
		PackageManager pm = context.getPackageManager();
	    Intent sendIntent = new Intent(Intent.ACTION_SEND);     
	    sendIntent.setType("text/plain");
	    
	    List<ResolveInfo> resInfo = pm.queryIntentActivities(sendIntent, 0); 
	    for (int i = 0; i < resInfo.size(); i++) {
	    	String packageName = resInfo.get(i).activityInfo.packageName;
	        if(packageName.contains("android.email")) {
	        	mMail  = new AppDetails("Mail", R.drawable.ic_launcher_email, packageName);
	        	details.add(mMail);	
	        }else if(packageName.contains("android.gm")){
	        	mGmail  = new AppDetails("Gmail", R.drawable.ic_launcher_mail, packageName);
	        	details.add(mGmail);
	        }else if(packageName.contains("twitter")){
	        	mTwitter  = new AppDetails("Twitter", R.drawable.ic_launcher_twitter, packageName);
	        	details.add(mTwitter);
	        }else if(packageName.contains("katana")){
	        	mFacebook  = new AppDetails("Facebook", R.drawable.icon_katana, packageName);
	        	details.add(mFacebook);
	        }
	    }
		
		final String message = res.getString(R.string.dialog_share_shareContent_1)+" "
							  +flyer.getTitle()+" "
							  +res.getString(R.string.dialog_share_shareContent_2)+" "
							  +flyer.getShareURL();
		final String messageFB = res.getString(R.string.dialog_share_shareContent_1)+" "
							  +flyer.getTitle()+" "
							  +res.getString(R.string.dialog_share_shareContent_2);
	    final String subject = res.getString(R.string.dialog_share_subject);
		
	    final AppDetails mF = mFacebook;
	    final AppDetails mT = mTwitter;
	    final AppDetails mG = mGmail;
	    final AppDetails mE = mMail;
	    
		shareList.setAdapter(new CustomAdapter(details, context));        
        shareList.setOnItemClickListener(new OnItemClickListener() {
	    	public void onItemClick(AdapterView<?> a, View v, int position, long id) {    			    		
	    		if(details.get(position).getAppName().equals(mF.getAppName())){
	    			Intent intent = new Intent(context, DummyFacebookActivity.class);
			    	intent.putExtra(Intent.EXTRA_TEXT, messageFB);
			    	intent.putExtra("EXTRA_LINK", flyer.getShareURL());
		            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
			    	context.startActivity(intent);
	    			flyer.facebook_shared++;
	    		}else if(details.get(position).getAppName().contains(mT.getAppName())){
	    			Intent intent = new Intent();
		            intent.setAction(Intent.ACTION_SEND);
		            intent.setType("text/plain");
	                intent.putExtra(Intent.EXTRA_TEXT, message);
		            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
	                intent.setPackage(mT.getPackage());
	                context.startActivity(intent);
	    			flyer.twitter_shared++;
	    		}else if(details.get(position).getAppName().contains(mG.getAppName())){
	    			Intent intent = new Intent();
		            intent.setAction(Intent.ACTION_SEND);
		            intent.setType("message/rfc822");
		            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
	                intent.putExtra(Intent.EXTRA_TEXT, message);
	                intent.setPackage(mG.getPackage());
	                context.startActivity(intent);
	    			flyer.mail_shared++;
	    		}else if(details.get(position).getAppName().contains(mE.getAppName())){
	    			Intent emailIntent = new Intent();
	    		    emailIntent.setAction(Intent.ACTION_SEND);
	    		    emailIntent.putExtra(Intent.EXTRA_TEXT, message);
	    		    emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
	    		    emailIntent.setType("message/rfc822");
	    		    emailIntent.setPackage(mE.getPackage());
	    		    context.startActivity(emailIntent);
	    			flyer.mail_shared++;
	    		}
	    		dismiss();
	    	}
	    });
	}

	public class CustomAdapter extends BaseAdapter {

	    private ArrayList<AppDetails> _data;
	    Context _c;

	    CustomAdapter (ArrayList<AppDetails> data, Context c){
	        _data = data;
	        _c = c;
	    }

	    public int getCount() {
	        return _data.size();
	    }

	    public Object getItem(int position) {
	        return _data.get(position);
	    }

	    public long getItemId(int position) {
	        return position;
	    }

	    public View getView(final int position, View convertView, ViewGroup parent) {
	    	View v = convertView;
	        if (v == null) {
	            LayoutInflater vi = (LayoutInflater)_c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            v = vi.inflate(R.layout.list_item_dialog_share, null);
	        }

	        ImageView image = (ImageView) v.findViewById(R.id.icon);
	        TextView nameView = (TextView)v.findViewById(R.id.applicationName);

	        AppDetails msg = _data.get(position);
	        image.setImageResource(msg.getImage());
	        nameView.setText(msg.getAppName());
			return v;
	    }
	}

	public class AppDetails {
		String appName, mPackage;
		int image;
		
		public AppDetails(String appName, int image, String mPackage) {
			this.appName = appName;
			this.image = image;
			this.mPackage = mPackage;
		}
		
		public String getAppName() {
			return appName;
		}
		public void setAppName(String appName) {
			this.appName = appName;
		}
		public int getImage() {
			return image;
		}
		public void setImage(int image) {
			this.image = image;
		}
		public String getPackage() {
			return mPackage;
		}
		public void setPackage(String mPackage) {
			this.mPackage = mPackage;
		}
	}	
}
