/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2002-2016, Open Source Geospatial Foundation (OSGeo)
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

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.geotools.factory.Hints;
import org.opengis.temporal.Instant;

/**
 * Converter factory which created converting between the various temporal types.
 * <p>
 * Supported save conversions:
 * <ul>
 * <li>{@link java.util.Date} to {@link Calendar}
 * <li>{@link java.sql.Time} to {@link java.util.Calendar}
 * <li>{@link java.util.Date} to {@link java.sql.Timestamp}
 * <li>{@link java.util.Date} to {@link java.sql.Date}
 * <li>{@link java.util.Calendar} to {@link java.util.Date}
 * <li>{@link java.util.Calendar} to {@link java.sql.Timestamp}
 * <li>{@link XMLGregorianCalendar} to {@link Calendar}
 * <li>{@link Calendar} to {@link XMLGregorianCalendar}
 * <li>{@link XMLGregorianCalendar} to {@link Date}
 * <li>{@link Date} to {@link XMLGregorianCalendar}
 * <li>{@link String} to {@link TimeZone}
 * </ul>
 * </p>
 * <p>
 * Supported unsafe (lossy) conversions:
 * <ul>
 * <li>{@link java.util.Date} to {@link java.sql.Time}
 * <li>{@link java.util.Calendar} to {@link java.sql.Time}
 * <li>{@link java.sql.Timestamp} to {@link java.util.Calendar}
 * </ul>
 * </p>
 * <p>
 * The hint {@link ConverterFactory#SAFE_CONVERSION} is used to control which conversions will be 
 * applied.
 * </p>
 * <p>
 * The hint {@link #DATE_FORMAT} can be used to control the format of converting a temporal value to
 * a String.
 * </p>
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * @since 2.4
 *
 *
 *
 * @source $URL$
 */
public class TemporalConverterFactory implements ConverterFactory {

    public Converter createConverter(Class source, Class target, Hints hints) {
        boolean isSafeOnly = false;
        
        if (hints != null){
            Object safe = hints.get(ConverterFactory.SAFE_CONVERSION);
            if (safe instanceof Boolean && ((Boolean)safe).booleanValue()){
                isSafeOnly = true;
            }
        }
        
        if (Date.class.isAssignableFrom(source)) {
            // handle all of (java.util.Date,java.sql.Timestamp,and java.sql.Time) ->
            // java.util.Calendar
            if (Calendar.class.isAssignableFrom(target)) {
                if (isSafeOnly && Timestamp.class.isAssignableFrom(source)){
                    //java.sql.Timestamp -> Calendar is not a safe conversion
                    return null;
                }

                return new Converter() {
                    public Object convert(Object source, Class target) throws Exception {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime((Date) source);

                        return calendar;
                    }
                };
            }

            // handle all of (java.util.Date) -> (java.sql.Timestamp,java.sql.Time)
            if (Timestamp.class.isAssignableFrom(target) || Time.class.isAssignableFrom(target)
                    || java.sql.Date.class.isAssignableFrom(target)) {

                if ( isSafeOnly && Time.class.isAssignableFrom( target ) ) {
                    //not safe
                    return null;
                }
                
                return new Converter() {

                    public Object convert(Object source, Class target) throws Exception {
                        Date date = (Date) source;
                        return timeMillisToDate(date.getTime(), target);
                    }

                };
            }

            if (XMLGregorianCalendar.class.isAssignableFrom(target)) {
                return new Converter() {
                    public <T> T convert(Object source, Class<T> target) throws Exception {
                        Date date = (Date) source;
                        Calendar calendar = createConverter(Date.class, Calendar.class, null)
                                .convert(date, Calendar.class);

                        return (T) createConverter(Calendar.class, XMLGregorianCalendar.class, null)
                                .convert(calendar, XMLGregorianCalendar.class);

                    }
                };
            }

            if (Long.class.equals(target)) {
                return new Converter() {
                    public <T> T convert(Object source, Class<T> target) throws Exception {
                        return (T) Long.valueOf(((Date)source).getTime());
                    }
                };
            }
        }

        // this should handle java.util.Calendar to
        // (java.util.Date,java.sql.Timestamp,java.util.Time}
        if (Calendar.class.isAssignableFrom(source)) {
            if (Date.class.isAssignableFrom(target)) {
                if (isSafeOnly && Time.class.isAssignableFrom( target )){
                    //Calendar -> Time is not saf
                    return null;
                }
                final Class fTarget = target;
                return new Converter() {
                    public Object convert(Object source, Class target) throws Exception {
                        Calendar calendar = (Calendar) source;

                        return timeMillisToDate(calendar.getTimeInMillis(), target, calendar.getTimeZone());
                    }
                };
            }
            if (XMLGregorianCalendar.class.isAssignableFrom(target)) {
                return new Converter() {
                    public <T> T convert(Object source, Class<T> target) throws Exception {
                        if (source instanceof GregorianCalendar) {
                            return (T) DatatypeFactory.newInstance().newXMLGregorianCalendar(
                                    (GregorianCalendar) source);
                        }

                        return null;
                    }
                };
            }
            
        }

        if (XMLGregorianCalendar.class.isAssignableFrom(source)) {
            if (Calendar.class.isAssignableFrom(target)) {
                return new Converter() {
                    public <T> T convert(Object source, Class<T> target) throws Exception {
                        XMLGregorianCalendar calendar = (XMLGregorianCalendar) source;
                        return (T) calendar.toGregorianCalendar();
                    }
                };
            }
            if (Date.class.isAssignableFrom(target)) {
                return new Converter() {
                    public <T> T convert(Object source, Class<T> target) throws Exception {
                        Calendar calendar = createConverter(XMLGregorianCalendar.class,
                                Calendar.class, null).convert(source, Calendar.class);
                        if (calendar != null) {
                            return (T) createConverter(Calendar.class, Date.class, null).convert(
                                    calendar, Date.class);
                        }
                        return null;
                    }
                };
            }
        }
        
        if (TimeZone.class.isAssignableFrom(source)) {
            if (String.class == target) {
                return new Converter() {
                    public <T> T convert(Object source, Class<T> target) throws Exception {
                        if (source == null) {
                            return null;
                        }
                        return target.cast(((TimeZone) source).getID());
                    }
                };
            }
        }
        
        if(Instant.class.isAssignableFrom(source)) {
            if(Date.class == target) {
                return new Converter() {

                    @Override
                    public <T> T convert(Object source, Class<T> target) throws Exception {
                        Instant instant = (Instant) source;
                        return (T) instant.getPosition().getDate();
                    }
                    
                };
            }
        }

        if (Long.class.isAssignableFrom(source) && java.util.Date.class.isAssignableFrom(target)) {
            return new Converter() {
                @Override
                public Object convert(Object source, Class target) throws Exception {
                    if (source != null) {
                        return new java.util.Date((Long) source);
                    } else {
                        return null;
                    }
                }
            };
        }

        // if we have a string as input and are targeting a date type we may be able to extract a date
        if (String.class.isAssignableFrom(source) && java.util.Date.class.isAssignableFrom(target)) {
            // we may need to convert the parsed date to another date subtype
            Converter dateConverter = createConverter(Date.class, target, hints);
            if (dateConverter == null) {
                // well the target date subtype is not supported
                return null;
            }
            return stringToDateConverter(dateConverter);
        }

        return null;
    }

