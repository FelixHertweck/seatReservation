"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useT } from "@/lib/i18n/hooks";
import {
  getApiUserEventsOptions,
  postApiUserReservationsMutation,
  getApiUserEventsQueryKey,
  getApiManagerEventsByIdOptions,
  getApiUserReservationsQueryKey,
  getApiUserLocationsOptions,
} from "@/api/@tanstack/react-query.gen";
import type {
  UserEventLocationResponseDto,
  UserEventResponseDto,
  UserReservationResponseDto,
  UserReservationsRequestDto,
} from "@/api";

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
    const request = createReservationMutation.mutateAsync({
      body: data,
    });

    toast.promise(request, {
      loading: t("common.loading"),
      success: (resultData) => {
        queryClient.setQueriesData(
          { queryKey: getApiUserReservationsQueryKey() },
          (oldData: UserReservationResponseDto[] | undefined) => {
            return oldData ? [...oldData, ...resultData] : [...resultData];
          },
        );
        queryClient.invalidateQueries({
          queryKey: getApiUserEventsQueryKey(),
        });
        return t("reservation.create.success.title");
      },
      error: t("reservation.create.error.title"),
    });
    return request;
  };

  const getEventById = (eventId: string) => {
    return queryClient.fetchQuery({
      ...getApiManagerEventsByIdOptions({
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
