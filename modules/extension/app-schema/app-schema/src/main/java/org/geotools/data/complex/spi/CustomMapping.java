package org.geotools.data.complex.spi;

import org.geotools.data.complex.NestedAttributeMapping;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.complex.filter.XPathUtil;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.Expression;
import org.xml.sax.helpers.NamespaceSupport;

import java.io.IOException;
import java.util.Map;

public interface CustomMapping {

    NestedAttributeMapping customNestedAttributeMapping(AppSchemaDataAccessConfigurator configuration,
                                                        Expression idExpression, Expression parentExpression,
                                                        XPathUtil.StepList targetXPath, boolean isMultiValued,
                                                        Map<Name, Expression> clientProperties, Expression sourceElement,
                                                        XPathUtil.StepList sourcePath, NamespaceSupport namespaces);
}
