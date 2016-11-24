package org.geotools.data.mongodb.complex;

import java.util.Map;

public final class CollectionInfoHolder {

    private boolean containsCollectionInfo;
    private String nodeId;
    private String collectionPath;
    private Map<String, String> objectsIds;

    public CollectionInfoHolder() {
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public boolean containsCollectionInfo() {
        return containsCollectionInfo;
    }

    public void setContainsCollectionInfo(boolean containsCollectionInfo) {
        this.containsCollectionInfo = containsCollectionInfo;
    }

    public String getCollectionPath() {
        return collectionPath;
    }

    public void setCollectionPath(String collectionPath) {
        this.collectionPath = collectionPath;
    }

    public Map<String, String> getObjectsIds() {
        return objectsIds;
    }

    public void setObjectsIds(Map<String, String> objectsIds) {
        this.objectsIds = objectsIds;
    }
}
