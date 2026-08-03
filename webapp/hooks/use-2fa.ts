"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";
import {
  getApiUsersMe2FaOptions,
  getApiUsersMe2FaQueryKey,
  postApiUsersMe2FaSetupTotpMutation,
  postApiUsersMe2FaSendSetupEmailMutation,
  postApiUsersMe2FaEnableMutation,
  postApiUsersMe2FaDisableMutation,
  putApiUsersMe2FaSettingsMutation,
  postApiUsersMe2FaBackupCodesMutation,
  postApiAuth2FaVerifyMutation,
  postApiAuth2FaResendEmailMutation,
} from "@/api/@tanstack/react-query.gen";
import type {
  TwoFactorMethod,
  TwoFactorEnableDto,
  TwoFactorDisableDto,
  TwoFactorSettingsUpdateDto,
  TwoFactorVerifyRequestDto,
  TwoFactorResendEmailRequestDto,
  TwoFactorRegenerateBackupCodesDto,
} from "@/api";

export function useTwoFactor() {
  const t = useT();
  const queryClient = useQueryClient();

  const {
    data: status,
    isLoading: isStatusLoading,
    refetch: refetchStatus,
  } = useQuery(getApiUsersMe2FaOptions());

  const setupTotpMutation = useMutation({
    ...postApiUsersMe2FaSetupTotpMutation(),
  });

  const sendSetupEmailMutation = useMutation({
    ...postApiUsersMe2FaSendSetupEmailMutation(),
  });

  const enableMutation = useMutation({
    ...postApiUsersMe2FaEnableMutation(),
  });

  const disableMutation = useMutation({
    ...postApiUsersMe2FaDisableMutation(),
  });

  const updateSettingsMutation = useMutation({
    ...putApiUsersMe2FaSettingsMutation(),
  });

  const regenerateBackupCodesMutation = useMutation({
    ...postApiUsersMe2FaBackupCodesMutation(),
  });

  const verifyChallengeMutation = useMutation({
    ...postApiAuth2FaVerifyMutation(),
  });

  const resendEmailCodeMutation = useMutation({
    ...postApiAuth2FaResendEmailMutation(),
  });

  const invalidateStatus = () => {
    return queryClient.invalidateQueries({
      queryKey: getApiUsersMe2FaQueryKey(),
    });
  };

  const setupTotp = async () => {
    try {
      const res = await setupTotpMutation.mutateAsync({});
      return res;
    } catch (error) {
      toast.error(t("twoFactor.error.setupFailed"));
      throw error;
    }
  };

  const sendSetupEmail = async () => {
    try {
      await sendSetupEmailMutation.mutateAsync({});
      toast.success(t("twoFactor.setupEmailSent"));
    } catch (error) {
      toast.error(t("twoFactor.error.sendEmailFailed"));
      throw error;
    }
  };

  const enableTwoFactor = async (method: TwoFactorMethod, code?: string) => {
    try {
      const body: TwoFactorEnableDto = { method, code };
      const res = await enableMutation.mutateAsync({ body });
      await invalidateStatus();
      toast.success(t("twoFactor.enabledSuccess"));
      // Populated only when this call minted a fresh set of backup codes the user hasn't seen
      // yet (e.g. first-time EMAIL activation) -- the caller shows them once, right here.
      return res;
    } catch (error) {
      toast.error(t("twoFactor.error.enableFailed"));
      throw error;
    }
  };

  const disableTwoFactor = async (method: TwoFactorMethod, code: string) => {
    try {
      const body: TwoFactorDisableDto = { method, code };
      await disableMutation.mutateAsync({ body });
      await invalidateStatus();
      toast.success(t("twoFactor.disabledSuccess"));
    } catch (error) {
      toast.error(t("twoFactor.error.disableFailed"));
      throw error;
    }
  };

  const updateSettings = async (twoFactorPasskeyEnabled?: boolean) => {
    try {
      const body: TwoFactorSettingsUpdateDto = { twoFactorPasskeyEnabled };
      await updateSettingsMutation.mutateAsync({ body });
      await invalidateStatus();
      toast.success(t("twoFactor.settingsUpdated"));
    } catch (error) {
      toast.error(t("twoFactor.error.updateFailed"));
      throw error;
    }
  };

  const regenerateBackupCodes = async (code: string) => {
    try {
      const body: TwoFactorRegenerateBackupCodesDto = { code };
      const res = await regenerateBackupCodesMutation.mutateAsync({ body });
      await invalidateStatus();
      toast.success(t("twoFactor.backupCodesRegenerated"));
      return res;
    } catch (error) {
      toast.error(t("twoFactor.error.backupCodesFailed"));
      throw error;
    }
  };

  const verifyChallenge = async (challengeToken: string, code: string) => {
    try {
      const body: TwoFactorVerifyRequestDto = { challengeToken, code };
      return await verifyChallengeMutation.mutateAsync({ body });
    } catch (error) {
      toast.error(t("twoFactor.error.invalidCode"));
      throw error;
    }
  };

  const resendChallengeEmail = async (challengeToken: string) => {
    try {
      const body: TwoFactorResendEmailRequestDto = { challengeToken };
      await resendEmailCodeMutation.mutateAsync({ body });
      toast.success(t("twoFactor.emailCodeResent"));
    } catch (error) {
      toast.error(t("twoFactor.error.resendEmailFailed"));
      throw error;
    }
  };

  return {
    status,
    isStatusLoading,
    refetchStatus,
    setupTotp,
    sendSetupEmail,
    enableTwoFactor,
    disableTwoFactor,
    updateSettings,
    regenerateBackupCodes,
    verifyChallenge,
    resendChallengeEmail,
    isSetupLoading:
      setupTotpMutation.isPending || sendSetupEmailMutation.isPending,
    isEnableLoading: enableMutation.isPending,
    isDisableLoading: disableMutation.isPending,
    isRegenerateLoading: regenerateBackupCodesMutation.isPending,
  };
}
