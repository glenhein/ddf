/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.source.opensearch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.Expression;
import org.geotools.filter.IsEqualsToImpl;
import org.geotools.filter.LikeFilterImpl;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.temporal.BinaryTemporalOperator;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.TOverlaps;
import org.opengis.temporal.Period;
import org.opengis.temporal.PeriodDuration;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import com.vividsolutions.jts.geom.Coordinate;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.impl.PropertyIsEqualToLiteral;
import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;

public class OpenSearchFilterVisitor extends DefaultFilterVisitor {
    private static final String ONLY_AND_MSG = "Opensearch only supports AND operations for non-contextual criteria.";

    private static XLogger logger = new XLogger(
            LoggerFactory.getLogger(OpenSearchFilterVisitor.class));

    private List<Filter> filters;

    // Can only have one each of each type of filter in an OpenSearch query
    private ContextualSearch contextualSearch;

    private TemporalFilter temporalSearch;

    private SpatialFilter spatialSearch;

    private String id;

    private NestedTypes currentNest = null;

    public OpenSearchFilterVisitor() {
        filters = new ArrayList<Filter>();

        contextualSearch = null;
        temporalSearch = null;
        spatialSearch = null;
        id = null;
    }

    @Override
    public Object visit(Not filter, Object data) {
        Object newData;
        NestedTypes parentNest = currentNest;
        logger.trace("ENTERING: NOT filter");
        currentNest = NestedTypes.NOT;
        filters.add(filter);
        newData = super.visit(filter, data);
        currentNest = parentNest;
        logger.trace("EXITING: NOT filter");

        return newData;
    }

    @Override
    public Object visit(Or filter, Object data) {
        Object newData;
        NestedTypes parentNest = currentNest;
        logger.trace("ENTERING: OR filter");
        currentNest = NestedTypes.OR;
        filters.add(filter);
        newData = super.visit(filter, data);
        currentNest = parentNest;
        logger.trace("EXITING: OR filter");

        return newData;
    }

    @Override
    public Object visit(And filter, Object data) {
        Object newData;
        NestedTypes parentNest = currentNest;
        logger.trace("ENTERING: AND filter");
        currentNest = NestedTypes.AND;
        filters.add(filter);
        newData = super.visit(filter, data);
        currentNest = parentNest;
        logger.trace("EXITING: AND filter");

        return newData;
    }

    /**
     * DWithin filter maps to a Point/Radius distance Spatial search criteria.
     */
    @Override
    public Object visit(DWithin filter, Object data) {
        logger.trace("ENTERING: DWithin filter");
        if (currentNest == null || NestedTypes.AND.equals(currentNest)) {
            // The geometric point is wrapped in a <Literal> element, so have to
            // get geometry expression as literal and then evaluate it to get
            // the geometry.
            // Example:
            // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl@dc33f184</ogc:Literal>
            Literal literalWrapper = (Literal) filter.getExpression2();

            // Luckily we know what type the geometry expression should be, so
            // we
            // can cast it
            PointImpl point = (PointImpl) literalWrapper.evaluate(null);
            double[] coords = point.getCentroid()
                    .getCoordinate();
            double distance = filter.getDistance();

            logger.debug("point: coords[0] = " + coords[0] + ",   coords[1] = " + coords[1]);
            logger.debug("radius = " + distance);

            this.spatialSearch = new SpatialDistanceFilter(coords[0], coords[1], distance);

            filters.add(filter);
        } else {
            logger.debug(ONLY_AND_MSG);
        }

        logger.trace("EXITING: DWithin filter");

        return super.visit(filter, data);
    }

