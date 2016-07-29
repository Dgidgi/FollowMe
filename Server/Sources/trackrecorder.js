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
function findLastSample( appId) {
    return mongoDbConnection.collection('tracking').find({'applicationId':appId}).sort({"loc.time":-1}).limit(1) ;
}

// Retourne le dernier Sample associé à appId avec une delta avec time
// inferieur à delai
///////////////////////////////////////////////////////////////////////////////
function findTimedLastSample( appId, time,delai) {

    var minTime = time - delai ;
    var maxTime = time + delai ;

    return mongoDbConnection.collection('tracking').find({'applicationId':appId, 'loc.time':{$lte:maxTime, $gte:minTime}}).sort({'loc.time':-1}).limit(1) ;
}

// Demande de status
// Retourne les derniers samples associés à l'appId et des autres tracks
///////////////////////////////////////////////////////////////////////////////
function requestClientStatus( sourceTopic) {

    if ( mongoDbConnection == null)
        return ;

    // Recherche du dernier sample acquis sur le même track
    var appId  =  extractApplicationIDFromTopic( sourceTopic) ;

    debug('request Cient Status for ['+appId+']') ;

    findLastSample(appId).nextObject( function(err, lastSample) {

        if ( lastSample != null) {

            debug('send  lastSample for ['+lastSample.applicationId  +']' ) ;

            mqttClient.publish(sourceTopic +"/status",JSON.stringify(lastSample) ) ;

            var cOtherTracks = mongoDbConnection.collection('tracks').find({applicationId:{$ne:appId}}) ;

            cOtherTracks.each(function(errTrack, otherTrack) {
                if ( otherTrack ) {
                    debug('found other track ('+otherTrack.applicationId+')') ;

                    findTimedLastSample(otherTrack.applicationId, lastSample.loc.time, 5000).nextObject(function(err, trackSample) {

                        if ( trackSample ) {
                            debug('send status ['+trackSample.applicationId  +'] time('+trackSample.loc.time+')' ) ;
                            mqttClient.publish(sourceTopic +"/status",JSON.stringify(trackSample) ) ;
                        }
                    });
                }
            });
        } else {
            debug('...no lastSample found ['+appId  +'] !!' ) ;
        }
    });
}

