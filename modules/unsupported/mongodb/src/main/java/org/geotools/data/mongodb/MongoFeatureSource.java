/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2015, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2014-2015, Boundless
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
package org.geotools.data.mongodb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.org.apache.xerces.internal.dom.AttributeMap;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.FilteringFeatureReader;
import org.geotools.data.Query;
import org.geotools.data.ReTypeFeatureReader;
import org.geotools.data.complex.AttributeMapping;
import org.geotools.data.complex.NestedAttributeMapping;
import org.geotools.data.complex.filter.XPathUtil;
import org.geotools.data.mongodb.complex.CollectionIdFunction;
import org.geotools.data.mongodb.complex.CollectionInfoHolder;
import org.geotools.data.mongodb.complex.JsonSelectAllFunction;
import org.geotools.data.mongodb.complex.JsonSelectFunction;
import org.geotools.data.mongodb.complex.MongoComplexUtilities;
import org.geotools.data.mongodb.complex.CollectionLinkFunction;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.IsEqualsToImpl;
import org.geotools.filter.LiteralExpressionImpl;
import org.geotools.filter.NestedAttributeExpression;
import org.geotools.filter.visitor.AbstractFilterVisitor;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.filter.visitor.PostPreProcessFilterSplittingVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNil;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.NilExpression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class MongoFeatureSource extends ContentFeatureSource {

    static Logger LOG = Logging.getLogger("org.geotools.data.mongodb");

    final DBCollection collection;

    CollectionMapper mapper;

    private final boolean isComplex;

    public MongoFeatureSource(ContentEntry entry, Query query, DBCollection collection, boolean isComplex) {
        super(entry, query);
        this.isComplex = isComplex;
        this.collection = collection;
        initMapper(isComplex);
    }

    final void initMapper(boolean isComplex) {
        // use schema with mapping info if it exists
        SimpleFeatureType type = entry.getState(null).getFeatureType();
        setMapper(type != null ? new MongoSchemaMapper(type, isComplex) : new MongoInferredMapper(isComplex));
    }

    public DBCollection getCollection() {
        return collection;
    }

    public CollectionMapper getMapper() {
        return mapper;
    }

    public void setMapper(CollectionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected SimpleFeatureType buildFeatureType() throws IOException {
        SimpleFeatureType type = mapper.buildFeatureType(entry.getName(), collection);
        getDataStore().getSchemaStore().storeSchema(type);
        return type;
    }

    @Override
    public MongoDataStore getDataStore() {
        return (MongoDataStore) super.getDataStore();
    }

    @Override
    protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        // TODO: crs?
        FeatureReader<SimpleFeatureType, SimpleFeature> r = getReader(query);
        try {
            ReferencedEnvelope e = new ReferencedEnvelope();
            if (r.hasNext()) {
                e.init(r.next().getBounds());
            }
            while (r.hasNext()) {
                e.include(r.next().getBounds());
            }
            return e;
        } finally {
            r.close();
        }
    }

    @Override
    protected int getCountInternal(Query query) throws IOException {
        Filter f = query.getFilter();
        if (isAll(f)) {
            LOG.fine("count(all)");
            return (int) collection.count();
        }

        Filter[] split = splitFilter(f);
        if (!isAll(split[1])) {
            return -1;
        }

        DBObject q = toQuery(f);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("count(" + q + ")");
        }
        return (int) collection.count(q);
    }

    private static class SubCollectionFilterVisitor extends DuplicatingFilterVisitor {

        @Override
        protected Expression visit(Expression expression, Object extraData) {
            return getSourceExpression(expression);
        }

        @Override
        public Object visit(PropertyName expression, Object extraData) {
            return super.visit(expression, extraData);
        }

        private Expression getSourceExpression(Expression expression) {
            if (!(expression instanceof NestedAttributeExpression)) {
                return expression;
            }
            NestedAttributeExpression nestedAttributeExpression = (NestedAttributeExpression) expression;
            NestedAttributeMapping nestedAttributeMapping = nestedAttributeExpression.getRootMapping();
            XPathUtil.StepList stepList = nestedAttributeExpression.getFullSteps();
            int steps = stepList.size();
            AttributeMapping attributeMapping = nestedAttributeMapping;
            for (int i = 1; i < steps; i++) {
                attributeMapping = match(attributeMapping, stepList.get(i));
                if (attributeMapping == null) {
                    break;
                }
            }
            if (attributeMapping == null) {
                return NilExpression.NIL;
            }
            Expression sourceExpression = attributeMapping.getSourceExpression();
            if (sourceExpression instanceof JsonSelectFunction) {
                List<Expression> parameters = new ArrayList<>();
                JsonSelectAllFunction jsonSelect = new JsonSelectAllFunction();
                jsonSelect.setParameters(((JsonSelectFunction)sourceExpression).getParameters());
                return jsonSelect;
            }
            return sourceExpression;
        }

        private AttributeMapping match(AttributeMapping attributeMapping, XPathUtil.Step step) {
            if (attributeMapping instanceof NestedAttributeMapping) {
                List<AttributeMapping> attributesMappings =  ((NestedAttributeMapping) attributeMapping).getAttributesMappings();
                for (AttributeMapping candidateAttributeMapping : attributesMappings) {
                    if(XPathUtil.equals(new NameImpl(step.getName()), candidateAttributeMapping.getTargetXPath())) {
                        return candidateAttributeMapping;
                    }
                }
            }
            return null;
        }

        @Override
        public Object visit(PropertyIsEqualTo filter, Object data) {
            Expression expression1 = filter.getExpression1();
            Expression expression2 = filter.getExpression2();
            if (expression1 instanceof CollectionLinkFunction) {
                CollectionLinkFunction link = (CollectionLinkFunction) expression1;
                String nodeId = filter.getExpression2().toString();
                Map<String, String> objectsIds = MongoComplexUtilities.extractCollectionsIndexes(nodeId);
                CollectionInfoHolder collectionInfoHolder = (CollectionInfoHolder) data;
                collectionInfoHolder.setContainsCollectionInfo(true);
                collectionInfoHolder.setCollectionPath(link.getParameters().get(0).toString());
                collectionInfoHolder.setObjectsIds(objectsIds);
                collectionInfoHolder.setNodeId(nodeId);
                String rootId = objectsIds.get("");
                //return buildIdCompare(link.getParameters().get(1), rootId);
            }
            if (expression1 instanceof CollectionIdFunction) {
                CollectionIdFunction link = (CollectionIdFunction) expression1;
                String nodeId = filter.getExpression2().toString();
                Map<String, String> objectsIds = MongoComplexUtilities.extractCollectionsIndexes(nodeId);
                CollectionInfoHolder collectionInfoHolder = (CollectionInfoHolder) data;
                collectionInfoHolder.setContainsCollectionInfo(false);
                collectionInfoHolder.setCollectionPath(link.getParameters().get(0).toString());
                collectionInfoHolder.setObjectsIds(objectsIds);
                collectionInfoHolder.setNodeId(nodeId);
                String rootId = objectsIds.get("");
                //return buildIdCompare(link.getParameters().get(1), rootId);
            }
            return ff.equals(getSourceExpression(expression1), getSourceExpression(expression2));
        }

        @Override
        public Object visit(PropertyIsBetween filter, Object extraData) {
            Expression expression1 = filter.getExpression();
            Expression expression2 = filter.getLowerBoundary();
            Expression expression3 = filter.getUpperBoundary();
            return ff.between(getSourceExpression(expression1), getSourceExpression(expression2), getSourceExpression(expression2));
        }

        @Override
        public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
            Expression expression1 = filter.getExpression1();
            Expression expression2 = filter.getExpression2();
            return ff.notEqual(getSourceExpression(expression1), getSourceExpression(expression2));
        }

        @Override
        public Object visit(PropertyIsGreaterThan filter, Object extraData) {
            Expression expression1 = filter.getExpression1();
            Expression expression2 = filter.getExpression2();
            return ff.greater(getSourceExpression(expression1), getSourceExpression(expression2));
        }

        @Override
        public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
            Expression expression1 = filter.getExpression1();
            Expression expression2 = filter.getExpression2();
            return ff.greaterOrEqual(getSourceExpression(expression1), getSourceExpression(expression2));
        }

        @Override
        public Object visit(PropertyIsLessThan filter, Object extraData) {
            return super.visit(filter, extraData);
        }

        @Override
        public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
            return super.visit(filter, extraData);
        }

        @Override
        public Object visit(PropertyIsLike filter, Object extraData) {
            return super.visit(filter, extraData);
        }

        @Override
        public Object visit(PropertyIsNull filter, Object extraData) {
            return super.visit(filter, extraData);
        }

        @Override
        public Object visit(PropertyIsNil filter, Object extraData) {
            return super.visit(filter, extraData);
        }

        private PropertyIsEqualTo buildIdCompare(Expression rootIdPath, String expectedId) {
            JsonSelectFunction idSelectExpression = new JsonSelectFunction();
            idSelectExpression.setParameters(Collections.singletonList(rootIdPath));
            IsEqualsToImpl comparison = new IsEqualsToImpl();
            comparison.setExpression1(idSelectExpression);
            comparison.setExpression2(new LiteralExpressionImpl(expectedId));
            return comparison;
        }
    }


    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query)
            throws IOException {

        SubCollectionFilterVisitor visitor = new SubCollectionFilterVisitor();
        CollectionInfoHolder collectionInfoHolder = new CollectionInfoHolder();
        Filter newFilter = (Filter) query.getFilter().accept(visitor, collectionInfoHolder);
        query.setFilter(newFilter);

        Map<String, String> objectsIds = collectionInfoHolder.getObjectsIds();
        if (objectsIds == null) {
            objectsIds = new HashMap<>();
            collectionInfoHolder.setObjectsIds(objectsIds);
        }
        objectsIds.put("QUERY.FILTER", query.getFilter().toString());

        List<Filter> postFilterList = new ArrayList<Filter>();
        List<String> postFilterAttributes = new ArrayList<String>();
        DBCursor cursor = toCursor(query, postFilterList, postFilterAttributes);
        FeatureReader<SimpleFeatureType, SimpleFeature> r = new MongoFeatureReader(cursor, this, collectionInfoHolder);

        if (!postFilterList.isEmpty() && !isAll(postFilterList.get(0))) {
            r = new FilteringFeatureReader<SimpleFeatureType, SimpleFeature>(r, postFilterList.get(0));

            // check whether attributes not present in the original query have been
            // added to the set of retrieved attributes for the sake of
            // post-filters; if so, wrap the FeatureReader in a ReTypeFeatureReader
            if (!postFilterAttributes.isEmpty()) {
                // build the feature type returned by this query
                SimpleFeatureType returnedSchema = SimpleFeatureTypeBuilder.retype(
                        getSchema(), query.getPropertyNames());
                r = new ReTypeFeatureReader(r, returnedSchema);
            }
        }
        return r;
    }

    @Override
    protected boolean canOffset() {
        return true;
    }

    @Override
    protected boolean canLimit() {
        return true;
    }

    @Override
    protected boolean canRetype() {
        return true;
    }

    @Override
    protected boolean canSort() {
        return true;
    }

    @Override
    protected boolean canFilter() {
        return true;
    }

    DBCursor toCursor(Query q, List<Filter> postFilter, List<String> postFilterAttrs) {
        DBObject query = new BasicDBObject();

        Filter f = q.getFilter();
        if (!isAll(f)) {
            Filter[] split = splitFilter(f);
            query = toQuery(split[0]);
            if (!isAll(split[1])) {
                postFilter.add(split[1]);
            }
        }

        DBCursor c;
        if (q.getPropertyNames() != Query.ALL_NAMES) {
            BasicDBObject keys = new BasicDBObject();
            for (String p : q.getPropertyNames()) {
                keys.put(mapper.getPropertyPath(p), 1);
            }
            // add properties from post filters
            for (Filter filter: postFilter) {
                String[] attributeNames = DataUtilities.attributeNames(filter);
                for (String attrName: attributeNames) {
                    if (attrName != null && !attrName.isEmpty() && !keys.containsField(attrName)) {
                        keys.put(mapper.getPropertyPath(attrName), 1);
                        postFilterAttrs.add(attrName);
                    }
                }
            }
            if (!keys.containsField(mapper.getGeometryPath())) {
                keys.put(mapper.getGeometryPath(), 1);
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine(String.format("find(%s, %s)", query, keys));
            }
            c = collection.find(query, keys);
        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine(String.format("find(%s)", query));
            }
            c = collection.find(query);
        }

        if (q.getStartIndex() != null && q.getStartIndex() != 0) {
            c = c.skip(q.getStartIndex());
        }
        if (q.getMaxFeatures() != Integer.MAX_VALUE) {
            c = c.limit(q.getMaxFeatures());
        }

        if (q.getSortBy() != null) {
            BasicDBObject orderBy = new BasicDBObject();
            for (SortBy sortBy : q.getSortBy()) {
                String propName = sortBy.getPropertyName().getPropertyName();
                orderBy.append(propName, sortBy.getSortOrder() == SortOrder.ASCENDING ? 1 : -1);
            }
            c = c.sort(orderBy);
        }

        return c;
    }

    DBObject toQuery(Filter f) {
        if (isAll(f)) {
            return new BasicDBObject();
        }

        FilterToMongo v = new FilterToMongo(mapper);
        v.setFeatureType(getSchema());

        return (DBObject) f.accept(v, null);
    }

    boolean isAll(Filter f) {
        return f == null || f == Filter.INCLUDE;
    }

    @SuppressWarnings("deprecation")
    Filter[] splitFilter(Filter f) {
        PostPreProcessFilterSplittingVisitor splitter = new PostPreProcessFilterSplittingVisitor(
                getDataStore().getFilterCapabilities(), null, null){
            public Object visit(PropertyIsLike filter, Object notUsed) {
                if (original == null)
                    original = filter;

                if (!fcs.supports(PropertyIsLike.class)) {
                    // MongoDB can only encode like expressions using propertyName
                    postStack.push(filter);
                    return null;
                }
                if(!(filter.getExpression() instanceof PropertyName)){
                    // MongoDB can only encode like expressions using propertyName
                    postStack.push(filter);
                    return null;
                }
                
                int i = postStack.size();
                filter.getExpression().accept(this, null);

                if (i < postStack.size()) {
                    postStack.pop();
                    postStack.push(filter);

                    return null;
                }

                preStack.pop(); // value
                preStack.push(filter);
                return null;
            }
        };
        f.accept(splitter, null);
        return new Filter[] { splitter.getFilterPre(), splitter.getFilterPost() };
    }

}
