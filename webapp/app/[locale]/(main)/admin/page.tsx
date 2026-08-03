"use client";

import { useMemo, useState } from "react";
import { UserManagement } from "@/components/admin/user-management";
import { UserTableSkeleton } from "@/components/admin/user-table-skeleton";
import { useAdmin } from "@/hooks/use-admin";
import { useT } from "@/lib/i18n/hooks";
import { PageHeader } from "@/components/page-header";
import { SearchAndFilter } from "@/components/common/search-and-filter";

export default function AdminPage() {
  const t = useT();

  const adminData = useAdmin();
  const [userSearchQuery, setUserSearchQuery] = useState("");

  const filteredUsers = useMemo(() => {
    const lowerCaseQuery = userSearchQuery.toLowerCase();
    return adminData.users.filter(
      (user) =>
        user.username?.toLowerCase().includes(lowerCaseQuery) ||
        user.firstname?.toLowerCase().includes(lowerCaseQuery) ||
        user.lastname?.toLowerCase().includes(lowerCaseQuery) ||
        user.email?.toLowerCase().includes(lowerCaseQuery) ||
        user.tags?.some((tag) => tag.toLowerCase().includes(lowerCaseQuery)),
    );
  }, [adminData.users, userSearchQuery]);

  return (
    <div className="container mx-auto p-4 sm:p-6">
      {adminData.isLoading ? (
        <>
          <PageHeader
            title={t("adminPage.dashboardTitle")}
            description={t("adminPage.dashboardDescription")}
            search={
              <SearchAndFilter
                onSearch={setUserSearchQuery}
                onFilter={() => {}}
                filterOptions={[]}
                initialQuery={userSearchQuery}
                className="w-full"
              />
            }
          />
          <UserTableSkeleton showImportButton={true} />
        </>
      ) : (
        <UserManagement
          {...adminData}
          users={filteredUsers}
          allUsers={adminData.users}
          searchQuery={userSearchQuery}
          onSearchChange={setUserSearchQuery}
        />
      )}
    </div>
  );
}
