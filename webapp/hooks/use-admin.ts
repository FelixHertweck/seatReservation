"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getApiUsersAdminOptions,
  getApiUsersAdminQueryKey,
  postApiUsersAdminMutation,
  putApiUsersAdminByIdMutation,
  deleteApiUsersAdminByIdMutation,
  getApiUsersRolesOptions,
} from "@/api/@tanstack/react-query.gen";
import type { UserCreationDto, AdminUserUpdateDto } from "@/api";

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
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: getApiUsersAdminQueryKey(),
      });
    },
  });

  const { mutateAsync: updateMutation } = useMutation({
    ...putApiUsersAdminByIdMutation(),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: getApiUsersAdminQueryKey(),
      });
    },
  });

  const { mutateAsync: deleteMutation } = useMutation({
    ...deleteApiUsersAdminByIdMutation(),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: getApiUsersAdminQueryKey(),
      });
    },
  });

  const createUser = async (userData: UserCreationDto) => {
    await createMutation({ body: userData });
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
    updateUser,
    deleteUser,
  };
}
