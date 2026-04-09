import api from "@/lib/api-client"
import type {
  TechnicianResponse,
  CreateTechnicianRequest,
  ChangeAvailabilityRequest,
  ChangeActivationRequest,
  Page,
} from "@/types/api"

const BASE = "/api/technicians"

export const technicianApi = {
  list: async (params?: Record<string, string>) => {
    const { data } = await api.get<Page<TechnicianResponse>>(BASE, { params })
    return data
  },

  getById: async (id: string) => {
    const { data } = await api.get<TechnicianResponse>(`${BASE}/${id}`)
    return data
  },

  create: async (req: CreateTechnicianRequest) => {
    const { data } = await api.post<TechnicianResponse>(BASE, req)
    return data
  },

  changeAvailability: async (id: string, req: ChangeAvailabilityRequest) => {
    const { data } = await api.patch<TechnicianResponse>(
      `${BASE}/${id}/availability`,
      req
    )
    return data
  },

  changeActivation: async (id: string, req: ChangeActivationRequest) => {
    const { data } = await api.patch<TechnicianResponse>(
      `${BASE}/${id}/activation`,
      req
    )
    return data
  },
}
