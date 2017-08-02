/**
 * Copyright:
 * Walker Zhao (the owner of this git repo) takes full privilege and ownership of the code.
 * No distribution by anyone else.
 * Will change to public license when the code is ready at owner's announcement.
 */

package com.skydeck.angelguardianv2;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "Angel Guardian";

    private TextView mGeoView;
    private TextView mRgeoView;

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
                new DownloadTask().execute("http://www.google.com");
                return true;
            // Clear the geo view fragment.
            case R.id.clear_action:
                mGeoView.setText("");
                return true;
            case R.id.map_action:
                Intent intentMap = new Intent(this, MapsActivity.class);
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
    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                return loadFromNetwork(urls[0]);
            } catch (IOException e) {
                return getString(R.string.connection_error);
            }
        }

        /**
         * Uses the logging framework to display the output of the fetch
         * operation in the log fragment.
         */
        @Override
        protected void onPostExecute(String result) {
            mGeoView.setText(result);
        }
    }

    /** Initiates the fetch operation. */
    private String loadFromNetwork(String urlString) throws IOException {
        InputStream stream = null;
        String str ="";

        try {
            stream = downloadUrl(urlString);
            str = readIt(stream, 500);
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
        return str;
    }

    /**
     * Given a string representation of a URL, sets up a connection and gets
     * an input stream.
     * @param urlString A string representation of a URL.
     * @return An InputStream retrieved from a successful HttpURLConnection.
     * @throws java.io.IOException
     */
    private InputStream downloadUrl(String urlString) throws IOException {
        // BEGIN_INCLUDE(get_inputstream)
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Accept", "application/json");
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Start the query
        conn.connect();
        return conn.getInputStream();
        // END_INCLUDE(get_inputstream)
    }

    /** Reads an InputStream and converts it to a String.
     * @param stream InputStream containing HTML from targeted site.
     * @param len Length of string that this method returns.
     * @return String concatenated according to len parameter.
     * @throws java.io.IOException
     */
    private String readIt(InputStream stream, int len) throws IOException {
        Reader reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }

}
