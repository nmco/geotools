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
import java.util.List;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.complex.IndexQueryManager.PartialIndexQueryManager;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;

/**
 * MappingFeatureIterator with partial index query support
 *
 * @author Fernando Miño, Geosolutions
 */
public class PartialIndexedMappingFeatureIterator extends IndexedMappingFeatureIterator {

    private int maxNumRound = 100;

    private FeatureIterator<? extends Feature> indexIterator;
    private FeatureIterator<? extends Feature> sourceIterator;
    private int sourceFeaturesCounter = 0;
    private PartialIndexQueryManager partialIQM;

    public PartialIndexedMappingFeatureIterator(
            AppSchemaDataAccess store,
            FeatureTypeMapping mapping,
            Query query,
            Filter unrolledFilter,
            Transaction transaction,
            IndexQueryManager indexModeProcessor) {
        this(store, mapping, query, unrolledFilter, transaction, indexModeProcessor, 100);
    }

    public PartialIndexedMappingFeatureIterator(
            AppSchemaDataAccess store,
            FeatureTypeMapping mapping,
            Query query,
            Filter unrolledFilter,
            Transaction transaction,
            IndexQueryManager indexModeProcessor,
            int maxNumRound) {
        super(store, mapping, query, unrolledFilter, transaction, indexModeProcessor);
        this.maxNumRound = maxNumRound;
        partialIQM = new PartialIndexQueryManager(mapping, query);
    }

    private void initializeIndexIterator() throws IOException {
        // get indexed Query and rebuild to fetch only id attributes:
        Query idQuery = transformQueryToIdsOnly();
        // get iterator on new query
        indexIterator = mapping.getIndexSource().getFeatures(idQuery).features();
    }

    private Query transformQueryToIdsOnly() {
        Query idsQuery = new Query(unrollIndexes(partialIQM.getIndexQuery()));
        idsQuery.setProperties(Query.NO_PROPERTIES);
        idsQuery.setTypeName(mapping.getIndexSource().getSchema().getTypeName());
        return idsQuery;
    }

    private FeatureIterator<? extends Feature> getIndexIterator() {
        if (indexIterator == null) {
            try {
                initializeIndexIterator();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return indexIterator;
    }

    private List<String> getNextIdList() {
        List<String> result = new ArrayList<>();
        int count = 0;
        while (getIndexIterator().hasNext() && count < maxNumRound) {
            result.add(getIndexIterator().next().getIdentifier().getID());
            count++;
        }
        return result;
    }

    private void nextSourceIteratorInstance() {
        List<String> ids = getNextIdList();
        Query nextQuery = partialIQM.buildCombinedQuery(ids);
        try {
            sourceIterator =
                    MappingFeatureIteratorFactory.getInstance(
                            store, mapping, nextQuery, unrolledFilter, transaction, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        // if sourceIterator is instanced and hasNext, return true
        if (sourceIterator != null && sourceIterator.hasNext()) {
            return true;
        }
        // if sourceiterator is not instanced or has not next
        if (sourceIterator == null || !sourceIterator.hasNext()) {
            closeSourceIterator();
            // if indexIterator hasNext
            if (getIndexIterator().hasNext()) {
                // there are more items in index, get them and rebuild sourceIterator
                nextSourceIteratorInstance();
                return hasNext();
            } else {
                // if no more items in index, return false
                return false;
            }
        }
        return false;
    }

    @Override
    public Feature next() {
        if (hasNext()) {
            sourceFeaturesCounter++;
            return sourceIterator.next();
        } else return null;
    }

    @Override
    public void close() {
        closeSourceIterator();
        closeIndexIterator();
    }
}
