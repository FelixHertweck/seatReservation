"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { toast } from "sonner";

import { useT } from "@/lib/i18n/hooks";
import type {
  BoxOfficeGuestReservationRequestDto,
  BoxOfficeReservationRequestDto,
} from "@/api";
import {
  getApiSupervisorBoxofficeUsersOptions,
  postApiSupervisorBoxofficeEventsByEventIdReleaseMutation,
  postApiSupervisorBoxofficeReservationsMutation,
  postApiSupervisorBoxofficeReservationsGuestMutation,
} from "@/api/@tanstack/react-query.gen";
import type { ErrorWithResponse } from "@/components/init-query-client";

export function useBoxOffice() {
  const t = useT();

  const { data: users, isLoading: isLoadingUsers } = useQuery({
    ...getApiSupervisorBoxofficeUsersOptions(),
  });

  const knownUserMutation = useMutation({
    ...postApiSupervisorBoxofficeReservationsMutation(),
  });
  const guestMutation = useMutation({
    ...postApiSupervisorBoxofficeReservationsGuestMutation(),
  });

  const cancelUncheckedMutation = useMutation({
    ...postApiSupervisorBoxofficeEventsByEventIdReleaseMutation(),
  });

  const createForKnownUser = async (data: BoxOfficeReservationRequestDto) => {
    const request = knownUserMutation.mutateAsync({ body: data });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("boxOffice.createSuccess"),
      error: (error: ErrorWithResponse) => ({
        message: t("boxOffice.createError"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const createForGuest = async (data: BoxOfficeGuestReservationRequestDto) => {
    const request = guestMutation.mutateAsync({ body: data });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("boxOffice.createSuccess"),
      error: (error: ErrorWithResponse) => ({
        message: t("boxOffice.createError"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const cancelUnchecked = (eventId: string) => {
    const request = cancelUncheckedMutation.mutateAsync({ path: { eventId } });
    toast.promise(request, {
      loading: t("common.loading"),
      success: (result) =>
        t("boxOffice.cancelUnchecked.success", {
          count: result.releasedCount ?? 0,
        }),
      error: (error: ErrorWithResponse) => ({
        message: t("boxOffice.cancelUnchecked.error"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
  };

  return {
    users: users ?? [],
    isLoadingUsers,
    createForKnownUser,
    createForGuest,
    isSubmitting: knownUserMutation.isPending || guestMutation.isPending,
    cancelUnchecked,
    isCancellingUnchecked: cancelUncheckedMutation.isPending,
  };
}
