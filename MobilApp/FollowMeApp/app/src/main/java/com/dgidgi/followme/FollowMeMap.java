package com.dgidgi.followme;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by gpr on 02/06/2016.
 */
public class FollowMeMap {

    GoogleMap mGoogleMap;

    Map<String, Marker> mLoggedUsersMarkers = new HashMap<String, Marker>();

    public FollowMeMap( GoogleMap googleMap ) {
        mGoogleMap = googleMap ;
    }

    // Mise à jour de la localisation de l'utilisateur
    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void updateUserLocationMarker( String appId, String userName, LoggedUser.KindOf kd, double dLat, double dLon,boolean isMe, boolean bCenterCamera ) {

        Marker marker = mLoggedUsersMarkers.get(appId) ;

        final LatLng l = new LatLng(dLat,dLon);

        if ( marker  == null) {
            BitmapDescriptor bd = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE) ;
            if (isMe)
                bd = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED) ;

            String sTitle = userName ;

            if( isMe )
                sTitle = sTitle+" (vous)";

            marker = mGoogleMap.addMarker(new MarkerOptions().position(l).icon(bd).title(sTitle));
            mLoggedUsersMarkers.put(appId, marker);
        } else
            marker.setPosition(l);


        if (bCenterCamera)
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(l,  19.0f)  );
    }

    public void centerTo(String appId) {
        Marker marker = mLoggedUsersMarkers.get(appId) ;
        if ( marker != null ) {
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(),19.0f));
            marker.showInfoWindow();
        }
    }
}
