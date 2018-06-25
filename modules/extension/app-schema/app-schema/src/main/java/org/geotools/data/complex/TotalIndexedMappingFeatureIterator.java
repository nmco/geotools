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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.identity.FeatureId;

public class TotalIndexedMappingFeatureIterator extends IndexedMappingFeatureIterator {

    private static int MAX_FEATURES_ROUND = 100;

    private FeatureIterator<? extends Feature> indexIterator;
    private FeatureIterator<? extends Feature> sourceIterator;

    public TotalIndexedMappingFeatureIterator(
            AppSchemaDataAccess store,
            FeatureTypeMapping mapping,
            Query query,
            Filter unrolledFilter,
            Transaction transaction,
            IndexQueryManager indexModeProcessor) {
        super(store, mapping, query, unrolledFilter, transaction, indexModeProcessor);
    }

    private FeatureIterator<? extends Feature> getIndexIterator() {
        if (indexIterator == null) {
            try {
                initializeIndexIterator();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        return indexIterator;
    }

    private void initializeIndexIterator() throws IOException {
        // rebuild Query to fetch only id attributes:
        Query idQuery = transformQueryToIdsOnly();
        // get iterator on new query
        indexIterator = mapping.getIndexSource().getFeatures(idQuery).features();
    }

    private Query transformQueryToIdsOnly() {
        Query idsQuery = new Query(unrollIndexes(query));
        idsQuery.setProperties(Query.NO_PROPERTIES);
        return idsQuery;
    }

    private void initNextSourceIndexRound() {
        Set<FeatureId> idlist = getNextSourceIds();
        // construct new Query with an unique IN function filter
        Query nextQuery = new Query(query);
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        // build list of equals filters
        List<Filter> idFilters = new ArrayList<>();
        for (FeatureId aid : idlist) {
            Expression e = ff.property(mapping.getTargetFeature().getName());
            idFilters.add(ff.equals(e, ff.literal(aid.getID())));
        }
        // create OR oprator with list, intead of using ff.id(idlist);
        Filter nextFilter = ff.or(idFilters);
        nextQuery.setFilter(nextFilter);
        try {
            sourceIterator =
                    MappingFeatureIteratorFactory.getInstance(
                            store, mapping, nextQuery, unrolledFilter, transaction, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<FeatureId> getNextSourceIds() {
        int numFeatures = 0;
        Set<FeatureId> flist = new HashSet<>();
        while (numFeatures < MAX_FEATURES_ROUND && getIndexIterator().hasNext()) {
            Feature feature = getIndexIterator().next();
            FilterFactory ff = CommonFactoryFinder.getFilterFactory();
            FeatureId fid = ff.featureId(this.getIdValue(feature.getIdentifier()));
            flist.add(fid);
            numFeatures++;
        }
        return flist;
    }

    private void closeIndexIterator() {
        if (indexIterator == null) return;
        indexIterator.close();
    }

    private void closeSourceIterator() {
        if (sourceIterator == null) return;
        sourceIterator.close();
    }

    @Override
    public boolean hasNext() {
        // if sourceIterator is unable to provide more features or it isn't initialized
        if (sourceIterator == null || !sourceIterator.hasNext()) {
            // If there are more features on index, fetch next MAX_FEATURES_ROUND
            if (getIndexIterator().hasNext()) {
                closeSourceIterator();
                initNextSourceIndexRound();
                return this.hasNext();
            } else {
                // no more features from index, return false
                return false;
            }
        }
        return sourceIterator.hasNext();
    }

    @Override
    public Feature next() {
        if (hasNext()) return sourceIterator.next();
        return null;
    }

    @Override
    public void close() {
        closeIndexIterator();
        closeSourceIterator();
    }
}
