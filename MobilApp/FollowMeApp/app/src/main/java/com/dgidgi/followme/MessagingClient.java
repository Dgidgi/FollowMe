package com.dgidgi.followme;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Created by gpr on 12/05/2016.
 */
public class MessagingClient {

    private static final String LOGTAG ="MessagingClient" ;
    private static final String MQTT_SERVER_URL = "tcp://test.mosquitto.org" ;
    private static final String MQTT_CLIENT_ID = "FollowMeClient" ;
    private static final String MQTT_MESSAGE_TOPIC = "dgidgi.followme.trackrecorder" ;

    private static boolean mbAlreadySending ;

   static public void initMessagingClient( Context context , final IMessagingClientListerner listener ) {
        MemoryPersistence memPer = new MemoryPersistence();
        final MqttAndroidClient mqttClient = new MqttAndroidClient(context, MQTT_SERVER_URL, MQTT_CLIENT_ID, memPer);

        try {
            mqttClient.connect(null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken mqttToken) {
                    Log.i(LOGTAG, "Client connected");
                    listener.onMessagingClientConnected(mqttClient);
                }

                @Override
                public void onFailure(IMqttToken arg0, Throwable arg1) {
                    // TODO Auto-generated method stub
                    Log.i(LOGTAG, "Client connection failed: " + arg1.getMessage());
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

   static public void sendMessage(MqttAndroidClient client, String strMessage ) {

       if ( client == null || !client.isConnected()) {
           Log.i(LOGTAG, "Client not connected please call initMessagingClient before sending a message");
           return ;
       }

       Log.i(LOGTAG, "Trying send message :["+strMessage+"]");
       MqttMessage message = new MqttMessage(strMessage.getBytes());
       message.setQos(2);
       message.setRetained(false);

       try {
           client.publish(MQTT_MESSAGE_TOPIC, message);
           Log.i(LOGTAG, "Message published");
       } catch (MqttPersistenceException e) {
           // TODO Auto-generated catch block
           e.printStackTrace();
       } catch (MqttException e) {
           // TODO Auto-generated catch block
           e.printStackTrace();
       }
    }

    static void releaseClient( MqttAndroidClient client) {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }
}
