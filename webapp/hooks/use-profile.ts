"use client";

import { useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "@/hooks/use-toast";
import {
  getApiUsersMeOptions,
  putApiUsersMeMutation,
  getApiUsersMeQueryKey,
  postApiUserResendEmailConfirmationMutation,
} from "@/api/@tanstack/react-query.gen";
import type { UserDto, UserProfileUpdateDto } from "@/api";

export function useProfile() {
  const queryClient = useQueryClient();
  const { t } = useTranslation();

  const { data: user, isLoading } = useQuery({
    ...getApiUsersMeOptions(),
  });

  const updateMutation = useMutation({
    ...putApiUsersMeMutation(),
    onSuccess: async (data) => {
      queryClient.setQueriesData({ queryKey: getApiUsersMeQueryKey() }, () => {
        return data;
      });
      toast({
        title: t("profile.update.success.title"),
        description: t("profile.update.success.description"),
      });
    },
  });

  const updateProfile = async (
    updateData: UserProfileUpdateDto,
  ): Promise<UserDto | undefined> => {
    const result = await updateMutation.mutateAsync({
      body: updateData,
    });
    return result;
  };

  const resendConfirmationMutation = useMutation({
    ...postApiUserResendEmailConfirmationMutation(),
  });

  const resendConfirmation = async (): Promise<void> => {
    await resendConfirmationMutation.mutateAsync({});
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
    resendConfirmation,
  };
}
