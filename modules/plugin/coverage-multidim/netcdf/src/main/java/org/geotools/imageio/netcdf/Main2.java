package org.geotools.imageio.netcdf;

        import ucar.ma2.*;
        import ucar.nc2.Attribute;
        import ucar.nc2.Dimension;
        import ucar.nc2.NetcdfFileWriter;
        import ucar.nc2.Variable;

        import java.io.IOException;
        import java.util.ArrayList;
        import java.util.List;

public class Main2 {


    public static void main(String[] args) throws Exception {

        String location = "/tmp/multiple_band.nc";
        NetcdfFileWriter writer = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, location, null);

        // add dimensions
        Dimension latDim = writer.addDimension(null, "lat", 64);
        Dimension lonDim = writer.addDimension(null, "lon", 128);

        // add Variable double temperature(lat,lon)
        List<Dimension> dims = new ArrayList<>();
        dims.add(latDim);
        dims.add(lonDim);
        Variable t = writer.addVariable(null, "temperature", DataType.DOUBLE, dims);
        t.addAttribute(new Attribute("units", "K"));   // add a 1D attribute of length 3
        Array data = Array.factory(int.class, new int[]{3}, new int[]{1, 2, 3});
        t.addAttribute(new Attribute("scale", data));

        // add a string-valued variable: char svar(80)
        Dimension svar_len = writer.addDimension(null, "svar_len", 80);
        writer.addVariable(null, "svar", DataType.CHAR, "svar_len");

        // add a 2D string-valued variable: char names(names, 80)
        Dimension names = writer.addDimension(null, "names", 3);
        writer.addVariable(null, "names", DataType.CHAR, "names svar_len");

        // add a scalar variable
        writer.addVariable(null, "scalar", DataType.DOUBLE, new ArrayList<>());

        // add global attributes
        writer.addGroupAttribute(null, new Attribute("yo", "face"));
        writer.addGroupAttribute(null, new Attribute("versionD", 1.2));
        writer.addGroupAttribute(null, new Attribute("versionF", (float) 1.2));
        writer.addGroupAttribute(null, new Attribute("versionI", 1));
        writer.addGroupAttribute(null, new Attribute("versionS", (short) 2));
        writer.addGroupAttribute(null, new Attribute("versionB", (byte) 3));

        // create the file
        try {
            writer.create();
        } catch (IOException e) {
            System.err.printf("ERROR creating file %s%n%s", location, e.getMessage());
        }
        writer.close();

        // write content to file

        writer = NetcdfFileWriter.openExisting(location);

        // write data to variable
        Variable v = writer.findVariable("temperature");
        int[] shape = v.getShape();
        ArrayDouble A = new ArrayDouble.D2(shape[0], shape[1]);
        int i, j;
        Index ima = A.getIndex();
        for (i = 0; i < shape[0]; i++) {
            for (j = 0; j < shape[1]; j++) {
                A.setDouble(ima.set(i, j), (double) (i * 1000000 + j * 1000));
            }
        }

        int[] origin = new int[2];
        try {
            writer.write(v, origin, A);
        } catch (IOException e) {
            System.err.println("ERROR writing file");
        } catch (InvalidRangeException e) {
            e.printStackTrace();
        }

        // write char variable as String
        v = writer.findVariable("svar");
        shape = v.getShape();
        int len = shape[0];
        try {
            ArrayChar ac2 = new ArrayChar.D1(len);
            ac2.setString("Two pairs of ladies stockings!");
            writer.write(v, ac2);
        } catch (IOException e) {
            System.err.println("ERROR writing Achar2");
            assert (false);
        } catch (InvalidRangeException e) {
            e.printStackTrace();
            assert (false);
        }

        // write String array
        v = writer.findVariable("names");
        shape = v.getShape();
        try {
            ArrayChar ac2 = new ArrayChar.D2(shape[0], shape[1]);
            ima = ac2.getIndex();
            ac2.setString(ima.set(0), "No pairs of ladies stockings!");
            ac2.setString(ima.set(1), "One pair of ladies stockings!");
            ac2.setString(ima.set(2), "Two pairs of ladies stockings!");
            writer.write(v, ac2);
        } catch (IOException e) {
            System.err.println("ERROR writing Achar3");
            assert (false);
        } catch (InvalidRangeException e) {
            e.printStackTrace();
            assert (false);
        }

        // write scalar data
        try {
            ArrayDouble.D0 datas = new ArrayDouble.D0();
            datas.set(222.333);
            v = writer.findVariable("scalar");

            writer.write(v, datas);
        } catch (IOException e) {
            System.err.println("ERROR writing scalar");
        } catch (InvalidRangeException e) {
            e.printStackTrace();
        }

        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

