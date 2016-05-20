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

// Retourne le dernier sample appId avec une distance non null
///////////////////////////////////////////////////////////////////////////////
function findLastLocationSampleWithDistance( appId) {
 return mongoDbConnection.collection('tracking').find({applicationId:appId,distance:{$exists:true}}).sort({'loc.time':-1}).limit(1) ;
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
function manageEndTracking( appId) {

    if (appId==null || appId=="")
        return ;

    debug('manageEndTracking for applicationId : ['+appId+']') ;

    if ( mongoDbConnection == null )
        return ;

    mongoDbConnection.collection('tracking').remove( {applicationId:appId}, function (err, result) {
            if(err) throw err;
            debug('tracking cleaned') ;
            debug(result) ;
    } );

    mongoDbConnection.collection('tracks').remove( {applicationId:appId}, function (err, result) {
            if(err) throw err;
            debug('track removed') ;
            debug(result) ;
    } );

}
// Appelé lorsqu'une capture est interompue
///////////////////////////////////////////////////////////////////////////////
function manageStartTracking( appId) {

    if (appId==null || appId=="")
        return ;

    debug('manageStartTracking for applicationId : ['+appId+']') ;

    if ( mongoDbConnection == null )
        return ;

    var track={applicationId:appId} ;
    // Stockage en base de la donnée
    mongoDbConnection.collection('tracks').insert(track, function (err, data) {
        if(err) throw err;
        debug('track added in db :') ;
        debug(JSON.stringify(data)) ;
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
// Mise à jour du sample status
///////////////////////////////////////////////////////////////////////////////
function updateTrackSampleStatus( lastSample, applicationId ) {

    // Recherche du dernier sample acquis sur le même track
    var cursor = findPreviousLocationSample(applicationId) ;

    lastSample.distance = 0.0 ;
    cursor.nextObject(function(err, prevSample) {

        // Calcul de la distance parcourue
        debug('found item :['+JSON.stringify(prevSample)+']') ;

        if ( prevSample != null) {
            var p1 = {"longitude":prevSample.loc.location.longitude, "latitude":prevSample.loc.location.latitude };
            var p2 = {"longitude":lastSample.loc.location.longitude, "latitude":lastSample.loc.location.latitude };
            var delta = geolib.getDistance(p1, p2, 1,2 ) ;

            debug(' compute distance beetween ['+JSON.stringify(p1)+'] and ['+JSON.stringify(p2)+'] :'+delta) ;

            lastSample.distance = prevSample.distance + delta;

            debug(' current distance '+prevSample.distance+' -> '+lastSample.distance) ;
	    }

        // Calcul des delta distance

        // Recherche des autres track en cours
        var others = mongoDbConnection.collection('tracks').find({applicationId:{$ne:applicationId}}) ;

        lastSample.delta = [] ;

        others.nextObject(function(err, other) {
            debug('found other :['+JSON.stringify(other)+']') ;
            if (other != null) {
                var lastLocationSample = findLastLocationSampleWithDistance(other.applicationId) ;
                lastLocationSample.nextObject(function(err, other) {

                    lastSample.delta.push( {applicationId:other.applicationId, distance:  lastSample['distance'] - other.distance  });

                    debug('lastSample withi status :['+JSON.stringify(lastSample)+']') ;
                });
            }
        });

        lastSample.delta.push( {applicationId:'test-application-id-1', distance:  lastSample['distance']   });
        lastSample.delta.push( {applicationId:'test-application-id-2', distance:  lastSample['distance'] -10    });


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
// Ajout d'un nouveau sample sur la track
///////////////////////////////////////////////////////////////////////////////
function manageAddTrackSample( lastSample ) {

    debug('manageAddTrackSample ['+JSON.stringify(lastSample)+']') ;

    // TODO Tester la confirmité du message
    if ( mongoDbConnection == null)
        return ;

    updateTrackSampleStatus( lastSample, lastSample.applicationId ) ;
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
            manageEndTracking(  extractApplicationIDFromTopic( topic) ) ;
            return ;

        } else if ( message.toString().indexOf("starttrack") != -1) {
            // Gestion du début tracking
            manageStartTracking(  extractApplicationIDFromTopic( topic) ) ;
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
