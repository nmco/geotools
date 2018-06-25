/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2018, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.solr.complex;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.digester.Digester;
import org.apache.commons.lang.StringUtils;
import org.geotools.data.DataAccess;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.data.complex.DataAccessMappingFeatureIterator;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.complex.config.AppSchemaDataAccessDTO;
import org.geotools.data.complex.config.AttributeMapping;
import org.geotools.data.complex.config.SourceDataStore;
import org.geotools.data.complex.config.TypeMapping;
import org.geotools.data.complex.config.XMLConfigDigester;
import org.geotools.data.complex.spi.CustomSourceDataStore;
import org.geotools.data.solr.IndexesConfig;
import org.geotools.data.solr.SingleLayerMapper;
import org.geotools.data.solr.SolrDataStore;
import org.geotools.data.solr.SolrFeatureSource;
import org.geotools.filter.FilterFactoryImplReportInvalidProperty;
import org.geotools.filter.expression.AbstractExpressionVisitor;
import org.geotools.util.Converters;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

public class ComplexDataStoreFactory implements CustomSourceDataStore {

    private final FilterFactory filterFactory = new FilterFactoryImplReportInvalidProperty();

    @Override
    public void configXmlDigesterDataStore(Digester digester) {
        XMLConfigDigester.setCommonSourceDataStoreRules(
                ComplexDataStoreConfigWithContext.class, "SolrDataStore", digester);
        String dataStores = "AppSchemaDataAccess/sourceDataStores/";
        // set a rule for passing the URL
        digester.addCallMethod(dataStores + "SolrDataStore/url", "setUrl", 1);
        digester.addCallParam(dataStores + "SolrDataStore/url", 0);
        // set rules for parsing geometries
        digester.addSetProperties(dataStores + "SolrDataStore/index", "name", "currentIndex");
        digester.addCallMethod(dataStores + "SolrDataStore/index/geometry", "addGeometry", 4);
        digester.addCallParam(dataStores + "SolrDataStore/index/geometry/name", 0);
        digester.addCallParam(dataStores + "SolrDataStore/index/geometry/srid", 1);
        digester.addCallParam(dataStores + "SolrDataStore/index/geometry/type", 2);
        digester.addCallParam(dataStores + "SolrDataStore/index/geometry", 3, "default");
    }

    @Override
    public void configXmlDigesterAttributesMappings(Digester digester) {
        String rootPath =
                "AppSchemaDataAccess/typeMappings/FeatureTypeMapping/attributeMappings/AttributeMapping";
        String multipleValuePath = rootPath + "/solrMultipleValue";
        digester.addObjectCreate(
                multipleValuePath, XMLConfigDigester.CONFIG_NS_URI, SolrMultipleValue.class);
        digester.addCallMethod(multipleValuePath, "setExpression", 1);
        digester.addCallParam(multipleValuePath, 0);
        digester.addSetNext(multipleValuePath, "setMultipleValue");
    }

