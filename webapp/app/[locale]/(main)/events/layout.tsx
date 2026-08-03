import type React from "react";

import { EventsSectionNav } from "@/components/events/events-section-nav";
import { PageHeaderNav } from "@/components/page-header";

export default function EventsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <>
      <PageHeaderNav>
        <EventsSectionNav />
      </PageHeaderNav>
      {children}
    </>
  );
}
