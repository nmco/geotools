/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2007-2016, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.coverage.io.netcdf;

import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.factory.Hints;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.test.TestData;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

import javax.media.jai.PlanarImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Tests that a data set that contains a dimension with multiple bands is correctly handled.
 * Is very important to use a clean test directory for each run, this guarantees that
 * generated files (like the binary index for example) are not reused.
 */
public final class NetCDFStationsTest extends Assert {

    @Test
    public void readMultipleBandsDimension() throws Exception {

        // create test directory for this test removing any existing one
        File testDirectory = new File(TestData.file(this, "."), "MultipleBandsDimensionTest");
        FileUtils.deleteQuietly(testDirectory);
        assertThat(testDirectory.mkdirs(), is(true));

        // just keeping a reference to the reader so we can close it in the finally block
        NetCDFReader reader = null;

        try {

            // move test files to the test directory
            FileUtils.copyFileToDirectory(TestData.file(this, "stations.nc"), testDirectory);
            FileUtils.copyFileToDirectory(TestData.file(this, "stations.xml"), testDirectory);
            File netCdfFile = new File(testDirectory, "stations.nc");
            File auxiliaryFile = new File(testDirectory, "stations.xml");

            // instantiate the netcdf reader, add the auxiliary file as an hint for the reader
            final Hints hints = new Hints(Utils.AUXILIARY_FILES_PATH, auxiliaryFile.getPath());
            reader = new NetCDFReader(netCdfFile, hints);

            // checking that we have four coverages (only the first station has values others stations values are zero)
            List<String> names = Arrays.asList(reader.getGridCoverageNames());
            assertThat(names.contains("stationA"), is(true));
            assertThat(names.contains("stationB"), is(true));
            assertThat(names.contains("stationC"), is(true));
            assertThat(names.contains("stationD"), is(true));

            // reading the stations data set
            ParameterValue<GridGeometry2D> grid = AbstractGridFormat.READ_GRIDGEOMETRY2D.createValue();
            GridCoverage2D coverage = reader.read("stationA", new GeneralParameterValue[]{grid});
            assertThat(coverage, notNullValue());
            Raster data = PlanarImage.wrapRenderedImage(coverage.getRenderedImage()).getData();
            assertThat(data.getNumBands(), is(3));
            assertThat(data.getDataBuffer().getNumBanks(), is(3));

            // let's check the data, we should have three bands (with an inverted axis)
            int[] bank1 = readBank(data.getDataBuffer(), 0);
            int[] bank2 = readBank(data.getDataBuffer(), 1);
            int[] bank3 = readBank(data.getDataBuffer(), 2);
            assertThat(Arrays.equals(bank1, new int[]{5, 6, 7, 8, 9, 0, 1, 2, 3, 4}), is(true));
            assertThat(Arrays.equals(bank2, new int[]{105, 106, 107, 108, 109, 100, 101, 102, 103, 104}), is(true));
            assertThat(Arrays.equals(bank3, new int[]{1005, 1006, 1007, 1008, 1009, 1000, 1001, 1002, 1003, 1004}), is(true));

        } finally {
            // cleaning the tests directory
            FileUtils.deleteQuietly(testDirectory);
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    /**
     * Helper method that simply reads a bank of data from a data buffer.
     */
    private int[] readBank(DataBuffer dataBuffer, int bank) {
        int[] data = new int[dataBuffer.getSize()];
        for (int i = 0; i < dataBuffer.getSize(); i++) {
            data[i] = dataBuffer.getElem(bank, i);
        }
        return data;
    }
}
