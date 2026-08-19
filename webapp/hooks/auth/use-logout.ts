"use client";

import { useParams, useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";
import {
  postApiAuthLogoutAllDevicesMutation,
  postApiAuthLogoutMutation,
} from "@/api/@tanstack/react-query.gen";
import { ErrorWithResponse } from "@/components/init-query-client";

export function useLogout() {
  const t = useT();
  const params = useParams();
  const locale = params.locale as string;
  const router = useRouter();
  const queryClient = useQueryClient();

  const { mutateAsync: logoutMutation } = useMutation({
    ...postApiAuthLogoutMutation(),
  });

  const logout = async () => {
    const handleSuccess = () => {
      queryClient.clear();
      router.push(`/${locale}/`);
      router.refresh();
      return t("logout.success.title");
    };

    const request = logoutMutation({}).catch((error) => {
      if ((error as ErrorWithResponse)?.response?.status === 401) {
        return;
      }
      throw error;
    });

    toast.promise(request, {
      loading: t("common.loading"),
      success: handleSuccess,
      error: (error: ErrorWithResponse) => {
        queryClient.clear();
        router.push(`/${locale}/`);
        return {
          message: t("logout.error.title"),
          description: error.response?.description ?? t("common.error.default"),
        };
      },
    });

    return request;
  };

  const { mutateAsync: logoutAllMutation } = useMutation({
    ...postApiAuthLogoutAllDevicesMutation(),
  });

  const logoutAll = async () => {
    const handleSuccess = () => {
      queryClient.clear();
      router.push(`/${locale}/`);
      router.refresh();
      return t("logoutAll.success.title");
    };

    const request = logoutAllMutation({}).catch((error) => {
      if ((error as ErrorWithResponse)?.response?.status === 401) {
        return;
      }
      throw error;
    });

    toast.promise(request, {
      loading: t("common.loading"),
      success: handleSuccess,
      error: (error: ErrorWithResponse) => {
        queryClient.clear();
        router.push(`/${locale}/`);
        return {
          message: t("logoutAll.error.title"),
          description: error.response?.description ?? t("common.error.default"),
        };
      },
    });

    return request;
  };

  return {
    logout,
    logoutAll,
  };
}
