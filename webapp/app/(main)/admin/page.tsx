"use client";

import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { UserManagement } from "@/components/admin/user-management";
import { useAdmin } from "@/hooks/use-admin";
import Loading from "./loading";
import { UserExport } from "@/components/admin/user-export";
import { t } from "i18next";

export default function AdminPage() {
  const adminData = useAdmin();

  if (adminData.isLoading) {
    return <Loading />;
  }

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
          <UserManagement {...adminData} />
        </TabsContent>

        <TabsContent value="export">
          <UserExport />
        </TabsContent>
      </Tabs>
    </div>
  );
}