//
// Emission du sample reçu à tous les clients connectés
////////////////////////////////////////////////////////////////////////////////
function broadcastLastTrackedSample( lastSample) {

    debug('Broadcast sample to all tracs' ) ;

    var allTracks = mongoDbConnection.collection('tracks').find() ;
    allTracks.each(function(errTrack, track) {
        if (track != null) {
            debug('send to ['+track.applicationId + ']' ) ;
            mqttClient.publish(topicBaseName+"/"+track.applicationId +"/status",JSON.stringify(lastSample) ) ;
        }
    });
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
            debug('tracking collection cleaned') ;
            debug(result) ;
    } );

    mongoDbConnection.collection('tracks').remove( {applicationId:appId}, function (err, result) {
            if(err) throw err;
            debug('track collection removed') ;
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

// Appeler au login d'un utilisateur
////////////////////////////////////////////////////////////////////////////////
function manageUserLogin(appId, user) {

    debug('login user: ['+user.userName+','+user.userKindOf+'] on ['+appId+']') ;

    // Stockage en base de la donnée
    mongoDbConnection.collection('loggedUsers').insert({user_id:user.userName+"_"+user.applicationId,user:user} , function (err, data) {
        if(err) throw err;
        debug('user added in db :') ;

    //    debug(JSON.stringify(data)) ;

        // Envoi d'un accusé reception au loggé
        sendUserLogginAcknowledge(appId);

        // Envoi de la liste des loggés à tout le monde
        broadcastLoggedUsers() ;

    } );
}

// Appeler au unlog d'un utilisateur
////////////////////////////////////////////////////////////////////////////////
function manageUserLogout(appId, user) {

    debug('logout user: ['+user.userName+','+user.userKindOf+'] on ['+appId+']') ;

    mongoDbConnection.collection('loggedUsers').remove( {user_id:user.userName+"_"+user.applicationId}, function (err, result) {
        if(err) throw err;
        debug('user  cleaned') ;
    //    debug(result) ;

        broadcastLoggedUsers() ;
    } );
}

// Reception de la position d'un des utilisateurs
///////////////////////////////////////////////////////////////////////////////
function manageUpdateUserPosition( appId, updateUserPosition) {
    broadcastUpdatedUserPosition( updateUserPosition ) ;
}

//
// Emission de la dernière position reçue à tous les clients connectés
////////////////////////////////////////////////////////////////////////////////
function broadcastUpdatedUserPosition(updateUserPosition) {

    var allLoggedUsers = mongoDbConnection.collection('loggedUsers').find({},{_id:false,user_id:false}) ;

    allLoggedUsers.each(function(err, user) {
        if (user != null) {
            mqttClient.publish(topicBaseName+"/"+user.user.applicationId +"/updatedUserPosition",JSON.stringify(updateUserPosition) ) ;
        }
    });
}
//
// Emission de la liste des user connectés à tous les clients connectés
////////////////////////////////////////////////////////////////////////////////
function broadcastLoggedUsers() {

    debug('Broadcast loggedUsers' ) ;

    var allLoggedUsers = mongoDbConnection.collection('loggedUsers').find({},{_id:false,user_id:false}) ;

    allLoggedUsers.map().toArray( function(err,  res) {

        var loggedUsers = new Array();
        var targetsApplicationIds = new Array();

        res.forEach( function(user) {
            loggedUsers.push(user.user) ;
            targetsApplicationIds.push( user.user.applicationId);
        } );
        debug('targetsApplicationIds : '+ JSON.stringify(targetsApplicationIds) ) ;
        debug('loggedUsers : '+ JSON.stringify(loggedUsers) ) ;

        targetsApplicationIds.forEach( function(appId) {
            mqttClient.publish(topicBaseName+"/"+ appId +"/loggedUsers",JSON.stringify(loggedUsers) ) ;
        });
    }) ;
}

//
///////////////////////////////////////////////////////////////////////////////
function sendUserLogginAcknowledge(appId ) {
    mqttClient.publish(topicBaseName+"/"+ appId +"/userLoginAcknowledge" , "ok" ) ;
}


//
//
//////////////////////////////////////////////////////////////////////////////
function sendUserMessage(message) {
    debug('sendUserMessage : to '+message.targetUserApplicationId+"message" + JSON.stringify(message) ) ;
    mqttClient.publish(topicBaseName+"/"+ message.targetUserApplicationId +"/userMessage" , JSON.stringify(message) ) ;
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
function addTrackSample( newSample, applicationId ) {

    // Recherche du précedent sample acquis sur le même track
    newSample.distance = 0.0 ;

    debug('update runned distance') ;

    findLastSample(applicationId).nextObject(function(err, prevSample) {

        // Calcul de la distance parcourue
        debug('found previous sample :['+JSON.stringify(prevSample)+']') ;

        if ( prevSample != null) {

            var p1 = {"longitude":prevSample.loc.location.longitude, "latitude":prevSample.loc.location.latitude };
            var p2 = {"longitude":newSample.loc.location.longitude, "latitude":newSample.loc.location.latitude };
            var delta = geolib.getDistance(p1, p2, 1,2 ) ;

            debug(' compute distance beetween ['+JSON.stringify(p1)+'] and ['+JSON.stringify(p2)+'] :'+delta) ;

            newSample.distance = prevSample.distance + delta;

            debug(' current distance '+prevSample.distance+' -> '+newSample.distance) ;
	    }

        debug('object to save :['+JSON.stringify(newSample)+']') ;

        // Stockage en base de la donnée
        mongoDbConnection.collection('tracking').insert(newSample, function (err, data) {
            if(err) throw err;
            debug('sample added in db :') ;
            debug(JSON.stringify(data)) ;

            // Emission du dernier sample aux clients
            broadcastLastTrackedSample(newSample) ;
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

    addTrackSample( lastSample, lastSample.applicationId ) ;


    simu = JSON.parse( JSON.stringify(lastSample)) ;

    simu.loc.location.longitude += 0.00 ;
    simu.loc.location.latitude += 0.0001;
    simu.applicationId =   "TRACK-SIMU-1" ;

//    addTrackSample( simu, simu.applicationId ) ;

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
        debug('msg:[') ;
        debug(message.toString()) ;


        if ( message.toString().indexOf("userLogin") != -1) {
            // Loggin d'un user
            debug("login message") ;

            var loginMessage = JSON.parse(message.toString());

            manageUserLogin(  extractApplicationIDFromTopic( topic), loginMessage.userLogin ) ;
            return ;

        } else if ( message.toString().indexOf("userLogout") != -1) {
            // UnLoggin d'un user
            var loginMessage = JSON.parse(message);

            manageUserLogout(  extractApplicationIDFromTopic( topic), loginMessage.userLogout ) ;
            return ;

        } else if ( message.toString().indexOf("endtrack") != -1) {
            // Gestion en fin de tracking
            manageEndTracking(  extractApplicationIDFromTopic( topic) ) ;
            //manageEndTracking(  "TRACK-SIMU-1" ) ;

            return ;

        } else if ( message.toString().indexOf("starttrack") != -1) {
            // Gestion du début tracking
            manageStartTracking(  extractApplicationIDFromTopic( topic) ) ;
        //    manageStartTracking(  "TRACK-SIMU-1" ) ;
            return ;

        } else if ( message.toString().indexOf("status") != -1) {
            // Publication du status courant demandé
            requestClientStatus( topic) ;
            return ;

        }  else if ( message.toString().indexOf("updateUserPosition") != -1) {
            // Ajout d'un location sample dans un track
            var updateUserPositionMsg = JSON.parse(message);
            manageUpdateUserPosition(extractApplicationIDFromTopic( topic) , updateUserPositionMsg.updateUserPosition ) ;

        } else if ( message.toString().indexOf("sendMessage") != -1) {
            // Ajout d'un location sample dans un track
            var sendMessageMsg = JSON.parse(message);
            sendUserMessage(  sendMessageMsg.sendMessage ) ;

        }
    });
});
