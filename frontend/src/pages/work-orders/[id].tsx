import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { useParams, useNavigate } from "react-router"
import { workOrderApi } from "@/services/work-order-service"
import { technicianApi } from "@/services/technician-service"
import { useAppAuth } from "@/hooks/use-app-auth"
import { useState } from "react"
import { toast } from "sonner"
import type { WorkOrderStatus, TechnicianResponse } from "@/types/api"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { Textarea } from "@/components/ui/textarea"
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Skeleton } from "@/components/ui/skeleton"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { ArrowLeft, MessageSquarePlus, UserPlus } from "lucide-react"

const VALID_TRANSITIONS: Record<WorkOrderStatus, WorkOrderStatus[]> = {
  OPEN: ["CANCELLED"],
  ASSIGNED: ["IN_PROGRESS", "CANCELLED"],
  IN_PROGRESS: ["COMPLETED", "CANCELLED"],
  COMPLETED: [],
  CANCELLED: [],
}

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

export default function WorkOrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { hasAnyRole } = useAppAuth()
  const queryClient = useQueryClient()

  const [commentOpen, setCommentOpen] = useState(false)
  const [assignOpen, setAssignOpen] = useState(false)
  const [selectedTechId, setSelectedTechId] = useState<string>("")

  const { data: wo, isLoading } = useQuery({
    queryKey: ["workOrder", id],
    queryFn: () => workOrderApi.getById(id!),
    enabled: !!id,
  })

  const { data: techniciansPage } = useQuery({
    queryKey: ["technicians"],
    queryFn: () => technicianApi.list({ size: "100" }),
    enabled: assignOpen,
  })

  const statusMutation = useMutation({
    mutationFn: (targetStatus: WorkOrderStatus) =>
      workOrderApi.changeStatus(id!, { targetStatus, changedBy: "dispatcher" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["workOrder", id] })
      queryClient.invalidateQueries({ queryKey: ["workOrders"] })
      toast.success("Status updated")
    },
    onError: () => toast.error("Failed to update status"),
  })

  const commentMutation = useMutation({
    mutationFn: (body: string) => workOrderApi.addComment(id!, { authorName: "dispatcher", body }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["workOrder", id] })
      setCommentOpen(false)
      toast.success("Comment added")
    },
    onError: () => toast.error("Failed to add comment"),
  })

  const assignMutation = useMutation({
    mutationFn: (technicianId: string) =>
      workOrderApi.assign(id!, technicianId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["workOrder", id] })
      queryClient.invalidateQueries({ queryKey: ["workOrders"] })
      setAssignOpen(false)
      toast.success("Technician assigned successfully")
    },
    onError: () => toast.error("Assignment failed — check eligibility"),
  })

  if (isLoading) {
    return (
      <div className="flex flex-col gap-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    )
  }

  if (!wo) {
    return (
      <div className="py-16 text-center text-muted-foreground">
        Work order not found.
      </div>
    )
  }

  const transitions = VALID_TRANSITIONS[wo.status]
  const canWrite = hasAnyRole("DISPATCHER", "ADMIN")
  const availableTechnicians = (techniciansPage?.content ?? []).filter(
    (t: TechnicianResponse) =>
      t.active &&
      t.availability === "AVAILABLE" &&
      t.countryCode === wo.countryCode
  )

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => navigate("/work-orders")}
        >
          <ArrowLeft />
        </Button>
        <div>
          <h1 className="text-2xl font-bold tracking-tight">{wo.title}</h1>
          <p className="text-muted-foreground">
            {wo.countryCode} ·{" "}
            <Badge variant={statusVariant[wo.status]}>{wo.status}</Badge>
          </p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Main Info */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Details</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <Row label="ID" value={wo.id} />
            <Row label="Priority" value={wo.priority} />
            <Row label="Country" value={wo.countryCode} />
            <Row
              label="Technician"
              value={wo.assignedTechnicianId ? wo.assignedTechnicianId.slice(0, 8) + "…" : "Unassigned"}
            />
            <Row
              label="Created"
              value={new Date(wo.createdAt).toLocaleString()}
            />
            <Row
              label="Updated"
              value={new Date(wo.updatedAt).toLocaleString()}
            />
            {wo.summary && (
              <>
                <Separator />
                <div>
                  <span className="text-sm font-medium">Summary</span>
                  <p className="mt-1 text-sm text-muted-foreground">
                    {wo.summary}
                  </p>
                </div>
              </>
            )}
          </CardContent>
        </Card>

        {/* Actions */}
        {canWrite && (
          <Card>
            <CardHeader>
              <CardTitle>Actions</CardTitle>
              <CardDescription>Manage this work order</CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-3">
              {/* Assign Technician */}
              {wo.status === "OPEN" && (
                <Dialog open={assignOpen} onOpenChange={setAssignOpen}>
                  <DialogTrigger asChild>
                    <Button className="w-full">
                      <UserPlus data-icon="inline-start" />
                      Assign Technician
                    </Button>
                  </DialogTrigger>
                  <DialogContent>
                    <DialogHeader>
                      <DialogTitle>Assign Technician</DialogTitle>
                      <DialogDescription>
                        Select an available technician in{" "}
                        {wo.countryCode}
                      </DialogDescription>
                    </DialogHeader>
                    {availableTechnicians.length === 0 ? (
                      <p className="py-4 text-center text-sm text-muted-foreground">
                        No available technicians in {wo.countryCode}.
                      </p>
                    ) : (
                      <Select
                        value={selectedTechId}
                        onValueChange={setSelectedTechId}
                      >
                        <SelectTrigger>
                          <SelectValue placeholder="Choose technician…" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectGroup>
                            {availableTechnicians.map((t: TechnicianResponse) => (
                              <SelectItem key={t.id} value={t.id}>
                                {t.firstName} {t.lastName} ({t.employeeCode})
                              </SelectItem>
                            ))}
                          </SelectGroup>
                        </SelectContent>
                      </Select>
                    )}
                    <DialogFooter>
                      <Button
                        disabled={
                          !selectedTechId || assignMutation.isPending
                        }
                        onClick={() => assignMutation.mutate(selectedTechId)}
                      >
                        {assignMutation.isPending
                          ? "Assigning…"
                          : "Assign"}
                      </Button>
                    </DialogFooter>
                  </DialogContent>
                </Dialog>
              )}

              {/* Status Transitions */}
              {transitions.length > 0 && (
                <div className="flex flex-col gap-2">
                  <span className="text-sm font-medium">Change Status:</span>
                  {transitions.map((status) => (
                    <Button
                      key={status}
                      variant={
                        status === "CANCELLED" ? "destructive" : "secondary"
                      }
                      className="w-full"
                      onClick={() => statusMutation.mutate(status)}
                      disabled={statusMutation.isPending}
                    >
                      → {status}
                    </Button>
                  ))}
                </div>
              )}

              {/* Add Comment */}
              <Dialog open={commentOpen} onOpenChange={setCommentOpen}>
                <DialogTrigger asChild>
                  <Button variant="outline" className="w-full">
                    <MessageSquarePlus data-icon="inline-start" />
                    Add Comment
                  </Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>Add Comment</DialogTitle>
                    <DialogDescription>
                      Leave a note on this work order
                    </DialogDescription>
                  </DialogHeader>
                  <form
                    onSubmit={(e) => {
                      e.preventDefault()
                      const body = new FormData(e.currentTarget).get(
                        "body"
                      ) as string
                      if (body.trim()) commentMutation.mutate(body)
                    }}
                    className="flex flex-col gap-4"
                  >
                    <Textarea
                      name="body"
                      placeholder="Write a comment…"
                      rows={3}
                      required
                    />
                    <DialogFooter>
                      <Button
                        type="submit"
                        disabled={commentMutation.isPending}
                      >
                        {commentMutation.isPending ? "Posting…" : "Post"}
                      </Button>
                    </DialogFooter>
                  </form>
                </DialogContent>
              </Dialog>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Tabs: Comments + Status History */}
      <Tabs defaultValue="comments">
        <TabsList>
          <TabsTrigger value="comments">
            Comments ({wo.comments.length})
          </TabsTrigger>
          <TabsTrigger value="history">
            Status History ({wo.statusHistory.length})
          </TabsTrigger>
        </TabsList>

        <TabsContent value="comments" className="mt-4">
          {wo.comments.length === 0 ? (
            <p className="text-sm text-muted-foreground">No comments yet.</p>
          ) : (
            <div className="flex flex-col gap-3">
              {wo.comments.map((c) => (
                <Card key={c.id}>
                  <CardContent className="pt-4">
                    <p className="text-sm">{c.body}</p>
                    <p className="mt-2 text-xs text-muted-foreground">
                      {c.authorName ?? "System"}{" "}
                      · {new Date(c.createdAt).toLocaleString()}
                    </p>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </TabsContent>

        <TabsContent value="history" className="mt-4">
          {wo.statusHistory.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              No status changes recorded.
            </p>
          ) : (
            <div className="flex flex-col gap-2">
              {wo.statusHistory.map((h) => (
                <div
                  key={h.id}
                  className="flex items-center gap-3 rounded border px-3 py-2 text-sm"
                >
                  <Badge variant="outline">
                    {h.fromStatus ?? "—"}
                  </Badge>
                  <span>→</span>
                  <Badge variant={statusVariant[h.toStatus]}>
                    {h.toStatus}
                  </Badge>
                  {h.changedBy && (
                    <span className="text-xs text-muted-foreground">
                      by {h.changedBy}
                    </span>
                  )}
                  <span className="ml-auto text-xs text-muted-foreground">
                    {new Date(h.changedAt).toLocaleString()}
                  </span>
                </div>
              ))}
            </div>
          )}
        </TabsContent>
      </Tabs>
    </div>
  )
}

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between">
      <span className="text-sm text-muted-foreground">{label}</span>
      <span className="text-sm font-medium">{value}</span>
    </div>
  )
}
