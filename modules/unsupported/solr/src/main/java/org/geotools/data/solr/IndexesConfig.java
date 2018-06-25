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
package org.geotools.data.solr;

import com.vividsolutions.jts.geom.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class IndexesConfig {

    private final ConcurrentHashMap<String, IndexConfig> indexesConfig = new ConcurrentHashMap<>();

    private final List<String> indexesNames = new ArrayList<>();

    public void addGeometry(
            String indexName, String attributeName, String srid, String type, String isDefault) {
        GeometryConfig geometry = GeometryConfig.create(attributeName, srid, type, isDefault);
        getIndexConfig(indexName).addGeometry(geometry);
    }

    public void addAttributes(String indexName, Collection<String> attributes) {
        IndexConfig indexConfig = getIndexConfig(indexName);
        indexConfig.getAttributes().addAll(attributes);
    }

    public IndexConfig getIndexConfig(String indexName) {
        IndexConfig indexConfig = indexesConfig.get(indexName);
        if (indexConfig == null) {
            indexConfig = new IndexConfig(indexName);
            IndexConfig existing = indexesConfig.putIfAbsent(indexName, indexConfig);
            if (existing == null) {
                indexesNames.add(indexName);
            } else {
                indexConfig = existing;
            }
        }
        return indexConfig;
    }

    public SimpleFeatureType buildFeatureType(
            String indexName, List<SolrAttribute> solrAttributes) {
        return getIndexConfig(indexName).buildFeatureType(solrAttributes);
    }

    public List<String> getIndexesNames() {
        return indexesNames;
    }

    public static final class IndexConfig {

        private static final Logger LOGGER = Logging.getLogger(IndexConfig.class);

        private final String indexName;
        private final List<GeometryConfig> geometries = new ArrayList<>();
        private final List<String> attributes = new ArrayList<>();

        public IndexConfig(String indexName) {
            this.indexName = indexName;
        }

        public void addGeometry(GeometryConfig geometry) {
            geometries.add(geometry);
        }

        public void addAttribute(String attributeName) {
            attributes.add(attributeName);
        }

        public List<String> getAttributes() {
            return attributes;
        }

        public GeometryConfig searchGeometry(String attributeName) {
            return search(
                    geometries,
                    geometry -> Objects.equals(geometry.getAttributeName(), attributeName));
        }

        public GeometryConfig searchDefaultGeometry() {
            return search(geometries, GeometryConfig::isDefault);
        }

        public SimpleFeatureType buildFeatureType(List<SolrAttribute> solrAttributes) {
            SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
            featureTypeBuilder.setName(indexName);
            for (String attributeName : attributes) {
                AttributeDescriptor attribute =
                        buildAttributeDescriptor(attributeName, solrAttributes);
                if (attribute == null) {
                    continue;
                }
                featureTypeBuilder.add(attribute);
            }
            GeometryConfig defaultGeometry = searchDefaultGeometry();
            if (defaultGeometry != null) {
                featureTypeBuilder.setDefaultGeometry(defaultGeometry.getAttributeName());
            }
            return featureTypeBuilder.buildFeatureType();
        }

        private AttributeDescriptor buildAttributeDescriptor(
                String attributeName, List<SolrAttribute> solrAttributes) {
            SolrAttribute solrAttribute = searchAttribute(attributeName, solrAttributes);
            if (solrAttribute == null) {
                LOGGER.log(
                        Level.WARNING,
                        String.format(
                                "Could not find attribute '%s' in Solar index '%s' schema.",
                                attributeName, indexName));
                return null;
            }
            AttributeDescriptor attribute =
                    buildAttributeDescriptor(solrAttribute, searchGeometry(attributeName));
            attribute
                    .getUserData()
                    .put(SolrFeatureSource.KEY_SOLR_TYPE, solrAttribute.getSolrType());
            return attribute;
        }

        private AttributeDescriptor buildAttributeDescriptor(
                SolrAttribute solrAttribute, GeometryConfig geometry) {
            AttributeTypeBuilder attributeBuilder = new AttributeTypeBuilder();
            if (geometry == null) {
                attributeBuilder.setName(solrAttribute.getName());
                attributeBuilder.setBinding(solrAttribute.getType());
                return attributeBuilder.buildDescriptor(
                        solrAttribute.getName(), attributeBuilder.buildType());
            }
            attributeBuilder.setCRS(geometry.getCrs());
            attributeBuilder.setName(geometry.getAttributeName());
            attributeBuilder.setBinding(geometry.getType());
            return attributeBuilder.buildDescriptor(
                    geometry.getAttributeName(), attributeBuilder.buildGeometryType());
        }

        private static SolrAttribute searchAttribute(
                String attributeName, List<SolrAttribute> solrAttributes) {
            return search(
                    solrAttributes,
                    solrAttribute -> Objects.equals(solrAttribute.getName(), attributeName));
        }

        private static <T> T search(List<T> objects, Function<T, Boolean> predicate) {
            for (T object : objects) {
                if (predicate.apply(object)) {
                    return object;
                }
            }
            return null;
        }
    }

    public static final class GeometryConfig {

        private final String attributeName;
        private final CoordinateReferenceSystem crs;
        private final Class<? extends Geometry> type;
        private final boolean isDefault;

        public GeometryConfig(
                String attributeName,
                CoordinateReferenceSystem crs,
                Class<? extends Geometry> type,
                boolean isDefault) {
            this.attributeName = attributeName;
            this.crs = crs;
            this.type = type;
            this.isDefault = isDefault;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public CoordinateReferenceSystem getCrs() {
            return crs;
        }

        public Class<? extends Geometry> getType() {
            return type;
        }

        public boolean isDefault() {
            return isDefault;
        }

        public static GeometryConfig create(
                String attributeName, String srid, String type, String isDefault) {
            CoordinateReferenceSystem crs;
            try {
                crs = CRS.decode("EPSG:" + srid);
            } catch (Exception exception) {
                throw new RuntimeException(
                        String.format("Error decoding CRS 'EPSG:%s'.", srid), exception);
            }
            return new GeometryConfig(
                    attributeName, crs, matchGeometryType(type), Boolean.parseBoolean(isDefault));
        }

        private static Class<? extends Geometry> matchGeometryType(String geometryTypeName) {
            switch (geometryTypeName) {
                case "POINT":
                    return Point.class;
                case "LINESTRING":
                    return LineString.class;
                case "POLYGON":
                    return Polygon.class;
                case "MULTIPOINT":
                    return MultiPoint.class;
                case "MULTILINESTRING":
                    return MultiLineString.class;
                case "MULTIPOLYGON":
                    return MultiPolygon.class;
                default:
                    return Geometry.class;
            }
        }
    }
}
