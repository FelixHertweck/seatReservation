"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
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
    },
  });

  const { mutateAsync: deleteMutation } = useMutation({
    ...deleteApiUsersAdminByIdMutation(),
    onSuccess: async (_, variables) => {
      queryClient.setQueriesData(
        { queryKey: getApiUsersAdminQueryKey() },
        (oldData: UserDto[] | undefined) => {
          return oldData
            ? oldData.filter((user) => user.id !== variables.path.id)
            : [];
        },
      );
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

  const deleteUser = async (id: bigint) => {
    await deleteMutation({ path: { id } });
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
