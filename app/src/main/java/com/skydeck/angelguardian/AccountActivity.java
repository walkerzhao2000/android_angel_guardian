/*
  Copyright:
  Walker Zhao (the owner of this git repo) takes full privilege and ownership of the code.
  No distribution by anyone else.
  Will change to public license when the code is ready at owner's announcement.
 */

package com.skydeck.angelguardian;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class AccountActivity extends AppCompatActivity {

    private class WifiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

    public static final String TAG = "AngelGuardian";

    private TextView mGeoView;
    private TextView mRgeoView;

    private WifiManager mWifi;

    private String mUser = "";
    private String mPass = "";
    private String mCryptoPass = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account);
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

        WifiInfo info = mWifi.getConnectionInfo();
        @SuppressLint("HardwareIds") String mac = info.getMacAddress();

        Crypto auth = new Crypto();
        try {
//            byte[] salt = auth.generateSalt();
            byte[] salt = mac2bytes(mac);
            mCryptoPass = bytes2Hex(auth.getEncryptedPassword(mPass, salt));
            Log.d(TAG, "CryptoPass=" + mCryptoPass);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.location_toolbar, menu);
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
//                if (mAddress == null) {
//                    Toast.makeText(getApplicationContext(), "[lat,lon] is not available for location", Toast.LENGTH_LONG).show();
//                    return false;
//                }
//                Intent intentMap = new Intent(this, MapActivity.class);
//                intentMap.putExtra("EXTRA_LAT", Double.toString(mAddress.getLat()));
//                intentMap.putExtra("EXTRA_LON", Double.toString(mAddress.getLon()));
//                // startActivity causes the Activity to start
//                startActivity(intentMap);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        savePreferences();

    }

    @Override
    public void onResume() {
        super.onResume();
        loadPreferences();
    }

    void savePreferences() {
        // Edit and commit
        LinearLayout location_layout = (LinearLayout) findViewById(R.id.geo_location_layout);
        EditText username_edit = (EditText) location_layout.findViewById(R.id.email_view);
        EditText password_edit = (EditText) location_layout.findViewById(R.id.password_view);

        String username = username_edit.getText().toString();
        String password = password_edit.getText().toString();
        Log.d(TAG, "onPause save name: " + username);
        Log.d(TAG, "onPause save password: " + password);

        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(MainActivity.PREF_USERNAME, username);
        editor.putString(MainActivity.PREF_PASSWORD, password);
        editor.commit();
    }

    void loadPreferences() {
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME,
                Context.MODE_PRIVATE);

        // Get value
        String username = settings.getString(MainActivity.PREF_USERNAME, MainActivity.DEFAULT_USERNAME);
        String password = settings.getString(MainActivity.PREF_PASSWORD, MainActivity.DEFAULT_PASSWORD);

        LinearLayout location_layout = (LinearLayout) findViewById(R.id.geo_location_layout);
        EditText username_edit = (EditText) location_layout.findViewById(R.id.email_view);
        EditText password_edit = (EditText) location_layout.findViewById(R.id.password_view);

        username_edit.setText(username);
        password_edit.setText(password);
        Log.d(TAG, "onResume load name: " + username);
        Log.d(TAG, "onResume load password: " + password);
    }

    private byte[] mac2bytes(String mac) {
        String[] macHexs = mac.split(":");
        byte[] macBytes = new byte[6];
        for (int i=0; i<6; ++i) {
            Integer hex = Integer.parseInt(macHexs[i], 16);
            macBytes[i] = hex.byteValue();
        }
        return macBytes;
    }

    private String bytes2Hex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
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
        HttpsURLConnection sconn = (HttpsURLConnection) url.openConnection();
        sconn.setInstanceFollowRedirects(false);
        URL redirectUrl = new URL(sconn.getHeaderField("Location"));
        HttpURLConnection conn = (HttpURLConnection) redirectUrl.openConnection();
        OutputStream out = null;
        InputStream in = null;
        LocationXmlParser.StreetAddress address = null;

        try {
            conn.setRequestProperty("Content-Type", "text/xml");
            conn.setRequestProperty("Content-Length", String.valueOf(request.length()));
            String credential = mUser + ":" + mCryptoPass;
            conn.setRequestProperty("Authorization", "basic " +
                    Base64.encodeToString(credential.getBytes(), Base64.NO_WRAP));
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
            if (status == HttpsURLConnection.HTTP_OK) {
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
