"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import {
  getApiUsersMeOptions,
  putApiUsersMeMutation,
  getApiUsersMeQueryKey,
  postApiUserResendEmailConfirmationMutation,
} from "@/api/@tanstack/react-query.gen";
import type { UserDto, UserProfileUpdateDto } from "@/api";
import { ErrorWithResponse } from "@/components/init-query-client";

export function useProfile() {
  const queryClient = useQueryClient();
  const { t } = useTranslation();

  const { data: user, isLoading } = useQuery({
    ...getApiUsersMeOptions(),
  });

  const updateMutation = useMutation({
    ...putApiUsersMeMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData({ queryKey: getApiUsersMeQueryKey() }, () => {
        return data;
      });
      queryClient.invalidateQueries({ queryKey: getApiUsersMeQueryKey() });
    },
  });

  const updateProfile = async (
    updateData: UserProfileUpdateDto,
  ): Promise<UserDto | undefined> => {
    const request = updateMutation.mutateAsync({
      body: updateData,
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("profile.update.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("profile.update.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const resendConfirmationMutation = useMutation({
    ...postApiUserResendEmailConfirmationMutation(),
  });

  const resendConfirmation = async (): Promise<void> => {
    const request = resendConfirmationMutation.mutateAsync({});
    toast.promise(request, {
      loading: t("emailVerification.resendingConfirmationEmail"),
      success: t("email.confirmationEmailSentTitle"),
      error: (error: ErrorWithResponse) => ({
        message: t("emailVerification.resendConfirmationEmailFailed"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  return {
    user,
    isLoading,
    updateProfile,
    resendConfirmation,
  };
}
