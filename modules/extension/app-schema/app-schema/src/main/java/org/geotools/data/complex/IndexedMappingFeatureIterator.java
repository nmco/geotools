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
package org.geotools.data.complex;

import static org.geotools.data.complex.ComplexFeatureConstants.DEFAULT_GEOMETRY_LOCAL_NAME;

import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.complex.IndexQueryManager.QueryIndexCoverage;
import org.geotools.data.complex.filter.IndexUnmappingVisitor;
import org.geotools.data.complex.filter.XPath;
import org.geotools.data.complex.filter.XPathUtil.StepList;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.filter.AttributeExpressionImpl;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.sort.SortBy;
import org.xml.sax.helpers.NamespaceSupport;

/** @author Fernando Miño (Geosolutions) */
public abstract class IndexedMappingFeatureIterator implements IMappingFeatureIterator {

    protected final AppSchemaDataAccess store;
    protected final FeatureTypeMapping mapping;
    protected final Query query;
    protected final Filter unrolledFilter;
    protected final Transaction transaction;

    protected IndexQueryManager indexModeProc;
    protected QueryIndexCoverage queryMode;

    public IndexedMappingFeatureIterator(
            AppSchemaDataAccess store,
            FeatureTypeMapping mapping,
            Query query,
            Filter unrolledFilter,
            Transaction transaction,
            IndexQueryManager indexModeProcessor) {
        this.store = store;
        this.mapping = mapping;
        this.query = query;
        this.unrolledFilter = unrolledFilter;
        this.transaction = transaction;
        this.indexModeProc = indexModeProcessor;
        selectExecutionPlan();
    }

    /**
     * Analyze query and select a plan: 1.- All fields indexed, execute all query on index layer 2.-
     * Mixed fields indexed and not, execute indexed operators and re-map query to database
     */
    protected void selectExecutionPlan() {
        queryMode = indexModeProc.getIndexMode();
    }

    protected Query unrollIndexes(Query query) {
        Query newQuery = new Query(query);
        newQuery.setFilter(unrollFilter(query.getFilter()));
        newQuery.setSortBy(unrollSortBy(query.getSortBy()));
        return newQuery;
    }

    protected SortBy[] unrollSortBy(SortBy[] sortArray) {
        if (sortArray == null) return null;
        ArrayList<SortBy> unrolledSorts = new ArrayList<>();
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        for (SortBy aSort : sortArray) {
            SortBy newSort =
                    ff.sort(
                            unrollIndex(aSort.getPropertyName(), mapping).getPropertyName(),
                            aSort.getSortOrder());
            unrolledSorts.add(newSort);
        }
        return unrolledSorts.toArray(new SortBy[] {});
    }

    public static PropertyName unrollIndex(PropertyName expression, FeatureTypeMapping mapping) {
        String targetXPath = expression.getPropertyName();
        // replace the artificial DEFAULT_GEOMETRY property with the actual one
        if (DEFAULT_GEOMETRY_LOCAL_NAME.equals(targetXPath)) {
            targetXPath = mapping.getDefaultGeometryXPath();
        }
        NamespaceSupport namespaces = mapping.getNamespaces();
        AttributeDescriptor root = mapping.getTargetFeature();
        StepList simplifiedSteps = XPath.steps(root, targetXPath, namespaces);
        AttributeMapping attMapping = mapping.getAttributeMapping(simplifiedSteps);
        if (attMapping == null || StringUtils.isEmpty(attMapping.getIndexField()))
            return expression;
        return new AttributeExpressionImpl(attMapping.getIndexField());
    }

    protected Filter unrollFilter(Filter filter) {
        IndexUnmappingVisitor visitor = new IndexUnmappingVisitor(mapping);
        Filter unrolledFilter = (Filter) filter.accept(visitor, null);
        return unrolledFilter;
    }

    protected String getIdValue(FeatureId fid) {
        return fid.getID().substring(fid.getID().indexOf('.') + 1, fid.getID().length());
    }
}
