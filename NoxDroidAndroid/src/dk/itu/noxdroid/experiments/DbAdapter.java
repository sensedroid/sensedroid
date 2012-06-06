/*
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package dk.itu.noxdroid.experiments;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * 
 * Create, read, update and save a track to a data base on the android device.
 * 
 * Based on the note tutorial xxx TODO: write link
 *  
 * For now locks not just since the each tables for now is uniquely connected to one service only
 * might need to be changed into: db.setLockingEnabled(true); 
 * 
 *  
 *  
 * Old note class description.
 * 
 * Simple notes database access helper class. Defines the basic CRUD operations
 * for the notepad example, and gives the ability to list all notes as well as
 * retrieve or modify a specific note.
 * 
 * This has been improved from the first version of this tutorial through the
 * addition of better error handling and also using returning a Cursor instead
 * of using a collection of inner classes (which is less scalable and not
 * recommended).
 */
public class DbAdapter {

    public static final String KEY_LATITUDE = "latitude"; // previously title KEY__TITLE 
    public static final String KEY_LONGITUDE = "longitude"; // previously title KEY__BODY
    public static final String KEY_LOCATION_PROVIDER = "location_provider";
    public static final String KEY_ROWID = "_id"; // 
    public static final String KEY_TRACKUUID = "track_uuid";
    public static final String KEY_DATETIME = "date_time";
    public static final String KEY_NOX = "nox";
    public static final String KEY_TEMPERATURE = "temperature";
    public static final String TIME_STAMP_END = "time_stamp_end";
    
    
    private static final String TAG = "NoxDroidDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    /*    

    TODO:

    make a  nox add handler 

    make a track add handler
    			 and update end time


    later make a accelerometer add record handler
        
        
      */  
    
    
    /**
     * Database creation sql statement(s)
     * 
     * one statement per block / var - didn't work well in one chunk
     * 
     * 
     * NB! more on sql lite 3 datatypes:
     * http://www.sqlite.org/datatype3.html 
     * 
     * if the sql statement get complex its possible to move to an xml file
     * but for this simple example we keep it here.
     * TODO: look up reference for that - took a short look it was placed in 
     * <App>/res/raw/media_cmd_line.sql but didn't find an immediate solution.
     * 
     * previously had time: date_time datetime default current_date
     * 
     * 
     * RECORD_TIME NOT NULL DEFAULT CURRENT_TIMESTAMP
     * nb set the utc time
     * if should be get as local time then
     * 
     * 
     */
    private static final String DATABASE_CREATE_TRACKS =
        "create table tracks (_id integer primary key autoincrement, "
		+ "track_uuid text not null, time_stamp_start integer not null default current_timestamp,"
		+ "time_stamp_end integer, title text, description text, sync_flag integer, city text, country text)"
		+ ";";
    
    private static final String DATABASE_CREATE_LOCATIONS =
    		"create table locations (time_stamp integer not null default current_timestamp,"
    		+ "latitude double not null, longitude double not null,"
    		+ "location_provider text)"
    		+ ";";
    
    private static final String DATABASE_CREATE_SKYHOOK_LOCATIONS =
    		"create table skyhooklocations (time_stamp integer not null default current_timestamp,"
    		+ "latitude double not null, longitude double not null,"
    		+ "location_provider text)"
    		+ ";";

    private static final String DATABASE_CREATE_NOX =
    		"create table nox (time_stamp integer not null default current_timestamp,"
    		+ "nox double not null, temperature double not null)" 
    		+ ";";
    
    //TODO: ensure its real/double(s) which should be used.
    private static final String DATABASE_CREATE_ACCELEROMETER =
    		"create table accelerometer (time_stamp integer not null default current_timestamp,"
    		+ "x real not null, y real not null, z real not null)" 
    		+ ";";

    private static final String DATABASE_NAME = "noxdroid.db";
    private static final String DATABASE_TRACKS = "tracks";
    private static final String DATABASE_TABLE_LOCATION = "locations";
    private static final String DATABASE_TABLE_SKYHOOKLOCATION = "skyhooklocations";
    private static final int DATABASE_VERSION = 2;

    private final Context mCtx;
    
    private static DbAdapter instance; 

    // TODO: Make this class a singleton which can be shared among all services 
    // using the database 
    
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE_TRACKS);
            db.execSQL(DATABASE_CREATE_LOCATIONS);
            db.execSQL(DATABASE_CREATE_NOX);
            db.execSQL(DATABASE_CREATE_ACCELEROMETER);
            db.execSQL(DATABASE_CREATE_SKYHOOK_LOCATIONS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS tracks");
            onCreate(db);
        }
    }
    

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public DbAdapter(Context ctx) {
        this.mCtx = ctx;
    }
    
    
    public static void initInstance(Context context) {
    	//this.mCtx = ctx;
    	if (instance == null) {
        	instance = new DbAdapter(context);
        	instance.open(context);
        }
    }
    
    public static DbAdapter getInstance() {
    	return instance;
    }
    
    
    void open(Context context) throws SQLException {
        
        Log.d(TAG, "open called before mDbHelper = new DatabaseHelper(mCtx)");
    	
    	mDbHelper = new DatabaseHelper(context);
    	
    	Log.d(TAG, "after mDbHelper = new DatabaseHelper(mCtx)");
    	
        mDb = mDbHelper.getWritableDatabase();
        
        Log.d(TAG, "before return");
    }

    /**
     * Open a tracks database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public DbAdapter open() throws SQLException {
        
        Log.d(TAG, "open called before mDbHelper = new DatabaseHelper(mCtx)");
    	
    	mDbHelper = new DatabaseHelper(mCtx);
    	
    	Log.d(TAG, "after mDbHelper = new DatabaseHelper(mCtx)");
    	
        mDb = mDbHelper.getWritableDatabase();
        
        Log.d(TAG, "before return");
        return this;
    }

    public void close() {
    	
    	Log.d(TAG, "close called");
    	
        mDbHelper.close();
    }


//    /**
//     * 
//     * TODO: clean up remove
//     * 
//     * @return a track id 
//     */
//    public long createTrackId() {
//
//    	long trackId;
//    	String sql = "select track_uuid from " + DATABASE_TABLE + " order by track_uuid desc limit 1";
//    	
//    	String[] selectionArgs = null; 
//    	Cursor listCursor = mDb.rawQuery(sql, selectionArgs);
//
//    	listCursor.moveToFirst();
//    	if(! listCursor.isAfterLast()) {
//    		do {    			
//    			trackId = listCursor.getLong(0);
//    		} while (listCursor.moveToNext());
//    	} else {
//    		trackId = 0;
//    	}
//    	listCursor.close();
//    	
//    	return trackId + 1;
//    }
    
    
    /**
     * Create a new track using the latitude and longitude provided. If a track is
     * successfully created return the new rowId for that track, otherwise return
     * a -1 to indicate failure.
     * 
     * Direct update sqlite statements looks like this:
     * UPDATE tracks SET time_stamp_end=current_timestamp WHERE track_uuid="9aa57056-8d6e-4c9d-9919-79f55cd7e180";
     * 
     * @param trackUUID
     * @param latitude
     * @param longitude
     * @return
     */
