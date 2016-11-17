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
import java.util.Collection;
import java.util.NoSuchElementException;

import org.geotools.data.mongodb.complex.CollectionInfoHolder;
import org.geotools.data.mongodb.complex.MongoComplexUtilities;
import org.geotools.data.mongodb.complex.MongoObjectHolder;
import org.geotools.data.simple.SimpleFeatureReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

public class MongoFeatureReader implements SimpleFeatureReader {

    DBCursor cursor;
    MongoFeatureSource featureSource;
    CollectionMapper mapper;
    CollectionInfoHolder collectionInfoHolder;

    private DBObject currentObject;
    private int subCollectionSize;
    private int subCollectionIndex;

    public MongoFeatureReader(DBCursor cursor, MongoFeatureSource featureSource) {
        this(cursor, featureSource, new CollectionInfoHolder());
    }

    public MongoFeatureReader(DBCursor cursor, MongoFeatureSource featureSource, CollectionInfoHolder collectionInfoHolder) {
        this.cursor = cursor;
        this.featureSource = featureSource;
        mapper = featureSource.getMapper();
        this.collectionInfoHolder = collectionInfoHolder;
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        return featureSource.getSchema();
    }

    @Override
    public boolean hasNext() throws IOException {
        if (!collectionInfoHolder.containsCollectionInfo()) {
            return cursor.hasNext();
        }
        if (currentObject != null) {
            return true;
        }
        if (!cursor.hasNext()) {
            return false;
        }
        currentObject = cursor.next();
        Collection subCollection = (Collection) MongoComplexUtilities.jsonSelect(currentObject, collectionInfoHolder.getObjectsIds(), collectionInfoHolder.getCollectionPath());
        if (subCollection == null || subCollection.isEmpty()) {
            return false;
        }
        subCollectionIndex = 0;
        subCollectionSize = subCollection.size();
        return true;
    }

    @Override
    public SimpleFeature next() throws IOException, IllegalArgumentException, NoSuchElementException {
        if (!collectionInfoHolder.containsCollectionInfo()) {
            DBObject obj = cursor.next();
            return mapper.buildFeature(new MongoObjectHolder(obj, collectionInfoHolder), featureSource.getSchema());
        }
        if (currentObject == null) {
            currentObject = cursor.next();
            Collection subCollection = (Collection) MongoComplexUtilities.jsonSelect(currentObject, collectionInfoHolder.getObjectsIds(), collectionInfoHolder.getCollectionPath());
            if (subCollection == null || subCollection.isEmpty()) {
                return null;
            }
            subCollectionIndex = 0;
            subCollectionSize = subCollection.size();
        }
        SimpleFeature feature = mapper.buildFeature(
                new MongoObjectHolder(currentObject, collectionInfoHolder, subCollectionIndex), featureSource.getSchema());
        subCollectionIndex++;
        if (subCollectionSize == subCollectionIndex) {
            currentObject = null;
        }
        return feature;
    }

    @Override
    public void close() throws IOException {
        cursor.close();
    }

}
