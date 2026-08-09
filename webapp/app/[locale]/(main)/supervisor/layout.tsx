import type React from "react";

import { SectionNav } from "@/components/supervisor/dashboard/section-nav";
import { PageHeaderNav } from "@/components/page-header";
import { SupervisorEventProvider } from "@/hooks/use-supervisor-event";

export default function SupervisorLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <SupervisorEventProvider>
      <PageHeaderNav>
        <SectionNav />
      </PageHeaderNav>
      {children}
    </SupervisorEventProvider>
  );
}
