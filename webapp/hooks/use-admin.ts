"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/hooks/use-toast";
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

export function useAdmin() {
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
    onSuccess: async (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiUsersAdminQueryKey() },
        (oldData: UserDto[] | undefined) => {
          return oldData ? [...oldData, data] : [data];
        },
      );
      toast({
        title: t("admin.user.create.success.title"),
        description: t("admin.user.create.success.description"),
      });
    },
  });

  const { mutateAsync: importMutation } = useMutation({
    ...postApiUsersAdminImportMutation(),
    onSuccess: async (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiUsersAdminQueryKey() },
        (oldData: UserDto[] | undefined) => {
          return oldData ? [...oldData, ...data] : [...data];
        },
      );
      toast({
        title: t("admin.user.import.success.title"),
        description: t("admin.user.import.success.description"),
      });
    },
  });

  const { mutateAsync: updateMutation } = useMutation({
    ...putApiUsersAdminByIdMutation(),
    onSuccess: async (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiUsersAdminQueryKey() },
        (oldData: UserDto[] | undefined) => {
          return oldData
            ? oldData.map((user) => (user.id === data.id ? data : user))
            : [data];
        },
      );
      toast({
        title: t("admin.user.update.success.title"),
        description: t("admin.user.update.success.description"),
      });
    },
  });

  const { mutateAsync: deleteMutation } = useMutation({
    ...deleteApiUsersAdminByIdMutation(),
    onSuccess: async (_, variables) => {
      queryClient.setQueriesData(
        { queryKey: getApiUsersAdminQueryKey() },
        (oldData: UserDto[] | undefined) => {
          return oldData
            ? oldData.filter(
                (user) =>
                  !variables.query?.ids?.includes(user.id ?? BigInt(-1)),
              )
            : [];
        },
      );
      toast({
        title: t("admin.user.delete.success.title"),
        description: t("admin.user.delete.success.description"),
      });
    },
  });

  const createUser = async (userData: AdminUserCreationDto) => {
    await createMutation({ body: userData });
  };

  const importUsers = async (userData: AdminUserCreationDto[]) => {
    await importMutation({ body: userData });
  };

  const updateUser = async (id: bigint, userData: AdminUserUpdateDto) => {
    await updateMutation({ body: userData, path: { id } });
  };

  const deleteUser = async (ids: bigint[]) => {
    await deleteMutation({ query: { ids } });
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
