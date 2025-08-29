import type React from "react";
import {
  SidebarProvider,
  SidebarInset,
  SidebarTrigger,
} from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/app-sidebar";
import { LoginRequiredPopup } from "@/components/common/login-required-popup";
import { EmailVerificationPrompt } from "@/components/common/email-verification-prompt";

export default function MainLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <SidebarProvider>
      <AppSidebar />
      <SidebarInset>
        <header className="flex h-14 items-center gap-4 border-b bg-background px-4 lg:h-[60px] lg:px-6">
          <SidebarTrigger className="hover:scale-110 transition-transform duration-200" />
          <div className="w-full flex-1">
            <h1 className="text-lg font-semibold md:text-xl">
              Seat Reservation
            </h1>
          </div>
        </header>
        <main className="flex-1 p-4 lg:p-6">{children}</main>
      </SidebarInset>
      <LoginRequiredPopup />
      <EmailVerificationPrompt />
    </SidebarProvider>
  );
}
