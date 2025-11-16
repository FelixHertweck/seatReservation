"use client";

import { useParams, useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/hooks/use-toast";
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
  type RegisterRequestDto,
  type RegistrationStatusDto,
  type VerifyEmailCodeRequestDto,
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
    onError: (error) => {
      const errorWithType = error as ErrorWithResponse;
      const errorResponse = errorWithType.response;
      const status = errorResponse?.status;
      if (status === 429) {
        setRetryAfter(errorResponse?.data?.retryAfter ?? null);
        toast({
          title: t("login.error.title"),
          description: t("login.error.tooManyAttemptsDescription"),
          variant: "destructive",
        });
      }
      // Only show toast for non-401 errors, let 401s be handled by the component
      else if (status !== 401) {
        toast({
          title: t("login.error.title"),
          description:
            (error as ErrorWithResponse).message ||
            t("login.error.description"),
          variant: "destructive",
        });
      }
    },
  });

  const login = async (
    username: string,
    password: string,
    returnToUrl?: string | null,
  ) => {
    await loginMutation({ body: { username, password } });
    setRetryAfter(null);
    await queryClient.invalidateQueries();
    await refetchUser();
    redirectUser(router, locale, user, returnToUrl);
  };

  const { mutateAsync: registerMutation } = useMutation({
    ...postApiAuthRegisterMutation(),
  });

  const register = async (
    userData: RegisterRequestDto,
    returnToUrl?: string | null,
  ) => {
    await registerMutation({ body: userData });
    await queryClient.invalidateQueries();
    await refetchUser();
    redirectUser(router, locale, user, returnToUrl);
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

  const { mutateAsync: logoutAllMutation } = useMutation({
    ...postApiAuthLogoutAllDevicesMutation(),
    onSuccess: async () => {
      toast({
        title: t("logoutAll.success.title"),
        description: t("logoutAll.success.description"),
      });
    },
  });

  const logoutAll = async () => {
    await logoutAllMutation({});
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
    redirectUser(router, locale, user, returnToUrl);
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
