package com.tomorrowdev.beacons.db;

import java.util.ArrayList;

import com.tomorrowdev.beacons.MM;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class FavBeaconsDataSource {

	// Database fields
	private SQLiteDatabase database;
	private SQLiteHelper dbHelper;
	
	public FavBeaconsDataSource(Context context) {
		dbHelper = new SQLiteHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public void addFavBeacon(int major, int minor) {	
	    ContentValues values = new ContentValues();
	    values.put(SQLiteHelper.COLUMN_MAJOR, major);
	    values.put(SQLiteHelper.COLUMN_MINOR, minor);
	    database.insert(SQLiteHelper.TABLE_FAV_BEACONS, null, values);
	}
	
	public void removeFavBeacon(int major, int minor){
		if(isBeaconFavorited(major, minor)){			
			database.delete(SQLiteHelper.TABLE_FAV_BEACONS,
					SQLiteHelper.COLUMN_MAJOR + "=? AND "+
					SQLiteHelper.COLUMN_MINOR + "=?", 
					new String[] {""+major, ""+minor});
		}
	}
  
	public boolean isBeaconFavorited(int major, int minor){
		final String selectQuery = "SELECT * FROM "+SQLiteHelper.TABLE_FAV_BEACONS
										   +" WHERE "+SQLiteHelper.COLUMN_MAJOR+" = "+"'"+major+"'"
										   +" AND "+SQLiteHelper.COLUMN_MINOR+" = "+"'"+minor+"'";
		Cursor cursor = database.rawQuery(selectQuery, null);
				
		boolean exists = (cursor.getCount() > 0);
		cursor.close();
		return exists;
	}
	
	public ArrayList<MM> getAllFavBeacon(){
		String selectQuery = "SELECT * FROM "+SQLiteHelper.TABLE_FAV_BEACONS;
		Cursor cursor = database.rawQuery(selectQuery, null);
		
		ArrayList<MM> favBeacons = new ArrayList<MM>(); 
		for(int i = 0; i < cursor.getCount(); i++){
			cursor.moveToPosition(i);
			favBeacons.add(new MM(cursor.getInt(0), cursor.getInt(1)));
		}
		cursor.close();
		return favBeacons;
	}
	
	public void removeAllData(){
		database.delete(SQLiteHelper.TABLE_FAV_BEACONS, null, null);
		Log.d("DB", "all data removed");
	}
}
