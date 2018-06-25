package org.geotools.data.complex.filter;

import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.IndexedMappingFeatureIterator;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.expression.PropertyName;

public class IndexUnmappingVisitor extends DuplicatingFilterVisitor {

    private FeatureTypeMapping mapping;

    public IndexUnmappingVisitor(FeatureTypeMapping mapping) {
        super();
        this.mapping = mapping;
    }

    @Override
    public Object visit(PropertyName expression, Object extraData) {
        return IndexedMappingFeatureIterator.unrollIndex(expression, mapping);
    }
}
