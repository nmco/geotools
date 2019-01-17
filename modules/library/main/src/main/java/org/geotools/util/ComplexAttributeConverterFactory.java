/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2009-2011, Open Source Geospatial Foundation (OSGeo)
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

import java.util.Collection;
import org.geotools.factory.Hints;
import org.geotools.feature.AttributeImpl;
import org.geotools.filter.identity.FeatureIdImpl;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.filter.identity.FeatureId;

/**
 * This converter retrieves the values out of attributes.
 *
 * @author Rini Angreani (CSIRO Earth Science and Resource Engineering)
 * @author Niels Charlier
 * @source $URL$
 */
public class ComplexAttributeConverterFactory implements ConverterFactory {

    public Converter createConverter(Class<?> source, Class<?> target, Hints hints) {
        if (ComplexAttribute.class.isAssignableFrom(source)) {
            return new Converter() {
                public Object convert(Object source, Class target) throws Exception {
                    if (source instanceof ComplexAttribute) {
                        Collection<? extends Property> valueMap =
                                ((ComplexAttribute) source).getValue();
                        if (valueMap.isEmpty() || valueMap.size() > 1) {
                            return null;
                        } else {
                            // there should only be one value
                            source = valueMap.iterator().next();
                            if (AttributeImpl.class.equals(source.getClass())) {
                                return Converters.convert(((Attribute) source).getValue(), target);
                            }
                        }
                    }
                    return null;
                }
            };
        }

        // GeometryAttribute unwrapper
        if (GeometryAttribute.class.isAssignableFrom(source)) {
            return new Converter() {
                public Object convert(Object source, Class target) throws Exception {
                    if (source instanceof GeometryAttribute) {
                        return Converters.convert(((GeometryAttribute) source).getValue(), target);
                    }
                    return null;
                }
            };
        }

        // String to FeatureId comparison
        if (FeatureId.class.isAssignableFrom(target) && String.class.isAssignableFrom(source)) {
            return new Converter() {
                public Object convert(Object source, Class target) {
                    if (source != null) {
                        return new FeatureIdImpl((String) source);
                    }
                    return null;
                }
            };
        }

        // gets the value of an attribute and tries to convert it to a string
        if (Attribute.class.isAssignableFrom(source)) {
            return new Converter() {
                public Object convert(Object source, Class target) {
                    if (source instanceof Attribute) {
                        // get the attribute value
                        Attribute attribute = (Attribute) source;
                        Object value = attribute.getValue();
                        // let's try to avoid an infinite loop
                        if (value instanceof Attribute) {
                            // looks like we are done
                            return null;
                        }
                        // let the available converters do their job
                        return Converters.convert(value, target);
                    }
                    // this should not happen, anyway we can only handle attributes
                    return null;
                }
            };
        }

        // handles a list of attributes conversion to string
        if (Collection.class.isAssignableFrom(source) && target == String.class) {
            return new Converter() {
                public Object convert(Object source, Class target) {
                    if (!isCollectionOf(source, Attribute.class)) {
                        return null;
                    }
                    StringBuilder builder = new StringBuilder();
                    Collection<?> collection = (Collection<?>) source;
                    for (Object element : collection) {
                        if (element == null) {
                            builder.append("NULL, ");
                        } else {
                            builder.append(Converters.convert(element, String.class));
                            builder.append(", ");
                        }
                    }
                    if (builder.length() == 0) {
                        return "";
                    }
                    builder.delete(builder.length() - 2, builder.length());
                    return builder.toString();
                }
            };
        }

        return null;
    }

    private boolean isCollectionOf(Object source, Class<?> expected) {
        if (!(source instanceof Collection<?>)) {
            return false;
        }
        Collection<?> collection = (Collection<?>) source;
        for (Object element : collection) {
            if (!(element != null && expected.isAssignableFrom(element.getClass()))) {
                return false;
            }
        }
        return true;
    }
}
