import { Outlet, NavLink, useNavigate } from "react-router"
import {
  LayoutDashboard,
  Users,
  ClipboardList,
  ScrollText,
  LogOut,
  ShieldCheck,
} from "lucide-react"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarTrigger,
} from "@/components/ui/sidebar"
import { Separator } from "@/components/ui/separator"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Button } from "@/components/ui/button"
import { useAppAuth } from "@/hooks/use-app-auth"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import { Toaster } from "sonner"

const navItems = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard },
  { to: "/technicians", label: "Technicians", icon: Users },
  { to: "/work-orders", label: "Work Orders", icon: ClipboardList },
  { to: "/audit", label: "Audit Log", icon: ScrollText, role: "ADMIN" as const },
]

export default function AppLayout() {
  const { user, logout, hasRole } = useAppAuth()
  const navigate = useNavigate()

  const initials = user
    ? user.name.slice(0, 2).toUpperCase()
    : "??"

  const roleBadge = user?.roles[0] ?? "USER"

  const visibleItems = navItems.filter(
    (item) => !item.role || hasRole(item.role)
  )

  return (
    <SidebarProvider>
      <Sidebar>
        <SidebarHeader className="p-4">
          <button
            onClick={() => navigate("/")}
            className="flex items-center gap-2 font-semibold tracking-tight"
          >
            <ShieldCheck className="size-6 text-primary" />
            <span className="text-lg">GlobalField Ops</span>
          </button>
        </SidebarHeader>

        <SidebarContent>
          <SidebarGroup>
            <SidebarGroupLabel>Navigation</SidebarGroupLabel>
            <SidebarGroupContent>
              <SidebarMenu>
                {visibleItems.map((item) => (
                  <SidebarMenuItem key={item.to}>
                    <SidebarMenuButton asChild>
                      <NavLink
                        to={item.to}
                        end={item.to === "/"}
                        className={({ isActive }) =>
                          cn(isActive && "bg-sidebar-accent text-sidebar-accent-foreground")
                        }
                      >
                        <item.icon />
                        <span>{item.label}</span>
                      </NavLink>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        </SidebarContent>

        <SidebarFooter className="p-4">
          <div className="flex items-center gap-3">
            <Avatar className="size-8">
              <AvatarFallback className="text-xs">{initials}</AvatarFallback>
            </Avatar>
            <div className="flex min-w-0 flex-1 flex-col">
              <span className="truncate text-sm font-medium">{user?.name}</span>
              <Badge variant="outline" className="w-fit text-[10px]">
                {roleBadge}
              </Badge>
            </div>
            <Button variant="ghost" size="icon" onClick={logout} title="Sign out">
              <LogOut />
            </Button>
          </div>
        </SidebarFooter>
      </Sidebar>

      <SidebarInset>
        <header className="flex h-12 shrink-0 items-center gap-2 border-b px-4">
          <SidebarTrigger className="-ml-1" />
          <Separator orientation="vertical" className="mr-2 h-4" />
          <span className="text-sm text-muted-foreground">
            GlobalField Ops — Field Operations Platform
          </span>
        </header>

        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </SidebarInset>

      <Toaster richColors position="top-right" />
    </SidebarProvider>
  )
}
