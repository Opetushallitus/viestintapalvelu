'use strict';

angular.module('letter-templates')
.controller('ValiController', ['$http', '$state','$location', function ($http, $state, $location) {
	$http.get("/viestintapalvelu/api/v1/template/ok").success(function (data) {}).error(function(data, status, headers, config) {}).then(function(result) {
        $location.path('/letter-templates-ui');
    });
}]);
