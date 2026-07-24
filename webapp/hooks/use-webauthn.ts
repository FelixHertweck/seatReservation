"use client";

import { useParams, useRouter } from "next/navigation";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";
import {
  getApiUsersMeOptions,
  getApiAuthWebauthnStatusOptions,
  getApiAuthWebauthnStatusQueryKey,
  getApiAuthWebauthnCredentialsOptions,
  getApiAuthWebauthnCredentialsQueryKey,
} from "@/api/@tanstack/react-query.gen";
import {
  postApiAuthWebauthnLoginOptions,
  postApiAuthWebauthnLogin,
  postApiAuthWebauthnRegisterOptions,
  postApiAuthWebauthnRegister,
  postApiAuthWebauthnRegisterNewOptions,
  postApiAuthWebauthnRegisterNew,
  deleteApiAuthWebauthnCredentialsById,
  putApiAuthWebauthnCredentialsById,
  type WebAuthnRegistrationStartDto,
} from "@/api";
import { ErrorWithResponse } from "@/components/init-query-client";
import { redirectUser } from "@/lib/redirect-User";
import {
  createCredential,
  getAssertion,
  isPasskeySupported,
  PasskeyCeremonyCancelledError,
} from "@/lib/webauthn";

/**
 * Passkey (WebAuthn) flows: passwordless login, passkey-only account creation,
 * and credential management for the signed-in user. Mirrors the conventions in
 * {@link useAuth} (react-query cache + toast feedback + role-based redirect) and
 * reuses the cookie-based JWT session the backend sets on a successful ceremony.
 */
export function useWebAuthn() {
  const t = useT();
  const params = useParams();
  const locale = params.locale as string;
  const router = useRouter();
  const queryClient = useQueryClient();

  const { data: user, refetch: refetchUser } = useQuery(getApiUsersMeOptions());

  const isSupported = isPasskeySupported();

  // Maps ceremony/backend failures to a user-facing message. Returns null when
  // the user simply cancelled the native prompt (no toast needed).
  const messageForError = (error: unknown): string | null => {
    if (error instanceof PasskeyCeremonyCancelledError) {
      return null;
    }
    const status = (error as ErrorWithResponse).response?.status;
    switch (status) {
      case 401:
        return t("webauthn.error.invalidCredential");
      case 403:
        return t("webauthn.error.registrationDisabled");
      case 409:
        return t("webauthn.error.usernameTaken");
      default:
        return t("webauthn.error.generic");
    }
  };

  const runCeremony = async (fn: () => Promise<void>) => {
    try {
      await fn();
    } catch (error) {
      const message = messageForError(error);
      if (message) {
        console.error("Passkey ceremony failed:", error);
        toast.error(message);
      }
      throw error;
    }
  };

  /**
   * Passwordless login. With no username this relies on a discoverable
   * (resident) credential; a username narrows the allowed credentials.
   */
  const loginWithPasskey = async (
    username?: string,
    returnToUrl?: string | null,
  ) => {
    return runCeremony(async () => {
      const trimmed = username?.trim();
      const { data: optionsJson } = await postApiAuthWebauthnLoginOptions({
        query: trimmed ? { username: trimmed } : undefined,
      });
      const assertion = await getAssertion(optionsJson);
      await postApiAuthWebauthnLogin({
        body: assertion,
        bodySerializer: null,
      });
      await queryClient.invalidateQueries();
      const { data: fresh } = await refetchUser();
      toast.success(t("webauthn.login.success"));
      redirectUser(router, locale, fresh ?? user, returnToUrl);
    });
  };

  /**
   * Creates a brand-new, passkey-only account and logs in. Password is not
   * required, but the backend still requires username, firstname, lastname,
   * and email.
   */
  const registerNewWithPasskey = async (
    registration: WebAuthnRegistrationStartDto,
    returnToUrl?: string | null,
  ) => {
    return runCeremony(async () => {
      const { data: optionsJson } = await postApiAuthWebauthnRegisterNewOptions(
        {
          body: registration,
        },
      );
      const credential = await createCredential(optionsJson);
      await postApiAuthWebauthnRegisterNew({
        body: JSON.stringify({
          registration,
          credential: JSON.parse(credential),
        }),
        bodySerializer: null,
      });
      await queryClient.invalidateQueries();
      const { data: fresh } = await refetchUser();
      toast.success(t("webauthn.register.success"));
      redirectUser(router, locale, fresh ?? user, returnToUrl);
    });
  };

  /** Adds a passkey to the currently signed-in account. */
  const addPasskey = async () => {
    return runCeremony(async () => {
      const { data: optionsJson } = await postApiAuthWebauthnRegisterOptions();
      const credential = await createCredential(optionsJson);
      await postApiAuthWebauthnRegister({
        body: credential,
        bodySerializer: null,
      });
      await queryClient.invalidateQueries({
        queryKey: getApiAuthWebauthnCredentialsQueryKey(),
      });
      await queryClient.invalidateQueries({
        queryKey: getApiAuthWebauthnStatusQueryKey(),
      });
      toast.success(t("webauthn.manage.addSuccess"));
    });
  };

  const deleteCredential = async (id: string) => {
    try {
      await deleteApiAuthWebauthnCredentialsById({ path: { id } });
      await queryClient.invalidateQueries({
        queryKey: getApiAuthWebauthnCredentialsQueryKey(),
      });
      await queryClient.invalidateQueries({
        queryKey: getApiAuthWebauthnStatusQueryKey(),
      });
      toast.success(t("webauthn.manage.deleteSuccess"));
    } catch (error) {
      const status = (error as ErrorWithResponse).response?.status;
      if (status === 409) {
        toast.error(t("webauthn.manage.deleteLastError"));
      } else if (status === 404) {
        toast.error(t("webauthn.manage.deleteNotFound"));
      } else {
        toast.error(t("webauthn.error.generic"));
      }
      throw error;
    }
  };

  const renameCredential = async (id: string, label: string) => {
    try {
      await putApiAuthWebauthnCredentialsById({
        path: { id },
        body: { label },
      });
      await queryClient.invalidateQueries({
        queryKey: getApiAuthWebauthnCredentialsQueryKey(),
      });
      toast.success(t("webauthn.manage.renameSuccess"));
    } catch (error) {
      const status = (error as ErrorWithResponse).response?.status;
      if (status === 404) {
        toast.error(t("webauthn.manage.deleteNotFound"));
      } else {
        toast.error(t("webauthn.error.generic"));
      }
      throw error;
    }
  };

  return {
    isSupported,
    loginWithPasskey,
    registerNewWithPasskey,
    addPasskey,
    deleteCredential,
    renameCredential,
  };
}

/**
 * Status + credential list for the signed-in user's passkey management UI.
 * @param enabled set to false to skip both queries, e.g. when the browser doesn't support passkeys
 */
export function useWebAuthnStatus(enabled = true) {
  const { data: status, isLoading: isStatusLoading } = useQuery({
    ...getApiAuthWebauthnStatusOptions(),
    enabled,
  });

  const {
    data: credentials,
    isLoading: isCredentialsLoading,
    isSuccess: isCredentialsSuccess,
  } = useQuery({
    ...getApiAuthWebauthnCredentialsOptions(),
    enabled,
  });

  return {
    status,
    isStatusLoading,
    credentials,
    isCredentialsLoading,
    isCredentialsSuccess,
  };
}
