// ── Enums ────────────────────────────────────────────────────

export type AvailabilityStatus = "AVAILABLE" | "ON_JOB" | "OFF_DUTY"
export type ProficiencyLevel = "JUNIOR" | "MID" | "SENIOR" | "EXPERT"
export type WorkOrderStatus = "OPEN" | "ASSIGNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED"
export type WorkOrderPriority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL"
export type AuditEventType =
  | "WORK_ORDER_CREATED"
  | "TECHNICIAN_ASSIGNED"
  | "WORK_ORDER_STATUS_CHANGED"
  | "WORK_ORDER_COMMENT_ADDED"

// ── Technician ───────────────────────────────────────────────

export interface TechnicianSkillResponse {
  id: string
  skillName: string
  proficiencyLevel: ProficiencyLevel
  createdAt: string
}

export interface TechnicianResponse {
  id: string
  employeeCode: string
  email: string
  firstName: string
  lastName: string
  countryCode: string
  region: string | null
  availability: AvailabilityStatus
  active: boolean
  skills: TechnicianSkillResponse[]
  createdAt: string
  updatedAt: string
}

export interface TechnicianSummaryResponse {
  id: string
  employeeCode: string
  email: string
  firstName: string
  lastName: string
  countryCode: string
  region: string | null
  availability: AvailabilityStatus
  active: boolean
  createdAt: string
}

export interface CreateTechnicianRequest {
  employeeCode: string
  email: string
  firstName: string
  lastName: string
  countryCode: string
  region?: string
  skills?: CreateTechnicianSkillRequest[]
}

export interface CreateTechnicianSkillRequest {
  skillName: string
  proficiencyLevel?: ProficiencyLevel
}

export interface ChangeAvailabilityRequest {
  availability: AvailabilityStatus
}

export interface ChangeActivationRequest {
  active: boolean
}

// ── Work Order ───────────────────────────────────────────────

export interface WorkOrderResponse {
  id: string
  title: string
  summary: string
  countryCode: string
  region: string | null
  priority: WorkOrderPriority
  status: WorkOrderStatus
  assignedTechnicianId: string | null
  comments: WorkOrderCommentResponse[]
  statusHistory: WorkOrderStatusHistoryResponse[]
  createdAt: string
  updatedAt: string
}

export interface WorkOrderSummaryResponse {
  id: string
  title: string
  countryCode: string
  region: string | null
  priority: WorkOrderPriority
  status: WorkOrderStatus
  assignedTechnicianId: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateWorkOrderRequest {
  title: string
  summary: string
  countryCode: string
  region?: string
  priority: WorkOrderPriority
}

export interface ChangeWorkOrderStatusRequest {
  targetStatus: WorkOrderStatus
  changedBy: string
  reason?: string
}

export interface AddWorkOrderCommentRequest {
  authorName: string
  body: string
}

export interface WorkOrderCommentResponse {
  id: string
  authorName: string | null
  body: string
  createdAt: string
}

export interface WorkOrderStatusHistoryResponse {
  id: string
  fromStatus: WorkOrderStatus | null
  toStatus: WorkOrderStatus
  changedBy: string | null
  changeReason: string | null
  changedAt: string
}

// ── Audit ────────────────────────────────────────────────────

export interface AuditEventResponse {
  id: string
  eventType: AuditEventType
  serviceName: string | null
  entityType: string
  entityId: string
  actor: string | null
  details: string | null
  correlationId: string | null
  createdAt: string
}

// ── API Error ────────────────────────────────────────────────

export interface ApiError {
  timestamp: string
  status: number
  error: string
  code: string
  message: string
  path: string
  correlationId: string
  details: Record<string, string> | null
}

// ── Pagination ───────────────────────────────────────────────

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}