    @Override
    public DataAccessMappingFeatureIterator buildIterator(
            AppSchemaDataAccess store,
            FeatureTypeMapping featureTypeMapping,
            Query query,
            Transaction transaction) {
        if (!(featureTypeMapping.getSource() instanceof SolrFeatureSource)) {
            return null;
        }
        SortBy[] sorting =
                featureTypeMapping
                        .getAttributeMappings()
                        .stream()
                        .filter(
                                mapping ->
                                        mapping.getIdentifierExpression() != null
                                                && !mapping.getIdentifierExpression()
                                                        .equals(Expression.NIL))
                        .map(
                                mapping ->
                                        filterFactory.sort(
                                                mapping.getTargetXPath().toString(),
                                                SortOrder.ASCENDING))
                        .toArray(SortBy[]::new);
        query.setSortBy(sorting);
        try {
            return new DataAccessMappingFeatureIterator(
                    store, featureTypeMapping, query, false, true);
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format(
                            "Error creating iterator for feature type mapping '%s'.",
                            featureTypeMapping.getMappingName()),
                    exception);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public DataAccess<? extends FeatureType, ? extends Feature> buildDataStore(
            SourceDataStore dataStoreConfig, AppSchemaDataAccessDTO appSchemaConfig) {
        if (!(dataStoreConfig instanceof ComplexDataStoreConfig)) {
            return null;
        }
        IndexesConfig indexesConfig = ((ComplexDataStoreConfig) dataStoreConfig).getIndexesConfig();
        for (TypeMapping mapping : (Set<TypeMapping>) appSchemaConfig.getTypeMappings()) {
            indexesConfig.addAttributes(
                    getTypeName(mapping, dataStoreConfig),
                    parseAttributeNames(mapping, dataStoreConfig));
        }
        return new SolrDataStore(
                ((ComplexDataStoreConfig) dataStoreConfig).getUrl(),
                new SingleLayerMapper(),
                indexesConfig);
    }

    private Set<String> extractAttributesNames(Expression expression) {
        AttributesExtractor visitor = new AttributesExtractor();
        expression.accept(visitor, null);
        return visitor.getAttributes();
    }

    public static final class ComplexDataStoreConfigWithContext extends ComplexDataStoreConfig {

        private String currentIndex;

        public void setCurrentIndex(String currentIndex) {
            this.currentIndex = currentIndex;
        }

        public void addGeometry(String attributeName, String srid, String type, String isDefault) {
            super.addGeometry(currentIndex, attributeName, srid, type, isDefault);
        }
    }

    private static class AttributesExtractor extends AbstractExpressionVisitor {

        private final Set<String> attributes = new HashSet<>();

        @Override
        @SuppressWarnings("unchecked")
        public Object visit(PropertyName expression, Object extraData) {
            attributes.add(expression.getPropertyName());
            return expression;
        }

        public Set<String> getAttributes() {
            return attributes;
        }
    }

    private Expression parseExpression(Object value) {
        if (value instanceof Expression) {
            return (Expression) value;
        }
        return parseExpression(Converters.convert(value, String.class));
    }

    private Expression parseExpression(String expressionText) {
        try {
            return AppSchemaDataAccessConfigurator.parseOgcCqlExpression(
                    expressionText, filterFactory);
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format("Error parsing expression '%s'.", expressionText), exception);
        }
    }

    private Set<String> parseIndexModeAttributes(TypeMapping mapping) {
        Set<String> attributes = new HashSet<>();
        ((List<AttributeMapping>) mapping.getAttributeMappings())
                .stream()
                .filter(
                        attributeMapping ->
                                StringUtils.isNotEmpty(attributeMapping.getIndexField()))
                .forEach(
                        attributeMapping -> {
                            attributes.add(attributeMapping.getIndexField());
                        });
        return attributes;
    }

    private Set<String> parseSourceModeAttributes(TypeMapping mapping) {
        Set<String> attributes = new HashSet<>();
        ((List<AttributeMapping>) mapping.getAttributeMappings())
                .forEach(
                        attributeMapping -> {
                            // mapped attributes
                            Expression expression =
                                    parseExpression(attributeMapping.getSourceExpression());
                            attributes.addAll(extractAttributesNames(expression));
                            if (attributeMapping.getMultipleValue() instanceof SolrMultipleValue) {
                                expression =
                                        ((SolrMultipleValue) attributeMapping.getMultipleValue())
                                                .getExpression();
                                attributes.addAll(extractAttributesNames(expression));
                            }
                            // mapped identifiers
                            expression =
                                    parseExpression(attributeMapping.getIdentifierExpression());
                            attributes.addAll(extractAttributesNames(expression));
                            // mapped client properties
                            attributes.addAll(
                                    (Set<String>)
                                            attributeMapping
                                                    .getClientProperties()
                                                    .values()
                                                    .stream()
                                                    .flatMap(
                                                            value ->
                                                                    extractAttributesNames(
                                                                                    parseExpression(
                                                                                            value))
                                                                            .stream())
                                                    .collect(Collectors.toSet()));
                        });
        return attributes;
    }

    private Set<String> parseAttributeNames(TypeMapping mapping, SourceDataStore dataStoreConfig) {
        // if mapping index points to dataStore: is index use case
        if (dataStoreConfig.getId().equals(mapping.getIndexDataStore())) {
            return parseIndexModeAttributes(mapping);
        }
        return parseSourceModeAttributes(mapping);
    }

    private String getTypeName(TypeMapping mapping, SourceDataStore dataStoreConfig) {
        if (dataStoreConfig.getId().equals(mapping.getIndexDataStore())) {
            return mapping.getIndexTypeName();
        }
        return mapping.getSourceTypeName();
    }
}
