// trackrecorder.js
var debug = require('debug')('trackrecorder')
var mqtt    = require('mqtt');
var geolib  = require('geolib');
var mqttClient  = mqtt.connect('mqtt://test.mosquitto.org');
var mongoClient = require('mongodb').MongoClient ;

var mongoDbUrl = 'mongodb://localhost:27017/followme'
var topicBaseName = 'dgidgi/followme/trackrecorder'

var mongoDbConnection = null ;
var lastCoordinate = "";

// Retourne le dernier Sample associé à appId
///////////////////////////////////////////////////////////////////////////////
function findPreviousLocationSample( appId) {
 return mongoDbConnection.collection('tracking').find({applicationId:appId}).sort({'loc.time':-1}).limit(1) ;
}

// Demande de status
// Retourne le dernier sample associé à l'appId
///////////////////////////////////////////////////////////////////////////////
function requestClientStatus( sourceTopic) {

    if ( mongoDbConnection == null)
        return ;

    // Recherche du dernier sample acquis sur le même track
    var appId  =  extractApplicationIDFromTopic( sourceTopic) ;

    var cursor = findPreviousLocationSample(appId) ;

    cursor.nextObject(function(err, item) {
        debug('found item :['+item+']') ;

        if ( item != null) {
            mqttClient.publish(sourceTopic +"/status",JSON.stringify(item) ) ;
        }
    } );
}

// Appelé lorsqu'une capture est interompue
///////////////////////////////////////////////////////////////////////////////
function manageEndTrack( sourceTopic) {
    var appId  =  extractApplicationIDFromTopic( sourceTopic) ;
    if (appId==null || appId=="")
        return ;

    debug('manageEndTrack for applicationId : ['+appId+']') ;

    if ( mongoDbConnection == null )
        return ;

    mongoDbConnection.collection('tracking').remove( {applicationId:appId}, function (err, result) {
            if(err) throw err;
            debug('track cleaned') ;
            debug(result) ;
    } );
}

// Extrait l'UUID de l'application à partir d'un topic
///////////////////////////////////////////////////////////////////////////////
function extractApplicationIDFromTopic( sourceTopic) {

    if (sourceTopic == null )
        return "";

    var splited = sourceTopic.split('/');
    if (splited == null || splited.length <2 )
        return "" ;

    // TODO Ajouter une contrôle du contenu !
    return splited[splited.length -1] ;
}

//
// Ajout d'un nouveau sample sur la track
///////////////////////////////////////////////////////////////////////////////
function manageAddTrackSample( lastSample ) {

    debug('manageAddTrackSample ['+JSON.stringify(lastSample)+']') ;

    // TODO Tester la confirmité du message
    if ( mongoDbConnection == null)
        return ;

    // Recherche du dernier sample acquis sur le même track
    var cursor = findPreviousLocationSample(lastSample.applicationId) ;


    lastSample.distance = 0.0 ;
    cursor.nextObject(function(err, prevSample) {
        debug('found item :['+JSON.stringify(prevSample)+']') ;

        if ( prevSample != null) {
            var p1 = {"longitude":prevSample.loc.location.longitude, "latitude":prevSample.loc.location.latitude };
            var p2 = {"longitude":lastSample.loc.location.longitude, "latitude":lastSample.loc.location.latitude };
            var delta = geolib.getDistance(p1, p2, 1,2 ) ;

            debug(' compute distance beetween ['+JSON.stringify(p1)+'] and ['+JSON.stringify(p2)+'] :'+delta) ;

            lastSample.distance = prevSample.distance + delta;

            debug(' current distance '+prevSample.distance+' -> '+lastSample.distance) ;
	    }

        debug('object to save :['+JSON.stringify(lastSample)+']') ;

        // Stockage en base de la donnée
        mongoDbConnection.collection('tracking').insert(lastSample, function (err, data) {
            if(err) throw err;
            debug('sample added in db :') ;
            debug(JSON.stringify(data)) ;
        } );
    } );
}

//
// Lancement du server
// - Connexion à la base mongodb
// - Connexion Mqtt
// - Ecoute des topic sur Mqtt
///////////////////////////////////////////////////////////////////////////////
mongoClient.connect( mongoDbUrl, function(err, mongodb) {
    if(err) throw err;
    debug('mongodb connection to'+ mongoDbUrl +' : ok') ;

    mongoDbConnection = mongodb ;

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
            // Gestion en fin de tracking
            manageEndTrack( topic) ;
            return ;

        } else if ( message.toString().indexOf("status") != -1) {
            // Publication du status courant demandé
            requestClientStatus( topic) ;
            return ;

        } else {
            // Ajout d'un location sample dans un track
            manageAddTrackSample(JSON.parse(message.toString()) ) ;

        }
    });
});