    /**
     * Contains filter maps to a Polygon or BBox Spatial search criteria.
     */
    @Override
    public Object visit(Contains filter, Object data) {
        logger.trace("ENTERING: Contains filter");
        if (currentNest == null || NestedTypes.AND.equals(currentNest)) {
            // The geometric point is wrapped in a <Literal> element, so have to
            // get geometry expression as literal and then evaluate it to get
            // the geometry.
            // Example:
            // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl@64a7c45e</ogc:Literal>
            Literal literalWrapper = (Literal) filter.getExpression2();
            Object geometryExpression = literalWrapper.getValue();

            StringBuffer geometryWkt = new StringBuffer();

            if (geometryExpression instanceof SurfaceImpl) {
                SurfaceImpl polygon = (SurfaceImpl) literalWrapper.evaluate(null);

                Coordinate[] coords = polygon.getJTSGeometry()
                        .getCoordinates();

                geometryWkt.append("POLYGON((");
                for (int i = 0; i < coords.length; i++) {
                    geometryWkt.append(coords[i].x);
                    geometryWkt.append(" ");
                    geometryWkt.append(coords[i].y);

                    if (i != (coords.length - 1)) {
                        geometryWkt.append(",");
                    }
                }
                geometryWkt.append("))");
                this.spatialSearch = new SpatialFilter(geometryWkt.toString());

                logger.debug("geometryWkt = [" + geometryWkt.toString() + "]");

                filters.add(filter);

            } else {
                logger.debug("Only POLYGON geometry WKT for Contains filter is supported");
            }
        } else {
            logger.debug(ONLY_AND_MSG);
        }

        logger.trace("EXITING: Contains filter");

        return super.visit(filter, data);
    }

    /**
     * Intersects filter maps to a Polygon or BBox Spatial search criteria.
     */
    @Override
    public Object visit(Intersects filter, Object data) {
        logger.trace("ENTERING: Intersects filter");
        if (currentNest == null || NestedTypes.AND.equals(currentNest)) {
            // The geometric point is wrapped in a <Literal> element, so have to
            // get geometry expression as literal and then evaluate it to get
            // the geometry.
            // Example:
            // <ogc:Literal>org.geotools.geometry.jts.spatialschema.geometry.primitive.SurfaceImpl@64a7c45e</ogc:Literal>
            Literal literalWrapper = (Literal) filter.getExpression2();
            Object geometryExpression = literalWrapper.getValue();

            StringBuffer geometryWkt = new StringBuffer();

            if (geometryExpression instanceof SurfaceImpl) {
                SurfaceImpl polygon = (SurfaceImpl) literalWrapper.evaluate(null);

                Coordinate[] coords = polygon.getJTSGeometry()
                        .getCoordinates();

                geometryWkt.append("POLYGON((");
                for (int i = 0; i < coords.length; i++) {
                    geometryWkt.append(coords[i].x);
                    geometryWkt.append(" ");
                    geometryWkt.append(coords[i].y);

                    if (i != (coords.length - 1)) {
                        geometryWkt.append(",");
                    }
                }
                geometryWkt.append("))");
                this.spatialSearch = new SpatialFilter(geometryWkt.toString());

                logger.debug("geometryWkt = [" + geometryWkt.toString() + "]");

                filters.add(filter);
            } else {
                logger.debug("Only POLYGON geometry WKT for Intersects filter is supported");
            }
        } else {
            logger.debug(ONLY_AND_MSG);
        }

        logger.trace("EXITING: Intersects filter");

        return super.visit(filter, data);
    }

    /**
     * TOverlaps filter maps to a Temporal (Absolute and Offset) search criteria.
     */
    @Override
    public Object visit(TOverlaps filter, Object data) {
        logger.trace("ENTERING: TOverlaps filter");
        if (currentNest == null || NestedTypes.AND.equals(currentNest)) {
            handleTemporal(filter);
        } else {
            logger.debug(ONLY_AND_MSG);
        }
        logger.trace("EXITING: TOverlaps filter");

        return super.visit(filter, data);
    }

    /**
     * During filter maps to a Temporal (Absolute and Offset) search criteria.
     */
    @Override
    public Object visit(During filter, Object data) {
        logger.trace("ENTERING: TOverlaps filter");
        if (currentNest == null || NestedTypes.AND.equals(currentNest)) {
            handleTemporal(filter);
        } else {
            logger.debug(ONLY_AND_MSG);
        }
        logger.trace("EXITING: TOverlaps filter");

        return super.visit(filter, data);
    }

