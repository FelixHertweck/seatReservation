"use client";

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";
import { postApiAuthLoginMutation } from "@/api/@tanstack/react-query.gen";
import { Instant, LoginLockedDto } from "@/api";
import { ErrorWithResponse } from "@/components/init-query-client";
import { redirectUser } from "@/lib/redirect-User";
import { useSession } from "./use-session";

export function useLogin() {
  const t = useT();
  const params = useParams();
  const locale = params.locale as string;
  const router = useRouter();
  const queryClient = useQueryClient();
  const { user, refetchUser } = useSession();

  const [retryAfter, setRetryAfter] = useState<Instant | null>(null);

  const { mutateAsync: loginMutation } = useMutation({
    ...postApiAuthLoginMutation(),
  });

  const login = async (
    username: string,
    password: string,
    returnToUrl?: string | null,
    altchaPayload?: string,
  ) => {
    try {
      const res = await loginMutation({
        body: { username, password, altchaPayload: altchaPayload ?? "" },
      });
      if (res?.twoFactorRequired) {
        return res;
      }
      setRetryAfter(null);
      await queryClient.invalidateQueries();
      const { data: freshUser } = await refetchUser();
      toast.success(t("login.success.title"));
      redirectUser(router, locale, freshUser ?? user, returnToUrl);
      return res;
    } catch (error) {
      const status = (error as ErrorWithResponse).response?.status;
      if (status === 429) {
        try {
          const parsed: LoginLockedDto = JSON.parse(
            (error as ErrorWithResponse).response?.rawData || "",
          );
          if (parsed?.retryAfter) {
            setRetryAfter(parsed.retryAfter);
          }
        } catch (e) {
          console.log("Failed to parse retryAfter from error response: ", e);
        }
        toast.error(t("login.error.tooManyAttemptsDescription"));
      } else if (status !== 401) {
        toast.error(t("login.error.description"));
      }
      throw error;
    }
  };

  return {
    login,
    retryAfter,
    setRetryAfter,
  };
}
