"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import {
  getApiUserReservationsOptions,
  getApiUserReservationsQueryKey,
  getApiUserEventsQueryKey,
  deleteApiUserReservationsMutation,
} from "@/api/@tanstack/react-query.gen";
import { type UserReservationResponseDto } from "@/api";
import { ErrorWithResponse } from "@/components/init-query-client";

export function useReservations() {
  const { data: reservations, isLoading } = useQuery({
    ...getApiUserReservationsOptions(),
  });

  const queryClient = useQueryClient();
  const { t } = useTranslation();

  const deleteMutation = useMutation({
    ...deleteApiUserReservationsMutation(),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: getApiUserEventsQueryKey(),
      });
      queryClient.setQueriesData(
        { queryKey: getApiUserReservationsQueryKey() },
        (
          oldData: UserReservationResponseDto[] | undefined,
        ): UserReservationResponseDto[] => {
          const idsSet = new Set(variables.query?.ids);
          return (oldData ?? []).filter(
            (reservation) => !idsSet.has(reservation.id ?? BigInt(-1)),
          );
        },
      );
    },
  });

  const deleteReservation = async (ids: bigint[]) => {
    const request = deleteMutation.mutateAsync({
      query: {
        ids,
      },
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("reservation.delete.success.title"),
      error: (error: ErrorWithResponse) => ({
        message: t("reservation.delete.error.title"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  return {
    reservations: reservations ?? [],
    isLoading,
    deleteReservation,
  };
}
