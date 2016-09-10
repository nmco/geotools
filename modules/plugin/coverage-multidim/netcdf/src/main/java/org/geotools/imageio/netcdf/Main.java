package org.geotools.imageio.netcdf;

import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.util.ArrayList;
import java.util.List;

public class Main {


    public static void main(String[] args) throws Exception {

        // create the netcdf file
        String location = "/tmp/multiple.nc";
        NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, location, null);

        // add dimensions
        Dimension dataDimension = writer.addDimension(null, "data", 3);
        Dimension timeDimension = writer.addDimension(null, "time", 2);
        Dimension latDimension = writer.addDimension(null, "lat", 2);
        Dimension lonDimension = writer.addDimension(null, "lon", 5);

        // add variables that contain dimensions (data, time, lat, long)
        List<Dimension> dimensions = new ArrayList<>();
        dimensions.add(dataDimension);
        dimensions.add(timeDimension);
        dimensions.add(latDimension);
        dimensions.add(lonDimension);

        // add core variables
        writer.addVariable(null, "data", DataType.INT, "data")
                .addAttribute( new Attribute("long_name", "data"));
        writer.addVariable(null, "time", DataType.INT, "time")
                .addAttribute( new Attribute("units", "hours since 2010-01-01 0:00:00"));
        writer.addVariable(null, "lat", DataType.FLOAT, "lat")
                .addAttribute( new Attribute("long_name", "latitudes"));
        writer.addVariable(null, "lon", DataType.FLOAT, "lon")
                .addAttribute( new Attribute("long_name", "longitudes"));

        // add some variables representing stations
        writer.addVariable(null, "stationA", DataType.INT, dimensions)
                .addAttribute( new Attribute("long_name", "stationA"));
        writer.addVariable(null, "stationB", DataType.INT, dimensions)
                .addAttribute( new Attribute("long_name", "stationB"));
        writer.addVariable(null, "stationC", DataType.INT, dimensions)
                .addAttribute( new Attribute("long_name", "stationC"));
        writer.addVariable(null, "stationD", DataType.INT, dimensions)
                .addAttribute( new Attribute("long_name", "stationD"));

        // add global attributes
        writer.addGroupAttribute(null, new Attribute("coverage", "multiple_bad_test"));
        writer.addGroupAttribute(null, new Attribute("time", "multiple_bad_test"));

        // create file and close it
        writer.create();
        writer.close();

        // write data to the file
        writer = NetcdfFileWriter.openExisting(location);

        // write data for lat variable
        Variable latitude = writer.findVariable("lat");
        ArrayFloat latitudeData = new ArrayFloat.D1(2);
        Index latitudeIndex = latitudeData.getIndex();
        latitudeData.setFloat(latitudeIndex.set(0), 0.0f);
        latitudeData.setFloat(latitudeIndex.set(1), 1.0f);
        writer.write(latitude, latitudeData);

        // write data for lon variable
        Variable longitude = writer.findVariable("lon");
        ArrayFloat longitudeData = new ArrayFloat.D1(5);
        Index longitudeIndex = longitudeData.getIndex();
        longitudeData.setFloat(longitudeIndex.set(0), -2.0f);
        longitudeData.setFloat(longitudeIndex.set(1), -1.0f);
        longitudeData.setFloat(longitudeIndex.set(2), 0.0f);
        longitudeData.setFloat(longitudeIndex.set(3), 1.0f);
        longitudeData.setFloat(longitudeIndex.set(4), 2.0f);
        writer.write(longitude, longitudeData);

        // write data for time variable
        Variable time = writer.findVariable("time");
        ArrayInt timeData = new ArrayInt.D1(2);
        Index timeIndex = timeData.getIndex();
        timeData.setInt(timeIndex.set(0), 1000);
        timeData.setInt(timeIndex.set(1), 2000);
        writer.write(time, timeData);

        // write data parameter order
        Variable data = writer.findVariable("data");
        ArrayInt dataData = new ArrayInt.D1(3);
        Index dataIndex = dataData.getIndex();
        dataData.setInt(dataIndex.set(0), 1);
        dataData.setInt(dataIndex.set(1), 2);
        dataData.setInt(dataIndex.set(2), 3);
        writer.write(data, dataData);

        // write station a data
        Variable stationA = writer.findVariable("stationA");
        ArrayInt stationDataD = new ArrayInt.D4(3, 2, 2, 5);
        Index stationAIndex = stationDataD.getIndex();
        int data1 = 0;
        int data2 = 100;
        int data3 = 1000;
        for (int t = 0; t < 2; t++) {
            for (int y = 0; y < 2; y++) {
                for (int x = 0; x < 5; x++) {
                    for (int v = 0; v < 3; v++) {
                        int value = 0;
                        switch (v) {
                            case 0:
                                value = data1;
                                data1++;
                                break;
                            case 1:
                                value = data2;
                                data2++;
                                break;
                            case 2:
                                value = data3;
                                data3++;
                                break;
                        }
                        stationDataD.setInt(stationAIndex.set(v, t, y, x), value);
                    }
                }
            }
        }
        writer.write(stationA, stationDataD);

        // close the writer
        writer.close();
    }
}
