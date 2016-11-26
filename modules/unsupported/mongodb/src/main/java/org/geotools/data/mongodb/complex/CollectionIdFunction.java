/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2007-2011, Open Source Geospatial Foundation (OSGeo)
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

package org.geotools.data.mongodb.complex;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import org.geotools.data.mongodb.MongoFeature;
import org.geotools.data.mongodb.MongoGeometryBuilder;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;


public class CollectionIdFunction extends FunctionExpressionImpl {

    public static FunctionName NAME = new FunctionNameImpl(
            "collectionId",
            parameter("value", Object.class),
            parameter("collectionPath", String.class),
            parameter("valuePath", String.class));

    public CollectionIdFunction() {
        super(NAME);
    }

    public Object evaluate(Object object) {
        MongoObjectHolder mongoObjectHolder = MongoObjectHolder.extract(object);
        if (this.params.get(0).toString().equals(mongoObjectHolder.getCollectionInfoHolder().getCollectionPath()) && mongoObjectHolder.getCollectionInfoHolder().containsCollectionInfo()) {
           // return mongoObjectHolder.getCollectionInfoHolder().getNodeId();
        }
        return MongoComplexUtilities.computeNodeId(mongoObjectHolder, (String)
                MongoComplexUtilities.jsonSelect(mongoObjectHolder.getObject(), this.params.get(1).toString(), false));
        //return 1;
        /*MongoObjectHolder mongoObjectHolder = MongoObjectHolder.extract(object);
        DBObject mongoObject = mongoObjectHolder.getObject();
        String collectionPath = this.params.get(0).toString();
        String valuePath = this.params.get(0).toString();
        if (valuePath != null && valuePath.charAt(0) == '/') {
            return MongoObjectHolder.extract(mongoObject, valuePath);
        }
        BasicDBList collection = (BasicDBList) MongoObjectHolder.extract(mongoObject, collectionPath);
        DBObject o2 = (DBObject) collection.get(mongoObjectHolder.getSubCollectionIndex());
        return MongoObjectHolder.extract(o2, valuePath);*/
    }
}
