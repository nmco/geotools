package org.geotools.data.complex.spi;

import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.NestedAttributeMapping;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.complex.filter.XPathUtil;
import org.geotools.filter.NestedAttributeExpression;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.Expression;
import org.xml.sax.helpers.NamespaceSupport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public final class CustomImplementationsFinder {

    private static List<CustomMappingFactory> mappingsFactories = new ArrayList<>();
    private static List<CustomAttributeExpressionFactory> attributesFactories = new ArrayList<>();

    static {
        mappingsFactories = initFactories(CustomMappingFactory.class);
        attributesFactories = initFactories(CustomAttributeExpressionFactory.class);
    }

    private CustomImplementationsFinder() {
    }

    private static <T> List<T> initFactories(Class<T> type) {
        ServiceLoader<T> loader = ServiceLoader.load(type);
        loader.reload();
        List<T> factories = new ArrayList<>();
        Iterator<T> factoriesIterator = loader.iterator();
        while (factoriesIterator.hasNext()) {
            factories.add(factoriesIterator.next());
        }
        return factories;
    }

    public static NestedAttributeMapping find(AppSchemaDataAccessConfigurator configuration,
                                              Expression idExpression, Expression parentExpression,
                                              XPathUtil.StepList targetXPath, boolean isMultiValued,
                                              Map<Name, Expression> clientProperties, Expression sourceElement,
                                              XPathUtil.StepList sourcePath, NamespaceSupport namespaces) {
        for (CustomMappingFactory factory : mappingsFactories) {
            NestedAttributeMapping mapping = factory.createNestedAttributeMapping(configuration, idExpression,
                    parentExpression, targetXPath, isMultiValued, clientProperties,
                    sourceElement, sourcePath, namespaces);
            if (mapping != null) {
                return mapping;
            }
        }
        return null;
    }

    public static Expression find(FeatureTypeMapping mappings, XPathUtil.StepList xpath,
                                  NestedAttributeMapping nestedMapping) {
        for (CustomAttributeExpressionFactory factory : attributesFactories) {
            Expression attributeExpression = factory.createNestedAttributeExpression(mappings, xpath, nestedMapping);
            if (attributeExpression != null) {
                return attributeExpression;
            }
        }
        return null;
    }
}
