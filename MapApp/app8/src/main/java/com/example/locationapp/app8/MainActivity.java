package com.example.locationapp.app8;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class MainActivity extends FragmentActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {


    LocationRequest mLocationRequest;

    LocationClient mLocationClient;

    private TextView mLatLng;
    private TextView mAddress;
    private ProgressBar mActivityIndicator;
    private TextView mConnectionState;
    private TextView mConnectionStatus;

    SharedPreferences mPrefs;

    SharedPreferences.Editor mEditor;

    boolean mUpdatesRequested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLatLng = (TextView) findViewById(R.id.lat_lng);
        mAddress = (TextView) findViewById(R.id.address);
        mActivityIndicator = (ProgressBar) findViewById(R.id.address_progress);
        mConnectionState = (TextView) findViewById(R.id.text_connection_state);
        mConnectionStatus = (TextView) findViewById(R.id.text_connection_status);

        mLocationRequest = LocationRequest.create();

        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        mUpdatesRequested = false;

        mPrefs = getSharedPreferences(LocationUtils.SHARED_PREFERENCES, Context.MODE_PRIVATE);

        mEditor = mPrefs.edit();

        mLocationClient = new LocationClient(this, this, this);



    }

    @Override
    public void onStop(){
        if(mLocationClient.isConnected()){
            stopPeriodicUpdates();
        }

        mLocationClient.disconnect();

        super.onStop();
    }

    @Override
    public void onStart(){
        super.onStart();

        mLocationClient.connect();
    }

    @Override
    public void onResume(){
        super.onResume();
        if(mPrefs.contains(LocationUtils.KEY_UPDATES_REQUESTED)){
            mUpdatesRequested = mPrefs.getBoolean(LocationUtils.KEY_UPDATES_REQUESTED, false);
        }
        else{
            mEditor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED, false);
            mEditor.commit();
        }
    }

    public void goMap(View view){
        Log.d("Message", "Map");
        Intent intent = new Intent(this, MapActivity.class);
        Location currentLocation = mLocationClient.getLastLocation();
        intent.putExtra("Long", currentLocation.getLongitude());
        intent.putExtra("Lat", currentLocation.getLatitude());
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        // Decide what to do based on the original request code
        switch (requestCode) {
            case LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST:
                switch (resultCode) {
                    case Activity.RESULT_OK :
                        Log.d(LocationUtils.APPTAG, getString(R.string.resolved));

                        mConnectionState.setText(R.string.connected);
                        mConnectionStatus.setText(R.string.resolved);
                        break;
                    default:
                        Log.d(LocationUtils.APPTAG, getString(R.string.no_resolution));

                        mConnectionState.setText(R.string.disconnected);
                        mConnectionStatus.setText(R.string.no_resolution);
                        break;
                }
                default:
                    Log.d(LocationUtils.APPTAG, getString(R.string.unknown_activity_request_code, requestCode));
                    break;
        }
    }

    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates",
                    "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason.
            // resultCode holds the error code.
        } else {
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    resultCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment

                errorFragment.show(getSupportFragmentManager(),"Location Updates");
            }
            return false;
        }
    }


    /**************************/
    public void getLocation(View v) {
        Log.d("Un log", "Entre");
        // If Google Play Services is available
        if (servicesConnected()) {
            Log.d("Un log", "Entre");
            // Get the current location
            Location currentLocation = mLocationClient.getLastLocation();

            // Display the current location in the UI
            mLatLng.setText(LocationUtils.getLatLng(this, currentLocation));
        }
    }

    /**
     * Invoked by the "Get Address" button.
     * Get the address of the current location, using reverse geocoding. This only works if
     * a geocoding service is available.
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    // For Eclipse with ADT, suppress warnings about Geocoder.isPresent()
    @SuppressLint("NewApi")
    public void getAddress(View v) {

        // In Gingerbread and later, use Geocoder.isPresent() to see if a geocoder is available.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD && !Geocoder.isPresent()) {
            // No geocoder is present. Issue an error message
            Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
            return;
        }

        if (servicesConnected()) {

            // Get the current location
            Location currentLocation = mLocationClient.getLastLocation();

            // Turn the indefinite activity indicator on
            mActivityIndicator.setVisibility(View.VISIBLE);

            // Start the background task
            (new MainActivity.GetAddressTask(this)).execute(currentLocation);
        }
    }

    /**
     * Invoked by the "Start Updates" button
     * Sends a request to start location updates
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    public void startUpdates(View v) {
        mUpdatesRequested = true;

        if (servicesConnected()) {
            startPeriodicUpdates();
        }
    }

    /**
     * Invoked by the "Stop Updates" button
     * Sends a request to remove location updates
     * request them.
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    public void stopUpdates(View v) {
        mUpdatesRequested = false;

        if (servicesConnected()) {
            stopPeriodicUpdates();
        }
    }

    @Override
    public void onConnected(Bundle dataBundle){
        mConnectionStatus.setText(R.string.connected);

        if (mUpdatesRequested) {
            startPeriodicUpdates();
        }
    }

    @Override
    public void onDisconnected(){
        mConnectionStatus.setText(R.string.disconnected);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult){
        if(connectionResult.hasResolution()){
            try{
                connectionResult.startResolutionForResult(
                        this,
                        LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);
            }
            catch(IntentSender.SendIntentException e){
                e.printStackTrace();
            }
        }
        else{
            //showErrorDialog(connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location){
        mConnectionStatus.setText(R.string.location_updated);

        // In the UI, set the latitude and longitude to the value received
        mLatLng.setText(LocationUtils.getLatLng(this, location));
    }

    private void startPeriodicUpdates() {
        Log.d("Message","Alog");
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
        mConnectionState.setText(R.string.location_requested);
    }

    /**
     * In response to a request to stop updates, send a request to
     * Location Services
     */
    private void stopPeriodicUpdates() {
        mLocationClient.removeLocationUpdates(this);
        mConnectionState.setText(R.string.location_updates_stopped);
    }

    /**
     * An AsyncTask that calls getFromLocation() in the background.
     * The class uses the following generic types:
     * Location - A {@link android.location.Location} object containing the current location,
     *            passed as the input parameter to doInBackground()
     * Void     - indicates that progress units are not used by this subclass
     * String   - An address passed to onPostExecute()
     */
    protected class GetAddressTask extends AsyncTask<Location, Void, String> {

        // Store the context passed to the AsyncTask when the system instantiates it.
        Context localContext;

        // Constructor called by the system to instantiate the task
        public GetAddressTask(Context context) {

            // Required by the semantics of AsyncTask
            super();

            // Set a Context for the background task
            localContext = context;
        }

        /**
         * Get a geocoding service instance, pass latitude and longitude to it, format the returned
         * address, and return the address to the UI thread.
         */
        @Override
        protected String doInBackground(Location... params) {
            /*
             * Get a new geocoding service instance, set for localized addresses. This example uses
             * android.location.Geocoder, but other geocoders that conform to address standards
             * can also be used.
             */
            Geocoder geocoder = new Geocoder(localContext, Locale.getDefault());

            // Get the current location from the input parameter list
            Location location = params[0];

            // Create a list to contain the result address
            List<Address> addresses = null;

            // Try to get an address for the current location. Catch IO or network problems.
            try {

                /*
                 * Call the synchronous getFromLocation() method with the latitude and
                 * longitude of the current location. Return at most 1 address.
                 */
                Log.d("Message" , "Aqui");
                addresses = geocoder.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1
                );

                // Catch network or other I/O problems.
            } catch (IOException exception1) {

                // Log an error and return an error message
                Log.e(LocationUtils.APPTAG, getString(R.string.IO_Exception_getFromLocation));

                // print the stack trace
                exception1.printStackTrace();

                // Return an error message
                return (getString(R.string.IO_Exception_getFromLocation));

                // Catch incorrect latitude or longitude values
            } catch (IllegalArgumentException exception2) {

                // Construct a message containing the invalid arguments
                String errorString = getString(
                        R.string.illegal_argument_exception,
                        location.getLatitude(),
                        location.getLongitude()
                );
                // Log the error and print the stack trace
                Log.e(LocationUtils.APPTAG, errorString);
                exception2.printStackTrace();

                //
                return errorString;
            }
            // If the reverse geocode returned an address
            if (addresses != null && addresses.size() > 0) {

                // Get the first address
                Address address = addresses.get(0);

                // Format the first line of address
                String addressText = getString(R.string.address_output_string,

                        // If there's a street address, add it
                        address.getMaxAddressLineIndex() > 0 ?
                                address.getAddressLine(0) : "",

                        // Locality is usually a city
                        address.getLocality(),

                        // The country of the address
                        address.getCountryName()
                );

                // Return the text
                return addressText;

                // If there aren't any addresses, post a message
            } else {
                return getString(R.string.no_address_found);
            }
        }

        /**
         * A method that's called once doInBackground() completes. Set the text of the
         * UI element that displays the address. This method runs on the UI thread.
         */
        @Override
        protected void onPostExecute(String address) {

            // Turn off the progress bar
            mActivityIndicator.setVisibility(View.GONE);

            // Set the address in the UI
            mAddress.setText(address);
        }
    }

    /**
     * Show a dialog returned by Google Play services for the
     * connection error code
     *
     * @param errorCode An error code returned from onConnectionFailed
     */
    private void showErrorDialog(int errorCode) {

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                errorCode,
                this,
                LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            // Create a new DialogFragment in which to show the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();

            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);

            // Show the error dialog in the DialogFragment
            errorFragment.show(getSupportFragmentManager(), LocationUtils.APPTAG);
        }
    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;

        /**
         * Default constructor. Sets the dialog field to null
         */
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        /**
         * Set the dialog to display
         *
         * @param dialog An error dialog
         */
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        /*
         * This method must return a Dialog to the DialogFragment.
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
    /*
     * Handle results returned to the FragmentActivity
     * by Google Play services
     */



}
