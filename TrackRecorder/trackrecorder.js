// trackrecorder.js
var debug = require('debug')('trackrecorder')
var mqtt    = require('mqtt');
var mqttClient  = mqtt.connect('mqtt://test.mosquitto.org');
var mongoClient = require('mongodb').MongoClient ;

var mongoDbUrl = 'mongodb://localhost:27017/followme'

// Connexion à la base
mongoClient.connect( mongoDbUrl, function(err, mongodb) {
    if(err) throw err;
    debug('mongodb connection to'+ mongoDbUrl +' : ok') ;
    debug('mongodb') ;

    // Connection Mqtt et subsciption
    mqttClient.on('connect', function () {
      mqttClient.subscribe('dgidgi.followme.trackrecorder');
    });

    // Reception d'un message
    mqttClient.on('message', function (topic, message) {
        debug('msg received') ;
        debug('topic:') ;
        debug(topic) ;
        debug('msg:') ;
        debug(message.toString()) ;

        var doc= {  message: message.toString() };

        mongodb.collection('testmessages').insert(doc, function (err, data) {
                if(err) throw err;

                debug('message added in database :') ;
                debug(data) ;
        });
    });

});
