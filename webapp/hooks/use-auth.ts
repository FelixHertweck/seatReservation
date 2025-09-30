"use client";

import { useParams, useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/hooks/use-toast";
import { useT } from "@/lib/i18n/hooks";
import {
  getApiUsersMeOptions,
  postApiAuthLoginMutation,
  postApiAuthLogoutMutation,
  postApiAuthRegisterMutation,
  postApiUserResendEmailConfirmationMutation,
  postApiUserVerifyEmailCodeMutation,
} from "@/api/@tanstack/react-query.gen";
import type { RegisterRequestDto, VerifyEmailCodeRequestDto } from "@/api";
import type { AppRouterInstance } from "next/dist/shared/lib/app-router-context.shared-runtime";
import { isValidRedirectUrlEncoded } from "@/lib/utils";

export function useAuth() {
  const t = useT();
  const params = useParams();
  const locale = params.locale as string;

  const router = useRouter();
  const queryClient = useQueryClient();

  const {
    data: user,
    isLoading,
    isSuccess,
    refetch: refetchUser,
  } = useQuery(getApiUsersMeOptions());

  const { mutateAsync: loginMutation } = useMutation({
    ...postApiAuthLoginMutation(),
    onSuccess: async () => {
      await refetchUser();
    },
    onError: (error: any) => {
      // Only show toast for non-401 errors, let 401s be handled by the component
      if (error?.response?.status !== 401) {
        toast({
          title: t("login.error.title"),
          description: error.message || t("login.error.description"),
          variant: "destructive",
        });
      }
    },
  });

  const login = async (
    identifier: string,
    password: string,
    returnToUrl?: string | null,
  ) => {
    await loginMutation({ body: { identifier, password } });
    await queryClient.invalidateQueries();
    redirectUser(router, locale, returnToUrl);
  };

  const { mutateAsync: registerMutation } = useMutation({
    ...postApiAuthRegisterMutation(),
    onSuccess: async () => {
      await refetchUser();
    },
  });

  const register = async (
    userData: RegisterRequestDto,
    returnToUrl?: string | null,
  ) => {
    await registerMutation({ body: userData });
    await queryClient.invalidateQueries();
    redirectUser(router, locale, returnToUrl);
  };

  const { mutateAsync: logoutMutation } = useMutation({
    ...postApiAuthLogoutMutation(),
    onSuccess: async () => {
      toast({
        title: t("logout.success.title"),
        description: t("logout.success.description"),
      });
    },
  });

  const logout = async () => {
    await logoutMutation({});
    router.push(`/${locale}/`);
    router.refresh();
  };

  const { mutateAsync: verifyEmailMutation } = useMutation({
    ...postApiUserVerifyEmailCodeMutation(),
    onSuccess: async () => {
      await refetchUser();
    },
  });

  const verifyEmail = async (code: string, returnToUrl?: string | null) => {
    const verificationDto: VerifyEmailCodeRequestDto = {
      verificationCode: code,
    };
    await verifyEmailMutation({ body: verificationDto });
    await queryClient.invalidateQueries();
    redirectUser(router, locale, returnToUrl);
  };

  const resendConfirmationMutation = useMutation({
    ...postApiUserResendEmailConfirmationMutation(),
  });

  const resendConfirmation = async (): Promise<void> => {
    await resendConfirmationMutation.mutateAsync({});
    toast({
      title: t("email.confirmationEmailSentTitle"),
      description: t("email.confirmationEmailSentDescription"),
    });
  };

  return {
    user,
    isLoggedIn: isSuccess,
    isLoading,
    login,
    register,
    logout,
    verifyEmail,
    resendConfirmation,
  };
}

function redirectUser(
  router: AppRouterInstance,
  locale: string,
  returnToUrl?: string | null,
) {
  router.push(
    returnToUrl && isValidRedirectUrlEncoded(returnToUrl)
      ? decodeURIComponent(returnToUrl)
      : `/${locale}/events`,
  );
}
