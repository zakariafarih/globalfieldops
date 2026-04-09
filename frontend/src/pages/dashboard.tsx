import { useQuery } from "@tanstack/react-query"
import { technicianApi } from "@/services/technician-service"
import { workOrderApi } from "@/services/work-order-service"
import { auditApi } from "@/services/audit-service"
import { useAppAuth } from "@/hooks/use-app-auth"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Users, ClipboardList, ScrollText, Activity } from "lucide-react"
import type { WorkOrderStatus, AvailabilityStatus, TechnicianResponse, WorkOrderResponse, AuditEventResponse } from "@/types/api"

function StatCard({
  title,
  value,
  description,
  icon: Icon,
  loading,
}: {
  title: string
  value: string | number
  description?: string
  icon: React.ComponentType<{ className?: string }>
  loading?: boolean
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <CardTitle className="text-sm font-medium">{title}</CardTitle>
        <Icon className="size-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        {loading ? (
          <Skeleton className="h-8 w-20" />
        ) : (
          <div className="text-2xl font-bold">{value}</div>
        )}
        {description && (
          <p className="text-xs text-muted-foreground">{description}</p>
        )}
      </CardContent>
    </Card>
  )
}

const statusColors: Record<WorkOrderStatus, string> = {
  OPEN: "default",
  ASSIGNED: "secondary",
  IN_PROGRESS: "outline",
  COMPLETED: "default",
  CANCELLED: "destructive",
} as unknown as Record<WorkOrderStatus, string>

const availabilityColors: Record<AvailabilityStatus, string> = {
  AVAILABLE: "default",
  ON_JOB: "secondary",
  OFF_DUTY: "destructive",
}

export default function DashboardPage() {
  const { user, hasRole } = useAppAuth()

  const technicians = useQuery({
    queryKey: ["technicians"],
    queryFn: () => technicianApi.list({ size: "100" }),
  })

  const workOrders = useQuery({
    queryKey: ["workOrders"],
    queryFn: () => workOrderApi.list({ size: "100" }),
  })

  const auditEvents = useQuery({
    queryKey: ["auditEvents"],
    queryFn: () => auditApi.list({ size: "10", sort: "createdAt,desc" }),
    enabled: hasRole("ADMIN"),
  })

  const techData = technicians.data?.content ?? []
  const woData = workOrders.data?.content ?? []
  const auditData = auditEvents.data?.content ?? []

  const activeTechs = techData.filter((t: TechnicianResponse) => t.active).length
  const availableTechs = techData.filter((t: TechnicianResponse) => t.availability === "AVAILABLE").length
  const openOrders = woData.filter((w: WorkOrderResponse) => w.status === "OPEN").length
  const inProgressOrders = woData.filter((w: WorkOrderResponse) => w.status === "IN_PROGRESS").length

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-muted-foreground">
          Welcome back, {user?.name}. Here's your operations overview.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total Technicians"
          value={techData.length}
          description={`${activeTechs} active, ${availableTechs} available`}
          icon={Users}
          loading={technicians.isLoading}
        />
        <StatCard
          title="Work Orders"
          value={woData.length}
          description={`${openOrders} open, ${inProgressOrders} in progress`}
          icon={ClipboardList}
          loading={workOrders.isLoading}
        />
        <StatCard
          title="Open Orders"
          value={openOrders}
          description="Awaiting assignment"
          icon={Activity}
          loading={workOrders.isLoading}
        />
        {hasRole("ADMIN") && (
          <StatCard
            title="Audit Events"
            value={auditEvents.data?.totalElements ?? 0}
            description="Total tracked events"
            icon={ScrollText}
            loading={auditEvents.isLoading}
          />
        )}
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Recent Work Orders */}
        <Card>
          <CardHeader>
            <CardTitle>Recent Work Orders</CardTitle>
            <CardDescription>Latest work orders across all teams</CardDescription>
          </CardHeader>
          <CardContent>
            {workOrders.isLoading ? (
              <div className="flex flex-col gap-3">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Skeleton key={i} className="h-10 w-full" />
                ))}
              </div>
            ) : woData.length === 0 ? (
              <p className="text-sm text-muted-foreground">No work orders yet.</p>
            ) : (
              <div className="flex flex-col gap-3">
                {woData.slice(0, 8).map((wo: WorkOrderResponse) => (
                  <div
                    key={wo.id}
                    className="flex items-center justify-between rounded-lg border p-3"
                  >
                    <div className="flex min-w-0 flex-col gap-1">
                      <span className="truncate text-sm font-medium">
                        {wo.title}
                      </span>
                      <div className="flex items-center gap-2">
                        <Badge
                          variant={
                            statusColors[wo.status] as
                              | "default"
                              | "secondary"
                              | "outline"
                              | "destructive"
                          }
                          className="text-[10px]"
                        >
                          {wo.status}
                        </Badge>
                        <span className="text-xs text-muted-foreground">
                          {wo.countryCode} · {wo.priority}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Technician Availability */}
        <Card>
          <CardHeader>
            <CardTitle>Technician Availability</CardTitle>
            <CardDescription>Current field team status</CardDescription>
          </CardHeader>
          <CardContent>
            {technicians.isLoading ? (
              <div className="flex flex-col gap-3">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Skeleton key={i} className="h-10 w-full" />
                ))}
              </div>
            ) : techData.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                No technicians registered.
              </p>
            ) : (
              <div className="flex flex-col gap-3">
                {techData.slice(0, 8).map((t: TechnicianResponse) => (
                  <div
                    key={t.id}
                    className="flex items-center justify-between rounded-lg border p-3"
                  >
                    <div className="flex items-center gap-3">
                      <div className="flex flex-col">
                        <span className="text-sm font-medium">
                          {t.firstName} {t.lastName}
                        </span>
                        <span className="text-xs text-muted-foreground">
                          {t.employeeCode} · {t.countryCode}
                        </span>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge
                        variant={
                          availabilityColors[t.availability] as
                            | "default"
                            | "secondary"
                            | "outline"
                            | "destructive"
                        }
                        className="text-[10px]"
                      >
                        {t.availability}
                      </Badge>
                      {!t.active && (
                        <Badge variant="destructive" className="text-[10px]">
                          INACTIVE
                        </Badge>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Audit Log Preview — admin only */}
        {hasRole("ADMIN") && (
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Recent Audit Events</CardTitle>
              <CardDescription>Last 10 system events</CardDescription>
            </CardHeader>
            <CardContent>
              {auditEvents.isLoading ? (
                <div className="flex flex-col gap-3">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <Skeleton key={i} className="h-8 w-full" />
                  ))}
                </div>
              ) : auditData.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  No audit events recorded.
                </p>
              ) : (
                <div className="flex flex-col gap-2">
                  {auditData.map((e: AuditEventResponse) => (
                    <div
                      key={e.id}
                      className="flex items-center justify-between rounded border px-3 py-2 text-sm"
                    >
                      <div className="flex items-center gap-3">
                        <Badge variant="outline" className="text-[10px]">
                          {e.eventType}
                        </Badge>
                        <span className="text-muted-foreground">
                          {e.entityType}:{e.entityId.slice(0, 8)}…
                        </span>
                      </div>
                      <span className="text-xs text-muted-foreground">
                        {new Date(e.createdAt).toLocaleString()}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  )
}
