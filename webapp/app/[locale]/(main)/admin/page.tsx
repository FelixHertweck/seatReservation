"use client";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { UserManagement } from "@/components/admin/user-management";
import {
  UserTableSkeleton,
  UserExportSkeleton,
} from "@/components/admin/user-table-skeleton";
import { useAdmin } from "@/hooks/use-admin";
import { UserExport } from "@/components/admin/user-export";
import { useT } from "@/lib/i18n/hooks";

export default function AdminPage() {
  const t = useT();

  const adminData = useAdmin();

  return (
    <div className="container mx-auto p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold mb-2">
          {t("adminPage.dashboardTitle")}
        </h1>
        <p className="text-muted-foreground">
          {t("adminPage.dashboardDescription")}
        </p>
      </div>

      <Tabs defaultValue="users" className="space-y-4">
        <TabsList>
          <TabsTrigger value="users">
            {t("adminPage.userManagementTab")}
          </TabsTrigger>
          <TabsTrigger value="export">
            {t("adminPage.exportDataTab")}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="users">
          {adminData.isLoading ? (
            <UserTableSkeleton showImportButton={true} />
          ) : (
            <UserManagement {...adminData} />
          )}
        </TabsContent>

        <TabsContent value="export">
          {adminData.isLoading ? <UserExportSkeleton /> : <UserExport />}
        </TabsContent>
      </Tabs>
    </div>
  );
}
