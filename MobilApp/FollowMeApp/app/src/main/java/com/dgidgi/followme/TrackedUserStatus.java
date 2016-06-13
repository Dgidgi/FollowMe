package com.dgidgi.followme;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by gpr on 29/05/2016.
 */
public class TrackedUserStatus {

    private static final String LOGTAG = "TrackedUserStatus";
    LatLng  mLocation = new LatLng(0.0,0.0) ;
    double  mDistance = 0.0 ;
    String mApplicationId = "" ;
    Marker mUserMarker = null ;
    int     mTime = 0 ;
    FollowMeMap mFollowMeMap = null ;

    public TrackedUserStatus(String  applicationId,FollowMeMap followMeMap ) {
        mApplicationId = applicationId ;
        mFollowMeMap = followMeMap ;
    }

    public TrackedUserStatus(String jsonStatus, FollowMeMap followMeMap, Boolean bCenterCamera) throws JSONException {
        mFollowMeMap = followMeMap ;
        updateFrom(  new JSONObject(jsonStatus),bCenterCamera);
    }

    public TrackedUserStatus(JSONObject jsonStatus, FollowMeMap followMeMap, Boolean bCenterCamera) {
        mFollowMeMap = followMeMap ;
        updateFrom(jsonStatus,bCenterCamera);
    }

    public TrackedUserStatus(TrackedUserStatus tus, FollowMeMap followMeMap, Boolean bCenterCamera) {
        mFollowMeMap = followMeMap ;
        updateFrom(tus,bCenterCamera);
    }

    // Mise à jour à partir d'un  JSON
    public void updateFrom(TrackedUserStatus tus, boolean bCenterCamera ) {

        mLocation = new LatLng(tus.getLocation().latitude,tus.getLocation().longitude) ;
        mDistance = tus.getDistance() ;
        mApplicationId = tus.getApplicationId() ;
        mTime = tus.getTime() ;

        updateMapMarker(bCenterCamera);
    }

    // Mise à jour à partir d'un  JSON
    public void updateFrom(JSONObject jsonStatus, Boolean bCenterCamera ) {

        // Mise à jour applicationId
        try {
            mApplicationId =  jsonStatus.getString("applicationId") ;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Distance parcourue
        try {
            mDistance =  jsonStatus.getDouble("distance") ;
        } catch (JSONException e) {
            e.printStackTrace();
            mDistance = 0.0 ;
        }
        // Localisation
        try {
            JSONObject jsoLoc       = jsonStatus.getJSONObject("loc") ;
            JSONObject jsoLocation  = jsoLoc.getJSONObject("location") ;
            mTime = jsoLoc.getInt("time");
            mLocation = new LatLng(jsoLocation.getDouble("latitude") , jsoLocation.getDouble("longitude"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        updateMapMarker(bCenterCamera);
    }

    // Mise à jour à partir d'un  JSON
    public void updateFrom(double dLong, double dLat , Boolean bCenterCamera ) {
        mLocation = new LatLng(dLat, dLong);
        updateMapMarker(bCenterCamera);
    }

    private void updateMapMarker(boolean bCenterCamera){
        if ( mFollowMeMap != null ) {

            Log.i(LOGTAG, "Update map marker for "+ this.mApplicationId );

            mUserMarker = mFollowMeMap.updateMarker(mUserMarker, this.getLocation().latitude, this.getLocation().longitude, bCenterCamera, isMyStatus() ) ;
        }
    }


    //
    // Retourne true s'il s'agit du status de l'application courante
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public boolean isMyStatus() {
        return mApplicationId.compareTo(MessagingClient.mApplicationUUID) == 0 ;
    }

   public String getApplicationId(){
      return mApplicationId ;
    }

    public LatLng getLocation() {
       return mLocation ;
    }

    public Double getDistance() {
        return mDistance ;
    }

    public int getTime() {
        return mTime ;
    }
}
