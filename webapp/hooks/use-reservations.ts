"use client";

import { useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "@/hooks/use-toast";
import {
  getApiUserReservationsOptions,
  getApiUserReservationsQueryKey,
  getApiUserEventsQueryKey,
  deleteApiUserReservationsMutation,
} from "@/api/@tanstack/react-query.gen";
import { type UserReservationResponseDto } from "@/api";

export function useReservations() {
  const { data: reservations, isLoading } = useQuery({
    ...getApiUserReservationsOptions(),
  });

  const queryClient = useQueryClient();
  const { t } = useTranslation();

  const deleteMutation = useMutation({
    ...deleteApiUserReservationsMutation(),
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

  const deleteReservation = async (ids: bigint[]) => {
    await deleteMutation.mutateAsync({
      query: {
        ids,
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
          const idsSet = new Set(deleteMutation.variables?.query?.ids ?? []);
          return (oldData ?? []).filter(
            (reservation) => !idsSet.has(reservation.id ?? BigInt(-1)),
          );
        },
      );
    }
  }, [
    deleteMutation.isSuccess,
    deleteMutation.variables?.query?.ids,
    queryClient,
  ]);

  return {
    reservations: reservations ?? [],
    isLoading,
    deleteReservation,
  };
}
