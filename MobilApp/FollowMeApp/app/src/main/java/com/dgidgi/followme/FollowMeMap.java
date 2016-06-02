package com.dgidgi.followme;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by gpr on 02/06/2016.
 */
public class FollowMeMap {
    GoogleMap mGoogleMap;

    public FollowMeMap( GoogleMap googleMap ) {
        mGoogleMap = googleMap ;
    }

    //
    // Mise à jour du marque dans la map
    //////////////////////////////////////////////////////////////////////////////////////////////////////
    public Marker updateMarker( Marker m, double dLat, double dLong, boolean centerCamera, boolean isMe) {

        final LatLng l = new LatLng(dLat,dLong);

        if ( m == null ) {

            BitmapDescriptor bd = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE) ;
            if (isMe)
                bd = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED) ;
            m = mGoogleMap.addMarker(new MarkerOptions().position(l).icon(bd));

        } else
            m.setPosition(l);

        if (centerCamera)
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(l, (float) 19.0)  );

        return m ;
    }
}
