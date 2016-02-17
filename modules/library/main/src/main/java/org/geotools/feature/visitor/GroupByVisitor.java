/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2016, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.feature.visitor;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.geotools.feature.DefaultFeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.expression.Expression;
import org.opengis.util.ProgressListener;

import java.util.*;

/**
 * Group features by one or several attributes and applies an aggregator visitor to each group.
 */
public class GroupByVisitor implements FeatureCalc {

    private final Aggregate aggregateVisitor;
    private final Expression aggregateAttribute;
    private final FeatureCalc aggregateVisitorProtoType;
    private final List<Expression> groupByAttributes;
    private final ProgressListener progressListener;

    private final InMemoryGroupBy inMemoryGroupBy = new InMemoryGroupBy();

    private CalcResult optimizationResult = CalcResult.NULL_RESULT;

    public GroupByVisitor(Aggregate aggregateVisitor, Expression aggregateAttribute,
                          List<Expression> groupByAttributes, ProgressListener progressListener) {
        this.aggregateVisitor = aggregateVisitor;
        this.aggregateAttribute = aggregateAttribute;
        this.groupByAttributes = groupByAttributes;
        this.progressListener = progressListener;
        aggregateVisitorProtoType = aggregateVisitor.create(aggregateAttribute);
    }

    public boolean wasOptimized() {
        return optimizationResult != null;
    }

    public boolean wasVisited() {
        return !inMemoryGroupBy.groupByIndexes.isEmpty();
    }

    /**
     * This method computes and returns the group by visitor result. If the computation was optimized
     * the optimization result is returned otherwise the result is computed in memory. If for some
     * reason an optimization result exists and there are visited features, an in memory computation
     * is performed and is merged with the existing optimization results.
     *
     * @return group by visitor result
     */
    @Override
    public CalcResult getResult() {
        // do a in memory computation for any visited feature
        Map<List<Object>, CalcResult> results = inMemoryGroupBy.visit();
        // create the result, if no feature was visited this will be an empty result that can be safely merged
        GroupByResult result = new GroupByResult(results, aggregateVisitor, groupByAttributes);
        if (optimizationResult == CalcResult.NULL_RESULT) {
            // there is no optimization result so we just return the created one
            return result;
        }
        // an optimization result exists, we merge both
        return optimizationResult.merge(result);
    }

    @Override
    public void visit(Feature feature) {
        inMemoryGroupBy.index((SimpleFeature) feature);
    }

    public Expression getExpression() {
        return aggregateAttribute;
    }

    public FeatureVisitor getAggregateVisitor() {
        return aggregateVisitorProtoType;
    }

    public List<Expression> getGroupByAttributes() {
        return groupByAttributes;
    }

    /**
     * Methods that allow optimizations to directly set the group by visitor result instead
     * of computing it visiting all the features. Aggregate visitor results are wrapped with
     * the appropriate feature calculation type.
     *
     * @param value the group by visitor result
     */
    public void setValue(List<GroupByRawResult> value) {
        Map<List<Object>, CalcResult> results = new HashMap<>();
        for (GroupByRawResult groupByRawResult : value) {
            // wrap the aggregate visitor result with the appropriate feature calculation type
            results.put(groupByRawResult.groupByValues, aggregateVisitor.wrap(aggregateAttribute, groupByRawResult.visitorValue));
        }
        // create a new group by result using the raw values returned by the optimization
        GroupByResult newResult = new GroupByResult(results, aggregateVisitor, groupByAttributes);
        if (optimizationResult == CalcResult.NULL_RESULT) {
            // if no current result we simply return the new one
            optimizationResult = newResult;
        } else {
            // if a result already exists we merge it with the new one
            optimizationResult = optimizationResult.merge(newResult);
        }
    }

    /**
     * Helper class that should be used by optimizations to set the results.
     */
    public static class GroupByRawResult {

        final List<Object> groupByValues;
        final Object visitorValue;

        public GroupByRawResult(List<Object> groupByValues, Object visitorsValue) {
            this.groupByValues = groupByValues;
            this.visitorValue = visitorsValue;
        }
    }

    /**
     * Helper class that do the computations for the group by visitor in memory.
     */
    private class InMemoryGroupBy {

        // feature collections grouped by the group by attributes
        private final Map<List<Object>, DefaultFeatureCollection> groupByIndexes = new HashMap<>();

        /**
         * Add a feature to the appropriate group by feature collection.
         *
         * @param feature the feature to be indexed
         */
        void index(SimpleFeature feature) {
            // list of group by attributes values
            List<Object> groupByValues = new ArrayList<>();
            for (Expression expression : groupByAttributes) {
                groupByValues.add(expression.evaluate(feature));
            }
            // check if a feature collection already for the group by values
            DefaultFeatureCollection featureCollection = groupByIndexes.get(groupByValues);
            if (featureCollection == null) {
                // we create a feature collection for the group by values
                featureCollection = new DefaultFeatureCollection();
                groupByIndexes.put(groupByValues, featureCollection);
            }
            featureCollection.add(feature);
        }

