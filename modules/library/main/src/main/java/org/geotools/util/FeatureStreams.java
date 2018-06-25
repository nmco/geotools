/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2018, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.util;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

/**
 * Provides toStream transform methods for Feature Collection & Iterator
 *
 * @author Fernando Miño, Geosolutions
 */
public abstract class FeatureStreams {

    public static Stream<Feature> toFeatureStream(FeatureCollection<FeatureType, Feature> fc) {
        StreamFeatureIterator<Feature> fi = new StreamFeatureIterator<>(fc.features());
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(fi, 0), false)
                .onClose(
                        () -> {
                            fc.features().close();
                        });
    }

    public static <T extends Feature, K extends FeatureType> Stream<T> toFeatureStreamGeneric(
            FeatureCollection<K, T> fc) {
        StreamFeatureIterator<T> fi = new StreamFeatureIterator<>(fc.features());
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(fi, 0), false)
                .onClose(
                        () -> {
                            fc.features().close();
                        });
    }

    public static Stream<SimpleFeature> toSimpleFeatureStream(
            FeatureCollection<SimpleFeatureType, SimpleFeature> fc) {
        StreamFeatureIterator<SimpleFeature> fi = new StreamFeatureIterator<>(fc.features());
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(fi, 0), false)
                .onClose(
                        () -> {
                            fc.features().close();
                        });
    }

    public static class StreamFeatureIterator<F extends Feature>
            implements Closeable, Iterator<F>, FeatureIterator<F> {

        private FeatureIterator<F> delegate;

        public StreamFeatureIterator(FeatureIterator<F> fiter) {
            delegate = fiter;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public F next() {
            return delegate.next();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