    /**
     * <p>Utility method that creates a converter that will try to extract a timestamp from a
     * string and convert it to a target date type.</p>
     *
     * <p>The date converter parameter will be used to convert the parsed data to the correct
     * targeted date type.</p>
     *
     * <p>If the source string doesn't contain a valid timestamp, i.e. is not a number NULL will
     * be returned.</p>
     *
     * @param dateConverter will be used to convert the parsed date to the targeted date type
     * @return a date type that represents the parsed timestamp or NULL
     */
    private Converter stringToDateConverter(Converter dateConverter) {

        return new Converter() {
            @Override
            public Object convert(Object source, Class target) throws Exception {
                if (source == null || target == null) {
                    // nothing to do here
                    return null;
                }
                // let's check that we are dealing with the correct types here
                if (!(source instanceof String && java.util.Date.class.isAssignableFrom(target))) {
                    // this is not good, the source needs to be a String and the target a date or a subtype of date
                    throw new RuntimeException(String.format(
                            "String to date converter received a wrong input type '%s' or a wrong target type '%s'.",
                            source.getClass().getCanonicalName(), target.getCanonicalName()));
                }
                long timeStamp;
                // trying to extract a timestamp from the source string
                try {
                    timeStamp = Long.parseLong((String) source);
                } catch(NumberFormatException exception) {
                    // the source string is not a timestamp
                    return null;
                }
                // let's create a date and convert it to the expected date type
                Date date = new Date(timeStamp);
                return dateConverter.convert(date, target);
            }
        };
    }
    
    /**
     * Turns a timestamp specified in milliseconds into a date, making sure to shave off
     * the un-necessary parts when building java.sql time related classes
     * @param time the number of milliseconds since January 1, 1970, 00:00:00 <b>GMT</b>
     * @param target
     * @return
     */
    Date timeMillisToDate(long time, Class target) {
        return timeMillisToDate(time, target, TimeZone.getDefault());
    }
    Date timeMillisToDate(long time, Class target, TimeZone zone) {
    	if(Timestamp.class.isAssignableFrom(target)) {
        	return new Timestamp(time);
        } else if(java.sql.Date.class.isAssignableFrom(target)) {
        	Calendar cal = Calendar.getInstance(zone);
        	cal.setTimeInMillis(time);
        	cal.set(Calendar.HOUR, 0);
        	cal.set(Calendar.MINUTE, 0);
        	cal.set(Calendar.SECOND, 0);
        	cal.set(Calendar.MILLISECOND, 0);
         	return new java.sql.Date(cal.getTimeInMillis());
        } else if(java.sql.Time.class.isAssignableFrom(target)) {
        	Calendar cal = Calendar.getInstance(zone);
        	cal.setTimeInMillis(time);
        	cal.set(Calendar.YEAR, 0);
        	cal.set(Calendar.MONTH, 0);
        	cal.set(Calendar.DAY_OF_MONTH, 0);
         	return new java.sql.Time(cal.getTimeInMillis());
        } else if(java.util.Date.class.isAssignableFrom(target)) {
        	return new java.util.Date(time);
        } else {
        	throw new IllegalArgumentException("Unsupported target type " + target);
        }
    }

}
