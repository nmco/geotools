package org.geotools.data.complex.spi;

import org.geotools.data.complex.NestedAttributeMapping;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.complex.filter.XPathUtil;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.Expression;
import org.xml.sax.helpers.NamespaceSupport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public final class CustomMappingFinder {

    private static List<CustomMapping> mappings = new ArrayList<>();

    static {
        initCustomNestedAttributeMappingsFactories();
    }

    private CustomMappingFinder() {
    }

    private static void initCustomNestedAttributeMappingsFactories() {
        ServiceLoader<CustomMapping> mappingsLoader = ServiceLoader.load(CustomMapping.class);
        mappingsLoader.reload();
        Iterator<CustomMapping> mappingsIterator = mappingsLoader.iterator();
        while (mappingsIterator.hasNext()) {
            mappings.add(mappingsIterator.next());
        }
    }

    public static NestedAttributeMapping find(AppSchemaDataAccessConfigurator configuration,
                                              Expression idExpression, Expression parentExpression,
                                              XPathUtil.StepList targetXPath, boolean isMultiValued,
                                              Map<Name, Expression> clientProperties, Expression sourceElement,
                                              XPathUtil.StepList sourcePath, NamespaceSupport namespaces) {
        for (CustomMapping factory : mappings) {
            NestedAttributeMapping mapping = factory.customNestedAttributeMapping(configuration, idExpression,
                    parentExpression, targetXPath, isMultiValued, clientProperties,
                    sourceElement, sourcePath, namespaces);
            if (mapping != null) {
                return mapping;
            }
        }
        return null;
    }
}
