package com.dgidgi.followme;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;

import com.google.android.gms.location.LocationListener ;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class FollowMeMainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String LOGTAG = "FollowMeMainActivity";
    private static final int SEND_PERIOD = 1000;
    GoogleApiClient mGoogleApiClient;

    private Timer timer = null;

    LocationRequest mLocationRequest;

    MqttAndroidClient mClient = null ;
    Location mLastLocation = null ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_me_main);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        if (ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION }, 200);
        }
    }

    protected void onStart() {
        mGoogleApiClient.connect();

        super.onStart();

        MessagingClient.initMessagingClient(this, new IMessagingClientListerner() {
            @Override
            public void onMessagingClientConnected(MqttAndroidClient client) {
                mClient = client ;
                ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
                toggleButton.setEnabled(true);
            }

            public void onMessagingStatusReceived(String status) {

                TextView textView = (TextView) findViewById(R.id.statusText) ;
                textView.setText(status);

                try {
                    // {userId:dgidgi, trackId:track#1, loc:{time:1463514800971,location:{latitude:43.5496149,longitude:1.4859633,altitude:0.0}}}
                    JSONObject jsonStatus = new JSONObject(status);
                    JSONObject jsonLoc = jsonStatus.getJSONObject("loc");
                    JSONObject jsonLocation = jsonLoc.getJSONObject("location");

                    TextView textViewLon = (TextView) findViewById(R.id.lonCoordinateText) ;
                    TextView textViewLat = (TextView) findViewById(R.id.latCoordinateText) ;

                    textViewLon.setText(jsonLocation.getString("longitude"));
                    textViewLat.setText(jsonLocation.getString("latitude"));


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        });
    }

    protected void onResume() {
        mGoogleApiClient.connect();
        super.onResume();
    }


    protected void onStop() {

        MessagingClient.sendMessage(mClient, "endtrack",MessagingClient.mApplicationUUID );

        mGoogleApiClient.disconnect();
        MessagingClient.releaseClient(mClient);
        super.onStop();
    }

    public String getLastLocationMessage() {
        try {
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);

            if (mLastLocation != null) {
                return formatLocationMessage(mLastLocation);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return "";
    }

    public String formatLocation(Location loc) {

        if (loc != null) {
            String msg = "{";
            msg += "time:" + System.currentTimeMillis() + ",";
            msg += "location:{";
            msg += "latitude:" + String.valueOf(loc.getLatitude()) + ",";
            msg += "longitude:" + String.valueOf(loc.getLongitude()) + ",";
            msg += "altitude:" + String.valueOf(loc.getAltitude());
            msg += "}";
            msg += "}";

            return msg;
        }

        return  "";
    }

    public String formatLocationMessage(  Location loc ) {

        if (loc == null )
            return "" ;

        String msg = "{userId:\"dgidgi\", trackId:\"track_1\", applicationId:\""+MessagingClient.mApplicationUUID+ "\",loc:" ;
        msg += formatLocation(loc) ;
        msg += "}";

        return msg;
    }

    public void toggleSendCoordinates(View view) {
        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);

        final Context ctxt = this.getBaseContext();

        if (toggleButton.isChecked()) {

            Log.i(LOGTAG, "Start sending ...");

            if (ActivityCompat.checkSelfPermission( this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission( this.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                Log.e(LOGTAG, "checkSelf ACCESS_FINE_LOCATION permission faild");
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient,
                    mLocationRequest,
                    (com.google.android.gms.location.LocationListener) this);

            timer = new Timer("FollowMeCoordinateEmissionThread" );
            timer.schedule(new TimerTask() {
                @Override
                public void run() {

                    if ( mLastLocation != null ) {

                        String locMsg = formatLocationMessage(mLastLocation);



                        Log.i(LOGTAG, " returned loc(" + locMsg + ")");

                        if (locMsg != null && !locMsg.isEmpty()) {
                            JSONObject jsonMsg = null;
                            try {
                                jsonMsg = new JSONObject(locMsg);
                                MessagingClient.sendMessage(mClient, jsonMsg.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            MessagingClient.sendMessage(mClient, "status",MessagingClient.mApplicationUUID );
                        }

                        mLastLocation = null ;
                    }
                }
            },
            0, SEND_PERIOD);

        } else {

            Log.i(LOGTAG, "Stop sending ...");

            // On préviens le serveur que l'on a stoppé
            MessagingClient.sendMessage(mClient, "endtrack",MessagingClient.mApplicationUUID );

            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (com.google.android.gms.location.LocationListener) this);

            if ( timer != null ) {
                timer.cancel();
                timer = null;
            }
        }

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Log.i(LOGTAG, " GoogleApi Connected !");

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOGTAG, " GoogleApi Connection Suspended !");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(LOGTAG, " GoogleApi Connection Failed !");
        Log.i(LOGTAG,         connectionResult.toString() );
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(LOGTAG, "Location Changed : "+location.toString());
        mLastLocation = location ;
    }

}
