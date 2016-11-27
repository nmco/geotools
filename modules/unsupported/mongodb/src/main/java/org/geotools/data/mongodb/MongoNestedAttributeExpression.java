package org.geotools.data.mongodb;

import com.mongodb.DBObject;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.MappingFeatureCollection;
import org.geotools.data.complex.MappingFeatureSource;
import org.geotools.data.complex.NestedAttributeMapping;
import org.geotools.data.complex.filter.XPathUtil;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.helpers.NamespaceSupport;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MongoNestedAttributeExpression implements Expression {

    private final FeatureTypeMapping mappings;
    private final XPathUtil.StepList xpath;
    private final NestedAttributeMapping nestedMapping;

    public MongoNestedAttributeExpression(FeatureTypeMapping mappings, XPathUtil.StepList xpath,
                                          NestedAttributeMapping nestedMapping) {
        this.mappings = mappings;
        this.xpath = xpath;
        this.nestedMapping = nestedMapping;
    }


    @Override
    public Object evaluate(Object object) {
        return null;
    }

    @Override
    public <T> T evaluate(Object object, Class<T> context) {
        return null;
    }

    @Override
    public Object accept(ExpressionVisitor visitor, Object extraData) {
        return null;
    }
}
