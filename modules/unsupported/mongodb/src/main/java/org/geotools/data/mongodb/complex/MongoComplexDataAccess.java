package org.geotools.data.mongodb.complex;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.data.ServiceInfo;
import org.geotools.data.mongodb.MongoSchemaDBStore;
import org.geotools.data.mongodb.MongoSchemaFileStore;
import org.geotools.data.mongodb.MongoSchemaStore;
import org.geotools.feature.type.ComplexFeatureTypeImpl;
import org.geotools.filter.FilterCapabilities;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;


public class MongoComplexDataAccess implements DataAccess {

//    final MongoSchemaStore schemaStore;
//
//    final MongoClient dataStoreClient;
//    final DB dataStoreDB;
//
//    @SuppressWarnings("deprecation")
//    FilterCapabilities filterCapabilities;

//    public MongoComplexDataAccess(String dataStoreURI, String schemaStoreURI, boolean createDatabaseIfNeeded) {
//
//        MongoClientURI dataStoreClientURI = createMongoClientURI(dataStoreURI);
//        dataStoreClient = createMongoClient(dataStoreClientURI);
//        dataStoreDB = createDB(dataStoreClient, dataStoreClientURI.getDatabase(), !createDatabaseIfNeeded);
//        if (dataStoreDB == null) {
//            dataStoreClient.close(); // This smells bad...
//            throw new IllegalArgumentException("Unknown mongodb database, \"" + dataStoreClientURI.getDatabase() + "\"");
//        }
//
//        schemaStore = createSchemaStore(schemaStoreURI);
//        if (schemaStore == null) {
//            dataStoreClient.close(); // This smells bad too...
//            throw new IllegalArgumentException("Unable to initialize schema store with URI \"" + schemaStoreURI + "\"");
//        }
//
//        //filterCapabilities = createFilterCapabilties();
//    }
//
//    final MongoClientURI createMongoClientURI(String dataStoreURI) {
//        if (dataStoreURI == null) {
//            throw new IllegalArgumentException("dataStoreURI may not be null");
//        }
//        if (!dataStoreURI.startsWith("mongodb://")) {
//            throw new IllegalArgumentException("incorrect scheme for URI, expected to begin with \"mongodb://\", found URI of \"" + dataStoreURI + "\"");
//        }
//        return new MongoClientURI(dataStoreURI.toString());
//    }
//
//    final MongoClient createMongoClient(MongoClientURI mongoClientURI) {
//        try {
//            return new MongoClient(mongoClientURI);
//        } catch (Exception e) {
//            throw new IllegalArgumentException("Unknown mongodb host(s)", e);
//        }
//    }
//
//    final DB createDB(MongoClient mongoClient, String databaseName, boolean databaseMustExist) {
//        if (databaseMustExist && !mongoClient.getDatabaseNames().contains(databaseName)) {
//            return null;
//        }
//        return mongoClient.getDB(databaseName);
//    }
//
//    private MongoSchemaStore createSchemaStore(String schemaStoreURI) {
//        if (schemaStoreURI.startsWith("file:")) {
//            try {
//                return new MongoSchemaFileStore(schemaStoreURI);
//            } catch (URISyntaxException e) {
//                //LOGGER.log(Level.SEVERE, "Unable to create file-based schema store with URI \"" + schemaStoreURI + "\"", e);
//            } catch (IOException e) {
//                //LOGGER.log(Level.SEVERE, "Unable to create file-based schema store with URI \"" + schemaStoreURI + "\"", e);
//            }
//        } else if (schemaStoreURI.startsWith("mongodb:")) {
//            try {
//                return new MongoSchemaDBStore(schemaStoreURI);
//            } catch (IOException e) {
//                //LOGGER.log(Level.SEVERE, "Unable to create mongodb-based schema store with URI \"" + schemaStoreURI + "\"", e);
//            }
//        }
//        //LOGGER.log(Level.SEVERE, "Unsupported URI \"{0}\" for schema store", schemaStoreURI);
//        return null;
//    }



    @Override
    public ServiceInfo getInfo() {
        return null;
    }

    @Override
    public void createSchema(FeatureType featureType) throws IOException {

    }

    @Override
    public void updateSchema(Name typeName, FeatureType featureType) throws IOException {

    }

    @Override
    public void removeSchema(Name typeName) throws IOException {

    }

    @Override
    public List<Name> getNames() throws IOException {
        return null;
    }

    @Override
    public FeatureType getSchema(Name name) throws IOException {
        // ComplexFeatureTypeImpl type = new ComplexFeatureTypeImpl(Name name);
        return null;
    }

    @Override
    public FeatureSource getFeatureSource(Name typeName) throws IOException {
        return null;
    }

    @Override
    public void dispose() {

    }
}
