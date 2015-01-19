'use strict';

angular.module('letter-templates')
    .controller('LetterTemplateCreateCtrl', ['$scope', 'Global', '$state', 'TemplateService', '$filter',
        function($scope, Global, $state, TemplateService, $filter) {

            // Default values
            $scope.editorOptions = Global.getEditorOptions();
            $scope.template = TemplateService.getTemplate();
            $scope.baseTemplate = TemplateService.getBase();
            TemplateService.getTemplateById($scope.baseTemplate.id)
                .success(function(template) {
                    $scope.templateContent = template;
                    $scope.structure = TemplateService.getStructureById(template.structureId)
                        .success(function(structure) {
                            $scope.replacements = structure.replacements;
                        }).error(function(error) {
                            console.log(error);
                        });
                }).error(function(error) {
                    console.log(error);
                });

            $scope.getMatchingTemplateReplacement = function(key) {
                var found = $filter('filter')($scope.templateContent.replacements, {name: key});
                return found.length ? found[0] : {name: key, defaultValue: ''};
            };

            $scope.cancel = function() {
                $state.go('letter-templates.overview');
            };
            $scope.save = function() {
                var template = $scope.templateContent;
                template = clearTemplate(template);
                template.applicationPeriods = [$scope.template.oid];
                template.state = 'luonnos';
                TemplateService.saveTemplate(template);
            };
            $scope.previewLetter = function() {
                console.log('Preview letter');
            };
            $scope.previewPDF = function() {
                console.log('Preview PDF');
            };
            $scope.publish = function() {
                var template = $scope.templateContent;
                template = clearTemplate(template);
                template.applicationPeriods = [$scope.template.oid];
                template.state = 'julkaistu';
                TemplateService.saveTemplate(template);
            };

            var clearTemplate = function(template) {
                delete template.id;
                delete template.timestamp;
                delete template.storingOid;
                delete template.organizationOid;
                template.replacements.forEach(function(repl) {
                    delete repl.id;
                    delete repl.timestamp;
                });
                delete template.usedAsDefault;
                return template;
            };

            $scope.buttons = [
                {label: $filter('i18n')('common.btn.cancel'), click: $scope.cancel, type: 'default'},
                {label: $filter('i18n')('template.btn.preview.pdf'), click: $scope.previewPDF, type: 'default'},
                {label: $filter('i18n')('template.btn.preview.email'), click: $scope.previewLetter, type: 'default'},
                {label: $filter('i18n')('common.btn.save'), click: $scope.save, primary: true},
                {label: $filter('i18n')('common.btn.publish'), click: $scope.publish}];
        }
    ]);