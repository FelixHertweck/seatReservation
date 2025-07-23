"use client";

import { useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  getApiUserReservationsOptions,
  deleteApiUserReservationsByIdMutation,
  getApiUserReservationsQueryKey,
} from "@/api/@tanstack/react-query.gen";
import type { ReservationResponseDto } from "@/api";

export function useReservations() {
  const { data: reservations, isLoading } = useQuery({
    ...getApiUserReservationsOptions(),
  });

  const queryClient = useQueryClient();

  const deleteMutation = useMutation({
    ...deleteApiUserReservationsByIdMutation(),
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
