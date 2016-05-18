# FollowMe
## Database
Database name : followme
Collections : 
tracking : mémorisation des samples
## MQTT Messaging
(M) -> (S) - Topic : **dgidgi/followme/trackrecorder** - Message : json location sample   
(M) -> (S) - Topic : **dgidgi/followme/trackrecorder/appUID**  - Message : "status" : demande de status   
(M) -> (S) - Topic : **dgidgi/followme/trackrecorder/appUID**  - Message : "endtrack" : fin de tracking  
(S) -> (M) - Topic : **dgidgi/followme/trackrecorder/appUID/status**  - Message : json status   
## JSON Formats
### json location sample
<p><code>
{userId: dgidgi,  trackId:track_1,applicationId: UUUUUUUUIIIIDDDD,loc: {time:time_milli_second,location: { latitude:0.0,longitude:0.0,altitude:0.0 }}}
</p></code>
### json status
<p><code>
{userId: dgidgi,  trackId:track_1,applicationId: UUUUUUUUIIIIDDDD,loc: {time:time_milli_second,location: { latitude:0.0,longitude:0.0,altitude:0.0 }}, distance=0.0}
</p></code>
