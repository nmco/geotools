package org.geotools.xml;

import org.eclipse.xsd.XSDSchema;

import java.io.IOException;

/**
 * Created by nuno on 12/3/16.
 */
public class Main {

    public static void main(String[] args) throws Exception {
        XSDSchema schema = Schemas.parse("/home/nuno/github/geotools-clean/modules/unsupported/mongodb/src/main/resources/datex/datex.xsd");
        System.out.println(schema.getTargetNamespace());
    }
}
