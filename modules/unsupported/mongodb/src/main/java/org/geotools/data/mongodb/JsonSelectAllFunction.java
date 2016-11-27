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

package org.geotools.data.mongodb;

import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;


public class JsonSelectAllFunction extends FunctionExpressionImpl {

    public static FunctionName DEFINITION = new FunctionNameImpl(
            "jsonSelectAll", parameter("path", String.class));

    public JsonSelectAllFunction() {
        super(DEFINITION);
    }

    public Object evaluate(Object object) {
        String path = (String) this.params.get(0).evaluate(object);
        if (object instanceof MongoFeature) {
            MongoFeature mongoFeature = (MongoFeature) object;
            return MongoComplexUtilities.jsonSelect(mongoFeature.getMongoObject(), path, true);
        }
        if (object instanceof MongoCollectionFeature) {
            MongoCollectionFeature collectionFeature = (MongoCollectionFeature) object;
            return MongoComplexUtilities.jsonSelect(collectionFeature.getMongoFeature().getMongoObject(),
                    collectionFeature.getCollectionsIndexes(), path, true);
        }
        throw new RuntimeException(String.format(
                "Function 'jsonSelectAll' cannot be applied to object of type '%s'.",
                object.getClass().getCanonicalName()));
    }

}
