var mqtt    = require('mqtt');
var client  = mqtt.connect('mqtt://test.mosquitto.org');

client.on('connect', function () {

  client.subscribe('dgidgi/followme/trackrecorder/appid_2/loggedUsers');



  var msg = JSON.stringify( {userLogin:{userName: "RUNNER 2",userKindOf:"RUNNER", applicationId:"appid_2"}} ) ;
  client.publish('dgidgi/followme/trackrecorder/appid_2', msg );

  msg = JSON.stringify( {updateUserPosition:{applicationId: "appid_2",loc:{ time:1468405889531, location:{latitude:43.55,longitude:1.47, altitude:0} }}} ) ;
  client.publish('dgidgi/followme/trackrecorder/appid_2',msg );



/*

  msg = JSON.stringify( {userLogout:{userName: "test 1",userKindOf:"RUNNER", applicationId:"appid_1"}} ) ;
  client.publish('dgidgi/followme/trackrecorder/appid_1', msg );

  msg = JSON.stringify( {userLogout:{userName: "test 2",userKindOf:"RUNNER", applicationId:"appid_2"}} ) ;
  client.publish('dgidgi/followme/trackrecorder/appid_2', msg );

  msg = JSON.stringify( {userLogout:{userName: "test 3",userKindOf:"RUNNER", applicationId:"appid_3"}} ) ;
  client.publish('dgidgi/followme/trackrecorder/appid_3', msg );
*/
});


client.on('message', function (topic, message) {
  // message is Buffer
  console.log(message.toString());
  client.end();
});
