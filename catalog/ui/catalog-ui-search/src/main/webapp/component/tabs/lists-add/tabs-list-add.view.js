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
/*global define, alert*/
define([
    'wreqr',
    'marionette',
    'underscore',
    'jquery',
    '../tabs.view',
    'js/store',
    'properties',
    './tabs-list-add',
], function (wreqr, Marionette, _, $, TabsView, store, properties, ListAddModel) {

    return TabsView.extend({
        className: 'is-list-add',
        setDefaultModel: function(){
            this.model = new ListAddModel();
        },
        selectionInterface: store,
        initialize: function(options){
            this.selectionInterface = options.selectionInterface || store;
            if (options.model === undefined){
                this.setDefaultModel();
            }
            TabsView.prototype.initialize.call(this);
        }
    });
});