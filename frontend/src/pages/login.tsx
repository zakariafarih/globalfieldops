import { useAppAuth } from "@/hooks/use-app-auth"
import { Navigate } from "react-router"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { ShieldCheck } from "lucide-react"

export default function LoginPage() {
  const { isAuthenticated, login, isLoading, error } = useAppAuth()

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  return (
    <div className="flex min-h-svh items-center justify-center bg-background p-4">
      <Card className="w-full max-w-sm">
        <CardHeader className="text-center">
          <div className="mx-auto mb-2 flex size-12 items-center justify-center rounded-full bg-primary/10">
            <ShieldCheck className="size-6 text-primary" />
          </div>
          <CardTitle className="text-2xl">GlobalField Ops</CardTitle>
          <CardDescription>
            Sign in to access the field operations platform
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {!!error && (
            <div className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">
              <p className="font-medium">Auth Error</p>
              <p>{String(error)}</p>
            </div>
          )}
          <Button
            className="w-full"
            size="lg"
            onClick={login}
            disabled={isLoading}
          >
            {isLoading ? "Redirecting…" : "Sign in with Keycloak"}
          </Button>
          <p className="text-center text-xs text-muted-foreground">
            Multinational Field Operations Management
          </p>
          {import.meta.env.DEV && (
            <pre className="mt-2 rounded bg-muted p-2 text-xs">
              {JSON.stringify({ isLoading, isAuthenticated, hasError: !!error }, null, 2)}
            </pre>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
