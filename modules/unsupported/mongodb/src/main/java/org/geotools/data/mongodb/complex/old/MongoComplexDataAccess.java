package org.geotools.data.mongodb.complex.old;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.data.ServiceInfo;
import org.geotools.data.mongodb.MongoSchemaDBStore;
import org.geotools.data.mongodb.MongoSchemaFileStore;
import org.geotools.data.mongodb.MongoSchemaStore;
import org.geotools.filter.FilterCapabilities;
import org.opengis.feature.type.Name;
import org.opengis.filter.And;
import org.opengis.filter.Id;
import org.opengis.filter.Not;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Within;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MongoComplexDataAccess implements DataAccess<MongoComplexFeatureType, MongoComplexFeature> {

    final MongoClient dataStoreClient;
    final DB dataStoreDB;
    final MongoSchemaStore schemaStore;
    FilterCapabilities filterCapabilities;

    public MongoComplexDataAccess(String dataStoreURI, String schemaStoreURI) {

        MongoClientURI dataStoreClientURI = createMongoClientURI(dataStoreURI);
        dataStoreClient = createMongoClient(dataStoreClientURI);
        dataStoreDB = createDB(dataStoreClient, dataStoreClientURI.getDatabase(), true);
        schemaStore = createSchemaStore(schemaStoreURI);
        if (schemaStore == null) {
            dataStoreClient.close(); // This smells bad too...
            throw new IllegalArgumentException("Unable to initialize schema store with URI \"" + schemaStoreURI + "\"");
        }

        filterCapabilities = createFilterCapabilties();
    }

    final FilterCapabilities createFilterCapabilties() {
        FilterCapabilities capabilities = new FilterCapabilities();

        /* disable FilterCapabilities.LOGICAL_OPENGIS since it contains
            Or.class (in addtions to And.class and Not.class.  MongodB 2.4
            doesn't supprt '$or' with spatial operations.
        */
//        capabilities.addAll(FilterCapabilities.LOGICAL_OPENGIS);
        capabilities.addType(And.class);
        capabilities.addType(Not.class);

        capabilities.addAll(FilterCapabilities.SIMPLE_COMPARISONS_OPENGIS);
        capabilities.addType(PropertyIsNull.class);
        capabilities.addType(PropertyIsBetween.class);
        capabilities.addType(PropertyIsLike.class);

        capabilities.addType(BBOX.class);
        capabilities.addType(Intersects.class);
        capabilities.addType(Within.class);

        capabilities.addType(Id.class);

        /*
        capabilities.addType(IncludeFilter.class);
        capabilities.addType(ExcludeFilter.class);

        //temporal filters
        capabilities.addType(After.class);
        capabilities.addType(Before.class);
        capabilities.addType(Begins.class);
        capabilities.addType(BegunBy.class);
        capabilities.addType(During.class);
        capabilities.addType(Ends.class);
        capabilities.addType(EndedBy.class);*/

        return capabilities;
    }

    private MongoSchemaStore createSchemaStore(String schemaStoreURI) {
        try {
            if (schemaStoreURI.startsWith("file:")) {
                return new MongoSchemaFileStore(schemaStoreURI);
            } else if (schemaStoreURI.startsWith("mongodb:")) {
                return new MongoSchemaDBStore(schemaStoreURI);
            } else {
                return new MongoSchemaFileStore("file:" + schemaStoreURI);
            }
        } catch(Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    final MongoClientURI createMongoClientURI(String dataStoreURI) {
        if (dataStoreURI == null) {
            throw new IllegalArgumentException("dataStoreURI may not be null");
        }
        if (!dataStoreURI.startsWith("mongodb://")) {
            throw new IllegalArgumentException("incorrect scheme for URI, expected to begin with \"mongodb://\", found URI of \"" + dataStoreURI + "\"");
        }
        return new MongoClientURI(dataStoreURI.toString());
    }

    final MongoClient createMongoClient(MongoClientURI mongoClientURI) {
        try {
            return new MongoClient(mongoClientURI);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown mongodb host(s)", e);
        }
    }

    final DB createDB(MongoClient mongoClient, String databaseName, boolean databaseMustExist) {
        if (databaseMustExist && !mongoClient.getDatabaseNames().contains(databaseName)) {
            return null;
        }
        return mongoClient.getDB(databaseName);
    }

    @Override
    public ServiceInfo getInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createSchema(MongoComplexFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateSchema(Name typeName, MongoComplexFeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeSchema(Name typeName) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Name> getNames() throws IOException {
        return Collections.emptyList();
    }

    @Override
    public MongoComplexFeatureType getSchema(Name name) throws IOException {
        return new MongoComplexFeatureType();
    }

    @Override
    public FeatureSource<MongoComplexFeatureType, MongoComplexFeature> getFeatureSource(Name typeName) throws IOException {
        return new MongoComplexFeatureSource(dataStoreDB.getCollection("datex"));
    }

    @Override
    public void dispose() {
    }
}
