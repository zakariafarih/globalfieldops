import api from "@/lib/api-client"
import type { AuditEventResponse, Page } from "@/types/api"

const BASE = "/api/audit-events"

export const auditApi = {
  list: async (params?: Record<string, string>) => {
    const { data } = await api.get<Page<AuditEventResponse>>(BASE, { params })
    return data
  },
}
