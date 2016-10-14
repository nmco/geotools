package org.geotools.data.mongodb.complex;

import org.geotools.data.DataAccess;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.complex.config.AppSchemaDataAccessDTO;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

import java.io.IOException;
import java.util.Map;
import java.util.Set;


public class FeatureTypeBuilder extends AppSchemaDataAccessConfigurator {


    protected FeatureTypeBuilder(AppSchemaDataAccessDTO config) {
        super(config);
    }

    @Override
    protected Set<FeatureTypeMapping> buildMappings() throws IOException {
        parseGmlSchemas();
        return null;
        /*try {
            parseGmlSchemas();
            Map<String, DataAccess<FeatureType, Feature>> sourceDataStores = null;
            Set<FeatureTypeMapping> featureTypeMappings = null;
            try {
                // -create source datastores
                sourceDataStores = acquireSourceDatastores();
                // -create FeatureType mappings
                featureTypeMappings = createFeatureTypeMappings(sourceDataStores);
                return featureTypeMappings;
            } finally  {
                disposeUnusedSourceDataStores(sourceDataStores, featureTypeMappings);
            }
        } finally {
            if (typeRegistry != null) {
                typeRegistry.disposeSchemaIndexes();
                typeRegistry = null;
            }
        }*/
    }
}
