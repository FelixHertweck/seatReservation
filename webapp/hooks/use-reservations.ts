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
import type { UserReservationResponseDto } from "@/api";

export function useReservations() {
  const { data: reservations, isLoading } = useQuery({
    ...getApiUserReservationsOptions(),
  });

  const queryClient = useQueryClient();
  const { t } = useTranslation();

  const deleteMutation = useMutation({
    ...deleteApiUserReservationsByIdMutation(),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: getApiUserEventsQueryKey(),
      });
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
          oldData: UserReservationResponseDto[] | undefined,
        ): UserReservationResponseDto[] => {
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
