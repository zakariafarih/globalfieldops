import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { useParams, useNavigate } from "react-router"
import { technicianApi } from "@/services/technician-service"
import { useAppAuth } from "@/hooks/use-app-auth"
import { toast } from "sonner"
import type { AvailabilityStatus } from "@/types/api"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { ArrowLeft, Power, PowerOff } from "lucide-react"

const AVAILABILITY_OPTIONS: AvailabilityStatus[] = [
  "AVAILABLE",
  "ON_JOB",
  "OFF_DUTY",
]

export default function TechnicianDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { hasRole } = useAppAuth()
  const queryClient = useQueryClient()

  const { data: tech, isLoading } = useQuery({
    queryKey: ["technician", id],
    queryFn: () => technicianApi.getById(id!),
    enabled: !!id,
  })

  const availabilityMutation = useMutation({
    mutationFn: (availability: AvailabilityStatus) =>
      technicianApi.changeAvailability(id!, { availability }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["technician", id] })
      queryClient.invalidateQueries({ queryKey: ["technicians"] })
      toast.success("Availability updated")
    },
    onError: () => toast.error("Failed to update availability"),
  })

  const activationMutation = useMutation({
    mutationFn: (active: boolean) =>
      technicianApi.changeActivation(id!, { active }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["technician", id] })
      queryClient.invalidateQueries({ queryKey: ["technicians"] })
      toast.success("Activation status updated")
    },
    onError: () => toast.error("Failed to update activation"),
  })

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  if (!tech) {
    return (
      <div className="py-16 text-center text-muted-foreground">
        Technician not found.
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate("/technicians")}>
          <ArrowLeft />
        </Button>
        <div>
          <h1 className="text-2xl font-bold tracking-tight">
            {tech.firstName} {tech.lastName}
          </h1>
          <p className="text-muted-foreground">
            {tech.employeeCode} · {tech.email}
          </p>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Details</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <Row label="Employee Code" value={tech.employeeCode} />
            <Row label="Email" value={tech.email} />
            <Row label="Country" value={tech.countryCode} />
            <Row label="Region" value={tech.region ?? "—"} />
            <Row
              label="Status"
              value={
                <Badge variant={tech.active ? "default" : "destructive"}>
                  {tech.active ? "Active" : "Inactive"}
                </Badge>
              }
            />
            <Row
              label="Availability"
              value={<Badge variant="secondary">{tech.availability}</Badge>}
            />
            <Row
              label="Created"
              value={new Date(tech.createdAt).toLocaleString()}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Skills</CardTitle>
          </CardHeader>
          <CardContent>
            {tech.skills.length === 0 ? (
              <p className="text-sm text-muted-foreground">No skills recorded.</p>
            ) : (
              <div className="flex flex-col gap-2">
                {tech.skills.map((s) => (
                  <div
                    key={s.id}
                    className="flex items-center justify-between rounded border px-3 py-2"
                  >
                    <span className="text-sm font-medium">{s.skillName}</span>
                    <Badge variant="outline">{s.proficiencyLevel}</Badge>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {hasRole("ADMIN") && (
        <>
          <Separator />
          <Card>
            <CardHeader>
              <CardTitle>Actions</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <div className="flex items-center gap-4">
                <span className="text-sm font-medium">Change Availability:</span>
                <Select
                  value={tech.availability}
                  onValueChange={(v) =>
                    availabilityMutation.mutate(v as AvailabilityStatus)
                  }
                  disabled={availabilityMutation.isPending}
                >
                  <SelectTrigger className="w-48">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectGroup>
                      {AVAILABILITY_OPTIONS.map((opt) => (
                        <SelectItem key={opt} value={opt}>
                          {opt}
                        </SelectItem>
                      ))}
                    </SelectGroup>
                  </SelectContent>
                </Select>
              </div>

              <div className="flex items-center gap-4">
                <span className="text-sm font-medium">Activation:</span>
                <Button
                  variant={tech.active ? "destructive" : "default"}
                  onClick={() => activationMutation.mutate(!tech.active)}
                  disabled={activationMutation.isPending}
                >
                  {tech.active ? (
                    <>
                      <PowerOff data-icon="inline-start" />
                      Deactivate
                    </>
                  ) : (
                    <>
                      <Power data-icon="inline-start" />
                      Activate
                    </>
                  )}
                </Button>
              </div>
            </CardContent>
          </Card>
        </>
      )}
    </div>
  )
}

function Row({
  label,
  value,
}: {
  label: string
  value: React.ReactNode
}) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className="text-sm font-medium">{value}</span>
    </div>
  )
}