    private void handleTemporal(BinaryTemporalOperator filter) {

        Literal literalWrapper = (Literal) filter.getExpression2();
        logger.trace("literalWrapper.getValue() = " + literalWrapper.getValue());

        Object literal = literalWrapper.evaluate(null);
        if (literal instanceof Period) {
            Period period = (Period) literal;

            // Extract the start and end dates from the filter
            Date start = period.getBeginning()
                    .getPosition()
                    .getDate();
            Date end = period.getEnding()
                    .getPosition()
                    .getDate();

            temporalSearch = new TemporalFilter(start, end);

            filters.add(filter);
        } else if (literal instanceof PeriodDuration) {

            DefaultPeriodDuration duration = (DefaultPeriodDuration) literal;

            // Extract the start and end dates from the filter
            Date end = Calendar.getInstance()
                    .getTime();
            Date start = new Date(end.getTime() - duration.getTimeInMillis());

            temporalSearch = new TemporalFilter(start, end);

            filters.add(filter);
        }

    }

    /**
     * PropertyIsLike filter maps to a Contextual search criteria.
     */
    @Override
    public Object visit(PropertyIsLike filter, Object data) {
        logger.trace("ENTERING: PropertyIsLike filter");

        if (currentNest != NestedTypes.NOT) {

            LikeFilterImpl likeFilter = (LikeFilterImpl) filter;

            AttributeExpressionImpl expression = (AttributeExpressionImpl) likeFilter.getExpression();
            String selectors = expression.getPropertyName();
            logger.debug("selectors = " + selectors);

            String searchPhrase = likeFilter.getLiteral();
            logger.debug("searchPhrase = [" + searchPhrase + "]");
            if (contextualSearch != null) {
                contextualSearch.setSearchPhrase(
                        contextualSearch.getSearchPhrase() + " " + currentNest.toString() + " "
                                + searchPhrase);
            } else {
                contextualSearch = new ContextualSearch(selectors, searchPhrase,
                        likeFilter.isMatchingCase());
            }
        }

        logger.trace("EXITING: PropertyIsLike filter");

        return super.visit(filter, data);
    }

    /**
     * PropertyIsEqualTo filter maps to a type/version criteria.
     */
    @Override
    public Object visit(PropertyIsEqualTo filter, Object data) {
        logger.trace("ENTERING: PropertyIsEqualTo filter");

        if (currentNest != NestedTypes.NOT) {
            if (filter instanceof IsEqualsToImpl) {
                IsEqualsToImpl isEqualsTo = (IsEqualsToImpl) filter;
                Expression leftValue = isEqualsTo.getLeftValue();
                if (Metacard.ID.equals(leftValue.toString())) {
                    id = isEqualsTo.getExpression2()
                            .toString();
                }
            } else if (filter instanceof PropertyIsEqualToLiteral) {
                PropertyIsEqualToLiteral isEqualsTo = (PropertyIsEqualToLiteral) filter;
                if (Metacard.ID.equals(isEqualsTo.getExpression1()
                        .toString())) {
                    id = isEqualsTo.getExpression2()
                            .toString();
                }
            }
        }

        filters.add(filter);

        logger.trace("EXITING: PropertyIsEqualTo filter");

        return super.visit(filter, data);
    }

    @Override
    public Object visit(PropertyName expression, Object data) {
        logger.trace("ENTERING: PropertyName expression");

        // countOccurrence( expression );

        logger.trace("EXITING: PropertyName expression");

        return data;
    }

    @Override
    public Object visit(Literal expression, Object data) {
        logger.trace("ENTERING: Literal expression");

        // countOccurrence( expression );

        logger.trace("EXITING: Literal expression");

        return data;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public ContextualSearch getContextualSearch() {
        return contextualSearch;
    }

    public TemporalFilter getTemporalSearch() {
        return temporalSearch;
    }

    public SpatialFilter getSpatialSearch() {
        return spatialSearch;
    }

    public String getMetacardId() {
        return id;
    }

    private enum NestedTypes {
        AND, OR, NOT
    }

}
