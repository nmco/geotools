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

import com.mongodb.DBObject;
import org.geotools.data.mongodb.MongoGeometryBuilder;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;


public class JsonSelectAllFunction extends FunctionExpressionImpl {

    public static FunctionName NAME = new FunctionNameImpl("jsonSelectAll", parameter("path", String.class));

    public JsonSelectAllFunction() {
        super(NAME);
    }

    public Object evaluate(Object object) {
        //return 1;
        MongoObjectHolder mongoObjectHolder = MongoObjectHolder.extract(object);
        return MongoComplexUtilities.jsonSelect(mongoObjectHolder.getObject(),
                this.params.get(0).toString(), true);
        //return MongoObjectHolder.extract(mongoObjectHolder.getObject(), this.params.get(0).toString());
        //return getValue(mongoObjectHolder.getObject());
    }

    private Object getValue(DBObject mongoObject) {
        try {
            String jsonPath = this.params.get(0).toString();
            if (jsonPath.equals("datex.time")) {
                return new Date();
            }
            if (jsonPath.equals("datex.situationRecord.severity1")) {
                List<Object> situations = new ArrayList<>();
                situations.add(Collections.singleton("HIGHEST"));
                situations.add(Collections.singleton("LOW"));
                return situations;
            }
            if (jsonPath.equals("datex.situationRecord.severity")) {
                return "HIGHEST";
            }
            String[] parts = jsonPath.split("\\.");
            if (parts.length == 1) {
                if (jsonPath.equals("geometry")) {
                    return new MongoGeometryBuilder().toGeometry((DBObject) mongoObject.get(parts[0]));
                }
                if (jsonPath.equals("type")) {
                    return "wind";
                }
                if (jsonPath.equals("value")) {
                    return "135";
                }
                return mongoObject.get(parts[0]);
            }
            Map<Object, Object> values = (Map<Object, Object>) mongoObject.get(parts[0]);
            for (int i = 1; i < parts.length - 1; i++) {
                values = (Map<Object, Object>) values.get(parts[i]);
            }
            return values.get(parts[parts.length - 1]);
        } catch(Exception exception) {
            throw new RuntimeException("Error extracting json value.", exception);
        }
    }

}
