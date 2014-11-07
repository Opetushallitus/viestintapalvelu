'use strict';

angular.module('letter-templates')
    .controller('LetterTemplateListCtrl', ['$scope',
        function($scope) {

            $scope.radioSelection;
            $scope.applicationTarget;

            $scope.letterTypes = [
                {
                    value: 'default',
                    text: 'Oletuskirjepohja'
                },{
                    value: 'applicationTarget',
                    text: 'Hakukohtaiset kirjepohjat'
                }
            ];

            $scope.applicationTargets = ['Korkeakoulujen yhteishaku syksy 2013', 'Korkeakoulujen yhteishaku syksy 2014'];

            $scope.letterTemplates = [
                {
                    applicationTarget: 'Korkeakoulujen yhteishaku syksy 2014',
                    type: 'Koekutsukirje',
                    languages: ['suomi', 'ruotsi'],
                    status: 'Keskeneräinen',
                    saveDate: '16.10.2140 10:17'
                },{
                    applicationTarget: 'Korkeakoulujen yhteishaku syksy 2014',
                    type: 'Hyväksymistesti',
                    languages: ['suomi', 'ruotsi', 'englanti'],
                    status: 'Käytössä',
                    saveDate: '16.10.2140 10:17'
                },{
                    applicationTarget: 'Korkeakoulujen yhteishaku syksy 2014',
                    type: 'Jälkiohjauskirje',
                    languages: ['suomi'],
                    status: 'Poistettu käytöstä',
                    saveDate: '16.10.2140 10:17'
                }
            ];

        }
    ]);