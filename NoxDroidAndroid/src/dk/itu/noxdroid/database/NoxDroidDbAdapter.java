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

package dk.itu.noxdroid.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import dk.itu.noxdroid.R;
import dk.itu.noxdroid.model.Track;

/**
 * 
 * Create, read, update and save a track to a data base on the android device.
 * 
 * Based on the note tutorial xxx TODO: write link
 * 
 * For now locks not just since the each tables for now is uniquely connected to
 * one service only might need to be changed into: db.setLockingEnabled(true);
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
public class NoxDroidDbAdapter {

	public static final String KEY_LATITUDE = "latitude"; // previously title
															// KEY__TITLE
	public static final String KEY_LONGITUDE = "longitude"; // previously title
															// KEY__BODY
	public static final String KEY_LOCATION_PROVIDER = "location_provider";
	public static final String KEY_ROWID = "_id"; //
	public static final String KEY_TRACKUUID = "track_uuid";
	public static final String KEY_DATETIME = "date_time";
	public static final String KEY_NOX = "nox";
	public static final String KEY_TEMPERATURE = "temperature";
	public static final String KEY_TIME_STAMP_START = "time_stamp_start";
	public static final String KEY_TIME_STAMP_END = "time_stamp_end";
	public static final String KEY_SYNC_FLAG = "sync_flag";

	private static String TAG;
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	/*
	 * 
	 * TODO:
	 * 
	 * make a nox add handler
	 * 
	 * make a track add handler and update end time
	 * 
	 * 
	 * later make a accelerometer add record handler
	 */

	/**
	 * Database creation sql statement(s)
	 * 
	 * one statement per block / var - didn't work well in one chunk
	 * 
	 * 
	 * NB! more on sql lite 3 datatypes: http://www.sqlite.org/datatype3.html
	 * 
	 * if the sql statement get complex its possible to move to an xml file but
	 * for this simple example we keep it here. TODO: look up reference for that
	 * - took a short look it was placed in <App>/res/raw/media_cmd_line.sql but
	 * didn't find an immediate solution.
	 * 
	 * previously had time: date_time datetime default current_date
	 * 
	 * 
	 * RECORD_TIME NOT NULL DEFAULT CURRENT_TIMESTAMP nb set the utc time if
	 * should be get as local time then
	 * 
	 * 
	 * tips: - remember to check if it exits "create table if not exists"
	 * 
	 */
	private static final String DATABASE_CREATE_TRACKS = "create table if not exists tracks (_id integer primary key autoincrement, "
			+ "track_uuid text not null, time_stamp_start integer not null default (datetime('now','localtime')),"
			+ "time_stamp_end integer, title text, description text, sync_flag integer default 0, city text, country text)"
			+ ";";

	private static final String DATABASE_CREATE_LOCATIONS = "create table if not exists locations (time_stamp integer not null default (datetime('now','localtime')),"
			+ "latitude double not null, longitude double not null,"
			+ "location_provider text)" + ";";

	private static final String DATABASE_CREATE_NOX = "create table if not exists nox (time_stamp integer not null default (datetime('now','localtime')),"
			+ "nox double not null, temperature double)" + ";";

	// TODO: ensure its real/double(s) which should be used.
	private static final String DATABASE_CREATE_ACCELEROMETER = "create table if not exists accelerometer (time_stamp integer not null default (datetime('now','localtime')),"
			+ "x real not null, y real not null, z real not null)" + ";";

	private static final String DATABASE_NAME = "noxdroid.db";
	private static final String DATABASE_TABLE_TRACKS = "tracks";
	private static final String DATABASE_TABLE_LOCATION = "locations";
	private static final String DATABASE_TABLE_NOX = "nox";
	private static final int DATABASE_VERSION = 2;

	private static NoxDroidDbAdapter instance = null;

	// TODO: Make this class a singleton which can be shared among all services
	// using the database

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.i(TAG, "Running SQL Create scripts");

			db.execSQL(DATABASE_CREATE_TRACKS);
			db.execSQL(DATABASE_CREATE_LOCATIONS);
			db.execSQL(DATABASE_CREATE_NOX);
			db.execSQL(DATABASE_CREATE_ACCELEROMETER);
			//db.execSQL(DATABASE_CREATE_SKYHOOK_LOCATIONS);
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
	 * @param ctx
	 *            the Context within which to work
	 */
	private NoxDroidDbAdapter() {
		// TODO Auto-generated constructor stub
	}

	public static void initInstance(Context context) {
		TAG = context.getApplicationContext().getString(R.string.LOGCAT_TAG,
				context.getApplicationContext().getString(R.string.app_name),
				NoxDroidDbAdapter.class.getSimpleName());
		if (instance == null) {
			instance = new NoxDroidDbAdapter();
			instance.open(context);
		}
	}

	/*
	 * Singleton approach to get the db adapter
	 */
	public static NoxDroidDbAdapter getInstance() {
		return instance;
	}

	public void open(Context context) throws SQLException {

		Log.d(TAG, "open called before mDbHelper = new DatabaseHelper(mCtx)");

		mDbHelper = new DatabaseHelper(context);

		Log.d(TAG, "after mDbHelper = new DatabaseHelper(mCtx)");

		mDb = mDbHelper.getWritableDatabase();

		Log.d(TAG, "before return");
	}

	public void close() {

		Log.d(TAG, "close called");

		mDbHelper.close();
	}


	/**
	 * Create / start a track
	 * 
	 * @param trackUUID
	 */
	public void createTrack(String trackUUID) {
		ContentValues initialValues = new ContentValues();

		initialValues.put(KEY_TRACKUUID, trackUUID);

		mDb.insert(DATABASE_TABLE_TRACKS, null, initialValues);
	}

	/**
	 * End a track - basically just a time stampe - end time
	 * 
	 * Direct update sqlite statements looks like this: UPDATE tracks SET
	 * time_stamp_end=current_timestamp WHERE
	 * track_uuid="9aa57056-8d6e-4c9d-9919-79f55cd7e180";
	 * 
	 * @param trackUUID
	 */
	public void endTrack(String trackUUID) {

		// Raw sqlite in use because we like to use the sqlite time stamp
		// mechanism
		// the previous approach args.put(TIME_STAMP_END, "current_timestamp")
		// dosen't work
		// - remember to surround UUID with ''
		String sql = "UPDATE " + DATABASE_TABLE_TRACKS + " SET "
				+ KEY_TIME_STAMP_END
				+ "=(datetime('now','localtime')) WHERE " + KEY_TRACKUUID + "='" + trackUUID
				+ "'";
		Log.d(TAG, "sql: " + sql);
		mDb.execSQL(sql);

	}
	

	/**
	 * Set track as synched 
	 * 
	 * query example:
	 * update tracks set sync_flag=1 where track_uuid="8c3adc99-3e51-4922-a3c9-d127117bb764"; 
	 *  
	 * @param trackUUID
	 */
	public void setTrackSync(String trackUUID) {

		String sql = "UPDATE " + DATABASE_TABLE_TRACKS + " SET "
				+ KEY_SYNC_FLAG
				+ "=1 WHERE " + KEY_TRACKUUID +"='" + trackUUID
				+ "'";
		Log.d(TAG, "sql: " + sql);
		mDb.execSQL(sql);

	}
	

	/**
	 * 
	 * Create location point
	 * 
	 * @param latitude
	 * @param longitude
	 */
	public void createLocationPoint(double latitude, double longitude,
			String locationProvider) {

		ContentValues initialValues = new ContentValues();

		// KEY_DATETIME - inserted automatically
		initialValues.put(KEY_LATITUDE, latitude);
		initialValues.put(KEY_LONGITUDE, longitude);
		initialValues.put(KEY_LOCATION_PROVIDER, locationProvider);

		mDb.insert(DATABASE_TABLE_LOCATION, null, initialValues);
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

		mDb.insert(DATABASE_TABLE_NOX, null, initialValues);

	}

	// /**
	// * Delete an entry with the given rowId
	// *
	// * @param rowId id of track to delete
	// * @return true if deleted, false otherwise
	// */
	// public boolean deleteRowId(long rowId) {
	//
	// return mDb.delete(DATABASE_TABLE_TRACKS, KEY_ROWID + "=" + rowId, null) >
	// 0;
	// }

	// TODO: make an deleteTrack
	// public boolean deleteTack(long trackId) {

	/**
	 * Return a Cursor over the list of all tracks in the database
	 * 
	 * @return Cursor over all tracks
	 */
	public Cursor fetchAllTracks(String order) {
		String orderBy = null;
		
		// do set "ORDER BY " its handled by the query handler
		if(order.equals("asc"))
			orderBy = KEY_ROWID+" asc";
		else if(order.equals("desc"))
			orderBy = KEY_ROWID+" desc"; // reverse		
		
		// query arguments:
		// public Cursor query (String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy)
		return mDb.query(DATABASE_TABLE_TRACKS, new String[] { KEY_ROWID,
				KEY_TRACKUUID, KEY_TIME_STAMP_START, KEY_TIME_STAMP_END, KEY_SYNC_FLAG }, null, null, null, null, orderBy);
	}

	/**
	 * Return a Cursor positioned at a track that matches the given rowId
	 * 
	 * @param rowId
	 *            id of track to retrieve
	 * @return Cursor positioned to matching track, if found
	 * @throws SQLException
	 *             if track could not be found/retrieved
	 */
	public Cursor fetchTrack(long rowId) throws SQLException {

		Cursor mCursor =

		mDb.query(true, DATABASE_TABLE_TRACKS, new String[] { KEY_ROWID,
				KEY_TRACKUUID, KEY_TIME_STAMP_START, KEY_TIME_STAMP_END }, KEY_ROWID + "=" + rowId, null,
				null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	/*
	 * 
	 * Fetch locations for a track
	 * 
	 * Each cursor consist of: latitude longitude time_stamp location_provider
	 * 
	 * Some raw sqlite queries: select count(*) from tracks, locations where
	 * track_uuid='8c3adc99-3e51-4922-a3c9-d127117bb764' and
	 * (locations.time_stamp >= tracks.time_stamp_start and locations.time_stamp
	 * <= tracks.time_stamp_end); select locations.latitude,
	 * locations.longitude, locations.time_stamp, locations.location_provider
	 * from tracks, locations where
	 * track_uuid='8c3adc99-3e51-4922-a3c9-d127117bb764' and
	 * (locations.time_stamp >= tracks.time_stamp_start and locations.time_stamp
	 * <= tracks.time_stamp_end) order by locations.time_stamp asc;
	 */
	public Cursor fetchLocations(String UUID) throws SQLException {

		String sql = "select locations.latitude, locations.longitude, locations.time_stamp, locations.location_provider " 
				+ "from tracks, locations "
				+ "where track_uuid='"
				+ UUID
				+ "' and (locations.time_stamp >= tracks.time_stamp_start and locations.time_stamp <= tracks.time_stamp_end) "
				+ "order by locations.time_stamp asc" + ";";

		Cursor mCursor = mDb.rawQuery(sql, null);

		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	/**
	 * 
	 * Fetch nox for a track
	 * 
	 * Each cursor consist of: nox, temperature, time_stamp
	 * 
	 * query example: select nox.nox, nox.temperature, nox.time_stamp from
	 * tracks, nox where track_uuid="8c3adc99-3e51-4922-a3c9-d127117bb764" and
	 * (nox.time_stamp >= tracks.time_stamp_start and nox.time_stamp <=
	 * tracks.time_stamp_end) order by nox.time_stamp asc;
	 * 
	 * @param UUID
	 * @return
	 * @throws SQLException
	 */
	public Cursor fetchNox(String UUID) throws SQLException {

		String sql = "select nox.nox, nox.temperature, nox.time_stamp "
				+ "from tracks, nox "
				+ " where track_uuid='"
				+ UUID
				+ "' and (nox.time_stamp >= tracks.time_stamp_start and nox.time_stamp <= tracks.time_stamp_end) " 
				+ " order by nox.time_stamp asc "
				+ ";";

		Cursor mCursor = mDb.rawQuery(sql, null);

		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

	
	/**
	 * 
	 * Get a single track as a track object
	 * 
	 * @param UUID
	 * @return
	 * @throws SQLException
	 */
	public Track getTrack(String UUID) throws SQLException {
		
		Track track = new Track(UUID);
		
		String sql = "select time_stamp_start, time_stamp_end, sync_flag from tracks "
			+ " where track_uuid='"
			+ UUID
			+ "'"
			+ ";";

		Cursor mCursor = mDb.rawQuery(sql, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
			track.setStartTime(mCursor.getString(0));
			track.setEndTime(mCursor.getString(1));
			track.setSyncFlag(mCursor.getInt(2));
		}		
		
		mCursor.close();
		
		// String id, String startTime, String endTime, int syncFlag				
		return track; 
	}
	
	
	
	
	
	
	
	
	
}