package com.dgidgi.followme;

import org.eclipse.paho.android.service.MqttAndroidClient;

/**
 * Created by gpr on 15/05/2016.
 */
public interface IMessagingClientListerner {
    void onMessagingClientConnected(MqttAndroidClient client) ;
    void onMessagingStatusReceived(String coordinate) ;
    void onMessagingClientConnectionLost(MqttAndroidClient client) ;
    void onMessagingLoggedUsersReceived( final String loggedUsers );
    void onMessagingLogginUserAcknowledge();
    void onMessagingUpdatedUserPosition(final String updatedUserPosition);
    void onMessagingUserMessageReceived(final String userMessage);

    }
