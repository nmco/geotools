package org.geotools.data.solr;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.geotools.data.DataAccess;
import org.geotools.data.DataAccessFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.complex.config.Types;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.test.OnlineTestCase;
import org.geotools.util.URLs;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * This class contains the integration tests (online tests) for the integration between App-Schema
 * and Apache Solr Create appschema.properties file in {{user-dir}}/.geotools folder Set solr_url
 * property URL config example: solr_url=http://localhost:8983/solr, and create "stations" core in
 * Solr
 */
public final class AppSchemaIntegrationTest extends OnlineTestCase {

    private static final String SOLR_URL_KEY = "solr_url";
    private static final String CORE_NAME = "stations";
    private static final String testData = "/test-data/appschema/";
    private static final String xmlFileName = "mappings_solr.xml";
    private static final String ST_NAMESPACE = "http://www.stations.org/1.0";
    private static final Name mappedTypeName = Types.typeName("multi_stations_solr");
    private static final String testDirStr =
            "target/test/" + AppSchemaIntegrationTest.class.getSimpleName();
    private static final File testDir = new File(testDirStr);

    private static final File appSchemaCacheDir =
            new File(
                    "target/test/"
                            + AppSchemaIntegrationTest.class.getSimpleName()
                            + "/app-schema-cache");

    private HttpSolrClient client;
    private static DataAccess<FeatureType, Feature> mappingDataStore;

    @Test
    public void testFeaturesData() throws Exception {
        FeatureSource<FeatureType, Feature> fSource =
                (FeatureSource<FeatureType, Feature>)
                        mappingDataStore.getFeatureSource(mappedTypeName);
        List<Feature> features = toFeaturesList(fSource.getFeatures());
        // check features count
        assertEquals(2, features.size());
        // check features data
        Name stationName = new NameImpl(ST_NAMESPACE, "stationName");
        for (Feature afeature : features) {
            String id = afeature.getIdentifier().getID();
            assertTrue(id.equals("13") || id.equals("7"));
            // check geom type
            assertTrue(afeature.getDefaultGeometryProperty().getValue() instanceof Point);
            Point theGeom = (Point) afeature.getDefaultGeometryProperty().getValue();
            GeometryFactory gf = new GeometryFactory();
            Point point1;
            switch (id) {
                case "7":
                    assertEquals("Bologna", (String) afeature.getProperty(stationName).getValue());
                    // check geom
                    point1 = gf.createPoint(new Coordinate(11.34, 44.5));
                    assertEquals(point1, theGeom);
                    break;
                case "13":
                    assertEquals(
                            "Alessandria", (String) afeature.getProperty(stationName).getValue());
                    // check geom
                    point1 = gf.createPoint(new Coordinate(8.63, 44.92));
                    assertEquals(point1, theGeom);
                    break;
            }
        }
    }

    private List<Feature> toFeaturesList(FeatureCollection<FeatureType, Feature> features) {
        List<Feature> result = new ArrayList<>();
        FeatureIterator<Feature> i = features.features();
        try {
            while (i.hasNext()) {
                result.add(i.next());
            }
        } finally {
            i.close();
        }
        return result;
    }

    private int size(FeatureCollection<FeatureType, Feature> features) {
        int size = 0;
        FeatureIterator<Feature> i = features.features();
        try {
            for (; i.hasNext(); i.next()) {
                size++;
            }
        } finally {
            i.close();
        }
        return size;
    }

    @Override
    protected void setUpInternal() throws Exception {
        client = new HttpSolrClient.Builder(getSolrCoreURL()).build();
        solrDataSetup();
        prepareFiles();
        setupDataStore();
    }

    protected void setupDataStore() throws Exception {
        final Map dsParams = new HashMap();
        dsParams.put("dbtype", "app-schema");
        final URL url = new URL(new URL("file:"), "./" + testDirStr + "/" + xmlFileName);
        dsParams.put("url", url.toExternalForm());
        mappingDataStore = DataAccessFinder.getDataStore(dsParams);
    }

    protected String getSolrCoreURL() {
        return fixture.getProperty(SOLR_URL_KEY) + "/" + CORE_NAME;
    }

    protected void solrDataSetup() throws Exception {
        typeSetup();
        fieldsSetup();
        indexSetup();
    }

    protected void typeSetup() {
        // create geometry type
        TestsSolrUtils.createGeometryFieldType(client);
    }

    protected void fieldsSetup() throws Exception {
        File inFile =
                new File(
                        AppSchemaIntegrationTest.class
                                .getResource(testData + "solr_types.xml")
                                .toURI());
        JAXBContext jcontext = JAXBContext.newInstance(SolrTypes.class);
        Unmarshaller um = jcontext.createUnmarshaller();
        SolrTypes types = (SolrTypes) um.unmarshal(inFile);
        for (SolrTypeData adata : types.getTypes()) {
            createField(adata.getName(), adata.getType(), adata.getMulti());
        }
    }

    protected void indexSetup() throws Exception {
        File inFile =
                new File(
                        AppSchemaIntegrationTest.class
                                .getResource(testData + "stationsData.xml")
                                .toURI());
        JAXBContext jcontext = JAXBContext.newInstance(Stations.class);
        Unmarshaller um = jcontext.createUnmarshaller();
        Stations stations = (Stations) um.unmarshal(inFile);
        for (StationData adata : stations.getStations()) {
            client.add(adata.toSolrDoc());
        }
        client.commit();
    }

    protected void createField(String name, String type, boolean multiValued) {
        TestsSolrUtils.createField(client, name, type, multiValued);
    }

