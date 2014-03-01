'use strict';

/* Layout-komponentti/direktiivistö formien toimintonappeja, ilmoitustekstejä ja muita yleisiä asioita varten.
 * 
 * 1. Direktiivit kommunikoivat scopessa määritellyn olion välityksellä joka controllerin on alustettava tyhjäksi
 *    olioksi:
 * 
 * 	 $scope.formControls = {};
 * 
 *   - Tämän olion sisältö ei ole osa direktiivistön julkista apia eli siihen ei pidä missään tapauksessa viitata
 *     suoraan.
 * 
 * 2. Toiminnot ja ilmoitukset määritellään controls-model-tagilla (tämä ei vielä lisää mitään sisältöä sivulle vaan
 *    kokoaa tiedot modeliin):
 * 
 *   <controls-model model="formControls" tt-create="..." tt-edit="..." title="..." dto="...">
 *   
 *   	<controls-button primary="true|false" tt="..." action="f()", disabled="f()"/>
 *   	...
 *   	<controls-message type="message|success|error|error-detail" type="" tt="..." tp="..." show="f()"/>
 *   	...
 *   
 *   </controls-model>
 *   
 *   - Parametrien tarkempi dokumentaatio on alempana, direktiivimääritysten yhteydessä
 *   - Otsikkona näytetään tt-create- (kun luodaan uutta) tai tt-edit (kun muokataan olemassaolevaa) -atribuuteillä
 *     määritelty kielistysavain jolle annetaan parametriksi title -atribuutilla annettu yksi- tai monikielinenteksti
 *     (jolloin näytetään valitun kielen mukainen sisältö), tai pelkkä title:n mukainen teksti jos em. avaimia ei ole
 *     määritelty
 *   - Se, luodaanko uutta vai muokataanko, päätellään dto-parametrin mukaan (olemassaolevasta oletetaan löytyvän created-
 *     ja createdBy -arvot); em. oliosta tarkastetaan myös arvo 'tila', joka näytetään muiden metatietojen kanssa
 *   
 *   Ilmoitustyypit:
 *   
 *   message		Normaali ilmoitusviestityyli, näytetään vain headerissa. Esim. "Olet muokkaamassa..."
 *   success		Ilmoitus onnistumisesta; tätä ei näytetä, jos yksikin error- tai error-detail -viesti on näkyvissä.
 *   error			Päätason virheviesti, esim. "Tallennus epäonnistui"
 *   error-detail   Virheviestin tarkennus, validointia ja muuta varten, esim. "Kenttä X on pakollinen"
 *   
 * 3. Määritelly napit ja ilmoitukset näytetään sivulla display-controls -tagilla:
 * 
 *   <display-controls model="formControls" display="header|footer"/>
 *  
 *   - Display-parametri määrittää kumpi variaatio näytetään; headerin ja footerin ero on käytännössä se, että footerissa
 *     ei näytetä message-tyypin ilmoitustekstiä vaan vaakaviiva (riippumatta siitä onko kyseisentyyppistä viestiä
 *     määritelty.
 * 
 * TODO
 * 
 *   - Sivun otsikon ja muokkaustietojen (kuka ja milloin) näyttäminen headerissa.
 *   - X-nappi jolla virheviestin saa piilotettua
 *   - Virheviestien tarkennusten piilotus niiden määrän ollessa suuri (esim. näytä lisää -linkki)
 * 
 * 
 */

var app = angular.module('ControlsLayout', ['localisation']);

