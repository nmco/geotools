package org.geotools.data.mongodb.complex;

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

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(MongoComplexUtilities.class);

    // some patterns used to parse node ids
    private static final Pattern DOCUMENT_ID_PATTERN = Pattern.compile("^(.+?)(-(?!/).*)?$");
    private static final Pattern COLLECTION_ID_PATTERN = Pattern.compile("-(?!/)(.+?)-(?!/)(.+?)-(?!/)");

    private MongoComplexUtilities() {
    }

    /**
     * A MongoDB complex feature id will be a simple id or will contain information about all the collections
     * related with the current path. For example node i 'document_id-collection_a-4-collection_b-3', means that
     * the current complex feature maps to document with id 'document_id', is contained by the element at position
     * '4' of collection 'collection_a' and is the element at position '3' of collection 'collection_b'.
     */
    public static Map<String, String> extractCollectionsIndexes(String nodeId) {
        // separating document id form the collections indexes
        Map<String, String> collectionsIndexes = new HashMap<>();
        Matcher matcher = DOCUMENT_ID_PATTERN.matcher(nodeId);
        if (matcher.matches()) {
            // extract the document id and store it
            String documentId = decodeSeparator(matcher.group(1));
            collectionsIndexes.put("", documentId);
            String rawCollectionsIndexes = matcher.group(2);
            if (rawCollectionsIndexes != null) {
                // we have collections indexes, so let's parse them
                matcher = COLLECTION_ID_PATTERN.matcher(rawCollectionsIndexes);
                while (matcher.find()) {
                    // extract collection path and index
                    String collectionPath = decodeSeparator(matcher.group(1));
                    String collectionIndex = matcher.group(2);
                    collectionsIndexes.put(collectionPath, collectionIndex);
                }
            }
        }
        LOGGER.fine(() -> String.format("Collections indexes extracted from '%s': %s.",
                collectionsIndexes.entrySet().stream().map((entry) -> "[" + entry.getKey() + " -> " + entry.getValue() + "]")));
        return collectionsIndexes;
    }

    public static Object jsonSelect(DBObject mongoObject, String jsonPath, boolean selectAll) {
        return jsonSelect(mongoObject, Collections.emptyMap(), jsonPath, selectAll);
    }

    public static Object jsonSelect(DBObject mongoObject, Map<String, String> collectionsIndexes, String jsonPath, boolean selectAll) {
        if (selectAll) {
            return getAllValues(mongoObject, jsonPath);
        }
        MongoObjectWalker walker = new MongoObjectWalker(mongoObject, collectionsIndexes, jsonPath);
        return walker.getValue();
    }

    private static String encodeSeparator(String text) {
        if (text == null) {
            return text;
        }
        return text.replace("-", "-/");
    }

    private static String decodeSeparator(String text) {
        if (text == null) {
            return text;
        }
        return text.replace("-/", "-");
    }

    public static String computeNodeId(MongoObjectHolder mongoObjectHolder, String rootId) {
        String nodeId = mongoObjectHolder.getCollectionInfoHolder().getNodeId();
        if (nodeId == null) {
            nodeId = rootId;
        }
        Matcher matcher = DOCUMENT_ID_PATTERN.matcher(nodeId);
        if (matcher.matches()) {
            String rawCollectionsIndexes = matcher.group(2);
            nodeId = rootId;
            if (rawCollectionsIndexes != null) {
                nodeId += rawCollectionsIndexes;
            }
        }
        return nodeId + "-"
                + mongoObjectHolder.getCollectionInfoHolder().getCollectionPath() + "-"
                + mongoObjectHolder.getSubCollectionIndex() + "-";
    }

    public static String computeNodeId(String rootId, String nodeId) {
        Matcher matcher = DOCUMENT_ID_PATTERN.matcher(nodeId == null ? rootId : nodeId);
        if (matcher.matches()) {
            String rawCollectionsIndexes = matcher.group(2);
            nodeId = rootId;
            if (rawCollectionsIndexes != null) {
                nodeId += rawCollectionsIndexes;
            }
        }
        return nodeId;
    }

    /**
     * Utility class class to extract information from a MongoDB object giving a certain path.
     */
    private static final class MongoObjectWalker {

        private final Map<String, String> collectionsIndexes;
        private final String[] jsonPathParts;

        private String currentJsonPath;
        private int currentJsonPathPartIndex;
        private Object currentObject;

        MongoObjectWalker(DBObject mongoObject, Map<String, String> collectionsIndexes, String jsonPath) {
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
            String rawCollectionIndex = collectionsIndexes.get(currentJsonPath);
            if (rawCollectionIndex == null) {
                throw new RuntimeException(String.format("There is no index available for collection '%s'.", currentJsonPath));
            }
            return basicDBList.get(Integer.parseInt(rawCollectionIndex));
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

    /**
     * Utility class class to extract information from a MongoDB object giving a certain path.
     * This walker will return all the possible values that match this.
     */
    private static final class MongoObjectWalkerAll {

        private final String[] jsonPathParts;
        private final DBObject mongoObject;

        MongoObjectWalkerAll(DBObject mongoObject, String jsonPath) {
            this.jsonPathParts = jsonPath.split("\\.");
            this.mongoObject = mongoObject;
        }

        public List<Object> getValues() {
            List<Object> values = new ArrayList<>();

            return values;
        }

        private void walk(DBObject dbObject, String[] jsonPathParts, List<Object> values, int index) {
            Object object = dbObject.get(jsonPathParts[index]);
            if (object == null) {
                return;
            }
            boolean finalPath = index == jsonPathParts.length - 1;
            index++;
            if (object instanceof List) {
                for (Object element : (List) object) {
                    if (finalPath) {
                        values.add(element);
                    } else {
                        walk((DBObject) object, jsonPathParts, values, index);
                    }
                }
            } else {
                values.add(object);
            }
        }
    }
}