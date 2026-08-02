"use client";

import {
  useMutation,
  useQueries,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import { toast } from "sonner";

import { useT } from "@/lib/i18n/hooks";
import type {
  BlockSeatsRequestDto,
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
  getApiManagerReservationsEventByIdOptions,
  getApiManagerReservationsEventByIdQueryKey,
  postApiManagerReservationsMutation,
  postApiManagerReservationsBlockMutation,
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

  // Broad seats across every location - the reused ReservationFormModal /
  // BlockSeatsModal each have their own internal event picker, so they need
  // seats for whichever location that picker resolves to, not just the one
  // currently selected on this page.
  const seatsQueries = useQueries({
    queries: (locations ?? [])
      .filter((l) => l.id)
      .map((l) => ({
        ...getApiManagerSeatsOptions({ query: { eventLocationId: l.id! } }),
      })),
  });
  const seats = seatsQueries.flatMap((q) => q.data ?? []);
  // While locations are still loading, seatsQueries is empty and `.some`
  // would report `false` even though nothing has actually loaded yet.
  const seatsLoading =
    locationsLoading || seatsQueries.some((q) => q.isLoading);

  const { data: reservations, isLoading: reservationsLoading } = useQuery({
    ...getApiManagerReservationsEventByIdOptions({
      path: { id: eventId ?? "" },
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

  const invalidateForEvent = () => {
    if (!eventId) return;
    queryClient.invalidateQueries({
      queryKey: getApiManagerReservationsEventByIdQueryKey({
        path: { id: eventId },
      }),
    });
    queryClient.invalidateQueries({
      queryKey: getApiManagerEventsOptions().queryKey,
    });
  };

  const createReservation = async (data: ReservationRequestDto) => {
    const request = createMutation.mutateAsync({ body: data }).then((res) => {
      invalidateForEvent();
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
      invalidateForEvent();
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
        queryClient.setQueriesData(
          {
            queryKey: getApiManagerReservationsEventByIdQueryKey({
              path: { id: eventId ?? "" },
            }),
          },
          (old: ReservationResponseDto[] | undefined) =>
            old ? old.filter((r) => !ids.includes(r.id ?? "")) : [],
        );
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

  return {
    events: events ?? [],
    locations: locations ?? [],
    users: users ?? [],
    seats,
    reservations: reservations ?? [],
    isLoading: eventsLoading || locationsLoading || usersLoading,
    isSeatsLoading: seatsLoading,
    isReservationsLoading: reservationsLoading,
    createReservation,
    blockSeats,
    deleteReservations,
    exportCsv,
    exportPdf,
  };
}
