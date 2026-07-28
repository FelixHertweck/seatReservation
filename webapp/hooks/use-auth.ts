"use client";

import { useSession, type RegistrationStatus } from "./auth/use-session";
import { useLogin } from "./auth/use-login";
import { useRegister } from "./auth/use-register";
import { useLogout } from "./auth/use-logout";
import { useVerifyEmail } from "./auth/use-verify-email";
import { usePasswordReset } from "./auth/use-password-reset";
import { useUsernameRecovery } from "./auth/use-username-recovery";

export function useAuth() {
  const { user, isLoading, isLoggedIn, registrationStatus } = useSession();
  const { login, retryAfter } = useLogin();
  const { register } = useRegister();
  const { logout, logoutAll } = useLogout();
  const { verifyEmail, resendConfirmation } = useVerifyEmail();
  const { requestPasswordReset, confirmPasswordReset } = usePasswordReset();
  const { requestUsernameRecovery } = useUsernameRecovery();

  return {
    user,
    isLoggedIn,
    isLoading,
    registrationStatus,
    login,
    register,
    logout,
    logoutAll,
    verifyEmail,
    resendConfirmation,
    requestPasswordReset,
    requestUsernameRecovery,
    confirmPasswordReset,
    retryAfter,
  };
}

export type { RegistrationStatus };
