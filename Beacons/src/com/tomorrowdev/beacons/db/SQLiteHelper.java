package com.tomorrowdev.beacons.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import android.util.Log;

public class SQLiteHelper extends SQLiteOpenHelper {

  public static final String TABLE_FAV_BEACONS = "FavoriteBeacons";
  public static final String COLUMN_MAJOR = "major";
  public static final String COLUMN_MINOR = "minor";

  private static final String DATABASE_NAME = "beaconfy.db";
  private static final int DATABASE_VERSION = 1;

  // Database creation sql statement
  private static final String DATABASE_CREATE = 
		  "CREATE TABLE "+ TABLE_FAV_BEACONS + "("
				  + COLUMN_MAJOR+ " integer, "
				  + COLUMN_MINOR+ " integer);";

  public SQLiteHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase database) {
    database.execSQL(DATABASE_CREATE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.w(SQLiteHelper.class.getName(),
        "Upgrading database from version " + oldVersion + " to "
            + newVersion + ", which will destroy all old data");
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAV_BEACONS);
    onCreate(db);
  }

}
