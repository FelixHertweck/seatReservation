"use client";

import { useParams, useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";
import {
  postApiUserResendEmailConfirmationMutation,
  postApiUserVerifyEmailCodeMutation,
} from "@/api/@tanstack/react-query.gen";
import { ErrorWithResponse } from "@/components/init-query-client";
import { redirectUser } from "@/lib/redirect-User";
import { useSession } from "./use-session";

const EMAIL_VERIFICATION_REDIRECT_DELAY_MS = 2000;

export function useVerifyEmail() {
  const t = useT();
  const params = useParams();
  const locale = params.locale as string;
  const router = useRouter();
  const queryClient = useQueryClient();
  const { user } = useSession();

  const { mutateAsync: verifyEmailMutation } = useMutation({
    ...postApiUserVerifyEmailCodeMutation(),
  });

  const verifyEmail = async (code: string, returnToUrl?: string | null) => {
    const request = verifyEmailMutation({ body: { verificationCode: code } });
    toast.promise(request, {
      loading: t("common.loading"),
      success: async () => {
        await queryClient.invalidateQueries();
        await new Promise((resolve) =>
          setTimeout(resolve, EMAIL_VERIFICATION_REDIRECT_DELAY_MS),
        );
        redirectUser(router, locale, user, returnToUrl);
        return t("emailVerification.success.title");
      },
      error: (error: ErrorWithResponse) => ({
        message: t("emailVerification.error.title"),
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
    verifyEmail,
    resendConfirmation,
  };
}
