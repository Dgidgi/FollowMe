# FollowMe
## MQTT Messaging

(M) -> (S) : dgidgi/followme/trackrecorder + sample : json   
(M) -> (S) : dgidgi/followme/trackrecorder/appUID + "status" : demande de status   
(M) -> (S) : dgidgi/followme/trackrecorder/appUID + "endtrack" : fin de tracking  
(S) -> (M) : dgidgi/followme/trackrecorder/appUID/status + status: retour du status   
