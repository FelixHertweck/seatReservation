"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { useT } from "@/lib/i18n/hooks";
import type {
  EventUserAllowanceUpdateDto,
  EventUserAllowancesCreateDto,
  EventUserAllowancesDto,
} from "@/api";
import {
  getApiManagerEventsOptions,
  getApiUsersManagerOptions,
  getApiManagerReservationAllowanceEventByEventIdOptions,
  getApiManagerReservationAllowanceEventByEventIdQueryKey,
  getApiManagerSeatsOptions,
  postApiManagerReservationAllowanceMutation,
  putApiManagerReservationAllowanceMutation,
  deleteApiManagerReservationAllowanceMutation,
} from "@/api/@tanstack/react-query.gen";
import type { ErrorWithResponse } from "@/components/init-query-client";

export function useManagementAllowances(eventId: string | null) {
  const t = useT();
  const queryClient = useQueryClient();

  const { data: events, isLoading: eventsLoading } = useQuery({
    ...getApiManagerEventsOptions(),
  });
  const { data: users, isLoading: usersLoading } = useQuery({
    ...getApiUsersManagerOptions(),
  });

  const eventLocationId = events?.find(
    (e) => e.id === eventId,
  )?.eventLocationId;
  const { data: locationSeats, isLoading: capacityLoading } = useQuery({
    ...getApiManagerSeatsOptions({ query: { eventLocationId } }),
    enabled: !!eventLocationId,
  });
  const capacity = locationSeats?.length ?? 0;

  const { data: allowances, isLoading: allowancesLoading } = useQuery({
    ...getApiManagerReservationAllowanceEventByEventIdOptions({
      path: { eventId: eventId ?? "" },
    }),
    enabled: !!eventId,
  });

  const createMutation = useMutation({
    ...postApiManagerReservationAllowanceMutation(),
  });
  const updateMutation = useMutation({
    ...putApiManagerReservationAllowanceMutation(),
  });
  const deleteMutation = useMutation({
    ...deleteApiManagerReservationAllowanceMutation(),
  });

  const queryKey = () =>
    getApiManagerReservationAllowanceEventByEventIdQueryKey({
      path: { eventId: eventId ?? "" },
    });

  const grantAllowances = async (data: EventUserAllowancesCreateDto) => {
    const request = createMutation.mutateAsync({ body: data }).then((res) => {
      queryClient.setQueriesData(
        { queryKey: queryKey() },
        (old: EventUserAllowancesDto[] | undefined) =>
          old ? [...old, ...res] : [...res],
      );
      return res;
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("management.allowances.grantSuccess"),
      error: (error: ErrorWithResponse) => ({
        message: t("management.allowances.grantError"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const updateAllowance = async (data: EventUserAllowanceUpdateDto) => {
    const request = updateMutation.mutateAsync({ body: data }).then((res) => {
      queryClient.setQueriesData(
        { queryKey: queryKey() },
        (old: EventUserAllowancesDto[] | undefined) =>
          old ? old.map((a) => (a.id === res.id ? res : a)) : [res],
      );
      return res;
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("management.allowances.updateSuccess"),
      error: (error: ErrorWithResponse) => ({
        message: t("management.allowances.updateError"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const deleteAllowances = async (ids: string[]) => {
    const request = deleteMutation
      .mutateAsync({ query: { ids } })
      .then((res) => {
        queryClient.setQueriesData(
          { queryKey: queryKey() },
          (old: EventUserAllowancesDto[] | undefined) =>
            old ? old.filter((a) => !ids.includes(a.id ?? "")) : [],
        );
        return res;
      });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("management.allowances.deleteSuccess"),
      error: (error: ErrorWithResponse) => ({
        message: t("management.allowances.deleteError"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  return {
    events: events ?? [],
    users: users ?? [],
    allowances: allowances ?? [],
    isLoading: eventsLoading || usersLoading,
    isAllowancesLoading: allowancesLoading,
    capacity,
    isCapacityLoading: capacityLoading,
    grantAllowances,
    updateAllowance,
    deleteAllowances,
  };
}
