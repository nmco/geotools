package org.geotools.data.mongodb.complex;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MongoComplexUtilities {

    private static final String COLLECTION_ID_SEPARATOR = "cb019a4ea79711e680f576304dec7eb7";
    private static final Pattern ROOT_ID_PATTERN = Pattern.compile(String.format("^(.+?)(%1$s.*)?$", COLLECTION_ID_SEPARATOR));
    private static final Pattern COLLECTION_ID_PATTERN = Pattern.compile(String.format("%1$s(.+?)%1$s(.+?)%1$s", COLLECTION_ID_SEPARATOR));
    
    private MongoComplexUtilities() {
    }

    public static final class ObjectId {

        public final String name;
        public final String id;

        ObjectId(String name, String id) {
            this.name = name;
            this.id = id;
        }
    }

    public static Map<String, String> extractCollectionsIndexes(String nodeId) {
        Map<String, String> collectionsIndexes = new HashMap<>();
        Matcher matcher = ROOT_ID_PATTERN.matcher(nodeId);
        if (matcher.matches()) {
            String rootId = matcher.group(1);
            collectionsIndexes.put("", rootId);
            String rawCollectionsIndexes = matcher.group(2);
            if (rawCollectionsIndexes != null) {
                matcher = COLLECTION_ID_PATTERN.matcher(rawCollectionsIndexes);
                while(matcher.find()) {
                    collectionsIndexes.put(matcher.group(1), matcher.group(2));
                }
            }
        }
        return collectionsIndexes;
    }

    public static String getRootId(Map<String, String> collectionsIndexes) {
        return collectionsIndexes.get("");
    }

    public static Object jsonSelect(DBObject mongoObject, String jsonPath) {
        return jsonSelect(mongoObject, Collections.emptyMap(), jsonPath);
    }

    public static Object jsonSelect(DBObject mongoObject, Map<String, String> collectionsIndexes, String jsonPath) {
        ObjectWalker walker = new ObjectWalker(mongoObject, collectionsIndexes, jsonPath);
        return walker.getValue();
    }

    public static String computeNodeId(MongoObjectHolder mongoObjectHolder, String rootId) {
        String nodeId = mongoObjectHolder.getCollectionInfoHolder().getNodeId();
        if (nodeId == null) {
            nodeId = rootId;
        }
        Matcher matcher = ROOT_ID_PATTERN.matcher(nodeId);
        if (matcher.matches()) {
            String rawCollectionsIndexes = matcher.group(2);
            nodeId = rootId;
            if (rawCollectionsIndexes != null) {
                nodeId += rawCollectionsIndexes;
            }
        }
        return nodeId + COLLECTION_ID_SEPARATOR
                + mongoObjectHolder.getCollectionInfoHolder().getCollectionPath() + COLLECTION_ID_SEPARATOR
                + mongoObjectHolder.getSubCollectionIndex() + COLLECTION_ID_SEPARATOR;
    }

    public static String computeNodeId(String rootId, String nodeId) {
        Matcher matcher = ROOT_ID_PATTERN.matcher(nodeId == null ? rootId : nodeId);
        if (matcher.matches()) {
            String rawCollectionsIndexes = matcher.group(2);
            nodeId = rootId;
            if (rawCollectionsIndexes != null) {
                nodeId += rawCollectionsIndexes;
            }
        }
        return nodeId;
    }

    private static final class ObjectWalker {

        private final Map<String, String> collectionsIndexes;
        private final String[] jsonPathParts;

        private String currentJsonPath;
        private int currentJsonPathPartIndex;
        private Object currentObject;

        ObjectWalker(DBObject mongoObject, Map<String, String> collectionsIndexes, String jsonPath) {
            this.collectionsIndexes = collectionsIndexes;
            this.jsonPathParts = jsonPath.split("\\.");
            this.currentJsonPath = "";
            this.currentJsonPathPartIndex = 0;
            this.currentObject = mongoObject;
        }

        Object getValue() {
            while (hasNext()) {
                next();
            }
            return currentObject;
        }

        private boolean hasNext() {
            return currentJsonPathPartIndex < jsonPathParts.length
                    || (currentObject instanceof BasicDBList && collectionsIndexes.get(currentJsonPath) != null);
        }

        private void next() {
            if (currentObject instanceof List) {
                currentObject = next((List) currentObject);
            } else if (currentObject instanceof DBObject){
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
            return basicDBList.get(Integer.parseInt(collectionsIndexes.get(currentJsonPath)));
        }
    }
}