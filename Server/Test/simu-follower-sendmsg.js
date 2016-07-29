
var mqtt    = require('mqtt');
var client  = mqtt.connect('mqtt://test.mosquitto.org');
var msgBase = 'dgidgi/followme/trackrecorder/';

var appId    = "appid_14" ;
var userName = "Oscar" ;
var userKind = "FOLLOWER" ;


function connectUser(appId,userName,userKind ) {

    console.log("Connect user :"+appId+","+userName+","+userKind );
    client.subscribe(msgBase+appId);
    var msg = JSON.stringify( {userLogin:{userName: userName,userKindOf:userKind, applicationId:appId}} ) ;
    client.publish(msgBase+appId, msg );
}

function sendMessage(fromAppId, targetAppId, message) {
    var msg = JSON.stringify( {sendMessage:{fromUserApplicationId: fromAppId,targetUserApplicationId:targetAppId, message:message}} ) ;
    client.publish(msgBase+appId, msg );
}

client.on('connect', function () {

    connectUser(appId, userName,userKind) ;

    sendMessage( appId, "8890ed88-9c08-4ce5-8f28-7ecfeb18bcb3","coucou mec") ;

});


client.on('message', function (topic, message) {
  console.log("Message received :");
  console.log(message.toString());

});
