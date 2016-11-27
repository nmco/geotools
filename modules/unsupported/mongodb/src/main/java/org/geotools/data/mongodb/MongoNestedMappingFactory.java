package org.geotools.data.mongodb;

import org.geotools.data.complex.NestedAttributeMapping;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.complex.filter.XPathUtil;
import org.geotools.data.complex.spi.CustomMapping;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.Expression;
import org.xml.sax.helpers.NamespaceSupport;

import java.util.Map;

public class MongoNestedMappingFactory implements CustomMapping {

    @Override
    public NestedAttributeMapping customNestedAttributeMapping(AppSchemaDataAccessConfigurator configuration, Expression idExpression,
                                                               Expression parentExpression, XPathUtil.StepList targetXPath, boolean isMultiValued,
                                                               Map<Name, Expression> clientProperties, Expression sourceElement,
                                                               XPathUtil.StepList sourcePath, NamespaceSupport namespaces) {
        try {
            // FIXME: check datsource type is mongodb
            return new MongoNestedMapping(idExpression, parentExpression, targetXPath,
                    isMultiValued, clientProperties, sourceElement, sourcePath, namespaces);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
