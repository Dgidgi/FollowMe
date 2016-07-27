
var mqtt    = require('mqtt');
var client  = mqtt.connect('mqtt://test.mosquitto.org');

client.on('connect', function () {

  client.subscribe('dgidgi/followme/trackrecorder/appid_3/loggedUsers');



  var msg = JSON.stringify( {userLogin:{userName: "OscarLeBaroudeur",userKindOf:"FOLLOWER", applicationId:"appid_14"}} ) ;
  client.publish('dgidgi/followme/trackrecorder/appid_14', msg );


});


client.on('message', function (topic, message) {
  // message is Buffer
  console.log(message.toString());
  client.end();
});
