/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.sld.bindings;

import org.geotools.filter.Filters;
import org.geotools.styling.SLD;
import org.geotools.styling.Stroke;

import java.util.Arrays;


/**
 * 
 *
 * @source $URL$
 */
public class SLDStrokeBindingTest extends SLDTestSupport {
    public void testType() throws Exception {
        assertEquals(Stroke.class, new SLDStrokeBinding(null,null).getType());
    }

    public void test() throws Exception {
        SLDMockData.stroke(document, document);

        Stroke stroke = (Stroke) parse();
        assertNotNull(stroke);

        assertEquals(Integer.parseInt("12", 16), SLD.color(stroke.getColor()).getRed());
        assertEquals(Integer.parseInt("34", 16), SLD.color(stroke.getColor()).getGreen());
        assertEquals(Integer.parseInt("56", 16), SLD.color(stroke.getColor()).getBlue());

        assertNotNull(stroke.getGraphicFill());
        assertEquals("butt", Filters.asString(stroke.getLineCap()));
        assertEquals("mitre", Filters.asString(stroke.getLineJoin()));

        assertTrue(Arrays.equals(new float[]{1.1f, 2.2f, 3.3f, 4.4f}, stroke.getDashArray().evaluate(null, float[].class)));

        assertEquals(1.0, Filters.asDouble(stroke.getOpacity()), 0d);
    }
}
