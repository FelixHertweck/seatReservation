"use client";

import { useState, useEffect, useCallback } from "react";
import { Plus, Edit, Trash2, FileText } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { SortableTableHead } from "@/components/common/sortable-table-head";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { UserFormModal } from "@/components/admin/user-form-modal";
import { UserImportModal } from "@/components/admin/user-import-modal";
import { TruncatedCell } from "@/components/common/truncated-cell";
import type { UserDto, AdminUserCreationDto, AdminUserUpdateDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";
import { PaginationWrapper } from "@/components/common/pagination-wrapper";
import { useSortableData } from "@/lib/table-sorting";

export interface UserManagementProps {
  users: UserDto[];
  availableRoles: string[];
  createUser: (user: AdminUserCreationDto) => Promise<void>;
  updateUser: (id: bigint, user: AdminUserUpdateDto) => Promise<void>;
  deleteUser: (ids: bigint[]) => Promise<void>;
  importUsers?: (users: AdminUserCreationDto[]) => Promise<void>;
  isLoading: boolean;
}

export function UserManagement({
  users,
  availableRoles,
  createUser,
  updateUser,
  deleteUser,
  importUsers,
  isLoading = false,
}: UserManagementProps) {
  const t = useT();

  const [filteredUsers, setFilteredUsers] = useState(users);
  const [selectedUser, setSelectedUser] = useState<UserDto | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<bigint>>(new Set());

  const { sortedData, sortKey, sortDirection, handleSort } = useSortableData(
    filteredUsers,
    "id",
    "asc",
  );

  useEffect(() => {
    setFilteredUsers(users);
  }, [users]);

  const applyFilters = useCallback(
    (searchQuery: string) => {
      const lowerCaseQuery = searchQuery.toLowerCase();
      const filtered = users.filter(
        (user) =>
          user.username?.toLowerCase().includes(lowerCaseQuery) ||
          user.firstname?.toLowerCase().includes(lowerCaseQuery) ||
          user.lastname?.toLowerCase().includes(lowerCaseQuery) ||
          user.email?.toLowerCase().includes(lowerCaseQuery) ||
          user.tags?.some((tag) => tag.toLowerCase().includes(lowerCaseQuery)),
      );
      setFilteredUsers(filtered);
    },
    [users],
  );

  const handleSearch = (query: string) => {
    applyFilters(query);
  };

  const handleFilter = () => {
    setFilteredUsers(users);
  };

  const handleCreateUser = () => {
    setSelectedUser(null);
    setIsCreating(true);
    setIsModalOpen(true);
  };

  const handleEditUser = (user: UserDto) => {
    setSelectedUser(user);
    setIsCreating(false);
    setIsModalOpen(true);
    setSelectedIds(new Set());
  };

  const handleDeleteUser = async (user: UserDto) => {
    if (
      user.id &&
      confirm(t("userManagement.confirmDelete", { username: user.username }))
    ) {
      await deleteUser([user.id]);
      setSelectedIds(new Set());
    }
  };

  const handleSelectAll = (paginatedData: UserDto[]) => {
    const newSelectedIds = new Set(selectedIds);
    const allCurrentSelected = paginatedData.every((user) =>
      user.id ? selectedIds.has(user.id) : false,
    );

    if (allCurrentSelected) {
      setSelectedIds(new Set());
    } else {
      paginatedData.forEach((user) => {
        if (user.id) newSelectedIds.add(user.id);
      });
      setSelectedIds(newSelectedIds);
    }
  };

  const handleToggleSelect = (id: bigint) => {
    const newSelectedIds = new Set(selectedIds);
    if (newSelectedIds.has(id)) {
      newSelectedIds.delete(id);
    } else {
      newSelectedIds.add(id);
    }
    setSelectedIds(newSelectedIds);
  };

  const handleDeleteSelected = async () => {
    if (
      selectedIds.size > 0 &&
      confirm(
        t("userManagement.confirmDeleteMultiple", {
          count: selectedIds.size,
        }),
      )
    ) {
      await deleteUser(Array.from(selectedIds));
      setSelectedIds(new Set());
    }
  };

  const handleModalSubmit = async (
    userData: AdminUserCreationDto | AdminUserUpdateDto,
  ) => {
    if (isCreating) {
      await createUser(userData as AdminUserCreationDto);
    } else if (selectedUser?.id) {
      await updateUser(selectedUser.id, userData as AdminUserUpdateDto);
    }
    setIsModalOpen(false);
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle className="text-xl sm:text-2xl">
              {t("userManagement.title")}
            </CardTitle>
            <CardDescription className="text-sm">
              {t("userManagement.description")}
            </CardDescription>
          </div>
          <div className="flex flex-col sm:flex-row gap-2 w-full sm:w-auto">
            {selectedIds.size > 0 && (
              <Button
                variant="destructive"
                onClick={handleDeleteSelected}
                className="w-full sm:w-auto"
              >
                <Trash2 className="mr-2 h-4 w-4" />
                {selectedIds.size}
              </Button>
            )}
            {importUsers && (
              <Button
                variant="outline"
                onClick={() => setIsImportModalOpen(true)}
                className="w-full sm:w-auto"
              >
                <FileText className="mr-2 h-4 w-4" />
                <span className="hidden sm:inline">
                  {t("userManagement.importJsonButton")}
                </span>
                <span className="sm:hidden">Import</span>
              </Button>
            )}
            <Button onClick={handleCreateUser} className="w-full sm:w-auto">
              <Plus className="mr-2 h-4 w-4" />
              <span className="hidden sm:inline">
                {t("userManagement.addUserButton")}
              </span>
              <span className="sm:hidden">Hinzufügen</span>
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent>
        <SearchAndFilter
          onSearch={handleSearch}
          onFilter={handleFilter}
          filterOptions={[]}
        />

        <PaginationWrapper
          data={sortedData}
          itemsPerPage={100}
          paginationLabel={t("userManagement.paginationLabel")}
        >
          {(paginatedData) => (
            <>
              <div className="hidden md:block overflow-x-auto">
                <div className="mb-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleSelectAll(paginatedData)}
                  >
                    {paginatedData.every((user) =>
                      user.id ? selectedIds.has(user.id) : false,
                    )
                      ? t("userManagement.deselectAll")
                      : t("userManagement.selectAll")}
                  </Button>
                </div>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-[50px]">
                        {t("userManagement.tableHeaderSelect")}
                      </TableHead>
                      <SortableTableHead
                        sortKey="username"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="min-w-[100px] max-w-[120px]"
                      >
                        {t("userManagement.tableHeaderUsername")}
                      </SortableTableHead>
                      <SortableTableHead
                        sortKey="firstname"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="min-w-[100px] max-w-[120px]"
                      >
                        {t("userManagement.tableHeaderName")}
                      </SortableTableHead>
                      <SortableTableHead
                        sortKey="email"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="min-w-[150px] max-w-[180px]"
                      >
                        {t("userManagement.tableHeaderEmail")}
                      </SortableTableHead>
                      <TableHead className="min-w-[100px] max-w-[120px]">
                        {t("userManagement.tableHeaderRoles")}
                      </TableHead>
                      <TableHead className="min-w-[100px] max-w-[120px]">
                        {t("userManagement.tableHeaderTags")}
                      </TableHead>
                      <SortableTableHead
                        sortKey="emailVerified"
                        currentSortKey={sortKey}
                        currentSortDirection={sortDirection}
                        onSort={handleSort}
                        className="min-w-[100px] max-w-[110px]"
                      >
                        {t("userManagement.tableHeaderVerified")}
                      </SortableTableHead>
                      <TableHead className="min-w-[70px] w-[80px]">
                        {t("userManagement.tableHeaderActions")}
                      </TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {isLoading
                      ? Array.from({ length: 8 }).map((_, index) => (
                          <TableRow key={index}>
                            <TableCell>
                              <Skeleton className="h-4 w-4" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-20" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-24" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-32" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-16" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-16" />
                            </TableCell>
                            <TableCell>
                              <Skeleton className="h-4 w-16" />
                            </TableCell>
                            <TableCell>
                              <div className="flex gap-2">
                                <Skeleton className="h-8 w-8" />
                                <Skeleton className="h-8 w-8" />
                              </div>
                            </TableCell>
                          </TableRow>
                        ))
                      : paginatedData.map((user) => (
                          <TableRow key={user.id?.toString()}>
                            <TableCell>
                              <Checkbox
                                checked={
                                  user.id ? selectedIds.has(user.id) : false
                                }
                                onCheckedChange={() =>
                                  user.id && handleToggleSelect(user.id)
                                }
                              />
                            </TableCell>
                            <TruncatedCell
                              content={user.username}
                              className="font-medium"
                            />
                            <TruncatedCell
                              content={`${user.firstname} ${user.lastname}`}
                            />
                            <TruncatedCell content={user.email} />
                            <TableCell>
                              <div className="flex gap-1 flex-wrap">
                                {user.roles?.map((role) => (
                                  <Badge key={role} variant="outline">
                                    {role}
                                  </Badge>
                                ))}
                              </div>
                            </TableCell>
                            <TableCell>
                              <div className="flex gap-1 flex-wrap">
                                {user.tags?.map((tag) => (
                                  <Badge key={tag} variant="secondary">
                                    {tag}
                                  </Badge>
                                ))}
                              </div>
                            </TableCell>
                            <TableCell>
                              <Badge
                                variant={
                                  user.emailVerified ? "default" : "secondary"
                                }
                              >
                                {user.emailVerified
                                  ? t("userManagement.verifiedStatus")
                                  : t("userManagement.pendingStatus")}
                              </Badge>
                            </TableCell>
                            <TableCell>
                              <div className="flex gap-2">
                                <Button
                                  variant="outline"
                                  size="sm"
                                  onClick={() => handleEditUser(user)}
                                >
                                  <Edit className="h-4 w-4" />
                                </Button>
                                <Button
                                  variant="destructive"
                                  size="sm"
                                  onClick={() => handleDeleteUser(user)}
                                >
                                  <Trash2 className="h-4 w-4" />
                                </Button>
                              </div>
                            </TableCell>
                          </TableRow>
                        ))}
                  </TableBody>
                </Table>
              </div>

              {/* Mobile Card View */}
              <div className="md:hidden space-y-4">
                <div className="mb-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleSelectAll(paginatedData)}
                  >
                    {paginatedData.every((user) =>
                      user.id ? selectedIds.has(user.id) : false,
                    )
                      ? t("userManagement.deselectAll")
                      : t("userManagement.selectAll")}
                  </Button>
                </div>
                {isLoading
                  ? Array.from({ length: 3 }).map((_, index) => (
                      <Card key={index}>
                        <CardHeader className="pb-3">
                          <Skeleton className="h-5 w-3/4" />
                          <Skeleton className="h-4 w-full mt-2" />
                        </CardHeader>
                        <CardContent>
                          <Skeleton className="h-4 w-full" />
                        </CardContent>
                      </Card>
                    ))
                  : paginatedData.map((user) => (
                      <Card key={user.id?.toString()}>
                        <CardHeader className="pb-3 flex flex-row items-start space-x-3 space-y-0">
                          <Checkbox
                            checked={user.id ? selectedIds.has(user.id) : false}
                            onCheckedChange={() =>
                              user.id && handleToggleSelect(user.id)
                            }
                            className="mt-1"
                          />
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2">
                              <CardTitle className="text-base">
                                {user.username}
                              </CardTitle>
                              <Badge
                                variant={
                                  user.emailVerified ? "default" : "secondary"
                                }
                                className="text-xs"
                              >
                                {user.emailVerified
                                  ? t("userManagement.verifiedStatus")
                                  : t("userManagement.pendingStatus")}
                              </Badge>
                            </div>
                            {user.firstname && (
                              <CardDescription className="text-sm mt-1">
                                {user.firstname} {user.lastname}
                              </CardDescription>
                            )}
                          </div>
                        </CardHeader>
                        <CardContent className="space-y-3">
                          <div>
                            <p className="text-xs text-muted-foreground mb-1">
                              {t("userManagement.tableHeaderEmail")}
                            </p>
                            <p className="text-sm break-all">{user.email}</p>
                          </div>

                          <div>
                            <p className="text-xs text-muted-foreground mb-1">
                              {t("userManagement.tableHeaderRoles")}
                            </p>
                            <div className="flex gap-1 flex-wrap">
                              {user.roles?.map((role) => (
                                <Badge
                                  key={role}
                                  variant="outline"
                                  className="text-xs"
                                >
                                  {role}
                                </Badge>
                              ))}
                            </div>
                          </div>

                          {user.tags && user.tags.length > 0 && (
                            <div>
                              <p className="text-xs text-muted-foreground mb-1">
                                {t("userManagement.tableHeaderTags")}
                              </p>
                              <div className="flex gap-1 flex-wrap">
                                {user.tags.map((tag) => (
                                  <Badge
                                    key={tag}
                                    variant="secondary"
                                    className="text-xs"
                                  >
                                    {tag}
                                  </Badge>
                                ))}
                              </div>
                            </div>
                          )}

                          <div className="flex gap-2 pt-2">
                            <Button
                              variant="outline"
                              size="sm"
                              className="flex-1"
                              onClick={() => handleEditUser(user)}
                            >
                              <Edit className="mr-2 h-4 w-4" />
                              {t("userManagement.editButtonLabel")}
                            </Button>
                            <Button
                              variant="destructive"
                              size="sm"
                              className="flex-1"
                              onClick={() => handleDeleteUser(user)}
                            >
                              <Trash2 className="mr-2 h-4 w-4" />
                              {t("userManagement.deleteButtonLabel")}
                            </Button>
                          </div>
                        </CardContent>
                      </Card>
                    ))}
              </div>
            </>
          )}
        </PaginationWrapper>
      </CardContent>

      {isModalOpen && (
        <UserFormModal
          user={selectedUser}
          availableRoles={availableRoles}
          isCreating={isCreating}
          onSubmit={handleModalSubmit}
          onClose={() => setIsModalOpen(false)}
        />
      )}

      {isImportModalOpen && importUsers && (
        <UserImportModal
          isOpen={isImportModalOpen}
          onClose={() => setIsImportModalOpen(false)}
          availableRoles={availableRoles}
          onImportUsers={importUsers}
        />
      )}
    </Card>
  );
}
