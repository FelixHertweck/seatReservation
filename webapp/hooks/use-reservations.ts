"use client";

import { useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "@/hooks/use-toast";
import {
  getApiUserReservationsOptions,
  deleteApiUserReservationsByIdMutation,
  getApiUserReservationsQueryKey,
  getApiUserEventsQueryKey,
} from "@/api/@tanstack/react-query.gen";
import type { EventResponseDto, ReservationResponseDto } from "@/api";

export function useReservations() {
  const { data: reservations, isLoading } = useQuery({
    ...getApiUserReservationsOptions(),
  });

  const queryClient = useQueryClient();
  const { t } = useTranslation();

  const deleteMutation = useMutation({
    ...deleteApiUserReservationsByIdMutation(),
    onSuccess: () => {
      queryClient.setQueriesData(
        { queryKey: getApiUserEventsQueryKey() },
        (oldData: EventResponseDto[] | undefined): EventResponseDto[] => {
          return (oldData ?? []).map((event) => {
            if (event.id === deleteMutation.variables?.path.id) {
              return {
                ...event,
                availableSeats: (event.reservationsAllowed ?? 0) + 1,
              };
            }
            return event;
          });
        },
      );
      toast({
        title: t("reservation.delete.success.title"),
        description: t("reservation.delete.success.description"),
      });
    },
  });

  const deleteReservation = async (id: bigint) => {
    await deleteMutation.mutateAsync({
      path: {
        id,
      },
    });
  };

  useEffect(() => {
    if (deleteMutation.isSuccess) {
      queryClient.setQueriesData(
        { queryKey: getApiUserReservationsQueryKey() },
        (
          oldData: ReservationResponseDto[] | undefined,
        ): ReservationResponseDto[] => {
          return (oldData ?? []).filter(
            (reservation) =>
              reservation.id !== deleteMutation.variables?.path.id,
          );
        },
      );
    }
  }, [
    deleteMutation.isSuccess,
    deleteMutation.variables?.path.id,
    queryClient,
  ]);

  return {
    reservations: reservations ?? [],
    isLoading,
    deleteReservation,
  };
}
