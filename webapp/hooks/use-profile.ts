"use client";

import { useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getApiUsersMeOptions,
  putApiUsersMeMutation,
  getApiUsersMeQueryKey,
} from "@/api/@tanstack/react-query.gen";
import type { UserDto, UserProfileUpdateDto } from "@/api";

export function useProfile() {
  const queryClient = useQueryClient();

  const { data: user, isLoading } = useQuery({
    ...getApiUsersMeOptions(),
  });

  const updateMutation = useMutation({
    ...putApiUsersMeMutation(),
  });

  const updateProfile = async (
    updateData: UserProfileUpdateDto,
  ): Promise<UserDto | undefined> => {
    const result = await updateMutation.mutateAsync({
      body: updateData,
    });
    return result;
  };

  useEffect(() => {
    if (updateMutation.isSuccess) {
      queryClient.invalidateQueries({ queryKey: getApiUsersMeQueryKey() });
    }
  }, [updateMutation.isSuccess, queryClient]);

  return {
    user,
    isLoading,
    updateProfile,
  };
}
