package org.geotools.data.mongodb;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class contains utilities methods for dealing with MongoDB complex features.
 */
public final class MongoComplexUtilities {

    private MongoComplexUtilities() {
    }

    public static Object jsonSelect(DBObject mongoObject, String jsonPath, boolean selectAll) {
        return jsonSelect(mongoObject, Collections.emptyMap(), jsonPath, selectAll);
    }

    public static Object jsonSelect(DBObject mongoObject, Map<String, Integer> collectionsIndexes, String jsonPath, boolean selectAll) {
        if (selectAll) {
            return getAllValues(mongoObject, jsonPath);
        }
        MongoObjectWalker walker = new MongoObjectWalker(mongoObject, collectionsIndexes, jsonPath);
        return walker.getValue();
    }

    /**
     * Utility class class to extract information from a MongoDB object giving a certain path.
     */
    private static final class MongoObjectWalker {

        private final Map<String, Integer> collectionsIndexes;
        private final String[] jsonPathParts;

        private String currentJsonPath;
        private int currentJsonPathPartIndex;
        private Object currentObject;

        MongoObjectWalker(DBObject mongoObject, Map<String, Integer> collectionsIndexes, String jsonPath) {
            this.collectionsIndexes = collectionsIndexes;
            this.jsonPathParts = jsonPath.split("\\.");
            this.currentJsonPath = "";
            this.currentJsonPathPartIndex = 0;
            this.currentObject = mongoObject;
        }

        /**
         * Returns the object value that matches the given path.
         */
        Object getValue() {
            while (hasNext() && currentObject != null) {
                next();
            }
            // end of the walked path or NULL value found
            return currentObject;
        }

        private boolean hasNext() {
            // we have a next element if we still have paths parts or we are currently waking
            // a collection and there is a index defined for this collection
            return currentJsonPathPartIndex < jsonPathParts.length
                    || (currentObject instanceof BasicDBList && collectionsIndexes.get(currentJsonPath) != null);
        }

        private void next() {
            if (currentObject instanceof List) {
                currentObject = next((List) currentObject);
            } else if (currentObject instanceof DBObject) {
                if (currentJsonPath.isEmpty()) {
                    currentJsonPath += jsonPathParts[currentJsonPathPartIndex];
                } else {
                    currentJsonPath += '.' + jsonPathParts[currentJsonPathPartIndex];
                }
                currentObject = next((DBObject) currentObject);
            } else {
                throw new RuntimeException(String.format(
                        "Trying to get data from a non MongoDB object, current json path is '%s'.", currentJsonPath));
            }
        }

        private Object next(DBObject dbObject) {
            Object result = dbObject.get(jsonPathParts[currentJsonPathPartIndex]);
            currentJsonPathPartIndex++;
            return result;
        }

        private Object next(List basicDBList) {
            Integer rawCollectionIndex = collectionsIndexes.get(currentJsonPath);
            if (rawCollectionIndex == null) {
                throw new RuntimeException(String.format("There is no index available for collection '%s'.", currentJsonPath));
            }
            return basicDBList.get(rawCollectionIndex);
        }
    }

    private static List<Object> getAllValues(DBObject dbObject, String jsonPath) {
        if (jsonPath == null || jsonPath.isEmpty() || dbObject == null) {
            return Collections.emptyList();
        }
        String[] jsonPathParts = jsonPath.split("\\.");
        return walkHelper(dbObject, jsonPathParts, new ArrayList<>(), 0);
    }

    private static List<Object> walkHelper(DBObject dbObject, String[] jsonPathParts, List<Object> values, int index) {
        Object object = dbObject.get(jsonPathParts[index]);
        if (object == null) {
            return values;
        }
        boolean finalPath = index == jsonPathParts.length - 1;
        index++;
        if (object instanceof List) {
            if (finalPath) {
                values.addAll((List) object);
            } else {
                for (Object element : (List) object) {
                    walkHelper((DBObject) element, jsonPathParts, values, index);
                }
            }
        } else {
            if (finalPath) {
                values.add(object);
            } else {
                walkHelper((DBObject) object, jsonPathParts, values, index);
            }
        }
        return values;
    }
}