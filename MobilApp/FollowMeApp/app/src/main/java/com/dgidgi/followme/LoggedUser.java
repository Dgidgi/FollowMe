package com.dgidgi.followme;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by gpr on 13/07/2016.
 */
public class LoggedUser {
    JSONObject mJsnUser ;

    public enum KindOf {RUNNER, FOLLOWER,UNKNOWN} ;

    public LoggedUser(JSONObject jsnUser) {
        mJsnUser = jsnUser ;
    }

    public String getUserName() {

        try {
            return mJsnUser.getString("userName");

        } catch( JSONException ex) {
            ex.printStackTrace();
        }
        return "unknown";
    }

    public KindOf getUserKindOf() {

        try {
            String ko =  mJsnUser.getString("userKindOf");
            if (ko.contains("RUNNER")) return KindOf.RUNNER ;
            if (ko.contains("FOLLOWER")) return KindOf.FOLLOWER ;
        } catch( JSONException ex) {
            ex.printStackTrace();
        }
        return KindOf.UNKNOWN;
    }

    public String getApplicationId() {
        try {
            return mJsnUser.getString("applicationId");

        } catch( JSONException ex) {
            ex.printStackTrace();
        }
        return "unknown";
    }


    public boolean itsMe()
    {
        return this.getApplicationId().contains(MessagingClient.mApplicationUUID) ;
    }



}
