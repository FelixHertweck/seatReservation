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
} from "@/api/@tanstack/react-query.gen";
import type { RegisterRequestDto } from "@/api";
import { AppRouterInstance } from "next/dist/shared/lib/app-router-context.shared-runtime";

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
      redirectUser(router, locale);
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

  const login = async (identifier: string, password: string) => {
    await loginMutation({ body: { identifier, password } });
    await queryClient.invalidateQueries();
  };

  const { mutateAsync: registerMutation } = useMutation({
    ...postApiAuthRegisterMutation(),
    onSuccess: async () => {
      await refetchUser();
      redirectUser(router, locale);
    },
  });

  const register = async (userData: RegisterRequestDto) => {
    await registerMutation({ body: userData });
    await queryClient.invalidateQueries();
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
    window.location.href = `/${locale}/`;
  };

  return {
    user,
    isLoggedIn: isSuccess,
    isLoading,
    login,
    register,
    logout,
  };
}

function redirectUser(router: AppRouterInstance, locale: string) {
  const urlParams = new URLSearchParams(window.location.search);
  const returnToUrl = urlParams.get("returnTo");
  router.push(
    returnToUrl ? decodeURIComponent(returnToUrl) : `/${locale}/events`,
  );
}
