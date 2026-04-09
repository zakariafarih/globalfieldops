package com.globalfieldops.common.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final UUID resourceId;

    public ResourceNotFoundException(String resourceName, UUID resourceId) {
        super(String.format("%s not found with id: %s", resourceName, resourceId));
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public UUID getResourceId() {
        return resourceId;
    }
}
