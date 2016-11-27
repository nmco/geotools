package org.geotools.data.mongodb;

import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.NestedAttributeMapping;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.complex.filter.XPathUtil;
import org.geotools.data.complex.spi.CustomAttributeExpressionFactory;
import org.geotools.data.complex.spi.CustomMappingFactory;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.Expression;
import org.xml.sax.helpers.NamespaceSupport;

import java.util.Map;

public class MongoNestedAttributeExpressionFactory implements CustomAttributeExpressionFactory {


    @Override
    public Expression createNestedAttributeExpression(FeatureTypeMapping mappings, XPathUtil.StepList xpath,
                                                      NestedAttributeMapping nestedMapping) {
        return new MongoNestedAttributeExpression(mappings, xpath, nestedMapping);
    }
}
