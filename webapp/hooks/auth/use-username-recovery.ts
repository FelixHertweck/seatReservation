"use client";

import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";
import { postApiAuthUsernameRecoveryMutation } from "@/api/@tanstack/react-query.gen";
import type { UsernameRecoveryRequestDto } from "@/api";
import { ErrorWithResponse } from "@/components/init-query-client";

export function useUsernameRecovery() {
  const t = useT();

  const { mutateAsync: requestUsernameRecoveryMutation } = useMutation({
    ...postApiAuthUsernameRecoveryMutation(),
  });

  const requestUsernameRecovery = async (
    requestData: UsernameRecoveryRequestDto,
  ) => {
    const request = requestUsernameRecoveryMutation({ body: requestData });
    // No success toast: the page already renders an inline success Alert with the same message.
    toast.promise(request, {
      loading: t("common.loading"),
      error: (error: ErrorWithResponse) => ({
        message: t("forgotUsername.error"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  return {
    requestUsernameRecovery,
  };
}
