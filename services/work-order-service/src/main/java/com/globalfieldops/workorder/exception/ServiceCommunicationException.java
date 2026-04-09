package com.globalfieldops.workorder.exception;

public class ServiceCommunicationException extends RuntimeException {

    private final String serviceName;

    public ServiceCommunicationException(String serviceName, Throwable cause) {
        super("Failed to communicate with " + serviceName, cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
