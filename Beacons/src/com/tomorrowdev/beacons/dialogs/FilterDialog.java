package com.tomorrowdev.beacons.dialogs;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.tomorrowdev.beacons.R;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FilterDialog extends Dialog{

	private ListView filterList;
	ArrayList<CategoryDetails> details;
	SharedPreferences prefs;

	public FilterDialog(Context context) {
		super(context);
		
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_filter, null);
		setContentView(layout);
		setTitle(context.getResources().getString(R.string.dialog_filter_title));
		filterList = (ListView) findViewById(R.id.filterList);
		
		addCategories(context);
	}

	private void addCategories(Context context) {
		details = new ArrayList<CategoryDetails>();
		
		prefs = context.getSharedPreferences("beaconsSettings", Context.MODE_PRIVATE);
		
		JSONArray string = new JSONArray();
		try {
			string = new JSONArray(prefs.getString("filterList", new JSONArray().toString()));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		for(int i = 0; i < string.length(); i++){			
			try {
				JSONObject obj = string.getJSONObject(i);			
				CategoryDetails dets = new CategoryDetails();
				if(obj.has("image_path")){
					dets.setCategoryImagePath(obj.getString("image_path"));
				}else{
					dets.setCategoryImagePath("");
				}
				dets.setCategoryImageUrl(obj.getString("image_url"));
				dets.setCategoryName(obj.getString("name"));
				dets.setChecked(obj.getBoolean("check"));
				details.add(dets);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
        filterList.setAdapter(new CustomAdapter(details, context));
        
	    filterList.setOnItemClickListener(new OnItemClickListener() {
	    	public void onItemClick(AdapterView<?> a, View v, int position, long id) {
	    		CheckBox check = (CheckBox) v.findViewById(R.id.checkBox);
	    		check.setChecked(!details.get(position).isChecked());
	    		details.get(position).setChecked(!details.get(position).isChecked());
	    	}
	    });
	}	
	
	@Override
	public void dismiss() {
		super.dismiss();
		try {
			JSONArray prefsArray = new JSONArray(prefs.getString("filterList", new JSONArray().toString()));
			JSONArray arrayToSave = new JSONArray();
			
			//We get the info from the prefs for preventing data removing
			for(int i = 0; i < prefsArray.length(); i++){
				
				boolean checkedValue = true;
				
				JSONObject obj = new JSONObject();
				obj.put("name", prefsArray.getJSONObject(i).getString("name"));
				if(prefsArray.getJSONObject(i).has("image_path")){
					obj.put("image_path", prefsArray.getJSONObject(i).getString("image_path"));
				}				
				obj.put("image_url", prefsArray.getJSONObject(i).getString("image_url"));
				
				for(CategoryDetails dets: details){
					if(dets.getCategoryName().equals(prefsArray.getJSONObject(i).getString("name"))){
						checkedValue = dets.isChecked();
					}
				}
				
				obj.put("check", checkedValue);
				
				arrayToSave.put(i, obj);
			}
			
			Editor editor = prefs.edit();
			editor.putString("filterList", arrayToSave.toString());
			editor.commit();

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public class CustomAdapter extends BaseAdapter {

	    private ArrayList<CategoryDetails> _data;
	    Context _c;

	    CustomAdapter (ArrayList<CategoryDetails> data, Context c){
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
	            v = vi.inflate(R.layout.list_item_dialog_filter, null);
	        }

	        ImageView image = (ImageView) v.findViewById(R.id.icon);
	        TextView nameView = (TextView)v.findViewById(R.id.categoryName);
	        final CheckBox checkBox = (CheckBox)v.findViewById(R.id.checkBox);
	        
	        checkBox.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					checkBox.setChecked(!details.get(position).isChecked());
		    		details.get(position).setChecked(!details.get(position).isChecked());					
				}
			});

	        CategoryDetails msg = _data.get(position);
	        //getting image from sdcard
	        File imgFile = new File(msg.getCategoryImagePath());
	        if(imgFile.exists()){
	        	Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
	            image.setImageBitmap(myBitmap);
	        }
	        nameView.setText(msg.getCategoryName());
	        checkBox.setChecked(msg.isChecked());
			return v;
	    }
	}

	public class CategoryDetails {
		boolean isChecked;
		String categoryName, categoryImagePath, categoryImageUrl;
		//boolean haveSubcategories; String[] subcategories;
		
		public boolean isChecked() {
			return isChecked;
		}
		public void setChecked(boolean isChecked) {
			this.isChecked = isChecked;
		}
		public String getCategoryName() {
			return categoryName;
		}
		public void setCategoryName(String categoryName) {
			this.categoryName = categoryName;
		}
		public String getCategoryImagePath() {
			return categoryImagePath;
		}
		public void setCategoryImagePath(String categoryImagePath) {
			this.categoryImagePath = categoryImagePath;
		}
		public String getCategoryImageUrl() {
			return categoryImageUrl;
		}
		public void setCategoryImageUrl(String categoryImageUrl) {
			this.categoryImageUrl = categoryImageUrl;
		}
	}	
}
