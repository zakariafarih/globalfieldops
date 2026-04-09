import { useAppAuth } from "@/hooks/use-app-auth"
import { Navigate, Outlet } from "react-router"
import { Skeleton } from "@/components/ui/skeleton"

export default function AuthGuard() {
  const { isAuthenticated, isLoading } = useAppAuth()

  if (isLoading) {
    return (
      <div className="flex min-h-svh items-center justify-center">
        <div className="flex flex-col items-center gap-4">
          <Skeleton className="size-12 rounded-full" />
          <Skeleton className="h-4 w-48" />
          <p className="text-sm text-muted-foreground">Authenticating…</p>
        </div>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}
