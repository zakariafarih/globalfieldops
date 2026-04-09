import { BrowserRouter, Routes, Route } from "react-router"
import AuthGuard from "@/components/auth-guard"
import AppLayout from "@/layouts/app-layout"
import LoginPage from "@/pages/login"
import DashboardPage from "@/pages/dashboard"
import TechniciansPage from "@/pages/technicians/index"
import TechnicianDetailPage from "@/pages/technicians/[id]"
import WorkOrdersPage from "@/pages/work-orders/index"
import WorkOrderDetailPage from "@/pages/work-orders/[id]"
import AuditPage from "@/pages/audit/index"

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<AuthGuard />}>
          <Route element={<AppLayout />}>
            <Route index element={<DashboardPage />} />
            <Route path="technicians" element={<TechniciansPage />} />
            <Route path="technicians/:id" element={<TechnicianDetailPage />} />
            <Route path="work-orders" element={<WorkOrdersPage />} />
            <Route path="work-orders/:id" element={<WorkOrderDetailPage />} />
            <Route path="audit" element={<AuditPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
