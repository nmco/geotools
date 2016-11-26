package org.geotools.data.mongodb.complex;

import com.mongodb.DBObject;
import org.geotools.data.mongodb.MongoGeometryBuilder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.feature.type.AttributeTypeImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MongoObjectHolder {

    public static final String ATTRIBUTE_KEY = "343f474a-a077-11e6-80f5-76304dec7eb7-MONGO_OBJECT_HOLDER";

    private final DBObject object;
    private final CollectionInfoHolder collectionInfoHolder;
    private final int subCollectionIndex;

    public MongoObjectHolder(DBObject object, CollectionInfoHolder collectionInfoHolder) {
        this.object = object;
        this.subCollectionIndex = 0;
        this.collectionInfoHolder = collectionInfoHolder;
    }

    public MongoObjectHolder(DBObject object, CollectionInfoHolder collectionInfoHolder, int subCollectionIndex) {
        this.object = object;
        this.collectionInfoHolder = collectionInfoHolder;
        this.subCollectionIndex = subCollectionIndex;
    }

    public DBObject getObject() {
        return object;
    }

    public int getSubCollectionIndex() {
        return subCollectionIndex;
    }

    public CollectionInfoHolder getCollectionInfoHolder() {
        return collectionInfoHolder;
    }

    public static void addAttributeDescriptor(SimpleFeatureTypeBuilder featureBuilder) {
        AttributeType attribute = new AttributeTypeImpl(
                new NameImpl(ATTRIBUTE_KEY), Object.class, false, false, Collections.EMPTY_LIST, null, null);
        AttributeDescriptor attributeDescriptor = new AttributeDescriptorImpl(
                attribute, new NameImpl(ATTRIBUTE_KEY), 0, 1, false, null);
        featureBuilder.add(attributeDescriptor);
    }

    public static MongoObjectHolder extract(Object object) {
        if (!(object instanceof SimpleFeature)) {
            throw new RuntimeException("Cannot extract mongo object holder from non simple feature object.");
        }
        SimpleFeature simpleFeature = (SimpleFeature) object;
        return (MongoObjectHolder) simpleFeature.getAttribute(ATTRIBUTE_KEY);
    }
}
