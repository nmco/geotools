package org.geotools.data.complex.spi;

import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.NestedAttributeMapping;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.complex.filter.XPathUtil;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.Expression;
import org.xml.sax.helpers.NamespaceSupport;

import java.util.Map;

public interface CustomAttributeExpressionFactory {

    Expression createNestedAttributeExpression(FeatureTypeMapping mappings, XPathUtil.StepList xpath,
                                               NestedAttributeMapping nestedMapping);
}
