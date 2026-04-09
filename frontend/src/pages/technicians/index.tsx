import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { technicianApi } from "@/services/technician-service"
import { useAppAuth } from "@/hooks/use-app-auth"
import { useNavigate } from "react-router"
import { useState } from "react"
import { toast } from "sonner"
import type {
  TechnicianResponse,
  CreateTechnicianRequest,
  AvailabilityStatus,
  ProficiencyLevel,
} from "@/types/api"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Plus, Eye } from "lucide-react"

const availabilityVariant: Record<
  AvailabilityStatus,
  "default" | "secondary" | "outline" | "destructive"
> = {
  AVAILABLE: "default",
  ON_JOB: "secondary",
  OFF_DUTY: "destructive",
}

export default function TechniciansPage() {
  const { hasRole } = useAppAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)

  const { data, isLoading } = useQuery({
    queryKey: ["technicians"],
    queryFn: () => technicianApi.list({ size: "100" }),
  })

  const createMutation = useMutation({
    mutationFn: (req: CreateTechnicianRequest) => technicianApi.create(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["technicians"] })
      setOpen(false)
      toast.success("Technician created successfully")
    },
    onError: () => {
      toast.error("Failed to create technician")
    },
  })

  const technicians = data?.content ?? []

  function handleCreate(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = new FormData(e.currentTarget)
    const skillName = form.get("skillName") as string
    const proficiency = form.get("proficiency") as ProficiencyLevel

    const req: CreateTechnicianRequest = {
      employeeCode: form.get("employeeCode") as string,
      email: form.get("email") as string,
      firstName: form.get("firstName") as string,
      lastName: form.get("lastName") as string,
      countryCode: form.get("countryCode") as string,
      region: (form.get("region") as string) || undefined,
      skills:
        skillName && proficiency
          ? [{ skillName, proficiencyLevel: proficiency }]
          : undefined,
    }
    createMutation.mutate(req)
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Technicians</h1>
          <p className="text-muted-foreground">
            Manage field technicians across all regions
          </p>
        </div>
        {hasRole("ADMIN") && (
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus data-icon="inline-start" />
                Add Technician
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Create Technician</DialogTitle>
                <DialogDescription>
                  Register a new field technician
                </DialogDescription>
              </DialogHeader>
              <form onSubmit={handleCreate} className="flex flex-col gap-4">
                <div className="grid grid-cols-2 gap-4">
                  <Input
                    name="firstName"
                    placeholder="First name"
                    required
                  />
                  <Input name="lastName" placeholder="Last name" required />
                </div>
                <Input
                  name="employeeCode"
                  placeholder="Employee code (e.g. EMP-001)"
                  required
                />
                <Input
                  name="email"
                  type="email"
                  placeholder="Email"
                  required
                />
                <div className="grid grid-cols-2 gap-4">
                  <Input
                    name="countryCode"
                    placeholder="Country (e.g. US)"
                    maxLength={3}
                    required
                  />
                  <Input name="region" placeholder="Region (optional)" />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <Input name="skillName" placeholder="Skill (optional)" />
                  <Select name="proficiency">
                    <SelectTrigger>
                      <SelectValue placeholder="Proficiency" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectGroup>
                        <SelectItem value="JUNIOR">Junior</SelectItem>
                        <SelectItem value="MID">Mid</SelectItem>
                        <SelectItem value="SENIOR">Senior</SelectItem>
                        <SelectItem value="EXPERT">Expert</SelectItem>
                      </SelectGroup>
                    </SelectContent>
                  </Select>
                </div>
                <DialogFooter>
                  <Button
                    type="submit"
                    disabled={createMutation.isPending}
                  >
                    {createMutation.isPending ? "Creating…" : "Create"}
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
        )}
      </div>

      {isLoading ? (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : technicians.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <p className="text-muted-foreground">
            No technicians registered yet.
          </p>
        </div>
      ) : (
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Employee</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Country</TableHead>
                <TableHead>Availability</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="w-[50px]" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {technicians.map((t: TechnicianResponse) => (
                <TableRow key={t.id}>
                  <TableCell className="font-mono text-sm">
                    {t.employeeCode}
                  </TableCell>
                  <TableCell>
                    {t.firstName} {t.lastName}
                  </TableCell>
                  <TableCell>{t.countryCode}</TableCell>
                  <TableCell>
                    <Badge variant={availabilityVariant[t.availability]}>
                      {t.availability}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant={t.active ? "default" : "destructive"}
                    >
                      {t.active ? "Active" : "Inactive"}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => navigate(`/technicians/${t.id}`)}
                    >
                      <Eye />
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  )
}
