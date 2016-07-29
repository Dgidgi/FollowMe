package com.dgidgi.followme;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class FollowMeMainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, FollowMeTrackingFragment.OnFragmentInteractionListener, LogginFragment.OnFragmentInteractionListener {

    private static final String LOGTAG = "FMMainActivity";
    private static final int SEND_PERIOD = 1000;

    GoogleApiClient mGoogleApiClient;

    FollowMeTrackingFragment mFollowMeTrackingFragment ;
    LogginFragment mLogginFragment ;

    private Timer timer = null;

    LocationRequest mLocationRequest;

    MqttAndroidClient mMessagingClient = null ;
    Location mLastLocation = null ;

    private  static Map<String, LoggedUser> mMapLoggedUsers = new HashMap<String, LoggedUser>() ;


    private static TextToSpeech mTextToSpeechService = null ;


    public static void speak( String text) {
        mTextToSpeechService.speak(text, TextToSpeech.QUEUE_FLUSH,  null);
    }
    //
    // Retourne la liste des utilisateurs connectés
    ////////////////////////////////////////////////////////////////////////////////////////////////
     public static  Map<String, LoggedUser> getLoggedUsersMap() {
        return mMapLoggedUsers;
    }

    // Retourne l'utilisateur connecté courrant
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static LoggedUser getCurrentUser() {
        return mMapLoggedUsers.get(MessagingClient.mApplicationUUID) ;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_follow_me_main);

        if (ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION }, 200);
        }

        // Récupération des Fragments d'IHM
        mLogginFragment =  (LogginFragment ) getSupportFragmentManager().findFragmentById(R.id.loggin_fragment) ;

        mFollowMeTrackingFragment =  (FollowMeTrackingFragment ) getSupportFragmentManager().findFragmentById(R.id.tracking_fragment) ;

        // Masquage du fragment associé au tracking
        getSupportFragmentManager().beginTransaction().hide(mFollowMeTrackingFragment).commit();

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mTextToSpeechService=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                mTextToSpeechService.setLanguage(Locale.FRANCE) ;

            }
        }
        );
    }

    //
    // Lancement de l'application
    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected void onStart() {

        Log.i(LOGTAG, " onStart !");

        mGoogleApiClient.connect();

        MessagingClient.initMessagingClient(this, new IMessagingClientListerner() {
            @Override
            public void onMessagingClientConnected(MqttAndroidClient client) {
                mMessagingClient = client ;
                mFollowMeTrackingFragment.enableTracking(true);
                Log.i(LOGTAG, " onMessagingClientConnected !");
            }
            @Override
            public void onMessagingClientConnectionLost(MqttAndroidClient client) {
                mFollowMeTrackingFragment.enableTracking(false);
                Log.i(LOGTAG, " onMessagingClientConnectionLost !");
            }

            public void onMessagingStatusReceived(final String status) {

                Log.i(LOGTAG, " onMessagingStatusReceived !");
                updateTrackingStatus(status);
            }

            //
            // Reception de la liste des utilisateurs connectés
            /////////////////////////////////////////////////////////////////////////////////////////////
            public void onMessagingLoggedUsersReceived( final String sLoggedUsers ) {

                Log.i(LOGTAG, " onMessagingLoggedUsersReceived !");

                try {
                    JSONArray loggedUsers = new JSONArray(sLoggedUsers) ;

                    Map<String,LoggedUser > loggedUsersMap = new HashMap<String, LoggedUser>() ;
                    for (int i = 0 ; i < loggedUsers.length() ; i++) {
                        LoggedUser usr = new LoggedUser(loggedUsers.getJSONObject(i)) ;
                        loggedUsersMap.put(usr.getApplicationId(),usr);
                    }

                    // Ajout des nouveaux + annonce si nécessaire
                    for(LoggedUser lu:loggedUsersMap.values()) {
                        if ( !mMapLoggedUsers.containsKey(lu.getApplicationId())) {
                            mMapLoggedUsers.put(lu.getApplicationId(), lu);

                            if (    (FollowMeMainActivity.getCurrentUser() != null)&&
                                    (FollowMeMainActivity.getCurrentUser().getUserKindOf() == LoggedUser.KindOf.RUNNER) &&
                                    ( mFollowMeTrackingFragment.showUser(lu))) {

                                FollowMeMainActivity.speak("Le spectateur "+lu.getUserName()+" vient de se connecter");

                            }
                        }
                    }

                    List<String> toRemove = new ArrayList<String>() ;
                    // Suppression de ceux qui ne sont plus loggés
                    for(LoggedUser lu:mMapLoggedUsers.values()) {
                        if ( !loggedUsersMap.containsKey(lu.getApplicationId() )) {
                            toRemove.add(lu.getApplicationId());
                        }
                    }

                    for( String sAppId:toRemove ) {
                        mMapLoggedUsers.remove(sAppId) ;
                    }

                    mFollowMeTrackingFragment.loggedUsersListChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            //
            // Reception d'un message d'AR de la connection au serveur
            ////////////////////////////////////////////////////////////////////////////////////////////
            public void onMessagingLogginUserAcknowledge() {
                Log.i(LOGTAG, " onMessagingLogginUserAcknowledge !");
                mFollowMeTrackingFragment.loggedUsersListChanged();

                // Masquage du fragment de login et affichage du fragment de suivi
                 getSupportFragmentManager().beginTransaction().hide(mLogginFragment).show(mFollowMeTrackingFragment).commit();
            }

            //
            // Reception d'un message de mise à jour de la position d'un utilisateur
            //////////////////////////////////////////////////////////////////////////////////////////////
            public void onMessagingUpdatedUserPosition(final String updatedUserPosition) {
                Log.i(LOGTAG, " onMessagingUpdatedUserPosition !");
                try {

                    UserPosition userPosition = new UserPosition(new JSONObject(updatedUserPosition )) ;
                    LoggedUser loggedUser = mMapLoggedUsers.get(userPosition.getApplicationId());

                    if (loggedUser != null ) {

                        mFollowMeTrackingFragment.updateUserLocation(  loggedUser, userPosition ) ;
                    } else {
                        Log.e(LOGTAG, " onMessagingUpdatedUserPosition : no associated logged user found !");
                    }

                } catch (JSONException e) {

                }
            }

            @Override
            public void onMessagingUserMessageReceived(String userMessage) {

                Log.i(LOGTAG, " onMessagingUserMessageReceived !");
                try {
                    JSONObject jsoUserMessage = new JSONObject(userMessage ) ;

                    LoggedUser fromUser = mMapLoggedUsers.get( jsoUserMessage.getString("fromUserApplicationId" ) );

                    String sFromName = "inconnu";
                    if ( fromUser != null ) {
                        sFromName = fromUser.getUserName();
                    }


                    FollowMeMainActivity.speak( "Nouveau message de "+sFromName);
                    FollowMeMainActivity.speak( jsoUserMessage.getString("message" ) );


                } catch (JSONException e) {

                }
            }
        });

        super.onStart();
    }



    public void updateTrackingStatus( final String status ){
        runOnUiThread(new Runnable() {
            //region Description
            @Override
            //endregion
            public void run() {
               // mFollowMeTrackingFragment.updateTrackingStatus(status);
            }
        });
    }

    protected void onResume() {
        mGoogleApiClient.connect();
        super.onResume();
    }

    protected void onStop() {

        super.onStop();
    }

    protected void onDestroy() {

        MessagingClient.sendMessage(mMessagingClient, "endtrack",MessagingClient.mApplicationUUID );

        LoggedUser usr = getCurrentUser();

        if ( usr != null )
            MessagingClient.sendUserLogoutMessage(mMessagingClient,usr.getUserName(),usr.getUserKindOf());

        mGoogleApiClient.disconnect();
        MessagingClient.releaseClient(mMessagingClient);
        super.onDestroy();
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

    //
    // Demarrage du tracking
    ///////////////////////////////////////////////////////////////////////////////////////////////
    public void doStartTracking() {
        Log.i(LOGTAG, "Start sending ...");

        timer = new Timer("FollowMeCoordinateEmissionThread" );
        timer.schedule(new TimerTask() {
               @Override
               public void run() {

                   // Envoi de la location courante si besoin
                   if ( mLastLocation != null ) {

                            MessagingClient.sendUpdatePositionMessage(mMessagingClient, mLastLocation);

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
        MessagingClient.sendMessage(mMessagingClient, "endtrack",MessagingClient.mApplicationUUID );

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (com.google.android.gms.location.LocationListener) this);

        if ( timer != null ) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        Log.i(LOGTAG, "Location Changed : "+location.toString());
        mLastLocation = location ;

        // Mise à jour position dans l'application
        mFollowMeTrackingFragment.currentUserLocationChanged(mLastLocation ) ;

        LoggedUser me = FollowMeMainActivity.getCurrentUser() ;
        // Si runner transmission de ma position courante
        if ( me != null  && (me.getUserKindOf() == LoggedUser.KindOf.RUNNER))
            MessagingClient.sendUpdatePositionMessage(mMessagingClient,mLastLocation);
    }

    @Override
    public void onFragmentStartTracking() {
        this.doStartTracking();
    }

    @Override
    public void onFragmentStopTracking() {
        this.doEndTracking();

    }

    public void onLogginRunner(String userName) {

        MessagingClient.sendUserLoginMessage(mMessagingClient, userName,LoggedUser.KindOf.RUNNER);
    }

    public void onLogginFollower(String userName) {


        MessagingClient.sendUserLoginMessage(mMessagingClient, userName,LoggedUser.KindOf.FOLLOWER);
    }


    @Override
    public void sendMessageToRunner( LoggedUser followingUser, String sMessage)
    {
        MessagingClient.sendMessageToUser(mMessagingClient, followingUser.getApplicationId() ,sMessage);
    }


}
