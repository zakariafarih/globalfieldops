import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { workOrderApi } from "@/services/work-order-service"
import { useAppAuth } from "@/hooks/use-app-auth"
import { useNavigate } from "react-router"
import { useState } from "react"
import { toast } from "sonner"
import type {
  WorkOrderResponse,
  CreateWorkOrderRequest,
  WorkOrderPriority,
  WorkOrderStatus,
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
import { Textarea } from "@/components/ui/textarea"
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

const statusVariant: Record<
  WorkOrderStatus,
  "default" | "secondary" | "outline" | "destructive"
> = {
  OPEN: "default",
  ASSIGNED: "secondary",
  IN_PROGRESS: "outline",
  COMPLETED: "default",
  CANCELLED: "destructive",
}

const priorityVariant: Record<
  WorkOrderPriority,
  "default" | "secondary" | "outline" | "destructive"
> = {
  LOW: "outline",
  MEDIUM: "secondary",
  HIGH: "default",
  CRITICAL: "destructive",
}

export default function WorkOrdersPage() {
  const { hasAnyRole } = useAppAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [priority, setPriority] = useState<WorkOrderPriority>("MEDIUM")

  const { data, isLoading } = useQuery({
    queryKey: ["workOrders"],
    queryFn: () => workOrderApi.list({ size: "100" }),
  })

  const createMutation = useMutation({
    mutationFn: (req: CreateWorkOrderRequest) => workOrderApi.create(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["workOrders"] })
      setOpen(false)
      toast.success("Work order created successfully")
    },
    onError: () => {
      toast.error("Failed to create work order")
    },
  })

  const workOrders = data?.content ?? []

  function handleCreate(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const form = new FormData(e.currentTarget)
    const req: CreateWorkOrderRequest = {
      title: form.get("title") as string,
      summary: form.get("summary") as string,
      countryCode: form.get("countryCode") as string,
      region: (form.get("region") as string) || undefined,
      priority,
    }
    createMutation.mutate(req)
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Work Orders</h1>
          <p className="text-muted-foreground">
            Manage and track field work orders
          </p>
        </div>
        {hasAnyRole("DISPATCHER", "ADMIN") && (
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus data-icon="inline-start" />
                New Work Order
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Create Work Order</DialogTitle>
                <DialogDescription>
                  Create a new field work order
                </DialogDescription>
              </DialogHeader>
              <form onSubmit={handleCreate} className="flex flex-col gap-4">
                <Input name="title" placeholder="Title" required />
                <Textarea
                  name="summary"
                  placeholder="Summary"
                  rows={3}
                  required
                />
                <div className="grid grid-cols-2 gap-4">
                  <Input
                    name="countryCode"
                    placeholder="Country (e.g. US)"
                    maxLength={2}
                    required
                  />
                  <Input name="region" placeholder="Region (optional)" />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <Select
                    value={priority}
                    onValueChange={(v) => setPriority(v as WorkOrderPriority)}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Priority" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectGroup>
                        <SelectItem value="LOW">Low</SelectItem>
                        <SelectItem value="MEDIUM">Medium</SelectItem>
                        <SelectItem value="HIGH">High</SelectItem>
                        <SelectItem value="CRITICAL">Critical</SelectItem>
                      </SelectGroup>
                    </SelectContent>
                  </Select>
                </div>
                <DialogFooter>
                  <Button type="submit" disabled={createMutation.isPending}>
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
      ) : workOrders.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <p className="text-muted-foreground">No work orders yet.</p>
        </div>
      ) : (
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Title</TableHead>
                <TableHead>Country</TableHead>
                <TableHead>Priority</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Assigned</TableHead>
                <TableHead>Created</TableHead>
                <TableHead className="w-[50px]" />
              </TableRow>
            </TableHeader>
            <TableBody>
              {workOrders.map((wo: WorkOrderResponse) => (
                <TableRow key={wo.id}>
                  <TableCell className="font-medium">{wo.title}</TableCell>
                  <TableCell>{wo.countryCode}</TableCell>
                  <TableCell>
                    <Badge variant={priorityVariant[wo.priority]}>
                      {wo.priority}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Badge variant={statusVariant[wo.status]}>
                      {wo.status}
                    </Badge>
                  </TableCell>
                  <TableCell className="font-mono text-xs">
                    {wo.assignedTechnicianId ? wo.assignedTechnicianId.slice(0, 8) + "…" : "—"}
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground">
                    {new Date(wo.createdAt).toLocaleDateString()}
                  </TableCell>
                  <TableCell>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => navigate(`/work-orders/${wo.id}`)}
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
