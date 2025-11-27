"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";
import {
  postApiSupervisorCheckinInfoMutation,
  postApiSupervisorCheckinInfoByUsernameMutation,
  postApiSupervisorCheckinProcessMutation,
  getApiSupervisorCheckinEventsOptions,
} from "@/api/@tanstack/react-query.gen";
import { getApiSupervisorCheckinUsernamesByEventId } from "@/api/sdk.gen";
import type {
  CheckInInfoRequestDto,
  CheckInProcessRequestDto,
  CheckInInfoResponseDto,
  SupervisorEventResponseDto,
} from "@/api";
import { ErrorWithResponse } from "@/components/init-query-client";

export interface UseCheckinReturn {
  // Usernames
  events: SupervisorEventResponseDto[] | undefined;
  isLoadingEvents: boolean;

  // Usernames by event ID
  getUsernamesByEventId: (eventId: bigint) => Promise<string[] | undefined>;

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
  const queryClient = useQueryClient();

  // GET endpoint: Fetch usernames
  const {
    data: events,
    isLoading: isLoadingEvents,
    error: errorEvents,
    isError: isErrorEvents,
  } = useQuery({
    ...getApiSupervisorCheckinEventsOptions(),
  });

  const getUsernamesByEventId = (eventId: bigint) => {
    // Use a manually-constructed, serializable queryKey (stringify the id)
    // but call the SDK function with the bigint eventId in the queryFn.
    const queryKey = [
      "getApiSupervisorCheckinUsernamesByEventId",
      { eventId: eventId.toString() },
    ];
    return queryClient.fetchQuery({
      queryKey,
      queryFn: async ({ signal }) => {
        const { data } = await getApiSupervisorCheckinUsernamesByEventId({
          path: { eventId },
          signal,
          throwOnError: true,
        });
        return data as string[] | undefined;
      },
    });
  };

  // POST endpoint: Fetch check-in info
  const checkInInfoMutation = useMutation({
    ...postApiSupervisorCheckinInfoMutation(),
  });

  const fetchCheckInInfo = async (
    checkinInfoRequest: CheckInInfoRequestDto,
  ) => {
    const request = checkInInfoMutation.mutateAsync({
      body: checkinInfoRequest,
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: () => t("checkin.info.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("checkin.info.error.title"),
        description: error.response?.description ?? t("checkin.error.default"),
      }),
    });
    return request;
  };

  // POST endpoint: Fetch check-in info by username
  const checkInInfoByUsernameMutation = useMutation({
    ...postApiSupervisorCheckinInfoByUsernameMutation(),
  });

  const fetchCheckInInfoByUsername = async (username: string) => {
    const request = checkInInfoByUsernameMutation.mutateAsync({
      path: {
        username,
      },
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: () => t("checkin.info.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("checkin.info.error.title"),
        description: error.response?.description ?? t("checkin.error.default"),
      }),
    });
    return request;
  };

  // POST endpoint: Process check-in
  const checkInMutation = useMutation({
    ...postApiSupervisorCheckinProcessMutation(),
  });

  const performCheckIn = async (checkInData: CheckInProcessRequestDto) => {
    const request = checkInMutation.mutateAsync({
      body: checkInData,
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("checkin.perform.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("checkin.perform.error.title"),
        description: error.response?.description ?? t("checkin.error.default"),
      }),
    });
    return request;
  };

  const isLoadingAll =
    isLoadingEvents ||
    checkInInfoMutation.isPending ||
    checkInInfoByUsernameMutation.isPending ||
    checkInMutation.isPending;

  const isErrorAll =
    isErrorEvents ||
    checkInInfoMutation.isError ||
    checkInInfoByUsernameMutation.isError ||
    checkInMutation.isError;

  const errorAll =
    errorEvents ||
    checkInInfoMutation.error ||
    checkInInfoByUsernameMutation.error ||
    checkInMutation.error;

  return {
    // Usernames (GET)
    events,
    isLoadingEvents,

    // Usernames by event ID (GET)
    getUsernamesByEventId,

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