    protected void prepareFiles() throws Exception {
        // copy meteo.xsd
        copyTestData("meteo.xsd", testDir);
        // stationsData.xml
        copyTestData("stationsData.xml", testDir);

        // Modify datasource and copy xml
        File xmlFile =
                URLs.urlToFile(AppSchemaIntegrationTest.class.getResource(testData + xmlFileName));
        Document doc =
                DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(new InputSource(new FileInputStream(xmlFile)));
        Node solrDs = doc.getElementsByTagName("SolrDataStore").item(0);
        NodeList dsChilds = solrDs.getChildNodes();
        for (int i = 0; i < dsChilds.getLength(); i++) {
            Node achild = dsChilds.item(i);
            if (achild.getNodeName().equals("url")) {
                achild.setTextContent(getSolrCoreURL());
            }
        }
        // write new xml file:
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(testDir.getPath() + "/" + xmlFileName));
        transformer.transform(source, result);

        // create app-schema-cache folder
        appSchemaCacheDir.mkdir();
    }

    private static void copyTestData(String baseFileName, File destDir) throws IOException {
        destDir.mkdirs();
        FileUtils.copyFileToDirectory(
                URLs.urlToFile(AppSchemaIntegrationTest.class.getResource(testData + baseFileName)),
                destDir);
    }

    /** solr.properties file required */
    @Override
    protected String getFixtureId() {
        return "appschema";
    }

    /** Station data model class */
    @XmlRootElement
    public static class StationData implements Serializable {
        private String measurementName;
        private String measurementTime;
        private String measurementUnit;
        private Integer measurementValue;
        private String stationCode;
        private Integer stationId;
        private String stationLocation;
        private String stationName;
        private List<String> stationTag = new ArrayList<>();
        private List<String> stationComment = new ArrayList<>();

        public StationData() {}

        public StationData(
                String measurementName,
                String measurementTime,
                String measurementUnit,
                Integer measurementValue,
                String stationCode,
                Integer stationId,
                String stationLocation,
                String stationName,
                List<String> stationTag,
                List<String> station_comment) {
            super();
            this.measurementName = measurementName;
            this.measurementTime = measurementTime;
            this.measurementUnit = measurementUnit;
            this.measurementValue = measurementValue;
            this.stationCode = stationCode;
            this.stationId = stationId;
            this.stationLocation = stationLocation;
            this.stationName = stationName;
            this.stationTag = stationTag;
            this.stationComment = station_comment;
        }

        public SolrInputDocument toSolrDoc() {
            SolrInputDocument result = new SolrInputDocument();
            result.addField("measurement_name", this.measurementName);
            result.addField("measurement_time", this.measurementTime);
            result.addField("measurement_unit", this.measurementUnit);
            result.addField("measurement_value", this.measurementValue);
            result.addField("station_code", this.stationCode);
            result.addField("station_id", this.stationId);
            result.addField("station_location", this.stationLocation);
            result.addField("station_name", this.stationName);
            if (this.stationTag != null) {
                for (String str1 : this.stationTag) {
                    result.addField("station_tag", str1);
                }
            }
            if (this.stationComment != null) {
                for (String str1 : this.stationComment) {
                    result.addField("station_comment", str1);
                }
            }

            return result;
        }

        public String getMeasurementName() {
            return measurementName;
        }

        public void setMeasurementName(String measurementName) {
            this.measurementName = measurementName;
        }

        public String getMeasurementTime() {
            return measurementTime;
        }

        public void setMeasurementTime(String measurementTime) {
            this.measurementTime = measurementTime;
        }

        public String getMeasurementUnit() {
            return measurementUnit;
        }

        public void setMeasurementUnit(String measurementUnit) {
            this.measurementUnit = measurementUnit;
        }

        public Integer getMeasurementValue() {
            return measurementValue;
        }

        public void setMeasurementValue(Integer measurementValue) {
            this.measurementValue = measurementValue;
        }

        public String getStationCode() {
            return stationCode;
        }

        public void setStationCode(String stationCode) {
            this.stationCode = stationCode;
        }

        public Integer getStationId() {
            return stationId;
        }

        public void setStationId(Integer stationId) {
            this.stationId = stationId;
        }

        public String getStationLocation() {
            return stationLocation;
        }

        public void setStationLocation(String stationLocation) {
            this.stationLocation = stationLocation;
        }

        public String getStationName() {
            return stationName;
        }

        public void setStationName(String stationName) {
            this.stationName = stationName;
        }

        public List<String> getStationTag() {
            return stationTag;
        }

        public void setStationTag(List<String> stationTag) {
            this.stationTag = stationTag;
        }

        public List<String> getStationComment() {
            return stationComment;
        }

        public void setStationComment(List<String> stationComment) {
            this.stationComment = stationComment;
        }
    }

    @XmlRootElement
    public static class Stations implements Serializable {
        private List<StationData> stations = new ArrayList<>();

        public Stations() {}

        public List<StationData> getStations() {
            return stations;
        }

        public void setStations(List<StationData> stations) {
            this.stations = stations;
        }
    }

    @XmlRootElement
    public static class SolrTypes implements Serializable {
        private List<SolrTypeData> types;

        public SolrTypes() {}

        public List<SolrTypeData> getTypes() {
            return types;
        }

        public void setTypes(List<SolrTypeData> types) {
            this.types = types;
        }
    }

    @XmlRootElement
    public static class SolrTypeData implements Serializable {
        private String name;
        private String type;
        private Boolean multi;

        public SolrTypeData() {}

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean getMulti() {
            return multi;
        }

        public void setMulti(Boolean multi) {
            this.multi = multi;
        }
    }
}
