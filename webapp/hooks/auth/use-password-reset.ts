"use client";

import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";
import {
  postApiAuthPasswordResetMutation,
  postApiAuthPasswordResetConfirmMutation,
} from "@/api/@tanstack/react-query.gen";
import type { PasswordResetConfirmDto, PasswordResetRequestDto } from "@/api";
import { ErrorWithResponse } from "@/components/init-query-client";

export function usePasswordReset() {
  const t = useT();

  const { mutateAsync: requestPasswordResetMutation } = useMutation({
    ...postApiAuthPasswordResetMutation(),
  });

  const requestPasswordReset = async (requestData: PasswordResetRequestDto) => {
    const request = requestPasswordResetMutation({ body: requestData });
    // No success toast: the page already renders an inline success Alert with the same message.
    toast.promise(request, {
      loading: t("common.loading"),
      error: (error: ErrorWithResponse) => ({
        message: t("forgotPassword.error"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const { mutateAsync: confirmPasswordResetMutation } = useMutation({
    ...postApiAuthPasswordResetConfirmMutation(),
  });

  const confirmPasswordReset = async (confirmData: PasswordResetConfirmDto) => {
    const request = confirmPasswordResetMutation({ body: confirmData });
    // No success toast: the page already renders an inline success Alert with the same message.
    toast.promise(request, {
      loading: t("common.loading"),
      error: (error: ErrorWithResponse) => {
        const status = error.response?.status;
        // The component renders its own inline message for a known-bad token;
        // only toast a generic message for anything unexpected.
        if (status === 400 || status === 410) {
          return { message: t("common.error.default") };
        }
        return {
          message: t("resetPassword.error.general"),
          description: error.response?.description ?? t("common.error.default"),
        };
      },
    });
    return request;
  };

  return {
    requestPasswordReset,
    confirmPasswordReset,
  };
}