        /**
         * We apply a copy of the aggregation visitor to each feature collection.
         *
         * @return the result of applying the aggregation visitor to eac feature collection
         */
        Map<List<Object>, CalcResult> visit() {
            Map<List<Object>, CalcResult> results = new HashMap<>();
            for (Map.Entry<List<Object>, DefaultFeatureCollection> entry : groupByIndexes.entrySet()) {
                // creating a new aggregation visitor for the current feature collection
                FeatureCalc visitor = aggregateVisitor.create(aggregateAttribute);
                try {
                    // visiting the feature collection with the aggregation visitor
                    entry.getValue().accepts(visitor, progressListener);
                } catch (Exception exception) {
                    throw new RuntimeException("Error visiting features collections.", exception);
                }
                // we add the aggregation visitor to the results
                results.put(entry.getKey(), visitor.getResult());
            }
            return results;
        }
    }

    /**
     * This class implements the feature calculation result of the group by visitor.
     */
    public static class GroupByResult implements CalcResult {

        private final Map<List<Object>, CalcResult> results;
        private final Aggregate aggregateVisitor;
        private final List<Expression> groupByAttributes;

        public GroupByResult(Map<List<Object>, CalcResult> results, Aggregate aggregateVisitor, List<Expression> groupByAttributes) {
            this.results = results;
            this.aggregateVisitor = aggregateVisitor;
            this.groupByAttributes = groupByAttributes;
        }

        public Map<List<Object>, CalcResult> getResults() {
            return results;
        }

        public Aggregate getAggregateVisitor() {
            return aggregateVisitor;
        }

        public List<Expression> getGroupByAttributes() {
            return groupByAttributes;
        }

        @Override
        public boolean isCompatible(CalcResult newResult) {
            if (newResult == CalcResult.NULL_RESULT) {
                // compatible with NULL result
                return true;
            }
            if (!(newResult instanceof GroupByResult)) {
                // not compatible with results that are not group by results
                return false;
            }
            GroupByResult groupByResult = (GroupByResult) newResult;
            // compatible only if the aggregation visitor is the same and the group by attributes are the same
            return aggregateVisitor == groupByResult.getAggregateVisitor()
                    && groupByAttributes.equals(groupByResult.getGroupByAttributes());
        }

        @Override
        public CalcResult merge(CalcResult newResult) {
            if (!isCompatible(newResult)) {
                // not compatible results
                throw new IllegalArgumentException(String.format(
                        "Feature calculation result '%s' is not compatible it this result '%s'.",
                        newResult.getClass().getSimpleName(), GroupByResult.class.getSimpleName()));
            }
            if (newResult == CalcResult.NULL_RESULT) {
                // if the new result is a NULL result we simply return a copy of this result
                return new GroupByResult(results, aggregateVisitor, groupByAttributes);
            }
            // the merged results are initialized with the content of this result
            Map<List<Object>, CalcResult> mergedResults = new HashMap<>(results);
            for (Map.Entry<List<Object>, CalcResult> entry : ((GroupByResult) newResult).getResults().entrySet()) {
                // check if this result contains the same aggregation result
                CalcResult existingResult = mergedResults.get(entry.getKey());
                if (existingResult != null) {
                    // the aggregation result exist in both results so we merge them
                    mergedResults.put(entry.getKey(), existingResult.merge(entry.getValue()));
                } else {
                    // the aggregation result only exists in the new result
                    mergedResults.put(entry.getKey(), entry.getValue());
                }
            }
            // we return a new group by result with the merged values
            return new GroupByResult(mergedResults, aggregateVisitor, groupByAttributes);
        }

        @Override
        public Object getValue() {
            return toArray();
        }

        @Override
        public int toInt() {
            return 0;
        }

        @Override
        public double toDouble() {
            return 0;
        }

        @Override
        public String toString() {
            return null;
        }

        @Override
        public long toLong() {
            return 0;
        }

        @Override
        public float toFloat() {
            return 0;
        }

        @Override
        public Geometry toGeometry() {
            return null;
        }

        @Override
        public Envelope toEnvelope() {
            return null;
        }

        @Override
        public Point toPoint() {
            return null;
        }

        @Override
        public Set toSet() {
            Set<Object[]> set = new HashSet<>();
            for (Map.Entry<List<Object>, CalcResult> result : results.entrySet()) {
                set.add(entryToArray(result));
            }
            return set;
        }

        @Override
        public List toList() {
            List<Object[]> list = new ArrayList<>();
            for (Map.Entry<List<Object>, CalcResult> result : results.entrySet()) {
                list.add(entryToArray(result));
            }
            return list;
        }

        @Override
        public Object[] toArray() {
            Object[][] array = new Object[results.size()][];
            int index = 0;
            for (Map.Entry<List<Object>, CalcResult> result : results.entrySet()) {
                array[index] = entryToArray(result);
                index++;
            }
            return array;
        }

        /**
         * The keys of the map will be List instead of arrays, since arrays don't give a decent hash code.
         */
        @Override
        public Map toMap() {
            Map<List<Object>, Object[]> map = new HashMap<>();
            for (Map.Entry<List<Object>, CalcResult> result : results.entrySet()) {
                map.put(result.getKey(), entryToArray(result));
            }
            return map;
        }

        private Object[] entryToArray(Map.Entry<List<Object>, CalcResult> entry) {
            Object[] result = Arrays.copyOf(entry.getKey().toArray(), entry.getKey().size() + 1);
            result[entry.getKey().size()] = entry.getValue().getValue();
            return result;
        }
    }
}
