package org.geotools.data.postgis;

import org.geotools.jdbc.JDBCGroupByVisitorTestSetup;
import org.geotools.jdbc.JDBCTestSetup;

public class PostgisGroupByVisitorTestSetup extends JDBCGroupByVisitorTestSetup {

    public PostgisGroupByVisitorTestSetup(JDBCTestSetup delegate) {
        super(delegate);
    }

    @Override
    protected void createBuildingsTable() throws Exception {
        run("CREATE TABLE BUILDINGS_GROUP_BY_TESTS (id int4 PRIMARY KEY, building_id text, " +
                "building_type text, energy_type text, energy_consumption numeric, position geometry);");
        run("INSERT INTO BUILDINGS_GROUP_BY_TESTS VALUES " +
                "(1, 'SCHOOL_A', 'SCHOOL', 'FLOWING_WATER', 50.0, ST_GeomFromText('POINT(-5 -5)', 4326))," +
                "(2, 'SCHOOL_A', 'SCHOOL', 'NUCLEAR', 10.0, ST_GeomFromText('POINT(-5 -5)', 4326))," +
                "(3, 'SCHOOL_A', 'SCHOOL', 'WIND', 20.0, ST_GeomFromText('POINT(-5 -5)', 4326))," +
                "(4, 'SCHOOL_B', 'SCHOOL', 'SOLAR', 30.0, ST_GeomFromText('POINT(5 5)', 4326))," +
                "(5, 'SCHOOL_B', 'SCHOOL', 'FUEL', 60.0, ST_GeomFromText('POINT(5 5)', 4326))," +
                "(6, 'SCHOOL_B', 'SCHOOL', 'NUCLEAR', 10.0, ST_GeomFromText('POINT(5 5)', 4326))," +
                "(7, 'FABRIC_A', 'FABRIC', 'FLOWING_WATER', 500.0, ST_GeomFromText('POINT(-5 5)', 4326))," +
                "(8, 'FABRIC_A', 'FABRIC', 'NUCLEAR', 150.0, ST_GeomFromText('POINT(-5 5)', 4326))," +
                "(9, 'FABRIC_B', 'FABRIC', 'WIND', 20.0, ST_GeomFromText('POINT(5 -5)', 4326))," +
                "(10, 'FABRIC_B', 'FABRIC', 'SOLAR', 30.0, ST_GeomFromText('POINT(5 -5)', 4326))," +
                "(11, 'HOUSE_A', 'HOUSE', 'FUEL', 6.0, ST_GeomFromText('POINT(0 0)', 4326))," +
                "(12, 'HOUSE_B', 'HOUSE', 'NUCLEAR', 4.0, ST_GeomFromText('POINT(0 0)', 4326));");
    }

    @Override
    protected void dropBuildingsTable() throws Exception {
        runSafe("DROP TABLE BUILDINGS_GROUP_BY_TESTS");
    }
}
