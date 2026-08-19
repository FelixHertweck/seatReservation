"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";

import { useT } from "@/lib/i18n/hooks";
import type {
  BlockSeatsRequestDto,
  EventUserAllowancesDto,
  ReservationRequestDto,
  ReservationResponseDto,
} from "@/api";
import {
  getApiManagerReservationsExportByEventIdCsv,
  getApiManagerReservationsExportByEventIdPdf,
} from "@/api";
import {
  getApiManagerEventsOptions,
  getApiManagerEventlocationsOptions,
  getApiUsersManagerOptions,
  getApiManagerSeatsOptions,
  getApiManagerAreasOptions,
  getApiManagerMarkersOptions,
  getApiManagerOverviewQueryKey,
  getApiManagerReservationAllowanceEventByEventIdOptions,
  getApiManagerReservationAllowanceEventByEventIdQueryKey,
  getApiManagerReservationsEventByIdOptions,
  getApiManagerReservationsEventByIdQueryKey,
  postApiManagerReservationsMutation,
  postApiManagerReservationsBlockMutation,
  postApiManagerReservationsResendConfirmationByEventIdByUserIdMutation,
  deleteApiManagerReservationsMutation,
} from "@/api/@tanstack/react-query.gen";
import type { ErrorWithResponse } from "@/components/init-query-client";

