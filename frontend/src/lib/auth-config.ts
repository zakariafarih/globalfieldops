import type { AuthProviderProps } from "react-oidc-context"

const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL ?? "http://localhost:8180"
const REALM = import.meta.env.VITE_KEYCLOAK_REALM ?? "globalfieldops"
const CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? "gfo-frontend"

export const oidcConfig: AuthProviderProps = {
  authority: `${KEYCLOAK_URL}/realms/${REALM}`,
  client_id: CLIENT_ID,
  redirect_uri: window.location.origin,
  post_logout_redirect_uri: window.location.origin,
  scope: "openid profile email",
  automaticSilentRenew: true,
  onSigninCallback: () => {
    // Remove OIDC query params from URL after sign-in
    window.history.replaceState({}, document.title, window.location.pathname)
  },
}
