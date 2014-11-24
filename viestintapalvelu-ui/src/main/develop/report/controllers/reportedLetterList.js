'use strict';

angular.module('report')
    .service('ReportedLetterListCtrlState', function() {
        var _searchTarget = "batch",
            _searchArgument = "",
            _sortAndOrder = {
                sortedby: 'timestamp',
                order: 'desc'
            },
            _pagination = {
                page: 1,
                pageSize: 10
            },
            _form = {
                organization: ''
            };
        this.store = function($scope) {
            _searchTarget = $scope.searchTarget;
            _searchArgument = $scope.searchArgument;
            _sortAndOrder = $scope.sortAndOrder;
            _pagination = $scope.pagination;
            _form = $scope.form;
        };
        this.restore = function($scope) {
            $scope.searchTarget = _searchTarget;
            $scope.searchArgument = _searchArgument;
            $scope.sortAndOrder = _sortAndOrder;
            $scope.pagination = _pagination;
            $scope.form = _form;
            $scope.fetch();
        };
    })
  .controller('ReportedLetterListCtrl', ['$scope', '$state', '$http', '$window', 'ReportedLetterListCtrlState',
        function ($scope, $state, $http, $window, ReportedLetterListCtrlState) {
    var  serviceAPIUrl = '/viestintapalvelu/api/v1',
        reportingAPIUrl = serviceAPIUrl + '/reporting',
        reportedLettersListUrl = serviceAPIUrl+'/reporting/list',
        reportedLettersSearchUrl = serviceAPIUrl+'/reporting/search';
    $scope.doneSearchTarget = 'batch';

    $scope.fetch = function () {
      var params = {}, url = reportedLettersListUrl;
      if ($scope.form.organization) {
        params.orgOid = $scope.form.organization.oid;
      }
      params.searchTarget = $scope.searchTarget || 'batch';
      if ($scope.searchTarget == 'batch' && $scope.searchArgument) {
        params.searchArgument = $scope.searchArgument;
        url = reportedLettersSearchUrl;
      }
      if ($scope.searchTarget == 'receiver' && $scope.searchArgument) {
        params.receiverSearchArgument = $scope.searchArgument;
        url = reportedLettersSearchUrl;
      }

      angular.extend(params, {
        page: $scope.pagination.page,
        nbrofrows: $scope.pagination.pageSize
      }, $scope.sortAndOrder);

      $http.get(url, {params: params})
        .success(function (reportedLettersDTO) {
          $scope.doneSearchTarget = params.searchTarget;
          $scope.reportedLettersDTO = reportedLettersDTO;
        })
        .error(function (err) {
          console.log(err);
        });
    };

    $scope.search = function () {
      // reset page for fetch
      $scope.pagination.page = 1;
      $scope.fetch();
    };

    $scope.sortBy = function (headerName) {
      var sortAndOrder = $scope.sortAndOrder;

      if (sortAndOrder.sortedby !== headerName) {
        sortAndOrder.sortedby = headerName;
        sortAndOrder.order = 'asc';
      } else {
        sortAndOrder.order = sortAndOrder.order === 'asc' ? 'desc' : 'asc';
      }

      $scope.pagination.page = 1;
      $scope.fetch();
    };

    $scope.showReportedLetter = function (reportedLetter) {
      ReportedLetterListCtrlState.store($scope);
      $state.go('letter_batch_view', {letterBatchID: reportedLetter.letterBatchID});
    };

    $scope.emptySearch = function () {
      $scope.searchTarget = 'batch';
      $scope.doneSearchTarget = 'batch';
      $scope.searchArgument = '';
      $scope.pagination.page = 1;
      $scope.fetch();
    };

    $scope.getSortClass = function (headerName) {
      var className = '';
      if ($scope.sortAndOrder.sortedby === headerName) {
        className = 'sort-' + $scope.sortAndOrder.order;
      }
      return className;
    };

    $scope.downloadLetter = function (letterReceiverLetterID) {
        var getLetterDownloadLink, letterDownloadLinkUrl = reportingAPIUrl + '/letter';

        getLetterDownloadLink = $http.get(letterDownloadLinkUrl, { params: { id: letterReceiverLetterID } });
        getLetterDownloadLink.success(function (downloadLink) {
            // changing window location will initiate download prompt
            // TODO: the backend should return only the documentID, not the whole url
            //       this way naturally exposes the user to security risks
            var acceptableDownloadLink = $window.location.origin + serviceAPIUrl + '/download/';
            if (downloadLink.indexOf(acceptableDownloadLink) === 0) {
                $window.location = downloadLink;
            }
        });
    };
    ReportedLetterListCtrlState.restore($scope);
  }]);
