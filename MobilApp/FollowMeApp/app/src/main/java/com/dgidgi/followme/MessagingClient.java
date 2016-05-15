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








   static boolean sendMessage( Context context, String topic, final String message) {

       if (mbAlreadySending ) {
           Log.i(LOGTAG, "Client already sending, message resumed.");
           return false ;
       }

       mbAlreadySending = true ;

              MemoryPersistence memPer = new MemoryPersistence();
       final MqttAndroidClient client = new MqttAndroidClient(context, MQTT_SERVER_URL, MQTT_CLIENT_ID, memPer);



       final String msg = message ;

       Log.i(LOGTAG, "Try sending message:["+message+"]");

       try {

           client.connect(null, new IMqttActionListener() {

               @Override
               public void onSuccess(IMqttToken mqttToken) {
                   Log.i(LOGTAG, "Client connected");

                   MqttMessage message = new MqttMessage(msg.getBytes());
                   message.setQos(2);
                   message.setRetained(false);

                   try {
                       client.publish(MQTT_MESSAGE_TOPIC, message);

                       Log.i(LOGTAG, "Message published");

                       client.disconnect();

                       Log.i(LOGTAG, "Client disconnected");

                   } catch (MqttPersistenceException e) {
                       // TODO Auto-generated catch block
                       e.printStackTrace();
                   } catch (MqttException e) {
                       // TODO Auto-generated catch block
                       e.printStackTrace();
                   }
               }

               @Override
               public void onFailure(IMqttToken arg0, Throwable arg1) {
                   // TODO Auto-generated method stub
                   Log.i(LOGTAG, "Client connection failed: " + arg1.getMessage());
                   mbAlreadySending = false;
               }
           });
           mbAlreadySending = false;
           return true ;

       } catch (MqttException e) {
           e.printStackTrace();
           mbAlreadySending = false;
           return false ;
       }
        /*
        try { MqttClient client = new MqttClient("'tcp://test.mosquitto.org", "androidClient", new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
            client.getTopic("dgidgi.followme.trackrecorder").publish("Coucou From Android".getBytes(), 0, false);
        } catch (MqttException e)
        {
            e.printStackTrace();
        }
        */
   }
}
