package com.globalfieldops.audit.entity;

/**
 * Machine-readable audit event types for the core business flow.
 */
public enum AuditEventType {
    WORK_ORDER_CREATED,
    TECHNICIAN_ASSIGNED,
    WORK_ORDER_STATUS_CHANGED,
    WORK_ORDER_COMMENT_ADDED
}
