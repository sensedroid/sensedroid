package dk.itu.noxdroid;

import java.io.IOException;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import Pachube.Feed;
import Pachube.Pachube;
import Pachube.PachubeException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import dk.itu.noxdroid.service.NoxDroidService;
import dk.itu.noxdroid.setup.PreferencesActivity;

public class NoxDroidActivity extends Activity {

	private String TAG;
	
	private String login = "noxdroid";
    private String password = "noxdroid";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		TAG = getString(R.string.LOGCAT_TAG, getString(R.string.app_name), this
				.getClass().getSimpleName());
		setContentView(R.layout.main);
		// startActivity(new Intent(this,IOIOSensorActivity.class));
		// startActivity(new Intent(this,NoxDroidGPSActivity.class));
		testDependencies();
		testHTTP();
	}

	public void updateFeed(View view) {
		// <script type="text/javascript"
		// src="http://www.google.com/jsapi"></script><script
		// language="JavaScript"
		// src="http://apps.pachube.com/google_viz/viz.js"></script><script
		// language="JavaScript">createViz(35611,1,600,200,"1DAB07");</script>
		Pachube p = new Pachube("lFw8Nl2AhdRz2wKSXMvavSuxfjhjQBhl3efwXrmiTPk");
		try {
			TextView info = (TextView) findViewById(R.id.txtInfo);

			Feed f = p.getFeed(38346);

			info.setText(String.format("%s\n%s\n%s\n%s\n", f.getTitle(),
					f.getEmail(), f.getWebsite(), f.getStatus()));

			info.append("Connecting to Pachube Feed\n");
			f.updateDatastream(1, 230.0);
			info.append("Feed Updated\n");
		} catch (PachubeException e) {
			e.printStackTrace();
		}
	}

	public void goIOIO(View view) {
		// startActivity(new Intent(this, IOIOSensorActivity.class));
	}

	public void startService(View view) {
		startService(new Intent(this, NoxDroidService.class));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.preferences:
			Intent intent = new Intent(NoxDroidActivity.this,
					PreferencesActivity.class);
			startActivity(intent);
			// test toast:
			// Toast.makeText(this, "Just a test", Toast.LENGTH_SHORT).show();
			break;
		}
		return true;
	}

	private void testDependencies() {
		ToggleButton gps = (ToggleButton) findViewById(R.id.toggleButton1);
		ToggleButton wifi = (ToggleButton) findViewById(R.id.toggleButton2);
		ToggleButton mobile = (ToggleButton) findViewById(R.id.toggleButton3);

		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		gps.setChecked(locationManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER));

		final ConnectivityManager connMan = (ConnectivityManager) this
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo wifiInfo = connMan
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		final NetworkInfo mobileInfo = connMan
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		wifi.setChecked(wifiInfo.isAvailable());
		mobile.setChecked(mobileInfo.isAvailable());
	}

	private void testHTTP() {
		OAuthConsumer consumer = new CommonsHttpOAuthConsumer("C2fq1JGYSJ7JVTfsOQir", "IU7koQxuG1qSVf6E3z7zbR9QRZuBtiC9pCD1uAjM");
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet("http://geocommons.com/maps/111886.json");
		
		//addAuthentication(request);
		HttpEntity results = null;

		try {
			consumer.sign(request);
			HttpResponse response = client.execute(request);
			results = response.getEntity();
			Log.i(TAG, EntityUtils.toString(results));
		} catch (Exception e) {
			throw new RuntimeException("Web Service Failure");
		} finally {
			if (results != null)
				try {
					results.consumeContent();
				} catch (IOException e) {
					// empty, Checked exception but don't care
				}
		}

	}
	
    /**
     * Add basic authentication header to request
     *
     * @param request
     */
    private void addAuthentication(HttpRequestBase request) {
        String usernamePassword = login + ":" + password;
        String encodedUsernamePassword = Base64.encodeToString(usernamePassword.getBytes(), Base64.DEFAULT);
        
        request.addHeader("Authorization", "Basic " + encodedUsernamePassword);
    }
	//
	// @Override
	// public void onBackPressed() {
	// moveTaskToBack (true);
	// }
}