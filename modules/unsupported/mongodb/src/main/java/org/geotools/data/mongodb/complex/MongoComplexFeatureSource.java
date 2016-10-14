package org.geotools.data.mongodb.complex;


import org.geotools.data.DataAccess;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

import java.awt.*;
import java.io.IOException;
import java.util.Set;

public class MongoComplexFeatureSource implements FeatureSource<FeatureType, Feature> {

    @Override
    public Name getName() {
        return null;
    }

    @Override
    public ResourceInfo getInfo() {
        return null;
    }

    @Override
    public DataAccess<FeatureType, Feature> getDataStore() {
        return null;
    }

    @Override
    public QueryCapabilities getQueryCapabilities() {
        return null;
    }

    @Override
    public void addFeatureListener(FeatureListener listener) {

    }

    @Override
    public void removeFeatureListener(FeatureListener listener) {

    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures(Filter filter) throws IOException {
        return null;
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures(Query query) throws IOException {
        return null;
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures() throws IOException {
        return null;
    }

    @Override
    public FeatureType getSchema() {
        return null;
    }

    @Override
    public ReferencedEnvelope getBounds() throws IOException {
        return null;
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        return null;
    }

    @Override
    public int getCount(Query query) throws IOException {
        return 0;
    }

    @Override
    public Set<RenderingHints.Key> getSupportedHints() {
        return null;
    }
}
