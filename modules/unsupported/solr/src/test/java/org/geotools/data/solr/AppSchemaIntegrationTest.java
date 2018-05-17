package org.geotools.data.solr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * This class contains the integration tests (online tests) for the integration between App-Schema and Apache Solr
 * Create appschema.properties file in {{user-dir}}/.geotools folder
 * Set solr_url property
 * URL config example:
 * solr_url=http://localhost:8983/solr
 * @author Fernando Miño
 */
public final class AppSchemaIntegrationTest extends OnlineTestCase {

    private static final String SOLR_URL_KEY = "solr_url";
    private static final String CORE_NAME = "stations";
    private static final String testData = "/test-data/appschema/";
    private static final String xmlFileName = "mappings_solr.xml";
    private static final Name mappedTypeName = Types.typeName("multi_stations_solr");
    private static final String testDirStr =
            "target/test/" + AppSchemaIntegrationTest.class.getSimpleName();
    private static final File testDir = new File(testDirStr);

    private static final File appSchemaCacheDir =
            new File(
                    "target/test/"
                            + AppSchemaIntegrationTest.class.getSimpleName()
                            + "/app-schema-cache");

    protected HttpSolrClient client;
    private static DataAccess<FeatureType, Feature> mappingDataStore;

    @Test
    public void testFeaturesCount() throws Exception {
        FeatureSource<FeatureType, Feature> fSource =
                (FeatureSource<FeatureType, Feature>)
                        mappingDataStore.getFeatureSource(mappedTypeName);
        assertEquals(2, size(fSource.getFeatures()));
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

    protected void fieldsSetup() {
        createField("station_id", "tlongs", false);
        createField("station_name", "strings", false);
        createField("station_code", "strings", false);
        createField("station_location", "geometry", false);
        createField("measurement_name", "strings", false);
        createField("measurement_time", "tdates", false);
        createField("measurement_unit", "strings", false);
        createField("measurement_value", "tlongs", false);
        createField("station_tag", "strings", true);
        createField("station_comment", "strings", true);
    }

    protected void indexSetup() throws Exception {
        StationData d1 =
                new StationData(
                        "temperature",
                        "2016-12-19T11:26:40",
                        "C",
                        20,
                        "ALS",
                        13,
                        "POINT(8.63 44.92)",
                        "Alessandria",
                        Arrays.asList(new String[] {"ALS_TAG_1", "ALS_TAG_2"}),
                        Arrays.asList(new String[] {"ALS_COMMENT_1", "ALS_COMMENT_2"}));
        client.add(d1.toSolrDoc());

        StationData d2 =
                new StationData(
                        "temperature",
                        "2016-12-19T11:27:13",
                        "Km/h",
                        155,
                        "ALS",
                        13,
                        "POINT(8.63 44.92)",
                        "Alessandria",
                        Arrays.asList(new String[] {"ALS_TAG_1", "ALS_TAG_2"}),
                        Arrays.asList(new String[] {"ALS_COMMENT_1", "ALS_COMMENT_2"}));
        client.add(d2.toSolrDoc());

        StationData d3 =
                new StationData(
                        "temperature",
                        "2016-12-19T11:28:31",
                        "C",
                        35,
                        "BOL",
                        7,
                        "POINT(11.34 44.5)",
                        "Bologna",
                        null,
                        null);
        client.add(d3.toSolrDoc());

        StationData d4 =
                new StationData(
                        "temperature",
                        "2016-12-19T11:28:55",
                        "C",
                        25,
                        "BOL",
                        7,
                        "POINT(11.34 44.5)",
                        "Bologna",
                        null,
                        null);
        client.add(d4.toSolrDoc());

        StationData d5 =
                new StationData(
                        "wind speed",
                        "2016-12-19T11:29:24",
                        "C",
                        80,
                        "BOL",
                        7,
                        "POINT(11.34 44.5)",
                        "Bologna",
                        null,
                        null);
        client.add(d5.toSolrDoc());

        StationData d6 =
                new StationData(
                        "pressure",
                        "2016-12-19T11:30:26",
                        "hPa",
                        1019,
                        "BOL",
                        7,
                        "POINT(11.34 44.5)",
                        "Bologna",
                        null,
                        null);
        client.add(d6.toSolrDoc());

        StationData d7 =
                new StationData(
                        "pressure",
                        "2016-12-19T11:30:51",
                        "hPa",
                        1015,
                        "BOL",
                        7,
                        "POINT(11.34 44.5)",
                        "Bologna",
                        null,
                        null);
        client.add(d7.toSolrDoc());

        client.commit();
    }

    protected void createField(String name, String type, boolean multiValued) {
        TestsSolrUtils.createField(client, name, type, multiValued);
    }

    protected void prepareFiles() throws Exception {
        // copy meteo.xsd
        copyTestData("meteo.xsd", testDir);

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
    protected static class StationData implements Serializable {
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
    }
}
