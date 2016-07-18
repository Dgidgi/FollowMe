var mqtt    = require('mqtt');
var client  = mqtt.connect('mqtt://test.mosquitto.org');

client.on('connect', function () {

  client.subscribe('dgidgi/followme/trackrecorder/appid_1/loggedUsers');
  client.subscribe('dgidgi/followme/trackrecorder/appid_2/loggedUsers');
  client.subscribe('dgidgi/followme/trackrecorder/appid_3/loggedUsers');


  var msg = JSON.stringify( {userLogin:{userName: "RUNNER 1",userKindOf:"RUNNER", applicationId:"appid_1"}} ) ;
  client.publish('dgidgi/followme/trackrecorder/appid_1', msg );

  msg = JSON.stringify( {userLogin:{userName: "RUNNER 2",userKindOf:"RUNNER", applicationId:"appid_2"}} ) ;
  client.publish('dgidgi/followme/trackrecorder/appid_2', msg );

  msg = JSON.stringify( {userLogin:{userName: "FOLLOWER 1",userKindOf:"FOLLOWER", applicationId:"appid_3"}} ) ;
  client.publish('dgidgi/followme/trackrecorder/appid_3', msg );

  msg = JSON.stringify( {userLogin:{userName: "FOLLOWER 2",userKindOf:"FOLLOWER", applicationId:"appid_4"}} ) ;
  client.publish('dgidgi/followme/trackrecorder/appid_4', msg );

  msg = JSON.stringify( {updateUserPosition:{applicationId: "appid_1",loc:{ time:1468405889531, location:{latitude:43.55,longitude:1.49, altitude:0} }}} ) ;
 client.publish('dgidgi/followme/trackrecorder/appid_1',msg );

 msg = JSON.stringify( {updateUserPosition:{applicationId: "appid_2",loc:{ time:1468405889531, location:{latitude:43.54,longitude:1.49, altitude:0} }}} ) ;
 client.publish('dgidgi/followme/trackrecorder/appid_2',msg );

 msg = JSON.stringify( {updateUserPosition:{applicationId: "appid_3",loc:{ time:1468405889531, location:{latitude:43.56,longitude:1.49, altitude:0} }}} ) ;
 client.publish('dgidgi/followme/trackrecorder/appid_3',msg );

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
