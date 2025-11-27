"use client";

import { useParams, useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";
import {
  getApiAuthRegistrationStatusOptions,
  getApiUsersMeOptions,
  postApiAuthLoginMutation,
  postApiAuthLogoutAllDevicesMutation,
  postApiAuthLogoutMutation,
  postApiAuthRegisterMutation,
  postApiUserResendEmailConfirmationMutation,
  postApiUserVerifyEmailCodeMutation,
} from "@/api/@tanstack/react-query.gen";
import {
  Instant,
  LoginLockedDto,
  type RegisterRequestDto,
  type RegistrationStatusDto,
} from "@/api";
import { ErrorWithResponse } from "@/components/init-query-client";
import { useState } from "react";
import { redirectUser } from "@/lib/redirect-User";

export function useAuth() {
  const t = useT();
  const params = useParams();
  const locale = params.locale as string;

  const router = useRouter();
  const queryClient = useQueryClient();

  const [retryAfter, setRetryAfter] = useState<Instant | null>(null);

  const {
    data: user,
    isLoading,
    isSuccess,
    refetch: refetchUser,
  } = useQuery(getApiUsersMeOptions());

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
            // parse json
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
        }
        // Only show toast for non-401 errors, let 401s be handled by the component
        else if (status !== 401) {
          return t("login.error.description");
        }
        return t("common.error.default"); // Fallback
      },
    });

    return request;
  };

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
        await refetchUser();
        redirectUser(router, locale, user, returnToUrl);
        return t("register.success.title");
      },
      error: (error: ErrorWithResponse) => ({
        message: t("register.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const { mutateAsync: logoutMutation } = useMutation({
    ...postApiAuthLogoutMutation(),
  });

  const logout = async () => {
    const request = logoutMutation({});
    toast.promise(request, {
      loading: t("common.loading"),
      success: () => {
        router.push(`/${locale}/`);
        router.refresh();
        return t("logout.success.title");
      },
      error: (error: ErrorWithResponse) => ({
        message: t("logout.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });

    return request;
  };

  const { mutateAsync: logoutAllMutation } = useMutation({
    ...postApiAuthLogoutAllDevicesMutation(),
  });

  const logoutAll = async () => {
    const request = logoutAllMutation({});
    toast.promise(request, {
      loading: t("common.loading"),
      success: () => {
        router.push(`/${locale}/`);
        router.refresh();

        return t("logoutAll.success.title");
      },
      error: (error: ErrorWithResponse) => ({
        message: t("logoutAll.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const { mutateAsync: verifyEmailMutation } = useMutation({
    ...postApiUserVerifyEmailCodeMutation(),
  });

  const verifyEmail = async (code: string, returnToUrl?: string | null) => {
    const request = verifyEmailMutation({ body: { verificationCode: code } });
    toast.promise(request, {
      loading: t("common.loading"),
      success: async () => {
        await queryClient.invalidateQueries();
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

  const {
    data: registrationStatus,
    isLoading: isLoadingRegistrationStatus,
    isSuccess: isSuccessRegistrationStatus,
  } = useQuery({
    ...getApiAuthRegistrationStatusOptions(),
    staleTime: Infinity,
    gcTime: Infinity,
  });

  return {
    user,
    isLoggedIn: isSuccess,
    isLoading,
    registrationStatus: {
      data: registrationStatus,
      isLoading: isLoadingRegistrationStatus,
      isSuccess: isSuccessRegistrationStatus,
    } as RegistrationStatus,
    login,
    register,
    logout,
    logoutAll,
    verifyEmail,
    resendConfirmation,
    retryAfter,
  };
}

interface RegistrationStatus {
  data: RegistrationStatusDto | undefined;
  isLoading: boolean;
  isSuccess: boolean;
}
