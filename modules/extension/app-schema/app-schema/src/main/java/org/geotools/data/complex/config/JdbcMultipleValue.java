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
package org.geotools.data.complex.config;

import static org.geotools.data.complex.config.AppSchemaDataAccessConfigurator.parseOgcCqlExpression;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.geotools.data.FeatureSource;
import org.geotools.data.complex.AttributeMapping;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.filter.ComplexFilterSplitter;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.FilterFactoryImplReportInvalidProperty;
import org.geotools.filter.visitor.PostPreProcessFilterSplittingVisitor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.jdbc.JDBCFeatureSource;
import org.opengis.feature.Feature;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;

public final class JdbcMultipleValue extends AttributeExpressionImpl implements MultipleValue {

    private static AtomicInteger ID = new AtomicInteger(0);

    private final FilterFactory filterFactory = new FilterFactoryImplReportInvalidProperty();

    private String sourceColumn;
    private String targetTable;
    private String targetColumn;
    private Expression targetValue;

    private FeatureTypeMapping featureTypeMapping;
    private AttributeMapping attributeMapping;

    private final String id;

    public JdbcMultipleValue() {
        super((String) null);
        id = "mv_" + ID.incrementAndGet();
    }

    public String getSourceColumn() {
        return sourceColumn;
    }

    public void setSourceColumn(String sourceColumn) {
        this.sourceColumn = sourceColumn;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public String getTargetColumn() {
        return targetColumn;
    }

    public void setTargetColumn(String targetColumn) {
        this.targetColumn = targetColumn;
    }

    public Expression getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(String targetValue) {
        try {
            this.targetValue = parseOgcCqlExpression(targetValue, filterFactory);
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format("Error parsing target value expression '%s'.", targetValue),
                    exception);
        }
    }

    public FeatureTypeMapping getFeatureTypeMapping() {
        return featureTypeMapping;
    }

    public void setFeatureTypeMapping(FeatureTypeMapping featureTypeMapping) {
        this.featureTypeMapping = featureTypeMapping;
    }

    public AttributeMapping getAttributeMapping() {
        return attributeMapping;
    }

    public void setAttributeMapping(AttributeMapping attributeMapping) {
        this.attributeMapping = attributeMapping;
    }

    @Override
    public List<Object> getValues(Feature features, AttributeMapping attributeMapping) {
        throw new RuntimeException("Explicitly handled in App-Schema");
    }

    private void validateAttributes() {
        checkNotNull(sourceColumn, "No source column available.");
        checkNotNull(targetTable, "No target table available.");
        checkNotNull(targetColumn, "No target column available.");
        checkNotNull(targetValue, "No target value available.");
    }

    private void validateFeatureTypeMapping() {
        checkNotNull(featureTypeMapping, "No feature type available.");
        FeatureSource dataSource = featureTypeMapping.getSource();
        checkNotNull(dataSource, "No data source available.");
        if (!(dataSource instanceof JDBCFeatureSource)) {
            throw new RuntimeException(
                    "JDBC multiple values can only be used with JDBC data sources.");
        }
    }

    private void checkNotNull(Object value, String errorMessage) {
        if (value == null) {
            throw new RuntimeException(errorMessage);
        }
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
        if (visitor instanceof ComplexFilterSplitter) {
            return targetValue.accept(visitor, extraData);
        }
        if (visitor instanceof PostPreProcessFilterSplittingVisitor) {
            return visitor.visit(this, null);
        }
        if (visitor instanceof SimplifyingFilterVisitor) {
            return this;
        }
        return null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        JdbcMultipleValue that = (JdbcMultipleValue) o;
        return Objects.equals(sourceColumn, that.sourceColumn)
                && Objects.equals(targetTable, that.targetTable)
                && Objects.equals(targetColumn, that.targetColumn)
                && Objects.equals(targetValue, that.targetValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sourceColumn, targetTable, targetColumn, targetValue);
    }

    @Override
    public String toString() {
        return targetValue.toString();
    }
}
