/*
  Copyright:
  Walker Zhao (the owner of this git repo) takes full privilege and ownership of the code.
  No distribution by anyone else.
  Will change to public license when the code is ready at owner's announcement.
 */

package com.skydeck.angelguardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private class WifiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mWifiScanResults = mWifi.getScanResults();
            final String request = createLocationRequest();
            Log.d(TAG, "request = " + request);
            new HttpPostTask().execute("https://angelguardian.com/location", request);
        }
    }

    public static final String TAG = "AngelGuardian";
    public static final String key = ""; // Do not put anything other than empty string.
    public static final String username = ""; // Do not put anything other than empty string.

    private TextView mGeoView;
    private TextView mRgeoView;

    private WifiManager mWifi;
    private List<ScanResult> mWifiScanResults;
    private WifiScanReceiver mReceiver;
    private LocationXmlParser.StreetAddress mAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        RelativeLayout location = (RelativeLayout) findViewById(R.id.location);
        location.setVisibility(View.VISIBLE);

        RelativeLayout account = (RelativeLayout) findViewById(R.id.account);
        account.setVisibility(View.GONE);
        final Button button = (Button) account.findViewById(R.id.button_locate);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                RelativeLayout account = (RelativeLayout) findViewById(R.id.account);
                account.setVisibility(View.GONE);
                RelativeLayout location = (RelativeLayout) findViewById(R.id.location);
                location.setVisibility(View.VISIBLE);
            }
        });

        LinearLayout location_layout = (LinearLayout) findViewById(R.id.geo_location_layout);
        mGeoView = (TextView) location_layout.findViewById(R.id.geo_view);
        mGeoView.append(" --- helllo --- geo location info");
        mRgeoView = (TextView) location_layout.findViewById(R.id.rgeo_view);
        mRgeoView.append(" --- helllo --- rgeo");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mWifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!mWifi.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            mWifi.setWifiEnabled(true);
        }

        mReceiver = new WifiScanReceiver();
        registerReceiver(mReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.account_action:
                RelativeLayout account = (RelativeLayout) findViewById(R.id.account);
                account.setVisibility(View.VISIBLE);
                RelativeLayout location = (RelativeLayout) findViewById(R.id.location);
                location.setVisibility(View.GONE);
            case R.id.fetch_action:
                mWifi.startScan();
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.d(TAG, "delayed 3s to run");
//                        new HttpPostTask().execute("https://api.skyhookwireless.com/wps2/location");
//                    }
//                }, 3000);
                return true;
            // Clear the geo view fragment.
            case R.id.clear_action:
                mGeoView.setText("");
                return true;
            case R.id.map_action:
                if (mAddress == null) {
                    Toast.makeText(getApplicationContext(), "[lat,lon] is not available for location", Toast.LENGTH_LONG).show();
                    return false;
                }
                System.out.println("mAddress.lat = " + mAddress.getLat());
                System.out.println("mAddress.lon = " + mAddress.getLon());
                Intent intentMap = new Intent(this, MapsActivity.class);
                intentMap.putExtra("EXTRA_LAT", Double.toString(mAddress.getLat()));
                intentMap.putExtra("EXTRA_LON", Double.toString(mAddress.getLon()));
                // startActivity causes the Activity to start
                startActivity(intentMap);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Implementation of AsyncTask, to fetch the data in the background away from
     * the UI thread.
     */
    private class HttpPostTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                LocationXmlParser.StreetAddress address = queryLocation(params[0], params[1]);
                Log.d(TAG, "latitude: " + address.getLat() + "\tlongitude: " + address.getLon()
                        + "\nhpe: " + address.getHpe());
                mAddress = new LocationXmlParser.StreetAddress(address);
                return serialize(address);
            } catch (IOException e) {
                e.printStackTrace();
                return getString(R.string.connection_error);
            }
        }

        /**
         * Uses the logging framework to display the output of the fetch
         * operation in the log fragment.
         */
        @Override
        protected void onPostExecute(String result) {
            LocationXmlParser.StreetAddress address = deserialize(result);
            mGeoView.setText("latitude: " + address.getLat() + "\tlongitude: " + address.getLon()
                    + "\nhpe: " + address.getHpe());
            mRgeoView.setText("Street address: " + address.getStreet_number() + " " + address.getAddress_line()
                    + ", " + address.getCity() + ", " + address.getCounty() + ", " + address.getState()
                    + ", " + address.getPostal_code() + ", " + address.getCountry()
            + "\nStreet address distance to your point: " + address.getDistance_to_point());
        }
    }

    /** Initiates the fetch operation. */
    private LocationXmlParser.StreetAddress queryLocation(String urlString, String request)
            throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        OutputStream out = null;
        InputStream in = null;
        LocationXmlParser.StreetAddress address = null;

        try {
            conn.setRequestProperty("Content-Type", "text/xml");
            conn.setRequestProperty("Content-Length", String.valueOf(request.length()));
            conn.setReadTimeout(3000 /* milliseconds */);
            conn.setConnectTimeout(5000 /* milliseconds */);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            for (String header : conn.getRequestProperties().keySet()) {
                if (header != null) {
                    for (String value : conn.getRequestProperties().get(header)) {
                        Log.d(TAG, header + ":" + value);
                    }
                }
            }

            out = new BufferedOutputStream(conn.getOutputStream());
            PrintWriter pw = new PrintWriter(out);
            pw.print(request);
            pw.flush();
            pw.close();

            int status = conn.getResponseCode();
            Log.d(TAG, "status = " + status);
            if (status == HttpURLConnection.HTTP_OK) {
                in = conn.getInputStream();
            } else {
                in = conn.getErrorStream();
            }

            for (String header : conn.getHeaderFields().keySet()) {
                if (header != null) {
                    for (String value : conn.getHeaderFields().get(header)) {
                        Log.d(TAG, header + ":" + value);
                    }
                }
            }

            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            // Response from server after login process will be stored in response variable.
            String response = sb.toString();
            Log.d(TAG, "response = " + response);
            isr.close();
            reader.close();
            address = parseLocationResponse(response);
        } finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            conn.disconnect();
        }
        return address;
    }

    private String createLocationRequest() {
        List<LocationXmlParser.AccessPoint> aps = new ArrayList<>();
        for (ScanResult result : mWifiScanResults) {
            LocationXmlParser.AccessPoint ap =
                    new LocationXmlParser.AccessPoint(result.BSSID.replace(":", ""), result.level);
            // result.BSSID: AP's mac address
            // result.level: AP's signal level in dBm
            aps.add(ap);
        }
        LocationXmlParser parser = new LocationXmlParser(key, username, aps);
        return parser.createLocationRequest();
    }

    private LocationXmlParser.StreetAddress parseLocationResponse(final String response) {
        LocationXmlParser parser = new LocationXmlParser();
        return parser.parseLocationResponse(response);
    }

    /**
     * Serialize an object to a json string.
     * @param address : the object to be transformed to a json string
     * @return the json string
     */
    private String serialize(LocationXmlParser.StreetAddress address) {
        return new Gson().toJson(address);
    }

    /**
     * Deserialize a json string to an object.
     * @param addressString : the json string to be transformed back to an object
     * @return the object
     */
    private LocationXmlParser.StreetAddress deserialize(String addressString) {
        return new Gson().fromJson(addressString, LocationXmlParser.StreetAddress.class);
    }
}
