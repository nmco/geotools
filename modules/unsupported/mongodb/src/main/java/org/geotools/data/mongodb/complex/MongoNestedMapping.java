package org.geotools.data.mongodb.complex;

import com.mongodb.DBObject;
import org.geotools.data.complex.NestedAttributeMapping;
import org.geotools.data.complex.filter.XPathUtil;
import org.geotools.data.mongodb.MongoCollectionFeature;
import org.geotools.data.mongodb.MongoFeature;
import org.opengis.feature.Feature;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.helpers.NamespaceSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MongoNestedMapping extends NestedAttributeMapping {

    public MongoNestedMapping(Expression idExpression, Expression parentExpression,
                              XPathUtil.StepList targetXPath, boolean isMultiValued,
                              Map<Name, Expression> clientProperties, Expression sourceElement,
                              XPathUtil.StepList sourcePath, NamespaceSupport namespaces) throws IOException {
        super(idExpression, parentExpression, targetXPath, isMultiValued, clientProperties, sourceElement, sourcePath, namespaces);
    }

    @Override
    public List<Feature> getFeatures(Object source, Object foreignKeyValue, List<Object> idValues, CoordinateReferenceSystem reprojection,
                                     Object feature, List<PropertyName> selectedProperties, boolean includeMandatory, int resolveDepth,
                                     Integer resolveTimeOut) throws IOException {
        if (!(foreignKeyValue instanceof LinkCollectionFunction.LinkCollection)) {
            throw new RuntimeException("MongoDB nesting only supports foreign keys of 'LinkCollection' type.");
        }
        LinkCollectionFunction.LinkCollection linkCollection = (LinkCollectionFunction.LinkCollection) foreignKeyValue;
        List collection = getSubCollection(feature, linkCollection.getCollectionPath());
        if (collection == null) {
            return Collections.emptyList();
        }
        List<Feature> features = new ArrayList<>();
        for (int i = 0; i < collection.size(); i++) {
            features.add(MongoCollectionFeature.build(feature, linkCollection.getCollectionPath(), i));
        }
        return features;
    }

    private List getSubCollection(Object feature, String collectionPath) {
        if (feature instanceof MongoFeature) {
            DBObject mongoObject = ((MongoFeature) feature).getMongoObject();
            return getSubCollection(mongoObject, collectionPath, Collections.emptyMap());
        } else if (feature instanceof MongoCollectionFeature) {
            DBObject mongoObject = ((MongoCollectionFeature) feature).getMongoFeature().getMongoObject();
            return getSubCollection(mongoObject, collectionPath, Collections.emptyMap());
        }
        throw new RuntimeException("MongoDB nesting only works with MongoDB features.");
    }

    private List getSubCollection(DBObject mongoObject, String collectionPath, Map<String, Integer> collectionsIndexes) {
        Object value = MongoComplexUtilities.jsonSelect(mongoObject, collectionsIndexes, collectionPath, false);
        if (value instanceof List) {
            return (List) value;
        }
        throw new RuntimeException("Could not extract collection from path.");
    }
}
