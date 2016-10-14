package org.geotools.data.mongodb.complex;

import org.geotools.data.DataAccess;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.DataStoreFactorySpi;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

import java.awt.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * Created by nuno on 10/8/16.
 */
public class MongoComplexDataAcessFactory implements DataAccessFactory {

    @Override
    public DataAccess<? extends FeatureType, ? extends Feature> createDataStore(Map<String, Serializable> params) throws IOException {
        return new MongoComplexDataAccess();
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Param[] getParametersInfo() {
        return new Param[0];
    }

    @Override
    public boolean canProcess(Map<String, Serializable> params) {
        return params.values().contains("mongo");
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Map<RenderingHints.Key, ?> getImplementationHints() {
        return null;
    }
}
