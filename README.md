# FollowMe
## Database
Database name : followme
Collections : 
tracking : mémorisation des samples
## MQTT Messaging
(M) -> (S) - Topic : **dgidgi/followme/trackrecorder** - Message : json sample   
(M) -> (S) - Topic : **dgidgi/followme/trackrecorder/appUID**  - Message : "status" : demande de status   
(M) -> (S) - Topic : **dgidgi/followme/trackrecorder/appUID**  - Message : "endtrack" : fin de tracking  
(S) -> (M) - Topic : **dgidgi/followme/trackrecorder/appUID/status**  - Message : json status   
