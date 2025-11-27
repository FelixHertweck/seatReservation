"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";
import {
  getApiUsersAdminOptions,
  getApiUsersAdminQueryKey,
  postApiUsersAdminMutation,
  putApiUsersAdminByIdMutation,
  deleteApiUsersAdminByIdMutation,
  getApiUsersRolesOptions,
  postApiUsersAdminImportMutation,
} from "@/api/@tanstack/react-query.gen";
import type { AdminUserCreationDto, AdminUserUpdateDto, UserDto } from "@/api";
import { UserManagementProps } from "@/components/admin/user-management";
import { ErrorWithResponse } from "@/components/init-query-client";

export function useAdmin(): UserManagementProps {
  const t = useT();
  const queryClient = useQueryClient();

  const { data: users, isLoading: userIsLoading } = useQuery(
    getApiUsersAdminOptions(),
  );
  const { data: availableRoles, isLoading: rolesIsLoading } = useQuery(
    getApiUsersRolesOptions(),
  );

  const { mutateAsync: createMutation } = useMutation({
    ...postApiUsersAdminMutation(),
  });

  const { mutateAsync: importMutation } = useMutation({
    ...postApiUsersAdminImportMutation(),
  });

  const { mutateAsync: updateMutation } = useMutation({
    ...putApiUsersAdminByIdMutation(),
  });

  const { mutateAsync: deleteMutation } = useMutation({
    ...deleteApiUsersAdminByIdMutation(),
  });

  const createUser = async (userData: AdminUserCreationDto): Promise<void> => {
    const request = createMutation({ body: userData });
    toast.promise(request, {
      loading: t("common.loading"),
      success: (data) => {
        queryClient.setQueriesData(
          { queryKey: getApiUsersAdminQueryKey() },
          (oldData: UserDto[] | undefined) => {
            return oldData ? [...oldData, data] : [data];
          },
        );
        return t("admin.user.create.success.title");
      },
      error: (error: ErrorWithResponse) => ({
        message: t("admin.user.create.error.title"),
        description:
          error.response?.description ?? t("admin.user.create.error.default"),
      }),
    });
  };

  const importUsers = async (
    userData: AdminUserCreationDto[],
  ): Promise<void> => {
    const request = importMutation({ body: userData });
    toast.promise(request, {
      loading: t("common.loading"),
      success: (data) => {
        queryClient.setQueriesData(
          { queryKey: getApiUsersAdminQueryKey() },
          (oldData: UserDto[] | undefined) => {
            return oldData ? [...oldData, ...data] : [...data];
          },
        );
        return t("admin.user.import.success.title");
      },
      error: (error: ErrorWithResponse) => ({
        message: t("admin.user.import.error.title"),
        description:
          error.response?.description ?? t("admin.user.import.error.default"),
      }),
    });
  };

  const updateUser = async (
    id: bigint,
    userData: AdminUserUpdateDto,
  ): Promise<void> => {
    const request = updateMutation({ body: userData, path: { id } });
    toast.promise(request, {
      loading: t("common.loading"),
      success: (data) => {
        queryClient.setQueriesData(
          { queryKey: getApiUsersAdminQueryKey() },
          (oldData: UserDto[] | undefined) => {
            return oldData
              ? oldData.map((user) => (user.id === data.id ? data : user))
              : [data];
          },
        );
        return t("admin.user.update.success.title");
      },
      error: (error: ErrorWithResponse) => ({
        message: t("admin.user.update.error.title"),
        description:
          error.response?.description ?? t("admin.user.update.error.default"),
      }),
    });
  };

  const deleteUser = async (ids: bigint[]): Promise<void> => {
    const request = deleteMutation({ query: { ids } });
    toast.promise(request, {
      loading: t("common.loading"),
      success: () => {
        queryClient.setQueriesData(
          { queryKey: getApiUsersAdminQueryKey() },
          (oldData: UserDto[] | undefined) => {
            return oldData
              ? oldData.filter((user) => !ids.includes(user.id ?? BigInt(-1)))
              : [];
          },
        );
        return t("admin.user.delete.success.title");
      },
      error: (error: ErrorWithResponse) => ({
        message: t("admin.user.delete.error.title"),
        description:
          error.response?.description ?? t("admin.user.delete.error.default"),
      }),
    });
  };

  return {
    users: users ?? [],
    availableRoles: availableRoles ?? [],
    isLoading: userIsLoading || rolesIsLoading,
    createUser,
    importUsers,
    updateUser,
    deleteUser,
  };
}
