"use client";

import { useState, useEffect } from "react";
import { Plus, Edit, Trash2, FileText } from "lucide-react";
import { Button } from "@/components/ui/button";
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
import { Badge } from "@/components/ui/badge";
import { SearchAndFilter } from "@/components/common/search-and-filter";
import { UserFormModal } from "@/components/admin/user-form-modal";
import { UserImportModal } from "@/components/admin/user-import-modal";
import type { UserDto, AdminUserCreationDto, AdminUserUpdateDto } from "@/api";
import { useT } from "@/lib/i18n/hooks";

export interface UserManagementProps {
  users: UserDto[];
  availableRoles: string[];
  createUser: (user: AdminUserCreationDto) => Promise<void>;
  updateUser: (id: bigint, user: AdminUserUpdateDto) => Promise<void>;
  deleteUser: (id: bigint) => Promise<void>;
  importUsers?: (users: AdminUserCreationDto[]) => Promise<void>;
}

export function UserManagement({
  users,
  availableRoles,
  createUser,
  updateUser,
  deleteUser,
  importUsers,
}: UserManagementProps) {
  const t = useT();

  const [filteredUsers, setFilteredUsers] = useState(users);
  const [selectedUser, setSelectedUser] = useState<UserDto | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);

  useEffect(() => {
    setFilteredUsers(users);
  }, [users]);

  const handleSearch = (query: string) => {
    const lowerCaseQuery = query.toLowerCase();
    const filtered = users.filter(
      (user) =>
        user.username?.toLowerCase().includes(lowerCaseQuery) ||
        user.firstname?.toLowerCase().includes(lowerCaseQuery) ||
        user.lastname?.toLowerCase().includes(lowerCaseQuery) ||
        user.email?.toLowerCase().includes(lowerCaseQuery) ||
        user.tags?.some((tag) => tag.toLowerCase().includes(lowerCaseQuery)), // Search by tags
    );
    setFilteredUsers(filtered);
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
  };

  const handleDeleteUser = async (user: UserDto) => {
    if (
      user.id &&
      confirm(t("userManagement.confirmDelete", { username: user.username }))
    ) {
      await deleteUser(user.id);
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
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>{t("userManagement.title")}</CardTitle>
            <CardDescription>{t("userManagement.description")}</CardDescription>
          </div>
          <div className="flex gap-2">
            {importUsers && (
              <Button
                variant="outline"
                onClick={() => setIsImportModalOpen(true)}
              >
                <FileText className="mr-2 h-4 w-4" />
                {t("userManagement.importJsonButton")}
              </Button>
            )}
            <Button onClick={handleCreateUser}>
              <Plus className="mr-2 h-4 w-4" />
              {t("userManagement.addUserButton")}
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

        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t("userManagement.tableHeaderUsername")}</TableHead>
              <TableHead>{t("userManagement.tableHeaderName")}</TableHead>
              <TableHead>{t("userManagement.tableHeaderEmail")}</TableHead>
              <TableHead>{t("userManagement.tableHeaderRoles")}</TableHead>
              <TableHead>{t("userManagement.tableHeaderTags")}</TableHead>{" "}
              {/* New TableHead for Tags */}
              <TableHead>{t("userManagement.tableHeaderVerified")}</TableHead>
              <TableHead>{t("userManagement.tableHeaderActions")}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredUsers.map((user) => (
              <TableRow key={user.id?.toString()}>
                <TableCell className="font-medium">{user.username}</TableCell>
                <TableCell>
                  {user.firstname} {user.lastname}
                </TableCell>
                <TableCell>{user.email}</TableCell>
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
                  <Badge variant={user.emailVerified ? "default" : "secondary"}>
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
