/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/


define([
    'backbone',
    'marionette',
    'underscore',
    'jquery',
    './builder.hbs',
    'js/CustomElements',
    'component/property/property.view',
    'component/property/property',
    'component/dropdown/details-filter/dropdown.details-filter.view',
    'component/dropdown/dropdown',
    'component/dropdown/details-interactions/dropdown.details-interactions.view',
    'component/singletons/user-instance',
    'properties',
    'component/property/property.collection.view',
    'component/loading/loading.view',
    'component/tabs/list-add/available-types.view',
    'component/tabs/list-add/builder',
    'component/dropdown/dropdown.view',
    'component/singletons/metacard-definitions'
], function (Backbone, Marionette, _, $, template, CustomElements, PropertyView, Property, DetailsFilterView, DropdownModel, DetailsInteractionsView,
    user, properties, PropertyCollectionView, LoadingView, AvailableTypesView, Builder, DropdownView, metacardDefinitions) {

    var availableTypes;

    var ajaxCall = $.get({
        url: '/search/catalog/internal/builder/availabletypes?wait=0'
    }).then((response) => {
        availableTypes = response;
    });

    return Marionette.LayoutView.extend({
            template: template,
            tagName: CustomElements.register('builder'),
            modelEvents: {
                'change:selectedAvailableType': 'handleSelectedAvailableType',
                'change:metacard': 'handleMetacard'
            },
            events: {
                'click .builder-edit': 'edit',
                'click .builder-save': 'save',
                'click .builder-cancel': 'cancel'
            },
            regions: {
                builderProperties: '> .builder-properties',
                builderAvailableType: '> .builder-select-available-type > .builder-select-available-type-dropdown'
            },
            initialize() {
                this.model = new Builder();

                if (!availableTypes) {
                    var loadingview = new LoadingView();
                    ajaxCall.then(() => {
                        loadingview.remove();
                        this.model.set('availableTypes', availableTypes);
                        // TODO this seems like a race condition. If the ajax call finishes before the initialize method finishes and the DOM is built, then the call the handleAvailableTypes will fail
                        this.handleAvailableTypes();
                    });
                } else {
                    this.model.set('availableTypes', availableTypes);
                }

            },
            handleAvailableTypes() {

                // if there is one available type, then render for that type

                // this.model.set('selectedMetacardType', GET THE ONLY TYPE)

                // if there are multiple types, then allow the user to select the type

                this.$el.addClass('is-selecting-available-types');

            },
            handleSelectedAvailableType() {
                this.$el.removeClass('is-selecting-available-types');

                const metacardDefinition = metacardDefinitions.metacardDefinitions[this.model.get('selectedAvailableType')];

                const nonInjectedAttributeNames = Object.keys(metacardDefinition)
                    .filter(attributeName => !metacardDefinition[attributeName].isInjected)
                    .filter(attributeName => attributeName !== "id");

                const propertyCollection = {
                    'metacard-type': this.model.get('selectedAvailableType')
                };

                nonInjectedAttributeNames.forEach(attribute => {
                    if(metacardDefinitions.enums[attribute]) {
                        propertyCollection[attribute] = metacardDefinitions.enums[attribute][0];
                    } else {
                        propertyCollection[attribute] = "";
                    }
                });

                this.model.set('metacard', propertyCollection);

            },
            handleMetacard() {
                console.log("handleMetacard");

                this.builderProperties.show(PropertyCollectionView.generatePropertyCollectionView([this.model.get('metacard')]));
                this.builderProperties.currentView.turnOnLimitedWidth();
                this.builderProperties.currentView.$el.addClass("is-list");

            },
            onBeforeShow() {

                    this.builderAvailableType.show(DropdownView.createSimpleDropdown({
                                                  componentToShow: AvailableTypesView,
                                                  modelForComponent: this.model,
                                                  leftIcon: 'fa fa-ellipsis-v'
                                                }));

                    this.handleAvailableTypes();

            },
            edit: function(){
                        this.$el.addClass('is-editing');
                        this.builderProperties.currentView.turnOnEditing();
                        this.builderProperties.currentView.focus();
            },
            cancel: function(){
                        this.$el.removeClass('is-editing');
                        //this.attributesAdded.reset();
                        //this.attributesRemoved.reset();
                        this.builderProperties.currentView.revert();
                        this.builderProperties.currentView.turnOffEditing();
                        //this.afterCancel();
            },
            save() {
                        this.$el.removeClass('is-editing');

                        // submit the metacard to the catalog

                        const editedMetacard = this.builderProperties.currentView.toJSON([], []);

                        debugger

                        console.log("edit complete" + editedMetacard);

                        // LoadingCompanionView.beginLoading(this);


//                        //var ephemeralAttributes = this.attributesAdded.map((model) => model.id);
//                        //var attributesToRemove = this.attributesRemoved.map((model) => model.id);
//                        this.afterSave(this.builderProperties.currentView.toJSON([], []));
//                        //this.attributesAdded.reset();
//                        //this.attributesRemoved.reset();
//                        this.builderProperties.currentView.revert();
//                        this.builderProperties.currentView.turnOffEditing();
            },
            afterSave: function(editorJSON){
                if (editorJSON.length > 0){
                   var payload = [
                       {
                           ids: [this.model.first().get('metacard').get('id')],
                           attributes: editorJSON
                       }
                   ];
                   LoadingCompanionView.beginLoading(this);
                   var self = this;
                   setTimeout(function(){
                       $.ajax({
                           url: '/search/catalog/internal/metacards',
                           type: 'PATCH',
                           data: JSON.stringify(payload),
                           contentType: 'application/json'
                       }).then(function(response){
                           ResultUtils.updateResults(self.model, response);
                       }).always(function(){
                           setTimeout(function(){  //let solr flush
                              LoadingCompanionView.endLoading(self);
                              if (!self.isDestroyed) {
                                self.getValidation();
                              }
                           }, 1000);
                       });
                   }, 1000);
                }
            }
    });
});    