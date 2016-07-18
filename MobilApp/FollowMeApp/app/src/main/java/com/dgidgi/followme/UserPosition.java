package com.dgidgi.followme;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by gpr on 16/07/2016.
 */
public class UserPosition {

    JSONObject mJsnUserPosition ;

    public UserPosition( JSONObject jsnUserPosition) {
        mJsnUserPosition = jsnUserPosition ;
    }

    public String getApplicationId() {
        try {
            return mJsnUserPosition.getString("applicationId");

        } catch( JSONException ex) {
            ex.printStackTrace();
        }
        return "unknown";
    }

    public double getLatitude() {
        try {
            JSONObject jsnLoc = mJsnUserPosition.getJSONObject("loc");
            JSONObject jsnLocation = jsnLoc.getJSONObject("location");

            return jsnLocation.getDouble("latitude");

        }    catch( JSONException ex) {
            ex.printStackTrace();
        }
        return 0.0;
    }

    public double getLongitude() {
        try {
            JSONObject jsnLoc = mJsnUserPosition.getJSONObject("loc");
            JSONObject jsnLocation = jsnLoc.getJSONObject("location");
            return jsnLocation.getDouble("longitude");

        }    catch( JSONException ex) {
            ex.printStackTrace();
        }
        return 0.0;
    }

}
