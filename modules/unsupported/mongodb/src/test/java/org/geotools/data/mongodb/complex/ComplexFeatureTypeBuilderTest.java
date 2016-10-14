package org.geotools.data.mongodb.complex;


import org.apache.commons.digester.Digester;
import org.geotools.data.DataUtilities;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.complex.config.AppSchemaDataAccessDTO;
import org.geotools.data.complex.config.AppSchemaFeatureTypeRegistry;
import org.geotools.data.complex.config.EmfComplexFeatureReader;
import org.geotools.data.complex.config.XMLConfigDigester;
import org.geotools.feature.type.ComplexFeatureTypeImpl;
import org.geotools.util.InterpolationProperties;
import org.geotools.xml.SchemaIndex;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.feature.type.FeatureType;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ComplexFeatureTypeBuilderTest {

    @Test
    public void tes() throws Exception {
        XMLConfigDigester digester = new XMLConfigDigester();
        URL dataStoreConfigUrl = this.getClass().getClassLoader().getResource("mappings.xml");
        AppSchemaDataAccessDTO dto = digester.parse(dataStoreConfigUrl);
        AppSchemaDataAccessConfigurator.buildMappings(dto);
        Assert.assertThat(dto, Matchers.notNullValue());
//        final URL dataStoreConfigUrl = this.getClass().getClassLoader().getResource("mappings.xml");
//        InputStream configStream = null;
//        String configString = null;
//        try {
//            configStream = dataStoreConfigUrl.openStream();
//            if (configStream == null) {
//                throw new IOException("Can't open datastore config file " + dataStoreConfigUrl);
//            } else {
//                InterpolationProperties props = new InterpolationProperties();
//                props.putAll(properties);
//                File dataStoreConfigFile = DataUtilities.urlToFile(dataStoreConfigUrl);
//                if (dataStoreConfigFile != null) {
//                    if (props.getProperty(CONFIG_FILE_PROPERTY) == null) {
//                        props.setProperty(CONFIG_FILE_PROPERTY, dataStoreConfigFile.getPath());
//                    }
//                    if (props.getProperty(CONFIG_PARENT_PROPERTY) == null) {
//                        props.setProperty(CONFIG_PARENT_PROPERTY, dataStoreConfigFile.getParent());
//                    }
//                }
//                configString = props.interpolate(InterpolationProperties.readAll(configStream));
//            }
//        } finally {
//            if (configStream != null) {
//                configStream.close();
//            }
//        }
//
//        Digester digester = new Digester();
//        // URL schema = getClass()
//        // .getResource("../test-data/AppSchemaDataAccess.xsd");
//        // digester.setSchema(schema.toExternalForm());
//        digester.setValidating(false);
//        digester.setNamespaceAware(true);
//        digester.setRuleNamespaceURI(XMLConfigDigester.CONFIG_NS_URI);
//
//        // digester.setRuleNamespaceURI(OGC_NS_URI);
//        AppSchemaDataAccessDTO configDto = new AppSchemaDataAccessDTO();
//        configDto.setBaseSchemasUrl(dataStoreConfigUrl.toExternalForm());
//
//        digester.push(configDto);
//
//        try {
//            setNamespacesRules(digester);
//
//            setIncludedTypesRules(digester);
//
//            setSourceDataStoresRules(digester);
//
//            setTargetSchemaUriRules(digester);
//
//            setTypeMappingsRules(digester);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            XMLConfigDigester.LOGGER.log(Level.SEVERE, "setting digester properties: ", e);
//            throw new IOException("Error setting digester properties: " + e.getMessage());
//        }
//
//        try {
//            digester.parse(new StringReader(configString));
//        } catch (SAXException e) {
//            e.printStackTrace();
//            XMLConfigDigester.LOGGER.log(Level.SEVERE, "parsing " + dataStoreConfigUrl, e);
//
//            IOException ioe = new IOException("Can't parse complex datastore config. ");
//            ioe.initCause(e);
//            throw ioe;
//        }
//
//        AppSchemaDataAccessDTO config = (AppSchemaDataAccessDTO) digester.getRoot();
//
//        return config;
    }

    @Test
    public void test2() throws Exception {
        XMLConfigDigester reader = new XMLConfigDigester();
        URL url = this.getClass().getClassLoader().getResource("mappings.xml");
        AppSchemaDataAccessDTO config = reader.parse(url);

        Set mappings = AppSchemaDataAccessConfigurator.buildMappings(config);

        assertNotNull(mappings);
        assertEquals(1, mappings.size());
        FeatureTypeMapping mapping = (FeatureTypeMapping) mappings.iterator().next();
        FeatureType featureType = (FeatureType) mapping.getTargetFeature().getType();

        assertEquals(8, mapping.getAttributeMappings().size());
        assertNotNull(mapping.getTargetFeature());
        assertNotNull(mapping.getSource());
    }


}
