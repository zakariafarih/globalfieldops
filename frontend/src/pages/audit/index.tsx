import { useQuery } from "@tanstack/react-query"
import { auditApi } from "@/services/audit-service"
import type { AuditEventResponse } from "@/types/api"
import { Badge } from "@/components/ui/badge"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"

export default function AuditPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["auditEvents"],
    queryFn: () => auditApi.list({ size: "100", sort: "createdAt,desc" }),
  })

  const events = data?.content ?? []

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Audit Log</h1>
        <p className="text-muted-foreground">
          Immutable record of all operational events
        </p>
      </div>

      {isLoading ? (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </div>
      ) : events.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 text-center">
          <p className="text-muted-foreground">No audit events recorded.</p>
        </div>
      ) : (
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Event Type</TableHead>
                <TableHead>Service</TableHead>
                <TableHead>Entity</TableHead>
                <TableHead>Entity ID</TableHead>
                <TableHead>Actor</TableHead>
                <TableHead>Details</TableHead>
                <TableHead>Correlation ID</TableHead>
                <TableHead>Timestamp</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {events.map((e: AuditEventResponse) => (
                <TableRow key={e.id}>
                  <TableCell>
                    <Badge variant="outline">{e.eventType}</Badge>
                  </TableCell>
                  <TableCell className="text-sm">{e.serviceName ?? "—"}</TableCell>
                  <TableCell className="text-sm">{e.entityType}</TableCell>
                  <TableCell className="font-mono text-xs">
                    {e.entityId.slice(0, 8)}…
                  </TableCell>
                  <TableCell className="text-sm">
                    {e.actor ?? "—"}
                  </TableCell>
                  <TableCell className="max-w-[200px] truncate text-xs">
                    {e.details ?? "—"}
                  </TableCell>
                  <TableCell className="font-mono text-xs">
                    {e.correlationId
                      ? `${e.correlationId.slice(0, 8)}…`
                      : "—"}
                  </TableCell>
                  <TableCell className="text-xs text-muted-foreground">
                    {new Date(e.createdAt).toLocaleString()}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {data && (
        <p className="text-xs text-muted-foreground">
          Showing {events.length} of {data.totalElements} events
        </p>
      )}
    </div>
  )
}
