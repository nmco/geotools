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

import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geotools.data.mongodb.complex.MongoObjectHolder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.AttributeDescriptorImpl;
import org.geotools.feature.type.AttributeTypeImpl;
import org.geotools.util.Converters;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;

/**
 * Maps a collection containing valid GeoJSON.
 * 
 * @author tkunicki@boundlessgeo.com
 */
public abstract class AbstractCollectionMapper implements CollectionMapper {

    private final boolean isComplex;

    public AbstractCollectionMapper(boolean isComplex) {
        this.isComplex = isComplex;
    }

    @Override
    public SimpleFeature buildFeature(MongoObjectHolder mongoObjectHolder, SimpleFeatureType featureType) {

        String gdLocalName = featureType.getGeometryDescriptor().getLocalName();
        List<AttributeDescriptor> adList = featureType.getAttributeDescriptors();

        List<Object> values = new ArrayList<Object>(adList.size());
        for (AttributeDescriptor descriptor : adList) {
            String adLocalName = descriptor.getLocalName();
            if (adLocalName.equals(MongoObjectHolder.ATTRIBUTE_KEY)) {
                values.add(mongoObjectHolder);
            }
            else if (gdLocalName.equals(adLocalName)) {
                values.add(getGeometry(mongoObjectHolder.getObject()));
            } else {
                String path = getPropertyPath(adLocalName);
                Object o = path == null ? null : MongoUtil.getDBOValue(mongoObjectHolder.getObject(), path);
                values.add(o == null ? null : Converters.convert(o, descriptor.getType()
                        .getBinding()));
            }
        }
        if (!this.isComplex) {
            return new MongoFeature(values.toArray(), featureType, mongoObjectHolder.getObject().get("_id").toString());
        }
        return new MongoFeature(values.toArray(), featureType, mongoObjectHolder.getObject().get("_id").toString(), mongoObjectHolder.getObject());
    }
}
