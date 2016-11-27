package org.geotools.data.mongodb;

import org.geotools.data.complex.AttributeMapping;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.NestedAttributeMapping;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.complex.filter.XPathUtil;
import org.geotools.data.complex.spi.CustomAttributeExpressionFactory;
import org.geotools.data.complex.spi.CustomMappingFactory;
import org.geotools.feature.NameImpl;
import org.geotools.filter.NestedAttributeExpression;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.NilExpression;
import org.xml.sax.helpers.NamespaceSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MongoNestedAttributeExpressionFactory implements CustomAttributeExpressionFactory {


    @Override
    public Expression createNestedAttributeExpression(FeatureTypeMapping mappings, XPathUtil.StepList xpath,
                                                      NestedAttributeMapping nestedMapping) {
        return getSourceExpression(mappings, xpath, nestedMapping);
    }

    private Expression getSourceExpression(FeatureTypeMapping mappings, XPathUtil.StepList xpath,
                                           NestedAttributeMapping nestedMapping) {
        int steps = xpath.size();
        AttributeMapping attributeMapping = nestedMapping;
        for (int i = 1; i < steps; i++) {
            try {
                attributeMapping = match(attributeMapping, xpath.get(i));
            } catch (Exception exception) {
                throw new RuntimeException("Error getting feature type mapping.");
            }
            if (attributeMapping == null) {
                break;
            }
        }
        if (attributeMapping == null) {
            return NilExpression.NIL;
        }
        Expression sourceExpression = attributeMapping.getSourceExpression();
        if (sourceExpression instanceof JsonSelectFunction) {
            List<Expression> parameters = new ArrayList<>();
            JsonSelectAllFunction jsonSelect = new JsonSelectAllFunction();
            jsonSelect.setParameters(((JsonSelectFunction)sourceExpression).getParameters());
            return jsonSelect;
        }
        return sourceExpression;
    }

    private AttributeMapping match(AttributeMapping attributeMapping, XPathUtil.Step step) throws IOException {
        if (attributeMapping instanceof NestedAttributeMapping) {
            FeatureTypeMapping mappings = ((NestedAttributeMapping) attributeMapping).getFeatureTypeMapping(null);
            List<AttributeMapping> attributesMappings =  mappings.getAttributeMappings();
            for (AttributeMapping candidateAttributeMapping : attributesMappings) {
                if(XPathUtil.equals(new NameImpl(step.getName()), candidateAttributeMapping.getTargetXPath())) {
                    return candidateAttributeMapping;
                }
            }
        }
        return null;
    }
}