app.directive('displayControls',function($log, LocalisationService, $filter) {
	
    return {
        restrict: 'E',
        templateUrl: "js/shared/directives/controlsLayout.html",
        replace: true,
        scope: {
        	model: "=",	// model johon controls-model viittaa
        	display: "@" // header|footer
        },
        controller: function($scope) {

        	switch ($scope.display) {
        	case "header":
        	case "footer":
        		break;
    		default:
    			throw new Error("Invalid display type: "+$scope.display);
        	}
        	
        	$scope.t = function(k, a) {
        		return LocalisationService.t(k, a);
        	}
        	
       		function showMessage(msgs, msg) {
       			if (msg==undefined) {
       				for (var i in msgs) {
       					var ms = msgs[i].show();
       					//console.log(msgs[i].tt+" -> MS=",ms);
       					if (ms==undefined || ms==null || ms==true) {
       						return true;
       					}
       				}
       				return false;
       			} else {
   					var ms = msg.show();
   					return ms==undefined || ms==null || ms==true;
       			}
       		}

       		$scope.showErrorDetail = function(msg) {
       			return showMessage($scope.model.notifs.errorDetail, msg);
       		};

       		$scope.showError = function(msg) {
       			if (msg==undefined) {
           			return showMessage($scope.model.notifs.error) || showMessage($scope.model.notifs.errorDetail);
       			}
       			return showMessage($scope.model.notifs.error, msg);
       		};
       		
       		$scope.showSuccess = function(msg) {
       			return showMessage($scope.model.notifs.success, msg) && (msg!=null || !$scope.showError());
       		};
       		
       		$scope.showMessage = function(msg) {
       			return showMessage($scope.model.notifs.message, msg);
       		};
       		
       		$scope.dto = $scope.model.dto();
       		
       		function appendMetadata(md, key, user, timestamp) {
       			if (!user && !timestamp) {
       				return;
       			}
       			if (!user || user.length==0) {
       				user = LocalisationService.t("tarjonta.metadata.unknown");
       			}
       			md.push(LocalisationService.t(key,
       					[ $filter("date")(timestamp, "d.M.yyyy"), $filter("date")(timestamp, "H:mm"), user ]));
       		}
       		
       		$scope.metadata = [];
       		if ($scope.dto.tila) {
       			$scope.metadata.push(LocalisationService.t("tarjonta.tila."+$scope.dto.tila));
       		}
       		
       		appendMetadata($scope.metadata, "tarjonta.metadata.modified", $scope.dto.modifiedBy, $scope.dto.modified)
       		appendMetadata($scope.metadata, "tarjonta.metadata.created", $scope.dto.createdBy, $scope.dto.created)
       		
       		$scope.isNew = function() {
       			return $scope.metadata.length==1;
       		}

       		function titleText() {
       			var title = $scope.model.title();
       			if ((typeof title)=="object") {
	       			if (title[LocalisationService.getLocale()]) {
	       				// käyttäjän localen mukaan
	       				return title[LocalisationService.getLocale()];
	       			} else {
	       				// vakiolocalejen mukaan
	       				for (var i in window.CONFIG.app.userLanguages) {
	       					var k = window.CONFIG.app.userLanguages[i];
	       					if (title[k]) {
	       	       				return title[k];
	       	       			}
	       				}
	       				// 1. vaihtoehto
	       				for (var i in title) {
	       					var k = title[i];
       	       				return title[window.CONFIG.app.userLanguages[i]];
	       				}
	       			}
       			}
       			return title;
       		}
       		
       		$scope.getTitle = function() {
       			var ttext = titleText();
       			var tkey = $scope.isNew() ? $scope.model.ttCreate : $scope.model.ttEdit;
       			
       			return tkey==null ? ttext : LocalisationService.t(tkey, [ ttext ]);
       		}
       		
       		return $scope;
        }
    }
    
});

app.directive('controlsModel',function($log) {
	
    return {
        restrict: 'E',
        template: "<div style=\"display:none;\" ng-transclude></div>",
        replace: true,
        transclude: true,
        scope: {
        	model: "=", // viittaus modeliin
        	ttCreate: "@", // otsikkoavain, jota käytetään luotaessa uutta
        	ttEdit: "@", // otsikkoavain, jota käytetään muokattaessa olemassaolevaa
        	title: "&", // otsikkoteksti (string tai monikielinen teksti), joka annetaan ttEdit:lle / ttCreate:lle parametriksi
        	dto: "&" // dto, josta haetaan muokkaustiedot (created, createdBy, ...); ttCreate/ttEdit valitaan näiden tietojen mukaan
        },
        controller: function($scope) {
        	$scope.model.notifs = {
   				message: [],
   				success: [],
   				error: [],
   				errorDetail: []
       		}
        	$scope.model.buttons = [];
        	
        	$scope.model.ttCreate = $scope.ttCreate;
        	$scope.model.ttEdit = $scope.ttEdit;
        	$scope.model.title = $scope.title;
        	$scope.model.dto = $scope.dto;

       		return $scope;
        }
    }
    
});

app.directive('controlsButton',function($log) {
		
    return {
        restrict: 'E',
        //replace: true,
        require: "^controlsModel",
        link: function (scope, element, attrs, controlsLayout) {
        	controlsLayout.model.buttons.push({
        		tt: scope.tt,
        		primary: scope.primary,
        		action: scope.action,
        		disabled: scope.disabled,
        		icon: scope.icon });
        },
        scope: {
        	tt: "@",	   // otsikko (lokalisaatioavain)
        	primary:"@",   // boolean; jos tosi, nappi on ensisijainen (vaikuttaa vain ulkoasuun)
        	action: "&",   // funktio jota klikatessa kutsutaan
        	disabled: "&", // funktio jonka perusteella nappi disabloidaan palauttaessa true
        	icon: "@"	   // napin ikoni (viittaus bootstrapin icon-x -luokkaan)
        		
        }
    }    
});

app.directive('controlsNotify',function($log) {
		
    return {
        restrict: 'E',
        ///replace: true,
        require: "^controlsModel",
        link: function (scope, element, attrs, controlsLayout) {
        	
        	var notifs;
        	switch (scope.type) {
        	case "message":
        	case "success":
        	case "error":
        		notifs = controlsLayout.model.notifs[scope.type];
    			break;
        	case "error-detail":
        		notifs = controlsLayout.model.notifs.errorDetail;
    			break;
    		default:
    			throw new Error("Invalid notification type: "+scope.type);
        	}
        	
        	notifs.push({
        		tt: scope.tt,
        		tp: scope.tp,
        		type: scope.type,
        		show: scope.show });
        },
        scope: {
        	tt: "&",	   // viesti (lokalisaatioavain); huom.: ei string vaan expr
        	tp: "&",	   // lokalisaatioavaimen parametrit
        	type: "@",     // ilmoituksen tyyli: message|success|error|error-detail
        	show: "&"      // funktio jonka perusteella viesti näytetään
        }
    }    
});