//    public long createTrack(String trackUUID, double latitude, double longitude) {
    public void createTrack(String trackUUID) {
    	
        ContentValues initialValues = new ContentValues();
        
        initialValues.put(KEY_TRACKUUID, trackUUID);
        
        mDb.insert(DATABASE_TRACKS, null, initialValues);
    }
    
    public boolean endTrack(String trackUUID) {
        ContentValues args = new ContentValues();
        
        // or datetime('now')
        // TODO: fix current_timestamp now it inserts as string ""current_timestamp" :( and not interpretated as the sqlite current_timestamp
        args.put(TIME_STAMP_END, "current_timestamp");

        // note: trackUUID should be compared as string  and for that reason sorruneded with ''
        return mDb.update(DATABASE_TRACKS, args, KEY_TRACKUUID + "=" + "'" + trackUUID + "'", null) > 0;
    }
    
    
    /**
     * 
     * Create location point
     * 
     * @param latitude
     * @param longitude
     */
    public void createLocationPoint(double latitude, double longitude, String locationProvider ) {
    	
        Log.d(TAG, "createLocationPoint called");

        
        // TODO: check that latitude and longitude has value ? - long story short had probelsm when they where not added right from Location service - if they where not initialized yet etc..
        
        ContentValues initialValues = new ContentValues();
        
        // KEY_DATETIME - inserted automatically
        initialValues.put(KEY_LATITUDE, latitude);
        initialValues.put(KEY_LONGITUDE, longitude);
        initialValues.put(KEY_LOCATION_PROVIDER, locationProvider);
        
        if (locationProvider.endsWith("skyhook") ) {
        	mDb.insert(DATABASE_TABLE_SKYHOOKLOCATION, null, initialValues);
        } else {

        	mDb.insert(DATABASE_TABLE_LOCATION, null, initialValues);
        }
    }
    
    /**
     * 
     * Create NOX 'recording'
     * 
     * @param nox
     * @param temperature
     * 
     */
    public void createNox(double nox, double temperature) {

        ContentValues initialValues = new ContentValues();
        
        // KEY_DATETIME - inserted automatically
        initialValues.put(KEY_NOX, nox);
        initialValues.put(KEY_TEMPERATURE, temperature);

        mDb.insert(DATABASE_CREATE_NOX, null, initialValues);
    	
    }    
    /**
     * Delete an entry with the given rowId
     * 
     * @param rowId id of track to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteRowId(long rowId) {

        return mDb.delete(DATABASE_TRACKS, KEY_ROWID + "=" + rowId, null) > 0;
    }
    
//    TODO: make an deleteTrack
//    public boolean deleteTack(long trackId) {

    /**
     * Return a Cursor over the list of all tracks in the database
     * 
     * @return Cursor over all tracks
     */
    public Cursor fetchAllTracks() {

        return mDb.query(DATABASE_TRACKS, new String[] {KEY_ROWID, KEY_LATITUDE,
                KEY_LONGITUDE}, null, null, null, null, null);
    }

    /**
     * Return a Cursor positioned at a track that matches the given rowId
     * 
     * @param rowId id of track to retrieve
     * @return Cursor positioned to matching track, if found
     * @throws SQLException if track could not be found/retrieved
     */
    public Cursor fetchTrack(long rowId) throws SQLException {

        Cursor mCursor =

            mDb.query(true, DATABASE_TRACKS, new String[] {KEY_ROWID,
                    KEY_LATITUDE, KEY_LONGITUDE}, KEY_ROWID + "=" + rowId, null,
                    null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    
//    TODO: probably just delete this one since we might not need to update 
//    modify existing records..
    
    /**
     * Update a track using the details provided. The track to be updated is
     * specified using the rowId, and it is altered to use the latitude and longitude
     * values passed in
     * 
     * @param rowId id of track to update
     * @param latitude value to set track latitude to
     * @param longitude value to set track longitude to
     * @return true if a track was successfully updated, false otherwise
     */
    public boolean updateNote(int rowId, long trackId, double latitude, double longitude) {
        ContentValues args = new ContentValues();
        
        args.put(KEY_TRACKUUID, trackId);
        args.put(KEY_LATITUDE, latitude);
        args.put(KEY_LONGITUDE, longitude);

        return mDb.update(DATABASE_TRACKS, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
}