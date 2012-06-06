package dk.itu.noxdroid.experiments;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import dk.itu.noxdroid.service.NoxDroidService;

public class ConnectivityTest extends AsyncTask<Object, Void, Boolean> {
	ProgressBar pb;
	NoxDroidService context;
	Handler messenger;
	private String TAG = "NoxDroid_ConnectivityTest"; 

	@Override
	protected void onPreExecute() {
		
	}

	@Override
	protected Boolean doInBackground(Object... params) {
		try {
			context = (NoxDroidService) params[0];
			
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
		final ConnectivityManager connMan = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		final NetworkInfo wifiInfo = connMan
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		final NetworkInfo mobileInfo = connMan
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		return wifiInfo.isAvailable() || mobileInfo.isAvailable();
	}

	@Override
	protected void onPostExecute(Boolean result) {
		Log.d(TAG, "PostExecute ConnTest");
		if (result) {
			
//			Message.obtain(messenger,
//					NoxDroidService.STATUS_CONNECTIVITY_SUCCESS).sendToTarget();
			context.notify(NoxDroidService.STATUS_CONNECTIVITY_OK);

		} else {
//			Message.obtain(messenger,
//					NoxDroidService.STATUS_CONNECTIVITY_FAILURE).sendToTarget();
			context.notify(NoxDroidService.ERROR_NO_CONNECTIVITY);
		}
	}
}