// trackrecorder.js
var debug = require('debug')('trackrecorder')
var mqtt    = require('mqtt');
var mqttClient  = mqtt.connect('mqtt://test.mosquitto.org');
var mongoClient = require('mongodb').MongoClient ;

var mongoDbUrl = 'mongodb://localhost:27017/followme'
var topicBaseName = 'dgidgi/followme/trackrecorder'


var lastCoordinate = "";

// Emission du status courant
function requestClientStatus( sourceTopic) {
    mqttClient.publish(sourceTopic +"/status",lastCoordinate ) ;
}

function manageEndTrack( sourceTopic) {
     
}


// Connexion à la base
mongoClient.connect( mongoDbUrl, function(err, mongodb) {
    if(err) throw err;
    debug('mongodb connection to'+ mongoDbUrl +' : ok') ;
    debug('mongodb') ;

    // Connection Mqtt et subsciption
    mqttClient.on('connect', function () {
      mqttClient.subscribe(topicBaseName);
      mqttClient.subscribe(topicBaseName+'/+');
    });

    // Reception d'un message
    mqttClient.on('message', function (topic, message) {

        debug('msg received') ;
        debug('topic:') ;
        debug(topic) ;
        debug('msg:') ;
        debug(message.toString()) ;

        if ( message.toString().indexOf("endtrack") != -1) {

            // Publication su status courant demandé

            manageEndTrack( topic) ;

            return ;

        } else if ( message.toString().indexOf("status") != -1) {

            // Publication su status courant demandé

            requestClientStatus( topic) ;

            return ;

        } else {

            // Stockage en base de la donnée
            // TODO Tester la confirmité du message

            var doc = JSON.parse(message.toString());

            // Temporaire
            lastCoordinate = message.toString()  ;

            mongodb.collection('tracking').insert(doc, function (err, data) {
                    if(err) throw err;

                    debug('message added in database :') ;
                    debug(data) ;
            } );
        }
    });
});
