import type React from "react";

import { SectionNav } from "@/components/management/dashboard/section-nav";
import { PageHeaderNav } from "@/components/page-header";

export default function ManagementLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <>
      <PageHeaderNav>
        <SectionNav />
      </PageHeaderNav>
      {children}
    </>
  );
}
