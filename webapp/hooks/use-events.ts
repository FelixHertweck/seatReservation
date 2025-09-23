"use client";

import { useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "@/hooks/use-toast";
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
  getEventById: (id: bigint) => Promise<UserEventResponseDto>;
  isLoading: boolean;
  createReservation: (
    eventId: bigint,
    seatIds: bigint[],
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
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiUserReservationsQueryKey() },
        (oldData: UserReservationResponseDto[] | undefined) => {
          return oldData ? [...oldData, ...data] : [...data];
        },
      );
      toast({
        title: t("reservation.create.success.title"),
        description: t("reservation.create.success.description"),
      });
    },
  });

  const createReservation = async (
    eventId: bigint,
    seatIds: bigint[],
  ): Promise<UserReservationResponseDto[]> => {
    const data: UserReservationsRequestDto = {
      eventId,
      seatIds,
    };
    return createReservationMutation.mutateAsync({
      body: data,
    });
  };

  useEffect(() => {
    if (createReservationMutation.isSuccess) {
      queryClient.invalidateQueries({ queryKey: getApiUserEventsQueryKey() });
    }
  }, [createReservationMutation.isSuccess, queryClient]);

  const getEventById = (eventId: bigint) => {
    return queryClient.fetchQuery({
      ...getApiManagerEventsByIdOptions({
        path: { id: BigInt(eventId.toString()) },
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
