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
} from "@/api/@tanstack/react-query.gen";
import type {
  EventResponseDto,
  ReservationResponseDto,
  ReservationsRequestCreateDto,
} from "@/api";

interface UseEventsReturn {
  events: EventResponseDto[];
  getEventById: (id: bigint) => Promise<EventResponseDto>;
  isLoading: boolean;
  createReservation: (
    eventId: bigint,
    seatIds: bigint[],
  ) => Promise<ReservationResponseDto[]>;
}

export function useEvents(): UseEventsReturn {
  const t = useT();
  const { data: events, isLoading: eventsIsLoading } = useQuery({
    ...getApiUserEventsOptions(),
  });

  const queryClient = useQueryClient();

  const createReservationMutation = useMutation({
    ...postApiUserReservationsMutation(),
    onSuccess: (data) => {
      queryClient.setQueriesData(
        { queryKey: getApiUserReservationsQueryKey() },
        (oldData: ReservationResponseDto[] | undefined) => {
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
  ): Promise<ReservationResponseDto[]> => {
    const data: ReservationsRequestCreateDto = {
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
    getEventById: getEventById,
    isLoading: eventsIsLoading,
    createReservation,
  };
}
