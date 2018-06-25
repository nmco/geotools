/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2018, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.complex.AttributeMapping;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.filter.XPath;
import org.geotools.data.complex.filter.XPathUtil.StepList;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.identity.FeatureId;

/** @author Fernando Miño - Geosolutions */
public abstract class IndexQueryUtils {

    public static List<String> getAttributesOnSort(Query query) {
        List<String> result = new ArrayList<>();
        if (query.getSortBy() == null) return result;
        for (int i = 0; i < query.getSortBy().length; i++) {
            result.add(query.getSortBy()[i].getPropertyName().getPropertyName());
        }
        return result;
    }

    public static List<String> getAttributesOnFilter(Filter filter) {
        String[] attrs = DataUtilities.attributeNames(filter);
        return new ArrayList<String>(Arrays.asList(attrs));
    }

    public static boolean isExpressionEmpty(Expression expression) {
        if (expression == null || Expression.NIL.equals(expression)) return true;
        return false;
    }

    public static boolean equalsProperty(AttributeMapping mapping, String propertyName) {
        return (equalsPropertyExpression(mapping.getSourceExpression(), propertyName)
                || equalsPropertyExpression(mapping.getIdentifierExpression(), propertyName));
    }

    public static boolean equalsXpath(
            FeatureTypeMapping mapping, AttributeMapping attMapping, String xpath) {
        StepList simplifiedSteps =
                XPath.steps(mapping.getTargetFeature(), xpath, mapping.getNamespaces());
        return Objects.equals(attMapping, simplifiedSteps);
    }

    public static boolean equalsPropertyExpression(Expression expression, String propertyName) {
        if (IndexQueryUtils.isExpressionEmpty(expression)) return false;
        String[] name = DataUtilities.attributeNames(expression);
        if (name.length != 1) return false;
        return Objects.equals(name[0], propertyName);
    }

    public static boolean checkAllUnrolledPropertiesIndexed(
            List<String> properties, FeatureTypeMapping mapping) {
        return !properties.stream().anyMatch(p -> mapping.getIndexAttributeNameUnrolled(p) == null);
    }

    public static boolean checkAllPropertiesIndexed(
            List<String> properties, FeatureTypeMapping mapping) {
        return !properties.stream().anyMatch(p -> mapping.getIndexAttributeName(p) == null);
    }

    public static Filter buildIdInExpression(
            FeatureCollection<? extends FeatureType, ? extends Feature> featureCollection,
            FeatureTypeMapping mapping) {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();

        // build filter list
        List<Filter> idFilters = new ArrayList<>();
        FeatureStreams.toFeatureStreamGeneric(featureCollection)
                .map(Feature::getIdentifier)
                .map(FeatureId::getID)
                .collect(Collectors.toList())
                .forEach(
                        idStr -> {
                            idFilters.add(
                                    ff.equals(mapping.getFeatureIdExpression(), ff.literal(idStr)));
                        });

        return ff.or(idFilters);
    }

    public static Filter buildIdInExpression(List<String> ids, FeatureTypeMapping mapping) {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();

        List<Filter> idFilters = new ArrayList<>();
        ids.forEach(
                idStr -> {
                    idFilters.add(ff.equals(mapping.getFeatureIdExpression(), ff.literal(idStr)));
                });

        return ff.or(idFilters);
    }

    public static Filter buildIdInExpressionSchema(List<String> ids, FeatureTypeMapping mapping) {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();

        List<Filter> idFilters = new ArrayList<>();
        ids.forEach(
                idStr -> {
                    idFilters.add(
                            ff.equals(
                                    ff.property(
                                            mapping.getTargetFeature().getName().getLocalPart()),
                                    ff.literal(idStr)));
                });

        return ff.or(idFilters);
    }
}
