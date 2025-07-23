"use client";

import { useState } from "react";
import { Plus, Edit, Trash2 } from "lucide-react";
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
import type { UserDto, UserCreationDto, AdminUserUpdateDto } from "@/api";

export interface UserManagementProps {
  users: UserDto[];
  availableRoles: string[];
  createUser: (user: UserCreationDto) => Promise<void>;
  updateUser: (id: bigint, user: AdminUserUpdateDto) => Promise<void>;
  deleteUser: (id: bigint) => Promise<void>;
}

export function UserManagement({
  users,
  availableRoles,
  createUser,
  updateUser,
  deleteUser,
}: UserManagementProps) {
  const [filteredUsers, setFilteredUsers] = useState(users);
  const [selectedUser, setSelectedUser] = useState<UserDto | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);

  const handleSearch = (query: string) => {
    const filtered = users.filter(
      (user) =>
        user.username?.toLowerCase().includes(query.toLowerCase()) ||
        user.firstName?.toLowerCase().includes(query.toLowerCase()) ||
        user.lastName?.toLowerCase().includes(query.toLowerCase()) ||
        user.email?.toLowerCase().includes(query.toLowerCase()),
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
      confirm(`Are you sure you want to delete ${user.username}?`)
    ) {
      await deleteUser(user.id);
    }
  };

  const handleModalSubmit = async (
    userData: UserCreationDto | AdminUserUpdateDto,
  ) => {
    if (isCreating) {
      await createUser(userData as UserCreationDto);
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
            <CardTitle>User Management</CardTitle>
            <CardDescription>
              Manage system users and their roles
            </CardDescription>
          </div>
          <Button onClick={handleCreateUser}>
            <Plus className="mr-2 h-4 w-4" />
            Add User
          </Button>
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
              <TableHead>Username</TableHead>
              <TableHead>Name</TableHead>
              <TableHead>Email</TableHead>
              <TableHead>Roles</TableHead>
              <TableHead>Verified</TableHead>
              <TableHead>Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredUsers.map((user) => (
              <TableRow key={user.id?.toString()}>
                <TableCell className="font-medium">{user.username}</TableCell>
                <TableCell>
                  {user.firstName} {user.lastName}
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
                  <Badge variant={user.emailVerified ? "default" : "secondary"}>
                    {user.emailVerified ? "Verified" : "Pending"}
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
    </Card>
  );
}
