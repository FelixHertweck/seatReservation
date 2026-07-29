"use client";

import { useParams, useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
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

  return {
    logout,
    logoutAll,
  };
}
