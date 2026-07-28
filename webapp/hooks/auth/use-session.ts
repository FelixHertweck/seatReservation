"use client";

import { useQuery } from "@tanstack/react-query";
import {
  getApiUsersMeOptions,
  getApiAuthRegistrationStatusOptions,
} from "@/api/@tanstack/react-query.gen";
import { RegistrationStatusDto } from "@/api";

export interface RegistrationStatus {
  data: RegistrationStatusDto | undefined;
  isLoading: boolean;
  isSuccess: boolean;
}

export function useSession() {
  const {
    data: user,
    isLoading,
    isSuccess,
    refetch: refetchUser,
  } = useQuery(getApiUsersMeOptions());

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
    isLoading,
    isLoggedIn: isSuccess,
    refetchUser,
    registrationStatus: {
      data: registrationStatus,
      isLoading: isLoadingRegistrationStatus,
      isSuccess: isSuccessRegistrationStatus,
    } as RegistrationStatus,
  };
}
