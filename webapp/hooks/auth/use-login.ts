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
  ) => {
    const request = loginMutation({ body: { username, password } });
    toast.promise(request, {
      loading: t("common.loading"),
      success: async () => {
        setRetryAfter(null);
        await queryClient.invalidateQueries();
        await refetchUser();
        redirectUser(router, locale, user, returnToUrl);
        return t("login.success.title");
      },
      error: (error: ErrorWithResponse) => {
        const status = error.response?.status;
        if (status === 429) {
          try {
            const parsed: LoginLockedDto = JSON.parse(error.response?.rawData);
            if (parsed?.retryAfter) {
              setRetryAfter(parsed.retryAfter);
            }
          } catch (error) {
            console.log(
              "Failed to parse retryAfter from error response: ",
              error,
            );
          }
          return t("login.error.tooManyAttemptsDescription");
        } else if (status !== 401) {
          return t("login.error.description");
        }
        return t("common.error.default");
      },
    });

    return request;
  };

  return {
    login,
    retryAfter,
    setRetryAfter,
  };
}
