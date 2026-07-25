import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  getApiUser2FaSettingsOptions,
  putApiUser2FaSettingsMutation,
  postApiUser2FaSetupTotpMutation,
  postApiUser2FaEnableMutation,
  deleteApiUser2FaDisableMutation,
} from "@/api/@tanstack/react-query.gen";
import { useT } from "@/lib/i18n/hooks";
import { TwoFactorSettingsDto } from "@/api";

export function useTwoFactor() {
  const t = useT();
  const queryClient = useQueryClient();

  const { data: settings, isLoading } = useQuery(
    getApiUser2FaSettingsOptions(),
  );

  const updateSettingsMutation = useMutation({
    ...putApiUser2FaSettingsMutation(),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: getApiUser2FaSettingsOptions().queryKey,
      }),
  });

  const setupTotpMutation = useMutation({
    ...postApiUser2FaSetupTotpMutation(),
  });

  const enableMutation = useMutation({
    ...postApiUser2FaEnableMutation(),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: getApiUser2FaSettingsOptions().queryKey,
      }),
  });

  const disableMutation = useMutation({
    ...deleteApiUser2FaDisableMutation(),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: getApiUser2FaSettingsOptions().queryKey,
      }),
  });

  return {
    settings,
    isLoading,
    updateSettings: async (data: TwoFactorSettingsDto) => {
      const p = updateSettingsMutation.mutateAsync({ body: data });
      toast.promise(p, {
        loading: t("common.loading"),
        success: t("profilePage.successUpdate"),
        error: t("common.error.default"),
      });
      return p;
    },
    setupTotp: async () => {
      return setupTotpMutation.mutateAsync({});
    },
    enable: async (code: string) => {
      const p = enableMutation.mutateAsync({ body: { code } });
      toast.promise(p, {
        loading: t("common.loading"),
        success: t("profilePage.twoFactorEnabled"),
        error: t("common.error.default"),
      });
      return p;
    },
    disable: async () => {
      const p = disableMutation.mutateAsync({});
      toast.promise(p, {
        loading: t("common.loading"),
        success: t("profilePage.twoFactorDisabled"),
        error: t("common.error.default"),
      });
      return p;
    },
  };
}
