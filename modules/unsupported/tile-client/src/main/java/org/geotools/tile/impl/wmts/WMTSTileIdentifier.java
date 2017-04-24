/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2017, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.tile.impl.wmts;

import org.geotools.tile.TileIdentifier;
import org.geotools.tile.impl.ZoomLevel;
import org.geotools.tile.impl.osm.OSMTileIdentifier;

/**
 * @author ian
 *
 */
public class WMTSTileIdentifier extends TileIdentifier {

    /**
     * create an identifier based on /layername/{TileMatrixSet}/{TileMatrix}/{TileCol}/{TileRow}.png
     * 
     * @param x
     * @param y
     * @param zoomLevel
     * @param serviceName
     */
    public WMTSTileIdentifier(int x, int y, ZoomLevel zoomLevel, String serviceName) {
        super(x, y, zoomLevel, serviceName);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getId() {
        final String separator = "_";
        StringBuilder sb = createGenericCodeBuilder(separator);
        sb.insert(0, separator).insert(0, getServiceName());
        return sb.toString();
    }

    @Override
    public String getCode() {
        final String separator = "/";
        return createGenericCodeBuilder(separator).toString();
    }

    private StringBuilder createGenericCodeBuilder(final String separator) {
        StringBuilder sb = new StringBuilder(50);
        sb.append(getZ()).append(separator).append(getX()).append(separator).append(getY());

        return sb;
    }

    @Override
    public TileIdentifier getRightNeighbour() {
        int maxTilePerRowNumber = getZoomLevel().getMaxTilePerRowNumber();
        return new WMTSTileIdentifier(
                TileIdentifier.arithmeticMod((getX() + 1), maxTilePerRowNumber),
                getY(), getZoomLevel(), getServiceName());
    }

    @Override
    public TileIdentifier getLowerNeighbour() {
        int maxTilePerRowNumber = getZoomLevel().getMaxTilePerRowNumber();
        return new WMTSTileIdentifier(getX(),
                TileIdentifier.arithmeticMod((getY() + 1), maxTilePerRowNumber),
                getZoomLevel(), getServiceName());
    }

}
