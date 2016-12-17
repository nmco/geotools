package org.geotools.data.mongodb;

import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.NestedAttributeMapping;
import org.geotools.data.complex.filter.XPathUtil;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;

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
