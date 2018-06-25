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

import java.net.URL;
import java.util.List;
import org.geotools.data.complex.config.SourceDataStore;
import org.geotools.data.solr.IndexesConfig;

public class ComplexDataStoreConfig extends SourceDataStore {

    private final IndexesConfig indexesConfig = new IndexesConfig();

    private URL url;

    public void addGeometry(
            String indexName, String attributeName, String srid, String type, String isDefault) {
        indexesConfig.addGeometry(indexName, attributeName, srid, type, isDefault);
    }

    public void addAttributes(String indexName, List<String> attributes) {
        indexesConfig.addAttributes(indexName, attributes);
    }

    public IndexesConfig getIndexesConfig() {
        return indexesConfig;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(String url) {
        try {
            this.url = new URL(url);
        } catch (Exception exception) {
            throw new RuntimeException(String.format("Error parsing URL '%s'.", url), exception);
        }
    }
}
