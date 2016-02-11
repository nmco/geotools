package org.geotools.data.postgis;

import org.geotools.jdbc.JDBCGroupByVisitorOnlineTest;

public class PostgisGroupByOnlineTest extends JDBCGroupByVisitorOnlineTest {

    @Override
    protected PostgisGroupByVisitorTestSetup createTestSetup() {
        return new PostgisGroupByVisitorTestSetup(new PostGISTestSetup());
    }
}
