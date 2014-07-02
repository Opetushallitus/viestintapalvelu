'use strict';

angular.module('reportingApp')
.factory('ErrorDialog',
    function($modal) {
        return {
            showError : function(msg) {
                if (msg.status == '403') {
                    msg.data = 'error.msg.userNotAuthorized';
                }
                
                return $modal.open({
                    templateUrl: './html/errorDialog.html',
                    controller: 'ErrorDialogCtrl',
                    size: 'lg',
                    resolve: {
                        msg: function() {
                            return angular.copy(msg.data);
                        }
                    }
                });
            }
        };
    }
);