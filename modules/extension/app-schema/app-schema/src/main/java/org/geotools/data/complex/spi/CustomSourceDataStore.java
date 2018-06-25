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
package org.geotools.data.complex.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.apache.commons.digester.Digester;
import org.geotools.data.DataAccess;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.data.complex.DataAccessMappingFeatureIterator;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.config.AppSchemaDataAccessDTO;
import org.geotools.data.complex.config.SourceDataStore;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

public interface CustomSourceDataStore {

    DataAccess<? extends FeatureType, ? extends Feature> buildDataStore(
            SourceDataStore dataStoreConfig, AppSchemaDataAccessDTO appSchemaConfig);

    void configXmlDigesterDataStore(Digester digester);

    void configXmlDigesterAttributesMappings(Digester digester);

    DataAccessMappingFeatureIterator buildIterator(
            AppSchemaDataAccess store,
            FeatureTypeMapping featureTypeMapping,
            Query query,
            Transaction transaction);

    static List<CustomSourceDataStore> loadExtensions() {
        ServiceLoader<CustomSourceDataStore> loader =
                ServiceLoader.load(CustomSourceDataStore.class);
        loader.reload();
        List<CustomSourceDataStore> extensions = new ArrayList<>();
        for (CustomSourceDataStore extension : loader) {
            extensions.add(extension);
        }
        return extensions;
    }

    static void applyDataStores(List<CustomSourceDataStore> extensions, Digester digester) {
        extensions.forEach(extension -> extension.configXmlDigesterDataStore(digester));
    }

    static void applyAttributesMappings(List<CustomSourceDataStore> extensions, Digester digester) {
        extensions.forEach(extension -> extension.configXmlDigesterAttributesMappings(digester));
    }

    @SuppressWarnings("unchecked")
    static DataAccess<FeatureType, Feature> apply(
            List<CustomSourceDataStore> extensions,
            SourceDataStore dataStoreConfig,
            AppSchemaDataAccessDTO appSchemaConfig) {
        for (CustomSourceDataStore extension : extensions) {
            DataAccess<? extends FeatureType, ? extends Feature> dataStore =
                    extension.buildDataStore(dataStoreConfig, appSchemaConfig);
            if (dataStore != null) {
                return (DataAccess<FeatureType, Feature>) dataStore;
            }
        }
        return null;
    }
}
