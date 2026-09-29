"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";
import {
  getApiUserEventsOptions,
  postApiUserReservationsMutation,
  getApiUserEventsQueryKey,
  getApiUserEventsByIdOptions,
  getApiUserEventsByIdQueryKey,
  getApiUserReservationsQueryKey,
  getApiUserLocationsOptions,
} from "@/api/@tanstack/react-query.gen";
import type {
  UserEventLocationResponseDto,
  UserEventResponseDto,
  UserReservationResponseDto,
  UserReservationsRequestDto,
} from "@/api";
import { isReservationEmailRequiredError } from "@/lib/reservation-errors";

interface UseEventsReturn {
  events: UserEventResponseDto[];
  locations: UserEventLocationResponseDto[];
  getEventById: (id: string) => Promise<UserEventResponseDto>;
  isLoading: boolean;
  createReservation: (
    eventId: string,
    seatIds: string[],
  ) => Promise<UserReservationResponseDto[]>;
}

export function useEvents(): UseEventsReturn {
  const t = useT();
  const { data: events, isLoading: eventsIsLoading } = useQuery({
    ...getApiUserEventsOptions(),
  });

  const { data: locations, isLoading: locationsIsLoading } = useQuery({
    ...getApiUserLocationsOptions(),
  });

  const queryClient = useQueryClient();

  const createReservationMutation = useMutation({
    ...postApiUserReservationsMutation(),
  });

  const createReservation = async (
    eventId: string,
    seatIds: string[],
  ): Promise<UserReservationResponseDto[]> => {
    const data: UserReservationsRequestDto = {
      eventId,
      seatIds,
    };
    const toastId = toast.loading(t("common.loading"));
    try {
      const resultData = await createReservationMutation.mutateAsync({
        body: data,
      });
      queryClient.setQueriesData(
        { queryKey: getApiUserReservationsQueryKey() },
        (oldData: UserReservationResponseDto[] | undefined) => {
          return oldData ? [...oldData, ...resultData] : [...resultData];
        },
      );
      queryClient.invalidateQueries({
        queryKey: getApiUserEventsQueryKey(),
      });
      queryClient.invalidateQueries({
        queryKey: getApiUserEventsByIdQueryKey({ path: { id: eventId } }),
      });
      toast.success(t("reservation.create.success.title"), { id: toastId });
      return resultData;
    } catch (error) {
      // The caller shows a dedicated dialog for this case instead of a toast.
      if (isReservationEmailRequiredError(error)) {
        toast.dismiss(toastId);
      } else {
        toast.error(t("reservation.create.error.title"), { id: toastId });
      }
      throw error;
    }
  };

  const getEventById = (eventId: string) => {
    return queryClient.fetchQuery({
      ...getApiUserEventsByIdOptions({
        path: { id: eventId },
      }),
    });
  };

  return {
    events: events ?? [],
    locations: locations ?? [],
    getEventById: getEventById,
    isLoading: eventsIsLoading || locationsIsLoading,
    createReservation,
  };
}
