import type React from "react";
import {
  SidebarProvider,
  SidebarInset,
  SidebarTrigger,
} from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/sidebar";
import { LoginRequiredPopup } from "@/components/common/login-required-popup";
import { EmailVerificationPrompt } from "@/components/common/email-verification-prompt";
import { UnsavedChangesAlert } from "@/components/common/unsaved-changes-alert";
import { AppFooter } from "@/components/footer";
import { PageHeaderProvider, PageHeaderSlot } from "@/components/page-header";

export default async function MainLayout({
  params,
  children,
}: {
  children: React.ReactNode;
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  return (
    <SidebarProvider>
      <AppSidebar />
      <PageHeaderProvider>
        <SidebarInset className="transition-[margin] duration-200 ease-linear md:!m-0 md:peer-data-[state=expanded]:!ml-[var(--sidebar-width)] md:peer-data-[state=collapsed]:!ml-[var(--sidebar-width-icon)]">
          <header className="flex h-14 items-center gap-4 border-b bg-background px-4 lg:h-[60px] lg:px-6 md:peer-data-[state=collapsed]:px-3 md:peer-data-[state=expanded]:px-6">
            <SidebarTrigger className="hover:scale-110 transition-transform duration-200" />
            <PageHeaderSlot />
          </header>
          <main className="flex-1 p-4 lg:p-6 md:peer-data-[state=collapsed]:p-3 md:peer-data-[state=expanded]:p-6">
            {children}
          </main>
          <AppFooter locale={locale} />
        </SidebarInset>
        <LoginRequiredPopup />
        <EmailVerificationPrompt />
        <UnsavedChangesAlert />
      </PageHeaderProvider>
    </SidebarProvider>
  );
}
