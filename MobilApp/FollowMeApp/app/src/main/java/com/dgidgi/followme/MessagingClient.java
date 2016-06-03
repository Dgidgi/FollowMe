package com.dgidgi.followme;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.UUID;

/**
 * Created by gpr on 12/05/2016.
 */
public class MessagingClient {

    private static final String LOGTAG ="MessagingClient" ;
    private static final String MQTT_SERVER_URL = "tcp://test.mosquitto.org" ;
    private static final String MQTT_CLIENT_ID = "FollowMeClient" ;
    private static final String MQTT_MESSAGE_TOPIC = "dgidgi/followme/trackrecorder" ;
    public static final String mApplicationUUID = UUID.randomUUID().toString() ;

    private static IMessagingClientListerner mListener ;

   static public void initMessagingClient( Context context , final IMessagingClientListerner listener ) {
       mListener = listener ;

        MemoryPersistence memPer = new MemoryPersistence();
        final MqttAndroidClient mqttClient = new MqttAndroidClient(context, MQTT_SERVER_URL, mApplicationUUID, memPer);

        try {
            mqttClient.connect(null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken mqttToken) {

                    Log.i(LOGTAG, "Client connected");

                    mqttClient.setCallback(new MqttCallback() {
                        @Override
                        public void connectionLost(Throwable cause) {
                            Log.i(LOGTAG, "Client message connexion lost");
                            mListener.onMessagingClientConnectionLost(mqttClient);
                        }

                        @Override
                        public void messageArrived(String topic, MqttMessage message) throws Exception {
                            Log.i(LOGTAG, "Client message arrived on topic ["+topic +"]: " + message);
                            if ( topic.contains(MQTT_MESSAGE_TOPIC + "/"+mApplicationUUID+"/"+"status") ) {
                                listener.onMessagingStatusReceived(message.toString() ) ;
                            }
                        }
                        @Override
                        public void deliveryComplete(IMqttDeliveryToken token) {

                        }
                    });

                    // On s'abonne aux message de reception de coordonnées provenant du serveur

                    try {

                        mqttClient.subscribe(MQTT_MESSAGE_TOPIC + "/"+mApplicationUUID+"/status", 0 );

                    } catch (MqttException e) {
                        e.printStackTrace();
                    }

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
        sendMessage( client,  strMessage, "" ) ;
    }

    static public void sendMessage(MqttAndroidClient client, String strMessage, String extensionTopic ) {

        String topic = MQTT_MESSAGE_TOPIC+"/"+extensionTopic ;
       if ( client == null || !client.isConnected()) {

           Log.i(LOGTAG, "Client not connected please call initMessagingClient before sending a message");
           return ;
       }

       Log.i(LOGTAG, "Trying send message :["+strMessage+"] on topic ["+topic + "]");
       MqttMessage message = new MqttMessage(strMessage.getBytes());
       message.setQos(2);
       message.setRetained(false);

       try {
           client.publish(topic, message);
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
