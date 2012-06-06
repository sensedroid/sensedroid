package dk.itu.noxdroid.tracks;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import dk.itu.noxdroid.NoxDroidApp;
import dk.itu.noxdroid.R;
import dk.itu.noxdroid.cloudservice.NoxDroidAppEngineUtils;
import dk.itu.noxdroid.database.NoxDroidDbAdapter;

//public class NoxDroidPostActivity extends Activity {
public class TracksListActivity extends ListActivity {

	private String TAG = this.getClass().getSimpleName();
	static final int DIALOG_INFINITE_PROGRESS = 0;
	private String cloudServiceURL;
	private String userName;
	private String userId;
	private NoxDroidDbAdapter mDbHelper;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		// setContentView(R.layout.main_simple);

		setContentView(R.layout.tracks_list);

		// note: based upon http://goo.gl/y5m4u - also take a look at the *real*
		// api
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		cloudServiceURL = prefs.getString(
				getString(dk.itu.noxdroid.R.string.SERVER_URL),
				"http://noxdroidcloudengine.appspot.com/add_track");

		userId = prefs.getString(getString(dk.itu.noxdroid.R.string.USER_ID),
				"test_user_id");
		userName = prefs
				.getString(getString(dk.itu.noxdroid.R.string.USER_NAME),
						"Test User Name");

		// note: sometimes a bit confused about the approach to get stuff from
		// <package>.R.string.*
		// webservice_url = prefs.getString("WEBSERVICE_URL",
		// "http://10.0.1.7:8888/add_track");
		// String server_url =
		// prefs.getString(dk.itu.noxdroid.R.string.WEBSERVICE_URL,
		// "http://10.0.1.7:8888/add_track");

		//
		// Get the global database adapter
		// - this approach needs no open commands and such its handled with the
		// adapter
		//
		mDbHelper = ((NoxDroidApp) getApplication()).getDbAdapter();

		// list view specific
		fillData();
		// registerForContextMenu(getListView());

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		// do something post to service
		// Log.d(TAG, "onListItemClick: " + position + " " + id);

		TextView trackItemUUIDText = (TextView) v
				.findViewById(R.id.trackItemUUID);
		String trackUID = (String) trackItemUUIDText.getText();

		TextView trackItemStartTimeText = (TextView) v
				.findViewById(R.id.trackItemStartTime);
		String trackStartTime = (String) trackItemStartTimeText.getText();

		TextView trackItemEndTimeText = (TextView) v
				.findViewById(R.id.trackItemEndTime);
		String trackEndTime = (String) trackItemEndTimeText.getText();

		TextView trackItemSyncFlagText = (TextView) v
				.findViewById(R.id.trackItemSyncFlag);
		String trackSyncFlag = (String) trackItemSyncFlagText.getText();

		String statusMessage = null;

		// TODO: move into a more elegant/convenient case approach
		// only post to cloud if track has start/end time etc...
		if (trackStartTime == null || trackEndTime == null
				|| trackSyncFlag.equals(1) || trackSyncFlag.equals("1")) {
			// statusMessage =
			// "Track is garbage needs both a start time and a end time";
			// statusMessage = "Track is already posted to cloud service";
			statusMessage = "Track is already posted to cloud service or Track is garbage needs both a start time and a end time";

		} else if (trackUID != null) {

			// start the SearchAsyncTask
			PostToCloudAsyncTask task = new PostToCloudAsyncTask();
			task.execute(trackUID);

			// if (NoxDroidAppEngineUtils.postForm(cloudServiceURL, trackUID,
			// userId, userName, mDbHelper)) {
			// statusMessage = "Post to cloud was successful";
			// // update page again
			// fillData();
			// } else {
			// Toast.makeText(TracksListActivity.this,
			// "Post to cloud was not successful please try again later",
			// Toast.LENGTH_SHORT).show();
			// }

		} else {
			Toast.makeText(TracksListActivity.this,
					"Post to cloud was not successful please try again",
					Toast.LENGTH_SHORT).show();
		}

		// hook post in here //
		// Intent i = new Intent(this, NoteEdit.class);
		// i.putExtra(NotesDbAdapter.KEY_ROWID, id);
		// startActivityForResult(i, ACTIVITY_EDIT);

	}

	// not sure about this one
	// @Override
	// protected void onActivityResult(int requestCode, int resultCode, Intent
	// intent) {
	// super.onActivityResult(requestCode, resultCode, intent);
	// fillData();
	//
	// }

	private void fillData() {
		// Get all of the rows from the database and create the item list
		Cursor mNotesCursor = mDbHelper.fetchAllTracks("desc");
		startManagingCursor(mNotesCursor);

		// Create an array to specify the fields we want to display in the list
		// (only TITLE)
		String[] from = new String[] { mDbHelper.KEY_ROWID,
				mDbHelper.KEY_TIME_STAMP_START, mDbHelper.KEY_TIME_STAMP_END,
				mDbHelper.KEY_TRACKUUID, mDbHelper.KEY_SYNC_FLAG };

		// and an array of the fields we want to bind those fields to (in this
		// case just trackItemText)
		int[] to = new int[] { R.id.trackItemId, R.id.trackItemStartTime,
				R.id.trackItemEndTime, R.id.trackItemUUID,
				R.id.trackItemSyncFlag };

		// Now create a simple cursor adapter and set it to display
		SimpleCursorAdapter tracksAdapter = new SimpleCursorAdapter(this,
				R.layout.tracks_row, mNotesCursor, from, to);
		setListAdapter(tracksAdapter);

		// try out bsed upon:
		// http://stackoverflow.com/questions/1254074/how-to-call-the-setlistadapter-in-android
		// myList.setAdapter(tracksAdapter);
		// but that crashed app
		// this one points out the problem
		// http://stackoverflow.com/questions/3033791/the-method-setlistadapterarrayadapter-is-undefined-for-the-type-create
		// "When you call this.setListAdapter this must extend ListActivity probably you class just extends Activity."
	}


	class PostToCloudAsyncTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {

			String trackUID = params[0];
			boolean postToCloudFlag = false;

			try {
				// post to cloud
				postToCloudFlag = NoxDroidAppEngineUtils.postForm(
						cloudServiceURL, trackUID, userId, userName, mDbHelper);

			
			} catch (Exception e) {
				Log.e("SearchAsyncTask", "can't search photos", e);
			} finally {
				// dismiss the dialog
				dismissDialog(DIALOG_INFINITE_PROGRESS);
			}

			return trackUID;
		}

		@Override
		protected void onPreExecute() {
			// while posting to cloud, show a dialog with infinite progress
			showDialog(DIALOG_INFINITE_PROGRESS);

		}
		
		@Override
		protected void onPostExecute(String trackUID) {
			// upload to cloud was succesfull
			// update page again
			fillData();
		}
		
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		
		if(DIALOG_INFINITE_PROGRESS == id) {
			return ProgressDialog.show(this, "", "Posting track to cloud, please wait...", true);
		}
		return super.onCreateDialog(id);
	}

}