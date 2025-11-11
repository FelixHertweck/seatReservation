"use client";

import { Dispatch, SetStateAction, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/hooks/use-toast";
import { useT } from "@/lib/i18n/hooks";
import {
  getApiSupervisorCheckinUsernamesOptions,
  postApiSupervisorCheckinInfoMutation,
  postApiSupervisorCheckinInfoByUsernameMutation,
  postApiSupervisorCheckinProcessMutation,
} from "@/api/@tanstack/react-query.gen";
import type {
  CheckInInfoRequestDto,
  CheckInProcessRequestDto,
  CheckInInfoResponseDto,
} from "@/api";
import { ErrorWithResponse } from "@/components/init-query-client";

export interface UseCheckinReturn {
  // Usernames
  usernames: string[] | undefined;
  isLoadingUsernames: boolean;

  // Check-in info
  fetchCheckInInfo: (
    request: CheckInInfoRequestDto,
  ) => Promise<CheckInInfoResponseDto | undefined>;
  isLoadingInfo: boolean;

  // Check-in info by username
  fetchCheckInInfoByUsername: (
    username: string,
  ) => Promise<CheckInInfoResponseDto | undefined>;
  isLoadingInfoByUsername: boolean;

  // Process check-in
  performCheckIn: (
    checkInData: CheckInProcessRequestDto,
  ) => Promise<void | undefined>;
  isLoadingPerformCheckIn: boolean;

  isLoadingAll: boolean;
  isErrorAll: boolean;
  errorAll: Error | null;
}

export function useCheckin(): UseCheckinReturn {
  const t = useT();

  // GET endpoint: Fetch usernames
  const {
    data: usernames,
    isLoading: isLoadingUsernames,
    isError: isErrorUsernames,
    error: errorUsernames,
  } = useQuery({
    ...getApiSupervisorCheckinUsernamesOptions(),
  });

  // POST endpoint: Fetch check-in info
  const checkInInfoMutation = useMutation({
    ...postApiSupervisorCheckinInfoMutation(),
    onError: async (error) => {
      const errorWithType = error as ErrorWithResponse;
      const errorMessage =
        errorWithType?.response?.data?.message || t("checkin.error.default");
      toast({
        title: t("checkin.error.title"),
        description: errorMessage,
        variant: "destructive",
      });
    },
  });

  const fetchCheckInInfo = async (
    checkinInfoRequest: CheckInInfoRequestDto,
  ) => {
    try {
      const result = await checkInInfoMutation.mutateAsync({
        body: checkinInfoRequest,
      });
      return result;
    } catch (error) {
      console.error("Check-In info error:", error);
      return undefined;
    }
  };

  // POST endpoint: Fetch check-in info by username
  const checkInInfoByUsernameMutation = useMutation({
    ...postApiSupervisorCheckinInfoByUsernameMutation(),
    onError: async (error) => {
      const errorWithType = error as ErrorWithResponse;
      const errorMessage =
        errorWithType?.response?.data?.message || t("checkin.error.default");
      toast({
        title: t("checkin.error.title"),
        description: errorMessage,
        variant: "destructive",
      });
    },
  });

  const fetchCheckInInfoByUsername = async (username: string) => {
    try {
      const result = await checkInInfoByUsernameMutation.mutateAsync({
        path: {
          username,
        },
      });
      return result;
    } catch (error) {
      console.error("Check-In info by username error:", error);
      return undefined;
    }
  };

  // POST endpoint: Process check-in
  const checkInMutation = useMutation({
    ...postApiSupervisorCheckinProcessMutation(),
    onSuccess: async () => {
      toast({
        title: t("checkin.success.title"),
        description: t("checkin.success.description"),
      });
    },
    onError: async (error) => {
      const errorWithType = error as ErrorWithResponse;
      const errorMessage =
        errorWithType?.response?.data?.message || t("checkin.error.default");
      toast({
        title: t("checkin.error.title"),
        description: errorMessage,
        variant: "destructive",
      });
    },
  });

  const performCheckIn = async (checkInData: CheckInProcessRequestDto) => {
    try {
      const result = await checkInMutation.mutateAsync({
        body: checkInData,
      });
      return result;
    } catch (error) {
      console.error("Check-In error:", error);
      return undefined;
    }
  };

  const isLoadingAll =
    isLoadingUsernames ||
    checkInInfoMutation.isPending ||
    checkInInfoByUsernameMutation.isPending ||
    checkInMutation.isPending;

  const isErrorAll =
    isErrorUsernames ||
    checkInInfoMutation.isError ||
    checkInInfoByUsernameMutation.isError ||
    checkInMutation.isError;

  const errorAll =
    errorUsernames ||
    checkInInfoMutation.error ||
    checkInInfoByUsernameMutation.error ||
    checkInMutation.error;

  return {
    // Usernames (GET)
    usernames,
    isLoadingUsernames,

    // Check-in info (POST to fetch data)
    fetchCheckInInfo,
    isLoadingInfo: checkInInfoMutation.isPending,

    // Check-in info by username (POST)
    fetchCheckInInfoByUsername,
    isLoadingInfoByUsername: checkInInfoByUsernameMutation.isPending,

    // Process check-in (POST)
    performCheckIn,
    isLoadingPerformCheckIn: checkInMutation.isPending,

    isLoadingAll,
    isErrorAll,
    errorAll,
  };
}
