import type React from "react";

import { SectionNav } from "@/components/supervisor/dashboard/section-nav";
import { PageHeaderNav } from "@/components/page-header";
import { SupervisorEventProvider } from "@/hooks/use-supervisor-event";
import { SupervisorLiveViewProvider } from "@/hooks/use-supervisor-liveview";

export default function SupervisorLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <SupervisorEventProvider>
      <SupervisorLiveViewProvider>
        <PageHeaderNav>
          <SectionNav />
        </PageHeaderNav>
        {children}
      </SupervisorLiveViewProvider>
    </SupervisorEventProvider>
  );
}
