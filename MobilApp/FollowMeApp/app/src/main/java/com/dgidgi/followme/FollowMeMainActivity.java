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
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class FollowMeMainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener,OnMapReadyCallback {

    private static final String LOGTAG = "FollowMeMainActivity";
    private static final int SEND_PERIOD = 1000;

    GoogleApiClient mGoogleApiClient;
    FollowMeMap     mFollowMeMap ;

    //
    // Map des TrackedUsers (y compris nous même)
    ////////////////////////////////////////////////////////////////////////////////////////////////
    Map<String, TrackedUserStatus> mTrackedUsersStatus = new HashMap<String, TrackedUserStatus>();

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

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    //
    // Lancement de l'application
    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected void onStart() {
        mGoogleApiClient.connect();

        super.onStart();

        MessagingClient.initMessagingClient(this, new IMessagingClientListerner() {
            @Override
            public void onMessagingClientConnected(MqttAndroidClient client) {
                mClient = client ;
                ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
                toggleButton.setEnabled(true);

                // Boucle de rafraichissement des status
                Timer timer = new Timer("Refresh Status" );
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        // Demande de rafraichissement du status distance parcourue, autres pistes ...
                        MessagingClient.sendMessage(mClient, "status",MessagingClient.mApplicationUUID );
                    }
                }, 0, SEND_PERIOD);
            }

            public void onMessagingStatusReceived(String status) {
                updateCurrentStatus( status ) ;
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

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Log.i(LOGTAG, " GoogleApi Connected !");

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);

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
    public void onMapReady(GoogleMap googleMap) {
        mFollowMeMap = new FollowMeMap(googleMap)  ;
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

    //
    // Demarrage du tracking
    ///////////////////////////////////////////////////////////////////////////////////////////////
    public void doStartTracking() {
        Log.i(LOGTAG, "Start sending ...");

        MessagingClient.sendMessage(mClient, "starttrack",MessagingClient.mApplicationUUID );

        timer = new Timer("FollowMeCoordinateEmissionThread" );
        timer.schedule(new TimerTask() {
               @Override
               public void run() {

                   // Envoi de la location courante si besoin
                   if ( mLastLocation != null ) {

                       String locMsg = formatLocationMessage(mLastLocation);

                       Log.i(LOGTAG, " lastLocation (" + locMsg + ")");

                       if (locMsg != null && !locMsg.isEmpty()) {
                           JSONObject jsonMsg = null;
                           try {

                               jsonMsg = new JSONObject(locMsg);

                               MessagingClient.sendMessage(mClient, jsonMsg.toString());

                           } catch (JSONException e) {
                               e.printStackTrace();
                           }
                       }
                       mLastLocation = null ;
                   }
               }
           }, 0, SEND_PERIOD);
    }

    //
    // Fin du Tracking
    ///////////////////////////////////////////////////////////////////////////////////////////////
    public void doEndTracking() {
        Log.i(LOGTAG, "Stop sending ...");

        // On prévient le serveur que l'on a stoppé
        MessagingClient.sendMessage(mClient, "endtrack",MessagingClient.mApplicationUUID );

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (com.google.android.gms.location.LocationListener) this);

        if ( timer != null ) {
            timer.cancel();
            timer = null;
        }
    }

    //
    // Mise à jour du status
    ///////////////////////////////////////////////////////////////////////////////////////////////
    public void updateCurrentStatus( String status  ) {

        TextView textView = (TextView) findViewById(R.id.statusText) ;
        textView.setText(status);

        try {

            final TrackedUserStatus tus = new TrackedUserStatus(status,  null, false) ;

            Log.i(LOGTAG, "Receive status for "+  tus.getApplicationId() );

            this.runOnUiThread(new Runnable() {
                //region Description
                @Override
                //endregion
                public void run() {

                    Log.i(LOGTAG, "Update status "+ tus.getApplicationId() );

                    // si delta de plus de 5 secondes -> cassos
                    /*
                    if (Math.abs( tus.getTime() - mMyUserStatus.getTime()) > 5000 ) {
                        Log.i(LOGTAG, "... to late "+ jsonStatus.getString("applicationId"));
                        return ;
                    }
                    */

                    TrackedUserStatus theTrackedStatus = getOrCreateTrackedUserStatus( tus.getApplicationId()) ;
                    Boolean bCenterCamera = tus.isMyStatus()&& isAutoCenterCamera() ;
                    theTrackedStatus.updateFrom(tus,bCenterCamera);

                    updateFromReceivedStatus( theTrackedStatus ) ;
                }
            }) ;

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //
    // Création ou Récuperation du tracked status pour l'applicationId
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private TrackedUserStatus getOrCreateTrackedUserStatus( String applicationId  ) {
        if (mTrackedUsersStatus.containsKey(applicationId)) {
            return mTrackedUsersStatus.get(applicationId) ;
        }
        TrackedUserStatus tus = new TrackedUserStatus(applicationId,mFollowMeMap ) ;
        mTrackedUsersStatus.put(applicationId, tus);

        return tus ;
    }

    //
    // Création ou Récuperation du tracked status de l'application courante
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private TrackedUserStatus getOrCreateMyUserStatus() {
        return getOrCreateTrackedUserStatus(MessagingClient.mApplicationUUID);
    }

    //
    // Mise à jour du status de l'utilisateur courant
    ///////////////////////////////////////////////////////////////////////////////////////////////
   private void updateFromReceivedStatus( TrackedUserStatus status ){
       if (status.isMyStatus()) {
           TextView textViewDistance = (TextView) findViewById(R.id.distanceText);
           textViewDistance.setText("" + status.getDistance());
       }
   }

    //
    //Toggle AutoCenter
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void toggleAutoCenter(View view) {
        if (isAutoCenterCamera() ) {
            TrackedUserStatus tus = this.getOrCreateMyUserStatus();
            tus.updateFrom(tus,isAutoCenterCamera());
        }
    }

    //
    // Toggle Tracking
    ///////////////////////////////////////////////////////////////////////////////////////////////
    public void toggleSendCoordinates(View view) {
        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);

        final Context ctxt = this.getBaseContext();

        if (toggleButton.isChecked()) {

            doStartTracking();

        } else {

            doEndTracking() ;
        }
    }



    @Override
    public void onLocationChanged(Location location) {
        Log.i(LOGTAG, "Location Changed : "+location.toString());
        mLastLocation = location ;

        TextView textViewLon = (TextView) findViewById(R.id.lonCoordinateText) ;
        TextView textViewLat = (TextView) findViewById(R.id.latCoordinateText) ;

        textViewLon.setText(""+mLastLocation.getLongitude());
        textViewLat.setText(""+mLastLocation.getLatitude());

        TrackedUserStatus tus =  getOrCreateMyUserStatus();
        tus.updateFrom(mLastLocation.getLongitude(), mLastLocation.getLatitude(), isAutoCenterCamera());
    }

    //
    // Retourne true si on est en mode autocenter
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private boolean isAutoCenterCamera(){
        CheckBox cbAUtoCenter = (CheckBox) findViewById(R.id.cbAutoCenter);
        return cbAUtoCenter.isChecked() ;
    }


}
