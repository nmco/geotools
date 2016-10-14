package org.geotools.data.mongodb.complex;


import org.geotools.data.complex.config.FeatureTypeRegistry;
import org.geotools.feature.type.ComplexFeatureTypeFactoryImpl;
import org.geotools.gml3.complex.GmlFeatureTypeRegistryConfiguration;
import org.xml.sax.helpers.NamespaceSupport;

public class MongoFeatureTypeRegistry extends FeatureTypeRegistry {
   

    public MongoFeatureTypeRegistry() {
        this(null);
    }

    public MongoFeatureTypeRegistry(NamespaceSupport namespaces) {
        super(new ComplexFeatureTypeFactoryImpl(), 
                new GmlFeatureTypeRegistryConfiguration(namespaces==null? null : namespaces.getURI("gml")) );
    }
    

}