export function useManagementReservations(eventId: string | null) {
  const t = useT();
  const queryClient = useQueryClient();

  const { data: events, isLoading: eventsLoading } = useQuery({
    ...getApiManagerEventsOptions(),
  });
  const { data: locations, isLoading: locationsLoading } = useQuery({
    ...getApiManagerEventlocationsOptions(),
  });
  const { data: users, isLoading: usersLoading } = useQuery({
    ...getApiUsersManagerOptions(),
  });

  const selectedEvent = (events ?? []).find((e) => e.id === eventId);
  const locationId = selectedEvent?.eventLocationId;

  const { data: areas } = useQuery({
    ...getApiManagerAreasOptions({
      query: { eventLocationId: locationId ?? "" },
    }),
    enabled: !!locationId,
  });

  const { data: markers } = useQuery({
    ...getApiManagerMarkersOptions({
      query: { eventLocationId: locationId ?? "" },
    }),
    enabled: !!locationId,
  });

  const { data: seats, isLoading: seatsLoading } = useQuery({
    ...getApiManagerSeatsOptions({
      query: { eventLocationId: locationId ?? "" },
    }),
    enabled: !!locationId,
  });

  const { data: reservations, isLoading: reservationsLoading } = useQuery({
    ...getApiManagerReservationsEventByIdOptions({
      path: { id: eventId ?? "" },
    }),
    enabled: !!eventId,
  });

  const { data: allowances } = useQuery({
    ...getApiManagerReservationAllowanceEventByEventIdOptions({
      path: { eventId: eventId ?? "" },
    }),
    enabled: !!eventId,
  });

  const createMutation = useMutation({
    ...postApiManagerReservationsMutation(),
  });
  const blockMutation = useMutation({
    ...postApiManagerReservationsBlockMutation(),
  });
  const deleteMutation = useMutation({
    ...deleteApiManagerReservationsMutation(),
  });

  const appendReservations = (
    newReservations: ReservationResponseDto[],
    targetEventId?: string,
  ) => {
    const evtId = targetEventId ?? eventId;
    if (!evtId) return;
    const updater = (old: ReservationResponseDto[] | undefined) => {
      if (!old) return newReservations;
      const existingIds = new Set(old.map((r) => r.id));
      const toAdd = newReservations.filter((r) => !existingIds.has(r.id));
      return [...old, ...toAdd];
    };
    queryClient.setQueriesData(
      {
        queryKey: getApiManagerReservationsEventByIdQueryKey({
          path: { id: evtId },
        }),
      },
      updater,
    );
  };

  const removeReservations = (ids: string[], targetEventId?: string) => {
    const evtId = targetEventId ?? eventId;
    if (!evtId) return;
    const idsSet = new Set(ids);
    const updater = (old: ReservationResponseDto[] | undefined) =>
      old
        ? old.filter(
            (r) => !idsSet.has(r.id ?? "") && !idsSet.has(r.seatId ?? ""),
          )
        : [];
    queryClient.setQueriesData(
      {
        queryKey: getApiManagerReservationsEventByIdQueryKey({
          path: { id: evtId },
        }),
      },
      updater,
    );
  };

  const deductUserAllowance = (
    userId: string,
    count: number,
    targetEventId?: string,
  ) => {
    const evtId = targetEventId ?? eventId;
    if (!evtId) return;
    const updater = (old: EventUserAllowancesDto[] | undefined) => {
      if (!old) return old;
      return old.map((a) => {
        if (a.userId?.toString() === userId) {
          return {
            ...a,
            reservationsAllowedCount: Math.max(
              0,
              (a.reservationsAllowedCount ?? 0) - count,
            ),
          };
        }
        return a;
      });
    };
    queryClient.setQueriesData(
      {
        queryKey: getApiManagerReservationAllowanceEventByEventIdQueryKey({
          path: { eventId: evtId },
        }),
      },
      updater,
    );
  };

  const invalidateAggregates = () => {
    queryClient.invalidateQueries({
      queryKey: getApiManagerEventsOptions().queryKey,
    });
    queryClient.invalidateQueries({
      queryKey: getApiManagerOverviewQueryKey(),
    });
  };

  const createReservation = async (data: ReservationRequestDto) => {
    const request = createMutation.mutateAsync({ body: data }).then((res) => {
      appendReservations(res, data.eventId);
      if (data.deductAllowance && data.userId) {
        deductUserAllowance(data.userId, data.seatIds.length, data.eventId);
      }
      invalidateAggregates();
      return res;
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("management.reservations.createSuccess"),
      error: (error: ErrorWithResponse) => ({
        message: t("management.reservations.createError"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const blockSeats = async (data: BlockSeatsRequestDto) => {
    const request = blockMutation.mutateAsync({ body: data }).then((res) => {
      appendReservations(res, data.eventId);
      invalidateAggregates();
      return res;
    });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("management.reservations.blockSuccess"),
      error: (error: ErrorWithResponse) => ({
        message: t("management.reservations.blockError"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const deleteReservations = async (ids: string[]) => {
    const request = deleteMutation
      .mutateAsync({ query: { ids } })
      .then((res) => {
        removeReservations(ids, eventId ?? undefined);
        invalidateAggregates();
        return res;
      });
    toast.promise(request, {
      loading: t("common.loading"),
      success: t("management.reservations.deleteSuccess"),
      error: (error: ErrorWithResponse) => ({
        message: t("management.reservations.deleteError"),
        description: error.response?.description ?? t("common.error.default"),
      }),
    });
    return request;
  };

  const exportCsv = async (id: string): Promise<Blob> => {
    const response = await getApiManagerReservationsExportByEventIdCsv({
      path: { eventId: id },
    });
    return new Blob([response.data as string], {
      type: "text/csv;charset=utf-8;",
    });
  };

  const exportPdf = async (id: string): Promise<Blob> => {
    const response = await getApiManagerReservationsExportByEventIdPdf({
      path: { eventId: id },
    });
    return new Blob([response.data as File], { type: "application/pdf" });
  };

  const resendConfirmationMutation = useMutation({
    ...postApiManagerReservationsResendConfirmationByEventIdByUserIdMutation(),
  });

  const resendConfirmationEmail = async (evtId: string, usrId: string) => {
    return resendConfirmationMutation.mutateAsync({
      path: { eventId: evtId, userId: usrId },
    });
  };

  return {
    events: events ?? [],
    locations: locations ?? [],
    users: users ?? [],
    seats: seats ?? [],
    areas: areas ?? [],
    markers: markers ?? [],
    reservations: reservations ?? [],
    allowances: allowances ?? [],
    isLoading: eventsLoading || locationsLoading || usersLoading,
    isSeatsLoading: !!locationId && seatsLoading,
    isReservationsLoading: reservationsLoading,
    createReservation,
    blockSeats,
    deleteReservations,
    exportCsv,
    exportPdf,
    resendConfirmationEmail,
  };
}
