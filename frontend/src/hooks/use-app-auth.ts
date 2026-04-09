import { useAuth } from "react-oidc-context"
import { useEffect, useRef } from "react"
import { setAuthInterceptor } from "@/lib/api-client"

export type AppRole = "ADMIN" | "DISPATCHER" | "TECHNICIAN"

interface UseAppAuth {
  isAuthenticated: boolean
  isLoading: boolean
  error: unknown
  user: {
    name: string
    email: string
    roles: AppRole[]
  } | null
  login: () => void
  logout: () => void
  hasRole: (role: AppRole) => boolean
  hasAnyRole: (...roles: AppRole[]) => boolean
}

export function useAppAuth(): UseAppAuth {
  const auth = useAuth()
  const interceptorSet = useRef(false)
  const userRef = useRef(auth.user)
  userRef.current = auth.user

  useEffect(() => {
    if (!interceptorSet.current) {
      setAuthInterceptor(() => userRef.current?.access_token)
      interceptorSet.current = true
    }
  }, [])

  const roles: AppRole[] =
    (auth.user?.profile?.roles as AppRole[] | undefined) ?? []

  const user = auth.user
    ? {
        name:
          (auth.user.profile?.preferred_username as string) ??
          auth.user.profile?.name ??
          "Unknown",
        email: (auth.user.profile?.email as string) ?? "",
        roles,
      }
    : null

  return {
    isAuthenticated: auth.isAuthenticated,
    isLoading: auth.isLoading,
    error: auth.error,
    user,
    login: () => {
      console.log("[auth] signinRedirect called, auth state:", {
        isLoading: auth.isLoading,
        isAuthenticated: auth.isAuthenticated,
        error: auth.error,
        activeNavigator: auth.activeNavigator,
      })
      auth.signinRedirect().catch((err) => {
        console.error("[auth] signinRedirect failed:", err)
      })
    },
    logout: () =>
      auth.signoutRedirect({ post_logout_redirect_uri: window.location.origin }),
    hasRole: (role) => roles.includes(role),
    hasAnyRole: (...r) => r.some((role) => roles.includes(role)),
  }
}
