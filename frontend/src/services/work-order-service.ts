import api from "@/lib/api-client"
import type {
  WorkOrderResponse,
  CreateWorkOrderRequest,
  ChangeWorkOrderStatusRequest,
  AddWorkOrderCommentRequest,
  Page,
} from "@/types/api"

const BASE = "/api/work-orders"

export const workOrderApi = {
  list: async (params?: Record<string, string>) => {
    const { data } = await api.get<Page<WorkOrderResponse>>(BASE, { params })
    return data
  },

  getById: async (id: string) => {
    const { data } = await api.get<WorkOrderResponse>(`${BASE}/${id}`)
    return data
  },

  create: async (req: CreateWorkOrderRequest) => {
    const { data } = await api.post<WorkOrderResponse>(BASE, req)
    return data
  },

  changeStatus: async (id: string, req: ChangeWorkOrderStatusRequest) => {
    const { data } = await api.post<WorkOrderResponse>(
      `${BASE}/${id}/status`,
      req
    )
    return data
  },

  addComment: async (id: string, req: AddWorkOrderCommentRequest) => {
    const { data } = await api.post<WorkOrderResponse>(
      `${BASE}/${id}/comments`,
      req
    )
    return data
  },

  assign: async (workOrderId: string, technicianId: string) => {
    const { data } = await api.post<WorkOrderResponse>(
      `${BASE}/${workOrderId}/assign/${technicianId}`
    )
    return data
  },
}
