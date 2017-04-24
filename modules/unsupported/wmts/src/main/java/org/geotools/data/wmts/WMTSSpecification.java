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
package org.geotools.data.wmts;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.geotools.data.ows.AbstractGetCapabilitiesRequest;
import org.geotools.data.ows.GetCapabilitiesRequest;
import org.geotools.data.ows.HTTPResponse;
import org.geotools.data.ows.Response;
import org.geotools.data.ows.Specification;
import org.geotools.data.ows.StyleImpl;
import org.geotools.data.wms.WMS1_0_0;
import org.geotools.data.wms.response.WMSGetCapabilitiesResponse;
import org.geotools.data.wmts.request.AbstractGetTileRequest;
import org.geotools.data.wmts.request.GetTileRequest;
import org.geotools.data.wmts.response.GetTileResponse;
import org.geotools.data.wmts.response.WMTSGetCapabilitiesResponse;
import org.geotools.map.WMTSMapLayer;
import org.geotools.ows.ServiceException;
import org.geotools.tile.Tile;
import org.geotools.tile.impl.wmts.WMTSServiceType;

/**
 * @author ian
 *
 */
public class WMTSSpecification extends Specification {

    public static final String WMTS_VERSION = "1.0.0";
    private WMTSServiceType type;

    /**
     * 
     */
    public WMTSSpecification() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getVersion() {
        // 
        return WMTS_VERSION;
    }

    @Override
    public GetCapabilitiesRequest createGetCapabilitiesRequest(URL server) {
        // TODO Auto-generated method stub
        return new GetCapsRequest(server);
    }
    
    public GetTileRequest createGetTileRequest(URL server, Properties props, WMTSCapabilities caps) {
        return new GetTileRequest(server, props, caps);
        
    }
    
    static public class GetTileRequest extends AbstractGetTileRequest{

        
        /**
         * @param onlineResource
         * @param properties
         * @param type 
         */
        public GetTileRequest(URL onlineResource, Properties properties, WMTSCapabilities capabilities) {
            super(onlineResource, properties);
            this.type = capabilities.getType();
            this.capabilities = capabilities;
        }

        @Override
        public Response createResponse(HTTPResponse response) throws ServiceException, IOException {
            // TODO Auto-generated method stub
            return new GetTileResponse(response, getType());
       }

        @Override
        protected void initVersion() {
            setProperty(VERSION, WMTS_VERSION);
            
        }
        protected void initRequest() {
            setProperty(REQUEST, "GetTile"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        /**
         * @return the type
         */
        public WMTSServiceType getType() {
            return type;
        }

        /**
         * @param type the type to set
         */
        public void setType(WMTSServiceType type) {
            this.type = type;
        }

 
       
    }
    
    static public class GetCapsRequest extends AbstractGetCapabilitiesRequest {
        /**
         * Construct a Request compatible with a 1.0.1 WMTS.
         * 
         * @param urlGetCapabilities URL of GetCapabilities document.
         */
        public GetCapsRequest( URL urlGetCapabilities ) {
            super(urlGetCapabilities);
        }

        protected void initVersion() {
            setProperty(VERSION, WMTS_VERSION); //$NON-NLS-1$ //$NON-NLS-2$
            
        }

        protected void initRequest() {
            setProperty(REQUEST, "GetCapabilities"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        protected void initService() {
            setProperty(SERVICE , "WMTS");
        }
        /*
         * @see org.geotools.data.wms.request.AbstractRequest#processKey(java.lang.String)
         */
        protected String processKey( String key ) {
            return WMTSSpecification.processKey(key);
        }
        
        public Response createResponse(HTTPResponse httpResponse) throws ServiceException,
                IOException {
            return new WMTSGetCapabilitiesResponse(httpResponse, hints);
        }
    }

    /**
     * @param key
     * @return
     */
    public static String processKey(String key) {
        
        return key.trim().toUpperCase();
    }

   
}
