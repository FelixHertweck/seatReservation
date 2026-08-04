"use client";

import { useParams, useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";
import { postApiAuthRegisterMutation } from "@/api/@tanstack/react-query.gen";
import { RegisterRequestDto } from "@/api";
import { ErrorWithResponse } from "@/components/init-query-client";
import { redirectUser } from "@/lib/redirect-User";
import { useSession } from "./use-session";

export function useRegister() {
  const t = useT();
  const params = useParams();
  const locale = params.locale as string;
  const router = useRouter();
  const queryClient = useQueryClient();
  const { user, refetchUser } = useSession();

  const { mutateAsync: registerMutation } = useMutation({
    ...postApiAuthRegisterMutation(),
  });

  const register = async (
    userData: RegisterRequestDto,
    returnToUrl?: string | null,
  ) => {
    const request = registerMutation({ body: userData });
    toast.promise(request, {
      loading: t("common.loading"),
      success: async () => {
        await queryClient.invalidateQueries();
        const { data: freshUser } = await refetchUser();
        redirectUser(router, locale, freshUser ?? user, returnToUrl);
        return t("register.success.title");
      },
      error: (error: ErrorWithResponse) => ({
        message: t("register.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  return {
    register,
  };
}
